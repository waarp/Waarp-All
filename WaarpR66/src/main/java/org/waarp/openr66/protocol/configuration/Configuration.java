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
package org.waarp.openr66.protocol.configuration;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.traffic.AbstractTrafficShapingHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.waarp.common.crypto.Des;
import org.waarp.common.crypto.ssl.WaarpSecureKeyStore;
import org.waarp.common.crypto.ssl.WaarpSslContextFactory;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.role.RoleDefault;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.common.utility.WaarpShutdownHook.ShutdownConfiguration;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.common.utility.WaarpThreadFactory;
import org.waarp.gateway.kernel.rest.HttpRestHandler;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.openr66.client.NoOpRecvThroughHandler;
import org.waarp.openr66.commander.ClientRunner;
import org.waarp.openr66.commander.Commander;
import org.waarp.openr66.commander.InternalRunner;
import org.waarp.openr66.commander.ThreadPoolRunnerExecutor;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.R66BusinessFactoryInterface;
import org.waarp.openr66.context.R66DefaultBusinessFactory;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.task.localexec.LocalExecClient;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.exception.ServerException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoSslException;
import org.waarp.openr66.protocol.http.HttpInitializer;
import org.waarp.openr66.protocol.http.adminssl.HttpReponsiveSslInitializer;
import org.waarp.openr66.protocol.http.adminssl.HttpSslHandler;
import org.waarp.openr66.protocol.http.adminssl.HttpSslInitializer;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;
import org.waarp.openr66.protocol.http.restv2.RestServiceInitializer;
import org.waarp.openr66.protocol.localhandler.LocalTransaction;
import org.waarp.openr66.protocol.localhandler.Monitoring;
import org.waarp.openr66.protocol.localhandler.RetrieveRunner;
import org.waarp.openr66.protocol.monitoring.ElasticsearchMonitoringExporterClientBuilder;
import org.waarp.openr66.protocol.monitoring.MonitorExporterTransfers;
import org.waarp.openr66.protocol.networkhandler.NetworkServerInitializer;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.networkhandler.R66ConstraintLimitHandler;
import org.waarp.openr66.protocol.networkhandler.ssl.NetworkSslServerInitializer;
import org.waarp.openr66.protocol.snmp.R66PrivateMib;
import org.waarp.openr66.protocol.snmp.R66VariableFactory;
import org.waarp.openr66.protocol.utils.R66ShutdownHook;
import org.waarp.openr66.protocol.utils.Version;
import org.waarp.openr66.thrift.R66ThriftServerService;
import org.waarp.snmp.WaarpMOFactory;
import org.waarp.snmp.WaarpSnmpAgent;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Configuration class
 */
public class Configuration {
  private static final String ISSUE_WHILE_DEBUGGING = "Issue while debugging";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(Configuration.class);

  // Static values
  /**
   * General Configuration object
   */
  public static Configuration configuration = new Configuration();

  public static final String SnmpName = "Waarp OpenR66 SNMP";
  public static final int SnmpPrivateId = 66666;
  public static final int SnmpR66Id = 66;
  public static final String SnmpDefaultAuthor = "Frederic Bregier";
  public static final String SnmpVersion = "Waarp OpenR66 " + Version.ID;
  public static final String SnmpDefaultLocalization = "Paris, France";
  public static final int SnmpService = 72;

  /**
   * Time elapse for retry in ms
   */
  public static final long RETRYINMS = 10;

  /**
   * Number of retry before error
   */
  public static final int RETRYNB = 3;

  /**
   * Hack to say Windows or Unix (USR1 not OK on Windows)
   */
  private static boolean isUnix;

  /**
   * Default size for buffers (NIO)
   */
  public static final int BUFFERSIZEDEFAULT = 0x10000; // 64K

  /**
   * Time elapse for WRITE OR CLOSE WAIT elaps in ms
   */
  public static final long WAITFORNETOP = 200;

  /**
   * Extension of file during transfer
   */
  public static final String EXT_R66 = ".r66";

  /**
   * Rank to redo when a restart occurs
   */
  private static int rankRestart = 30;
  /**
   * Number of DbSession for internal needs
   */
  private static int nbDbSession;
  /**
   * FileParameter
   */
  private static final FilesystemBasedFileParameterImpl fileParameter =
      new FilesystemBasedFileParameterImpl();

  private R66BusinessFactoryInterface r66BusinessFactory =
      new R66DefaultBusinessFactory();
  // Global Dynamic values
  /**
   * Version validation
   */
  private boolean extendedProtocol = true;
  /**
   * Global digest
   */
  private boolean globalDigest = true;
  /**
   * Local digest
   */
  private boolean localDigest = true;
  /**
   * White List of allowed Partners to use Business Requests
   */
  private final HashSet<String> businessWhiteSet = new HashSet<String>();
  /**
   * Roles list for identified partners
   */
  private final HashMap<String, RoleDefault> roles =
      new HashMap<String, RoleDefault>();
  /**
   * Aliases list for identified partners
   */
  private final HashMap<String, String> aliases = new HashMap<String, String>();
  /**
   * reverse Aliases list for identified partners
   */
  private final HashMap<String, String[]> reverseAliases =
      new HashMap<String, String[]>();
  /**
   * Versions for each HostID
   */
  private final ConcurrentHashMap<String, PartnerConfiguration> versions =
      new ConcurrentHashMap<String, PartnerConfiguration>();
  /**
   * Actual Host ID
   */
  private String hostId;
  /**
   * Actual SSL Host ID
   */
  private String hostSslId;

  /**
   * Server Administration user name
   */
  private String adminName;
  /**
   * Server Administration Key
   */
  private byte[] serverAdminKey;
  /**
   * Server Administration Key file
   */
  private String serverKeyFile;
  /**
   * Server Actual Authentication
   */
  private DbHostAuth hostAuth;
  /**
   * Server Actual SSL Authentication
   */
  private DbHostAuth hostSslAuth;

  private String authFile;

  /**
   * Default number of threads in pool for Server (true network listeners).
   * Server will change this value on
   * startup if not set. The value should be closed to the number of CPU.
   */
  private int serverThread;

  /**
   * Default number of threads in pool for Client. The value is for true
   * client
   * for Executor in the Pipeline for
   * Business logic. The value does not indicate a limit of concurrent
   * clients,
   * but a limit on truly packet
   * concurrent actions.
   */
  private int clientThread = 10;

  /**
   * Default session limit 1 Gbit, so up to 100 full simultaneous clients
   */
  private static final long DEFAULT_SESSION_LIMIT = 1073741824L;

  /**
   * Default global limit 100 Gbit
   */
  private static final long DEFAULT_GLOBAL_LIMIT = 107374182400L;

  /**
   * Default server port
   */
  private int serverPort = 6666;

  /**
   * Default SSL server port
   */
  private int serverSslPort = 6667;

  /**
   * Default HTTP server port
   */
  private int serverHttpport = 8066;

  /**
   * Default HTTP server port
   */
  private int serverHttpsPort = 8067;

  /**
   * Default server IPs
   */
  private String[] serverAddresses = null;

  /**
   * Default SSL server IPs
   */
  private String[] serverSslAddresses = null;

  /**
   * Default HTTP server IPs
   */
  private String[] serverHttpAddresses = null;

  /**
   * Default HTTP server IPs
   */
  private String[] serverHttpsAddresses = null;

  /**
   * Nb of milliseconds after connection is in timeout
   */
  private long timeoutCon = 30000;

  /**
   * Size by default of block size for receive/sending files. Should be a
   * multiple of 8192 (maximum = 2^30K due
   * to block limitation to 4 bytes)
   */
  private int blockSize = 0x10000; // 64K

  /**
   * Max global memory limit: default is 1GB
   * (used in Web and REST API)
   */
  private int maxGlobalMemory = 1073741824;

  /**
   * Rest configuration list
   */
  private final List<RestConfiguration> restConfigurations =
      new ArrayList<RestConfiguration>();

  /**
   * Base Directory
   */
  private String baseDirectory;

  /**
   * In path (receive)
   */
  private String inPath;

  /**
   * Out path (send, copy, pending)
   */
  private String outPath;

  /**
   * Archive path
   */
  private String archivePath;

  /**
   * Working path
   */
  private String workingPath;

  /**
   * Config path
   */
  private String configPath;

  /**
   * Http Admin base
   */
  private String httpBasePath = "src/main/admin/";

  /**
   * Model for Http Admin: 0 = standard (i18n only), 1 = responsive (i18n +
   * bootstrap + dynamic table + refresh)
   */
  private int httpModel = 1;

  /**
   * True if the service is going to shutdown
   */
  private boolean isShutdown;

  /**
   * Limit in Write byte/s to apply globally to the FTP Server
   */
  private long serverGlobalWriteLimit = getDEFAULT_GLOBAL_LIMIT();

  /**
   * Limit in Read byte/s to apply globally to the FTP Server
   */
  private long serverGlobalReadLimit = getDEFAULT_GLOBAL_LIMIT();

  /**
   * Limit in Write byte/s to apply by session to the FTP Server
   */
  private long serverChannelWriteLimit = getDEFAULT_SESSION_LIMIT();

  /**
   * Limit in Read byte/s to apply by session to the FTP Server
   */
  private long serverChannelReadLimit = getDEFAULT_SESSION_LIMIT();

  /**
   * Delay in ms between two checks
   */
  private long delayLimit =
      AbstractTrafficShapingHandler.DEFAULT_CHECK_INTERVAL;

  /**
   * Does this OpenR66 server will use and accept SSL connections
   */
  private boolean useSSL;
  /**
   * Does this OpenR66 server will use and accept non SSL connections
   */
  private boolean useNOSSL = true;
  /**
   * Algorithm to use for Digest: fastest, replacement could be SHA512
   */
  private FilesystemBasedDigest.DigestAlgo digest = DigestAlgo.MD5;

  /**
   * Does this OpenR66 server will try to compress HTTP connections
   */
  private boolean useHttpCompression;

  /**
   * Does this OpenR66 server will use Waarp LocalExec Daemon for ExecTask and
   * ExecMoveTask
   */
  private boolean useLocalExec;

