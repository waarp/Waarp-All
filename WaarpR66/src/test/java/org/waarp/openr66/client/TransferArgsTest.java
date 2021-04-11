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

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.openr66.client.utils.OutputFormat;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.junit.NetworkClientTest;
import org.waarp.openr66.protocol.junit.TestAbstract;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket;
import org.waarp.openr66.protocol.utils.R66Future;

import java.io.File;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;
import static org.waarp.common.database.DbConstant.*;
import static org.waarp.openr66.client.TransferArgs.*;

/**
 * AbstractTransfer Tester.
 */
public class TransferArgsTest extends TestAbstract {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "Linux/config/config-serverInitA.xml";
  private static final String CONFIG_CLIENT_A_XML = "config-clientA.xml";
  public static final String FOLLOWARGJSON = "{\"" + FOLLOW_JSON_KEY + "\":";

  /**
   * @throws Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ResourceLeakDetector.setLevel(Level.PARANOID);
    final ClassLoader classLoader = NetworkClientTest.class.getClassLoader();
    final File file =
        new File(classLoader.getResource("logback-test.xml").getFile());
    setUpBeforeClassMinimal(LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML);
    setUpDbBeforeClass();
    setUpBeforeClassServer(LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML,
                           CONFIG_SERVER_A_MINIMAL_XML, true);
    setUpBeforeClassClient(CONFIG_CLIENT_A_XML);
    final File totestBig = generateOutFile("/tmp/R66/out/testTaskBig.txt", 100);
  }

  /**
   * @throws Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.NONE));

    Thread.sleep(100);
    final DbHostAuth host = new DbHostAuth("hostas");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    final byte scode = -1;

    // Shutdown server
    logger.warn("Shutdown Server");
    Configuration.configuration.setTimeoutCon(100);
    final R66Future future = new R66Future(true);
    final ShutdownOrBlockJsonPacket node8 = new ShutdownOrBlockJsonPacket();
    node8.setRestartOrBlock(false);
    node8.setShutdownOrBlock(true);
    node8.setKey(FilesystemBasedDigest.passwdCrypt(
        Configuration.configuration.getServerAdminKey()));
    final AbstractLocalPacket valid =
        new JsonCommandPacket(node8, LocalPacketFactory.BLOCKREQUESTPACKET);
    sendInformation(valid, socketServerAddress, future, scode, false,
                    R66FiniteDualStates.SHUTDOWN, true);
    Thread.sleep(200);

    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
  }

  /**
   * Method: getParamsInternal(int rank, String[] args)
   */
  @Test
  public void testGetParamsInternal() throws Exception {
    // Wrong arguments
    TransferArgs transferArgs = AbstractTransfer.getParamsInternal(0, null);
    assertNull(transferArgs);
    transferArgs = AbstractTransfer.getParamsInternal(0, new String[0]);
    assertNull(transferArgs);
    String[] argsIncomplete = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG
    };
    transferArgs = AbstractTransfer.getParamsInternal(0, argsIncomplete);
    assertNull(transferArgs);
    String[] argsIncomplete2 = {
        TransferArgs.TO_ARG, "hosta"
    };
    transferArgs = AbstractTransfer.getParamsInternal(0, argsIncomplete2);
    assertNull(transferArgs);
    String[] argsWrongBlock = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.BLOCK_ARG, "123abc"
    };
    transferArgs = AbstractTransfer.getParamsInternal(0, argsWrongBlock);
    assertNull(transferArgs);
    String[] argsWrongDelay = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.DELAY_ARG, "123abc"
    };
    transferArgs = AbstractTransfer.getParamsInternal(0, argsWrongDelay);
    assertNull(transferArgs);
    String[] argsWrongDelayPlus = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.DELAY_ARG, "+123abc"
    };
    transferArgs = AbstractTransfer.getParamsInternal(0, argsWrongDelayPlus);
    assertNull(transferArgs);
    String[] argsWrongStart = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.START_ARG, "123abc"
    };
    transferArgs = AbstractTransfer.getParamsInternal(0, argsWrongStart);
    assertNull(transferArgs);
    String[] argsWrongId = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.ID_ARG, "123abc"
    };
    transferArgs = AbstractTransfer.getParamsInternal(0, argsWrongId);
    assertNull(transferArgs);
    String[] argsNotExistId = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.ID_ARG, "123456"
    };
    transferArgs = AbstractTransfer.getParamsInternal(0, argsNotExistId);
    assertNull(transferArgs);

    // Check complete example
    String[] argsComplete = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.HASH_ARG,
        TransferArgs.NO_FOLLOW_ARG, TransferArgs.BLOCK_ARG, "1000",
        TransferArgs.INFO_ARG, "no_information", "otherNotTaken"
    };
    transferArgs = AbstractTransfer.getParamsInternal(0, argsComplete);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertEquals("no_information", transferArgs.getTransferInfo());
    assertEquals(true, transferArgs.isMD5());
    assertEquals(1000, transferArgs.getBlockSize());
    assertNull(transferArgs.getStartTime());
    assertEquals(ILLEGALVALUE, transferArgs.getId());

    // Check example on outputs
    String[] argsCompleteWithFormatQuiet = {
        "FakeConfigFile", TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG,
        "testTaskBig.txt", TransferArgs.RULE_ARG, "rule3",
        TransferArgs.NO_FOLLOW_ARG, "-quiet", TransferArgs.INFO_ARG,
        "no_information", "otherNotTaken"
    };
    transferArgs =
        AbstractTransfer.getParamsInternal(1, argsCompleteWithFormatQuiet);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertEquals("no_information", transferArgs.getTransferInfo());
    assertNull(transferArgs.getStartTime());
    assertEquals(ILLEGALVALUE, transferArgs.getId());
    assertTrue(OutputFormat.isQuiet());
    String[] argsCompleteWithFormatXml = {
        "FakeConfigFile", TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG,
        "testTaskBig.txt", TransferArgs.RULE_ARG, "rule3",
        TransferArgs.NO_FOLLOW_ARG, "-xml", TransferArgs.INFO_ARG,
        "no_information", "otherNotTaken"
    };
    transferArgs =
        AbstractTransfer.getParamsInternal(1, argsCompleteWithFormatXml);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertEquals("no_information", transferArgs.getTransferInfo());
    assertNull(transferArgs.getStartTime());
    assertEquals(ILLEGALVALUE, transferArgs.getId());
    assertTrue(OutputFormat.isXml());
    String[] argsCompleteWithFormatCsv = {
        "FakeConfigFile", TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG,
        "testTaskBig.txt", TransferArgs.RULE_ARG, "rule3",
        TransferArgs.NO_FOLLOW_ARG, "-csv", TransferArgs.INFO_ARG,
        "no_information", "otherNotTaken"
    };
    transferArgs =
        AbstractTransfer.getParamsInternal(1, argsCompleteWithFormatCsv);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertEquals("no_information", transferArgs.getTransferInfo());
    assertNull(transferArgs.getStartTime());
    assertEquals(ILLEGALVALUE, transferArgs.getId());
    assertTrue(OutputFormat.isCsv());
    String[] argsCompleteWithFormatJson = {
        "FakeConfigFile", TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG,
        "testTaskBig.txt", TransferArgs.RULE_ARG, "rule3",
        TransferArgs.NO_FOLLOW_ARG, "-json", TransferArgs.INFO_ARG,
        "no_information", "otherNotTaken"
    };
    transferArgs =
        AbstractTransfer.getParamsInternal(1, argsCompleteWithFormatJson);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertEquals("no_information", transferArgs.getTransferInfo());
    assertNull(transferArgs.getStartTime());
    assertEquals(ILLEGALVALUE, transferArgs.getId());
    assertTrue(OutputFormat.isJson());
    String[] argsCompleteWithFormatProp = {
        "FakeConfigFile", TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG,
        "testTaskBig.txt", TransferArgs.RULE_ARG, "rule3",
        TransferArgs.NO_FOLLOW_ARG, "-property", TransferArgs.INFO_ARG,
        "no_information", "otherNotTaken"
    };
    transferArgs =
        AbstractTransfer.getParamsInternal(1, argsCompleteWithFormatProp);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertEquals("no_information", transferArgs.getTransferInfo());
    assertNull(transferArgs.getStartTime());
    assertEquals(ILLEGALVALUE, transferArgs.getId());
    assertTrue(OutputFormat.isProperty());

    // Check complete example
    String[] argsCompleteSeparator = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.HASH_ARG,
        TransferArgs.NO_FOLLOW_ARG, TransferArgs.BLOCK_ARG, "1000",
        TransferArgs.INFO_ARG, "no_information", TransferArgs.SEPARATOR_SEND,
        TransferArgs.DELAY_ARG, "abc"
    };
    transferArgs = AbstractTransfer.getParamsInternal(0, argsCompleteSeparator);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertEquals("no_information", transferArgs.getTransferInfo());
    assertEquals(true, transferArgs.isMD5());
    assertEquals(1000, transferArgs.getBlockSize());
    assertNull(transferArgs.getStartTime());
    assertEquals(ILLEGALVALUE, transferArgs.getId());

    String[] argsCompleteRankNot0 = {
        TransferArgs.SEPARATOR_SEND, TransferArgs.DELAY_ARG, "abc",
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.HASH_ARG,
        TransferArgs.NO_FOLLOW_ARG, TransferArgs.BLOCK_ARG, "1000",
        TransferArgs.INFO_ARG, "no_information"
    };
    transferArgs = AbstractTransfer.getParamsInternal(3, argsCompleteRankNot0);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertEquals("no_information", transferArgs.getTransferInfo());
    assertEquals(true, transferArgs.isMD5());
    assertEquals(1000, transferArgs.getBlockSize());
    assertNull(transferArgs.getStartTime());
    assertEquals(ILLEGALVALUE, transferArgs.getId());

    String[] argsCompleteAlias = {
        TransferArgs.TO_ARG, "myhosta", TransferArgs.FILE_ARG,
        "testTaskBig.txt", TransferArgs.RULE_ARG, "rule3",
        TransferArgs.HASH_ARG, TransferArgs.BLOCK_ARG, "1000",
        TransferArgs.INFO_ARG, "no_information", "otherNotTaken"
    };
    transferArgs = AbstractTransfer.getParamsInternal(0, argsCompleteAlias);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertTrue(transferArgs.getTransferInfo().startsWith("no_information {"));
    assertEquals(true, transferArgs.isMD5());
    assertEquals(1000, transferArgs.getBlockSize());
    assertNull(transferArgs.getStartTime());
    assertEquals(ILLEGALVALUE, transferArgs.getId());

    // Check complete example
    String[] argsCompleteWithId = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.HASH_ARG,
        TransferArgs.BLOCK_ARG, "1000", TransferArgs.ID_ARG, "1234",
        TransferArgs.INFO_ARG, "no_information", "otherNotTaken"
    };
    transferArgs = AbstractTransfer.getParamsInternal(0, argsCompleteWithId);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertTrue(transferArgs.getTransferInfo().startsWith("no_information {"));
    assertEquals(true, transferArgs.isMD5());
    assertEquals(1000, transferArgs.getBlockSize());
    assertNull(transferArgs.getStartTime());
    assertEquals(1234, transferArgs.getId());

    // Check complete example
    String[] argsCompleteWithTimeStart = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.HASH_ARG,
        TransferArgs.BLOCK_ARG, "1000", TransferArgs.START_ARG,
        "20200601211201", TransferArgs.INFO_ARG, "no_information",
        "otherNotTaken"
    };
    transferArgs =
        AbstractTransfer.getParamsInternal(0, argsCompleteWithTimeStart);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertTrue(transferArgs.getTransferInfo().startsWith("no_information {"));
    assertEquals(true, transferArgs.isMD5());
    assertEquals(1000, transferArgs.getBlockSize());
    Date date;
    final SimpleDateFormat dateFormat =
        new SimpleDateFormat(AbstractTransfer.TIMESTAMP_FORMAT);
    try {
      date = dateFormat.parse("20200601211201");
      assertTrue(
          transferArgs.getStartTime().equals(new Timestamp(date.getTime())));
    } catch (final ParseException ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
      fail(ignored.getMessage());
    }
    assertEquals(ILLEGALVALUE, transferArgs.getId());

    // Check complete example
    String[] argsCompleteWithDelay = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.HASH_ARG,
        TransferArgs.NOTLOGWARN_ARG, TransferArgs.BLOCK_ARG, "1000",
        TransferArgs.NO_FOLLOW_ARG, TransferArgs.DELAY_ARG, "+100",
        TransferArgs.INFO_ARG, "no_information", "otherNotTaken"
    };
    transferArgs = AbstractTransfer.getParamsInternal(0, argsCompleteWithDelay);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertEquals("no_information", transferArgs.getTransferInfo());
    assertEquals(true, transferArgs.isMD5());
    assertEquals(1000, transferArgs.getBlockSize());
    assertTrue(transferArgs.getStartTime().before(
        new Timestamp(System.currentTimeMillis() + 101)));
    assertEquals(ILLEGALVALUE, transferArgs.getId());
    assertFalse(transferArgs.isNolog());
    assertFalse(transferArgs.isNormalInfoAsWarn());

    // Check complete example
    String[] argsCompleteWithNotLog = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.HASH_ARG,
        TransferArgs.BLOCK_ARG, "1000", TransferArgs.NOTLOG_ARG,
        TransferArgs.LOGWARN_ARG, TransferArgs.INFO_ARG, "no_information",
        "otherNotTaken"
    };
    transferArgs =
        AbstractTransfer.getParamsInternal(0, argsCompleteWithNotLog);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertTrue(transferArgs.getTransferInfo().startsWith("no_information {"));
    assertEquals(true, transferArgs.isMD5());
    assertEquals(1000, transferArgs.getBlockSize());
    assertTrue(transferArgs.isNolog());
    assertTrue(transferArgs.isNormalInfoAsWarn());
    assertEquals(ILLEGALVALUE, transferArgs.getId());
    assertNotNull(transferArgs.getFollowId());

    // Check complete example
    String[] argsCompleteWithFollow = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.HASH_ARG,
        TransferArgs.BLOCK_ARG, "1000", TransferArgs.INFO_ARG, "no_information",
        "otherNotTaken"
    };
    transferArgs =
        AbstractTransfer.getParamsInternal(0, argsCompleteWithFollow);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    logger.warn(transferArgs.getTransferInfo());
    assertTrue(transferArgs.getTransferInfo().startsWith("no_information {"));
    assertTrue(transferArgs.getTransferInfo().contains(FOLLOW_JSON_KEY));
    assertEquals(true, transferArgs.isMD5());
    assertEquals(1000, transferArgs.getBlockSize());
    assertFalse(transferArgs.isNolog());
    assertTrue(transferArgs.isNormalInfoAsWarn());
    assertEquals(ILLEGALVALUE, transferArgs.getId());
    assertFalse(transferArgs.getFollowId().isEmpty());

    // Check complete example
    String[] argsCompleteWithFollowIncluded = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.HASH_ARG,
        TransferArgs.BLOCK_ARG, "1000", TransferArgs.INFO_ARG, "no_information",
        FOLLOWARGJSON, "1234}"
    };
    transferArgs = TransferArgs
        .getParamsInternal(0, argsCompleteWithFollowIncluded, false);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertEquals("no_information", transferArgs.getTransferInfo());
    assertEquals(true, transferArgs.isMD5());
    assertEquals(1000, transferArgs.getBlockSize());
    assertFalse(transferArgs.isNolog());
    assertTrue(transferArgs.isNormalInfoAsWarn());
    assertEquals(ILLEGALVALUE, transferArgs.getId());
    assertTrue(transferArgs.getFollowId().isEmpty());
    assertNotEquals("1234", transferArgs.getFollowId());
    TransferArgs
        .getAllInfo(transferArgs, 0, argsCompleteWithFollowIncluded, null);
    logger.warn(transferArgs.getTransferInfo());
    assertEquals("1234", transferArgs.getFollowId());
    assertTrue(transferArgs.getTransferInfo().startsWith("no_information {"));
    assertTrue(transferArgs.getTransferInfo().contains(FOLLOW_JSON_KEY));

    String[] argsCompleteWithFollowIncludedAndMore = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.HASH_ARG,
        TransferArgs.BLOCK_ARG, "1000", TransferArgs.INFO_ARG, "no_information",
        FOLLOWARGJSON, "1234} {'key': 'value'} test after"
    };
    transferArgs = TransferArgs
        .getParamsInternal(0, argsCompleteWithFollowIncludedAndMore, false);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertEquals("no_information", transferArgs.getTransferInfo());
    assertEquals(true, transferArgs.isMD5());
    assertEquals(1000, transferArgs.getBlockSize());
    assertFalse(transferArgs.isNolog());
    assertTrue(transferArgs.isNormalInfoAsWarn());
    assertEquals(ILLEGALVALUE, transferArgs.getId());
    assertTrue(transferArgs.getFollowId().isEmpty());
    assertNotEquals("1234", transferArgs.getFollowId());
    TransferArgs
        .getAllInfo(transferArgs, 0, argsCompleteWithFollowIncludedAndMore,
                    null);
    logger.warn(transferArgs.getTransferInfo());
    String compare = "no_information " + FOLLOWARGJSON + " 1234}" +
                     " {'key': 'value'} test after";
    assertEquals("1234", transferArgs.getFollowId());
    assertTrue(transferArgs.getTransferInfo().startsWith(compare));
    assertTrue(transferArgs.getTransferInfo().contains(FOLLOW_JSON_KEY));
    logger.warn("{}",
                DbTaskRunner.getMapFromString(transferArgs.getTransferInfo()));
    assertTrue(DbTaskRunner.getMapFromString(transferArgs.getTransferInfo())
                           .containsKey("key"));
    assertTrue(DbTaskRunner.getMapFromString(transferArgs.getTransferInfo())
                           .containsKey(FOLLOW_JSON_KEY));

    String[] argsCompleteWithFollowCopied = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.HASH_ARG,
        TransferArgs.BLOCK_ARG, "1000", TransferArgs.INFO_ARG, "no_information",
        "extra-info"
    };
    transferArgs =
        TransferArgs.getParamsInternal(0, argsCompleteWithFollowCopied, false);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertEquals("no_information", transferArgs.getTransferInfo());
    assertEquals(true, transferArgs.isMD5());
    assertEquals(1000, transferArgs.getBlockSize());
    assertFalse(transferArgs.isNolog());
    assertTrue(transferArgs.isNormalInfoAsWarn());
    assertEquals(ILLEGALVALUE, transferArgs.getId());
    assertTrue(transferArgs.getFollowId().isEmpty());
    assertNotEquals("1234", transferArgs.getFollowId());
    TransferArgs.getAllInfo(transferArgs, 0, argsCompleteWithFollowCopied,
                            FOLLOWARGJSON + " 1234}");
    logger.warn(transferArgs.getTransferInfo());
    assertEquals("1234", transferArgs.getFollowId());
    assertTrue(transferArgs.getTransferInfo().startsWith(
        FOLLOWARGJSON + " 1234} no_information extra-info"));
    assertTrue(transferArgs.getTransferInfo().contains(FOLLOW_JSON_KEY));

    String[] argsCompleteWithFollowCopied2 = {
        TransferArgs.TO_ARG, "hosta", TransferArgs.FILE_ARG, "testTaskBig.txt",
        TransferArgs.RULE_ARG, "rule3", TransferArgs.HASH_ARG,
        TransferArgs.BLOCK_ARG, "1000", TransferArgs.INFO_ARG, "no_information",
        "extra-info"
    };
    transferArgs =
        TransferArgs.getParamsInternal(0, argsCompleteWithFollowCopied2, false);
    assertNotNull(transferArgs);
    assertEquals("hosta", transferArgs.getRemoteHost());
    assertEquals("testTaskBig.txt", transferArgs.getFilename());
    assertEquals("rule3", transferArgs.getRulename());
    assertEquals("no_information", transferArgs.getTransferInfo());
    assertEquals(true, transferArgs.isMD5());
    assertEquals(1000, transferArgs.getBlockSize());
    assertFalse(transferArgs.isNolog());
    assertTrue(transferArgs.isNormalInfoAsWarn());
    assertEquals(ILLEGALVALUE, transferArgs.getId());
    assertTrue(transferArgs.getFollowId().isEmpty());
    assertNotEquals("1234", transferArgs.getFollowId());
    TransferArgs.getAllInfo(transferArgs, 0, argsCompleteWithFollowCopied2,
                            FOLLOWARGJSON + " 1234} other {'test': 'test2'}");
    logger.warn(transferArgs.getTransferInfo());
    assertEquals("1234", transferArgs.getFollowId());
    assertTrue(transferArgs.getTransferInfo().startsWith(
        FOLLOWARGJSON + " 1234} other {'test': 'test2'} no_information " +
        "extra-info"));
    assertTrue(transferArgs.getTransferInfo().contains(FOLLOW_JSON_KEY));
  }
} 
