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

/**
 * Classes implementing digests support (MD2, MD5, SHA1, SHA-256/384/512, CRC32,
 * ADLER32)<br>
 * <br>
 * <p>
 * <p>
 * MD5 can be implemented through Fast MD5 implementation, but can be reverted
 * to JVM native digest also.<br>
 * <br>
 * <p>
 * Originally a C library support was optional but it is decided to stay full
 * Java.
 * <p>
 * Recommendation for best performance would be to use FastMD5 if possible for
 * MD5, but for portability to use
 * native JVM digest implementations (so no FastMD5 at all).
 * <p>
 * In order to let you make some choice, here is a short performance reports:
 * (done using java -server option)
 * <ul>
 * <li>File based only: (15 MB file used)</li>
 * <ul>
 * <li>FastMD5 in C is almost the fastest (+20%), while FastMD5 in Java is the
 * slowest (-20%) and JVM version
 * is in the middle.</li>
 * <li>If ADLER32 is the referenced time ADLER32=1, CRC32=2.5, MD5=4, SHA1=7,
 * SHA256=11, SHA512=25</li>
 * </ul>
 * <li>Buffer based only: (256 MB buffer used)</li>
 * <ul>
 * <li>JVM version is the fastest (+20%), while FastMD5 in C or in Java are the
 * same (-20% than JVM).</li>
 * <li>If ADLER32 is the referenced time ADLER32=1, CRC32=2.5, MD5=4, SHA1=8,
 * SHA256=13, SHA384=29,
 * SHA512=31</li>
 * </ul>
 * </ul>
 * <br>
 * <br>
 * <p>
 * For information, sphlib (http://www.saphir2.com/sphlib/) were compared to
 * native JVM implementation for all
 * those digests, and it appears on small benchmarks (speed from sphlib) that
 * native JVM implementation
 * performs better and that on MD5, FastMD5 performs better than sphlib but less
 * than native JVM.
 * <p>
 * Therefore it is recommended to use native JVM MD5 support if possible.
 */
package org.waarp.common.digest;
