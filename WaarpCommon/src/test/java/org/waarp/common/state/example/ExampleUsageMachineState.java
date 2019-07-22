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
package org.waarp.common.state.example;

import org.waarp.common.exception.IllegalFiniteStateException;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.state.MachineState;
import org.waarp.common.state.example.ExampleEnumState.ExampleTransition;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Example of usage for MachineState based on ExampleEnumState enum class
 *
 *
 */
public class ExampleUsageMachineState {
  /**
   * An example of usage.
   *
   * @param args
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));

    // Example

    // First create a HashMap and fill it directly
    final ConcurrentHashMap<ExampleEnumState, EnumSet<ExampleEnumState>>
        stateMap =
        new ConcurrentHashMap<ExampleEnumState, EnumSet<ExampleEnumState>>();
    stateMap.put(ExampleTransition.tRUNNING.elt.getState(),
                 (EnumSet<ExampleEnumState>) ExampleTransition.tRUNNING.elt
                     .getSet());
    // Second create the MachineState with the right Map
    final MachineState<ExampleEnumState> machineState1 =
        new MachineState(ExampleEnumState.PAUSED, stateMap);
    // Third, if not already done, fill the Map with the transitions
    for (final ExampleTransition trans : ExampleTransition.values()) {
      machineState1.addNewAssociation(trans.elt);
    }

    // Or First create the MachineSate but empty
    final MachineState<ExampleEnumState> machineState2 =
        new MachineState(ExampleEnumState.PAUSED);
    // Second fill the associations with transitions since none exist yet
    for (final ExampleTransition trans : ExampleTransition.values()) {
      machineState2.addNewAssociation(trans.elt);
    }

    System.out.println("Machine1 states...");
    changeState(machineState1, ExampleEnumState.CONFIGURING);
    changeState(machineState1, ExampleEnumState.PAUSED);
    changeState(machineState1, ExampleEnumState.RUNNING);
    changeState(machineState1, ExampleEnumState.ENDED);
    changeState(machineState1, ExampleEnumState.PAUSED);
    changeState(machineState1, ExampleEnumState.RESET);
    changeState(machineState1, ExampleEnumState.PAUSED);

    System.out.println("Machine2 states...");
    changeState(machineState2, ExampleEnumState.CONFIGURING);
    changeState(machineState2, ExampleEnumState.PAUSED);
    changeState(machineState2, ExampleEnumState.RUNNING);
    changeState(machineState2, ExampleEnumState.ENDED);
    changeState(machineState2, ExampleEnumState.PAUSED);
    changeState(machineState2, ExampleEnumState.RESET);
    changeState(machineState2, ExampleEnumState.PAUSED);
  }

  private static void changeState(MachineState<ExampleEnumState> mach,
                                  ExampleEnumState desired) {
    try {
      printState(mach);
      mach.setCurrent(desired);
      printState(mach);
    } catch (final IllegalFiniteStateException e) {
      printWrongState(mach, desired);
    }
  }

  private static void printState(MachineState<ExampleEnumState> mach) {
    System.out.println("State is " + mach.getCurrent());
  }

  private static void printWrongState(MachineState<ExampleEnumState> mach,
                                      ExampleEnumState desired) {
    System.out.println(
        "Cannot go from State " + mach.getCurrent() + " to State " + desired);
  }

}
