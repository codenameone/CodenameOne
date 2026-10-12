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
import com.codename1.ui.plaf.UIManager;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Where an open `ComboBox` popup lands on a desktop, and how big it is.
class ComboBoxPopupGeometryTest extends UITestBase {

    /// What the popup looked like while it was up.
    private static final class Shot implements PopupScript {
        int listHeight;
        int rowHeight;
        int scrollY;
        boolean scrollable;
        int popupTop;
        int popupBottom;
        int popupWidth;
        int selectedRowY;

        @Override
        public void run(Dialog popup, List<String> list) {
            Container box = popup.getDialogComponent();
            listHeight = list.getHeight();
            rowHeight = list.getElementSize(false, true).getHeight() + list.getItemGap();
            scrollY = list.getScrollY();
            scrollable = list.isScrollableY();
            popupTop = box.getAbsoluteY();
            popupBottom = popupTop + box.getHeight();
            popupWidth = box.getWidth();
            selectedRowY = ComboBoxPopupTestSupport.rowCenterY(list, list.getSelectedIndex());
        }
    }

    private ScriptedComboBox comboOnForm(String where, int rows) {
        Form form = new Form("combo", new BorderLayout());
        ScriptedComboBox combo = new ScriptedComboBox(ComboBoxPopupTestSupport.rows(rows));
        // A flow layout, so the combo is as wide as it asks to be rather than as wide as
        // the form: the width of the popup is only a question when there is room either way.
        Container holder = new Container(new FlowLayout());
        holder.add(combo);
        form.add(where, holder);
        form.show();
        DisplayTest.flushEdt();
        return combo;
    }

    private Shot open(ScriptedComboBox combo) throws Exception {
        Shot shot = new Shot();
        ComboBoxPopupTestSupport.open(combo, shot);
        return shot;
    }

    /// Sixty rows used to make a popup sixty rows tall: it filled the window from the top
    /// and covered the combo it belonged to.
    @FormTest
    void aLongListIsCappedAndDropsBelowTheCombo() throws Exception {
        implementation.setDesktop(true);
        ScriptedComboBox combo = comboOnForm(BorderLayout.NORTH, 60);
        Shot shot = open(combo);

        assertTrue(shot.listHeight <= shot.rowHeight * 10 + shot.rowHeight / 2,
                "ten rows at most, was " + shot.listHeight + " at " + shot.rowHeight + " a row");
        assertTrue(shot.listHeight >= shot.rowHeight * 9, "and not fewer when there is room");
        assertTrue(shot.popupTop >= combo.getAbsoluteY() + combo.getHeight(),
                "the popup starts under the combo instead of covering it");
        assertTrue(shot.scrollable, "the rows that do not fit are reached by scrolling");
    }

    @FormTest
    void aComboNearTheBottomOpensUpwards() throws Exception {
        implementation.setDesktop(true);
        ScriptedComboBox combo = comboOnForm(BorderLayout.SOUTH, 60);
        Shot shot = open(combo);

        assertTrue(shot.listHeight <= shot.rowHeight * 10 + shot.rowHeight / 2);
        assertTrue(shot.popupBottom <= combo.getAbsoluteY(),
                "no room below, so the popup ends where the combo starts");
        assertTrue(shot.popupTop >= 0);
        assertTrue(shot.scrollable);
    }

    @FormTest
    void theCappedPopupOpensScrolledToTheCurrentValue() throws Exception {
        implementation.setDesktop(true);
        ScriptedComboBox combo = comboOnForm(BorderLayout.NORTH, 60);
        combo.setSelectedIndex(40);
        Shot shot = open(combo);

        assertTrue(shot.scrollY > 0, "row 40 is far below the first ten");
        assertTrue(shot.selectedRowY > shot.popupTop && shot.selectedRowY < shot.popupBottom,
                "and it is the one on screen");
        assertEquals(40, combo.getSelectedIndex());
    }

    @FormTest
    void theRowCapIsAThemeConstant() throws Exception {
        implementation.setDesktop(true);
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("@comboPopupMaxRowsInt", "4");
        UIManager.getInstance().setThemeProps(theme);
        ScriptedComboBox combo = comboOnForm(BorderLayout.NORTH, 60);
        Shot shot = open(combo);

        assertTrue(shot.listHeight <= shot.rowHeight * 4 + shot.rowHeight / 2,
                "four rows asked for, was " + shot.listHeight + " at " + shot.rowHeight + " a row");
        assertTrue(shot.listHeight >= shot.rowHeight * 3);
        assertTrue(shot.scrollable);
    }

    @FormTest
    void aShortListIsNotStretchedToTheCap() throws Exception {
        implementation.setDesktop(true);
        ScriptedComboBox combo = comboOnForm(BorderLayout.NORTH, 3);
        Shot shot = open(combo);

        assertTrue(shot.listHeight <= shot.rowHeight * 3 + shot.rowHeight / 2);
        assertFalse(shot.scrollable);
    }

    /// The cap is a desktop convention. A touch popup keeps the size it always had.
    @FormTest
    void aTouchPopupIsNotCapped() throws Exception {
        implementation.setDesktop(false);
        ScriptedComboBox combo = comboOnForm(BorderLayout.NORTH, 30);
        Shot shot = open(combo);

        assertTrue(shot.listHeight > shot.rowHeight * 12,
                "uncapped, was " + shot.listHeight + " at " + shot.rowHeight + " a row");
    }
}
