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

/// The iOS 27 Liquid Glass tab selection motion, as UIKit plays it.
///
/// Every number in this class was MEASURED, not tuned by eye. An instrumented
/// native app (scripts/fidelity-app/ios-native-ref/motion-probe) hosts a real
/// `UITabBarController` on the iOS 27 simulator, drives genuine taps through
/// XCUITest, and logs the presentation geometry of every layer under the tab bar
/// on every display frame. UIKit attaches no `CAAnimation` for this motion -- it
/// writes the layer values itself each frame -- so the per-frame log IS the curve.
///
/// Most channels turned out to be springs UIKit evaluates on the display's vsync
/// clock, and are written here in closed form (DampedSpring): the lens travel is a
/// spring of duration 0.4 s and bounce 0.15, the lift and the platter critically
/// damped springs of 0.25 s and 0.4 s, the touch glow critically damped springs of
/// 0.1 s and 0.5 s. They reproduce the capture to its noise, which the averaged
/// tables they replaced did not: every captured frame renders at exactly
/// `start + n / 60` s (to 0.03 ms), so the per-frame log times, which carry the
/// main thread's jitter, are not the times the curves were evaluated at. The
/// deformation and the whole-bar pulse have no closed form that matched the
/// capture as well as a table, and stay 60 Hz tables resampled from the moment the
/// lens starts to move. `scripts/fidelity-app/tools/tab-motion/` regenerates the
/// tables and checks the closed forms against a fresh capture.
///
/// What UIKit does, channel by channel (lens = the selection capsule):
///
/// - The lens centre travels on one normalized curve for every jump distance:
///   at the target after ~0.33 s, a 0.6% overshoot at ~0.38 s; UIKit ends it once
///   less than SETTLE_PT remains past the overshoot.
/// - On touch the lens LIFTS: its bounds grow by 16 pt in both directions, the
///   grey platter under it fades out (it becomes clear glass) and the accent copy
///   of the tab content seen through it is magnified by up to 16%. The lift is
///   released once the lens is within 3.5 pt of its target, and falls back on the
///   same spring while the platter springs back from wherever it was.
/// - The lens deforms: a distance-independent stretch pulse (x up to 1.12, y down
///   to 0.84) leading the travel, then a squash on arrival whose depth is linear
///   in the distance travelled (x 0.96 after 64 pt, 0.85 after 258 pt), and a
///   small second wobble about a second in.
/// - The whole bar -- glass, content and lens together -- scales about its centre
///   by a pulse that adds ~8.7 pt to its width at 0.13 s and undershoots slightly.
/// - A white glow brightens under the finger, then, from the touch-up, expands to
///   four times its size across the bar and fades.
///
/// Deformation is `E(t) + d * S(t)` for travel distance `d`; that fits every
/// captured jump from 64 to 258 pt to within 0.01 of scale.
///
/// Pure data, no Display or Graphics dependency: `Tabs` resolves the geometry and
/// converts points to pixels, and TabGlassMotionTest pins the model against the
/// native capture.
final class TabGlassMotion {
    /// Sample rate of the tables below.
    static final int SAMPLE_HZ = 60;
    /// Height of the native floating tab bar; points convert to pixels by the
    /// ratio of the theme's pill height to this.
    static final float BAR_HEIGHT_PT = 62f;
    /// How much the lens bounds grow, in both directions, at full lift.
    static final float LIFT_PT = 16f;
    /// Magnification of the accent content under the lens at full lift.
    static final float CONTENT_MAGNIFICATION = 0.16f;
    /// Remaining travel below which the lift is released.
    static final float RELEASE_PT = 3.5f;
    /// UIKit snaps the rising lift to 1 once it passes this value.
    static final float LIFT_SNAP = 0.975f;
    /// Diameter of the touch glow before it expands.
    static final float GLOW_DIAMETER_PT = 93f;

