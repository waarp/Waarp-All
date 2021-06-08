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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;

import java.io.Closeable;

public interface ElasticsearchClient extends Closeable {

  /**
   * Will use a "bulk" request to upsert data into ElasticSearch
   *
   * @param monitoredTransfers the Json objet to push as POST
   * @param start the DateTime for the 'from' interval
   * @param stop the DateTime for the 'to' interval
   * @param serverId the serverId that is sending this monitoring information
   *
   * @return True if the Bulk upsert succeeded
   */
  boolean post(final ObjectNode monitoredTransfers, final DateTime start,
               final DateTime stop, final String serverId);
}
