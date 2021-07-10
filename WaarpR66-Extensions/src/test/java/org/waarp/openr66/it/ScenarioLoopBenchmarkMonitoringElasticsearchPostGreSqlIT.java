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

package org.waarp.openr66.it;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.elasticsearch.ElasticsearchMonitoringExporterClientImpl;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.monitoring.ElasticsearchMonitoringExporterClientBuilder;
import org.waarp.openr66.protocol.monitoring.MonitorExporterTransfers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.waarp.openr66.protocol.monitoring.ElasticsearchMonitoringExporterClientBuilder.*;
import static org.waarp.openr66.protocol.monitoring.MonitorExporterTransfers.*;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScenarioLoopBenchmarkMonitoringElasticsearchPostGreSqlIT
    extends S3ScenarioBaseLoopBenchmark {

  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  protected static final Map<String, String> TMPFSMAP =
      new HashMap<String, String>();

  static {
    TMPFSMAP.clear();
    TMPFSMAP.put("/tmp/postgresql/data", "rw");
  }

  @ClassRule
  public static PostgreSQLContainer db =
      (PostgreSQLContainer) new PostgreSQLContainer().withCommand(
          "postgres -c fsync=false -c synchronous_commit=off -c " +
          "full_page_writes=false -c wal_level=minimal -c " +
          "max_wal_senders=0").withTmpFs(TMPFSMAP);

  private static ElasticsearchContainer elasticsearchContainer;
  private static MonitorExporterTransfers monitorExporterTransfers;

  private static final String index = "waarpR66-" +
                                      ElasticsearchMonitoringExporterClientBuilder.ELASTIC_WAARPHOST +
                                      "-" +
                                      ElasticsearchMonitoringExporterClientBuilder.ELASTIC_DATE;
  private static HttpHost[] httpHosts;

  public JdbcDatabaseContainer getJDC() {
    return db;
  }

  @BeforeClass
  public static void setup() throws Exception {
    rulename = "loopnos3";
    logger.warn("START PostGreSQL IT TEST");
    scenarioBase =
        new ScenarioLoopBenchmarkMonitoringElasticsearchPostGreSqlIT();
    setUpBeforeClass();
    elasticsearchContainer = new ElasticsearchContainer(DockerImageName.parse(
        "docker.elastic.co/elasticsearch/elasticsearch-oss").withTag("7.10.2"));
    elasticsearchContainer.start();

    // Start Repetitive Monitoring
    final String uriElastic =
        "http://" + elasticsearchContainer.getHttpHostAddress();
    monitorExporterTransfers =
        new MonitorExporterTransfers(uriElastic, null, index, null, null, null,
                                     null, true, false, true);
    httpHosts = new HttpHost[] {
        HttpHost.create(elasticsearchContainer.getHttpHostAddress())
    };
    Configuration.configuration
        .scheduleWithFixedDelay(monitorExporterTransfers, 1, TimeUnit.SECONDS);
    ResourceLeakDetector.setLevel(Level.PARANOID);
  }

  private static class ElasticsearchMonitoringExporterClientImplExtend
      extends ElasticsearchMonitoringExporterClientImpl {

    /**
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
    public ElasticsearchMonitoringExporterClientImplExtend(
        final String username, final String pwd, final String token,
        final String apiKey, final String prefix, final String index,
        final boolean compression, final HttpHost... httpHosts) {
      super(username, pwd, token, apiKey, prefix, index, compression,
            httpHosts);
    }

    /**
     * Count items in index, with only serverId replaced.
     * Used by testing
     *
     * @param serverId
     *
     * @return the number of items, or -1 if an error occurs
     */
    public long countReferences(final String serverId) {
      if (client == null) {
        client = new RestHighLevelClient(builder);
      }
      final String partialIndex = index.replace(ELASTIC_WAARPHOST, serverId);
      final int posPercent = partialIndex.indexOf('%');
      final String finalIndex = posPercent >= 0?
          partialIndex.substring(0, posPercent).toLowerCase() + "*" :
          partialIndex.toLowerCase(Locale.ROOT);
      SearchRequest searchRequest = new SearchRequest(finalIndex);
      SearchSourceBuilder builder = new SearchSourceBuilder();
      builder.query(QueryBuilders.matchAllQuery()).docValueField(FOLLOW_ID)
             .docValueField(SPECIAL_ID).fetchSource(false);
      builder.size(1);
      searchRequest.source(builder);
      logger.debug("Will get count from {}", finalIndex);
      try {
        CountResponse countResponse =
            client.count(new CountRequest(finalIndex), RequestOptions.DEFAULT);
        if (countResponse.getCount() > 0) {
          SearchResponse searchResponse =
              client.search(searchRequest, RequestOptions.DEFAULT);
          if (searchResponse.status().getStatus() == 200) {
            SearchHits searchHits = searchResponse.getHits();
            SearchHit[] searchHits1 = searchHits.getHits();
            if (searchHits1.length > 0) {
              SearchHit searchHit = searchHits1[0];
              Map<String, DocumentField> map = searchHit.getFields();
              for (String key : map.keySet()) {
                logger.warn("{} : {}", key, map.get(key).toString());
              }
            }
          }
        }
        return countResponse.getCount();
      } catch (IOException e) {
        logger.error(e.getMessage());
        return -1;
      }
    }

  }

  @Override
  protected Thread getDeletgated() {
    return getDelegateThread();
  }

  private static Thread getDelegateThread() {
    return new Thread() {
      final ElasticsearchMonitoringExporterClientImplExtend
          elasticsearchClient =
          new ElasticsearchMonitoringExporterClientImplExtend(null, null, null,
                                                              null, null, index,
                                                              true, httpHosts);

      @Override
      public void run() {
        try {
          long numberEs = elasticsearchClient.countReferences("server1");
          logger.warn("ES Contains {} items", numberEs);
        } catch (final Exception e) {
          // nothing
        }
      }
    };
  }

  @AfterClass
  public static void tearDownContainerAfterClass() throws Exception {
    tearDownAfterClass(getDelegateThread());
    monitorExporterTransfers.close();
    WaarpSystemUtil.stopLogger(true);
    elasticsearchContainer.close();
  }

}
