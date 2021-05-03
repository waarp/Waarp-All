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

package org.waarp.openr66.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;

import java.sql.Types;

import static org.waarp.common.database.data.AbstractDbData.*;

/**
 * MultipleMonitor data object
 */
public class MultipleMonitor {

  private String hostid;

  private int countConfig;

  private int countHost;

  private int countRule;

  /**
   * Empty constructor
   */
  public MultipleMonitor() {
    // Nothing
  }

  public MultipleMonitor(final String hostid, final int countConfig,
                         final int countHost, final int countRule)
      throws WaarpDatabaseSqlException {
    this.hostid = hostid;
    this.countConfig = countConfig;
    this.countHost = countHost;
    this.countRule = countRule;
    checkValues();
  }

  @JsonIgnore
  public void checkValues() throws WaarpDatabaseSqlException {
    validateLength(Types.NVARCHAR, hostid);
  }

  public String getHostid() {
    return hostid;
  }

  public void setHostid(final String hostid) {
    this.hostid = hostid;
  }

  public int getCountConfig() {
    return countConfig;
  }

  public void setCountConfig(final int countConfig) {
    this.countConfig = countConfig;
  }

  public int getCountHost() {
    return countHost;
  }

  public void setCountHost(final int countHost) {
    this.countHost = countHost;
  }

  public int getCountRule() {
    return countRule;
  }

  public void setCountRule(final int countRule) {
    this.countRule = countRule;
  }
}
