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

package org.waarp.openr66.protocol.http.restv2.utils;

import io.netty.handler.codec.http.HttpRequest;
import org.glassfish.jersey.message.internal.AcceptableLanguageTag;
import org.glassfish.jersey.message.internal.HttpHeaderReader;

import java.text.ParseException;
import java.util.List;
import java.util.Locale;

import static javax.ws.rs.core.HttpHeaders.*;

/**
 * A series of utility methods shared by all handlers of the RESTv2 API.
 */
public final class RestUtils {

  /**
   * Makes the default constructor of this utility class inaccessible.
   */
  private RestUtils() throws InstantiationException {
    throw new InstantiationException(
        getClass().getName() + " cannot be instantiated.");
  }

  // ######################### PUBLIC METHODS #################################

  /**
   * Returns the language of the given request.
   *
   * @param request the HTTP request
   *
   * @return the request's language
   */
  public static Locale getLocale(final HttpRequest request) {
    final String langHead = request.headers().get(ACCEPT_LANGUAGE);

    try {
      final List<AcceptableLanguageTag> acceptableLanguages =
          HttpHeaderReader.readAcceptLanguage(langHead);
      AcceptableLanguageTag bestMatch = acceptableLanguages.get(0);
      for (final AcceptableLanguageTag acceptableLanguage : acceptableLanguages) {
        if (acceptableLanguage.getQuality() > bestMatch.getQuality()) {
          bestMatch = acceptableLanguage;
        }
      }
      return bestMatch.getAsLocale();
    } catch (final ParseException e) {
      return Locale.getDefault();
    }
  }

  /**
   * Converts a String into its' corresponding boolean value.
   *
   * @param string the String to convert
   *
   * @return the corresponding boolean value
   *
   * @throws IllegalArgumentException If the String does not represent
   *     a valid boolean value.
   */
  public static boolean stringToBoolean(final String string) {
    if ("true".equalsIgnoreCase(string)) {
      return true;
    } else if ("false".equalsIgnoreCase(string)) {
      return false;
    } else {
      throw new IllegalArgumentException();
    }
  }

}