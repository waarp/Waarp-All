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
package org.waarp.gateway.kernel.http.saplink;

import org.waarp.common.logging.SysErrLogger;
import org.waarp.gateway.kernel.HttpJsonDefinition;
import org.waarp.gateway.kernel.HttpPageHandler;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;

/**
 *
 */
public final class HttpGerenateJsonConfiguration {

  /**
   * @param args
   */
  public static void main(final String[] args) {
    if (args.length < 1) {
      SysErrLogger.FAKE_LOGGER
          .syserr("Not enough argument: filepath for output");
      System.exit(1);//NOSONAR
    }
    final HttpPageHandler httpPageHandler =
        HttpSapBusinessFactory.initializeHttpPageHandler();
    try {
      HttpJsonDefinition.exportConfiguration(httpPageHandler, args[0]);
    } catch (final HttpIncorrectRequestException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
    }
  }

}
