/**
 * This file is part of Waarp Project.
 *
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 *
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.context;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.exception.IllegalFiniteStateException;
import org.waarp.common.exception.NoRestartException;
import org.waarp.common.file.SessionInterface;
import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.state.MachineState;
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
import org.waarp.openr66.protocol.utils.FileUtils;

/**
 * The global object session in OpenR66, a session by local channel
 *
 * @author frederic bregier
 *
 */
public class R66Session implements SessionInterface {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(R66Session.class);

    /**
     * Block size used during file transfer
     */
    private int blockSize = Configuration.configuration.getBLOCKSIZE();
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
    private volatile boolean isReady = false;
    /**
     * Used to prevent deny of service
     */
    private volatile int numOfError = 0;

    /**
     * Current Restart information
     */
    private final R66Restart restart;

    /**
     * DbTaskRunner
     */
    private DbTaskRunner runner = null;

    private String status = "NoStatus";

    /**
     * The Finite Machine State
     */
    private final MachineState<R66FiniteDualStates> state;
    /**
     * Business Object if used
     */
    private R66BusinessInterface businessObject = null;
    /**
     * Extended protocol or not
     */
    private boolean extendedProtocol = Configuration.configuration.isExtendedProtocol();

    private final HashMap<String, R66Dir> dirsFromSession = new HashMap<String, R66Dir>();

