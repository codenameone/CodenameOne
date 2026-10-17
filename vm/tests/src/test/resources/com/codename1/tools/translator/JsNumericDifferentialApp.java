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

/// A differential fixture: the same arithmetic on every primitive type, every
/// conversion between them, the stack shapes javac writes for compound
/// assignments on fields and arrays of each width, and the handful of `Math`
/// natives layout code leans on, over a few thousand generated operands. Each
/// section folds its results into a hash and prints it, so a target that
/// disagrees with the JVM names the section; `result` folds the sections.
///
/// It exists because a float result that is not rounded, a double result that
/// is, or a two-slot value shuffled as if it had one are all silent: nothing
/// throws, and a layout lands a pixel away from where every other target puts
/// it. The expected values are what a JVM prints for this source.
public class JsNumericDifferentialApp {
    public static int result;

    static int seed;
    static long sl;
    static double sd;
    static float sf;
    static int si;

    int fi;
    long fl;
    float ff;
    double fd;
    short fs;
    char fc;
    byte fb;

    static final double[] DOUBLES = {
        0.0, -0.0, 0.1, -0.1, 0.5, -0.5, 1.5, -1.5, 2.5, -2.5, 1.0e10, -1.0e10, 3.0e9, -3.0e9,
        1.0e19, -1.0e19, 1.0e39, -1.0e39, 16777217.0, 9007199254740993.0, 2147483647.0, 2147483648.0,
        -2147483648.0, -2147483649.0, 9.223372036854775807e18, 4.9e-324, 1.4e-45, 1.0e-46, 3.4028235e38,
        3.4028236e38, 1.7976931348623157e308, 0.1 + 0.2, 1.0 / 3.0, 22.999999999, 23.0000001,
        Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
    };

    // No multiply: the generator must not depend on what it is testing.
    static int next() {
        int x = seed;
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        seed = x;
        return x;
    }

    static long nextLong() {
        return ((long) next() << 32) ^ (next() & 0xffffffffL);
    }

    /// Floats of every magnitude, built from bits so nothing rounds on the way in.
    static float nextFloat() {
        int kind = next() & 7;
        if (kind == 0) {
            return (float) (next() >> 20);
        }
        if (kind == 1) {
            return (next() >> 8) / 1024f;
        }
        int bits = next();
        int exponent = ((bits >>> 23) & 0xff);
        if (exponent == 0xff) {
            bits &= 0xbfffffff;
        }
        return Float.intBitsToFloat(bits);
    }

    static double nextDouble() {
        int kind = next() & 7;
        if (kind == 0) {
            return (double) (next() >> 16);
        }
        if (kind == 1) {
            return (next() >> 4) / 4096.0;
        }
        if (kind == 2) {
            return (double) nextFloat();
        }
        long bits = nextLong();
        long exponent = (bits >>> 52) & 0x7ffL;
        if (exponent == 0x7ffL) {
            bits &= 0xbfffffffffffffffL;
        }
        return Double.longBitsToDouble(bits);
    }

    static int mixI(int hash, int value) {
        return hash * 31 + value;
    }

    static int mixL(int hash, long value) {
        return (hash * 31 + (int) value) * 31 + (int) (value >>> 32);
    }

    static int mixF(int hash, float value) {
        hash = mixI(hash, Float.floatToIntBits(value));
        return mixL(hash, Double.doubleToLongBits((double) value));
    }

    static int mixD(int hash, double value) {
        return mixL(hash, Double.doubleToLongBits(value));
    }

    static int mixZ(int hash, boolean value) {
        return hash * 31 + (value ? 1231 : 1237);
    }

