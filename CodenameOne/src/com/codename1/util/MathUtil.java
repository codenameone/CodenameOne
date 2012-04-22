package com.codename1.util;
/*
 * Ported from the Sun Microsystems FDLIBM C-library.
 * (Freely Distributable Library for Math)
 * ====================================================
 * Copyright (C) 2004 by Sun Microsystems, Inc. All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice
 * is preserved.
 * ====================================================
 */

/**
 * MathUtil for Java ME.
 * This fills the gap in Java ME Math with a port of Sun's public FDLIBM C-library for IEEE-754.
 *
 * @author kmashint
 *
 * @see http://www.netlib.org/fdlibm/readme
 *    For the Freely Distributable C-library conforming to IEEE-754 floating point math.
 * @see http://web.mit.edu/source/third/gcc/libjava/java/lang/
 *    For the GNU C variant of the same IEEE-754 routines.
 * @see http://www.dclausen.net/projects/microfloat/
 *    Another take on the IEEE-754 routines.
 * @see http://real-java.sourceforge.net/Real.html
 *    Yet another take on the IEEE-754 routines.
 * @see http://today.java.net/pub/a/today/2007/11/06/creating-java-me-math-pow-method.html
 *    For other approximations.
 * @see http://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/
 *    For fast but rough approximations.
 * @see http://martin.ankerl.com/2007/02/11/optimized-exponential-functions-for-java/
 *    For more fast but rough approximations.
 */
public abstract class MathUtil {

    /* Common constants. */
    private static final double zero = 0.0,
            one = 1.0,
            two = 2.0,
            tiny = 1.0e-300,
            huge = 1.0e+300,
            two53 = 9007199254740992.0, /* 0x43400000, 0x00000000 */
            two54 = 1.80143985094819840000e+16, /* 0x43500000, 0x00000000 */
            twom54 = 5.55111512312578270212e-17, /* 0x3C900000, 0x00000000 */
            P1 = 1.66666666666666019037e-01, /* 0x3FC55555, 0x5555553E */
            P2 = -2.77777777770155933842e-03, /* 0xBF66C16C, 0x16BEBD93 */
            P3 = 6.61375632143793436117e-05, /* 0x3F11566A, 0xAF25DE2C */
            P4 = -1.65339022054652515390e-06, /* 0xBEBBBD41, 0xC5D26BF1 */
            P5 = 4.13813679705723846039e-08; /* 0x3E663769, 0x72BEA4D0 */

    private static final double pio2_hi = 1.57079632679489655800e+00, /* 0x3FF921FB, 0x54442D18 */
            pio2_lo = 6.12323399573676603587e-17, /* 0x3C91A626, 0x33145C07 */
            pio4_hi = 7.85398163397448278999e-01, /* 0x3FE921FB, 0x54442D18 */
            /* coefficient for R(x^2) */
            pS0 = 1.66666666666666657415e-01, /* 0x3FC55555, 0x55555555 */
            pS1 = -3.25565818622400915405e-01, /* 0xBFD4D612, 0x03EB6F7D */
            pS2 = 2.01212532134862925881e-01, /* 0x3FC9C155, 0x0E884455 */
            pS3 = -4.00555345006794114027e-02, /* 0xBFA48228, 0xB5688F3B */
            pS4 = 7.91534994289814532176e-04, /* 0x3F49EFE0, 0x7501B288 */
            pS5 = 3.47933107596021167570e-05, /* 0x3F023DE1, 0x0DFDF709 */
            qS1 = -2.40339491173441421878e+00, /* 0xC0033A27, 0x1C8A2D4B */
            qS2 = 2.02094576023350569471e+00, /* 0x40002AE5, 0x9C598AC8 */
            qS3 = -6.88283971605453293030e-01, /* 0xBFE6066C, 0x1B8D0159 */
            qS4 = 7.70381505559019352791e-02; /* 0x3FB3B8C5, 0xB12E9282 */

    private static final double pi_o_4 = 7.8539816339744827900E-01, /* 0x3FE921FB, 0x54442D18 */
            pi_o_2 = 1.5707963267948965580E+00, /* 0x3FF921FB, 0x54442D18 */
            pi = 3.1415926535897931160E+00, /* 0x400921FB, 0x54442D18 */
            pi_lo = 1.2246467991473531772E-16; /* 0x3CA1A626, 0x33145C07 */

    private static final double log10 = 2.302585092994046D; /* Natural log(10.0D). */

    private static final long HI_MASK = 0xffffffff00000000L,
            LO_MASK = 0x00000000ffffffffL;
    private static final int HI_SHIFT = 32;

    /**
     * Return Math.E to the exponent a.
     * This in turn uses ieee7854_exp(double).
     */
    public static final double exp(double a) {
        return ieee754_exp(a);
    }

    /**
     * Return the natural logarithm, ln(a), as it relates to Math.E.
     * This in turn uses ieee7854_log(double).
     */
    public static final double log(double a) {
        return ieee754_log(a);
    }

    /**
     * Return the common base-10 logarithm, log10(a).
     * This in turn uses ieee7854_log(double)/ieee7854_log(10.0).
     */
    public static final double log10(double a) {
        return ieee754_log(a) / log10;
    }

    /**
     * Return a to the power of b, sometimes written as a ** b
     * but not to be confused with the bitwise ^ operator.
     * This in turn uses ieee7854_log(double).
     */
    public static final double pow(double a, double b) {
        return ieee754_pow(a, b);
    }

    /**
     * Return the arcsine of a.
     */
    public static final double asin(double a) {
        return ieee754_asin(a);
    }

    
    /**
     * Return the arccosine of a.
     */
    public static final double acos(double a) {
        return ieee754_acos(a);
    }

    /**
     * Return the arctangent of a, call it b, where a = tan(b).
     */
    public static final double atan(double a) {
        return ieee754_atan(a);
    }

    /**
     * For any real arguments x and y not both equal to zero, atan2(y, x)
     * is the angle in radians between the positive x-axis of a plane
     * and the point given by the coordinates (x, y) on it.
     * The angle is positive for counter-clockwise angles (upper half-plane, y > 0),
     * and negative for clockwise angles (lower half-plane, y < 0).
     * This in turn uses ieee7854_arctan2(double).
     */
    public static final double atan2(double b, double a) {
        return ieee754_atan2(a, b);
    }

