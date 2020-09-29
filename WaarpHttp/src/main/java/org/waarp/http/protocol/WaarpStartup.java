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

package org.waarp.http.protocol;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.server.R66Server;

import javax.servlet.ServletException;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Point to startup Waarp R66 server a unique time
 */
public class WaarpStartup {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpStartup.class);
  private static final AtomicBoolean STARTED = new AtomicBoolean(false);

  private WaarpStartup() {
    // Empty and private
  }

  /**
   * Will try to start the Waarp R66 server (next calls will be ignored)
   *
   * @throws ServletException
   */
  public static void startupWaarp(final File configurationFile)
      throws ServletException {
    if (STARTED.compareAndSet(false, true)) {
      R66Server.main(new String[] { configurationFile.getAbsolutePath() });
      logger.info("Start Done");
    }
  }
}
