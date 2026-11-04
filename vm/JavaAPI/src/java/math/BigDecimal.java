/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package java.math;

/// `java.math.BigDecimal`: an exact decimal number, an arbitrary precision
/// unscaled integer and a scale, the value being `unscaled * 10^-scale`.
///
/// This class library has no `java.math.BigInteger`, so the unscaled value
/// is kept as its decimal digits and the arithmetic is schoolbook decimal
/// arithmetic. That is exact and simple, and slow on numbers of hundreds of
/// digits -- this is meant for money and measurements, not for number
/// theory. For the same reason there is no `unscaledValue()`,
/// `toBigInteger()` or `MathContext` here.
public class BigDecimal extends Number implements Comparable<BigDecimal> {

    public static final BigDecimal ZERO = new BigDecimal(0, "0", 0);
    public static final BigDecimal ONE = new BigDecimal(1, "1", 0);
    public static final BigDecimal TEN = new BigDecimal(1, "10", 0);

    public static final int ROUND_UP = 0;
    public static final int ROUND_DOWN = 1;
    public static final int ROUND_CEILING = 2;
    public static final int ROUND_FLOOR = 3;
    public static final int ROUND_HALF_UP = 4;
    public static final int ROUND_HALF_DOWN = 5;
    public static final int ROUND_HALF_EVEN = 6;
    public static final int ROUND_UNNECESSARY = 7;

    /// -1, 0 or 1.
    private final int signum;
    /// The unscaled magnitude: decimal digits, no leading zero, "0" for zero.
    private final String mag;
    private final int scale;

    private BigDecimal(int signum, String mag, int scale) {
        this.signum = "0".equals(mag) ? 0 : signum;
        this.mag = mag;
        this.scale = scale;
    }

