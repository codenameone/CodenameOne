/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * Codename One note: this is a rewrite of the digit generation in OpenJDK's
 * FloatingDecimal.BinaryToASCIIBuffer.dtoa (and the arithmetic it needs from
 * FDBigInteger), reduced to what DecimalFormat uses and written against the
 * JDK subset vm/JavaAPI and Ports/CLDC11 both provide. The decisions -- which
 * path a value takes, the stopping tests, the rounding of the last digit --
 * are OpenJDK's and are kept exactly, quirks included, because reproducing
 * the JDK's output is the whole point of the class.
 */
package java.text;

/// The decimal digits the JDK's `DecimalFormat` starts from when it formats a
/// `double`.
///
/// That is NOT `Double.toString`. Since JDK-4511638 `Double.toString` writes
/// the shortest string that round-trips, but `DecimalFormat` still takes its
/// digits from the older `FloatingDecimal` conversion, which is not always the
/// shortest: `1.0E23` comes out as `9.999999999999999E22`, and a value above
/// two to the 53rd whose shortest form ends in zeros keeps its seventeenth
/// digit (`7.2057594037927936E16`). Formatting from the shortest digits
/// therefore disagrees with the desktop in the last digits of large numbers,
/// so this class reproduces the old conversion.
///
/// The conversion picks one of three routes and the routes are NOT
/// equivalent at their edges (`int` and `long` arithmetic use a strict
/// stopping test where the big-integer route uses an inclusive one), so all
/// three are kept rather than doing everything with big integers.
final class LegacyDoubleDigits {
    private static final int EXP_SHIFT = 52;
    private static final long FRACT_HOB = 1L << EXP_SHIFT;
    private static final long EXP_ONE = 1023L << EXP_SHIFT;
    private static final long SIGNIF_BIT_MASK = FRACT_HOB - 1;
    private static final int MAX_SMALL_BIN_EXP = 62;
    private static final int MIN_SMALL_BIN_EXP = -(63 / 3);

    private static final int[] SMALL_5_POW = new int[14];
    private static final long[] LONG_5_POW = new long[27];

    static {
        SMALL_5_POW[0] = 1;
        for (int i = 1; i < SMALL_5_POW.length; i++) {
            SMALL_5_POW[i] = SMALL_5_POW[i - 1] * 5;
        }
        LONG_5_POW[0] = 1L;
        for (int i = 1; i < LONG_5_POW.length; i++) {
            LONG_5_POW[i] = LONG_5_POW[i - 1] * 5L;
        }
    }

    /// Approximately `ceil(log2(5^i))`, as OpenJDK tabulates it; the route
    /// choice depends on these exact values.
    private static final int[] N_5_BITS = {
        0, 3, 5, 7, 10, 12, 14, 17, 19, 21, 24, 26, 28, 31, 33, 35, 38, 40, 42, 45, 47, 49, 52, 54, 56, 59, 61
    };

    /// `insignificantDigits(1L << i)`: how many low decimal digits of an
    /// integer value are below its precision.
    private static final int[] INSIGNIFICANT_DIGITS_NUMBER = {
        0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3,
        4, 4, 4, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7,
        8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 11, 11, 11,
        12, 12, 12, 12, 13, 13, 13, 14, 14, 14,
        15, 15, 15, 15, 16, 16, 16, 17, 17, 17,
        18, 18, 18, 19
    };

    /// The digits, `digits[first]` to `digits[first + count - 1]`.
    final char[] digits = new char[24];
    int first;
    int count;
    /// The position of the decimal point relative to the first digit:
    /// the value is `0.d1d2d3... * 10^decExponent`.
    int decExponent;

    /// Converts a finite, strictly positive value.
    LegacyDoubleDigits(double value) {
        long bits = Double.doubleToLongBits(value);
        long fractBits = bits & SIGNIF_BIT_MASK;
        int binExp = (int) ((bits >> EXP_SHIFT) & 0x7ffL);
        int nSignificantBits;
        if (binExp == 0) {
            // Subnormal: normalize so the high-order bit sits at EXP_SHIFT.
            int leadingZeros = Long.numberOfLeadingZeros(fractBits);
            int shift = leadingZeros - (63 - EXP_SHIFT);
            fractBits <<= shift;
            binExp = 1 - shift;
            nSignificantBits = 64 - leadingZeros;
        } else {
            fractBits |= FRACT_HOB;
            nSignificantBits = EXP_SHIFT + 1;
        }
        binExp -= 1023;
        dtoa(binExp, fractBits, nSignificantBits);
    }

