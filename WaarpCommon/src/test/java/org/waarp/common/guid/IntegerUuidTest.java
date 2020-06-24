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
package org.waarp.common.guid;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.utility.TestWatcherJunit4;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class IntegerUuidTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final int NB = 1000000;

  @Test
  public void testStructure() {
    IntegerUuid id = new IntegerUuid();
    String str = id.toString();

    assertEquals(8, str.length());
  }

  @Test
  public void testParsing() {
    IntegerUuid id1 = new IntegerUuid();
    IntegerUuid id2 = new IntegerUuid(id1.toString());
    assertEquals(id1, id2);
    assertEquals(id1.hashCode(), id2.hashCode());
    assertEquals(id1.getInt(), id2.getInt());

    IntegerUuid id3 = new IntegerUuid(id1.getBytes());
    assertEquals(id1, id3);
    IntegerUuid id4 = new IntegerUuid(id1.getInt());
    assertEquals(id1, id4);
  }

  @Test
  public void testNonSequentialValue() {
    final int n = NB;
    long[] ids = new long[n];

    for (int i = 0; i < n; i++) {
      ids[i] = new IntegerUuid().getInt();
    }

    for (int i = 1; i < n; i++) {
      assertTrue(ids[i - 1] != ids[i]);
    }
  }

  @Test
  public void testGetBytesImmutability() {
    IntegerUuid id = new IntegerUuid();
    byte[] bytes = id.getBytes();
    byte[] original = Arrays.copyOf(bytes, bytes.length);
    bytes[0] = 0;
    bytes[1] = 0;
    bytes[2] = 0;

    assertArrayEquals(id.getBytes(), original);
  }

  @Test
  public void testConstructorImmutability() {
    IntegerUuid id = new IntegerUuid();
    byte[] bytes = id.getBytes();
    byte[] original = Arrays.copyOf(bytes, bytes.length);

    IntegerUuid id2 = new IntegerUuid(bytes);
    bytes[0] = 0;
    bytes[1] = 0;

    assertArrayEquals(id2.getBytes(), original);
  }

  @Test
  public void testForDuplicates() {
    int n = NB;
    Set<Integer> uuids = new HashSet<Integer>();
    IntegerUuid[] uuidArray = new IntegerUuid[n];

    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      uuidArray[i] = new IntegerUuid();
    }
    long stop = System.currentTimeMillis();
    if (stop == start) {
      stop += 1;
    }
    System.out.println(
        "Time = " + (stop - start) + " so " + n * 1000 / (stop - start) +
        " Uuids/s");

    for (int i = 0; i < n; i++) {
      uuids.add(uuidArray[i].getInt());
    }

    System.out.println("Create " + n + " and get: " + uuids.size());
    assertEquals(n, uuids.size());
    int i = 1;
    int largest = 0;
    for (; i < n; i++) {
      if (uuidArray[i].getInt() != uuidArray[i - 1].getInt()) {
        int j = i + 1;
        long time = uuidArray[i].getInt();
        for (; j < n; j++) {
          if (uuidArray[j].getInt() != time) {
            if (largest < j - i + 2) {
              largest = j - i + 2;
            }
          } else {
            i = j;
            break;
          }
        }
        i = j;
      }
    }
    if (largest == 0) {
      largest = n;
    }
    System.out.println(uuidArray[0] + "(" + uuidArray[0].getTimestamp() + ':' +
                       uuidArray[0].getInt() + ") - " + uuidArray[n - 1] + '(' +
                       uuidArray[n - 1].getTimestamp() + ':' +
                       uuidArray[n - 1].getInt() + ") = " +
                       (uuidArray[n - 1].getInt() - uuidArray[0].getInt() + 1));
    System.out.println(largest + " different consecutive elements");
  }

  private static class Generator extends Thread {
    private final IntegerUuid[] uuids;
    int id;
    int n;
    int numThreads;

    private Generator(int n, IntegerUuid[] uuids, int id, int numThreads) {
      this.n = n;
      this.uuids = uuids;
      this.id = id;
      this.numThreads = numThreads;
    }

    @Override
    public void run() {
      for (int i = 0; i < n; i++) {
        uuids[numThreads * i + id] = new IntegerUuid();
      }
    }
  }

  @Test
  public void concurrentGeneration() throws Exception {
    int numThreads = 10;
    Thread[] threads = new Thread[numThreads];
    int n = NB;
    IntegerUuid[] uuids = new IntegerUuid[n];

    long start = System.currentTimeMillis();
    for (int i = 0; i < numThreads; i++) {
      threads[i] = new Generator(n / numThreads, uuids, i, numThreads);
      threads[i].start();
    }

    for (int i = 0; i < numThreads; i++) {
      threads[i].join();
    }
    long stop = System.currentTimeMillis();
    System.out.println(
        "Time = " + (stop - start) + " so " + n * 1000 / (stop - start) +
        " Uuids/s");

    Set<IntegerUuid> uuidSet = new HashSet<IntegerUuid>();

    int effectiveN = n / numThreads * numThreads;
    uuidSet.addAll(Arrays.asList(uuids).subList(0, effectiveN));

    assertEquals(effectiveN, uuidSet.size());
  }
}