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
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.it.ScenarioBaseLoopBenchmark;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScenarioLoopBenchmarkMonitoringPostGreSqlIT
    extends ScenarioBaseLoopBenchmark {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  protected static final Map<String, String> TMPFSMAP =
      new HashMap<String, String>();

  static {
    TMPFSMAP.clear();
    TMPFSMAP.put("/tmp/postgresql/data", "rw");
  }

  @ClassRule
  public static PostgreSQLContainer db =
      (PostgreSQLContainer) new PostgreSQLContainer().withCommand(
          "postgres -c fsync=false -c synchronous_commit=off -c " +
          "full_page_writes=false -c wal_level=minimal -c " +
          "max_wal_senders=0").withTmpFs(TMPFSMAP);

  private static HttpServerExample httpServerExample;
  private static boolean useExternalLogstash = false;
  private static final int port = 5044;

  public JdbcDatabaseContainer getJDC() {
    return db;
  }

  @BeforeClass
  public static void setup() throws Exception {
    useExternalLogstash = SystemPropertyUtil.get("useExternalLogstash", false);
    logger.warn("START PostGreSQL IT TEST");
    scenarioBase = new ScenarioLoopBenchmarkMonitoringPostGreSqlIT();
    setUpBeforeClass();
    MonitorExporterTransfers monitorExporterTransfers;
    if (useExternalLogstash) {
      // Use an external Logstash instance already started
      // Start Repetitive Monitoring
      monitorExporterTransfers =
          new MonitorExporterTransfers("http://localhost:" + port, "/", true,
                                       true, true, Configuration.configuration
                                           .getHttpWorkerGroup());

    } else {
      // Start Fake Server HTTP REST
      httpServerExample = new HttpServerExample(port);
      // Start Repetitive Monitoring
      monitorExporterTransfers =
          new MonitorExporterTransfers("http://localhost:" + port, "/", true,
                                       true, false, Configuration.configuration
                                           .getHttpWorkerGroup());
    }
    Configuration.configuration
        .scheduleWithFixedDelay(monitorExporterTransfers, 1, TimeUnit.SECONDS);
    ResourceLeakDetector.setLevel(Level.PARANOID);
  }

}
