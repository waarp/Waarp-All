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

/*
 * Copyright © 2014-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * Service and components to build Netty based Http web service. {@code
 * NettyHttpService} sets up the
 * necessary pipeline and manages starting, stopping, state-management of the
 * web service.
 *
 * <p>
 * In-order to handle http requests, {@code HttpHandler} must be implemented.
 * The methods in the classes
 * implemented from {@code HttpHandler} must be annotated with Jersey
 * annotations to specify http uri paths
 * and http methods. Note: Only supports the following annotations: {@link
 * javax.ws.rs.Path Path},
 * {@link javax.ws.rs.PathParam PathParam}, {@link javax.ws.rs.GET GET}, {@link
 * javax.ws.rs.PUT PUT},
 * {@link javax.ws.rs.POST POST}, {@link javax.ws.rs.DELETE DELETE}.
 * <p>
 * Note: Doesn't support getting Annotations from base class if the HttpHandler
 * implements also extends a
 * class with annotation.
 * <p>
 * Sample usage Handlers and Netty service setup:
 *
 * <pre>
 * //Setup Handlers
 *
 * {@literal @}Path("/common/v1/")
 * public class ApiHandler implements HttpHandler {
 *
 *   {@literal @}Path("widgets")
 *   {@literal @}GET
 *   public void widgetHandler(HttpRequest request, HttpResponder responder) {
 *     responder.sendJson(HttpResponseStatus.OK, "{\"key\": \"value\"}");
 *   }
 *
 *   {@literal @}Override
 *   public void init(HandlerContext context) {
 *     //Perform bootstrap operations before any of the handlers in this class gets called.
 *   }
 *
 *   {@literal @}Override
 *   public void destroy(HandlerContext context) {
 *    //Perform teardown operations the server shuts down.
 *   }
 * }
 *
 * //Set up and start the http service
 * NettyHttpService service = NettyHttpService.builder()
 *                                            .addHttpHandlers(ImmutableList.of(new Handler())
 *                                            .setPort(8989)
 *                                            .build();
 * service.start();
 *
 * // ....
 *
 * //Stop the web-service
 * service.shutdown();
 *
 * </pre>
 */
package io.cdap.http;