    static int conversions(int hash) {
        for (int i = 0; i < DOUBLES.length; i++) {
            double d = DOUBLES[i];
            float f = (float) d;
            hash = mixF(hash, f);
            hash = mixI(hash, (int) d);
            hash = mixL(hash, (long) d);
            hash = mixI(hash, (int) f);
            hash = mixL(hash, (long) f);
            hash = mixD(hash, (double) f);
            hash = mixI(hash, (short) d);
            hash = mixI(hash, (byte) f);
            hash = mixI(hash, (char) d);
            long l = (long) d;
            hash = mixF(hash, (float) l);
            hash = mixD(hash, (double) l);
            hash = mixI(hash, (int) l);
            int n = (int) d;
            hash = mixF(hash, (float) n);
            hash = mixD(hash, (double) n);
            hash = mixL(hash, (long) n);
            hash = mixI(hash, (byte) n);
            hash = mixI(hash, (short) n);
            hash = mixI(hash, (char) n);
        }
        for (int i = 0; i < 400; i++) {
            long l = nextLong() >> (next() & 63);
            int n = next() >> (next() & 31);
            float f = nextFloat();
            double d = nextDouble();
            hash = mixF(hash, (float) l);
            hash = mixD(hash, (double) l);
            hash = mixF(hash, (float) n);
            hash = mixD(hash, (double) n);
            hash = mixI(hash, (int) f);
            hash = mixL(hash, (long) f);
            hash = mixI(hash, (int) d);
            hash = mixL(hash, (long) d);
            hash = mixF(hash, (float) d);
            hash = mixD(hash, (double) f);
            hash = mixI(hash, (int) (f * 10));
            hash = mixI(hash, (int) (d * 10));
            hash = mixL(hash, (long) (f / 3));
        }
        return hash;
    }

    static int arithmetic(int hash) {
        for (int i = 0; i < 600; i++) {
            int a = next() >> (next() & 31);
            int b = next() >> (next() & 31);
            long la = nextLong() >> (next() & 63);
            long lb = nextLong() >> (next() & 63);
            float fa = nextFloat();
            float fb = nextFloat();
            double da = nextDouble();
            double db = nextDouble();

            hash = mixI(hash, a + b);
            hash = mixI(hash, a - b);
            hash = mixI(hash, a * b);
            hash = mixI(hash, -a);
            if (b != 0) {
                hash = mixI(hash, a / b);
                hash = mixI(hash, a % b);
            }
            hash = mixI(hash, a << b);
            hash = mixI(hash, a >> b);
            hash = mixI(hash, a >>> b);
            hash = mixI(hash, (a & b) ^ (a | b));

            hash = mixL(hash, la + lb);
            hash = mixL(hash, la - lb);
            hash = mixL(hash, la * lb);
            hash = mixL(hash, -la);
            if (lb != 0) {
                hash = mixL(hash, la / lb);
                hash = mixL(hash, la % lb);
            }
            hash = mixL(hash, la << b);
            hash = mixL(hash, la >> b);
            hash = mixL(hash, la >>> b);
            hash = mixL(hash, (la & lb) ^ (la | lb));
            hash = mixI(hash, la < lb ? 1 : la > lb ? 2 : 3);

            hash = mixF(hash, fa + fb);
            hash = mixF(hash, fa - fb);
            hash = mixF(hash, fa * fb);
            hash = mixF(hash, fa / fb);
            hash = mixF(hash, fa % fb);
            hash = mixF(hash, -fa);
            hash = mixF(hash, fa * fb + fa);
            hash = mixF(hash, (fa + fb) * (fa - fb));
            hash = mixZ(hash, fa < fb);
            hash = mixZ(hash, fa > fb);
            hash = mixZ(hash, fa <= fb);
            hash = mixZ(hash, fa >= fb);
            hash = mixZ(hash, fa == fb);
            hash = mixZ(hash, fa != fb);

            hash = mixD(hash, da + db);
            hash = mixD(hash, da - db);
            hash = mixD(hash, da * db);
            hash = mixD(hash, da / db);
            hash = mixD(hash, da % db);
            hash = mixD(hash, -da);
            hash = mixD(hash, da * db + da);
            hash = mixZ(hash, da < db);
            hash = mixZ(hash, da > db);
            hash = mixZ(hash, da <= db);
            hash = mixZ(hash, da >= db);
            hash = mixZ(hash, da == db);
            hash = mixZ(hash, da != db);

            // Binary numeric promotion: the wider operand decides the type of the result.
            hash = mixF(hash, a * fa);
            hash = mixF(hash, la + fa);
            hash = mixD(hash, fa * da);
            hash = mixD(hash, la / db);
            hash = mixD(hash, a - da);
            hash = mixD(hash, fa * 3.141592653589793 / 180.0);
            hash = mixF(hash, (float) (fa * 3.141592653589793 / 180.0));
            hash = mixL(hash, a + la);
            hash = mixD(hash, (double) fa * (double) fb);
            hash = mixF(hash, (float) ((double) fa * (double) fb));
        }
        return hash;
    }