  /**
   * Crypto Key
   */
  private Des cryptoKey;
  /**
   * Associated file for CryptoKey
   */
  private String cryptoFile;

  /**
   * List of all Server Channels to enable the close call on them using Netty
   * ChannelGroup
   */
  protected ChannelGroup serverChannelGroup;

  /**
   * List of all Server Channels connected to remote to enable the close call
   * on them using Netty ChannelGroup
   */
  protected ChannelGroup serverConnectedChannelGroup;
  /**
   * Main bind address in no ssl mode
   */
  protected Channel bindNoSSL;
  /**
   * Main bind address in ssl mode
   */
  protected Channel bindSSL;

  /**
   * Does the current program running as Server
   */
  private boolean isServer;

  /**
   * ExecutorService Other Worker
   */
  protected final ExecutorService execOtherWorker =
      Executors.newCachedThreadPool(new WaarpThreadFactory("OtherWorker"));

  protected EventLoopGroup serverGroup;
  protected EventLoopGroup workerGroup;
  protected EventLoopGroup handlerGroup;
  protected EventLoopGroup subTaskGroup;
  protected EventLoopGroup httpWorkerGroup;
  protected ThreadPoolRunnerExecutor retrieveRunnerGroup;

  /**
   * ExecutorService Scheduled tasks
   */
  protected final ScheduledExecutorService scheduledExecutorService;

  /**
   * Bootstrap for server
   */
  protected ServerBootstrap serverBootstrap;

  /**
   * Bootstrap for SSL server
   */
  protected ServerBootstrap serverSslBootstrap;
  /**
   * Factory for NON SSL Server
   */
  protected NetworkServerInitializer networkServerInitializer;
  /**
   * Factory for SSL Server
   */
  protected NetworkSslServerInitializer networkSslServerInitializer;

  /**
   * Bootstrap for Http server
   */
  protected ServerBootstrap httpBootstrap;
  /**
   * Bootstrap for Https server
   */
  protected ServerBootstrap httpsBootstrap;
  /**
   * List of all Http Channels to enable the close call on them using Netty
   * ChannelGroup
   */
  protected ChannelGroup httpChannelGroup;

  /**
   * Timer for CloseOpertations
   */
  private final Timer timerCloseOperations =
      new HashedWheelTimer(new WaarpThreadFactory("TimerClose"), 50,
                           TimeUnit.MILLISECONDS, 1024);
  private final AtomicBoolean timerCloseClosed = new AtomicBoolean(false);
  /**
   * Global TrafficCounter (set from global configuration)
   */
  protected GlobalTrafficShapingHandler globalTrafficShapingHandler;

  /**
   * LocalTransaction
   */
  protected LocalTransaction localTransaction;
  /**
   * InternalRunner
   */
  private InternalRunner internalRunner;
  /**
   * Maximum number of concurrent active transfer by submission.
   */
  private int runnerThread = Commander.LIMIT_SUBMIT;
  /**
   * Delay in ms between two steps of Commander
   */
  private long delayCommander = 5000;
  /**
   * Delay in ms between two retries
   */
  private long delayRetry = 30000;
  /**
   * Constraint Limit Handler on CPU usage and Connection limitation
   */
  private R66ConstraintLimitHandler constraintLimitHandler =
      new R66ConstraintLimitHandler();
  /**
   * Do we check Remote Address from DbHost
   */
  private boolean checkRemoteAddress;
  /**
   * Do we check address even for Client
   */
  private boolean checkClientAddress;
  /**
   * For No Db client, do we saved TaskRunner in a XML
   */
  private boolean saveTaskRunnerWithNoDb;
  /**
   * In case of Multiple OpenR66 monitor servers behing a load balancer (HA
   * solution)
   */
  private int multipleMonitors = 1;
  /**
   * Monitoring object
   */
  private Monitoring monitoring;
  /**
   * Monitoring: how long in ms to get back in monitoring
   */
  private long pastLimit = 86400000; // 24H
  /**
   * Monitoring: minimal interval in ms before redo real monitoring
   */
  private long minimalDelay = 5000; // 5 seconds
  /**
   * Monitoring: snmp configuration file (empty means no snmp support)
   */
  private String snmpConfig;
  /**
   * SNMP Agent (if any)
   */
  private WaarpSnmpAgent agentSnmp;
  /**
   * Associated MIB
   */
  private R66PrivateMib r66Mib;

  protected boolean configured;

  private static WaarpSecureKeyStore waarpSecureKeyStore;

  private static WaarpSslContextFactory waarpSslContextFactory;
  /**
   * Thrift support
   */
  private R66ThriftServerService thriftService;
  private int thriftport = -1;

  private boolean isExecuteErrorBeforeTransferAllowed = true;

  private final ShutdownConfiguration shutdownConfiguration =
      new ShutdownConfiguration();

  private boolean isHostProxyfied;

  private boolean authentNoReuse;

  private boolean warnOnStartup = true;

  private boolean chrootChecked = true;

  private boolean blacklistBadAuthent;

  private int maxfilenamelength = 255;

  private MonitorExporterTransfers monitorExporterTransfers = null;

  private boolean isMonitorExporterApiRest;
  private String monitorExporterUrl = null;
  private String monitorExporterEndPoint = null;
  private int monitorExporterDelay = 1000;
  private boolean monitorExporterKeepConnection = false;
  private boolean monitorIntervalIncluded = true;
  private boolean monitorTransformLongAsString = false;
  private String monitorBasicAuthent = null;
  private String monitorUsername = null;
  private String monitorPwd = null;
  private String monitorToken = null;
  private String monitorApiKey = null;
  private String monitorPrefix = null;
  private String monitorIndex = null;
  private boolean monitorCompression = true;

  private boolean compressionAvailable = false;

  private int timeStat;

  private int limitCache = 5000;

  private long timeLimitCache = 180000;

  private final java.util.Timer timerCleanLruCache =
      new java.util.Timer("CleanLruCache", true);

  private final java.util.Timer timerStatistic =
      new java.util.Timer("R66Statistic", true);

  public Configuration() {
    // Init signal handler
    getShutdownConfiguration().timeout = getTimeoutCon();
    if (WaarpShutdownHook.shutdownHook == null) {
      new R66ShutdownHook(getShutdownConfiguration());
    }
    computeNbThreads();
    scheduledExecutorService = Executors.newScheduledThreadPool(4,
                                                                new WaarpThreadFactory(
                                                                    "ScheduledRestartTask"));
    // Init FiniteStates
    R66FiniteDualStates.initR66FiniteStates();
    if (!SystemPropertyUtil.isFileEncodingCorrect()) {
      logger.error(
          "Issue while trying to set UTF-8 as default file encoding: use -Dfile.encoding=UTF-8 as java command argument");
      logger.warn("Currently file.encoding is: " +
                  SystemPropertyUtil.get(SystemPropertyUtil.FILE_ENCODING));
    }
    setExecuteErrorBeforeTransferAllowed(SystemPropertyUtil.getBoolean(
        R66SystemProperties.OPENR66_EXECUTEBEFORETRANSFERRED, true));
    final boolean useSpaceSeparator = SystemPropertyUtil.getBoolean(
        R66SystemProperties.OPENR66_USESPACESEPARATOR, false);
    if (useSpaceSeparator) {
      PartnerConfiguration.setSEPARATOR_FIELD(
          PartnerConfiguration.BLANK_SEPARATOR_FIELD);
    }
    setHostProxyfied(SystemPropertyUtil.getBoolean(
        R66SystemProperties.OPENR66_ISHOSTPROXYFIED, false));
    setWarnOnStartup(SystemPropertyUtil.getBoolean(
        R66SystemProperties.OPENR66_STARTUP_WARNING, true));
    setAuthentNoReuse(SystemPropertyUtil.getBoolean(
        R66SystemProperties.OPENR66_AUTHENT_NO_REUSE, false));
    if (!SystemPropertyUtil.get(
        R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, "").isEmpty()) {
      logger.warn("{} is deprecated in system properties use {} instead",
                  R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK,
                  R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE);
      FileBasedConfiguration.autoupgrade = SystemPropertyUtil.getBoolean(
          R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, false);
    } else {
      FileBasedConfiguration.autoupgrade = SystemPropertyUtil.getBoolean(
          R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE, false);
    }

    setChrootChecked(SystemPropertyUtil.getBoolean(
        R66SystemProperties.OPENR66_CHROOT_CHECKED, true));
    setBlacklistBadAuthent(SystemPropertyUtil.getBoolean(
        R66SystemProperties.OPENR66_BLACKLIST_BADAUTHENT, !isHostProxyfied()));
    setMaxfilenamelength(SystemPropertyUtil.getInt(
        R66SystemProperties.OPENR66_FILENAME_MAXLENGTH, 255));
    setTimeStat(
        SystemPropertyUtil.getInt(R66SystemProperties.OPENR66_TRACE_STATS, 0));
    setLimitCache(
        SystemPropertyUtil.getInt(R66SystemProperties.OPENR66_CACHE_LIMIT,
                                  20000));
    if (getLimitCache() <= 100) {
      setLimitCache(100);
    }
    setTimeLimitCache(
        SystemPropertyUtil.getLong(R66SystemProperties.OPENR66_CACHE_TIMELIMIT,
                                   180000));
    if (getTimeLimitCache() < 1000) {
      setTimeLimitCache(1000);
    }
    DbTaskRunner.createLruCache(getLimitCache(), getTimeLimitCache());
    if (getLimitCache() > 0 && getTimeLimitCache() > 1000) {
      timerCleanLruCache.schedule(new CleanLruCache(), getTimeLimitCache());
    }
    if (isHostProxyfied()) {
      setBlacklistBadAuthent(false);
    }
  }

  private String arrayToString(final String[] array) {
    final StringBuilder ip;
    if (array != null && array.length > 0) {
      ip = new StringBuilder("[" + array[0]);
      for (int i = 1; i < array.length; i++) {
        ip.append(",").append(array[i]);
      }
      ip.append("]");
    } else {
      ip = new StringBuilder("[All Interfaces]");
    }
    return ip.toString();
  }

