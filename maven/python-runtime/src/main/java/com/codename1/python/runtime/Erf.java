/*
 * Ported from the Sun Microsystems FDLIBM C-library (s_erf.c).
 * (Freely Distributable Library for Math)
 * ====================================================
 * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
 *
 * Developed at SunPro, a Sun Microsystems, Inc. business.
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice
 * is preserved.
 * ====================================================
 */
package com.codename1.python.runtime;

import com.codename1.util.MathUtil;

/// `math.erf` and `math.erfc`, a Java rewrite of FDLIBM's `s_erf.c`.
///
/// CPython calls the C library's functions, and the C libraries it runs on
/// derive theirs from this code, so a series of our own disagreed with
/// CPython in the last bit and that showed in `statistics.NormalDist`.
final class Erf {
    private static final double TINY = 1e-300;
    private static final double ERX = 8.45062911510467529297e-01;
    private static final double EFX = 1.28379167095512586316e-01;
    private static final double EFX8 = 1.02703333676410069053e+00;
    private static final double PP0 = 1.28379167095512558561e-01;
    private static final double PP1 = -3.25042107247001499370e-01;
    private static final double PP2 = -2.84817495755985104766e-02;
    private static final double PP3 = -5.77027029648944159157e-03;
    private static final double PP4 = -2.37630166566501626084e-05;
    private static final double QQ1 = 3.97917223959155352819e-01;
    private static final double QQ2 = 6.50222499887672944485e-02;
    private static final double QQ3 = 5.08130628187576562776e-03;
    private static final double QQ4 = 1.32494738004321644526e-04;
    private static final double QQ5 = -3.96022827877536812320e-06;
    private static final double PA0 = -2.36211856075265944077e-03;
    private static final double PA1 = 4.14856118683748331666e-01;
    private static final double PA2 = -3.72207876035701323847e-01;
    private static final double PA3 = 3.18346619901161753674e-01;
    private static final double PA4 = -1.10894694282396677476e-01;
    private static final double PA5 = 3.54783043256182359371e-02;
    private static final double PA6 = -2.16637559486879084300e-03;
    private static final double QA1 = 1.06420880400844228286e-01;
    private static final double QA2 = 5.40397917702171048937e-01;
    private static final double QA3 = 7.18286544141962662868e-02;
    private static final double QA4 = 1.26171219808761642112e-01;
    private static final double QA5 = 1.36370839120290507362e-02;
    private static final double QA6 = 1.19844998467991074170e-02;
    private static final double RA0 = -9.86494403484714822705e-03;
    private static final double RA1 = -6.93858572707181764372e-01;
    private static final double RA2 = -1.05586262253232909814e+01;
    private static final double RA3 = -6.23753324503260060396e+01;
    private static final double RA4 = -1.62396669462573470355e+02;
    private static final double RA5 = -1.84605092906711035994e+02;
    private static final double RA6 = -8.12874355063065934246e+01;
    private static final double RA7 = -9.81432934416914548592e+00;
    private static final double SA1 = 1.96512716674392571292e+01;
    private static final double SA2 = 1.37657754143519042600e+02;
    private static final double SA3 = 4.34565877475229228821e+02;
    private static final double SA4 = 6.45387271733267880336e+02;
    private static final double SA5 = 4.29008140027567833386e+02;
    private static final double SA6 = 1.08635005541779435134e+02;
    private static final double SA7 = 6.57024977031928170135e+00;
    private static final double SA8 = -6.04244152148580987438e-02;
    private static final double RB0 = -9.86494292470009928597e-03;
    private static final double RB1 = -7.99283237680523006574e-01;
    private static final double RB2 = -1.77579549177547519889e+01;
    private static final double RB3 = -1.60636384855821916062e+02;
    private static final double RB4 = -6.37566443368389627722e+02;
    private static final double RB5 = -1.02509513161107724954e+03;
    private static final double RB6 = -4.83519191608651397019e+02;
    private static final double SB1 = 3.03380607434824582924e+01;
    private static final double SB2 = 3.25792512996573918826e+02;
    private static final double SB3 = 1.53672958608443695994e+03;
    private static final double SB4 = 3.19985821950859553908e+03;
    private static final double SB5 = 2.55305040643316442583e+03;
    private static final double SB6 = 4.74528541206955367215e+02;
    private static final double SB7 = -2.24409524465858183362e+01;

