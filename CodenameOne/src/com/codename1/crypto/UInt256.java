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
package com.codename1.crypto;

/// An unsigned 256-bit integer: the width of every EVM quantity and wide enough for any
/// Bitcoin or Solana amount. Immutable.
///
/// Arithmetic is exact and refuses to wrap: an overflow or a negative result throws
/// `ArithmeticException`, because a balance that silently wraps is a loss of funds.
/// [#parseUnits] and [#formatUnits] move between base units and the decimal amounts people
/// read (`"1.5"` ether is `1500000000000000000` wei).
///
/// The device class library has no `java.math`, so this is what the shared crypto API uses
/// for amounts.
@com.codename1.impl.SharedWithBackend
public final class UInt256 implements Comparable<UInt256> {
    /// Zero.
    public static final UInt256 ZERO = new UInt256(new int[8]);
    /// One.
    public static final UInt256 ONE = valueOf(1);
    /// 2^256 - 1.
    public static final UInt256 MAX = new UInt256(new int[] {-1, -1, -1, -1, -1, -1, -1, -1});

    /// Little-endian 32-bit limbs.
    private final int[] limbs;

    private UInt256(int[] limbs) {
        this.limbs = limbs;
    }

    /// The value of a non-negative long.
    public static UInt256 valueOf(long value) {
        if (value < 0) {
            throw new ArithmeticException("negative value");
        }
        int[] l = new int[8];
        l[0] = (int) value;
        l[1] = (int) (value >>> 32);
        return new UInt256(l);
    }

    /// Reads a big-endian unsigned integer of at most 32 significant bytes; leading zero
    /// bytes beyond 32 are allowed.
    public static UInt256 fromBytes(byte[] bigEndian) {
        return fromBytes(bigEndian, 0, bigEndian.length);
    }

    /// Reads `length` big-endian bytes at `offset`.
    public static UInt256 fromBytes(byte[] data, int offset, int length) {
        int[] l = new int[8];
        for (int iter = 0; iter < length; iter++) {
            int b = data[offset + length - 1 - iter] & 0xff;
            if (iter >= 32) {
                if (b != 0) {
                    throw new ArithmeticException("more than 256 bits");
                }
                continue;
            }
            l[iter >> 2] |= b << ((iter & 3) * 8);
        }
        return new UInt256(l);
    }

    /// Parses decimal digits, or hexadecimal after `0x`.
    ///
    /// #### Throws
    ///
    /// - `NumberFormatException`: for anything else, a sign, or a value above [#MAX]
    public static UInt256 parse(String text) {
        if (text == null || text.length() == 0) {
            throw new NumberFormatException("empty number");
        }
        if (text.length() > 2 && text.charAt(0) == '0' && (text.charAt(1) == 'x' || text.charAt(1) == 'X')) {
            return parse(text.substring(2), 16);
        }
        return parse(text, 10);
    }

    /// Parses digits in radix 10 or 16, no prefix.
    public static UInt256 parse(String digits, int radix) {
        if (radix != 10 && radix != 16) {
            throw new IllegalArgumentException("radix 10 or 16");
        }
        if (digits.length() == 0) {
            throw new NumberFormatException("empty number");
        }
        int[] l = new int[8];
        for (int iter = 0; iter < digits.length(); iter++) {
            int d = Character.digit(digits.charAt(iter), radix);
            if (d < 0) {
                throw new NumberFormatException("bad digit in " + digits);
            }
            if (mulSmallAdd(l, radix, d) != 0) {
                throw new NumberFormatException("more than 256 bits: " + digits);
            }
        }
        return new UInt256(l);
    }

    /// Parses a decimal amount into base units: `parseUnits("1.5", 18)` is 1.5 ether in wei.
    ///
    /// #### Throws
    ///
    /// - `NumberFormatException`: for a malformed amount or more fraction digits than
    ///   `decimals`
    public static UInt256 parseUnits(String amount, int decimals) {
        if (amount == null || decimals < 0 || decimals > 77) {
            throw new NumberFormatException("bad amount");
        }
        String a = amount.trim();
        int dot = a.indexOf('.');
        String whole = dot < 0 ? a : a.substring(0, dot);
        String fraction = dot < 0 ? "" : a.substring(dot + 1);
        if ((whole.length() == 0 && fraction.length() == 0) || fraction.length() > decimals
                || fraction.indexOf('.') >= 0) {
            throw new NumberFormatException("bad amount: " + amount);
        }
        StringBuilder digits = new StringBuilder(whole.length() == 0 ? "0" : whole).append(fraction);
        for (int iter = fraction.length(); iter < decimals; iter++) {
            digits.append('0');
        }
        return parse(digits.toString(), 10);
    }

