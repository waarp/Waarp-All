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
package org.waarp.gateway.ftp.snmp;

import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.snmp.interf.WaarpCounter32;

/**
 * Ftp Exec Counter32 SNMP implementation
 *
 *
 */
class FtpCounter32 extends WaarpCounter32 {

  /**
   *
   */
  private static final long serialVersionUID = -4099770842827593900L;
  private int type = 1;
  private final int entry;

  public FtpCounter32(int type, int entry) {
    this.type = type;
    this.entry = entry;
    setInternalValue();
  }

  public FtpCounter32(int type, int entry, long value) {
    this.type = type;
    this.entry = entry;
    setInternalValue(value);
  }

  @Override
  protected void setInternalValue() {
    FileBasedConfiguration.fileBasedConfiguration.monitoring.run(type, entry);
  }

  @Override
  protected void setInternalValue(long value) {
    setValue(value);
  }
}
