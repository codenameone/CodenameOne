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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.ActivityTimeline;
import com.codename1.components.AudioWaveform;
import com.codename1.components.CircularProgress;
import com.codename1.components.ExpandableText;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.Layout;

/// `CircularProgress` rings, an `AudioWaveform` part way through, a collapsed
/// `ExpandableText` and an `ActivityTimeline` with done, current and pending
/// steps, in light and dark. The waveform's levels are computed, not random,
/// so the frame is deterministic.
public class ProgressAndMediaScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=ProgressAndMedia status=SKIPPED reason=phone-layout-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "ProgressAndMedia";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        body.setScrollVisible(false);
        Container rings = new Container(new FlowLayout(CN.CENTER));
        rings.add(new CircularProgress(25));
        rings.add(new CircularProgress(70));
        CircularProgress steps = new CircularProgress(100);
        steps.setText("7/7");
        rings.add(steps);
        body.add(rings);
        float[] levels = new float[40];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = 0.2f + 0.8f * (((i * 37) % 11) / 10f);
        }
        AudioWaveform wave = new AudioWaveform(levels);
        wave.setProgress(0.4f);
        body.add(wave);
        body.add(new ExpandableText("Codename One builds native apps for iOS, Android, desktop and the web "
                + "from a single Java or Kotlin code base. This paragraph is long enough to be cut "
                + "after two lines, so the Read more link shows under it on every screen size.", 2));
        body.add(new ActivityTimeline()
                .addStep("Order placed", "We received your order", "9:02", ActivityTimeline.DONE)
                .addStep("Shipped", "Left the warehouse", "13:40", ActivityTimeline.DONE)
                .addStep("Out for delivery", "Arriving today", null, ActivityTimeline.CURRENT)
                .addStep("Delivered", null, null, ActivityTimeline.PENDING));
        form.add(BorderLayout.CENTER, body);
    }
}
