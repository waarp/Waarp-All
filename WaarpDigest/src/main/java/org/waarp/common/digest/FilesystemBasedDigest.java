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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static org.waarp.common.digest.WaarpBC.*;

/**
 * Class implementing digest like MD5, SHA1. MD5 is based on the Fast MD5
 * implementation, without C library
 * support, but can be revert to JVM native digest.<br>
 * <br>
 * <p>
 * Some performance reports: (done using java -server option)
 * <ul>
 * <li>File based only:</li>
 * <ul>
 * <li>FastMD5 in C is almost the fastest (+20%), while FastMD5 in Java is the
 * slowest (-20%) and JVM version
 * is in the middle.</li>
 * <li>If ADLER32 is the referenced time ADLER32=1, CRC32=2.5, MD5=4, SHA1=7,
 * SHA256=11, SHA512=25</li>
 * </ul>
 * <li>Buffer based only:</li>
 * <ul>
 * <li>JVM version is the fastest (+20%), while FastMD5 in C or in Java are the
 * same (-20% than JVM).</li>
 * <li>If ADLER32 is the referenced time ADLER32=1, CRC32=2.5, MD5=4, SHA1=8,
 * SHA256=13, SHA384=29,
 * SHA512=31</li>
 * </ul>
 * </ul>
 */
public class FilesystemBasedDigest {

  private static final String ALGORITHM_NOT_SUPPORTED_BY_THIS_JVM =
      " Algorithm not supported by this JVM";

  /**
   * Format used for Files
   */
  public static final Charset UTF8 = Charset.forName("UTF-8");
  private static final byte[] EMPTY = {};

  static {
    initializedTlsContext();
  }

  MD5 md5;
  Checksum checksum;
  MessageDigest digest;
  DigestAlgo algo;

  /**
   * Constructor of an independent Digest
   *
   * @param algo
   *
   * @throws NoSuchAlgorithmException
   */
  public FilesystemBasedDigest(final DigestAlgo algo)
      throws NoSuchAlgorithmException {
    initialize(algo);
  }

  /**
   * (Re)Initialize the digest
   *
   * @throws NoSuchAlgorithmException
   */
  public void initialize() throws NoSuchAlgorithmException {
    if (algo == DigestAlgo.MD5 && isUseFastMd5()) {
      md5 = new MD5();
      return;
    }
    switch (algo) {
      case ADLER32:
        checksum = new Adler32();
        return;
      case CRC32:
        checksum = new CRC32();
        return;
      case MD5:
      case MD2:
      case SHA1:
      case SHA256:
      case SHA384:
      case SHA512:
        final String algoname = algo.algoName;
        try {
          digest = MessageDigest.getInstance(algoname);
        } catch (final NoSuchAlgorithmException e) {
          throw new NoSuchAlgorithmException(
              algo + ALGORITHM_NOT_SUPPORTED_BY_THIS_JVM, e);
        }
        return;
      default:
        throw new NoSuchAlgorithmException(
            algo.algoName + ALGORITHM_NOT_SUPPORTED_BY_THIS_JVM);
    }
  }

  /**
   * (Re)Initialize the digest
   *
   * @param algo
   *
   * @throws NoSuchAlgorithmException
   */
  public void initialize(final DigestAlgo algo)
      throws NoSuchAlgorithmException {
    this.algo = algo;
    initialize();
  }

  public DigestAlgo getAlgo() {
    return algo;
  }

  /**
   * Update the digest with new bytes
   *
   * @param bytes
   * @param offset
   * @param length
   */
  public void Update(final byte[] bytes, final int offset, final int length) {
    if (md5 != null) {
      md5.Update(bytes, offset, length);
      return;
    }
    switch (algo) {
      case ADLER32:
      case CRC32:
        checksum.update(bytes, offset, length);
        return;
      case MD5:
      case MD2:
      case SHA1:
      case SHA256:
      case SHA384:
      case SHA512:
        digest.update(bytes, offset, length);
    }
  }

  private byte[] reusableBytes;

  public final byte[] getBytes(final ByteBuf buffer) {
    byte[] bytes;
    final int length = buffer.readableBytes();
    if (buffer.hasArray()) {
      bytes = buffer.array();
    } else {
      if (reusableBytes == null || reusableBytes.length < length) {
        reusableBytes = new byte[length];
      }
      bytes = reusableBytes;
      buffer.getBytes(buffer.readerIndex(), bytes, 0, length);
    }
    return bytes;
  }

