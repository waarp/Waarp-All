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
package org.waarp.openr66.client.spooledService;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.service.EngineAbstract;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.openr66.client.SpooledDirectoryTransfer;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.configuration.R66SystemProperties;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66ShutdownHook;

/**
 * Engine used to start and stop the SpooledDirectory service
 * 
 * @author Frederic Bregier
 *
 */
public class SpooledEngine extends EngineAbstract {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(SpooledEngine.class);

    static final WaarpFuture closeFuture = new WaarpFuture(true);

    @Override
    public void run() {
        String config = SystemPropertyUtil.get(R66SystemProperties.OPENR66_CONFIGFILE);
        if (config == null) {
            logger.error("Cannot find " + R66SystemProperties.OPENR66_CONFIGFILE + " parameter for SpooledEngine");
            closeFuture.cancel();
            shutdown();
            return;
        }
        Configuration.configuration.getShutdownConfiguration().serviceFuture = closeFuture;
        try {
            Properties prop = new Properties();
            FileInputStream in = new FileInputStream(config);
            try {
                prop.load(in);
                ArrayList<String> array = new ArrayList<String>();
                for (Object okey : prop.keySet()) {
                    String key = (String) okey;
                    String val = prop.getProperty(key);
                    if (key.equals("xmlfile")) {
                        if (val != null && !val.trim().isEmpty()) {
                            array.add(0, val);
                        } else {
                            throw new Exception("Initialization in error: missing xmlfile");
                        }
                    } else {
                        array.add("-" + key);
                        if (val != null && !val.trim().isEmpty()) {
                            array.add(val);
                        }
                    }
                }
                if (!SpooledDirectoryTransfer.initialize((String[]) array.toArray(new String[] {}), false)) {
                    throw new Exception("Initialization in error");
                }
            } finally {
                in.close();
            }
        } catch (Throwable e) {
            logger.error("Cannot start SpooledDirectory", e);
            closeFuture.cancel();
            shutdown();
            return;
        }
        logger.warn("SpooledDirectory Service started with " + config);
    }

    @Override
    public void shutdown() {
        R66ShutdownHook.shutdownWillStart();
        logger.info("Shutdown");
        for (SpooledDirectoryTransfer spooled : SpooledDirectoryTransfer.list) {
            spooled.stop();
        }
        Configuration.configuration.setTIMEOUTCON(Configuration.configuration.getTIMEOUTCON() / 10);
        try {
            while (!SpooledDirectoryTransfer.executorService.awaitTermination(Configuration.configuration.getTIMEOUTCON(),
                    TimeUnit.MILLISECONDS)) {
                Thread.sleep(Configuration.configuration.getTIMEOUTCON());
            }
        } catch (InterruptedException e) {
        }
        for (SpooledDirectoryTransfer spooledDirectoryTransfer : SpooledDirectoryTransfer.list) {
            logger.warn(Messages.getString("SpooledDirectoryTransfer.58") + spooledDirectoryTransfer.name + ": "
                    + spooledDirectoryTransfer.getSent()
                    + " success, " + spooledDirectoryTransfer.getError()
                    + Messages.getString("SpooledDirectoryTransfer.60")); //$NON-NLS-1$
        }
        SpooledDirectoryTransfer.list.clear();
        logger.info("Shutdown network");
        SpooledDirectoryTransfer.networkTransactionStatic.closeAll(false);
        logger.info("All");
        ChannelUtils.startShutdown();
        closeFuture.setSuccess();
        logger.info("SpooledDirectory Service stopped");
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