  @Override
  public final String toString() {
    StringBuilder rest = null;
    for (final RestConfiguration config : getRestConfigurations()) {
      if (rest == null) {
        rest = new StringBuilder((config.getRestAddress() != null?
            '\'' + config.getRestAddress() + ':' : "'All:") +
                                 config.getRestPort() + '\'');
      } else {
        rest.append(", ").append(config.getRestAddress() != null?
                                     '\'' + config.getRestAddress() + ':' :
                                     "'All:").append(config.getRestPort())
            .append('\'');
      }
    }
    final String serverIp = arrayToString(getServerIpsAddresses());
    final String serverSslIp = arrayToString(getServerSslAddresses());
    final String serverHttpIp = arrayToString(getServerHttpAddresses());
    final String serverHttpsIp = arrayToString(getServerHttpsAddresses());
    return "Config: { ServerIp: " + serverIp + ", ServerPort: " +
           getServerPort() + ", ServerSslIp: " + serverSslIp +
           ", ServerSslPort: " + getServerSslPort() + ", ServerViewIp: " +
           serverHttpIp + ", ServerView: " + getServerHttpport() +
           ", ServerAdminIp: " + serverHttpsIp + ", ServerAdmin: " +
           getServerHttpsPort() + ", ThriftPort: " +
           (getThriftport() > 0? getThriftport() : "'NoThriftSupport'") +
           ", RestAddress: [" +
           (rest != null? rest.toString() : "'NoRestSupport'") + ']' +
           ", TimeOut: " + getTimeoutCon() + ", BaseDir: '" +
           getBaseDirectory() + "', DigestAlgo: '" + getDigest().algoName +
           "', checkRemote: " + isCheckRemoteAddress() + ", checkClient: " +
           isCheckClientAddress() + ", snmpActive: " +
           (getAgentSnmp() != null) + ", chrootChecked: " + isChrootChecked() +
           ", blacklist: " + isBlacklistBadAuthent() + ", isHostProxified: " +
           isHostProxyfied() + ", isCompressionEnabled: " +
           isCompressionAvailable() + '}';
  }

