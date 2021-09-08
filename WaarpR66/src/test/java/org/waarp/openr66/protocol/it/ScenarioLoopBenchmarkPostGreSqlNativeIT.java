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

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.utility.TestWatcherJunit4;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScenarioLoopBenchmarkPostGreSqlNativeIT
    extends ScenarioBaseLoopBenchmark {
  public static final String NATIVE_POSTGRESQL = "test.native.postgresql";
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private String jdbc = null;

  public JdbcDatabaseContainer getJDC() {
    return null;
  }

  public String getJDBC() {
    // To adapt to local database already installed
    return jdbc;
    //"jdbc:postgresql://172.20.192.1:5533/test"
  }

  @BeforeClass
  public static void setup() throws Exception {
    final String propJdbc = SystemPropertyUtil.get(NATIVE_POSTGRESQL);
    if (propJdbc == null || propJdbc.isEmpty()) {
      scenarioBase = null;
      logger.warn("PostgreSQL Native not available");
      return;
    }
    logger.warn("START PostGreSQL Native IT TEST");
    scenarioBase = new ScenarioLoopBenchmarkPostGreSqlNativeIT();
    ((ScenarioLoopBenchmarkPostGreSqlNativeIT) scenarioBase).jdbc = propJdbc;
    setUpBeforeClass();
  }

}
