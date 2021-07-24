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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.EventLoopGroup;
import org.apache.http.HttpHost;
import org.joda.time.DateTime;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.client.TransferArgs;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.http.restv2.converters.TransferConverter;

import javax.ws.rs.InternalServerErrorException;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.waarp.openr66.dao.database.DBTransferDAO.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;

/**
 * The Monitor exports Transfers into a Json Format to a remote API REST
 * or an Elasticsearch server (if JRE >= 8)
 * in order to allow to monitor multiple Waarp Servers from one central
 * monitoring, such as using Elasticsearch with Kibana/Grafana through RESP
 * API from Logstash engine or equivalent, or your own REST API server
 * or a Elasticsearch server.<br>
 * <br>
 * Json format is:
 * <pre>{@code
 *  {
 *    "results": [                            # Array of Transfer information
 *      {
 *        "specialId": 12345,                     # Id as Long (-2^63 to 2^63 - 1)
 *        "uniqueId": "owner.requester.requested.specialId", # Unique global Id
 *        "hostId": "R66Owner",                   # R66 Owner (Server name)
 *        "globalStep": "step",                   # Global Current Step
 *        "globalLastStep": "laststep",           # Global Last Step previous Current
 *        "step": 1,                              # Current Step in Global Current Step
 *        "rank": 123,                            # Current Rank in transfer step
 *        "status": "status",                     # Current status
 *        "stepStatus": "stepstatus",             # Status of previous Step
 *        "originalFilename": "originalFilename", # Original Filename
 *        "originalSize": 123456,                 # Original file size
 *        "filename": "filename",                 # Resolved local filename
 *        "ruleName": "ruleName",                 # Rule name
 *        "blockSize": 123,                       # Block size during transfer
 *        "fileInfo": "fileInfo",                 # File information, containing associated file transfer information
 *        "followId": 123456,                     # Follow Id as Long (-2^63 to 2^63 - 1)
 *        "transferInfo": "transferInfo as Json", # Transfer internal information as Json String
 *        "start": "2021-03-28T11:55:15Z",        # Start date time of the transfer operation
 *        "stop": "2021-03-28T11:58:32Z",         # Current last date time event of the transfer operation
 *        "requested": "requested",               # Requested R66 hostname
 *        "requester": "requester",               # Requester R66 hostname
 *        "retrieve": true,                       # True if the request is a Pull, False if it is a Push
 *        "errorCode": "errorCode",               # Code of error as one char
 *        "errorMessage": "errorMessage",         # String message of current Error
 *        "waarpMonitor": {                       # Extra information for indexing if necessary
 *          "from": "2021-03-28T11:58:15Z",       # filter from (could be empty if none)
 *          "to": "2021-03-28T11:59:15Z",         # filter to
 *          "index": "r66owner"                   # R66 Hostname lowercase
 *        }
 *      },
 *      ...
 *    ]
 *  }
 * }</pre>
 * And the header of the HTTP request will contain:<br>
 * X-WAARP-ID (as the host Id), X-WAARP-START (as the waarpMonitor.from),
 * X-WAARP-STOP  (as the waarpMonitor.to)
 */
public class MonitorExporterTransfers extends Thread implements Closeable {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(MonitorExporterTransfers.class);

  public static final boolean MONITOR_KEEP_CONNECTION_DEFAULT = true;
  public static final boolean MONITOR_INTERVAL_INCLUDED_DEFAULT = true;
  public static final boolean MONITOR_LONG_AS_STRING_DEFAULT = false;

  public static final String HEADER_WAARP_ID = "X-WAARP-ID";
  public static final String HEADER_WAARP_START = "X-WAARP-START";
  public static final String HEADER_WAARP_STOP = "X-WAARP-STOP";

  public static final String SPECIAL_ID = "specialId";
  public static final String FOLLOW_ID = "followId";
  public static final String UNIQUE_ID = "uniqueId";
  public static final String HOST_ID = "hostId";
  public static final String ORIGINAL_SIZE = "originalSize";
  public static final String RESULTS = "results";
  public static final String WAARP_MONITOR = "waarpMonitor";
  public static final String FROM_DATE_TIME = "from";
  public static final String TO_DATE_TIME = "to";
  public static final String INDEX_NAME = "index";

