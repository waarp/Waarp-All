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
package org.waarp.openr66.client.spooledService;

import org.waarp.common.file.FileUtils;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.service.EngineAbstract;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.openr66.client.SpooledDirectoryTransfer;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.configuration.R66SystemProperties;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Engine used to start and stop the SpooledDirectory service
 */
public class SpooledEngine extends EngineAbstract {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(SpooledEngine.class);

  static final WaarpFuture closeFuture = new WaarpFuture(true);
  private static final String[] STRING_0_LENGTH = {};

  @Override
  public void run() {
    final String config =
        SystemPropertyUtil.get(R66SystemProperties.OPENR66_CONFIGFILE);
    if (config == null) {
      logger.error("Cannot find " + R66SystemProperties.OPENR66_CONFIGFILE +
                   " parameter for SpooledEngine");
      closeFuture.cancel();
      shutdown();
      return;
    }
    Configuration.configuration.getShutdownConfiguration().serviceFuture =
        closeFuture;
    try {
      final Properties prop = new Properties();
      final FileInputStream in = new FileInputStream(config);
      try {
        prop.load(in);
        final ArrayList<String> array = new ArrayList<String>();
        for (final Object okey : prop.keySet()) {
          final String key = (String) okey;
          final String val = prop.getProperty(key);
          if ("xmlfile".equals(key)) {
            if (val != null && !val.trim().isEmpty()) {
              array.add(0, val);
            } else {
              throw new Exception("Initialization in error: missing xmlfile");
            }
          } else {
            array.add('-' + key);
            if (val != null && !val.trim().isEmpty()) {
              array.add(val);
            }
          }
        }
        if (!SpooledDirectoryTransfer
            .initialize(array.toArray(STRING_0_LENGTH), false)) {
          throw new Exception("Initialization in error");
        }
      } finally {
        FileUtils.close(in);
      }
    } catch (final Throwable e) {
      logger.error("Cannot start SpooledDirectory", e);
      closeFuture.cancel();
      shutdown();
      return;
    }
    logger.warn("SpooledDirectory Service started with " + config);
  }

  @Override
  public void shutdown() {
    WaarpShutdownHook.shutdownWillStart();
    logger.info("Shutdown");
    for (final SpooledDirectoryTransfer spooled : SpooledDirectoryTransfer.list) {
      spooled.stop();
    }
    Configuration.configuration
        .setTimeoutCon(Configuration.configuration.getTimeoutCon() / 10);
    try {
      while (!SpooledDirectoryTransfer.executorService
          .awaitTermination(Configuration.configuration.getTimeoutCon(),
                            TimeUnit.MILLISECONDS)) {
        Thread.sleep(Configuration.configuration.getTimeoutCon());
      }
    } catch (final InterruptedException e) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
    for (final SpooledDirectoryTransfer spooledDirectoryTransfer : SpooledDirectoryTransfer.list) {
      logger.warn(Messages.getString("SpooledDirectoryTransfer.58") +
                  spooledDirectoryTransfer.name + ": " +
                  spooledDirectoryTransfer.getSent() + " success, " +
                  spooledDirectoryTransfer.getError() + Messages
                      .getString("SpooledDirectoryTransfer.60")); //$NON-NLS-1$
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
  public boolean waitShutdown() {
    closeFuture.awaitOrInterruptible();
    logger.info("Shutdown on going: {}", closeFuture.isSuccess());
    return closeFuture.isSuccess();
  }
}
