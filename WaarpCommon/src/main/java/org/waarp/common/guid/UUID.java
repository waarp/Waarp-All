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

import org.waarp.common.utility.Base64;
import org.waarp.common.utility.Hexa;
import org.waarp.common.utility.StringUtils;

import java.io.IOException;
import java.util.Arrays;

/**
 * UUID Generator (also Global UUID Generator) <br>
 * <br>
 * Inspired from com.groupon locality-uuid which used combination of internal
 * counter value - process id -
 * fragment of MAC address and Timestamp. see https://github.com/groupon/locality-uuid.java
 * <br>
 * <br>
 * But force sequence and take care of errors and improves some performance
 * issues
 *
 *
 */
public final class UUID implements Comparable<UUID> {
  /**
   * Native size of the UUID
   */
  public static final int KEYSIZE = 20;
  private static final int KEYB64SIZE = 28;
  private static final int KEYB16SIZE = KEYSIZE * 2;
  private static final int UTILUUIDKEYSIZE = 16;

  /**
   * Version to store (to check correctness if future algorithm)
   */
  private static final int VERSION = 1 & 0x0F;

  /**
   * Counter part
   */
  private static final int MAXSTART = Integer.MAX_VALUE >> 4;
  private static volatile int counter = StringUtils.RANDOM.nextInt(MAXSTART);
  /**
   * Counter reset
   */
  private static volatile long lastTimeStamp;

  /**
   * real UUID
   */
  private final byte[] uuid;

  /**
   * Constructor that generates a new UUID using the current process id, MAC
   * address, and timestamp
   */
  public UUID() {
    this((byte) 0);
  }

  /**
   * Constructor that generates a new UUID using the current process id, MAC
   * address, and timestamp
   *
   * @param id the id of the subset of UUID from which it belongs to
   *     (default being 0, else between 1 and 255)
   */
  public UUID(int id) {
    if (id < 0 || id > 255) {
      throw new IllegalArgumentException("ID must be between 0 and 255");
    }

    // atomically
    final long time;
    final int count;
    synchronized (StringUtils.RANDOM) {
      time = System.currentTimeMillis();
      if (lastTimeStamp != time) {
        counter = StringUtils.RANDOM.nextInt(MAXSTART);
        lastTimeStamp = time;
      }
      count = ++counter;
      if (count == Integer.MAX_VALUE) {
        counter = Integer.MIN_VALUE + 1;
      }
    }

    uuid = new byte[KEYSIZE];

    // UUID cycle default to 0
    uuid[0] = (byte) (id & 0xFF);

    // switch the order of the count in 4 byte segments and place into uuid
    uuid[1] = (byte) (count & 0xFF);
    uuid[2] = (byte) (count >> 8 & 0xFF);
    uuid[3] = (byte) (count >> 16 & 0xFF);

    // place UUID version (value between 0 and 3) in first 2 bits
    // copy pid to uuid
    uuid[4] = (byte) (VERSION << 6 | (JvmProcessId.JVMPID & 0x3F0000) >> 16);
    uuid[5] = (byte) (JvmProcessId.JVMPID >> 8);
    uuid[6] = (byte) JvmProcessId.JVMPID;

    // copy rest of mac address into uuid
    uuid[7] = JvmProcessId.MAC[0];
    uuid[8] = JvmProcessId.MAC[1];
    uuid[9] = JvmProcessId.MAC[2];
    uuid[10] = JvmProcessId.MAC[3];
    uuid[11] = JvmProcessId.MAC[4];
    uuid[12] = JvmProcessId.MAC[5];

    // copy timestamp into uuid (up to 56 bits so up to 2 200 000 years after Time 0)
    uuid[13] = (byte) (time >> 48);
    uuid[14] = (byte) (time >> 40);
    uuid[15] = (byte) (time >> 32);
    uuid[16] = (byte) (time >> 24);
    uuid[17] = (byte) (time >> 16);
    uuid[18] = (byte) (time >> 8);
    uuid[19] = (byte) time;
  }

  /**
   * Create a UUID immediately compatible with a standard UUID implementation
   *
   * @param on128bits True for 128 bits limitation (16 Bytes instead
   *     of 20
   *     Bytes)
   */
  public UUID(boolean on128bits) {
    this();
    if (on128bits) {
      uuid[0] = 0;
      uuid[4] =
          VERSION; // Special Version 0 for compatibility with standard UUID
      uuid[7] = 0;
      uuid[8] = 0;
    }
  }

