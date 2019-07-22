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
package org.waarp.common.state;

import java.util.EnumSet;

/**
 * Transition object that joins one state and one set of acceptable following
 * states <br>
 * <br>
 * Note: the type EnumSet< ? > is in fact of type EnumSet< EnumState >
 *
 *
 */
public class Transition<EnumState> {

  private EnumState state;
  private EnumSet<?> set;

  public Transition(EnumState state, EnumSet<?> set) {
    setState(state);
    setSet(set);
  }

  /**
   * @return the state
   */
  public EnumState getState() {
    return state;
  }

  /**
   * @param state the state to set
   */
  public void setState(EnumState state) {
    this.state = state;
  }

  /**
   * @return the set
   */
  public EnumSet<?> getSet() {
    return set;
  }

  /**
   * @param set the set to set
   */
  public void setSet(EnumSet<?> set) {
    this.set = set;
  }
}