    // ---- closed forms (checked against the capture by tabmotion.py check) ----
    /// The springs start together, this long after the tables' t = 0 (the last
    /// frame at rest).
    static final float START_S = 0.00226f;
    /// Lens travel: duration 0.4 s, bounce 0.15 (omega = 2 pi / duration,
    /// zeta = 1 - bounce).
    static final double POSITION_OMEGA = 2 * Math.PI / 0.4;
    static final double POSITION_ZETA = 0.85;
    /// Past its overshoot, UIKit ends the travel once less than this remains
    /// (the capture puts it between 0.194 and 0.197 pt).
    static final float SETTLE_PT = 0.195f;
    /// Lift: critically damped, 0.25 s, both the rise from the press and the fall
    /// from full lift at the release.
    private static final double LIFT_OMEGA = 2 * Math.PI / 0.25;
    /// The lift fall ends (snaps to 0) this long after the release: between the
    /// 13th and 14th frame in the capture, so half way.
    static final float LIFT_FALL_S = 13.5f / SAMPLE_HZ;
    /// Grey platter: 1 - rise until the release, then critically damped, 0.4 s,
    /// back to 1 from its opacity and velocity at the release.
    private static final double PLATTER_OMEGA = 2 * Math.PI / 0.4;
    /// The platter return ends (snaps to 1) this long after the release: between
    /// the 28th and 29th frame in the capture.
    static final float PLATTER_RETURN_S = 28.5f / SAMPLE_HZ;
    /// Touch glow layer: critically damped, 0.1 s, towards GLOW_PEAK from START_S;
    /// at GLOW_UP_S (the tap's touch-up reaching the glow) it turns back to 0 on a
    /// critically damped 0.5 s spring, from where it was and how fast it moved.
    private static final double GLOW_RISE_OMEGA = 2 * Math.PI / 0.1;
    private static final double GLOW_FALL_OMEGA = 2 * Math.PI / 0.5;
    private static final float GLOW_PEAK = 0.8445f;
    static final float GLOW_UP_S = 4f / SAMPLE_HZ;
    /// The glow's mask opacity is this fraction of its layer opacity; the glow is
    /// drawn with their product.
    private static final float GLOW_MASK = 0.33675f;
    /// Each glow opacity is hidden (0) once it has fallen under this.
    private static final float GLOW_HIDE = 0.005f;
    /// From GLOW_UP_S the glow grows by this multiple of GLOW_DIAMETER_PT, on the
    /// glow's 0.5 s spring.
    private static final float GLOW_GROWTH = 3f;

    /// Lens scaleX - 1, distance-independent part.
    private static final float[] STRETCH_X = {
            0.0000f, 0.0000f, 0.0055f, 0.0184f, 0.0384f, 0.0604f, 0.0837f, 0.1028f, 0.1135f,
            0.1140f, 0.1093f, 0.1043f, 0.0986f, 0.0931f, 0.0848f, 0.0761f, 0.0659f, 0.0549f,
            0.0437f, 0.0328f, 0.0227f, 0.0134f, 0.0057f, -0.0007f, -0.0056f, -0.0098f, -0.0113f,
            -0.0124f, -0.0126f, -0.0120f, -0.0109f, -0.0094f, -0.0077f, -0.0061f, -0.0045f,
            -0.0030f, -0.0010f, 0.0005f, 0.0024f, 0.0035f, 0.0045f, 0.0054f, 0.0057f, 0.0055f,
            0.0054f, 0.0048f, 0.0042f, 0.0035f, 0.0021f, 0.0016f, 0.0020f, 0.0008f, 0.0003f,
            -0.0001f, -0.0006f, -0.0009f, -0.0008f, -0.0006f, -0.0004f, -0.0002f, 0.0022f, 0.0067f,
            0.0098f, 0.0093f, 0.0077f, 0.0059f, 0.0044f, 0.0029f, 0.0015f, -0.0001f, -0.0021f,
            -0.0039f, -0.0058f, -0.0077f, -0.0093f, -0.0106f, -0.0116f, -0.0122f, -0.0124f,
            -0.0123f, -0.0119f, -0.0114f, -0.0107f, -0.0098f, -0.0089f, -0.0080f, -0.0070f,
            -0.0060f, -0.0050f, -0.0041f, -0.0032f, -0.0023f, -0.0015f, -0.0011f, -0.0003f,
            -0.0002f, 0.0000f
    };