  private final boolean intervalMonitoringIncluded;
  private final boolean transformLongAsString;
  private final boolean asApiRest;
  private final HttpMonitoringExporterClient httpMonitoringExporterClient;
  private final ElasticsearchMonitoringExporterClient
      elasticsearchMonitoringExporterClient;
  private final DbHostConfiguration hostConfiguration;

  private DateTime lastDateTime;
  private Timestamp lastTimestamp;

  /**
   * Note that only one among (basicAuthent, token, apikey) is allowed and
   * will be taken into account.
   *
   * @param remoteBaseUrl as 'http://myhost.com:8080' or 'https://myhost.com:8443'
   * @param endpoint as '/waarpr66monitor' or simply '/'
   * @param basicAuthent Basic Authent in Base64 format to connect to
   *     REST API if any (Basic authentication from 'username:paswwd')
   *     (nullable)
   * @param token access token (Bearer Token authorization
   *     by Header) (nullable)
   * @param apiKey API Key (Base64 of 'apiId:apiKey') (ApiKey authorization
   *     by Header) (nullable)
   * @param keepConnection True to keep the connexion opened, False to release the connexion each time
   * @param intervalMonitoringIncluded True to include the interval information within 'waarpMonitor' field
   * @param transformLongAsString True to transform Long as String (ELK)
   * @param group the EventLoopGroup to use for HttpMonitoringExporterClient
   *
   * @throws IllegalArgumentException if the setup is in error
   */
  public MonitorExporterTransfers(final String remoteBaseUrl,
                                  final String endpoint,
                                  final String basicAuthent, final String token,
                                  final String apiKey,
                                  final boolean keepConnection,
                                  final boolean intervalMonitoringIncluded,
                                  final boolean transformLongAsString,
                                  final EventLoopGroup group) {
    try {
      ParametersChecker.checkSanityString(remoteBaseUrl, endpoint);
    } catch (final InvalidArgumentException e) {
      throw new IllegalArgumentException(e);
    }
    if (ParametersChecker.isEmpty(remoteBaseUrl)) {
      throw new IllegalArgumentException("RemoteBaseUrl cannot be null");
    }
    this.intervalMonitoringIncluded = intervalMonitoringIncluded;
    this.transformLongAsString = transformLongAsString;
    this.asApiRest = true;
    this.elasticsearchMonitoringExporterClient = null;
    this.httpMonitoringExporterClient =
        new HttpMonitoringExporterClient(remoteBaseUrl, basicAuthent, token,
                                         apiKey, endpoint, keepConnection,
                                         group);
    DbHostConfiguration temp = null;
    try {
      temp = new DbHostConfiguration(Configuration.configuration.getHostId());
    } catch (final WaarpDatabaseException e) {//NOSONAR
      logger.error(e.getMessage());
    }
    if (temp == null) {
      DbHostConfiguration.getLastDateTimeMonitoring(
          Configuration.configuration.getHostId());
      try {
        temp = new DbHostConfiguration(Configuration.configuration.getHostId());
      } catch (final WaarpDatabaseException e) {//NOSONAR
        logger.error(e.getMessage());
      }
    }
    this.hostConfiguration = temp;
    lastDateTime = hostConfiguration.getLastDateTimeMonitoring();//NOSONAR
    if (lastDateTime != null) {
      lastTimestamp = new Timestamp(lastDateTime.getMillis());
    }
  }

