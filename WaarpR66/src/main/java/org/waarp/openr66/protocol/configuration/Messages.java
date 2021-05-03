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

package org.waarp.openr66.protocol.configuration;

import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.SystemPropertyUtil;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
  private static final String BUNDLE_NAME = "messages"; //$NON-NLS-1$

  private static ResourceBundle resourceBundle;
  private static String slocale = "en";

  static {
    try {
      setSlocale(
          SystemPropertyUtil.get(R66SystemProperties.OPENR66_LOCALE, "en"));
      if (ParametersChecker.isEmpty(getSlocale())) {
        setSlocale("en");
      }
      init(new Locale(getSlocale()));
    } catch (final Throwable ignored) {//NOSONAR
      SysErrLogger.FAKE_LOGGER //NOSONAR
                               .syserr("Error during static execution",//NOSONAR
                                       ignored);//NOSONAR
    }
  }

  private Messages() {
  }

  public static void init(Locale locale) {
    if (locale == null) {
      setSlocale("en");
      locale = new Locale(getSlocale());
    } else {
      setSlocale(locale.getLanguage());
    }
    resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
  }

  public static String getString(final String key) {
    try {
      return resourceBundle.getString(key);
    } catch (final MissingResourceException e) {
      return '!' + key + '!';
    }
  }

  public static String getString(final String key, final Object... args) {
    try {
      final String source = resourceBundle.getString(key);
      return MessageFormat.format(source, args);
    } catch (final MissingResourceException e) {
      return '!' + key + '!';
    }
  }

  /**
   * @return the slocale
   */
  public static String getSlocale() {
    return slocale;
  }

  /**
   * @param slocale the slocale to set
   */
  public static void setSlocale(final String slocale) {
    Messages.slocale = slocale;
  }
}
