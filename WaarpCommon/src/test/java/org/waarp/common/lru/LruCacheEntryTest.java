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

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public abstract class LruCacheEntryTest {
  private static String value;
  private static long ttl;

  @BeforeClass
  public static void setUpClass() {
    ttl = 3600 * 1000;
    value = "aaa";
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorZeroTtlTest() {
    createCacheEntry(value, 0);
  }

  protected abstract InterfaceLruCacheEntry<String> createCacheEntry(
      String value, long ttl);

  @Test(expected = IllegalArgumentException.class)
  public void constructorNegativeTtlTest() {
    createCacheEntry(value, -5);
  }

  @Test
  public void getValueTest() {
    final InterfaceLruCacheEntry<String> cacheEntry =
        createCacheEntry(value, ttl);

    assertEquals(value, cacheEntry.getValue());
  }

  @Test
  public void expirationTimeTest() throws InterruptedException {
    final InterfaceLruCacheEntry<String> cacheEntry =
        createCacheEntry(value, 10);

    assertNotNull(cacheEntry.getValue());
    cacheEntry.resetTime(5);
    assertTrue(cacheEntry.isStillValid(5));

    Thread.sleep(20);

    assertNull(cacheEntry.getValue());
  }
}