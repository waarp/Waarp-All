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

public class LongUuidNativeTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final int NB = 1000000;
  private static final int NB_THREAD = 10;

  @Test
  public void testParsing() {
    final long id1 = LongUuid.getLongUuid();
    final LongUuid id2 = new LongUuid(id1);
    assertEquals(id1, id2.getLong());
  }

  @Test
  public void testNonSequentialValue() {
    final int n = NB;
    final long[] ids = new long[n];

    for (int i = 0; i < n; i++) {
      ids[i] = LongUuid.getLongUuid();
    }

    for (int i = 1; i < n; i++) {
      assertTrue(ids[i - 1] != ids[i]);
    }
  }

  @Test
  public void testPIDField() throws Exception {
    final long id = LongUuid.getLongUuid();
    final LongUuid longUuid = new LongUuid(id);
    assertEquals((JvmProcessId.jvmInstanceId() >> 4) & 0x0F,
                 longUuid.getProcessId());
  }

  @Test
  public void testForDuplicates() {
    final int n = NB;
    final Set<Long> uuids = new HashSet<Long>(n);
    final Long[] uuidArray = new Long[n];

    final long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      uuidArray[i] = LongUuid.getLongUuid();
    }
    final long stop = System.currentTimeMillis();
    System.out.println(
        "Time = " + (stop - start) + " so " + (n / (stop - start)) * 1000 +
        " Uuids/s");

    uuids.addAll(Arrays.asList(uuidArray));

    System.out.println("Create " + n + " and get: " + uuids.size());
    assertEquals(n, uuids.size());
    System.out.println(
        "Time elapsed: " + uuidArray[0] + " - " + uuidArray[n - 1] + " = " +
        (uuidArray[n - 1] - uuidArray[0]) + " & " +
        (new LongUuid(uuidArray[n - 1]).getTimestamp() -
         new LongUuid(uuidArray[0]).getTimestamp()));
  }

  @Test
  public void concurrentGeneration() throws Exception {
    final int numThreads = NB_THREAD;
    final Thread[] threads = new Thread[numThreads];
    final int n = NB;
    final int effectiveN = n / numThreads * numThreads;
    final Long[] uuids = new Long[effectiveN];

    final long start = System.currentTimeMillis();
    for (int i = 0; i < numThreads; i++) {
      threads[i] = new Generator(n / numThreads, uuids, i);
      threads[i].start();
    }

    for (int i = 0; i < numThreads; i++) {
      threads[i].join();
    }
    final long stop = System.currentTimeMillis();
    System.out.println(
        "Time = " + (stop - start) + " so " + (n / (stop - start)) * 1000 +
        " Uuids/s");

    final Set<Long> uuidSet = new HashSet<Long>(effectiveN);
    uuidSet.addAll(Arrays.asList(uuids));

    assertEquals(effectiveN, uuidSet.size());
  }

  @Test
  public void concurrentCounterGeneration() throws Exception {
    final int numThreads = NB_THREAD;
    final Thread[] threads = new Thread[numThreads];
    final int n = NB;
    final int effectiveN = n / numThreads * numThreads;
    final Long[] uuids = new Long[effectiveN];

    final long start = System.currentTimeMillis();
    for (int i = 0; i < numThreads; i++) {
      threads[i] = new CounterParallel(n / numThreads, uuids, i);
      threads[i].start();
    }

    for (int i = 0; i < numThreads; i++) {
      threads[i].join();
    }
    final long stop = System.currentTimeMillis();
    System.out.println(
        "Time = " + (stop - start) + " so " + (n / (stop - start)) * 1000 +
        " Counter/s");

    final Set<Long> uuidSet = new HashSet<Long>(effectiveN);
    uuidSet.addAll(Arrays.asList(uuids));

    assertEquals(effectiveN, uuidSet.size());
  }

  private static class CounterParallel extends Thread {
    private final Long[] uuids;
    final int id;
    final int n;

    public CounterParallel(final int n, final Long[] uuids, final int id) {
      this.n = n;
      this.uuids = uuids;
      this.id = id * n;
    }

    @Override
    public void run() {
      for (int i = 0; i < n; i++) {
        uuids[id + i] =
            (System.currentTimeMillis() << 20) + LongUuid.getCounter();
      }
    }
  }

  private static class Generator extends Thread {
    private final Long[] uuids;
    final int id;
    final int n;

    public Generator(final int n, final Long[] uuids, final int id) {
      this.n = n;
      this.uuids = uuids;
      this.id = id * n;
    }

    @Override
    public void run() {
      for (int i = 0; i < n; i++) {
        uuids[id + i] = LongUuid.getLongUuid();
      }
    }
  }
}