    /// Lens scaleX - 1 per point of travel.
    private static final float[] STRETCH_X_PER_PT = {
            0.0000000f, -0.0000001f, -0.0000001f, -0.0000003f, 0.0000000f, 0.0000014f, 0.0000011f,
            0.0000224f, 0.0000376f, 0.0000084f, -0.0000603f, -0.0001281f, -0.0001555f, -0.0001468f,
            -0.0001004f, -0.0000470f, 0.0000066f, 0.0000440f, 0.0000571f, 0.0000444f, 0.0000084f,
            -0.0000442f, -0.0001096f, -0.0001796f, -0.0002477f, -0.0003013f, -0.0003635f,
            -0.0004053f, -0.0004351f, -0.0004533f, -0.0004613f, -0.0004595f, -0.0004497f,
            -0.0004325f, -0.0004111f, -0.0003857f, -0.0003623f, -0.0003343f, -0.0003101f,
            -0.0002786f, -0.0002474f, -0.0002169f, -0.0001844f, -0.0001508f, -0.0001221f,
            -0.0000937f, -0.0000686f, -0.0000474f, -0.0000252f, -0.0000125f, -0.0000118f,
            0.0000034f, 0.0000077f, 0.0000101f, 0.0000124f, 0.0000123f, 0.0000091f, 0.0000053f,
            0.0000012f, -0.0000024f, 0.0000071f, 0.0000215f, 0.0000224f, 0.0000229f, 0.0000204f,
            0.0000195f, 0.0000175f, 0.0000163f, 0.0000130f, 0.0000094f, 0.0000069f, 0.0000024f,
            -0.0000028f, -0.0000052f, -0.0000074f, -0.0000103f, -0.0000114f, -0.0000127f,
            -0.0000134f, -0.0000135f, -0.0000132f, -0.0000119f, -0.0000107f, -0.0000094f,
            -0.0000080f, -0.0000066f, -0.0000049f, -0.0000034f, -0.0000023f, -0.0000010f,
            0.0000001f, -0.0000003f, -0.0000001f, 0.0000015f, 0.0000017f, 0.0000013f, 0.0000000f
    };

    /// Lens scaleY - 1, distance-independent part.
    private static final float[] STRETCH_Y = {
            0.0000f, 0.0000f, -0.0074f, -0.0249f, -0.0520f, -0.0817f, -0.1135f, -0.1413f, -0.1574f,
            -0.1573f, -0.1489f, -0.1402f, -0.1317f, -0.1238f, -0.1129f, -0.1011f, -0.0868f,
            -0.0716f, -0.0563f, -0.0412f, -0.0265f, -0.0124f, 0.0002f, 0.0109f, 0.0192f, 0.0261f,
            0.0281f, 0.0293f, 0.0288f, 0.0269f, 0.0242f, 0.0209f, 0.0174f, 0.0139f, 0.0105f,
            0.0076f, 0.0041f, 0.0012f, -0.0021f, -0.0041f, -0.0059f, -0.0075f, -0.0082f, -0.0081f,
            -0.0079f, -0.0071f, -0.0063f, -0.0053f, -0.0034f, -0.0026f, -0.0027f, -0.0013f,
            -0.0008f, -0.0002f, 0.0005f, 0.0012f, 0.0011f, 0.0008f, 0.0005f, 0.0002f, -0.0028f,
            -0.0089f, -0.0130f, -0.0121f, -0.0098f, -0.0072f, -0.0051f, -0.0032f, -0.0013f, 0.0009f,
            0.0035f, 0.0060f, 0.0085f, 0.0112f, 0.0134f, 0.0151f, 0.0165f, 0.0172f, 0.0176f,
            0.0174f, 0.0168f, 0.0160f, 0.0150f, 0.0139f, 0.0125f, 0.0112f, 0.0098f, 0.0084f,
            0.0070f, 0.0057f, 0.0046f, 0.0033f, 0.0022f, 0.0016f, 0.0005f, 0.0003f, 0.0000f
    };

