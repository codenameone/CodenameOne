/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

/**
 * Dart's {@code BigInt}: an immutable arbitrary-precision integer.
 *
 * Neither {@code vm/JavaAPI} nor {@code Ports/CLDC11} has {@code java.math.BigInteger},
 * so this is sign-magnitude over an {@code int[]} of 32-bit words, least significant
 * first, never with a zero top word (zero is the empty array). Bitwise operators and
 * shifts follow Dart's two's complement semantics on an infinitely sign-extended value.
 * Operators carry the transpiler's mangled names ($plus, $minus, $tdiv, $mod ...).
 */
public final class BigInt implements Comparable<BigInt> {
    private static final long MASK = 0xffffffffL;
    private static final int[] EMPTY = new int[0];

    public static final BigInt zero = new BigInt(0, EMPTY);
    public static final BigInt one = new BigInt(1, new int[] {1});
    public static final BigInt two = new BigInt(1, new int[] {2});

    /** -1, 0 or 1. */
    private final int signum;
    private final int[] mag;

    private BigInt(int signum, int[] mag) {
        int n = mag.length;
        while (n > 0 && mag[n - 1] == 0) {
            n--;
        }
        if (n != mag.length) {
            int[] t = new int[n];
            System.arraycopy(mag, 0, t, 0, n);
            mag = t;
        }
        this.mag = mag;
        this.signum = n == 0 ? 0 : signum;
    }

    // ---- construction --------------------------------------------------------

    public static BigInt from(long v) {
        if (v == 0) {
            return zero;
        }
        int s = v < 0 ? -1 : 1;
        long m = v < 0 ? -v : v; // Long.MIN_VALUE stays itself, read as unsigned below
        return new BigInt(s, new int[] {(int) m, (int) (m >>> 32)});
    }

    public static BigInt from(int v) {
        return from((long) v);
    }

    /** Dart's {@code BigInt.from(num)}: a double is truncated toward zero. */
    public static BigInt from(double d) {
        if (d != d || d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY) {
            throw new UnsupportedError("Infinity or NaN toInt");
        }
        boolean neg = d < 0;
        if (neg) {
            d = -d;
        }
        if (d < 1) {
            return zero;
        }
        long bits = Double.doubleToLongBits(d);
        int exp = (int) ((bits >>> 52) & 0x7ff) - 1075;
        long mant = (bits & 0xfffffffffffffL) | 0x10000000000000L;
        BigInt r;
        if (exp >= 0) {
            r = from(mant).$shl(exp);
        } else {
            r = from(mant >>> -exp);
        }
        return neg ? r.$minus() : r;
    }

    public static BigInt from(Number n) {
        if (n instanceof Double || n instanceof Float) {
            return from(n.doubleValue());
        }
        return from(n.longValue());
    }

    public static BigInt from(Object n) {
        if (n instanceof BigInt) {
            return (BigInt) n;
        }
        if (n instanceof Number) {
            return from((Number) n);
        }
        throw new ArgumentError("Not a number: " + n);
    }

    public static BigInt parse(String source) {
        return parse(source, null);
    }

    public static BigInt parse(String source, Object radix) {
        BigInt r = tryParse(source, radix);
        if (r == null) {
            throw new FormatException("Could not parse BigInt " + source);
        }
        return r;
    }

    public static BigInt tryParse(String source) {
        return tryParse(source, null);
    }

