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
import com.codename1.ui.plaf.UIManager;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The keyboard in an open `ComboBox` popup on a desktop: arrows move the highlight,
/// Enter takes it, Escape puts everything back.
///
/// The test implementation maps a key code to the game action of the same value, so the
/// arrows and Enter are sent as their `Display.GAME_*` constants. Escape is the key code
/// 27 on every port. Only Down is used to move: the same implementation answers 1 for its
/// right soft key, which is also `Display.GAME_UP`, so an Up sent here would be a soft key.
class ComboBoxPopupKeyboardTest extends UITestBase {

    private static final int ESCAPE = 27;

    private ScriptedComboBox comboOnForm(int rows) {
        Form form = new Form("combo", new BorderLayout());
        ScriptedComboBox combo = new ScriptedComboBox(ComboBoxPopupTestSupport.rows(rows));
        form.add(BorderLayout.NORTH, combo);
        form.show();
        DisplayTest.flushEdt();
        return combo;
    }

    private void theme(String constant) {
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("@" + constant, "true");
        UIManager.getInstance().setThemeProps(theme);
    }

    @FormTest
    void arrowsMoveTheHighlightFromTheMomentThePopupOpens() throws Exception {
        implementation.setDesktop(true);
        ScriptedComboBox combo = comboOnForm(5);
        final int[] seen = {-1, -1};
        final boolean[] open = {false};
        ComboBoxPopupTestSupport.open(combo, new PopupScript() {
            @Override
            public void run(Dialog popup, List<String> list) {
                ComboBoxPopupTestSupport.type(popup, Display.GAME_DOWN);
                ComboBoxPopupTestSupport.type(popup, Display.GAME_DOWN);
                seen[0] = list.getSelectedIndex();
                ComboBoxPopupTestSupport.type(popup, Display.GAME_DOWN);
                seen[1] = list.getSelectedIndex();
                open[0] = !popup.isDisposed();
            }
        });
        assertEquals(2, seen[0], "two presses of Down are two rows");
        assertEquals(3, seen[1], "and a third is a third");
        assertTrue(open[0], "moving the highlight does not close the popup");
        assertEquals(0, combo.getSelectedIndex(), "nor choose anything");
    }

    @FormTest
    void enterChoosesTheHighlightedRow() throws Exception {
        implementation.setDesktop(true);
        ScriptedComboBox combo = comboOnForm(5);
        final boolean[] closed = {false};
        ComboBoxPopupTestSupport.open(combo, new PopupScript() {
            @Override
            public void run(Dialog popup, List<String> list) {
                ComboBoxPopupTestSupport.type(popup, Display.GAME_DOWN);
                ComboBoxPopupTestSupport.type(popup, Display.GAME_DOWN);
                ComboBoxPopupTestSupport.type(popup, Display.GAME_FIRE);
                closed[0] = popup.isDisposed();
            }
        });
        assertTrue(closed[0], "Enter closes the popup");
        assertEquals(2, combo.getSelectedIndex(), "on the row the arrows reached");
        assertEquals(1, combo.actionCount);
    }

    /// A popup with a Cancel button in its body has two things to focus, which is where
    /// the list used to hand the keyboard away: Left or Right dropped it out of input
    /// mode, the arrows after that moved nothing, and Enter only switched input back on.
    @FormTest
    void theListKeepsTheKeyboardWhenThePopupHasButtons() throws Exception {
        implementation.setDesktop(true);
        theme("popupCancelBodyBool");
        ScriptedComboBox combo = comboOnForm(5);
        final int[] seen = {-1};
        final boolean[] closed = {false};
        ComboBoxPopupTestSupport.open(combo, new PopupScript() {
            @Override
            public void run(Dialog popup, List<String> list) {
                ComboBoxPopupTestSupport.type(popup, Display.GAME_RIGHT);
                ComboBoxPopupTestSupport.type(popup, Display.GAME_DOWN);
                ComboBoxPopupTestSupport.type(popup, Display.GAME_DOWN);
                ComboBoxPopupTestSupport.type(popup, Display.GAME_DOWN);
                seen[0] = list.getSelectedIndex();
                ComboBoxPopupTestSupport.type(popup, Display.GAME_FIRE);
                closed[0] = popup.isDisposed();
            }
        });
        assertEquals(3, seen[0]);
        assertTrue(closed[0]);
        assertEquals(3, combo.getSelectedIndex());
    }

    @FormTest
    void escapeCancelsAndRestoresTheOldValue() throws Exception {
        implementation.setDesktop(true);
        ScriptedComboBox combo = comboOnForm(5);
        combo.setSelectedIndex(1);
        final boolean[] closed = {false};
        ComboBoxPopupTestSupport.open(combo, new PopupScript() {
            @Override
            public void run(Dialog popup, List<String> list) {
                ComboBoxPopupTestSupport.type(popup, Display.GAME_DOWN);
                ComboBoxPopupTestSupport.type(popup, Display.GAME_DOWN);
                ComboBoxPopupTestSupport.type(popup, ESCAPE);
                closed[0] = popup.isDisposed();
            }
        });
        assertTrue(closed[0], "Escape closes the popup");
        assertEquals(1, combo.getSelectedIndex(), "and the highlight it moved is forgotten");
        assertEquals(0, combo.actionCount);
    }

    /// The same, in the two arrangements that used to install no back command at all:
    /// Select and Cancel switched off, with a centred popup and with a spinner one.
    @FormTest
    void escapeCancelsWithoutSelectAndCancelCommands() throws Exception {
        implementation.setDesktop(true);
        theme("centeredPopupBool");
        ScriptedComboBox combo = comboOnForm(5);
        combo.setIncludeSelectCancel(false);
        combo.setSelectedIndex(1);
        final boolean[] closed = {false};
        final boolean[] hadBack = {false};
        ComboBoxPopupTestSupport.open(combo, new PopupScript() {
            @Override
            public void run(Dialog popup, List<String> list) {
                hadBack[0] = popup.getBackCommand() != null;
                ComboBoxPopupTestSupport.type(popup, Display.GAME_DOWN);
                ComboBoxPopupTestSupport.type(popup, Display.GAME_DOWN);
                ComboBoxPopupTestSupport.type(popup, ESCAPE);
                closed[0] = popup.isDisposed();
            }
        });
        assertTrue(hadBack[0], "a desktop popup can always be backed out of");
        assertTrue(closed[0]);
        assertEquals(1, combo.getSelectedIndex());
        assertEquals(0, combo.actionCount);
    }
}