    static int id(int v) {
        seed ^= 1;
        return v;
    }

    static long id(long v) {
        seed ^= 2;
        return v;
    }

    static float id(float v) {
        seed ^= 4;
        return v;
    }

    static double id(double v) {
        seed ^= 8;
        return v;
    }

    static int mixed(int a, long b, float c, double d, int e, double f, long g, float h) {
        int hash = mixI(7, a);
        hash = mixL(hash, b);
        hash = mixF(hash, c);
        hash = mixD(hash, d);
        hash = mixI(hash, e);
        hash = mixD(hash, f);
        hash = mixL(hash, g);
        return mixF(hash, h);
    }

    /// The shapes javac writes with DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1 and DUP2_X2:
    /// an assignment or an increment whose value is used, on a local, a static, a
    /// field and an array element, for one-slot and two-slot types alike.
    static int shuffles(int hash) {
        JsNumericDifferentialApp o = new JsNumericDifferentialApp();
        JsNumericDifferentialApp p = new JsNumericDifferentialApp();
        int[] ia = new int[4];
        long[] la = new long[4];
        float[] fa = new float[4];
        double[] da = new double[4];
        short[] sa = new short[4];
        char[] ca = new char[4];
        byte[] ba = new byte[4];
        for (int i = 0; i < 300; i++) {
            int k = next() & 3;
            int j = next() & 3;
            int n = next() >> 12;
            long l = nextLong() >> 20;
            float f = (next() >> 10) / 64f;
            double d = (next() >> 6) / 4096.0;

            int i1;
            int i2 = i1 = n;
            long l1;
            long l2 = l1 = l;
            float f1;
            float f2 = f1 = f;
            double d1;
            double d2 = d1 = d;
            hash = mixL(mixI(mixI(hash, i1), i2), l1 + l2);
            hash = mixD(mixF(mixF(hash, f1), f2), d1 + d2);

            hash = mixI(hash, si = n);
            hash = mixL(hash, sl = l);
            hash = mixF(hash, sf = f);
            hash = mixD(hash, sd = d);
            hash = mixI(hash, si++);
            hash = mixI(hash, ++si);
            hash = mixL(hash, sl++);
            hash = mixL(hash, --sl);
            hash = mixF(hash, sf++);
            hash = mixF(hash, sf += f);
            hash = mixD(hash, sd--);
            hash = mixD(hash, sd *= 1.5);
            hash = mixI(hash, si += f);
            hash = mixL(hash, sl += d);
            hash = mixF(hash, sf *= d);

            hash = mixI(hash, o.fi = p.fi = n);
            hash = mixL(hash, o.fl = p.fl = l);
            hash = mixF(hash, o.ff = p.ff = f);
            hash = mixD(hash, o.fd = p.fd = d);
            hash = mixI(hash, o.fi++);
            hash = mixI(hash, --o.fi);
            hash = mixL(hash, o.fl++);
            hash = mixL(hash, ++o.fl);
            hash = mixL(hash, o.fl += n);
            hash = mixF(hash, o.ff++);
            hash = mixF(hash, o.ff /= 3);
            hash = mixD(hash, o.fd++);
            hash = mixD(hash, o.fd -= f);
            hash = mixI(hash, o.fs += n);
            hash = mixI(hash, o.fs++);
            hash = mixI(hash, o.fc += n);
            hash = mixI(hash, o.fc--);
            hash = mixI(hash, o.fb *= 3);
            hash = mixI(hash, o.fi *= 1.5f);
            hash = mixI(hash, o.fi >>= 1);
            hash = mixL(hash, o.fl <<= n);
            hash = mixL(hash, o.fl >>>= 3);
            hash = mixL(mixL(hash, p.fl), o.fl);
            hash = mixD(mixF(hash, p.ff), p.fd);

            hash = mixI(hash, ia[k] = ia[j] = n);
            hash = mixL(hash, la[k] = la[j] = l);
            hash = mixF(hash, fa[k] = fa[j] = f);
            hash = mixD(hash, da[k] = da[j] = d);
            hash = mixI(hash, ia[k]++);
            hash = mixI(hash, ++ia[j]);
            hash = mixI(hash, ia[k] += l);
            hash = mixL(hash, la[k]++);
            hash = mixL(hash, --la[j]);
            hash = mixL(hash, la[k] += f);
            hash = mixL(hash, la[j] ^= l);
            hash = mixF(hash, fa[k]++);
            hash = mixF(hash, fa[j] += d);
            hash = mixF(hash, fa[k] *= 0.1f);
            hash = mixD(hash, da[k]++);
            hash = mixD(hash, da[j] /= 3);
            hash = mixD(hash, da[k] += fa[j]);
            hash = mixI(hash, sa[k] += n);
            hash = mixI(hash, sa[j]++);
            hash = mixI(hash, ca[k] += n);
            hash = mixI(hash, ca[j]--);
            hash = mixI(hash, ba[k] += n);
            hash = mixI(hash, ba[j] *= 0.7);
            for (int q = 0; q < 4; q++) {
                hash = mixL(mixI(hash, ia[q]), la[q]);
                hash = mixD(mixF(hash, fa[q]), da[q]);
                hash = mixI(mixI(mixI(hash, sa[q]), ca[q]), ba[q]);
            }

            // Results of calls carried into the instruction that uses them.
            hash = mixZ(hash, id(f) > id(d));
            hash = mixZ(hash, id(d) < id(l));
            hash = mixZ(hash, id(l) >= id(n));
            hash = mixZ(hash, id(f) == id(f1));
            hash = mixI(hash, id(n) + (int) id(l) + (int) id(f) + (int) id(d));
            hash = mixD(hash, id(n) * id(d) + id(f) * id(l));
            hash = mixF(hash, id(f) * id(n) + id(f));
            hash = mixI(hash, mixed(id(n), id(l), id(f), id(d), n, d, l, f));
            hash = mixL(hash, id(n) > 0 ? id(l) : -id(l));
            hash = mixD(hash, id(f) > 0 ? id(d) : id(f));
            hash = mixI(hash, seed);
        }
        return hash;
    }