    public static BigInt tryParse(String source, Object radix) {
        if (source == null) {
            return null;
        }
        int rdx = -1;
        if (radix instanceof Number) {
            rdx = ((Number) radix).intValue();
            // An omitted named radix can arrive as the type's zero: read it as absent.
            if (rdx == 0) {
                rdx = -1;
            } else if (rdx < 2 || rdx > 36) {
                throw new RangeError("Radix " + rdx + " not in range 2..36");
            }
        }
        String s = source.trim();
        int i = 0;
        boolean neg = false;
        if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
            neg = s.charAt(i) == '-';
            i++;
        }
        if (rdx < 0) {
            if (i + 1 < s.length() && s.charAt(i) == '0'
                    && (s.charAt(i + 1) == 'x' || s.charAt(i + 1) == 'X')) {
                rdx = 16;
                i += 2;
            } else {
                rdx = 10;
            }
        }
        if (i >= s.length()) {
            return null;
        }
        int[] acc = EMPTY;
        for (; i < s.length(); i++) {
            int dgt = digit(s.charAt(i));
            if (dgt < 0 || dgt >= rdx) {
                return null;
            }
            acc = mulAddSmall(acc, rdx, dgt);
        }
        return new BigInt(neg ? -1 : 1, acc);
    }

    private static int digit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'Z') {
            return c - 'A' + 10;
        }
        return -1;
    }

    // ---- magnitude helpers ---------------------------------------------------

    private static int[] mulAddSmall(int[] a, int m, int add) {
        int[] r = new int[a.length + 1];
        long carry = add & MASK;
        for (int i = 0; i < a.length; i++) {
            long p = (a[i] & MASK) * (m & MASK) + carry;
            r[i] = (int) p;
            carry = p >>> 32;
        }
        r[a.length] = (int) carry;
        return r;
    }

    private static int cmpMag(int[] a, int[] b) {
        if (a.length != b.length) {
            return a.length < b.length ? -1 : 1;
        }
        for (int i = a.length - 1; i >= 0; i--) {
            long x = a[i] & MASK;
            long y = b[i] & MASK;
            if (x != y) {
                return x < y ? -1 : 1;
            }
        }
        return 0;
    }

    private static int[] addMag(int[] a, int[] b) {
        if (a.length < b.length) {
            int[] t = a;
            a = b;
            b = t;
        }
        int[] r = new int[a.length + 1];
        long carry = 0;
        for (int i = 0; i < a.length; i++) {
            long s = (a[i] & MASK) + (i < b.length ? b[i] & MASK : 0) + carry;
            r[i] = (int) s;
            carry = s >>> 32;
        }
        r[a.length] = (int) carry;
        return r;
    }

    /** a - b where |a| >= |b|. */
    private static int[] subMag(int[] a, int[] b) {
        int[] r = new int[a.length];
        long borrow = 0;
        for (int i = 0; i < a.length; i++) {
            long d = (a[i] & MASK) - (i < b.length ? b[i] & MASK : 0) - borrow;
            r[i] = (int) d;
            borrow = d < 0 ? 1 : 0;
        }
        return r;
    }

    private static int[] mulMag(int[] a, int[] b) {
        int[] r = new int[a.length + b.length];
        for (int i = 0; i < a.length; i++) {
            long carry = 0;
            long x = a[i] & MASK;
            if (x == 0) {
                continue;
            }
            for (int j = 0; j < b.length; j++) {
                long t = x * (b[j] & MASK) + (r[i + j] & MASK) + carry;
                r[i + j] = (int) t;
                carry = t >>> 32;
            }
            r[i + b.length] = (int) carry;
        }
        return r;
    }

    private static int nlz(int x) {
        if (x == 0) {
            return 32;
        }
        int n = 0;
        while ((x & 0x80000000) == 0) {
            x <<= 1;
            n++;
        }
        return n;
    }

    /**
     * Knuth's algorithm D on magnitudes (Hacker's Delight divmnu): returns
     * {quotient, remainder}. b must be non-empty.
     */
    private static int[][] divMag(int[] u, int[] v) {
        int m = u.length;
        int n = v.length;
        if (cmpMag(u, v) < 0) {
            return new int[][] {EMPTY, u};
        }
        int[] q = new int[m - n + 1];
        if (n == 1) {
            long d = v[0] & MASK;
            long rem = 0;
            for (int j = m - 1; j >= 0; j--) {
                long cur = (rem << 32) | (u[j] & MASK);
                q[j] = (int) divU(cur, d);
                rem = cur - (q[j] & MASK) * d;
            }
            return new int[][] {q, new int[] {(int) rem}};
        }
        int s = nlz(v[n - 1]);
        int[] vn = new int[n];
        for (int i = n - 1; i > 0; i--) {
            vn[i] = s == 0 ? v[i] : (v[i] << s) | (v[i - 1] >>> (32 - s));
        }
        vn[0] = v[0] << s;
        int[] un = new int[m + 1];
        un[m] = s == 0 ? 0 : u[m - 1] >>> (32 - s);
        for (int i = m - 1; i > 0; i--) {
            un[i] = s == 0 ? u[i] : (u[i] << s) | (u[i - 1] >>> (32 - s));
        }
        un[0] = u[0] << s;
        long vtop = vn[n - 1] & MASK;
        long vnext = vn[n - 2] & MASK;
        for (int j = m - n; j >= 0; j--) {
            long num = ((un[j + n] & MASK) << 32) | (un[j + n - 1] & MASK);
            long qhat = divU(num, vtop);
            long rhat = num - qhat * vtop;
            while (qhat >= 0x100000000L
                    || unsignedGreater(qhat * vnext, (rhat << 32) | (un[j + n - 2] & MASK))) {
                qhat--;
                rhat += vtop;
                if (rhat >= 0x100000000L) {
                    break;
                }
            }
            long borrow = 0;
            long t;
            for (int i = 0; i < n; i++) {
                long p = qhat * (vn[i] & MASK);
                t = (un[i + j] & MASK) - borrow - (p & MASK);
                un[i + j] = (int) t;
                borrow = (p >>> 32) - (t >> 32);
            }
            t = (un[j + n] & MASK) - borrow;
            un[j + n] = (int) t;
            if (t < 0) {
                qhat--;
                long carry = 0;
                for (int i = 0; i < n; i++) {
                    long sum = (un[i + j] & MASK) + (vn[i] & MASK) + carry;
                    un[i + j] = (int) sum;
                    carry = sum >>> 32;
                }
                un[j + n] = (int) ((un[j + n] & MASK) + carry);
            }
            q[j] = (int) qhat;
        }
        int[] r = new int[n];
        for (int i = 0; i < n; i++) {
            r[i] = s == 0 ? un[i] : (un[i] >>> s) | (un[i + 1] << (32 - s));
        }
        return new int[][] {q, r};
    }

    /** Unsigned 64-by-64 division, for a divisor below 2^32. */
    private static long divU(long n, long d) {
        if (n >= 0) {
            return n / d;
        }
        long q = ((n >>> 1) / d) << 1;
        long r = n - q * d;
        while (r < 0 || r >= d) {
            q++;
            r -= d;
        }
        return q;
    }

    private static boolean unsignedGreater(long a, long b) {
        return (a + Long.MIN_VALUE) > (b + Long.MIN_VALUE);
    }

    // ---- arithmetic ------------------------------------------------------------

    public BigInt $plus(BigInt o) {
        if (o.signum == 0) {
            return this;
        }
        if (signum == 0) {
            return o;
        }
        if (signum == o.signum) {
            return new BigInt(signum, addMag(mag, o.mag));
        }
        int c = cmpMag(mag, o.mag);
        if (c == 0) {
            return zero;
        }
        return c > 0 ? new BigInt(signum, subMag(mag, o.mag)) : new BigInt(o.signum, subMag(o.mag, mag));
    }

    public BigInt $minus(BigInt o) {
        return $plus(o.$minus());
    }

    /** Unary minus. */
    public BigInt $minus() {
        return signum == 0 ? this : new BigInt(-signum, mag);
    }

    public BigInt $times(BigInt o) {
        if (signum == 0 || o.signum == 0) {
            return zero;
        }
        return new BigInt(signum * o.signum, mulMag(mag, o.mag));
    }

    /** Dart's {@code /}: both operands as doubles. */
    public double $div(BigInt o) {
        return toDouble() / o.toDouble();
    }

    private int[][] divRem(BigInt o) {
        if (o.signum == 0) {
            throw new IntegerDivisionByZeroException();
        }
        return divMag(mag, o.mag);
    }

    /** Truncating division. */
    public BigInt $tdiv(BigInt o) {
        if (signum == 0) {
            divRem(o);
            return zero;
        }
        return new BigInt(signum * o.signum, divRem(o)[0]);
    }

    /** Remainder with the sign of the dividend. */
    public BigInt remainder(BigInt o) {
        if (signum == 0) {
            divRem(o);
            return zero;
        }
        return new BigInt(signum, divRem(o)[1]);
    }

    /** Euclidean modulo: never negative. */
    public BigInt $mod(BigInt o) {
        BigInt r = remainder(o);
        if (r.signum < 0) {
            r = o.signum < 0 ? r.$minus(o) : r.$plus(o);
        }
        return r;
    }

    public BigInt abs() {
        return signum < 0 ? $minus() : this;
    }

    public BigInt pow(long exponent) {
        if (exponent < 0) {
            throw new ArgumentError("Exponent must not be negative: " + exponent);
        }
        BigInt result = one;
        BigInt base = this;
        while (exponent > 0) {
            if ((exponent & 1) != 0) {
                result = result.$times(base);
            }
            exponent >>= 1;
            if (exponent > 0) {
                base = base.$times(base);
            }
        }
        return result;
    }

    public BigInt modPow(BigInt exponent, BigInt modulus) {
        if (exponent.signum < 0) {
            throw new ArgumentError("exponent must be positive: " + exponent);
        }
        if (modulus.signum <= 0) {
            throw new ArgumentError("modulus must be strictly positive: " + modulus);
        }
        BigInt result = one.$mod(modulus);
        BigInt base = $mod(modulus);
        int bits = (int) exponent.bitLength();
        for (int i = 0; i < bits; i++) {
            if (exponent.testBit(i)) {
                result = result.$times(base).$mod(modulus);
            }
            if (i + 1 < bits) {
                base = base.$times(base).$mod(modulus);
            }
        }
        return result;
    }

    public BigInt modInverse(BigInt modulus) {
        if (modulus.signum <= 0) {
            throw new ArgumentError("Modulus must be strictly positive: " + modulus);
        }
        BigInt a = $mod(modulus);
        BigInt m = modulus;
        BigInt x0 = zero;
        BigInt x1 = one;
        BigInt r0 = m;
        BigInt r1 = a;
        while (r1.signum != 0) {
            BigInt q = r0.$tdiv(r1);
            BigInt t = r0.$minus(q.$times(r1));
            r0 = r1;
            r1 = t;
            t = x0.$minus(q.$times(x1));
            x0 = x1;
            x1 = t;
        }
        if (!(r0.signum == 1 && r0.mag.length == 1 && r0.mag[0] == 1)) {
            throw new UnsupportedError("Not coprime");
        }
        return x0.$mod(m);
    }

    public BigInt gcd(BigInt o) {
        BigInt a = abs();
        BigInt b = o.abs();
        while (b.signum != 0) {
            BigInt t = a.remainder(b);
            a = b;
            b = t;
        }
        return a;
    }

    // ---- bitwise (two's complement) ------------------------------------------

    /** Two's complement words, sign-extended to {@code len} words. */
    private int[] twos(int len) {
        int[] r = new int[len];
        System.arraycopy(mag, 0, r, 0, Math.min(mag.length, len));
        if (signum < 0) {
            long carry = 1;
            for (int i = 0; i < len; i++) {
                long t = ((~r[i]) & MASK) + carry;
                r[i] = (int) t;
                carry = t >>> 32;
            }
        }
        return r;
    }

    private static BigInt fromTwos(int[] w) {
        if (w.length == 0 || w[w.length - 1] >= 0) {
            return new BigInt(1, w);
        }
        int[] r = new int[w.length];
        long carry = 1;
        for (int i = 0; i < w.length; i++) {
            long t = ((~w[i]) & MASK) + carry;
            r[i] = (int) t;
            carry = t >>> 32;
        }
        return new BigInt(-1, r);
    }

    private BigInt bitwise(BigInt o, int op) {
        int len = Math.max(mag.length, o.mag.length) + 1;
        int[] a = twos(len);
        int[] b = o.twos(len);
        for (int i = 0; i < len; i++) {
            a[i] = op == 0 ? a[i] & b[i] : op == 1 ? a[i] | b[i] : a[i] ^ b[i];
        }
        return fromTwos(a);
    }

    public BigInt $bitAnd(BigInt o) {
        return bitwise(o, 0);
    }

    public BigInt $bitOr(BigInt o) {
        return bitwise(o, 1);
    }

    public BigInt $bitXor(BigInt o) {
        return bitwise(o, 2);
    }

    /** {@code ~x == -x - 1}. */
    public BigInt $bitNot() {
        return $minus().$minus(one);
    }

    public BigInt $shl(long shift) {
        if (shift < 0) {
            throw new ArgumentError("Shift amount must be non-negative: " + shift);
        }
        if (signum == 0 || shift == 0) {
            return this;
        }
        int words = (int) (shift >>> 5);
        int bits = (int) (shift & 31);
        int[] r = new int[mag.length + words + 1];
        for (int i = 0; i < mag.length; i++) {
            long v = (mag[i] & MASK) << bits;
            r[i + words] |= (int) v;
            r[i + words + 1] |= (int) (v >>> 32);
        }
        return new BigInt(signum, r);
    }

    /** Arithmetic shift: floor(this / 2^shift). */
    public BigInt $shr(long shift) {
        if (shift < 0) {
            throw new ArgumentError("Shift amount must be non-negative: " + shift);
        }
        if (signum == 0 || shift == 0) {
            return this;
        }
        int words = (int) Math.min(shift >>> 5, (long) mag.length + 1);
        int bits = (int) (shift & 31);
        int[] w = twos(mag.length + 1);
        int fill = signum < 0 ? -1 : 0;
        int n = Math.max(w.length - words, 1);
        int[] r = new int[n];
        for (int i = 0; i < n; i++) {
            int lo = i + words < w.length ? w[i + words] : fill;
            int hi = i + words + 1 < w.length ? w[i + words + 1] : fill;
            r[i] = bits == 0 ? lo : (lo >>> bits) | (hi << (32 - bits));
        }
        return fromTwos(r);
    }

    public BigInt toUnsigned(long width) {
        return $bitAnd(one.$shl(width).$minus(one));
    }

    public BigInt toSigned(long width) {
        if (width <= 0) {
            return zero;
        }
        BigInt m = toUnsigned(width);
        return m.testBit((int) (width - 1)) ? m.$minus(one.$shl(width)) : m;
    }

    private boolean testBit(int n) {
        int word = n >>> 5;
        int[] w = twos(Math.max(mag.length, word) + 1);
        return ((w[word] >>> (n & 31)) & 1) != 0;
    }

    // ---- queries ---------------------------------------------------------------

    public long sign() {
        return signum;
    }

    /** Bits needed for the two's complement value, without the sign bit. */
    public long bitLength() {
        if (signum == 0) {
            return 0;
        }
        BigInt m = signum < 0 ? $bitNot() : this;
        if (m.signum == 0) {
            return 0;
        }
        return (long) (m.mag.length - 1) * 32 + (32 - nlz(m.mag[m.mag.length - 1]));
    }

    public boolean isEven() {
        return signum == 0 || (mag[0] & 1) == 0;
    }

    public boolean isOdd() {
        return !isEven();
    }

    public boolean isNegative() {
        return signum < 0;
    }

    public boolean isValidInt() {
        return bitLength() <= 63;
    }

    /** Clamps to the int range when the value does not fit, as the Dart VM does. */
    public long toInt() {
        if (!isValidInt()) {
            return signum < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        long v = 0;
        for (int i = Math.min(mag.length, 2) - 1; i >= 0; i--) {
            v = (v << 32) | (mag[i] & MASK);
        }
        return signum < 0 ? -v : v;
    }

    /** Nearest double, ties to even. */
    public double toDouble() {
        if (signum == 0) {
            return 0.0;
        }
        long bl = abs().bitLength();
        if (bl > 1024) {
            return signum < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        double r;
        if (bl <= 63) {
            r = (double) abs().toInt();
        } else {
            BigInt a = abs();
            int drop = (int) (bl - 54);
            long top = a.$shr(drop).toInt(); // 54 bits: 53 + one rounding bit
            boolean sticky = a.$bitAnd(one.$shl(drop).$minus(one)).signum != 0;
            long mant = top >>> 1;
            if ((top & 1) != 0 && (sticky || (mant & 1) != 0)) {
                mant++;
            }
            r = (double) mant;
            int e = drop + 1;
            while (e > 0) {
                int step = Math.min(e, 60);
                r *= (double) (1L << step);
                e -= step;
            }
        }
        return signum < 0 ? -r : r;
    }

    public String toRadixString(long radix) {
        if (radix < 2 || radix > 36) {
            throw new RangeError("Radix " + radix + " not in range 2..36");
        }
        if (signum == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        int[] cur = mag;
        int r = (int) radix;
        while (cur.length > 0) {
            long rem = 0;
            int[] q = new int[cur.length];
            for (int j = cur.length - 1; j >= 0; j--) {
                long v = (rem << 32) | (cur[j] & MASK);
                q[j] = (int) divU(v, r);
                rem = v - (q[j] & MASK) * r;
            }
            sb.append("0123456789abcdefghijklmnopqrstuvwxyz".charAt((int) rem));
            int n = q.length;
            while (n > 0 && q[n - 1] == 0) {
                n--;
            }
            if (n != q.length) {
                int[] t = new int[n];
                System.arraycopy(q, 0, t, 0, n);
                q = t;
            }
            cur = q;
        }
        if (signum < 0) {
            sb.append('-');
        }
        return sb.reverse().toString();
    }

    @Override
    public String toString() {
        return toRadixString(10);
    }

    // ---- comparison --------------------------------------------------------------

    @Override
    public int compareTo(BigInt o) {
        if (signum != o.signum) {
            return signum < o.signum ? -1 : 1;
        }
        int c = cmpMag(mag, o.mag);
        return signum < 0 ? -c : c;
    }

    public boolean $lt(BigInt o) {
        return compareTo(o) < 0;
    }

    public boolean $le(BigInt o) {
        return compareTo(o) <= 0;
    }

    public boolean $gt(BigInt o) {
        return compareTo(o) > 0;
    }

    public boolean $ge(BigInt o) {
        return compareTo(o) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BigInt && compareTo((BigInt) o) == 0;
    }

    @Override
    public int hashCode() {
        int h = signum;
        for (int i = 0; i < mag.length; i++) {
            h = 31 * h + mag[i];
        }
        return h;
    }
}
