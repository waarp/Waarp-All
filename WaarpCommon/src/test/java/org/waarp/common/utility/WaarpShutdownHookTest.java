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

import org.junit.BeforeClass;
import org.junit.Test;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.utility.WaarpShutdownHook.ShutdownConfiguration;
import org.waarp.common.utility.WaarpShutdownHook.ShutdownTimerTask;

import java.util.Timer;

import static org.junit.Assert.*;

public class WaarpShutdownHookTest {
  private class WaarpShutdownHookForTest extends WaarpShutdownHook {

    public WaarpShutdownHookForTest(ShutdownConfiguration configuration) {
      super(configuration);
    }

    @Override
    protected void exitService() {
      // Do nothing
    }
  }

  @BeforeClass
  public static void setup() {
    DetectionUtils.setJunit(true);
    WaarpShutdownHook.removeShutdownHook();
  }

  @Test
  public void addShutdownHook() throws InterruptedException {
    WaarpShutdownHook.shutdownHook =
        new WaarpShutdownHookForTest(new ShutdownConfiguration());
    WaarpShutdownHook.addShutdownHook();
    assertNotNull(WaarpShutdownHook.shutdownHook);
    assertFalse(WaarpShutdownHook.isInShutdown());
    assertFalse(WaarpShutdownHook.isShutdownStarting());
    assertFalse(WaarpShutdownHook.isRestart());
    WaarpShutdownHook.setRestart(true);
    assertTrue(WaarpShutdownHook.isRestart());
    WaarpShutdownHook.setRestart(false);
    assertFalse(WaarpShutdownHook.isRestart());
    WaarpShutdownHook.registerMain(FileConvert.class, new String[0]);
    assertFalse(WaarpShutdownHook.isRestart());

    WaarpShutdownHook.shutdownWillStart();
    assertTrue(WaarpShutdownHook.isShutdownStarting());
    WaarpShutdownHook.terminate(false);
    assertFalse(WaarpShutdownHook.isInShutdown());
    assertNull(WaarpShutdownHook.shutdownHook);

    ShutdownConfiguration shutdownConfiguration = new ShutdownConfiguration();
    shutdownConfiguration.timeout = 100;
    shutdownConfiguration.serviceFuture = new WaarpFuture();
    WaarpShutdownHook.shutdownHook =
        new WaarpShutdownHookForTest(shutdownConfiguration);
    new WaarpShutdownHookForTest(shutdownConfiguration);
    WaarpShutdownHook.addShutdownHook();
    assertNotNull(WaarpShutdownHook.shutdownHook);
    final Timer timer = new Timer("WaarpFinalExit", true);
    final ShutdownTimerTask timerTask = new ShutdownTimerTask();
    timer.schedule(timerTask, 100);
    shutdownConfiguration.serviceFuture.awaitOrInterruptible();
    assertFalse(WaarpShutdownHook.isInShutdown());
    WaarpShutdownHook.terminate(true);
    assertFalse(WaarpShutdownHook.isInShutdown());
    assertNull(WaarpShutdownHook.shutdownHook);

    shutdownConfiguration.timeout = 100;
    shutdownConfiguration.serviceFuture = new WaarpFuture();
    WaarpShutdownHook.shutdownHook =
        new WaarpShutdownHookForTest(shutdownConfiguration);
    WaarpShutdownHook.addShutdownHook();
    assertNotNull(WaarpShutdownHook.shutdownHook);
    assertFalse(WaarpShutdownHook.isInShutdown());
    WaarpShutdownHook.shutdownHook.run();
    assertFalse(WaarpShutdownHook.isInShutdown());
    assertNull(WaarpShutdownHook.shutdownHook);
  }
}