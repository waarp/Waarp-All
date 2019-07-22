/**
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation; either version 3.0 of the
 * License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this
 * software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.waarp.gateway.ftp;

import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.ftp.core.exception.FtpNoConnectionException;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.control.ExecBusinessHandler;
import org.waarp.gateway.ftp.data.FileSystemBasedDataBusinessHandler;
import org.waarp.gateway.ftp.service.FtpEngine;
import org.waarp.gateway.kernel.exec.AbstractExecutor;
import org.waarp.openr66.protocol.configuration.Configuration;

/**
 * Exec FTP Server using simple authentication (XML FileInterface based), and standard Directory and
 * FileInterface implementation (Filesystem based).
 * 
 * @author Frederic Bregier
 * 
 */
public class ExecGatewayFtpServer {
    /**
     * Internal Logger
     */
    private static WaarpLogger logger = null;

    /**
     * Take a simple XML file as configuration.
     * 
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: " +
                    ExecGatewayFtpServer.class.getName() + " <config-file> [<r66config-file>]");
            return;
        }
        WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
        logger = WaarpLoggerFactory
                .getLogger(ExecGatewayFtpServer.class);
        initialize(args[0], args.length > 1 ? args[1] : null);
    }

    public static boolean initialize(String config, String r66file) {
        boolean asAService = false;
        if (logger == null) {
            // Called as a service
            logger = WaarpLoggerFactory
                    .getLogger(ExecGatewayFtpServer.class);
            asAService = true;
        }
        FileBasedConfiguration configuration = new FileBasedConfiguration(
                ExecGatewayFtpServer.class, ExecBusinessHandler.class,
                FileSystemBasedDataBusinessHandler.class,
                new FilesystemBasedFileParameterImpl());
        if (asAService) {
            configuration.getShutdownConfiguration().serviceFuture = FtpEngine.closeFuture;
        }
        if (!configuration.setConfigurationServerFromXml(config)) {
            System.err.println("Bad main configuration");
            return false;
        }
        Configuration.configuration.setUseLocalExec(configuration.useLocalExec);
        if (AbstractExecutor.useDatabase) {
            // Use R66 module
            if (r66file != null) {
                if (!org.waarp.openr66.configuration.FileBasedConfiguration
                        .setSubmitClientConfigurationFromXml(Configuration.configuration,
                                r66file)) {
                    System.err.println("Bad R66 configuration");
                    return false;
                }
            } else {
                // Cannot get R66 functional
                System.err.println("No R66PrepareTransfer configuration file");
            }
        } else {
            System.err.println("No R66PrepareTransfer support");
        }
        FileBasedConfiguration.fileBasedConfiguration = configuration;
        // Start server.
        configuration.configureLExec();
        try {
            configuration.serverStartup();
        } catch (FtpNoConnectionException e1) {
            e1.printStackTrace();
            configuration.releaseResources();
            return false;
        }
        configuration.configureHttps();
        configuration.configureConstraint();
        try {
            configuration.configureSnmp();
        } catch (FtpNoConnectionException e) {
            System.err.println("Cannot start SNMP support: " + e.getMessage());
        }
        logger.warn("FTP started " +
                (configuration.getFtpInternalConfiguration().isUsingNativeSsl() ? "Implicit SSL On" :
                        configuration.getFtpInternalConfiguration().isAcceptAuthProt() ? "Explicit SSL On" :
                                "with SSL Off"));
        return true;
    }

}
