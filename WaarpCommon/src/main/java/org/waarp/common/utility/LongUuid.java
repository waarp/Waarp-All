/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.utility;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UUID Generator (also Global UUID Generator) but limited to 1 Long (64 bits) <br>
 * <br>
 * Inspired from com.groupon locality-uuid which used combination of internal counter value - process id -
 * and Timestamp. see https://github.com/groupon/locality-uuid.java <br>
 * <br>
 * But force sequence and take care of errors and improves some performance issues
 * 
 * @author "Frederic Bregier"
 *
 */
public final class LongUuid {

    /**
     * Random Generator
     */
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();
    /**
     * So MAX value on 2 bytes
     */
    private static final int MAX_PID = 65536;
    /**
     * 2 bytes value maximum
     */
    private static final int JVMPID = jvmProcessId();
    /**
     * Counter part
     */
    private static final AtomicInteger COUNTER = new AtomicInteger(RANDOM.nextInt());
    /**
     * Byte size of UUID
     */
    private static final int UUIDSIZE = 8;

    /**
     * real UUID
     */
    private final byte[] uuid;

    /**
     * Constructor that generates a new UUID using the current process id, MAC address, and timestamp
     */
    public LongUuid() {
        final long time = System.currentTimeMillis();
        uuid = new byte[UUIDSIZE];

        // atomically
        final int count;
        synchronized (COUNTER) {
            count = COUNTER.incrementAndGet();
            if (count == Integer.MAX_VALUE) {
                COUNTER.set(0);
            }
        }

        // copy pid to uuid
        uuid[0] = (byte) (JVMPID >> 8);
        uuid[1] = (byte) (JVMPID);

        // copy timestamp into uuid (up to 2^36 s = 2 years rolling)
        uuid[2] = (byte) (time >> 28);
        uuid[3] = (byte) (time >> 20);
        uuid[4] = (byte) (time >> 12);

        // Keep 4 first bytes, 4 bytes coming from Timestamp => 2^20 (at most 1M / 1/2s)
        uuid[5] = (byte) (((count >> 16) & 0x0F) | ((time >> 4) & 0xF0));
        uuid[6] = (byte) (count >> 8);
        uuid[7] = (byte) (count);
    }

    /**
     * Constructor that takes a byte array as this UUID's content
     * 
     * @param bytes
     *            UUID content
     */
    public LongUuid(final byte[] bytes) {
        if (bytes.length != UUIDSIZE)
            throw new RuntimeException("Attempted to parse malformed UUID: " + Arrays.toString(bytes));

        uuid = Arrays.copyOf(bytes, UUIDSIZE);
    }

    public LongUuid(final long value) {
        uuid = new byte[UUIDSIZE];
        uuid[0] = (byte) (value >> 56);
        uuid[1] = (byte) (value >> 48);
        uuid[2] = (byte) (value >> 40);
        uuid[3] = (byte) (value >> 32);
        uuid[4] = (byte) (value >> 24);
        uuid[5] = (byte) (value >> 16);
        uuid[6] = (byte) (value >> 8);
        uuid[7] = (byte) (value);
    }