    /* __ieee754_exp(x)
     * Returns the exponential of x.
     *
     * Method
     *   1. Argument reduction:
     *      Reduce x to an r so that |r| <= 0.5*ln2 ~ 0.34658.
     *  Given x, find r and integer k such that
     *
     *               x = k*ln2 + r,  |r| <= 0.5*ln2.
     *
     *      Here r will be represented as r = hi-lo for better
     *  accuracy.
     *
     *   2. Approximation of exp(r) by a special rational function on
     *  the interval [0,0.34658]:
     *  Write
     *      R(r**2) = r*(exp(r)+1)/(exp(r)-1) = 2 + r*r/6 - r**4/360 + ...
     *      We use a special Remes algorithm on [0,0.34658] to generate
     *  a polynomial of degree 5 to approximate R. The maximum error
     *  of this polynomial approximation is bounded by 2**-59. In
     *  other words,
     *      R(z) ~ 2.0 + P1*z + P2*z**2 + P3*z**3 + P4*z**4 + P5*z**5
     *    (where z=r*r, and the values of P1 to P5 are listed below)
     *  and
     *      |                  5          |     -59
     *      | 2.0+P1*z+...+P5*z   -  R(z) | <= 2
     *      |                             |
     *  The computation of exp(r) thus becomes
     *                             2*r
     *    exp(r) = 1 + -------
     *                  R - r
     *                                 r*R1(r)
     *           = 1 + r + ----------- (for better accuracy)
     *                      2 - R1(r)
     *  where
     *               2       4             10
     *    R1(r) = r - (P1*r  + P2*r  + ... + P5*r   ).
     *
     *   3. Scale back to obtain exp(x):
     *  From step 1, we have
     *     exp(x) = 2^k * exp(r)
     *
     * Special cases:
     *  exp(INF) is INF, exp(NaN) is NaN;
     *  exp(-INF) is 0, and
     *  for finite argument, only exp(0)=1 is exact.
     *
     * Accuracy:
     *  according to an error analysis, the error is always less than
     *  1 ulp (unit in the last place).
     *
     * Misc. info.
     *  For IEEE double
     *      if x >  7.09782712893383973096e+02 then exp(x) overflow
     *      if x < -7.45133219101941108420e+02 then exp(x) underflow
     *
     * Constants:
     * The hexadecimal values are the intended ones for the following
     * constants. The decimal values may be used, provided that the
     * compiler will convert from decimal to binary accurately enough
     * to produce the hexadecimal values shown.
     */
    private static final double twom1000 = 9.33263618503218878990e-302, /* 2**-1000=0x01700000,0*/
            o_threshold = 7.09782712893383973096e+02, /* 0x40862E42, 0xFEFA39EF */
            u_threshold = -7.45133219101941108420e+02, /* 0xc0874910, 0xD52D3051 */
            invln2 = 1.44269504088896338700e+00; /* 0x3ff71547, 0x652b82fe */

    private static final double[] halF = new double[]{0.5, -0.5},
            ln2HI = new double[]{6.93147180369123816490e-01, /* 0x3fe62e42, 0xfee00000 */
        -6.93147180369123816490e-01}, /* 0xbfe62e42, 0xfee00000 */
            ln2LO = new double[]{1.90821492927058770002e-10, /* 0x3dea39ef, 0x35793c76 */
        -1.90821492927058770002e-10}; /* 0xbdea39ef, 0x35793c76 */


    private static final double ieee754_exp(double x) {
        double y, c, t;
        double hi = 0, lo = 0;
        int k = 0;
        int xsb, hx, lx;
        long yl;
        long xl = Double.doubleToLongBits(x);

        hx = (int) ((long) xl >>> HI_SHIFT); /* high word of x */
        xsb = (hx >> 31) & 1;   /* sign bit of x */
        hx &= 0x7fffffff;   /* high word of |x| */

        /* filter out non-finite argument */
        if (hx >= 0x40862E42) {      /* if |x|>=709.78... */
            if (hx >= 0x7ff00000) {
                lx = (int) ((long) xl & LO_MASK);  /* low word of x */
                if (((hx & 0xfffff) | lx) != 0) {
                    return x + x;    /* NaN */
                } else {
                    return (xsb == 0) ? x : 0.0;  /* exp(+-inf)={inf,0} */
                }
            }
            if (x > o_threshold) {
                return huge * huge; /* overflow */
            }
            if (x < u_threshold) {
                return twom1000 * twom1000; /* underflow */
            }
        }

        /* argument reduction */
        if (hx > 0x3fd62e42) {   /* if  |x| > 0.5 ln2 */
            if (hx < 0x3FF0A2B2) { /* and |x| < 1.5 ln2 */
                hi = x - ln2HI[xsb];
                lo = ln2LO[xsb];
                k = 1 - xsb - xsb;
            } else {
                k = (int) (invln2 * x + halF[xsb]);
                t = k;
                hi = x - t * ln2HI[0];  /* t*ln2HI is exact here */
                lo = t * ln2LO[0];
            }
            x = hi - lo;
        } else if (hx < 0x3e300000) { /* when |x|<2**-28 */
            if (huge + x > one) {
                return one + x;/* trigger inexact */
            }
        }
        //else k = 0; // handled at declaration

        /* x is now in primary range */
        t = x * x;
        c = x - t * (P1 + t * (P2 + t * (P3 + t * (P4 + t * P5))));
        if (k == 0) {
            return one - ((x * c) / (c - 2.0) - x);
        } else {
            y = one - ((lo - (x * c) / (2.0 - c)) - hi);
        }
        yl = Double.doubleToLongBits(y);
        if (k >= -1021) {
            yl += ((long) k << (20 + HI_SHIFT)); /* add k to y's exponent */
            return Double.longBitsToDouble(yl);
        } else {
            yl += ((long) (k + 1000) << (20 + HI_SHIFT));/* add k to y's exponent */
            return Double.longBitsToDouble(yl) * twom1000;
        }
    }