    private void developLongDigits(int decExp, long lvalue, int insignificantDigits) {
        if (insignificantDigits != 0) {
            // Drop the digits below the value's precision, rounding.
            long pow10 = LONG_5_POW[insignificantDigits] << insignificantDigits;
            long residue = lvalue % pow10;
            lvalue /= pow10;
            decExp += insignificantDigits;
            if (residue >= (pow10 >> 1)) {
                lvalue++;
            }
        }
        int digitno = digits.length - 1;
        int c = (int) (lvalue % 10L);
        lvalue /= 10L;
        while (c == 0) {
            decExp++;
            c = (int) (lvalue % 10L);
            lvalue /= 10L;
        }
        while (lvalue != 0L) {
            digits[digitno--] = (char) (c + '0');
            decExp++;
            c = (int) (lvalue % 10L);
            lvalue /= 10L;
        }
        digits[digitno] = (char) (c + '0');
        decExponent = decExp + 1;
        first = digitno;
        count = digits.length - digitno;
    }

    private void dtoa(int binExp, long fractBits, int nSignificantBits) {
        final int tailZeros = Long.numberOfTrailingZeros(fractBits);
        final int nFractBits = EXP_SHIFT + 1 - tailZeros;
        int nTinyBits = Math.max(0, nFractBits - binExp - 1);
        if (binExp <= MAX_SMALL_BIN_EXP && binExp >= MIN_SMALL_BIN_EXP
                && nTinyBits < LONG_5_POW.length && nFractBits + N_5_BITS[nTinyBits] < 64
                && nTinyBits == 0) {
            // An integer that fits a long: its digits, less the ones below
            // its precision.
            int insignificant = 0;
            if (binExp > nSignificantBits) {
                int p2 = binExp - nSignificantBits - 1;
                if (p2 > 1 && p2 < INSIGNIFICANT_DIGITS_NUMBER.length) {
                    insignificant = INSIGNIFICANT_DIGITS_NUMBER[p2];
                }
            }
            if (binExp >= EXP_SHIFT) {
                fractBits <<= (binExp - EXP_SHIFT);
            } else {
                fractBits >>>= (EXP_SHIFT - binExp);
            }
            developLongDigits(0, fractBits, insignificant);
            return;
        }
        // value = (B / S) * 10^decExp with 1 <= B / S < 10, and M is half an
        // ULP scaled like B; digits are developed until the remainder is
        // within M of either neighbour.
        int decExp = estimateDecExp(fractBits, binExp);
        int b5 = Math.max(0, -decExp);
        int b2 = b5 + nTinyBits + binExp;
        int s5 = Math.max(0, decExp);
        int s2 = s5 + nTinyBits;
        int m5 = b5;
        int m2 = b2 - nSignificantBits;

        fractBits >>>= tailZeros;
        b2 -= nFractBits - 1;
        int common2factor = Math.min(b2, s2);
        b2 -= common2factor;
        s2 -= common2factor;
        m2 -= common2factor;
        // Below an exact power of two the next smaller double is only half
        // as far away.
        if (nFractBits == 1) {
            m2 -= 1;
        }
        if (m2 < 0) {
            b2 -= m2;
            s2 -= m2;
            m2 = 0;
        }

        int ndigit = 0;
        boolean low;
        boolean high;
        long lowDigitDifference;
        int q;
        int bBits = nFractBits + b2 + (b5 < N_5_BITS.length ? N_5_BITS[b5] : b5 * 3);
        int tenSBits = s2 + 1 + (s5 + 1 < N_5_BITS.length ? N_5_BITS[s5 + 1] : (s5 + 1) * 3);
        if (bBits < 64 && tenSBits < 64) {
            if (bBits < 32 && tenSBits < 32) {
                int b = ((int) fractBits * SMALL_5_POW[b5]) << b2;
                int s = SMALL_5_POW[s5] << s2;
                int m = SMALL_5_POW[m5] << m2;
                int tens = s * 10;
                q = b / s;
                b = 10 * (b % s);
                m *= 10;
                low = b < m;
                high = b + m > tens;
                if (q == 0 && !high) {
                    // The exponent estimate was one too high.
                    decExp--;
                } else {
                    digits[ndigit++] = (char) ('0' + q);
                }
                // Java's E-form always shows a digit after the point, so
                // outside the plain-notation range a second digit is forced.
                if (decExp < -3 || decExp >= 8) {
                    high = false;
                    low = false;
                }
                while (!low && !high) {
                    q = b / s;
                    b = 10 * (b % s);
                    m *= 10;
                    if (m > 0) {
                        low = b < m;
                        high = b + m > tens;
                    } else {
                        // m overflowed, so it certainly exceeds both tests.
                        low = true;
                        high = true;
                    }
                    digits[ndigit++] = (char) ('0' + q);
                }
                lowDigitDifference = (b << 1) - tens;
            } else {
                long b = (fractBits * LONG_5_POW[b5]) << b2;
                long s = LONG_5_POW[s5] << s2;
                long m = LONG_5_POW[m5] << m2;
                long tens = s * 10L;
                q = (int) (b / s);
                b = 10L * (b % s);
                m *= 10L;
                low = b < m;
                high = b + m > tens;
                if (q == 0 && !high) {
                    decExp--;
                } else {
                    digits[ndigit++] = (char) ('0' + q);
                }
                if (decExp < -3 || decExp >= 8) {
                    high = false;
                    low = false;
                }
                while (!low && !high) {
                    q = (int) (b / s);
                    b = 10 * (b % s);
                    m *= 10;
                    if (m > 0L) {
                        low = b < m;
                        high = b + m > tens;
                    } else {
                        low = true;
                        high = true;
                    }
                    digits[ndigit++] = (char) ('0' + q);
                }
                lowDigitDifference = (b << 1) - tens;
            }
        } else {
            // OpenJDK normalizes these by a common power of two first to
            // speed its division; that scales every comparison equally, so
            // it is left out. Note the HIGH test here is inclusive (>=),
            // unlike the int and long routes above.
            Big sVal = Big.pow52(s5, s2);
            Big bVal = Big.pow52(0, 0);
            bVal.multiplyLong(fractBits);
            bVal.multiplyPow5(b5);
            bVal.shiftLeft(b2);
            Big mVal = Big.pow52(m5 + 1, m2 + 1);
            Big tenSVal = Big.pow52(s5 + 1, s2 + 1);
            q = bVal.quoRemIteration(sVal);
            low = bVal.compareTo(mVal) < 0;
            high = tenSVal.compareToSum(bVal, mVal) <= 0;
            if (q == 0 && !high) {
                decExp--;
            } else {
                digits[ndigit++] = (char) ('0' + q);
            }
            if (decExp < -3 || decExp >= 8) {
                high = false;
                low = false;
            }
            while (!low && !high) {
                q = bVal.quoRemIteration(sVal);
                mVal.multiplySmall(10);
                low = bVal.compareTo(mVal) < 0;
                high = tenSVal.compareToSum(bVal, mVal) <= 0;
                digits[ndigit++] = (char) ('0' + q);
            }
            if (high && low) {
                bVal.shiftLeft(1);
                lowDigitDifference = bVal.compareTo(tenSVal);
            } else {
                lowDigitDifference = 0L;
            }
        }
        decExponent = decExp + 1;
        first = 0;
        count = ndigit;
        // The last digit is rounded by the stopping condition.
        if (high) {
            if (low) {
                if (lowDigitDifference == 0L) {
                    if ((digits[first + count - 1] & 1) != 0) {
                        roundup();
                    }
                } else if (lowDigitDifference > 0) {
                    roundup();
                }
            } else {
                roundup();
            }
        }
    }

