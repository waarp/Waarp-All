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

package org.waarp.openr66.protocol.it;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.BeforeClass;
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.SystemPropertyUtil;

import java.io.File;

import static org.waarp.openr66.protocol.it.ScenarioBase.*;

public class SpooledNoDbIT extends SpooledITAbstract {
  /**
   * @throws Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    noDb = true;
    logger.warn("DEBUG retest {}", noDb);
    if (!SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      ResourceLeakDetector.setLevel(Level.PARANOID);
    } else {
      ResourceLeakDetector.setLevel(Level.SIMPLE);
    }
    final ClassLoader classLoader = SpooledIT.class.getClassLoader();
    final File file =
        new File(classLoader.getResource("logback-test.xml").getFile());
    setUpBeforeClassMinimal(LINUX_CONFIG_CONFIG_SERVER_INIT_B_XML);
    setUpDbBeforeClass();
    setUpBeforeClassServer(LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML);
    Processes.setJvmArgsDefault("-Xms2048m -Xmx2048m ");
    File configFile = new File(dir, CONFIG_SERVER_A_MINIMAL_XML);
    r66Pid1 = startServer(configFile.getAbsolutePath());
    Processes.setJvmArgsDefault(null);
    Thread.sleep(2000);
    logger.warn("Is No DB ? {}", noDb);
    setUpBeforeClassClient(
        noDb? CONFIG_CLIENT_A_NODB_XML : CONFIG_CLIENT_A_XML);
  }
}
