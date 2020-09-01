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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.BaseXx;
import org.waarp.common.utility.SingletonUtils;

import java.util.Arrays;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class GUID implements Comparable<GUID> {
  /**
   * ARK header
   */
  public static final String ARK = "ark:/";

  private static final String ATTEMPTED_TO_PARSE_MALFORMED_ARK_GUID =
      "Attempted to parse malformed ARK GUID: ";
  private static final Object FORSYNC = new Object();
  /**
   * Native size of the GUID
   */
  static final int KEYSIZE = 21;
  static final int KEYB64SIZE = 28;
  static final int KEYB32SIZE = 34;
  static final int KEYB16SIZE = KEYSIZE * 2;
  static final int HEADER_POS = 0;
  static final int HEADER_SIZE = 1;
  static final int TENANT_POS = 1;
  static final int TENANT_SIZE = 4;
  static final int PLATFORM_POS = 5;
  static final int PLATFORM_SIZE = 4;
  static final int PID_POS = 9;
  static final int PID_SIZE = 3;
  static final int TIME_POS = 12;
  static final int TIME_SIZE = 6;
  static final int COUNTER_POS = 18;
  static final int COUNTER_SIZE = 3;
  /**
   * Version to store (to check correctness if future algorithm) between 0 and
   * 255
   */
  static final int VERSION = 1 & 0xFF;

  static final int BYTE_SIZE = 8;
  /**
   * Counter part
   */
  private static volatile int counter;
  /**
   * Counter reset
   */
  private static volatile long lastTimeStamp;
  /**
   * real GUID
   */
  @JsonIgnore
  private final byte[] bguid;

  /**
   * @return the KeySize
   */
  public static int getKeySize() {
    return KEYSIZE;
  }


  /**
   * Internal constructor
   *
   * @param size size of the byte representation
   */
  GUID(final int size) {
    bguid = new byte[size];
  }

  /**
   * Constructor that takes a byte array as this GUID's content
   *
   * @param bytes GUID content
   *
   * @throws InvalidArgumentException
   */
  public GUID(final byte[] bytes) throws InvalidArgumentException {
    this(KEYSIZE);
    setBytes(bytes, KEYSIZE);
    if (getVersion() != VERSION) {
      throw new InvalidArgumentException(
          "Version is incorrect: " + getVersion());
    }
  }

  /**
   * Build from String key
   *
   * @param idsource
   *
   * @throws InvalidArgumentException
   */
  public GUID(final String idsource) throws InvalidArgumentException {
    this(KEYSIZE);
    setString(idsource);
    if (getVersion() != VERSION) {
      throw new InvalidArgumentException(
          "Version is incorrect: " + getVersion());
    }
  }

  /**
   * Internal function
   *
   * @param bytes
   * @param size size of the byte representation
   *
   * @return this
   *
   * @throws InvalidArgumentException
   */
  @JsonIgnore
  GUID setBytes(final byte[] bytes, final int size)
      throws InvalidArgumentException {
    if (bytes == null) {
      throw new InvalidArgumentException("Empty argument");
    }
    if (bytes.length != size) {
      throw new InvalidArgumentException(
          "Attempted to parse malformed GUID: (" + bytes.length + ") " +
          Arrays.toString(bytes));
    }
    System.arraycopy(bytes, 0, bguid, 0, size);
    return this;
  }

  /**
   * Constructor that generates a new GUID using the current process id,
   * Platform Id and timestamp with no tenant
   */
  public GUID() {
    this(0, JvmProcessId.macAddressAsInt() & 0x7FFFFFFF);
  }

  /**
   * Constructor that generates a new GUID using the current process id and
   * timestamp
   *
   * @param tenantId tenant id between 0 and 2^30-1
   * @param platformId platform Id between 0 and 2^31-1
   *
   * @throws IllegalArgumentException if any of the argument are out
   *     of range
   */
  public GUID(final int tenantId, final int platformId) {
    this(KEYSIZE);
    if (tenantId < 0 || tenantId > 0x3FFFFFFF) {
      throw new IllegalArgumentException(
          "DomainId must be between 0 and 2^30-1: " + tenantId);
    }
    if (platformId < 0 || platformId > 0x7FFFFFFF) {
      throw new IllegalArgumentException(
          "PlatformId must be between 0 and 2^31-1: " + platformId);
    }

    // atomically
    final long time;
    final int count;
    synchronized (FORSYNC) {
      long tmptime = System.currentTimeMillis();
      if (lastTimeStamp != tmptime) {
        counter = 0;
        lastTimeStamp = tmptime;
      }
      count = ++counter;
      if (count > 0xFFFFFF) {
        try {
          FORSYNC.wait(1);//NOSONAR
        } catch (final InterruptedException e) {//NOSONAR
          // ignore
          SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        tmptime = System.currentTimeMillis();
        counter = 0;
        lastTimeStamp = tmptime;
      }
      time = tmptime;
    }
    // 1 bytes = Version (8)
    bguid[HEADER_POS] = (byte) VERSION;

    // 4 bytes - 2 bits = Domain (30)
    int value = tenantId;
    bguid[TENANT_POS + 3] = (byte) (value & 0xFF);
    value >>>= BYTE_SIZE;
    bguid[TENANT_POS + 2] = (byte) (value & 0xFF);
    value >>>= BYTE_SIZE;
    bguid[TENANT_POS + 1] = (byte) (value & 0xFF);
    value >>>= BYTE_SIZE;
    bguid[TENANT_POS] = (byte) (value & 0x3F);

    // 4 bytes = -1 bit Platform (31)
    value = platformId;
    bguid[PLATFORM_POS + 3] = (byte) (value & 0xFF);
    value >>>= BYTE_SIZE;
    bguid[PLATFORM_POS + 2] = (byte) (value & 0xFF);
    value >>>= BYTE_SIZE;
    bguid[PLATFORM_POS + 1] = (byte) (value & 0xFF);
    value >>>= BYTE_SIZE;
    bguid[PLATFORM_POS] = (byte) (value & 0x7F);

    // 3 bytes = -2 bits JVMPID (22)
    value = JvmProcessId.JVMPID;
    bguid[PID_POS + 2] = (byte) (value & 0xFF);
    value >>>= BYTE_SIZE;
    bguid[PID_POS + 1] = (byte) (value & 0xFF);
    value >>>= BYTE_SIZE;
    bguid[PID_POS] = (byte) (value & 0xFF);

    // 6 bytes = timestamp (so up to 8 925 years after Time 0 so year 10
    // 895)
    long lvalue = time;
    bguid[TIME_POS + 5] = (byte) (lvalue & 0xFF);
    lvalue >>>= BYTE_SIZE;
    bguid[TIME_POS + 4] = (byte) (lvalue & 0xFF);
    lvalue >>>= BYTE_SIZE;
    bguid[TIME_POS + 3] = (byte) (lvalue & 0xFF);
    lvalue >>>= BYTE_SIZE;
    bguid[TIME_POS + 2] = (byte) (lvalue & 0xFF);
    lvalue >>>= BYTE_SIZE;
    bguid[TIME_POS + 1] = (byte) (lvalue & 0xFF);
    lvalue >>>= BYTE_SIZE;
    bguid[TIME_POS] = (byte) (lvalue & 0xFF);

    // 3 bytes = counter against collision
    value = count;
    bguid[COUNTER_POS + 2] = (byte) (value & 0xFF);
    value >>>= BYTE_SIZE;
    bguid[COUNTER_POS + 1] = (byte) (value & 0xFF);
    value >>>= BYTE_SIZE;
    bguid[COUNTER_POS] = (byte) (value & 0xFF);

  }

  /**
   * @return the Base32 representation (default of toString)
   */
  @JsonIgnore
  public String toBase32() {
    return BaseXx.getBase32(bguid);
  }

  /**
   * @return the Base64 representation (default of toString)
   */
  @JsonIgnore
  public String toBase64() {
    return BaseXx.getBase64UrlWithoutPadding(bguid);
  }

  /**
   * @return the Hexadecimal representation
   */
  @JsonIgnore
  public String toHex() {
    return BaseXx.getBase16(bguid);
  }

  /**
   * @return the Ark representation of this GUID
   */
  @JsonIgnore
  public String toArk() {
    return new StringBuilder(ARK).append(getTenantId()).append('/')
                                 .append(toArkName()).toString();
  }

  /**
   * @return the String representation of this GUID
   */
  @JsonGetter("id")
  public String getId() {
    return toString();
  }

  /**
   * Internal function
   *
   * @param idsource
   *
   * @return this
   *
   * @throws InvalidArgumentException
   */
  @JsonSetter("id")
  GUID setString(final String idsource) throws InvalidArgumentException {
    if (idsource == null) {
      throw new InvalidArgumentException("Empty argument");
    }
    final String id = idsource.trim();
    if (idsource.startsWith(ARK)) {
      String ids = idsource;
      ids = ids.substring(ARK.length());
      final int separator = ids.indexOf('/');
      if (separator <= 0) {
        throw new InvalidArgumentException(
            ATTEMPTED_TO_PARSE_MALFORMED_ARK_GUID + id);
      }
      int tenantId;
      try {
        tenantId = Integer.parseInt(ids.substring(0, separator));
      } catch (final NumberFormatException e) {
        throw new InvalidArgumentException(
            ATTEMPTED_TO_PARSE_MALFORMED_ARK_GUID + id);
      }
      // BASE32
      ids = ids.substring(separator + 1);
      final byte[] base32 = BaseXx.getFromBase32(ids);
      if (base32.length != KEYSIZE - TENANT_SIZE) {
        throw new InvalidArgumentException(
            ATTEMPTED_TO_PARSE_MALFORMED_ARK_GUID + id);
      }
      System.arraycopy(base32, 0, bguid, HEADER_POS, HEADER_SIZE);
      // GUID Domain default to 0 (from 0 to 2^30-1)
      bguid[TENANT_POS + 3] = (byte) (tenantId & 0xFF);
      tenantId >>>= BYTE_SIZE;
      bguid[TENANT_POS + 2] = (byte) (tenantId & 0xFF);
      tenantId >>>= BYTE_SIZE;
      bguid[TENANT_POS + 1] = (byte) (tenantId & 0xFF);
      tenantId >>>= BYTE_SIZE;
      bguid[TENANT_POS] = (byte) (tenantId & 0x3F);
      // BASE32
      System.arraycopy(base32, HEADER_SIZE, bguid, PLATFORM_POS,
                       PLATFORM_SIZE + PID_SIZE + TIME_SIZE + COUNTER_SIZE);
      return this;
    }
    final int len = id.length();
    try {
      if (len == KEYB16SIZE) {
        // HEXA BASE16
        System.arraycopy(BaseXx.getFromBase16(idsource), 0, bguid, 0, KEYSIZE);
      } else if (len == KEYB32SIZE) {
        // BASE32
        System.arraycopy(BaseXx.getFromBase32(idsource), 0, bguid, 0, KEYSIZE);
      } else if (len == KEYB64SIZE) {
        // BASE64
        System.arraycopy(BaseXx.getFromBase64UrlWithoutPadding(idsource), 0,
                         bguid, 0, KEYSIZE);
      } else {
        throw new InvalidArgumentException(
            "Attempted to parse malformed GUID: (" + len + ") " + id);
      }
    } catch (final IllegalArgumentException e) {
      throw new InvalidArgumentException(
          "Attempted to parse malformed GUID: " + id, e);
    }
    return this;
  }

  /**
   * extract version field as a hex char from raw GUID bytes
   *
   * @return version char
   */
  @JsonIgnore
  public final int getVersion() {
    return bguid[HEADER_POS] & 0xFF;
  }

  /**
   * @return the Tenant Id of GUID from which it belongs to (default being 0)
   */
  @JsonIgnore
  public final int getTenantId() {
    return (bguid[TENANT_POS] & 0x3F) << BYTE_SIZE * 3 |
           (bguid[TENANT_POS + 1] & 0xFF) << BYTE_SIZE * 2 |
           (bguid[TENANT_POS + 2] & 0xFF) << BYTE_SIZE |
           bguid[TENANT_POS + 3] & 0xFF;
  }

  /**
   * Extract Platform id as int. Could be using partial MAC address.
   *
   * @return the Platform id as int, or -1 for unrecognized format
   */
  @JsonIgnore
  public final int getPlatformId() {
    return (bguid[PLATFORM_POS] & 0x7F) << BYTE_SIZE * 3 |
           (bguid[PLATFORM_POS + 1] & 0xFF) << BYTE_SIZE * 2 |
           (bguid[PLATFORM_POS + 2] & 0xFF) << BYTE_SIZE |
           bguid[PLATFORM_POS + 3] & 0xFF;
  }

  /**
   * Extract Platform id as bytes. Could be using partial MAC address.
   *
   * @return byte array of GUID fragment, or null for unrecognized format
   */
  @JsonIgnore
  public final byte[] getMacFragment() {
    if (getVersion() != VERSION) {
      return SingletonUtils.getSingletonByteArray();
    }
    final byte[] x = new byte[6];
    x[0] = 0;
    x[1] = 0;
    x[2] = (byte) (bguid[PLATFORM_POS] & 0x7F);
    x[3] = bguid[PLATFORM_POS + 1];
    x[4] = bguid[PLATFORM_POS + 2];
    x[5] = bguid[PLATFORM_POS + 3];
    return x;
  }

  /**
   * Extract process id and return as int
   *
   * @return id of process that generated the GUID, or -1 for unrecognized
   *     format
   */
  @JsonIgnore
  public final int getProcessId() {
    if (getVersion() != VERSION) {
      return -1;
    }
    return (bguid[PID_POS] & 0xFF) << BYTE_SIZE * 2 |
           (bguid[PID_POS + 1] & 0xFF) << BYTE_SIZE | bguid[PID_POS + 2] & 0xFF;
  }

  /**
   * Extract timestamp and return as long
   *
   * @return millisecond UTC timestamp from generation of the GUID, or -1 for
   *     unrecognized format
   */
  @JsonIgnore
  public final long getTimestamp() {
    if (getVersion() != VERSION) {
      return -1;
    }
    long time = 0;
    for (int i = 0; i < TIME_SIZE; i++) {
      time <<= BYTE_SIZE;
      time |= bguid[TIME_POS + i] & 0xFF;
    }
    return time;
  }

  /**
   * @return the associated counter against collision value
   */
  @JsonIgnore
  public final int getCounter() {
    return (bguid[COUNTER_POS] & 0xFF) << BYTE_SIZE * 2 |
           (bguid[COUNTER_POS + 1] & 0xFF) << BYTE_SIZE |
           bguid[COUNTER_POS + 2] & 0xFF;
  }

  /**
   * @return the Ark Name part of Ark representation
   */
  public final String toArkName() {
    final byte[] temp = new byte[KEYSIZE - TENANT_SIZE];
    System.arraycopy(bguid, HEADER_POS, temp, 0, HEADER_SIZE);
    System.arraycopy(bguid, PLATFORM_POS, temp, HEADER_SIZE,
                     PLATFORM_SIZE + PID_SIZE + TIME_SIZE + COUNTER_SIZE);
    return BaseXx.getBase32(temp);
  }


  @Override
  public String toString() {
    return toBase32();
  }

  /**
   * copy the uuid of this GUID, so that it can't be changed, and return it
   *
   * @return raw byte array of GUID
   */
  @JsonIgnore
  public byte[] getBytes() {
    return Arrays.copyOf(bguid, bguid.length);
  }

  @Override
  @JsonIgnore
  public int hashCode() {
    return Arrays.hashCode(bguid);
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof GUID)) {
      return false;
    }
    return this == o || Arrays.equals(bguid, ((GUID) o).bguid);
  }

  @Override
  public int compareTo(final GUID guid) {
    final int id = getTenantId();
    final int id2 = guid.getTenantId();
    if (id != id2) {
      return id < id2? -1 : 1;
    }
    final long ts = getTimestamp();
    final long ts2 = guid.getTimestamp();
    if (ts == ts2) {
      final int ct = getCounter();
      final int ct2 = guid.getCounter();
      if (ct == ct2) {
        // then all must be equals, else whatever
        return Arrays.equals(this.bguid, guid.getBytes())? 0 : -1;
      }
      // Cannot be equal
      return ct < ct2? -1 : 1;
    }
    // others as ProcessId or Platform are unimportant in comparison
    return ts < ts2? -1 : 1;
  }

}
