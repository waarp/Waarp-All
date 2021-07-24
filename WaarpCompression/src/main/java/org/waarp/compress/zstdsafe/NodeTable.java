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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.waarp.compress.zstdsafe;

import java.util.Arrays;

class NodeTable {
  final int[] count;
  final short[] parents;
  final int[] symbols;
  final byte[] numberOfBits;

  public NodeTable(final int size) {
    count = new int[size];
    parents = new short[size];
    symbols = new int[size];
    numberOfBits = new byte[size];
  }

  public void reset() {
    Arrays.fill(count, 0);
    Arrays.fill(parents, (short) 0);
    Arrays.fill(symbols, 0);
    Arrays.fill(numberOfBits, (byte) 0);
  }

  public void copyNode(final int from, final int to) {
    count[to] = count[from];
    parents[to] = parents[from];
    symbols[to] = symbols[from];
    numberOfBits[to] = numberOfBits[from];
  }
}
