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
package com.codename1.spreadsheet;

import com.codename1.util.MathUtil;

/// The probability distributions and the tests built on them.
///
/// Everything here rests on three special functions: the logarithm of the gamma function,
/// the regularized incomplete gamma function and the regularized incomplete beta function.
/// An inverse is found by bisection on the distribution itself, which is slow next to a
/// closed form and never disagrees with the function it inverts.
final class DistributionFunctions {
    static final String SCALAR_NAMES = "BETA.DIST BETA.INV BETADIST BETAINV BINOM.DIST BINOM.DIST.RANGE BINOM.INV"
            + " BINOMDIST CHIDIST CHIINV CHISQ.DIST CHISQ.DIST.RT CHISQ.INV CHISQ.INV.RT CONFIDENCE CONFIDENCE.NORM"
            + " CONFIDENCE.T CRITBINOM EXPON.DIST EXPONDIST F.DIST F.DIST.RT F.INV F.INV.RT FDIST FINV GAMMA GAMMA.DIST"
            + " GAMMA.INV GAMMADIST GAMMAINV GAMMALN GAMMALN.PRECISE GAUSS HYPGEOM.DIST HYPGEOMDIST LOGINV LOGNORM.DIST"
            + " LOGNORM.INV LOGNORMDIST NEGBINOM.DIST NEGBINOMDIST NORM.DIST NORM.INV NORM.S.DIST NORM.S.INV NORMDIST"
            + " NORMINV NORMSDIST NORMSINV PHI POISSON POISSON.DIST T.DIST T.DIST.2T T.DIST.RT T.INV T.INV.2T TDIST TINV"
            + " WEIBULL WEIBULL.DIST";

    static final String RANGE_NAMES = "CHISQ.TEST CHITEST F.TEST FTEST T.TEST TTEST Z.TEST ZTEST";

    private static final double[] LANCZOS = {0.99999999999980993, 676.5203681218851, -1259.1392167224028,
        771.32342877765313, -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6,
        1.5056327351493116e-7};

    private DistributionFunctions() {
    }

    private static Double d(double v) {
        return Double.valueOf(v);
    }

    private static double exp(double x) {
        return MathUtil.exp(x);
    }

    private static double log(double x) {
        return MathUtil.log(x);
    }

    static double lgamma(double x) {
        if (x < 0.5) {
            // reflection
            return log(Math.PI / Math.abs(Math.sin(Math.PI * x))) - lgamma(1 - x);
        }
        x -= 1;
        double sum = LANCZOS[0];
        double t = x + 7.5;
        for (int i = 1; i < 9; i++) {
            sum += LANCZOS[i] / (x + i);
        }
        return 0.5 * log(2 * Math.PI) + (x + 0.5) * log(t) - t + log(sum);
    }

    static double gamma(double x) {
        if (x == Math.floor(x)) {
            if (x <= 0) {
                throw EvalError.NUM;
            }
            if (x <= 171) {
                return MathFunctions.fact(x - 1);
            }
        }
        if (x < 0.5) {
            return Math.PI / (Math.sin(Math.PI * x) * gamma(1 - x));
        }
        if (x > 171.7) {
            throw EvalError.NUM;
        }
        return exp(lgamma(x));
    }

    /// The regularized lower incomplete gamma function P(a, x).
    static double gammaP(double a, double x) {
        if (x <= 0) {
            return 0;
        }
        if (x < a + 1) {
            double term = 1 / a;
            double sum = term;
            for (int n = 1; n < 1000; n++) {
                term *= x / (a + n);
                sum += term;
                if (Math.abs(term) < Math.abs(sum) * 1e-16) {
                    break;
                }
            }
            return sum * exp(-x + a * log(x) - lgamma(a));
        }
        return 1 - gammaQ(a, x);
    }