  public final int getOffset(final ByteBuf buffer) {
    if (buffer.hasArray()) {
      return buffer.arrayOffset();
    }
    return 0;
  }

  /**
   * Update the digest with new buffer
   */
  public void Update(final ByteBuf buffer) {
    byte[] bytes = getBytes(buffer);
    int start = getOffset(buffer);
    final int length = buffer.readableBytes();
    Update(bytes, start, length);
  }

  /**
   * @return the digest in array of bytes
   */
  public byte[] Final() {
    if (md5 != null) {
      return md5.Final();
    }
    switch (algo) {
      case ADLER32:
      case CRC32:
        return Long.toOctalString(checksum.getValue()).getBytes(UTF8);
      case MD5:
      case MD2:
      case SHA1:
      case SHA256:
      case SHA384:
      case SHA512:
        return digest.digest();
    }
    return EMPTY;
  }

  /**
   * Initialize the MD5 support
   *
   * @param mustUseFastMd5 True will use FastMD5 support, False will
   *     use JVM
   *     native MD5
   *
   * @return True if the native library is loaded
   */
  public static boolean initializeMd5(final boolean mustUseFastMd5) {
    setUseFastMd5(mustUseFastMd5);
    return true;
  }

  /**
   * @return the useFastMd5
   */
  public static boolean isUseFastMd5() {
    return useFastMd5;
  }

  /**
   * @param useFastMd5 the useFastMd5 to set
   */
  public static void setUseFastMd5(final boolean useFastMd5) {
    FilesystemBasedDigest.useFastMd5 = useFastMd5;
  }

  /**
   * All Algo that Digest Class could handle
   */
  public enum DigestAlgo {
    CRC32("CRC32", 11), ADLER32("ADLER32", 9), MD5("MD5", 16), MD2("MD2", 16),
    SHA1("SHA-1", 20), SHA256("SHA-256", 32), SHA384("SHA-384", 48),
    SHA512("SHA-512", 64);

    public final String algoName;
    public final int byteSize;

    /**
     * @return the length in bytes of one Digest
     */
    public int getByteSize() {
      return byteSize;
    }

    /**
     * @return the length in Hex form of one Digest
     */
    public int getHexSize() {
      return byteSize * 2;
    }

    DigestAlgo(final String algoName, final int byteSize) {
      this.algoName = algoName;
      this.byteSize = byteSize;
    }

    public static DigestAlgo getFromName(final String name) {
      try {
        return valueOf(name);
      } catch (IllegalArgumentException ignore) {//NOSONAR
        // ignore
      }
      if ("CRC32".equalsIgnoreCase(name)) {
        return CRC32;
      } else if ("ADLER32".equalsIgnoreCase(name)) {
        return ADLER32;
      } else if ("MD5".equalsIgnoreCase(name)) {
        return MD5;
      } else if ("MD2".equalsIgnoreCase(name)) {
        return MD2;
      } else if ("SHA-1".equalsIgnoreCase(name)) {
        return SHA1;
      } else if ("SHA-256".equalsIgnoreCase(name)) {
        return SHA256;
      } else if ("SHA-384".equalsIgnoreCase(name)) {
        return SHA384;
      } else if ("SHA-512".equalsIgnoreCase(name)) {
        return SHA512;
      } else {
        throw new IllegalArgumentException("Digest Algo not found");
      }
    }
  }

  /**
   * Should a file MD5 be computed using FastMD5
   */
  private static boolean useFastMd5;

  /**
   * @param dig1
   * @param dig2
   *
   * @return True if the two digest are equals
   */
  public static final boolean digestEquals(final byte[] dig1,
                                           final byte[] dig2) {
    return MessageDigest.isEqual(dig1, dig2);
  }

  /**
   * @param dig1
   * @param dig2
   *
   * @return True if the two digest are equals
   */
  public static final boolean digestEquals(final String dig1,
                                           final byte[] dig2) {
    final byte[] bdig1 = getFromHex(dig1);
    return MessageDigest.isEqual(bdig1, dig2);
  }

  /**
   * get the byte array of the MD5 for the given FileInterface using Nio
   * access
   *
   * @param f
   *
   * @return the byte array representing the MD5
   *
   * @throws IOException
   */
  public static byte[] getHashMd5Nio(final File f) throws IOException {
    if (isUseFastMd5()) {
      return MD5.getHashNio(f);
    }
    return getHash(f, true, DigestAlgo.MD5);
  }

