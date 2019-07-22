/*******************************************************************************
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.waarp.openr66.protocol.test;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.ChModTask;
import org.waarp.openr66.context.task.CopyRenameTask;
import org.waarp.openr66.context.task.CopyTask;
import org.waarp.openr66.context.task.DeleteTask;
import org.waarp.openr66.context.task.ExecOutputTask;
import org.waarp.openr66.context.task.ExecTask;
import org.waarp.openr66.context.task.FileCheckTask;
import org.waarp.openr66.context.task.LogTask;
import org.waarp.openr66.context.task.MoveRenameTask;
import org.waarp.openr66.context.task.MoveTask;
import org.waarp.openr66.context.task.RenameTask;
import org.waarp.openr66.context.task.SnmpTask;
import org.waarp.openr66.context.task.TarTask;
import org.waarp.openr66.context.task.TranscodeTask;
import org.waarp.openr66.context.task.UnzeroedFileTask;
import org.waarp.openr66.context.task.ValidFilePathTask;
import org.waarp.openr66.context.task.ZipTask;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket.TRANSFERMODE;

import java.io.File;

import static org.junit.Assert.*;

/**
 * The object of this class is to test various tasks.
 *
 * @author "Frederic Bregier"
 */
public class TestTasks {

  /**
   * @param args
   */
  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(
        WaarpLogLevel.WARN));
    if (args.length < 4) {
      System.err.println(
          "Need config inDirectory outDirectory filename (in inDirectory)");
      return;
    }
    if (!FileBasedConfiguration
        .setSubmitClientConfigurationFromXml(Configuration.configuration, args[0])) {
      System.err
          .println("Needs a correct configuration file as first argument");
      return;
    }
    String in = args[1];
    String out = args[2];
    String filename = args[3];
    File file = new File(in + "/" + filename);
    File out2 = new File(out + "/move");
    out2.mkdirs();
    long size = file.length();
    String argRule = out + "/#DATE#_%s_%s_" + filename;
    String argTransfer = "basic information";
    R66Session session = new R66Session();
    DbRule rule =
        new DbRule("idRule", (String) null, TRANSFERMODE.SENDMODE.ordinal(),
                   out, null, null, in,
                   null, null, null, null, null, null);
    RequestPacket requestPacket =
        new RequestPacket(rule.getIdRule(), rule.getMode(), filename,
                          Configuration.BUFFERSIZEDEFAULT,
                          (int) size / Configuration.BUFFERSIZEDEFAULT + 1,
                          1, argTransfer, size,
                          PartnerConfiguration.BAR_SEPARATOR_FIELD);
    DbTaskRunner runner = null;
    DbConstant.admin = new DbAdmin();
    session.getAuth().specialNoSessionAuth(false, "false");
    try {
      runner = new DbTaskRunner(session, rule, false, requestPacket);
    } catch (WaarpDatabaseException e) {
    }
    runner.setPostTask();
    session.setBadRunner(runner, ErrorCode.QueryAlreadyFinished);

        // FILECHECK
        FileCheckTask fileCheckTask =
                new FileCheckTask("SIZE LT 10000 SIZE GT 5 DFCHECK", 1,
                                  argTransfer, session);
        fileCheckTask.run();
        try {
            fileCheckTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println("FileCheckTask: " +
                           fileCheckTask.getFutureCompletion().isSuccess());
        assertEquals("FileCheckTask should be OK", true,
                     fileCheckTask.getFutureCompletion().isSuccess());

        // UnzeroedFileTask
        UnzeroedFileTask unzeroedFileTask =
                new UnzeroedFileTask("", 0, argTransfer, session);
        unzeroedFileTask.run();
        try {
            unzeroedFileTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println("UnzeroedFileTask: " +
                           unzeroedFileTask.getFutureCompletion().isSuccess());
        assertEquals("UnzeroedFileTask should be OK", true,
                     unzeroedFileTask.getFutureCompletion().isSuccess());

        // CHMOD
        ChModTask chModTask = new ChModTask("u=rw", 1, argTransfer, session);
        chModTask.run();
        try {
            chModTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println(
                "ChModTask: " + chModTask.getFutureCompletion().isSuccess());
        assertEquals("ChModTask should be OK", true,
                     chModTask.getFutureCompletion().isSuccess());

        // SNMP
        SnmpTask snmpTask = new SnmpTask(argRule, 0, argTransfer, session);
        snmpTask.run();
        try {
            snmpTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println(
                "SnmpTask: " + snmpTask.getFutureCompletion().isSuccess());
        assertEquals("SnmpTask should be OK", true,
                     snmpTask.getFutureCompletion().isSuccess());

        // LOG
        LogTask logTask = new LogTask(argRule, 1, argTransfer, session);
        logTask.run();
        try {
            logTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println("LOG: " + logTask.getFutureCompletion().isSuccess());
        assertEquals("LOG should be OK", true,
                     logTask.getFutureCompletion().isSuccess());
        // LOG
        logTask = new LogTask(argRule + " " + out + "/log.txt", 3, argTransfer,
                              session);
        logTask.run();
        try {
            logTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out
                .println("LOG2: " + logTask.getFutureCompletion().isSuccess());
        assertEquals("LOG2 should be OK", true,
                     logTask.getFutureCompletion().isSuccess());

        // COPYRENAME
        CopyRenameTask copyRenameTask =
                new CopyRenameTask(argRule + "_copyrename", 0, argTransfer,
                                   session);
        copyRenameTask.run();
        try {
            copyRenameTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println("COPYRENAME: " +
                           copyRenameTask.getFutureCompletion().isSuccess());
        assertEquals("COPYRENAME should be OK", true,
                     copyRenameTask.getFutureCompletion().isSuccess());

        // COPY
        CopyTask copyTask = new CopyTask(out, 0, argTransfer, session);
        copyTask.run();
        try {
            copyTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out
                .println("COPY: " + copyTask.getFutureCompletion().isSuccess());
        assertEquals("COPY should be OK", true,
                     copyTask.getFutureCompletion().isSuccess());

        // EXEC
        ExecTask execTask =
                new ExecTask("ping -c 1 127.0.0.1", 10000, argTransfer,
                             session);
        execTask.run();
        try {
            execTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out
                .println("EXEC: " + execTask.getFutureCompletion().isSuccess());
        assertEquals("EXEC should be OK", true,
                     execTask.getFutureCompletion().isSuccess());

        // EXECOUTPUT
        ExecOutputTask execOutputTask =
                new ExecOutputTask("ping -c 1 127.0.0.1", 10000, argTransfer,
                                   session);
        execOutputTask.run();
        try {
            execOutputTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println("EXECOUTPUT: " +
                           execOutputTask.getFutureCompletion().isSuccess());
        assertEquals("EXECOUTPUT should be OK", true,
                     execOutputTask.getFutureCompletion().isSuccess());

        // VALIDFILEPATH
        ValidFilePathTask validFilePathTask =
                new ValidFilePathTask(in, 1, argTransfer, session);
        validFilePathTask.run();
        try {
            validFilePathTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println("VALIDFILEPATH: " +
                           validFilePathTask.getFutureCompletion().isSuccess());
        assertEquals("VALIDFILEPATH should be OK", true,
                     validFilePathTask.getFutureCompletion().isSuccess());

        // TAR
        TarTask tarTask =
                new TarTask(out + "/test.tar " + out, 2, argTransfer, session);
        tarTask.run();
        try {
            tarTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println("TAR: " + tarTask.getFutureCompletion().isSuccess());
        assertEquals("TAR should be OK", true,
                     tarTask.getFutureCompletion().isSuccess());

        tarTask =
                new TarTask(out + "/test.tar " + out + "/move", 1, argTransfer,
                            session);
        tarTask.run();
        try {
            tarTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out
                .println("UNTAR: " + tarTask.getFutureCompletion().isSuccess());
        assertEquals("UNTAR should be OK", true,
                     tarTask.getFutureCompletion().isSuccess());

        // ZIP
        ZipTask zipTask =
                new ZipTask(out + "/test.zip " + out, 2, argTransfer, session);
        zipTask.run();
        try {
            zipTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println("ZIP: " + zipTask.getFutureCompletion().isSuccess());
        assertEquals("ZIP should be OK", true,
                     zipTask.getFutureCompletion().isSuccess());

        zipTask =
                new ZipTask(out + "/test.zip " + out + "/move", 1, argTransfer,
                            session);
        zipTask.run();
        try {
            zipTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out
                .println("UNZIP: " + zipTask.getFutureCompletion().isSuccess());
        assertEquals("UNZIP should be OK", true,
                     zipTask.getFutureCompletion().isSuccess());

        // TRANSCODE
        TranscodeTask transcodeTask =
                new TranscodeTask("-from UTF8 -to ISO-8859-15", 0, argTransfer,
                                  session);
        transcodeTask.run();
        try {
            transcodeTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println("TRANSCODE: " +
                           transcodeTask.getFutureCompletion().isSuccess());
        assertEquals("TRANSCODE should be OK", true,
                     transcodeTask.getFutureCompletion().isSuccess());

        // MOVERENAME
        MoveRenameTask moveReameTask =
                new MoveRenameTask(argRule + "_moverename", 0, argTransfer,
                                   session);
        moveReameTask.run();
        try {
            moveReameTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println("MOVERENAME: " +
                           moveReameTask.getFutureCompletion().isSuccess());
        assertEquals("MOVERENAME should be OK", true,
                     moveReameTask.getFutureCompletion().isSuccess());

      try {
          session.setFileAfterPreRunner(false);
      } catch (OpenR66RunnerErrorException e) {
          e.printStackTrace();
          fail(e.getMessage());
      } catch (CommandAbstractException e) {
          e.printStackTrace();
          fail(e.getMessage());
      }
      copyRenameTask = new CopyRenameTask(in + "/" + filename, 0, argTransfer,
                                            session);
        copyRenameTask.run();
        try {
            copyRenameTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println("COPYRENAME: " +
                           copyRenameTask.getFutureCompletion().isSuccess());
        assertEquals("COPYRENAME should be OK", true,
                     copyRenameTask.getFutureCompletion().isSuccess());

        // MOVE
        MoveTask moveTask =
                new MoveTask(out + "/move", 0, argTransfer, session);
        moveTask.run();
        try {
            moveTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out
                .println("MOVE: " + moveTask.getFutureCompletion().isSuccess());
        assertEquals("MOVE should be OK", true,
                     moveTask.getFutureCompletion().isSuccess());

        zipTask =
                new ZipTask(out + "/testx.zip " + out, 2, argTransfer, session);
        zipTask.run();
        try {
            zipTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println("ZIP: " + zipTask.getFutureCompletion().isSuccess());
        assertEquals("ZIP should be OK", true,
                     zipTask.getFutureCompletion().isSuccess());

        zipTask =
                new ZipTask(out + "/testx.zip " + out + "/move", 1, argTransfer,
                            session);
        zipTask.run();
        try {
            zipTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out
                .println("UNZIP: " + zipTask.getFutureCompletion().isSuccess());
        assertEquals("UNZIP should be OK", true,
                     zipTask.getFutureCompletion().isSuccess());

        // RENAME
        RenameTask renameTask =
                new RenameTask(out + "_rename2", 0, argTransfer, session);
        renameTask.run();
        try {
            renameTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println(
                "RenameTask: " + renameTask.getFutureCompletion().isSuccess());
        assertEquals("RenameTask should be OK", true,
                     renameTask.getFutureCompletion().isSuccess());

        // DELETE
        DeleteTask deleteTask =
                new DeleteTask(out + "/move", 0, argTransfer, session);
        deleteTask.run();
        try {
            deleteTask.getFutureCompletion().await();
        } catch (InterruptedException e) {
        }
        System.out.println(
                "DELETE: " + deleteTask.getFutureCompletion().isSuccess());
        assertEquals("DELETE should be OK", true,
                     deleteTask.getFutureCompletion().isSuccess());
    }

}