    /* __ieee754_log(x)
     * Return the logrithm of x
     *
     * Method :
     *   1. Argument Reduction: find k and f such that
     *      x = 2^k * (1+f),
     *     where  sqrt(2)/2 < 1+f < sqrt(2) .
     *
     *   2. Approximation of log(1+f).
     *  Let s = f/(2+f) ; based on log(1+f) = log(1+s) - log(1-s)
     *     = 2s + 2/3 s**3 + 2/5 s**5 + .....,
     *         = 2s + s*R
     *      We use a special Reme algorithm on [0,0.1716] to generate
     *  a polynomial of degree 14 to approximate R The maximum error
     *  of this polynomial approximation is bounded by 2**-58.45. In
     *  other words,
     *            2      4      6      8      10      12      14
     *      R(z) ~ Lg1*s +Lg2*s +Lg3*s +Lg4*s +Lg5*s  +Lg6*s  +Lg7*s
     *    (the values of Lg1 to Lg7 are listed in the program)
     *  and
     *      |      2          14          |     -58.45
     *      | Lg1*s +...+Lg7*s    -  R(z) | <= 2
     *      |                             |
     *  Note that 2s = f - s*f = f - hfsq + s*hfsq, where hfsq = f*f/2.
     *  In order to guarantee error in log below 1ulp, we compute log
     *  by
     *    log(1+f) = f - s*(f - R)  (if f is not too large)
     *    log(1+f) = f - (hfsq - s*(hfsq+R)). (better accuracy)
     *
     *  3. Finally,  log(x) = k*ln2 + log(1+f).
     *          = k*ln2_hi+(f-(hfsq-(s*(hfsq+R)+k*ln2_lo)))
     *     Here ln2 is split into two floating point number:
     *      ln2_hi + ln2_lo,
     *     where n*ln2_hi is always exact for |n| < 2000.
     *
     * Special cases:
     *  log(x) is NaN with signal if x < 0 (including -INF) ;
     *  log(+INF) is +INF; log(0) is -INF with signal;
     *  log(NaN) is that NaN with no signal.
     *
     * Accuracy:
     *  according to an error analysis, the error is always less than
     *  1 ulp (unit in the last place).
     *
     * Constants:
     * The hexadecimal values are the intended ones for the following
     * constants. The decimal values may be used, provided that the
     * compiler will convert from decimal to binary accurately enough
     * to produce the hexadecimal values shown.
     */
    private static final double ln2_hi = 6.93147180369123816490e-01, /* 3fe62e42 fee00000 */
            ln2_lo = 1.90821492927058770002e-10, /* 3dea39ef 35793c76 */
            Lg1 = 6.666666666666735130e-01, /* 3FE55555 55555593 */
            Lg2 = 3.999999999940941908e-01, /* 3FD99999 9997FA04 */
            Lg3 = 2.857142874366239149e-01, /* 3FD24924 94229359 */
            Lg4 = 2.222219843214978396e-01, /* 3FCC71C5 1D8E78AF */
            Lg5 = 1.818357216161805012e-01, /* 3FC74664 96CB03DE */
            Lg6 = 1.531383769920937332e-01, /* 3FC39A09 D078C69F */
            Lg7 = 1.479819860511658591e-01;  /* 3FC2F112 DF3E5244 */


    private static final double ieee754_log(double x) {
        double hfsq, f, s, z, R, w, t1, t2, dk;
        int k, hx, lx, i, j;
        long xl = Double.doubleToLongBits(x);

        hx = (int) (xl >> HI_SHIFT);   /* high word of x */
        lx = (int) (xl & LO_MASK);   /* low  word of x */

        k = 0;
        if (hx < 0x00100000) {      /* x < 2**-1022  */
            if (((hx & 0x7fffffff) | lx) == 0) {
                return -two54 / zero;   /* log(+-0)=-inf */
            }
            if (hx < 0) {
                return (x - x) / zero;  /* log(-#) = NaN */
            }
            k -= 54;
            x *= two54; /* subnormal number, scale up x */
            hx = (int) (Double.doubleToLongBits(x) >>> HI_SHIFT);    /* high word of x */
        }
        if (hx >= 0x7ff00000) {
            return x + x;
        }
        k += (hx >> 20) - 1023;
        hx &= 0x000fffff;
        i = (hx + 0x95f64) & 0x100000;
        //__HI(x) = hx|(i^0x3ff00000);  /* normalize x or x/2 */
        x = Double.longBitsToDouble(((long) (hx | (i ^ 0x3ff00000)) << HI_SHIFT) | (Double.doubleToLongBits(x) & LO_MASK));
        k += (i >> 20);
        f = x - 1.0;
        if ((0x000fffff & (2 + hx)) < 3) { /* |f| < 2**-20 */
            if (f == zero) {
                if (k == 0) {
                    return zero;
                } else {
                    dk = (double) k;
                    return dk * ln2_hi + dk * ln2_lo;
                }
            }
            R = f * f * (0.5 - 0.33333333333333333 * f);
            if (k == 0) {
                return f - R;
            } else {
                dk = (double) k;
                return dk * ln2_hi - ((R - dk * ln2_lo) - f);
            }
        }
        s = f / (2.0 + f);
        dk = (double) k;
        z = s * s;
        i = hx - 0x6147a;
        w = z * z;
        j = 0x6b851 - hx;
        t1 = w * (Lg2 + w * (Lg4 + w * Lg6));
        t2 = z * (Lg1 + w * (Lg3 + w * (Lg5 + w * Lg7)));
        i |= j;
        R = t2 + t1;
        if (i > 0) {
            hfsq = 0.5 * f * f;
            if (k == 0) {
                return f - (hfsq - s * (hfsq + R));
            } else {
                return dk * ln2_hi - ((hfsq - (s * (hfsq + R) + dk * ln2_lo)) - f);
            }
        } else {
            if (k == 0) {
                return f - s * (f - R);
            } else {
                return dk * ln2_hi - ((s * (f - R) - dk * ln2_lo) - f);
            }
        }
    }

    /* __ieee754_pow(x,y) return x**y
     *
     *          n
     * Method:  Let x =  2   * (1+f)
     *  1. Compute and return log2(x) in two pieces:
     *    log2(x) = w1 + w2,
     *     where w1 has 53-24 = 29 bit trailing zeros.
     *  2. Perform y*log2(x) = n+y' by simulating muti-precision
     *     arithmetic, where |y'|<=0.5.
     *  3. Return x**y = 2**n*exp(y'*log2)
     *
     * Special cases:
     *  1.  (anything) ** 0  is 1
     *  2.  (anything) ** 1  is itself
     *  3.  (anything) ** NAN is NAN
     *  4.  NAN ** (anything except 0) is NAN
     *  5.  +-(|x| > 1) **  +INF is +INF
     *  6.  +-(|x| > 1) **  -INF is +0
     *  7.  +-(|x| < 1) **  +INF is +0
     *  8.  +-(|x| < 1) **  -INF is +INF
     *  9.  +-1         ** +-INF is NAN
     *  10. +0 ** (+anything except 0, NAN)               is +0
     *  11. -0 ** (+anything except 0, NAN, odd integer)  is +0
     *  12. +0 ** (-anything except 0, NAN)               is +INF
     *  13. -0 ** (-anything except 0, NAN, odd integer)  is +INF
     *  14. -0 ** (odd integer) = -( +0 ** (odd integer) )
     *  15. +INF ** (+anything except 0,NAN) is +INF
     *  16. +INF ** (-anything except 0,NAN) is +0
     *  17. -INF ** (anything)  = -0 ** (-anything)
     *  18. (-anything) ** (integer) is (-1)**(integer)*(+anything**integer)
     *  19. (-anything except 0 and inf) ** (non-integer) is NAN
     *
     * Accuracy:
     *  pow(x,y) returns x**y nearly rounded. In particular
     *      pow(integer,integer)
     *  always returns the correct integer provided it is
     *  representable.
     *
     * Constants :
     * The hexadecimal values are the intended ones for the following
     * constants. The decimal values may be used, provided that the
     * compiler will convert from decimal to binary accurately enough
     * to produce the hexadecimal values shown.
     */
    private static final double bp[] = {1.0, 1.5,},
            dp_h[] = {0.0, 5.84962487220764160156e-01,}, /* 0x3FE2B803, 0x40000000 */
            dp_l[] = {0.0, 1.35003920212974897128e-08,}, /* 0x3E4CFDEB, 0x43CFD006 */
            /* poly coefs for (3/2)*(log(x)-2s-2/3*s**3 */
            L1 = 5.99999999999994648725e-01, /* 0x3FE33333, 0x33333303 */
            L2 = 4.28571428578550184252e-01, /* 0x3FDB6DB6, 0xDB6FABFF */
            L3 = 3.33333329818377432918e-01, /* 0x3FD55555, 0x518F264D */
            L4 = 2.72728123808534006489e-01, /* 0x3FD17460, 0xA91D4101 */
            L5 = 2.30660745775561754067e-01, /* 0x3FCD864A, 0x93C9DB65 */
            L6 = 2.06975017800338417784e-01, /* 0x3FCA7E28, 0x4A454EEF */
            lg2 = 6.93147180559945286227e-01, /* 0x3FE62E42, 0xFEFA39EF */
            lg2_h = 6.93147182464599609375e-01, /* 0x3FE62E43, 0x00000000 */
            lg2_l = -1.90465429995776804525e-09, /* 0xBE205C61, 0x0CA86C39 */
            ovt = 8.0085662595372944372e-0017, /* -(1024-log2(ovfl+.5ulp)) */
            cp = 9.61796693925975554329e-01, /* 0x3FEEC709, 0xDC3A03FD =2/(3ln2) */
            cp_h = 9.61796700954437255859e-01, /* 0x3FEEC709, 0xE0000000 =(float)cp */
            cp_l = -7.02846165095275826516e-09, /* 0xBE3E2FE0, 0x145B01F5 =tail of cp_h*/
            ivln2 = 1.44269504088896338700e+00, /* 0x3FF71547, 0x652B82FE =1/ln2 */
            ivln2_h = 1.44269502162933349609e+00, /* 0x3FF71547, 0x60000000 =24b 1/ln2*/
            ivln2_l = 1.92596299112661746887e-08; /* 0x3E54AE0B, 0xF85DDF44 =1/ln2 tail*/


