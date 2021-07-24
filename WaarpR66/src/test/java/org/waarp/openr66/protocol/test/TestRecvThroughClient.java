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
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.client.NoOpRecvThroughHandler;
import org.waarp.openr66.client.RecvThroughClient;
import org.waarp.openr66.client.TransferArgs;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.R66Future;

/**
 * Test class for Recv Through client
 */
public class TestRecvThroughClient extends RecvThroughClient {
  /**
   * @param future
   * @param remoteHost
   * @param filename
   * @param rulename
   * @param fileinfo
   * @param isMD5
   * @param blocksize
   * @param networkTransaction
   */
  public TestRecvThroughClient(R66Future future, NoOpRecvThroughHandler handler,
                               String remoteHost, String filename,
                               String rulename, String fileinfo, boolean isMD5,
                               int blocksize,
                               NetworkTransaction networkTransaction) {
    super(future, handler, remoteHost, filename, rulename, fileinfo, isMD5,
          blocksize, DbConstantR66.ILLEGALVALUE, networkTransaction);
    if (!fileinfo.contains("-nofollow")) {
      TransferArgs.forceAnalyzeFollow(this);
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(null));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(NoOpRecvThroughHandler.class);
    }
    if (!getParams(args, false)) {
      logger.error("Wrong initialization");
      if (DbConstantR66.admin != null) {
        DbConstantR66.admin.close();
      }
      WaarpSystemUtil.systemExit(1);
      return;
    }
    Configuration.configuration.pipelineInit();
    final NetworkTransaction networkTransaction = new NetworkTransaction();
    try {
      final R66Future future = new R66Future(true);
      final NoOpRecvThroughHandler handler = new NoOpRecvThroughHandler();
      final TestRecvThroughClient transaction =
          new TestRecvThroughClient(future, handler, rhost, localFilename, rule,
                                    transferInfo, ismd5, block,
                                    networkTransaction);
      transaction.normalInfoAsWarn = snormalInfoAsWarn;
      final long time1 = System.currentTimeMillis();
      transaction.run();
      future.awaitOrInterruptible();

      final long time2 = System.currentTimeMillis();
      final long delay = time2 - time1;
      final R66Result result = future.getResult();
      if (future.isSuccess()) {
        if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
          logger.warn("Warning with Id: " + result.getRunner().getSpecialId() +
                      " on file: " +
                      (result.getFile() != null? result.getFile().toString() :
                          "no file") + " delay: " + delay);
        } else {
          logger.warn("Success with Id: " + result.getRunner().getSpecialId() +
                      " on Final file: " +
                      (result.getFile() != null? result.getFile().toString() :
                          "no file") + " delay: " + delay);
        }
      } else {
        if (result == null || result.getRunner() == null) {
          logger.warn("Transfer in Error with no Id" + " : {}",
                      future.getCause() != null?
                          future.getCause().getMessage() : "");
          networkTransaction.closeAll();
          WaarpSystemUtil.systemExit(1);
          return;
        }
        if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
          logger.warn("Transfer in Warning with Id: " +
                      result.getRunner().getSpecialId() + " : {}",
                      future.getCause() != null?
                          future.getCause().getMessage() : "");
          networkTransaction.closeAll();
          WaarpSystemUtil.systemExit(result.getCode().ordinal());
        } else {
          logger.error(
              "Transfer in Error with Id: " + result.getRunner().getSpecialId(),
              future.getCause());
          networkTransaction.closeAll();
          WaarpSystemUtil.systemExit(result.getCode().ordinal());
        }
      }
    } finally {
      networkTransaction.closeAll();
    }

  }

}
