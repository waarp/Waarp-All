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

package org.waarp.openr66.it;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.AfterClass;
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
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNetworkException;
import org.waarp.openr66.protocol.monitoring.MonitorExporterTransfers;
import org.waarp.openr66.s3.WaarpR66S3Client;
import org.waarp.openr66.s3.util.MinioContainer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScenarioLoopBenchmarkMonitoringS3PostGreSqlIT
    extends S3ScenarioBaseLoopBenchmark {

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

  private static final int port = 5044;
  private static MinioContainer container;

  public JdbcDatabaseContainer getJDC() {
    return db;
  }

  static MonitorExporterTransfers monitorExporterTransfers;

  @BeforeClass
  public static void setup() throws Exception {
    rulename = "loop";
    final boolean useExternalLogstash =
        SystemPropertyUtil.get("useExternalLogstash", false);
    logger.warn("START PostGreSQL IT TEST");
    scenarioBase = new ScenarioLoopBenchmarkMonitoringS3PostGreSqlIT();
    setUpBeforeClass();
    if (useExternalLogstash) {
      // Use an external Logstash instance already started
      // Start Repetitive Monitoring
      monitorExporterTransfers =
          new MonitorExporterTransfers("http://localhost:" + port, "/", null,
                                       null, null, true, true, true,
                                       Configuration.configuration
                                           .getHttpWorkerGroup());

    } else {
      // Start Fake Server HTTP REST
      final HttpServerExample httpServerExample = new HttpServerExample(port);
      // Start Repetitive Monitoring
      monitorExporterTransfers =
          new MonitorExporterTransfers("http://localhost:" + port, "/", null,
                                       null, null, true, true, false,
                                       Configuration.configuration
                                           .getHttpWorkerGroup());
    }
    Configuration.configuration
        .scheduleWithFixedDelay(monitorExporterTransfers, 1, TimeUnit.SECONDS);
    ResourceLeakDetector.setLevel(Level.PARANOID);
    container = new MinioContainer(
        new MinioContainer.CredentialsProvider(ACCESS_KEY, SECRET_KEY));
    container.start();
    s3Url = container.getURL();
  }

  @Override
  protected Thread getDeletgated() {
    return getDelegateThread();
  }

  private static Thread getDelegateThread() {
    return new Thread() {
      final WaarpR66S3Client client =
          new WaarpR66S3Client(ACCESS_KEY, SECRET_KEY, s3Url);

      @Override
      public void run() {
        try {
          Iterator<String> iterator = null;
          try {
            iterator = client.listObjectsFromBucket(BUCKET, null, true, 0);
          } catch (OpenR66ProtocolNetworkException e) {
            logger.warn(e);
            return;
          }
          int count = 0;
          while (iterator.hasNext()) {
            String next = iterator.next();
            logger.debug("Contains {}", next);
            count++;
          }
          logger.warn("Contains {} items", count);
        } catch (final Exception e) {
          // Ignore
        }
      }
    };
  }

  @AfterClass
  public static void tearDownContainerAfterClass() throws Exception {
    tearDownAfterClass(getDelegateThread());
    monitorExporterTransfers.close();
    WaarpSystemUtil.stopLogger(true);
    container.stop();
  }

}
