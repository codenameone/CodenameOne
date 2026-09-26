/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.ui;

import com.codename1.util.MathUtil;

/// The parts of the iOS 27 tab selection that depend on the GESTURE rather than
/// on a tap: holding the press, scrubbing the lens along the bar with the finger,
/// and letting go of a scrub. Measured like TabGlassMotion, with the same probe
/// (scripts/fidelity-app/ios-native-ref/motion-probe), from held presses and drags
/// driven through XCUITest:
///
/// - While the finger is down the lift holds, and the whole bar grows toward
///   +14.6 pt of width on a spring (omega 17.6 rad/s, zeta 0.59, 20 ms behind the
///   touch); lifting the finger springs it back. A quick tap is the same spring
///   released early.
/// - Dragging along the bar makes the lens FOLLOW the finger on a stiffer spring
///   (omega 32 rad/s, zeta 0.90, 20 ms behind), within the first and last tab.
/// - Releasing a scrub selects the tab nearest the FINGER (not the lens, which
///   can be far behind after a flick; a tie keeps the current tab). The lens
///   travels there on the tap's position curve, corrected by its velocity and its
///   lag behind the finger at release (SETTLE_V, SETTLE_E).
/// - While scrubbing, the lens deforms with its own motion: a kernel over its
///   recent speed and acceleration (the FIR_ tables).
/// - Every release ends in a small wobble that starts WOBBLE_START_S after the
///   finger lifts -- whether it was a tap, a hold or a scrub.
///
/// Pure data and arithmetic, evaluated from the recorded touch events at any
/// time, so a frame is reproducible: TabGlassGestureTest pins it to the capture.
final class TabGlassGesture {
    /// Bar press spring (whole-bar width growth while pressed).
    static final float BAR_OMEGA = 17.63f;
    static final float BAR_ZETA = 0.593f;
    static final float BAR_PRESS_PT = 14.58f;
    /// Scrub follow spring.
    static final float FOLLOW_OMEGA = 32.0f;
    static final float FOLLOW_ZETA = 0.903f;
    /// Both springs, and the settle, run this far behind the touch.
    static final float TOUCH_LAG_S = 0.02f;
    /// The release wobble starts this long after touch-up.
    static final float WOBBLE_START_S = 0.9f;
    /// The tap tables start at the last frame at rest, one frame after the
    /// touch; a press is evaluated on them this much later.
    static final float TEMPLATE_DELAY_S = 0.018f;
    /// Integration step of the follow spring.
    private static final float STEP_S = 1f / 240f;

    // ---- generated tables (tabmotion.py gesture) ----
    /// Release wobble, lens scaleX - 1, from WOBBLE_START_S after touch-up.
    static final float[] WOBBLE_X = {
            0.00000f, 0.00000f, 0.00000f, 0.00361f, 0.01156f, 0.01577f, 0.01579f,
            0.01397f, 0.01188f, 0.00980f, 0.00795f, 0.00580f, 0.00341f, 0.00063f,
            -0.00225f, -0.00522f, -0.00795f, -0.01039f, -0.01244f, -0.01396f, -0.01503f,
            -0.01553f, -0.01552f, -0.01517f, -0.01453f, -0.01358f, -0.01263f, -0.01144f,
            -0.01016f, -0.00876f, -0.00743f, -0.00614f, -0.00479f, -0.00367f, -0.00264f,
            -0.00183f, -0.00113f, -0.00005f
    };

    /// Release wobble, lens scaleY - 1.
    static final float[] WOBBLE_Y = {
            0.00000f, 0.00000f, 0.00000f, -0.00473f, -0.01511f, -0.02060f, -0.02037f,
            -0.01772f, -0.01475f, -0.01190f, -0.00938f, -0.00653f, -0.00337f, 0.00030f,
            0.00424f, 0.00825f, 0.01199f, 0.01525f, 0.01793f, 0.02006f, 0.02140f,
            0.02202f, 0.02193f, 0.02140f, 0.02049f, 0.01915f, 0.01775f, 0.01604f,
            0.01418f, 0.01233f, 0.01044f, 0.00863f, 0.00672f, 0.00510f, 0.00375f,
            0.00255f, 0.00164f, 0.00007f
    };