  /**
   * Create a UUID immediately compatible with a standard UUID implementation
   *
   * @param mostSigBits
   * @param leastSigBits
   */
  public UUID(long mostSigBits, long leastSigBits) {
    uuid = new byte[KEYSIZE];
    uuid[0] = 0;
    uuid[1] = (byte) (mostSigBits >> 56);
    uuid[2] = (byte) (mostSigBits >> 48);
    uuid[3] = (byte) (mostSigBits >> 40);
    uuid[4] = VERSION; // Special Version 0 for compatibility with standard UUID
    uuid[5] = (byte) (mostSigBits >> 32);
    uuid[6] = (byte) (mostSigBits >> 24);
    uuid[7] = 0;
    uuid[8] = 0;
    uuid[9] = (byte) (mostSigBits >> 16);
    uuid[10] = (byte) (mostSigBits >> 8);
    uuid[11] = (byte) mostSigBits;

    uuid[12] = (byte) (leastSigBits >> 56);
    uuid[13] = (byte) (leastSigBits >> 48);
    uuid[14] = (byte) (leastSigBits >> 40);
    uuid[15] = (byte) (leastSigBits >> 32);
    uuid[16] = (byte) (leastSigBits >> 24);
    uuid[17] = (byte) (leastSigBits >> 16);
    uuid[18] = (byte) (leastSigBits >> 8);
    uuid[19] = (byte) leastSigBits;
  }

