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
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.plaf.Style;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The closed face of a `ComboBox`, as `DefaultLookAndFeel.drawComboBox` paints it.
class ComboBoxFacePaintTest extends UITestBase {

    private static final int ARROW = 10;

    /// A renderer that remembers how it was last asked to draw, and where it was put.
    private static final class RecordingRenderer extends Label implements ListCellRenderer<String> {
        private boolean selected;
        private int calls;

        @Override
        public Component getListCellRendererComponent(List list, String value, int index, boolean isSelected) {
            selected = isSelected;
            calls++;
            setText(value);
            return this;
        }

        @Override
        public Component getListFocusComponent(List list) {
            return null;
        }
    }

    private RecordingRenderer renderer;

    private ComboBox<String> comboOnForm(boolean rtl) {
        Form form = new Form("combo", new BorderLayout());
        ComboBox<String> combo = new ComboBox<String>("One", "Two", "Three");
        renderer = new RecordingRenderer();
        combo.setRenderer(renderer);
        combo.setComboBoxImage(Image.createImage(ARROW, ARROW));
        Style all = combo.getAllStyles();
        all.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        // Top and bottom deliberately nothing like left and right.
        all.setPadding(3, 3, 40, 7);
        form.add(BorderLayout.NORTH, combo);
        form.show();
        DisplayTest.flushEdt();
        // After the show: joining a form resets a component to the look and feel's direction.
        combo.setRTL(rtl);
        return combo;
    }

    private void paint(ComboBox<String> combo) {
        Form form = combo.getComponentForm();
        Image img = Image.createImage(form.getWidth(), form.getHeight());
        renderer.calls = 0;
        combo.paintComponent(img.getGraphics());
        assertTrue(renderer.calls > 0, "the face is drawn through the renderer");
    }

    /// The value used to be inset from the left edge by the TOP padding.
    @FormTest
    void theValueIsInsetByTheLeftPadding() {
        ComboBox<String> combo = comboOnForm(false);
        paint(combo);

        assertEquals(combo.getX() + 40, renderer.getX());
        assertEquals(combo.getY() + 3, renderer.getY());
        assertEquals(combo.getWidth() - 40 - 7 - ARROW, renderer.getWidth());
        assertEquals(combo.getHeight() - 6, renderer.getHeight());
    }

    /// Mirrored, the arrow is on the left and the value starts after it; the padding on
    /// that side is the one the style calls "right".
    @FormTest
    void theValueIsInsetByTheMirroredPaddingInRtl() {
        ComboBox<String> combo = comboOnForm(true);
        paint(combo);

        assertEquals(combo.getX() + 7 + ARROW, renderer.getX());
        assertEquals(combo.getWidth() - 40 - 7 - ARROW, renderer.getWidth());
        assertEquals(combo.getX() + combo.getWidth() - 40, renderer.getX() + renderer.getWidth(),
                "the value ends where the far padding starts");
    }
}