    /// Parses `[sign] digits [. digits] [e|E [sign] digits]`.
    public BigDecimal(String val) {
        int len = val.length();
        if (len == 0) {
            throw new NumberFormatException("Empty string");
        }
        int i = 0;
        int sign = 1;
        char c = val.charAt(0);
        if (c == '-' || c == '+') {
            sign = c == '-' ? -1 : 1;
            i++;
        }
        StringBuilder digits = new StringBuilder();
        int fraction = 0;
        boolean dot = false;
        boolean any = false;
        long exp = 0;
        for (; i < len; i++) {
            c = val.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
                any = true;
                if (dot) {
                    fraction++;
                }
            } else if (c == '.' && !dot) {
                dot = true;
            } else if (c == 'e' || c == 'E') {
                i++;
                if (i >= len) {
                    throw new NumberFormatException("No exponent digits");
                }
                boolean neg = false;
                c = val.charAt(i);
                if (c == '-' || c == '+') {
                    neg = c == '-';
                    i++;
                }
                if (i >= len) {
                    throw new NumberFormatException("No exponent digits");
                }
                for (; i < len; i++) {
                    c = val.charAt(i);
                    if (c < '0' || c > '9') {
                        throw new NumberFormatException("Bad exponent: " + val);
                    }
                    exp = exp * 10 + (c - '0');
                    if (exp > Integer.MAX_VALUE) {
                        throw new NumberFormatException("Exponent overflow");
                    }
                }
                if (neg) {
                    exp = -exp;
                }
                break;
            } else {
                throw new NumberFormatException("Character " + c + " is neither a decimal digit number,"
                        + " decimal point, nor \"e\" notation exponential mark.");
            }
        }
        if (!any) {
            throw new NumberFormatException("No digits: " + val);
        }
        long s = fraction - exp;
        if (s > Integer.MAX_VALUE || s < Integer.MIN_VALUE) {
            throw new NumberFormatException("Scale out of range");
        }
        String m = trim(digits.toString());
        this.signum = "0".equals(m) ? 0 : sign;
        this.mag = m;
        this.scale = (int) s;
    }

    public BigDecimal(char[] in) {
        this(new String(in));
    }

    public BigDecimal(int val) {
        this((long) val);
    }

    public BigDecimal(long val) {
        this.signum = val > 0 ? 1 : (val < 0 ? -1 : 0);
        this.mag = magOf(val);
        this.scale = 0;
    }

    /// The exact binary value of `val`, as the JDK's constructor gives it:
    /// `new BigDecimal(0.1)` is 0.1000000000000000055511151231257827...
    public BigDecimal(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) {
            throw new NumberFormatException("Infinite or NaN");
        }
        long bits = Double.doubleToLongBits(val);
        int sign = (bits >> 63) == 0 ? 1 : -1;
        int exponent = (int) ((bits >> 52) & 0x7ffL);
        long significand = exponent == 0 ? (bits & ((1L << 52) - 1)) << 1
                : (bits & ((1L << 52) - 1)) | (1L << 52);
        exponent -= 1075;
        if (significand == 0) {
            this.signum = 0;
            this.mag = "0";
            this.scale = 0;
            return;
        }
        while ((significand & 1) == 0) {
            significand >>= 1;
            exponent++;
        }
        String m = magOf(significand);
        int s = 0;
        if (exponent >= 0) {
            m = mul(m, pow(2, exponent));
        } else {
            // m / 2^k == m * 5^k / 10^k
            m = mul(m, pow(5, -exponent));
            s = -exponent;
        }
        this.signum = sign;
        this.mag = m;
        this.scale = s;
    }

    public static BigDecimal valueOf(long val) {
        if (val == 0) {
            return ZERO;
        }
        return new BigDecimal(val);
    }

    public static BigDecimal valueOf(long unscaledVal, int scale) {
        BigDecimal v = new BigDecimal(unscaledVal);
        return new BigDecimal(v.signum, v.mag, scale);
    }

    /// The value `Double.toString(val)` spells, as the JDK does.
    public static BigDecimal valueOf(double val) {
        return new BigDecimal(Double.toString(val));
    }

    public int signum() {
        return signum;
    }

    public int scale() {
        return scale;
    }

    /// The number of digits in the unscaled value.
    public int precision() {
        return mag.length();
    }

    public BigDecimal negate() {
        return new BigDecimal(-signum, mag, scale);
    }

    public BigDecimal abs() {
        return signum < 0 ? negate() : this;
    }

    public BigDecimal plus() {
        return this;
    }

    public BigDecimal add(BigDecimal augend) {
        int s = Math.max(scale, augend.scale);
        String a = shift(mag, s - scale);
        String b = shift(augend.mag, s - augend.scale);
        return addSigned(signum, a, augend.signum, b, s);
    }

    public BigDecimal subtract(BigDecimal subtrahend) {
        return add(subtrahend.negate());
    }

    public BigDecimal multiply(BigDecimal multiplicand) {
        return new BigDecimal(signum * multiplicand.signum, mul(mag, multiplicand.mag),
                checkScale((long) scale + multiplicand.scale));
    }

    /// The exact quotient; throws `ArithmeticException` when its decimal
    /// expansion does not terminate. The scale is the smallest that holds
    /// the result, but not below `this.scale() - divisor.scale()`.
    public BigDecimal divide(BigDecimal divisor) {
        if (divisor.signum == 0) {
            throw new ArithmeticException(signum == 0 ? "Division undefined" : "Division by zero");
        }
        int preferred = checkScale((long) scale - divisor.scale);
        if (signum == 0) {
            return new BigDecimal(0, "0", preferred);
        }
        // A terminating quotient a/b needs at most as many extra digits as
        // the largest power of 2 or 5 dividing b, under 4 per digit of b.
        int extra = 4 * divisor.mag.length() + 4;
        String[] qr = divmod(shift(mag, extra), divisor.mag);
        if (!"0".equals(qr[1])) {
            throw new ArithmeticException("Non-terminating decimal expansion; no exact representable decimal result.");
        }
        String q = qr[0];
        int s = checkScale((long) scale - divisor.scale + extra);
        while (s > preferred && q.length() > 1 && q.charAt(q.length() - 1) == '0') {
            q = q.substring(0, q.length() - 1);
            s--;
        }
        return new BigDecimal(signum * divisor.signum, q, s);
    }

    public BigDecimal divide(BigDecimal divisor, RoundingMode roundingMode) {
        return divide(divisor, scale, roundingMode);
    }

    public BigDecimal divide(BigDecimal divisor, int scale, RoundingMode roundingMode) {
        if (divisor.signum == 0) {
            throw new ArithmeticException(signum == 0 ? "Division undefined" : "Division by zero");
        }
        // unscaled result = a * 10^(scale - this.scale + divisor.scale) / b
        long k = (long) scale - this.scale + divisor.scale;
        String a = mag;
        String b = divisor.mag;
        if (k >= 0) {
            a = shift(a, checkScale(k));
        } else {
            b = shift(b, checkScale(-k));
        }
        String[] qr = divmod(a, b);
        int sign = signum * divisor.signum;
        return new BigDecimal(sign, round(sign, qr[0], qr[1], b, roundingMode), scale);
    }

    public BigDecimal divide(BigDecimal divisor, int scale, int roundingMode) {
        return divide(divisor, scale, RoundingMode.valueOf(roundingMode));
    }

    public BigDecimal divide(BigDecimal divisor, int roundingMode) {
        return divide(divisor, scale, RoundingMode.valueOf(roundingMode));
    }

    /// The integer part of `this / divisor`, at the preferred scale.
    public BigDecimal divideToIntegralValue(BigDecimal divisor) {
        BigDecimal q = divide(divisor, 0, RoundingMode.DOWN);
        int preferred = checkScale((long) scale - divisor.scale);
        return preferred > 0 ? q.setScale(preferred) : q;
    }

    /// `this - this.divideToIntegralValue(divisor) * divisor`.
    public BigDecimal remainder(BigDecimal divisor) {
        return subtract(divide(divisor, 0, RoundingMode.DOWN).multiply(divisor));
    }

    public BigDecimal setScale(int newScale, RoundingMode roundingMode) {
        if (newScale == scale) {
            return this;
        }
        if (newScale > scale) {
            return new BigDecimal(signum, shift(mag, newScale - scale), newScale);
        }
        String div = shift("1", checkScale((long) scale - newScale));
        String[] qr = divmod(mag, div);
        return new BigDecimal(signum, round(signum, qr[0], qr[1], div, roundingMode), newScale);
    }

    public BigDecimal setScale(int newScale, int roundingMode) {
        return setScale(newScale, RoundingMode.valueOf(roundingMode));
    }

    /// The same value at `newScale`; throws `ArithmeticException` when that
    /// would need rounding.
    public BigDecimal setScale(int newScale) {
        return setScale(newScale, RoundingMode.UNNECESSARY);
    }

    public BigDecimal movePointLeft(int n) {
        BigDecimal r = new BigDecimal(signum, mag, checkScale((long) scale + n));
        return r.scale < 0 ? r.setScale(0) : r;
    }

    public BigDecimal movePointRight(int n) {
        BigDecimal r = new BigDecimal(signum, mag, checkScale((long) scale - n));
        return r.scale < 0 ? r.setScale(0) : r;
    }

    public BigDecimal scaleByPowerOfTen(int n) {
        return new BigDecimal(signum, mag, checkScale((long) scale - n));
    }

    /// The same value with no trailing zeros in the unscaled value; zero is
    /// [#ZERO].
    public BigDecimal stripTrailingZeros() {
        if (signum == 0) {
            return ZERO;
        }
        int end = mag.length();
        while (end > 1 && mag.charAt(end - 1) == '0') {
            end--;
        }
        int removed = mag.length() - end;
        return removed == 0 ? this : new BigDecimal(signum, mag.substring(0, end), checkScale((long) scale - removed));
    }

    public BigDecimal pow(int n) {
        if (n < 0 || n > 999999999) {
            throw new ArithmeticException("Invalid operation");
        }
        BigDecimal r = ONE;
        for (int i = 0; i < n; i++) {
            r = r.multiply(this);
        }
        return r;
    }

    public BigDecimal min(BigDecimal val) {
        return compareTo(val) <= 0 ? this : val;
    }

    public BigDecimal max(BigDecimal val) {
        return compareTo(val) >= 0 ? this : val;
    }

    /// Numeric order: `2.0` and `2.00` compare equal.
    @Override
    public int compareTo(BigDecimal val) {
        if (signum != val.signum) {
            return signum < val.signum ? -1 : 1;
        }
        if (signum == 0) {
            return 0;
        }
        int s = Math.max(scale, val.scale);
        int c = cmp(shift(mag, s - scale), shift(val.mag, s - val.scale));
        return signum > 0 ? c : -c;
    }

    /// Equal in value AND scale: `2.0` does not equal `2.00`.
    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        }
        if (!(x instanceof BigDecimal)) {
            return false;
        }
        BigDecimal o = (BigDecimal) x;
        return scale == o.scale && signum == o.signum && mag.equals(o.mag);
    }

    /// The JDK's hash, `31 * unscaledValue().hashCode() + scale()`, where a
    /// `BigInteger` hashes its big-endian 32-bit words.
    @Override
    public int hashCode() {
        int h = 0;
        if (signum != 0) {
            // Little-endian 32-bit words of the magnitude.
            int[] words = new int[mag.length() / 9 + 2];
            int n = 0;
            String m = mag;
            while (!"0".equals(m)) {
                long[] rem = new long[1];
                m = divSmall(m, 1L << 32, rem);
                words[n++] = (int) rem[0];
            }
            for (int i = n - 1; i >= 0; i--) {
                h = (int) (31 * h + (words[i] & 0xffffffffL));
            }
            h *= signum;
        }
        return 31 * h + scale;
    }

    /// The JDK's text: plain when the scale is not negative and the
    /// adjusted exponent is at least -6, scientific otherwise.
    @Override
    public String toString() {
        if (scale == 0) {
            return signum < 0 ? "-" + mag : mag;
        }
        long adjusted = -(long) scale + (mag.length() - 1);
        if (scale > 0 && adjusted >= -6) {
            return toPlainString();
        }
        StringBuilder sb = new StringBuilder();
        if (signum < 0) {
            sb.append('-');
        }
        sb.append(mag.charAt(0));
        if (mag.length() > 1) {
            sb.append('.').append(mag, 1, mag.length());
        }
        if (adjusted != 0) {
            sb.append('E');
            if (adjusted > 0) {
                sb.append('+');
            }
            sb.append(adjusted);
        }
        return sb.toString();
    }

    /// The JDK's engineering notation is not provided; this is [#toString].
    public String toEngineeringString() {
        return toString();
    }

    /// The value with no exponent.
    public String toPlainString() {
        StringBuilder sb = new StringBuilder();
        if (signum < 0) {
            sb.append('-');
        }
        if (scale <= 0) {
            sb.append(mag);
            if (signum != 0) {
                for (int i = 0; i < -scale; i++) {
                    sb.append('0');
                }
            } else if (scale < 0) {
                // The JDK prints 0E+3 plainly as "0".
                return "0";
            }
            return sb.toString();
        }
        int len = mag.length();
        if (len > scale) {
            sb.append(mag, 0, len - scale).append('.').append(mag, len - scale, len);
        } else {
            sb.append("0.");
            for (int i = len; i < scale; i++) {
                sb.append('0');
            }
            sb.append(mag);
        }
        return sb.toString();
    }

    /// The integer part, wrapped to 64 bits as the JDK's `longValue` does.
    @Override
    public long longValue() {
        if (signum == 0) {
            return 0;
        }
        int end = mag.length() - Math.max(scale, 0);
        long v = 0;
        for (int i = 0; i < end; i++) {
            v = v * 10 + (mag.charAt(i) - '0');
        }
        if (scale < 0) {
            for (int i = 0; i < -scale && v != 0; i++) {
                v *= 10;
            }
        }
        return signum < 0 ? -v : v;
    }

    /// The integer part; `ArithmeticException` if a fraction is discarded or
    /// it does not fit in a long.
    public long longValueExact() {
        BigDecimal i = setScale(0, RoundingMode.UNNECESSARY);
        if (i.mag.length() > 19 || (i.mag.length() == 19
                && cmp(i.mag, signum < 0 ? "9223372036854775808" : "9223372036854775807") > 0)) {
            throw new ArithmeticException("Overflow");
        }
        return i.longValue();
    }

    public int intValueExact() {
        long v = longValueExact();
        if (v != (int) v) {
            throw new ArithmeticException("Overflow");
        }
        return (int) v;
    }

    @Override
    public int intValue() {
        return (int) longValue();
    }

    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    @Override
    public double doubleValue() {
        return Double.parseDouble(toString());
    }

    // ---- decimal magnitude arithmetic -------------------------------------

    private static int checkScale(long s) {
        if (s > Integer.MAX_VALUE || s < Integer.MIN_VALUE) {
            throw new ArithmeticException("Scale out of range");
        }
        return (int) s;
    }

    private static String magOf(long v) {
        if (v == Long.MIN_VALUE) {
            return "9223372036854775808";
        }
        return Long.toString(v < 0 ? -v : v);
    }

    private static String trim(String s) {
        int i = 0;
        while (i < s.length() - 1 && s.charAt(i) == '0') {
            i++;
        }
        return i == 0 ? s : s.substring(i);
    }

    /// `m * 10^n`.
    private static String shift(String m, int n) {
        if (n == 0 || "0".equals(m)) {
            return m;
        }
        StringBuilder sb = new StringBuilder(m.length() + n).append(m);
        for (int i = 0; i < n; i++) {
            sb.append('0');
        }
        return sb.toString();
    }

    private static int cmp(String a, String b) {
        if (a.length() != b.length()) {
            return a.length() < b.length() ? -1 : 1;
        }
        int c = a.compareTo(b);
        return c < 0 ? -1 : (c > 0 ? 1 : 0);
    }

    private static String add(String a, String b) {
        StringBuilder sb = new StringBuilder(Math.max(a.length(), b.length()) + 1);
        int i = a.length() - 1;
        int j = b.length() - 1;
        int carry = 0;
        while (i >= 0 || j >= 0 || carry != 0) {
            int d = carry + (i >= 0 ? a.charAt(i--) - '0' : 0) + (j >= 0 ? b.charAt(j--) - '0' : 0);
            sb.append((char) ('0' + d % 10));
            carry = d / 10;
        }
        return trim(sb.reverse().toString());
    }

    /// `a - b` for `a >= b`.
    private static String sub(String a, String b) {
        StringBuilder sb = new StringBuilder(a.length());
        int i = a.length() - 1;
        int j = b.length() - 1;
        int borrow = 0;
        while (i >= 0) {
            int d = (a.charAt(i--) - '0') - borrow - (j >= 0 ? b.charAt(j--) - '0' : 0);
            if (d < 0) {
                d += 10;
                borrow = 1;
            } else {
                borrow = 0;
            }
            sb.append((char) ('0' + d));
        }
        return trim(sb.reverse().toString());
    }

    private static BigDecimal addSigned(int sa, String a, int sb, String b, int scale) {
        if (sa == 0) {
            return new BigDecimal(sb, b, scale);
        }
        if (sb == 0) {
            return new BigDecimal(sa, a, scale);
        }
        if (sa == sb) {
            return new BigDecimal(sa, add(a, b), scale);
        }
        int c = cmp(a, b);
        if (c == 0) {
            return new BigDecimal(0, "0", scale);
        }
        return c > 0 ? new BigDecimal(sa, sub(a, b), scale) : new BigDecimal(sb, sub(b, a), scale);
    }

    private static String mul(String a, String b) {
        if ("0".equals(a) || "0".equals(b)) {
            return "0";
        }
        int la = a.length();
        int lb = b.length();
        int[] r = new int[la + lb];
        for (int i = la - 1; i >= 0; i--) {
            int da = a.charAt(i) - '0';
            if (da == 0) {
                continue;
            }
            for (int j = lb - 1; j >= 0; j--) {
                r[i + j + 1] += da * (b.charAt(j) - '0');
            }
            // Normalise this row so no cell can overflow on large inputs.
            for (int k = i + lb; k > i; k--) {
                if (r[k] >= 10) {
                    r[k - 1] += r[k] / 10;
                    r[k] %= 10;
                }
            }
        }
        StringBuilder sb = new StringBuilder(r.length);
        for (int k = 0; k < r.length; k++) {
            sb.append((char) ('0' + r[k]));
        }
        return trim(sb.toString());
    }

    private static String pow(int base, int n) {
        String r = "1";
        String b = Integer.toString(base);
        while (n > 0) {
            if ((n & 1) != 0) {
                r = mul(r, b);
            }
            n >>= 1;
            if (n > 0) {
                b = mul(b, b);
            }
        }
        return r;
    }

    /// `m / d` for a small positive `d` (at most 2^32); the remainder goes
    /// into `rem[0]`.
    private static String divSmall(String m, long d, long[] rem) {
        StringBuilder q = new StringBuilder(m.length());
        long r = 0;
        for (int i = 0; i < m.length(); i++) {
            r = r * 10 + (m.charAt(i) - '0');
            q.append((char) ('0' + (r / d)));
            r %= d;
        }
        rem[0] = r;
        return trim(q.toString());
    }

    /// `{a / b, a % b}` by long division, one decimal digit at a time.
    private static String[] divmod(String a, String b) {
        if (cmp(a, b) < 0) {
            return new String[] {"0", a};
        }
        StringBuilder q = new StringBuilder(a.length());
        String r = "0";
        for (int i = 0; i < a.length(); i++) {
            r = "0".equals(r) ? String.valueOf(a.charAt(i)) : r + a.charAt(i);
            r = trim(r);
            int digit = 0;
            while (cmp(r, b) >= 0) {
                r = sub(r, b);
                digit++;
            }
            q.append((char) ('0' + digit));
        }
        return new String[] {trim(q.toString()), r};
    }

    /// The magnitude `q`, rounded by `mode` given the discarded remainder
    /// `rem` of a division by `div`, for a result of sign `sign`.
    private static String round(int sign, String q, String rem, String div, RoundingMode mode) {
        if ("0".equals(rem)) {
            return q;
        }
        boolean up;
        switch (mode) {
            case UP:
                up = true;
                break;
            case DOWN:
                up = false;
                break;
            case CEILING:
                up = sign > 0;
                break;
            case FLOOR:
                up = sign < 0;
                break;
            case UNNECESSARY:
                throw new ArithmeticException("Rounding necessary");
            default:
                int c = cmp(add(rem, rem), div);
                if (mode == RoundingMode.HALF_UP) {
                    up = c >= 0;
                } else if (mode == RoundingMode.HALF_DOWN) {
                    up = c > 0;
                } else {
                    up = c > 0 || (c == 0 && ((q.charAt(q.length() - 1) - '0') & 1) != 0);
                }
                break;
        }
        return up ? add(q, "1") : q;
    }
}
