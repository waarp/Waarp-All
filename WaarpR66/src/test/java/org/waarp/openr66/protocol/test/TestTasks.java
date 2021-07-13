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
package org.waarp.openr66.protocol.test;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.ChModTask;
import org.waarp.openr66.context.task.CompressTask;
import org.waarp.openr66.context.task.CopyRenameTask;
import org.waarp.openr66.context.task.CopyTask;
import org.waarp.openr66.context.task.DeleteTask;
import org.waarp.openr66.context.task.ExecMoveTask;
import org.waarp.openr66.context.task.ExecOutputTask;
import org.waarp.openr66.context.task.ExecTask;
import org.waarp.openr66.context.task.FileCheckTask;
import org.waarp.openr66.context.task.FtpTransferTask;
import org.waarp.openr66.context.task.LinkRenameTask;
import org.waarp.openr66.context.task.LogTask;
import org.waarp.openr66.context.task.MoveRenameTask;
import org.waarp.openr66.context.task.MoveTask;
import org.waarp.openr66.context.task.RenameTask;
import org.waarp.openr66.context.task.RescheduleTransferTask;
import org.waarp.openr66.context.task.RestartServerTask;
import org.waarp.openr66.context.task.SnmpTask;
import org.waarp.openr66.context.task.TarTask;
import org.waarp.openr66.context.task.TranscodeTask;
import org.waarp.openr66.context.task.TransferTask;
import org.waarp.openr66.context.task.UnzeroedFileTask;
import org.waarp.openr66.context.task.ValidFilePathTask;
import org.waarp.openr66.context.task.ZipTask;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.context.task.javatask.AddDigestJavaTask;
import org.waarp.openr66.context.task.javatask.AddUuidJavaTask;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket.TRANSFERMODE;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * The object of this class is to test various tasks.
 */
public class TestTasks {

