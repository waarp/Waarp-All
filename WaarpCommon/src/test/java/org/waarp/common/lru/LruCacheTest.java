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

package org.waarp.common.lru;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.Callable;

import static org.junit.Assert.*;

abstract public class LruCacheTest {
  private static String[] keys;
  private static String[] values;
  private static String key;
  private static String value;
  private static int capacity;
  private static long ttl;

  @BeforeClass
  public static void setUpClass() {
    keys = new String[] { "aa", "bb", "cc", "dd", "ee", "ff", "gg", "hh" };
    values =
        new String[] { "aaa", "bbb", "ccc", "ddd", "eer", "fff", "ggg", "hhh" };
    key = "cc";
    value = "ccc";
    capacity = keys.length * 2;
    ttl = 3600 * 1000;
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorZeroCapacityTest() {
    createCache(0, ttl);
  }

  abstract protected InterfaceLruCache<String, String> createCache(int capacity,
                                                                   long ttl);

  @Test(expected = IllegalArgumentException.class)
  public void constructorNegativeCapacityTest() {
    createCache(-5, ttl);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorZeroTtlTest() {
    createCache(capacity, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNegativeTtlTest() {
    createCache(capacity, -5);
  }

  @Test
  public void clearTest() {
    InterfaceLruCache<String, String> cache = createCache(capacity, ttl);
    insertData(cache);
    cache.clear();

    assertEquals(0, cache.size());
    for (int i = 0; i < keys.length; ++i) {
      assertFalse(cache.contains(keys[i]));
    }
  }

  private void insertData(InterfaceLruCache<String, String> cache) {
    for (int i = 0; i < keys.length; ++i) {
      cache.put(keys[i], values[i]);
    }
  }

  @Test
  public void containsTest() {
    InterfaceLruCache<String, String> cache = createCache(capacity, ttl);
    insertData(cache);

    for (int i = 0; i < keys.length; ++i) {
      assertTrue(cache.contains(keys[i]));
    }

    assertFalse(cache.contains("a"));
    assertFalse(cache.contains("b"));
    assertFalse(cache.contains("c"));
    assertFalse(cache.contains("d"));
  }

  @Test
  public void getPutTest() {
    InterfaceLruCache<String, String> cache = createCache(capacity, ttl);

    cache.put(key, value);

    assertEquals(value, cache.get(key));
  }

  @Test
  public void getCallableTest() throws Exception {
    InterfaceLruCache<String, String> cache = createCache(capacity, ttl);

    String val = cache.get(key, new SimpleCallable());

    assertEquals(SimpleCallable.str, val);
  }

  @Test
  public void getCallableTtl() throws Exception {
    InterfaceLruCache<String, String> cache = createCache(capacity, ttl);
    String val = cache.get(key, new SimpleCallable(), 10);

    assertTrue(cache.contains(key));
    assertEquals(SimpleCallable.str, val);
    assertEquals(SimpleCallable.str, cache.get(key));

    Thread.sleep(20);

    assertFalse(cache.contains(key));
    assertNull(cache.get(key));
  }

  @Test
  public void getCapacityTest() {
    InterfaceLruCache<String, String> cache = createCache(capacity, ttl);

    assertEquals(capacity, cache.getCapacity());
  }

  @Test
  public void getSizeTest() {
    InterfaceLruCache<String, String> cache = createCache(capacity, ttl);

    insertData(cache);
    assertEquals(keys.length, cache.size());

    //retest
    insertData(cache);
    assertEquals(keys.length, cache.size());

    cache.clear();

    assertEquals(0, cache.size());
  }

  @Test
  public void getTtlTest() {
    InterfaceLruCache<String, String> cache = createCache(capacity, ttl);

    assertEquals(ttl, cache.getTtl());
    cache.setNewTtl(ttl);
    assertEquals(ttl, cache.getTtl());
  }

  @Test
  public void isEmptyTest() {
    InterfaceLruCache<String, String> cache = createCache(capacity, ttl);

    assertTrue(cache.isEmpty());

    insertData(cache);
    cache.clear();
    assertTrue(cache.isEmpty());
  }

  @Test
  public void ttlTest() throws InterruptedException {
    InterfaceLruCache<String, String> cache = createCache(capacity, ttl);
    cache.put(key, value, 10);

    assertTrue(cache.contains(key));
    assertEquals(value, cache.get(key));

    Thread.sleep(20);

    assertFalse(cache.contains(key));
    assertNull(cache.get(key));

    cache.put(key, value, 10);
    cache.updateTtl(key);
    Thread.sleep(5);
    assertTrue(cache.contains(key));
    assertEquals(value, cache.get(key));

    try {
      cache.setNewTtl(-5);
      fail("Should throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // OK
    }

  }

  @Test
  public void nullsNotStoredTest() {
    InterfaceLruCache<String, String> cache = createCache(capacity, ttl);

    for (int i = 0; i < keys.length; ++i) {
      cache.put(keys[i], null);
    }

    assertEquals(0, cache.size());

    for (int i = 0; i < keys.length; ++i) {
      assertFalse(cache.contains(keys[i]));
    }
  }

  private class SimpleCallable implements Callable<String> {
    public static final String str = "callableResult";

    @Override
    public String call() throws Exception {
      return str;
    }

  }
}