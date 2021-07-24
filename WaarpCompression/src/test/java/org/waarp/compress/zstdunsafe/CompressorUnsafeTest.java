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
package org.waarp.compress.zstdunsafe;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class CompressorUnsafeTest {
  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void testMagic() {
    final byte[] buffer = new byte[4];
    final int address = 0;

    ZstdFrameCompressor.writeMagic(buffer, address, address + buffer.length);
    ZstdFrameDecompressor.verifyMagic(buffer, address, address + buffer.length);
  }

  @Test
  public void testMagicFailsWithSmallBuffer() {
    exceptionRule.expect(IllegalArgumentException.class);
    exceptionRule.expectMessage("buffer too small");
    final byte[] buffer = new byte[3];
    ZstdFrameCompressor.writeMagic(buffer, 0, buffer.length);
  }

  @Test
  public void testFrameHeaderFailsWithSmallBuffer() {
    exceptionRule.expect(IllegalArgumentException.class);
    exceptionRule.expectMessage("buffer too small");
    final byte[] buffer =
        new byte[ZstdFrameCompressor.MAX_FRAME_HEADER_SIZE - 1];
    ZstdFrameCompressor.writeFrameHeader(buffer, 0, buffer.length, 1000, 1024);
  }

  @Test
  public void testFrameHeader() {
    verifyFrameHeader(1, 1024, new FrameHeader(2, -1, 1, -1, true));
    verifyFrameHeader(256, 1024, new FrameHeader(3, -1, 256, -1, true));

    verifyFrameHeader(65536 + 256, 1024 + 128,
                      new FrameHeader(6, 1152, 65536 + 256, -1, true));
    verifyFrameHeader(65536 + 256, 1024 + 128 * 2,
                      new FrameHeader(6, 1024 + 128 * 2, 65536 + 256, -1,
                                      true));
    verifyFrameHeader(65536 + 256, 1024 + 128 * 3,
                      new FrameHeader(6, 1024 + 128 * 3, 65536 + 256, -1,
                                      true));
    verifyFrameHeader(65536 + 256, 1024 + 128 * 4,
                      new FrameHeader(6, 1024 + 128 * 4, 65536 + 256, -1,
                                      true));
    verifyFrameHeader(65536 + 256, 1024 + 128 * 5,
                      new FrameHeader(6, 1024 + 128 * 5, 65536 + 256, -1,
                                      true));
    verifyFrameHeader(65536 + 256, 1024 + 128 * 6,
                      new FrameHeader(6, 1024 + 128 * 6, 65536 + 256, -1,
                                      true));
    verifyFrameHeader(65536 + 256, 1024 + 128 * 7,
                      new FrameHeader(6, 1024 + 128 * 7, 65536 + 256, -1,
                                      true));
    verifyFrameHeader(65536 + 256, 1024 + 128 * 8,
                      new FrameHeader(6, 1024 + 128 * 8, 65536 + 256, -1,
                                      true));

    verifyFrameHeader(65536 + 256, 2048,
                      new FrameHeader(6, 2048, 65536 + 256, -1, true));

    verifyFrameHeader(Integer.MAX_VALUE, 1024,
                      new FrameHeader(6, 1024, Integer.MAX_VALUE, -1, true));
  }

  @Test
  public void testMinimumWindowSize() {
    exceptionRule.expect(IllegalArgumentException.class);
    exceptionRule.expectMessage("Minimum window size is 1024");
    final byte[] buffer = new byte[ZstdFrameCompressor.MAX_FRAME_HEADER_SIZE];
    final int address = 0;

    ZstdFrameCompressor.writeFrameHeader(buffer, address,
                                         address + buffer.length, 2000, 1023);
  }

  @Test
  public void testWindowSizePrecision() {
    exceptionRule.expect(IllegalArgumentException.class);
    exceptionRule.expectMessage(
        "Window size of magnitude 2^10 must be multiple of 128");
    final byte[] buffer = new byte[ZstdFrameCompressor.MAX_FRAME_HEADER_SIZE];
    final int address = 0;

    ZstdFrameCompressor.writeFrameHeader(buffer, address,
                                         address + buffer.length, 2000, 1025);
  }

  private void verifyFrameHeader(final int inputSize, final int windowSize,
                                 final FrameHeader expected) {
    final byte[] buffer = new byte[ZstdFrameCompressor.MAX_FRAME_HEADER_SIZE];
    final int address = 0;

    final int size = ZstdFrameCompressor.writeFrameHeader(buffer, address,
                                                          address +
                                                          buffer.length,
                                                          inputSize,
                                                          windowSize);

    assertEquals(size, expected.headerSize);

    final FrameHeader actual =
        ZstdFrameDecompressor.readFrameHeader(buffer, address,
                                              address + buffer.length);
    assertEquals(actual, expected);
  }
}