    private static final double ieee754_pow(double x, double y) {
        double z, ax, z_h, z_l, p_h, p_l;
        double y1, t1, t2, r, s, t, u, v, w;
        //int i0,i1;
        int i, j, k, yisint, n;
        int hx, hy, ix, iy;
        int lx, ly;

        //i0 = (int)((Double.doubleToLongBits(one)) >>> (29+HI_SHIFT))^1;
        //i1 = 1-i0;
        hx = (int) (Double.doubleToLongBits(x) >>> HI_SHIFT);
        lx = (int) (Double.doubleToLongBits(x) & LO_MASK);
        hy = (int) (Double.doubleToLongBits(y) >>> HI_SHIFT);
        ly = (int) (Double.doubleToLongBits(y) & LO_MASK);
        ix = hx & 0x7fffffff;
        iy = hy & 0x7fffffff;

        /* y==zero: x**0 = 1 */
        if ((iy | ly) == 0) {
            return one;
        }

        /* +-NaN return x+y */
        if (ix > 0x7ff00000 || ((ix == 0x7ff00000) && (lx != 0))
                || iy > 0x7ff00000 || ((iy == 0x7ff00000) && (ly != 0))) {
            return x + y;
        }

        /* determine if y is an odd int when x < 0
         * yisint = 0 ... y is not an integer
         * yisint = 1 ... y is an odd int
         * yisint = 2 ... y is an even int
         */
        yisint = 0;
        if (hx < 0) {
            if (iy >= 0x43400000) {
                yisint = 2; /* even integer y */
            } else if (iy >= 0x3ff00000) {
                k = (iy >> 20) - 0x3ff;    /* exponent */
                if (k > 20) {
                    j = ly >> (52 - k);
                    if ((j << (52 - k)) == ly) {
                        yisint = 2 - (j & 1);
                    }
                } else if (ly == 0) {
                    j = iy >> (20 - k);
                    if ((j << (20 - k)) == iy) {
                        yisint = 2 - (j & 1);
                    }
                }
            }
        }

        /* special value of y */
        if (ly == 0) {
            if (iy == 0x7ff00000) { /* y is +-inf */
                if (((ix - 0x3ff00000) | lx) == 0) {
                    return y - y;  /* inf**+-1 is NaN */
                } else if (ix >= 0x3ff00000)/* (|x|>1)**+-inf = inf,0 */ {
                    return (hy >= 0) ? y : zero;
                } else /* (|x|<1)**-,+inf = inf,0 */ {
                    return (hy < 0) ? -y : zero;
                }
            }
            if (iy == 0x3ff00000) {  /* y is  +-1 */
                if (hy < 0) {
                    return one / x;
                } else {
                    return x;
                }
            }
            if (hy == 0x40000000) {
                return x * x; /* y is  2 */
            }
            if (hy == 0x3fe00000) {  /* y is  0.5 */
                if (hx >= 0) /* x >= +0 */ {
                    return Math.sqrt(x);
                }
            }
        }

        ax = Math.abs(x);
        /* special value of x */
        if (lx == 0) {
            if (ix == 0x7ff00000 || ix == 0 || ix == 0x3ff00000) {
                z = ax;     /*x is +-0,+-inf,+-1*/
                if (hy < 0) {
                    z = one / z; /* z = (1/|x|) */
                }
                if (hx < 0) {
                    if (((ix - 0x3ff00000) | yisint) == 0) {
                        z = (z - z) / (z - z); /* (-1)**non-int is NaN */
                    } else if (yisint == 1) {
                        z = -z;   /* (x<0)**odd = -(|x|**odd) */
                    }
                }
                return z;
            }
        }

        n = (hx >>> 31) + 1;

        /* (x<0)**(non-int) is NaN */
        if ((n | yisint) == 0) {
            return (x - x) / (x - x);
        }

        s = one; /* s (sign of result -ve**odd) = -1 else = 1 */
        if ((n | (yisint - 1)) == 0) {
            s = -one;/* (-ve)**(odd int) */
        }

        /* |y| is huge */
        if (iy > 0x41e00000) { /* if |y| > 2**31 */
            if (iy > 0x43f00000) {  /* if |y| > 2**64, must o/uflow */
                if (ix <= 0x3fefffff) {
                    return (hy < 0) ? huge * huge : tiny * tiny;
                }
                if (ix >= 0x3ff00000) {
                    return (hy > 0) ? huge * huge : tiny * tiny;
                }
            }
            /* over/underflow if x is not close to one */
            if (ix < 0x3fefffff) {
                return (hy < 0) ? s * huge * huge : s * tiny * tiny;
            }
            if (ix > 0x3ff00000) {
                return (hy > 0) ? s * huge * huge : s * tiny * tiny;
            }
            /* now |1-x| is tiny <= 2**-20, suffice to compute
            log(x) by x-x^2/2+x^3/3-x^4/4 */
            t = x - one;    /* t has 20 trailing zeros */
            w = (t * t) * (0.5 - t * (0.3333333333333333333333 - t * 0.25));
            u = ivln2_h * t;  /* ivln2_h has 21 sig. bits */
            v = t * ivln2_l - w * ivln2;
            t1 = u + v;
            //__LO(t1) = 0; // keep high word
            t1 = Double.longBitsToDouble(Double.doubleToLongBits(t1) & HI_MASK);
            t2 = v - (t1 - u);
        } else {
            double ss, s2, s_h, s_l, t_h, t_l;
            n = 0;
            /* take care subnormal number */
            if (ix < 0x00100000) {
                ax *= two53;
                n -= 53;
                ix = (int) (Double.doubleToLongBits(ax) >>> HI_SHIFT);
            }
            n += ((ix) >> 20) - 0x3ff;
            j = ix & 0x000fffff;
            /* determine interval */
            ix = j | 0x3ff00000;    /* normalize ix */
            if (j <= 0x3988E) {
                k = 0;   /* |x|<sqrt(3/2) */
            } else if (j < 0xBB67A) {
                k = 1; /* |x|<sqrt(3)   */
            } else {
                k = 0;
                n += 1;
                ix -= 0x00100000;
            }
            //__HI(ax) = ix;
            ax = Double.longBitsToDouble(((long) ix << HI_SHIFT) | (Double.doubleToLongBits(ax) & LO_MASK));

            /* compute ss = s_h+s_l = (x-1)/(x+1) or (x-1.5)/(x+1.5) */
            u = ax - bp[k];   /* bp[0]=1.0, bp[1]=1.5 */
            v = one / (ax + bp[k]);
            ss = u * v;
            s_h = ss;
            //__LO(s_h) = 0; // keep high word
            s_h = Double.longBitsToDouble(Double.doubleToLongBits(s_h) & HI_MASK);
            /* t_h=ax+bp[k] High */
            t_h = zero;
            //__HI(t_h)=((ix>>1)|0x20000000)+0x00080000+(k<<18);
            t_h = Double.longBitsToDouble(((long) ((int) ((ix >> 1) | 0x20000000) + 0x00080000 + (k << 18)) << HI_SHIFT) | (Double.doubleToLongBits(t_h) & LO_MASK));
            t_l = ax - (t_h - bp[k]);
            s_l = v * ((u - s_h * t_h) - s_h * t_l);
            /* compute log(ax) */
            s2 = ss * ss;
            r = s2 * s2 * (L1 + s2 * (L2 + s2 * (L3 + s2 * (L4 + s2 * (L5 + s2 * L6)))));
            r += s_l * (s_h + ss);
            s2 = s_h * s_h;
            t_h = 3.0 + s2 + r;
            //__LO(t_h) = 0; // keep high word
            t_h = Double.longBitsToDouble(Double.doubleToLongBits(t_h) & HI_MASK);
            t_l = r - ((t_h - 3.0) - s2);
            /* u+v = ss*(1+...) */
            u = s_h * t_h;
            v = s_l * t_h + t_l * ss;
            /* 2/(3log2)*(ss+...) */
            p_h = u + v;
            //__LO(p_h) = 0; // keep high word
            p_h = Double.longBitsToDouble(Double.doubleToLongBits(p_h) & HI_MASK);
            p_l = v - (p_h - u);
            z_h = cp_h * p_h;   /* cp_h+cp_l = 2/(3*log2) */
            z_l = cp_l * p_h + p_l * cp + dp_l[k];
            /* log2(ax) = (ss+..)*2/(3*log2) = n + dp_h + z_h + z_l */
            t = (double) n;
            t1 = (((z_h + z_l) + dp_h[k]) + t);
            //__LO(t1) = 0; // keep high word
            t1 = Double.longBitsToDouble(Double.doubleToLongBits(t1) & HI_MASK);
            t2 = z_l - (((t1 - t) - dp_h[k]) - z_h);
        }

        /* split up y into y1+y2 and compute (y1+y2)*(t1+t2) */
        y1 = y;
        //__LO(y1) = 0; // keep high word
        y1 = Double.longBitsToDouble(Double.doubleToLongBits(y1) & HI_MASK);
        p_l = (y - y1) * t1 + y * t2;
        p_h = y1 * t1;
        z = p_l + p_h;
        j = (int) (Double.doubleToLongBits(z) >>> HI_SHIFT);
        i = (int) (Double.doubleToLongBits(z) & LO_MASK);
        if (j >= 0x40900000) {        /* z >= 1024 */
            if (((j - 0x40900000) | i) != 0) /* if z > 1024 */ {
                return s * huge * huge;     /* overflow */
            } else {
                if (p_l + ovt > z - p_h) {
                    return s * huge * huge; /* overflow */
                }
            }
        } else if ((j & 0x7fffffff) >= 0x4090cc00) {  /* z <= -1075 */
            if (((j - 0xc090cc00) | i) != 0) /* z < -1075 */ {
                return s * tiny * tiny;   /* underflow */
            } else {
                if (p_l <= z - p_h) {
                    return s * tiny * tiny;  /* underflow */
                }
            }
        }
        /*
         * compute 2**(p_h+p_l)
         */
        i = j & 0x7fffffff;
        k = (i >> 20) - 0x3ff;
        n = 0;
        if (i > 0x3fe00000) {    /* if |z| > 0.5, set n = [z+0.5] */
            n = j + (0x00100000 >> (k + 1));
            k = ((n & 0x7fffffff) >> 20) - 0x3ff; /* new k for n */
            t = zero;
            //__HI(t) = (n&~(0x000fffff>>k));
            t = Double.longBitsToDouble(((long) (n & ~(0x000fffff >> k)) << HI_SHIFT) | (Double.doubleToLongBits(t) & LO_MASK));
            n = ((n & 0x000fffff) | 0x00100000) >> (20 - k);
            if (j < 0) {
                n = -n;
            }
            p_h -= t;
        }
        t = p_l + p_h;
        //__LO(t) = 0; // keep high word
        t = Double.longBitsToDouble(Double.doubleToLongBits(t) & HI_MASK);
        u = t * lg2_h;
        v = (p_l - (t - p_h)) * lg2 + t * lg2_l;
        z = u + v;
        w = v - (z - u);
        t = z * z;
        t1 = z - t * (P1 + t * (P2 + t * (P3 + t * (P4 + t * P5))));
        r = (z * t1) / (t1 - two) - (w + z * w);
        z = one - (r - z);
        j = (int) ((long) Double.doubleToLongBits(z) >>> HI_SHIFT);
        j += (n << 20);
        if ((j >> 20) <= 0) {
            z = scalbn(z, n); /* subnormal output */
        } else //__HI(z) = j;
        {
            z = Double.longBitsToDouble(((long) j << HI_SHIFT) | (Double.doubleToLongBits(z) & LO_MASK));
        }
        return s * z;
    }


