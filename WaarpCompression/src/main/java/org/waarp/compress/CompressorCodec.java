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
package org.waarp.compress;

import java.io.File;

public interface CompressorCodec {
  /**
   * @param uncompressedSize
   *
   * @return the maximum size for Compressed length
   */
  int maxCompressedLength(int uncompressedSize);

  /**
   * @return the compressed byte array
   */
  byte[] compress(byte[] input, final int inputLength)
      throws MalformedInputException;

  /**
   * @param input
   * @param inputLength
   * @param output
   * @param maxOutputLength
   *
   * @return the size of ouput
   *
   * @throws MalformedInputException
   */
  int compress(final byte[] input, final int inputLength, final byte[] output,
               final int maxOutputLength) throws MalformedInputException;

  /**
   * Compress the file input to the file output
   *
   * @param input
   * @param output
   *
   * @return the new length
   *
   * @throws MalformedInputException
   */
  long compress(final File input, final File output)
      throws MalformedInputException;

  /**
   * @return the decompressed byte array
   */
  byte[] decompress(byte[] input, final int length)
      throws MalformedInputException;

  /**
   * @param input
   * @param inputLength
   * @param output
   * @param maxOutputLength
   *
   * @return the size of output
   *
   * @throws MalformedInputException
   */
  int decompress(final byte[] input, final int inputLength, final byte[] output,
                 final int maxOutputLength) throws MalformedInputException;

  /**
   * Decompress the file input to the file output
   *
   * @param input
   * @param output
   *
   * @return the new length
   *
   * @throws MalformedInputException
   */
  long decompress(final File input, final File output)
      throws MalformedInputException;

  /**
   * @param input
   * @param length
   *
   * @return the size of the decompressed array, 0 if unknown
   */
  int getDecompressedSize(final byte[] input, final int length);


}
