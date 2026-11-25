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
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/// Pointer events sent to the form, not to the widget, so hit testing is part
/// of what is tested: a plain Component that asks for no pointer events is
/// skipped by it and never sees the tap.
class NewComponentsPointerTest extends UITestBase {

    private Form shown(Component c) {
        Form f = new Form("t", BoxLayout.y());
        f.getContentPane().setScrollableY(true);
        f.add(c);
        f.show();
        f.revalidate();
        return f;
    }

    private static int cx(Component c, float fx) {
        return c.getAbsoluteX() + Math.round((c.getWidth() - 1) * fx);
    }

    private static int cy(Component c, float fy) {
        return c.getAbsoluteY() + Math.round((c.getHeight() - 1) * fy);
    }

    private void drag(Form f, int x0, int y0, int x1, int y1) {
        // The harness resets this between tests.
        implementation.setHasDragStarted(true);
        f.pointerPressed(x0, y0);
        for (int i = 1; i <= 4; i++) {
            f.pointerDragged(x0 + (x1 - x0) * i / 4, y0 + (y1 - y0) * i / 4);
        }
        f.pointerReleased(x1, y1);
    }

    @FormTest
    void colorPickerSquareStripAndPresetTakeTaps() {
        ColorPicker p = new ColorPicker(0x808080);
        p.setPresets(0x10b981);
        Form f = shown(p);
        Component square = p.getComponentAt(0);
        Component strip = p.getComponentAt(1);

        f.pointerPressed(cx(strip, 0.5f), cy(strip, 0.5f));
        f.pointerReleased(cx(strip, 0.5f), cy(strip, 0.5f));
        assertEquals(180f, p.getHue(), 3f);

        f.pointerPressed(cx(square, 1f), cy(square, 0f));
        f.pointerReleased(cx(square, 1f), cy(square, 0f));
        float[] hsv = ColorPicker.rgbToHsv(p.getColor());
        assertEquals(1f, hsv[1], 0.02f);
        assertEquals(1f, hsv[2], 0.02f);

        int scroll = f.getContentPane().getScrollY();
        drag(f, cx(square, 0.5f), cy(square, 0.2f), cx(square, 0.5f), cy(square, 0.8f));
        assertEquals(0.2f, ColorPicker.rgbToHsv(p.getColor())[2], 0.05f);
        assertEquals(scroll, f.getContentPane().getScrollY());

        Component swatch = ((com.codename1.ui.Container) p.getComponentAt(3)).getComponentAt(0);
        f.pointerPressed(cx(swatch, 0.5f), cy(swatch, 0.5f));
        f.pointerReleased(cx(swatch, 0.5f), cy(swatch, 0.5f));
        assertEquals(0x10b981, p.getColor());
    }

    @FormTest
    void rangeSliderThumbDrags() {
        RangeSlider r = new RangeSlider(0, 100, 20, 80);
        Form f = shown(r);
        int y = cy(r, 0.5f);
        drag(f, r.getAbsoluteX() + r.positionOf(80), y, r.getAbsoluteX() + r.positionOf(50), y);
        assertEquals(20, r.getLowerValue());
        assertEquals(50, r.getUpperValue(), 2);
    }

    @FormTest
    void waveformSeeksOnTap() {
        AudioWaveform w = new AudioWaveform(new float[] {0.3f, 0.6f, 0.9f, 0.5f});
        Form f = shown(w);
        f.pointerPressed(cx(w, 0.75f), cy(w, 0.5f));
        f.pointerReleased(cx(w, 0.75f), cy(w, 0.5f));
        assertEquals(0.75f, w.getProgress(), 0.05f);
    }

    @FormTest
    void chipTapTogglesAndCloseIconRemoves() {
        final int[] removed = {0};
        Chip c = new Chip("Travel").setSelectable(true).setRemovable(true);
        c.addRemoveListener(e -> removed[0]++);
        Form f = shown(new com.codename1.ui.Container().add(c));
        Component text = c.getTextLabel();
        f.pointerPressed(cx(text, 0.3f), cy(text, 0.5f));
        f.pointerReleased(cx(text, 0.3f), cy(text, 0.5f));
        assertTrue(c.isSelected());
        assertEquals(0, removed[0]);
        Component close = c.getRemoveButton();
        f.pointerPressed(cx(close, 0.5f), cy(close, 0.5f));
        f.pointerReleased(cx(close, 0.5f), cy(close, 0.5f));
        assertEquals(1, removed[0]);
        assertTrue(c.isSelected());
    }

    @FormTest
    void mentionSuggestionTapCompletes() {
        MentionField m = new MentionField();
        m.setCandidates("Ada Lovelace", "Grace Hopper");
        Form f = shown(m);
        m.setText("hi @gr");
        f.revalidate();
        Component row = ((com.codename1.ui.Container) m.getComponentAt(1)).getComponentAt(0);
        f.pointerPressed(cx(row, 0.5f), cy(row, 0.5f));
        f.pointerReleased(cx(row, 0.5f), cy(row, 0.5f));
        assertEquals("hi @Grace Hopper ", m.getText());
    }

    @FormTest
    void emojiTapPicks() {
        EmojiPicker p = new EmojiPicker();
        p.setRecentsEnabled(false);
        Form f = shown(p);
        f.revalidate();
        com.codename1.ui.Container grid = (com.codename1.ui.Container) ((com.codename1.ui.layouts.BorderLayout) p.getLayout())
                .getCenter();
        Component first = grid.getComponentAt(0);
        f.pointerPressed(cx(first, 0.5f), cy(first, 0.5f));
        f.pointerReleased(cx(first, 0.5f), cy(first, 0.5f));
        assertEquals(EmojiPicker.getEmoji(0)[0], p.getSelectedEmoji());
    }
}
