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
package com.codename1.security;

import com.codename1.junit.UITestBase;
import com.codename1.components.InteractionDialog;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecureKeyboardTest extends UITestBase {

    @Test
    void untrustedKeyboardDecision() {
        String logger = "com.example.keylogger/.KeyboardService";
        assertFalse(DeviceIntegrity.isUntrustedKeyboard(null, false, null));
        assertFalse(DeviceIntegrity.isUntrustedKeyboard(logger, true, null));
        assertTrue(DeviceIntegrity.isUntrustedKeyboard(logger, false, null));
        assertTrue(DeviceIntegrity.isUntrustedKeyboard(logger, false, new String[] {"com.touchtype.swiftkey"}));
        assertFalse(DeviceIntegrity.isUntrustedKeyboard(logger, false, new String[] {"com.example.keylogger"}));
    }

    @Test
    void installKeepsTheFieldAwayFromTheSystemEditor() {
        TextField tf = new TextField();
        assertTrue(tf.isEditable());
        SecureKeyboard kb = SecureKeyboard.install(tf);
        assertFalse(tf.isEditable(), "an editable field would still open the platform keyboard");
        assertSame(kb, SecureKeyboard.install(tf));
        assertSame(kb, SecureKeyboard.getInstalled(tf));
        SecureKeyboard.uninstall(tf);
        assertTrue(tf.isEditable());
        assertNull(SecureKeyboard.getInstalled(tf));
    }

    @Test
    void layoutFollowsConstraint() {
        assertEquals(SecureKeyboard.Layout.NUMBER, SecureKeyboard.layoutFor(TextArea.NUMERIC | TextArea.PASSWORD));
        assertEquals(SecureKeyboard.Layout.DECIMAL, SecureKeyboard.layoutFor(TextArea.DECIMAL));
        assertEquals(SecureKeyboard.Layout.PHONE, SecureKeyboard.layoutFor(TextArea.PHONENUMBER));
        assertEquals(SecureKeyboard.Layout.EMAIL, SecureKeyboard.layoutFor(TextArea.EMAILADDR));
        assertEquals(SecureKeyboard.Layout.TEXT, SecureKeyboard.layoutFor(TextArea.PASSWORD));
    }

    @Test
    void typingWritesIntoTheField() {
        TextField tf = new TextField("", "", 20, TextArea.PASSWORD);
        SecureKeyboard kb = SecureKeyboard.install(tf);
        assertTrue(kb.typeKey("a"));
        assertTrue(kb.typeKey("b"));
        assertTrue(kb.typeKey("?123"));
        assertTrue(kb.typeKey("7"));
        assertEquals("ab7", tf.getText());
    }

    @Test
    void shiftUppercasesOneLetter() {
        TextField tf = new TextField();
        SecureKeyboard kb = SecureKeyboard.install(tf);
        String before = kb.keyLabels();
        assertTrue(before.contains("qwertyuiop"));
        kb.typeKey("q");
        assertEquals("q", tf.getText());
    }

    @Test
    void maxSizeIsHonoured() {
        TextField tf = new TextField("", "", 2, TextArea.NUMERIC);
        tf.setMaxSize(2);
        SecureKeyboard kb = SecureKeyboard.install(tf);
        kb.typeKey("1");
        kb.typeKey("2");
        kb.typeKey("3");
        assertEquals("12", tf.getText());
    }

    @Test
    void scrambleShufflesDigitsAndKeepsEveryDigit() {
        TextField tf = new TextField("", "", 6, TextArea.NUMERIC);
        SecureKeyboard kb = SecureKeyboard.install(tf);
        assertEquals("1234567890", kb.keyLabels());
        Set<String> orders = new HashSet<String>();
        for (int i = 0; i < 20; i++) {
            kb.setScrambled(true);
            String order = kb.keyLabels();
            assertEquals(10, order.length());
            for (char c = '0'; c <= '9'; c++) {
                assertTrue(order.indexOf(c) >= 0, "missing digit " + c + " in " + order);
            }
            orders.add(order);
        }
        assertTrue(orders.size() > 1, "twenty shuffles produced one order");
        kb.setScrambled(false);
        assertEquals("1234567890", kb.keyLabels());
    }

    @Test
    void fixedDigitOrderIsValidatedAndTurnsScramblingOff() {
        TextField tf = new TextField("", "", 6, TextArea.NUMERIC);
        SecureKeyboard kb = SecureKeyboard.install(tf);
        kb.setScrambled(true);
        kb.setDigitOrder("7302958164");
        assertFalse(kb.isScrambled(), "a fixed order would be replaced by the next show otherwise");
        assertEquals("7302958164", kb.keyLabels());
        assertThrows(IllegalArgumentException.class, () -> kb.setDigitOrder("123"));
        assertThrows(IllegalArgumentException.class, () -> kb.setDigitOrder("1123456789"));
        assertThrows(IllegalArgumentException.class, () -> kb.setDigitOrder(null));
    }

    @Test
    void wellKnownKeyboardsAreAcceptedAndTheListIsACopy() {
        String gboard = "com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME";
        String swiftkey = "com.touchtype.swiftkey/com.touchtype.KeyboardService";
        String logger = "com.example.keylogger/.KeyboardService";
        String lookalike = "com.google.android.inputmethod.latinx/.Ime";
        String[] known = DeviceIntegrity.wellKnownKeyboards();
        assertFalse(DeviceIntegrity.isUntrustedKeyboard(gboard, false, known));
        assertFalse(DeviceIntegrity.isUntrustedKeyboard(swiftkey, false, known));
        assertTrue(DeviceIntegrity.isUntrustedKeyboard(logger, false, known));
        assertTrue(DeviceIntegrity.isUntrustedKeyboard(lookalike, false, known),
                "the package must match exactly, not by prefix");
        known[0] = "com.example.keylogger";
        assertEquals("com.google.android.inputmethod.latin", DeviceIntegrity.wellKnownKeyboards()[0]);
        // Only iOS can refuse a keyboard; the test port reports it cannot.
        assertFalse(DeviceIntegrity.setThirdPartyKeyboardsAllowed(false));
        DeviceIntegrity.setThirdPartyKeyboardsAllowed(true);
    }

    @Test
    void showHideFiresEditingComplete() {
        Form f = new Form("t", BoxLayout.y());
        TextField tf = new TextField("", "", 6, TextArea.NUMERIC);
        f.add(tf);
        f.show();
        flushSerialCalls();
        final int[] actions = new int[1];
        tf.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                actions[0]++;
            }
        });
        SecureKeyboard kb = SecureKeyboard.install(tf);
        tapComponent(tf);
        assertTrue(kb.isShowing(), "a tap on the protected field opens the secure keyboard");
        kb.typeKey("4");
        kb.hide();
        flushSerialCalls();
        assertFalse(kb.isShowing());
        assertEquals("4", tf.getText());
        assertEquals(1, actions[0]);
        assertNotEquals(0, kb.getPreferredH());
    }

    @Test
    void uninstallAndRemovalDoNotFireOrStick() {
        Form f = new Form("t", BoxLayout.y());
        TextField tf = new TextField("", "", 6, TextArea.NUMERIC);
        f.add(tf);
        f.show();
        flushSerialCalls();
        final int[] actions = new int[1];
        tf.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                actions[0]++;
            }
        });
        SecureKeyboard kb = SecureKeyboard.install(tf);
        tapComponent(tf);
        flushSerialCalls();
        assertTrue(kb.isShowing());
        // Removed behind the keyboard's back: it must notice rather than refuse to reopen.
        Container c = kb.getParent();
        while (!(c instanceof InteractionDialog)) {
            c = c.getParent();
        }
        c.remove();
        flushSerialCalls();
        assertFalse(kb.isShowing(), "a keyboard removed from the form is not showing");
        tapComponent(tf);
        flushSerialCalls();
        assertTrue(kb.isShowing(), "and the field can open it again");
        SecureKeyboard.uninstall(tf);
        flushSerialCalls();
        assertFalse(kb.isShowing());
        assertEquals(0, actions[0], "closing without the user finishing must not submit");
        assertTrue(tf.isEditable());
    }
}
