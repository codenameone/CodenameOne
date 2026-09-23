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

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

/// Visualises a smooth-scroll animation. Off-screen forms aren't initialized,
/// so `scrollRectToVisible` would jump rather than tween; instead we drive the
/// container's scrollY directly with the same linear motion the framework's
/// `initScrollMotion` uses, then paint a frame for each motion sample.
public class SmoothScrollScreenshotTest extends AbstractAnimationScreenshotTest {
    private static class ScrollContainer extends Container {
        ScrollContainer(Layout l) {
            super(l);
            setScrollableY(true);
        }

        void scrollTo(int y) {
            setScrollY(y);
        }
    }

    private Form scrollHost;
    private ScrollContainer scrollContainer;
    private Motion scrollMotion;

    @Override
    protected int getAnimationDurationMillis() {
        return 800;
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        scrollHost = new Form("Smooth Scroll");
        scrollHost.setLayout(new BorderLayout());
        scrollHost.setWidth(frameWidth);
        scrollHost.setHeight(frameHeight);
        scrollHost.setVisible(true);

        scrollContainer = new ScrollContainer(BoxLayout.y());
        Style cs = scrollContainer.getAllStyles();
        cs.setBgColor(0xfafafa);
        cs.setBgTransparency(255);
        cs.setPadding(4, 4, 4, 4);
        int tileCount = 24;
        for (int i = 0; i < tileCount; i++) {
            Label tile = new Label("Item " + (i + 1));
            Style ts = tile.getAllStyles();
            ts.setBgColor(rowColor(i));
            ts.setFgColor(0xffffff);
            ts.setBgTransparency(255);
            ts.setMargin(2, 2, 2, 2);
            ts.setPadding(14, 14, 12, 12);
            scrollContainer.add(tile);
        }
        scrollHost.add(BorderLayout.CENTER, scrollContainer);
        layoutOffScreen(scrollHost);

        int contentHeight = scrollContainer.getScrollDimension().getHeight();
        int maxScroll = Math.max(0, contentHeight - scrollContainer.getHeight());
        scrollMotion = Motion.createEaseInOutMotion(0, maxScroll, getAnimationDurationMillis());
        scrollMotion.start();
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        scrollContainer.scrollTo(scrollMotion.getValue());
        scrollHost.paintComponent(g, true);
    }

    @Override
    protected void finishCapture() {
        scrollHost = null;
        scrollContainer = null;
        scrollMotion = null;
        super.finishCapture();
    }

    private static int rowColor(int i) {
        int[] palette = {0x118ab2, 0x06d6a0, 0xffd166, 0xef476f, 0x8338ec, 0x073b4c};
        return palette[i % palette.length];
    }
}
