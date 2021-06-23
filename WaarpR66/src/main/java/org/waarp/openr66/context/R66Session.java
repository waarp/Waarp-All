/*
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 * Waarp . If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.context;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.exception.IllegalFiniteStateException;
import org.waarp.common.exception.NoRestartException;
import org.waarp.common.file.SessionInterface;
import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.state.MachineState;
import org.waarp.compress.WaarpZstdCodec;
import org.waarp.openr66.context.authentication.R66Auth;
import org.waarp.openr66.context.filesystem.R66Dir;
import org.waarp.openr66.context.filesystem.R66File;
import org.waarp.openr66.context.filesystem.R66Restart;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.database.data.DbTaskRunner.TASKSTEP;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.compression.ZstdCompressionCodecDataPacket;
import org.waarp.openr66.protocol.utils.FileUtils;

import java.io.File;
import java.lang.ref.SoftReference;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The global object session in OpenR66, a session by local channel
 */
public class R66Session implements SessionInterface {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(R66Session.class);
  private static final String FILE_IS_IN_THROUGH_MODE =
      "File is in through mode: {}";
  private static final String FILE_CANNOT_BE_WRITE = "File cannot be write";

  /**
   * Global Codec for packet compression
   */
  private static final ZstdCompressionCodecDataPacket codec =
      Configuration.configuration.isCompressionAvailable()?
          new ZstdCompressionCodecDataPacket() : null;

  /**
   * Block size used during file transfer
   */
  private int blockSize = Configuration.configuration.getBlockSize();
  /**
   * The local channel reference
   */
  private LocalChannelReference localChannelReference;
  /**
   * Authentication
   */
  private final R66Auth auth;
  /**
   * Remote Address
   */
  private SocketAddress raddress;
  /**
   * Local Address
   */
  private SocketAddress laddress;

  /**
   * Current directory
   */
  private final R66Dir dir;
  /**
   * Current file
   */
  private R66File file;
  /**
   * Does this session is Ready to serve a request
   */
  private boolean isReady;
  /**
   * Used to prevent deny of service
   */
  private final AtomicInteger numOfError = new AtomicInteger(0);

  /**
   * Current Restart information
   */
  private final R66Restart restart;

  /**
   * DbTaskRunner
   */
  private DbTaskRunner runner;

  private String status = "NoStatus";

  /**
   * The Finite Machine State
   */
  private final MachineState<R66FiniteDualStates> state;
  /**
   * Business Object if used
   */
  private R66BusinessInterface businessObject;
  /**
   * Extended protocol or not
   */
  private final boolean extendedProtocol =
      Configuration.configuration.isExtendedProtocol();

  private final HashMap<String, R66Dir> dirsFromSession =
      new HashMap<String, R66Dir>();
  private SoftReference<byte[]> reusableBuffer = null;
  private SoftReference<byte[]> reusableDataPacketBuffer = null;
  private SoftReference<byte[]> reusableCompressionBuffer = null;
  private FilesystemBasedDigest digestBlock = null;
  private boolean isSender;
  private boolean isCompressionEnabled;
  private int compressionMaxSize =
      WaarpZstdCodec.getMaxCompressedSize(blockSize);

  /**
   * @return the compression codec
   */
  public static ZstdCompressionCodecDataPacket getCodec() {
    return codec;
  }

  /**
   * Create the session
   */
  public R66Session() {
    isReady = false;
    auth = new R66Auth(this);
    dir = new R66Dir(this);
    restart = new R66Restart(this);
    state = R66FiniteDualStates.newSessionMachineState();
    isCompressionEnabled = Configuration.configuration.isCompressionAvailable();
  }

  /**
   * @return extendedProtocol
   */
  public boolean getExtendedProtocol() {
    return extendedProtocol;
  }

  /**
   * @return the businessObject
   */
  public R66BusinessInterface getBusinessObject() {
    return businessObject;
  }

  /**
   * @param businessObject the businessObject to set
   */
  public void setBusinessObject(final R66BusinessInterface businessObject) {
    this.businessObject = businessObject;
  }

  /**
   * Propose a new State
   *
   * @param desiredstate
   *
   * @throws IllegalFiniteStateException if the new status if not ok
   */
  public void newState(final R66FiniteDualStates desiredstate) {
    try {
      state.setCurrent(desiredstate);
    } catch (final IllegalFiniteStateException e) {
      logger
          .warn("Should not changed of State: {} {}", this, e.getMessage(), e);
      state.setDryCurrent(desiredstate);
    }
  }

  public void setErrorState() {
    try {
      state.setCurrent(R66FiniteDualStates.ERROR);
    } catch (final IllegalFiniteStateException e) {
      logger.error("Couldn't pass to error state. This should not happen");
    }
  }

