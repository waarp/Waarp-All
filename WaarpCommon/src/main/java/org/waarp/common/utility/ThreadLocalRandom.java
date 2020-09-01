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
package org.waarp.common.utility;

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

import java.util.Random;

/**
 * A random number generator isolated to the current thread. Like the global
 * {@link Random} generator used by
 * the {@link Math} class, a {@code ThreadLocalRandom} is initialized with an
 * internally generated seed that
 * may not otherwise be modified. When applicable, use of {@code
 * ThreadLocalRandom} rather than shared
 * {@code Random} objects in concurrent programs will typically encounter much
 * less overhead and contention.
 * Use of {@code ThreadLocalRandom} is particularly appropriate when multiple
 * tasks use random numbers in
 * parallel in thread pools.
 *
 * <p>
 * Usages of this class should typically be of the form: {@code
 * ThreadLocalRandom.current().nextX(...)} (where
 * {@code X} is {@code Int}, {@code Long}, etc). When all usages are of this
 * form, it is never possible to
 * accidently share a {@code ThreadLocalRandom} across multiple threads.
 *
 * <p>
 * This class also provides additional commonly used bounded random generation
 * methods.
 *
 * @since 1.7
 */
public final class ThreadLocalRandom extends Random {
  // same constants as Random, but must be redeclared because private
  private static final long MULTIPLIER = 0x5DEECE66DL;
  private static final long ADDEND = 0xBL;
  private static final long MASK = (1L << 48) - 1;

  /**
   * The random seed. We can't use super.seed.
   */
  private long rnd;

  /**
   * Initialization flag to permit the first and only allowed call to setSeed
   * (inside Random constructor) to
   * succeed. We can't allow others since it would cause setting seed in one
   * part of a program to
   * unintentionally impact other usages by the thread.
   */
  private boolean initialized;

  /**
   * The actual ThreadLocal
   */
  private static final ThreadLocal<ThreadLocalRandom> localRandom =//NOSONAR
      new ThreadLocal<ThreadLocalRandom>() {
        @Override
        protected ThreadLocalRandom initialValue() {
          return new ThreadLocalRandom();
        }
      };//NOSONAR

  /**
   * Returns the current thread's {@code ThreadLocalRandom}.
   *
   * @return the current thread's {@code ThreadLocalRandom}
   */
  public static ThreadLocalRandom current() {
    return localRandom.get();
  }

  /**
   * Throws {@code UnsupportedOperationException}. Setting seeds in this
   * generator is not supported.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public void setSeed(final long seed) {//NOSONAR
    // We rely on the fact that the superclass no-arg constructor
    // invokes setSeed exactly once to initialize.
    if (initialized) {
      throw new UnsupportedOperationException();
    }
    initialized = true;
    rnd = (seed ^ MULTIPLIER) & MASK;
  }

  @Override
  protected int next(final int bits) {
    rnd = rnd * MULTIPLIER + ADDEND & MASK;
    return (int) (rnd >>> 48 - bits);
  }

  private static final long serialVersionUID = -5851777807851030925L;
}