    /// Release wobble, lens centre offset in points (always towards +x).
    static final float[] WOBBLE_TX = {
            0.0000f, 0.0000f, 0.0000f, 0.1684f, 0.5498f, 0.7540f, 0.8391f,
            0.8995f, 0.9182f, 0.8944f, 0.8411f, 0.7892f, 0.7481f, 0.7238f,
            0.7200f, 0.7283f, 0.7467f, 0.7583f, 0.7764f, 0.7800f, 0.7717f,
            0.7533f, 0.7252f, 0.6868f, 0.6394f, 0.5891f, 0.5339f, 0.4642f,
            0.3859f, 0.3211f, 0.2607f, 0.2010f, 0.1529f, 0.1125f, 0.0813f,
            0.0608f, 0.0408f, 0.0017f
    };

    /// Settle after a scrub: lens offset (pt) per pt/s of lens velocity at release.
    static final float[] SETTLE_V = {
            0.002263f, 0.000871f, -0.002719f, -0.004932f, -0.006817f, -0.005184f, -0.005578f,
            -0.004491f, -0.004612f, -0.004095f, -0.003479f, -0.002938f, -0.002392f, -0.001918f,
            -0.001562f, -0.001233f, -0.000900f, -0.000713f, -0.000527f, -0.000408f, -0.000284f,
            -0.000168f, -0.000102f, -0.000181f, -0.000157f, -0.000261f, -0.000194f, -0.000197f,
            -0.000055f, 0.000069f, 0.000068f, 0.000054f, 0.000020f, 0.000010f, 0.000006f,
            0.000002f
    };

    /// Settle after a scrub: lens offset (pt) per pt of follow lag (target - lens) at release.
    static final float[] SETTLE_E = {
            0.00516f, 0.34670f, 0.68555f, 0.87646f, 0.96867f, 0.91844f, 0.87645f,
            0.77691f, 0.69398f, 0.59510f, 0.49772f, 0.40891f, 0.32887f, 0.25944f,
            0.20447f, 0.15461f, 0.11400f, 0.08268f, 0.05785f, 0.03895f, 0.02456f,
            0.01347f, 0.00612f, 0.00311f, -0.00014f, -0.00018f, -0.00205f, -0.00232f,
            -0.00480f, -0.00664f, -0.00547f, -0.00434f, -0.00163f, -0.00082f, -0.00047f,
            -0.00020f, -0.00003f
    };

    /// Scrub deformation kernels: scaleX - 1 per |v|/1000 pt/s, per frame of delay.
    static final float[] FIR_SX_SPEED = {
            0.005386f, 0.012912f, -0.028749f, -0.016629f, -0.033210f, -0.024031f, -0.020600f,
            -0.012343f, -0.002749f, 0.004030f, 0.009242f, 0.008953f, 0.012166f, 0.012893f,
            0.012872f, 0.010809f, 0.008817f, 0.007096f, 0.001731f, -0.004369f, -0.012404f,
            -0.019141f, -0.025249f, -0.022780f, -0.017689f, -0.005807f, 0.003809f, 0.012035f,
            0.016827f, 0.016782f, 0.031737f, 0.031957f, 0.015308f, 0.007353f, 0.003907f,
            0.015685f, 0.024954f, 0.027492f, 0.020445f, 0.014757f, 0.003888f, 0.002255f,
            -0.002126f, -0.000802f, -0.007250f, -0.002595f, -0.007373f, -0.000028f, -0.001768f,
            0.000206f, -0.005118f, -0.013183f, -0.010548f, -0.015463f, -0.008038f, -0.011520f,
            -0.009560f, -0.010342f, -0.011173f, -0.008117f
    };

    /// scaleX - 1 per (a * sign(v))/10000 pt/s^2.
    static final float[] FIR_SX_ACCEL = {
            0.003827f, -0.006486f, 0.002025f, 0.002359f, 0.010644f, 0.015898f, 0.024219f,
            0.029469f, 0.035889f, 0.038609f, 0.042037f, 0.043221f, 0.044077f, 0.043458f,
            0.041961f, 0.040211f, 0.037419f, 0.035090f, 0.032585f, 0.030200f, 0.029762f,
            0.029196f, 0.031131f, 0.032903f, 0.034329f, 0.033706f, 0.032755f, 0.028075f,
            0.025746f, 0.018850f, 0.017224f, 0.005670f, 0.003474f, -0.000335f, -0.001967f,
            -0.001836f, -0.009062f, -0.010145f, -0.020024f, -0.016717f, -0.025467f, -0.018039f,
            -0.025212f, -0.017825f, -0.022364f, -0.018854f, -0.019743f, -0.022799f, -0.015406f,
            -0.021934f, -0.006911f, -0.022476f, -0.013041f, -0.008297f, -0.012494f, -0.001811f,
            -0.013308f, 0.002084f, -0.007376f, 0.005347f
    };

