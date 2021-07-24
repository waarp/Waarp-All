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

package org.waarp.openr66.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.joda.time.DateTime;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.protocol.http.restv2.utils.JsonUtils;
import org.waarp.openr66.protocol.monitoring.ElasticsearchMonitoringExporterClient;
import org.waarp.openr66.protocol.networkhandler.ssl.NetworkSslServerInitializer;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Iterator;

import static org.waarp.openr66.protocol.monitoring.ElasticsearchMonitoringExporterClientBuilder.*;
import static org.waarp.openr66.protocol.monitoring.MonitorExporterTransfers.*;

/**
 * Elasticsearch client for Waarp
 */
public class ElasticsearchMonitoringExporterClientImpl
    implements ElasticsearchMonitoringExporterClient {
  private static final WaarpLogger logger = WaarpLoggerFactory.getLogger(
      ElasticsearchMonitoringExporterClientImpl.class);

  protected final String index;
  protected final RestClientBuilder builder;

  protected RestHighLevelClient client;

  /**
   * Note that only one among (username/pwd, token, apikey) is allowed and
   * will be taken into account.
   *
   * @param username username to connect to Elasticsearch if any (Basic
   *     authentication) (nullable)
   * @param pwd password to connect to Elasticsearch if any (Basic
   *     authentication) (nullable)
   * @param token access token (Bearer Token authorization
   *     by Header) (nullable)
   * @param apiKey API Key (Base64 of 'apiId:apiKey') (ApiKey authorization
   *     by Header) (nullable)
   * @param prefix as '/prefix' or null if none
   * @param index as 'waarpr66monitor' as the index name within
   *     Elasticsearch, including extra dynamic information
   * @param compression True to compress REST exchanges between the client
   *     and the Elasticsearch server
   * @param httpHosts array of HttpHost
   */
  public ElasticsearchMonitoringExporterClientImpl(final String username,
                                                   final String pwd,
                                                   final String token,
                                                   final String apiKey,
                                                   final String prefix,
                                                   final String index,
                                                   final boolean compression,
                                                   final HttpHost... httpHosts) {
    this.index = index;
    builder = RestClient.builder(httpHosts).setCompressionEnabled(compression);
    if (ParametersChecker.isNotEmpty(prefix)) {
      builder.setPathPrefix(prefix);
    }
    int headerLen = 1;
    if (ParametersChecker.isNotEmpty(apiKey, token)) {
      headerLen = 2;
    }
    final Header[] defaultHeaders = new Header[headerLen];
    headerLen = 0;
    if (ParametersChecker.isNotEmpty(token)) {
      defaultHeaders[headerLen] =
          new BasicHeader("Authorization", "Bearer " + token);
      headerLen++;
    } else if (ParametersChecker.isNotEmpty(apiKey)) {
      defaultHeaders[headerLen] =
          new BasicHeader("Authorization", "ApiKey " + apiKey);
      headerLen++;
    }
    if (headerLen > 0) {
      builder.setDefaultHeaders(defaultHeaders);
    }
    boolean tls = false;
    for (final HttpHost httpHost : httpHosts) {
      tls |= httpHost.getSchemeName().equalsIgnoreCase("https");
    }
    final SSLContext sslContext;
    if (tls) {
      try {
        final SSLContextBuilder sslBuilder = SSLContexts.custom()
                                                        .loadKeyMaterial(
                                                            NetworkSslServerInitializer.getWaarpSecureKeyStore()
                                                                                       .getKeyStore(),
                                                            NetworkSslServerInitializer.getWaarpSecureKeyStore()
                                                                                       .getKeyStorePassword())
                                                        .loadTrustMaterial(
                                                            NetworkSslServerInitializer.getWaarpSecureKeyStore()
                                                                                       .getKeyTrustStore(),
                                                            null);
        sslContext = sslBuilder.build();
      } catch (final NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException | KeyManagementException e) {
        logger.error(e.getMessage());
        throw new IllegalArgumentException(e);
      }
    } else {
      sslContext = null;
    }
    if (ParametersChecker.isNotEmpty(username, pwd)) {
      final CredentialsProvider credentialsProvider =
          new BasicCredentialsProvider();
      credentialsProvider.setCredentials(AuthScope.ANY,
                                         new UsernamePasswordCredentials(
                                             username, pwd));
      builder.setHttpClientConfigCallback(new HttpClientConfigCallback() {
        @Override
        public HttpAsyncClientBuilder customizeHttpClient(
            final HttpAsyncClientBuilder httpClientBuilder) {
          if (sslContext != null) {
            return httpClientBuilder.setDefaultCredentialsProvider(
                credentialsProvider).setSSLContext(sslContext);
          } else {
            return httpClientBuilder.setDefaultCredentialsProvider(
                credentialsProvider);
          }
        }
      });
    }
    logger.info("Elasticsearch client: user {} pwd {} token {} apikey {} " +
                "prefix {} index {}", username, pwd, token, apiKey, prefix,
                index);
    client = new RestHighLevelClient(builder);
  }

  @Override
  public final boolean post(final ObjectNode monitoredTransfers,
                            final DateTime start, final DateTime stop,
                            final String serverId) {
    if (client == null) {
      client = new RestHighLevelClient(builder);
    }
    final String finalIndex = index.replace(ELASTIC_WAARPHOST, serverId)
                                   .replaceAll(ELASTIC_DATETIME,
                                               stop.toString(FORMAT_DATETIME))
                                   .replaceAll(ELASTIC_DATEHOUR,
                                               stop.toString(FORMAT_DATEHOUR))
                                   .replaceAll(ELASTIC_DATE,
                                               stop.toString(FORMAT_DATE))
                                   .replaceAll(ELASTIC_YEAR_MONTH,
                                               stop.toString(FORMAT_YEAR_MONTH))
                                   .replaceAll(ELASTIC_YEAR,
                                               stop.toString(FORMAT_YEAR))
                                   .toLowerCase();
    logger.debug("Will post to {}", finalIndex);
    final BulkRequest bulkRequest = new BulkRequest(finalIndex);
    final ArrayNode arrayNode = (ArrayNode) monitoredTransfers.get(RESULTS);
    final Iterator<JsonNode> iterator = arrayNode.elements();
    while (iterator.hasNext()) {
      final ObjectNode node = (ObjectNode) iterator.next();
      final IndexRequest indexRequest =
          new IndexRequest().id(node.get(UNIQUE_ID).asText());
      indexRequest.source(
          JsonUtils.nodeToString(node).getBytes(StandardCharsets.UTF_8),
          XContentType.JSON);
      bulkRequest.add(indexRequest);
    }
    final BulkResponse bulkResponse;
    try {
      bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
    } catch (final IOException e) {
      logger.error(e.getMessage());
      return false;
    }
    logger.debug("ES failure? {} {}", bulkResponse.hasFailures(),
                 bulkResponse.status().getStatus());
    if (logger.isDebugEnabled() && bulkResponse.hasFailures()) {
      final Iterator<BulkItemResponse> iterator1 = bulkResponse.iterator();
      while (iterator1.hasNext()) {
        final BulkItemResponse response = iterator1.next();
        logger.debug("ES item: {}", response.getFailureMessage());
      }
    }
    return !bulkResponse.hasFailures();
  }

  @Override
  public final void close() {
    try {
      client.close();
    } catch (final IOException e) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
    client = null;
  }
}