    private Erf() {
    }

    private static int high(double x) {
        return (int) (Double.doubleToRawLongBits(x) >> 32);
    }

    private static double clearLow(double x) {
        return Double.longBitsToDouble(Double.doubleToRawLongBits(x) & 0xffffffff00000000L);
    }

    private static double smallP(double z) {
        return PP0 + z * (PP1 + z * (PP2 + z * (PP3 + z * PP4)));
    }

    private static double smallQ(double z) {
        return 1.0 + z * (QQ1 + z * (QQ2 + z * (QQ3 + z * (QQ4 + z * QQ5))));
    }

    private static double midP(double s) {
        return PA0 + s * (PA1 + s * (PA2 + s * (PA3 + s * (PA4 + s * (PA5 + s * PA6)))));
    }

    private static double midQ(double s) {
        return 1.0 + s * (QA1 + s * (QA2 + s * (QA3 + s * (QA4 + s * (QA5 + s * QA6)))));
    }

    /// `exp(-x*x - 0.5625) * exp(...)/x` for the tails, `x` positive.
    private static double tail(double x, int ix, int bound) {
        double s = 1.0 / (x * x);
        double r;
        double ss;
        if (ix < bound) {
            r = RA0 + s * (RA1 + s * (RA2 + s * (RA3 + s * (RA4 + s * (RA5 + s * (RA6 + s * RA7))))));
            ss = 1.0 + s * (SA1 + s * (SA2 + s * (SA3 + s * (SA4 + s * (SA5 + s * (SA6 + s * (SA7
                    + s * SA8)))))));
        } else {
            r = RB0 + s * (RB1 + s * (RB2 + s * (RB3 + s * (RB4 + s * (RB5 + s * RB6)))));
            ss = 1.0 + s * (SB1 + s * (SB2 + s * (SB3 + s * (SB4 + s * (SB5 + s * (SB6 + s * SB7))))));
        }
        double z = clearLow(x);
        return MathUtil.exp(-z * z - 0.5625) * MathUtil.exp((z - x) * (z + x) + r / ss) / x;
    }

    static double erf(double x) {
        int hx = high(x);
        int ix = hx & 0x7fffffff;
        if (ix >= 0x7ff00000) {
            if (Double.isNaN(x)) {
                return x;
            }
            return hx < 0 ? -1.0 : 1.0;
        }
        if (ix < 0x3feb0000) {
            if (ix < 0x3e300000) {
                if (ix < 0x00800000) {
                    return 0.125 * (8.0 * x + EFX8 * x);
                }
                return x + EFX * x;
            }
            double z = x * x;
            return x + x * (smallP(z) / smallQ(z));
        }
        if (ix < 0x3ff40000) {
            double s = Math.abs(x) - 1.0;
            double pq = midP(s) / midQ(s);
            return hx >= 0 ? ERX + pq : -ERX - pq;
        }
        if (ix >= 0x40180000) {
            return hx >= 0 ? 1.0 - TINY : TINY - 1.0;
        }
        double r = tail(Math.abs(x), ix, 0x4006DB6E);
        return hx >= 0 ? 1.0 - r : r - 1.0;
    }

    static double erfc(double x) {
        int hx = high(x);
        int ix = hx & 0x7fffffff;
        if (ix >= 0x7ff00000) {
            if (Double.isNaN(x)) {
                return x;
            }
            return hx < 0 ? 2.0 : 0.0;
        }
        if (ix < 0x3feb0000) {
            if (ix < 0x3c700000) {
                return 1.0 - x;
            }
            double z = x * x;
            double y = smallP(z) / smallQ(z);
            if (hx < 0x3fd00000) {
                return 1.0 - (x + x * y);
            }
            double r = x * y;
            r += x - 0.5;
            return 0.5 - r;
        }
        if (ix < 0x3ff40000) {
            double s = Math.abs(x) - 1.0;
            double pq = midP(s) / midQ(s);
            return hx >= 0 ? (1.0 - ERX) - pq : 1.0 + (ERX + pq);
        }
        if (ix < 0x403c0000) {
            if (hx < 0 && ix >= 0x40180000) {
                return 2.0 - TINY;
            }
            double r = tail(Math.abs(x), ix, 0x4006DB6D);
            return hx > 0 ? r : 2.0 - r;
        }
        return hx > 0 ? TINY * TINY : 2.0 - TINY;
    }
}