    /// Formats base units as a decimal amount, without trailing fraction zeros:
    /// `formatUnits(1500000000000000000, 18)` is `"1.5"`.
    public String formatUnits(int decimals) {
        String digits = toString();
        if (decimals <= 0) {
            return digits;
        }
        StringBuilder padded = new StringBuilder();
        for (int iter = digits.length(); iter <= decimals; iter++) {
            padded.append('0');
        }
        padded.append(digits);
        String all = padded.toString();
        String whole = all.substring(0, all.length() - decimals);
        String fraction = all.substring(all.length() - decimals);
        int end = fraction.length();
        while (end > 0 && fraction.charAt(end - 1) == '0') {
            end--;
        }
        return end == 0 ? whole : whole + "." + fraction.substring(0, end);
    }

    /// `this + other`.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: when the sum exceeds [#MAX]
    public UInt256 add(UInt256 other) {
        int[] out = new int[8];
        long carry = 0;
        for (int iter = 0; iter < 8; iter++) {
            carry += (limbs[iter] & 0xffffffffL) + (other.limbs[iter] & 0xffffffffL);
            out[iter] = (int) carry;
            carry >>>= 32;
        }
        if (carry != 0) {
            throw new ArithmeticException("256-bit overflow");
        }
        return new UInt256(out);
    }

    /// `this - other`.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: when `other` is larger
    public UInt256 subtract(UInt256 other) {
        int[] out = new int[8];
        if (sub(limbs, other.limbs, out) != 0) {
            throw new ArithmeticException("negative result");
        }
        return new UInt256(out);
    }

    /// `this * other`.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: when the product exceeds [#MAX]
    public UInt256 multiply(UInt256 other) {
        long[] wide = new long[16];
        for (int i = 0; i < 8; i++) {
            long a = limbs[i] & 0xffffffffL;
            if (a == 0) {
                continue;
            }
            long carry = 0;
            for (int j = 0; j < 8; j++) {
                long t = a * (other.limbs[j] & 0xffffffffL) + (wide[i + j] & 0xffffffffL) + carry;
                wide[i + j] = t & 0xffffffffL;
                carry = t >>> 32;
            }
            wide[i + 8] = carry;
        }
        int[] out = new int[8];
        for (int iter = 0; iter < 16; iter++) {
            if (iter < 8) {
                out[iter] = (int) wide[iter];
            } else if (wide[iter] != 0) {
                throw new ArithmeticException("256-bit overflow");
            }
        }
        return new UInt256(out);
    }

    /// `this * factor` for a small non-negative factor.
    public UInt256 multiply(long factor) {
        return multiply(valueOf(factor));
    }

    /// The quotient and remainder of `this / divisor`.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: for a zero divisor
    public UInt256[] divideAndRemainder(UInt256 divisor) {
        if (divisor.isZero()) {
            throw new ArithmeticException("division by zero");
        }
        int[] q = new int[8];
        int[] r = new int[8];
        int[] tmp = new int[8];
        for (int bit = bitLength() - 1; bit >= 0; bit--) {
            // r = r << 1 | bit
            int carry = (limbs[bit >> 5] >>> (bit & 31)) & 1;
            for (int iter = 0; iter < 8; iter++) {
                int next = r[iter] >>> 31;
                r[iter] = (r[iter] << 1) | carry;
                carry = next;
            }
            boolean ge = carry != 0 || compare(r, divisor.limbs) >= 0;
            if (ge) {
                sub(r, divisor.limbs, tmp);
                System.arraycopy(tmp, 0, r, 0, 8);
                q[bit >> 5] |= 1 << (bit & 31);
            }
        }
        return new UInt256[] {new UInt256(q), new UInt256(r)};
    }

    /// `this / divisor`, rounded down.
    public UInt256 divide(UInt256 divisor) {
        return divideAndRemainder(divisor)[0];
    }

    /// `this mod divisor`.
    public UInt256 mod(UInt256 divisor) {
        return divideAndRemainder(divisor)[1];
    }

    /// `this << bits`, refusing to drop set bits.
    public UInt256 shiftLeft(int bits) {
        if (bits < 0 || bits > 256 || (bits > 0 && bitLength() + bits > 256)) {
            if (isZero()) {
                return this;
            }
            throw new ArithmeticException("256-bit overflow");
        }
        int[] out = new int[8];
        for (int bit = 0; bit < 256 - bits; bit++) {
            if (testBit(bit)) {
                out[(bit + bits) >> 5] |= 1 << ((bit + bits) & 31);
            }
        }
        return new UInt256(out);
    }

    /// `this >>> bits`.
    public UInt256 shiftRight(int bits) {
        int[] out = new int[8];
        for (int bit = bits; bit < 256; bit++) {
            if (testBit(bit)) {
                out[(bit - bits) >> 5] |= 1 << ((bit - bits) & 31);
            }
        }
        return new UInt256(out);
    }

    /// `10^exponent`, for unit conversions.
    public static UInt256 tenPow(int exponent) {
        UInt256 out = ONE;
        UInt256 ten = valueOf(10);
        for (int iter = 0; iter < exponent; iter++) {
            out = out.multiply(ten);
        }
        return out;
    }

