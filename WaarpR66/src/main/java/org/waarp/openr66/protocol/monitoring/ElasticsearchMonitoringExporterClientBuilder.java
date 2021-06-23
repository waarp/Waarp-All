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

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.WaarpSystemUtil;

/**
 * Builder of ElasticsearchMonitoringExporterClientFactory
 */
public class ElasticsearchMonitoringExporterClientBuilder {
  private static ElasticsearchMonitoringExporterClientFactory factory = null;

  public static final String ELASTIC_WAARPHOST = "%%WAARPHOST%%";
  public static final String ELASTIC_DATETIME = "%%DATETIME%%";
  public static final DateTimeFormatter FORMAT_DATETIME =
      DateTimeFormat.forPattern("yyyy.MM.dd.HH.mm");
  public static final String ELASTIC_DATEHOUR = "%%DATEHOUR%%";
  public static final DateTimeFormatter FORMAT_DATEHOUR =
      DateTimeFormat.forPattern("yyyy.MM.dd.HH");
  public static final String ELASTIC_DATE = "%%DATE%%";
  public static final DateTimeFormatter FORMAT_DATE =
      DateTimeFormat.forPattern("yyyy.MM.dd");
  public static final String ELASTIC_YEAR_MONTH = "%%YEARMONTH%%";
  public static final DateTimeFormatter FORMAT_YEAR_MONTH =
      DateTimeFormat.forPattern("yyyy.MM");
  public static final String ELASTIC_YEAR = "%%YEAR%%";
  public static final DateTimeFormatter FORMAT_YEAR =
      DateTimeFormat.forPattern("yyyy");

  public static final String ELASTICSEARCH_CLIENT_FACTORY_IMPL =
      "org.waarp.openr66.elasticsearch.ElasticsearchMonitoringExporterClientFactoryImpl";

  static {
    // Try to load ElasticsearchFactory if class exists
    try {
      Class elasticsearchFactoryClass =
          Class.forName(ELASTICSEARCH_CLIENT_FACTORY_IMPL);
      factory = (ElasticsearchMonitoringExporterClientFactory) WaarpSystemUtil
          .newInstance(elasticsearchFactoryClass);
    } catch (Exception ignore) {
      // Not found and ignore
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignore);
    }
  }

  /**
   * Set from JRE8 or greather with a valid ElasticsearchMonitoringExporterClientFactory
   *
   * @param factoryToSet
   */
  public static void setFactory(
      final ElasticsearchMonitoringExporterClientFactory factoryToSet) {
    factory = factoryToSet;
  }

  /**
   * @return the current ElasticsearchMonitoringExporterClientFactory
   *
   * @throws IllegalArgumentException if no Factory is setup
   */
  public static ElasticsearchMonitoringExporterClientFactory getFactory() {
    if (factory == null) {
      throw new IllegalArgumentException(
          "No Factory setup for Elasticsearch client");
    } else {
      return factory;
    }
  }
}
