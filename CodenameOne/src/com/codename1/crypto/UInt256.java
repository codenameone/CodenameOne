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

import com.codename1.util.BigInteger;

/// An unsigned 256-bit integer: the width of every EVM quantity and wide enough for any
/// Bitcoin or Solana amount. Immutable.
///
/// Arithmetic is exact and refuses to wrap: an overflow or a negative result throws
/// `ArithmeticException`, because a balance that silently wraps is a loss of funds.
/// [#parseUnits] and [#formatUnits] move between base units and the decimal amounts people
/// read (`"1.5"` ether is `1500000000000000000` wei).
///
/// The arithmetic is [com.codename1.util.BigInteger]'s, which both the device and the
/// backend compile; this class adds the 256-bit range check and the wallet encodings.
@com.codename1.impl.SharedWithBackend
public final class UInt256 implements Comparable<UInt256> {
    private static final BigInteger LIMIT = BigInteger.ONE.shiftLeft(256);

    /// Zero.
    public static final UInt256 ZERO = new UInt256(BigInteger.ZERO);
    /// One.
    public static final UInt256 ONE = new UInt256(BigInteger.ONE);
    /// 2^256 - 1.
    public static final UInt256 MAX = new UInt256(LIMIT.subtract(BigInteger.ONE));

    /// Always in [0, 2^256).
    private final BigInteger value;

    private UInt256(BigInteger value) {
        this.value = value;
    }

    /// Wraps `v`, refusing anything outside [0, 2^256).
    private static UInt256 checked(BigInteger v) {
        if (v.signum() < 0) {
            throw new ArithmeticException("negative result");
        }
        if (v.bitLength() > 256) {
            throw new ArithmeticException("256-bit overflow");
        }
        return new UInt256(v);
    }

    /// The value of a non-negative long.
    public static UInt256 valueOf(long value) {
        if (value < 0) {
            throw new ArithmeticException("negative value");
        }
        return new UInt256(BigInteger.valueOf(value));
    }

    /// Reads a big-endian unsigned integer of at most 32 significant bytes; leading zero
    /// bytes beyond 32 are allowed.
    public static UInt256 fromBytes(byte[] bigEndian) {
        return fromBytes(bigEndian, 0, bigEndian.length);
    }

    /// Reads `length` big-endian bytes at `offset`.
    public static UInt256 fromBytes(byte[] data, int offset, int length) {
        byte[] mag = new byte[length];
        System.arraycopy(data, offset, mag, 0, length);
        BigInteger v = new BigInteger(1, mag);
        if (v.bitLength() > 256) {
            throw new ArithmeticException("more than 256 bits");
        }
        return new UInt256(v);
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
        // BigInteger accepts a sign; an amount must not have one.
        for (int iter = 0; iter < digits.length(); iter++) {
            if (Character.digit(digits.charAt(iter), radix) < 0) {
                throw new NumberFormatException("bad digit in " + digits);
            }
        }
        BigInteger v = new BigInteger(digits, radix);
        if (v.bitLength() > 256) {
            throw new NumberFormatException("more than 256 bits: " + digits);
        }
        return new UInt256(v);
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
        return checked(value.add(other.value));
    }

    /// `this - other`.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: when `other` is larger
    public UInt256 subtract(UInt256 other) {
        return checked(value.subtract(other.value));
    }

    /// `this * other`.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: when the product exceeds [#MAX]
    public UInt256 multiply(UInt256 other) {
        return checked(value.multiply(other.value));
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
        BigInteger[] qr = value.divideAndRemainder(divisor.value);
        return new UInt256[] {new UInt256(qr[0]), new UInt256(qr[1])};
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
        if (isZero()) {
            return this;
        }
        if (bits < 0 || bits > 256) {
            throw new ArithmeticException("256-bit overflow");
        }
        return checked(value.shiftLeft(bits));
    }

    /// `this >>> bits`.
    public UInt256 shiftRight(int bits) {
        if (bits < 0) {
            throw new IllegalArgumentException("negative shift");
        }
        return bits >= 256 ? ZERO : new UInt256(value.shiftRight(bits));
    }

    /// `10^exponent`, for unit conversions.
    ///
    /// #### Throws
    ///
    /// - `ArithmeticException`: when the power exceeds [#MAX]
    public static UInt256 tenPow(int exponent) {
        if (exponent < 0) {
            return ONE;
        }
        if (exponent > 78) {
            throw new ArithmeticException("256-bit overflow");
        }
        return checked(BigInteger.valueOf(10).pow(exponent));
    }

    /// Tells whether bit `n` (0 is the lowest) is set.
    public boolean testBit(int n) {
        return n >= 0 && n < 256 && value.testBit(n);
    }

    /// The number of significant bits; 0 for zero.
    public int bitLength() {
        return value.bitLength();
    }

    /// Tells whether this is zero.
    public boolean isZero() {
        return value.signum() == 0;
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
        return value.longValue();
    }

    /// The 32 big-endian bytes.
    public byte[] toBytes32() {
        byte[] min = toMinimalBytes();
        byte[] out = new byte[32];
        System.arraycopy(min, 0, out, 32 - min.length, min.length);
        return out;
    }

    /// The minimal big-endian bytes, empty for zero: the RLP and JSON-RPC quantity form.
    public byte[] toMinimalBytes() {
        // Two's complement: a value with its top bit set gains a leading zero byte.
        byte[] twos = value.toByteArray();
        int start = 0;
        while (start < twos.length && twos[start] == 0) {
            start++;
        }
        byte[] out = new byte[twos.length - start];
        System.arraycopy(twos, start, out, 0, out.length);
        return out;
    }

    /// Lowercase hexadecimal without leading zeros or prefix; `"0"` for zero.
    public String toHex() {
        String hex = com.codename1.security.Hash.toHex(toMinimalBytes());
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
        return value.toString();
    }

    @Override
    public int compareTo(UInt256 other) {
        return value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UInt256 && value.equals(((UInt256) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
