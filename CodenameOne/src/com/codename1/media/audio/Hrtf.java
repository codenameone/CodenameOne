/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.media.audio;

import com.codename1.util.MathUtil;

/// Binaural rendering of one mono source from a structural model of the head,
/// after C. P. Brown and R. O. Duda, "A Structural Model for Binaural Sound
/// Synthesis", IEEE Transactions on Speech and Audio Processing 6(5), 1998:
///
/// - an interaural time difference from a spherical head (Woodworth's formula
///   as the paper gives it),
/// - a one-pole, one-zero head shadow filter per ear whose high frequency gain
///   follows the angle of incidence,
/// - five pinna reflections whose delays follow azimuth and elevation, the
///   cue for elevation and front/back.
///
/// The model needs no measured data, so the output is identical everywhere.
/// Delays are interpolated across each quantum and filter coefficients change
/// once per quantum, so a moving source does not click. Render thread only.
final class Hrtf {
    private static final double HEAD_RADIUS = 0.0875;
    private static final double SPEED_OF_SOUND = 343;
    private static final double BETA = 2 * SPEED_OF_SOUND / HEAD_RADIUS;
    private static final double ALPHA_MIN = 0.1;
    private static final double THETA_MIN = Math.PI * 150 / 180;
    /// Pinna reflection coefficients and delay constants (in frames at 44.1 kHz).
    private static final double[] RHO = {0.5, -1, 0.5, -0.25, 0.25};
    private static final double[] A = {1, 5, 5, 5, 5};
    private static final double[] B = {2, 4, 7, 11, 13};
    private static final double[] D = {1, 0.5, 0.5, 0.5, 0.5};
    private static final int RING = 1024;
    private static final int MASK = RING - 1;

    private final double sampleRate;
    private final float[] ring = new float[RING];
    private long written;
    private boolean primed;
    private final double[] itd = new double[2];
    private final double[][] tau = new double[2][RHO.length];
    private final double[] nextItd = new double[2]; // NOPMD SingularField - preallocated scratch, no per-quantum allocation
    private final double[][] nextTau = new double[2][RHO.length]; // NOPMD SingularField - preallocated scratch, no per-quantum allocation
    private final double[] x1 = new double[2];
    private final double[] y1 = new double[2];

    Hrtf(double sampleRate) {
        this.sampleRate = sampleRate;
    }

    /// Renders one quantum of `mono` into `left`/`right`, multiplied by `gain`,
    /// for a source at `azimuth` and `elevation` degrees (azimuth 0 is ahead,
    /// positive to the right; elevation positive is up).
    void process(float[] mono, float[] left, float[] right, double azimuth, double elevation, float[] gain) {
        double az = Math.toRadians(azimuth);
        double el = Math.toRadians(elevation);
        double lateral = Math.sin(az) * Math.cos(el);
        for (int ear = 0; ear < 2; ear++) {
            double cosPsi = ear == 1 ? lateral : -lateral;
            double psi = MathUtil.acos(Math.max(-1, Math.min(1, cosPsi)));
            double t = psi < Math.PI / 2 ? 1 - Math.cos(psi) : psi - Math.PI / 2 + 1;
            nextItd[ear] = t * HEAD_RADIUS / SPEED_OF_SOUND * sampleRate;
            double earAz = ear == 1 ? azimuth : -azimuth;
            earAz = Math.toRadians(Math.max(-90, Math.min(90, earAz)));
            double scale = sampleRate / 44100.0;
            for (int n = 0; n < RHO.length; n++) {
                nextTau[ear][n] = (A[n] * Math.cos(earAz / 2) * Math.sin(D[n] * (Math.PI / 2 - el)) + B[n]) * scale;
            }
        }
        if (!primed) {
            primed = true;
            for (int ear = 0; ear < 2; ear++) {
                itd[ear] = nextItd[ear];
                System.arraycopy(nextTau[ear], 0, tau[ear], 0, RHO.length);
            }
        }
        double k = 2 * sampleRate;
        for (int ear = 0; ear < 2; ear++) {
            double cosPsi = ear == 1 ? lateral : -lateral;
            double psi = MathUtil.acos(Math.max(-1, Math.min(1, cosPsi)));
            double alpha = (1 + ALPHA_MIN / 2) + (1 - ALPHA_MIN / 2) * Math.cos(psi / THETA_MIN * Math.PI);
            double norm = k + BETA;
            double b0 = (alpha * k + BETA) / norm;
            double b1 = (BETA - alpha * k) / norm;
            double a1 = (BETA - k) / norm;
            float[] out = ear == 0 ? left : right;
            double d0 = itd[ear];
            double dStep = (nextItd[ear] - d0) / BaseAudioContext.RENDER_QUANTUM;
            double[] t0 = tau[ear];
            double[] t1 = nextTau[ear];
            double sx = x1[ear];
            double sy = y1[ear];
            for (int i = 0; i < BaseAudioContext.RENDER_QUANTUM; i++) {
                long w = written + i;
                if (ear == 0) {
                    ring[(int) (w & MASK)] = mono[i];
                }
                double f = (i + 1) / (double) BaseAudioContext.RENDER_QUANTUM;
                double delay = d0 + dStep * (i + 1);
                double x = read(w - delay);
                for (int n = 0; n < RHO.length; n++) {
                    double tn = t0[n] + (t1[n] - t0[n]) * f;
                    x += RHO[n] * read(w - delay - tn);
                }
                double y = b0 * x + b1 * sx - a1 * sy;
                if (Math.abs(y) < 1e-30) {
                    y = 0;
                }
                sx = x;
                sy = y;
                out[i] = (float) (y * gain[i]);
            }
            x1[ear] = sx;
            y1[ear] = sy;
            itd[ear] = nextItd[ear];
            System.arraycopy(t1, 0, t0, 0, RHO.length);
        }
        written += BaseAudioContext.RENDER_QUANTUM;
    }

    private double read(double pos) {
        if (pos < 0) {
            return 0;
        }
        long i0 = (long) pos;
        double frac = pos - i0;
        float a = ring[(int) (i0 & MASK)];
        float b = ring[(int) ((i0 + 1) & MASK)];
        return a + (b - a) * frac;
    }
}