  /**
   * get the byte array of the MD5 for the given FileInterface using standard
   * access
   *
   * @param f
   *
   * @return the byte array representing the MD5
   *
   * @throws IOException
   */
  public static byte[] getHashMd5(final File f) throws IOException {
    if (isUseFastMd5()) {
      return MD5.getHash(f);
    }
    return getHash(f, false, DigestAlgo.MD5);
  }

  /**
   * get the byte array of the SHA-1 for the given FileInterface using Nio
   * access
   *
   * @param f
   *
   * @return the byte array representing the SHA-1
   *
   * @throws IOException
   */
  public static byte[] getHashSha1Nio(final File f) throws IOException {
    return getHash(f, true, DigestAlgo.SHA1);
  }

  /**
   * get the byte array of the SHA-1 for the given FileInterface using
   * standard
   * access
   *
   * @param f
   *
   * @return the byte array representing the SHA-1
   *
   * @throws IOException
   */
  public static byte[] getHashSha1(final File f) throws IOException {
    return getHash(f, false, DigestAlgo.SHA1);
  }

  /**
   * Internal function for No NIO InputStream support
   *
   * @param in will be closed at the end of this call
   * @param algo
   * @param buf
   *
   * @return the digest
   *
   * @throws IOException
   */
  private static byte[] getHashNoNio(final InputStream in,
                                     final DigestAlgo algo, final byte[] buf)
      throws IOException {
    // Not NIO
    Checksum checksum = null;
    int size;
    try {
      switch (algo) {
        case ADLER32:
          checksum = new Adler32();
          return getBytesCrc(in, buf, checksum);
        case CRC32:
          return getBytesCrc(in, buf, checksum);
        case MD5:
        case MD2:
        case SHA1:
        case SHA256:
        case SHA384:
        case SHA512:
          final String algoname = algo.algoName;
          MessageDigest digest;
          try {
            digest = MessageDigest.getInstance(algoname);
          } catch (final NoSuchAlgorithmException e) {
            throw new IOException(algo + ALGORITHM_NOT_SUPPORTED_BY_THIS_JVM,
                                  e);
          }
          while ((size = in.read(buf)) >= 0) {
            digest.update(buf, 0, size);
          }
          return digest.digest();
        default:
          throw new IOException(
              algo.algoName + ALGORITHM_NOT_SUPPORTED_BY_THIS_JVM);
      }
    } finally {
      in.close();
    }
  }

  private static byte[] getBytesCrc(final InputStream in, final byte[] buf,
                                    Checksum checksum) throws IOException {
    int size;
    if (checksum == null) { // not ADLER32
      checksum = new CRC32();
    }
    while ((size = in.read(buf)) >= 0) {
      checksum.update(buf, 0, size);
    }
    return Long.toOctalString(checksum.getValue()).getBytes(UTF8);
  }

