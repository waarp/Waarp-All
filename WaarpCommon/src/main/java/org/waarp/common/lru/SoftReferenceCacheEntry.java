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
package org.waarp.common.lru;

import java.lang.ref.SoftReference;

/**
 * Cache entry which uses SoftReference to store value
 *
 * @author Damian Momot
 */
class SoftReferenceCacheEntry<V> implements InterfaceLruCacheEntry<V> {

  private final SoftReference<V> valueReference;

  private long expirationTime;

  /**
   * Creates LruCacheEntry with desired ttl
   *
   * @param value
   * @param ttl time to live in milliseconds
   *
   * @throws IllegalArgumentException if ttl is not positive
   */
  SoftReferenceCacheEntry(final V value, final long ttl) {
    if (ttl <= 0) {
      throw new IllegalArgumentException("ttl must be positive");
    }

    valueReference = new SoftReference<V>(value);
    expirationTime = System.currentTimeMillis() + ttl;
  }

  /**
   * Returns value if entry is valid, null otherwise.
   * <p>
   * Entry is invalid if SoftReference is cleared or entry has expired
   *
   * @return value if entry is valid
   */
  @Override
  public final V getValue() {
    V value = null;

    // check expiration time
    if (System.currentTimeMillis() <= expirationTime) {
      value = valueReference.get();
    }

    return value;
  }

  @Override
  public final boolean isStillValid(final long timeRef) {
    return timeRef <= expirationTime;
  }

  @Override
  public final boolean resetTime(final long ttl) {
    expirationTime = System.currentTimeMillis() + ttl;
    return true;
  }
}
