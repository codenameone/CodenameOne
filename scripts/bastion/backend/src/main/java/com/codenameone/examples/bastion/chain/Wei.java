/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.chain;

/// Amounts on the chain, which do not fit a `long`.
///
/// A wei is 10^-18 ETH, so a `long` holds only 9.2 ETH of them, and the
/// server has no `java.math` to reach for. The chain's numbers are therefore
/// kept as unsigned big-endian byte arrays of any length and the ledger's in
/// its own unit, gwei (10^-9 ETH, the `decimals` of 9 the ETH asset row
/// carries). The two meet only here: [#fromGwei] is exact, and [#toGwei]
/// rounds down, so a deposit of 1.0000000005 ETH credits 1 ETH and the dust
/// stays in the wallet -- the ledger never claims a coin the chain does not
/// hold.
public final class Wei {
    /// Wei in one gwei.
    static final long GWEI = 1000000000L;

    private Wei() {
    }

    /// `value` as 8 unsigned big-endian bytes.
    public static byte[] ofLong(long value) {
        byte[] out = new byte[8];
        for (int iter = 7; iter >= 0; iter--) {
            out[iter] = (byte) value;
            value >>>= 8;
        }
        return out;
    }

    /// `value` without leading zero bytes; zero is the empty array.
    public static byte[] trim(byte[] value) {
        int start = 0;
        while (start < value.length && value[start] == 0) {
            start++;
        }
        byte[] out = new byte[value.length - start];
        System.arraycopy(value, start, out, 0, out.length);
        return out;
    }

    /// The wei in `gwei` ledger units, exactly.
    public static byte[] fromGwei(long gwei) {
        if (gwei < 0) {
            throw new IllegalArgumentException("negative amount");
        }
        return trim(mulSmall(ofLong(gwei), GWEI));
    }

    /// The whole gwei in `wei`, rounded down. Throws if the amount does not
    /// fit the ledger, which no honest deposit reaches (9.2 billion ETH).
    public static long toGwei(byte[] wei) {
        long[] qr = divSmall(wei, GWEI);
        return qr[0];
    }

    /// True when `wei` is not a whole number of gwei.
    public static boolean hasDust(byte[] wei) {
        return divSmall(wei, GWEI)[1] != 0;
    }

    /// `a * m` for a small non-negative `m` (below 2^31 is plenty here).
    static byte[] mulSmall(byte[] a, long m) {
        byte[] out = new byte[a.length + 8];
        long carry = 0;
        for (int iter = a.length - 1, o = out.length - 1; o >= 0; iter--, o--) {
            long digit = iter >= 0 ? a[iter] & 0xff : 0;
            long product = digit * m + carry;
            out[o] = (byte) product;
            carry = product >>> 8;
        }
        return out;
    }

    /// `{a / d, a % d}` for `0 < d < 2^31`; the quotient must fit a long.
    static long[] divSmall(byte[] a, long d) {
        long rem = 0;
        long quotient = 0;
        for (int iter = 0; iter < a.length; iter++) {
            long cur = (rem << 8) | (a[iter] & 0xff);
            long q = cur / d;
            rem = cur % d;
            if ((quotient >>> 55) != 0) {
                throw new ArithmeticException("amount too large for the ledger");
            }
            quotient = (quotient << 8) | q;
        }
        if (quotient < 0) {
            throw new ArithmeticException("amount too large for the ledger");
        }
        return new long[] {quotient, rem};
    }

    /// A JSON-RPC quantity, `0x` and hex digits, as unsigned bytes.
    public static byte[] parseQuantity(String hex) {
        if (hex == null || !hex.startsWith("0x") || hex.length() < 3) {
            throw new IllegalArgumentException("not a quantity: " + hex);
        }
        String digits = hex.substring(2);
        if ((digits.length() & 1) == 1) {
            digits = "0" + digits;
        }
        return Hex32.decode(digits);
    }

    /// A JSON-RPC quantity that fits a long (block numbers, nonces, gas).
    public static long parseLong(String hex) {
        byte[] bytes = trim(parseQuantity(hex));
        if (bytes.length > 7) {
            throw new IllegalArgumentException("quantity too large: " + hex);
        }
        long out = 0;
        for (byte b : bytes) {
            out = (out << 8) | (b & 0xff);
        }
        return out;
    }

    /// `value` as a JSON-RPC quantity: `0x0`, or `0x` and hex without
    /// leading zeros.
    public static String quantity(byte[] value) {
        String hex = Hex32.encode(trim(value));
        int start = 0;
        while (start < hex.length() - 1 && hex.charAt(start) == '0') {
            start++;
        }
        return "0x" + (hex.length() == 0 ? "0" : hex.substring(start));
    }

    public static String quantity(long value) {
        return quantity(ofLong(value));
    }
}
