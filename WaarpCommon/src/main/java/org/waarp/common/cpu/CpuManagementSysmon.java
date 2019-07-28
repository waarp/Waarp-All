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

import com.jezhumble.javasysmon.CpuTimes;
import com.jezhumble.javasysmon.JavaSysMon;

/**
 *
 */
class CpuManagementSysmon implements CpuManagementInterface {
  public static final long DELAY = 1000;

  final JavaSysMon sysMon;

  CpuTimes cpuTimesOld;
  CpuTimes cpuTimesOldNext;
  long time;

  /**
   * @throws UnsupportedOperationException if System Load Average is
   *     not
   *     supported
   */
  public CpuManagementSysmon() throws UnsupportedOperationException {
    sysMon = new JavaSysMon();
    cpuTimesOld = sysMon.cpuTimes();
    cpuTimesOldNext = cpuTimesOld;
    time = System.currentTimeMillis();
  }

  /**
   * @return the load average
   */
  @Override
  public double getLoadAverage() {
    final long newTime = System.currentTimeMillis();
    final CpuTimes cpuTimes = sysMon.cpuTimes();
    final double rate = cpuTimes.getCpuUsage(cpuTimesOld);
    final long delta = newTime - time;
    if (delta > DELAY) {
      if (delta > 10 * DELAY) {
        time = newTime;
        cpuTimesOldNext = cpuTimes;
        cpuTimesOld = cpuTimes;
      } else {
        time = newTime;
        cpuTimesOldNext = cpuTimes;
        cpuTimesOld = cpuTimesOldNext;
      }
    }
    return rate;
  }

}