  /**
   * Configure the pipeline for client (to be called only once)
   */
  public final void pipelineInit() {
    if (isConfigured()) {
      return;
    }
    // To verify against limit of database
    setRunnerThread(getRunnerThread());
    serverGroup = new NioEventLoopGroup(getServerThread(),
                                        new WaarpThreadFactory("Service"));
    workerGroup = new NioEventLoopGroup(getClientThread(),
                                        new WaarpThreadFactory("Worker"));
    handlerGroup = new NioEventLoopGroup(getClientThread(),
                                         new WaarpThreadFactory("Handler"));
    subTaskGroup = new NioEventLoopGroup(getClientThread(),
                                         new WaarpThreadFactory("SubTask"));
    final RejectedExecutionHandler rejectedExecutionHandler =
        new RejectedExecutionHandler() {

          @Override
          public final void rejectedExecution(final Runnable r,
                                              final ThreadPoolExecutor executor) {
            if (r instanceof RetrieveRunner) {
              final RetrieveRunner retrieveRunner = (RetrieveRunner) r;
              logger.info("Try to reschedule RetrieveRunner: {}",
                          retrieveRunner.getLocalId());
              try {
                Thread.sleep(WAITFORNETOP * 2);
              } catch (final InterruptedException e) {//NOSONAR
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                retrieveRunner.notStartRunner();
                return;
              }
              getRetrieveRunnerGroup().execute(retrieveRunner);
            } else {
              logger.warn("Not RetrieveRunner: {}", r.getClass().getName());
            }
          }
        };

    int nbRunnerThread = getRunnerThread();
    if (nbRunnerThread == 1) {
      nbRunnerThread = 2;
    }
    retrieveRunnerGroup =
        new ThreadPoolRunnerExecutor(nbRunnerThread / 2, nbRunnerThread * 3, 1,
                                     TimeUnit.SECONDS,
                                     new SynchronousQueue<Runnable>(),
                                     new WaarpThreadFactory("RetrieveRunner"),
                                     rejectedExecutionHandler);
    localTransaction = new LocalTransaction();
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(null));
    if (isWarnOnStartup()) {
      logger.warn("Server Thread: " + getServerThread() + " Client Thread: " +
                  getClientThread() + " Runner Thread: " + getRunnerThread());
    } else {
      logger.info("Server Thread: " + getServerThread() + " Client Thread: " +
                  getClientThread() + " Runner Thread: " + getRunnerThread());
    }
    logger.info("Current launched threads: " +
                ManagementFactory.getThreadMXBean().getThreadCount());
    if (isUseLocalExec()) {
      LocalExecClient.initialize();
    }
    setConfigured(true);
  }

  public final void setConfigured(final boolean configured) {
    this.configured = configured;
  }

  public final boolean isConfigured() {
    return configured;
  }

  public final void serverPipelineInit() {
    httpWorkerGroup = new NioEventLoopGroup(getServerThread() * 10,
                                            new WaarpThreadFactory(
                                                "HttpWorker"));
  }

  /**
   * Startup the server
   *
   * @throws WaarpDatabaseSqlException
   * @throws WaarpDatabaseNoConnectionException
   */
  public void serverStartup()
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             ServerException {
    setServer(true);
    if (isBlacklistBadAuthent()) {
      setBlacklistBadAuthent(!DbHostAuth.hasProxifiedHosts());
    }
    getShutdownConfiguration().timeout = getTimeoutCon();
    if (getTimeLimitCache() < getTimeoutCon() * 10) {
      setTimeLimitCache(getTimeoutCon() * 10);
      DbTaskRunner.updateLruCacheTimeout(getTimeLimitCache());
    }
    WaarpShutdownHook.addShutdownHook();
    logger.debug("Use NoSSL: {} Use SSL: {}", isUseNOSSL(), isUseSSL());
    if (!isUseNOSSL() && !isUseSSL()) {
      logger.error(Messages.getString("Configuration.NoSSL")); //$NON-NLS-1$
      WaarpSystemUtil.systemExit(-1);
      return;
    }
    if (SystemPropertyUtil.getBoolean(
        R66SystemProperties.OPENR66_JUNIT_RECV_THROUGH, false)) {
      logger.warn("DEBUG PERF MODE using RECV THROUGH");
      ClientRunner.setRecvHandlerJunit(new NoOpRecvThroughHandler());
    }
    pipelineInit();
    serverPipelineInit();
    r66Startup();
    startHttpSupport();
    startMonitoring();
    launchStatistics();
    startRestSupport();
    startMonitorExporterTransfers();
    logger.info("Current launched threads: " +
                ManagementFactory.getThreadMXBean().getThreadCount());
  }

  /**
   * Used to log statistics information regularly
   */
  public final void launchStatistics() {
    if (getTimeStat() > 0) {
      timerStatistic.scheduleAtFixedRate(new UsageStatistic(), 1000,
                                         getTimeStat() * 1000L);
    }
  }

  private Channel bindTo(final String ip, final int port,
                         final ServerBootstrap serverBootstrapToBind,
                         final String messageError) throws ServerException {
    final InetSocketAddress inetSocketAddress =
        ip == null? new InetSocketAddress(port) :
            new InetSocketAddress(ip, port);
    final ChannelFuture future =
        serverBootstrapToBind.bind(inetSocketAddress).awaitUninterruptibly();
    if (future.isSuccess()) {
      final Channel channel = future.channel();
      serverChannelGroup.add(channel);
      return channel;
    } else {
      throw new ServerException(messageError + " [" + ip + ":" + port + "]",
                                future.cause());
    }
  }

  private Channel bindTo(final String[] ips, final int port,
                         final ServerBootstrap serverBootstrapToBind,
                         final String messageError) throws ServerException {
    if (ips == null || ips.length == 0) {
      return bindTo((String) null, port, serverBootstrapToBind, messageError);
    } else {
      Channel channel = null;
      for (final String ip : ips) {
        channel = bindTo(ip, port, serverBootstrapToBind, messageError);
      }
      return channel;
    }
  }

  public void r66Startup()
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             ServerException {
    logger.info(Messages.getString("Configuration.Start") +
                arrayToString(getServerIpsAddresses()) + ':' + getServerPort() +
                ':' + isUseNOSSL() + ':' + getHostId() + //$NON-NLS-1$
                ' ' + arrayToString(getServerSslAddresses()) + ':' +
                getServerSslPort() + ':' + isUseSSL() + ':' + getHostSslId());
    // add into configuration
    getConstraintLimitHandler().setServer(true);
    // Global Server
    serverChannelGroup =
        new DefaultChannelGroup("OpenR66", subTaskGroup.next());
    serverConnectedChannelGroup =
        new DefaultChannelGroup("OpenR66Connected", subTaskGroup.next());
    if (isUseNOSSL()) {
      serverBootstrap = new ServerBootstrap();
      WaarpNettyUtil.setServerBootstrap(serverBootstrap, serverGroup,
                                        workerGroup, (int) getTimeoutCon(),
                                        getBlockSize() + 64, false);
      networkServerInitializer = new NetworkServerInitializer(true);
      serverBootstrap.childHandler(networkServerInitializer);
      final String[] serverIps = getServerIpsAddresses();
      bindNoSSL = bindTo(serverIps, getServerPort(), serverBootstrap,
                         Messages.getString("Configuration.R66NotBound"));
    } else {
      networkServerInitializer = null;
      logger.warn(
          Messages.getString("Configuration.NOSSLDeactivated")); //$NON-NLS-1$
    }

    if (isUseSSL() && getHostSslId() != null) {
      serverSslBootstrap = new ServerBootstrap();
      WaarpNettyUtil.setServerBootstrap(serverSslBootstrap, serverGroup,
                                        workerGroup, (int) getTimeoutCon(),
                                        getBlockSize() + 64, false);
      networkSslServerInitializer = new NetworkSslServerInitializer(false);
      serverSslBootstrap.childHandler(networkSslServerInitializer);
      final String[] serverIps = getServerSslAddresses();
      bindSSL = bindTo(serverIps, getServerSslPort(), serverSslBootstrap,
                       Messages.getString("Configuration.R66SSLNotBound"));
    } else {
      networkSslServerInitializer = null;
      logger.warn(
          Messages.getString("Configuration.SSLMODEDeactivated")); //$NON-NLS-1$
    }

    // Factory for TrafficShapingHandler
    setupLimitHandler();

    // Now start the InternalRunner
    internalRunner = new InternalRunner();

    if (getThriftport() > 0) {
      setThriftService(
          new R66ThriftServerService(new WaarpFuture(true), getThriftport()));
      execOtherWorker.execute(getThriftService());
      getThriftService().awaitInitialization();
    } else {
      setThriftService(null);
    }
  }

  public void startHttpSupport() throws ServerException {
    // Now start the HTTP support
    logger.info(
        Messages.getString("Configuration.HTTPStart") + getServerHttpport() +
        //$NON-NLS-1$
        " HTTPS: " + getServerHttpsPort());
    httpChannelGroup =
        new DefaultChannelGroup("HttpOpenR66", subTaskGroup.next());
    if (getServerHttpport() > 0) {
      // Configure the server.
      httpBootstrap = new ServerBootstrap();
      WaarpNettyUtil.setServerBootstrap(httpBootstrap, httpWorkerGroup,
                                        httpWorkerGroup, (int) getTimeoutCon());
      // Set up the event pipeline factory.
      httpBootstrap.childHandler(new HttpInitializer(isUseHttpCompression()));
      // Bind and start to accept incoming connections.
      final String[] serverIps = getServerHttpAddresses();
      bindTo(serverIps, getServerHttpport(), httpBootstrap,
             "Can't start HTTP service");
    }
    // Now start the HTTPS support
    // Bind and start to accept incoming connections.
    if (getServerHttpsPort() > 0) {
      // Configure the server.
      httpsBootstrap = new ServerBootstrap();
      // Set up the event pipeline factory.
      WaarpNettyUtil.setServerBootstrap(httpsBootstrap, httpWorkerGroup,
                                        httpWorkerGroup, (int) getTimeoutCon());
      if (getHttpModel() == 0) {
        httpsBootstrap.childHandler(
            new HttpSslInitializer(isUseHttpCompression()));
      } else {
        // Default
        httpsBootstrap.childHandler(
            new HttpReponsiveSslInitializer(isUseHttpCompression()));
      }
      final String[] serverIps = getServerHttpsAddresses();
      bindTo(serverIps, getServerHttpsPort(), httpsBootstrap,
             "Can't start HTTPS service");
    }
  }

  public final void startRestSupport() {
    HttpRestHandler.initialize(
        getBaseDirectory() + '/' + getWorkingPath() + "/httptemp");
    for (final RestConfiguration config : getRestConfigurations()) {
      RestServiceInitializer.initRestService(config);
      // REST V1 is included within V2
      // so no HttpRestR66Handler.initializeService(config)
      logger.info(
          Messages.getString("Configuration.HTTPStart") + " (REST Support) " +
          config);
    }
  }

  public final void startMonitoring() throws WaarpDatabaseSqlException {
    setMonitoring(new Monitoring(getPastLimit(), getMinimalDelay(), null));
    setNbDbSession(getNbDbSession() + 1);
    if (getSnmpConfig() != null) {
      final int snmpPortShow =
          isUseNOSSL()? getServerPort() : getServerSslPort();
      final R66PrivateMib r66MibTemp =
          new R66PrivateMib(SnmpName, snmpPortShow, SnmpPrivateId, SnmpR66Id,
                            SnmpDefaultAuthor, SnmpVersion,
                            SnmpDefaultLocalization, SnmpService);
      WaarpMOFactory.setFactory(new R66VariableFactory());
      setAgentSnmp(
          new WaarpSnmpAgent(new File(getSnmpConfig()), getMonitoring(),
                             r66MibTemp));
      try {
        getAgentSnmp().start();
      } catch (final IOException e) {
        throw new WaarpDatabaseSqlException(
            Messages.getString("Configuration.SNMPError"), e); //$NON-NLS-1$
      }
      setR66Mib(r66MibTemp);
    }
  }

  public final void startJunitRestSupport(final RestConfiguration config) {
    HttpRestR66Handler.initializeService(config);
  }

  public final InternalRunner getInternalRunner() {
    return internalRunner;
  }

  /**
   * Prepare the server to stop
   * <p>
   * To be called early before other stuff will be closed
   */
  public final void prepareServerStop() {
    if (getThriftService() != null) {
      getThriftService().releaseResources();
    }
    if (internalRunner != null) {
      internalRunner.prepareStopInternalRunner();
    }
  }

  /**
   * Unbind network connectors
   */
  public final void unbindServer() {
    if (bindNoSSL != null) {
      bindNoSSL.close();
      bindNoSSL = null;
    }
    if (bindSSL != null) {
      bindSSL.close();
      bindSSL = null;
    }
  }

  public final void shutdownGracefully() {
    if (workerGroup != null && !workerGroup.isShuttingDown()) {
      workerGroup.shutdownGracefully();
    }
    if (handlerGroup != null && !handlerGroup.isShuttingDown()) {
      handlerGroup.shutdownGracefully();
    }
    if (httpWorkerGroup != null && !httpWorkerGroup.isShuttingDown()) {
      httpWorkerGroup.shutdownGracefully();
    }
    if (subTaskGroup != null && !subTaskGroup.isShuttingDown()) {
      subTaskGroup.shutdownGracefully();
    }
    if (serverGroup != null && !serverGroup.isShuttingDown()) {
      serverGroup.shutdownGracefully();
    }
    if (retrieveRunnerGroup != null && !retrieveRunnerGroup.isShutdown()) {

      retrieveRunnerGroup.shutdown();
      try {
        if (!retrieveRunnerGroup.awaitTermination(getTimeoutCon() / 2,
                                                  TimeUnit.MILLISECONDS)) {
          retrieveRunnerGroup.shutdownNow();
        }
      } catch (final InterruptedException e) {//NOSONAR
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        retrieveRunnerGroup.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  public final void shutdownQuickly() {
    if (workerGroup != null && !workerGroup.isShuttingDown()) {
      workerGroup.shutdownGracefully(10, 10, TimeUnit.MILLISECONDS);
    }
    if (httpWorkerGroup != null && !httpWorkerGroup.isShuttingDown()) {
      httpWorkerGroup.shutdownGracefully(10, 10, TimeUnit.MILLISECONDS);
    }
    if (handlerGroup != null && !handlerGroup.isShuttingDown()) {
      handlerGroup.shutdownGracefully(10, 10, TimeUnit.MILLISECONDS);
    }
    if (subTaskGroup != null && !subTaskGroup.isShuttingDown()) {
      subTaskGroup.shutdownGracefully(10, 10, TimeUnit.MILLISECONDS);
    }
    if (serverGroup != null && !serverGroup.isShuttingDown()) {
      serverGroup.shutdownGracefully(10, 10, TimeUnit.MILLISECONDS);
    }
    if (retrieveRunnerGroup != null && !retrieveRunnerGroup.isShutdown()) {
      retrieveRunnerGroup.shutdownNow();
    }
  }

  /**
   * Stops the server
   * <p>
   * To be called after all other stuff are closed (channels, connections)
   */
  public void serverStop() {
    WaarpSslUtility.forceCloseAllSslChannels();
    if (internalRunner != null) {
      internalRunner.stopInternalRunner();
    }
    if (scheduledExecutorService != null) {
      scheduledExecutorService.shutdown();
    }
    timerCleanLruCache.cancel();
    timerStatistic.cancel();
    if (monitorExporterTransfers != null) {
      try {
        monitorExporterTransfers.close();
      } catch (final IOException e) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      }
      monitorExporterTransfers = null;
    }
    if (getAgentSnmp() != null) {
      getAgentSnmp().stop();
      setAgentSnmp(null);
    } else if (getMonitoring() != null) {
      getMonitoring().releaseResources();
      setMonitoring(null);
    }
    shutdownGracefully();
    if (execOtherWorker != null) {
      if (!WaarpSystemUtil.isJunit()) {
        execOtherWorker.shutdownNow();
      }
    }
    if (timerCloseOperations != null) {
      timerCloseClosed.set(true);
      timerCloseOperations.stop();
    }
  }

  /**
   * To be called after all other stuff are closed for Client
   */
  public final void clientStop() {
    clientStop(true);
  }

  /**
   * To be called after all other stuff are closed for Client
   *
   * @param shutdownQuickly For client only, shall be true to speedup
   *     the
   *     end of the process
   */
  public final void clientStop(final boolean shutdownQuickly) {
    WaarpSslUtility.forceCloseAllSslChannels();
    if (!configuration.isServer()) {
      WaarpSystemUtil.stopLogger(false);
    }
    if (scheduledExecutorService != null) {
      scheduledExecutorService.shutdown();
    }
    timerCleanLruCache.cancel();
    timerStatistic.cancel();
    if (localTransaction != null) {
      localTransaction.closeAll();
      localTransaction = null;
    }
    if (shutdownQuickly) {
      shutdownQuickly();
    } else {
      shutdownGracefully();
    }
    if (isUseLocalExec()) {
      LocalExecClient.releaseResources();
    }
    getR66BusinessFactory().releaseResources();
    if (timerCloseOperations != null && !timerCloseClosed.get()) {
      timerCloseClosed.set(true);
      timerCloseOperations.stop();
    }
  }

  /**
   * Try to reload the Commander
   *
   * @return True if reloaded, else in error
   */
  public final boolean reloadCommanderDelay() {
    if (internalRunner != null) {
      try {
        internalRunner.reloadInternalRunner();
        return true;
      } catch (final WaarpDatabaseNoConnectionException ignored) {
        // nothing
      } catch (final WaarpDatabaseSqlException ignored) {
        // nothing
      }
    }
    return false;
  }

  /**
   * submit a task in a fixed delay
   *
   * @param thread
   * @param delay
   * @param unit
   */
  public final void launchInFixedDelay(final Thread thread, final long delay,
                                       final TimeUnit unit) {
    scheduledExecutorService.schedule(thread, delay, unit);
  }

  /**
   * submit a task in a repeated delay
   *
   * @param thread
   * @param delay
   * @param unit
   */
  public final void scheduleWithFixedDelay(final Thread thread,
                                           final long delay,
                                           final TimeUnit unit) {
    scheduledExecutorService.scheduleWithFixedDelay(thread, delay, delay, unit);
  }

  public final void setupLimitHandler() {
    if (globalTrafficShapingHandler != null) {
      return;
    }
    globalTrafficShapingHandler = new GlobalTrafficShapingHandler(subTaskGroup,
                                                                  getServerGlobalWriteLimit(),
                                                                  getServerGlobalReadLimit(),
                                                                  getDelayLimit());
    getConstraintLimitHandler().setHandler(globalTrafficShapingHandler);
  }

  /**
   * Reset the global monitor for bandwidth limitation and change future
   * channel
   * monitors
   *
   * @param writeGlobalLimit
   * @param readGlobalLimit
   * @param writeSessionLimit
   * @param readSessionLimit
   * @param delayLimit
   */
  public final void changeNetworkLimit(long writeGlobalLimit,
                                       long readGlobalLimit,
                                       long writeSessionLimit,
                                       long readSessionLimit,
                                       final long delayLimit) {
    if (writeGlobalLimit <= 0) {
      writeGlobalLimit = 0;
    }
    if (readGlobalLimit <= 0) {
      readGlobalLimit = 0;
    }
    if (writeSessionLimit <= 0) {
      writeSessionLimit = 0;
    }
    if (readSessionLimit <= 0) {
      readSessionLimit = 0;
    }
    if (writeGlobalLimit < writeSessionLimit) {
      writeSessionLimit = writeGlobalLimit;
      logger.warn("Wanted global write limit is inferior " +
                  "to session limit. Will force session limit to {} ",
                  writeGlobalLimit);
    }
    if (readGlobalLimit < readSessionLimit) {
      readSessionLimit = readGlobalLimit;
      logger.warn("Wanted global read limit is inferior " +
                  "to session limit. Will force session limit to {} ",
                  readGlobalLimit);
    }
    setServerGlobalReadLimit(readGlobalLimit);
    setServerGlobalWriteLimit(writeGlobalLimit);
    setServerChannelReadLimit(readSessionLimit);
    setServerChannelWriteLimit(writeSessionLimit);
    setDelayLimit(delayLimit);
    if (globalTrafficShapingHandler != null) {
      globalTrafficShapingHandler.configure(writeGlobalLimit, readGlobalLimit,
                                            delayLimit);
      logger.info(Messages.getString("Configuration.BandwidthChange"),
                  globalTrafficShapingHandler);
    }
  }

  /**
   * Compute number of threads for both client and server from the real number
   * of available processors (double +
   * 1) if the value is less than 32 threads else (available +1).
   */
  public void computeNbThreads() {
    int nb = Runtime.getRuntime().availableProcessors() * 2 + 1;
    if (nb > 32) {
      nb = Runtime.getRuntime().availableProcessors() + 1;
    }
    if (getServerThread() <= 0 || getServerThread() > nb) {
      logger.info(Messages.getString("Configuration.ThreadNumberChange") +
                  nb); //$NON-NLS-1$
      setServerThread(nb);
      if (getClientThread() < getServerThread() * 10) {
        setClientThread(getServerThread() * 10);
      }
    } else if (getClientThread() < nb) {
      setClientThread(nb);
    }
  }

  /**
   * @return an executorService to be used for any thread
   */
  public final ExecutorService getExecutorService() {
    return execOtherWorker;
  }

  public final Timer getTimerClose() {
    return timerCloseOperations;
  }

  public final boolean isTimerCloseReady() {
    return !timerCloseClosed.get();
  }

  /**
   * @return the globalTrafficShapingHandler
   */
  public final GlobalTrafficShapingHandler getGlobalTrafficShapingHandler() {
    return globalTrafficShapingHandler;
  }

  /**
   * @return the serverChannelGroup
   */
  public final ChannelGroup getServerChannelGroup() {
    return serverChannelGroup;
  }

  /**
   * @return the server connected channels group
   */
  public final ChannelGroup getServerConnectedChannelGroup() {
    return serverConnectedChannelGroup;
  }

  /**
   * @return the httpChannelGroup
   */
  public final ChannelGroup getHttpChannelGroup() {
    return httpChannelGroup;
  }

  /**
   * @return the serverPipelineExecutor
   */
  public final EventLoopGroup getNetworkWorkerGroup() {
    return workerGroup;
  }

  /**
   * @return the retrieveRunnerGroup
   */
  public final ThreadPoolRunnerExecutor getRetrieveRunnerGroup() {
    return retrieveRunnerGroup;
  }

  /**
   * @return the serverPipelineExecutor
   */
  public final EventLoopGroup getHandlerGroup() {
    return handlerGroup;
  }

  /**
   * @return the subTaskGroup
   */
  public final EventLoopGroup getSubTaskGroup() {
    return subTaskGroup;
  }

  /**
   * @return the httpWorkerGroup
   */
  public final EventLoopGroup getHttpWorkerGroup() {
    return httpWorkerGroup;
  }

  /**
   * @return the localTransaction
   */
  public final LocalTransaction getLocalTransaction() {
    return localTransaction;
  }

  /**
   * @return the FilesystemBasedFileParameterImpl
   */
  public static FilesystemBasedFileParameterImpl getFileParameter() {
    return fileParameter;
  }

  /**
   * @return the SERVERADMINKEY
   */
  public final byte[] getServerAdminKey() {
    return serverAdminKey;
  }

  /**
   * Is the given key a valid one
   *
   * @param newkey
   *
   * @return True if the key is valid (or any key is valid)
   */
  public final boolean isKeyValid(final byte[] newkey) {
    if (newkey == null) {
      return false;
    }
    return FilesystemBasedDigest.equalPasswd(serverAdminKey, newkey);
  }

  /**
   * @param serverkey the SERVERADMINKEY to set
   */
  public final void setSERVERKEY(final byte[] serverkey) {
    serverAdminKey = serverkey;
  }

  /**
   * @param isSSL
   *
   * @return the HostId according to SSL
   *
   * @throws OpenR66ProtocolNoSslException
   */
  public final String getHostId(final boolean isSSL)
      throws OpenR66ProtocolNoSslException {
    if (isSSL) {
      if (getHostSslId() == null) {
        throw new OpenR66ProtocolNoSslException(
            Messages.getString("Configuration.ExcNoSSL")); //$NON-NLS-1$
      }
      return getHostSslId();
    } else {
      return getHostId();
    }
  }

  /**
   * @param remoteHost
   *
   * @return the HostId according to remoteHost (and its SSL status)
   *
   * @throws WaarpDatabaseException
   */
  public final String getHostId(final String remoteHost)
      throws WaarpDatabaseException {
    final DbHostAuth dbHostAuth = new DbHostAuth(remoteHost);
    try {
      return configuration.getHostId(dbHostAuth.isSsl());
    } catch (final OpenR66ProtocolNoSslException e) {
      throw new WaarpDatabaseException(e);
    }
  }

  private static class UsageStatistic extends TimerTask {

    @Override
    public void run() {
      logger.warn(hashStatus());
    }

  }

  public static String hashStatus() {
    String result = "\n";
    try {
      result += configuration.localTransaction.hashStatus() + '\n';
    } catch (final Exception e) {
      logger.warn(ISSUE_WHILE_DEBUGGING + " : {}", e.getMessage());
    }
    try {
      result += ClientRunner.hashStatus() + '\n';
    } catch (final Exception e) {
      logger.warn(ISSUE_WHILE_DEBUGGING + " : {}", e.getMessage());
    }
    try {
      result += DbTaskRunner.hashStatus() + '\n';
    } catch (final Exception e) {
      logger.warn(ISSUE_WHILE_DEBUGGING + " : {}", e.getMessage());
    }
    try {
      result += HttpSslHandler.hashStatus() + '\n';
    } catch (final Exception e) {
      logger.warn(ISSUE_WHILE_DEBUGGING + " : {}", e.getMessage());
    }
    try {
      result += NetworkTransaction.hashStatus();
    } catch (final Exception e) {
      logger.warn(ISSUE_WHILE_DEBUGGING + " : {}", e.getMessage());
    }
    return result;
  }

  /**
   * @return the nBDBSESSION
   */
  public static int getNbDbSession() {
    return nbDbSession;
  }

  /**
   * @param nBDBSESSION the nBDBSESSION to set
   */
  public static void setNbDbSession(final int nBDBSESSION) {
    nbDbSession = nBDBSESSION;
  }

  /**
   * @return the rANKRESTART
   */
  public static int getRankRestart() {
    return rankRestart;
  }

  /**
   * @param rANKRESTART the rANKRESTART to set
   */
  public static void setRankRestart(final int rANKRESTART) {
    rankRestart = rANKRESTART;
  }

  /**
   * @return the iSUNIX
   */
  public static boolean isIsUnix() {
    return isUnix;
  }

  /**
   * @param iSUNIX the iSUNIX to set
   */
  public static void setIsUnix(final boolean iSUNIX) {
    isUnix = iSUNIX;
  }

  /**
   * @return the r66BusinessFactory
   */
  public final R66BusinessFactoryInterface getR66BusinessFactory() {
    return r66BusinessFactory;
  }

  /**
   * @return the extendedProtocol
   */
  public final boolean isExtendedProtocol() {
    return extendedProtocol;
  }

  /**
   * @param extendedProtocol the extendedProtocol to set
   */
  public final void setExtendedProtocol(final boolean extendedProtocol) {
    this.extendedProtocol = extendedProtocol;
  }

  /**
   * @return the globalDigest
   */
  public final boolean isGlobalDigest() {
    return globalDigest;
  }

  /**
   * @param globalDigest the globalDigest to set
   */
  public final void setGlobalDigest(final boolean globalDigest) {
    this.globalDigest = globalDigest;
  }

  /**
   * @return the localDigest
   */
  public final boolean isLocalDigest() {
    return localDigest;
  }

  /**
   * @param localDigest the localDigest to set
   */
  public final void setLocalDigest(final boolean localDigest) {
    this.localDigest = localDigest;
  }

  /**
   * @return the businessWhiteSet
   */
  public final Set<String> getBusinessWhiteSet() {
    return businessWhiteSet;
  }

  /**
   * @return the roles
   */
  public final Map<String, RoleDefault> getRoles() {
    return roles;
  }

  /**
   * @return the aliases
   */
  public final Map<String, String> getAliases() {
    return aliases;
  }

  /**
   * @return the reverseAliases
   */
  public final Map<String, String[]> getReverseAliases() {
    return reverseAliases;
  }

  /**
   * @return the versions
   */
  public final ConcurrentMap<String, PartnerConfiguration> getVersions() {
    return versions;
  }

  /**
   * @return the hOST_ID
   */
  public final String getHostId() {
    return hostId;
  }

  /**
   * @param hostID the hOST_ID to set
   */
  public final void setHostId(final String hostID) {
    hostId = hostID;
    WaarpLoggerFactory.setLocalName(hostId);
  }

  /**
   * @return the hOST_SSLID
   */
  public final String getHostSslId() {
    return hostSslId;
  }

  /**
   * @param hostSSLID the hOST_SSLID to set
   */
  public final void setHostSslId(final String hostSSLID) {
    hostSslId = hostSSLID;
  }

  /**
   * @return the aDMINNAME
   */
  public final String getAdminName() {
    return adminName;
  }

  /**
   * @param aDMINNAME the aDMINNAME to set
   */
  public final void setAdminName(final String aDMINNAME) {
    adminName = aDMINNAME;
  }

  /**
   * @return the serverKeyFile
   */
  public final String getServerKeyFile() {
    return serverKeyFile;
  }

  /**
   * @param serverKeyFile the serverKeyFile to set
   */
  public final void setServerKeyFile(final String serverKeyFile) {
    this.serverKeyFile = serverKeyFile;
  }

  /**
   * @return the hOST_AUTH
   */
  public final DbHostAuth getHostAuth() {
    return hostAuth;
  }

  /**
   * @param hostAUTH the hOST_AUTH to set
   */
  public final void setHostAuth(final DbHostAuth hostAUTH) {
    hostAuth = hostAUTH;
  }

  /**
   * @return the hOST_SSLAUTH
   */
  public final DbHostAuth getHostSslAuth() {
    return hostSslAuth;
  }

  /**
   * @param hostSSLAUTH the hOST_SSLAUTH to set
   */
  public final void setHostSslAuth(final DbHostAuth hostSSLAUTH) {
    hostSslAuth = hostSSLAUTH;
  }

  public final String getAuthFile() {
    return authFile;
  }

  public final void setAuthFile(final String file) {
    authFile = file;
  }

  /**
   * @return the sERVER_THREAD
   */
  public final int getServerThread() {
    return serverThread;
  }

  /**
   * @param serverTHREAD the sERVER_THREAD to set
   */
  public final void setServerThread(final int serverTHREAD) {
    serverThread = serverTHREAD;
  }

  /**
   * @return the cLIENT_THREAD
   */
  public final int getClientThread() {
    return clientThread;
  }

  /**
   * @param clientTHREAD the cLIENT_THREAD to set
   */
  public final void setClientThread(final int clientTHREAD) {
    if (clientTHREAD > Commander.LIMIT_MAX_SUBMIT) {
      clientThread = Commander.LIMIT_MAX_SUBMIT;
    } else {
      clientThread = clientTHREAD;
    }
  }

  /**
   * @return the dEFAULT_SESSION_LIMIT
   */
  public final long getDEFAULT_SESSION_LIMIT() {
    return DEFAULT_SESSION_LIMIT;
  }

  /**
   * @return the dEFAULT_GLOBAL_LIMIT
   */
  public final long getDEFAULT_GLOBAL_LIMIT() {
    return DEFAULT_GLOBAL_LIMIT;
  }

  /**
   * @return the sERVER_PORT
   */
  public final int getServerPort() {
    return serverPort;
  }

  /**
   * @param serverPORT the sERVER_PORT to set
   */
  public final void setServerPort(final int serverPORT) {
    serverPort = serverPORT;
  }

  /**
   * @return the sERVER_SSLPORT
   */
  public final int getServerSslPort() {
    return serverSslPort;
  }

  /**
   * @param serverSSLPORT the sERVER_SSLPORT to set
   */
  public final void setServerSslPort(final int serverSSLPORT) {
    serverSslPort = serverSSLPORT;
  }

  /**
   * @return the sERVER_HTTPPORT
   */
  public final int getServerHttpport() {
    return serverHttpport;
  }

  /**
   * @param serverHTTPPORT the sERVER_HTTPPORT to set
   */
  public final void setServerHttpport(final int serverHTTPPORT) {
    serverHttpport = serverHTTPPORT;
  }

  /**
   * @return the sERVER_HTTPSPORT
   */
  public final int getServerHttpsPort() {
    return serverHttpsPort;
  }

  /**
   * @param serverHTTPSPORT the sERVER_HTTPSPORT to set
   */
  public final void setServerHttpsPort(final int serverHTTPSPORT) {
    serverHttpsPort = serverHTTPSPORT;
  }

  /**
   * @return the sERVER_Addresses
   */
  public final String[] getServerIpsAddresses() {
    return serverAddresses;
  }

  /**
   * @param serverAddresses the sERVER_Addresses to set
   */
  public final void setServerAddresses(final String[] serverAddresses) {
    this.serverAddresses = serverAddresses;
  }

  /**
   * @return the sERVER_SSLAddresses
   */
  public final String[] getServerSslAddresses() {
    return serverSslAddresses;
  }

  /**
   * @param serverSSLAddresses the sERVER_SSLIAddresses to set
   */
  public final void setServerSslAddresses(final String[] serverSSLAddresses) {
    serverSslAddresses = serverSSLAddresses;
  }

  /**
   * @return the sERVER_HTTPAddresses
   */
  public final String[] getServerHttpAddresses() {
    return serverHttpAddresses;
  }

  /**
   * @param serverHTTPAddresses the sERVER_HTTPAddresses to set
   */
  public final void setServerHttpAddresses(final String[] serverHTTPAddresses) {
    serverHttpAddresses = serverHTTPAddresses;
  }

  /**
   * @return the sERVER_HTTPSAddresses
   */
  public final String[] getServerHttpsAddresses() {
    return serverHttpsAddresses;
  }

  /**
   * @param serverHTTPSAddresses the sERVER_HTTPSAddresses to set
   */
  public final void setServerHttpsAddresses(
      final String[] serverHTTPSAddresses) {
    serverHttpsAddresses = serverHTTPSAddresses;
  }

  /**
   * @return the tIMEOUTCON
   */
  public final long getTimeoutCon() {
    return timeoutCon;
  }

  /**
   * @param timeoutCON the timeoutCON to set
   */
  public final void setTimeoutCon(final long timeoutCON) {
    timeoutCon = timeoutCON;
  }

  /**
   * @return the bLOCKSIZE
   */
  public final int getBlockSize() {
    return blockSize;
  }

  /**
   * @param blockSIZE the bLOCKSIZE to set
   */
  public final void setBlockSize(final int blockSIZE) {
    blockSize = blockSIZE;
  }

  /**
   * @return the maxGlobalMemory
   */
  public final int getMaxGlobalMemory() {
    return maxGlobalMemory;
  }

  /**
   * @param maxGlobalMemory the maxGlobalMemory to set
   */
  public final void setMaxGlobalMemory(final int maxGlobalMemory) {
    this.maxGlobalMemory = maxGlobalMemory;
  }

  /**
   * @return the restConfigurations
   */
  public final List<RestConfiguration> getRestConfigurations() {
    return restConfigurations;
  }

  /**
   * @return the baseDirectory
   */
  public final String getBaseDirectory() {
    return baseDirectory;
  }

  /**
   * @param baseDirectory the baseDirectory to set
   */
  public final void setBaseDirectory(final String baseDirectory) {
    this.baseDirectory = baseDirectory;
  }

  /**
   * @return the inPath
   */
  public final String getInPath() {
    return inPath;
  }

  /**
   * @param inPath the inPath to set
   */
  public final void setInPath(final String inPath) {
    this.inPath = inPath;
  }

  /**
   * @return the outPath
   */
  public final String getOutPath() {
    return outPath;
  }

  /**
   * @param outPath the outPath to set
   */
  public final void setOutPath(final String outPath) {
    this.outPath = outPath;
  }

  /**
   * @return the archivePath
   */
  public final String getArchivePath() {
    return archivePath;
  }

  /**
   * @param archivePath the archivePath to set
   */
  public final void setArchivePath(final String archivePath) {
    this.archivePath = archivePath;
  }

  /**
   * @return the workingPath
   */
  public final String getWorkingPath() {
    return workingPath;
  }

  /**
   * @param workingPath the workingPath to set
   */
  public final void setWorkingPath(final String workingPath) {
    this.workingPath = workingPath;
  }

  /**
   * @return the configPath
   */
  public final String getConfigPath() {
    return configPath;
  }

  /**
   * @param configPath the configPath to set
   */
  public final void setConfigPath(final String configPath) {
    this.configPath = configPath;
  }

  /**
   * @return the httpBasePath
   */
  public final String getHttpBasePath() {
    return httpBasePath;
  }

  /**
   * @param httpBasePath the httpBasePath to set
   */
  public final void setHttpBasePath(final String httpBasePath) {
    this.httpBasePath = httpBasePath;
  }

  /**
   * @return the httpModel
   */
  public final int getHttpModel() {
    return httpModel;
  }

  /**
   * @param httpModel the httpModel to set
   */
  public final void setHttpModel(final int httpModel) {
    this.httpModel = httpModel;
  }

  /**
   * @return the isShutdown
   */
  public final boolean isShutdown() {
    return isShutdown;
  }

  /**
   * @param isShutdown the isShutdown to set
   */
  public final void setShutdown(final boolean isShutdown) {
    this.isShutdown = isShutdown;
  }

  /**
   * @return the serverGlobalWriteLimit
   */
  public final long getServerGlobalWriteLimit() {
    return serverGlobalWriteLimit;
  }

  /**
   * @param serverGlobalWriteLimit the serverGlobalWriteLimit to set
   */
  public final void setServerGlobalWriteLimit(
      final long serverGlobalWriteLimit) {
    this.serverGlobalWriteLimit = serverGlobalWriteLimit;
  }

  /**
   * @return the serverGlobalReadLimit
   */
  public final long getServerGlobalReadLimit() {
    return serverGlobalReadLimit;
  }

  /**
   * @param serverGlobalReadLimit the serverGlobalReadLimit to set
   */
  public final void setServerGlobalReadLimit(final long serverGlobalReadLimit) {
    this.serverGlobalReadLimit = serverGlobalReadLimit;
  }

  /**
   * @return the serverChannelWriteLimit
   */
  public final long getServerChannelWriteLimit() {
    return serverChannelWriteLimit;
  }

  /**
   * @param serverChannelWriteLimit the serverChannelWriteLimit to set
   */
  public final void setServerChannelWriteLimit(
      final long serverChannelWriteLimit) {
    this.serverChannelWriteLimit = serverChannelWriteLimit;
  }

  /**
   * @return the serverChannelReadLimit
   */
  public final long getServerChannelReadLimit() {
    return serverChannelReadLimit;
  }

  /**
   * @param serverChannelReadLimit the serverChannelReadLimit to set
   */
  public final void setServerChannelReadLimit(
      final long serverChannelReadLimit) {
    this.serverChannelReadLimit = serverChannelReadLimit;
  }

  /**
   * @return the delayLimit
   */
  public final long getDelayLimit() {
    return delayLimit;
  }

  /**
   * @param delayLimit the delayLimit to set
   */
  public final void setDelayLimit(final long delayLimit) {
    this.delayLimit = delayLimit;
  }

  /**
   * @return the useSSL
   */
  public final boolean isUseSSL() {
    return useSSL;
  }

  /**
   * @param useSSL the useSSL to set
   */
  public final void setUseSSL(final boolean useSSL) {
    this.useSSL = useSSL;
  }

  /**
   * @return the useNOSSL
   */
  public final boolean isUseNOSSL() {
    return useNOSSL;
  }

  /**
   * @param useNOSSL the useNOSSL to set
   */
  public final void setUseNOSSL(final boolean useNOSSL) {
    this.useNOSSL = useNOSSL;
  }

  /**
   * @return the digest
   */
  public final FilesystemBasedDigest.DigestAlgo getDigest() {
    return digest;
  }

  /**
   * @param digest the digest to set
   */
  public final void setDigest(final FilesystemBasedDigest.DigestAlgo digest) {
    this.digest = digest;
  }

  /**
   * @return the useHttpCompression
   */
  public final boolean isUseHttpCompression() {
    return useHttpCompression;
  }

  /**
   * @param useHttpCompression the useHttpCompression to set
   */
  public final void setUseHttpCompression(final boolean useHttpCompression) {
    this.useHttpCompression = useHttpCompression;
  }

  /**
   * @return the cryptoKey
   */
  public final Des getCryptoKey() {
    return cryptoKey;
  }

  /**
   * @param cryptoKey the cryptoKey to set
   */
  public final void setCryptoKey(final Des cryptoKey) {
    this.cryptoKey = cryptoKey;
  }

  /**
   * @return the cryptoFile
   */
  public final String getCryptoFile() {
    return cryptoFile;
  }

  /**
   * @param cryptoFile the cryptoFile to set
   */
  public final void setCryptoFile(final String cryptoFile) {
    this.cryptoFile = cryptoFile;
  }

  /**
   * @return the useLocalExec
   */
  public final boolean isUseLocalExec() {
    return useLocalExec;
  }

  /**
   * @param useLocalExec the useLocalExec to set
   */
  public final void setUseLocalExec(final boolean useLocalExec) {
    this.useLocalExec = useLocalExec;
  }

  /**
   * @return the isServer
   */
  public final boolean isServer() {
    return isServer;
  }

  /**
   * @param isServer the isServer to set
   */
  protected final void setServer(final boolean isServer) {
    this.isServer = isServer;
  }

  /**
   * @return the rUNNER_THREAD
   */
  public final int getRunnerThread() {
    return runnerThread;
  }

  /**
   * @param runnerTHREAD the rUNNER_THREAD to set
   */
  public final void setRunnerThread(final int runnerTHREAD) {
    if (runnerTHREAD > Commander.LIMIT_MAX_SUBMIT) {
      logger.warn("RunnerThread at {} will be limited to default maximum {}",
                  runnerTHREAD, Commander.LIMIT_MAX_SUBMIT);
      runnerThread = Commander.LIMIT_MAX_SUBMIT;
    } else {
      runnerThread = runnerTHREAD <= 1? 2 : runnerTHREAD;
    }
  }

  /**
   * @return the delayCommander
   */
  public final long getDelayCommander() {
    return delayCommander;
  }

  /**
   * @param delayCommander the delayCommander to set
   */
  public final void setDelayCommander(final long delayCommander) {
    this.delayCommander = delayCommander;
  }

  /**
   * @return the delayRetry
   */
  public final long getDelayRetry() {
    return delayRetry;
  }

  /**
   * @param delayRetry the delayRetry to set
   */
  public final void setDelayRetry(final long delayRetry) {
    this.delayRetry = delayRetry;
  }

  /**
   * @return the constraintLimitHandler
   */
  public final R66ConstraintLimitHandler getConstraintLimitHandler() {
    return constraintLimitHandler;
  }

  /**
   * @param constraintLimitHandler the constraintLimitHandler to set
   */
  public final void setConstraintLimitHandler(
      final R66ConstraintLimitHandler constraintLimitHandler) {
    this.constraintLimitHandler = constraintLimitHandler;
  }

  /**
   * @return the checkRemoteAddress
   */
  public final boolean isCheckRemoteAddress() {
    return checkRemoteAddress;
  }

  /**
   * @param checkRemoteAddress the checkRemoteAddress to set
   */
  public final void setCheckRemoteAddress(final boolean checkRemoteAddress) {
    this.checkRemoteAddress = checkRemoteAddress;
  }

  /**
   * @return the checkClientAddress
   */
  public final boolean isCheckClientAddress() {
    return checkClientAddress;
  }

  /**
   * @param checkClientAddress the checkClientAddress to set
   */
  public final void setCheckClientAddress(final boolean checkClientAddress) {
    this.checkClientAddress = checkClientAddress;
  }

  /**
   * @return the saveTaskRunnerWithNoDb
   */
  public final boolean isSaveTaskRunnerWithNoDb() {
    return saveTaskRunnerWithNoDb;
  }

  /**
   * @param saveTaskRunnerWithNoDb the saveTaskRunnerWithNoDb to set
   */
  public final void setSaveTaskRunnerWithNoDb(
      final boolean saveTaskRunnerWithNoDb) {
    this.saveTaskRunnerWithNoDb = saveTaskRunnerWithNoDb;
  }

  /**
   * @return the multipleMonitors
   */
  public final int getMultipleMonitors() {
    return multipleMonitors;
  }

  /**
   * @param multipleMonitors the multipleMonitors to set
   */
  public final void setMultipleMonitors(final int multipleMonitors) {
    this.multipleMonitors = multipleMonitors;
  }

  /**
   * @return the monitoring
   */
  public final Monitoring getMonitoring() {
    return monitoring;
  }

  /**
   * @param monitoring the monitoring to set
   */
  public final void setMonitoring(final Monitoring monitoring) {
    this.monitoring = monitoring;
  }

  /**
   * @return the pastLimit
   */
  public final long getPastLimit() {
    return pastLimit;
  }

  /**
   * @param pastLimit the pastLimit to set
   */
  public final void setPastLimit(final long pastLimit) {
    this.pastLimit = pastLimit;
  }

  /**
   * @return the minimalDelay
   */
  public final long getMinimalDelay() {
    return minimalDelay;
  }

  /**
   * @param minimalDelay the minimalDelay to set
   */
  public final void setMinimalDelay(final long minimalDelay) {
    this.minimalDelay = minimalDelay;
  }

  /**
   * @return the snmpConfig
   */
  public final String getSnmpConfig() {
    return snmpConfig;
  }

  /**
   * @param snmpConfig the snmpConfig to set
   */
  public final void setSnmpConfig(final String snmpConfig) {
    this.snmpConfig = snmpConfig;
  }

  /**
   * @return the agentSnmp
   */
  public final WaarpSnmpAgent getAgentSnmp() {
    return agentSnmp;
  }

  /**
   * @param agentSnmp the agentSnmp to set
   */
  public final void setAgentSnmp(final WaarpSnmpAgent agentSnmp) {
    this.agentSnmp = agentSnmp;
  }

  /**
   * @return the r66Mib
   */
  public final R66PrivateMib getR66Mib() {
    return r66Mib;
  }

  /**
   * @param r66Mib the r66Mib to set
   */
  public final void setR66Mib(final R66PrivateMib r66Mib) {
    this.r66Mib = r66Mib;
  }

  /**
   * @return the waarpSecureKeyStore
   */
  public static WaarpSecureKeyStore getWaarpSecureKeyStore() {
    return waarpSecureKeyStore;
  }

  /**
   * @param waarpSecureKeyStore the waarpSecureKeyStore to set
   */
  public static void setWaarpSecureKeyStore(
      final WaarpSecureKeyStore waarpSecureKeyStore) {
    Configuration.waarpSecureKeyStore = waarpSecureKeyStore;
  }

  /**
   * @return the waarpSslContextFactory
   */
  public static WaarpSslContextFactory getWaarpSslContextFactory() {
    return waarpSslContextFactory;
  }

  /**
   * @param waarpSslContextFactory the waarpSslContextFactory to set
   */
  public static void setWaarpSslContextFactory(
      final WaarpSslContextFactory waarpSslContextFactory) {
    Configuration.waarpSslContextFactory = waarpSslContextFactory;
  }

  /**
   * @return the thriftService
   */
  public final R66ThriftServerService getThriftService() {
    return thriftService;
  }

  /**
   * @param thriftService the thriftService to set
   */
  public final void setThriftService(
      final R66ThriftServerService thriftService) {
    this.thriftService = thriftService;
  }

  /**
   * @return the thriftport
   */
  public final int getThriftport() {
    return thriftport;
  }

  /**
   * @param thriftport the thriftport to set
   */
  public final void setThriftport(final int thriftport) {
    this.thriftport = thriftport;
  }

  /**
   * @return the isExecuteErrorBeforeTransferAllowed
   */
  public final boolean isExecuteErrorBeforeTransferAllowed() {
    return isExecuteErrorBeforeTransferAllowed;
  }

  /**
   * @param isExecuteErrorBeforeTransferAllowed the
   *     isExecuteErrorBeforeTransferAllowed
   *     to set
   */
  public final void setExecuteErrorBeforeTransferAllowed(
      final boolean isExecuteErrorBeforeTransferAllowed) {
    this.isExecuteErrorBeforeTransferAllowed =
        isExecuteErrorBeforeTransferAllowed;
  }

  /**
   * @return the shutdownConfiguration
   */
  public final ShutdownConfiguration getShutdownConfiguration() {
    return shutdownConfiguration;
  }

  /**
   * @return the isHostProxyfied
   */
  public final boolean isHostProxyfied() {
    return isHostProxyfied;
  }

  /**
   * @param isHostProxyfied the isHostProxyfied to set
   */
  public final void setHostProxyfied(final boolean isHostProxyfied) {
    this.isHostProxyfied = isHostProxyfied;
  }

  /**
   * @return True if Authentication cannot be reused
   */
  public final boolean isAuthentNoReuse() {
    return authentNoReuse;
  }

  /**
   * @param authentNoReuse
   */
  public final void setAuthentNoReuse(final boolean authentNoReuse) {
    this.authentNoReuse = authentNoReuse;
  }

  /**
   * @return the warnOnStartup
   */
  public final boolean isWarnOnStartup() {
    return warnOnStartup;
  }

  /**
   * @param warnOnStartup the warnOnStartup to set
   */
  public final void setWarnOnStartup(final boolean warnOnStartup) {
    this.warnOnStartup = warnOnStartup;
  }

  /**
   * @return the chrootChecked
   */
  public final boolean isChrootChecked() {
    return chrootChecked;
  }

  /**
   * @param chrootChecked the chrootChecked to set
   */
  public final void setChrootChecked(final boolean chrootChecked) {
    this.chrootChecked = chrootChecked;
  }

  /**
   * @return the blacklistBadAuthent
   */
  public final boolean isBlacklistBadAuthent() {
    return blacklistBadAuthent;
  }

  /**
   * @param blacklistBadAuthent the blacklistBadAuthent to set
   */
  public final void setBlacklistBadAuthent(final boolean blacklistBadAuthent) {
    this.blacklistBadAuthent = blacklistBadAuthent;
  }

  /**
   * @return the maxfilenamelength
   */
  public final int getMaxfilenamelength() {
    return maxfilenamelength;
  }

  /**
   * @param maxfilenamelength the maxfilenamelength to set
   */
  public final void setMaxfilenamelength(final int maxfilenamelength) {
    this.maxfilenamelength = maxfilenamelength;
  }

  /**
   * @return the timeStat
   */
  public final int getTimeStat() {
    return timeStat;
  }

  /**
   * @param timeStat the timeStat to set
   */
  public final void setTimeStat(final int timeStat) {
    this.timeStat = timeStat;
  }

  /**
   * @return the limitCache
   */
  public final int getLimitCache() {
    return limitCache;
  }

  /**
   * @param limitCache the limitCache to set
   */
  public final void setLimitCache(final int limitCache) {
    this.limitCache = limitCache;
  }

  /**
   * @return the timeLimitCache
   */
  public final long getTimeLimitCache() {
    return timeLimitCache;
  }

  /**
   * @param timeLimitCache the timeLimitCache to set
   */
  public final void setTimeLimitCache(final long timeLimitCache) {
    this.timeLimitCache = timeLimitCache;
  }

  /**
   * Set the parameters for MonitorExporterTransfers using API REST
   *
   * @param url as 'http://myhost.com:8080' or 'https://myhost.com:8443'
   * @param basicAuthent Basic Authent in Base64 to connect to REST API if
   *     any (Basic authentication) (nullable)
   * @param token access token (Bearer Token authorization
   *     by Header) (nullable)
   * @param apiKey API Key (Base64 of 'apiId:apiKey') (ApiKey authorization
   *     by Header) (nullable)
   * @param endpoint as '/waarpr66monitor' or simply '/'
   * @param keepConnection True to keep the connexion opened, False to release the connexion each time
   * @param monitorIntervalIncluded True to include the interval information within 'waarpMonitor'
   *     field
   * @param monitorTransformLongAsString True to transform Long as String (ELK)
   * @param delay delay between 2 exports
   */
  public final void setMonitorExporterTransfers(final String url,
                                                final String basicAuthent,
                                                final String token,
                                                final String apiKey,
                                                final String endpoint,
                                                final int delay,
                                                final boolean keepConnection,
                                                final boolean monitorIntervalIncluded,
                                                final boolean monitorTransformLongAsString) {
    this.monitorExporterDelay = delay;
    this.monitorExporterUrl = url;
    this.monitorExporterEndPoint = endpoint;
    this.monitorExporterKeepConnection = keepConnection;
    this.monitorIntervalIncluded = monitorIntervalIncluded;
    this.monitorTransformLongAsString = monitorTransformLongAsString;
    this.monitorBasicAuthent = basicAuthent;
    this.monitorToken = token;
    this.monitorApiKey = apiKey;
    isMonitorExporterApiRest = true;
  }

  /**
   * @return True if the compression is available
   */
  public final boolean isCompressionAvailable() {
    return compressionAvailable;
  }

  /**
   * @param compressionAvailable
   */
  public final void setCompressionAvailable(
      final boolean compressionAvailable) {
    this.compressionAvailable = compressionAvailable;
  }

  /**
   * Set the parameters for MonitorExporterTransfers using Elasticsearch (JRE
   * >= 8)
   *
   * @param remoteBaseUrl as 'http://myelastic.com:9200' or 'https://myelastic.com:9201'
   * @param username username to connect to Elasticsearch if any (Basic
   *     authentication) (nullable)
   * @param pwd password to connect to Elasticsearch if any (Basic
   *     authentication) (nullable)
   * @param token access token (Bearer Token authorization
   *     by Header) (nullable)
   * @param apiKey API Key (Base64 of 'apiId:apiKey') (ApiKey authorization
   *     by Header) (nullable)
   * @param prefix as '/prefix' or null if none
   * @param index as 'waarpr66monitor' as the index name within
   *     Elasticsearch, including extra dynamic information
   * @param intervalMonitoringIncluded True to include the interval information within 'waarpMonitor' field
   * @param transformLongAsString True to transform Long as String (ELK)
   * @param compression True to compress REST exchanges between the client
   *     and the Elasticsearch server
   * @param delay delay between 2 exports
   *
   * @return True if the Elasticsearch factory is available
   */
  public final boolean setMonitorExporterTransfers(final String remoteBaseUrl,
                                                   final String username,
                                                   final String pwd,
                                                   final String token,
                                                   final String apiKey,
                                                   final String prefix,
                                                   final String index,
                                                   final boolean intervalMonitoringIncluded,
                                                   final boolean transformLongAsString,
                                                   final boolean compression,
                                                   final int delay) {
    this.monitorExporterDelay = delay;
    this.monitorExporterUrl = remoteBaseUrl;
    this.monitorIntervalIncluded = intervalMonitoringIncluded;
    this.monitorTransformLongAsString = transformLongAsString;
    this.monitorUsername = username;
    this.monitorPwd = pwd;
    this.monitorToken = token;
    this.monitorApiKey = apiKey;
    this.monitorPrefix = prefix;
    this.monitorIndex = index;
    this.monitorCompression = compression;
    isMonitorExporterApiRest = false;
    try {
      ElasticsearchMonitoringExporterClientBuilder.getFactory();
    } catch (final Exception e) {
      logger.error("Elasticsearch for MonitorExpoerter is not available in " +
                   "the classpath: {}", e.getMessage());
      return false;
    }
    return true;
  }

  /**
   * Start the Monitor Exporter Transfers (through REST API)
   */
  public final void startMonitorExporterTransfers() {
    if (monitorExporterUrl != null && monitorExporterEndPoint != null &&
        monitorExporterDelay > 500) {
      if (isMonitorExporterApiRest) {
        this.monitorExporterTransfers =
            new MonitorExporterTransfers(monitorExporterUrl,
                                         monitorExporterEndPoint,
                                         monitorBasicAuthent, monitorToken,
                                         monitorApiKey,
                                         monitorExporterKeepConnection,
                                         monitorIntervalIncluded,
                                         monitorTransformLongAsString,
                                         getHttpWorkerGroup());
      } else {
        this.monitorExporterTransfers =
            new MonitorExporterTransfers(monitorExporterUrl, monitorPrefix,
                                         monitorIndex, monitorUsername,
                                         monitorPwd, monitorToken,
                                         monitorApiKey, monitorIntervalIncluded,
                                         monitorTransformLongAsString,
                                         monitorCompression);
      }
      scheduleWithFixedDelay(monitorExporterTransfers, monitorExporterDelay,
                             TimeUnit.MILLISECONDS);
    }
  }

  /**
   * @param r66BusinessFactory the r66BusinessFactory to set
   */
  public final void setR66BusinessFactory(
      final R66BusinessFactoryInterface r66BusinessFactory) {
    this.r66BusinessFactory = r66BusinessFactory;
  }

  private static class CleanLruCache extends TimerTask {

    @Override
    public void run() {
      final int nb = DbTaskRunner.clearCache();
      logger.info("Clear Cache: " + nb);
    }

  }
}
