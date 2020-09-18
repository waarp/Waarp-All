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
package org.waarp.openr66.client;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.filemonitor.FileMonitor;
import org.waarp.common.filemonitor.FileMonitor.FileItem;
import org.waarp.common.filemonitor.FileMonitorCommandFactory;
import org.waarp.common.filemonitor.FileMonitorCommandRunnableFuture;
import org.waarp.common.filemonitor.RegexFileFilter;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.common.utility.WaarpThreadFactory;
import org.waarp.common.xml.XmlDecl;
import org.waarp.common.xml.XmlHash;
import org.waarp.common.xml.XmlType;
import org.waarp.common.xml.XmlUtil;
import org.waarp.common.xml.XmlValue;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.task.SpooledInformTask;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.localhandler.packet.BusinessRequestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.waarp.common.database.DbConstant.*;
import static org.waarp.openr66.context.ErrorCode.*;

/**
 * Direct Transfer from a client with or without database connection or Submit
 * Transfer from a client with
 * database connection to transfer files from a spooled directory to possibly
 * multiple hosts at once.<br>
 * -to Hosts will have to be separated by ','.<br>
 * -rule Rule to be used to send files to partners<br>
 * <br>
 * Mandatory additional elements:<br>
 * -directory source (directory to spooled on ; many directories can be
 * specified using a comma separated list
 * as "directory1,directory2,directory3")<br>
 * -statusfile file (file to use as permanent status (if process is killed or
 * aborts))<br>
 * -stopfile file (file when created will stop the dameon)<br>
 * Other options:<br>
 * -info info to be send with the file as filetransfer information<br>
 * -md5 for md5 option<br>
 * -block size for block size specification<br>
 * -nolog to prevent saving action locally<br>
 * -regex regex (regular expression to filter file names from directory
 * source)<br>
 * -elapse elapse (elapse time in ms > 100 ms between 2 checks of the
 * directory)<br>
 * -submit (to submit only: default: only one between submit and direct is
 * allowed)<br>
 * -direct (to directly transfer only: only one between submit and direct is
 * allowed)<br>
 * -recursive (to scan recursively from the root)<br>
 * -waarp WaarpHosts (seperated by ',') to inform of running spooled directory
 * (information stays in memory of
 * Waarp servers, not in database)<br>
 * -name name to be used as name in list printing in Waarp servers. Note this
 * name must be unique
 * globally.<br>
 * -elapseWaarp elapse to specify a specific timing > 1000ms between to
 * information sent to Waarp servers
 * (default: 5000ms)<br>
 * -parallel to allow (default) parallelism between send actions and
 * information<br>
 * -sequential to not allow parallelism between send actions and
 * information<br>
 * -limitParallel limit to specify the number of concurrent actions in -direct
 * mode only<br>
 * -minimalSize limit to specify the minimal size of each file that will be
 * transferred (default: no
 * limit)<br>
 * -notlogWarn | -logWarn to deactivate or activate (default) the logging in
 * Warn mode of Send/Remove
 * information of the spool<br>
 */
public class SpooledDirectoryTransfer implements Runnable {
  public static final String NEEDFULL = "needfull";
  public static final String PARTIALOK = "Validated";

  /**
   * Internal Logger
   */
  protected static volatile WaarpLogger logger;

  protected static String infoArgs =
      Messages.getString("SpooledDirectoryTransfer.0"); //$NON-NLS-1$

  protected static final String NO_INFO_ARGS = "noinfo";

  protected final R66Future future;

  public final String name;

  protected final List<String> directory;

  protected final String statusFile;

  protected final String stopFile;

  protected final String ruleName;

  protected final String fileInfo;

  protected final boolean isMD5;

  protected final List<String> remoteHosts;

  protected final String regexFilter;

  protected final List<String> waarpHosts;

  protected final int blocksize;

  protected final long elapseTime;

  protected final long elapseWaarpTime;

  protected final boolean parallel;

  protected final int limitParallelTasks;

  protected final boolean submit;

  protected final boolean nolog;

  protected final boolean recurs;

  protected final long minimalSize;

  protected final boolean normalInfoAsWarn;

  protected final boolean ignoreAlreadyUsed;

  protected final NetworkTransaction networkTransaction;

  protected FileMonitor monitor;

  private long sent;
  private long error;