    /// scaleY - 1 per |v|/1000 pt/s.
    static final float[] FIR_SY_SPEED = {
            0.093688f, 0.096136f, 0.128985f, 0.107764f, 0.118752f, 0.093075f, 0.079969f,
            0.053671f, 0.030938f, 0.007693f, -0.010222f, -0.022671f, -0.038274f, -0.050198f,
            -0.059146f, -0.063568f, -0.063998f, -0.064000f, -0.055460f, -0.044408f, -0.029172f,
            -0.016135f, -0.003080f, -0.003318f, -0.008979f, -0.022428f, -0.034123f, -0.042198f,
            -0.044854f, -0.039248f, -0.056633f, -0.054080f, -0.027495f, -0.015488f, -0.010086f,
            -0.026753f, -0.038404f, -0.040022f, -0.029777f, -0.022289f, -0.017102f, -0.014955f,
            -0.000277f, -0.003336f, 0.012116f, 0.003973f, 0.014495f, 0.003322f, 0.007905f,
            0.004756f, 0.013255f, 0.026090f, 0.021316f, 0.029225f, 0.016846f, 0.022248f,
            0.019017f, 0.019924f, 0.022059f, 0.015518f
    };

    /// scaleY - 1 per (a * sign(v))/10000 pt/s^2.
    static final float[] FIR_SY_ACCEL = {
            -0.005353f, -0.023268f, -0.043439f, -0.063703f, -0.085980f, -0.106162f, -0.127102f,
            -0.142632f, -0.157555f, -0.164858f, -0.171349f, -0.171984f, -0.170825f, -0.165800f,
            -0.157269f, -0.148163f, -0.135737f, -0.124395f, -0.112132f, -0.100828f, -0.092703f,
            -0.084886f, -0.081606f, -0.077825f, -0.074960f, -0.068167f, -0.062789f, -0.050827f,
            -0.044336f, -0.030841f, -0.026838f, -0.008056f, -0.004023f, 0.002576f, 0.005864f,
            0.006493f, 0.017673f, 0.019618f, 0.033694f, 0.029886f, 0.041609f, 0.036438f,
            0.044942f, 0.037809f, 0.042021f, 0.039654f, 0.037349f, 0.044313f, 0.030477f,
            0.041752f, 0.014996f, 0.040840f, 0.024000f, 0.015865f, 0.022673f, 0.003961f,
            0.023268f, -0.004889f, 0.012982f, -0.011256f
    };

    /// Centre offset (pt) per v/1000 pt/s.
    static final float[] FIR_TX_VEL = {
            -0.21568f, -0.69702f, -0.27793f, -0.68219f, -0.39789f, -0.68948f, -0.52792f,
            -0.67577f, -0.58201f, -0.59134f, -0.52522f, -0.42958f, -0.37108f, -0.23407f,
            -0.17440f, -0.04827f, 0.01979f, 0.09806f, 0.17122f, 0.20404f, 0.26861f,
            0.26498f, 0.31941f, 0.28912f, 0.34208f, 0.29301f, 0.33705f, 0.29164f,
            0.31769f, 0.27556f, 0.28484f, 0.24653f, 0.24153f, 0.20891f, 0.19330f,
            0.16887f, 0.14559f, 0.13959f, 0.10056f, 0.11453f, 0.06234f, 0.09931f,
            0.02636f, 0.08559f, -0.00115f, 0.08183f, -0.00969f, 0.06894f, -0.02416f,
            0.05977f, -0.05126f, 0.05368f, -0.05600f, 0.06126f, -0.01526f, 0.07498f,
            -0.05010f, 0.05472f, -0.06587f, 0.16911f
    };

