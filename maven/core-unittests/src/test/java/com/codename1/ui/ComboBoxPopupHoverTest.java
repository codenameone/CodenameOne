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

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The row under the mouse is the highlighted row of an open `ComboBox` popup.
class ComboBoxPopupHoverTest extends UITestBase {

    private ScriptedComboBox comboOnForm(int rows) {
        Form form = new Form("combo", new BorderLayout());
        ScriptedComboBox combo = new ScriptedComboBox(ComboBoxPopupTestSupport.rows(rows));
        form.add(BorderLayout.NORTH, combo);
        form.show();
        DisplayTest.flushEdt();
        return combo;
    }

    private static void hover(Dialog popup, List<String> list, int row) {
        popup.pointerHover(new int[]{ComboBoxPopupTestSupport.centerX(list)},
                new int[]{ComboBoxPopupTestSupport.rowCenterY(list, row)});
    }

    @FormTest
    void theRowUnderThePointerIsHighlighted() throws Exception {
        ScriptedComboBox combo = comboOnForm(5);
        final int[] seen = {-1, -1};
        ComboBoxPopupTestSupport.open(combo, new PopupScript() {
            @Override
            public void run(Dialog popup, List<String> list) {
                hover(popup, list, 3);
                seen[0] = list.getSelectedIndex();
                hover(popup, list, 1);
                seen[1] = list.getSelectedIndex();
            }
        });
        assertEquals(3, seen[0], "hovering a row highlights it");
        assertEquals(1, seen[1], "and the highlight follows the pointer");
    }

    /// The highlight is the shared selection, so a popup that goes away without a choice
    /// has to put back what was selected before it opened -- however it goes away.
    @FormTest
    void aHoverIsNotAChoice() throws Exception {
        ScriptedComboBox combo = comboOnForm(5);
        combo.setSelectedIndex(2);
        ComboBoxPopupTestSupport.open(combo, new PopupScript() {
            @Override
            public void run(Dialog popup, List<String> list) {
                hover(popup, list, 4);
                // A bare dispose: no cancel command, no press outside, no rotation.
                popup.dispose();
            }
        });
        assertEquals(2, combo.getSelectedIndex(), "closing without choosing keeps the old value");
        assertEquals(0, combo.actionCount, "and tells nobody a choice was made");
    }

    @FormTest
    void clickingTheHoveredRowChoosesIt() throws Exception {
        ScriptedComboBox combo = comboOnForm(5);
        ComboBoxPopupTestSupport.open(combo, new PopupScript() {
            @Override
            public void run(Dialog popup, List<String> list) {
                hover(popup, list, 3);
                int x = ComboBoxPopupTestSupport.centerX(list);
                int y = ComboBoxPopupTestSupport.rowCenterY(list, 3);
                popup.pointerPressed(x, y);
                popup.pointerReleased(x, y);
            }
        });
        assertEquals(3, combo.getSelectedIndex(), "a click on the highlighted row is the choice");
        assertEquals(1, combo.actionCount);
    }

    /// Only the popup follows the pointer. In a list on a form the selection is state the
    /// application reads and listens to, and passing the mouse over it must not change it.
    @FormTest
    void aPlainListDoesNotSelectOnHover() {
        Form form = new Form("list", new BorderLayout());
        List<String> list = new List<String>(ComboBoxPopupTestSupport.rows(5));
        form.add(BorderLayout.CENTER, list);
        form.show();
        DisplayTest.flushEdt();

        form.pointerHover(new int[]{ComboBoxPopupTestSupport.centerX(list)},
                new int[]{ComboBoxPopupTestSupport.rowCenterY(list, 3)});
        assertEquals(0, list.getSelectedIndex());
    }
}
