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

package org.waarp.openr66.protocol.monitoring;

import org.apache.http.HttpHost;

/**
 * ElasticsearchClient Factory (only available in JRE 8 and over and if a
 * Factory is set into ElasticsearchClientBuilder)
 */
public interface ElasticsearchClientFactory {
  /**
   * Will use something like:<br>
   * setCompressionEnabled(true);<br>
   * setPathPrefix(prefix); if not null<br>
   * Index will be used as final Elasticsearch index name
   *
   * @param username username to connect to Elasticsearch if any (Basic
   *     authentication) (nullable)
   * @param pwd password to connect to Elasticsearch if any (Basic
   *     authentication) (nullable)
   * @param token access token (Bearer Token authorization
   *     by Header) (nullable)
   * @param apiKey API Key (Base64 of 'apiId:apiKey') (ApiKey authorization
   *     by Header) (nullable)
   * @param prefix Path to set as Prefix for every requests, can be null
   * @param index Index name within Elasticsearch
   * @param compression True to compress REST exchanges between the client
   *     and the Elasticsearch server
   * @param httpHosts 1 or more HttpHost pointing to Elasticsearch nodes
   *
   * @return a new ElasticsearchClient
   */
  ElasticsearchClient createElasticsearchClient(final String username,
                                                final String pwd,
                                                final String token,
                                                final String apiKey,
                                                final String prefix,
                                                final String index,
                                                final boolean compression,
                                                final HttpHost... httpHosts);
}
