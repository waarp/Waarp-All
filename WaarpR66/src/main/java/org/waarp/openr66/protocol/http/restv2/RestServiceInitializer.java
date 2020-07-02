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

package org.waarp.openr66.protocol.http.restv2;

import io.cdap.http.ChannelPipelineModifier;
import io.cdap.http.NettyHttpService;
import io.cdap.http.SSLConfig;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import org.waarp.common.crypto.ssl.WaarpSecureKeyStore;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.AbstractRestDbHandler;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.HostConfigHandler;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.HostIdHandler;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.HostsHandler;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.LimitsHandler;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.RuleIdHandler;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.RulesHandler;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.ServerHandler;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.TransferIdHandler;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.TransfersHandler;
import org.waarp.openr66.protocol.http.restv2.resthandlers.RestExceptionHandler;
import org.waarp.openr66.protocol.http.restv2.resthandlers.RestHandlerHook;
import org.waarp.openr66.protocol.http.restv2.resthandlers.RestSignatureHandler;
import org.waarp.openr66.protocol.http.restv2.resthandlers.RestVersionHandler;
import org.waarp.openr66.protocol.networkhandler.ssl.NetworkSslServerInitializer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static org.waarp.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;

/**
 * This class is called to initialize the RESTv2 API.
 */
public final class RestServiceInitializer {

  private static final String ROUTER = "router";

  /**
   * The logger for all unexpected errors during the service initialization.
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RestServiceInitializer.class);

  /**
   * This is the {@link NettyHttpService} in charge of handling the RESTv2
   * API.
   */
  private static NettyHttpService restService;

  /**
   * This is a static class that should never be instantiated with a
   * constructor.
   */
  private RestServiceInitializer() {
    throw new UnsupportedOperationException(
        getClass().getName() + " cannot be instantiated.");
  }

  /**
   * The list of all {@link AbstractRestDbHandler} used by the API.
   */
  public static final Collection<AbstractRestDbHandler> handlers =
      new ArrayList<AbstractRestDbHandler>();

  /**
   * Fills the list of {@link AbstractRestDbHandler} with all handlers
   * activated in the API configuration.
   *
   * @param config The REST API configuration object.
   */
  private static void initHandlers(RestConfiguration config) {
    final byte hostsCRUD = config.getResthandlersCrud()[DbHostAuth.ordinal()];
    final byte rulesCRUD = config.getResthandlersCrud()[DbRule.ordinal()];
    final byte transferCRUD =
        config.getResthandlersCrud()[DbTaskRunner.ordinal()];
    final byte configCRUD =
        config.getResthandlersCrud()[DbHostConfiguration.ordinal()];
    final byte limitCRUD = config.getResthandlersCrud()[Bandwidth.ordinal()];
    final int serverCRUD = config.getResthandlersCrud()[Business.ordinal()] +
                           config.getResthandlersCrud()[Config.ordinal()] +
                           config.getResthandlersCrud()[Information.ordinal()] +
                           config.getResthandlersCrud()[Log.ordinal()] +
                           config.getResthandlersCrud()[Server.ordinal()] +
                           config.getResthandlersCrud()[Control.ordinal()];

    if (hostsCRUD != 0) {
      handlers.add(new HostsHandler(hostsCRUD));
      handlers.add(new HostIdHandler(hostsCRUD));
    }
    if (rulesCRUD != 0) {
      handlers.add(new RulesHandler(rulesCRUD));
      handlers.add(new RuleIdHandler(rulesCRUD));
    }
    if (transferCRUD != 0) {
      handlers.add(new TransfersHandler(transferCRUD));
      handlers.add(new TransferIdHandler(transferCRUD));
    }
    if (configCRUD != 0) {
      handlers.add(new HostConfigHandler(configCRUD));
    }
    if (limitCRUD != 0) {
      handlers.add(new LimitsHandler(limitCRUD));
    }
    if (serverCRUD != 0) {
      handlers.add(new ServerHandler(config.getResthandlersCrud()));
    }
  }

