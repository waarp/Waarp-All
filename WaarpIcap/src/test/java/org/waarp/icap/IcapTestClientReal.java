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

package org.waarp.icap;

import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class IcapTestClientReal {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(IcapTestClientReal.class);

  private static void createFile(File file, int len) throws IOException {
    file.delete();
    byte[] from = new byte[len];
    Arrays.fill(from, (byte) 'A');
    FileOutputStream outputStream = new FileOutputStream(file);
    outputStream.write(from);
    outputStream.flush();
    outputStream.close();
  }

  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    File file = null;
    try {
      file = new File("/tmp/toscan.bin");
      createFile(file, 10000);

      String service = "avscan";
      String IP = "172.17.0.2";
      if (args.length > 0) {
        IP = args[0];
      }
      String[] fullArgs = new String[] {
          IcapScanFile.FILE_ARG, file.getAbsolutePath(), IcapScanFile.TO_ARG,
          IP, IcapScanFile.SERVICE_ARG, service, "-logger", "DEBUG"
      };
      testScanIcap(file, service, fullArgs);
      service = "virus_scan";
      fullArgs[5] = service;
      testScanIcap(file, service, fullArgs);
      service = "srv_clamav";
      fullArgs[5] = service;
      testScanIcap(file, service, fullArgs);

      logger.warn("\n***********REDO in WARN mode**********\n");
      service = "avscan";
      fullArgs[5] = service;
      fullArgs[7] = "WARN";
      testScanIcap(file, service, fullArgs);
      service = "virus_scan";
      fullArgs[5] = service;
      testScanIcap(file, service, fullArgs);
      service = "srv_clamav";
      fullArgs[5] = service;
      testScanIcap(file, service, fullArgs);

    } catch (Exception e) {
      logger.warn(e);
    } finally {
      if (file != null) {
        file.delete();
      }
    }
  }

  private static void testScanIcap(final File file, final String service,
                                   final String[] fullArgs) {
    int status;
    logger.warn("\n-----------\nTest service: {}\n-----------\n", service);
    fullArgs[1] = file.getAbsolutePath();
    status = IcapScanFile.scanFile(fullArgs);
    if (status == 0) {
      logger.warn("\n===========\nOK with service: {}\n===========", service);
      fullArgs[1] = IcapClient.EICARTEST;
      status = IcapScanFile.scanFile(fullArgs);
      if (status == 0) {
        logger.warn("\n###########\nKO Virus: {}\n###########", service);
      } else {
        logger.warn("\n===========\nOK with virus: {}\n===========", status);
      }
    } else {
      logger.warn("\n###########\nKO with service: {}\n###########", service,
                  status);
    }
    logger.warn("\n-----------\nEnd Test service: {}\n-----------\n", service);
  }

}
