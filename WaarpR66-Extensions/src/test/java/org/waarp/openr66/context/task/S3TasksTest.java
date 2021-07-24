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

package org.waarp.openr66.context.task;

import io.netty.util.ResourceLeakDetector;
import org.junit.BeforeClass;
import org.junit.Test;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.extension.AbstractExtendedTaskFactory;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.xml.XMLTransferDAO;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket.TRANSFERMODE;
import org.waarp.openr66.s3.taskfactory.S3TaskFactory;
import org.waarp.openr66.s3.taskfactory.S3TaskFactory.S3TaskType;
import org.waarp.openr66.s3.util.MinioContainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * S3 Tasks Tester.
 */
public class S3TasksTest {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(S3TasksTest.class);
  private static final String ACCESS_KEY = "accessKey";
  private static final String SECRET_KEY = "secretKey";
  private static final String BUCKET = "bucket-test";
  private static final String FILEPATH11 = "/directory1/file1";
  private static final String argS3Get =
      "-URL %s -accessKey %s -secretKey %s -bucketName " +
      "%s -sourceName %s -file %s -getTags *";
  private static final String taskS3Get =
      "<tasks><task><type>" + S3TaskType.S3GET.name() + "</type><path>" +
      argS3Get + "</path><delay>0</delay><comment></comment></task></tasks>";
  private static final String argS3GetFilter =
      "-URL %s -accessKey %s -secretKey %s -bucketName " +
      "%s -sourceName %s -file %s -getTags %s";
  private static final String taskS3GetDelete =
      "<tasks><task><type>" + S3TaskType.S3GETDELETE.name() + "</type><path>" +
      argS3GetFilter +
      "</path><delay>0</delay><comment></comment></task></tasks>";
  private static final String argS3Put =
      "-URL %s -accessKey %s -secretKey %s -bucketName " +
      "%s -targetName %s -setTags %s";
  private static final String taskS3Put =
      "<tasks><task><type>" + S3TaskType.S3PUT.name() + "</type><path>" +
      argS3Put + "</path><delay>0</delay><comment></comment></task></tasks>";
  private static final String taskS3PutDelete =
      "<tasks><task><type>" + S3TaskType.S3PUTR66DELETE.name() +
      "</type><path>" + argS3Put +
      "</path><delay>0</delay><comment></comment></task></tasks>";
  private static final String argS3Delete =
      "-URL %s -accessKey %s -secretKey %s -bucketName " + "%s -sourceName %s";
  private static final String taskS3Delete =
      "<tasks><task><type>" + S3TaskType.S3DELETE.name() + "</type><path>" +
      argS3Delete + "</path><delay>0</delay><comment></comment></task></tasks>";

  // Should be automatic: private static final S3TaskFactory s3TaskFactory = new S3TaskFactory();
  private static S3TaskFactory s3TaskFactory;

  @BeforeClass
  public static void beforeClass() throws WaarpDatabaseException {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
    Collection<AbstractExtendedTaskFactory> collection =
        TaskType.getExtendedFactories();
    AbstractExtendedTaskFactory futureFactory = null;
    for (AbstractExtendedTaskFactory extendedTaskFactory : collection) {
      if (extendedTaskFactory instanceof S3TaskFactory) {
        futureFactory = extendedTaskFactory;
      }
    }
    s3TaskFactory = (S3TaskFactory) futureFactory;
    final String strTmp = System.getProperty("java.io.tmpdir");
    Configuration.configuration.setBaseDirectory(strTmp);
    File file = new File(strTmp + "/arch");
    file.mkdirs();
    Configuration.configuration.setArchivePath("/arch");
    Configuration.configuration.setAuthFile(strTmp + "/noAuth.xml");
    Configuration.configuration.setHostId("testServer");
    XMLTransferDAO.setNoFile(true);
    DAOFactory.initialize();
    DbRule rule = new DbRule(S3TaskType.S3GET.name(), null,
                             TRANSFERMODE.SENDMD5THROUGHMODE.ordinal(), "in",
                             "out", "arch", "work", null, null, null, taskS3Get,
                             null, null);
    rule.insert();
    rule = new DbRule(S3TaskType.S3GETDELETE.name(), null,
                      TRANSFERMODE.SENDMD5THROUGHMODE.ordinal(), "in", "out",
                      "arch", "work", null, null, null, taskS3GetDelete, null,
                      null);
    rule.insert();
    rule = new DbRule(S3TaskType.S3PUT.name(), null,
                      TRANSFERMODE.SENDMD5THROUGHMODE.ordinal(), "in", "out",
                      "arch", "work", null, taskS3Put, null, null, null, null);
    rule.insert();
    rule = new DbRule(S3TaskType.S3PUTR66DELETE.name(), null,
                      TRANSFERMODE.SENDMD5THROUGHMODE.ordinal(), "in", "out",
                      "arch", "work", null, taskS3PutDelete, null, null, null,
                      null);
    rule.insert();
    rule = new DbRule(S3TaskType.S3DELETE.name(), null,
                      TRANSFERMODE.SENDMD5THROUGHMODE.ordinal(), "in", "out",
                      "arch", "work", null, taskS3Delete, null, null, null,
                      null);
    rule.insert();
  }

