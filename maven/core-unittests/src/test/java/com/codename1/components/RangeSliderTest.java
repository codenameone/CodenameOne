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

class RangeSliderTest extends UITestBase {

    @FormTest
    void setRangeSwapsSnapsAndClamps() {
        RangeSlider r = new RangeSlider(0, 100, 20, 80);
        r.setStep(10);
        r.setRange(74, 12);
        assertEquals(10, r.getLowerValue());
        assertEquals(70, r.getUpperValue());
        r.setRange(-50, 500);
        assertEquals(0, r.getLowerValue());
        assertEquals(100, r.getUpperValue());
    }

    @FormTest
    void firesOnChangeOnly() {
        RangeSlider r = new RangeSlider(0, 10, 2, 8);
        final int[] n = {0};
        r.addActionListener(e -> n[0]++);
        r.setRange(3, 8);
        r.setRange(3, 8);
        assertEquals(1, n[0]);
    }

    @FormTest
    void positionAndValueRoundTrip() {
        RangeSlider r = new RangeSlider(0, 100, 0, 100);
        r.setWidth(400);
        r.setHeight(40);
        assertEquals(0, r.valueAt(r.positionOf(0)));
        assertEquals(50, r.valueAt(r.positionOf(50)));
        assertEquals(100, r.valueAt(r.positionOf(100)));
    }

    @FormTest
    void unthemedPaintsNoBackground() {
        assertEquals(0, new RangeSlider().getStyle().getBgTransparency() & 0xff);
        assertEquals(0, new CircularProgress().getStyle().getBgTransparency() & 0xff);
        assertEquals(0, new AudioWaveform().getStyle().getBgTransparency() & 0xff);
        assertEquals(0, new ActivityTimeline().getStyle().getBgTransparency() & 0xff);
        assertEquals(0, new ChipInput().getStyle().getBgTransparency() & 0xff);
        assertEquals(0, new ColorPicker().getStyle().getBgTransparency() & 0xff);
        assertEquals(0, new MentionField().getStyle().getBgTransparency() & 0xff);
    }

    @FormTest
    void tapFiresOnceWhileAdjustingAndOnceFinal() {
        RangeSlider r = new RangeSlider(0, 100, 20, 80);
        com.codename1.ui.Form f = new com.codename1.ui.Form("t", new com.codename1.ui.layouts.BorderLayout());
        f.add(com.codename1.ui.layouts.BorderLayout.NORTH, r);
        f.show();
        f.revalidate();
        final int[] adjusting = {0};
        final int[] finals = {0};
        r.addActionListener(e -> {
            if (r.isAdjusting()) {
                adjusting[0]++;
            } else {
                finals[0]++;
            }
        });
        int x = r.getAbsoluteX() + r.positionOf(60);
        int y = r.getAbsoluteY() + r.getHeight() / 2;
        f.pointerPressed(x, y);
        f.pointerReleased(x, y);
        assertEquals(1, adjusting[0]);
        assertEquals(1, finals[0]);
        // a tap that moves nothing fires nothing
        f.pointerPressed(x, y);
        f.pointerReleased(x, y);
        assertEquals(1, adjusting[0]);
        assertEquals(1, finals[0]);
    }

    @FormTest
    void coincidentThumbsSplitInRtl() {
        RangeSlider r = new RangeSlider(0, 100, 50, 50);
        r.setRTL(true);
        com.codename1.ui.Form f = new com.codename1.ui.Form("t", new com.codename1.ui.layouts.BorderLayout());
        f.add(com.codename1.ui.layouts.BorderLayout.NORTH, r);
        f.show();
        f.revalidate();
        int y = r.getAbsoluteY() + r.getHeight() / 2;
        // In RTL the higher values are on the left.
        int x = r.getAbsoluteX() + r.positionOf(80);
        f.pointerPressed(x, y);
        f.pointerReleased(x, y);
        assertEquals(50, r.getLowerValue());
        assertEquals(80, r.getUpperValue(), 2);
    }
}
