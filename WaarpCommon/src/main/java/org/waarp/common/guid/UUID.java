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

import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.BaseXx;
import org.waarp.common.utility.Hexa;
import org.waarp.common.utility.StringUtils;

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
 * @deprecated Will be replaced by GUUID
 */
@Deprecated
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
  private final byte[] buuid;

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

    buuid = new byte[KEYSIZE];

    // UUID cycle default to 0
    buuid[0] = (byte) (id & 0xFF);

    // switch the order of the count in 4 byte segments and place into uuid
    buuid[1] = (byte) (count & 0xFF);
    buuid[2] = (byte) (count >> 8 & 0xFF);
    buuid[3] = (byte) (count >> 16 & 0xFF);

    // place UUID version (value between 0 and 3) in first 2 bits
    // copy pid to uuid
    buuid[4] = (byte) (VERSION << 6 | (JvmProcessId.JVMPID & 0x3F0000) >> 16);
    buuid[5] = (byte) (JvmProcessId.JVMPID >> 8);
    buuid[6] = (byte) JvmProcessId.JVMPID;

    // copy rest of mac address into uuid
    buuid[7] = JvmProcessId.mac[0];
    buuid[8] = JvmProcessId.mac[1];
    buuid[9] = JvmProcessId.mac[2];
    buuid[10] = JvmProcessId.mac[3];
    buuid[11] = JvmProcessId.mac[4];
    buuid[12] = JvmProcessId.mac[5];

    // copy timestamp into uuid (up to 56 bits so up to 2 200 000 years after Time 0)
    buuid[13] = (byte) (time >> 48);
    buuid[14] = (byte) (time >> 40);
    buuid[15] = (byte) (time >> 32);
    buuid[16] = (byte) (time >> 24);
    buuid[17] = (byte) (time >> 16);
    buuid[18] = (byte) (time >> 8);
    buuid[19] = (byte) time;
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
      buuid[0] = 0;
      buuid[4] =
          VERSION; // Special Version 0 for compatibility with standard UUID
      buuid[7] = 0;
      buuid[8] = 0;
    }
  }

  /**
   * Create a UUID immediately compatible with a standard UUID implementation
   *
   * @param mostSigBits
   * @param leastSigBits
   */
  public UUID(long mostSigBits, long leastSigBits) {
    buuid = new byte[KEYSIZE];
    buuid[0] = 0;
    buuid[1] = (byte) (mostSigBits >> 56);
    buuid[2] = (byte) (mostSigBits >> 48);
    buuid[3] = (byte) (mostSigBits >> 40);
    buuid[4] =
        VERSION; // Special Version 0 for compatibility with standard UUID
    buuid[5] = (byte) (mostSigBits >> 32);
    buuid[6] = (byte) (mostSigBits >> 24);
    buuid[7] = 0;
    buuid[8] = 0;
    buuid[9] = (byte) (mostSigBits >> 16);
    buuid[10] = (byte) (mostSigBits >> 8);
    buuid[11] = (byte) mostSigBits;

    buuid[12] = (byte) (leastSigBits >> 56);
    buuid[13] = (byte) (leastSigBits >> 48);
    buuid[14] = (byte) (leastSigBits >> 40);
    buuid[15] = (byte) (leastSigBits >> 32);
    buuid[16] = (byte) (leastSigBits >> 24);
    buuid[17] = (byte) (leastSigBits >> 16);
    buuid[18] = (byte) (leastSigBits >> 8);
    buuid[19] = (byte) leastSigBits;
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
    buuid = new byte[KEYSIZE];
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
  private UUID setBytes(final byte[] bytes) throws RuntimeException {
    if (bytes.length != KEYSIZE && bytes.length != UTILUUIDKEYSIZE) {
      throw new RuntimeException(
          "Attempted to parse malformed UUID: (" + bytes.length + ") " +
          Arrays.toString(bytes));
    }
    if (bytes.length == UTILUUIDKEYSIZE) {
      buuid[0] = 0;
      System.arraycopy(bytes, 0, buuid, 1, 3);
      buuid[4] = VERSION;
      System.arraycopy(bytes, 3, buuid, 5, 2);
      buuid[7] = 0;
      buuid[8] = 0;
      System.arraycopy(bytes, 5, buuid, 9, 11);
    } else {
      System.arraycopy(bytes, 0, buuid, 0, KEYSIZE);
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
      buuid = Hexa.fromHex(id);
    } else if (len == KEYB64SIZE || len == KEYB64SIZE + 1) {
      // BASE64
      buuid = BaseXx.getFromBase64(id);
    } else {
      throw new RuntimeException(
          "Attempted to parse malformed UUID: (" + len + ") " + id);
    }
  }

  public String toBase64() {
    return BaseXx.getBase64(buuid);
  }

  public String toHex() {
    return Hexa.toHex(buuid);
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
    return Arrays.copyOf(buuid, KEYSIZE);
  }

  /**
   * extract version field as a hex char from raw UUID bytes
   *
   * @return version char
   */
  public int getVersion() {
    return (buuid[4] & 0xC0) >> 6;
  }

  /**
   * @return the id of the subset of UUID from which it belongs to (default
   *     being 0)
   */
  public int getId() {
    return buuid[0] & 0xFF;
  }

  /**
   * extract process id from raw UUID bytes and return as int
   *
   * @return id of process that generated the UUID, or -1 for unrecognized
   *     format
   */
  public int getProcessId() {
    if (getVersion() != VERSION) {
      return -1;
    }

    return (buuid[4] & 0x3F) << 16 | (buuid[5] & 0xFF) << 8 | buuid[6] & 0xFF;
  }

  /**
   * @return the associated counter value
   */
  public int getCounter() {
    int count = (buuid[3] & 0xFF) << 16;
    count |= (buuid[2] & 0xFF) << 8;
    count |= buuid[1] & 0xFF;
    return count;
  }

  /**
   * extract timestamp from raw UUID bytes and return as int
   *
   * @return millisecond UTC timestamp from generation of the UUID, or -1 for
   *     unrecognized format
   */
  public long getTimestamp() {
    if (getVersion() != VERSION) {
      return -1;
    }

    long time;
    time = ((long) buuid[13] & 0xFF) << 48;
    time |= ((long) buuid[14] & 0xFF) << 40;
    time |= ((long) buuid[15] & 0xFF) << 32;
    time |= ((long) buuid[16] & 0xFF) << 24;
    time |= ((long) buuid[17] & 0xFF) << 16;
    time |= ((long) buuid[18] & 0xFF) << 8;
    time |= (long) buuid[19] & 0xFF;
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
  public byte[] getMacFragment() {
    if (getVersion() != VERSION) {
      return null;
    }

    final byte[] x = new byte[6];

    x[0] = buuid[7];
    x[1] = buuid[8];
    x[2] = buuid[9];
    x[3] = buuid[10];
    x[4] = buuid[11];
    x[5] = buuid[12];

    return x;
  }

  /**
   * @return the least significant bits (as in standard UUID implementation)
   */
  public long getLeastSignificantBits() {
    long least;
    least = ((long) buuid[12] & 0xFF) << 56;
    least |= ((long) buuid[13] & 0xFF) << 48;
    least |= ((long) buuid[14] & 0xFF) << 40;
    least |= ((long) buuid[15] & 0xFF) << 32;
    least |= ((long) buuid[16] & 0xFF) << 24;
    least |= ((long) buuid[17] & 0xFF) << 16;
    least |= ((long) buuid[18] & 0xFF) << 8;
    least |= (long) buuid[19] & 0xFF;
    return least;
  }

  /**
   * @return the most significant bits (as in standard UUID implementation)
   */
  public long getMostSignificantBits() {
    long most;
    most = ((long) buuid[1] & 0xFF) << 56;
    most |= ((long) buuid[2] & 0xFF) << 48;
    most |= ((long) buuid[3] & 0xFF) << 40;
    most |= ((long) buuid[5] & 0xFF) << 32;
    most |= ((long) buuid[6] & 0xFF) << 24;
    most |= ((long) buuid[9] & 0xFF) << 16;
    most |= ((long) buuid[10] & 0xFF) << 8;
    most |= (long) buuid[11] & 0xFF;
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
    System.arraycopy(buuid, 1, newUuid, 0, 3);
    System.arraycopy(buuid, 5, newUuid, 3, 2);
    System.arraycopy(buuid, 9, newUuid, 5, 11);
    return newUuid;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof UUID)) {
      return false;
    }
    return this == o || Arrays.equals(buuid, ((UUID) o).buuid);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(buuid);
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
        return Arrays.equals(buuid, arg0.buuid)? 0 : -1;
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
      SysErrLogger.FAKE_LOGGER.sysout(uuid);
      System.exit(0);//NOSONAR
    }
    if (args[0].length() <= 3) {
      final int val = Integer.parseInt(args[0]);
      final UUID uuid = new UUID(val);
      SysErrLogger.FAKE_LOGGER.sysout(uuid);
      System.exit(0);//NOSONAR
    }
    try {
      new UUID(args[0]);
    } catch (final RuntimeException e) {
      SysErrLogger.FAKE_LOGGER.sysout(e.getMessage());
      System.exit(2);//NOSONAR
    }
    SysErrLogger.FAKE_LOGGER.sysout("ok");
    System.exit(0);//NOSONAR
  }
}
