/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.crypto.encoding;

/// Bech32 (BIP-173) and Bech32m (BIP-350) encoding, bit regrouping and the
/// SegWit address helpers built on them.
@com.codename1.impl.SharedWithBackend
public final class Bech32 {

    /// Checksum variant constant for original Bech32 (BIP-173).
    public static final int BECH32 = 1;

    /// Checksum variant constant for Bech32m (BIP-350).
    public static final int BECH32M = 2;

    private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
    private static final int[] REV = new int[128];
    private static final int BECH32_CONST = 1;
    private static final int BECH32M_CONST = 0x2bc830a3;

    static {
        for (int i = 0; i < REV.length; i++) {
            REV[i] = -1;
        }
        for (int i = 0; i < CHARSET.length(); i++) {
            REV[CHARSET.charAt(i)] = i;
        }
    }

    private Bech32() {
    }

    /// The result of [#decode(String)].
    public static final class Decoded {
        private final String hrp;
        private final byte[] data;
        private final int variant;

        Decoded(String hrp, byte[] data, int variant) {
            this.hrp = hrp;
            this.data = data;
            this.variant = variant;
        }

        /// Returns the human readable part, lowercase.
        public String getHrp() {
            return hrp;
        }

        /// Returns the 5-bit data values, without the checksum.
        public byte[] getData() {
            byte[] c = new byte[data.length];
            System.arraycopy(data, 0, c, 0, data.length);
            return c;
        }

        /// Returns [#BECH32] or [#BECH32M].
        public int getVariant() {
            return variant;
        }
    }

    /// The result of [#decodeSegwitAddress(String, String)].
    public static final class SegwitAddress {
        private final int version;
        private final byte[] program;

        SegwitAddress(int version, byte[] program) {
            this.version = version;
            this.program = program;
        }

        /// Returns the witness version, 0 to 16.
        public int getVersion() {
            return version;
        }

        /// Returns the witness program bytes.
        public byte[] getProgram() {
            byte[] c = new byte[program.length];
            System.arraycopy(program, 0, c, 0, program.length);
            return c;
        }
    }

    private static int polymod(byte[] values) {
        int chk = 1;
        int[] gen = {0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3};
        for (byte value : values) {
            int top = chk >>> 25;
            chk = ((chk & 0x1ffffff) << 5) ^ (value & 0xff);
            for (int j = 0; j < 5; j++) {
                if (((top >>> j) & 1) != 0) {
                    chk ^= gen[j];
                }
            }
        }
        return chk;
    }

