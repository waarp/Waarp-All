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

package org.waarp.ftp;

import org.waarp.common.file.FileUtils;
import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.ftp.core.exception.FtpNoConnectionException;
import org.waarp.ftp.simpleimpl.SimpleGatewayFtpServer;
import org.waarp.ftp.simpleimpl.config.FileBasedConfiguration;
import org.waarp.ftp.simpleimpl.config.FileBasedSslConfiguration;
import org.waarp.ftp.simpleimpl.control.SimpleBusinessHandler;
import org.waarp.ftp.simpleimpl.data.FileSystemBasedDataBusinessHandler;

import java.io.File;

public class FtpServer {
  /**
   * Internal Logger
   */
  protected static WaarpLogger logger;
  protected static File dir;
  protected static FileBasedConfiguration configuration;

  public static void startFtpServer(String config, String sslconfig,
                                    boolean useSsl, boolean useNative) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(FtpServer.class);
    }
    final ClassLoader classLoader = FtpServer.class.getClassLoader();
    final File file = new File(classLoader.getResource(config).getFile());
    if (file.exists()) {
      dir = file.getParentFile();
      final File base = new File("/tmp/GGFTP");
      base.mkdir();
      FileUtils.forceDeleteRecursiveDir(base);
      configuration = new FileBasedConfiguration(SimpleGatewayFtpServer.class,
                                                 SimpleBusinessHandler.class,
                                                 FileSystemBasedDataBusinessHandler.class,
                                                 new FilesystemBasedFileParameterImpl());
      if (!configuration.setConfigurationFromXml(file.getAbsolutePath())) {
        logger.error("Bad configuration");
        return;
      }
      configuration.setTimeoutCon(1000);
      if (useSsl) {
        if (!FileBasedSslConfiguration
            .setConfigurationServerFromXml(configuration, sslconfig)) {
          SysErrLogger.FAKE_LOGGER.syserr("Bad Ssl configuration");
          return;
        }
        if (useNative) {
          // native SSL support
          configuration.getFtpInternalConfiguration().setUsingNativeSsl(true);
          configuration.getFtpInternalConfiguration().setAcceptAuthProt(false);
        } else {
          // AUTH, PROT, ... support
          configuration.getFtpInternalConfiguration().setUsingNativeSsl(false);
          configuration.getFtpInternalConfiguration().setAcceptAuthProt(true);
        }
      }
      // Start server.
      try {
        configuration.serverStartup();
      } catch (final FtpNoConnectionException e) {
        logger.error("FTP not started", e);
      }
      logger.info("FTP started");
    }
  }

  public static void stopFtpServer() {
    configuration.setShutdown(true);
    configuration.getFtpInternalConfiguration().releaseResources();
  }
}