    /// Lens scaleY - 1 per point of travel.
    private static final float[] STRETCH_Y_PER_PT = {
            0.0000000f, 0.0000001f, 0.0000001f, 0.0000005f, 0.0000000f, -0.0000019f, 0.0000010f,
            -0.0000202f, -0.0000359f, -0.0000249f, 0.0000211f, 0.0000764f, 0.0000872f, 0.0000533f,
            -0.0000259f, -0.0001097f, -0.0001924f, -0.0002409f, -0.0002491f, -0.0002207f,
            -0.0001652f, -0.0000940f, -0.0000107f, 0.0000789f, 0.0001687f, 0.0002433f, 0.0003386f,
            0.0004095f, 0.0004668f, 0.0005101f, 0.0005386f, 0.0005534f, 0.0005560f, 0.0005485f,
            0.0005319f, 0.0005068f, 0.0004832f, 0.0004533f, 0.0004256f, 0.0003860f, 0.0003465f,
            0.0003062f, 0.0002638f, 0.0002177f, 0.0001783f, 0.0001372f, 0.0001027f, 0.0000722f,
            0.0000416f, 0.0000219f, 0.0000162f, -0.0000014f, -0.0000074f, -0.0000108f, -0.0000148f,
            -0.0000159f, -0.0000122f, -0.0000070f, -0.0000014f, 0.0000035f, -0.0000090f,
            -0.0000277f, -0.0000287f, -0.0000281f, -0.0000248f, -0.0000242f, -0.0000215f,
            -0.0000191f, -0.0000149f, -0.0000109f, -0.0000070f, -0.0000014f, 0.0000055f, 0.0000079f,
            0.0000118f, 0.0000154f, 0.0000169f, 0.0000191f, 0.0000190f, 0.0000192f, 0.0000182f,
            0.0000169f, 0.0000151f, 0.0000122f, 0.0000120f, 0.0000094f, 0.0000071f, 0.0000044f,
            0.0000030f, 0.0000017f, -0.0000001f, -0.0000006f, -0.0000006f, -0.0000022f, -0.0000024f,
            -0.0000018f, 0.0000000f
    };

    /// Lens centre lead in points along the travel direction, distance-independent part.
    private static final float[] LEAD_PT = {
            0.0000f, 0.0007f, 0.2938f, 1.0074f, 2.1367f, 3.4290f, 4.8636f, 6.0673f, 6.7727f,
            7.3152f, 7.6047f, 7.3912f, 6.8652f, 6.3555f, 5.4543f, 4.5959f, 3.9845f, 3.3075f,
            2.7106f, 2.2127f, 1.8042f, 1.4686f, 1.2083f, 1.0004f, 0.8345f, 0.7228f, 0.5881f,
            0.4936f, 0.3948f, 0.3138f, 0.2226f, 0.1479f, 0.0715f, 0.0112f, -0.0638f, -0.1546f,
            -0.2847f, -0.3548f, -0.4029f, -0.3828f, -0.3557f, -0.3253f, -0.2810f, -0.2326f,
            -0.2015f, -0.1652f, -0.1288f, -0.1034f, -0.0651f, -0.0673f, -0.1023f, -0.0594f,
            -0.0480f, -0.0359f, -0.0157f, 0.0479f, 0.0616f, 0.0662f, 0.0745f, 0.0675f, 0.2305f,
            0.6303f, 0.8126f, 0.8855f, 0.9489f, 0.9535f, 0.9188f, 0.8523f, 0.8000f, 0.7517f,
            0.7266f, 0.7139f, 0.7224f, 0.7269f, 0.7335f, 0.7476f, 0.7428f, 0.7294f, 0.7084f,
            0.6744f, 0.6362f, 0.5815f, 0.5427f, 0.4898f, 0.4272f, 0.3580f, 0.2872f, 0.2241f,
            0.1722f, 0.1406f, 0.1024f, 0.0708f, 0.0599f, 0.0344f, -0.0121f, -0.0112f, -0.0002f,
            0.0000f
    };

