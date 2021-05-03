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
import com.fasterxml.jackson.annotation.JsonProperty;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;

import java.sql.Types;

import static org.waarp.common.database.data.AbstractDbData.*;

/**
 * Limit data object
 */
public class Limit {

  @JsonProperty("HOSTID")
  private String hostid;

  @JsonProperty("READGLOBALLIMIT")
  private long readGlobalLimit;

  @JsonProperty("WRITEGLOBALLIMIT")
  private long writeGlobalLimit;

  @JsonProperty("READSESSIONLIMIT")
  private long readSessionLimit;

  @JsonProperty("WRITESESSIONLIMIT")
  private long writeSessionLimit;

  @JsonProperty("DELAYLIMIT")
  private long delayLimit;

  @JsonProperty("UPDATEDINFO")
  private UpdatedInfo updatedInfo = UpdatedInfo.UNKNOWN;

  /**
   * Empty constructor
   */
  public Limit() {
    // Nothing
  }

  public Limit(final String hostid, final long delayLimit,
               final long readGlobalLimit, final long writeGlobalLimit,
               final long readSessionLimit, final long writeSessionLimit,
               final UpdatedInfo updatedInfo) throws WaarpDatabaseSqlException {
    this(hostid, delayLimit, readGlobalLimit, writeGlobalLimit,
         readSessionLimit, writeSessionLimit);
    this.updatedInfo = updatedInfo;
  }

  public Limit(final String hostid, final long delayLimit,
               final long readGlobalLimit, final long writeGlobalLimit,
               final long readSessionLimit, final long writeSessionLimit)
      throws WaarpDatabaseSqlException {
    this.hostid = hostid;
    this.delayLimit = delayLimit;
    this.readGlobalLimit = readGlobalLimit;
    this.writeGlobalLimit = writeGlobalLimit;
    this.readSessionLimit = readSessionLimit;
    this.writeSessionLimit = writeSessionLimit;
    checkValues();
  }

  public Limit(final String hostid, final long delayLimit)
      throws WaarpDatabaseSqlException {
    this(hostid, delayLimit, 0, 0, 0, 0);
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

  public long getReadGlobalLimit() {
    return readGlobalLimit;
  }

  public void setReadGlobalLimit(final long readGlobalLimit) {
    this.readGlobalLimit = readGlobalLimit;
  }

  public long getWriteGlobalLimit() {
    return writeGlobalLimit;
  }

  public void setWriteGlobalLimit(final long writeGlobalLimit) {
    this.writeGlobalLimit = writeGlobalLimit;
  }

  public long getReadSessionLimit() {
    return readSessionLimit;
  }

  public void setReadSessionLimit(final long readSessionLimit) {
    this.readSessionLimit = readSessionLimit;
  }

  public long getWriteSessionLimit() {
    return writeSessionLimit;
  }

  public void setWriteSessionLimit(final long writeSessionLimit) {
    this.writeSessionLimit = writeSessionLimit;
  }

  public long getDelayLimit() {
    return delayLimit;
  }

  public void setDelayLimit(final long delayLimit) {
    this.delayLimit = delayLimit;
  }

  public UpdatedInfo getUpdatedInfo() {
    return updatedInfo;
  }

  public void setUpdatedInfo(final UpdatedInfo info) {
    updatedInfo = info;
  }
}
