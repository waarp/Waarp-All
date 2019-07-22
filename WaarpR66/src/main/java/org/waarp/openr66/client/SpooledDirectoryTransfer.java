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
package org.waarp.openr66.client;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.filemonitor.FileMonitor;
import org.waarp.common.filemonitor.FileMonitorCommandFactory;
import org.waarp.common.filemonitor.FileMonitorCommandRunnableFuture;
import org.waarp.common.filemonitor.RegexFileFilter;
import org.waarp.common.filemonitor.FileMonitor.FileItem;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
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
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.localhandler.packet.BusinessRequestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.protocol.utils.R66ShutdownHook;

/**
 * Direct Transfer from a client with or without database connection
 * or Submit Transfer from a client with database connection
 * to transfer files from a spooled directory to possibly multiple hosts at once.<br>
 * -to Hosts will have to be separated by ','.<br>
 * -rule Rule to be used to send files to partners<br>
 * <br>
 * Mandatory additional elements:<br>
 * -directory source (directory to spooled on ; many directories can be specified using a comma separated list as
 * "directory1,directory2,directory3")<br>
 * -statusfile file (file to use as permanent status (if process is killed or aborts))<br>
 * -stopfile file (file when created will stop the dameon)<br>
 * Other options:<br>
 * -info info to be send with the file as filetransfer information<br>
 * -md5 for md5 option<br>
 * -block size for block size specification<br>
 * -nolog to prevent saving action locally<br>
 * -regex regex (regular expression to filter file names from directory source)<br>
 * -elapse elapse (elapse time in ms > 100 ms between 2 checks of the directory)<br>
 * -submit (to submit only: default: only one between submit and direct is allowed)<br>
 * -direct (to directly transfer only: only one between submit and direct is allowed)<br>
 * -recursive (to scan recursively from the root)<br>
 * -waarp WaarpHosts (seperated by ',') to inform of running spooled directory (information stays in memory of Waarp servers, not
 * in database)<br>
 * -name name to be used as name in list printing in Waarp servers. Note this name must be unique globally.<br>
 * -elapseWaarp elapse to specify a specific timing > 1000ms between to information sent to Waarp servers (default: 5000ms)<br>
 * -parallel to allow (default) parallelism between send actions and information<br>
 * -sequential to not allow parallelism between send actions and information<br>
 * -limitParallel limit to specify the number of concurrent actions in -direct mode only<br>
 * -minimalSize limit to specify the minimal size of each file that will be transferred (default: no limit)<br>
 * -notlogWarn | -logWarn to deactivate or activate (default) the logging in Warn mode of Send/Remove information of the spool<br>
 * 
 * @author Frederic Bregier
 * 
 */
public class SpooledDirectoryTransfer implements Runnable {
    public static final String NEEDFULL = "needfull";
    public static final String PARTIALOK = "Validated";

    /**
     * Internal Logger
     */
    static protected volatile WaarpLogger logger;

    protected static String _INFO_ARGS =
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

    protected FileMonitor monitor = null;

    private long sent = 0;
    private long error = 0;

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
    public SpooledDirectoryTransfer(R66Future future, String name, List<String> directory,
            String statusfile, String stopfile, String rulename,
            String fileinfo, boolean isMD5,
            List<String> remoteHosts, int blocksize, String regex,
            long elapse, boolean submit, boolean nolog, boolean recursive,
            long elapseWaarp, boolean parallel, int limitParallel,
            List<String> waarphost, long minimalSize, boolean logWarn, NetworkTransaction networkTransaction) {
        if (logger == null) {
            logger = WaarpLoggerFactory.getLogger(SpooledDirectoryTransfer.class);
        }
        this.future = future;
        this.name = name;
        this.directory = directory;
        this.statusFile = statusfile;
        this.stopFile = stopfile;
        this.rulename = rulename;
        this.fileinfo = fileinfo;
        this.isMD5 = isMD5;
        this.remoteHosts = remoteHosts;
        this.blocksize = blocksize;
        this.regexFilter = regex;
        this.elapseTime = elapse;
        this.submit = submit;
        this.nolog = nolog && (!submit);
        AbstractTransfer.nolog = this.nolog;
        this.recurs = recursive;
        this.elapseWaarpTime = elapseWaarp;
        if (this.submit) {
            this.parallel = false;
        } else {
            this.parallel = parallel;
        }
        this.limitParallelTasks = limitParallel;
        this.waarpHosts = waarphost;
        this.minimalSize = minimalSize;
        this.normalInfoAsWarn = logWarn;
        this.networkTransaction = networkTransaction;
    }