    /// The regularized upper incomplete gamma function Q(a, x).
    static double gammaQ(double a, double x) {
        if (x <= 0) {
            return 1;
        }
        if (x < a + 1) {
            return 1 - gammaP(a, x);
        }
        double tiny = 1e-300;
        double b = x + 1 - a;
        double c = 1 / tiny;
        double dd = 1 / b;
        double h = dd;
        for (int i = 1; i < 1000; i++) {
            double an = -i * (i - a);
            b += 2;
            dd = an * dd + b;
            if (Math.abs(dd) < tiny) {
                dd = tiny;
            }
            c = b + an / c;
            if (Math.abs(c) < tiny) {
                c = tiny;
            }
            dd = 1 / dd;
            double delta = dd * c;
            h *= delta;
            if (Math.abs(delta - 1) < 1e-16) {
                break;
            }
        }
        return exp(-x + a * log(x) - lgamma(a)) * h;
    }

    /// The regularized incomplete beta function I(x; a, b).
    static double betaI(double x, double a, double b) {
        if (x <= 0) {
            return 0;
        }
        if (x >= 1) {
            return 1;
        }
        double front = exp(lgamma(a + b) - lgamma(a) - lgamma(b) + a * log(x) + b * log(1 - x));
        if (x < (a + 1) / (a + b + 2)) {
            return front * betaFraction(x, a, b) / a;
        }
        return 1 - front * betaFraction(1 - x, b, a) / b;
    }

    private static double betaFraction(double x, double a, double b) {
        double tiny = 1e-300;
        double c = 1;
        double dd = 1 - (a + b) * x / (a + 1);
        if (Math.abs(dd) < tiny) {
            dd = tiny;
        }
        dd = 1 / dd;
        double h = dd;
        for (int m = 1; m < 1000; m++) {
            int m2 = 2 * m;
            double aa = m * (b - m) * x / ((a + m2 - 1) * (a + m2));
            dd = 1 + aa * dd;
            if (Math.abs(dd) < tiny) {
                dd = tiny;
            }
            c = 1 + aa / c;
            if (Math.abs(c) < tiny) {
                c = tiny;
            }
            dd = 1 / dd;
            h *= dd * c;
            aa = -(a + m) * (a + b + m) * x / ((a + m2) * (a + m2 + 1));
            dd = 1 + aa * dd;
            if (Math.abs(dd) < tiny) {
                dd = tiny;
            }
            c = 1 + aa / c;
            if (Math.abs(c) < tiny) {
                c = tiny;
            }
            dd = 1 / dd;
            double delta = dd * c;
            h *= delta;
            if (Math.abs(delta - 1) < 1e-16) {
                break;
            }
        }
        return h;
    }

    static double erf(double x) {
        if (x < 0) {
            return -erf(-x);
        }
        return gammaP(0.5, x * x);
    }

    static double erfc(double x) {
        if (x < 0) {
            return 2 - erfc(-x);
        }
        return gammaQ(0.5, x * x);
    }

    static double normCdf(double z) {
        return 0.5 * erfc(-z / Math.sqrt(2));
    }

    static double normPdf(double z) {
        return exp(-z * z / 2) / Math.sqrt(2 * Math.PI);
    }