    private static byte[] expandHrp(String hrp) {
        int n = hrp.length();
        byte[] out = new byte[n * 2 + 1];
        for (int i = 0; i < n; i++) {
            char c = hrp.charAt(i);
            out[i] = (byte) (c >>> 5);
            out[i + n + 1] = (byte) (c & 31);
        }
        return out;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static int constant(int variant) {
        if (variant == BECH32) {
            return BECH32_CONST;
        }
        if (variant == BECH32M) {
            return BECH32M_CONST;
        }
        throw new IllegalArgumentException("Unknown Bech32 variant");
    }

    private static char fold(char c) {
        return (c >= 'A' && c <= 'Z') ? (char) (c + 32) : c;
    }

    /// Encodes an HRP and 5-bit data into a Bech32 or Bech32m string.
    ///
    /// #### Parameters
    ///
    /// - `hrp`: the human readable part, 1 to 83 printable ASCII characters (33 to 126); uppercase is folded
    /// - `data`: values in the range 0 to 31
    /// - `variant`: [#BECH32] or [#BECH32M]
    ///
    /// #### Returns
    ///
    /// the lowercase encoded string
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for an invalid HRP, data value, variant or total length over 90
    public static String encode(String hrp, byte[] data, int variant) {
        int k = constant(variant);
        if (hrp.length() < 1 || hrp.length() > 83) {
            throw new IllegalArgumentException("Bad HRP length");
        }
        StringBuilder h = new StringBuilder(hrp.length());
        boolean lower = false;
        boolean upper = false;
        for (int i = 0; i < hrp.length(); i++) {
            char c = hrp.charAt(i);
            if (c < 33 || c > 126) {
                throw new IllegalArgumentException("Bad HRP character");
            }
            if (c >= 'a' && c <= 'z') {
                lower = true;
            }
            if (c >= 'A' && c <= 'Z') {
                upper = true;
            }
            h.append(fold(c));
        }
        if (lower && upper) {
            throw new IllegalArgumentException("Mixed case HRP");
        }
        String hr = h.toString();
        if (hr.length() + 1 + data.length + 6 > 90) {
            throw new IllegalArgumentException("Bech32 string too long");
        }
        for (byte datum : data) {
            if ((datum & 0xe0) != 0) {
                throw new IllegalArgumentException("Data value out of range");
            }
        }
        byte[] values = concat(concat(expandHrp(hr), data), new byte[6]);
        int mod = polymod(values) ^ k;
        StringBuilder sb = new StringBuilder(hr.length() + 1 + data.length + 6);
        sb.append(hr).append('1');
        for (byte datum : data) {
            sb.append(CHARSET.charAt(datum));
        }
        for (int i = 0; i < 6; i++) {
            sb.append(CHARSET.charAt((mod >>> (5 * (5 - i))) & 31));
        }
        return sb.toString();
    }

    /// Decodes and validates a Bech32 or Bech32m string.
    ///
    /// #### Parameters
    ///
    /// - `str`: the encoded text
    ///
    /// #### Returns
    ///
    /// the HRP, data and checksum variant
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for any violation of the BIP-173/BIP-350 rules
    public static Decoded decode(String str) {
        int n = str.length();
        if (n < 8 || n > 90) {
            throw new IllegalArgumentException("Bad Bech32 length");
        }
        boolean lower = false;
        boolean upper = false;
        for (int i = 0; i < n; i++) {
            char c = str.charAt(i);
            if (c < 33 || c > 126) {
                throw new IllegalArgumentException("Bad Bech32 character");
            }
            if (c >= 'a' && c <= 'z') {
                lower = true;
            } else if (c >= 'A' && c <= 'Z') {
                upper = true;
            }
        }
        if (lower && upper) {
            throw new IllegalArgumentException("Mixed case Bech32");
        }
        int pos = str.lastIndexOf('1');
        if (pos < 1 || pos + 7 > n) {
            throw new IllegalArgumentException("Bad Bech32 separator position");
        }
        StringBuilder h = new StringBuilder(pos);
        for (int i = 0; i < pos; i++) {
            h.append(fold(str.charAt(i)));
        }
        String hrp = h.toString();
        byte[] values = new byte[n - pos - 1];
        for (int i = 0; i < values.length; i++) {
            char c = fold(str.charAt(pos + 1 + i));
            int d = c < 128 ? REV[c] : -1;
            if (d < 0) {
                throw new IllegalArgumentException("Bad Bech32 data character");
            }
            values[i] = (byte) d;
        }
        int chk = polymod(concat(expandHrp(hrp), values));
        int variant;
        if (chk == BECH32_CONST) {
            variant = BECH32;
        } else if (chk == BECH32M_CONST) {
            variant = BECH32M;
        } else {
            throw new IllegalArgumentException("Bad Bech32 checksum");
        }
        byte[] data = new byte[values.length - 6];
        System.arraycopy(values, 0, data, 0, data.length);
        return new Decoded(hrp, data, variant);
    }

    /// Regroups bits, for example 8-bit bytes to 5-bit values and back.
    ///
    /// #### Parameters
    ///
    /// - `data`: input values, each fitting in `fromBits` bits
    /// - `fromBits`: input group size, 1 to 8
    /// - `toBits`: output group size, 1 to 8
    /// - `pad`: whether leftover bits are zero padded (encoding) or must be zero and shorter than a group (decoding)
    ///
    /// #### Returns
    ///
    /// the regrouped values
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for an out-of-range value or invalid padding
    public static byte[] convertBits(byte[] data, int fromBits, int toBits, boolean pad) {
        if (fromBits < 1 || fromBits > 8 || toBits < 1 || toBits > 8) {
            throw new IllegalArgumentException("Bad group size");
        }
        int acc = 0;
        int bits = 0;
        int maxv = (1 << toBits) - 1;
        byte[] out = new byte[(data.length * fromBits + toBits - 1) / toBits + 1];
        int len = 0;
        for (byte datum : data) {
            int v = datum & 0xff;
            if ((v >>> fromBits) != 0) {
                throw new IllegalArgumentException("Value out of range");
            }
            acc = ((acc << fromBits) | v) & 0xffff;
            bits += fromBits;
            while (bits >= toBits) {
                bits -= toBits;
                out[len++] = (byte) ((acc >>> bits) & maxv);
            }
        }
        if (pad) {
            if (bits > 0) {
                out[len++] = (byte) ((acc << (toBits - bits)) & maxv);
            }
        } else if (bits >= fromBits || ((acc << (toBits - bits)) & maxv) != 0) {
            throw new IllegalArgumentException("Invalid padding");
        }
        byte[] res = new byte[len];
        System.arraycopy(out, 0, res, 0, len);
        return res;
    }

    /// Encodes a SegWit address, using Bech32 for witness version 0 and Bech32m otherwise.
    ///
    /// #### Parameters
    ///
    /// - `hrp`: the network prefix such as `bc` or `tb`
    /// - `witnessVersion`: 0 to 16
    /// - `program`: the witness program, 2 to 40 bytes (20 or 32 for version 0)
    ///
    /// #### Returns
    ///
    /// the address
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the version or program length is invalid
    public static String encodeSegwitAddress(String hrp, int witnessVersion, byte[] program) {
        checkProgram(witnessVersion, program.length);
        byte[] conv = convertBits(program, 8, 5, true);
        byte[] data = new byte[conv.length + 1];
        data[0] = (byte) witnessVersion;
        System.arraycopy(conv, 0, data, 1, conv.length);
        return encode(hrp, data, witnessVersion == 0 ? BECH32 : BECH32M);
    }

    /// Decodes and validates a SegWit address.
    ///
    /// #### Parameters
    ///
    /// - `hrp`: the expected network prefix, compared without regard to case
    /// - `addr`: the address
    ///
    /// #### Returns
    ///
    /// the witness version and program
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the address is invalid or has another HRP
    public static SegwitAddress decodeSegwitAddress(String hrp, String addr) {
        Decoded d = decode(addr);
        if (!d.getHrp().equalsIgnoreCase(hrp)) {
            throw new IllegalArgumentException("Unexpected HRP");
        }
        byte[] data = d.data;
        if (data.length < 1) {
            throw new IllegalArgumentException("Empty SegWit data");
        }
        int version = data[0];
        if (version > 16) {
            throw new IllegalArgumentException("Bad witness version");
        }
        if ((version == 0) != (d.variant == BECH32)) {
            throw new IllegalArgumentException("Wrong checksum variant for witness version");
        }
        byte[] rest = new byte[data.length - 1];
        System.arraycopy(data, 1, rest, 0, rest.length);
        byte[] program = convertBits(rest, 5, 8, false);
        checkProgram(version, program.length);
        return new SegwitAddress(version, program);
    }

    private static void checkProgram(int version, int len) {
        if (version < 0 || version > 16) {
            throw new IllegalArgumentException("Bad witness version");
        }
        if (len < 2 || len > 40) {
            throw new IllegalArgumentException("Bad witness program length");
        }
        if (version == 0 && len != 20 && len != 32) {
            throw new IllegalArgumentException("Bad version 0 program length");
        }
    }
}
