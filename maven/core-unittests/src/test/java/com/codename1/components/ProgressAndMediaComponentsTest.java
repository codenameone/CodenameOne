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
package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class ProgressAndMediaComponentsTest extends UITestBase {

    @FormTest
    void circularProgressClampsAndLabels() {
        CircularProgress p = new CircularProgress(150);
        assertEquals(100, p.getProgress());
        p.setProgress(-3);
        assertEquals(0, p.getProgress());
        p.setProgress(42);
        assertEquals("42%", p.getDisplayedText());
        p.setText("3 of 7");
        assertEquals("3 of 7", p.getDisplayedText());
        assertTrue(p.getPreferredW() > 0 && p.getPreferredH() > 0);
    }

    @FormTest
    void waveformFromPcmIsNormalised() {
        short[] pcm = new short[1000];
        for (int i = 0; i < pcm.length; i++) {
            pcm[i] = (short) (i < 500 ? 1000 : 16000);
        }
        float[] levels = AudioWaveform.levelsFromPcm(pcm, 10);
        assertEquals(10, levels.length);
        float max = 0;
        for (float l : levels) {
            assertTrue(l >= 0 && l <= 1);
            max = Math.max(max, l);
        }
        assertEquals(1f, max, 0.001f);
        assertTrue(levels[0] < levels[9]);
    }

    @FormTest
    void waveformProgressClampsAndSeeks() {
        AudioWaveform w = new AudioWaveform(new float[] {0.2f, 0.5f, 1f});
        w.setProgress(2f);
        assertEquals(1f, w.getProgress(), 0.0001f);
        w.setWidth(300);
        w.setHeight(40);
        assertEquals(0f, w.progressAt(w.getAbsoluteX() - 50), 0.0001f);
        assertEquals(1f, w.progressAt(w.getAbsoluteX() + 1000), 0.0001f);
    }

    @FormTest
    void expandableTextToggles() {
        ExpandableText t = new ExpandableText("short", 2);
        assertFalse(t.isExpanded());
        String more = t.getLink().getText();
        t.setExpanded(true);
        assertTrue(t.isExpanded());
        assertNotEquals(more, t.getLink().getText());
        t.setMaxLines(0);
        assertTrue(t.getMaxLines() >= 1);
    }

    @FormTest
    void timelineKeepsSteps() {
        ActivityTimeline t = new ActivityTimeline()
                .addStep("Ordered", null, "9:00", ActivityTimeline.DONE)
                .addStep("Shipped", "On its way", null, ActivityTimeline.CURRENT)
                .addStep("Delivered", null, null, ActivityTimeline.PENDING);
        assertEquals(3, t.getStepCount());
        assertEquals(ActivityTimeline.CURRENT, t.getState(1));
        t.setState(1, ActivityTimeline.DONE);
        assertEquals(ActivityTimeline.DONE, t.getState(1));
        t.clearSteps();
        assertEquals(0, t.getStepCount());
    }
}
