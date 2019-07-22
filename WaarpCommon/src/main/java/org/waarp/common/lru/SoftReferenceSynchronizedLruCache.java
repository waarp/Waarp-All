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

/**
 * Modification of SynchronizedLruCache which uses SoftReference to store values
 *
 *
 * @author Damian Momot
 */
public class SoftReferenceSynchronizedLruCache<K, V>
    extends SynchronizedLruCache<K, V> {

  public SoftReferenceSynchronizedLruCache(int capacity, long ttl,
                                           int initialCapacity,
                                           float loadFactor) {
    super(capacity, ttl, initialCapacity, loadFactor);
  }

  public SoftReferenceSynchronizedLruCache(int capacity, long ttl,
                                           int initialCapacity) {
    super(capacity, ttl, initialCapacity);
  }

  public SoftReferenceSynchronizedLruCache(int capacity, long ttl) {
    super(capacity, ttl);
  }

  @Override
  protected InterfaceLruCacheEntry<V> createEntry(V value, long ttl) {
    return new SoftReferenceCacheEntry<V>(value, ttl);
  }
}