  /**
   * @return the current state in the finite state machine
   */
  public R66FiniteDualStates getState() {
    return state.getCurrent();
  }

  /**
   * Debugging purpose (trace)
   *
   * @param stat
   */
  public void setStatus(final int stat) {
    if (logger.isDebugEnabled()) {
      final StackTraceElement elt = Thread.currentThread().getStackTrace()[2];
      status =
          '(' + elt.getFileName() + ':' + elt.getLineNumber() + "):" + stat;
    } else {
      status = ":" + stat;
    }
  }

  @Override
  public void clear() {
    partialClear();
    if (dir != null) {
      dir.clear();
    }
    if (auth != null) {
      auth.clear();
    }
    if (runner != null) {
      runner.clear();
    }
    if (state != null) {
      try {
        state.setCurrent(R66FiniteDualStates.CLOSEDCHANNEL);
      } catch (final IllegalFiniteStateException ignored) {
        // nothing
      }
      R66FiniteDualStates.endSessionMachineSate(state);
    }
  }

  public void partialClear() {
    // First check if a transfer was on going
    if (runner != null && !runner.isFinished() && !runner.continueTransfer()) {
      if (localChannelReference != null) {
        if (!localChannelReference.getFutureRequest().isDone()) {
          final R66Result result = new R66Result(
              new OpenR66RunnerErrorException("Close before ending"), this,
              true, ErrorCode.Disconnection,
              runner);// True since called from closed
          result.setRunner(runner);
          try {
            setFinalizeTransfer(false, result);
          } catch (final OpenR66RunnerErrorException ignored) {
            // nothing
          } catch (final OpenR66ProtocolSystemException ignored) {
            // nothing
          }
        }
      }
    }
    // No clean of file since it can be used after channel is closed
    isReady = false;
    if (businessObject != null) {
      businessObject.releaseResources(this);
      businessObject = null;
    }
    digestBlock = null;
    if (reusableBuffer != null) {
      reusableBuffer.clear();
    }
    reusableBuffer = null;
    if (reusableDataPacketBuffer != null) {
      reusableDataPacketBuffer.clear();
    }
    reusableDataPacketBuffer = null;
    if (reusableCompressionBuffer != null) {
      reusableCompressionBuffer.clear();
    }
    reusableCompressionBuffer = null;
  }

  @Override
  public R66Auth getAuth() {
    return auth;
  }

  @Override
  public int getBlockSize() {
    return blockSize;
  }

  /**
   * @param blocksize the blocksize to set
   */
  public void setBlockSize(final int blocksize) {
    blockSize = blocksize;
    compressionMaxSize = WaarpZstdCodec.getMaxCompressedSize(blockSize);
  }

  private SoftReference<byte[]> getBuffer(SoftReference<byte[]> softReference,
                                          final int length) {
    if (softReference == null) {
      softReference = new SoftReference<byte[]>(new byte[length]);
      return softReference;
    }
    final byte[] buffer = softReference.get();
    if (buffer != null && buffer.length >= length) {
      return softReference;
    }
    softReference.clear();
    softReference = new SoftReference<byte[]>(new byte[length]);
    return softReference;
  }

  /**
   * @param length the target size
   *
   * @return the reusable buffer for sending per Session
   */
  public byte[] getReusableBuffer(final int length) {
    reusableBuffer = getBuffer(reusableBuffer, length);
    return reusableBuffer.get();
  }

  /**
   * @param length the target size
   *
   * @return the reusable buffer for received DataPacket per Session
   */
  public byte[] getReusableDataPacketBuffer(final int length) {
    reusableDataPacketBuffer = getBuffer(reusableDataPacketBuffer, length);
    return reusableDataPacketBuffer.get();
  }

  /**
   * @param length the original size
   *
   * @return the possible reusable compression buffer per Session
   */
  public byte[] getSessionReusableCompressionBuffer(final int length) {
    reusableCompressionBuffer = getBuffer(reusableCompressionBuffer,
                                          WaarpZstdCodec
                                              .getMaxCompressedSize(length));
    return reusableCompressionBuffer.get();
  }

  /**
   * @return True if compression is enabled in this session
   */
  public boolean isCompressionEnabled() {
    return isCompressionEnabled;
  }

  /**
   * @param compressionEnabled True if compression is enabled in this session
   */
  public void setCompressionEnabled(final boolean compressionEnabled) {
    isCompressionEnabled = compressionEnabled;
    logger.debug("Compression enabled? {}", compressionEnabled);
  }

  /**
   * @return the current max compression size according to block size
   */
  public int getCompressionMaxSize() {
    return compressionMaxSize;
  }

