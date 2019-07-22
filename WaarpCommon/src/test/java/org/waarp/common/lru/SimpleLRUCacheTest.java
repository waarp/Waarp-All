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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class SimpleLRUCacheTest {
  private static String[] keys;
  private static String[] values;
  private static int capacity;
  private SimpleLRUCache<String, String> map;

  @BeforeClass
  public static void setUpClass() {
    keys = new String[] { "aa", "bb", "cc", "dd", "ee", "ff", "gg", "hh" };
    values = new String[] {
        "aaa", "bbb", "ccc", "ddd", "eer", "fff", "ggg", "hhh"
    };
    capacity = 5;
  }

  @Before
  public void init() {
    map = new SimpleLRUCache<String, String>(capacity);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNegativeCapacityTest() {
    map = new SimpleLRUCache<String, String>(-5);
  }

  @Test
  public void capacityTest() {
    assertTrue(map.isEmpty());

    for (int i = 0; i < capacity; ++i) {
      map.put(keys[i], values[i]);
    }

    assertEquals(capacity, map.size());

    for (int i = capacity; i < keys.length; ++i) {
      map.put(keys[i], values[i]);
    }

    assertEquals(capacity, map.size());

    for (int i = 0; i < keys.length - capacity; ++i) {
      assertFalse(map.containsKey(keys[i]));
    }

    for (int i = keys.length - capacity; i < keys.length; ++i) {
      assertTrue(map.containsKey(keys[i]));
    }
  }
}