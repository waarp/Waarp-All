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

/**
 * Autogenerated by Thrift Compiler (0.9.0)
 * <p>
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 */
package org.waarp.thrift.r66;

import org.apache.thrift.TEnum;

public enum Action implements TEnum {
  Exist(1), Cancel(2), Stop(3), Restart(4), Detail(10), List(11), Mlsx(12);

  private final int value;

  Action(final int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  @Override
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   *
   * @return null if the value is not found.
   */
  public static Action findByValue(final int value) {
    switch (value) {
      case 1:
        return Exist;
      case 2:
        return Cancel;
      case 3:
        return Stop;
      case 4:
        return Restart;
      case 10:
        return Detail;
      case 11:
        return List;
      case 12:
        return Mlsx;
      default:
        return null;
    }
  }
}