    /// Lens centre lead per point of travel.
    private static final float[] LEAD_PER_PT = {
            0.0000000f, -0.0000043f, 0.0000178f, 0.0000811f, 0.0002187f, 0.0004269f, 0.0004713f,
            0.0020937f, 0.0031627f, -0.0016846f, -0.0072994f, -0.0096333f, -0.0114983f, -0.0147134f,
            -0.0160013f, -0.0165079f, -0.0188178f, -0.0182175f, -0.0162945f, -0.0132486f,
            -0.0093188f, -0.0048343f, -0.0002613f, 0.0041333f, 0.0080513f, 0.0111611f, 0.0141693f,
            0.0162235f, 0.0178172f, 0.0187634f, 0.0193351f, 0.0194228f, 0.0192204f, 0.0186776f,
            0.0180465f, 0.0173706f, 0.0168869f, 0.0157468f, 0.0142691f, 0.0122342f, 0.0102847f,
            0.0084948f, 0.0068241f, 0.0052873f, 0.0041313f, 0.0030503f, 0.0021473f, 0.0014806f,
            0.0007969f, 0.0004478f, 0.0006007f, -0.0000826f, -0.0003399f, -0.0005395f, -0.0007964f,
            -0.0012456f, -0.0013857f, -0.0014389f, -0.0014865f, -0.0014572f, -0.0032151f,
            -0.0070253f, -0.0086748f, -0.0092016f, -0.0097185f, -0.0096760f, -0.0092408f,
            -0.0085522f, -0.0079977f, -0.0075674f, -0.0073058f, -0.0072455f, -0.0073654f,
            -0.0074485f, -0.0075856f, -0.0077260f, -0.0077119f, -0.0076050f, -0.0074005f,
            -0.0070310f, -0.0066410f, -0.0060947f, -0.0056725f, -0.0050960f, -0.0044412f,
            -0.0037326f, -0.0030212f, -0.0023771f, -0.0018567f, -0.0015059f, -0.0011018f,
            -0.0007806f, -0.0006532f, -0.0003938f, 0.0000630f, 0.0000641f, 0.0000014f, 0.0000000f
    };

    /// Whole-bar width growth in points; the bar scales uniformly by (w + grow) / w.
    private static final float[] BAR_GROW_PT = {
            0.0000f, 0.5275f, 2.0830f, 4.0540f, 6.4835f, 8.1467f, 8.7335f, 8.5351f, 7.7808f,
            6.6930f, 5.4604f, 4.2033f, 3.0110f, 2.0482f, 1.0941f, 0.4194f, -0.0029f, -0.4939f,
            -0.7085f, -0.8119f, -0.8255f, -0.7726f, -0.6875f, -0.5753f, -0.4631f, -0.2694f,
            -0.0007f, 0.0000f
    };

    // ---- outputs ----
    /// Lens centre travel, 0 at the source and 1 at the target (overshoots slightly).
    float position;
    /// Lift 0..1: bounds grow by LIFT_PT * lift, content magnifies by
    /// CONTENT_MAGNIFICATION * lift.
    float lift;
    /// Opacity of the grey platter under the lens, 1 at rest.
    float platterOpacity;
    /// Magnification of the accent content seen through the lens.
    float contentScale;
    /// Lens deformation about its centre.
    float scaleX;
    float scaleY;
    /// Lens centre offset in points along the travel direction (signed).
    float leadPt;
    /// Width the whole bar grows by, in points.
    float barGrowPt;
    /// Touch glow diameter scale and opacity (0 = no glow).
    float glowScale;
    float glowOpacity;

