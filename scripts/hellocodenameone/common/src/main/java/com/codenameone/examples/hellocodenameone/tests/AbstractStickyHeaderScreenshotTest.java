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

import com.codename1.components.StickyHeaderContainer;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// Shared scaffolding for [StickyHeaderContainer] screenshot tests. Subclasses
/// build a host form with a fixed list of contrasting sections and choose
/// either a fast scroll sweep (driving the container's scroll position
/// across all boundaries) or a slow scroll demo (holding scroll past one
/// boundary and stepping `AnimationTime` through the swap animation).
abstract class AbstractStickyHeaderScreenshotTest extends AbstractAnimationScreenshotTest {
    protected static final int SECTION_COUNT = 5;
    protected static final int ITEMS_PER_SECTION = 4;
    protected static final int[] HEADER_COLORS = {
            0x118ab2, 0xef476f, 0x06d6a0, 0xffd166, 0x8338ec
    };

    protected Form host;
    protected StickyHeaderContainer sticky;

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        host = new Form(getDisplayTitle());
        host.setLayout(new BorderLayout());
        host.setWidth(frameWidth);
        host.setHeight(frameHeight);
        host.setVisible(true);

        sticky = new StickyHeaderContainer();
        configureTransition(sticky);
        Style ss = sticky.getAllStyles();
        ss.setBgColor(0xf3f4f6);
        ss.setBgTransparency(255);
        ss.setPadding(0, 0, 0, 0);
        ss.setMargin(0, 0, 0, 0);

        Style scrollerStyle = sticky.getScrollContainer().getAllStyles();
        scrollerStyle.setBgColor(0xf3f4f6);
        scrollerStyle.setBgTransparency(255);
        scrollerStyle.setPadding(0, 0, 0, 0);
        scrollerStyle.setMargin(0, 0, 0, 0);

        for (int s = 0; s < SECTION_COUNT; s++) {
            Label header = makeHeader("Section " + (char) ('A' + s), HEADER_COLORS[s]);
            Container content = new Container(BoxLayout.y());
            Style cs = content.getAllStyles();
            cs.setBgColor(0xffffff);
            cs.setBgTransparency(255);
            cs.setPadding(0, 0, 0, 0);
            cs.setMargin(0, 0, 0, 0);
            for (int i = 0; i < ITEMS_PER_SECTION; i++) {
                Label item = new Label(((char) ('A' + s)) + " - row " + (i + 1));
                Style is = item.getAllStyles();
                is.setBgColor(0xffffff);
                is.setBgTransparency(255);
                is.setFgColor(0x111827);
                is.setPadding(10, 10, 14, 14);
                is.setMargin(0, 0, 0, 0);
                content.add(item);
            }
            sticky.addSection(header, content);
        }

        host.add(BorderLayout.CENTER, sticky);
        layoutOffScreen(host);
        sticky.layoutContainer();
        sticky.getScrollContainer().layoutContainer();
    }

    /// Subclasses configure the transition style and duration here before
    /// sections are added.
    protected abstract void configureTransition(StickyHeaderContainer sticky);

    @Override
    protected void finishCapture() {
        host = null;
        sticky = null;
        super.finishCapture();
    }

    private static Label makeHeader(String text, int color) {
        Label l = new Label(text);
        Style s = l.getAllStyles();
        s.setBgColor(color);
        s.setBgTransparency(255);
        s.setFgColor(0xffffff);
        s.setPadding(12, 12, 18, 18);
        s.setMargin(0, 0, 0, 0);
        return l;
    }

    protected int sectionStrideHeight() {
        if (sticky == null) {
            return 0;
        }
        Container scroller = sticky.getScrollContainer();
        if (scroller.getComponentCount() < 2) {
            return 0;
        }
        return scroller.getComponentAt(2).getY() - scroller.getComponentAt(0).getY();
    }
}
