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

package org.waarp.ftp.client;

import java.io.OutputStream;

/**
 * Simple /dev/null output stream
 */
public class NullOutputStream extends OutputStream {

  /**
   *
   */
  public NullOutputStream() {
    // nothing
  }

  @Override
  public void close() {
    // nothing
  }

  @Override
  public void flush() {
    // nothing
  }

  @Override
  public void write(final byte[] b, final int off, final int len) {
    // nothing
  }

  @Override
  public void write(final byte[] b) {
    // nothing
  }

  @Override
  public void write(final int b) {
    // nothing
  }

}