  /**
   * Create a UUID immediately compatible with a standard UUID implementation
   *
   * @param uuid
   */
  public UUID(java.util.UUID uuid) {
    this(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
  }

  /**
   * Constructor that takes a byte array as this UUID's content
   *
   * @param bytes UUID content
   *
   * @throws RuntimeException
   */
  public UUID(final byte[] bytes) throws RuntimeException {
    uuid = new byte[KEYSIZE];
    setBytes(bytes);
  }

  /**
   * Internal function
   *
   * @param bytes
   *
   * @return
   *
   * @throws RuntimeException
   */
  protected UUID setBytes(final byte[] bytes) throws RuntimeException {
    if (bytes.length != KEYSIZE && bytes.length != UTILUUIDKEYSIZE) {
      throw new RuntimeException(
          "Attempted to parse malformed UUID: (" + bytes.length + ") " +
          Arrays.toString(bytes));
    }
    if (bytes.length == UTILUUIDKEYSIZE) {
      uuid[0] = 0;
      System.arraycopy(bytes, 0, uuid, 1, 3);
      uuid[4] = VERSION;
      System.arraycopy(bytes, 3, uuid, 5, 2);
      uuid[7] = 0;
      uuid[8] = 0;
      System.arraycopy(bytes, 5, uuid, 9, 11);
    } else {
      System.arraycopy(bytes, 0, uuid, 0, KEYSIZE);
    }
    return this;
  }

  /**
   * Build from String key
   *
   * @param idsource
   *
   * @throws RuntimeException
   */
  public UUID(final String idsource) throws RuntimeException {
    final String id = idsource.trim();

    final int len = id.length();
    if (len == KEYB16SIZE) {
      // HEXA
      uuid = Hexa.fromHex(id);
    } else if (len == KEYB64SIZE || len == KEYB64SIZE + 1) {
      // BASE64
      try {
        uuid = Base64.decode(id, Base64.URL_SAFE | Base64.DONT_GUNZIP);
      } catch (final IOException e) {
        throw new RuntimeException("Attempted to parse malformed UUID: " + id,
                                   e);
      }
    } else {
      throw new RuntimeException(
          "Attempted to parse malformed UUID: (" + len + ") " + id);
    }
  }

  public final String toBase64() {
    try {
      return Base64.encodeBytes(uuid, Base64.URL_SAFE);
    } catch (final IOException e) {
      return Base64.encodeBytes(uuid);
    }
  }

  public final String toHex() {
    return Hexa.toHex(uuid);
  }

  @Override
  public String toString() {
    return toBase64();
  }

  /**
   * copy the uuid of this UUID, so that it can't be changed, and return it
   *
   * @return raw byte array of UUID
   */
  public byte[] getBytes() {
    return Arrays.copyOf(uuid, KEYSIZE);
  }

  /**
   * extract version field as a hex char from raw UUID bytes
   *
   * @return version char
   */
  public final int getVersion() {
    return (uuid[4] & 0xC0) >> 6;
  }

  /**
   * @return the id of the subset of UUID from which it belongs to (default
   *     being 0)
   */
  public final int getId() {
    return uuid[0] & 0xFF;
  }

  /**
   * extract process id from raw UUID bytes and return as int
   *
   * @return id of process that generated the UUID, or -1 for unrecognized
   *     format
   */
  public final int getProcessId() {
    if (getVersion() != VERSION) {
      return -1;
    }

    return (uuid[4] & 0x3F) << 16 | (uuid[5] & 0xFF) << 8 | uuid[6] & 0xFF;
  }

  /**
   * @return the associated counter value
   */
  public final int getCounter() {
    int count = (uuid[3] & 0xFF) << 16;
    count |= (uuid[2] & 0xFF) << 8;
    count |= uuid[1] & 0xFF;
    return count;
  }

  /**
   * extract timestamp from raw UUID bytes and return as int
   *
   * @return millisecond UTC timestamp from generation of the UUID, or -1 for
   *     unrecognized format
   */
  public final long getTimestamp() {
    if (getVersion() != VERSION) {
      return -1;
    }

    long time;
    time = ((long) uuid[13] & 0xFF) << 48;
    time |= ((long) uuid[14] & 0xFF) << 40;
    time |= ((long) uuid[15] & 0xFF) << 32;
    time |= ((long) uuid[16] & 0xFF) << 24;
    time |= ((long) uuid[17] & 0xFF) << 16;
    time |= ((long) uuid[18] & 0xFF) << 8;
    time |= (long) uuid[19] & 0xFF;
    return time;
  }

  /**
   * extract MAC address fragment from raw UUID bytes, setting missing values
   * to
   * 0, from the active MAC address
   * when the UUID was generated
   *
   * @return byte array of UUID fragment, or null for unrecognized format
   */
  public final byte[] getMacFragment() {
    if (getVersion() != VERSION) {
      return null;
    }

    final byte[] x = new byte[6];

    x[0] = uuid[7];
    x[1] = uuid[8];
    x[2] = uuid[9];
    x[3] = uuid[10];
    x[4] = uuid[11];
    x[5] = uuid[12];

    return x;
  }

  /**
   * @return the least significant bits (as in standard UUID implementation)
   */
  public long getLeastSignificantBits() {
    long least;
    least = ((long) uuid[12] & 0xFF) << 56;
    least |= ((long) uuid[13] & 0xFF) << 48;
    least |= ((long) uuid[14] & 0xFF) << 40;
    least |= ((long) uuid[15] & 0xFF) << 32;
    least |= ((long) uuid[16] & 0xFF) << 24;
    least |= ((long) uuid[17] & 0xFF) << 16;
    least |= ((long) uuid[18] & 0xFF) << 8;
    least |= (long) uuid[19] & 0xFF;
    return least;
  }

  /**
   * @return the most significant bits (as in standard UUID implementation)
   */
  public long getMostSignificantBits() {
    long most;
    most = ((long) uuid[1] & 0xFF) << 56;
    most |= ((long) uuid[2] & 0xFF) << 48;
    most |= ((long) uuid[3] & 0xFF) << 40;
    most |= ((long) uuid[5] & 0xFF) << 32;
    most |= ((long) uuid[6] & 0xFF) << 24;
    most |= ((long) uuid[9] & 0xFF) << 16;
    most |= ((long) uuid[10] & 0xFF) << 8;
    most |= (long) uuid[11] & 0xFF;
    return most;
  }

  /**
   * @return a UUID compatible with Java.Util package implementation
   */
  public java.util.UUID getJavaUuid() {
    return new java.util.UUID(getMostSignificantBits(),
                              getLeastSignificantBits());
  }

  /**
   * copy the uuid of this UUID, so that it can't be changed, and return it
   *
   * @return raw byte array of UUID for a 16 Bytes UUID
   */
  public byte[] javaUuidGetBytes() {
    final byte[] newUuid = new byte[UTILUUIDKEYSIZE];
    System.arraycopy(uuid, 1, newUuid, 0, 3);
    System.arraycopy(uuid, 5, newUuid, 3, 2);
    System.arraycopy(uuid, 9, newUuid, 5, 11);
    return newUuid;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof UUID)) {
      return false;
    }
    return this == o || Arrays.equals(uuid, ((UUID) o).uuid);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(uuid);
  }

  @Override
  public int compareTo(UUID arg0) {
    if (getId() != arg0.getId()) {
      return getId() < arg0.getId()? -1 : 1;
    }
    final long ts = getTimestamp();
    final long ts2 = arg0.getTimestamp();
    if (ts == ts2) {
      final int ct = getCounter();
      final int ct2 = arg0.getCounter();
      if (ct == ct2) {
        // then all must be equals, else whatever
        return Arrays.equals(uuid, arg0.uuid)? 0 : -1;
      }
      // Cannot be equal
      return ct < ct2? -1 : 1;
    }
    // others as ProcessId or MacFragment is unimportant in comparison
    return ts < ts2? -1 : 1;
  }

  /**
   * Simply return a new UUID. If an argument is provided, 2 cases
   * occur:</br>
   * - length <= 3 = same as UUID(int) - length > 3 = same as checking the
   * given
   * UUID in String format
   *
   * @param args
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      final UUID uuid = new UUID();
      System.out.println(uuid);
      System.exit(0);
    }
    if (args[0].length() <= 3) {
      final int val = Integer.parseInt(args[0]);
      final UUID uuid = new UUID(val);
      System.out.println(uuid);
      System.exit(0);
    }
    try {
      new UUID(args[0]);
    } catch (final RuntimeException e) {
      System.out.println(e.getMessage());
      System.exit(2);
    }
    System.out.println("ok");
    System.exit(0);
  }
}
