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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class NullPrintStream extends PrintStream {

  /**
   * The singleton instance.
   */
  public static final NullPrintStream NULL_PRINT_STREAM = new NullPrintStream();

  /**
   * Constructs an instance.
   */
  public NullPrintStream() {
    // Relies on the default charset which is OK since we are not writing.
    super(NullOutputStream.NULL_OUTPUT_STREAM);
  }


  /**
   * This OutputStream writes all data to the famous <b>/dev/null</b>.
   * <p>
   * This output stream has no destination (file/socket etc.) and all
   * bytes written to it are ignored and lost.
   * </p>
   */
  public static class NullOutputStream extends OutputStream {

    /**
     * Deprecated in favor of {@link #NULL_OUTPUT_STREAM}.
     *
     * TODO: Will be private in 3.0.
     *
     * @deprecated Use {@link #NULL_OUTPUT_STREAM}.
     */
    @Deprecated
    public NullOutputStream() {
      super();
    }

    /**
     * A singleton.
     */
    public static final NullOutputStream NULL_OUTPUT_STREAM =
        new NullOutputStream();

    /**
     * Does nothing - output to <code>/dev/null</code>.
     *
     * @param b The bytes to write
     * @param off The start offset
     * @param len The number of bytes to write
     */
    @Override
    public void write(final byte[] b, final int off, final int len) {
      // To /dev/null
    }

    /**
     * Does nothing - output to <code>/dev/null</code>.
     *
     * @param b The byte to write
     */
    @Override
    public void write(final int b) {
      // To /dev/null
    }

    /**
     * Does nothing - output to <code>/dev/null</code>.
     *
     * @param b The bytes to write
     *
     * @throws IOException never
     */
    @Override
    public void write(final byte[] b) throws IOException {
      // To /dev/null
    }

  }
}
