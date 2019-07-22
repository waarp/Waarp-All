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
package org.waarp.common.file.passthrough;

import io.netty.buffer.ByteBuf;

import java.nio.channels.FileChannel;
import java.util.List;

/**
 * This interface is for Object used in Passthrough mode.
 *
 *
 */
public interface PassthroughFile {

  boolean isDirectory();

  boolean isFile();

  ByteBuf read(int sizeblock) throws PassthroughException;

  int write(ByteBuf buffer) throws PassthroughException;

  long length();

  boolean canRead();

  boolean canWrite();

  boolean isInReading();

  boolean isInWriting();

  boolean exists();

  boolean delete() throws PassthroughException;

  boolean renameTo(String path) throws PassthroughException;

  void position(long position) throws PassthroughException;

  void flush() throws PassthroughException;

  /**
   * Note: be aware to not directly use transferTo or transferFrom at once but
   * to use them by chunk to prevent
   * memory usage (mmap used under the wood by the JVM)
   *
   * @param out
   *
   * @return the size in bytes transfered
   *
   * @throws PassthroughException
   */
  long transferTo(FileChannel out) throws PassthroughException;

  void close() throws PassthroughException;

  // Some extra functions that could be not implemented but just throwing the exception
  List<String> wildcard(String subPath) throws PassthroughException;

  boolean mkdir() throws PassthroughException;

  boolean rmdir() throws PassthroughException;

  boolean changeDirectory(String path) throws PassthroughException;

  /**
   * Return the Modification time
   *
   * @return the Modification time as a String YYYYMMDDHHMMSS.sss
   */
  String getModificationTime() throws PassthroughException;

  List<String> list() throws PassthroughException;

  List<String> listFull(boolean lsFormat) throws PassthroughException;

  String fileFull(boolean lsFormat) throws PassthroughException;

  long getFreeSpace() throws PassthroughException;

  long getCRC() throws PassthroughException;

  byte[] getMD5() throws PassthroughException;

  byte[] getSHA1() throws PassthroughException;

}
