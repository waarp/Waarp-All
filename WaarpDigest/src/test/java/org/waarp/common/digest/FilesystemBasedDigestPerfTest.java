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

package org.waarp.common.digest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

import static org.junit.Assert.*;

public class FilesystemBasedDigestPerfTest {
  private static final String TESTPHRASE;
  private static final int COUNT = 1000;

  static {
    StringBuilder builder = new StringBuilder(64 * 1024);
    builder.setLength(64 * 1024);
    TESTPHRASE = builder.toString();
  }

  private static final byte[] TESTPHRASEBYTES = TESTPHRASE.getBytes();

  @Test
  public void testGetHashByteBufDigestAlgo() {
    try {
      ByteBuf buf = Unpooled.wrappedBuffer(TESTPHRASEBYTES);
      for (final DigestAlgo algo : DigestAlgo.values()) {
        FilesystemBasedDigest.setUseFastMd5(false);
        FilesystemBasedDigest digest = new FilesystemBasedDigest(algo);
        digest.Update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
        byte[] bmd5 = digest.Final();
        String hex = FilesystemBasedDigest.getHex(bmd5);
        long start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
          FilesystemBasedDigest.setUseFastMd5(false);
          digest = new FilesystemBasedDigest(algo);
          digest.Update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
          bmd5 = digest.Final();
          hex = FilesystemBasedDigest.getHex(bmd5);
          assertTrue(algo + " Hex Not Equals",
                     FilesystemBasedDigest.digestEquals(hex, bmd5));
        }
        long end = System.currentTimeMillis();
        System.out.println("Byte Algo: " + algo + " Time: " + (end - start));
        start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
          buf.readerIndex(0);
          FilesystemBasedDigest.setUseFastMd5(false);
          digest = new FilesystemBasedDigest(algo);
          digest.Update(buf);
          bmd5 = digest.Final();
          hex = FilesystemBasedDigest.getHex(bmd5);
          assertTrue(algo + " Hex Not Equals",
                     FilesystemBasedDigest.digestEquals(hex, bmd5));
        }
        end = System.currentTimeMillis();
        System.out.println("Buf Algo: " + algo + " Time: " + (end - start));
        start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
          FilesystemBasedDigest.setUseFastMd5(true);
          final FilesystemBasedDigest digest2 = new FilesystemBasedDigest(algo);
          digest2.Update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
          final byte[] bmd52 = digest2.Final();
          final String hex2 = FilesystemBasedDigest.getHex(bmd52);
          assertTrue(algo + " Hex Not Equals",
                     FilesystemBasedDigest.digestEquals(hex2, bmd52));
        }
        end = System.currentTimeMillis();
        System.out
            .println("Byte Fast Algo: " + algo + " Time: " + (end - start));
        start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
          buf.readerIndex(0);
          FilesystemBasedDigest.setUseFastMd5(true);
          final FilesystemBasedDigest digest2 = new FilesystemBasedDigest(algo);
          final byte[] bmd53 = FilesystemBasedDigest.getHash(buf, algo);
          final String hex3 = FilesystemBasedDigest.getHex(bmd53);
          assertTrue(algo + " Hex Not Equals",
                     FilesystemBasedDigest.digestEquals(hex3, bmd53));
        }
        end = System.currentTimeMillis();
        System.out
            .println("Buf Fast Algo: " + algo + " Time: " + (end - start));
      }
      Security.addProvider(new BouncyCastleProvider());
      Provider[] providers = Security.getProviders();
      for (Provider provider : providers) {
        System.out.println("Provider: " + provider.getName());
      }
      FilesystemBasedDigest.setUseFastMd5(false);
      FilesystemBasedDigest digest =
          new FilesystemBasedDigest(DigestAlgo.MD5);
      digest.Update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
      byte[] bmd5 = digest.Final();
      String hex = FilesystemBasedDigest.getHex(bmd5);
      long start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        FilesystemBasedDigest.setUseFastMd5(false);
        digest =
            new FilesystemBasedDigest(DigestAlgo.MD5);
        digest.Update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
        bmd5 = digest.Final();
        hex = FilesystemBasedDigest.getHex(bmd5);
        assertTrue(DigestAlgo.MD5 + " Hex Not Equals",
                   FilesystemBasedDigest.digestEquals(hex, bmd5));
      }
      long end = System.currentTimeMillis();
      System.out.println(
          "Byte Algo: " + DigestAlgo.MD5 + " Time: " + (end - start));
      MessageDigest digest2 = MessageDigest.getInstance("MD5", "SUN");
      digest2.update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
      byte[] bmd52 = digest2.digest();
      String hex2 = FilesystemBasedDigest.getHex(bmd52);
      start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        digest2 = MessageDigest.getInstance("MD5", "SUN");
        digest2.update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
        bmd52 = digest2.digest();
        hex2 = FilesystemBasedDigest.getHex(bmd52);
        assertTrue("Native1 Hex Not Equals",
                   FilesystemBasedDigest.digestEquals(hex2, bmd52));
      }
      end = System.currentTimeMillis();
      System.out.println("Buf Algo: MD5 Native1 Time: " + (end - start));
      digest2 = MessageDigest.getInstance("MD5", "SUN");
      System.out.println(digest2.getProvider().getName());
      digest2 = MessageDigest.getInstance("MD5", "BC");
      digest2.update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
      bmd52 = digest2.digest();
      hex2 = FilesystemBasedDigest.getHex(bmd52);
      start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        digest2 = MessageDigest.getInstance("MD5", "BC");
        digest2.update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
        bmd52 = digest2.digest();
        hex2 = FilesystemBasedDigest.getHex(bmd52);
        assertTrue("Native2 Hex Not Equals",
                   FilesystemBasedDigest.digestEquals(hex2, bmd52));
      }
      end = System.currentTimeMillis();
      System.out.println("Buf Algo: MD5 Native2 Time: " + (end - start));
      digest =
          new FilesystemBasedDigest(DigestAlgo.SHA512);
      digest.Update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
      bmd5 = digest.Final();
      hex = FilesystemBasedDigest.getHex(bmd5);
      start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        FilesystemBasedDigest.setUseFastMd5(false);
        digest =
            new FilesystemBasedDigest(DigestAlgo.SHA512);
        digest.Update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
        bmd5 = digest.Final();
        hex = FilesystemBasedDigest.getHex(bmd5);
        assertTrue(DigestAlgo.SHA512 + " Hex Not Equals",
                   FilesystemBasedDigest.digestEquals(hex, bmd5));
      }
      end = System.currentTimeMillis();
      System.out.println(
          "Byte Algo: " + DigestAlgo.SHA512 + " Time: " + (end - start));
      digest2 = MessageDigest.getInstance("SHA-512", "SUN");
      digest2.update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
      bmd52 = digest2.digest();
      hex2 = FilesystemBasedDigest.getHex(bmd52);
      start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        digest2 = MessageDigest.getInstance("SHA-512", "SUN");
        digest2.update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
        bmd52 = digest2.digest();
        hex2 = FilesystemBasedDigest.getHex(bmd52);
        assertTrue("Native1 Hex Not Equals",
                   FilesystemBasedDigest.digestEquals(hex2, bmd52));
      }
      end = System.currentTimeMillis();
      System.out.println("Buf Algo: Native1 Time: " + (end - start));
      digest2 = MessageDigest.getInstance("SHA-512", "SUN");
      System.out.println(digest2.getProvider().getName());
      digest2 = MessageDigest.getInstance("SHA-512", "BC");
      digest2.update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
      bmd52 = digest2.digest();
      hex2 = FilesystemBasedDigest.getHex(bmd52);
      start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        digest2 = MessageDigest.getInstance("SHA-512", "BC");
        digest2.update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
        bmd52 = digest2.digest();
        hex2 = FilesystemBasedDigest.getHex(bmd52);
        assertTrue("Native2 Hex Not Equals",
                   FilesystemBasedDigest.digestEquals(hex2, bmd52));
      }
      end = System.currentTimeMillis();
      System.out.println("Buf Algo: Native2 Time: " + (end - start));
      digest2 = MessageDigest.getInstance("SHA-512", "BC");
      System.out.println(digest2.getProvider().getName());
      digest2 = MessageDigest.getInstance("SHA-512");
      digest2.update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
      bmd52 = digest2.digest();
      hex2 = FilesystemBasedDigest.getHex(bmd52);
      start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        digest2 = MessageDigest.getInstance("SHA-512");
        digest2.update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
        bmd52 = digest2.digest();
        hex2 = FilesystemBasedDigest.getHex(bmd52);
        assertTrue("Native3 Hex Not Equals",
                   FilesystemBasedDigest.digestEquals(hex2, bmd52));
      }
      end = System.currentTimeMillis();
      System.out.println("Buf Algo: Native3 Time: " + (end - start));
      digest2 = MessageDigest.getInstance("SHA-512");
      System.out.println(digest2.getProvider().getName());
    } catch (final NoSuchAlgorithmException e) {
      fail(e.getMessage());
    } catch (final IOException e) {
      fail(e.getMessage());
    } catch (NoSuchProviderException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetHashFileDigestAlgo() throws IOException {
    final File file = File.createTempFile("testHash", ".txt", new File("/tmp"));
    final FileWriter fileWriterBig = new FileWriter(file);
    for (int i = 0; i < 256; i++) {
      fileWriterBig.write(TESTPHRASE);
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    try {
      FilesystemBasedDigest.setUseFastMd5(false);
      for (final DigestAlgo algo : DigestAlgo.values()) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 2; i++) {
          byte[] bmd5 = FilesystemBasedDigest.getHash(file, false, algo);
        }
        long end = System.currentTimeMillis();
        System.out.println("NoNio Algo: " + algo + " Time: " + (end - start));
        start = System.currentTimeMillis();
        for (int i = 0; i < 2; i++) {
          byte[] bmd52 = FilesystemBasedDigest.getHash(file, true, algo);
        }
        end = System.currentTimeMillis();
        System.out.println("Nio Algo: " + algo + " Time: " + (end - start));
        start = System.currentTimeMillis();
        for (int i = 0; i < 2; i++) {
          FileInputStream stream = new FileInputStream(file);
          byte[] bmd53 = FilesystemBasedDigest.getHash(stream, algo);
          stream.close();
        }
        end = System.currentTimeMillis();
        System.out.println("Stream Algo: " + algo + " Time: " + (end - start));
      }
    } catch (final IOException e) {
      fail(e.getMessage());
    } finally {
      file.delete();
    }
  }
}
