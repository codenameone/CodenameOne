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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.RoundBorder;
import java.util.Hashtable;
import static org.junit.jupiter.api.Assertions.*;

class SliderNativeProgressSizeTest extends UITestBase {
    @FormTest
    void thinProgressTrackReservesTextHeightOnlyWhenTextIsEnabled() {
        Hashtable theme = new Hashtable();
        theme.put("@progressTrackThicknessMM", "0.3");
        UIManager.getInstance().setThemeProps(theme);
        Slider progress = new Slider();
        progress.setEditable(false);
        progress.getAllStyles().setPadding(2, 2, 0, 0);
        progress.getAllStyles().setBorder(null);
        progress.getAllStyles().setBgTransparency(255);
        progress.getSliderFullUnselectedStyle().setBgTransparency(255);
        progress.getSliderFullSelectedStyle().setBgTransparency(255);
        progress.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE));
        int trackHeight = Math.max(2, Display.getInstance().convertToPixels(0.3f));
        int textHeight = progress.getStyle().getFont().getHeight();
        assertEquals(trackHeight + 4, progress.getPreferredH());
        progress.setRenderPercentageOnTop(true);
        assertTrue(progress.getPreferredH() >= textHeight + 4, "percentage text must fit after toggling a cached size");
        progress.setRenderPercentageOnTop(false);
        assertEquals(trackHeight + 4, progress.getPreferredH());
        progress.setRenderValueOnTop(true);
        assertTrue(progress.getPreferredH() >= textHeight + 4, "value text must fit after toggling a cached size");
        progress.setRenderValueOnTop(false);
        assertEquals(trackHeight + 4, progress.getPreferredH());
        progress.setEditable(true);
        assertTrue(progress.getPreferredH() >= Font.getDefaultFont().getHeight() + 4,
                "editable sliders must discard the cached thin progress height");
        progress.setEditable(false);
        assertEquals(trackHeight + 4, progress.getPreferredH(),
                "switching back to a progress bar must discard the cached slider height");
        progress.setVertical(true);
        assertTrue(progress.getPreferredH() >= Font.getDefaultFont().getHeight() + 4,
                "vertical mode must discard the cached horizontal track height");
        progress.setVertical(false);
        assertEquals(trackHeight + 4, progress.getPreferredH(),
                "horizontal mode must discard the cached vertical height");
        progress.setInfinite(true);
        assertTrue(progress.getPreferredH() >= Font.getDefaultFont().getHeight() + 4,
                "indeterminate mode must discard the cached thin-track height");
        progress.setInfinite(false);
        assertEquals(trackHeight + 4, progress.getPreferredH(),
                "returning to determinate mode must discard the cached full height");
    }

    private Slider nativeProgress() {
        Hashtable theme = new Hashtable();
        theme.put("@progressTrackThicknessMM", "0.3");
        UIManager.getInstance().setThemeProps(theme);
        Slider progress = new Slider();
        progress.setEditable(false);
        progress.setProgress(50);
        progress.setWidth(100);
        progress.setHeight(40);
        for (Style style : new Style[] {progress.getUnselectedStyle(), progress.getSelectedStyle(),
                progress.getDisabledStyle(), progress.getPressedStyle(),
                progress.getSliderFullUnselectedStyle(), progress.getSliderFullSelectedStyle()}) {
            style.setPadding(0, 0, 0, 0);
            style.setBgTransparency(255);
            style.setBackgroundType(Style.BACKGROUND_NONE);
            style.setBorder(null);
        }
        return progress;
    }

    @FormTest
    void customProgressAssetsKeepLegacyHeightAndPainters() {
        Slider progress = nativeProgress();
        int trackHeight = Math.max(2, Display.getInstance().convertToPixels(0.3f));
        assertEquals(trackHeight, progress.getPreferredH());
        Style full = progress.getSliderFullUnselectedStyle();
        Painter original = full.getBgPainter();
        int[] paints = {0};
        full.setBgPainter((g, rect) -> paints[0]++);
        assertTrue(progress.getPreferredH() >= Font.getDefaultFont().getHeight());
        progress.paintComponentBackground(Image.createImage(100, 40).getGraphics());
        assertEquals(1, paints[0], "custom fill painter must run");
        full.setBgPainter(original);
        assertEquals(trackHeight, progress.getPreferredH());

        Style empty = progress.getUnselectedStyle();
        Painter emptyOriginal = empty.getBgPainter();
        empty.setBgPainter((g, rect) -> paints[0]++);
        progress.paintComponentBackground(Image.createImage(100, 40).getGraphics());
        assertEquals(2, paints[0], "custom empty painter must run");
        empty.setBgPainter(emptyOriginal);
        assertEquals(trackHeight, progress.getPreferredH());

        full.setBgImage(Image.createImage(8, 24));
        assertTrue(progress.getPreferredH() > trackHeight, "fill image must use legacy sizing");
        full.setBgImage(null);
        assertEquals(trackHeight, progress.getPreferredH());
        empty.setBorder(Border.createLineBorder(2));
        assertTrue(progress.getPreferredH() > trackHeight, "custom border must use legacy sizing");
        empty.setBorder(null);
        assertEquals(trackHeight, progress.getPreferredH());
        progress.setThumbImage(Image.createImage(8, 32));
        assertTrue(progress.getPreferredH() >= 32, "thumb must fit after caching thin size");
        progress.setThumbImage(null);
        assertEquals(trackHeight, progress.getPreferredH());
        empty.setBackgroundType(Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL);
        assertTrue(progress.getPreferredH() > trackHeight, "gradient must retain legacy painter");
        empty.setBackgroundType(Style.BACKGROUND_NONE);
        empty.setBgTransparency(100);
        assertTrue(progress.getPreferredH() > trackHeight, "translucent backgrounds must remain translucent");
    }

    @FormTest
    void bundledPlainPillsStayNativeWhileDecoratedPillsKeepLegacyPath() {
        Slider progress = nativeProgress();
        int trackHeight = Math.max(2, Display.getInstance().convertToPixels(0.3f));
        for (Style style : new Style[] {progress.getUnselectedStyle(), progress.getSelectedStyle(),
                progress.getDisabledStyle(), progress.getPressedStyle(),
                progress.getSliderFullUnselectedStyle(), progress.getSliderFullSelectedStyle()}) {
            style.setBgColor(0x007aff);
            style.setBgTransparency(0);
            style.setBorder(RoundBorder.create().rectangle(true).color(0x007aff)
                    .stroke(1, false).strokeOpacity(0));
        }
        assertEquals(trackHeight, progress.getPreferredH(), "CSS pill borders must retain native sizing");
        progress.getSliderFullUnselectedStyle().setBorder(RoundBorder.create().rectangle(true)
                .color(0x007aff).stroke(2, false).strokeOpacity(255));
        assertTrue(progress.getPreferredH() > trackHeight, "decorated pill border must be preserved");
    }


    @FormTest
    void customStateArtworkReservesLegacyHeightBeforeEnteringThatState() {
        boolean pureTouch = display.isPureTouch();
        display.setPureTouch(false);
        try {
            for (int state = 0; state < 4; state++) {
                Slider progress = nativeProgress();
                Style custom;
                if (state == 0) {
                    custom = new Style(progress.getUnselectedStyle());
                    progress.setHoverStyle(custom);
                } else if (state == 1) {
                    custom = progress.getSelectedStyle();
                } else if (state == 2) {
                    custom = progress.getDisabledStyle();
                } else {
                    custom = progress.getPressedStyle();
                    Button lead = new Button();
                    Container parent = new Container();
                    parent.add(progress).add(lead);
                    parent.setLeadComponent(lead);
                    Form form = new Form();
                    form.add(parent);
                    form.show();
                    progress.setWidth(100);
                    progress.setHeight(40);
                }
                int[] paints = {0};
                custom.setBgPainter((g, rect) -> paints[0]++);
                int cachedHeight = progress.getPreferredH();
                assertTrue(cachedHeight >= Font.getDefaultFont().getHeight(),
                        "normal state must already reserve the custom state's legacy height: " + state);
                if (state == 0) {
                    progress.setHovered(true);
                } else if (state == 1) {
                    progress.setFocusable(true);
                    progress.setFocus(true);
                } else if (state == 2) {
                    progress.setEnabled(false);
                } else {
                    ((Button) progress.getLeadComponent()).setState(Button.STATE_PRESSED);
                }
                assertEquals(cachedHeight, progress.getPreferredH());
                progress.paintComponentBackground(Image.createImage(100, 40).getGraphics());
                assertEquals(1, paints[0], "custom state painter must run: " + state);
            }
        } finally {
            display.setPureTouch(pureTouch);
        }
    }

}