    /**
     * Create the session
     */
    public R66Session() {
        isReady = false;
        auth = new R66Auth(this);
        dir = new R66Dir(this);
        restart = new R66Restart(this);
        state = R66FiniteDualStates.newSessionMachineState();
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
     * @param businessObject
     *            the businessObject to set
     */
    public void setBusinessObject(R66BusinessInterface businessObject) {
        this.businessObject = businessObject;
    }

    /**
     * Propose a new State
     *
     * @param desiredstate
     * @throws IllegalFiniteStateException
     *             if the new status if not ok
     */
    public void newState(R66FiniteDualStates desiredstate) {
        try {
            state.setCurrent(desiredstate);
        } catch (IllegalFiniteStateException e) {
            logger.warn("Should not changed of State: {} {}", this, e.getMessage());
            state.setDryCurrent(desiredstate);
        }
    }

    public void setErrorState() {
        try {
            state.setCurrent(R66FiniteDualStates.ERROR);
        } catch (IllegalFiniteStateException e) {
            logger.error("Couldn't pass to error state. This should not happen");
	}
    }

    /**
     *
     * @return the current state in the finite state machine
     */
    public R66FiniteDualStates getState() {
        return state.getCurrent();
    }

    /**
     * Debugging purpose
     *
     * @param stat
     */
    @Deprecated
    public void setStatus(int stat) {
        StackTraceElement elt = Thread.currentThread().getStackTrace()[2];
        this.status = "(" + elt.getFileName() + ":" + elt.getLineNumber() + "):" + stat;
    }

    public void clear() {
        // First check if a transfer was on going
        if (runner != null && (!runner.isFinished()) && (!runner.continueTransfer())) {
            if (localChannelReference != null) {
                if (!localChannelReference.getFutureRequest().isDone()) {
                    R66Result result = new R66Result(new OpenR66RunnerErrorException(
                            "Close before ending"), this, true,
                            ErrorCode.Disconnection, runner);// True since called from closed
                    result.setRunner(runner);
                    try {
                        setFinalizeTransfer(false, result);
                    } catch (OpenR66RunnerErrorException e) {
                    } catch (OpenR66ProtocolSystemException e) {
                    }
                }
            }
        }
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
            } catch (IllegalFiniteStateException e) {
            }
            // R66FiniteDualStates.endSessionMachineSate(state);
        }
        // No clean of file since it can be used after channel is closed
        isReady = false;
        if (businessObject != null) {
            businessObject.releaseResources(this);
            businessObject = null;
        }
    }

    public void partialClear() {
        // First check if a transfer was on going
        if (runner != null && (!runner.isFinished()) && (!runner.continueTransfer())) {
            if (localChannelReference != null) {
                if (!localChannelReference.getFutureRequest().isDone()) {
                    R66Result result = new R66Result(new OpenR66RunnerErrorException(
                            "Close before ending"), this, true,
                            ErrorCode.Disconnection, runner);// True since called from closed
                    result.setRunner(runner);
                    try {
                        setFinalizeTransfer(false, result);
                    } catch (OpenR66RunnerErrorException e) {
                    } catch (OpenR66ProtocolSystemException e) {
                    }
                }
            }
        }
        /*
        if (dir != null) {
        	dir.clear();
        }
        if (auth != null) {
        	auth.clear();
        }
        if (runner != null) {
        	runner.clear();
        }
        */
        // No clean of file since it can be used after channel is closed
        isReady = false;
        if (businessObject != null) {
            businessObject.releaseResources(this);
            businessObject = null;
        }
    }

    public R66Auth getAuth() {
        return auth;
    }

    public int getBlockSize() {
        return blockSize;
    }

    /**
     * @param blocksize
     *            the blocksize to set
     */
    public void setBlockSize(int blocksize) {
        blockSize = blocksize;
    }

    public R66Dir getDir() {
        return dir;
    }

    public FilesystemBasedFileParameterImpl getFileParameter() {
        return Configuration.getFileParameter();
    }

    public R66Restart getRestart() {
        return restart;
    }

    /**
     *
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
     * @param isReady
     *            the isReady for transfer to set
     */
    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    /**
     * @return the runner
     */
    public DbTaskRunner getRunner() {
        return runner;
    }

    /**
     * @param localChannelReference
     *            the localChannelReference to set
     */
    public void setLocalChannelReference(
            LocalChannelReference localChannelReference) {
        this.localChannelReference = localChannelReference;
        this.localChannelReference.setSession(this);
        if (this.localChannelReference.getNetworkChannel() != null) {
            this.raddress = this.localChannelReference.getNetworkChannel().remoteAddress();
            this.laddress = this.localChannelReference.getNetworkChannel().localAddress();
        } else {
            this.raddress = this.laddress = new InetSocketAddress(0);
        }
    }

    /**
     *
     * @return the remote SocketAddress
     */
    public SocketAddress getRemoteAddress() {
        return this.raddress;
    }

    /**
     *
     * @return the local SocketAddress
     */
    public SocketAddress getLocalAddress() {
        return this.laddress;
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
    public void setNoSessionRunner(DbTaskRunner runner, LocalChannelReference localChannelReference) {
        this.runner = runner;
        // Warning: the file is not correctly setup
        try {
            file = (R66File) dir.setFile(this.runner.getFilename(),
                    false);
        } catch (CommandAbstractException e1) {
        }
        this.auth.specialNoSessionAuth(false, Configuration.configuration.getHOST_ID());
        this.localChannelReference = localChannelReference;
        if (this.localChannelReference == null) {
            if (this.runner.getLocalChannelReference() != null) {
                this.localChannelReference = this.runner.getLocalChannelReference();
            } else {
                this.localChannelReference = new LocalChannelReference();
            }
            this.localChannelReference.setErrorMessage(this.runner.getErrorInfo().getMesg(),
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
            file = FileUtils.getFile(logger, this, this.runner.getOriginalFilename(),
                    this.runner.isPreTaskStarting(), this.runner.isSender(),
                    this.runner.isSendThrough(), file);
        } catch (OpenR66RunnerErrorException e) {
            this.runner.setErrorExecutionStatus(ErrorCode.FileNotFound);
            throw e;
        }
        if (this.runner.isSender() && !runner.isSendThrough()) {
            // possibly resolved filename
            try {
                runner.setOriginalFilename(file.getFile());
                runner.setFilename(file.getFile());
                logger.debug("Old size: " + runner.getOriginalSize() + " => " + file.length());
                if (runner.getOriginalSize() <= 0) {
                    long originalSize = file.length();
                    if (originalSize > 0) {
                        this.runner.setOriginalSize(originalSize);
                    }
                }
            } catch (CommandAbstractException e) {
                throw new OpenR66RunnerErrorException(e);
            }
        }
    }

    /**
     * Set the File from the runner once PRE operation are done
     *
     * @param createFile
     *            When True, the file can be newly created if needed. If False, no new file will be
     *            created, thus having an Exception.
     * @throws OpenR66RunnerErrorException
     * @throws CommandAbstractException
     *             only when new received created file cannot be created
     */
    public void setFileAfterPreRunner(boolean createFile) throws OpenR66RunnerErrorException, CommandAbstractException {
        if (this.businessObject != null) {
            this.businessObject.checkAtChangeFilename(this);
        }
        // Now create the associated file
        if (this.runner.isSender()) {
            try {
                if (file == null) {
                    try {
                        file = (R66File) dir.setFile(this.runner.getFilename(),
                                false);
                    } catch (CommandAbstractException e) {
                        // file is not under normal base directory, so is external
                        // File must already exist but can be using special code ('*?')
                        file = dir.setFileNoCheck(this.runner.getFilename());
                        // file = new R66File(this, dir, this.runner.getFilename());
                    }
                }
                if (runner.isSendThrough()) {
                    // no test on file since it does not really exist
                    logger.debug("File is in through mode: {}", file);
                } else if (!file.canRead()) {
                    // file is not under normal base directory, so is external
                    // File must already exist but cannot used special code ('*?')
                    file = new R66File(this, dir, this.runner.getFilename());
                    if (!file.canRead()) {
                        this.runner.setErrorExecutionStatus(ErrorCode.FileNotFound);
                        throw new OpenR66RunnerErrorException("File cannot be read: " +
                                file.getTrueFile().getAbsolutePath());
                    }
                }
            } catch (CommandAbstractException e) {
                throw new OpenR66RunnerErrorException(e);
            }
        } else {
            // File should not exist except if restart
            if (runner.getRank() > 0) {
                // Filename should be get back from runner load from database
                try {
                    file = (R66File) dir.setFile(this.runner
                            .getFilename(), true);
                    if (runner.isRecvThrough()) {
                        // no test on file since it does not really exist
                        logger.debug("File is in through mode: {}", file);
                    } else if (!file.canWrite()) {
                        throw new OpenR66RunnerErrorException(
                                "File cannot be write");
                    }
                } catch (CommandAbstractException e) {
                    throw new OpenR66RunnerErrorException(e);
                }
            } else {
                // New FILENAME if necessary and store it
                if (createFile) {
                    file = null;
                    String newfilename = this.runner.getOriginalFilename();
                    if (newfilename.charAt(1) == ':') {
                        // Windows path
                        newfilename = newfilename.substring(2);
                    }
                    newfilename = R66File.getBasename(newfilename);
                    try {
                        file = dir.setUniqueFile(this.runner.getSpecialId(),
                                newfilename);
                        this.runner.setFilename(file.getBasename());
                    } catch (CommandAbstractException e) {
                        this.runner.deleteTempFile();
                        throw e;
                    }
                    try {
                        if (runner.isRecvThrough()) {
                            // no test on file since it does not really exist
                            logger.debug("File is in through mode: {}", file);
                            this.runner.deleteTempFile();
                        } else if (!file.canWrite()) {
                            this.runner.deleteTempFile();
                            throw new OpenR66RunnerErrorException(
                                    "File cannot be write");
                        }
                    } catch (CommandAbstractException e) {
                        this.runner.deleteTempFile();
                        throw new OpenR66RunnerErrorException(e);
                    }
                } else {
                    throw new OpenR66RunnerErrorException("No file created");
                }
            }
        }
        // Store TRUEFILENAME
        try {
            if (this.runner.isFileMoved()) {
                this.runner.setFileMoved(file.getFile(), true);
            } else {
                this.runner.setFilename(file.getFile());
            }
        } catch (CommandAbstractException e) {
            this.runner.deleteTempFile();
            throw new OpenR66RunnerErrorException(e);
        }
        // check fileSize
        if (runner.isSender() && file != null) {
            logger.debug("could change size: " + this.runner.getOriginalSize() + " => " + file.length());
            if (this.runner.getOriginalSize() < 0) {
                long originalSize = file.length();
                if (originalSize > 0) {
                    this.runner.setOriginalSize(originalSize);
                }
            }
        }
    }

    /**
     * To be used when a request comes with a bad code so it cannot be set normally
     *
     * @param runner
     * @param code
     */
    public void setBadRunner(DbTaskRunner runner, ErrorCode code) {
        this.runner = runner;
        if (code == ErrorCode.QueryAlreadyFinished) {
            if (this.runner.isSender()) {
                // Change dir
                try {
                    dir.changeDirectory(this.runner.getRule().getSendPath());
                } catch (CommandAbstractException e) {
                }
            } else {
                // Change dir
                try {
                    dir.changeDirectory(this.runner.getRule().getWorkPath());
                } catch (CommandAbstractException e) {
                }
            }
            if (this.businessObject != null) {
                this.businessObject.checkAtError(this);
            }
            this.runner.setPostTask();
            try {
                setFileAfterPreRunner(false);
            } catch (OpenR66RunnerErrorException e) {
            } catch (CommandAbstractException e) {
            }
        }
    }

    /**
     * Set the runner, and setup the directory first.
     *
     * This call should be followed by a startup() call.
     *
     * @param runner
     *            the runner to set
     * @throws OpenR66RunnerErrorException
     */
    public void setRunner(DbTaskRunner runner)
            throws OpenR66RunnerErrorException {
        this.runner = runner;
        logger.debug("Runner to set: {} {}", runner.shallIgnoreSave(), runner);
        this.runner.checkThroughMode();
        if (this.businessObject != null) {
            this.businessObject.checkAtStartup(this);
        }
        if (this.runner.isSender()) {
            if (runner.isSendThrough()) {
                // May not change dir as needed
                // Change dir
                try {
                    dir.changeDirectory(this.runner.getRule().getSendPath());
                } catch (CommandAbstractException e) {
                    // ignore
                }
            } else {
                // Change dir
                try {
                    dir.changeDirectory(this.runner.getRule().getSendPath());
                } catch (CommandAbstractException e) {
                    throw new OpenR66RunnerErrorException(e);
                }
            }
        } else {
            if (runner.isRecvThrough()) {
                // May not change dir as needed
                // Change dir
                try {
                    dir.changeDirectory(this.runner.getRule().getWorkPath());
                } catch (CommandAbstractException e) {
                }
            } else {
                // Change dir
                try {
                    dir.changeDirectory(this.runner.getRule().getWorkPath());
                } catch (CommandAbstractException e) {
                    throw new OpenR66RunnerErrorException(e);
                }
            }
        }
        logger.debug("Dir is: " + dir.getFullPath());
    }

    /**
     * START from the PreTask if necessary, and prepare the file
     *
     * @param checkNotExternal
     *            if True, the file as Sender should not be external to current directory
     * @throws OpenR66RunnerErrorException
     */
    public void startup(boolean checkNotExternal) throws OpenR66RunnerErrorException {
        if (runner.getRank() > 0) {
            logger.debug("restart at " + runner.getRank() + " {}", runner);
            logger.debug("restart at " + runner.getRank() + " {}", dir);
            runner.setTransferTask(runner.getRank());
            restart.restartMarker(runner.getBlocksize() * runner.getRank());
        } else {
            restart.restartMarker(0);
        }
        logger.debug("GlobalLastStep: " + runner.getGloballaststep() + " vs " + TASKSTEP.NOTASK.ordinal() + ":"
                + TASKSTEP.PRETASK.ordinal());
        if (runner.getGloballaststep() == TASKSTEP.NOTASK.ordinal() ||
                runner.getGloballaststep() == TASKSTEP.PRETASK.ordinal()) {
            setFileBeforePreRunner();
            if (runner.isSender() && !runner.isSendThrough() && file != null && checkNotExternal) {
                String path = null;
                try {
                    path = file.getFile();
                } catch (CommandAbstractException e1) {
                }
                if (file.isExternal() || (path != null && !dir.isPathInCurrentDir(path))) {
                    // should not be
                    logger.error("File cannot be found in the current output directory: {} not in {}", file, dir);
                    this.runner.setErrorExecutionStatus(ErrorCode.FileNotAllowed);
                    throw new OpenR66RunnerErrorException("File cannot be found in the current output directory");
                }
            }
            this.runner.setPreTask();
            runner.saveStatus();
            this.runner.run();
            if (runner.isSender() && !runner.isSendThrough()) {
                if (file != null) {
                    try {
                        long originalSize = file.length();
                        if (originalSize > 0) {
                            this.runner.setOriginalSize(originalSize);
                        }
                    } catch (CommandAbstractException e) {
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
        // Now create the associated file
        try {
            setFileAfterPreRunner(true);
        } catch (CommandAbstractException e2) {
            // generated due to a possible wildcard not ready
            file = null;
        }
        logger.debug("GlobalLastStep: " + runner.getGloballaststep());
        if (runner.getGloballaststep() == TASKSTEP.TRANSFERTASK.ordinal()) {
            if (this.businessObject != null) {
                this.businessObject.checkAfterPreCommand(this);
            }
            if (!this.runner.isSender()) {
                // Check file length according to rank
                if (runner.isRecvThrough()) {
                    // no size can be checked
                } else {
                    long length = 0;
                    if (file != null) {
                        try {
                            length = file.length();
                        } catch (CommandAbstractException e) {
                        }
                    }
                    long needed = this.runner.getOriginalSize() - length;
                    long available = 0;
                    String targetDir = null;
                    try {
                        available = this.dir.getFreeSpace();
                        targetDir = this.dir.getPwd();
                    } catch (CommandAbstractException e1) {
                    }
                    if (file != null) {
                        File truefile = file.getTrueFile().getParentFile();
                        available = truefile.getFreeSpace();
                        targetDir = truefile.getPath();
                    }
                    logger.debug("Check available space: " + available + " >? " + needed + "(+" + length + ")");
                    // Available > 0 since some system returns 0 (wrong size)
                    if (available > 0 && needed > available) {
                        // not enough space
                        this.runner.setErrorExecutionStatus(ErrorCode.Internal);
                        throw new OpenR66RunnerErrorException(
                                "File cannot be written due to unsufficient space available: " +
                                        targetDir + " need " + needed + " more while available is " + available);
                    }
                    if (file == null) {
                        this.runner.saveStatus();
                        logger.info("Final PARTIAL init: {}", this.runner);
                        return;
                    }
                    // First check available space
                    try {
                        long oldPosition = restart.getPosition();
                        restart.setSet(true);
                        if (oldPosition > length) {
                            int newRank = ((int) (length / this.runner.getBlocksize()))
                                    - Configuration.getRANKRESTART();
                            if (newRank <= 0) {
                                newRank = 1;
                            }
                            logger.info("OldPos: " + oldPosition + ":" + runner.getRank() +
                                    " curLength: " + length + ":" + newRank);
                            logger.warn("Decreased Rank Restart for {} at " + newRank, runner);
                            runner.setTransferTask(newRank);
                            restart.restartMarker(this.runner.getBlocksize()
                                    * this.runner.getRank());
                        }
                        try {
                            file.restartMarker(restart);
                        } catch (CommandAbstractException e) {
                            this.runner.deleteTempFile();
                            throw new OpenR66RunnerErrorException(e);
                        }
                    } catch (NoRestartException e) {
                        // length is not to be changed
                    }
                }
            } else {
                try {
                    this.localChannelReference.getFutureRequest().setFilesize(file.length());
                } catch (CommandAbstractException e1) {
                }
                try {
                    file.restartMarker(restart);
                } catch (CommandAbstractException e) {
                    this.runner.deleteTempFile();
                    throw new OpenR66RunnerErrorException(e);
                }
            }
        }
        this.runner.saveStatus();
        logger.debug("Final init: {} {}", this.runner, this.file != null);
    }

    /**
     * Rename the current receive file from the very beginning since the sender has a post action
     * that changes its name
     *
     * @param newFilename
     * @throws OpenR66RunnerErrorException
     */
    public void renameReceiverFile(String newFilename) throws OpenR66RunnerErrorException {
        if (runner == null) {
            return;
        }
        // First delete the temporary file if needed
        if (runner.getRank() > 0) {
            logger.error("Renaming file is not correct since transfer does not start from first block");
            // Not correct
            throw new OpenR66RunnerErrorException(
                    "Renaming file not correct since transfer already started");
        }
        if (!runner.isRecvThrough()) {
            this.runner.deleteTempFile();
        }
        // Now rename it
        this.runner.setOriginalFilename(newFilename);
        try {
            this.setFileAfterPreRunner(true);
        } catch (CommandAbstractException e) {
            throw new OpenR66RunnerErrorException(e);
        }
        this.runner.saveStatus();
    }

    /**
     * Finalize the transfer step by running the error or post operation according to the status.
     *
     * @param status
     * @param finalValue
     * @throws OpenR66RunnerErrorException
     * @throws OpenR66ProtocolSystemException
     */
    public void setFinalizeTransfer(boolean status, R66Result finalValue)
            throws OpenR66RunnerErrorException, OpenR66ProtocolSystemException {
        logger.debug(status + ":" + finalValue + ":" + runner);
        if (runner == null) {
            if (localChannelReference != null) {
                if (status) {
                    localChannelReference.validateRequest(finalValue);
                } else {
                    localChannelReference.invalidateRequest(finalValue);
                }
            }
            if (this.businessObject != null) {
                if (status) {
                    this.businessObject.checkAfterTransfer(this);
                } else {
                    this.businessObject.checkAtError(this);
                }
            }
            return;
        }
        if (this.businessObject != null) {
            if (status) {
                this.businessObject.checkAfterTransfer(this);
            } else {
                this.businessObject.checkAtError(this);
            }
        }
        if (runner.isAllDone()) {
            logger.debug(
                    "Transfer already done but " + status + " on " + file + runner.toShortString(),
                    new OpenR66RunnerErrorException(finalValue.toString()));
            // FIXME ??
            /*
             * if (! status) runner.finalizeTransfer(localChannelReference, file, finalValue,
             * status);
             */
            return;
        }
        if (localChannelReference.getFutureRequest().isDone()) {
            logger.debug(
                    "Request already done but " + status + " on " + file + runner.toShortString(),
                    new OpenR66RunnerErrorException(finalValue.toString()));
            // Already finished once so do nothing more
            return;
        }
        if (!status) {
            this.runner.deleteTempFile();
            runner.setErrorExecutionStatus(finalValue.getCode());
        }
        if (status) {
            runner.finishTransferTask(ErrorCode.TransferOk);
        } else {
            runner.finishTransferTask(finalValue.getCode());
        }
        runner.saveStatus();
        logger.debug("Transfer " + status + " on {} and {}", file, runner);
        if (!runner.ready()) {
            // Pre task in error (or even before)
            OpenR66RunnerErrorException runnerErrorException;
            if (!status && finalValue.getException() != null) {
                runnerErrorException = new OpenR66RunnerErrorException(
                        "Pre task in error (or even before)",
                        finalValue.getException());
            } else {
                runnerErrorException = new OpenR66RunnerErrorException(
                        "Pre task in error (or even before)");
            }
            finalValue.setException(runnerErrorException);
            logger.debug("Pre task in error (or even before) : " +
                    runnerErrorException.getMessage());
            if (Configuration.configuration.isExecuteErrorBeforeTransferAllowed()) {
                runner.finalizeTransfer(localChannelReference, file, finalValue, status);
            }
            localChannelReference.invalidateRequest(finalValue);
            throw runnerErrorException;
        }
        try {
            if (file != null) {
                file.closeFile();
            }
        } catch (CommandAbstractException e1) {
            R66Result result = finalValue;
            if (status) {
                result = new R66Result(new OpenR66RunnerErrorException(e1),
                        this, false, ErrorCode.Internal, runner);
            }
            localChannelReference.invalidateRequest(result);
            throw (OpenR66RunnerErrorException) result.getException();
        }
        runner.finalizeTransfer(localChannelReference, file, finalValue, status);
        if (this.businessObject != null) {
            this.businessObject.checkAfterPost(this);
        }
    }

    /**
     * Try to finalize the request if possible
     *
     * @param errorValue
     * @throws OpenR66RunnerErrorException
     * @throws OpenR66ProtocolSystemException
     */
    public void tryFinalizeRequest(R66Result errorValue)
            throws OpenR66RunnerErrorException, OpenR66ProtocolSystemException {
        if (this.getLocalChannelReference() == null) {
            return;
        }
        if (this.getLocalChannelReference().getFutureRequest().isDone()) {
            return;
        }
        // setRunnerFromLocalChannelReference(localChannelReference);
        if (runner == null) {
            localChannelReference.invalidateRequest(errorValue);
            return;
        }
        // do the real end
        if (runner.getStatus() == ErrorCode.CompleteOk) {
            // status = true;
            runner.setAllDone();
            runner.forceSaveStatus();
            localChannelReference.validateRequest(
                    new R66Result(this, true, ErrorCode.CompleteOk, runner));
        } else if (runner.getStatus() == ErrorCode.TransferOk &&
                ((!runner.isSender()) || errorValue.getCode() == ErrorCode.QueryAlreadyFinished)) {
            // Try to finalize it
            // status = true;
            try {
                this.setFinalizeTransfer(true,
                        new R66Result(this, true, ErrorCode.CompleteOk, runner));
                localChannelReference.validateRequest(
                        localChannelReference.getFutureEndTransfer().getResult());
            } catch (OpenR66ProtocolSystemException e) {
                logger.error("Cannot validate runner:     {}", runner.toShortString());
                runner.changeUpdatedInfo(UpdatedInfo.INERROR);
                runner.setErrorExecutionStatus(errorValue.getCode());
                runner.forceSaveStatus();
                this.setFinalizeTransfer(false, errorValue);
            } catch (OpenR66RunnerErrorException e) {
                logger.error("Cannot validate runner:     {}", runner.toShortString());
                runner.changeUpdatedInfo(UpdatedInfo.INERROR);
                runner.setErrorExecutionStatus(errorValue.getCode());
                runner.forceSaveStatus();
                this.setFinalizeTransfer(false, errorValue);
            }
        } else {
            // invalidate Request
            this.setFinalizeTransfer(false, errorValue);
        }
    }

    /**
     * @return the file
     */
    public R66File getFile() {
        return file;
    }

    /**
     *
     * @return True if the number of Error is still acceptable
     */
    public boolean addError() {
        numOfError++;
        return (numOfError < Configuration.RETRYNB);
    }

    @Override
    public String toString() {
        return "Session: FS[" + state.getCurrent() + "] " + status + "  " +
                (auth != null ? auth.toString() : "no Auth") + "     " +
                (dir != null ? dir.toString() : "no Dir") + "     " +
                (file != null ? file.toString() : "no File") + "     " +
                (runner != null ? runner.toShortString() : "no Runner");
    }

    public String getUniqueExtension() {
        return Configuration.EXT_R66;
    }

    /**
     * @return the dirsFromSession
     */
    public HashMap<String, R66Dir> getDirsFromSession() {
        return dirsFromSession;
    }
}
