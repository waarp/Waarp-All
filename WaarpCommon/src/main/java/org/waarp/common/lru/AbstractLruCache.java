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

import java.util.concurrent.Callable;

/**
 * Base class for concrete implementations
 *
 * @author Damian Momot
 */
public abstract class AbstractLruCache<K, V>
    implements InterfaceLruCache<K, V> {
  private long ttl;

  /**
   * Constructs BaseLruCache
   *
   * @param ttl
   *
   * @throws IllegalArgumentException if ttl is not positive
   */
  protected AbstractLruCache(long ttl) {
    if (ttl <= 0) {
      throw new IllegalArgumentException("ttl must be positive");
    }

    this.ttl = ttl;
  }

  @Override
  public boolean contains(K key) {
    // can't use contains because of expiration policy
    final V value = get(key);

    return value != null;
  }

  /**
   * Creates new LruCacheEntry<V>.
   * <p>
   * It can be used to change implementation of LruCacheEntry
   *
   * @param value
   * @param ttl
   *
   * @return LruCacheEntry<V>
   */
  protected InterfaceLruCacheEntry<V> createEntry(V value, long ttl) {
    return new StrongReferenceCacheEntry<V>(value, ttl);
  }

  @Override
  public V get(K key) {
    return getValue(key);
  }

  @Override
  public V get(K key, Callable<V> callback) throws Exception {
    return get(key, callback, ttl);
  }

  @Override
  public V get(K key, Callable<V> callback, long ttl) throws Exception {
    V value = get(key);

    // if element doesn't exist create it using callback
    if (value == null) {
      value = callback.call();
      put(key, value, ttl);
    }

    return value;
  }

  @Override
  public long getTtl() {
    return ttl;
  }

  @Override
  public void setNewTtl(long ttl) {
    if (ttl <= 0) {
      throw new IllegalArgumentException("ttl must be positive");
    }
    this.ttl = ttl;
  }

  /**
   * Returns LruCacheEntry mapped by key or null if it does not exist
   *
   * @param key
   *
   * @return LruCacheEntry<V>
   */
  protected abstract InterfaceLruCacheEntry<V> getEntry(K key);

  @Override
  public void updateTtl(K key) {
    final InterfaceLruCacheEntry<V> cacheEntry = getEntry(key);
    if (cacheEntry != null) {
      cacheEntry.resetTime(ttl);
    }
  }

  /**
   * Tries to retrieve value by it's key. Automatically removes entry if it's
   * not valid
   * (LruCacheEntry.getValue() returns null)
   *
   * @param key
   *
   * @return Value
   */
  protected V getValue(K key) {
    V value = null;

    final InterfaceLruCacheEntry<V> cacheEntry = getEntry(key);

    if (cacheEntry != null) {
      value = cacheEntry.getValue();

      // autoremove entry from cache if it's not valid
      if (value == null) {
        remove(key);
      }
    }

    return value;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public void put(K key, V value) {
    put(key, value, ttl);
  }

  @Override
  public void put(K key, V value, long ttl) {
    if (value != null) {
      putEntry(key, createEntry(value, ttl));
    }
  }

  /**
   * Puts entry into cache
   *
   * @param key
   * @param entry
   */
  protected abstract void putEntry(K key, InterfaceLruCacheEntry<V> entry);
}