    public LongUuid(final String idsource) {
        final String id = idsource.trim();

        if (id.length() != UUIDSIZE * 2)
            throw new RuntimeException("Attempted to parse malformed UUID: " + id);

        uuid = Hexa.fromHex(id);
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
    public byte[] getBytes() {
        return Arrays.copyOf(uuid, UUIDSIZE);
    }

    /**
     * extract process id from raw UUID bytes and return as int
     * 
     * @return id of process that generated the UUID
     */
    public int getProcessId() {
        return ((uuid[0] & 0xFF) << 8) | (uuid[1] & 0xFF);
    }

    /**
     * extract timestamp from raw UUID bytes and return as int
     * 
     * @return millisecond UTC timestamp from generation of the UUID
     */
    public long getTimestamp() {
        long time;
        time = ((long) uuid[2] & 0xFF) << 28;
        time |= ((long) uuid[3] & 0xFF) << 20;
        time |= ((long) uuid[4] & 0xFF) << 12;
        time |= ((long) uuid[5] & 0xF0) << 4;
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof LongUuid))
            return false;
        return (this == o) || Arrays.equals(this.uuid, ((LongUuid) o).uuid);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(uuid);
    }

    /**
     * 
     * @return the equivalent UUID as long
     */
    public long getLong() {
        long value = ((long) uuid[0] & 0xFF) << 56;
        value |= ((long) uuid[1] & 0xFF) << 48;
        value |= ((long) uuid[2] & 0xFF) << 40;
        value |= ((long) uuid[3] & 0xFF) << 32;
        value |= ((long) uuid[4] & 0xFF) << 24;
        value |= ((long) uuid[5] & 0xFF) << 16;
        value |= ((long) uuid[6] & 0xFF) << 8;
        value |= ((long) uuid[7] & 0xFF);
        return value;
    }

    /**
     * 
     * @param length
     * @return a byte array with random values
     */
    public static final byte[] getRandom(final int length) {
        final byte[] result = new byte[length];
        RANDOM.nextBytes(result);
        return result;
    }

    // pulled from http://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id
    public static int jvmProcessId() {
        // Note: may fail in some JVM implementations
        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');

        if (index < 1) {
            System.err.println("Could not get JVMPID");
            return RANDOM.nextInt(MAX_PID);
        }
        try {
            return Integer.parseInt(jvmName.substring(0, index)) % MAX_PID;
        } catch (NumberFormatException e) {
            System.err.println("Could not get JVMPID");
            e.printStackTrace();
            return RANDOM.nextInt(MAX_PID);
        }
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        long pseudoMax = Long.MAX_VALUE >> 16;
        System.out.println(new Date(pseudoMax));
        System.out.println(new LongUuid().toString());

        for (int i = 0; i < 10; i++) {
            LongUuid uuid = new LongUuid();
            System.out.println(System.currentTimeMillis() + " " + uuid + "=" + uuid.getLong() + ":"
                    + uuid.getProcessId() + ":" + uuid.getTimestamp());
        }
        for (int i = 0; i < 10; i++) {
            LongUuid uuid = new LongUuid();
            System.out.println(System.currentTimeMillis() + " " + uuid + "=" + uuid.getLong() + ":"
                    + uuid.getProcessId() + ":" + uuid.getTimestamp());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }

        final int n = 100000000;

        for (int i = 0; i < n; i++) {
            LongUuid uuid = new LongUuid();
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            LongUuid uuid = new LongUuid();
        }
        long stop = System.currentTimeMillis();
        System.out.println("TimeW = " + (stop - start) + " so " + (n * 1000 / (stop - start)) + " Uuids/s");

        start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            LongUuid uuid = new LongUuid();
            uuid.getLong();
        }
        stop = System.currentTimeMillis();
        System.out.println("TimeW+getLong = " + (stop - start) + " so " + (n * 1000 / (stop - start)) + " Uuids/s");

        start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            LongUuid uuid = new LongUuid();
            uuid = new LongUuid(uuid.getLong());
        }
        stop = System.currentTimeMillis();
        System.out.println("TimeW+reloadFromgetLong = " + (stop - start) + " so " + (n * 1000 / (stop - start))
                + " Uuids/s");

        int count = 0;
        start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            LongUuid uuid = new LongUuid();
            LongUuid uuid2 = new LongUuid(uuid.getLong());
            if (uuid2.equals(uuid))
                count++;
        }
        stop = System.currentTimeMillis();
        System.out.println("TimeWAndTest = " + (stop - start) + " so " + (n * 1000 / (stop - start)) + " Uuids/s "
                + count);

        start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            LongUuid uuid = new LongUuid();
            uuid.getBytes();
        }
        stop = System.currentTimeMillis();
        System.out.println("TimeW+getBytes = " + (stop - start) + " so " + (n * 1000 / (stop - start)) + " Uuids/s");

        start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            LongUuid uuid = new LongUuid();
            uuid = new LongUuid(uuid.getBytes());
        }
        stop = System.currentTimeMillis();
        System.out.println("TimeW+reloadFromgetBytes = " + (stop - start) + " so " + (n * 1000 / (stop - start))
                + " Uuids/s");

        count = 0;
        start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            LongUuid uuid = new LongUuid();
            LongUuid uuid2 = new LongUuid(uuid.getBytes());
            if (uuid2.equals(uuid))
                count++;
        }
        stop = System.currentTimeMillis();
        System.out.println("TimeWAndTest = " + (stop - start) + " so " + (n * 1000 / (stop - start)) + " Uuids/s "
                + count);
    }
}
