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
package org.waarp.snmp.utils;

import org.snmp4j.agent.MOAccess;

/**
 * Entry for MIB definition
 */
public class WaarpEntry {
  /**
   * ConstantsType as defined in SNMP4J
   */
  final int smiConstantsType;
  /**
   * Access as defined in SNMP4J
   */
  final MOAccess access;

  /**
   * @param smiConstantsType as defined in SNMP4J
   * @param access as defined in SNMP4J
   */
  public WaarpEntry(final int smiConstantsType, final MOAccess access) {
    this.smiConstantsType = smiConstantsType;
    this.access = access;
  }

}
