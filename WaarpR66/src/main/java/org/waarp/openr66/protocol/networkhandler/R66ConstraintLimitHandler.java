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
package org.waarp.openr66.protocol.networkhandler;

import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.waarp.common.cpu.WaarpConstraintLimitHandler;
import org.waarp.openr66.protocol.configuration.Configuration;

/**
 * R66 Constraint Limit Handler
 * <p>
 * Constraint Limit (CPU and connection - network and local -) handler, only for
 * server side (requested or
 * requester).
 */
public class R66ConstraintLimitHandler extends WaarpConstraintLimitHandler {
  public R66ConstraintLimitHandler() {
  }

  /**
   * @param useJdKCpuLimit True to use JDK Cpu native or False for
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
  public R66ConstraintLimitHandler(final boolean useJdKCpuLimit,
                                   final double lowcpuLimit,
                                   final double highcpuLimit,
                                   final double percentageDecrease,
                                   final GlobalTrafficShapingHandler handler,
                                   final long delay,
                                   final long limitLowBandwidth) {
    super(1000, Configuration.configuration != null?
              Configuration.configuration.getTimeoutCon() : 30000, useJdKCpuLimit,
          lowcpuLimit, highcpuLimit, percentageDecrease, handler, delay,
          limitLowBandwidth);
  }

  /**
   * @param useCpuLimit True to enable cpuLimit on connection check
   * @param useJdKCpuLimit True to use JDK Cpu native or False for
   *     JavaSysMon
   * @param cpulimit high cpu limit (0<= x < 1) to refuse new
   *     connections
   * @param channellimit number of connection limit (0<= x)
   */
  public R66ConstraintLimitHandler(final boolean useCpuLimit,
                                   final boolean useJdKCpuLimit,
                                   final double cpulimit,
                                   final int channellimit) {
    super(1000, Configuration.configuration != null?
              Configuration.configuration.getTimeoutCon() : 30000, useCpuLimit,
          useJdKCpuLimit, cpulimit, channellimit);
  }

  /**
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
  public R66ConstraintLimitHandler(final boolean useCpuLimit,
                                   final boolean useJdKCpuLimit,
                                   final double cpulimit,
                                   final int channellimit,
                                   final double lowcpuLimit,
                                   final double highcpuLimit,
                                   final double percentageDecrease,
                                   final GlobalTrafficShapingHandler handler,
                                   final long delay,
                                   final long limitLowBandwidth) {
    super(1000, Configuration.configuration != null?
              Configuration.configuration.getTimeoutCon() : 30000, useCpuLimit,
          useJdKCpuLimit, cpulimit, channellimit, lowcpuLimit, highcpuLimit,
          percentageDecrease, handler, delay, limitLowBandwidth);
  }

  @Override
  protected final int getNumberLocalChannel() {
    if (Configuration.configuration.getLocalTransaction() != null) {
      return Configuration.configuration.getLocalTransaction()
                                        .getNumberLocalChannel();
    }
    return 0;
  }

  @Override
  protected final long getReadLimit() {
    return Configuration.configuration.getServerGlobalReadLimit();
  }

  @Override
  protected final long getWriteLimit() {
    return Configuration.configuration.getServerGlobalWriteLimit();
  }

}