    /* __ieee754_acos(x)
     * Method :
     *  acos(x)  = pi/2 - asin(x)
     *  acos(-x) = pi/2 + asin(x)
     * For |x|<=0.5
     *  acos(x) = pi/2 - (x + x*x^2*R(x^2)) (see asin.c)
     * For x>0.5
     *  acos(x) = pi/2 - (pi/2 - 2asin(sqrt((1-x)/2)))
     *    = 2asin(sqrt((1-x)/2))
     *    = 2s + 2s*z*R(z)  ...z=(1-x)/2, s=sqrt(z)
     *    = 2f + (2c + 2s*z*R(z))
     *     where f=hi part of s, and c = (z-f*f)/(s+f) is the correction term
     *     for f so that f+c ~ sqrt(z).
     * For x<-0.5
     *  acos(x) = pi - 2asin(sqrt((1-|x|)/2))
     *    = pi - 0.5*(s+s*z*R(z)), where z=(1-|x|)/2,s=sqrt(z)
     *
     * Special cases:
     *  if x is NaN, return x itself;
     *  if |x|>1, return NaN with invalid signal.
     *
     * Function needed: sqrt
     */
    private static final double ieee754_acos(double x) {
        double z, p, q, r, w, s, c, df;
        int hx, ix;
        hx = (int) (Double.doubleToLongBits(x) >>> HI_SHIFT);
        ix = hx & 0x7fffffff;
        if (ix >= 0x3ff00000) {  /* |x| >= 1 */
            if (((ix - 0x3ff00000) | (int) (Double.doubleToLongBits(x) & LO_MASK)) == 0) {  /* |x|==1 */
                if (hx > 0) {
                    return 0.0;    /* acos(1) = 0  */
                } else {
                    return pi + 2.0 * pio2_lo; /* acos(-1)= pi */
                }
            }
            return (x - x) / (x - x);   /* acos(|x|>1) is NaN */
        }
        if (ix < 0x3fe00000) { /* |x| < 0.5 */
            if (ix <= 0x3c600000) {
                return pio2_hi + pio2_lo;/*if|x|<2**-57*/
            }
            z = x * x;
            p = z * (pS0 + z * (pS1 + z * (pS2 + z * (pS3 + z * (pS4 + z * pS5)))));
            q = one + z * (qS1 + z * (qS2 + z * (qS3 + z * qS4)));
            r = p / q;
            return pio2_hi - (x - (pio2_lo - x * r));
        } else if (hx < 0) {   /* x < -0.5 */
            z = (one + x) * 0.5;
            p = z * (pS0 + z * (pS1 + z * (pS2 + z * (pS3 + z * (pS4 + z * pS5)))));
            q = one + z * (qS1 + z * (qS2 + z * (qS3 + z * qS4)));
            s = Math.sqrt(z);
            r = p / q;
            w = r * s - pio2_lo;
            return pi - 2.0 * (s + w);
        } else {      /* x > 0.5 */
            z = (one - x) * 0.5;
            s = Math.sqrt(z);
            df = s;
            //__LO(df) = 0; // keep high word
            df = Double.longBitsToDouble(Double.doubleToLongBits(df) & HI_MASK);
            c = (z - df * df) / (s + df);
            p = z * (pS0 + z * (pS1 + z * (pS2 + z * (pS3 + z * (pS4 + z * pS5)))));
            q = one + z * (qS1 + z * (qS2 + z * (qS3 + z * qS4)));
            r = p / q;
            w = r * s + c;
            return 2.0 * (df + w);
        }
    }


