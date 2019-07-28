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

import org.apache.tools.ant.Project;
import org.junit.Test;
import org.waarp.common.guid.JvmProcessId;

import java.io.File;

import static org.junit.Assert.*;

public class ProcessesTest {

  private static final int TIMEMS = 100;

  public boolean waitForPidDone(int pid) {
    int i;
    for (i = 0; i < 10; i++) {
      try {
        Thread.sleep(TIMEMS);
      } catch (InterruptedException ignored) {
      }
      if (!Processes.exists(pid)) {
        break;
      }
    }
    System.out.println("Time kill: " + i * TIMEMS);
    return Processes.exists(pid);
  }

  @Test
  public void launchProcess() throws Exception {
    Process process = Processes.launchJavaProcess(ToRun.class.getName());
    int currentPid = Processes.getCurrentPid();
    int getPid = JvmProcessId.jvmProcessId();
    int pid = Processes.getPidLinux(process);
    System.out.println(
        "Current PID is " + currentPid + ':' + getPid + " while launched " +
        pid);
    assertTrue(Processes.exists(pid));
    assertEquals(currentPid, getPid);
    int pid2 =
        Processes.getPidOfRunnerCommandLinux("java", ToRun.class.getName());
    assertEquals(pid, pid2);
    Processes.kill(pid, true);
    if (waitForPidDone(pid)) {
      System.err.println("Process still running " + pid);
      process.destroy();
      if (waitForPidDone(pid)) {
        System.err.println("Process still running " + pid);
        Processes.kill(pid, false);
        if (waitForPidDone(pid)) {
          System.err.println("Process still running " + pid);
        }
      }
    }
    assertFalse(pid + " Should have stopped before", Processes.exists(pid));
  }

  @Test
  public void executeJvm() throws InterruptedException {
    File homeDir = new File("/tmp");
    Project project = Processes.getProject(homeDir);
    int pid = Processes
        .executeJvm(project, homeDir, ToRun.class, new String[0], true);
    System.out.println(" While launched " + pid);
    assertTrue(Processes.exists(pid));
    int pid2 =
        Processes.getPidOfRunnerCommandLinux("java", ToRun.class.getName());
    assertEquals(pid, pid2);
    Processes.kill(pid, true);
    if (waitForPidDone(pid)) {
      System.err.println("Process still running " + pid);
      Processes.kill(pid, false);
      if (waitForPidDone(pid)) {
        System.err.println("Process still running " + pid);
      }
    }
    Processes.finalizeProject(project);
    assertFalse(pid + " Should have stopped before", Processes.exists(pid));
  }
}