  /**
   * @param args
   */
  public static void main(String[] args) throws WaarpDatabaseSqlException {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    if (args.length < 4) {
      System.err.println(
          "Need config inDirectory outDirectory filename (in inDirectory)");
      return;
    }
    if (!FileBasedConfiguration
        .setSubmitClientConfigurationFromXml(Configuration.configuration,
                                             args[0])) {
      System.err
          .println("Needs a correct configuration file as first argument");
      return;
    }
    final String in = args[1];
    final String out = args[2];
    final String filename = args[3];
    final File file = new File(in + '/' + filename);
    final File out2 = new File(out + "/move");
    out2.mkdirs();
    final long size = file.length();
    final String argRule = out + "/#DATE#_%s_%s_" + filename;
    final String argTransfer = "basic information";
    final R66Session session = new R66Session(false);
    final DbRule rule =
        new DbRule("idRule", (String) null, TRANSFERMODE.SENDMODE.ordinal(),
                   out, null, null, in, null, null, null, null, null, null);
    final RequestPacket requestPacket =
        new RequestPacket(rule.getIdRule(), rule.getMode(), filename,
                          Configuration.BUFFERSIZEDEFAULT,
                          (int) size / Configuration.BUFFERSIZEDEFAULT + 1, 1,
                          argTransfer, size,
                          PartnerConfiguration.BAR_SEPARATOR_FIELD);
    DbTaskRunner runner = null;
    DbConstantR66.admin = new DbAdmin();
    session.getAuth().specialNoSessionAuth(false, "false");
    try {
      runner = new DbTaskRunner(session, rule, false, requestPacket);
    } catch (final WaarpDatabaseException ignored) {
    }
    runner.setPostTask();
    session.setBadRunner(runner, ErrorCode.QueryAlreadyFinished);

    // FILECHECK
    final FileCheckTask fileCheckTask =
        new FileCheckTask("SIZE LT 10000 SIZE GT 5 DFCHECK", 1, argTransfer,
                          session);
    fileCheckTask.run();
    fileCheckTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println(
        "FileCheckTask: " + fileCheckTask.getFutureCompletion().isSuccess());
    assertTrue("FileCheckTask should be OK",
               fileCheckTask.getFutureCompletion().isSuccess());

    // UnzeroedFileTask
    final UnzeroedFileTask unzeroedFileTask =
        new UnzeroedFileTask("", 0, argTransfer, session);
    unzeroedFileTask.run();
    unzeroedFileTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("UnzeroedFileTask: " +
                       unzeroedFileTask.getFutureCompletion().isSuccess());
    assertTrue("UnzeroedFileTask should be OK",
               unzeroedFileTask.getFutureCompletion().isSuccess());

    // CHMOD
    final ChModTask chModTask = new ChModTask("u=rw", 1, argTransfer, session);
    chModTask.run();
    chModTask.getFutureCompletion().awaitOrInterruptible();
    System.out
        .println("ChModTask: " + chModTask.getFutureCompletion().isSuccess());
    assertTrue("ChModTask should be OK",
               chModTask.getFutureCompletion().isSuccess());

    // SNMP
    final SnmpTask snmpTask = new SnmpTask(argRule, 0, argTransfer, session);
    snmpTask.run();
    snmpTask.getFutureCompletion().awaitOrInterruptible();
    System.out
        .println("SnmpTask: " + snmpTask.getFutureCompletion().isSuccess());
    assertTrue("SnmpTask should be OK",
               snmpTask.getFutureCompletion().isSuccess());

    // LOG
    LogTask logTask = new LogTask(argRule, 1, argTransfer, session);
    logTask.run();
    logTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("LOG: " + logTask.getFutureCompletion().isSuccess());
    assertTrue("LOG should be OK", logTask.getFutureCompletion().isSuccess());
    // LOG
    logTask =
        new LogTask(argRule + ' ' + out + "/log.txt", 3, argTransfer, session);
    logTask.run();
    logTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("LOG2: " + logTask.getFutureCompletion().isSuccess());
    assertTrue("LOG2 should be OK", logTask.getFutureCompletion().isSuccess());

    // ADDDIGEST
    final AddDigestJavaTask addDigestJavaTask = new AddDigestJavaTask();
    String arg = "-digest SHA512 -format -##DIGEST##";
    addDigestJavaTask
        .setArgs(session, true, false, 0, AddDigestJavaTask.class.getName(),
                 arg, false, false);
    addDigestJavaTask.run();
    System.out.println(
        "ADDDIGEST: " + addDigestJavaTask.getFinalStatus() + ":" +
        session.getRunner().getFileInformation());
    assertEquals("ADDIGEST should be OK", 0,
                 addDigestJavaTask.getFinalStatus());

    // UUID
    final AddUuidJavaTask addUuidJavaTask = new AddUuidJavaTask();
    arg = "-format -##UUID##";
    addUuidJavaTask
        .setArgs(session, true, false, 0, AddUuidJavaTask.class.getName(), arg,
                 false, false);
    addUuidJavaTask.run();
    System.out.println("ADDUUID: " + addUuidJavaTask.getFinalStatus() + ":" +
                       session.getRunner().getFileInformation());
    assertEquals("ADDUUID should be OK", 0, addUuidJavaTask.getFinalStatus());


    // COPYRENAME
    CopyRenameTask copyRenameTask =
        new CopyRenameTask(argRule + "_copyrename", 0, argTransfer, session);
    copyRenameTask.run();
    copyRenameTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println(
        "COPYRENAME: " + copyRenameTask.getFutureCompletion().isSuccess());
    assertTrue("COPYRENAME should be OK",
               copyRenameTask.getFutureCompletion().isSuccess());

    // COPY
    final CopyTask copyTask = new CopyTask(out, 0, argTransfer, session);
    copyTask.run();
    copyTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("COPY: " + copyTask.getFutureCompletion().isSuccess());
    assertTrue("COPY should be OK", copyTask.getFutureCompletion().isSuccess());

    // EXEC
    final ExecTask execTask =
        new ExecTask("ping -c 1 127.0.0.1", 10000, argTransfer, session);
    execTask.run();
    execTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("EXEC: " + execTask.getFutureCompletion().isSuccess());
    assertTrue("EXEC should be OK", execTask.getFutureCompletion().isSuccess());

    // EXECOUTPUT
    final ExecOutputTask execOutputTask =
        new ExecOutputTask("ping -c 1 127.0.0.1", 10000, argTransfer, session);
    execOutputTask.run();
    execOutputTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println(
        "EXECOUTPUT: " + execOutputTask.getFutureCompletion().isSuccess());
    assertTrue("EXECOUTPUT should be OK",
               execOutputTask.getFutureCompletion().isSuccess());

    // VALIDFILEPATH
    final ValidFilePathTask validFilePathTask =
        new ValidFilePathTask(in, 1, argTransfer, session);
    validFilePathTask.run();
    validFilePathTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("VALIDFILEPATH: " +
                       validFilePathTask.getFutureCompletion().isSuccess());
    assertTrue("VALIDFILEPATH should be OK",
               validFilePathTask.getFutureCompletion().isSuccess());

    // TAR
    TarTask tarTask =
        new TarTask(out + "/test.tar " + out, 2, argTransfer, session);
    tarTask.run();
    tarTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("TAR: " + tarTask.getFutureCompletion().isSuccess());
    assertTrue("TAR should be OK", tarTask.getFutureCompletion().isSuccess());

    tarTask = new TarTask(out + "/test.tar " + out + "/move", 1, argTransfer,
                          session);
    tarTask.run();
    tarTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("UNTAR: " + tarTask.getFutureCompletion().isSuccess());
    assertTrue("UNTAR should be OK", tarTask.getFutureCompletion().isSuccess());

    // ZIP
    ZipTask zipTask =
        new ZipTask(out + "/test.zip " + out, 2, argTransfer, session);
    zipTask.run();
    zipTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("ZIP: " + zipTask.getFutureCompletion().isSuccess());
    assertTrue("ZIP should be OK", zipTask.getFutureCompletion().isSuccess());

    zipTask = new ZipTask(out + "/test.zip " + out + "/move", 1, argTransfer,
                          session);
    zipTask.run();
    zipTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("UNZIP: " + zipTask.getFutureCompletion().isSuccess());
    assertTrue("UNZIP should be OK", zipTask.getFutureCompletion().isSuccess());

    // TRANSCODE
    final TranscodeTask transcodeTask =
        new TranscodeTask("-from UTF8 -to ISO-8859-15", 0, argTransfer,
                          session);
    transcodeTask.run();
    transcodeTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println(
        "TRANSCODE: " + transcodeTask.getFutureCompletion().isSuccess());
    assertTrue("TRANSCODE should be OK",
               transcodeTask.getFutureCompletion().isSuccess());

    // COMPRESS
    final CompressTask compressTask =
        new CompressTask(out + "/move.zstd", 0, argTransfer, session);
    compressTask.run();
    compressTask.getFutureCompletion().awaitOrInterruptible();
    System.out
        .println("COMPRESS: " + compressTask.getFutureCompletion().isSuccess());
    assertTrue("COMPRESS should be OK",
               compressTask.getFutureCompletion().isSuccess());
    final CompressTask decompressTask =
        new CompressTask(out + "/testTask.txt", 1, argTransfer, session);
    decompressTask.run();
    decompressTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println(
        "DECOMPRESS: " + decompressTask.getFutureCompletion().isSuccess());
    assertTrue("DECOMPRESS should be OK",
               decompressTask.getFutureCompletion().isSuccess());
    File file1 = new File(out + "/" + filename);
    try {
      FileUtils.copy(file1, file, false, false);
    } catch (Reply550Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
    }

    // MOVERENAME
    final MoveRenameTask moveReameTask =
        new MoveRenameTask(argRule + "_moverename", 0, argTransfer, session);
    moveReameTask.run();
    moveReameTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println(
        "MOVERENAME: " + moveReameTask.getFutureCompletion().isSuccess());
    assertTrue("MOVERENAME should be OK",
               moveReameTask.getFutureCompletion().isSuccess());

    try {
      session.setFileAfterPreRunner(false);
    } catch (final OpenR66RunnerErrorException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (final CommandAbstractException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    copyRenameTask =
        new CopyRenameTask(in + '/' + filename, 0, argTransfer, session);
    copyRenameTask.run();
    copyRenameTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println(
        "COPYRENAME: " + copyRenameTask.getFutureCompletion().isSuccess());
    assertTrue("COPYRENAME should be OK",
               copyRenameTask.getFutureCompletion().isSuccess());

    // MOVE
    final MoveTask moveTask =
        new MoveTask(out + "/move", 0, argTransfer, session);
    moveTask.run();
    moveTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("MOVE: " + moveTask.getFutureCompletion().isSuccess());
    assertTrue("MOVE should be OK", moveTask.getFutureCompletion().isSuccess());

    zipTask = new ZipTask(out + "/testx.zip " + out, 2, argTransfer, session);
    zipTask.run();
    zipTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("ZIP: " + zipTask.getFutureCompletion().isSuccess());
    assertTrue("ZIP should be OK", zipTask.getFutureCompletion().isSuccess());

    zipTask = new ZipTask(out + "/testx.zip " + out + "/move", 1, argTransfer,
                          session);
    zipTask.run();
    zipTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("UNZIP: " + zipTask.getFutureCompletion().isSuccess());
    assertTrue("UNZIP should be OK", zipTask.getFutureCompletion().isSuccess());

    // RENAME
    final RenameTask renameTask =
        new RenameTask(out + "/file_rename2", 0, argTransfer, session);
    renameTask.run();
    renameTask.getFutureCompletion().awaitOrInterruptible();
    System.out
        .println("RenameTask: " + renameTask.getFutureCompletion().isSuccess());
    assertTrue("RenameTask should be OK",
               renameTask.getFutureCompletion().isSuccess());

    // EXECMOVE
    File fileTemp = new File("/tmp/R66/out/file_rename3");
    try {
      fileTemp.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    final ExecMoveTask execMoveTask =
        new ExecMoveTask("echo /tmp/R66/out/file_rename3", 0, argTransfer,
                         session);
    execMoveTask.run();
    execMoveTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println(
        "ExecMoveTask: " + execMoveTask.getFutureCompletion().isSuccess());
    assertTrue("ExecMoveTask should be OK",
               execMoveTask.getFutureCompletion().isSuccess());

    // TRANSFER
    final TransferTask transferTask =
        new TransferTask("-file " + out + "/test.tar -to hostas -rule rule3", 0,
                         argTransfer, session);
    transferTask.run();
    transferTask.getFutureCompletion().awaitOrInterruptible();
    System.out
        .println("TRANSFER: " + transferTask.getFutureCompletion().isSuccess());
    assertTrue("TRANSFER should be OK",
               transferTask.getFutureCompletion().isSuccess());
    waitForAllDone(transferTask.getFutureCompletion().getResult().getRunner());

    // FTPTRANSFER
    final FtpTransferTask ftpTransferTask = new FtpTransferTask(
        "-file " + out + "/test.tar -to localhost -port 2022 " +
        " -user test -pwd test -account test -command put", 0, argTransfer,
        session);
    ftpTransferTask.run();
    ftpTransferTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println(
        "FTPTRANSFER: " + ftpTransferTask.getFutureCompletion().isSuccess());
    assertTrue("FTPTRANSFER should be KO (no connection)",
               ftpTransferTask.getFutureCompletion().isFailed());

    // LINKRENAME
    final LinkRenameTask linkRenameTask =
        new LinkRenameTask(out + "/linkrename", 0, argTransfer, session);
    linkRenameTask.run();
    linkRenameTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println(
        "LINKRENAME: " + linkRenameTask.getFutureCompletion().isSuccess());
    assertTrue("LINKRENAME should be OK",
               linkRenameTask.getFutureCompletion().isSuccess());

    // RESCHEDULETRANSFER
    final RescheduleTransferTask rescheduleTransferTask =
        new RescheduleTransferTask(
            "-delay 3600000 -case ConnectionImpossible,ServerOverloaded,Shutdown" +
            " -count 2" + " -notbetween H7:m0:S0;H19:m0:S0" +
            " -notbetween H1:m0:S0;H=3:m0:S0", 0, argTransfer, session);
    rescheduleTransferTask.run();
    rescheduleTransferTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println("RESCHEDULETRANSFER: " +
                       rescheduleTransferTask.getFutureCompletion()
                                             .isSuccess());
    assertTrue("RESCHEDULETRANSFER should be KO",
               rescheduleTransferTask.getFutureCompletion().isFailed());

    // DELETE
    final DeleteTask deleteTask =
        new DeleteTask(out + "/move", 0, argTransfer, session);
    deleteTask.run();
    deleteTask.getFutureCompletion().awaitOrInterruptible();
    System.out
        .println("DELETE: " + deleteTask.getFutureCompletion().isSuccess());
    assertTrue("DELETE should be OK",
               deleteTask.getFutureCompletion().isSuccess());

    // RESTART
    final RestartServerTask restartServerTask =
        new RestartServerTask("", 0, argTransfer, session);
    restartServerTask.run();
    restartServerTask.getFutureCompletion().awaitOrInterruptible();
    System.out.println(
        "RESTART: " + restartServerTask.getFutureCompletion().isSuccess());
    assertTrue("RESTART should be KO (unallowed)",
               restartServerTask.getFutureCompletion().isFailed());

  }

  private static void waitForAllDone(DbTaskRunner runner) {
    while (true) {
      try {
        runner.select();
        if (runner.isAllDone()) {
          SysErrLogger.FAKE_LOGGER.sysout("DbTaskRunner done");
          return;
        } else if (runner.isInError()) {
          SysErrLogger.FAKE_LOGGER.syserr("DbTaskRunner in error");
          return;
        }
        Thread.sleep(100);
      } catch (InterruptedException e) {//NOSONAR
        SysErrLogger.FAKE_LOGGER.syserr("Interrupted", e);
        return;
      } catch (WaarpDatabaseException e) {
        SysErrLogger.FAKE_LOGGER.syserr("Cannot found DbTaskRunner", e);
        return;
      }
    }
  }
}