    /* __ieee754_asin(x)
     * Method :
     *  Since  asin(x) = x + x^3/6 + x^5*3/40 + x^7*15/336 + ...
     *  we approximate asin(x) on [0,0.5] by
     *    asin(x) = x + x*x^2*R(x^2)
     *  where
     *    R(x^2) is a rational approximation of (asin(x)-x)/x^3
     *  and its remez error is bounded by
     *    |(asin(x)-x)/x^3 - R(x^2)| < 2^(-58.75)
     *
     *  For x in [0.5,1]
     *    asin(x) = pi/2-2*asin(sqrt((1-x)/2))
     *  Let y = (1-x), z = y/2, s := sqrt(z), and pio2_hi+pio2_lo=pi/2;
     *  then for x>0.98
     *    asin(x) = pi/2 - 2*(s+s*z*R(z))
     *      = pio2_hi - (2*(s+s*z*R(z)) - pio2_lo)
     *  For x<=0.98, let pio4_hi = pio2_hi/2, then
     *    f = hi part of s;
     *    c = sqrt(z) - f = (z-f*f)/(s+f)   ...f+c=sqrt(z)
     *  and
     *    asin(x) = pi/2 - 2*(s+s*z*R(z))
     *      = pio4_hi+(pio4-2s)-(2s*z*R(z)-pio2_lo)
     *      = pio4_hi+(pio4-2f)-(2s*z*R(z)-(pio2_lo+2c))
     *
     * Special cases:
     *  if x is NaN, return x itself;
     *  if |x|>1, return NaN with invalid signal.
     *
     */
    private static final double ieee754_asin(double x) {
        double t, w, p, q, c, r, s;
        int hx, ix;
        hx = (int) (Double.doubleToLongBits(x) >>> HI_SHIFT);
        ix = hx & 0x7fffffff;
        if (ix >= 0x3ff00000) {   /* |x|>= 1 */
            if (((ix - 0x3ff00000) | (int) (Double.doubleToLongBits(x) & LO_MASK)) == 0) /* asin(1)=+-pi/2 with inexact */ {
                return x * pio2_hi + x * pio2_lo;
            }
            return (x - x) / (x - x);   /* asin(|x|>1) is NaN */
        } else if (ix < 0x3fe00000) { /* |x|<0.5 */
            if (ix < 0x3e400000) {   /* if |x| < 2**-27 */
                if (huge + x > one) {
                    return x;/* return x with inexact if x!=0*/
                }
            } else {
                t = x * x;
                p = t * (pS0 + t * (pS1 + t * (pS2 + t * (pS3 + t * (pS4 + t * pS5)))));
                q = one + t * (qS1 + t * (qS2 + t * (qS3 + t * qS4)));
                w = p / q;
                return x + x * w;
            }
        }
        /* 1> |x|>= 0.5 */
        w = one - Math.abs(x);
        t = w * 0.5;
        p = t * (pS0 + t * (pS1 + t * (pS2 + t * (pS3 + t * (pS4 + t * pS5)))));
        q = one + t * (qS1 + t * (qS2 + t * (qS3 + t * qS4)));
        s = Math.sqrt(t);
        if (ix >= 0x3FEF3333) {  /* if |x| > 0.975 */
            w = p / q;
            t = pio2_hi - (2.0 * (s + s * w) - pio2_lo);
        } else {
            w = s;
            //__LO(w) = 0; // keep the high word
            w = Double.longBitsToDouble(Double.doubleToLongBits(w) & HI_MASK);
            c = (t - w * w) / (s + w);
            r = p / q;
            p = 2.0 * s * r - (pio2_lo - 2.0 * c);
            q = pio4_hi - 2.0 * w;
            t = pio4_hi - (p - q);
        }
        if (hx > 0) {
            return t;
        } else {
            return -t;
        }
    }
    /* atan(x)
     * Method
     *   1. Reduce x to positive by atan(x) = -atan(-x).
     *   2. According to the integer k=4t+0.25 chopped, t=x, the argument
     *      is further reduced to one of the following intervals and the
     *      arctangent of t is evaluated by the corresponding formula:
     *
     *      [0,7/16]      atan(x) = t-t^3*(a1+t^2*(a2+...(a10+t^2*a11)...)
     *      [7/16,11/16]  atan(x) = atan(1/2) + atan( (t-0.5)/(1+t/2) )
     *      [11/16.19/16] atan(x) = atan( 1 ) + atan( (t-1)/(1+t) )
     *      [19/16,39/16] atan(x) = atan(3/2) + atan( (t-1.5)/(1+1.5t) )
     *      [39/16,INF]   atan(x) = atan(INF) + atan( -1/t )
     *
     * Constants:
     * The hexadecimal values are the intended ones for the following
     * constants. The decimal values may be used, provided that the
     * compiler will convert from decimal to binary accurately enough
     * to produce the hexadecimal values shown.
     */
    private static final double atanhi[] = {
        4.63647609000806093515e-01, /* atan(0.5)hi 0x3FDDAC67, 0x0561BB4F */
        7.85398163397448278999e-01, /* atan(1.0)hi 0x3FE921FB, 0x54442D18 */
        9.82793723247329054082e-01, /* atan(1.5)hi 0x3FEF730B, 0xD281F69B */
        1.57079632679489655800e+00, /* atan(inf)hi 0x3FF921FB, 0x54442D18 */};
    private static final double atanlo[] = {
        2.26987774529616870924e-17, /* atan(0.5)lo 0x3C7A2B7F, 0x222F65E2 */
        3.06161699786838301793e-17, /* atan(1.0)lo 0x3C81A626, 0x33145C07 */
        1.39033110312309984516e-17, /* atan(1.5)lo 0x3C700788, 0x7AF0CBBD */
        6.12323399573676603587e-17, /* atan(inf)lo 0x3C91A626, 0x33145C07 */};
    private static final double aT[] = {
        3.33333333333329318027e-01, /* 0x3FD55555, 0x5555550D */
        -1.99999999998764832476e-01, /* 0xBFC99999, 0x9998EBC4 */
        1.42857142725034663711e-01, /* 0x3FC24924, 0x920083FF */
        -1.11111104054623557880e-01, /* 0xBFBC71C6, 0xFE231671 */
        9.09088713343650656196e-02, /* 0x3FB745CD, 0xC54C206E */
        -7.69187620504482999495e-02, /* 0xBFB3B0F2, 0xAF749A6D */
        6.66107313738753120669e-02, /* 0x3FB10D66, 0xA0D03D51 */
        -5.83357013379057348645e-02, /* 0xBFADDE2D, 0x52DEFD9A */
        4.97687799461593236017e-02, /* 0x3FA97B4B, 0x24760DEB */
        -3.65315727442169155270e-02, /* 0xBFA2B444, 0x2C6A6C2F */
        1.62858201153657823623e-02, /* 0x3F90AD3A, 0xE322DA11 */};

