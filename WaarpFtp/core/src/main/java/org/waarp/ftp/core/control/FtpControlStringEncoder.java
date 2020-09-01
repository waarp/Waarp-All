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
package org.waarp.ftp.core.control;

import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.Charset;

/**
 * Exactly same as StringEncoder from Netty
 */
class FtpControlStringEncoder extends StringEncoder {
  /**
   *
   */
  FtpControlStringEncoder() {
  }

  /**
   * @param charset
   *
   * @see StringEncoder
   */
  FtpControlStringEncoder(final String charset) {
    super(Charset.forName(charset));
  }

  /**
   * @param charset
   *
   * @see StringEncoder
   */
  FtpControlStringEncoder(final Charset charset) {
    super(charset);
  }
}