    /// Centre offset (pt) per a/10000 pt/s^2.
    static final float[] FIR_TX_ACCEL = {
            -0.13426f, 0.18676f, -0.04449f, 0.35988f, 0.02187f, 0.39009f, -0.04113f,
            0.16225f, -0.25327f, -0.17034f, -0.48529f, -0.46244f, -0.58653f, -0.59003f,
            -0.55739f, -0.58258f, -0.43902f, -0.45427f, -0.31792f, -0.29220f, -0.18283f,
            -0.15240f, -0.07240f, -0.06801f, -0.01168f, 0.01510f, 0.00410f, 0.05807f,
            0.04825f, 0.09856f, 0.08710f, 0.12995f, 0.11285f, 0.14468f, 0.12012f,
            0.14313f, 0.08782f, 0.13510f, 0.07518f, 0.11465f, 0.04567f, 0.10793f,
            0.04117f, 0.08256f, 0.01128f, 0.02560f, 0.03866f, 0.04343f, 0.02751f,
            0.08128f, 0.01827f, 0.01423f, -0.02272f, -0.12222f, -0.04116f, 0.10452f,
            0.06077f, 0.04731f, -0.34318f, 0.32561f
    };

    // ---- end of generated tables ----

    // Touch events, in seconds after the press.
    private final float fromPt;
    private final float toPt;
    private float upS = -1;
    private float scrubStartS = -1;
    private float scrubX0;
    private float scrubV0;
    private float minPt;
    private float maxPt;
    private float settleToPt;
    private float[] fingerS = new float[32];
    private float[] fingerPt = new float[32];
    private int fingerCount;

    /// A press that starts the lens from `fromPt` towards the tab at `toPt`
    /// (lens centres in points; equal for a press on the selected tab).
    TabGlassGesture(float fromPt, float toPt) {
        this.fromPt = fromPt;
        this.toPt = toPt;
    }

    float getFromPt() {
        return fromPt;
    }

    float getToPt() {
        return toPt;
    }

    float getUpS() {
        return upS;
    }

    boolean isScrubbing() {
        return scrubStartS >= 0;
    }

    /// Touch-up, `s` seconds after the press.
    void up(float s) {
        if (upS < 0) {
            upS = s;
        }
    }

    /// Turns the press into a scrub at `s`: the lens leaves its tap motion from
    /// where it is now and follows the finger, kept within `[minPt, maxPt]`.
    void startScrub(float s, float minPt, float maxPt) {
        if (scrubStartS >= 0) {
            return;
        }
        float dt = 1f / TabGlassMotion.SAMPLE_HZ;
        scrubX0 = tapCentre(s);
        scrubV0 = (tapCentre(s + dt / 2) - tapCentre(s - dt / 2)) / dt;
        this.minPt = Math.min(minPt, maxPt);
        this.maxPt = Math.max(minPt, maxPt);
        scrubStartS = s;
        settleToPt = scrubX0;
    }

    /// The finger's position in lens-centre points at `s` seconds.
    void finger(float s, float pt) {
        if (fingerCount == fingerS.length) {
            float[] ns = new float[fingerCount * 2];
            float[] np = new float[fingerCount * 2];
            System.arraycopy(fingerS, 0, ns, 0, fingerCount);
            System.arraycopy(fingerPt, 0, np, 0, fingerCount);
            fingerS = ns;
            fingerPt = np;
        }
        fingerS[fingerCount] = s;
        fingerPt[fingerCount] = pt;
        fingerCount++;
    }

    /// Where a finished scrub goes (the centre of the tab chosen at release).
    void settleTo(float pt) {
        settleToPt = pt;
    }

    /// The finger at `s` (the last sample at or before it), or NaN before any.
    float fingerAt(float s) {
        float v = Float.NaN;
        for (int i = 0; i < fingerCount && fingerS[i] <= s; i++) {
            v = fingerPt[i];
        }
        return v;
    }

    private float clampPt(float v) {
        return v < minPt ? minPt : (v > maxPt ? maxPt : v);
    }

    /// The lens centre on the tap motion (no scrub).
    private float tapCentre(float s) {
        float t = s - TEMPLATE_DELAY_S;
        return fromPt + (toPt - fromPt) * TabGlassMotion.position(t < 0 ? 0 : t);
    }

