/*******************************************************************************
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.waarp.common.state;

import org.junit.Test;
import org.waarp.common.exception.IllegalFiniteStateException;
import org.waarp.common.state.MachineStateTest.ExampleEnumState.ExampleTransition;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

public class MachineStateTest {
  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testMachineStateEnumStateConcurrentHashMapOfEnumStateEnumSetOfQ() {
    // First create a HashMap and fill it directly
    ConcurrentHashMap<ExampleEnumState, EnumSet<ExampleEnumState>> stateMap =
        new ConcurrentHashMap<ExampleEnumState, EnumSet<ExampleEnumState>>();
    stateMap.put(ExampleTransition.tRUNNING.elt.getState(),
                 (EnumSet<ExampleEnumState>) ExampleTransition.tRUNNING.elt
                     .getSet());
    // Second create the MachineState with the right Map
    MachineState<ExampleEnumState> machineState1 =
        new MachineState(ExampleEnumState.PAUSED, stateMap);
    // Third, if not already done, fill the Map with the transitions
    for (ExampleTransition trans : ExampleTransition.values()) {
      machineState1.addNewAssociation(trans.elt);
    }
    System.out.println("Machine1 states...");
    assertTrue(changeState(machineState1, ExampleEnumState.CONFIGURING));
    assertTrue(changeState(machineState1, ExampleEnumState.PAUSED));
    assertTrue(changeState(machineState1, ExampleEnumState.RUNNING));
    assertTrue(changeState(machineState1, ExampleEnumState.ENDED));
    assertFalse(changeState(machineState1, ExampleEnumState.PAUSED));
    assertTrue(changeState(machineState1, ExampleEnumState.RESET));
    assertTrue(changeState(machineState1, ExampleEnumState.PAUSED));
  }

  static private boolean changeState(MachineState<ExampleEnumState> mach,
                                     ExampleEnumState desired) {
    try {
      printState(mach);
      mach.setCurrent(desired);
      printState(mach);
      return true;
    } catch (IllegalFiniteStateException e) {
      printWrongState(mach, desired);
      return false;
    }
  }

  static private final void printState(MachineState<ExampleEnumState> mach) {
    System.out.println("State is " + mach.getCurrent());
  }

  static private final void printWrongState(MachineState<ExampleEnumState> mach,
                                            ExampleEnumState desired) {
    System.out.println(
        "Cannot go from State " + mach.getCurrent() + " to State " + desired);
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void testMachineStateEnumState() {
    // Or First create the MachineSate but empty
    MachineState<ExampleEnumState> machineState2 =
        new MachineState(ExampleEnumState.PAUSED);
    // Second fill the associations with transitions since none exist yet
    for (ExampleTransition trans : ExampleTransition.values()) {
      machineState2.addNewAssociation(trans.elt);
    }
    System.out.println("Machine2 states...");
    assertTrue(changeState(machineState2, ExampleEnumState.CONFIGURING));
    assertTrue(changeState(machineState2, ExampleEnumState.PAUSED));
    assertTrue(changeState(machineState2, ExampleEnumState.RUNNING));
    assertTrue(changeState(machineState2, ExampleEnumState.ENDED));
    assertFalse(changeState(machineState2, ExampleEnumState.PAUSED));
    assertTrue(changeState(machineState2, ExampleEnumState.RESET));
    assertTrue(changeState(machineState2, ExampleEnumState.PAUSED));
  }

  public static enum ExampleEnumState {
    RUNNING, PAUSED, CONFIGURING, RESET, ENDED;

    public static enum ExampleTransition {
      tRUNNING(RUNNING, EnumSet.of(PAUSED, ENDED)),
      tPAUSED(PAUSED, EnumSet.of(RUNNING, RESET, CONFIGURING)),
      tENDED(ENDED, EnumSet.of(RESET)),
      tCONFIGURING(CONFIGURING, EnumSet.of(PAUSED)),
      tRESET(RESET, EnumSet.of(PAUSED, RESET));

      public Transition<ExampleEnumState> elt;

      private ExampleTransition(ExampleEnumState state,
                                EnumSet<ExampleEnumState> set) {
        this.elt = new Transition<ExampleEnumState>(state, set);
      }
    }

  }

}
