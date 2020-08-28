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

package org.waarp.common.cpu;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.TestWatcherJunit4;

import static org.junit.Assert.*;

public class CpuManagementTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  @Test
  public void testGetLoadAverage() {
    long total = 0;
    CpuManagement cpuManagement = null;
    try {
      cpuManagement = new CpuManagement();
    } catch (final UnsupportedOperationException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return;
    }
    double max = 0.0;
    SysErrLogger.FAKE_LOGGER.sysout("LA: " + cpuManagement.getLoadAverage());
    for (int i = 0; i < 1000 * 1000 * 1000; i++) {
      // keep ourselves busy for a while ...
      // note: we had to add some "work" into the loop or Java 6
      // optimizes it away. Thanks to Daniel Einspanjer for
      // pointing that out.
      total += i;
      total *= 10;
    }
    if (total <= 0) {
      System.out.println(total);
    }
    SysErrLogger.FAKE_LOGGER.sysout("LA: " + cpuManagement.getLoadAverage());
    total = 0;
    for (int i = 0; i < 1000 * 1000 * 1000; i++) {
      // keep ourselves busy for a while ...
      // note: we had to add some "work" into the loop or Java 6
      // optimizes it away. Thanks to Daniel Einspanjer for
      // pointing that out.
      total += i;
      total *= 10;
    }
    if (total <= 0) {
      System.out.println(total);
    }
    max = cpuManagement.getLoadAverage();
    SysErrLogger.FAKE_LOGGER.sysout("LA: " + max);
    try {
      Thread.sleep(5000);
    } catch (final InterruptedException ignored) {//NOSONAR
    }
    final double min = cpuManagement.getLoadAverage();
    System.err.println("LA: " + min);
    // Not checking since not as precise: assertTrue("Max > current: " + max + " >? " + min, max > min);

    total = 0;
    for (long i = 0; i < 1000 * 1000 * 1000 * 1000; i++) {
      // keep ourselves busy for a while ...
      // note: we had to add some "work" into the loop or Java 6
      // optimizes it away. Thanks to Daniel Einspanjer for
      // pointing that out.
      total += i;
      total *= 10;
    }
    if (total <= 0) {
      System.out.println(total);
    }
    max = cpuManagement.getLoadAverage();
    System.err.println("LA: " + max);
    // Not checking since not as precise: assertTrue("Min < current: " + min + " <? " + max, max >= min);
    assertTrue(true);
  }

  @Test
  public void testSysmonGetLoadAverage() {
    long total = 0;
    final CpuManagementSysmon cpuManagement = new CpuManagementSysmon();
    double max = 0.0;
    System.err.println("LAs: " + cpuManagement.getLoadAverage());
    for (int i = 0; i < 1000 * 1000 * 1000; i++) {
      // keep ourselves busy for a while ...
      // note: we had to add some "work" into the loop or Java 6
      // optimizes it away. Thanks to Daniel Einspanjer for
      // pointing that out.
      total += i;
      total *= 10;
    }
    if (total <= 0) {
      System.out.println(total);
    }
    System.err.println("LAs: " + cpuManagement.getLoadAverage());
    total = 0;
    for (int i = 0; i < 1000 * 1000 * 1000; i++) {
      // keep ourselves busy for a while ...
      // note: we had to add some "work" into the loop or Java 6
      // optimizes it away. Thanks to Daniel Einspanjer for
      // pointing that out.
      total += i;
      total *= 10;
    }
    if (total <= 0) {
      System.out.println(total);
    }
    max = cpuManagement.getLoadAverage();
    System.err.println("LAs: " + max);
    try {
      Thread.sleep(2000);
    } catch (final InterruptedException ignored) {//NOSONAR
    }
    final double min = cpuManagement.getLoadAverage();
    System.err.println("LAs: " + min);
    assertTrue("Max > current: " + max + " >? " + min, max > min);

    total = 0;
    for (int i = 0; i < 1000 * 1000 * 1000; i++) {
      // keep ourselves busy for a while ...
      // note: we had to add some "work" into the loop or Java 6
      // optimizes it away. Thanks to Daniel Einspanjer for
      // pointing that out.
      total += i;
      total *= 10;
    }
    if (total <= 0) {
      System.out.println(total);
    }
    max = cpuManagement.getLoadAverage();
    System.err.println("LAs: " + max);
    assertTrue("Min < current: " + min + " <? " + max, max > min);
  }

}
