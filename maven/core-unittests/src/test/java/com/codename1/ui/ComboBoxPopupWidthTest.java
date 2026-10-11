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
import com.codename1.ui.ComboBoxPopupTestSupport.PopupScript;
import com.codename1.ui.ComboBoxPopupTestSupport.ScriptedComboBox;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// How wide an open `ComboBox` popup is.
class ComboBoxPopupWidthTest extends UITestBase {

    private static final class Widths implements PopupScript {
        int popupWidth;
        int popupLeft;
        int sideGap;

        @Override
        public void run(Dialog popup, List<String> list) {
            // The dialog is placed by its margins, so the box inside them is the popup.
            Container box = popup.getDialogComponent();
            popupWidth = box.getWidth();
            popupLeft = box.getAbsoluteX();
            sideGap = list.getSideGap();
        }
    }

    private ScriptedComboBox comboOnForm(String... items) {
        Form form = new Form("combo", new BorderLayout());
        ScriptedComboBox combo = new ScriptedComboBox(items) {
            @Override
            protected List<String> createPopupList() {
                List<String> l = super.createPopupList();
                // Reserve a scrollbar gutter, which is what every desktop theme does and
                // what the extra width used to be.
                l.setIsScrollVisible(true);
                return l;
            }
        };
        Container holder = new Container(new FlowLayout());
        holder.add(combo);
        form.add(BorderLayout.NORTH, holder);
        form.show();
        DisplayTest.flushEdt();
        return combo;
    }

    /// The list's preferred width already contains its scrollbar gutter, so adding the
    /// gutter again made every popup one scrollbar wider than the combo it hangs off.
    @FormTest
    void aDesktopPopupIsAsWideAsItsCombo() throws Exception {
        implementation.setDesktop(true);
        ScriptedComboBox combo = comboOnForm("One", "Two", "Three");
        Widths w = new Widths();
        ComboBoxPopupTestSupport.open(combo, w);

        assertTrue(w.sideGap > 0, "the case needs a scrollbar gutter to mean anything");
        assertEquals(combo.getWidth(), w.popupWidth, "the popup lines up with both edges of the combo");
        assertEquals(combo.getAbsoluteX(), w.popupLeft);
    }

    @FormTest
    void aPopupWithWiderContentStillGrows() throws Exception {
        implementation.setDesktop(true);
        ScriptedComboBox combo = comboOnForm("One", "Two", "Three");
        combo.setPreferredW(40);
        combo.getComponentForm().revalidate();
        DisplayTest.flushEdt();
        Widths w = new Widths();
        ComboBoxPopupTestSupport.open(combo, w);

        assertTrue(combo.getWidth() <= 40);
        assertTrue(w.popupWidth > combo.getWidth(),
                "rows wider than the combo are not clipped to it");
    }
}