    /// The scrub lens centre at 60 Hz, from the scrub start to `endS`: the follow
    /// spring while the finger is down, then the settle. Returns the samples and
    /// writes the release state into `release` (x, v, e) when the finger lifted.
    private float[] scrubTrack(float endS, float[] release) {
        int frames = Math.max(1, (int) Math.ceil((endS - scrubStartS) * TabGlassMotion.SAMPLE_HZ) + 2);
        float[] out = new float[frames];
        float x = scrubX0;
        float v = scrubV0;
        float s = scrubStartS;
        float rx = Float.NaN;
        float rv = 0;
        float re = 0;
        float w = FOLLOW_OMEGA;
        float z = FOLLOW_ZETA;
        int substeps = Math.round(1f / (TabGlassMotion.SAMPLE_HZ * STEP_S));
        for (int i = 0; i < frames; i++) {
            float fs = scrubStartS + i / (float) TabGlassMotion.SAMPLE_HZ;
            if (upS >= 0 && fs >= upS) {
                if (rx != rx) {
                    // The finger lifted during the previous frame: integrate up to it.
                    while (s < upS) {
                        float f = fingerAt(s - TOUCH_LAG_S);
                        float target = f != f ? scrubX0 : clampPt(f);
                        float a = w * w * (target - x) - 2 * z * w * v;
                        v += a * STEP_S;
                        x += v * STEP_S;
                        s += STEP_S;
                    }
                    float f = fingerAt(upS - TOUCH_LAG_S);
                    rx = x;
                    rv = v;
                    re = (f != f ? scrubX0 : clampPt(f)) - x;
                }
                out[i] = settle(fs - upS, rx, rv, re);
                continue;
            }
            for (int k = 0; k < substeps && s < fs; k++) {
                float f = fingerAt(s - TOUCH_LAG_S);
                float target = f != f ? scrubX0 : clampPt(f);
                float a = w * w * (target - x) - 2 * z * w * v;
                v += a * STEP_S;
                x += v * STEP_S;
                s += STEP_S;
            }
            out[i] = x;
        }
        if (release != null) {
            release[0] = rx;
            release[1] = rv;
            release[2] = re;
        }
        return out;
    }

    /// The settle `t` seconds after a scrub released at `x` with velocity `v`
    /// and follow lag `e`.
    private float settle(float t, float x, float v, float e) {
        float p = TabGlassMotion.position(t - TOUCH_LAG_S);
        if (t - TOUCH_LAG_S <= 0) {
            p = 0;
        }
        return x + (settleToPt - x) * p + v * table(SETTLE_V, t) + e * table(SETTLE_E, t);
    }

    /// A 60 Hz table that is zero outside itself.
    private static float table(float[] a, float t) {
        if (t < 0) {
            return 0;
        }
        float fx = t * TabGlassMotion.SAMPLE_HZ;
        int i = (int) fx;
        if (i >= a.length - 1) {
            return i == a.length - 1 ? a[i] * (1 - (fx - i)) : 0;
        }
        float f = fx - i;
        return a[i] * (1 - f) + a[i + 1] * f;
    }

    /// The release wobble `sinceUpS` seconds after touch-up.
    static float wobble(float[] table, float sinceUpS) {
        return table(table, sinceUpS - WOBBLE_START_S);
    }

    /// The whole bar's width growth in points `s` seconds after the press, for a
    /// touch-up at `upS` (negative while still down).
    static float barGrowPt(float s, float upS) {
        float g = step(s - TOUCH_LAG_S);
        if (upS >= 0) {
            g -= step(s - upS - TOUCH_LAG_S);
        }
        return BAR_PRESS_PT * g;
    }

    /// Unit step response of the bar spring.
    private static float step(float t) {
        if (t <= 0) {
            return 0;
        }
        double w = BAR_OMEGA;
        double z = BAR_ZETA;
        double wd = w * Math.sqrt(1 - z * z);
        double e = MathUtil.exp(-z * w * t);
        return (float) (1 - e * (Math.cos(wd * t) + z / Math.sqrt(1 - z * z) * Math.sin(wd * t)));
    }

    /// When the lift is released, in seconds after the press, or a large value
    /// while the finger is down.
    float releaseS(float nowS) {
        if (upS < 0) {
            return Float.MAX_VALUE;
        }
        if (!isScrubbing()) {
            // The release reaches the lens one touch lag after the finger lifts.
            return TabGlassMotion.releaseForPress(toPt - fromPt, upS + TOUCH_LAG_S - TEMPLATE_DELAY_S)
                    + TEMPLATE_DELAY_S;
        }
        // A scrub releases once the settling lens is within RELEASE_PT of its tab.
        float end = Math.max(nowS, upS) + 1f;
        float[] track = scrubTrack(end, null);
        for (int i = 0; i < track.length; i++) {
            float fs = scrubStartS + i / (float) TabGlassMotion.SAMPLE_HZ;
            if (fs >= upS && Math.abs(settleToPt - track[i]) < TabGlassMotion.RELEASE_PT) {
                return Math.max(fs - 1f / TabGlassMotion.SAMPLE_HZ, upS + TOUCH_LAG_S);
            }
        }
        return end;
    }

