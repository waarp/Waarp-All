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
import org.waarp.common.database.DbAdmin;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class for Constraint Limit Handler for Waarp project
 *
 *
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

  private final Random random = new Random();
  private CpuManagementInterface cpuManagement;
  private double cpuLimit = 1.0; // was 0.8;
  private int channelLimit; // was 1000;
  private boolean isServer;
  private double lastLA;
  private long lastTime;

  // Dynamic throttling
  private long WAITFORNETOP = 1000;
  private long TIMEOUTCON = 10000;
  private double highCpuLimit; // was 0.8;
  private double lowCpuLimit; // was 0.5;
  private double percentageDecreaseRatio = 0.25;
  private long delay = 1000;
  private long limitLowBandwidth = LOWBANDWIDTH_DEFAULT;
  private AbstractTrafficShapingHandler handler;
  private ScheduledThreadPoolExecutor executor;

  private static class CurLimits {
    long read;
    long write;

    private CurLimits(long read, long write) {
      this.read = read;
      this.write = write;
    }
  }

  private final LinkedList<CurLimits> curLimits = new LinkedList<CurLimits>();
  private int nbSinceLastDecrease;
  private static final int payload = 5;
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
   * @param WAITFORNETOP2 1000 ms as wait for a network operation
   * @param TIMEOUTCON2 10000 ms as timeout limit
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
  public WaarpConstraintLimitHandler(long WAITFORNETOP2, long TIMEOUTCON2,
                                     boolean useJdkCpuLimit, double lowcpuLimit,
                                     double highcpuLimit,
                                     double percentageDecrease,
                                     AbstractTrafficShapingHandler handler,
                                     long delay, long limitLowBandwidth) {
    this(WAITFORNETOP2, TIMEOUTCON2, true, useJdkCpuLimit, 0, 0, lowcpuLimit,
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
  public WaarpConstraintLimitHandler(long WAITFORNETOP2, long TIMEOUTCON2,
                                     boolean useCpuLimit,
                                     boolean useJdKCpuLimit, double cpulimit,
                                     int channellimit) {
    this(WAITFORNETOP2, TIMEOUTCON2, useCpuLimit, useJdKCpuLimit, cpulimit,
         channellimit, 0, 0, 0.01, null, 1000000, LOWBANDWIDTH_DEFAULT);
  }

  /**
   * This constructor enables both Connection check ability and throttling
   * bandwidth with cpu usage
   *
   * @param WAITFORNETOP2 1000 ms as wait for a network operation
   * @param TIMEOUTCON2 10000 ms as timeout limit
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
  public WaarpConstraintLimitHandler(long WAITFORNETOP2, long TIMEOUTCON2,
                                     boolean useCpuLimit,
                                     boolean useJdKCpuLimit, double cpulimit,
                                     int channellimit, double lowcpuLimit,
                                     double highcpuLimit,
                                     double percentageDecrease,
                                     AbstractTrafficShapingHandler handler,
                                     long delay, long limitLowBandwidth) {
    useCpuLimits = useCpuLimit;
    WAITFORNETOP = WAITFORNETOP2;
    TIMEOUTCON = TIMEOUTCON2;
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
    if (delay < WAITFORNETOP >> 1) {
      this.delay = WAITFORNETOP;
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
  public void setServer(boolean isServer) {
    this.isServer = isServer;
  }

  private double getLastLA() {
    final long newTime = System.currentTimeMillis();
    // first check if last test was done too shortly
    if (newTime - lastTime < WAITFORNETOP >> 1) {
      // If last test was wrong, then redo the test
      if (lastLA <= cpuLimit) {
        // last test was OK, so Continue
        return lastLA;
      }
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
      if (lastLA > cpuLimit) {
        lastAlert = "CPU Constraint: " + lastLA + " > " + cpuLimit;
        logger.debug(lastAlert);
        return true;
      }
    }
    if (channelLimit > 0) {
      int nb = DbAdmin.getNbConnection() - DbAdmin.getHttpSession();
      if (channelLimit < nb) {
        lastAlert = "Network Constraint: " + nb + " > " + channelLimit;
        logger.debug(lastAlert);
        return true;
      }
      nb = getNumberLocalChannel();
      if (channelLimit < nb) {
        lastAlert = "LocalNetwork Constraint: " + nb + " > " + channelLimit;
        logger.debug(lastAlert);
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
  public boolean checkConstraintsSleep(int step) {
    if (!isServer) {
      return false;
    }
    long delay = WAITFORNETOP >> 1;
    if (useCpuLimits && cpuLimit < 1 && cpuLimit > 0) {
      final long newTime = System.currentTimeMillis();
      // first check if last test was done too shortly
      if (newTime - lastTime < delay) {
        // If last test was wrong, then wait a bit then redo the test
        if (lastLA > cpuLimit) {
          final double sleep = lastLA * delay * (step + 1) * random.nextFloat();
          final long shorttime = ((long) sleep / 10) * 10;
          if (shorttime >= 10) {
            try {
              Thread.sleep(shorttime);
            } catch (final InterruptedException e) {
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
      delay = getSleepTime() * (step + 1);
      try {
        Thread.sleep(delay);
      } catch (final InterruptedException e) {
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
    return (((long) (TIMEOUTCON * random.nextFloat()) + 5000) / 10) * 10;
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
  public void setCpuLimit(double cpuLimit) {
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
  public void setChannelLimit(int channelLimit) {
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
  public void setHandler(AbstractTrafficShapingHandler handler) {
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
   * constraint
   * the bandwidth
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
      CurLimits curlimit = null;
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
        logger.debug(
            "Set new low limit since CPU = " + curLA + " " + newwrite + ":" +
            newread);
        handler.configure(newlimit.write, newlimit.read);
        nbSinceLastDecrease += payload;
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
      CurLimits newlimit = null;
      if (curLimits.isEmpty()) {
        // reset to default limits
        final long newread = getReadLimit();
        final long newwrite = getWriteLimit();
        logger.debug(
            "restore limit since CPU = " + curLA + " " + newwrite + ":" +
            newread);
        handler.configure(newwrite, newread);
      } else {
        // set next upper values
        newlimit = curLimits.getLast();
        final long newread = newlimit.read;
        final long newwrite = newlimit.write;
        logger.debug(
            "Set new upper limit since CPU = " + curLA + " " + newwrite + ":" +
            newread);
        handler.configure(newwrite, newread);
        // give extra payload to prevent a brutal return to normal
        nbSinceLastDecrease = payload;
      }
    }
  }
}
