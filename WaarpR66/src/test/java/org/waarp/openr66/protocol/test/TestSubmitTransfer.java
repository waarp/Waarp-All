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

import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.protocol.utils.R66Future;

import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Test class for multiple SubmitTransfer
 *
 *
 */
public class TestSubmitTransfer extends SubmitTransfer {
  static int nb = 100;

  public TestSubmitTransfer(R66Future future, String remoteHost,
                            String filename, String rulename, String fileinfo,
                            boolean isMD5, int blocksize, Timestamp starttime) {
    super(future, remoteHost, filename, rulename, fileinfo, isMD5, blocksize,
          DbConstant.ILLEGALVALUE, starttime);
  }

  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(SubmitTransfer.class);
    }
    if (!getParams(args, true)) {
      logger.error("Wrong initialization");
      if (DbConstant.admin != null && DbConstant.admin.isActive()) {
        DbConstant.admin.close();
      }
      DetectionUtils.SystemExit(1);
      return;
    }
    getSpecialParams(args, 1);

    final ExecutorService executorService = Executors.newCachedThreadPool();
    final R66Future[] arrayFuture = new R66Future[nb];

    logger.warn("Start Test Submit");
    for (int i = 0; i < nb; i++) {
      arrayFuture[i] = new R66Future(true);
      Timestamp newstart = ttimestart;
      if (newstart != null) {
        // delay of 10 ms between each
        newstart = new Timestamp(newstart.getTime() + i * 10);
      }
      final TestSubmitTransfer transaction =
          new TestSubmitTransfer(arrayFuture[i], rhost, localFilename, rule,
                                 fileInfo, ismd5, block, newstart);
      transaction.normalInfoAsWarn = snormalInfoAsWarn;
      // executorService.execute(transaction);
      transaction.run();
    }
    int success = 0;
    int error = 0;
    for (int i = 0; i < nb; i++) {
      arrayFuture[i].awaitOrInterruptible();
      if (arrayFuture[i].isSuccess()) {
        success++;
      } else {
        error++;
      }
    }
    executorService.shutdown();
    logger.warn("Prepare transfer Success: " + success + " Error: " + error);
  }

  /**
   * @param args
   * @param rank
   *
   * @return True if OK
   */
  protected static boolean getSpecialParams(String[] args, int rank) {
    for (int i = rank; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("-nb")) {
        i++;
        nb = Integer.parseInt(args[i]);
      } else if (args[i].equalsIgnoreCase("-md5")) {
      } else if (args[i].charAt(0) == '-') {
        i++;// jump one
      }
    }
    return true;
  }

}
