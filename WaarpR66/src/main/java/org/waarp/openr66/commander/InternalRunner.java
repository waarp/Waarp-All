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
package org.waarp.openr66.commander;

import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpThreadFactory;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.waarp.common.database.DbConstant.*;

/**
 * This class launch and control the Commander and enable TaskRunner job
 * submissions
 */
public class InternalRunner {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(InternalRunner.class);

  private final ScheduledExecutorService scheduledExecutorService;
  private ScheduledFuture<?> scheduledFuture;
  private CommanderInterface commander;
  private volatile boolean isRunning = true;
  private final ThreadPoolExecutor threadPoolExecutor;
  private final NetworkTransaction networkTransaction;

  /**
   * Create the structure to enable submission by database
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public InternalRunner()
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    // FIXME always true since change for DbAdmin
    if (admin.isActive()) {
      commander = new Commander(this, true);
    } else {
      commander = new CommanderNoDb(this, true);
    }
    // This is not daemon intentionally
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        new WaarpThreadFactory("InternalRunner", false));
    isRunning = true;
    final BlockingQueue<Runnable> workQueue = new SynchronousQueue<Runnable>();
    threadPoolExecutor =
        new ThreadPoolExecutor(Configuration.configuration.getRunnerThread(),
                               Configuration.configuration.getRunnerThread() *
                               3, 1000, TimeUnit.MILLISECONDS, workQueue,
                               new WaarpThreadFactory("ClientRunner"));
    scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(commander,
                                                                      Configuration.configuration
                                                                          .getDelayCommander(),
                                                                      Configuration.configuration
                                                                          .getDelayCommander(),
                                                                      TimeUnit.MILLISECONDS);
    networkTransaction = new NetworkTransaction();
  }

  public NetworkTransaction getNetworkTransaction() {
    return networkTransaction;
  }

  public int allowedToSubmit() {
    if ((isRunning || !Configuration.configuration.isShutdown()) &&
        (threadPoolExecutor.getActiveCount() <
         Configuration.configuration.getRunnerThread())) {
      return Configuration.configuration.getRunnerThread() -
             threadPoolExecutor.getActiveCount();
    }
    return 0;
  }

  /**
   * Submit a task
   *
   * @param taskRunner
   */
  public void submitTaskRunner(DbTaskRunner taskRunner) {
    if (isRunning || !Configuration.configuration.isShutdown()) {
      if (threadPoolExecutor.getActiveCount() <
          Configuration.configuration.getRunnerThread()) {
        logger.debug("Will run {}", taskRunner);
        final ClientRunner runner =
            new ClientRunner(networkTransaction, taskRunner, null);
        if (taskRunner.isSendThrough() && (taskRunner.isRescheduledTransfer() ||
                                           taskRunner.isPreTaskStarting())) {
          runner.setSendThroughMode();
          taskRunner.checkThroughMode();
        }
        // create the client, connect and run
        threadPoolExecutor.execute(runner);
      } else {
        // too many current active threads
        logger.info("Task rescheduled {}", taskRunner);
      }
    }
  }

  /**
   * First step while shutting down the service
   */
  public void prepareStopInternalRunner() {
    isRunning = false;
    scheduledFuture.cancel(false);
    if (commander != null) {
      commander.finalizeCommander();
    }
    scheduledExecutorService.shutdown();
    threadPoolExecutor.shutdown();
  }

  /**
   * This should be called when the server is shutting down, after stopping
   * active requests if possible.
   */
  public void stopInternalRunner() {
    isRunning = false;
    logger.info("Stopping Commander and Runner Tasks");
    scheduledFuture.cancel(true);
    if (commander != null) {
      commander.finalizeCommander();
    }
    scheduledExecutorService.shutdownNow();
    threadPoolExecutor.shutdownNow();
    networkTransaction.closeAll(false);
  }

  public int nbInternalRunner() {
    return threadPoolExecutor.getActiveCount();
  }

  public void reloadInternalRunner()
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    scheduledFuture.cancel(false);
    if (commander != null) {
      commander.finalizeCommander();
    }
    // FIXME always true since change for DbAdmin
    if (admin.isActive()) {
      commander = new Commander(this);
    } else {
      commander = new CommanderNoDb(this);
    }
    scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(commander,
                                                                      2 *
                                                                      Configuration.configuration
                                                                          .getDelayCommander(),
                                                                      Configuration.configuration
                                                                          .getDelayCommander(),
                                                                      TimeUnit.MILLISECONDS);
  }
}
