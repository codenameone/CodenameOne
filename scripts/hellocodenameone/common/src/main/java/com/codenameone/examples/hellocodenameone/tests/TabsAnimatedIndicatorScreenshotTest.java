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

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Tabs;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;

/// Walks the animated tabs indicator from tab 0 to tab 2 across six frames so
/// the under-line slide is captured deterministically. Reads
/// [com.codename1.ui.animations.AnimationTime] (set per-frame by the harness)
/// to interpolate `indicatorFromX/W -> indicatorToX/W` via the Motion the
/// Tabs class started in `prepareCapture`.
public class TabsAnimatedIndicatorScreenshotTest extends AbstractAnimationScreenshotTest {
    private Form host;
    private Tabs tabs;

    @Override
    protected int getAnimationDurationMillis() {
        return 200;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        host = new Form("Tabs Indicator", new BorderLayout());
        host.setWidth(frameWidth);
        host.setHeight(frameHeight);
        host.setVisible(true);
        Style cps = host.getContentPane().getAllStyles();
        cps.setBgColor(0xf0f4f8);
        cps.setBgTransparency(255);

        tabs = new Tabs();
        tabs.setAnimatedIndicator(true);
        tabs.addTab("Home", new Button("Home content"));
        tabs.addTab("Search", new Button("Search content"));
        tabs.addTab("Profile", new Button("Profile content"));
        host.add(BorderLayout.CENTER, tabs);
        layoutOffScreen(host);

        // Kick off the indicator slide -- the Motion this starts reads
        // AnimationTime which the harness advances per frame.
        tabs.setSelectedIndex(2, false);
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        host.paintComponent(g, true);
    }

    @Override
    protected void finishCapture() {
        host = null;
        tabs = null;
        super.finishCapture();
    }
}