  /**
   * Builds and returns a {@link CorsConfig} to be used by the {@link
   * CorsHandler} to allow the REST API to
   * support CORS.
   *
   * @return The configuration used for dealing with CORS requests.
   */
  private static CorsConfig corsConfig() {
    final CorsConfigBuilder builder = CorsConfigBuilder.forAnyOrigin();

    builder.exposeHeaders(ALLOW, "transferURI", "hostURI", "ruleURI");
    builder.allowedRequestHeaders(AUTHORIZATION, AUTH_USER, AUTH_TIMESTAMP,
                                  AUTH_SIGNATURE, CONTENT_TYPE);
    builder.allowedRequestMethods(GET, POST, PUT, DELETE, OPTIONS);
    builder.maxAge(600);
    return builder.build();
  }

  /**
   * Initializes the RESTv2 service with the given {@link RestConfiguration}.
   *
   * @param config The REST API configuration object.
   */
  public static void initRestService(final RestConfiguration config) {
    initHandlers(config);

    final NettyHttpService.Builder restServiceBuilder =
        NettyHttpService.builder("R66_RESTv2").setPort(config.getRestPort())
                        .setHost(config.getRestAddress())
                        .setExecThreadPoolSize(20).setHttpChunkLimit(
            Configuration.configuration.getMaxGlobalMemory())
                        .setChannelConfig(ChannelOption.SO_REUSEADDR, true)
                        .setChildChannelConfig(ChannelOption.TCP_NODELAY, false)
                        .setChildChannelConfig(ChannelOption.SO_REUSEADDR, true)
                        .setHttpHandlers(handlers).setHandlerHooks(Collections
                                                                       .singleton(
                                                                           new RestHandlerHook(
                                                                               config
                                                                                   .isRestAuthenticated(),
                                                                               config
                                                                                   .getHmacSha256(),
                                                                               config
                                                                                   .getRestTimeLimit())))
                        .setExceptionHandler(new RestExceptionHandler())
                        .setExecThreadKeepAliveSeconds(-1L)
                        .setChannelPipelineModifier(
                            new ChannelPipelineModifier() {
                              @Override
                              public void modify(
                                  ChannelPipeline channelPipeline) {
                                channelPipeline.addBefore(ROUTER, "aggregator",
                                                          new HttpObjectAggregator(
                                                              Configuration.configuration
                                                                  .getMaxGlobalMemory()));
                                channelPipeline.addBefore(ROUTER,
                                                          RestVersionHandler.HANDLER_NAME,
                                                          new RestVersionHandler(
                                                              config));
                                channelPipeline
                                    .addBefore(RestVersionHandler.HANDLER_NAME,
                                               "cors",
                                               new CorsHandler(corsConfig()));
                                if (config.isRestAuthenticated() &&
                                    config.isRestSignature()) {
                                  channelPipeline.addAfter(ROUTER, "signature",
                                                           new RestSignatureHandler(
                                                               config
                                                                   .getHmacSha256()));
                                }

                                // Removes the HTTP compressor which causes problems
                                // on systems running java6 or earlier
                                final double JRE_version = Double.parseDouble(
                                    System.getProperty(
                                        "java.specification.version"));
                                if (JRE_version <= 1.6) {
                                  logger.info(
                                      "Removed REST HTTP compressor due to incompatibility " +
                                      "with the Java Runtime version");
                                  channelPipeline.remove("compressor");
                                }
                              }
                            });

    if (config.isRestSsl()) {
      final WaarpSecureKeyStore keyStore =
          NetworkSslServerInitializer.getWaarpSecureKeyStore();
      final String keyStoreFilename = keyStore.getKeyStoreFilename();
      final String keyStorePass = new String(keyStore.getKeyStorePassword());
      final String certificatePassword =
          new String(keyStore.getCertificatePassword());

      restServiceBuilder.enableSSL(
          SSLConfig.builder(new File(keyStoreFilename), keyStorePass)
                   .setCertificatePassword(certificatePassword).build());
    }

    restService = restServiceBuilder.build();
    try {
      restService.start();
    } catch (final Throwable t) {
      logger.error(t);
      throw new ExceptionInInitializerError(
          "FATAL ERROR_TASK : Failed to initialize RESTv2 service");
    }
  }

  /**
   * Stops the REST service.
   */
  public static void stopRestService() {
    if (restService != null) {
      try {
        restService.stop();
      } catch (final Throwable e) {
        logger.error("Exception caught during RESTv2 service shutdown", e);
      }
    } else {
      logger.warn("Error RESTv2 service is not running, cannot stop");
    }
  }
}
