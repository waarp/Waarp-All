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

package org.waarp.gateway.ftp;

import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.ftp.core.exception.FtpNoConnectionException;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.control.ExecBusinessHandler;
import org.waarp.gateway.ftp.data.FileSystemBasedDataBusinessHandler;
import org.waarp.gateway.ftp.exec.AbstractExecutor;
import org.waarp.gateway.ftp.service.FtpEngine;
import org.waarp.openr66.protocol.configuration.Configuration;

/**
 * Exec FTP Server using simple authentication (XML FileInterface based), and
 * standard Directory and
 * FileInterface implementation (Filesystem based).
 */
public class ExecGatewayFtpServer {
  /**
   * Internal Logger
   */
  private static WaarpLogger logger;

  private ExecGatewayFtpServer() {
  }

  /**
   * Take a simple XML file as configuration.
   *
   * @param args
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      SysErrLogger.FAKE_LOGGER.syserr(
          "Usage: " + ExecGatewayFtpServer.class.getName() +
          " <config-file> [<r66config-file>]");
      return;
    }
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
    logger = WaarpLoggerFactory.getLogger(ExecGatewayFtpServer.class);
    initialize(args[0], args.length > 1? args[1] : null);
  }

  public static boolean initialize(String config, String r66file) {
    boolean asAService = false;
    if (logger == null) {
      // Called as a service
      logger = WaarpLoggerFactory.getLogger(ExecGatewayFtpServer.class);
      asAService = true;
    }
    final FileBasedConfiguration configuration =
        new FileBasedConfiguration(ExecGatewayFtpServer.class,
                                   ExecBusinessHandler.class,
                                   FileSystemBasedDataBusinessHandler.class,
                                   new FilesystemBasedFileParameterImpl());
    if (asAService) {
      configuration.getShutdownConfiguration().serviceFuture =
          FtpEngine.closeFuture;
    }
    if (!configuration.setConfigurationServerFromXml(config)) {
      SysErrLogger.FAKE_LOGGER.syserr("Bad main configuration");
      return false;
    }
    Configuration.configuration.setUseLocalExec(configuration.isUseLocalExec());
    if (AbstractExecutor.useDatabase) {
      // Use R66 module
      if (r66file != null) {
        if (!org.waarp.openr66.configuration.FileBasedConfiguration
            .setSubmitClientConfigurationFromXml(Configuration.configuration,
                                                 r66file)) {
          SysErrLogger.FAKE_LOGGER.syserr("Bad R66 configuration");
          return false;
        }
      } else {
        // Cannot get R66 functional
        SysErrLogger.FAKE_LOGGER
            .syserr("No R66PrepareTransfer configuration file");
      }
    } else {
      SysErrLogger.FAKE_LOGGER.syserr("No R66PrepareTransfer support");
    }
    FileBasedConfiguration.fileBasedConfiguration = configuration;
    // Start server.
    configuration.configureLExec();
    try {
      configuration.serverStartup();
    } catch (final FtpNoConnectionException e1) {
      SysErrLogger.FAKE_LOGGER.syserr(e1);
      configuration.releaseResources();
      return false;
    }
    configuration.configureHttps();
    configuration.configureConstraint();
    try {
      configuration.configureSnmp();
    } catch (final FtpNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER
          .syserr("Cannot start SNMP support: " + e.getMessage());
    }
    logger.warn("FTP started " +
                (configuration.getFtpInternalConfiguration().isUsingNativeSsl()?
                    "Implicit SSL On" :
                    configuration.getFtpInternalConfiguration()
                                 .isAcceptAuthProt()? "Explicit SSL On" :
                        "with SSL Off"));
    return true;
  }

}
