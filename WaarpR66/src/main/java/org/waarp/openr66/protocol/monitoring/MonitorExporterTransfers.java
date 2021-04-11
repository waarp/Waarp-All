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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.EventLoopGroup;
import org.joda.time.DateTime;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.waarp.openr66.dao.database.DBTransferDAO.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;

/**
 * The Monitor exports Transfers into a Json Format to a remote API REST
 * in order to allow to monitor multiple Waarp Servers from one central
 * monitoring, such as using Elasticsearch with Kibana/Grafana, through
 * a Logstash engine\n\n<br>
 * \n<br>
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
 */
public class MonitorExporterTransfers extends Thread {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(MonitorExporterTransfers.class);

  public static final boolean MONITOR_KEEP_CONNECTION_DEFAULT = true;
  public static final boolean MONITOR_INTERVAL_INCLUDED_DEFAULT = true;
  public static final boolean MONITOR_LONG_AS_STRING_DEFAULT = false;

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
  private final HttpClient httpClient;
  private final DbHostConfiguration hostConfiguration;

  private DateTime lastDateTime;
  private Timestamp lastTimestamp;

  /**
   * @param remoteBaseUrl as 'http://myhost.com:8080' or 'https://myhost.com:8443'
   * @param endpoint as '/waarpr66monitor' or simply '/'
   * @param keepConnection True to keep the connexion opened, False to release the connexion each time
   * @param intervalMonitoringIncluded True to include the interval information within 'waarpMonitor' field
   * @param transformLongAsString True to transform Long as String (ELK)
   * @param group the EventLoopGroup to use
   */
  public MonitorExporterTransfers(final String remoteBaseUrl,
                                  final String endpoint,
                                  final boolean keepConnection,
                                  final boolean intervalMonitoringIncluded,
                                  final boolean transformLongAsString,
                                  final EventLoopGroup group) {
    this.intervalMonitoringIncluded = intervalMonitoringIncluded;
    this.transformLongAsString = transformLongAsString;
    this.httpClient =
        new HttpClient(remoteBaseUrl, endpoint, keepConnection, group);
    DbHostConfiguration temp = null;
    try {
      temp = new DbHostConfiguration(Configuration.configuration.getHostId());
    } catch (WaarpDatabaseException e) {
      logger.error(e);
    }
    if (temp == null) {
      DbHostConfiguration
          .getLastDateTimeMonitoring(Configuration.configuration.getHostId());
      try {
        temp = new DbHostConfiguration(Configuration.configuration.getHostId());
      } catch (WaarpDatabaseException e) {
        logger.error(e);
      }
    }
    this.hostConfiguration = temp;
    lastDateTime = hostConfiguration.getLastDateTimeMonitoring();
    if (lastDateTime != null) {
      lastTimestamp = new Timestamp(lastDateTime.getMillis());
    }
  }

  @Override
  public void run() {
    DateTime now = new DateTime();
    Timestamp timestamp = new Timestamp(now.getMillis());
    logger.info("Start from {} to {}", lastDateTime, now);
    TransferConverter.Order order = TransferConverter.Order.ascId;
    final List<Filter> filters = new ArrayList<Filter>();
    if (lastTimestamp != null) {
      filters.add(new Filter(TRANSFER_STOP_FIELD, ">=", lastTimestamp));
    }
    filters.add(new Filter(TRANSFER_STOP_FIELD, "<=", timestamp));
    TransferDAO transferDAO = null;
    List<Transfer> transferList;
    try {
      transferDAO = DAO_FACTORY.getTransferDAO();
      transferList = transferDAO.find(filters, order.column, order.ascend);
      logger.debug("Get List {}", transferList.size());
    } catch (final DAOConnectionException e) {
      logger.error(e);
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

    final ObjectNode monitoredTransfers =
        new ObjectNode(JsonNodeFactory.instance);
    final ArrayNode resultList = monitoredTransfers.putArray(RESULTS);
    final String owner = Configuration.configuration.getHostId();
    for (final Transfer transfer : transferList) {
      ObjectNode item = TransferConverter.transferToNode(transfer);
      long specialId = item.get(TransferFields.TRANSFER_ID).asLong();
      String transferInfo = item.get(TransferFields.TRANSFER_INFO).asText();
      ObjectNode root = JsonHandler.getFromString(transferInfo);
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
      String uniqueId =
          owner + "." + item.get(TransferFields.REQUESTER).asText() + "." +
          item.get(TransferFields.REQUESTED).asText() + "." + specialId;
      item.put(UNIQUE_ID, uniqueId);
      item.put(HOST_ID, owner);
      item.remove(TransferFields.TRANSFER_ID);
      if (intervalMonitoringIncluded) {
        ObjectNode waarpMonitor = item.putObject(WAARP_MONITOR);
        waarpMonitor.put(FROM_DATE_TIME,
                         lastDateTime != null? lastDateTime.toString() : "");
        waarpMonitor.put(TO_DATE_TIME, now.toString());
        waarpMonitor.put(INDEX_NAME, owner.toLowerCase());
      }
      resultList.add(item);
    }
    int size = resultList.size();
    logger.debug("Create Json {}", size);
    transferList.clear();
    if (httpClient.post(monitoredTransfers, lastDateTime, now,
                        Configuration.configuration.getHostId())) {
      logger.warn("Transferred from {} to {} = {}", lastDateTime, now, size);
      lastDateTime = now;
      lastTimestamp = timestamp;
      hostConfiguration.updateLastDateTimeMonitoring(lastDateTime);
    } else {
      logger
          .error("Not Transferred from {} to {} = {}", lastDateTime, now, size);
    }
  }
}
