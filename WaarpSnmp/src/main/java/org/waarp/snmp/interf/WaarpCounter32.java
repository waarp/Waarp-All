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
package org.waarp.snmp.interf;

import org.snmp4j.smi.Counter32;

/**
 * Generic Counter32 with update possibility for GoldenGate
 */
public abstract class WaarpCounter32 extends Counter32 {
  /**
   *
   */
  private static final long serialVersionUID = -2577821681860324820L;

  /**
   * Function to set the data before it is accessed by SNMP4J. This function
   * MUST call setValue(long)
   */
  protected abstract void setInternalValue();

  /**
   * Function to set the data before it is accessed by SNMP4J. This function
   * MUST call setValue(long)
   */
  protected abstract void setInternalValue(long value);

  protected WaarpCounter32() {
    setInternalValue();
  }

  protected WaarpCounter32(final long value) {
    setInternalValue(value);
  }

  @Override
  public final long getValue() {
    setInternalValue();
    return super.getValue();
  }

  @Override
  public final Object clone() {
    setInternalValue();
    return super.clone();
  }

  @Override
  public final boolean isDynamic() {
    return true;
  }

}
