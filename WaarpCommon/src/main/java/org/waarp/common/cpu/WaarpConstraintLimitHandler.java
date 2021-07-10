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
package org.waarp.common.cpu;

import io.netty.handler.traffic.AbstractTrafficShapingHandler;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.ThreadLocalRandom;

import java.util.LinkedList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class for Constraint Limit Handler for Waarp project
 */
public abstract class WaarpConstraintLimitHandler implements Runnable {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpConstraintLimitHandler.class);

  private static final String NOALERT = "noAlert";
  public static final long LOWBANDWIDTH_DEFAULT = 1048576;
  public String lastAlert = NOALERT;
  private boolean constraintInactive = true;
  private boolean useCpuLimits;
  private boolean useBandwidthLimit;

  private final ThreadLocalRandom random = ThreadLocalRandom.current();
  private CpuManagementInterface cpuManagement;
  private double cpuLimit = 1.0; // was 0.8
  private int channelLimit; // was 1000
  private boolean isServer;
  private double lastLA;
  private long lastTime;

  // Dynamic throttling
  private long waitForNetOp = 1000;
  private long timeoutCon = 10000;
  private double highCpuLimit; // was 0.8
  private double lowCpuLimit; // was 0.5
  private double percentageDecreaseRatio = 0.25;
  private long delay = 1000;
  private long limitLowBandwidth = LOWBANDWIDTH_DEFAULT;
  private AbstractTrafficShapingHandler handler;
  private ScheduledThreadPoolExecutor executor;

  private static class CurLimits {
    long read;
    long write;

    private CurLimits(final long read, final long write) {
      this.read = read;
      this.write = write;
    }
  }

  private final LinkedList<CurLimits> curLimits = new LinkedList<CurLimits>();
  private int nbSinceLastDecrease;
  private static final int PAYLOAD = 5;
  // 5 seconds of payload when new high cpu

  /**
   * Empty constructor
   */
  public WaarpConstraintLimitHandler() {
    // Do nothing except setup standard value for inactivity
    if (cpuManagement == null) {
      cpuManagement = new CpuManagementNoInfo();
    }
  }

  /**
   * This constructor enables only throttling bandwidth with cpu usage
   *
   * @param waitForNetOp 1000 ms as wait for a network operation
   * @param timeOutCon 10000 ms as timeout limit
   * @param useJdkCpuLimit True to use JDK Cpu native or False for
   *     JavaSysMon
   * @param lowcpuLimit for proactive cpu limitation (throttling
   *     bandwidth)
   *     (0<= x < 1 & highcpulimit)
   * @param highcpuLimit for proactive cpu limitation (throttling
   *     bandwidth)
   *     (0<= x <= 1) 0 meaning no
   *     throttle activated
   * @param percentageDecrease for proactive cpu limitation,
   *     throttling
   *     bandwidth reduction (0 < x < 1) as 0.25
   *     for 25% of reduction
   * @param handler the GlobalTrafficShapingHandler associated (null
   *     to have
   *     no proactive cpu
   *     limitation)
   * @param delay the delay between 2 tests for proactive cpu
   *     limitation
   * @param limitLowBandwidth the minimal bandwidth (read or write) to
   *     apply
   *     when decreasing bandwidth (low
   *     limit = 4096)
   */
  public WaarpConstraintLimitHandler(final long waitForNetOp,
                                     final long timeOutCon,
                                     final boolean useJdkCpuLimit,
                                     final double lowcpuLimit,
                                     final double highcpuLimit,
                                     final double percentageDecrease,
                                     final AbstractTrafficShapingHandler handler,
                                     final long delay,
                                     final long limitLowBandwidth) {
    this(waitForNetOp, timeOutCon, true, useJdkCpuLimit, 0, 0, lowcpuLimit,
         highcpuLimit, percentageDecrease, handler, delay, limitLowBandwidth);
  }

  /**
   * This constructor enables only Connection check ability
   *
   * @param useCpuLimit True to enable cpuLimit on connection check
   * @param useJdKCpuLimit True to use JDK Cpu native or False for
   *     JavaSysMon
   * @param cpulimit high cpu limit (0<= x < 1) to refuse new
   *     connections
   * @param channellimit number of connection limit (0<= x)
   */
  public WaarpConstraintLimitHandler(final long waitForNetOp,
                                     final long timeOutCon,
                                     final boolean useCpuLimit,
                                     final boolean useJdKCpuLimit,
                                     final double cpulimit,
                                     final int channellimit) {
    this(waitForNetOp, timeOutCon, useCpuLimit, useJdKCpuLimit, cpulimit,
         channellimit, 0, 0, 0.01, null, 1000000, LOWBANDWIDTH_DEFAULT);
  }

  /**
   * This constructor enables both Connection check ability and throttling
   * bandwidth with cpu usage
   *
   * @param waitForNetOp2 1000 ms as wait for a network operation
   * @param timeOutCon2 10000 ms as timeout limit
   * @param useCpuLimit True to enable cpuLimit on connection check
   * @param useJdKCpuLimit True to use JDK Cpu native or False for
   *     JavaSysMon
   * @param cpulimit high cpu limit (0<= x < 1) to refuse new
   *     connections
   * @param channellimit number of connection limit (0<= x)
   * @param lowcpuLimit for proactive cpu limitation (throttling
   *     bandwidth)
   *     (0<= x < 1 & highcpulimit)
   * @param highcpuLimit for proactive cpu limitation (throttling
   *     bandwidth)
   *     (0<= x <= 1) 0 meaning no
   *     throttle activated
   * @param percentageDecrease for proactive cpu limitation,
   *     throttling
   *     bandwidth reduction (0 < x < 1) as 0.25
   *     for 25% of reduction
   * @param handler the GlobalTrafficShapingHandler associated (null
   *     to have
   *     no proactive cpu
   *     limitation)
   * @param delay the delay between 2 tests for proactive cpu
   *     limitation
   * @param limitLowBandwidth the minimal bandwidth (read or write) to
   *     apply
   *     when decreasing bandwidth (low
   *     limit = 4096)
   */
  public WaarpConstraintLimitHandler(final long waitForNetOp2,
                                     final long timeOutCon2,
                                     final boolean useCpuLimit,
                                     final boolean useJdKCpuLimit,
                                     final double cpulimit,
                                     final int channellimit,
                                     final double lowcpuLimit,
                                     final double highcpuLimit,
                                     final double percentageDecrease,
                                     final AbstractTrafficShapingHandler handler,
                                     final long delay,
                                     final long limitLowBandwidth) {
    useCpuLimits = useCpuLimit;
    waitForNetOp = waitForNetOp2;
    timeoutCon = timeOutCon2;
    lowCpuLimit = lowcpuLimit;
    highCpuLimit = highcpuLimit;
    this.limitLowBandwidth = limitLowBandwidth;
    if (this.limitLowBandwidth < LOWBANDWIDTH_DEFAULT) {
      this.limitLowBandwidth = LOWBANDWIDTH_DEFAULT;
    }
    this.delay = delay;
    if (lowCpuLimit <= 0) {
      lowCpuLimit = highCpuLimit / 2;
    }
    percentageDecreaseRatio = percentageDecrease;
    if (percentageDecreaseRatio <= 0) {
      percentageDecreaseRatio = 0.01;
    } else if (percentageDecreaseRatio >= 1) {
      percentageDecreaseRatio /= 100;
    }
    if (delay < waitForNetOp >> 1) {
      this.delay = waitForNetOp;
    }
    this.handler = handler;
    if (useCpuLimits || highCpuLimit > 0) {
      if (useJdKCpuLimit) {
        try {
          cpuManagement = new CpuManagement();
          constraintInactive = false;
        } catch (final UnsupportedOperationException e) {
          cpuManagement = new CpuManagementNoInfo();
          constraintInactive = true;
        }
      } else {
        cpuManagement = new CpuManagementSysmon();
        constraintInactive = false;
      }
    } else {
      // no test at all
      constraintInactive = true;
      cpuManagement = new CpuManagementNoInfo();
    }
    useBandwidthLimit = highcpuLimit > 0;
    cpuLimit = cpulimit;
    channelLimit = channellimit;
    lastTime = System.currentTimeMillis();
    if (this.handler != null && !constraintInactive && !useBandwidthLimit) {
      executor = new ScheduledThreadPoolExecutor(1);
      executor.scheduleWithFixedDelay(this, this.delay, this.delay,
                                      TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Release the resources
   */
  public void release() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  /**
   * To explicitly set this handler as server mode
   *
   * @param isServer
   */
  public void setServer(final boolean isServer) {
    this.isServer = isServer;
  }

  private double getLastLA() {
    final long newTime = System.currentTimeMillis();
    // first check if last test was done too shortly
    // If last test was wrong, then redo the test
    if (newTime - lastTime < waitForNetOp >> 1 && lastLA <= cpuLimit) {
      // last test was OK, so Continue
      return lastLA;
    }
    lastTime = newTime;
    lastLA = cpuManagement.getLoadAverage();
    return lastLA;
  }

  /**
   * @return True if one of the limit is exceeded. Always False if not a
   *     server mode
   */
  public boolean checkConstraints() {
    if (!isServer) {
      return false;
    }
    if (useCpuLimits && cpuLimit < 1 && cpuLimit > 0) {
      getLastLA();
      if (lastLA <= cpuLimit) {
        lastAlert = NOALERT;
        return false;
      }
      lastAlert = "CPU Constraint: " + lastLA + " > " + cpuLimit;
      logger.info(lastAlert);
      return true;
    }
    if (channelLimit > 0) {
      final int nb = getNumberLocalChannel();
      if (channelLimit < nb) {
        lastAlert = "LocalNetwork Constraint: " + nb + " > " + channelLimit;
        logger.info(lastAlert);
        return true;
      }
    }
    lastAlert = NOALERT;
    return false;
  }

  /**
   * @return the current number of active Local Channel
   */
  protected abstract int getNumberLocalChannel();

  /**
   * Same as checkConstraints except that the thread will sleep some time
   * proportionally to the current Load (if
   * CPU related)
   *
   * @param step the current step in retry
   *
   * @return True if one of the limit is exceeded. Always False if not a
   *     server mode
   */
  public boolean checkConstraintsSleep(final int step) {
    if (!isServer) {
      return false;
    }
    long delayNew = waitForNetOp >> 1;
    if (useCpuLimits && cpuLimit < 1 && cpuLimit > 0) {
      final long newTime = System.currentTimeMillis();
      // first check if last test was done too shortly
      if (newTime - lastTime < delayNew) {
        // If last test was wrong, then wait a bit then redo the test
        if (lastLA > cpuLimit) {
          final double sleep =
              lastLA * delayNew * (step + 1) * random.nextFloat();
          final long shorttime = ((long) sleep / 10) * 10;
          if (shorttime >= 10) {
            try {
              Thread.sleep(shorttime);
            } catch (final InterruptedException ignore) {//NOSONAR
              SysErrLogger.FAKE_LOGGER.ignoreLog(ignore);
            }
          }
        } else {
          // last test was OK, so Continue
          lastAlert = NOALERT;
          return false;
        }
      }
    }
    if (checkConstraints()) {
      delayNew = getSleepTime() * (step + 1);
      try {
        Thread.sleep(delayNew);
      } catch (final InterruptedException ignore) {//NOSONAR
        SysErrLogger.FAKE_LOGGER.ignoreLog(ignore);
      }
      return true;
    } else {
      lastAlert = NOALERT;
      return false;
    }
  }

  /**
   * @return a time below TIMEOUTCON with a random
   */
  public long getSleepTime() {
    return (((long) (timeoutCon * random.nextFloat()) + 5000) / 10) * 10;
  }

  /**
   * @return the cpuLimit
   */
  public double getCpuLimit() {
    return cpuLimit;
  }

  /**
   * @param cpuLimit the cpuLimit to set
   */
  public void setCpuLimit(final double cpuLimit) {
    this.cpuLimit = cpuLimit;
  }

  /**
   * @return the channelLimit
   */
  public int getChannelLimit() {
    return channelLimit;
  }

  /**
   * @param channelLimit the channelLimit to set
   */
  public void setChannelLimit(final int channelLimit) {
    this.channelLimit = channelLimit;
  }

  /**
   * Get the current setting on Read Limit (supposed to be not the value in
   * the
   * handler but in the
   * configuration)
   *
   * @return the current setting on Read Limit
   */
  protected abstract long getReadLimit();

  /**
   * Get the current setting on Write Limit (supposed to be not the value in
   * the
   * handler but in the
   * configuration)
   *
   * @return the current setting on Write Limit
   */
  protected abstract long getWriteLimit();

  /**
   * Set the handler
   *
   * @param handler
   */
  public void setHandler(final AbstractTrafficShapingHandler handler) {
    this.handler = handler;
    if (!constraintInactive && this.handler != null && useBandwidthLimit) {
      if (executor != null) {
        executor.shutdownNow();
      }
      logger.debug("Activate Throttle bandwidth according to CPU usage");
      executor = new ScheduledThreadPoolExecutor(1);
      executor
          .scheduleWithFixedDelay(this, delay, delay, TimeUnit.MILLISECONDS);
    } else {
      if (executor != null) {
        executor.shutdownNow();
        executor = null;
      }
    }
  }

  /**
   * Check every delay if the current cpu usage needs to relax or to
   * constraint the bandwidth
   */
  @Override
  public void run() {
    if (constraintInactive) {
      return;
    }
    final double curLA = getLastLA();
    if (!useBandwidthLimit) {
      return;
    }
    if (curLA > highCpuLimit) {
      final CurLimits curlimit;
      if (curLimits.isEmpty()) {
        // get current limit setting
        curlimit = new CurLimits(getReadLimit(), getWriteLimit());
        if (curlimit.read == 0) {
          // take the current bandwidth
          curlimit.read = handler.trafficCounter().lastReadThroughput();
          if (curlimit.read < limitLowBandwidth) {
            curlimit.read = 0;
          }
        }
        if (curlimit.write == 0) {
          // take the current bandwidth
          curlimit.write = handler.trafficCounter().lastWriteThroughput();
          if (curlimit.write < limitLowBandwidth) {
            curlimit.write = 0;
          }
        }
      } else {
        curlimit = curLimits.getLast();
      }
      long newread = (long) (curlimit.read * (1 - percentageDecreaseRatio));
      if (newread < limitLowBandwidth) {
        newread = limitLowBandwidth;
      }
      long newwrite = (long) (curlimit.write * (1 - percentageDecreaseRatio));
      if (newwrite < limitLowBandwidth) {
        newwrite = limitLowBandwidth;
      }
      final CurLimits newlimit = new CurLimits(newread, newwrite);
      if (curLimits.isEmpty() || curlimit.read != newread ||
          curlimit.write != newwrite) {
        // Not same limit so add this limit
        curLimits.add(newlimit);
        logger.info("Set new low limit since CPU = {} {}:{}", curLA, newwrite,
                    newread);
        handler.configure(newlimit.write, newlimit.read);
        nbSinceLastDecrease += PAYLOAD;
      }
    } else if (curLA < lowCpuLimit) {
      if (curLimits.isEmpty()) {
        // nothing to do
        return;
      }
      if (nbSinceLastDecrease > 0) {
        nbSinceLastDecrease--;
        // wait a bit more in case
        return;
      }
      nbSinceLastDecrease = 0;
      curLimits.pollLast();
      final CurLimits newlimit;
      if (curLimits.isEmpty()) {
        // reset to default limits
        final long newread = getReadLimit();
        final long newwrite = getWriteLimit();
        logger.info("Restore limit since CPU = {} {}:{}", curLA, newwrite,
                    newread);
        handler.configure(newwrite, newread);
      } else {
        // set next upper values
        newlimit = curLimits.getLast();
        final long newread = newlimit.read;
        final long newwrite = newlimit.write;
        logger.info("Set new upper limit since CPU = {} {}:{}", curLA, newwrite,
                    newread);
        handler.configure(newwrite, newread);
        // give extra payload to prevent a brutal return to normal
        nbSinceLastDecrease = PAYLOAD;
      }
    }
  }
}
