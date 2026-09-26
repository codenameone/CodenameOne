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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Holds TabGlassGesture to native iOS 27 captures of held presses and scrubs
 * (fixture native-gestures.csv, written by tabmotion.py gesture-fixture from
 * the motion probe). Each episode replays its recorded touch events through
 * the model and compares every captured frame.
 */
class TabGlassGestureTest {

    private static final class Episode {
        String tag;
        float fromPt;
        float toPt;
        float minPt;
        float maxPt;
        float upS;
        float scrubS;
        float settlePt;
        final List<float[]> touches = new ArrayList<float[]>();
        final List<float[]> frames = new ArrayList<float[]>();
    }

    private static Map<Integer, Episode> load() throws Exception {
        Map<Integer, Episode> eps = new LinkedHashMap<Integer, Episode>();
        InputStream in = TabGlassGestureTest.class.getResourceAsStream("/tab-glass-motion/native-gestures.csv");
        assertNotNull(in, "fixture missing");
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("#") || line.trim().length() == 0) {
                    continue;
                }
                String[] p = line.split(",");
                int id = Integer.parseInt(p[1]);
                if ("E".equals(p[0])) {
                    Episode e = new Episode();
                    e.tag = p[2];
                    e.fromPt = Float.parseFloat(p[3]);
                    e.toPt = Float.parseFloat(p[4]);
                    e.minPt = Float.parseFloat(p[5]);
                    e.maxPt = Float.parseFloat(p[6]);
                    e.upS = Float.parseFloat(p[7]);
                    e.scrubS = Float.parseFloat(p[8]);
                    e.settlePt = Float.parseFloat(p[9]);
                    eps.put(id, e);
                } else {
                    float[] v = new float[p.length - 2];
                    for (int i = 2; i < p.length; i++) {
                        v[i - 2] = Float.parseFloat(p[i]);
                    }
                    (("T".equals(p[0])) ? eps.get(id).touches : eps.get(id).frames).add(v);
                }
            }
        } finally {
            r.close();
        }
        return eps;
    }

    private static TabGlassGesture replay(Episode e) {
        TabGlassGesture g = new TabGlassGesture(e.fromPt, e.toPt);
        if (e.scrubS >= 0) {
            g.startScrub(e.scrubS, e.minPt, e.maxPt);
            for (float[] t : e.touches) {
                g.finger(t[0], t[1]);
            }
            g.settleTo(e.settlePt);
        }
        g.up(e.upS);
        return g;
    }

    /// Worst error per channel (x, lift, scaleX, scaleY, lead, bar), then the sum
    /// of squared x and lift errors and the frame count.
    private static float[] errors(Episode e) {
        TabGlassGesture g = replay(e);
        float[] worst = new float[9];
        float[] c = new float[1];
        for (float[] f : e.frames) {
            float s = f[0];
            if (s < 0) {
                continue;
            }
            TabGlassMotion m = g.at(s, c);
            float[] err = {c[0] - f[1], m.lift - f[2], (m.scaleX - 1) - f[3], (m.scaleY - 1) - f[4],
                m.leadPt - f[5], m.barGrowPt - f[6]};
            for (int i = 0; i < 6; i++) {
                worst[i] = Math.max(worst[i], Math.abs(err[i]));
            }
            worst[6] += err[0] * err[0];
            worst[7] += err[1] * err[1];
            worst[8]++;
        }
        return worst;
    }

    // Worst error allowed per episode: x (pt), lift, scaleX, scaleY, lead (pt), bar (pt).
    private static final float[] HOLD_LIMITS = {1.5f, 0.05f, 0.02f, 0.025f, 1.2f, 1.2f};
    // Scrubs include flicks released with the lens ~100 pt behind the finger,
    // where the linear settle is a few points off; the RMS bounds below keep the
    // ordinary releases tight.
    private static final float[] SCRUB_LIMITS = {8f, 0.45f, 0.04f, 0.05f, 2f, 2.2f};

    @Test
    void reproducesNativeHoldsAndScrubs() throws Exception {
        Map<Integer, Episode> eps = load();
        assertEquals(21, eps.size());
        StringBuilder report = new StringBuilder();
        float sx2 = 0;
        float sl2 = 0;
        float n = 0;
        String[] names = {"x", "lift", "scaleX", "scaleY", "lead", "bar"};
        for (Map.Entry<Integer, Episode> en : eps.entrySet()) {
            Episode e = en.getValue();
            if (en.getKey() == 0) {
                // The first press of a capture run lags a frame on the device (the
                // tap fit leaves those out for the same reason).
                continue;
            }
            float[] w = errors(e);
            boolean scrub = e.scrubS >= 0;
            float[] lim = scrub ? SCRUB_LIMITS : HOLD_LIMITS;
            for (int i = 0; i < 6; i++) {
                assertTrue(w[i] <= lim[i], "episode " + en.getKey() + " (" + e.tag + ") " + names[i]
                        + " off by " + w[i] + ", limit " + lim[i]);
            }
            if (scrub) {
                sx2 += w[6];
                sl2 += w[7];
                n += w[8];
            }
            report.append(en.getKey()).append(' ').append(e.tag).append(": x ").append(w[0])
                    .append(" lift ").append(w[1]).append('\n');
        }
        float rmsX = (float) Math.sqrt(sx2 / n);
        float rmsLift = (float) Math.sqrt(sl2 / n);
        assertTrue(rmsX <= 0.8f, "scrub x rms " + rmsX + "\n" + report);
        assertTrue(rmsLift <= 0.04f, "scrub lift rms " + rmsLift + "\n" + report);
    }

    @Test
    void scrubReleaseGoesWhereTheFingerIsNotTheLens() {
        // A flick: the finger reaches the last tab while the lens is far behind.
        TabGlassGesture g = new TabGlassGesture(51, 51);
        g.startScrub(0.3f, 51, 223);
        g.finger(0.3f, 51);
        g.finger(0.35f, 223);
        g.settleTo(223);
        g.up(0.4f);
        float[] c = new float[1];
        g.at(0.4f, c);
        assertTrue(c[0] < 200, "lens still behind the finger at release: " + c[0]);
        g.at(1.5f, c);
        assertEquals(223, c[0], 0.5f);
    }

    @Test
    void heldPressKeepsTheLiftUntilRelease() {
        TabGlassGesture g = new TabGlassGesture(51, 137);
        float[] c = new float[1];
        assertEquals(1f, g.at(1.2f, c).lift, 1e-6f, "lift holds while pressed");
        assertFalse(g.isFinished(10f), "never finishes while pressed");
        g.up(1.5f);
        assertEquals(1f, g.at(1.5f, c).lift, 1e-6f);
        assertTrue(g.at(1.7f, c).lift < 0.9f, "falls after release");
        assertTrue(g.isFinished(4f));
    }
}
