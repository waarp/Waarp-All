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
package org.waarp.openr66.service;

import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.service.EngineAbstract;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.R66SystemProperties;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.server.R66Server;

/**
 * Engine used to start and stop the real R66 service
 * 
 * @author Frederic Bregier
 *
 */
public class R66Engine extends EngineAbstract {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(R66Engine.class);

    static WaarpFuture closeFuture = new WaarpFuture(true);

    @Override
    public void run() {
        String config = SystemPropertyUtil.get(R66SystemProperties.OPENR66_CONFIGFILE);
        if (config == null) {
            logger.error("Cannot find " + R66SystemProperties.OPENR66_CONFIGFILE + " parameter");
            closeFuture.cancel();
            shutdown();
            return;
        }
        Configuration.configuration.getShutdownConfiguration().serviceFuture = closeFuture;
        try {
            if (!R66Server.initialize(config)) {
                throw new Exception("Initialization in error");
            }
        } catch (Throwable e) {
            logger.error("Cannot start R66", e);
            closeFuture.cancel();
            shutdown();
            return;
        }
        logger.warn("Service started with " + config);
    }

    @Override
    public void shutdown() {
        logger.warn("Shutdown");
        ChannelUtils.startShutdown();
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
        logger.info("Shutdown on going: " + closeFuture.isSuccess());
        return closeFuture.isSuccess();
    }
}
