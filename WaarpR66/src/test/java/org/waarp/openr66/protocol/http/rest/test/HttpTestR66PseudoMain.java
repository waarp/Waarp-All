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
package org.waarp.openr66.protocol.http.rest.test;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.waarp.common.exception.CryptoException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.gateway.kernel.rest.HttpRestHandler;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.gateway.kernel.rest.RestConfiguration.CRUD;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS;
import org.waarp.openr66.server.R66Server;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 */
public class HttpTestR66PseudoMain {

  public static RestConfiguration config;

  public static RestConfiguration getTestConfiguration2()
      throws CryptoException, IOException {
    final RestConfiguration configuration = new RestConfiguration();
    configuration.setRestPort(8089);
    configuration.setRestSsl(false);
    configuration.setResthandlersCrud(new byte[RESTHANDLERS.values().length]);
    Arrays.fill(configuration.getResthandlersCrud(), CRUD.READ.mask);
    configuration.setRestAuthenticated(false);
    configuration.setRestTimeLimit(100000);
    configuration.setRestSignature(false);
    configuration.setRestAddress("127.0.0.1");
    return configuration;
  }

  /**
   * @param args
   *
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(null));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    final WaarpLogger logger =
        WaarpLoggerFactory.getLogger(HttpTestR66PseudoMain.class);
    final String pathTemp = "/tmp";
    if (!R66Server.initialize(args[0])) {
      SysErrLogger.FAKE_LOGGER.syserr("Error during startup");
      WaarpSystemUtil.systemExit(1);
      return;
    }

    config = getTestConfiguration();
    HttpRestHandler.initialize(pathTemp);
    HttpRestR66Handler.initializeService(config);

    logger.warn("Server RestOpenR66 starts");
    /*
     * HmacSha256 sha = new HmacSha256(); sha.generateKey(); sha.saveSecretKey(new
     * File("J:/Temp/temp/key.sha256"));
     */
  }

  public static RestConfiguration getTestConfiguration()
      throws CryptoException, IOException {
    final RestConfiguration configuration = new RestConfiguration();
    configuration.setRestPort(8088);
    configuration.setRestSsl(false);
    configuration.setResthandlersCrud(new byte[RESTHANDLERS.values().length]);
    Arrays.fill(configuration.getResthandlersCrud(), CRUD.ALL.mask);
    configuration.setRestAuthenticated(true);
    configuration.initializeKey(new File(HttpTestRestR66Client.keydesfilename));
    configuration.setRestTimeLimit(10000);
    configuration.setRestSignature(true);
    configuration.setRestAddress("127.0.0.1");
    return configuration;
  }

}
