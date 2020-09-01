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

import java.util.Collection;
import java.util.Iterator;

/**
 * Threadsafe synchronized implementation of LruCache based on LinkedHashMap.
 * Threadsafety is provided by
 * method synchronization.
 * <p>
 * This cache implementation should be used with low number of threads.
 *
 * @author Damian Momot
 */
public class SynchronizedLruCache<K, V> extends AbstractLruCache<K, V> {
  private static final int DEFAULT_INITIAL_CAPACITY = 16;

  private static final float DEFAULT_LOAD_FACTOR = 0.75f;

  private final CapacityLruLinkedHashMap<K, InterfaceLruCacheEntry<V>> cacheMap;

  /**
   * Creates new SynchronizedLruCache
   *
   * @param capacity max cache capacity
   * @param ttl time to live in milliseconds
   * @param initialCapacity initial cache capacity
   * @param loadFactor
   */
  public SynchronizedLruCache(final int capacity, final long ttl,
                              final int initialCapacity,
                              final float loadFactor) {
    super(ttl);
    cacheMap =
        new CapacityLruLinkedHashMap<K, InterfaceLruCacheEntry<V>>(capacity,
                                                                   initialCapacity,
                                                                   loadFactor);
  }

  /**
   * Creates new SynchronizedLruCache with DEFAULT_LOAD_FACTOR
   *
   * @param capacity max cache capacity
   * @param ttl time to live in milliseconds
   * @param initialCapacity initial cache capacity
   */
  public SynchronizedLruCache(final int capacity, final long ttl,
                              final int initialCapacity) {
    this(capacity, ttl, initialCapacity, DEFAULT_LOAD_FACTOR);
  }

  /**
   * Creates new SynchronizedLruCache with DEFAULT_LOAD_FACTOR and
   * DEFAULT_INITIAL_CAPACITY
   *
   * @param capacity max cache capacity
   * @param ttl time to live in milliseconds
   */
  public SynchronizedLruCache(final int capacity, final long ttl) {
    this(capacity, ttl, DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
  }

  @Override
  public synchronized void clear() {
    cacheMap.clear();
  }

  @Override
  public synchronized V get(final K key) {
    return super.get(key);
  }

  @Override
  public int getCapacity() {
    return cacheMap.getCapacity();
  }

  @Override
  protected InterfaceLruCacheEntry<V> getEntry(final K key) {
    return cacheMap.get(key);
  }

  @Override
  public synchronized int size() {
    return cacheMap.size();
  }

  @Override
  public synchronized void put(final K key, final V value, final long ttl) {
    super.put(key, value, ttl);
  }

  @Override
  protected void putEntry(final K key, final InterfaceLruCacheEntry<V> entry) {
    cacheMap.put(key, entry);
  }

  @Override
  public synchronized V remove(final K key) {
    final InterfaceLruCacheEntry<V> cv = cacheMap.remove(key);
    if (cv != null) {
      return cv.getValue();
    }
    return null;
  }

  @Override
  public synchronized int forceClearOldest() {
    final long timeRef = System.currentTimeMillis();
    final Collection<InterfaceLruCacheEntry<V>> collection = cacheMap.values();
    final Iterator<InterfaceLruCacheEntry<V>> iterator = collection.iterator();
    int nb = 0;
    while (iterator.hasNext()) {
      final InterfaceLruCacheEntry<V> v = iterator.next();
      if (!v.isStillValid(timeRef)) {
        iterator.remove();
        nb++;
      }
    }
    return nb;
  }

}
