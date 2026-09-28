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

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the iOS 27 tab selection motion model against the NATIVE capture it was
 * measured from: every row of tab-glass-motion/native-ios27.csv is one display
 * frame of a real UITabBarController selection on the iOS 27 simulator, and the
 * model must reproduce it frame by frame. The tolerances are in points on the
 * native 62 pt bar -- at @3x a point is three device pixels.
 *
 * Every captured frame is evaluated at its VSYNC time, round(t * 60) / 60: the
 * springs are rendered on the display clock (inverting them recovers each frame
 * at exactly n / 60 s, to 0.03 ms), while the logged times carry the main
 * thread's jitter -- the frame after the touch-up is logged 1.3 ms late in every
 * tap. The closed-form channels' tolerances sit just above their measured worst
 * error on that clock; the deformation and bar-pulse tables keep theirs.
 */
class TabGlassMotionTest {

    private static final class Frame {
        String tap;
        float travelPt;
        boolean fastLift;
        float t;
        float position;
        float liftPt;
        float scaleX;
        float scaleY;
        float leadPt;
        float platterOpacity;
        float contentScale;
        float barGrowPt;
    }

    private static List<Frame> load() throws IOException {
        List<Frame> out = new ArrayList<Frame>();
        InputStream in = TabGlassMotionTest.class.getResourceAsStream("/tab-glass-motion/native-ios27.csv");
        assertNotNull(in, "native capture fixture missing");
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("#") || line.trim().length() == 0) {
                    continue;
                }
                String[] p = line.split(",");
                Frame f = new Frame();
                f.tap = p[0] + "#" + p[1];
                f.travelPt = Float.parseFloat(p[2]);
                f.fastLift = "fast".equals(p[3]);
                f.t = Float.parseFloat(p[4]);
                f.position = Float.parseFloat(p[5]);
                f.liftPt = Float.parseFloat(p[6]);
                f.scaleX = Float.parseFloat(p[7]);
                f.scaleY = Float.parseFloat(p[8]);
                f.leadPt = Float.parseFloat(p[9]);
                f.platterOpacity = Float.parseFloat(p[10]);
                f.contentScale = Float.parseFloat(p[11]);
                f.barGrowPt = Float.parseFloat(p[12]);
                out.add(f);
            }
        } finally {
            r.close();
        }
        assertTrue(out.size() > 500, "fixture looks truncated: " + out.size());
        return out;
    }

    private static float restWidthPt(Frame f) {
        // The native 3-tab bar's lens is 94 pt wide at rest, the 5-tab bar's 84 pt.
        return f.tap.startsWith("light-grey-5b") ? 84f : 94f;
    }

    @Test
    void reproducesTheNativeMotionFrameByFrame() throws IOException {
        float worstPos = 0;
        float worstW = 0;
        float worstH = 0;
        float worstLead = 0;
        float worstLift = 0;
        float worstPlatter = 0;
        float worstContent = 0;
        float worstBar = 0;
        for (Frame f : load()) {
            float vsync = Math.round(f.t * TabGlassMotion.SAMPLE_HZ) / (float) TabGlassMotion.SAMPLE_HZ;
            TabGlassMotion m = TabGlassMotion.at(vsync * 1000f, f.travelPt);
            String where = f.tap + " t=" + f.t;
            float d = Math.abs(f.travelPt);
            worstPos = Math.max(worstPos, Math.abs(m.position - f.position) * d);
            worstLead = Math.max(worstLead, Math.abs(m.leadPt - f.leadPt));
            worstBar = Math.max(worstBar, Math.abs(m.barGrowPt - f.barGrowPt));
            if (f.fastLift) {
                float liftPt = TabGlassMotion.LIFT_PT * m.lift;
                worstLift = Math.max(worstLift, Math.abs(liftPt - f.liftPt));
                worstW = Math.max(worstW, Math.abs((restWidthPt(f) + liftPt) * m.scaleX
                        - (restWidthPt(f) + f.liftPt) * f.scaleX));
                worstH = Math.max(worstH, Math.abs((54f + liftPt) * m.scaleY - (54f + f.liftPt) * f.scaleY));
                worstPlatter = Math.max(worstPlatter, Math.abs(m.platterOpacity - f.platterOpacity));
                worstContent = Math.max(worstContent, Math.abs(m.contentScale - f.contentScale));
            } else {
                // The slow-lift regime changes the lens SIZE, not its travel or its
                // deformation; the scales alone still have to match.
                assertEquals(f.scaleX, m.scaleX, 0.02f, "scaleX " + where);
                assertEquals(f.scaleY, m.scaleY, 0.03f, "scaleY " + where);
            }
        }
        // Closed forms (measured worst: 0.77 pt, 0.12 pt, 0.007).
        assertTrue(worstPos <= 0.9f, "lens centre off by " + worstPos + " pt");
        assertTrue(worstLead <= 1.6f, "lens lead off by " + worstLead + " pt");
        assertTrue(worstBar <= 0.5f, "bar growth off by " + worstBar + " pt");
        assertTrue(worstLift <= 0.15f, "lift off by " + worstLift + " pt");
        assertTrue(worstW <= 2.0f, "lens width off by " + worstW + " pt");
        assertTrue(worstH <= 1.5f, "lens height off by " + worstH + " pt");
        assertTrue(worstPlatter <= 0.01f, "platter opacity off by " + worstPlatter);
        assertTrue(worstContent <= 0.004f, "content magnification off by " + worstContent);
    }

    @Test
    void releaseFollowsTheRemainingDistance() {
        // UIKit drops the lift once the lens is within 3.5 pt of its target: a
        // longer jump holds the lift longer.
        float near = TabGlassMotion.releaseSeconds(86f);
        float far = TabGlassMotion.releaseSeconds(172f);
        assertTrue(near < far, near + " vs " + far);
        assertEquals(0.233f, near, 0.01f);
        assertEquals(0.267f, far, 0.01f);
        // A short jump still waits for the lift to snap to full before releasing.
        assertEquals(near, TabGlassMotion.releaseSeconds(64f), 0.001f);
    }

    @Test
    void restsAtTheEndAndIsDirectionSymmetric() {
        TabGlassMotion done = TabGlassMotion.at(TabGlassMotion.durationMs() + 1, 100f);
        assertEquals(1f, done.position, 0f);
        assertEquals(1f, done.scaleX, 0f);
        assertEquals(1f, done.platterOpacity, 0f);
        for (int ms = 0; ms < TabGlassMotion.durationMs(); ms += 17) {
            TabGlassMotion r = TabGlassMotion.at(ms, 120f);
            TabGlassMotion l = TabGlassMotion.at(ms, -120f);
            assertEquals(r.position, l.position, 0f);
            assertEquals(r.scaleX, l.scaleX, 0f);
            assertEquals(r.leadPt, -l.leadPt, 0f);
            assertEquals(r.lift, l.lift, 0f);
        }
    }

    /// Native touch glow, per-frame means over the eleven clean taps of the capture
    /// (the fixture does not carry the glow): frame, diameter scale, drawn opacity.
    private static final float[][] NATIVE_GLOW = {
        {2, 1.0000f, 0.08153f}, {3, 1.0000f, 0.15444f}, {5, 1.0573f, 0.21906f}, {8, 1.6145f, 0.17483f},
        {12, 2.4973f, 0.07740f}, {20, 3.5425f, 0.00782f}, {30, 3.9165f, 0.00028f}, {34, 3.9592f, 0f},
        {45, 3.9946f, 0f}
    };

    @Test
    void theTouchGlowIsTheMeasuredSprings() {
        for (float[] g : NATIVE_GLOW) {
            TabGlassMotion m = TabGlassMotion.at(g[0] * 1000f / TabGlassMotion.SAMPLE_HZ, 86f);
            // The scale has no tap-to-tap spread in the capture; the opacity spreads
            // by up to 0.0024 on the rising frames.
            assertEquals(g[1], m.glowScale, 0.0005f, "glow scale at frame " + (int) g[0]);
            assertEquals(g[2], m.glowOpacity, 0.001f, "glow opacity at frame " + (int) g[0]);
        }
    }

    @Test
    void theGlowMaskIsAFixedFractionOfTheLayerAndHidesAtTheSameLevel() {
        // Drawn opacity = mask * layer with mask = 0.33675 * layer, so while both
        // are visible it is 0.33675 * layer^2; the mask drops out first.
        float s = 10f / TabGlassMotion.SAMPLE_HZ;
        float layer = TabGlassMotion.glowLayerOpacity(s);
        assertEquals(0.33675f * layer * layer, TabGlassMotion.glowOpacity(s), 1e-6f);
        assertEquals(0f, TabGlassMotion.glowOpacity(34f / TabGlassMotion.SAMPLE_HZ), 0f);
        assertTrue(TabGlassMotion.glowLayerOpacity(39f / TabGlassMotion.SAMPLE_HZ) > 0f);
        assertEquals(0f, TabGlassMotion.glowLayerOpacity(40f / TabGlassMotion.SAMPLE_HZ), 0f);
    }

    @Test
    void theTravelSettlesLikeUikit() {
        // UIKit ends the travel past its overshoot once less than 0.2 pt remains:
        // the frame it happened at in the capture, per jump distance.
        float[][] snaps = {{64.04f, 29}, {86f, 31}, {128.1f, 33}, {172f, 34}};
        for (float[] sn : snaps) {
            float at = sn[1] / TabGlassMotion.SAMPLE_HZ;
            float before = (sn[1] - 1) / TabGlassMotion.SAMPLE_HZ;
            assertEquals(1f, TabGlassMotion.settledPosition(at, sn[0]), 0f, sn[0] + " pt at frame " + (int) sn[1]);
            assertTrue(TabGlassMotion.settledPosition(before, sn[0]) > 1f, sn[0] + " pt before frame " + (int) sn[1]);
        }
        // The unsettled curve is the spring of duration 0.4 s and bounce 0.15: its
        // overshoot peak is 0.6% at ~0.38 s.
        float peak = 0;
        for (int ms = 300; ms < 500; ms++) {
            peak = Math.max(peak, TabGlassMotion.position(ms / 1000f));
        }
        assertEquals(1.0063f, peak, 0.0002f);
    }

    @Test
    void theLiftAndPlatterEndOnTheCapturedFrames() {
        // Counted from the release frame, the capture drops the lift to 0 on frame 14
        // (13 in one tap) and lands the platter on 1 on frame 29 (28 in one tap).
        float release = TabGlassMotion.releaseSeconds(86f);
        float hz = TabGlassMotion.SAMPLE_HZ;
        assertTrue(TabGlassMotion.liftFall(13f / hz) > 0f, "lift gone before frame 14");
        assertEquals(0f, TabGlassMotion.liftFall(14f / hz), 0f);
        assertTrue(TabGlassMotion.platterReturn(28f / hz, release) < 1f, "platter back before frame 29");
        assertEquals(1f, TabGlassMotion.platterReturn(29f / hz, release), 0f);
    }

    @Test
    void theClosedFormsRestBeforeTheTablesEnd() {
        float end = TabGlassMotion.durationMs() / 1000f;
        float release = TabGlassMotion.releaseSeconds(258f);
        assertEquals(1f, TabGlassMotion.settledPosition(end, 258f), 0f);
        assertEquals(0f, TabGlassMotion.liftFall(end - release), 0f);
        assertEquals(1f, TabGlassMotion.platterReturn(end - release, release), 0f);
        assertEquals(0f, TabGlassMotion.glowOpacity(end), 0f);
        assertEquals(4f, TabGlassMotion.glowScale(end), 0.001f);
    }

    @Test
    void theBarPulsesAndTheLensLiftsAndSquashes() {
        // Coarse shape checks that read as the native description, so a table
        // regenerated from a bad capture fails loudly.
        float peakBar = 0;
        float minBar = 0;
        float peakLift = 0;
        float minScaleX = 2;
        for (int ms = 0; ms < 700; ms += 5) {
            TabGlassMotion m = TabGlassMotion.at(ms, 172f);
            peakBar = Math.max(peakBar, m.barGrowPt);
            minBar = Math.min(minBar, m.barGrowPt);
            peakLift = Math.max(peakLift, m.lift);
            minScaleX = Math.min(minScaleX, m.scaleX);
        }
        assertEquals(8.7f, peakBar, 0.3f);
        assertTrue(minBar < -0.5f, "bar should undershoot, min " + minBar);
        assertEquals(1f, peakLift, 0f);
        assertEquals(0.905f, minScaleX, 0.01f);
    }
}