  /**
   * @param future
   * @param arguments
   * @param networkTransaction
   */
  public SpooledDirectoryTransfer(final R66Future future,
                                  final Arguments arguments,
                                  final NetworkTransaction networkTransaction) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(SpooledDirectoryTransfer.class);
    }
    this.future = future;
    this.name = arguments.name;
    this.directory = arguments.localDirectory;
    statusFile = arguments.statusFile;
    stopFile = arguments.stopFile;
    this.ruleName = arguments.rule;
    this.fileInfo = arguments.fileInfo;
    this.isMD5 = arguments.isMd5;
    this.remoteHosts = arguments.remoteHosts;
    this.blocksize = arguments.block;
    regexFilter = arguments.regex;
    elapseTime = arguments.elapsed;
    this.submit = arguments.toSubmit;
    this.nolog = arguments.noLog && !arguments.toSubmit;
    AbstractTransfer.nolog = this.nolog;
    recurs = arguments.recursive;
    elapseWaarpTime = arguments.elapsedWaarp;
    if (this.submit) {
      this.parallel = false;
    } else {
      this.parallel = arguments.isParallel;
    }
    limitParallelTasks = arguments.limitParallel;
    waarpHosts = arguments.waarpHosts;
    this.minimalSize = arguments.minimalSize;
    normalInfoAsWarn = arguments.logWarn;
    this.ignoreAlreadyUsed = arguments.ignoreAlreadyUsed;
    this.networkTransaction = networkTransaction;
  }

  @Override
  public void run() {
    setSent(0);
    setError(0);
    // first check if rule is for SEND
    final DbRule dbrule;
    try {
      dbrule = new DbRule(ruleName);
    } catch (final WaarpDatabaseException e1) {
      logger.error(Messages.getString("Transfer.18"), e1); //$NON-NLS-1$
      future.setFailure(e1);
      if (Configuration.configuration
              .getShutdownConfiguration().serviceFuture != null) {
        Configuration.configuration.getShutdownConfiguration().serviceFuture
            .setFailure(new Exception(
                Messages.getString("Transfer.18") + e1.getMessage()));
      }
      return;
    }
    if (dbrule.isRecvMode()) {
      logger.error(
          Messages.getString("SpooledDirectoryTransfer.5")); //$NON-NLS-1$
      future.cancel();
      if (Configuration.configuration
              .getShutdownConfiguration().serviceFuture != null) {
        Configuration.configuration.getShutdownConfiguration().serviceFuture
            .setFailure(new Exception(
                Messages.getString("SpooledDirectoryTransfer.5")));
      }
      return;
    }
    final File status = new File(statusFile);
    if (status.isDirectory()) {
      logger.error(
          Messages.getString("SpooledDirectoryTransfer.6")); //$NON-NLS-1$
      future.cancel();
      if (Configuration.configuration
              .getShutdownConfiguration().serviceFuture != null) {
        Configuration.configuration.getShutdownConfiguration().serviceFuture
            .setFailure(new Exception(
                Messages.getString("SpooledDirectoryTransfer.6")));
      }
      return;
    }
    final File stop = new File(stopFile);
    if (stop.isDirectory()) {
      logger.error(
          Messages.getString("SpooledDirectoryTransfer.7")); //$NON-NLS-1$
      future.cancel();
      if (Configuration.configuration
              .getShutdownConfiguration().serviceFuture != null) {
        Configuration.configuration.getShutdownConfiguration().serviceFuture
            .setFailure(new Exception(
                Messages.getString("SpooledDirectoryTransfer.7")));
      }
      return;
    } else if (stop.exists()) {
      logger
          .warn(Messages.getString("SpooledDirectoryTransfer.8")); //$NON-NLS-1$
      future.setSuccess();
      if (Configuration.configuration
              .getShutdownConfiguration().serviceFuture != null) {
        Configuration.configuration.getShutdownConfiguration().serviceFuture
            .setFailure(new Exception(
                Messages.getString("SpooledDirectoryTransfer.8")));
      }
      return;
    }
    for (final String dirname : directory) {
      final File dir = new File(dirname);
      if (!dir.isDirectory()) {
        logger.error(Messages.getString("SpooledDirectoryTransfer.9") + " : " +
                     dir); //$NON-NLS-1$
        future.cancel();
        if (Configuration.configuration
                .getShutdownConfiguration().serviceFuture != null) {
          Configuration.configuration.getShutdownConfiguration().serviceFuture
              .setFailure(new Exception(
                  Messages.getString("SpooledDirectoryTransfer.9")));
        }
        return;
      }
    }
    FileFilter filter = null;
    if (regexFilter != null) {
      filter = new RegexFileFilter(regexFilter, minimalSize);
    } else if (minimalSize > 0) {
      filter = new RegexFileFilter(minimalSize);
    }
    // Will be used if no parallelism
    final FileMonitorCommandRunnableFuture commandValidFile =
        new SpooledRunner(null);
    final FileMonitorCommandRunnableFuture waarpRemovedCommand =
        new FileMonitorCommandRunnableFuture() {
          @Override
          public void run(final FileItem file) {
            if (normalInfoAsWarn) {
              logger.warn("File removed: {}", file.file);
            } else {
              logger.info("File removed: {}", file.file);
            }
          }
        };
    final FileMonitorCommandRunnableFuture waarpHostCommand;
    File dir = new File(directory.get(0));
    monitor = new FileMonitor(name, status, stop, dir, null, elapseTime, filter,
                              recurs, commandValidFile, waarpRemovedCommand,
                              null);
    monitor.setIgnoreAlreadyUsed(ignoreAlreadyUsed);
    if (!monitor.initialized()) {
      // wrong
      logger.error(
          Messages.getString("Configuration.WrongInit") + " : already running");
      future.cancel();
      if (Configuration.configuration
              .getShutdownConfiguration().serviceFuture != null) {
        Configuration.configuration.getShutdownConfiguration().serviceFuture
            .setFailure(new Exception(
                Messages.getString("Configuration.WrongInit") +
                " : already running"));
      }
      return;
    }
    commandValidFile.setMonitor(monitor);
    if (parallel) {
      final FileMonitorCommandFactory factory =
          new FileMonitorCommandFactory() {

            @Override
            public FileMonitorCommandRunnableFuture create(
                final FileItem fileItem) {
              final SpooledRunner runner = new SpooledRunner(fileItem);
              runner.setMonitor(monitor);
              return runner;
            }
          };
      monitor.setCommandValidFileFactory(factory, limitParallelTasks);
    }
    final FileMonitor monitorArg = monitor;
    if (waarpHosts != null && !waarpHosts.isEmpty()) {
      waarpHostCommand = new FileMonitorCommandRunnableFuture() {
        @Override
        public void run(final FileItem notused) {
          try {
            Thread.currentThread().setName("FileMonitorInformation_" + name);
            if (admin.getSession() != null &&
                admin.getSession().isDisActive()) {
              admin.getSession().checkConnectionNoException();
            }
            String status = monitorArg.getStatus();
            if (normalInfoAsWarn) {
              logger.warn("Will inform back Waarp hosts of current history: " +
                          monitorArg.getCurrentHistoryNb());
            } else {
              logger.info("Will inform back Waarp hosts of current history: {}",
                          monitorArg.getCurrentHistoryNb());
            }
            for (String host : waarpHosts) {
              host = host.trim();
              if (host != null && !host.isEmpty()) {
                final R66Future r66Future = new R66Future(true);
                final BusinessRequestPacket packet = new BusinessRequestPacket(
                    SpooledInformTask.class.getName() + ' ' + status, 0);
                final BusinessRequest transaction =
                    new BusinessRequest(networkTransaction, r66Future, host,
                                        packet);
                transaction.run();
                r66Future.awaitOrInterruptible();
                if (!r66Future.isSuccess()) {
                  logger.info("Can't inform Waarp server: {} since {}", host,
                              r66Future.getCause());
                } else {
                  final R66Result result = r66Future.getResult();
                  if (result == null) {
                    monitorArg.setNextAsFullStatus();
                  } else {
                    status = (String) result.getOther();
                    if (status == null || status.equalsIgnoreCase(NEEDFULL)) {
                      monitorArg.setNextAsFullStatus();
                    }
                  }
                  logger.debug("Inform back Waarp hosts over for: {}", host);
                }
              }
            }
          } catch (final Throwable e) {
            logger.error("Issue during Waarp information", e);
            // ignore
          }
        }
      };
      monitor.setCommandCheckIteration(waarpHostCommand);
      monitor.setElapseWaarpTime(elapseWaarpTime);
    }
    for (int i = 1; i < directory.size(); i++) {
      dir = new File(directory.get(i));
      monitor.addDirectory(dir);
    }
    logger.warn("SpooledDirectoryTransfer starts name:" + name + " directory:" +
                directory + " statusFile:" + statusFile + " stopFile:" +
                stopFile + " rulename:" + ruleName + " fileinfo:" + fileInfo +
                " hosts:" + remoteHosts + " regex:" + regexFilter +
                " minimalSize:" + minimalSize + " waarp:" + waarpHosts +
                " elapse:" + elapseTime + " waarpElapse:" + elapseWaarpTime +
                " parallel:" + parallel + " limitParallel:" +
                limitParallelTasks + " submit:" + submit + " recursive:" +
                recurs);
    monitor.start();
    monitor.waitForStopFile();
    future.setSuccess();
    if (Configuration.configuration.getShutdownConfiguration().serviceFuture !=
        null) {
      Configuration.configuration.getShutdownConfiguration().serviceFuture
          .setSuccess();
    }
  }

  public void stop() {
    if (monitor != null) {
      logger.info("Stop Monitor");
      monitor.stop();
      logger.info("Monitor Stopped");
    } else {
      logger.warn("NO MONITOR found");
    }
  }

  public class SpooledRunner extends FileMonitorCommandRunnableFuture {
    private static final String REQUEST_INFORMATION_FAILURE =
        "RequestInformation.Failure";
    private static final String REMOTE2 = "</REMOTE>";
    private static final String REMOTE = "<REMOTE>";

    public SpooledRunner(final FileItem fileItem) {
      super(fileItem);
      if (logger == null) {
        logger = WaarpLoggerFactory.getLogger(SpooledDirectoryTransfer.class);
      }
    }

    @Override
    public void run(final FileItem fileItem) {
      setFileItem(fileItem);
      checkReuse(ignoreAlreadyUsed);
      if (admin.getSession() != null && admin.getSession().isDisActive()) {
        admin.getSession().checkConnectionNoException();
      }
      boolean finalStatus = false;
      int ko = 0;
      long specialId =
          remoteHosts.size() > 1? ILLEGALVALUE : fileItem.specialId;
      long newSpecialId = ILLEGALVALUE;
      // check if already launched before
      if (isIgnored(ignoreAlreadyUsed)) {
        // Nothing to do
        return;
      }
      specialId = checkReuseUniqueHost(fileItem, specialId);
      try {
        for (String host : remoteHosts) {
          host = host.trim();
          if (host != null && !host.isEmpty()) {
            final String filename = fileItem.file.getAbsolutePath();
            logger
                .info("Launch transfer to " + host + " with file " + filename);
            R66Future r66Future = new R66Future(true);
            final String text;
            if (submit) {
              text = submitTransfer(specialId, host, filename, r66Future);
            } else {
              if (specialId != ILLEGALVALUE) {
                // Clean previously transfer if any
                cleanPreviousTransfer(specialId, host, filename, r66Future);
              }
              text = "Direct Transfer: ";
              r66Future = new R66Future(true);
              directTransfer(host, filename, r66Future, text);
            }
            r66Future.awaitOrInterruptible();
            final R66Result r66result = r66Future.getResult();
            if (r66Future.isSuccess()) {
              finalStatus = true;
              newSpecialId =
                  finalizeInSuccess(newSpecialId, host, text, r66result);
            } else {
              setError(getError() + 1);
              ko++;
              final DbTaskRunner runner;
              if (r66result != null) {
                String errMsg = "Unknown Error Message";
                if (r66Future.getCause() != null) {
                  errMsg = r66Future.getCause().getMessage();
                }
                final boolean isConnectionImpossible =
                    r66result.getCode() == ErrorCode.ConnectionImpossible &&
                    !normalInfoAsWarn;
                runner = r66result.getRunner();
                if (runner != null) {
                  newSpecialId = koOnFoundRunner(host, text, runner, errMsg,
                                                 isConnectionImpossible);
                } else {
                  ko = getKoOnNoRunner(ko, host, r66Future, text, r66result,
                                       isConnectionImpossible);
                }
              } else {
                logger.error(
                    text + Messages.getString(REQUEST_INFORMATION_FAILURE)
                    //$NON-NLS-1$
                    + REMOTE + host + REMOTE2, r66Future.getCause());
              }
            }
          }
        }
      } catch (final Throwable e) {
        // catch any exception
        logger.error("Error in SpooledDirectory", e);
        finalStatus = false;
      }
      specialId = remoteHosts.size() > 1? ILLEGALVALUE : newSpecialId;
      if (ko > 0) {
        // If at least one is in error, the transfer is in error so should be redone
        finalStatus = false;
      }
      finalizeValidFile(finalStatus, specialId);
    }

    private void directTransfer(final String host, final String filename,
                                final R66Future r66Future, final String text) {
      final DirectTransfer transaction =
          new DirectTransfer(r66Future, host, filename, ruleName, fileInfo,
                             isMD5, blocksize, ILLEGALVALUE,
                             networkTransaction);
      if (!fileInfo.contains("-nofollow")) {
        TransferArgs.forceAnalyzeFollow(transaction);
      }
      // If retry indefinitely is useful transaction.setLimitRetryConnection(true)
      transaction.normalInfoAsWarn = normalInfoAsWarn;
      logger.info("{}{}", text, host);
      transaction.run();
    }

    private String submitTransfer(final long specialId, final String host,
                                  final String filename,
                                  final R66Future r66Future) {
      final String text;
      text = "Submit Transfer: ";
      final SubmitTransfer transaction =
          new SubmitTransfer(r66Future, host, filename, ruleName, fileInfo,
                             isMD5, blocksize, specialId, null);
      if (!fileInfo.contains("-nofollow")) {
        TransferArgs.forceAnalyzeFollow(transaction);
      }
      transaction.normalInfoAsWarn = normalInfoAsWarn;
      logger.info("{}{}", text, host);
      transaction.run();
      return text;
    }

    private long koOnFoundRunner(final String host, final String text,
                                 final DbTaskRunner runner, final String errMsg,
                                 final boolean isConnectionImpossible) {
      final long newSpecialId;
      newSpecialId = runner.getSpecialId();
      DbTaskRunner.removeNoDbSpecialId(newSpecialId);
      if (isConnectionImpossible) {
        if (logger.isInfoEnabled()) {
          logger.info("{}{}{}{}{}{}{}{}", text,
                      Messages.getString(REQUEST_INFORMATION_FAILURE),
                      //$NON-NLS-1$
                      runner.toShortString(), REMOTE, host, "</REMOTE><REASON>",
                      errMsg, "</REASON>");
        }
      } else {
        logger.error(text + Messages.getString(REQUEST_INFORMATION_FAILURE) +
                     //$NON-NLS-1$
                     runner.toShortString() + REMOTE + host +
                     "</REMOTE><REASON>" + errMsg + "</REASON>");
      }
      return newSpecialId;
    }

    private int getKoOnNoRunner(int ko, final String host,
                                final R66Future r66Future, final String text,
                                final R66Result r66result,
                                final boolean isConnectionImpossible) {
      if (isConnectionImpossible) {
        logger.info("{}{}{}{}{}", text,
                    Messages.getString(REQUEST_INFORMATION_FAILURE), REMOTE,
                    host, REMOTE2, r66Future.getCause());
      } else {
        if (r66result.getCode() == QueryRemotelyUnknown) {
          logger.info("Transfer not found {}{}{}", REMOTE, host, REMOTE2);
          // False negative
          ko--;
          setError(getError() - 1);
        } else {
          logger.error(
              text + Messages.getString(REQUEST_INFORMATION_FAILURE) + REMOTE +
              host + REMOTE2, r66Future.getCause());
        }
      }
      return ko;
    }

    private long finalizeInSuccess(long newSpecialId, final String host,
                                   final String text,
                                   final R66Result r66result) {
      setSent(getSent() + 1);
      final DbTaskRunner runner;
      if (r66result != null) {
        runner = r66result.getRunner();
        if (runner != null) {
          newSpecialId = runner.getSpecialId();
          String status =
              Messages.getString("RequestInformation.Success"); //$NON-NLS-1$
          if (runner.getErrorInfo() == ErrorCode.Warning) {
            status =
                Messages.getString("RequestInformation.Warned"); //$NON-NLS-1$
          }
          if (normalInfoAsWarn) {
            logger.warn(
                text + " status: " + status + "     " + runner.toShortString() +
                "     <REMOTE>" + host + REMOTE2 + "     <FILEFINAL>" +
                (r66result.getFile() != null?
                    r66result.getFile() + "</FILEFINAL>" : "no file"));
          } else if (logger.isInfoEnabled()) {
            logger.info(
                "{} status: {}     {}     <REMOTE>{}</REMOTE>     <FILEFINAL>{}",
                text, status, runner.toShortString(), host,
                (r66result.getFile() != null?
                    r66result.getFile() + "</FILEFINAL>" : "no file"));
          }
          if (nolog && !submit) {
            // In case of success, delete the runner
            try {
              runner.delete();
            } catch (final WaarpDatabaseException e) {
              logger.warn("Cannot apply nolog to     " + runner.toShortString(),
                          e);
            }
          }
          DbTaskRunner.removeNoDbSpecialId(newSpecialId);
        } else {
          if (normalInfoAsWarn) {
            logger.warn(text + Messages.getString("RequestInformation.Success")
                        //$NON-NLS-1$
                        + REMOTE + host + REMOTE2);
          } else {
            logger.info("{}{}{}{}{}", text,
                        Messages.getString("RequestInformation.Success"),
                        //$NON-NLS-1$
                        REMOTE, host, REMOTE2);
          }
        }
      } else {
        if (normalInfoAsWarn) {
          logger.warn(text + Messages.getString("RequestInformation.Success")
                      //$NON-NLS-1$
                      + REMOTE + host + REMOTE2);
        } else {
          logger.info("{}{}{}{}{}", text,
                      Messages.getString("RequestInformation.Success"),
                      //$NON-NLS-1$
                      REMOTE, host, REMOTE2);
        }
      }
      return newSpecialId;
    }

    private void cleanPreviousTransfer(final long specialId, final String host,
                                       final String filename,
                                       final R66Future r66Future) {
      final String text;
      try {
        final String srequester = Configuration.configuration.getHostId(host);
        text =
            "Request Transfer Cancelled: " + specialId + ' ' + filename + ' ';
        // Cancel
        logger.debug("Will try to cancel {}", specialId);
        final RequestTransfer transaction2 =
            new RequestTransfer(r66Future, specialId, host, srequester, true,
                                false, false, networkTransaction);
        transaction2.normalInfoAsWarn = normalInfoAsWarn;
        logger.warn(text + host);
        transaction2.run();
        // special task
        r66Future.awaitOrInterruptible();
      } catch (final WaarpDatabaseException e) {
        if (admin.getSession() != null) {
          admin.getSession().checkConnectionNoException();
        }
        logger.warn(Messages.getString("RequestTransfer.5") + host,
                    e); //$NON-NLS-1$
      }
    }

    private long checkReuseUniqueHost(final FileItem fileItem, long specialId) {
      if (isReuse() && remoteHosts.size() == 1) {
        // if specialId is not IllegalValue, then used was necessarily true
        // before
        if (!submit) {
          // reset fileItem usage
          setValid(fileItem);
        } else {
          // Cancel the unique previous transfer
          final String host = remoteHosts.get(0).trim();
          if (host != null && !host.isEmpty()) {
            final String filename = fileItem.file.getAbsolutePath();
            final String text =
                "Request Transfer to be cancelled: " + fileItem.specialId +
                ' ' + filename + ' ';
            try {
              final R66Future r66Future = new R66Future(true);
              final String srequester =
                  Configuration.configuration.getHostId(host);
              // Try restart
              final RequestTransfer transaction =
                  new RequestTransfer(r66Future, fileItem.specialId, host,
                                      srequester, true, false, false,
                                      networkTransaction);
              transaction.normalInfoAsWarn = normalInfoAsWarn;
              logger.info("{}{}", text, host);
              // special task
              transaction.run();
              r66Future.awaitOrInterruptible();
              // reset fileItem usage
              setValid(fileItem);
              specialId = fileItem.specialId;
            } catch (final WaarpDatabaseException e) {
              if (admin.getSession() != null) {
                admin.getSession().checkConnectionNoException();
              }
              logger.warn(Messages.getString("RequestTransfer.5") + host,
                          e); //$NON-NLS-1$
            }
          }
        }
      }
      return specialId;
    }
  }

  /**
   * Default arguments
   */
  public static class Arguments {
    private String name;
    private final List<String> remoteHosts = new ArrayList<String>();
    private final List<String> localDirectory = new ArrayList<String>();
    private String rule;
    private String fileInfo = NO_INFO_ARGS;
    private boolean isMd5;
    private int block = 0x10000; // 64K as default
    private String statusFile;
    private String stopFile;
    private String regex;
    private long elapsed = 1000;
    private long elapsedWaarp = 5000;
    private boolean toSubmit = true;
    private boolean noLog;
    private boolean recursive;
    private final List<String> waarpHosts = new ArrayList<String>();
    private boolean isParallel = true;
    private int limitParallel;
    private long minimalSize;
    private boolean logWarn = true;
    private boolean ignoreAlreadyUsed = false;

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public List<String> getRemoteHosts() {
      return remoteHosts;
    }

    public List<String> getLocalDirectory() {
      return localDirectory;
    }

    public String getRule() {
      return rule;
    }

    public void setRule(final String rule) {
      this.rule = rule;
    }

    public String getFileInfo() {
      return fileInfo;
    }

    public void setFileInfo(final String fileInfo) {
      this.fileInfo = fileInfo;
    }

    public boolean isMd5() {
      return isMd5;
    }

    public void setMd5(final boolean md5) {
      this.isMd5 = md5;
    }

    public int getBlock() {
      return block;
    }

    public void setBlock(final int block) {
      this.block = block;
    }

    public String getStatusFile() {
      return statusFile;
    }

    public void setStatusFile(final String statusFile) {
      this.statusFile = statusFile;
    }

    public String getStopFile() {
      return stopFile;
    }

    public void setStopFile(final String stopFile) {
      this.stopFile = stopFile;
    }

    public String getRegex() {
      return regex;
    }

    public void setRegex(final String regex) {
      this.regex = regex;
    }

    public long getElapsed() {
      return elapsed;
    }

    public void setElapsed(final long elapsed) {
      this.elapsed = elapsed;
    }

    public long getElapsedWaarp() {
      return elapsedWaarp;
    }

    public void setElapsedWaarp(final long elapsedWaarp) {
      this.elapsedWaarp = elapsedWaarp;
    }

    public boolean isToSubmit() {
      return toSubmit;
    }

    public void setToSubmit(final boolean toSubmit) {
      this.toSubmit = toSubmit;
    }

    public boolean isNoLog() {
      return noLog;
    }

    public void setNoLog(final boolean noLog) {
      this.noLog = noLog;
    }

    public boolean isRecursive() {
      return recursive;
    }

    public void setRecursive(final boolean recursive) {
      this.recursive = recursive;
    }

    public List<String> getWaarpHosts() {
      return waarpHosts;
    }

    public boolean isParallel() {
      return isParallel;
    }

    public void setParallel(final boolean parallel) {
      this.isParallel = parallel;
    }

    public int getLimitParallel() {
      return limitParallel;
    }

    public void setLimitParallel(final int limitParallel) {
      this.limitParallel = limitParallel;
    }

    public long getMinimalSize() {
      return minimalSize;
    }

    public void setMinimalSize(final long minimalSize) {
      this.minimalSize = minimalSize;
    }

    public boolean isLogWarn() {
      return logWarn;
    }

    public void setLogWarn(final boolean logWarn) {
      this.logWarn = logWarn;
    }

    public boolean isIgnoreAlreadyUsed() {
      return ignoreAlreadyUsed;
    }

    public void setIgnoreAlreadyUsed(final boolean ignoreAlreadyUsed) {
      this.ignoreAlreadyUsed = ignoreAlreadyUsed;
    }
  }

  protected static final List<Arguments> arguments = new ArrayList<Arguments>();
  private static final String XML_ROOT = "/config/";
  private static final String XML_SPOOLEDDAEMON = "spooleddaemon";
  private static final String XML_STOPFILE = "stopfile";
  private static final String XML_SPOOLED = "spooled";
  private static final String XML_NAME = "name";
  private static final String XML_TO = "to";
  private static final String XML_RULE = "rule";
  private static final String XML_STATUSFILE = "statusfile";
  private static final String XML_DIRECTORY = "directory";
  private static final String XML_REGEX = "regex";
  private static final String XML_RECURSIVE = "recursive";
  private static final String XML_ELAPSE = "elapse";
  private static final String XML_SUBMIT = "submit";
  private static final String XML_PARALLEL = "parallel";
  private static final String XML_LIMIT_PARALLEL = "limitParallel";
  private static final String XML_INFO = "info";
  private static final String XML_MD_5 = "md5";
  private static final String XML_BLOCK = "block";
  private static final String XML_NOLOG = "nolog";
  private static final String XML_WAARP = "waarp";
  private static final String XML_ELAPSE_WAARP = "elapseWaarp";
  private static final String XML_MINIMAL_SIZE = "minimalSize";
  private static final String XML_LOG_WARN = "logWarn";
  private static final String XML_IGNORED_ALREADY_USED = "ignoreAlreadyUsed";

  private static final XmlDecl[] subSpooled = {
      new XmlDecl(XmlType.STRING, XML_NAME),
      new XmlDecl(XML_TO, XmlType.STRING, XML_TO, true),
      new XmlDecl(XmlType.STRING, XML_RULE),
      new XmlDecl(XmlType.STRING, XML_STATUSFILE),
      new XmlDecl(XML_DIRECTORY, XmlType.STRING, XML_DIRECTORY, true),
      new XmlDecl(XmlType.STRING, XML_REGEX),
      new XmlDecl(XmlType.BOOLEAN, XML_RECURSIVE),
      new XmlDecl(XmlType.LONG, XML_ELAPSE),
      new XmlDecl(XmlType.BOOLEAN, XML_SUBMIT),
      new XmlDecl(XmlType.BOOLEAN, XML_PARALLEL),
      new XmlDecl(XmlType.INTEGER, XML_LIMIT_PARALLEL),
      new XmlDecl(XmlType.STRING, XML_INFO),
      new XmlDecl(XmlType.BOOLEAN, XML_MD_5),
      new XmlDecl(XmlType.INTEGER, XML_BLOCK),
      new XmlDecl(XmlType.BOOLEAN, XML_NOLOG),
      new XmlDecl(XML_WAARP, XmlType.STRING, XML_WAARP, true),
      new XmlDecl(XmlType.LONG, XML_ELAPSE_WAARP),
      new XmlDecl(XmlType.BOOLEAN, XML_IGNORED_ALREADY_USED),
      new XmlDecl(XmlType.LONG, XML_MINIMAL_SIZE)
  };
  private static final XmlDecl[] spooled = {
      new XmlDecl(XmlType.STRING, XML_STOPFILE),
      new XmlDecl(XmlType.BOOLEAN, XML_LOG_WARN),
      new XmlDecl(XML_SPOOLED, XmlType.XVAL, XML_SPOOLED, subSpooled, true)
  };
  private static final XmlDecl[] configSpooled = {
      new XmlDecl(XML_SPOOLEDDAEMON, XmlType.XVAL, XML_ROOT + XML_SPOOLEDDAEMON,
                  spooled, false)
  };

  @SuppressWarnings("unchecked")
  protected static boolean getParamsFromConfigFile(final String filename) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(SpooledDirectoryTransfer.class);
    }
    final Document document;
    // Open config file
    try {
      document = XmlUtil.getNewSaxReader().read(filename);
    } catch (final DocumentException e) {
      logger.error(
          Messages.getString("FileBasedConfiguration.CannotReadXml") + filename,
          e); //$NON-NLS-1$
      return false;
    }
    if (document == null) {
      logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") +
                   filename); //$NON-NLS-1$
      return false;
    }
    final XmlValue[] configuration = XmlUtil.read(document, configSpooled);
    final XmlHash hashConfig = new XmlHash(configuration);
    XmlValue value = hashConfig.get(XML_STOPFILE);
    final String stopfile;
    if (value == null || value.isEmpty()) {
      return false;
    }
    stopfile = value.getString();
    value = hashConfig.get(XML_LOG_WARN);
    boolean logWarn = true;
    if (value != null && !value.isEmpty()) {
      logWarn = value.getBoolean();
    }
    value = hashConfig.get(XML_SPOOLED);
    if (value != null && value.getList() != null) {
      for (final XmlValue[] xml : (Iterable<XmlValue[]>) value.getList()) {
        final Arguments arg = new Arguments();
        arg.setStopFile(stopfile);
        arg.setLogWarn(logWarn);
        final XmlHash subHash = new XmlHash(xml);
        value = subHash.get(XML_NAME);
        if (value != null && !value.isEmpty()) {
          arg.setName(value.getString());
        }
        value = subHash.get(XML_TO);
        if (value != null && value.getList() != null) {
          for (final String to : (Iterable<String>) value.getList()) {
            if (to.trim().isEmpty()) {
              continue;
            }
            arg.getRemoteHosts().add(to.trim());
          }
          if (arg.getRemoteHosts().isEmpty()) {
            logger.warn("to directive is empty but must not");
            continue;
          }
        } else {
          logger.warn("to directive is empty but must not");
          continue;
        }
        value = subHash.get(XML_RULE);
        if (value != null && !value.isEmpty()) {
          arg.setRule(value.getString());
        } else {
          logger.warn("rule directive is empty but must not");
          continue;
        }
        value = subHash.get(XML_STATUSFILE);
        if (value != null && !value.isEmpty()) {
          arg.setStatusFile(value.getString());
        } else {
          logger.warn("statusfile directive is empty but must not");
          continue;
        }
        value = subHash.get(XML_DIRECTORY);
        if (value != null && value.getList() != null) {
          for (final String dir : (Iterable<String>) value.getList()) {
            if (dir.trim().isEmpty()) {
              continue;
            }
            arg.getLocalDirectory().add(dir.trim());
          }
          if (arg.getLocalDirectory().isEmpty()) {
            logger.warn("directory directive is empty but must not");
            continue;
          }
        } else {
          logger.warn("directory directive is empty but must not");
          continue;
        }
        value = subHash.get(XML_REGEX);
        if (value != null && !value.isEmpty()) {
          arg.setRegex(value.getString());
        }
        value = subHash.get(XML_RECURSIVE);
        if (value != null && !value.isEmpty()) {
          arg.setRecursive(value.getBoolean());
        }
        value = subHash.get(XML_ELAPSE);
        if (value != null && !value.isEmpty()) {
          arg.setElapsed(value.getLong());
        }
        value = subHash.get(XML_SUBMIT);
        if (value != null && !value.isEmpty()) {
          arg.setToSubmit(value.getBoolean());
        }
        value = subHash.get(XML_PARALLEL);
        if (value != null && !value.isEmpty()) {
          arg.setParallel(value.getBoolean());
        }
        value = subHash.get(XML_LIMIT_PARALLEL);
        if (value != null && !value.isEmpty()) {
          arg.setLimitParallel(value.getInteger());
        }
        value = subHash.get(XML_INFO);
        if (value != null && !value.isEmpty()) {
          arg.setFileInfo(value.getString());
        }
        value = subHash.get(XML_MD_5);
        if (value != null && !value.isEmpty()) {
          arg.setMd5(value.getBoolean());
        }
        value = subHash.get(XML_BLOCK);
        if (value != null && !value.isEmpty()) {
          arg.setBlock(value.getInteger());
        }
        value = subHash.get(XML_NOLOG);
        if (value != null && !value.isEmpty()) {
          arg.setNoLog(value.getBoolean());
        }
        value = subHash.get(XML_WAARP);
        if (value != null && value.getList() != null) {
          for (final String host : (Iterable<String>) value.getList()) {
            if (host.trim().isEmpty()) {
              continue;
            }
            arg.getWaarpHosts().add(host.trim());
          }
        }
        value = subHash.get(XML_ELAPSE_WAARP);
        if (value != null && !value.isEmpty()) {
          arg.setElapsedWaarp(value.getLong());
        }
        value = subHash.get(XML_MINIMAL_SIZE);
        if (value != null && !value.isEmpty()) {
          arg.setMinimalSize(value.getLong());
        }
        value = subHash.get(XML_IGNORED_ALREADY_USED);
        if (value != null && !value.isEmpty()) {
          arg.setIgnoreAlreadyUsed(value.getBoolean());
        }
        arguments.add(arg);
      }
    }
    hashConfig.clear();
    return !arguments.isEmpty();
  }

  /**
   * Parse the parameter and set current values
   *
   * @param args
   *
   * @return True if all parameters were found and correct
   */
  protected static boolean getParams(final String[] args) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(SpooledDirectoryTransfer.class);
    }
    infoArgs = Messages.getString("SpooledDirectoryTransfer.0"); //$NON-NLS-1$
    if (args.length < 1) {
      logger.error(infoArgs);
      return false;
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger.error(
          Messages.getString("Configuration.NeedCorrectConfig")); //$NON-NLS-1$
      return false;
    }
    // Now check if the configuration file contains already elements of specifications
    if (!getParamsFromConfigFile(args[0])) {
      if (args.length < 11) {
        logger.error(infoArgs);
        return false;
      }
      // Now set default values from configuration
      final Arguments arg = new Arguments();
      arg.setBlock(Configuration.configuration.getBlockSize());
      int i = 1;
      try {
        for (i = 1; i < args.length; i++) {
          if ("-to".equalsIgnoreCase(args[i])) {
            i++;
            final String[] rhosts = args[i].split(",");
            for (String string : rhosts) {
              string = string.trim();
              if (string.isEmpty()) {
                continue;
              }
              if (Configuration.configuration.getAliases()
                                             .containsKey(string)) {
                string = Configuration.configuration.getAliases().get(string);
              }
              arg.getRemoteHosts().add(string);
            }
          } else if ("-name".equalsIgnoreCase(args[i])) {
            i++;
            arg.setName(args[i]);
          } else if ("-directory".equalsIgnoreCase(args[i])) {
            i++;
            final String[] dir = args[i].split(",");
            for (final String string : dir) {
              if (string.trim().isEmpty()) {
                continue;
              }
              arg.getLocalDirectory().add(string.trim());
            }
          } else if ("-rule".equalsIgnoreCase(args[i])) {
            i++;
            arg.setRule(args[i]);
          } else if ("-statusfile".equalsIgnoreCase(args[i])) {
            i++;
            arg.setStatusFile(args[i]);
          } else if ("-stopfile".equalsIgnoreCase(args[i])) {
            i++;
            arg.setStopFile(args[i]);
          } else if ("-info".equalsIgnoreCase(args[i])) {
            i++;
            arg.setFileInfo(args[i]);
          } else if ("-md5".equalsIgnoreCase(args[i])) {
            arg.setMd5(true);
          } else if ("-block".equalsIgnoreCase(args[i])) {
            i++;
            arg.setBlock(Integer.parseInt(args[i]));
            if (arg.getBlock() < 100) {
              logger.error(Messages.getString("AbstractTransfer.1") +
                           arg.getBlock()); //$NON-NLS-1$
              return false;
            }
          } else if ("-nolog".equalsIgnoreCase(args[i])) {
            arg.setNoLog(true);
          } else if ("-submit".equalsIgnoreCase(args[i])) {
            arg.setToSubmit(true);
          } else if ("-direct".equalsIgnoreCase(args[i])) {
            arg.setToSubmit(false);
          } else if ("-recursive".equalsIgnoreCase(args[i])) {
            arg.setRecursive(true);
          } else if ("-logWarn".equalsIgnoreCase(args[i])) {
            arg.setLogWarn(true);
          } else if ("-notlogWarn".equalsIgnoreCase(args[i])) {
            arg.setLogWarn(false);
          } else if ("-regex".equalsIgnoreCase(args[i])) {
            i++;
            arg.setRegex(args[i]);
          } else if ("-waarp".equalsIgnoreCase(args[i])) {
            i++;
            final String[] host = args[i].split(",");
            for (final String string : host) {
              if (string.trim().isEmpty()) {
                continue;
              }
              arg.getWaarpHosts().add(string.trim());
            }
          } else if ("-elapse".equalsIgnoreCase(args[i])) {
            i++;
            arg.setElapsed(Long.parseLong(args[i]));
          } else if ("-elapseWaarp".equalsIgnoreCase(args[i])) {
            i++;
            arg.setElapsedWaarp(Long.parseLong(args[i]));
          } else if ("-minimalSize".equalsIgnoreCase(args[i])) {
            i++;
            arg.setMinimalSize(Long.parseLong(args[i]));
          } else if ("-limitParallel".equalsIgnoreCase(args[i])) {
            i++;
            arg.setLimitParallel(Integer.parseInt(args[i]));
          } else if ("-parallel".equalsIgnoreCase(args[i])) {
            arg.setParallel(true);
          } else if ("-sequential".equalsIgnoreCase(args[i])) {
            arg.setParallel(false);
          } else if ("-ignoreAlreadyUsed".equalsIgnoreCase(args[i])) {
            arg.setIgnoreAlreadyUsed(true);
          }
        }
      } catch (final NumberFormatException e) {
        logger
            .error(Messages.getString("AbstractTransfer.20") + i); //$NON-NLS-1$
        return false;
      }
      if (arg.getFileInfo() == null) {
        arg.setFileInfo(NO_INFO_ARGS);
      }
      if (arg.getName() == null) {
        arg.setName(Configuration.configuration.getHostId() + " : " +
                    arg.getLocalDirectory());
      }
      if (!arg.getRemoteHosts().isEmpty() && arg.getRule() != null &&
          !arg.getLocalDirectory().isEmpty() && arg.getStatusFile() != null &&
          arg.getStopFile() != null) {
        arguments.add(arg);
        return true;
      }
      logger.error(Messages.getString("SpooledDirectoryTransfer.56") +
                   //$NON-NLS-1$
                   infoArgs);
      return false;
    }
    return !arguments.isEmpty();
  }

  public static void main(final String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(SpooledDirectoryTransfer.class);
    }
    initialize(args, true);
  }

  public static final List<SpooledDirectoryTransfer> list =
      new ArrayList<SpooledDirectoryTransfer>();
  public static NetworkTransaction networkTransactionStatic;
  public static ExecutorService executorService;

  /**
   * @param args
   * @param normalStart if True, will exit JVM when all daemons are
   *     stopped;
   *     else False let the caller do (used
   *     by SpooledEngine)
   */
  public static boolean initialize(final String[] args,
                                   final boolean normalStart) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(SpooledDirectoryTransfer.class);
    }
    arguments.clear();
    if (!getParams(args)) {
      logger.error(Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
      if (admin != null) {
        admin.close();
      }
      if (normalStart) {
        ChannelUtils.stopLogger();
        System.exit(2);//NOSONAR
      }
      return false;
    }

    Configuration.configuration.pipelineInit();
    networkTransactionStatic = new NetworkTransaction();
    try {
      executorService = Executors.newCachedThreadPool(
          new WaarpThreadFactory("SpooledDirectoryDaemon"));
      for (final Arguments arg : arguments) {
        final R66Future future = new R66Future(true);
        final SpooledDirectoryTransfer spooled =
            new SpooledDirectoryTransfer(future, arg, networkTransactionStatic);
        executorService.submit(spooled);
        list.add(spooled);
      }
      arguments.clear();
      Thread.sleep(1000);
      executorService.shutdown();
      Configuration.configuration.launchStatistics();
      if (normalStart) {
        while (!executorService
            .awaitTermination(Configuration.configuration.getTimeoutCon(),
                              TimeUnit.MILLISECONDS)) {
          Thread.sleep(Configuration.configuration.getTimeoutCon());
        }
        for (final SpooledDirectoryTransfer spooledDirectoryTransfer : list) {
          logger.warn(Messages.getString("SpooledDirectoryTransfer.58") +
                      spooledDirectoryTransfer.name + ": " +
                      spooledDirectoryTransfer.getSent() + " success, " +
                      spooledDirectoryTransfer.getError() + Messages.getString(
              "SpooledDirectoryTransfer.60")); //$NON-NLS-1$
        }
        list.clear();
      }
      return true;
    } catch (final Throwable e) {
      logger.error("Exception", e);
      return false;
    } finally {
      if (normalStart) {
        WaarpShutdownHook.shutdownWillStart();
        networkTransactionStatic.closeAll();
        System.exit(0);//NOSONAR
      }
    }
  }

  /**
   * @return the sent
   */
  public long getSent() {
    return sent;
  }

  /**
   * @param sent the sent to set
   */
  private void setSent(final long sent) {
    this.sent = sent;
  }

  /**
   * @return the error
   */
  public long getError() {
    return error;
  }

  /**
   * @param error the error to set
   */
  private void setError(final long error) {
    this.error = error;
  }

}