  private File createTestFile() throws IOException {
    final File file = File.createTempFile("testSend", ".txt");
    final byte[] bytes = new byte[1024];
    Arrays.fill(bytes, (byte) 1);
    try (final FileOutputStream outputStream = new FileOutputStream(file)) {
      outputStream.write(bytes);
      outputStream.flush();
    }
    return file;
  }

  @Test
  public void testS3Tasks() throws Exception {
    try (final MinioContainer container = new MinioContainer(
        new MinioContainer.CredentialsProvider(ACCESS_KEY, SECRET_KEY))) {
      container.start();
      logger.warn("{} with {} : {}", container.getURL(), ACCESS_KEY,
                  SECRET_KEY);
      final File test = createTestFile();

      // First PUT
      String taskName = S3TaskType.S3PUT.name();
      final Transfer transfer = new Transfer();
      transfer.setFilename(test.getAbsolutePath());
      transfer.setOriginalName(test.getAbsolutePath());
      transfer.setRetrieveMode(true);
      transfer.setRule(taskName);
      transfer.setFileInfo(
          container.getURL() + " " + ACCESS_KEY + " " + SECRET_KEY + " " +
          BUCKET + " " + FILEPATH11 + " key1:value1,key2:value2" +
          " rest of info");
      final DbTaskRunner runner = new DbTaskRunner(transfer);
      runner.insert();
      final R66Session session = new R66Session(false);
      final LocalChannelReference lcr = new LocalChannelReference();
      lcr.setSendThroughMode(true);
      session.setNoSessionRunner(runner, lcr);

      AbstractTask task =
          s3TaskFactory.getTaskFromId(taskName, argS3Put, 0, session);
      task.run();
      assertTrue(task.isSuccess());
      assertTrue(test.canRead());
      assertTrue(lcr.isSendThroughMode());
      Map<String, Object> map =
          new HashMap<String, Object>(runner.getTransferMap());

      // Second GET
      taskName = S3TaskType.S3GET.name();
      final File testDest = File.createTempFile("testRecv", ".txt");
      lcr.setSendThroughMode(true);
      transfer.setFilename(test.getAbsolutePath());
      transfer.setOriginalName(test.getAbsolutePath());
      transfer.setRule(taskName);
      transfer.setFileInfo(
          container.getURL() + " " + ACCESS_KEY + " " + SECRET_KEY + " " +
          BUCKET + " " + FILEPATH11 + " " + testDest.getAbsolutePath() +
          " rest of info");
      session.setNoSessionRunner(runner, lcr);
      task = s3TaskFactory.getTaskFromId(taskName, argS3Get, 0, session);
      task.run();
      assertTrue(task.isSuccess());
      assertTrue(testDest.canRead());
      assertTrue(test.canRead());
      assertFalse(lcr.isSendThroughMode());
      assertEquals(runner.getFilename(), testDest.getAbsolutePath());
      Map<String, Object> map2 =
          new HashMap<String, Object>(runner.getTransferMap());
      assertEquals(map.size() + 2, map2.size());
      testDest.delete();

      // Third DELETE
      taskName = S3TaskType.S3DELETE.name();
      lcr.setSendThroughMode(true);
      transfer.setFilename(test.getAbsolutePath());
      transfer.setOriginalName(test.getAbsolutePath());
      transfer.setRule(taskName);
      transfer.setFileInfo(
          container.getURL() + " " + ACCESS_KEY + " " + SECRET_KEY + " " +
          BUCKET + " " + FILEPATH11 + " rest of info");
      session.setNoSessionRunner(runner, lcr);
      task = s3TaskFactory.getTaskFromId(taskName, argS3Delete, 0, session);
      task.run();
      assertTrue(task.isSuccess());
      assertTrue(test.canRead());
      assertTrue(lcr.isSendThroughMode());
      assertEquals(runner.getFilename(), test.getAbsolutePath());

      // Fourth PUT DELETE
      taskName = S3TaskType.S3PUTR66DELETE.name();
      transfer.setFilename(test.getAbsolutePath());
      transfer.setOriginalName(test.getAbsolutePath());
      transfer.setRetrieveMode(true);
      transfer.setRule(taskName);
      transfer.setFileInfo(
          container.getURL() + " " + ACCESS_KEY + " " + SECRET_KEY + " " +
          BUCKET + " " + FILEPATH11 + " key3:value3,key4:value4" +
          " rest of info");
      lcr.setSendThroughMode(true);
      session.setNoSessionRunner(runner, lcr);
      task = s3TaskFactory.getTaskFromId(taskName, argS3Put, 0, session);
      task.run();
      assertTrue(task.isSuccess());
      assertFalse(test.canRead());
      assertTrue(lcr.isSendThroughMode());
      map = new HashMap<String, Object>(runner.getTransferMap());

      // Fifth GET DELETE
      taskName = S3TaskType.S3GETDELETE.name();
      final File testDest2 = File.createTempFile("testRecv", ".txt");
      lcr.setSendThroughMode(true);
      transfer.setFilename(test.getAbsolutePath());
      transfer.setOriginalName(test.getAbsolutePath());
      transfer.setRule(taskName);
      transfer.setFileInfo(
          container.getURL() + " " + ACCESS_KEY + " " + SECRET_KEY + " " +
          BUCKET + " " + FILEPATH11 + " " + testDest2.getAbsolutePath() +
          " key3 rest of info");
      session.setNoSessionRunner(runner, lcr);
      task = s3TaskFactory.getTaskFromId(taskName, argS3GetFilter, 0, session);
      task.run();
      assertTrue(task.isSuccess());
      assertTrue(testDest2.canRead());
      assertFalse(test.canRead());
      assertFalse(lcr.isSendThroughMode());
      assertEquals(runner.getFilename(), testDest2.getAbsolutePath());
      map2 = new HashMap<String, Object>(runner.getTransferMap());
      logger.warn("{} {}", map, map2);
      assertEquals(1, map2.size());
      testDest2.delete();

      // Error cases

      // Retry GET shall raised an error
      taskName = S3TaskType.S3GET.name();
      lcr.setSendThroughMode(true);
      transfer.setFilename(test.getAbsolutePath());
      transfer.setOriginalName(test.getAbsolutePath());
      transfer.setRule(taskName);
      transfer.setFileInfo(
          container.getURL() + " " + ACCESS_KEY + " " + SECRET_KEY + " " +
          BUCKET + " " + FILEPATH11 + " " + testDest2.getAbsolutePath() +
          " rest of info");
      session.setNoSessionRunner(runner, lcr);
      task = s3TaskFactory.getTaskFromId(taskName, argS3Get, 0, session);
      task.run();
      assertFalse(task.isSuccess());
      assertFalse(test.canRead());
      assertFalse(testDest2.canRead());
      assertTrue(lcr.isSendThroughMode());
      assertEquals(runner.getFilename(), test.getAbsolutePath());

      // Wrong arguments
      task = s3TaskFactory.getTaskFromId(taskName, argS3Put, 0, session);
      task.run();
      assertFalse(task.isSuccess());
    } catch (final IOException e) {
      logger.error(e);
      fail(e.getMessage());
    }
  }

} 
