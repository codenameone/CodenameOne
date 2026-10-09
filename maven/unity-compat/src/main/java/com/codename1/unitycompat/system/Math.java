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
package com.codename1.unitycompat.system;

/// `System.Math`.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Math {
    public static final double PI = 3.14159265358979323846;
    public static final double E = 2.7182818284590452354;

    private Math() {
    }

    public static double Sqrt(double v) {
        return java.lang.Math.sqrt(v);
    }

    public static double Floor(double v) {
        return java.lang.Math.floor(v);
    }

    public static double Ceiling(double v) {
        return java.lang.Math.ceil(v);
    }

    /// Rounds half to even, which is what .NET does and `java.lang.Math.round`
    /// does not.
    public static double Round(double v) {
        double floor = java.lang.Math.floor(v);
        double fraction = v - floor;
        if (fraction < 0.5) {
            return floor;
        }
        if (fraction > 0.5) {
            return floor + 1;
        }
        return floor % 2 == 0 ? floor : floor + 1;
    }

    /// The smallest `int` has no positive counterpart, and .NET documents
    /// an `OverflowException` for it where Java answers the same negative
    /// number. `UnityEngine.Mathf.Abs(int)` is another method, written in C#
    /// among the value types, and is not held to this: Unity's documentation
    /// promises no exception for it.
    public static int Abs(int v) {
        if (v >= 0) {
            return v;
        }
        if (v == Integer.MIN_VALUE) {
            throw new OverflowException("Negating the minimum value of a twos complement number is invalid.");
        }
        return -v;
    }

    /// As [#Abs(int)]: the smallest `long` throws.
    public static long Abs(long v) {
        if (v >= 0) {
            return v;
        }
        if (v == Long.MIN_VALUE) {
            throw new OverflowException("Negating the minimum value of a twos complement number is invalid.");
        }
        return -v;
    }

    public static float Abs(float v) {
        return v <= 0f ? 0f - v : v;
    }

    public static double Abs(double v) {
        return v <= 0d ? 0d - v : v;
    }

    public static int Max(int a, int b) {
        return a > b ? a : b;
    }

    public static int Min(int a, int b) {
        return a < b ? a : b;
    }

    public static long Max(long a, long b) {
        return a > b ? a : b;
    }

    public static long Min(long a, long b) {
        return a < b ? a : b;
    }

    public static float Max(float a, float b) {
        return java.lang.Math.max(a, b);
    }

    public static float Min(float a, float b) {
        return java.lang.Math.min(a, b);
    }

    public static double Max(double a, double b) {
        return java.lang.Math.max(a, b);
    }

    public static double Min(double a, double b) {
        return java.lang.Math.min(a, b);
    }

    public static double Sin(double v) {
        return java.lang.Math.sin(v);
    }

    public static double Cos(double v) {
        return java.lang.Math.cos(v);
    }

    public static double Tan(double v) {
        return java.lang.Math.tan(v);
    }

    // The device class libraries have a sine, a cosine and a square root and
    // nothing else of this family, so the rest is worked out here. Each is
    // good to a few units in the last place of a double, which is far inside
    // what the float a game takes from it can show.

    private static final double LN2 = 0.6931471805599453;
    private static final double SQRT3 = 1.7320508075688772;

    public static double Atan(double v) {
        if (v != v) {
            return v;
        }
        if (v < 0) {
            return -Atan(-v);
        }
        if (v > 1) {
            return PI / 2 - Atan(1 / v);
        }
        if (v > 0.2679491924311227) {
            // atan(v) = pi/6 + atan((v * sqrt(3) - 1) / (sqrt(3) + v))
            return PI / 6 + Atan((v * SQRT3 - 1) / (SQRT3 + v));
        }
        double square = v * v;
        double term = v;
        double sum = v;
        for (int n = 3; n <= 29; n += 2) {
            term = -term * square;
            sum += term / n;
        }
        return sum;
    }

    public static double Atan2(double y, double x) {
        if (x != x || y != y) {
            return 0.0 / 0.0;
        }
        if (x > 0) {
            return Atan(y / x);
        }
        if (x < 0) {
            return y >= 0 ? Atan(y / x) + PI : Atan(y / x) - PI;
        }
        return y > 0 ? PI / 2 : y < 0 ? -PI / 2 : 0;
    }

    public static double Asin(double v) {
        if (v != v || v > 1 || v < -1) {
            return 0.0 / 0.0;
        }
        return Atan2(v, java.lang.Math.sqrt(1 - v * v));
    }

    public static double Acos(double v) {
        if (v != v || v > 1 || v < -1) {
            return 0.0 / 0.0;
        }
        return Atan2(java.lang.Math.sqrt(1 - v * v), v);
    }

    public static double Exp(double v) {
        if (v != v) {
            return v;
        }
        if (v > 709.78) {
            return 1.0 / 0.0;
        }
        if (v < -745.2) {
            return 0;
        }
        // v = k * ln 2 + r with r no larger than half of ln 2.
        int k = (int) java.lang.Math.floor(v / LN2 + 0.5);
        double r = v - k * LN2;
        double term = 1;
        double sum = 1;
        for (int n = 1; n <= 18; n++) {
            term = term * r / n;
            sum += term;
        }
        while (k > 0) {
            sum *= 2;
            k--;
        }
        while (k < 0) {
            sum *= 0.5;
            k++;
        }
        return sum;
    }

    public static double Log(double v) {
        if (v != v || v < 0) {
            return 0.0 / 0.0;
        }
        if (v == 0) {
            return -1.0 / 0.0;
        }
        if (v == 1.0 / 0.0) {
            return v;
        }
        // v = m * 2^e with m between the square roots of a half and of two.
        int e = 0;
        double m = v;
        while (m > 1.4142135623730951) {
            m *= 0.5;
            e++;
        }
        while (m < 0.7071067811865476) {
            m *= 2;
            e--;
        }
        // ln m = 2 atanh((m - 1) / (m + 1))
        double s = (m - 1) / (m + 1);
        double square = s * s;
        double term = s;
        double sum = s;
        for (int n = 3; n <= 31; n += 2) {
            term *= square;
            sum += term / n;
        }
        return 2 * sum + e * LN2;
    }

    public static double Log10(double v) {
        return Log(v) / 2.302585092994046;
    }

    public static double Pow(double a, double b) {
        if (b == 0) {
            return 1;
        }
        if (a != a || b != b) {
            return 0.0 / 0.0;
        }
        double whole = java.lang.Math.floor(b);
        if (whole == b && b >= -1024 && b <= 1024) {
            // A whole power is repeated multiplication, which is exact
            // wherever the true result is.
            int n = (int) (b < 0 ? -b : b);
            double result = 1;
            double square = a;
            while (n > 0) {
                if ((n & 1) != 0) {
                    result *= square;
                }
                square *= square;
                n >>= 1;
            }
            return b < 0 ? 1 / result : result;
        }
        if (a < 0) {
            return 0.0 / 0.0;
        }
        if (a == 0) {
            return b > 0 ? 0 : 1.0 / 0.0;
        }
        return Exp(b * Log(a));
    }
}
