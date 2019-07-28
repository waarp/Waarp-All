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
import org.dom4j.io.SAXReader;
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

  protected static String _infoArgs =
      Messages.getString("SpooledDirectoryTransfer.0"); //$NON-NLS-1$

  protected static final String NO_INFO_ARGS = "noinfo";

  protected final R66Future future;

  public final String name;

  protected final List<String> directory;

  protected final String statusFile;

  protected final String stopFile;

  protected final String rulename;

  protected final String fileinfo;

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

  protected final NetworkTransaction networkTransaction;

  protected FileMonitor monitor;

  private long sent;
  private long error;

  /**
   * @param future
   * @param name
   * @param directory
   * @param statusfile
   * @param stopfile
   * @param rulename
   * @param fileinfo
   * @param isMD5
   * @param remoteHosts
   * @param blocksize
   * @param regex
   * @param elapse
   * @param submit
   * @param nolog
   * @param recursive
   * @param elapseWaarp
   * @param parallel
   * @param waarphost
   * @param minimalSize
   * @param networkTransaction
   */
  public SpooledDirectoryTransfer(R66Future future, String name,
                                  List<String> directory, String statusfile,
                                  String stopfile, String rulename,
                                  String fileinfo, boolean isMD5,
                                  List<String> remoteHosts, int blocksize,
                                  String regex, long elapse, boolean submit,
                                  boolean nolog, boolean recursive,
                                  long elapseWaarp, boolean parallel,
                                  int limitParallel, List<String> waarphost,
                                  long minimalSize, boolean logWarn,
                                  NetworkTransaction networkTransaction) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(SpooledDirectoryTransfer.class);
    }
    this.future = future;
    this.name = name;
    this.directory = directory;
    statusFile = statusfile;
    stopFile = stopfile;
    this.rulename = rulename;
    this.fileinfo = fileinfo;
    this.isMD5 = isMD5;
    this.remoteHosts = remoteHosts;
    this.blocksize = blocksize;
    regexFilter = regex;
    elapseTime = elapse;
    this.submit = submit;
    this.nolog = nolog && !submit;
    AbstractTransfer.nolog = this.nolog;
    recurs = recursive;
    elapseWaarpTime = elapseWaarp;
    if (this.submit) {
      this.parallel = false;
    } else {
      this.parallel = parallel;
    }
    limitParallelTasks = limitParallel;
    waarpHosts = waarphost;
    this.minimalSize = minimalSize;
    normalInfoAsWarn = logWarn;
    this.networkTransaction = networkTransaction;
  }

  @Override
  public void run() {
    // FIXME never true since change for DbAdmin
    if (submit && !admin.isActive()) {
      logger.error(
          Messages.getString("SpooledDirectoryTransfer.2")); //$NON-NLS-1$
      future.cancel();
      if (Configuration.configuration
              .getShutdownConfiguration().serviceFuture != null) {
        Configuration.configuration.getShutdownConfiguration().serviceFuture
            .setFailure(new Exception(
                Messages.getString("SpooledDirectoryTransfer.2")));
      }
      return;
    }
    setSent(0);
    setError(0);
    // first check if rule is for SEND
    DbRule dbrule;
    try {
      dbrule = new DbRule(rulename);
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
          public void run(FileItem file) {
            if (normalInfoAsWarn) {
              logger.warn("File removed: {}", file.file);
            } else {
              logger.info("File removed: {}", file.file);
            }
          }
        };
    FileMonitorCommandRunnableFuture waarpHostCommand;
    File dir = new File(directory.get(0));
    monitor = new FileMonitor(name, status, stop, dir, null, elapseTime, filter,
                              recurs, commandValidFile, waarpRemovedCommand,
                              null);
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
            public FileMonitorCommandRunnableFuture create(FileItem fileItem) {
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
        public void run(FileItem notused) {
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
              logger.info("Will inform back Waarp hosts of current history: " +
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
                  logger.info("Can't inform Waarp server: " + host + " since " +
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
                  logger.debug("Inform back Waarp hosts over for: " + host);
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
                stopFile + " rulename:" + rulename + " fileinfo:" + fileinfo +
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

    public SpooledRunner(FileItem fileItem) {
      super(fileItem);
    }

    @Override
    public void run(FileItem fileItem) {
      setFileItem(fileItem);
      if (admin.getSession() != null && admin.getSession().isDisActive()) {
        admin.getSession().checkConnectionNoException();
      }
      boolean finalStatus = false;
      int ko = 0;
      long specialId =
          remoteHosts.size() > 1? ILLEGALVALUE : fileItem.specialId;
      try {
        for (String host : remoteHosts) {
          host = host.trim();
          if (host != null && !host.isEmpty()) {
            final String filename = fileItem.file.getAbsolutePath();
            logger
                .info("Launch transfer to " + host + " with file " + filename);
            R66Future r66Future = new R66Future(true);
            String text;
            if (submit) {
              text = "Submit Transfer: ";
              final SubmitTransfer transaction =
                  new SubmitTransfer(r66Future, host, filename, rulename,
                                     fileinfo, isMD5, blocksize, specialId,
                                     null);
              transaction.normalInfoAsWarn = normalInfoAsWarn;
              logger.info(text + host);
              transaction.run();
            } else {
              if (specialId != ILLEGALVALUE) {
                boolean direct = false;
                // Transfer try at least once
                text = "Request Transfer try Restart: " + specialId + ' ' +
                       filename + ' ';
                try {
                  final String srequester = Configuration.configuration
                      .getHostId(admin.getSession(), host);
                  // Try restart
                  final RequestTransfer transaction =
                      new RequestTransfer(r66Future, specialId, host,
                                          srequester, false, false, true,
                                          networkTransaction);
                  transaction.normalInfoAsWarn = normalInfoAsWarn;
                  logger.info(text + host);
                  // special task
                  transaction.run();
                  r66Future.awaitOrInterruptible();
                  if (!r66Future.isSuccess()) {
                    direct = true;
                    text =
                        "Request Transfer Cancelled and Restart: " + specialId +
                        ' ' + filename + ' ';
                    r66Future = new R66Future(true);
                    // Cancel
                    final RequestTransfer transaction2 =
                        new RequestTransfer(r66Future, specialId, host,
                                            srequester, true, false, false,
                                            networkTransaction);
                    transaction.normalInfoAsWarn = normalInfoAsWarn;
                    logger.warn(text + host);
                    transaction2.run();
                    // special task
                    r66Future.awaitOrInterruptible();
                    // FIXME never true since change for DbAdmin
                    if (!admin.isActive()) {
                      DbTaskRunner.removeNoDbSpecialId(specialId);
                    }
                  }
                } catch (final WaarpDatabaseException e) {
                  direct = true;
                  if (admin.getSession() != null) {
                    admin.getSession().checkConnectionNoException();
                  }
                  logger.warn(Messages.getString("RequestTransfer.5") + host,
                              e); //$NON-NLS-1$
                }
                if (direct) {
                  text = "Direct Transfer: ";
                  r66Future = new R66Future(true);
                  final DirectTransfer transaction =
                      new DirectTransfer(r66Future, host, filename, rulename,
                                         fileinfo, isMD5, blocksize,
                                         ILLEGALVALUE, networkTransaction);
                  transaction.normalInfoAsWarn = normalInfoAsWarn;
                  logger.info(text + host);
                  transaction.run();
                }
              } else {
                text = "Direct Transfer: ";
                final DirectTransfer transaction =
                    new DirectTransfer(r66Future, host, filename, rulename,
                                       fileinfo, isMD5, blocksize, ILLEGALVALUE,
                                       networkTransaction);
                transaction.normalInfoAsWarn = normalInfoAsWarn;
                logger.info(text + host);
                transaction.run();
              }
            }
            r66Future.awaitOrInterruptible();
            final R66Result r66result = r66Future.getResult();
            if (r66Future.isSuccess()) {
              finalStatus = true;
              setSent(getSent() + 1);
              DbTaskRunner runner;
              if (r66result != null) {
                runner = r66result.getRunner();
                if (runner != null) {
                  specialId = runner.getSpecialId();
                  String status = Messages
                      .getString("RequestInformation.Success"); //$NON-NLS-1$
                  if (runner.getErrorInfo() == ErrorCode.Warning) {
                    status = Messages
                        .getString("RequestInformation.Warned"); //$NON-NLS-1$
                  }
                  if (normalInfoAsWarn) {
                    logger.warn(text + " status: " + status + "     " +
                                runner.toShortString() + "     <REMOTE>" +
                                host + REMOTE2 + "     <FILEFINAL>" +
                                (r66result.getFile() != null?
                                    r66result.getFile() + "</FILEFINAL>" :
                                    "no file"));
                  } else {
                    logger.info(text + " status: " + status + "     " +
                                runner.toShortString() + "     <REMOTE>" +
                                host + REMOTE2 + "     <FILEFINAL>" +
                                (r66result.getFile() != null?
                                    r66result.getFile() + "</FILEFINAL>" :
                                    "no file"));
                  }
                  if (nolog && !submit) {
                    // In case of success, delete the runner
                    try {
                      runner.delete();
                    } catch (final WaarpDatabaseException e) {
                      logger.warn(
                          "Cannot apply nolog to     " + runner.toShortString(),
                          e);
                    }
                  }
                  DbTaskRunner.removeNoDbSpecialId(specialId);
                } else {
                  if (normalInfoAsWarn) {
                    logger.warn(
                        text + Messages.getString("RequestInformation.Success")
                        //$NON-NLS-1$
                        + REMOTE + host + REMOTE2);
                  } else {
                    logger.info(
                        text + Messages.getString("RequestInformation.Success")
                        //$NON-NLS-1$
                        + REMOTE + host + REMOTE2);
                  }
                }
              } else {
                if (normalInfoAsWarn) {
                  logger.warn(
                      text + Messages.getString("RequestInformation.Success")
                      //$NON-NLS-1$
                      + REMOTE + host + REMOTE2);
                } else {
                  logger.info(
                      text + Messages.getString("RequestInformation.Success")
                      //$NON-NLS-1$
                      + REMOTE + host + REMOTE2);
                }
              }
            } else {
              setError(getError() + 1);
              ko++;
              DbTaskRunner runner;
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
                  specialId = runner.getSpecialId();
                  // FIXME never true since change for DbAdmin
                  if (!admin.isActive() && remoteHosts.size() > 1) {
                    DbTaskRunner.removeNoDbSpecialId(specialId);
                    specialId = ILLEGALVALUE;
                    // FIXME always true since change for DbAdmin
                  } else if (admin.isActive()) {
                    DbTaskRunner.removeNoDbSpecialId(specialId);
                  }
                  if (isConnectionImpossible) {
                    logger.info(
                        text + Messages.getString(REQUEST_INFORMATION_FAILURE) +
                        //$NON-NLS-1$
                        runner.toShortString() + REMOTE + host +
                        "</REMOTE><REASON>" + errMsg + "</REASON>");
                  } else {
                    logger.error(
                        text + Messages.getString(REQUEST_INFORMATION_FAILURE) +
                        //$NON-NLS-1$
                        runner.toShortString() + REMOTE + host +
                        "</REMOTE><REASON>" + errMsg + "</REASON>");
                  }
                } else {
                  if (isConnectionImpossible) {
                    logger.info(
                        text + Messages.getString(REQUEST_INFORMATION_FAILURE) +
                        REMOTE + host + REMOTE2, r66Future.getCause());
                  } else {
                    logger.error(
                        text + Messages.getString(REQUEST_INFORMATION_FAILURE) +
                        REMOTE + host + REMOTE2, r66Future.getCause());
                  }
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
      specialId = remoteHosts.size() > 1? ILLEGALVALUE : specialId;
      if (ko > 0) {
        // If at least one is in error, the transfer is in error so should be redone
        finalStatus = false;
      }
      finalizeValidFile(finalStatus, specialId);
    }
  }

  /**
   * Default arguments
   */
  protected static class Arguments {
    protected String sname;
    protected final List<String> rhosts = new ArrayList<String>();
    protected final List<String> localDirectory = new ArrayList<String>();
    protected String rule;
    protected String fileInfo = NO_INFO_ARGS;
    protected boolean ismd5;
    protected int block = 0x10000; // 64K as default
    protected String statusfile;
    protected String stopfile;
    protected String regex;
    protected long elapsed = 1000;
    protected long elapsedWaarp = 5000;
    protected boolean tosubmit = true;
    protected boolean noLog;
    protected boolean recursive;
    protected final List<String> waarphosts = new ArrayList<String>();
    protected boolean isparallel = true;
    protected int limitParallel;
    protected long minimalSize;
    protected boolean logWarn = true;
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
  protected static boolean getParamsFromConfigFile(String filename) {
    Document document;
    // Open config file
    try {
      document = new SAXReader().read(filename);
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
    XmlValue[] configuration = XmlUtil.read(document, configSpooled);
    XmlHash hashConfig = new XmlHash(configuration);
    XmlValue value = hashConfig.get(XML_STOPFILE);
    String stopfile;
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
        arg.stopfile = stopfile;
        arg.logWarn = logWarn;
        final XmlHash subHash = new XmlHash(xml);
        value = subHash.get(XML_NAME);
        if (value != null && !value.isEmpty()) {
          arg.sname = value.getString();
        }
        value = subHash.get(XML_TO);
        if (value != null && value.getList() != null) {
          for (final String to : (Iterable<String>) value.getList()) {
            if (to.trim().isEmpty()) {
              continue;
            }
            arg.rhosts.add(to.trim());
          }
          if (arg.rhosts.isEmpty()) {
            logger.warn("to directive is empty but must not");
            continue;
          }
        } else {
          logger.warn("to directive is empty but must not");
          continue;
        }
        value = subHash.get(XML_RULE);
        if (value != null && !value.isEmpty()) {
          arg.rule = value.getString();
        } else {
          logger.warn("rule directive is empty but must not");
          continue;
        }
        value = subHash.get(XML_STATUSFILE);
        if (value != null && !value.isEmpty()) {
          arg.statusfile = value.getString();
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
            arg.localDirectory.add(dir.trim());
          }
          if (arg.localDirectory.isEmpty()) {
            logger.warn("directory directive is empty but must not");
            continue;
          }
        } else {
          logger.warn("directory directive is empty but must not");
          continue;
        }
        value = subHash.get(XML_REGEX);
        if (value != null && !value.isEmpty()) {
          arg.regex = value.getString();
        }
        value = subHash.get(XML_RECURSIVE);
        if (value != null && !value.isEmpty()) {
          arg.recursive = value.getBoolean();
        }
        value = subHash.get(XML_ELAPSE);
        if (value != null && !value.isEmpty()) {
          arg.elapsed = value.getLong();
        }
        value = subHash.get(XML_SUBMIT);
        if (value != null && !value.isEmpty()) {
          arg.tosubmit = value.getBoolean();
        }
        value = subHash.get(XML_PARALLEL);
        if (value != null && !value.isEmpty()) {
          arg.isparallel = value.getBoolean();
        }
        value = subHash.get(XML_LIMIT_PARALLEL);
        if (value != null && !value.isEmpty()) {
          arg.limitParallel = value.getInteger();
        }
        value = subHash.get(XML_INFO);
        if (value != null && !value.isEmpty()) {
          arg.fileInfo = value.getString();
        }
        value = subHash.get(XML_MD_5);
        if (value != null && !value.isEmpty()) {
          arg.ismd5 = value.getBoolean();
        }
        value = subHash.get(XML_BLOCK);
        if (value != null && !value.isEmpty()) {
          arg.block = value.getInteger();
        }
        value = subHash.get(XML_NOLOG);
        if (value != null && !value.isEmpty()) {
          arg.noLog = value.getBoolean();
        }
        value = subHash.get(XML_WAARP);
        if (value != null && value.getList() != null) {
          for (final String host : (Iterable<String>) value.getList()) {
            if (host.trim().isEmpty()) {
              continue;
            }
            arg.waarphosts.add(host.trim());
          }
        }
        value = subHash.get(XML_ELAPSE_WAARP);
        if (value != null && !value.isEmpty()) {
          arg.elapsedWaarp = value.getLong();
        }
        value = subHash.get(XML_MINIMAL_SIZE);
        if (value != null && !value.isEmpty()) {
          arg.minimalSize = value.getLong();
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
  protected static boolean getParams(String[] args) {
    _infoArgs = Messages.getString("SpooledDirectoryTransfer.0"); //$NON-NLS-1$
    if (args.length < 1) {
      logger.error(_infoArgs);
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
        logger.error(_infoArgs);
        return false;
      }
      // Now set default values from configuration
      final Arguments arg = new Arguments();
      arg.block = Configuration.configuration.getBlockSize();
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
              arg.rhosts.add(string);
            }
          } else if ("-name".equalsIgnoreCase(args[i])) {
            i++;
            arg.sname = args[i];
          } else if ("-directory".equalsIgnoreCase(args[i])) {
            i++;
            final String[] dir = args[i].split(",");
            for (final String string : dir) {
              if (string.trim().isEmpty()) {
                continue;
              }
              arg.localDirectory.add(string.trim());
            }
          } else if ("-rule".equalsIgnoreCase(args[i])) {
            i++;
            arg.rule = args[i];
          } else if ("-statusfile".equalsIgnoreCase(args[i])) {
            i++;
            arg.statusfile = args[i];
          } else if ("-stopfile".equalsIgnoreCase(args[i])) {
            i++;
            arg.stopfile = args[i];
          } else if ("-info".equalsIgnoreCase(args[i])) {
            i++;
            arg.fileInfo = args[i];
          } else if ("-md5".equalsIgnoreCase(args[i])) {
            arg.ismd5 = true;
          } else if ("-block".equalsIgnoreCase(args[i])) {
            i++;
            arg.block = Integer.parseInt(args[i]);
            if (arg.block < 100) {
              logger.error(Messages.getString("AbstractTransfer.1") +
                           arg.block); //$NON-NLS-1$
              return false;
            }
          } else if ("-nolog".equalsIgnoreCase(args[i])) {
            arg.noLog = true;
          } else if ("-submit".equalsIgnoreCase(args[i])) {
            arg.tosubmit = true;
          } else if ("-direct".equalsIgnoreCase(args[i])) {
            arg.tosubmit = false;
          } else if ("-recursive".equalsIgnoreCase(args[i])) {
            arg.recursive = true;
          } else if ("-logWarn".equalsIgnoreCase(args[i])) {
            arg.logWarn = true;
          } else if ("-notlogWarn".equalsIgnoreCase(args[i])) {
            arg.logWarn = false;
          } else if ("-regex".equalsIgnoreCase(args[i])) {
            i++;
            arg.regex = args[i];
          } else if ("-waarp".equalsIgnoreCase(args[i])) {
            i++;
            final String[] host = args[i].split(",");
            for (final String string : host) {
              if (string.trim().isEmpty()) {
                continue;
              }
              arg.waarphosts.add(string.trim());
            }
          } else if ("-elapse".equalsIgnoreCase(args[i])) {
            i++;
            arg.elapsed = Long.parseLong(args[i]);
          } else if ("-elapseWaarp".equalsIgnoreCase(args[i])) {
            i++;
            arg.elapsedWaarp = Long.parseLong(args[i]);
          } else if ("-minimalSize".equalsIgnoreCase(args[i])) {
            i++;
            arg.minimalSize = Long.parseLong(args[i]);
          } else if ("-limitParallel".equalsIgnoreCase(args[i])) {
            i++;
            arg.limitParallel = Integer.parseInt(args[i]);
          } else if ("-parallel".equalsIgnoreCase(args[i])) {
            arg.isparallel = true;
          } else if ("-sequential".equalsIgnoreCase(args[i])) {
            arg.isparallel = false;
          }
        }
      } catch (final NumberFormatException e) {
        logger
            .error(Messages.getString("AbstractTransfer.20") + i); //$NON-NLS-1$
        return false;
      }
      if (arg.fileInfo == null) {
        arg.fileInfo = NO_INFO_ARGS;
      }
      if (arg.sname == null) {
        arg.sname = Configuration.configuration.getHostId() + " : " +
                    arg.localDirectory;
      }
      // FIXME never true since change for DbAdmin
      if (arg.tosubmit && !admin.isActive()) {
        logger.error(
            Messages.getString("SpooledDirectoryTransfer.2")); //$NON-NLS-1$
        return false;
      }
      if (!arg.rhosts.isEmpty() && arg.rule != null &&
          !arg.localDirectory.isEmpty() && arg.statusfile != null &&
          arg.stopfile != null) {
        arguments.add(arg);
        return true;
      }
      logger.error(Messages.getString("SpooledDirectoryTransfer.56") +
                   //$NON-NLS-1$
                   _infoArgs);
      return false;
    }
    return !arguments.isEmpty();
  }

  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
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
  public static boolean initialize(String[] args, boolean normalStart) {
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
            new SpooledDirectoryTransfer(future, arg.sname, arg.localDirectory,
                                         arg.statusfile, arg.stopfile, arg.rule,
                                         arg.fileInfo, arg.ismd5, arg.rhosts,
                                         arg.block, arg.regex, arg.elapsed,
                                         arg.tosubmit, arg.noLog, arg.recursive,
                                         arg.elapsedWaarp, arg.isparallel,
                                         arg.limitParallel, arg.waarphosts,
                                         arg.minimalSize, arg.logWarn,
                                         networkTransactionStatic);
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
  private void setSent(long sent) {
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
  private void setError(long error) {
    this.error = error;
  }

}
