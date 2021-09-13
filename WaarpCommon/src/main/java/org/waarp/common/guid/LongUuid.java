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
package org.waarp.common.guid;

import org.waarp.common.utility.Hexa;
import org.waarp.common.utility.StringUtils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UUID Generator (also Global UUID Generator) but limited to 1 Long (64 bits)
 * <br>
 * <br>
 * Inspired from com.groupon locality-uuid which used combination of internal
 * counter value - process id - and
 * Timestamp. see https://github.com/groupon/locality-uuid.java <br>
 * <br>
 * But force sequence and take care of errors and improves some performance
 * issues
 */
public final class LongUuid {
  /**
   * Bits size of Counter
   */
  private static final int SIZE_COUNTER = 20;
  /**
   * Min Counter value
   */
  private static final int MIN_COUNTER = 0;
  /**
   * Max Counter value
   */
  private static final int MAX_COUNTER = (1 << SIZE_COUNTER) - 1;
  /**
   * Counter part
   */
  private static final AtomicInteger COUNTER =
      new AtomicInteger(Math.abs(StringUtils.RANDOM.nextInt(SIZE_COUNTER)));
  /**
   * Byte size of UUID
   */
  private static final int UUIDSIZE = 8;


  /**
   * real UUID
   */
  private final byte[] uuid = { 0, 0, 0, 0, 0, 0, 0, 0 };

  static final synchronized int getCounter() {
    if (COUNTER.compareAndSet(MAX_COUNTER, MIN_COUNTER)) {
      return MAX_COUNTER;
    } else {
      return COUNTER.getAndIncrement();
    }
  }

  public static final long getLongUuid() {
    final long time = System.currentTimeMillis();
    // atomically
    final int count = getCounter();
    // Jvmd Id on 4 first bits
    // Timestamp on 40 bits (2^40 ms = 35 years rolling)
    // Count on 20 bits => 2^20 (1M / ms)
    long uuidAsLong = (JvmProcessId.jvmId & 0xF0L) << 56;
    uuidAsLong |= (time & 0xFFFFFFFFFFL) << 20;
    uuidAsLong |= count & 0xFFFFFL;
    return uuidAsLong;
  }

  /**
   * Constructor that generates a new UUID using the current process id and
   * MAC address, timestamp and a counter
   */
  public LongUuid() {
    this(getLongUuid());
  }

  /**
   * Constructor that takes a byte array as this UUID's content
   *
   * @param bytes UUID content
   */
  public LongUuid(final byte[] bytes) {
    if (bytes.length != UUIDSIZE) {
      throw new RuntimeException(
          "Attempted to parse malformed UUID: " + Arrays.toString(bytes));
    }
    System.arraycopy(bytes, 0, uuid, 0, UUIDSIZE);
  }

  public LongUuid(final long value) {
    uuid[0] = (byte) (value >> 56);
    uuid[1] = (byte) (value >> 48);
    uuid[2] = (byte) (value >> 40);
    uuid[3] = (byte) (value >> 32);
    uuid[4] = (byte) (value >> 24);
    uuid[5] = (byte) (value >> 16);
    uuid[6] = (byte) (value >> 8);
    uuid[7] = (byte) value;
  }

  public LongUuid(final String idsource) {
    final String id = idsource.trim();

    if (id.length() != UUIDSIZE * 2) {
      throw new RuntimeException("Attempted to parse malformed UUID: " + id);
    }
    System.arraycopy(Hexa.fromHex(id), 0, uuid, 0, UUIDSIZE);
  }

  @Override
  public String toString() {
    return Hexa.toHex(uuid);
  }

  /**
   * copy the uuid of this UUID, so that it can't be changed, and return it
   *
   * @return raw byte array of UUID
   */
  public final byte[] getBytes() {
    return Arrays.copyOf(uuid, UUIDSIZE);
  }

  /**
   * extract process id from raw UUID bytes and return as int
   *
   * @return id of process that generated the UUID
   */
  public final int getProcessId() {
    return (uuid[0] >> 4 & 0x0F);
  }

  /**
   * extract timestamp from raw UUID bytes and return as int
   *
   * @return millisecond UTC timestamp from generation of the UUID
   */
  public final long getTimestamp() {
    long time;
    time = ((long) uuid[0] & 0x0F) << 36;
    time |= ((long) uuid[1] & 0xFF) << 28;
    time |= ((long) uuid[2] & 0xFF) << 20;
    time |= ((long) uuid[3] & 0xFF) << 12;
    time |= ((long) uuid[4] & 0xFF) << 4;
    time |= ((long) uuid[5] & 0xF0) >> 4;
    return time;
  }

  @Override
  public final boolean equals(final Object o) {
    if (!(o instanceof LongUuid)) {
      return false;
    }
    return this == o || Arrays.equals(uuid, ((LongUuid) o).uuid);
  }

  @Override
  public final int hashCode() {
    return Arrays.hashCode(uuid);
  }

  /**
   * @return the equivalent UUID as long
   */
  public final long getLong() {
    long value = ((long) uuid[0] & 0xFF) << 56;
    value |= ((long) uuid[1] & 0xFF) << 48;
    value |= ((long) uuid[2] & 0xFF) << 40;
    value |= ((long) uuid[3] & 0xFF) << 32;
    value |= ((long) uuid[4] & 0xFF) << 24;
    value |= ((long) uuid[5] & 0xFF) << 16;
    value |= ((long) uuid[6] & 0xFF) << 8;
    value |= (long) uuid[7] & 0xFF;
    return value;
  }
}
