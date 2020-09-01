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

/**
 * Copyright Santeri Paavolainen <sjpaavol@cc.helsinki.fi> and Timothy W Macinta
 * (twm@alum.mit.edu)
 * (optimizations and bug fixes) and individual contributors by the @author
 * tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual
 * contributors.
 * <p>
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation; either
 * version 2.1 of the
 * License, or (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this
 * software; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor,
 * Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.waarp.common.digest;

/**
 * Fast implementation of RSA's MD5 hash generator in Java JDK Beta-2 or
 * higher<br>
 * Originally written by Santeri Paavolainen, Helsinki Finland 1996 <br>
 * (c) Santeri Paavolainen, Helsinki Finland 1996 <br>
 * Some changes Copyright (c) 2002 Timothy W Macinta <br>
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library
 * General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or
 * (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Library General Public
 * License for more details.
 * <p>
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not,
 * write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA
 * 02139, USA.
 * <p>
 * See http://www.twmacinta.com/myjava/fast_md5.php for more information on this
 * file.
 * <p>
 * Contains internal state of the MD5 class
 * <p>
 * Please note: I (Timothy Macinta) have put this code in the com.twmacinta.util
 * package only because it came
 * without a package. I was not the the original author of the code, although I
 * did optimize it
 * (substantially) and fix some bugs.
 *
 * @author Santeri Paavolainen <sjpaavol@cc.helsinki.fi>
 * @author Timothy W Macinta (twm@alum.mit.edu) (optimizations and bug fixes)
 **/

class MD5State {
  /**
   * 128-bit state
   */
  final int[] state;

  /**
   * 64-bit character count
   */
  long count;

  /**
   * 64-byte buffer (512 bits) for storing to-be-hashed characters
   */
  final byte[] buffer;

  /**
   * Constructor
   */
  MD5State() {
    buffer = new byte[64];
    count = 0;
    state = new int[4];

    state[0] = 0x67452301;
    state[1] = 0xefcdab89;
    state[2] = 0x98badcfe;
    state[3] = 0x10325476;

  }

  /**
   * Create this State as a copy of another state
   *
   * @param from
   */
  MD5State(final MD5State from) {
    this();
    System.arraycopy(from.buffer, 0, buffer, 0, buffer.length);
    System.arraycopy(from.state, 0, state, 0, state.length);

    count = from.count;
  }
}