    /// Tells whether bit `n` (0 is the lowest) is set.
    public boolean testBit(int n) {
        if (n < 0 || n >= 256) {
            return false;
        }
        return ((limbs[n >> 5] >>> (n & 31)) & 1) != 0;
    }

    /// The number of significant bits; 0 for zero.
    public int bitLength() {
        for (int iter = 7; iter >= 0; iter--) {
            if (limbs[iter] != 0) {
                int v = limbs[iter];
                int n = 32;
                while (v >= 0) {
                    v <<= 1;
                    n--;
                }
                return iter * 32 + n;
            }
        }
        return 0;
    }

    /// Tells whether this is zero.
    public boolean isZero() {
        for (int iter = 0; iter < 8; iter++) {
            if (limbs[iter] != 0) {
                return false;
            }
        }
        return true;
    }

    /// The value as a long.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: when it does not fit in 63 bits
    public long longValueExact() {
        if (bitLength() > 63) {
            throw new ArithmeticException("does not fit in a long");
        }
        return (limbs[0] & 0xffffffffL) | ((long) limbs[1] << 32);
    }

    /// The 32 big-endian bytes.
    public byte[] toBytes32() {
        byte[] out = new byte[32];
        for (int iter = 0; iter < 32; iter++) {
            out[31 - iter] = (byte) ((limbs[iter >> 2] >> ((iter & 3) * 8)) & 0xff);
        }
        return out;
    }

    /// The minimal big-endian bytes, empty for zero: the RLP and JSON-RPC quantity form.
    public byte[] toMinimalBytes() {
        byte[] full = toBytes32();
        int start = 0;
        while (start < 32 && full[start] == 0) {
            start++;
        }
        byte[] out = new byte[32 - start];
        System.arraycopy(full, start, out, 0, out.length);
        return out;
    }

    /// Lowercase hexadecimal without leading zeros or prefix; `"0"` for zero.
    public String toHex() {
        String hex = com.codename1.crypto.encoding.Hex.encode(toMinimalBytes());
        int start = 0;
        while (start < hex.length() - 1 && hex.charAt(start) == '0') {
            start++;
        }
        return hex.length() == 0 ? "0" : hex.substring(start);
    }

    /// The JSON-RPC quantity form, `0x` and minimal hex: `0x0`, `0x1a`.
    public String toQuantity() {
        return "0x" + toHex();
    }

    /// The decimal digits.
    @Override
    public String toString() {
        if (isZero()) {
            return "0";
        }
        int[] l = new int[8];
        System.arraycopy(limbs, 0, l, 0, 8);
        StringBuilder rev = new StringBuilder();
        while (!allZero(l)) {
            long rem = 0;
            for (int iter = 7; iter >= 0; iter--) {
                long cur = (rem << 32) | (l[iter] & 0xffffffffL);
                l[iter] = (int) (cur / 1000000000L);
                rem = cur % 1000000000L;
            }
            String chunk = String.valueOf(rem);
            boolean last = allZero(l);
            for (int iter = chunk.length() - 1; iter >= 0; iter--) {
                rev.append(chunk.charAt(iter));
            }
            if (!last) {
                for (int iter = chunk.length(); iter < 9; iter++) {
                    rev.append('0');
                }
            }
        }
        return rev.reverse().toString();
    }

    @Override
    public int compareTo(UInt256 other) {
        return compare(limbs, other.limbs);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UInt256 && compare(limbs, ((UInt256) o).limbs) == 0;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int iter = 0; iter < 8; iter++) {
            h = h * 31 + limbs[iter];
        }
        return h;
    }

    private static boolean allZero(int[] l) {
        for (int iter = 0; iter < l.length; iter++) {
            if (l[iter] != 0) {
                return false;
            }
        }
        return true;
    }

    private static int compare(int[] a, int[] b) {
        for (int iter = 7; iter >= 0; iter--) {
            long x = a[iter] & 0xffffffffL;
            long y = b[iter] & 0xffffffffL;
            if (x != y) {
                return x < y ? -1 : 1;
            }
        }
        return 0;
    }

    /// out = a - b, answering the borrow.
    private static int sub(int[] a, int[] b, int[] out) {
        long borrow = 0;
        for (int iter = 0; iter < 8; iter++) {
            long t = (a[iter] & 0xffffffffL) - (b[iter] & 0xffffffffL) - borrow;
            out[iter] = (int) t;
            borrow = t < 0 ? 1 : 0;
        }
        return (int) borrow;
    }

    /// l = l * m + add in place, answering the carry out of the top limb.
    private static long mulSmallAdd(int[] l, int m, int add) {
        long carry = add;
        for (int iter = 0; iter < 8; iter++) {
            long t = (l[iter] & 0xffffffffL) * m + carry;
            l[iter] = (int) t;
            carry = t >>> 32;
        }
        return carry;
    }
}