    private static final double ieee754_atan(double x) {
        double w, s1, s2, z;
        int ix, hx, id;

        hx = (int) (Double.doubleToLongBits(x) >>> HI_SHIFT);
        ix = hx & 0x7fffffff;
        if (ix >= 0x44100000) {  /* if |x| >= 2^66 */
            if (ix > 0x7ff00000
                    || (ix == 0x7ff00000 && ((int) (Double.doubleToLongBits(x) & LO_MASK) != 0))) {
                return x + x;   /* NaN */
            }
            if (hx > 0) {
                return atanhi[3] + atanlo[3];
            } else {
                return -atanhi[3] - atanlo[3];
            }
        }
        if (ix < 0x3fdc0000) {  /* |x| < 0.4375 */
            if (ix < 0x3e200000) {  /* |x| < 2^-29 */
                if (huge + x > one) {
                    return x;  /* raise inexact */
                }
            }
            id = -1;
        } else {
            x = Math.abs(x);
            if (ix < 0x3ff30000) {    /* |x| < 1.1875 */
                if (ix < 0x3fe60000) {  /* 7/16 <=|x|<11/16 */
                    id = 0;
                    x = (2.0 * x - one) / (2.0 + x);
                } else {      /* 11/16<=|x|< 19/16 */
                    id = 1;
                    x = (x - one) / (x + one);
                }
            } else {
                if (ix < 0x40038000) {  /* |x| < 2.4375 */
                    id = 2;
                    x = (x - 1.5) / (one + 1.5 * x);
                } else {      /* 2.4375 <= |x| < 2^66 */
                    id = 3;
                    x = -1.0 / x;
                }
            }
        }
        /* end of argument reduction */
        z = x * x;
        w = z * z;
        /* break sum from i=0 to 10 aT[i]z**(i+1) into odd and even poly */
        s1 = z * (aT[0] + w * (aT[2] + w * (aT[4] + w * (aT[6] + w * (aT[8] + w * aT[10])))));
        s2 = w * (aT[1] + w * (aT[3] + w * (aT[5] + w * (aT[7] + w * aT[9]))));
        if (id < 0) {
            return x - x * (s1 + s2);
        } else {
            z = atanhi[id] - ((x * (s1 + s2) - atanlo[id]) - x);
            return (hx < 0) ? -z : z;
        }
    }