  /**
   * Initialize per block digest
   */
  public void initializeDigest() {
    if (digestBlock == null && RequestPacket.isMD5Mode(getRunner().getMode())) {
      try {
        digestBlock = new FilesystemBasedDigest(
            localChannelReference.getPartner().getDigestAlgo());
      } catch (final NoSuchAlgorithmException e) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      }
    }
  }

  /**
   * @return the digest used per block
   */
  public FilesystemBasedDigest getDigestBlock() {
    return digestBlock;
  }

  @Override
  public R66Dir getDir() {
    return dir;
  }

  @Override
  public FilesystemBasedFileParameterImpl getFileParameter() {
    return Configuration.getFileParameter();
  }

  @Override
  public R66Restart getRestart() {
    return restart;
  }

  /**
   * @return True if the connection is currently authenticated
   */
  public boolean isAuthenticated() {
    if (auth == null) {
      return false;
    }
    return auth.isIdentified();
  }

  /**
   * @return True if the Channel is ready to accept transfer
   */
  public boolean isReady() {
    return isReady;
  }

  /**
   * @param isReady the isReady for transfer to set
   */
  public void setReady(final boolean isReady) {
    this.isReady = isReady;
  }

  /**
   * @return the runner
   */
  public DbTaskRunner getRunner() {
    return runner;
  }

  /**
   * @param localChannelReference the localChannelReference to set
   */
  public void setLocalChannelReference(
      final LocalChannelReference localChannelReference) {
    this.localChannelReference = localChannelReference;
    this.localChannelReference.setSession(this);
    if (this.localChannelReference.getNetworkChannel() != null) {
      raddress = this.localChannelReference.getNetworkChannel().remoteAddress();
      laddress = this.localChannelReference.getNetworkChannel().localAddress();
    } else {
      raddress = laddress = new InetSocketAddress(0);
    }
  }

  /**
   * @return the remote SocketAddress
   */
  public SocketAddress getRemoteAddress() {
    return raddress;
  }

  /**
   * @return the local SocketAddress
   */
  public SocketAddress getLocalAddress() {
    return laddress;
  }

  /**
   * @return the localChannelReference
   */
  public LocalChannelReference getLocalChannelReference() {
    return localChannelReference;
  }

  /**
   * To be called in case of No Session not from a valid LocalChannelHandler
   *
   * @param runner
   * @param localChannelReference
   */
  public void setNoSessionRunner(final DbTaskRunner runner,
                                 final LocalChannelReference localChannelReference) {
    this.runner = runner;
    // Warning: the file is not correctly setup
    auth.specialNoSessionAuth(false, Configuration.configuration.getHostId());
    try {
      file = (R66File) dir.setFile(this.runner.getFilename(), false);
    } catch (final CommandAbstractException ignored) {
      // nothing
    }
    this.localChannelReference = localChannelReference;
    if (this.localChannelReference == null) {
      if (this.runner.getLocalChannelReference() != null) {
        this.localChannelReference = this.runner.getLocalChannelReference();
      } else {
        this.localChannelReference = new LocalChannelReference();
      }
      this.localChannelReference
          .setErrorMessage(this.runner.getErrorInfo().getMesg(),
                           this.runner.getErrorInfo());
    }
    runner.setLocalChannelReference(this.localChannelReference);
    this.localChannelReference.setSession(this);
  }

  /**
   * Set the File from the runner before PRE operation are done
   *
   * @throws OpenR66RunnerErrorException
   */
  public void setFileBeforePreRunner() throws OpenR66RunnerErrorException {
    // check first if the next step is the PRE task from beginning
    try {
      file = FileUtils.getFile(logger, this, runner.getOriginalFilename(),
                               runner.isPreTaskStarting(), isSender,
                               runner.isSendThrough(), file);
    } catch (final OpenR66RunnerErrorException e) {
      runner.setErrorExecutionStatus(ErrorCode.FileNotFound);
      throw e;
    }
    if (isSender && !runner.isSendThrough()) {
      // possibly resolved filename
      try {
        runner.setOriginalFilename(file.getFile());
        runner.setFilename(file.getFile());
        logger.debug("Old size: {} => {}", runner.getOriginalSize(),
                     file.length());
        if (runner.getOriginalSize() <= 0) {
          final long originalSize = file.length();
          if (originalSize > 0) {
            runner.setOriginalSize(originalSize);
          }
        }
      } catch (final CommandAbstractException e) {
        throw new OpenR66RunnerErrorException(e);
      }
    }
  }

  /**
   * Set the File from the runner once PRE operation are done, only for Receiver
   *
   * @param createFile When True, the file can be newly created if
   *     needed.
   *     If False, no new file will be
   *     created, thus having an Exception.
   *
   * @throws OpenR66RunnerErrorException
   * @throws CommandAbstractException only when new received created
   *     file
   *     cannot be created
   */
  public void setFileAfterPreRunnerReceiver(final boolean createFile)
      throws OpenR66RunnerErrorException, CommandAbstractException {
    if (businessObject != null) {
      businessObject.checkAtChangeFilename(this);
    }
    // File should not exist except if restart
    if (runner.getRank() > 0) {
      // Filename should be get back from runner load from database
      try {
        file = (R66File) dir.setFile(runner.getFilename(), true);
        if (runner.isRecvThrough()) {
          // no test on file since it does not really exist
          logger.debug(FILE_IS_IN_THROUGH_MODE, file);
        } else if (!file.canWrite()) {
          throw new OpenR66RunnerErrorException(FILE_CANNOT_BE_WRITE);
        }
      } catch (final CommandAbstractException e) {
        throw new OpenR66RunnerErrorException(e);
      }
    } else {
      // New FILENAME if necessary and store it
      if (createFile) {
        file = null;
        String newfilename = runner.getOriginalFilename();
        if (newfilename.charAt(1) == ':') {
          // Windows path
          newfilename = newfilename.substring(2);
        }
        newfilename = R66File.getBasename(newfilename);
        try {
          file = dir.setUniqueFile(runner.getSpecialId(), newfilename);
          runner.setFilename(file.getBasename());
        } catch (final CommandAbstractException e) {
          runner.deleteTempFile();
          throw e;
        }
        try {
          if (runner.isRecvThrough()) {
            // no test on file since it does not really exist
            logger.debug(FILE_IS_IN_THROUGH_MODE, file);
            runner.deleteTempFile();
          } else if (!file.canWrite()) {
            runner.deleteTempFile();
            throw new OpenR66RunnerErrorException(FILE_CANNOT_BE_WRITE);
          }
        } catch (final CommandAbstractException e) {
          runner.deleteTempFile();
          throw new OpenR66RunnerErrorException(e);
        }
      } else {
        throw new OpenR66RunnerErrorException("No file created");
      }
    }
    // Store TRUEFILENAME
    try {
      if (runner.isFileMoved()) {
        runner.setFileMoved(file.getFile(), true);
      } else {
        runner.setFilename(file.getFile());
      }
    } catch (final CommandAbstractException e) {
      runner.deleteTempFile();
      throw new OpenR66RunnerErrorException(e);
    }
  }

  /**
   * Set the File from the runner once PRE operation are done
   *
   * @param createFile When True, the file can be newly created if
   *     needed.
   *     If False, no new file will be
   *     created, thus having an Exception.
   *
   * @throws OpenR66RunnerErrorException
   * @throws CommandAbstractException only when new received created
   *     file
   *     cannot be created
   */
  public void setFileAfterPreRunner(final boolean createFile)
      throws OpenR66RunnerErrorException, CommandAbstractException {
    if (businessObject != null) {
      businessObject.checkAtChangeFilename(this);
    }
    // Now create the associated file
    if (isSender != runner.isSender()) {
      logger.warn("Not same SenderSide {} {}", isSender, runner.isSender());
    }
    if (isSender) {
      try {
        if (file == null) {
          try {
            file = (R66File) dir.setFile(runner.getFilename(), false);
          } catch (final CommandAbstractException e) {
            // file is not under normal base directory, so is external
            // File must already exist but can be using special code ('*?')
            file = dir.setFileNoCheck(runner.getFilename());
          }
        }
        if (runner.isSendThrough()) {
          // no test on file since it does not really exist
          logger.debug(FILE_IS_IN_THROUGH_MODE, file);
        } else if (!file.canRead()) {
          // file is not under normal base directory, so is external
          // File must already exist but cannot used special code ('*?')
          final R66File newFile = new R66File(this, dir, runner.getFilename());
          if (!newFile.canRead()) {
            runner.setErrorExecutionStatus(ErrorCode.FileNotFound);
            throw new OpenR66RunnerErrorException(
                "File cannot be read: " + file.getTrueFile().getAbsolutePath() +
                " to " + newFile.getTrueFile().getAbsolutePath());
          }
        }
      } catch (final CommandAbstractException e) {
        throw new OpenR66RunnerErrorException(e);
      }
    } else {
      // File should not exist except if restart
      if (runner.getRank() > 0) {
        // Filename should be get back from runner load from database
        try {
          file = (R66File) dir.setFile(runner.getFilename(), true);
          if (runner.isRecvThrough()) {
            // no test on file since it does not really exist
            logger.debug(FILE_IS_IN_THROUGH_MODE, file);
          } else if (!file.canWrite()) {
            throw new OpenR66RunnerErrorException(FILE_CANNOT_BE_WRITE);
          }
        } catch (final CommandAbstractException e) {
          throw new OpenR66RunnerErrorException(e);
        }
      } else {
        // New FILENAME if necessary and store it
        if (createFile) {
          file = null;
          String newfilename = runner.getOriginalFilename();
          if (newfilename.charAt(1) == ':') {
            // Windows path
            newfilename = newfilename.substring(2);
          }
          newfilename = R66File.getBasename(newfilename);
          try {
            file = dir.setUniqueFile(runner.getSpecialId(), newfilename);
            runner.setFilename(file.getBasename());
          } catch (final CommandAbstractException e) {
            runner.deleteTempFile();
            throw e;
          }
          try {
            if (runner.isRecvThrough()) {
              // no test on file since it does not really exist
              logger.debug(FILE_IS_IN_THROUGH_MODE, file);
              runner.deleteTempFile();
            } else if (!file.canWrite()) {
              runner.deleteTempFile();
              throw new OpenR66RunnerErrorException(FILE_CANNOT_BE_WRITE);
            }
          } catch (final CommandAbstractException e) {
            runner.deleteTempFile();
            throw new OpenR66RunnerErrorException(e);
          }
        } else {
          throw new OpenR66RunnerErrorException("No file created");
        }
      }
    }
    // Store TRUEFILENAME
    try {
      if (runner.isFileMoved()) {
        runner.setFileMoved(file.getFile(), true);
      } else {
        runner.setFilename(file.getFile());
      }
    } catch (final CommandAbstractException e) {
      runner.deleteTempFile();
      throw new OpenR66RunnerErrorException(e);
    }
    // check fileSize
    if (isSender && file != null) {
      logger.debug("could change size: {} => {}", runner.getOriginalSize(),
                   file.length());
      if (runner.getOriginalSize() < 0) {
        final long originalSize = file.length();
        if (originalSize > 0) {
          runner.setOriginalSize(originalSize);
        }
      }
    }
  }

  /**
   * To be used when a request comes with a bad code so it cannot be set
   * normally
   *
   * @param runner
   * @param code
   */
  public void setBadRunner(final DbTaskRunner runner, final ErrorCode code) {
    this.runner = runner;
    if (code == ErrorCode.QueryAlreadyFinished) {
      if (this.runner.isSender()) {
        // Change dir
        try {
          dir.changeDirectory(this.runner.getRule().getSendPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
      } else {
        // Change dir
        try {
          dir.changeDirectory(this.runner.getRule().getWorkPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
      }
      if (businessObject != null) {
        businessObject.checkAtError(this);
      }
      this.runner.setPostTask();
      try {
        setFileAfterPreRunner(false);
      } catch (final OpenR66RunnerErrorException ignored) {
        // nothing
      } catch (final CommandAbstractException ignored) {
        // nothing
      }
    }
  }

  /**
   * Set the runner, and setup the directory first.
   * <p>
   * This call should be followed by a startup() call.
   *
   * @param runner the runner to set
   *
   * @throws OpenR66RunnerErrorException
   */
  public void setRunner(final DbTaskRunner runner)
      throws OpenR66RunnerErrorException {
    this.runner = runner;
    if (localChannelReference != null) {
      this.runner.setLocalChannelReference(localChannelReference);
    }
    this.isSender = runner.isSender();
    logger.debug("Runner to set: {} {}", runner.shallIgnoreSave(), runner);
    this.runner.checkThroughMode();
    if (businessObject != null) {
      businessObject.checkAtStartup(this);
    }
    if (this.runner.isSender()) {
      if (runner.isSendThrough()) {
        // May not change dir as needed
        // Change dir
        try {
          dir.changeDirectory(this.runner.getRule().getSendPath());
        } catch (final CommandAbstractException e) {
          // ignore
        }
      } else {
        // Change dir
        try {
          dir.changeDirectory(this.runner.getRule().getSendPath());
        } catch (final CommandAbstractException e) {
          throw new OpenR66RunnerErrorException(e);
        }
      }
    } else {
      if (runner.isRecvThrough()) {
        // May not change dir as needed
        // Change dir
        try {
          dir.changeDirectory(this.runner.getRule().getWorkPath());
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
      } else {
        // Change dir
        try {
          dir.changeDirectory(this.runner.getRule().getWorkPath());
        } catch (final CommandAbstractException e) {
          throw new OpenR66RunnerErrorException(e);
        }
      }
    }
    logger.debug("Dir is: {}", dir.getFullPath());
  }

  /**
   * START from the PreTask if necessary, and prepare the file
   *
   * @param checkNotExternal if True, the file as Sender should not be
   *     external to current directory
   *
   * @throws OpenR66RunnerErrorException
   */
  public void startup(final boolean checkNotExternal)
      throws OpenR66RunnerErrorException {
    setRestartMarker();
    logger.debug("GlobalLastStep: {} vs {}:{}", runner.getGloballaststep(),
                 TASKSTEP.NOTASK.ordinal(), TASKSTEP.PRETASK.ordinal());
    initializeTransfer(checkNotExternal);
    // Now create the associated file
    try {
      setFileAfterPreRunner(true);
    } catch (final CommandAbstractException e2) {
      // generated due to a possible wildcard not ready
      file = null;
    }
    logger.debug("GlobalLastStep: {}", runner.getGloballaststep());
    if (runner.getGloballaststep() == TASKSTEP.TRANSFERTASK.ordinal()) {
      if (businessObject != null) {
        businessObject.checkAfterPreCommand(this);
      }
      if (!runner.isSender()) {
        if (initializeReceiver()) {
          return;
        }
      } else {
        initializeSender();
      }
    }
    runner.saveStatus();
    logger.debug("Final init: {} {}", runner, file != null);
  }

  private void initializeSender() throws OpenR66RunnerErrorException {
    if (file != null) {
      try {
        localChannelReference.getFutureRequest().setFilesize(file.length());
      } catch (final CommandAbstractException ignored) {
        // nothing
      }
      try {
        file.restartMarker(restart);
      } catch (final CommandAbstractException e) {
        runner.deleteTempFile();
        throw new OpenR66RunnerErrorException(e);
      }
    }
  }

  private boolean initializeReceiver() throws OpenR66RunnerErrorException {
    // Check file length according to rank
    if (runner.isRecvThrough()) {
      // no size can be checked
    } else {
      long length = 0;
      if (file != null) {
        try {
          length = file.length();
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
      }
      final long needed = runner.getOriginalSize() - length;
      long available = 0;
      String targetDir = null;
      try {
        available = dir.getFreeSpace();
        targetDir = dir.getPwd();
      } catch (final CommandAbstractException ignored) {
        // nothing
      }
      if (file != null) {
        final File truefile = file.getTrueFile().getParentFile();
        available = truefile.getFreeSpace();
        targetDir = truefile.getPath();
      }
      logger.debug("Check available space: {} >? {}(+{})", available, needed,
                   length);
      // Available > 0 since some system returns 0 (wrong size)
      if (available > 0 && needed > available) {
        // not enough space
        runner.setErrorExecutionStatus(ErrorCode.Internal);
        throw new OpenR66RunnerErrorException(
            "File cannot be written due to unsufficient space available: " +
            targetDir + " need " + needed + " more while available is " +
            available);
      }
      if (file == null) {
        runner.saveStatus();
        logger.info("Final PARTIAL init: {}", runner);
        return true;
      }
      checkPosition(length);
    }
    return false;
  }

  private void checkPosition(final long length)
      throws OpenR66RunnerErrorException {
    // First check available space
    try {
      final long oldPosition = restart.getPosition();
      restart.setSet(true);
      if (oldPosition > length) {
        int newRank = (int) (length / runner.getBlocksize()) -
                      Configuration.getRankRestart();
        if (newRank <= 0) {
          newRank = 1;
        }
        logger.info("OldPos: {}:{} curLength: {}:{}", oldPosition,
                    runner.getRank(), length, newRank);
        logger.warn("Decreased Rank Restart for {} at " + newRank, runner);
        runner.setTransferTask(newRank);
        restart.restartMarker(runner.getBlocksize() * runner.getRank());
      }
      try {
        file.restartMarker(restart);
      } catch (final CommandAbstractException e) {
        runner.deleteTempFile();
        throw new OpenR66RunnerErrorException(e);
      }
    } catch (final NoRestartException e) {
      // length is not to be changed
    }
  }

  private void initializeTransfer(final boolean checkNotExternal)
      throws OpenR66RunnerErrorException {
    if (runner.isSelfRequest() && runner.getRule().isSendMode()) {
      // It might be the sender and initiator side, already changed by
      // receiver, reset globalLastStep and globalStep
      runner.setInitialTask();
    }
    if (runner.getGloballaststep() == TASKSTEP.NOTASK.ordinal() ||
        runner.getGloballaststep() == TASKSTEP.PRETASK.ordinal()) {
      setFileBeforePreRunner();
      if (runner.isSender() && !runner.isSendThrough() && file != null &&
          checkNotExternal) {
        String path = null;
        try {
          path = file.getFile();
        } catch (final CommandAbstractException ignored) {
          // nothing
        }
        if (file.isExternal() ||
            path != null && !dir.isPathInCurrentDir(path)) {
          // should not be
          logger.error(
              "File cannot be found in the current output directory: {} not in {}",
              file, dir);
          runner.setErrorExecutionStatus(ErrorCode.FileNotAllowed);
          throw new OpenR66RunnerErrorException(
              "File cannot be found in the current output directory");
        }
      }
      runner.setPreTask();
      runner.saveStatus();
      runner.run();
      if (runner.isSender() && !runner.isSendThrough()) {
        if (file != null) {
          try {
            final long originalSize = file.length();
            if (originalSize > 0) {
              runner.setOriginalSize(originalSize);
            }
          } catch (final CommandAbstractException e) {
            // ignore
          }
        }
      }
      runner.saveStatus();
      runner.setTransferTask(runner.getRank());
    } else {
      runner.reset();
      runner.changeUpdatedInfo(UpdatedInfo.RUNNING);
      runner.saveStatus();
    }
  }

  private void setRestartMarker() {
    if (runner.getRank() > 0) {
      logger.debug("restart at {} {}", runner.getRank(), runner);
      logger.debug("restart at {} {}", runner.getRank(), dir);
      runner.setTransferTask(runner.getRank());
      restart.restartMarker(runner.getBlocksize() * runner.getRank());
    } else {
      restart.restartMarker(0);
    }
  }

  /**
   * Rename the current receive file from the very beginning since the sender
   * has a post action that changes its
   * name
   *
   * @param newFilename
   *
   * @throws OpenR66RunnerErrorException
   */
  public void renameReceiverFile(final String newFilename)
      throws OpenR66RunnerErrorException {
    if (runner == null) {
      return;
    }
    // First delete the temporary file if needed
    if (runner.getRank() > 0) {
      logger.error(
          "Renaming file is not correct since transfer does not start from first block");
      // Not correct
      throw new OpenR66RunnerErrorException(
          "Renaming file not correct since transfer already started");
    }
    if (!runner.isRecvThrough()) {
      runner.deleteTempFile();
    }
    // Now rename it
    runner.setOriginalFilename(newFilename);
    try {
      setFileAfterPreRunnerReceiver(true);
    } catch (final CommandAbstractException e) {
      throw new OpenR66RunnerErrorException(e);
    }
    runner.saveStatus();
  }

  /**
   * Finalize the transfer step by running the error or post operation
   * according
   * to the status.
   *
   * @param status
   * @param finalValue
   *
   * @throws OpenR66RunnerErrorException
   * @throws OpenR66ProtocolSystemException
   */
  public void setFinalizeTransfer(final boolean status,
                                  final R66Result finalValue)
      throws OpenR66RunnerErrorException, OpenR66ProtocolSystemException {
    logger.debug("{}:{}:{}", status, finalValue, runner);
    if (runner == null) {
      if (localChannelReference != null) {
        if (status) {
          localChannelReference.validateRequest(finalValue);
        } else {
          localChannelReference.invalidateRequest(finalValue);
        }
      }
      if (businessObject != null) {
        if (status) {
          businessObject.checkAfterTransfer(this);
        } else {
          businessObject.checkAtError(this);
        }
      }
      return;
    }
    if (businessObject != null) {
      if (status) {
        businessObject.checkAfterTransfer(this);
      } else {
        businessObject.checkAtError(this);
      }
    }
    if (runner.isAllDone()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Transfer already done but {} on {} {}", status, file,
                     runner.toShortString(),
                     new OpenR66RunnerErrorException(finalValue.toString()));
      }
      // FIXME ??
      /*
       * if (! status) runner.finalizeTransfer(localChannelReference, file, finalValue, status)
       */
      return;
    }
    if (runner.isInError()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Transfer already done in error but {} on {} {}", status,
                     file, runner.toShortString(),
                     new OpenR66RunnerErrorException(finalValue.toString()));
      }
      // FIXME ??
      /*
       * if (! status) runner.finalizeTransfer(localChannelReference, file, finalValue, status)
       */
      return;
    }
    if (localChannelReference.getFutureRequest().isDone()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Request already done but {} on {} {}" + status, file,
                     runner.toShortString(),
                     new OpenR66RunnerErrorException(finalValue.toString()));
      }
      // Already finished once so do nothing more
      return;
    }
    if (!status) {
      runner.deleteTempFile();
      runner.setErrorExecutionStatus(finalValue.getCode());
    }
    if (status) {
      runner.finishTransferTask(ErrorCode.TransferOk);
    } else {
      runner.finishTransferTask(finalValue.getCode());
    }
    runner.saveStatus();
    logger.debug("Transfer {} on {} and {}", status, file, runner);
    if (!runner.ready()) {
      // Pre task in error (or even before)
      final OpenR66RunnerErrorException runnerErrorException;
      if (!status && finalValue.getException() != null) {
        runnerErrorException = new OpenR66RunnerErrorException(
            "Pre task in error (or even before)", finalValue.getException());
      } else {
        runnerErrorException = new OpenR66RunnerErrorException(
            "Pre task in error (or even before)");
      }
      finalValue.setException(runnerErrorException);
      logger.info("Pre task in error (or even before) : {}",
                  runnerErrorException.getMessage());
      if (Configuration.configuration.isExecuteErrorBeforeTransferAllowed()) {
        runner
            .finalizeTransfer(localChannelReference, file, finalValue, status);
      }
      localChannelReference.invalidateRequest(finalValue);
      throw runnerErrorException;
    }
    try {
      if (file != null) {
        file.closeFile();
      }
    } catch (final CommandAbstractException e1) {
      R66Result result = finalValue;
      if (status) {
        result = new R66Result(new OpenR66RunnerErrorException(e1), this, false,
                               ErrorCode.Internal, runner);
      }
      localChannelReference.invalidateRequest(result);
      throw (OpenR66RunnerErrorException) result.getException();
    }
    runner.finalizeTransfer(localChannelReference, file, finalValue, status);
    if (businessObject != null) {
      businessObject.checkAfterPost(this);
    }
  }

  /**
   * Try to finalize the request if possible
   *
   * @param errorValue
   *
   * @throws OpenR66RunnerErrorException
   * @throws OpenR66ProtocolSystemException
   */
  public void tryFinalizeRequest(final R66Result errorValue)
      throws OpenR66RunnerErrorException, OpenR66ProtocolSystemException {
    if (getLocalChannelReference() == null) {
      return;
    }
    if (getLocalChannelReference().getFutureRequest().isDone()) {
      return;
    }
    if (runner == null) {
      localChannelReference.invalidateRequest(errorValue);
      return;
    }
    // do the real end
    if (runner.getStatus() == ErrorCode.CompleteOk) {
      runner.setAllDone();
      runner.forceSaveStatus();
      localChannelReference.validateRequest(
          new R66Result(this, true, ErrorCode.CompleteOk, runner));
    } else if (runner.getStatus() == ErrorCode.TransferOk &&
               (!runner.isSender() ||
                errorValue.getCode() == ErrorCode.QueryAlreadyFinished)) {
      // Try to finalize it
      try {
        setFinalizeTransfer(true,
                            new R66Result(this, true, ErrorCode.CompleteOk,
                                          runner));
        localChannelReference.validateRequest(
            localChannelReference.getFutureEndTransfer().getResult());
      } catch (final OpenR66ProtocolSystemException e) {
        logger.error("Cannot validate runner:     {}", runner.toShortString());
        runner.changeUpdatedInfo(UpdatedInfo.INERROR);
        runner.setErrorExecutionStatus(errorValue.getCode());
        runner.forceSaveStatus();
        setFinalizeTransfer(false, errorValue);
      } catch (final OpenR66RunnerErrorException e) {
        logger.error("Cannot validate runner:     {}", runner.toShortString());
        runner.changeUpdatedInfo(UpdatedInfo.INERROR);
        runner.setErrorExecutionStatus(errorValue.getCode());
        runner.forceSaveStatus();
        setFinalizeTransfer(false, errorValue);
      }
    } else {
      // invalidate Request
      logger.error(
          "Runner {} will be shutdown while in status {} and future status will be {}",
          runner.getSpecialId(), runner.getStatus().getMesg(),
          errorValue.getCode().getMesg());
      setFinalizeTransfer(false, errorValue);
    }
  }

  /**
   * @return the file
   */
  public R66File getFile() {
    return file;
  }

  /**
   * @return True if the number of Error is still acceptable
   */
  public boolean addError() {
    final int value = numOfError.incrementAndGet();
    return value < Configuration.RETRYNB;
  }

  @Override
  public String toString() {
    return "Session: FS[" + state.getCurrent() + "] " + status + "  " +
           (auth != null? auth.toString() : "no Auth") + "     " +
           (dir != null? dir.toString() : "no Dir") + "     " +
           (file != null? file.toString() : "no File") + "     " +
           (runner != null? runner.toShortString() : "no Runner");
  }

  @Override
  public String getUniqueExtension() {
    return Configuration.EXT_R66;
  }

  /**
   * @return the dirsFromSession
   */
  public HashMap<String, R66Dir> getDirsFromSession() {
    return dirsFromSession;
  }

  /**
   * @return True if according to session, it is the sender side (to byPass
   *     send to itself issue)
   */
  public boolean isSender() {
    return isSender;
  }
}
