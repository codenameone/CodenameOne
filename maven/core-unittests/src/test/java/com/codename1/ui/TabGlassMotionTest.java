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
            TabGlassMotion m = TabGlassMotion.at(f.t * 1000f, f.travelPt);
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
        assertTrue(worstPos <= 1.6f, "lens centre off by " + worstPos + " pt");
        assertTrue(worstLead <= 1.6f, "lens lead off by " + worstLead + " pt");
        assertTrue(worstBar <= 0.5f, "bar growth off by " + worstBar + " pt");
        assertTrue(worstLift <= 0.25f, "lift off by " + worstLift + " pt");
        assertTrue(worstW <= 2.0f, "lens width off by " + worstW + " pt");
        assertTrue(worstH <= 1.5f, "lens height off by " + worstH + " pt");
        assertTrue(worstPlatter <= 0.02f, "platter opacity off by " + worstPlatter);
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