  /**
   * The index can be a combination of a fixed name and extra dynamic
   * information:<br>
   * <ul>
   *   <li>%%WAARPHOST%% to be replaced by R66 host name</li>
   *   <li>%%DATETIME%% to be replaced by date in format YYYY.MM.dd.HH.mm</li>
   *   <li>%%DATEHOUR%% to be replaced by date in format YYYY.MM.dd.HH</li>
   *   <li>%%DATE%% to be replaced by date in format YYYY.MM.dd</li>
   *   <li>%%YEARMONTH%% to be replaced by date in format YYYY.MM</li>
   *   <li>%%YEAR%% to be replaced by date in format YYYY</li>
   * </ul>
   * <br>DATE is about current last-time check.<br>
   * So 'waarpr66-%%WAARPHOST%%-%%DATE%%' will give for instance
   * 'waarpr66-hosta-2021-06-21' as index name.<br>
   * Note that only one among (username/pwd, token, apikey) is allowed and
   * will be taken into account.
   *
   * @param remoteBaseUrl as 'http://myelastic.com:9200' or 'https://myelastic.com:9201'
   * @param prefix as '/prefix' or null if none
   * @param index as 'waarpr66monitor' as the index name within
   *     Elasticsearch, including extra dynamic information
   * @param username username to connect to Elasticsearch if any (Basic
   *     authentication) (nullable)
   * @param pwd password to connect to Elasticsearch if any (Basic
   *     authentication) (nullable)
   * @param token access token (Bearer Token authorization
   *     by Header) (nullable)
   * @param apiKey API Key (Base64 of 'apiId:apiKey') (ApiKey authorization
   *     by Header) (nullable)
   * @param intervalMonitoringIncluded True to include the interval information within 'waarpMonitor' field
   * @param transformLongAsString True to transform Long as String (ELK)
   * @param compression True to compress REST exchanges between the client
   *     and the Elasticsearch server
   *
   * @throws IllegalArgumentException if the setup is in error
   */
  public MonitorExporterTransfers(final String remoteBaseUrl,
                                  final String prefix, final String index,
                                  final String username, final String pwd,
                                  final String token, final String apiKey,
                                  final boolean intervalMonitoringIncluded,
                                  final boolean transformLongAsString,
                                  final boolean compression) {
    try {
      ParametersChecker.checkSanityString(remoteBaseUrl, index);
    } catch (final InvalidArgumentException e) {
      throw new IllegalArgumentException(e);
    }
    if (ParametersChecker.isEmpty(remoteBaseUrl, index)) {
      throw new IllegalArgumentException(
          "RemoteBaseUrl or Index cannot be null");
    }
    this.intervalMonitoringIncluded = intervalMonitoringIncluded;
    this.transformLongAsString = transformLongAsString;
    this.asApiRest = false;
    this.httpMonitoringExporterClient = null;
    final String[] urls = remoteBaseUrl.split(",");
    final ArrayList<HttpHost> httpHostArray =
        new ArrayList<HttpHost>(urls.length);
    for (final String url : urls) {
      try {
        final URI finalUri = new URI(url);
        final String host =
            finalUri.getHost() == null? "127.0.0.1" : finalUri.getHost();
        final int port = finalUri.getPort();
        final String scheme =
            finalUri.getScheme() == null? "http" : finalUri.getScheme();
        logger.info("Elasticsearch from {} Host: {} on port {} using {}", url,
                    host, port, scheme);
        httpHostArray.add(new HttpHost(host, port, scheme));
      } catch (final URISyntaxException e) {
        logger.error("URI syntax error: {}", e.getMessage());
        throw new IllegalArgumentException(e);
      }
    }
    this.elasticsearchMonitoringExporterClient =
        ElasticsearchMonitoringExporterClientBuilder.getFactory()
                                                    .createElasticsearchClient(
                                                        username, pwd, token,
                                                        apiKey, prefix, index,
                                                        compression,
                                                        httpHostArray.toArray(
                                                            new HttpHost[0]));
    DbHostConfiguration temp = null;
    try {
      temp = new DbHostConfiguration(Configuration.configuration.getHostId());
    } catch (final WaarpDatabaseException e) {//NOSONAR
      logger.error(e.getMessage());
    }
    if (temp == null) {
      DbHostConfiguration.getLastDateTimeMonitoring(
          Configuration.configuration.getHostId());
      try {
        temp = new DbHostConfiguration(Configuration.configuration.getHostId());
      } catch (final WaarpDatabaseException e) {//NOSONAR
        logger.error(e.getMessage());
      }
    }
    this.hostConfiguration = temp;
    lastDateTime = hostConfiguration.getLastDateTimeMonitoring();//NOSONAR
    if (lastDateTime != null) {
      lastTimestamp = new Timestamp(lastDateTime.getMillis());
    }
  }

