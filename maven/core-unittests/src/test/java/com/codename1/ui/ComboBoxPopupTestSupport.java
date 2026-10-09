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

import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Drives an open `ComboBox` popup from a test.
///
/// The popup is modal: showing it parks the caller until it goes away. So the click
/// happens on a thread of its own, and everything the test wants to do to the open
/// popup runs from the popup's show listener, which is dispatch-thread work. Whatever
/// the script leaves open is disposed afterwards, because a caller left parked in
/// invokeAndBlock outlives the test.
final class ComboBoxPopupTestSupport {

    private ComboBoxPopupTestSupport() {
    }

    /// What a test does to the popup while it is up.
    interface PopupScript {
        void run(Dialog popup, List<String> list);
    }

    /// A combo box that runs a script against its popup as soon as it shows.
    static class ScriptedComboBox extends ComboBox<String> {
        private PopupScript script;
        private boolean shown;
        private Throwable failure;
        /// How often the combo's own action listeners heard a selection.
        int actionCount;

        ScriptedComboBox(String... items) {
            super((Object[]) items);
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    actionCount++;
                }
            });
        }

        @Override
        protected Dialog createPopupDialog(final List<String> l) {
            final Dialog d = super.createPopupDialog(l);
            d.addShowListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    shown = true;
                    try {
                        if (script != null) {
                            script.run(d, l);
                        }
                    } catch (Throwable t) {
                        failure = t;
                    } finally {
                        if (!d.isDisposed()) {
                            d.dispose();
                        }
                    }
                }
            });
            return d;
        }
    }

    static String[] rows(int count) {
        String[] items = new String[count];
        for (int i = 0; i < count; i++) {
            items[i] = "Item " + i;
        }
        return items;
    }

    /// Opens the popup, runs the script against it and waits for it to come down.
    static void open(final ScriptedComboBox combo, PopupScript script) throws Exception {
        combo.script = script;
        combo.shown = false;
        combo.failure = null;
        Thread caller = new Thread(new Runnable() {
            @Override
            public void run() {
                combo.fireClicked();
            }
        }, "cn1-test-combo-popup");
        caller.start();
        for (int i = 0; i < 400 && caller.isAlive(); i++) {
            DisplayTest.flushEdt();
            Thread.sleep(5);
        }
        caller.join(2000);
        DisplayTest.flushEdt();
        assertFalse(caller.isAlive(),
                "the popup has to come back down, or its caller is parked for good");
        assertTrue(combo.shown, "the popup never showed");
        if (combo.failure != null) {
            throw new AssertionError("the popup script failed", combo.failure);
        }
    }

    /// One full keystroke, delivered the way a port delivers it.
    static void type(Dialog popup, int keyCode) {
        popup.keyPressed(keyCode);
        popup.keyReleased(keyCode);
    }

    /// The middle of a row of the popup list, in screen coordinates.
    static int rowCenterY(List<String> list, int row) {
        int rowHeight = list.getElementSize(false, true).getHeight() + list.getItemGap();
        return list.getAbsoluteY() + list.getStyle().getPaddingTop() + rowHeight * row + rowHeight / 2;
    }

    static int centerX(List<String> list) {
        return list.getAbsoluteX() + list.getWidth() / 2;
    }
}