    private void roundup() {
        int i = first + count - 1;
        int q = digits[i];
        if (q == '9') {
            while (q == '9' && i > first) {
                digits[i] = '0';
                q = digits[--i];
            }
            if (q == '9') {
                decExponent += 1;
                digits[first] = '1';
                return;
            }
        }
        digits[i] = (char) (q + 1);
    }

    /// Estimates `floor(log10(value))` from a linear approximation of
    /// `log10` over the mantissa; it may come out one too high, which the
    /// first digit corrects.
    private static int estimateDecExp(long fractBits, int binExp) {
        double d2 = Double.longBitsToDouble(EXP_ONE | (fractBits & SIGNIF_BIT_MASK));
        double d = (d2 - 1.5D) * 0.289529654D + 0.176091259 + (double) binExp * 0.301029995663981;
        long dBits = Double.doubleToLongBits(d);
        int exponent = (int) ((dBits >> EXP_SHIFT) & 0x7ffL) - 1023;
        boolean isNegative = dBits < 0;
        if (exponent >= 0 && exponent < 52) {
            long mask = SIGNIF_BIT_MASK >> exponent;
            int r = (int) (((dBits & SIGNIF_BIT_MASK) | FRACT_HOB) >> (EXP_SHIFT - exponent));
            return isNegative ? (((mask & dBits) == 0L) ? -r : -r - 1) : r;
        } else if (exponent < 0) {
            return ((dBits & ~(1L << 63)) == 0) ? 0 : (isNegative ? -1 : 0);
        } else {
            return (int) d;
        }
    }

