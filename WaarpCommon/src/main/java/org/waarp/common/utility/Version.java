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

package org.waarp.common.utility;

import org.waarp.common.logging.SysErrLogger;

import java.io.IOException;
import java.util.Properties;

public class Version {
  private static final String VERSION;
  private static final String ARTIFACT_ID;
  private static final String FULL_VERSION;

  static {
    final Properties properties = new Properties();
    try {
      properties.load(Version.class.getClassLoader()
                                   .getResourceAsStream("project.properties"));
    } catch (final IOException ignore) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignore);
    }
    VERSION = properties.getProperty("version");
    ARTIFACT_ID = properties.getProperty("artifactId");
    FULL_VERSION = ARTIFACT_ID + "." + VERSION;
  }

  public static String version() {
    return VERSION;
  }

  public static String artifactId() {
    return ARTIFACT_ID;
  }

  public static String fullIdentifier() {
    return FULL_VERSION;
  }
}