    private TabGlassMotion() {
    }

    /// Duration of the whole motion in milliseconds, the late wobble included.
    static int durationMs() {
        int n = 0;
        // The closed forms are at rest well before the tables end (see
        // TabGlassMotionTest.theClosedFormsRestBeforeTheTablesEnd).
        float[][] all = {STRETCH_X, STRETCH_X_PER_PT, STRETCH_Y, STRETCH_Y_PER_PT, LEAD_PT, LEAD_PER_PT,
            BAR_GROW_PT};
        for (float[] a : all) {
            if (a.length > n) {
                n = a.length;
            }
        }
        return (n - 1) * 1000 / SAMPLE_HZ;
    }

    /// The resting frame: lens on its cell, no deformation, no glow.
    static TabGlassMotion rest() {
        TabGlassMotion m = new TabGlassMotion();
        m.position = 1f;
        m.platterOpacity = 1f;
        m.contentScale = 1f;
        m.scaleX = 1f;
        m.scaleY = 1f;
        m.glowScale = 1f;
        return m;
    }

    /// Linear interpolation into a 60 Hz table; clamps to the first/last sample.
    static float sample(float[] table, float seconds) {
        float x = seconds * SAMPLE_HZ;
        if (x <= 0) {
            return table[0];
        }
        int i = (int) x;
        if (i >= table.length - 1) {
            return table[table.length - 1];
        }
        float f = x - i;
        return table[i] * (1 - f) + table[i + 1] * f;
    }

    /// Seconds after the start at which the lift is released for a jump of
    /// `travelPt`. The first FALLING frame is the first one whose remaining
    /// distance is under RELEASE_PT, but never before the frame after the lift has
    /// snapped to full (a short jump waits for the lift); the release instant is
    /// the frame before it, because UIKit already shows a falling value there.
    static float releaseSeconds(float travelPt) {
        float d = Math.abs(travelPt);
        int last = 2 * SAMPLE_HZ;
        int snap = 0;
        while (snap < last && liftRise(snap / (float) SAMPLE_HZ) < LIFT_SNAP) {
            snap++;
        }
        for (int i = 1; i < last; i++) {
            if (d * (1f - position(i / (float) SAMPLE_HZ)) < RELEASE_PT) {
                return (Math.max(i, snap + 1) - 1) / (float) SAMPLE_HZ;
            }
        }
        return last / (float) SAMPLE_HZ;
    }

    /// Computes the frame `elapsedMs` after the selection started, for a jump of
    /// `travelPt` points (signed: positive = towards larger x).
    static TabGlassMotion at(float elapsedMs, float travelPt) {
        if (elapsedMs >= durationMs()) {
            return rest();
        }
        TabGlassMotion m = new TabGlassMotion();
        float t = elapsedMs < 0 ? 0 : elapsedMs / 1000f;
        float d = Math.abs(travelPt);
        float sign = travelPt < 0 ? -1f : 1f;
        m.position = settledPosition(t, d);
        float release = releaseSeconds(travelPt);
        if (t < release) {
            float u = liftRise(t);
            m.lift = u >= LIFT_SNAP ? 1f : u;
            m.platterOpacity = 1f - u;
        } else {
            float s = t - release;
            m.lift = liftFall(s);
            m.platterOpacity = platterReturn(s, release);
        }
        m.contentScale = 1f + CONTENT_MAGNIFICATION * m.lift;
        m.scaleX = 1f + sample(STRETCH_X, t) + d * sample(STRETCH_X_PER_PT, t);
        m.scaleY = 1f + sample(STRETCH_Y, t) + d * sample(STRETCH_Y_PER_PT, t);
        m.leadPt = sign * (sample(LEAD_PT, t) + d * sample(LEAD_PER_PT, t));
        m.barGrowPt = sample(BAR_GROW_PT, t);
        m.glowScale = glowScale(t);
        m.glowOpacity = glowOpacity(t);
        return m;
    }