    /// A non-negative integer of any size: little-endian 32-bit words held
    /// in an `int[]`, worked on through `long`s. Only the operations the
    /// digit loop needs.
    private static final class Big {
        private static final long MASK = 0xffffffffL;
        private int[] words = new int[8];
        private int length;

        static Big pow52(int p5, int p2) {
            Big b = new Big();
            b.words[0] = 1;
            b.length = 1;
            b.multiplyPow5(p5);
            b.shiftLeft(p2);
            return b;
        }

        private void ensure(int size) {
            if (size > words.length) {
                int[] bigger = new int[Math.max(size, words.length * 2)];
                System.arraycopy(words, 0, bigger, 0, length);
                words = bigger;
            }
        }

        private void trim() {
            while (length > 0 && words[length - 1] == 0) {
                length--;
            }
        }

        /// Multiplies by a non-negative factor below two to the 31st.
        void multiplySmall(int factor) {
            long carry = 0;
            for (int i = 0; i < length; i++) {
                long v = (words[i] & MASK) * factor + carry;
                words[i] = (int) v;
                carry = v >>> 32;
            }
            if (carry != 0) {
                ensure(length + 1);
                words[length++] = (int) carry;
            }
        }

        /// Multiplies by a non-negative long.
        void multiplyLong(long factor) {
            int lowFactor = (int) (factor & 0x7fffffffL);
            long high = factor >>> 31;
            if (high == 0) {
                multiplySmall(lowFactor);
                return;
            }
            Big highPart = copy();
            highPart.multiplyLong(high);
            highPart.shiftLeft(31);
            multiplySmall(lowFactor);
            add(highPart);
        }

        void multiplyPow5(int p5) {
            int left = p5;
            while (left >= 13) {
                multiplySmall(SMALL_5_POW[13]);
                left -= 13;
            }
            if (left > 0) {
                multiplySmall(SMALL_5_POW[left]);
            }
        }

        void shiftLeft(int bits) {
            if (length == 0 || bits == 0) {
                return;
            }
            int wordShift = bits >>> 5;
            int bitShift = bits & 31;
            ensure(length + wordShift + 1);
            if (bitShift == 0) {
                // The extra top word is counted below; a word left over from
                // an earlier, larger value must not survive in it.
                words[length + wordShift] = 0;
                for (int i = length - 1; i >= 0; i--) {
                    words[i + wordShift] = words[i];
                }
            } else {
                words[length + wordShift] = words[length - 1] >>> (32 - bitShift);
                for (int i = length - 1; i > 0; i--) {
                    words[i + wordShift] = (words[i] << bitShift) | (words[i - 1] >>> (32 - bitShift));
                }
                words[wordShift] = words[0] << bitShift;
            }
            for (int i = 0; i < wordShift; i++) {
                words[i] = 0;
            }
            length += wordShift + 1;
            trim();
        }

        private Big copy() {
            Big c = new Big();
            c.ensure(length);
            System.arraycopy(words, 0, c.words, 0, length);
            c.length = length;
            return c;
        }

        void add(Big other) {
            int size = Math.max(length, other.length);
            ensure(size + 1);
            long carry = 0;
            for (int i = 0; i < size; i++) {
                long a = i < length ? (words[i] & MASK) : 0L;
                long b = i < other.length ? (other.words[i] & MASK) : 0L;
                long v = a + b + carry;
                words[i] = (int) v;
                carry = v >>> 32;
            }
            length = size;
            if (carry != 0) {
                words[length++] = (int) carry;
            }
        }

        /// Subtracts a value no greater than this one.
        private void subtract(Big other) {
            long borrow = 0;
            for (int i = 0; i < length; i++) {
                long b = i < other.length ? (other.words[i] & MASK) : 0L;
                long v = (words[i] & MASK) - b - borrow;
                words[i] = (int) v;
                borrow = v < 0 ? 1 : 0;
            }
            trim();
        }

        int compareTo(Big other) {
            if (length != other.length) {
                return length < other.length ? -1 : 1;
            }
            for (int i = length - 1; i >= 0; i--) {
                long a = words[i] & MASK;
                long b = other.words[i] & MASK;
                if (a != b) {
                    return a < b ? -1 : 1;
                }
            }
            return 0;
        }

        /// The sign of this minus `(x + y)`.
        int compareToSum(Big x, Big y) {
            Big sum = x.copy();
            sum.add(y);
            return compareTo(sum);
        }

        /// `q = this / s; this = 10 * (this % s)`, returning `q`. The
        /// quotient is a single decimal digit, so repeated subtraction is
        /// both exact and cheap.
        int quoRemIteration(Big s) {
            int q = 0;
            while (compareTo(s) >= 0) {
                subtract(s);
                q++;
            }
            multiplySmall(10);
            return q;
        }
    }
}
