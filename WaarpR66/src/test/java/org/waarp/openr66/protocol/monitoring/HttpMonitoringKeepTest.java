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

package org.waarp.openr66.protocol.monitoring;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.waarp.common.utility.TestWatcherJunit4;

import java.io.File;

/**
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HttpMonitoringKeepTest extends HttpMonitoringAbstract {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();


  private static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal-monitoring-keep.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "Linux/config/config-serverInitA.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_B_XML =
      "Linux/config/config-serverInitB.xml";
  private static final String CONFIG_CLIENT_A_XML = "config-clientA.xml";

  /**
   * @throws Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ResourceLeakDetector.setLevel(Level.PARANOID);
    final ClassLoader classLoader =
        HttpMonitoringCloseTest.class.getClassLoader();
    final File file =
        new File(classLoader.getResource("logback-test.xml").getFile());
    setUpBeforeClassMinimal(LINUX_CONFIG_CONFIG_SERVER_INIT_B_XML);
    setUpDbBeforeClass();
    // setUpBeforeClassServer("Linux/config/config-serverInitB.xml", "config-serverB.xml", false);
    setUpBeforeClassServer(LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML,
                           CONFIG_SERVER_A_MINIMAL_XML, true);
    setUpBeforeClassClient(CONFIG_CLIENT_A_XML);
    Thread.sleep(1000);
    // Start Fake Server HTTP REST
    httpServerExample = new HttpServerExample(port);
    // Start Repetitive Monitoring
    /*
    MonitorExporterTransfers monitorExporterTransfers =
        new MonitorExporterTransfers("http://127.0.0.1:" + port, "/log",
                                     Configuration.configuration
                                         .getHttpWorkerGroup());
    Configuration.configuration
        .scheduleWithFixedDelay(monitorExporterTransfers, 1, TimeUnit.SECONDS);
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
     */
    ResourceLeakDetector.setLevel(Level.PARANOID);
  }
}
