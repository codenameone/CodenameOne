/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link OtpField} through its public API: construction guards,
 * value get/set, box structure, the auto-advance / backspace editing logic,
 * paste distribution and the completion-listener firing. All driven on the
 * EDT (via {@link FormTest}) since the boxes call {@code startEditingAsync}.
 */
class OtpFieldTest extends UITestBase {

    // ---- construction / guards --------------------------------------

    @FormTest
    void defaultConstructorIsSixNumericBoxes() {
        OtpField f = new OtpField();
        assertEquals(6, f.getLength());
        assertEquals(6, f.getComponentCount());
        assertEquals("OtpField", f.getUIID());
        assertEquals("OtpDigit", f.getBox(0).getUIID());
    }

    @FormTest
    void lengthConstructorHonoursLength() {
        OtpField f = new OtpField(4);
        assertEquals(4, f.getLength());
        assertEquals(4, f.getComponentCount());
        for (int i = 0; i < 4; i++) {
            assertNotNull(f.getBox(i));
            assertSame(f.getBox(i), f.getComponentAt(i));
        }
    }

    @FormTest
    void numericConstructorAppliesNumericConstraint() {
        OtpField numeric = new OtpField(4, true);
        assertEquals(TextField.NUMERIC, numeric.getBox(0).getConstraint());

        OtpField anyChar = new OtpField(4, false);
        assertEquals(0, anyChar.getBox(0).getConstraint());
    }

    @FormTest
    void constructorRejectsTooShortLength() {
        assertThrows(IllegalArgumentException.class, () -> new OtpField(1));
    }

    @FormTest
    void constructorRejectsTooLongLength() {
        assertThrows(IllegalArgumentException.class, () -> new OtpField(17));
    }

    @FormTest
    void boundaryLengthsAreAccepted() {
        assertEquals(2, new OtpField(2).getLength());
        assertEquals(16, new OtpField(16).getLength());
    }

    // ---- value get / set --------------------------------------------

    @FormTest
    void setTextDistributesOneCharPerBox() {
        OtpField f = new OtpField(6);
        f.setText("123456");
        assertEquals("123456", f.getText());
        assertEquals("1", f.getBox(0).getText());
        assertEquals("6", f.getBox(5).getText());
    }

    @FormTest
    void setTextDropsExcessCharacters() {
        OtpField f = new OtpField(4);
        f.setText("123456789");
        assertEquals("1234", f.getText());
    }

    @FormTest
    void setTextShorterLeavesTrailingBoxesEmpty() {
        OtpField f = new OtpField(6);
        f.setText("12");
        assertEquals("12", f.getText());
        assertEquals("", f.getBox(2).getText());
        assertEquals("", f.getBox(5).getText());
    }

    @FormTest
    void setTextNullClearsAllBoxes() {
        OtpField f = new OtpField(4);
        f.setText("1234");
        f.setText(null);
        assertEquals("", f.getText());
    }

    @FormTest
    void getTextOmitsEmptyBoxesForPartialEntry() {
        OtpField f = new OtpField(6);
        f.getBox(0).setText("9");
        f.getBox(2).setText("7");
        // boxes 1, 3, 4, 5 left empty -> concatenation skips them
        assertEquals("97", f.getText());
    }

    @FormTest
    void clearEmptiesEveryBox() {
        OtpField f = new OtpField(6);
        f.setText("424242");
        f.clear();
        assertEquals("", f.getText());
    }

    // ---- editing behaviour (data-changed driven) --------------------

    @FormTest
    void typingSingleCharAdvancesAndFinalKeyCompletes() {
        OtpField f = new OtpField(3);
        AtomicInteger fired = new AtomicInteger();
        f.addCompleteListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.incrementAndGet();
            }
        });
        // Type one digit at a time; each setText triggers the DataChangedListener.
        f.getBox(0).setText("1");
        f.getBox(1).setText("2");
        assertEquals(0, fired.get(), "must not fire until the last box is filled");
        f.getBox(2).setText("3");
        assertEquals("123", f.getText());
        assertEquals(1, fired.get(), "completion fires exactly once when the field is full");
    }

    @FormTest
    void backspaceOnEmptyBoxDoesNotFireOrThrow() {
        OtpField f = new OtpField(3);
        AtomicInteger fired = new AtomicInteger();
        f.addCompleteListener(evt -> fired.incrementAndGet());
        f.getBox(2).setText("3");
        // emptying a box (backspace) steps back; must not complete
        f.getBox(2).setText("");
        assertEquals(0, fired.get());
        assertEquals("", f.getText());
    }

    // ---- paste distribution -----------------------------------------

    @FormTest
    void pasteIntoFirstBoxSpreadsAcrossBoxesAndCompletes() {
        OtpField f = new OtpField(6);
        AtomicInteger fired = new AtomicInteger();
        f.addCompleteListener(evt -> fired.incrementAndGet());
        // simulate a paste of the whole code into the first box
        f.getBox(0).setText("135790");
        assertEquals("135790", f.getText());
        assertEquals("1", f.getBox(0).getText());
        assertEquals("0", f.getBox(5).getText());
        assertEquals(1, fired.get());
    }

    @FormTest
    void pasteSkipsNonDigitsWhenNumeric() {
        OtpField f = new OtpField(4, true);
        f.getBox(0).setText("1a2b3c4d");
        // non-digit chars are skipped, leaving the four digits
        assertEquals("1234", f.getText());
    }

    @FormTest
    void pasteKeepsNonDigitsWhenNotNumeric() {
        OtpField f = new OtpField(4, false);
        f.getBox(0).setText("ab12");
        assertEquals("ab12", f.getText());
    }

    @FormTest
    void pasteStartingMidFieldOnlyFillsFromThatIndex() {
        OtpField f = new OtpField(6);
        f.getBox(2).setText("789");
        assertEquals("", f.getBox(0).getText());
        assertEquals("", f.getBox(1).getText());
        assertEquals("7", f.getBox(2).getText());
        assertEquals("9", f.getBox(4).getText());
        assertEquals("789", f.getText());
    }

    // ---- listener management ----------------------------------------

    @FormTest
    void removedListenerIsNotInvoked() {
        OtpField f = new OtpField(2);
        AtomicInteger fired = new AtomicInteger();
        ActionListener l = evt -> fired.incrementAndGet();
        f.addCompleteListener(l);
        f.removeCompleteListener(l);
        f.getBox(0).setText("1");
        f.getBox(1).setText("2");
        assertEquals(0, fired.get());
    }

    @FormTest
    void nullListenerIsIgnored() {
        OtpField f = new OtpField(2);
        f.addCompleteListener(null);
        // no NPE when the field fills
        f.setText("12");
        f.getBox(1).setText("3"); // re-trigger a change on the last box
        assertEquals("13", f.getText());
    }

    @FormTest
    void consumingListenerStopsLaterListeners() {
        OtpField f = new OtpField(2);
        AtomicInteger second = new AtomicInteger();
        f.addCompleteListener(ActionEvent::consume);
        f.addCompleteListener(evt -> second.incrementAndGet());
        f.getBox(0).setText("1");
        f.getBox(1).setText("2");
        assertEquals(0, second.get(), "second listener skipped once the event is consumed");
    }
}