    /* __ieee754_atan2(y,x)
     * Method :
     *  1. Reduce y to positive by atan2(y,x)=-atan2(-y,x).
     *  2. Reduce x to positive by (if x and y are unexceptional):
     *    ARG (x+iy) = arctan(y/x)       ... if x > 0,
     *    ARG (x+iy) = pi - arctan[y/(-x)]   ... if x < 0,
     *
     * Special cases:
     *
     *  ATAN2((anything), NaN ) is NaN;
     *  ATAN2(NAN , (anything) ) is NaN;
     *  ATAN2(+-0, +(anything but NaN)) is +-0  ;
     *  ATAN2(+-0, -(anything but NaN)) is +-pi ;
     *  ATAN2(+-(anything but 0 and NaN), 0) is +-pi/2;
     *  ATAN2(+-(anything but INF and NaN), +INF) is +-0 ;
     *  ATAN2(+-(anything but INF and NaN), -INF) is +-pi;
     *  ATAN2(+-INF,+INF ) is +-pi/4 ;
     *  ATAN2(+-INF,-INF ) is +-3pi/4;
     *  ATAN2(+-INF, (anything but,0,NaN, and INF)) is +-pi/2;
     *
     * Constants:
     * The hexadecimal values are the intended ones for the following
     * constants. The decimal values may be used, provided that the
     * compiler will convert from decimal to binary accurately enough
     * to produce the hexadecimal values shown.
     */
    private static final double ieee754_atan2(double x, double y) {
        double z;
        int k, m;
        int hx, hy, ix, iy;
        int lx, ly;

        //i0 = (int)((Double.doubleToLongBits(one)) >> (29+HI_SHIFT))^1;
        //i1 = 1-i0;
        hx = (int) (Double.doubleToLongBits(x) >>> HI_SHIFT);
        lx = (int) (Double.doubleToLongBits(x) & LO_MASK);
        hy = (int) (Double.doubleToLongBits(y) >>> HI_SHIFT);
        ly = (int) (Double.doubleToLongBits(y) & LO_MASK);
        ix = hx & 0x7fffffff;
        iy = hy & 0x7fffffff;

        if (((ix | ((lx | -lx) >> 31)) > 0x7ff00000)
                || ((iy | ((ly | -ly) >> 31)) > 0x7ff00000)) /* x or y is NaN */ {
            return x + y;
        }
        if ((hx - 0x3ff00000 | lx) == 0) {
            return ieee754_atan(y);   /* x=1.0 */
        }
        m = ((hy >> 31) & 1) | ((hx >> 30) & 2);  /* 2*sign(x)+sign(y) */

        /* when y = 0 */
        if ((iy | ly) == 0) {
            switch (m) {
                case 0:
                case 1:
                    return y;   /* atan(+-0,+anything)=+-0 */
                case 2:
                    return pi + tiny;/* atan(+0,-anything) = pi */
                case 3:
                    return -pi - tiny;/* atan(-0,-anything) =-pi */
            }
        }
        /* when x = 0 */
        if ((ix | lx) == 0) {
            return (hy < 0) ? -pi_o_2 - tiny : pi_o_2 + tiny;
        }

        /* when x is INF */
        if (ix == 0x7ff00000) {
            if (iy == 0x7ff00000) {
                switch (m) {
                    case 0:
                        return pi_o_4 + tiny;/* atan(+INF,+INF) */
                    case 1:
                        return -pi_o_4 - tiny;/* atan(-INF,+INF) */
                    case 2:
                        return 3.0 * pi_o_4 + tiny;/*atan(+INF,-INF)*/
                    case 3:
                        return -3.0 * pi_o_4 - tiny;/*atan(-INF,-INF)*/
                }
            } else {
                switch (m) {
                    case 0:
                        return zero; /* atan(+...,+INF) */
                    case 1:
                        return -zero; /* atan(-...,+INF) */
                    case 2:
                        return pi + tiny;  /* atan(+...,-INF) */
                    case 3:
                        return -pi - tiny;  /* atan(-...,-INF) */
                }
            }
        }
        /* when y is INF */
        if (iy == 0x7ff00000) {
            return (hy < 0) ? -pi_o_2 - tiny : pi_o_2 + tiny;
        }

        /* compute y/x */
        k = (iy - ix) >> 20;
        if (k > 60) {
            z = pi_o_2 + 0.5 * pi_lo;  /* |y/x| >  2**60 */
        } else if (hx < 0 && k < -60) {
            z = 0.0;   /* |y|/x < -2**60 */
        } else {
            z = ieee754_atan(Math.abs(y / x));   /* safe to do y/x */
        }
        switch (m) {
            case 0:
                return z; /* atan(+,+) */
            case 1:
                z = Double.longBitsToDouble(Double.doubleToLongBits(z) ^ 0x80000000); // __HI(z) ^= 0x80000000;
                return z; /* atan(-,+) */
            case 2:
                return pi - (z - pi_lo);/* atan(+,-) */
            default: /* case 3 */
                return (z - pi_lo) - pi;/* atan(-,-) */
        }
    }

    /**
     * scalbn (double x, int n)
     * scalbn(x,n) returns x* 2**n  computed by  exponent
     * manipulation rather than by actually performing an
     * exponentiation or a multiplication.
     */
    public static final double scalbn(double x, int n) {
        int k, hx, lx;
        hx = (int) (Double.doubleToLongBits(x) >>> HI_SHIFT);
        lx = (int) (Double.doubleToLongBits(x) & LO_MASK);
        k = (hx & 0x7ff00000) >> 20;    /* extract exponent */
        if (k == 0) {       /* 0 or subnormal x */
            if ((lx | (hx & 0x7fffffff)) == 0) {
                return x; /* +-0 */
            }
            x *= two54;
            hx = (int) (Double.doubleToLongBits(x) >>> HI_SHIFT);
            k = ((hx & 0x7ff00000) >> 20) - 54;
            if (n < -50000) {
                return tiny * x;   /*underflow*/
            }
        }
        if (k == 0x7ff) {
            return x + x;   /* NaN or Inf */
        }
        k = k + n;
        if (k > 0x7fe) {
            return huge * copysign(huge, x); /* overflow  */
        }
        if (k > 0) /* normal result */ {
            //__HI(x) = (hx&0x800fffff)|(k<<20);
            x = Double.longBitsToDouble(((long) ((int) (hx & 0x800fffff) | (k << 20)) << HI_SHIFT) | (Double.doubleToLongBits(x) & LO_MASK));
            return x;
        }
        if (k <= -54) {
            if (n > 50000) /* in case integer overflow in n+k */ {
                return huge * copysign(huge, x); /*overflow*/
            } else {
                return tiny * copysign(tiny, x);  /*underflow*/
            }
        }
        k += 54;        /* subnormal result */
        //__HI(x) = (hx&0x800fffff)|(k<<20);
        x = Double.longBitsToDouble(((long) ((int) (hx & 0x800fffff) | (k << 20)) << HI_SHIFT) | (Double.doubleToLongBits(x) & LO_MASK));
        return x * twom54;
    }

    /*
     * copysign(double x, double y)
     * copysign(x,y) returns a value with the magnitude of x and
     * with the sign bit of y.
     */
    public static final double copysign(final double x, final double y) {
        //__HI(x) = (__HI(x)&0x7fffffff)|(__HI(y)&0x80000000);
        // The below is actually about 30% faster than doing greater/less comparisons.
        return Double.longBitsToDouble((Double.doubleToLongBits(x) & 0x7fffffffffffffffL)
                | (Double.doubleToLongBits(y) & 0x8000000000000000L));
    }

    /*
     * fabs(x) returns the absolute value of x.
     * This is already handled by Java ME.
    public static final double fabs(double x)
    {
    //__HI(x) &= 0x7fffffff;
    //return Double.longBitsToDouble(Double.doubleToLongBits(x) & 0x7fffffffffffffffL);
    }
     */
}