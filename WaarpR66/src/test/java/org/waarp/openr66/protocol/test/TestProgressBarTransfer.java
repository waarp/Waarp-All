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
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.client.ProgressBarTransfer;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.R66Future;

/**
 *
 */
public class TestProgressBarTransfer extends ProgressBarTransfer {

  /**
   * @param future
   * @param remoteHost
   * @param filename
   * @param rulename
   * @param fileinfo
   * @param isMD5
   * @param blocksize
   * @param id
   * @param networkTransaction
   * @param callbackdelay
   */
  public TestProgressBarTransfer(R66Future future, String remoteHost,
                                 String filename, String rulename,
                                 String fileinfo, boolean isMD5, int blocksize,
                                 long id, NetworkTransaction networkTransaction,
                                 long callbackdelay) {
    super(future, remoteHost, filename, rulename, fileinfo, isMD5, blocksize,
          id, networkTransaction, callbackdelay);
  }

  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(null));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ProgressBarTransfer.class);
    }
    if (!getParams(args, false)) {
      logger.error("Wrong initialization");
      if (DbConstantR66.admin != null) {
        DbConstantR66.admin.close();
      }
      if (WaarpSystemUtil.isJunit()) {
        return;
      }
      WaarpSystemUtil.stopLogger(false);
      WaarpSystemUtil.systemExit(2);
      return;
    }
    final long time1 = System.currentTimeMillis();
    final R66Future future = new R66Future(true);

    Configuration.configuration.pipelineInit();
    final NetworkTransaction networkTransaction = new NetworkTransaction();
    try {
      final TestProgressBarTransfer transaction =
          new TestProgressBarTransfer(future, rhost, localFilename, rule,
                                      transferInfo, ismd5, block, idt,
                                      networkTransaction, 100);
      transaction.normalInfoAsWarn = snormalInfoAsWarn;
      transaction.run();
      future.awaitOrInterruptible();
      final long time2 = System.currentTimeMillis();
      final long delay = time2 - time1;
      final R66Result result = future.getResult();
      if (future.isSuccess()) {
        if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
          logger.warn("Transfer in status: WARNED     " +
                      result.getRunner().toShortString() + "     <REMOTE>" +
                      rhost + "</REMOTE>" + "     <FILEFINAL>" +
                      (result.getFile() != null?
                          result.getFile() + "</FILEFINAL>" : "no file") +
                      "     delay: " + delay);
        } else {
          logger.info("Transfer in status: SUCCESS     " +
                      result.getRunner().toShortString() + "     <REMOTE>" +
                      rhost + "</REMOTE>" + "     <FILEFINAL>" +
                      (result.getFile() != null?
                          result.getFile() + "</FILEFINAL>" : "no file") +
                      "     delay: " + delay);
        }
        if (nolog) {
          // In case of success, delete the runner
          try {
            result.getRunner().delete();
          } catch (final WaarpDatabaseException e) {
            logger.warn("Cannot apply nolog to     " +
                        result.getRunner().toShortString() + " : {}",
                        e.getMessage());
          }
        }
      } else {
        if (result == null || result.getRunner() == null) {
          logger.error("Transfer in     FAILURE with no Id", future.getCause());
          networkTransaction.closeAll();
          WaarpSystemUtil.systemExit(ErrorCode.Unknown.ordinal());
          return;
        }
        if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
          logger.warn("Transfer is     WARNED     " +
                      result.getRunner().toShortString() + "     <REMOTE>" +
                      rhost + "</REMOTE>" + " : {}", future.getCause() != null?
                          future.getCause().getMessage() : "");
          networkTransaction.closeAll();
          WaarpSystemUtil.systemExit(result.getCode().ordinal());
        } else {
          logger.error("Transfer in     FAILURE     " +
                       result.getRunner().toShortString() + "     <REMOTE>" +
                       rhost + "</REMOTE>", future.getCause());
          networkTransaction.closeAll();
          WaarpSystemUtil.systemExit(result.getCode().ordinal());
        }
      }
    } finally {
      networkTransaction.closeAll();
      // In case something wrong append
      if (future.isDone() && future.isSuccess()) {
        WaarpSystemUtil.systemExit(0);
      } else {
        WaarpSystemUtil.systemExit(66);
      }
    }
  }

  @Override
  public void callBack(int currentBlock, int blocksize) {
    if (filesize == 0) {
      System.err.println("Block: " + currentBlock + " BSize: " + blocksize);
    } else {
      System.err.println(
          "Block: " + currentBlock + " BSize: " + blocksize + " on " +
          (int) Math.ceil((double) filesize / (double) blocksize));
    }
  }

  @Override
  public void lastCallBack(boolean success, int currentBlock, int blocksize) {
    if (filesize == 0) {
      System.err.println(
          "Status: " + success + " Block: " + currentBlock + " BSize: " +
          blocksize);
    } else {
      System.err.println(
          "Status: " + success + " Block: " + currentBlock + " BSize: " +
          blocksize + " Size=" + filesize);
    }
  }

}