    /// Template time of the touch-up the tap tables were captured with; their
    /// release wobble sits WOBBLE_START_S after it.
    static final float TAP_UP_S = 0.048f;
    /// Shortest jump in the tap captures (one tab of a three-tab bar).
    static final float SHORTEST_CAPTURED_JUMP_PT = 64f;

    /// The selection frame for a real press that may outlast a tap: `upS` is the
    /// touch-up in seconds after the press (negative while the finger is down) and
    /// `releaseS` the lift release (see releaseForPress). Everything a press does
    /// differently from the captured quick tap:
    ///
    /// - the lift holds while the finger is down, then falls from `releaseS`;
    /// - the whole-bar growth is the press spring (TabGlassGesture.barGrowPt)
    ///   rather than the tap's recorded pulse;
    /// - the release wobble moves from the tap's touch-up to the real one;
    /// - the touch glow waits, at its tap-up state, until the finger lifts.
    ///
    /// For a quick tap this is the captured motion to within the spring fit.
    static TabGlassMotion held(float elapsedS, float travelPt, float upS, float releaseS) {
        TabGlassMotion m = new TabGlassMotion();
        float t = elapsedS < 0 ? 0 : elapsedS;
        float d = Math.abs(travelPt);
        float sign = travelPt < 0 ? -1f : 1f;
        m.position = settledPosition(t, d);
        if (upS < 0 || t < releaseS) {
            float u = liftRise(t);
            m.lift = u >= LIFT_SNAP ? 1f : u;
            m.platterOpacity = 1f - u;
        } else {
            float s = t - releaseS;
            m.lift = liftFall(s);
            m.platterOpacity = platterReturn(s, releaseS);
        }
        m.contentScale = 1f + CONTENT_MAGNIFICATION * m.lift;
        float tapWobble = t - TAP_UP_S;
        // The stretch pulse leads the TRAVEL: a press that does not move the lens
        // (the selected tab) has none, only the release wobble. Captured jumps start
        // at one tab (64 pt); shorter ones fade the pulse in.
        float lead = Math.min(1f, d / SHORTEST_CAPTURED_JUMP_PT);
        m.scaleX = 1f + lead * sample(STRETCH_X, t) + d * sample(STRETCH_X_PER_PT, t)
                - lead * TabGlassGesture.wobble(TabGlassGesture.WOBBLE_X, tapWobble);
        m.scaleY = 1f + lead * sample(STRETCH_Y, t) + d * sample(STRETCH_Y_PER_PT, t)
                - lead * TabGlassGesture.wobble(TabGlassGesture.WOBBLE_Y, tapWobble);
        m.leadPt = sign * lead * (sample(LEAD_PT, t) - TabGlassGesture.wobble(TabGlassGesture.WOBBLE_TX, tapWobble))
                + sign * d * sample(LEAD_PER_PT, t);
        if (upS >= 0) {
            m.scaleX += TabGlassGesture.wobble(TabGlassGesture.WOBBLE_X, t - upS);
            m.scaleY += TabGlassGesture.wobble(TabGlassGesture.WOBBLE_Y, t - upS);
            m.leadPt += TabGlassGesture.wobble(TabGlassGesture.WOBBLE_TX, t - upS);
        }
        float glowT;
        if (t <= TAP_UP_S) {
            glowT = t;
        } else if (upS < 0 || t < upS) {
            glowT = TAP_UP_S;
        } else {
            glowT = TAP_UP_S + t - Math.max(upS, TAP_UP_S);
        }
        m.glowScale = glowScale(glowT);
        m.glowOpacity = glowOpacity(glowT);
        return m;
    }

    /// The normalized lens-centre travel `s` seconds into a selection: the travel
    /// spring, without UIKit's settle (see settledPosition).
    static float position(float s) {
        return (float) (1 + DampedSpring.displacement(POSITION_OMEGA, POSITION_ZETA, -1, 0, s - START_S));
    }

