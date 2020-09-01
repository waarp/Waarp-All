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

import com.google.common.io.BaseEncoding;

/**
 * Base16, Base32 and Base64 codecs
 */
public final class BaseXx {
  private static final String ARGUMENT_NULL_NOT_ALLOWED =
      "argument null not allowed";
  private static final BaseEncoding BASE64_URL_WITHOUT_PADDING =
      BaseEncoding.base64Url().omitPadding();
  private static final BaseEncoding BASE64_URL_WITH_PADDING =
      BaseEncoding.base64Url();
  private static final BaseEncoding BASE64 = BaseEncoding.base64();
  private static final BaseEncoding BASE32 =
      BaseEncoding.base32().lowerCase().omitPadding();
  private static final BaseEncoding BASE16 =
      BaseEncoding.base16().lowerCase().omitPadding();

  private BaseXx() {
    // empty
  }

  /**
   * @param bytes to transform
   *
   * @return the Base 16 representation
   *
   * @throws IllegalArgumentException if argument is not compatible
   */
  public static String getBase16(final byte[] bytes) {
    ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, bytes);
    return BASE16.encode(bytes);
  }

  /**
   * @param bytes to transform
   *
   * @return the Base 32 representation
   *
   * @throws IllegalArgumentException if argument is not compatible
   */
  public static String getBase32(final byte[] bytes) {
    ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, bytes);
    return BASE32.encode(bytes);
  }

  /**
   * @param bytes to transform
   *
   * @return the Base 64 Without Padding representation (used only for url)
   *
   * @throws IllegalArgumentException if argument is not compatible
   */
  public static String getBase64UrlWithoutPadding(final byte[] bytes) {
    ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, bytes);
    return BASE64_URL_WITHOUT_PADDING.encode(bytes);
  }

  /**
   * @param bytes to transform
   *
   * @return the Base 64 With Padding representation (used only for url)
   *
   * @throws IllegalArgumentException if argument is not compatible
   */
  public static String getBase64UrlWithPadding(final byte[] bytes) {
    ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, bytes);
    return BASE64_URL_WITH_PADDING.encode(bytes);
  }

  /**
   * @param bytes to transform
   *
   * @return the Base 64 With Padding representation
   *
   * @throws IllegalArgumentException if argument is not compatible
   */
  public static String getBase64(final byte[] bytes) {
    ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, bytes);
    return BASE64.encode(bytes);
  }

  /**
   * @param base16 to transform
   *
   * @return the byte from Base 16
   *
   * @throws IllegalArgumentException if argument is not compatible
   */
  public static byte[] getFromBase16(final String base16) {
    ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, base16);
    return BASE16.decode(base16);
  }

  /**
   * @param base32 to transform
   *
   * @return the byte from Base 32
   *
   * @throws IllegalArgumentException if argument is not compatible
   */
  public static byte[] getFromBase32(final String base32) {
    ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, base32);
    return BASE32.decode(base32);
  }

  /**
   * @param base64 to transform
   *
   * @return the byte from Base 64 Without Padding (used only for url)
   *
   * @throws IllegalArgumentException if argument is not compatible
   */
  public static byte[] getFromBase64UrlWithoutPadding(final String base64) {
    ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, base64);
    return BASE64_URL_WITHOUT_PADDING.decode(base64);
  }

  /**
   * @param base64Padding to transform
   *
   * @return the byte from Base 64 With Padding (used only for url)
   *
   * @throws IllegalArgumentException if argument is not compatible
   */
  public static byte[] getFromBase64UrlPadding(final String base64Padding) {
    ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, base64Padding);
    return BASE64_URL_WITH_PADDING.decode(base64Padding);
  }

  /**
   * @param base64Padding to transform
   *
   * @return the byte from Base 64 With Padding
   *
   * @throws IllegalArgumentException if argument is not compatible
   */
  public static byte[] getFromBase64(final String base64Padding) {
    ParametersChecker.checkParameter(ARGUMENT_NULL_NOT_ALLOWED, base64Padding);
    return BASE64.decode(base64Padding);
  }
}
