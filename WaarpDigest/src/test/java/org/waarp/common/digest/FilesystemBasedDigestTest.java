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
import org.junit.Test;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

public class FilesystemBasedDigestTest {
  private static final String TESTPHRASE = "This is a phrase to test";
  private static final byte[] TESTPHRASEBYTES = TESTPHRASE.getBytes();

  @Test
  public void testGetHashByteBufDigestAlgo() {

    try {
      for (final DigestAlgo algo : DigestAlgo.values()) {
        final long start = System.currentTimeMillis();
        byte[] bmd5 = null;
        for (int i = 0; i < 10000; i++) {
          FilesystemBasedDigest digest = new FilesystemBasedDigest(algo);
          digest.Update(TESTPHRASEBYTES, 0, TESTPHRASEBYTES.length);
          bmd5 = digest.Final();
          String hex = FilesystemBasedDigest.getHex(bmd5);
          assertTrue(algo + " Hex Not Equals",
                     FilesystemBasedDigest.digestEquals(hex, bmd5));
          digest = new FilesystemBasedDigest(algo);
          ByteBuf buf = Unpooled.wrappedBuffer(TESTPHRASEBYTES);
          digest.Update(buf);
          bmd5 = digest.Final();
          hex = FilesystemBasedDigest.getHex(bmd5);
          assertTrue(algo + " Hex Not Equals",
                     FilesystemBasedDigest.digestEquals(hex, bmd5));
          buf = Unpooled.wrappedBuffer(TESTPHRASEBYTES);
          final byte[] bmd53 = FilesystemBasedDigest.getHash(buf, algo);
          buf.release();
          final String hex3 = FilesystemBasedDigest.getHex(bmd53);
          assertTrue(algo + " Hex Not Equals",
                     FilesystemBasedDigest.digestEquals(hex3, bmd53));
          assertTrue(algo + " Through ByteBuf vs Direct Not Equals",
                     FilesystemBasedDigest.digestEquals(bmd53, bmd5));
          assertTrue(algo + " FromHex Not Equals",
                     FilesystemBasedDigest.digestEquals(bmd53,
                                                        FilesystemBasedDigest.getFromHex(
                                                            hex3)));
        }
        final long end = System.currentTimeMillis();
        System.out.println(
            "Algo: " + algo + " KeyLength: " + bmd5.length + " Time: " +
            (end - start));
      }
      final ByteBuf buf = Unpooled.wrappedBuffer(TESTPHRASEBYTES);
      final byte[] bmd5 = FilesystemBasedDigest.getHashMd5(buf);
      final byte[] bmd52 = FilesystemBasedDigest.getHash(buf, DigestAlgo.MD5);
      assertTrue(DigestAlgo.MD5 + " Hex Not Equals",
                 FilesystemBasedDigest.digestEquals(bmd52, bmd5));
    } catch (final NoSuchAlgorithmException e) {
      fail(e.getMessage());
    } catch (final IOException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetHashFileDigestAlgo() throws IOException {
    final File file = File.createTempFile("testHash", ".txt", new File("/tmp"));
    final FileWriter fileWriterBig = new FileWriter(file);
    fileWriterBig.write(TESTPHRASE);
    fileWriterBig.flush();
    fileWriterBig.close();
    try {
      Thread.sleep(50);
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
    while (!file.isFile() && !file.canRead() && file.length() == 0) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
    }
    try {
      for (final DigestAlgo algo : DigestAlgo.values()) {
        final long start = System.currentTimeMillis();
        byte[] bmd5 = null;
        for (int i = 0; i < 1000; i++) {
          bmd5 = FilesystemBasedDigest.getHash(file, false, algo);
          byte[] bmd52 = FilesystemBasedDigest.getHash(file, true, algo);
          FileInputStream stream = new FileInputStream(file);
          byte[] bmd53 = FilesystemBasedDigest.getHash(stream, algo);
          stream.close();
          assertTrue(algo + " Hex Not Equals",
                     FilesystemBasedDigest.digestEquals(bmd5, bmd52));
          assertTrue(algo + " Hex Not Equals",
                     FilesystemBasedDigest.digestEquals(bmd5, bmd53));
        }
        final long end = System.currentTimeMillis();
        System.out.println(
            "Algo: " + algo + " KeyLength: " + bmd5.length + " Time: " +
            (end - start));
      }
    } catch (final IOException e) {
      fail(e.getMessage());
    } finally {
      file.delete();
    }
  }

  @Test
  public void testPasswdCryptString() {
    final long start = System.currentTimeMillis();
    byte[] bmd5 = null;
    for (int i = 0; i < 100000; i++) {
      final String crypt = FilesystemBasedDigest.passwdCrypt(TESTPHRASE);
      final byte[] bcrypt = FilesystemBasedDigest.passwdCrypt(TESTPHRASEBYTES);
      bmd5 = bcrypt;
      assertTrue("Password Hex Not Equals",
                 FilesystemBasedDigest.digestEquals(crypt, bcrypt));
      assertTrue("Password Not Equals",
                 FilesystemBasedDigest.equalPasswd(TESTPHRASEBYTES, bcrypt));
      assertTrue("Password Not Equals",
                 FilesystemBasedDigest.equalPasswd(TESTPHRASE, crypt));
    }
    final long end = System.currentTimeMillis();
    System.out.println(
        "Algo: CRYPT KeyLength: " + bmd5.length + " Time: " + (end - start));
  }

  @Test
  public void testSpecificMD5() throws IOException {
    assertEquals(DigestAlgo.MD5.getByteSize() * 2, DigestAlgo.MD5.getHexSize());

    final File file = File.createTempFile("testHash", ".txt", new File("/tmp"));
    final FileWriter fileWriterBig = new FileWriter(file);
    fileWriterBig.write(TESTPHRASE);
    fileWriterBig.flush();
    fileWriterBig.close();
    try {
      Thread.sleep(50);
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
    while (!file.isFile() && !file.canRead() && file.length() == 0) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
    }
    try {
      final DigestAlgo algo = DigestAlgo.MD5;
      final long start = System.currentTimeMillis();
      byte[] bmd5 = null;
      for (int i = 0; i < 1000; i++) {
        byte[] bmd51 = FilesystemBasedDigest.getHashMd5Nio(file);
        bmd5 = bmd51;
        byte[] bmd52 = FilesystemBasedDigest.getHashMd5(file);

        assertTrue(algo + " Hex Not Equals",
                   FilesystemBasedDigest.digestEquals(bmd51, bmd52));
      }
      final long end = System.currentTimeMillis();
      System.out.println(
          "Algo: " + algo + " KeyLength: " + bmd5.length + " Time: " +
          (end - start));
    } catch (final IOException e) {
      fail(e.getMessage());
    } finally {
      file.delete();
    }
  }
}