    @Override
    public void run() {
        if (submit && !DbConstant.admin.isActive()) {
            logger.error(Messages.getString("SpooledDirectoryTransfer.2")); //$NON-NLS-1$
            this.future.cancel();
            if (Configuration.configuration.getShutdownConfiguration().serviceFuture != null) {
                Configuration.configuration.getShutdownConfiguration().serviceFuture.setFailure(new Exception(Messages
                        .getString("SpooledDirectoryTransfer.2")));
            }
            return;
        }
        setSent(0);
        setError(0);
        // first check if rule is for SEND
        DbRule dbrule = null;
        try {
            dbrule = new DbRule(rulename);
        } catch (WaarpDatabaseException e1) {
            logger.error(Messages.getString("Transfer.18"), e1); //$NON-NLS-1$
            this.future.setFailure(e1);
            if (Configuration.configuration.getShutdownConfiguration().serviceFuture != null) {
                Configuration.configuration.getShutdownConfiguration().serviceFuture.setFailure(new Exception(Messages
                        .getString("Transfer.18") + e1.getMessage()));
            }
            return;
        }
        if (dbrule.isRecvMode()) {
            logger.error(Messages.getString("SpooledDirectoryTransfer.5")); //$NON-NLS-1$
            this.future.cancel();
            if (Configuration.configuration.getShutdownConfiguration().serviceFuture != null) {
                Configuration.configuration.getShutdownConfiguration().serviceFuture.setFailure(new Exception(Messages
                        .getString("SpooledDirectoryTransfer.5")));
            }
            return;
        }
        File status = new File(statusFile);
        if (status.isDirectory()) {
            logger.error(Messages.getString("SpooledDirectoryTransfer.6")); //$NON-NLS-1$
            this.future.cancel();
            if (Configuration.configuration.getShutdownConfiguration().serviceFuture != null) {
                Configuration.configuration.getShutdownConfiguration().serviceFuture.setFailure(new Exception(Messages
                        .getString("SpooledDirectoryTransfer.6")));
            }
            return;
        }
        File stop = new File(stopFile);
        if (stop.isDirectory()) {
            logger.error(Messages.getString("SpooledDirectoryTransfer.7")); //$NON-NLS-1$
            this.future.cancel();
            if (Configuration.configuration.getShutdownConfiguration().serviceFuture != null) {
                Configuration.configuration.getShutdownConfiguration().serviceFuture.setFailure(new Exception(Messages
                        .getString("SpooledDirectoryTransfer.7")));
            }
            return;
        } else if (stop.exists()) {
            logger.warn(Messages.getString("SpooledDirectoryTransfer.8")); //$NON-NLS-1$
            this.future.setSuccess();
            if (Configuration.configuration.getShutdownConfiguration().serviceFuture != null) {
                Configuration.configuration.getShutdownConfiguration().serviceFuture.setFailure(new Exception(Messages
                        .getString("SpooledDirectoryTransfer.8")));
            }
            return;
        }
        for (String dirname : directory) {
            File dir = new File(dirname);
            if (!dir.isDirectory()) {
                logger.error(Messages.getString("SpooledDirectoryTransfer.9") + " : " + dir); //$NON-NLS-1$
                this.future.cancel();
                if (Configuration.configuration.getShutdownConfiguration().serviceFuture != null) {
                    Configuration.configuration.getShutdownConfiguration().serviceFuture.setFailure(new Exception(Messages
                            .getString("SpooledDirectoryTransfer.9")));
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
        FileMonitorCommandRunnableFuture commandValidFile = new SpooledRunner(null);
        FileMonitorCommandRunnableFuture waarpRemovedCommand = new FileMonitorCommandRunnableFuture() {
            public void run(FileItem file) {
                if (normalInfoAsWarn) {
                    logger.warn("File removed: {}", file.file);
                } else {
                    logger.info("File removed: {}", file.file);
                }
            }
        };
        FileMonitorCommandRunnableFuture waarpHostCommand = null;
        File dir = new File(directory.get(0));
        monitor = new FileMonitor(name, status, stop, dir, null, elapseTime, filter,
                recurs, commandValidFile, waarpRemovedCommand, null);
        if (!monitor.initialized()) {
            // wrong
            logger.error(Messages.getString("Configuration.WrongInit") + " : already running");
            this.future.cancel();
            if (Configuration.configuration.getShutdownConfiguration().serviceFuture != null) {
                Configuration.configuration.getShutdownConfiguration().serviceFuture.setFailure(new Exception(Messages
                        .getString("Configuration.WrongInit") + " : already running"));
            }
            return;
        }
        commandValidFile.setMonitor(monitor);
        if (parallel) {
            FileMonitorCommandFactory factory = new FileMonitorCommandFactory() {

                @Override
                public FileMonitorCommandRunnableFuture create(FileItem fileItem) {
                    SpooledRunner runner = new SpooledRunner(fileItem);
                    runner.setMonitor(monitor);
                    return runner;
                }
            };
            monitor.setCommandValidFileFactory(factory, limitParallelTasks);
        }
        final FileMonitor monitorArg = monitor;
        if (waarpHosts != null && !waarpHosts.isEmpty()) {
            waarpHostCommand = new FileMonitorCommandRunnableFuture() {
                public void run(FileItem notused) {
                    try {
                        Thread.currentThread().setName("FileMonitorInformation_" + name);
                        if (DbConstant.admin.getSession() != null && DbConstant.admin.getSession().isDisActive()) {
                            DbConstant.admin.getSession().checkConnectionNoException();
                        }
                        String status = monitorArg.getStatus();
                        if (normalInfoAsWarn) {
                            logger.warn("Will inform back Waarp hosts of current history: "
                                    + monitorArg.getCurrentHistoryNb());
                        } else {
                            logger.info("Will inform back Waarp hosts of current history: "
                                    + monitorArg.getCurrentHistoryNb());
                        }
                        for (String host : waarpHosts) {
                            host = host.trim();
                            if (host != null && !host.isEmpty()) {
                                R66Future future = new R66Future(true);
                                BusinessRequestPacket packet =
                                        new BusinessRequestPacket(SpooledInformTask.class.getName() + " " + status, 0);
                                BusinessRequest transaction = new BusinessRequest(networkTransaction, future, host,
                                        packet);
                                transaction.run();
                                future.awaitUninterruptibly(Configuration.configuration.getTIMEOUTCON(),
                                        TimeUnit.MILLISECONDS);
                                while (!future.isDone()) {
                                    logger.warn("Out of time during information to Waarp server: " + host);
                                    future.awaitUninterruptibly(Configuration.configuration.getTIMEOUTCON(),
                                            TimeUnit.MILLISECONDS);
                                }
                                if (!future.isSuccess()) {
                                    logger.info("Can't inform Waarp server: " + host + " since " + future.getCause());
                                } else {
                                    R66Result result = future.getResult();
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
                    } catch (Throwable e) {
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
        logger.warn("SpooledDirectoryTransfer starts name:" + name + " directory:" + directory + " statusFile:"
                + statusFile + " stopFile:" + stopFile +
                " rulename:" + rulename + " fileinfo:" + fileinfo + " hosts:" + remoteHosts + " regex:" + regexFilter
                + " minimalSize:" + minimalSize + " waarp:" + waarpHosts +
                " elapse:" + elapseTime + " waarpElapse:" + elapseWaarpTime + " parallel:" + parallel
                + " limitParallel:" + limitParallelTasks +
                " submit:" + submit + " recursive:" + recurs);
        monitor.start();
        monitor.waitForStopFile();
        this.future.setSuccess();
        if (Configuration.configuration.getShutdownConfiguration().serviceFuture != null) {
            Configuration.configuration.getShutdownConfiguration().serviceFuture.setSuccess();
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
        public SpooledRunner(FileItem fileItem) {
            super(fileItem);
        }

        public void run(FileItem fileItem) {
            this.setFileItem(fileItem);
            if (DbConstant.admin.getSession() != null && DbConstant.admin.getSession().isDisActive()) {
                DbConstant.admin.getSession().checkConnectionNoException();
            }
            boolean finalStatus = false;
            int ko = 0;
            long specialId = remoteHosts.size() > 1 ? DbConstant.ILLEGALVALUE : fileItem.specialId;
            try {
                for (String host : remoteHosts) {
                    host = host.trim();
                    if (host != null && !host.isEmpty()) {
                        String filename = fileItem.file.getAbsolutePath();
                        logger.info("Launch transfer to " + host + " with file " + filename);
                        R66Future future = new R66Future(true);
                        String text = null;
                        if (submit) {
                            text = "Submit Transfer: ";
                            SubmitTransfer transaction = new SubmitTransfer(future,
                                    host, filename, rulename, fileinfo, isMD5, blocksize,
                                    specialId, null);
                            transaction.normalInfoAsWarn = normalInfoAsWarn;
                            logger.info(text + host);
                            transaction.run();
                        } else {
                            if (specialId != DbConstant.ILLEGALVALUE) {
                                boolean direct = false;
                                // Transfer try at least once
                                text = "Request Transfer try Restart: " + specialId + " " + filename + " ";
                                try {
                                    String srequester = Configuration.configuration.getHostId(DbConstant.admin.getSession(),
                                            host);
                                    // Try restart
                                    RequestTransfer transaction = new RequestTransfer(future, specialId, host,
                                            srequester,
                                            false, false, true, networkTransaction);
                                    transaction.normalInfoAsWarn = normalInfoAsWarn;
                                    logger.info(text + host);
                                    // special task
                                    transaction.run();
                                    future.awaitUninterruptibly();
                                    if (!future.isSuccess()) {
                                        direct = true;
                                        text = "Request Transfer Cancelled and Restart: " + specialId + " " + filename
                                                + " ";
                                        future = new R66Future(true);
                                        // Cancel
                                        RequestTransfer transaction2 = new RequestTransfer(future, specialId, host,
                                                srequester,
                                                true, false, false, networkTransaction);
                                        transaction.normalInfoAsWarn = normalInfoAsWarn;
                                        logger.warn(text + host);
                                        transaction2.run();
                                        // special task
                                        future.awaitUninterruptibly();
                                        if (!DbConstant.admin.isActive()) {
                                            DbTaskRunner.removeNoDbSpecialId(specialId);
                                        }
                                    }
                                } catch (WaarpDatabaseException e) {
                                    direct = true;
                                    if (DbConstant.admin.getSession() != null) {
                                        DbConstant.admin.getSession().checkConnectionNoException();
                                    }
                                    logger.warn(Messages.getString("RequestTransfer.5") + host, e); //$NON-NLS-1$
                                }
                                if (direct) {
                                    text = "Direct Transfer: ";
                                    future = new R66Future(true);
                                    DirectTransfer transaction = new DirectTransfer(future,
                                            host, filename, rulename, fileinfo, isMD5, blocksize,
                                            DbConstant.ILLEGALVALUE, networkTransaction);
                                    transaction.normalInfoAsWarn = normalInfoAsWarn;
                                    logger.info(text + host);
                                    transaction.run();
                                }
                            } else {
                                text = "Direct Transfer: ";
                                DirectTransfer transaction = new DirectTransfer(future,
                                        host, filename, rulename, fileinfo, isMD5, blocksize,
                                        DbConstant.ILLEGALVALUE, networkTransaction);
                                transaction.normalInfoAsWarn = normalInfoAsWarn;
                                logger.info(text + host);
                                transaction.run();
                            }
                        }
                        future.awaitUninterruptibly();
                        R66Result r66result = future.getResult();
                        if (future.isSuccess()) {
                            finalStatus = true;
                            setSent(getSent() + 1);
                            DbTaskRunner runner = null;
                            if (r66result != null) {
                                runner = r66result.getRunner();
                                if (runner != null) {
                                    specialId = runner.getSpecialId();
                                    String status = Messages.getString("RequestInformation.Success"); //$NON-NLS-1$
                                    if (runner.getErrorInfo() == ErrorCode.Warning) {
                                        status = Messages.getString("RequestInformation.Warned"); //$NON-NLS-1$
                                    }
                                    if (normalInfoAsWarn) {
                                        logger.warn(text + " status: " + status + "     "
                                                + runner.toShortString()
                                                + "     <REMOTE>" + host + "</REMOTE>"
                                                + "     <FILEFINAL>" +
                                                (r66result.getFile() != null ?
                                                        r66result.getFile().toString() + "</FILEFINAL>"
                                                        : "no file"));
                                    } else {
                                        logger.info(text + " status: " + status + "     "
                                                + runner.toShortString()
                                                + "     <REMOTE>" + host + "</REMOTE>"
                                                + "     <FILEFINAL>" +
                                                (r66result.getFile() != null ?
                                                        r66result.getFile().toString() + "</FILEFINAL>"
                                                        : "no file"));
                                    }
                                    if (nolog && !submit) {
                                        // In case of success, delete the runner
                                        try {
                                            runner.delete();
                                        } catch (WaarpDatabaseException e) {
                                            logger.warn("Cannot apply nolog to     " +
                                                    runner.toShortString(),
                                                    e);
                                        }
                                    }
                                    DbTaskRunner.removeNoDbSpecialId(specialId);
                                } else {
                                    if (normalInfoAsWarn) {
                                        logger.warn(text + Messages.getString("RequestInformation.Success") //$NON-NLS-1$
                                                + "<REMOTE>" + host + "</REMOTE>");
                                    } else {
                                        logger.info(text + Messages.getString("RequestInformation.Success") //$NON-NLS-1$
                                                + "<REMOTE>" + host + "</REMOTE>");
                                    }
                                }
                            } else {
                                if (normalInfoAsWarn) {
                                    logger.warn(text + Messages.getString("RequestInformation.Success") //$NON-NLS-1$
                                            + "<REMOTE>" + host + "</REMOTE>");
                                } else {
                                    logger.info(text + Messages.getString("RequestInformation.Success") //$NON-NLS-1$
                                            + "<REMOTE>" + host + "</REMOTE>");
                                }
                            }
                        } else {
                            setError(getError() + 1);
                            ko++;
                            DbTaskRunner runner = null;
                            if (r66result != null) {
                                String errMsg = "Unknown Error Message";
                                if (future.getCause() != null) {
                                    errMsg = future.getCause().getMessage();
                                }
                                boolean isConnectionImpossible = (r66result.getCode() == ErrorCode.ConnectionImpossible)
                                        && !normalInfoAsWarn;
                                runner = r66result.getRunner();
                                if (runner != null) {
                                    specialId = runner.getSpecialId();
                                    if (!DbConstant.admin.isActive() && remoteHosts.size() > 1) {
                                        DbTaskRunner.removeNoDbSpecialId(specialId);
                                        specialId = DbConstant.ILLEGALVALUE;
                                    } else if (DbConstant.admin.isActive()) {
                                        DbTaskRunner.removeNoDbSpecialId(specialId);
                                    }
                                    if (isConnectionImpossible) {
                                        logger.info(text + Messages.getString("RequestInformation.Failure") + //$NON-NLS-1$
                                                runner.toShortString() +
                                                "<REMOTE>" + host + "</REMOTE><REASON>" + errMsg + "</REASON>");
                                    } else {
                                        logger.error(text + Messages.getString("RequestInformation.Failure") + //$NON-NLS-1$
                                                runner.toShortString() +
                                                "<REMOTE>" + host + "</REMOTE><REASON>" + errMsg + "</REASON>");
                                    }
                                } else {
                                    if (isConnectionImpossible) {
                                        logger.info(text + Messages.getString("RequestInformation.Failure") + //$NON-NLS-1$
                                                "<REMOTE>" + host + "</REMOTE>",
                                                future.getCause());
                                    } else {
                                        logger.error(text + Messages.getString("RequestInformation.Failure") + //$NON-NLS-1$
                                                "<REMOTE>" + host + "</REMOTE>",
                                                future.getCause());
                                    }
                                }
                            } else {
                                logger.error(text + Messages.getString("RequestInformation.Failure") //$NON-NLS-1$
                                        + "<REMOTE>" + host + "</REMOTE>",
                                        future.getCause());
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                // catch any exception
                logger.error("Error in SpooledDirectory", e);
                finalStatus = false;
            }
            specialId = remoteHosts.size() > 1 ? DbConstant.ILLEGALVALUE : specialId;
            if (ko > 0) {
                // If at least one is in error, the transfer is in error so should be redone
                finalStatus = false;
            }
            finalize(finalStatus, specialId);
        }
    }

    /**
     * Default arguments
     * 
     * @author "Frederic Bregier"
     *
     */
    protected static class Arguments {
        protected String sname = null;
        protected List<String> rhosts = new ArrayList<String>();
        protected List<String> localDirectory = new ArrayList<String>();
        protected String rule = null;
        protected String fileInfo = NO_INFO_ARGS;
        protected boolean ismd5 = false;
        protected int block = 0x10000; // 64K as default
        protected String statusfile = null;
        protected String stopfile = null;
        protected String regex = null;
        protected long elapsed = 1000;
        protected long elapsedWaarp = 5000;
        protected boolean tosubmit = true;
        protected boolean noLog = false;
        protected boolean recursive = false;
        protected List<String> waarphosts = new ArrayList<String>();
        protected boolean isparallel = true;
        protected int limitParallel = 0;
        protected long minimalSize = 0;
        protected boolean logWarn = true;
    }

    protected static final List<Arguments> arguments = new ArrayList<Arguments>();
    private static final String XML_ROOT = "/config/";
    private static final String XML_SPOOLEDDAEMON = "spooleddaemon";
    private static final String XML_stopfile = "stopfile";
    private static final String XML_spooled = "spooled";
    private static final String XML_name = "name";
    private static final String XML_to = "to";
    private static final String XML_rule = "rule";
    private static final String XML_statusfile = "statusfile";
    private static final String XML_directory = "directory";
    private static final String XML_regex = "regex";
    private static final String XML_recursive = "recursive";
    private static final String XML_elapse = "elapse";
    private static final String XML_submit = "submit";
    private static final String XML_parallel = "parallel";
    private static final String XML_limitParallel = "limitParallel";
    private static final String XML_info = "info";
    private static final String XML_md5 = "md5";
    private static final String XML_block = "block";
    private static final String XML_nolog = "nolog";
    private static final String XML_waarp = "waarp";
    private static final String XML_elapseWaarp = "elapseWaarp";
    private static final String XML_minimalSize = "minimalSize";
    private static final String XML_logWarn = "logWarn";

    private static final XmlDecl[] subSpooled = {
            new XmlDecl(XmlType.STRING, XML_name),
            new XmlDecl(XML_to, XmlType.STRING, XML_to, true),
            new XmlDecl(XmlType.STRING, XML_rule),
            new XmlDecl(XmlType.STRING, XML_statusfile),
            new XmlDecl(XML_directory, XmlType.STRING, XML_directory, true),
            new XmlDecl(XmlType.STRING, XML_regex),
            new XmlDecl(XmlType.BOOLEAN, XML_recursive),
            new XmlDecl(XmlType.LONG, XML_elapse),
            new XmlDecl(XmlType.BOOLEAN, XML_submit),
            new XmlDecl(XmlType.BOOLEAN, XML_parallel),
            new XmlDecl(XmlType.INTEGER, XML_limitParallel),
            new XmlDecl(XmlType.STRING, XML_info),
            new XmlDecl(XmlType.BOOLEAN, XML_md5),
            new XmlDecl(XmlType.INTEGER, XML_block),
            new XmlDecl(XmlType.BOOLEAN, XML_nolog),
            new XmlDecl(XML_waarp, XmlType.STRING, XML_waarp, true),
            new XmlDecl(XmlType.LONG, XML_elapseWaarp),
            new XmlDecl(XmlType.LONG, XML_minimalSize)
    };
    private static final XmlDecl[] spooled = {
            new XmlDecl(XmlType.STRING, XML_stopfile),
            new XmlDecl(XmlType.BOOLEAN, XML_logWarn),
            new XmlDecl(XML_spooled, XmlType.XVAL, XML_spooled, subSpooled, true)
    };
    private static final XmlDecl[] configSpooled = {
            new XmlDecl(XML_SPOOLEDDAEMON, XmlType.XVAL, XML_ROOT + XML_SPOOLEDDAEMON,
                    spooled, false)
    };

    @SuppressWarnings("unchecked")
    protected static boolean getParamsFromConfigFile(String filename) {
        Document document = null;
        // Open config file
        try {
            document = new SAXReader().read(filename);
        } catch (DocumentException e) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename, e); //$NON-NLS-1$
            return false;
        }
        if (document == null) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename); //$NON-NLS-1$
            return false;
        }
        XmlValue[] configuration = XmlUtil.read(document, configSpooled);
        XmlHash hashConfig = new XmlHash(configuration);
        XmlValue value = hashConfig.get(XML_stopfile);
        String stopfile = null;
        if (value == null || (value.isEmpty())) {
            return false;
        }
        stopfile = value.getString();
        value = hashConfig.get(XML_logWarn);
        boolean logWarn = true;
        if (value != null && (!value.isEmpty())) {
            logWarn = value.getBoolean();
        }
        value = hashConfig.get(XML_spooled);
        if (value != null && (value.getList() != null)) {
            for (XmlValue[] xml : (List<XmlValue[]>) value.getList()) {
                Arguments arg = new Arguments();
                arg.stopfile = stopfile;
                arg.logWarn = logWarn;
                XmlHash subHash = new XmlHash(xml);
                value = subHash.get(XML_name);
                if (value != null && (!value.isEmpty())) {
                    arg.sname = value.getString();
                }
                value = subHash.get(XML_to);
                if (value != null && (value.getList() != null)) {
                    for (String to : (List<String>) value.getList()) {
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
                value = subHash.get(XML_rule);
                if (value != null && (!value.isEmpty())) {
                    arg.rule = value.getString();
                } else {
                    logger.warn("rule directive is empty but must not");
                    continue;
                }
                value = subHash.get(XML_statusfile);
                if (value != null && (!value.isEmpty())) {
                    arg.statusfile = value.getString();
                } else {
                    logger.warn("statusfile directive is empty but must not");
                    continue;
                }
                value = subHash.get(XML_directory);
                if (value != null && (value.getList() != null)) {
                    for (String dir : (List<String>) value.getList()) {
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
                value = subHash.get(XML_regex);
                if (value != null && (!value.isEmpty())) {
                    arg.regex = value.getString();
                }
                value = subHash.get(XML_recursive);
                if (value != null && (!value.isEmpty())) {
                    arg.recursive = value.getBoolean();
                }
                value = subHash.get(XML_elapse);
                if (value != null && (!value.isEmpty())) {
                    arg.elapsed = value.getLong();
                }
                value = subHash.get(XML_submit);
                if (value != null && (!value.isEmpty())) {
                    arg.tosubmit = value.getBoolean();
                }
                value = subHash.get(XML_parallel);
                if (value != null && (!value.isEmpty())) {
                    arg.isparallel = value.getBoolean();
                }
                value = subHash.get(XML_limitParallel);
                if (value != null && (!value.isEmpty())) {
                    arg.limitParallel = value.getInteger();
                }
                value = subHash.get(XML_info);
                if (value != null && (!value.isEmpty())) {
                    arg.fileInfo = value.getString();
                }
                value = subHash.get(XML_md5);
                if (value != null && (!value.isEmpty())) {
                    arg.ismd5 = value.getBoolean();
                }
                value = subHash.get(XML_block);
                if (value != null && (!value.isEmpty())) {
                    arg.block = value.getInteger();
                }
                value = subHash.get(XML_nolog);
                if (value != null && (!value.isEmpty())) {
                    arg.noLog = value.getBoolean();
                }
                value = subHash.get(XML_waarp);
                if (value != null && (value.getList() != null)) {
                    for (String host : (List<String>) value.getList()) {
                        if (host.trim().isEmpty()) {
                            continue;
                        }
                        arg.waarphosts.add(host.trim());
                    }
                }
                value = subHash.get(XML_elapseWaarp);
                if (value != null && (!value.isEmpty())) {
                    arg.elapsedWaarp = value.getLong();
                }
                value = subHash.get(XML_minimalSize);
                if (value != null && (!value.isEmpty())) {
                    arg.minimalSize = value.getLong();
                }
                arguments.add(arg);
            }
        }
        document = null;
        hashConfig.clear();
        hashConfig = null;
        configuration = null;
        return !arguments.isEmpty();
    }

    /**
     * Parse the parameter and set current values
     * 
     * @param args
     * @return True if all parameters were found and correct
     */
    protected static boolean getParams(String[] args) {
        _INFO_ARGS = Messages.getString("SpooledDirectoryTransfer.0"); //$NON-NLS-1$
        if (args.length < 1) {
            logger.error(_INFO_ARGS);
            return false;
        }
        if (!FileBasedConfiguration
                .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
            logger
                    .error(Messages.getString("Configuration.NeedCorrectConfig")); //$NON-NLS-1$
            return false;
        }
        // Now check if the configuration file contains already elements of specifications
        if (!getParamsFromConfigFile(args[0])) {
            if (args.length < 11) {
                logger
                        .error(_INFO_ARGS);
                return false;
            }
            // Now set default values from configuration
            Arguments arg = new Arguments();
            arg.block = Configuration.configuration.getBLOCKSIZE();
            int i = 1;
            try {
                for (i = 1; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("-to")) {
                        i++;
                        String[] rhosts = args[i].split(",");
                        for (String string : rhosts) {
                            string = string.trim();
                            if (string.isEmpty()) {
                                continue;
                            }
                            if (Configuration.configuration.getAliases().containsKey(string)) {
                                string = Configuration.configuration.getAliases().get(string);
                            }
                            arg.rhosts.add(string);
                        }
                    } else if (args[i].equalsIgnoreCase("-name")) {
                        i++;
                        arg.sname = args[i];
                    } else if (args[i].equalsIgnoreCase("-directory")) {
                        i++;
                        String[] dir = args[i].split(",");
                        for (String string : dir) {
                            if (string.trim().isEmpty()) {
                                continue;
                            }
                            arg.localDirectory.add(string.trim());
                        }
                    } else if (args[i].equalsIgnoreCase("-rule")) {
                        i++;
                        arg.rule = args[i];
                    } else if (args[i].equalsIgnoreCase("-statusfile")) {
                        i++;
                        arg.statusfile = args[i];
                    } else if (args[i].equalsIgnoreCase("-stopfile")) {
                        i++;
                        arg.stopfile = args[i];
                    } else if (args[i].equalsIgnoreCase("-info")) {
                        i++;
                        arg.fileInfo = args[i];
                    } else if (args[i].equalsIgnoreCase("-md5")) {
                        arg.ismd5 = true;
                    } else if (args[i].equalsIgnoreCase("-block")) {
                        i++;
                        arg.block = Integer.parseInt(args[i]);
                        if (arg.block < 100) {
                            logger.error(Messages.getString("AbstractTransfer.1") + arg.block); //$NON-NLS-1$
                            return false;
                        }
                    } else if (args[i].equalsIgnoreCase("-nolog")) {
                        arg.noLog = true;
                    } else if (args[i].equalsIgnoreCase("-submit")) {
                        arg.tosubmit = true;
                    } else if (args[i].equalsIgnoreCase("-direct")) {
                        arg.tosubmit = false;
                    } else if (args[i].equalsIgnoreCase("-recursive")) {
                        arg.recursive = true;
                    } else if (args[i].equalsIgnoreCase("-logWarn")) {
                        arg.logWarn = true;
                    } else if (args[i].equalsIgnoreCase("-notlogWarn")) {
                        arg.logWarn = false;
                    } else if (args[i].equalsIgnoreCase("-regex")) {
                        i++;
                        arg.regex = args[i];
                    } else if (args[i].equalsIgnoreCase("-waarp")) {
                        i++;
                        String[] host = args[i].split(",");
                        for (String string : host) {
                            if (string.trim().isEmpty()) {
                                continue;
                            }
                            arg.waarphosts.add(string.trim());
                        }
                    } else if (args[i].equalsIgnoreCase("-elapse")) {
                        i++;
                        arg.elapsed = Long.parseLong(args[i]);
                    } else if (args[i].equalsIgnoreCase("-elapseWaarp")) {
                        i++;
                        arg.elapsedWaarp = Long.parseLong(args[i]);
                    } else if (args[i].equalsIgnoreCase("-minimalSize")) {
                        i++;
                        arg.minimalSize = Long.parseLong(args[i]);
                    } else if (args[i].equalsIgnoreCase("-limitParallel")) {
                        i++;
                        arg.limitParallel = Integer.parseInt(args[i]);
                    } else if (args[i].equalsIgnoreCase("-parallel")) {
                        arg.isparallel = true;
                    } else if (args[i].equalsIgnoreCase("-sequential")) {
                        arg.isparallel = false;
                    }
                }
            } catch (NumberFormatException e) {
                logger.error(Messages.getString("AbstractTransfer.20") + i); //$NON-NLS-1$
                return false;
            }
            if (arg.fileInfo == null) {
                arg.fileInfo = NO_INFO_ARGS;
            }
            if (arg.sname == null) {
                arg.sname = Configuration.configuration.getHOST_ID() + " : " + arg.localDirectory;
            }
            if (arg.tosubmit && !DbConstant.admin.isActive()) {
                logger.error(Messages.getString("SpooledDirectoryTransfer.2")); //$NON-NLS-1$
                return false;
            }
            if (!arg.rhosts.isEmpty() && arg.rule != null &&
                    !arg.localDirectory.isEmpty() &&
                    arg.statusfile != null && arg.stopfile != null) {
                arguments.add(arg);
                return true;
            }
            logger.error(Messages.getString("SpooledDirectoryTransfer.56") + //$NON-NLS-1$
                    _INFO_ARGS);
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

    public static final List<SpooledDirectoryTransfer> list = new ArrayList<SpooledDirectoryTransfer>();
    public static NetworkTransaction networkTransactionStatic = null;
    public static ExecutorService executorService = null;

    /**
     * @param args
     * @param normalStart
     *            if True, will exit JVM when all daemons are stopped; else False let the caller do (used by SpooledEngine)
     */
    public static boolean initialize(String[] args, boolean normalStart) {
        if (logger == null) {
            logger = WaarpLoggerFactory.getLogger(SpooledDirectoryTransfer.class);
        }
        arguments.clear();
        if (!getParams(args)) {
            logger.error(Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
            if (DbConstant.admin != null && DbConstant.admin.isActive()) {
                DbConstant.admin.close();
            }
            if (normalStart) {
                ChannelUtils.stopLogger();
                System.exit(2);
            }
            return false;
        }

        Configuration.configuration.pipelineInit();
        networkTransactionStatic = new NetworkTransaction();
        try {
            executorService = Executors.newCachedThreadPool(new WaarpThreadFactory("SpooledDirectoryDaemon"));
            for (Arguments arg : arguments) {
                R66Future future = new R66Future(true);
                SpooledDirectoryTransfer spooled =
                        new SpooledDirectoryTransfer(future, arg.sname, arg.localDirectory, arg.statusfile,
                                arg.stopfile,
                                arg.rule, arg.fileInfo, arg.ismd5, arg.rhosts, arg.block, arg.regex, arg.elapsed,
                                arg.tosubmit, arg.noLog, arg.recursive,
                                arg.elapsedWaarp, arg.isparallel, arg.limitParallel, arg.waarphosts, arg.minimalSize,
                                arg.logWarn,
                                networkTransactionStatic);
                executorService.submit(spooled);
                list.add(spooled);
            }
            arguments.clear();
            Thread.sleep(1000);
            executorService.shutdown();
            Configuration.configuration.launchStatistics();
            if (normalStart) {
                while (!executorService.awaitTermination(Configuration.configuration.getTIMEOUTCON(), TimeUnit.MILLISECONDS)) {
                    Thread.sleep(Configuration.configuration.getTIMEOUTCON());
                }
                for (SpooledDirectoryTransfer spooledDirectoryTransfer : list) {
                    logger.warn(Messages.getString("SpooledDirectoryTransfer.58") + spooledDirectoryTransfer.name
                            + ": " + spooledDirectoryTransfer.getSent()
                            + " success, " + spooledDirectoryTransfer.getError()
                            + Messages.getString("SpooledDirectoryTransfer.60")); //$NON-NLS-1$
                }
                list.clear();
            }
            return true;
        } catch (Throwable e) {
            logger.error("Exception", e);
            return false;
        } finally {
            if (normalStart) {
                R66ShutdownHook.shutdownWillStart();
                networkTransactionStatic.closeAll();
                System.exit(0);
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