    /// The travel of a jump of `d` points as UIKit shows it: the spring until, past
    /// its overshoot peak, less than SETTLE_PT remains, then exactly 1.
    static float settledPosition(float s, float d) {
        float p = position(s);
        double peak = START_S + Math.PI / (POSITION_OMEGA * Math.sqrt(1 - POSITION_ZETA * POSITION_ZETA));
        return s >= peak && d * Math.abs(p - 1f) < SETTLE_PT ? 1f : p;
    }

    /// The lift's rise `s` seconds into a selection, before UIKit's LIFT_SNAP.
    static float liftRise(float s) {
        return (float) (1 + DampedSpring.displacement(LIFT_OMEGA, 1, -1, 0, s - START_S));
    }

    /// The lift `s` seconds after its release, falling from full lift at rest.
    static float liftFall(float s) {
        return s >= LIFT_FALL_S ? 0f : (float) DampedSpring.displacement(LIFT_OMEGA, 1, 1, 0, s);
    }

    /// The grey platter's opacity `s` seconds after a release at `releaseS`: it
    /// leaves `1 - liftRise` with that curve's velocity and springs back to 1.
    static float platterReturn(float s, float releaseS) {
        if (s >= PLATTER_RETURN_S) {
            return 1f;
        }
        double x0 = -liftRise(releaseS);
        double v0 = -DampedSpring.velocity(LIFT_OMEGA, 1, -1, 0, releaseS - START_S);
        return (float) (1 + DampedSpring.displacement(PLATTER_OMEGA, 1, x0, v0, s));
    }

    /// The touch glow's layer opacity `s` seconds into a tap (see GLOW_PEAK).
    static float glowLayerOpacity(float s) {
        if (s < GLOW_UP_S) {
            return (float) (GLOW_PEAK + DampedSpring.displacement(GLOW_RISE_OMEGA, 1, -GLOW_PEAK, 0, s - START_S));
        }
        double up = GLOW_UP_S - START_S;
        double x0 = GLOW_PEAK + DampedSpring.displacement(GLOW_RISE_OMEGA, 1, -GLOW_PEAK, 0, up);
        double v0 = DampedSpring.velocity(GLOW_RISE_OMEGA, 1, -GLOW_PEAK, 0, up);
        float v = (float) DampedSpring.displacement(GLOW_FALL_OMEGA, 1, x0, v0, s - GLOW_UP_S);
        return v < GLOW_HIDE ? 0f : v;
    }

    /// The touch glow's drawn opacity (mask times layer) `s` seconds into a tap.
    static float glowOpacity(float s) {
        float layer = glowLayerOpacity(s);
        float mask = GLOW_MASK * layer;
        if (s >= GLOW_UP_S && mask < GLOW_HIDE) {
            return 0f;
        }
        return mask * layer;
    }

    /// The touch glow's diameter, as a multiple of GLOW_DIAMETER_PT, `s` seconds
    /// into a tap.
    static float glowScale(float s) {
        return (float) (1 + GLOW_GROWTH * (1 + DampedSpring.displacement(GLOW_FALL_OMEGA, 1, -1, 0, s - GLOW_UP_S)));
    }

    /// When a press on a jump of `travelPt` releases its lift: where the tap would
    /// (releaseSeconds), but never while the finger is still down.
    static float releaseForPress(float travelPt, float upS) {
        return Math.max(releaseSeconds(travelPt), upS);
    }

    /// Seconds after the press at which a held press has fully played out.
    static float heldDurationS(float upS, float releaseS) {
        float tables = durationMs() / 1000f;
        float fall = releaseS + Math.max(LIFT_FALL_S, PLATTER_RETURN_S);
        float wobble = upS + TabGlassGesture.WOBBLE_START_S
                + (TabGlassGesture.WOBBLE_X.length - 1) / (float) SAMPLE_HZ;
        return Math.max(tables, Math.max(fall, wobble));
    }
}
