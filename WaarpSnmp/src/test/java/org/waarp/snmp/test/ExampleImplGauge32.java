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
package org.waarp.snmp.test;

import org.snmp4j.smi.OID;
import org.waarp.snmp.interf.WaarpGauge32;

/**
 * Example of WaarpGauge32 Usage
 */
@SuppressWarnings("serial")
public class ExampleImplGauge32 extends WaarpGauge32 {
  /**
   *
   */
  private static final long serialVersionUID = -3267743878853550376L;

  public static final long STARTUP = 42;

  public OID oid;

  long _internalValue = STARTUP;

  /**
   *
   */
  public ExampleImplGauge32(OID oid) {
    this.oid = oid;
  }

  /**
   *
   */
  public ExampleImplGauge32(OID oid, long value) {
    super(value);
    this.oid = oid;
  }

  @Override
  protected final void setInternalValue() {
    _internalValue++;
    setValue(_internalValue);
  }

  @Override
  protected final void setInternalValue(long value) {
    _internalValue = value;
    setValue(_internalValue);
  }
}