    static int arrays(int hash) {
        double[][] dd = new double[3][4];
        long[][][] lll = new long[2][3][2];
        float[][] ff = new float[2][];
        int[][] ii = new int[4][1];
        ff[0] = new float[3];
        ff[1] = new float[1];
        hash = mixI(mixI(mixI(hash, dd.length), dd[2].length), lll.length);
        hash = mixI(mixI(mixI(hash, lll[1].length), lll[1][2].length), ii[3].length);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 4; c++) {
                dd[r][c] = nextDouble();
                dd[r][c] += r * 0.1;
                hash = mixD(hash, dd[r][c]);
            }
        }
        for (int a = 0; a < 2; a++) {
            for (int b = 0; b < 3; b++) {
                for (int c = 0; c < 2; c++) {
                    lll[a][b][c] = nextLong();
                    hash = mixL(hash, lll[a][b][c]++);
                    hash = mixL(hash, lll[a][b][c]);
                }
            }
        }
        for (int a = 0; a < 2; a++) {
            for (int b = 0; b < ff[a].length; b++) {
                // A float array keeps a float: the store rounds a double it is handed.
                ff[a][b] = (float) (nextDouble() * 0.1);
                hash = mixF(hash, ff[a][b]);
                ff[a][b] *= 1.1f;
                hash = mixF(hash, ff[a][b]);
            }
        }
        float[] copy = new float[4];
        System.arraycopy(ff[0], 0, copy, 1, 3);
        for (int a = 0; a < 4; a++) {
            hash = mixF(hash, copy[a]);
        }
        return hash;
    }

    static int natives(int hash) {
        for (int i = 0; i < 400; i++) {
            float fa = i < DOUBLES.length ? (float) DOUBLES[i] : nextFloat();
            float fb = nextFloat();
            double da = i < DOUBLES.length ? DOUBLES[i] : nextDouble();
            double db = nextDouble();
            int a = next() >> (next() & 31);
            int b = next();
            long la = nextLong() >> (next() & 63);
            long lb = nextLong();
            hash = mixD(hash, Math.sqrt(da));
            hash = mixF(hash, (float) Math.sqrt(fa));
            hash = mixD(hash, Math.floor(da));
            hash = mixD(hash, Math.ceil(da));
            hash = mixD(hash, Math.floor(fa));
            hash = mixD(hash, Math.abs(da));
            hash = mixF(hash, Math.abs(fa));
            hash = mixI(hash, Math.abs(a));
            hash = mixL(hash, Math.abs(la));
            hash = mixD(hash, Math.min(da, db));
            hash = mixD(hash, Math.max(da, db));
            hash = mixF(hash, Math.min(fa, fb));
            hash = mixF(hash, Math.max(fa, fb));
            hash = mixF(hash, Math.min(fa, (float) da));
            hash = mixD(hash, Math.max(da, fa));
            hash = mixI(hash, Math.min(a, b));
            hash = mixI(hash, Math.max(a, b));
            hash = mixL(hash, Math.min(la, lb));
            hash = mixL(hash, Math.max(la, a));
            // Not Math.round: the class library defines it as floor(x + 0.5) in
            // the argument's own type, which is not what a current JDK computes
            // for 0.49999997f or for an odd float above 2^23. And no bit
            // pattern is built from a NaN: floatToIntBits hands back the raw
            // bits on every Codename One target, where a JDK folds every NaN
            // into one. An infinity is left out for the same reason: flipping
            // its low bit makes a NaN.
            hash = mixI(hash, Float.floatToIntBits(fa));
            hash = mixL(hash, Double.doubleToLongBits(da));
            if (!Float.isNaN(fa) && !Float.isInfinite(fa)) {
                hash = mixF(hash, Float.intBitsToFloat(Float.floatToIntBits(fa) ^ 1));
            }
            if (!Double.isNaN(da) && !Double.isInfinite(da)) {
                hash = mixD(hash, Double.longBitsToDouble(Double.doubleToLongBits(da) ^ 1L));
            }
            hash = mixZ(hash, Float.isNaN(fa));
            hash = mixZ(hash, Double.isNaN(da));
            hash = mixZ(hash, Float.isInfinite(fa));
            hash = mixI(hash, Float.compare(fa, fb));
            hash = mixI(hash, Double.compare(da, db));
        }
        return hash;
    }

    static int intField;
    static int[] intCells = new int[2];

    static int sum(int a, int b) {
        return a + b;
    }

    /// An int sum that overflows, handed to everything that can read an int. A
    /// target whose add does not wrap by itself has to wrap in each reader.
    static int overflow(int hash) {
        int[] values = {Integer.MAX_VALUE, Integer.MIN_VALUE, 0x7ffffff0, 0x40000000, -0x40000001, 1, -1, 0x12345678};
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values.length; j++) {
                int a = values[i];
                int b = values[j];
                hash = mixD(hash, (double) (a + b));
                hash = mixD(hash, (double) (a - b));
                hash = mixD(hash, (double) -a);
                hash = mixD(hash, (a + b) * 0.5);
                hash = mixD(hash, (a + b) / 2.0);
                hash = mixF(hash, (float) (a + b));
                hash = mixF(hash, (float) (a - b));
                hash = mixF(hash, (a - b) * 0.5f);
                hash = mixF(hash, (a + b) / 3f);
                hash = mixL(hash, (long) (a + b));
                hash = mixL(hash, (a - b) + 1L);
                hash = mixI(hash, (a + b) / 3);
                hash = mixI(hash, (a - b) % 7);
                hash = mixI(hash, (a + b) >> 1);
                hash = mixI(hash, (a + b) >>> 1);
                hash = mixI(hash, 1 << (a + b));
                hash = mixI(hash, (short) (a + b));
                hash = mixI(hash, (char) (a - b));
                hash = mixI(hash, (byte) (a + b));
                hash = mixZ(hash, a + b > a - b);
                hash = mixZ(hash, a + b == -(-a - b));
                hash = mixZ(hash, a - b < 0);
                hash = mixI(hash, Math.abs(a + b));
                hash = mixI(hash, Math.max(a + b, a - b));
                hash = mixD(hash, Math.abs(a - b));
                hash = mixD(hash, Math.sqrt(a + b));
                hash = mixD(hash, sum(a, b));
                hash = mixF(hash, sum(a, -b));
                intField = a + b;
                hash = mixD(hash, intField);
                intCells[1] = a - b;
                hash = mixF(hash, intCells[1]);
                intCells[0] = a + b;
                intCells[0]++;
                hash = mixD(hash, intCells[0]);
                int local = a + b;
                local += b;
                hash = mixD(hash, local);
                hash = mixD(hash, a + b > 0 ? a + b : a - b);
                String text = String.valueOf(a + b);
                hash = mixI(mixI(hash, text.length()), text.charAt(0));
                // No float goes into the text: Float.toString prints the
                // shortest digits here and one digit more on an older JDK.
                text = "v" + (a - b) + (a + b + 0.5) + (long) (a + b);
                hash = mixI(mixI(hash, text.length()), text.hashCode());
                hash = mixI(hash, Integer.valueOf(a + b).hashCode());
                hash = mixI(hash, new StringBuilder().append(a + b).append(-a).length());
                switch (a + b) {
                    case -2:
                        hash = mixI(hash, 101);
                        break;
                    case Integer.MIN_VALUE:
                        hash = mixI(hash, 102);
                        break;
                    case 0:
                        hash = mixI(hash, 103);
                        break;
                    default:
                        hash = mixI(hash, 104);
                        break;
                }
            }
        }
        return hash;
    }

    static float maxFloat = Float.MAX_VALUE;
    static float minFloat = Float.MIN_VALUE;
    static float nanFloat = Float.NaN;
    static int minInt = Integer.MIN_VALUE;
    static long minLong = Long.MIN_VALUE;
    static int minusOne = -1;

    static int constants(int hash) {
        hash = mixF(hash, Float.MAX_VALUE);
        hash = mixF(hash, Float.MIN_VALUE);
        hash = mixF(hash, Float.NaN);
        hash = mixF(hash, Float.POSITIVE_INFINITY);
        hash = mixF(hash, Float.NEGATIVE_INFINITY);
        hash = mixF(hash, -0f);
        hash = mixF(hash, 0.1f);
        hash = mixF(hash, 16777217f);
        hash = mixF(hash, 3.4028235e38f);
        hash = mixF(hash, 1.17549435e-38f);
        hash = mixD(hash, Double.MAX_VALUE);
        hash = mixD(hash, Double.MIN_VALUE);
        hash = mixD(hash, Double.NaN);
        hash = mixD(hash, 0.1);
        hash = mixD(hash, -0.0);
        hash = mixZ(hash, maxFloat == Float.MAX_VALUE);
        hash = mixZ(hash, minFloat == Float.MIN_VALUE);
        hash = mixZ(hash, nanFloat == Float.NaN);
        hash = mixZ(hash, nanFloat != nanFloat);
        hash = mixZ(hash, maxFloat * 2 == Float.POSITIVE_INFINITY);
        hash = mixZ(hash, minFloat / 2 == 0f);
        hash = mixF(hash, maxFloat + maxFloat);
        hash = mixF(hash, maxFloat * 1.0000001f);
        hash = mixF(hash, minFloat * 0.5f);
        hash = mixF(hash, minFloat * 0.75f);
        hash = mixD(hash, maxFloat * 2.0);
        hash = mixI(hash, minInt / minusOne);
        hash = mixI(hash, minInt % minusOne);
        hash = mixI(hash, -minInt);
        hash = mixL(hash, minLong / minusOne);
        hash = mixL(hash, minLong % minusOne);
        hash = mixL(hash, -minLong);
        hash = mixI(hash, (int) nanFloat);
        hash = mixL(hash, (long) nanFloat);
        hash = mixI(hash, (int) maxFloat);
        hash = mixL(hash, (long) -maxFloat);
        hash = mixI(hash, (int) (2.3f * 10));
        hash = mixI(hash, (int) (2.3 * 10));
        hash = mixI(hash, (int) (0.7f + 0.1f + 0.2f));
        return hash;
    }

    /// What a chart computes on the way to a pixel: a float angle in radians, a
    /// double scale applied to a double value and narrowed to a float coordinate.
    static int layout(int hash) {
        for (int angle = -360; angle <= 360; angle += 15) {
            float degrees = angle;
            float radians = (float) (degrees * Math.PI / 180.0);
            hash = mixF(hash, radians);
            hash = mixD(hash, degrees * Math.PI / 180.0);
            hash = mixF(hash, -degrees + 0f);
        }
        int left = 60;
        int bottom = 1100;
        float size = 20f;
        for (int i = 0; i < 200; i++) {
            double min = (next() >> 20) / 8.0;
            double max = min + 1 + (next() >>> 20) / 16.0;
            double value = min + (max - min) * ((next() >>> 8) / 16777216.0);
            int width = 300 + (next() >>> 23);
            double perUnit = (float) ((double) width / (max - min));
            float x = (float) (left + perUnit * (value - min));
            float y = (float) (bottom - perUnit * (value - min));
            hash = mixD(hash, perUnit);
            hash = mixF(mixF(hash, x), y);
            hash = mixI(mixI(hash, (int) x), (int) y);
            hash = mixI(hash, Math.round(x));
            hash = mixF(hash, y - size * 4 / 3);
            hash = mixF(hash, x + size / 2 + width / 2f);
            hash = mixI(hash, (int) (size * 4 / 3 + y / 2));
            float scale = (float) bottom / width;
            hash = mixF(mixF(hash, scale), 1 / scale);
            hash = mixF(hash, Math.abs(width - bottom) / 2f);
        }
        return hash;
    }

    /// The hash of each section, in the order `compute` runs them, for a harness
    /// that reads statics; the printed line is for one that reads the console.
    // Values whose shortest text and whose JDK 8 text are the same string, so
    // that the answer does not depend on which JDK computed the expectation.
    // They are read out of an array: javac folds "" + 0.1f into a constant.
    static float[] printedFloats = {
        0.1f, 5.0f, 123.456f, 9999999.0f, 0.33333334f, -0.5f, 0.001f,
        3.4028235E38f, 1.0E10f, 1.6777216E7f, 9.999E-4f, 2.5E-7f, 3.0E9f
    };
    static double[] printedDoubles = {
        0.1, 5.0, 123.456, 9999999.0, 1.0 / 3.0, -0.5, 0.001, 1.0E10, 2.5E-7, 100.0
    };

    static int text(int hash) {
        for (int i = 0; i < printedFloats.length; i++) {
            String printed = "" + printedFloats[i];
            hash = mixI(mixI(hash, printed.length()), printed.hashCode());
            printed = new StringBuilder().append(printedFloats[i] * 2.0f).toString();
            hash = mixI(mixI(hash, printed.length()), printed.hashCode());
        }
        for (int i = 0; i < printedDoubles.length; i++) {
            String printed = String.valueOf(printedDoubles[i]);
            hash = mixI(mixI(hash, printed.length()), printed.hashCode());
        }
        return hash;
    }

    public static int[] sections = new int[9];
    static int sectionCount;

    static int section(String name, int hash) {
        sections[sectionCount++] = hash;
        System.out.println("NUMDIFF:" + name + "=" + hash);
        return hash;
    }

    public static int compute() {
        seed = 0x2545f491;
        int total = 17;
        total = mixI(total, section("conversions", conversions(1)));
        total = mixI(total, section("arithmetic", arithmetic(2)));
        total = mixI(total, section("shuffles", shuffles(3)));
        total = mixI(total, section("arrays", arrays(4)));
        total = mixI(total, section("natives", natives(5)));
        total = mixI(total, section("constants", constants(6)));
        total = mixI(total, section("layout", layout(7)));
        total = mixI(total, section("overflow", overflow(8)));
        total = mixI(total, section("text", text(9)));
        return total;
    }

    public static void main(String[] args) {
        result = compute();
        System.out.println("NUMDIFF:total=" + result);
    }
}
