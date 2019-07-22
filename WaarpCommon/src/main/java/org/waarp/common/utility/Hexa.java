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

/**
 * @author "Frederic Bregier"
 *
 */
public class Hexa {
    /**
     * HEX_CHARS
     */
    private static final char[] HEX_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', };

    public static final byte asByte(char a, char b) {
        if (a >= HEX_CHARS[10]) {
            a -= HEX_CHARS[10] - 10;
        } else {
            a -= HEX_CHARS[0];
        }
        if (b >= HEX_CHARS[10]) {
            b -= HEX_CHARS[10] - 10;
        } else {
            b -= HEX_CHARS[0];
        }
        return (byte) ((a << 4) + b);
    }

    public static final byte[] fromHex(final char[] hex) {
        final int size = hex.length / 2;
        final byte[] bytes = new byte[size];
        for (int i = 0, j = 0; i < size;) {
            bytes[i++] = asByte(hex[j++], hex[j++]);
        }
        return bytes;
    }

    public static final byte[] fromHex(final String hex) {
        final char[] chars = hex.toCharArray();
        final int size = chars.length / 2;
        final byte[] bytes = new byte[size];
        for (int i = 0, j = 0; i < size;) {
            bytes[i++] = asByte(chars[j++], chars[j++]);
        }
        return bytes;
    }

    public static final char getHighHex(final byte value) {
        return HEX_CHARS[(value & 0xF0) >> 4];
    }

    public static final char getLowHex(final byte value) {
        return HEX_CHARS[(value & 0x0F)];
    }

    public static final String toHex(final byte[] bytes) {
        final int size = bytes.length;
        final int b16size = size * 2;
        final char[] id = new char[b16size];

        // split each byte into 4 bit numbers and map to hex characters
        for (int i = 0, j = 0; i < size; i++) {
            id[j++] = HEX_CHARS[(bytes[i] & 0xF0) >> 4];
            id[j++] = HEX_CHARS[(bytes[i] & 0x0F)];
        }
        return new String(id);
    }
}
