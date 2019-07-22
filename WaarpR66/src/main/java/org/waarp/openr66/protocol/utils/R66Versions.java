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

package org.waarp.openr66.protocol.utils;

public enum R66Versions {
  /**
   * Not usable for extra information
   */
  V2_4_12,
  /**
   * Introducing different separator, adding HostConfiguration table
   */
  V2_4_13,
  /**
   * Add TransferInformation to TaskRunner table
   */
  V2_4_17,
  /**
   * Add IsActive on DbHostAuth table
   */
  V2_4_23,
  /**
   * Change VARCHAR(255) to VARCHAR(8096)
   */
  V2_4_25,
  /**
   * Add support for FileInformation change
   */
  V3_0_4;

  public String getVersion() {
    return name().substring(1).replace('_', '.');
  }
}