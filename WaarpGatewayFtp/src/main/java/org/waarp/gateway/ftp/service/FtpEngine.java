/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.gateway.ftp.service;

import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.service.EngineAbstract;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.ftp.core.config.FtpConfiguration;
import org.waarp.ftp.core.utils.FtpChannelUtils;
import org.waarp.gateway.ftp.ExecGatewayFtpServer;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;

/**
 * Engine used to start and stop the real Gateway Ftp service
 * 
 * @author Frederic Bregier
 *
 */
public class FtpEngine extends EngineAbstract {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(FtpEngine.class);

    public static final WaarpFuture closeFuture = new WaarpFuture(true);

    public static final String CONFIGFILE = "org.waarp.gateway.ftp.config.file";

    public static final String R66CONFIGFILE = "org.waarp.r66.config.file";

    @Override
    public void run() {
        String ftpfile = SystemPropertyUtil.get(CONFIGFILE);
        String r66file = SystemPropertyUtil.get(R66CONFIGFILE);
        if (ftpfile == null) {
            logger.error("Cannot find " + CONFIGFILE + " parameter");
            shutdown();
            return;
        }
        try {
            if (!ExecGatewayFtpServer.initialize(ftpfile, r66file)) {
                logger.error("Cannot start Gateway FTP");
                shutdown();
                return;
            }
        } catch (Throwable e) {
            logger.error("Cannot start Gateway FTP", e);
            shutdown();
            return;
        }
        logger.warn("Service started with " + ftpfile);
    }

    private static void exit(FtpConfiguration configuration) {
        FtpChannelUtils util = new FtpChannelUtils(configuration);
        util.run();
    }

    @Override
    public void shutdown() {
        exit(FileBasedConfiguration.fileBasedConfiguration);
        closeFuture.setSuccess();
        logger.info("Service stopped");
    }

    @Override
    public boolean isShutdown() {
        return closeFuture.isDone();
    }

    @Override
    public boolean waitShutdown() throws InterruptedException {
        closeFuture.await();
        return closeFuture.isSuccess();
    }
}
