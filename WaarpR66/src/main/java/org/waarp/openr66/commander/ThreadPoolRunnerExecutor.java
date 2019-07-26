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

import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.ErrorCode;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Thread Pool Executor for ClientRunner
 *
 *
 */
public class ThreadPoolRunnerExecutor extends ThreadPoolExecutor {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(ThreadPoolRunnerExecutor.class);

  /**
   * RejectedExecutionHandler for this ThreadPoolRunnerExecutor
   *
   *
   */
  private static class RunnerRejectedExecutionHandler
      implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable arg0, ThreadPoolExecutor arg1) {
      if (arg0 instanceof ClientRunner) {
        ClientRunner runner = (ClientRunner) arg0;
        runner.changeUpdatedInfo(AbstractDbData.UpdatedInfo.INERROR,
                                 ErrorCode.Unknown, true);
      } else {
        logger.warn("Not ClientRunner: {}", arg0.getClass().getName());
      }
    }

  }

  /**
   * @param corePoolSize
   * @param maximumPoolSize
   * @param keepAliveTime
   * @param unit
   * @param workQueue
   */
  public ThreadPoolRunnerExecutor(int corePoolSize, int maximumPoolSize,
                                  long keepAliveTime, TimeUnit unit,
                                  BlockingQueue<Runnable> workQueue) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    setRejectedExecutionHandler(new RunnerRejectedExecutionHandler());
  }

  /**
   * @param corePoolSize
   * @param maximumPoolSize
   * @param keepAliveTime
   * @param unit
   * @param workQueue
   * @param threadFactory
   */
  public ThreadPoolRunnerExecutor(int corePoolSize, int maximumPoolSize,
                                  long keepAliveTime, TimeUnit unit,
                                  BlockingQueue<Runnable> workQueue,
                                  ThreadFactory threadFactory) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
          threadFactory);
    setRejectedExecutionHandler(new RunnerRejectedExecutionHandler());
  }

  /**
   * @param corePoolSize
   * @param maximumPoolSize
   * @param keepAliveTime
   * @param unit
   * @param workQueue
   * @param handler
   */
  public ThreadPoolRunnerExecutor(int corePoolSize, int maximumPoolSize,
                                  long keepAliveTime, TimeUnit unit,
                                  BlockingQueue<Runnable> workQueue,
                                  RejectedExecutionHandler handler) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
          handler);
    setRejectedExecutionHandler(handler);
  }

  /**
   * @param corePoolSize
   * @param maximumPoolSize
   * @param keepAliveTime
   * @param unit
   * @param workQueue
   * @param threadFactory
   * @param handler
   */
  public ThreadPoolRunnerExecutor(int corePoolSize, int maximumPoolSize,
                                  long keepAliveTime, TimeUnit unit,
                                  BlockingQueue<Runnable> workQueue,
                                  ThreadFactory threadFactory,
                                  RejectedExecutionHandler handler) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
          threadFactory, handler);
    setRejectedExecutionHandler(handler);
  }

}