  /**
   * Get the Digest for the file using the specified algorithm using access
   * through NIO or not
   *
   * @param f
   * @param nio
   * @param algo
   *
   * @return the digest
   *
   * @throws IOException
   */
  @SuppressWarnings("resource")
  public static byte[] getHash(final File f, final boolean nio,
                               final DigestAlgo algo) throws IOException {
    if (!f.exists()) {
      throw new FileNotFoundException(f.toString());
    }
    if (algo == DigestAlgo.MD5 && isUseFastMd5()) {
      if (nio) {
        return MD5.getHashNio(f);
      } else {
        return MD5.getHash(f);
      }
    }
    FileInputStream in = null;
    try {
      long bufSize = f.length();
      if (bufSize < 512) {
        bufSize = 512;
      }
      if (bufSize > 65536) {
        bufSize = 65536;
      }
      byte[] buf = new byte[(int) bufSize];
      in = new FileInputStream(f);
      if (nio) { // NIO
        final FileChannel fileChannel = in.getChannel();
        try {
          ByteBuffer bb = ByteBuffer.wrap(buf);
          Checksum checksum = null;
          int size;
          switch (algo) {
            case ADLER32:
              checksum = new Adler32();
              buf = getBytesCrcFileChannel(buf, fileChannel, bb, checksum);
              break;
            case CRC32:
              buf = getBytesCrcFileChannel(buf, fileChannel, bb, checksum);
              break;
            case MD5:
            case MD2:
            case SHA1:
            case SHA256:
            case SHA384:
            case SHA512:
              final String algoname = algo.algoName;
              MessageDigest digest;
              try {
                digest = MessageDigest.getInstance(algoname);
              } catch (final NoSuchAlgorithmException e) {
                throw new IOException(
                    algo + ALGORITHM_NOT_SUPPORTED_BY_THIS_JVM, e);
              }
              while ((size = fileChannel.read(bb)) >= 0) {
                digest.update(buf, 0, size);
                bb.clear();
              }
              buf = digest.digest();
              break;
            default:
              throw new IOException(
                  algo.algoName + ALGORITHM_NOT_SUPPORTED_BY_THIS_JVM);
          }
        } finally {
          fileChannel.close();
        }
      } else { // Not NIO
        return getHashNoNio(in, algo, buf);
      }
      return buf;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (final Exception ignored) {
          // nothing
        }
      }
    }
  }

  private static byte[] getBytesCrcFileChannel(final byte[] buf,
                                               final FileChannel fileChannel,
                                               final ByteBuffer bb,
                                               Checksum checksum)
      throws IOException {
    int size;
    if (checksum == null) { // Not ADLER32
      checksum = new CRC32();
    }
    while ((size = fileChannel.read(bb)) >= 0) {
      checksum.update(buf, 0, size);
      bb.clear();
    }
    return Long.toOctalString(checksum.getValue()).getBytes(UTF8);
  }

  /**
   * Get the Digest for the file using the specified algorithm using access
   * through NIO or not
   *
   * @param stream will be closed at the end of this call
   * @param algo
   *
   * @return the digest
   *
   * @throws IOException
   */
  public static byte[] getHash(final InputStream stream, final DigestAlgo algo)
      throws IOException {
    if (stream == null) {
      throw new FileNotFoundException();
    }
    if (algo == DigestAlgo.MD5 && isUseFastMd5()) {
      return MD5.getHash(stream);
    }
    try {
      final int buf_size = 65536;
      byte[] buf = new byte[buf_size];
      // Not NIO
      return getHashNoNio(stream, algo, buf);
    } catch (final IOException e) {
      try {
        stream.close();
      } catch (final Exception ignored) {
        // nothing
      }
      throw e;
    }
  }

  /**
   * Get hash with given {@link ByteBuf} (from Netty)
   *
   * @param buffer this buffer will not be changed
   * @param algo
   *
   * @return the hash
   *
   * @throws IOException
   */
  public static byte[] getHash(final ByteBuf buffer, final DigestAlgo algo)
      throws IOException {
    Checksum checksum = null;
    byte[] bytes;
    int start = 0;
    final int length = buffer.readableBytes();
    if (buffer.hasArray()) {
      start = buffer.arrayOffset();
      bytes = buffer.array();
    } else {
      bytes = new byte[length];
      buffer.getBytes(buffer.readerIndex(), bytes);
    }
    switch (algo) {
      case ADLER32:
        checksum = new Adler32();
        return getBytesCrcByteBuf(checksum, bytes, start, length);
      case CRC32:
        return getBytesCrcByteBuf(checksum, bytes, start, length);
      case MD5:
        if (isUseFastMd5()) {
          MD5 md5 = new MD5();
          md5.Update(bytes, start, length);
          bytes = md5.Final();
          return bytes;
        }
        return getBytesVarious(algo, bytes, start, length);
      case MD2:
      case SHA1:
      case SHA256:
      case SHA384:
      case SHA512:
        return getBytesVarious(algo, bytes, start, length);
      default:
        throw new IOException(
            algo.algoName + ALGORITHM_NOT_SUPPORTED_BY_THIS_JVM);
    }
  }

  private static byte[] getBytesVarious(final DigestAlgo algo,
                                        final byte[] bytes, final int start,
                                        final int length) throws IOException {
    final String algoname = algo.algoName;
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(algoname);
    } catch (final NoSuchAlgorithmException e) {
      throw new IOException(algoname + ALGORITHM_NOT_SUPPORTED_BY_THIS_JVM, e);
    }
    digest.update(bytes, start, length);
    return digest.digest();
  }

  private static byte[] getBytesCrcByteBuf(Checksum checksum,
                                           final byte[] bytes, final int start,
                                           final int length) {
    if (checksum == null) { // not ADLER32
      checksum = new CRC32();
    }
    checksum.update(bytes, start, length);
    return Long.toOctalString(checksum.getValue()).getBytes(UTF8);
  }

  /**
   * Get hash with given {@link ByteBuf} (from Netty)
   *
   * @param buffer ByteBuf to use to get the hash and this buffer will
   *     not
   *     be changed
   *
   * @return the hash
   */
  public static byte[] getHashMd5(final ByteBuf buffer) {
    try {
      return getHash(buffer, DigestAlgo.MD5);
    } catch (final IOException e) {
      MD5 md5 = new MD5();
      md5.Update(buffer);
      return md5.Final();
    }
  }

  /**
   * Internal representation of Hexadecimal Code
   */
  private static final char[] HEX_CHARS = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
      'f',
  };

  /**
   * Get the hexadecimal representation as a String of the array of bytes
   *
   * @param hash
   *
   * @return the hexadecimal representation as a String of the array of bytes
   */
  public static final String getHex(final byte[] hash) {
    final char[] buf = new char[hash.length * 2];
    for (int i = 0, x = 0; i < hash.length; i++) {
      buf[x++] = HEX_CHARS[hash[i] >>> 4 & 0xf];
      buf[x++] = HEX_CHARS[hash[i] & 0xf];
    }
    return new String(buf);
  }

  /**
   * Get the array of bytes representation of the hexadecimal String
   *
   * @param hex
   *
   * @return the array of bytes representation of the hexadecimal String
   */
  public static final byte[] getFromHex(final String hex) {
    final byte[] from = hex.getBytes(UTF8);
    final byte[] hash = new byte[from.length / 2];
    for (int i = 0, x = 0; i < hash.length; i++) {
      byte code1 = from[x++];
      byte code2 = from[x++];
      if (code1 >= HEX_CHARS[10]) {
        code1 -= HEX_CHARS[10] - 10;
      } else {
        code1 -= HEX_CHARS[0];
      }
      if (code2 >= HEX_CHARS[10]) {
        code2 -= HEX_CHARS[10] - 10;
      } else {
        code2 -= HEX_CHARS[0];
      }
      hash[i] = (byte) ((code1 << 4) + (code2 & 0xFF));
    }
    return hash;
  }

  private static final byte[] salt =
      { 'G', 'o', 'l', 'd', 'e', 'n', 'G', 'a', 't', 'e' };

  /**
   * Crypt a password
   *
   * @param pwd to crypt
   *
   * @return the crypted password
   *
   * @throws IOException
   */
  public static final String passwdCrypt(final String pwd) {
    if (isUseFastMd5()) {
      return MD5.passwdCrypt(pwd);
    }
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(DigestAlgo.MD5.algoName);
    } catch (final NoSuchAlgorithmException e) {
      return MD5.passwdCrypt(pwd);
    }
    final byte[] bpwd = pwd.getBytes(UTF8);
    for (int i = 0; i < 16; i++) {
      digest.update(bpwd, 0, bpwd.length);
      digest.update(salt, 0, salt.length);
    }
    return getHex(digest.digest());
  }

  /**
   * Crypt a password
   *
   * @param pwd to crypt
   *
   * @return the crypted password
   *
   * @throws IOException
   */
  public static final byte[] passwdCrypt(final byte[] pwd) {
    if (isUseFastMd5()) {
      return MD5.passwdCrypt(pwd);
    }
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(DigestAlgo.MD5.algoName);
    } catch (final NoSuchAlgorithmException e) {
      return MD5.passwdCrypt(pwd);
    }
    for (int i = 0; i < 16; i++) {
      digest.update(pwd, 0, pwd.length);
      digest.update(salt, 0, salt.length);
    }
    return digest.digest();
  }

  /**
   * @param pwd
   * @param cryptPwd
   *
   * @return True if the pwd is comparable with the cryptPwd
   *
   * @throws IOException
   */
  public static final boolean equalPasswd(final String pwd,
                                          final String cryptPwd) {
    String asHex;
    asHex = passwdCrypt(pwd);
    return cryptPwd.equals(asHex);
  }

  /**
   * @param pwd
   * @param cryptPwd
   *
   * @return True if the pwd is comparable with the cryptPwd
   */
  public static final boolean equalPasswd(final byte[] pwd,
                                          final byte[] cryptPwd) {
    byte[] bytes;
    bytes = passwdCrypt(pwd);
    return Arrays.equals(cryptPwd, bytes);
  }

}
