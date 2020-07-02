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

package org.waarp.common.utility;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.waarp.common.logging.SysErrLogger;

/**
 * Logger for each method to print current test name and duration.<br>
 * <br>
 * Include in each Junit4 Test classes:<br>
 * <pre>
 *  @Rule(order = Integer.MIN_VALUE)
 *  public TestWatcher watchman= new TestWatcherJunit4();
 * </pre>
 */
public class TestWatcherJunit4 extends TestWatcher {
  enum Color {
    /**
     * Color end string, color reset
     */
    RESET("\033[0m"),
    /**
     * YELLOW
     */
    YELLOW("\033[0;33m"),
    /**
     * BLUE
     */
    BLUE("\033[0;34m");
    private final String code;

    Color(String code) {
      this.code = code;
    }

    @Override
    public String toString() {
      return code;
    }
  }

  private long startTime;

  protected void starting(Description description) {
    SysErrLogger.FAKE_LOGGER.sysout(
        Color.YELLOW.toString() + "==============\nStarting test: " +
        description.getMethodName() + Color.RESET.toString());
    startTime = System.nanoTime();
  }

  protected void finished(Description description) {
    long time = (System.nanoTime() - startTime) / 1000000;
    SysErrLogger.FAKE_LOGGER.sysout(
        Color.BLUE.toString() + "Ending test: " + description.getMethodName() +
        " in " + time + " ms\n==============" + Color.RESET.toString());
  }
}
