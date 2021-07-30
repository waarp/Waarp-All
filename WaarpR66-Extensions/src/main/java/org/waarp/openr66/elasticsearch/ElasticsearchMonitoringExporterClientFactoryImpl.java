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

import org.apache.http.HttpHost;
import org.waarp.openr66.protocol.monitoring.ElasticsearchMonitoringExporterClient;
import org.waarp.openr66.protocol.monitoring.ElasticsearchMonitoringExporterClientFactory;

public class ElasticsearchMonitoringExporterClientFactoryImpl
    implements ElasticsearchMonitoringExporterClientFactory {
  @Override
  public final ElasticsearchMonitoringExporterClient createElasticsearchClient(
      final String username, final String pwd, final String token,
      final String apiKey, final String prefix, final String index,
      final boolean compression, final HttpHost... httpHosts) {
    return new ElasticsearchMonitoringExporterClientImpl(username, pwd, token,
                                                         apiKey, prefix, index,
                                                         compression,
                                                         httpHosts);
  }
}