    /// True once everything this gesture started has played out at `s`.
    boolean isFinished(float s) {
        if (upS < 0) {
            return false;
        }
        float rel = releaseS(s);
        float end = TabGlassMotion.heldDurationS(upS, rel) + TEMPLATE_DELAY_S;
        if (isScrubbing()) {
            end = Math.max(end, upS + SETTLE_V.length / (float) TabGlassMotion.SAMPLE_HZ
                    + FIR_SX_SPEED.length / (float) TabGlassMotion.SAMPLE_HZ);
        }
        return s >= end;
    }

    /// The frame `s` seconds after the press. `centreOut[0]` receives the lens
    /// centre in points.
    TabGlassMotion at(float s, float[] centreOut) {
        float rel = releaseS(s);
        float d = TEMPLATE_DELAY_S;
        TabGlassMotion m = TabGlassMotion.held(s - d, toPt - fromPt, upS < 0 ? -1 : upS - d,
                rel == Float.MAX_VALUE ? rel : rel - d);
        m.barGrowPt = barGrowPt(s, upS);
        if (!isScrubbing() || s < scrubStartS) {
            centreOut[0] = tapCentre(s);
            return m;
        }
        float[] track = scrubTrack(s + 2f / TabGlassMotion.SAMPLE_HZ, null);
        float fx = (s - scrubStartS) * TabGlassMotion.SAMPLE_HZ;
        int n = Math.min((int) fx, track.length - 2);
        float f = fx - n;
        centreOut[0] = track[n] * (1 - f) + track[n + 1] * f;
        // Deformation from the lens's own motion, on top of the press's.
        float dx0 = deform(track, n, FIR_SX_SPEED, FIR_SX_ACCEL, true);
        float dx1 = deform(track, n + 1, FIR_SX_SPEED, FIR_SX_ACCEL, true);
        float dy0 = deform(track, n, FIR_SY_SPEED, FIR_SY_ACCEL, true);
        float dy1 = deform(track, n + 1, FIR_SY_SPEED, FIR_SY_ACCEL, true);
        float dt0 = deform(track, n, FIR_TX_VEL, FIR_TX_ACCEL, false);
        float dt1 = deform(track, n + 1, FIR_TX_VEL, FIR_TX_ACCEL, false);
        m.scaleX += dx0 * (1 - f) + dx1 * f;
        m.scaleY += dy0 * (1 - f) + dy1 * f;
        m.leadPt += dt0 * (1 - f) + dt1 * f;
        return m;
    }

    /// One output of a deformation kernel at frame `n` of `track`.
    private static float deform(float[] track, int n, float[] speedK, float[] accelK, boolean unsigned) {
        float sum = 0;
        int taps = Math.max(speedK.length, accelK.length);
        for (int k = 0; k < taps && n - k >= 0; k++) {
            int i = n - k;
            float v = velocity(track, i);
            float a = (velocity(track, i + 1) - velocity(track, i - 1)) * TabGlassMotion.SAMPLE_HZ / 2f;
            if (i == 0) {
                a = (velocity(track, 1) - velocity(track, 0)) * TabGlassMotion.SAMPLE_HZ;
            }
            float sv = v < 0 ? -1f : (v > 0 ? 1f : 0f);
            float in1 = unsigned ? Math.abs(v) / 1000f : v / 1000f;
            float in2 = unsigned ? a * sv / 10000f : a / 10000f;
            if (k < speedK.length) {
                sum += speedK[k] * in1;
            }
            if (k < accelK.length) {
                sum += accelK[k] * in2;
            }
        }
        return sum;
    }

    /// Central-difference velocity of `track` at frame `i`, in points per second.
    private static float velocity(float[] track, int i) {
        int last = track.length - 1;
        if (last < 1) {
            return 0;
        }
        if (i <= 0) {
            return (track[1] - track[0]) * TabGlassMotion.SAMPLE_HZ;
        }
        if (i >= last) {
            return (track[last] - track[last - 1]) * TabGlassMotion.SAMPLE_HZ;
        }
        return (track[i + 1] - track[i - 1]) * TabGlassMotion.SAMPLE_HZ / 2f;
    }
}