  @Override
  public void run() {
    final DateTime now = new DateTime();
    final Timestamp timestamp = new Timestamp(now.getMillis());
    logger.info("Start from {} to {}", lastDateTime, now);
    final TransferConverter.Order order = TransferConverter.Order.ascId;
    final List<Filter> filters = new ArrayList<Filter>(2);
    filters.add(DbTaskRunner.getOwnerFilter());
    if (lastTimestamp != null) {
      filters.add(new Filter(TRANSFER_STOP_FIELD, Filter.BETWEEN, lastTimestamp,
                             timestamp));
    } else {
      filters.add(new Filter(TRANSFER_STOP_FIELD, "<=", timestamp));
    }
    TransferDAO transferDAO = null;
    List<Transfer> transferList;
    try {
      transferDAO = DAO_FACTORY.getTransferDAO();
      transferList = transferDAO.find(filters, order.column, order.ascend);
      logger.debug("Get List {}", transferList.size());
    } catch (final DAOConnectionException e) {
      logger.error(e.getMessage());
      throw new InternalServerErrorException(e);
    } finally {
      DAOFactory.closeDAO(transferDAO);
    }
    if (transferList.isEmpty()) {
      logger.info("No Transfer from {} to {}", lastDateTime, now);
      lastDateTime = now;
      lastTimestamp = timestamp;
      hostConfiguration.updateLastDateTimeMonitoring(lastDateTime);
      return;
    }
    logger.debug("Create Json");

    final ObjectNode monitoredTransfers = JsonHandler.createObjectNode();
    final ArrayNode resultList = monitoredTransfers.putArray(RESULTS);
    final String owner = Configuration.configuration.getHostId();
    for (final Transfer transfer : transferList) {
      final ObjectNode item = TransferConverter.transferToNode(transfer);
      final long specialId = item.get(TransferFields.TRANSFER_ID).asLong();
      final String transferInfo =
          item.get(TransferFields.TRANSFER_INFO).asText();
      final ObjectNode root = JsonHandler.getFromString(transferInfo);
      long followId = Long.MIN_VALUE;
      long originalSize = -1;
      if (root != null) {
        JsonNode node = root.get(TransferArgs.FOLLOW_JSON_KEY);
        if (node != null) {
          followId = node.asLong();
        }
        node = root.get(DbTaskRunner.JSON_ORIGINALSIZE);
        if (node != null) {
          originalSize = node.asLong();
        }
      }
      if (transformLongAsString) {
        item.put(SPECIAL_ID, Long.toString(specialId));
        item.put(FOLLOW_ID, Long.toString(followId));
        item.put(ORIGINAL_SIZE, Long.toString(originalSize));
      } else {
        item.put(SPECIAL_ID, specialId);
        item.put(FOLLOW_ID, followId);
        item.put(ORIGINAL_SIZE, originalSize);
      }
      final String uniqueId =
          owner + "." + item.get(TransferFields.REQUESTER).asText() + "." +
          item.get(TransferFields.REQUESTED).asText() + "." + specialId;
      item.put(UNIQUE_ID, uniqueId);
      item.put(HOST_ID, owner);
      item.remove(TransferFields.TRANSFER_ID);
      if (intervalMonitoringIncluded) {
        final ObjectNode waarpMonitor = item.putObject(WAARP_MONITOR);
        waarpMonitor.put(FROM_DATE_TIME,
                         lastDateTime != null? lastDateTime.toString() : "");
        waarpMonitor.put(TO_DATE_TIME, now.toString());
        waarpMonitor.put(INDEX_NAME, owner.toLowerCase());
      }
      resultList.add(item);
    }
    final int size = resultList.size();
    logger.debug("Create Json {}", size);
    transferList.clear();
    if (asApiRest) {
      if (httpMonitoringExporterClient.post(monitoredTransfers, lastDateTime,
                                            now,
                                            Configuration.configuration.getHostId())) {
        logger.info("Transferred from {} to {} = {}", lastDateTime, now, size);
        lastDateTime = now;
        lastTimestamp = timestamp;
        hostConfiguration.updateLastDateTimeMonitoring(lastDateTime);
      } else {
        logger.error("Not Transferred from {} to {} = {}", lastDateTime, now,
                     size);
      }
    } else if (elasticsearchMonitoringExporterClient.post(monitoredTransfers,
                                                          lastDateTime, now,
                                                          Configuration.configuration.getHostId())) {
      logger.info("ES Transferred from {} to {} = {}", lastDateTime, now, size);
      lastDateTime = now;
      lastTimestamp = timestamp;
      hostConfiguration.updateLastDateTimeMonitoring(lastDateTime);
    } else {
      logger.error("ES Not Transferred from {} to {} = {}", lastDateTime, now,
                   size);
    }
  }

  @Override
  public void close() throws IOException {
    if (httpMonitoringExporterClient != null) {
      httpMonitoringExporterClient.close();
    }
    if (elasticsearchMonitoringExporterClient != null) {
      elasticsearchMonitoringExporterClient.close();
    }
  }
}
