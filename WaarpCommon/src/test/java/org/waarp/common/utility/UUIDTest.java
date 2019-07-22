/*******************************************************************************
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.waarp.common.utility;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.*;

public class UUIDTest {
  private static final int VERSION = 1 & 0x0F;
  private static final int NB = 100000;

  @Test
  public void testStructure() {
    UUID id = new UUID();
    String str = id.toHex();

    assertEquals('0', str.charAt(0));
    assertEquals('0', str.charAt(1));
    assertEquals(40, str.length());
    long least = id.getLeastSignificantBits();
    long most = id.getMostSignificantBits();
    UUID id2 = new UUID(most, least);
    assertTrue(id2.getLeastSignificantBits() == least);
    assertTrue(id2.getMostSignificantBits() == most);
    java.util.UUID id3 = new java.util.UUID(most, least);
    UUID id4 = new UUID(id3);
    assertTrue(id3.getLeastSignificantBits() == least);
    assertTrue(id3.getMostSignificantBits() == most);
    assertTrue(id4.getLeastSignificantBits() == least);
    assertTrue(id4.getMostSignificantBits() == most);
    java.util.UUID id5 = id4.getJavaUuid();
    assertTrue(id5.getLeastSignificantBits() == least);
    assertTrue(id5.getMostSignificantBits() == most);
    UUID id6 = new UUID(id.javaUuidGetBytes());
    assertTrue(id6.getLeastSignificantBits() == least);
    assertTrue(id6.getMostSignificantBits() == most);

    byte[] random = UUID.getRandom(6);
    UUID.setMAC(random);
    UUID id7 = new UUID(1);
    assertArrayEquals(id7.getMacFragment(), random);
    assertTrue(id7.getVersion() == 1);
  }

  @Test
  public void testParsing() {
    UUID id1 = new UUID();
    UUID id2 = new UUID(id1.toString());
    assertEquals(id1, id2);
    assertEquals(id1.hashCode(), id2.hashCode());

    id2 = new UUID(id1.toHex());
    assertEquals(id1, id2);
    assertEquals(id1.hashCode(), id2.hashCode());

    id2 = new UUID(id1.toBase64());
    assertEquals(id1, id2);
    assertEquals(id1.hashCode(), id2.hashCode());

    UUID id3 = new UUID(id1.getBytes());
    assertEquals(id1, id3);
  }

  @Test
  public void testNonSequentialValue() {
    final int n = NB;
    String[] ids = new String[n];

    for (int i = 0; i < n; i++) {
      ids[i] = new UUID().toString();
    }

    for (int i = 1; i < n; i++) {
      assertFalse(ids[i - 1].equals(ids[i]));
    }
  }

  @Test
  public void testGetBytesImmutability() {
    UUID id = new UUID();
    byte[] bytes = id.getBytes();
    byte[] original = Arrays.copyOf(bytes, bytes.length);
    bytes[0] = 0;
    bytes[1] = 0;
    bytes[2] = 0;

    assertArrayEquals(id.getBytes(), original);
  }

  @Test
  public void testConstructorImmutability() {
    UUID id = new UUID();
    byte[] bytes = id.getBytes();
    byte[] original = Arrays.copyOf(bytes, bytes.length);

    UUID id2 = new UUID(bytes);
    bytes[0] = 0;
    bytes[1] = 0;

    assertArrayEquals(id2.getBytes(), original);
  }

  @Test
  public void testVersionField() {
    UUID generated = new UUID();
    assertEquals(VERSION, generated.getVersion());

    UUID parsed1 = new UUID("AKG8zkAvFAgAJ3vUswABS0W_baw=");
    assertEquals(VERSION, parsed1.getVersion());
    UUID parsed2 = new UUID("00a1bcce402f140800277bd4b300014b45bf6dac");
    assertEquals(VERSION, parsed2.getVersion());
    assertEquals(parsed1, parsed2);
  }

  @Test
  public void testPIDField() throws Exception {
    UUID id = new UUID();

    assertEquals(UUID.jvmProcessId(), id.getProcessId());
  }

  @Test
  public void testDateField() {
    UUID id = new UUID();
    assertTrue(id.getTimestamp() > new Date().getTime() - 100);
    assertTrue(id.getTimestamp() < new Date().getTime() + 100);
  }

  @Test
  public void testMacAddressField() throws Exception {
    byte[] mac = UUID.macAddress();

    // if the machine is not connected to a network it has no active MAC address
    if (mac == null || mac.length < 6) {
      mac = UUID.getRandom(6);
      UUID.setMAC(mac);
    }

    UUID id = new UUID();
    byte[] field = id.getMacFragment();
    assertEquals(mac[0], field[0]);
    assertEquals(mac[1], field[1]);
    assertEquals(mac[2], field[2]);
    assertEquals(mac[3], field[3]);
    assertEquals(mac[4], field[4]);
    assertEquals(mac[5], field[5]);
  }

  @Test
  public void testForDuplicates() {
    int n = NB;
    Set<UUID> uuids = new HashSet<UUID>();
    UUID[] uuidArray = new UUID[n];

    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      uuidArray[i] = new UUID();
    }
    long stop = System.currentTimeMillis();
    System.out.println(
        "Time = " + (stop - start) + " so " + n * 1000 / (stop - start) +
        " Uuids/s");

    for (int i = 0; i < n; i++) {
      uuids.add(uuidArray[i]);
    }

    System.out.println("Create " + n + " and get: " + uuids.size());
    assertEquals(n, uuids.size());
    checkConsecutive(uuidArray);
  }

  private static void checkConsecutive(final UUID[] UUIDArray) {
    final int n = UUIDArray.length;
    int i = 1;
    int largest = 0;
    for (; i < n; i++) {
      if (UUIDArray[i].getTimestamp() > UUIDArray[i - 1].getTimestamp()) {
        int j = i + 1;
        final long time = UUIDArray[i].getTimestamp();
        for (; j < n; j++) {
          if (UUIDArray[i].compareTo(UUIDArray[j]) >= 0) {
            for (int k = i; k <= j; k++) {
              /*System.out.println(k + "=" + UUIDArray[k].getId() + ":" +
                                 UUIDArray[k].getCounter() + ":" +
                                 UUIDArray[k].getTimestamp() + ":" +
                                 UUIDArray[k].getVersion() + ":" +
                                 UUIDArray[k].getProcessId());*/
            }
          }
          assertEquals(-1, UUIDArray[i].compareTo(UUIDArray[j]));
          if (UUIDArray[j].getTimestamp() > time) {
            if (largest < j - i) {
              largest = j - i;
            }
            i = j;
            break;
          }
        }
      } else {
        if (UUIDArray[i - 1].compareTo(UUIDArray[i]) >= 0) {
          for (int k = i - 1; k <= i; k++) {
            /*System.out.println(k + "=" + UUIDArray[k].getId() + ":" +
                               UUIDArray[k].getCounter() + ":" +
                               UUIDArray[k].getTimestamp() + ":" +
                               UUIDArray[k].getVersion() + ":" +
                               UUIDArray[k].getProcessId());*/
          }
        }
        assertEquals(-1, UUIDArray[i - 1].compareTo(UUIDArray[i]));
        int j = i + 1;
        final long time = UUIDArray[i].getTimestamp();
        for (; j < n; j++) {
          if (UUIDArray[i - 1].compareTo(UUIDArray[j]) >= 0) {
            for (int k = i - 1; k <= j; k++) {
              /*System.out.println(k + "=" + UUIDArray[k].getId() + ":" +
                                 UUIDArray[k].getCounter() + ":" +
                                 UUIDArray[k].getTimestamp() + ":" +
                                 UUIDArray[k].getVersion() + ":" +
                                 UUIDArray[k].getProcessId());*/
            }
          }
          assertNotEquals(0, UUIDArray[i - 1].compareTo(UUIDArray[j]));
          if (UUIDArray[j].getTimestamp() > time) {
            if (largest < j - i + 1) {
              largest = j - i + 1;
            }
            i = j;
            break;
          }
        }
      }
    }
    System.out.println(largest + " different consecutive elements");
  }

  @Test
  public void concurrentGeneration() throws Exception {
    int numThreads = 10;
    Thread[] threads = new Thread[numThreads];
    int n = NB;
    UUID[] uuids = new UUID[n];

    final long start = System.currentTimeMillis();
    for (int i = 0; i < numThreads; i++) {
      threads[i] = new Generator(n / numThreads, uuids, i, numThreads);
      threads[i].start();
    }

    for (int i = 0; i < numThreads; i++) {
      threads[i].join();
    }
    final long stop = System.currentTimeMillis();

    Set<UUID> uuidSet = new HashSet<UUID>();

    int effectiveN = n / numThreads * numThreads;
    for (int i = 0; i < effectiveN; i++) {
      uuidSet.add(uuids[i]);
    }

    assertEquals(effectiveN, uuidSet.size());
    uuidSet.clear();
    System.out.println(
        "TimeConcurrent = " + (stop - start) + " so " +
        uuids.length * 1000 / (stop - start) + " UUIDs/s");
    final TreeSet<UUID> set = new TreeSet<UUID>();
    for (int i = 0; i < uuids.length; i++) {
      set.add(uuids[i]);
    }
    checkConsecutive(set.toArray(new UUID[0]));

  }

  private static class Generator extends Thread {
    private final UUID[] uuids;
    int id;
    int n;
    int numThreads;

    public Generator(int n, UUID[] uuids, int id, int numThreads) {
      this.n = n;
      this.uuids = uuids;
      this.id = id;
      this.numThreads = numThreads;
    }

    @Override
    public void run() {
      for (int i = 0; i < n; i++) {
        uuids[numThreads * i + id] = new UUID();
      }
    }
  }
}
