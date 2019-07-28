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

package org.waarp.uip;

import org.junit.Assert;
import org.junit.Test;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.WaarpStringUtils;

import java.io.File;

public class WaarpPasswordTest {

  @Test
  public void testWaarpPassword() throws Exception {
    DetectionUtils.setJunit(true);
    int step = 0;
    System.out.println("Step " + step++);
    {
      final String[] args = {};
      WaarpPassword.main(args);
    }
    Thread.sleep(5);
    // using DES
    System.out.println("Step " + step++);
    final File keyFile = new File("/tmp/out.ggp");
    keyFile.delete();
    {
      final String[] args = { "-clear", "-des", "-ko", "/tmp/out.ggp" };
      WaarpPassword.main(args);
      Assert.assertTrue(keyFile.exists());
    }
    Thread.sleep(5);
    System.out.println("Step " + step++);
    {
      final String[] args = { "-clear", "-ki", "/tmp/out.ggp" };
      WaarpPassword.main(args);
    }
    Thread.sleep(5);
    System.out.println("Step " + step++);
    final File pwd = new File("/tmp/pwd.ggp");
    pwd.delete();
    {
      final String[] args = {
          "-clear", "-ki", "/tmp/out.ggp", "-po", "/tmp/pwd.ggp", "-pwd", "pwd"
      };
      WaarpPassword.main(args);
      Assert.assertTrue(pwd.exists());
    }
    Thread.sleep(5);
    System.out.println("Step " + step++);
    {
      final String[] args =
          { "-clear", "-ki", "/tmp/out.ggp", "-pi", "/tmp/pwd.ggp" };
      WaarpPassword.main(args);
    }
    Thread.sleep(5);
    System.out.println("Step " + step++);
    String cpwd = WaarpStringUtils.readFile("/tmp/pwd.ggp");
    {
      final String[] args = {
          "-clear", "-ki", "/tmp/out.ggp", "-po", "/tmp/pwd.ggp", "-cpwd", cpwd
      };
      WaarpPassword.main(args);
      Assert.assertTrue(pwd.exists());
    }
    // Using BlowFish
    keyFile.delete();
    pwd.delete();
    Thread.sleep(5);
    System.out.println("Step " + step++);

    {
      final String[] args = { "-clear", "-blf", "-ko", "/tmp/out.ggp" };
      WaarpPassword.main(args);
      Assert.assertTrue(keyFile.exists());
    }
    Thread.sleep(5);
    System.out.println("Step " + step++);
    {
      final String[] args = { "-clear", "-blf", "-ki", "/tmp/out.ggp" };
      WaarpPassword.main(args);
    }
    Thread.sleep(5);
    System.out.println("Step " + step++);
    {
      final String[] args = {
          "-clear", "-blf", "-ki", "/tmp/out.ggp", "-po", "/tmp/pwd.ggp",
          "-pwd", "pwd"
      };
      WaarpPassword.main(args);
      Assert.assertTrue(pwd.exists());
    }
    Thread.sleep(5);
    System.out.println("Step " + step++);
    {
      final String[] args = {
          "-clear", "-blf", "-ki", "/tmp/out.ggp", "-pi", "/tmp/pwd.ggp"
      };
      WaarpPassword.main(args);
    }
    Thread.sleep(5);
    System.out.println("Step " + step++);
    cpwd = WaarpStringUtils.readFile("/tmp/pwd.ggp");
    {
      final String[] args = {
          "-clear", "-blf", "-ki", "/tmp/out.ggp", "-po", "/tmp/pwd.ggp",
          "-cpwd", cpwd
      };
      WaarpPassword.main(args);
      Assert.assertTrue(pwd.exists());
    }
    pwd.delete();
    keyFile.delete();
  }

}