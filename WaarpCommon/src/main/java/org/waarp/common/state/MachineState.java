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

import org.waarp.common.exception.IllegalFiniteStateException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This is the base class for the basic support of Finite State Machine in
 * GoldenGate. One need to implement
 * an Enum class to use with it. <br>
 * <br>
 * Note: the type EnumSet< ? > is in fact of type EnumSet< EnumState >
 *
 * @param <E>
 */
public class MachineState<E extends Enum<E>> {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(MachineState.class);

  private ConcurrentMap<E, EnumSet<E>> statemap;
  private E currentState;

  /**
   * Initialize with an initialState
   *
   * @param initialState initial MachineState
   * @param map the association of state and set of acceptable
   *     following
   *     states
   */
  public MachineState(final E initialState,
                      final ConcurrentMap<E, EnumSet<E>> map) {
    statemap = map;
    currentState = initialState;
  }

  /**
   * Initialize with an initialState but no association (Machine State is
   * empty)
   *
   * @param initialState initial MachineState
   */
  public MachineState(final E initialState) {
    statemap = new ConcurrentHashMap<E, EnumSet<E>>();
    currentState = initialState;
  }

  /**
   * Add a new association from one state to a set of acceptable following
   * states (can replace an existing
   * association)
   *
   * @param state
   * @param set the new association
   *
   * @return the previous association if any
   */
  public EnumSet<E> addNewAssociation(final E state, final EnumSet<E> set) {
    return statemap.put(state, set);
  }

  /**
   * Add a new association from one state to a set of acceptable following
   * states (can replace an existing
   * association)
   *
   * @param elt
   *
   * @return the previous association if any
   */
  public EnumSet<E> addNewAssociation(final Transition<E> elt) {
    return statemap.put(elt.getState(), elt.getSet());
  }

  /**
   * Remove an association from one state to any acceptable following states
   *
   * @param state the state to remove any acceptable following states
   *
   * @return the previous association if any
   */
  public EnumSet<E> removeAssociation(final E state) {
    return statemap.remove(state);
  }

  /**
   * Return the current application state.
   *
   * @return the current State
   */
  public E getCurrent() {
    return currentState;
  }

  /**
   * Sets the current application state.
   *
   * @param desiredState
   *
   * @return the requested state, if it was reachable
   *
   * @throws IllegalFiniteStateException if the state is not allowed
   */
  public E setCurrent(final E desiredState) throws IllegalFiniteStateException {
    if (!isReachable(desiredState)) {
      logger.debug(
          "State " + desiredState + " not reachable from: " + currentState);
      throw new IllegalFiniteStateException(
          desiredState + " not allowed from " + currentState);
    }
    return setAsFinal(desiredState);
  }

  /**
   * Sets the current application state, but no exception if not compatible.
   *
   * @param desiredState
   *
   * @return the requested state, even if it was not reachable
   */
  public E setDryCurrent(final E desiredState) {
    return setAsFinal(desiredState);
  }

  /**
   * Determine if the given state is allowed to be next.
   *
   * @param desiredState desired MachineState
   *
   * @return True if the desiredState is valid from currentState
   */
  private boolean isReachable(final E desiredState) {
    if (currentState == null || statemap == null) {
      return false;
    }
    final EnumSet<?> set = statemap.get(currentState);
    if (set != null) {
      return set.contains(desiredState);
    }
    return false;
  }

  /**
   * Finalizes the new requested state
   *
   * @param desiredState
   *
   * @return the requested state
   */
  private E setAsFinal(final E desiredState) {
    logger.debug("New State: " + desiredState + " from " + currentState);
    currentState = desiredState;
    return currentState;
  }

  /**
   * Release the Machine State
   */
  public void release() {
    currentState = null;
    statemap = null;
  }
}