    /// The inverse of the standard normal distribution: Acklam's rational approximation and
    /// one step of Halley's method on top of it.
    static double normInv(double p) {
        if (p <= 0 || p >= 1) {
            throw EvalError.NUM;
        }
        double[] a = {-3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02, 1.383577518672690e+02,
            -3.066479806614716e+01, 2.506628277459239e+00};
        double[] b = {-5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02, 6.680131188771972e+01,
            -1.328068155288572e+01};
        double[] c = {-7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00, -2.549732539343734e+00,
            4.374664141464968e+00, 2.938163982698783e+00};
        double[] dd = {7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00};
        double x;
        if (p < 0.02425) {
            double q = Math.sqrt(-2 * log(p));
            x = (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
                    / ((((dd[0] * q + dd[1]) * q + dd[2]) * q + dd[3]) * q + 1);
        } else if (p > 1 - 0.02425) {
            double q = Math.sqrt(-2 * log(1 - p));
            x = -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
                    / ((((dd[0] * q + dd[1]) * q + dd[2]) * q + dd[3]) * q + 1);
        } else {
            double q = p - 0.5;
            double r = q * q;
            x = (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q
                    / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1);
        }
        for (int i = 0; i < 2; i++) {
            double e = normCdf(x) - p;
            double u = e / normPdf(x);
            x -= u / (1 + x * u / 2);
        }
        return x;
    }

    static double tCdf(double t, double df) {
        double x = df / (df + t * t);
        double tail = 0.5 * betaI(x, df / 2, 0.5);
        return t > 0 ? 1 - tail : tail;
    }

    static double fCdf(double x, double d1, double d2) {
        if (x <= 0) {
            return 0;
        }
        return betaI(d1 * x / (d1 * x + d2), d1 / 2, d2 / 2);
    }

    private static final int F_GAMMA = 0;
    private static final int F_BETA = 1;
    private static final int F_T = 2;
    private static final int F_F = 3;

    private static double cdf(int which, double x, double p1, double p2) {
        switch (which) {
            case F_GAMMA:
                return gammaP(p1, x / p2);
            case F_BETA:
                return betaI(x, p1, p2);
            case F_T:
                return tCdf(x, p1);
            default:
                return fCdf(x, p1, p2);
        }
    }

    /// The x at which an increasing distribution reaches p.
    private static double inverse(int which, double p, double p1, double p2, double lo, double hi) {
        if (p < 0 || p > 1) {
            throw EvalError.NUM;
        }
        if (Double.isInfinite(hi)) {
            hi = Math.max(1, lo + 1);
            for (int i = 0; i < 1100 && cdf(which, hi, p1, p2) < p; i++) {
                hi *= 2;
            }
        }
        if (Double.isInfinite(lo)) {
            lo = -1;
            for (int i = 0; i < 1100 && cdf(which, lo, p1, p2) > p; i++) {
                lo *= 2;
            }
        }
        for (int i = 0; i < 200; i++) {
            double mid = (lo + hi) / 2;
            if (mid == lo || mid == hi) {
                break;
            }
            if (cdf(which, mid, p1, p2) < p) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2;
    }

    private static double binomPmf(double k, double n, double p) {
        if (k < 0 || k > n) {
            return 0;
        }
        if (p == 0) {
            return k == 0 ? 1 : 0;
        }
        if (p == 1) {
            return k == n ? 1 : 0;
        }
        return exp(lgamma(n + 1) - lgamma(k + 1) - lgamma(n - k + 1) + k * log(p) + (n - k) * log(1 - p));
    }

    private static double binomCdf(double k, double n, double p) {
        if (k < 0) {
            return 0;
        }
        if (k >= n) {
            return 1;
        }
        return 1 - betaI(p, k + 1, n - k);
    }

    private static void probability(double p) {
        if (p < 0 || p > 1) {
            throw EvalError.NUM;
        }
    }

    private static void positive(double v) {
        if (v <= 0) {
            throw EvalError.NUM;
        }
    }

    static Object call(String name, Evaluator ev, Object[] a) {
        switch (Functions.index(SWITCH_1, name)) {
            case 0: // NORM.DIST
            case 1: { // NORMDIST
                double sd = Evaluator.num(a, 2);
                positive(sd);
                double z = (Evaluator.num(a, 0) - Evaluator.num(a, 1)) / sd;
                return d(Evaluator.bool(a, 3, true) ? normCdf(z) : normPdf(z) / sd);
            }
            case 2: // NORM.S.DIST
                return d(Evaluator.bool(a, 1, true) ? normCdf(Evaluator.num(a, 0)) : normPdf(Evaluator.num(a, 0)));
            case 3: // NORMSDIST
                return d(normCdf(Evaluator.num(a, 0)));
            case 4: // GAUSS
                return d(normCdf(Evaluator.num(a, 0)) - 0.5);
            case 5: // PHI
                return d(normPdf(Evaluator.num(a, 0)));
            case 6: // NORM.INV
            case 7: { // NORMINV
                double sd = Evaluator.num(a, 2);
                positive(sd);
                return d(Evaluator.num(a, 1) + sd * normInv(Evaluator.num(a, 0)));
            }
            case 8: // NORM.S.INV
            case 9: // NORMSINV
                return d(normInv(Evaluator.num(a, 0)));
            case 10: // LOGNORM.DIST
            case 11: { // LOGNORMDIST
                double x = Evaluator.num(a, 0);
                double sd = Evaluator.num(a, 2);
                positive(x);
                positive(sd);
                double z = (log(x) - Evaluator.num(a, 1)) / sd;
                boolean cumulative = "LOGNORMDIST".equals(name) || Evaluator.bool(a, 3, true);
                return d(cumulative ? normCdf(z) : normPdf(z) / (x * sd));
            }
            case 12: // LOGNORM.INV
            case 13: { // LOGINV
                double sd = Evaluator.num(a, 2);
                positive(sd);
                return d(exp(Evaluator.num(a, 1) + sd * normInv(Evaluator.num(a, 0))));
            }
            case 14: // EXPON.DIST
            case 15: { // EXPONDIST
                double x = Evaluator.num(a, 0);
                double lambda = Evaluator.num(a, 1);
                positive(lambda);
                if (x < 0) {
                    throw EvalError.NUM;
                }
                return d(Evaluator.bool(a, 2, true) ? 1 - exp(-lambda * x) : lambda * exp(-lambda * x));
            }
            case 16: // POISSON
            case 17: { // POISSON.DIST
                double k = Math.floor(Evaluator.num(a, 0));
                double mean = Evaluator.num(a, 1);
                if (k < 0 || mean < 0) {
                    throw EvalError.NUM;
                }
                if (Evaluator.bool(a, 2, true)) {
                    return d(gammaQ(k + 1, mean));
                }
                return d(mean == 0 ? k == 0 ? 1 : 0 : exp(k * log(mean) - mean - lgamma(k + 1)));
            }
            case 18: // WEIBULL
            case 19: { // WEIBULL.DIST
                double x = Evaluator.num(a, 0);
                double alpha = Evaluator.num(a, 1);
                double beta = Evaluator.num(a, 2);
                positive(alpha);
                positive(beta);
                if (x < 0) {
                    throw EvalError.NUM;
                }
                double e = exp(-MathUtil.pow(x / beta, alpha));
                if (Evaluator.bool(a, 3, true)) {
                    return d(1 - e);
                }
                return d(alpha / beta * MathUtil.pow(x / beta, alpha - 1) * e);
            }
            case 20: // GAMMA
                return d(gamma(Evaluator.num(a, 0)));
            case 21: // GAMMALN
            case 22: { // GAMMALN.PRECISE
                double x = Evaluator.num(a, 0);
                positive(x);
                return d(lgamma(x));
            }
            case 23: // GAMMA.DIST
            case 24: { // GAMMADIST
                double x = Evaluator.num(a, 0);
                double alpha = Evaluator.num(a, 1);
                double beta = Evaluator.num(a, 2);
                positive(alpha);
                positive(beta);
                if (x < 0) {
                    throw EvalError.NUM;
                }
                if (Evaluator.bool(a, 3, true)) {
                    return d(gammaP(alpha, x / beta));
                }
                if (x == 0) {
                    return d(alpha == 1 ? 1 / beta : 0);
                }
                return d(exp((alpha - 1) * log(x) - x / beta - lgamma(alpha) - alpha * log(beta)));
            }
            case 25: // GAMMA.INV
            case 26: { // GAMMAINV
                double p = Evaluator.num(a, 0);
                double alpha = Evaluator.num(a, 1);
                double beta = Evaluator.num(a, 2);
                positive(alpha);
                positive(beta);
                if (p >= 1) {
                    throw EvalError.NUM;
                }
                return d(inverse(F_GAMMA, p, alpha, beta, 0, Double.POSITIVE_INFINITY));
            }
            case 27: // BETA.DIST
            case 28: { // BETADIST
                boolean modern = "BETA.DIST".equals(name);
                double x = Evaluator.num(a, 0);
                double alpha = Evaluator.num(a, 1);
                double beta = Evaluator.num(a, 2);
                boolean cumulative = !modern || Evaluator.bool(a, 3, true);
                double lo = Evaluator.num(a, modern ? 4 : 3, 0);
                double hi = Evaluator.num(a, modern ? 5 : 4, 1);
                positive(alpha);
                positive(beta);
                if (x < lo || x > hi || lo == hi) {
                    throw EvalError.NUM;
                }
                double u = (x - lo) / (hi - lo);
                if (cumulative) {
                    return d(betaI(u, alpha, beta));
                }
                if (u <= 0 || u >= 1) {
                    return d(0);
                }
                return d(exp(lgamma(alpha + beta) - lgamma(alpha) - lgamma(beta) + (alpha - 1) * log(u)
                        + (beta - 1) * log(1 - u)) / (hi - lo));
            }
            case 29: // BETA.INV
            case 30: { // BETAINV
                double p = Evaluator.num(a, 0);
                double alpha = Evaluator.num(a, 1);
                double beta = Evaluator.num(a, 2);
                double lo = Evaluator.num(a, 3, 0);
                double hi = Evaluator.num(a, 4, 1);
                positive(alpha);
                positive(beta);
                return d(lo + (hi - lo) * inverse(F_BETA, p, alpha, beta, 0, 1));
            }
            case 31: { // CHISQ.DIST
                double x = Evaluator.num(a, 0);
                double df = Math.floor(Evaluator.num(a, 1));
                if (x < 0 || df < 1) {
                    throw EvalError.NUM;
                }
                if (Evaluator.bool(a, 2, true)) {
                    return d(gammaP(df / 2, x / 2));
                }
                if (x == 0) {
                    return d(df == 2 ? 0.5 : 0);
                }
                return d(exp((df / 2 - 1) * log(x) - x / 2 - lgamma(df / 2) - df / 2 * log(2)));
            }
            case 32: // CHISQ.DIST.RT
            case 33: { // CHIDIST
                double x = Evaluator.num(a, 0);
                double df = Math.floor(Evaluator.num(a, 1));
                if (x < 0 || df < 1) {
                    throw EvalError.NUM;
                }
                return d(gammaQ(df / 2, x / 2));
            }
            case 34: // CHISQ.INV
            case 35: // CHISQ.INV.RT
            case 36: { // CHIINV
                double p = Evaluator.num(a, 0);
                double df = Math.floor(Evaluator.num(a, 1));
                probability(p);
                if (df < 1) {
                    throw EvalError.NUM;
                }
                if (!"CHISQ.INV".equals(name)) {
                    p = 1 - p;
                }
                if (p >= 1) {
                    throw EvalError.NUM;
                }
                return d(inverse(F_GAMMA, p, df / 2, 2, 0, Double.POSITIVE_INFINITY));
            }
            case 37: { // T.DIST
                double t = Evaluator.num(a, 0);
                double df = Math.floor(Evaluator.num(a, 1));
                if (df < 1) {
                    throw EvalError.NUM;
                }
                if (Evaluator.bool(a, 2, true)) {
                    return d(tCdf(t, df));
                }
                return d(exp(lgamma((df + 1) / 2) - lgamma(df / 2)) / Math.sqrt(df * Math.PI)
                        * MathUtil.pow(1 + t * t / df, -(df + 1) / 2));
            }
            case 38: // T.DIST.RT
            case 39: // T.DIST.2T
            case 40: { // TDIST
                double t = Evaluator.num(a, 0);
                double df = Math.floor(Evaluator.num(a, 1));
                if (df < 1) {
                    throw EvalError.NUM;
                }
                if ("T.DIST.RT".equals(name)) {
                    return d(1 - tCdf(t, df));
                }
                if (t < 0) {
                    throw EvalError.NUM;
                }
                double tails = "TDIST".equals(name) ? Evaluator.num(a, 2) : 2;
                if (tails != 1 && tails != 2) {
                    throw EvalError.NUM;
                }
                return d(tails * (1 - tCdf(t, df)));
            }
            case 41: // T.INV
            case 42: // T.INV.2T
            case 43: { // TINV
                double p = Evaluator.num(a, 0);
                double df = Math.floor(Evaluator.num(a, 1));
                if (p <= 0 || p > 1 || df < 1) {
                    throw EvalError.NUM;
                }
                if ("T.INV".equals(name)) {
                    if (p >= 1) {
                        throw EvalError.NUM;
                    }
                    return d(inverse(F_T, p, df, 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
                }
                return d(inverse(F_T, 1 - p / 2, df, 0, 0, Double.POSITIVE_INFINITY));
            }
            case 44: { // F.DIST
                double x = Evaluator.num(a, 0);
                double d1 = Math.floor(Evaluator.num(a, 1));
                double d2 = Math.floor(Evaluator.num(a, 2));
                if (x < 0 || d1 < 1 || d2 < 1) {
                    throw EvalError.NUM;
                }
                if (Evaluator.bool(a, 3, true)) {
                    return d(fCdf(x, d1, d2));
                }
                if (x == 0) {
                    return d(d1 == 2 ? 1 : 0);
                }
                return d(exp(lgamma((d1 + d2) / 2) - lgamma(d1 / 2) - lgamma(d2 / 2) + d1 / 2 * log(d1 / d2)
                        + (d1 / 2 - 1) * log(x) - (d1 + d2) / 2 * log(1 + d1 * x / d2)));
            }
            case 45: // F.DIST.RT
            case 46: { // FDIST
                double x = Evaluator.num(a, 0);
                double d1 = Math.floor(Evaluator.num(a, 1));
                double d2 = Math.floor(Evaluator.num(a, 2));
                if (x < 0 || d1 < 1 || d2 < 1) {
                    throw EvalError.NUM;
                }
                return d(1 - fCdf(x, d1, d2));
            }
            case 47: // F.INV
            case 48: // F.INV.RT
            case 49: { // FINV
                double p = Evaluator.num(a, 0);
                double d1 = Math.floor(Evaluator.num(a, 1));
                double d2 = Math.floor(Evaluator.num(a, 2));
                probability(p);
                if (d1 < 1 || d2 < 1) {
                    throw EvalError.NUM;
                }
                if (!"F.INV".equals(name)) {
                    p = 1 - p;
                }
                if (p >= 1) {
                    throw EvalError.NUM;
                }
                return d(inverse(F_F, p, d1, d2, 0, Double.POSITIVE_INFINITY));
            }
            case 50: // BINOM.DIST
            case 51: { // BINOMDIST
                double k = Math.floor(Evaluator.num(a, 0));
                double n = Math.floor(Evaluator.num(a, 1));
                double p = Evaluator.num(a, 2);
                probability(p);
                if (k < 0 || k > n) {
                    throw EvalError.NUM;
                }
                return d(Evaluator.bool(a, 3, false) ? binomCdf(k, n, p) : binomPmf(k, n, p));
            }
            case 52: { // BINOM.DIST.RANGE
                double n = Math.floor(Evaluator.num(a, 0));
                double p = Evaluator.num(a, 1);
                double lo = Math.floor(Evaluator.num(a, 2));
                double hi = Evaluator.has(a, 3) ? Math.floor(Evaluator.num(a, 3)) : lo;
                probability(p);
                if (lo < 0 || lo > hi || hi > n) {
                    throw EvalError.NUM;
                }
                return d(binomCdf(hi, n, p) - binomCdf(lo - 1, n, p));
            }
            case 53: // BINOM.INV
            case 54: { // CRITBINOM
                double n = Math.floor(Evaluator.num(a, 0));
                double p = Evaluator.num(a, 1);
                double alpha = Evaluator.num(a, 2);
                probability(p);
                probability(alpha);
                if (n < 0 || n > 1e7) {
                    throw EvalError.NUM;
                }
                double total = 0;
                for (double k = 0; k <= n; k++) {
                    total += binomPmf(k, n, p);
                    if (total >= alpha - 1e-15) {
                        return d(k);
                    }
                }
                return d(n);
            }
            case 55: // NEGBINOM.DIST
            case 56: { // NEGBINOMDIST
                double f = Math.floor(Evaluator.num(a, 0));
                double s = Math.floor(Evaluator.num(a, 1));
                double p = Evaluator.num(a, 2);
                probability(p);
                if (f < 0 || s < 1) {
                    throw EvalError.NUM;
                }
                if ("NEGBINOM.DIST".equals(name) && Evaluator.bool(a, 3, false)) {
                    return d(betaI(p, s, f + 1));
                }
                return d(exp(lgamma(f + s) - lgamma(f + 1) - lgamma(s) + s * log(p) + (p == 1 ? 0 : f * log(1 - p))));
            }
            case 57: // HYPGEOM.DIST
            case 58: { // HYPGEOMDIST
                double k = Math.floor(Evaluator.num(a, 0));
                double n = Math.floor(Evaluator.num(a, 1));
                double m = Math.floor(Evaluator.num(a, 2));
                double pop = Math.floor(Evaluator.num(a, 3));
                if (k < 0 || n <= 0 || m < 0 || pop <= 0 || n > pop || m > pop || k > n || k > m) {
                    throw EvalError.NUM;
                }
                if ("HYPGEOM.DIST".equals(name) && Evaluator.bool(a, 4, false)) {
                    double total = 0;
                    for (double i = 0; i <= k; i++) {
                        total += hypergeometric(i, n, m, pop);
                    }
                    return d(total);
                }
                return d(hypergeometric(k, n, m, pop));
            }
            case 59: // CONFIDENCE
            case 60: // CONFIDENCE.NORM
            case 61: { // CONFIDENCE.T
                double alpha = Evaluator.num(a, 0);
                double sd = Evaluator.num(a, 1);
                double n = Math.floor(Evaluator.num(a, 2));
                if (alpha <= 0 || alpha >= 1 || sd <= 0 || n < 1) {
                    throw EvalError.NUM;
                }
                if ("CONFIDENCE.T".equals(name)) {
                    if (n < 2) {
                        throw EvalError.DIV0;
                    }
                    return d(inverse(F_T, 1 - alpha / 2, n - 1, 0, 0, Double.POSITIVE_INFINITY) * sd / Math.sqrt(n));
                }
                return d(normInv(1 - alpha / 2) * sd / Math.sqrt(n));
            }
            default:
                return tests(name, ev, a);
        }
    }

    private static double hypergeometric(double k, double n, double m, double pop) {
        if (k < Math.max(0, n + m - pop) || k > Math.min(n, m)) {
            return 0;
        }
        return exp(lchoose(m, k) + lchoose(pop - m, n - k) - lchoose(pop, n));
    }

    private static double lchoose(double n, double k) {
        return lgamma(n + 1) - lgamma(k + 1) - lgamma(n - k + 1);
    }

    private static Object tests(String name, Evaluator ev, Object[] a) {
        switch (Functions.index(SWITCH_2, name)) {
            case 0: // Z.TEST
            case 1: { // ZTEST
                DoubleList list = new DoubleList();
                ev.numbers(Evaluator.arg(a, 0), list, false);
                double x = ev.scalarNum(a, 1);
                double sd = Evaluator.has(a, 2) ? ev.scalarNum(a, 2) : Math.sqrt(StatFunctions.variance(list, true));
                if (sd <= 0) {
                    throw EvalError.DIV0;
                }
                return d(1 - normCdf((list.mean() - x) / (sd / Math.sqrt(list.size))));
            }
            case 2: // F.TEST
            case 3: { // FTEST
                DoubleList x = new DoubleList();
                DoubleList y = new DoubleList();
                ev.numbers(Evaluator.arg(a, 0), x, false);
                ev.numbers(Evaluator.arg(a, 1), y, false);
                double vx = StatFunctions.variance(x, true);
                double vy = StatFunctions.variance(y, true);
                if (vx == 0 || vy == 0) {
                    throw EvalError.DIV0;
                }
                double p = fCdf(vx / vy, x.size - 1, y.size - 1);
                return d(2 * Math.min(p, 1 - p));
            }
            case 4: // T.TEST
            case 5: { // TTEST
                int tails = (int) ev.scalarNum(a, 2);
                int type = (int) ev.scalarNum(a, 3);
                if (tails != 1 && tails != 2 || type < 1 || type > 3) {
                    throw EvalError.NUM;
                }
                double t;
                double df;
                if (type == 1) {
                    double[][] pairs = StatFunctions.pairs(ev, a[0], a[1]);
                    DoubleList diff = new DoubleList();
                    for (int i = 0; i < pairs[0].length; i++) {
                        diff.add(pairs[0][i] - pairs[1][i]);
                    }
                    double sd = Math.sqrt(StatFunctions.variance(diff, true));
                    if (sd == 0) {
                        throw EvalError.DIV0;
                    }
                    t = diff.mean() / (sd / Math.sqrt(diff.size));
                    df = diff.size - 1;
                } else {
                    DoubleList x = new DoubleList();
                    DoubleList y = new DoubleList();
                    ev.numbers(a[0], x, false);
                    ev.numbers(a[1], y, false);
                    double vx = StatFunctions.variance(x, true);
                    double vy = StatFunctions.variance(y, true);
                    double nx = x.size;
                    double ny = y.size;
                    if (type == 2) {
                        df = nx + ny - 2;
                        double pooled = ((nx - 1) * vx + (ny - 1) * vy) / df;
                        t = (x.mean() - y.mean()) / Math.sqrt(pooled * (1 / nx + 1 / ny));
                    } else {
                        double sx = vx / nx;
                        double sy = vy / ny;
                        t = (x.mean() - y.mean()) / Math.sqrt(sx + sy);
                        df = (sx + sy) * (sx + sy) / (sx * sx / (nx - 1) + sy * sy / (ny - 1));
                    }
                }
                return d(tails * (1 - tCdf(Math.abs(t), df)));
            }
            case 6: // CHISQ.TEST
            case 7: { // CHITEST
                Object[][] actual = ev.matrix(Evaluator.arg(a, 0));
                Object[][] expected = ev.matrix(Evaluator.arg(a, 1));
                int rows = actual.length;
                int cols = Evaluator.width(actual);
                if (rows != expected.length || cols != Evaluator.width(expected)) {
                    throw EvalError.NA;
                }
                double chi = 0;
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        if (actual[r][c] instanceof Double && expected[r][c] instanceof Double) {
                            double e = ((Double) expected[r][c]).doubleValue();
                            double o = ((Double) actual[r][c]).doubleValue();
                            if (e == 0) {
                                throw EvalError.DIV0;
                            }
                            chi += (o - e) * (o - e) / e;
                        }
                    }
                }
                double df = rows > 1 && cols > 1 ? (rows - 1) * (cols - 1) : rows * cols - 1;
                if (df < 1) {
                    throw EvalError.NA;
                }
                return d(gammaQ(df / 2, chi / 2));
            }
            default:
                return CellError.NAME;
        }
    }

    private static final java.util.HashMap<String, Integer> SWITCH_1 = Functions.switchTable(
            "NORM.DIST", "NORMDIST", "NORM.S.DIST", "NORMSDIST", "GAUSS", "PHI", "NORM.INV",
            "NORMINV", "NORM.S.INV", "NORMSINV", "LOGNORM.DIST", "LOGNORMDIST", "LOGNORM.INV",
            "LOGINV", "EXPON.DIST", "EXPONDIST", "POISSON", "POISSON.DIST", "WEIBULL",
            "WEIBULL.DIST", "GAMMA", "GAMMALN", "GAMMALN.PRECISE", "GAMMA.DIST", "GAMMADIST",
            "GAMMA.INV", "GAMMAINV", "BETA.DIST", "BETADIST", "BETA.INV", "BETAINV", "CHISQ.DIST",
            "CHISQ.DIST.RT", "CHIDIST", "CHISQ.INV", "CHISQ.INV.RT", "CHIINV", "T.DIST",
            "T.DIST.RT", "T.DIST.2T", "TDIST", "T.INV", "T.INV.2T", "TINV", "F.DIST", "F.DIST.RT",
            "FDIST", "F.INV", "F.INV.RT", "FINV", "BINOM.DIST", "BINOMDIST", "BINOM.DIST.RANGE",
            "BINOM.INV", "CRITBINOM", "NEGBINOM.DIST", "NEGBINOMDIST", "HYPGEOM.DIST",
            "HYPGEOMDIST", "CONFIDENCE", "CONFIDENCE.NORM", "CONFIDENCE.T");

    private static final java.util.HashMap<String, Integer> SWITCH_2 = Functions.switchTable(
            "Z.TEST", "ZTEST", "F.TEST", "FTEST", "T.TEST", "TTEST", "CHISQ.TEST", "CHITEST");
}
