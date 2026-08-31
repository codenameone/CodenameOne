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
import com.codename1.ui.EditField;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link OtpField} through its public API: construction guards, the
 * value, the boxes that display it, and the single field that owns it --
 * including the path a platform-offered code takes, which is the reason the
 * whole code lands in one field rather than one character per box.
 */
class OtpFieldTest extends UITestBase {

    // ---- construction / guards --------------------------------------

    @FormTest
    void defaultConstructorIsSixNumericBoxes() {
        OtpField f = new OtpField();
        assertEquals(6, f.getLength());
        assertTrue(f.isNumericOnly());
        assertEquals("OtpField", f.getUIID());
        assertEquals("OtpDigit", f.getBox(0).getUIID());
    }

    @FormTest
    void lengthConstructorHonoursLength() {
        OtpField f = new OtpField(4);
        assertEquals(4, f.getLength());
        for (int i = 0; i < 4; i++) {
            assertNotNull(f.getBox(i));
        }
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

    // ---- the hint that makes the platform offer the code -------------

    @FormTest
    void inputCarriesTheOneTimeCodeHint() {
        EditField input = new OtpField(6).getInputField();
        assertNotEquals(0, input.getConstraint() & TextArea.ONE_TIME_CODE,
                "without the hint no platform offers the code from the SMS");
        assertNotEquals(0, input.getConstraint() & TextArea.NUMERIC);
    }

    @FormTest
    void nonNumericFieldStillCarriesTheHint() {
        EditField input = new OtpField(6, false).getInputField();
        assertNotEquals(0, input.getConstraint() & TextArea.ONE_TIME_CODE);
        assertEquals(0, input.getConstraint() & TextArea.NUMERIC);
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
    void setTextNullClears() {
        OtpField f = new OtpField(4);
        f.setText("1234");
        f.setText(null);
        assertEquals("", f.getText());
    }

    @FormTest
    void setTextDropsCharactersTheFieldDoesNotAccept() {
        OtpField f = new OtpField(4);
        f.setText("1a2b3c4d");
        assertEquals("1234", f.getText());
    }

    @FormTest
    void clearEmptiesEveryBox() {
        OtpField f = new OtpField(6);
        f.setText("424242");
        f.clear();
        assertEquals("", f.getText());
        assertEquals("", f.getBox(0).getText());
        assertFalse(f.isComplete());
    }

    @FormTest
    void setTextLeavesTheCaretAfterTheLastCharacter() {
        OtpField f = new OtpField(6);
        f.setText("12");
        assertEquals(2, f.getInputField().getCaretOffset(),
                "typing continues after what was set, not in front of it");
    }

    // ---- typing, pasting, and a code the platform offers -------------

    @FormTest
    void typingOneDigitAtATimeFillsTheBoxesInOrder() {
        OtpField f = new OtpField(3);
        EditField input = f.getInputField();
        input.insertText("1");
        assertEquals("1", f.getBox(0).getText());
        assertEquals("", f.getBox(1).getText());
        input.insertText("2");
        input.insertText("3");
        assertEquals("123", f.getText());
        assertEquals("3", f.getBox(2).getText());
    }

    @FormTest
    void wholeCodeArrivingAtOnceFillsEveryBox() {
        // this is the platform offering the code out of the SMS, and it is also
        // a paste: both arrive as one commit of the whole value
        OtpField f = new OtpField(6);
        AtomicInteger fired = new AtomicInteger();
        f.addCompleteListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.incrementAndGet();
            }
        });
        f.getInputField().insertText("135790");
        assertEquals("135790", f.getText());
        assertEquals("1", f.getBox(0).getText());
        assertEquals("0", f.getBox(5).getText());
        assertEquals(1, fired.get());
    }

    @FormTest
    void charactersPastTheLastBoxAreDropped() {
        OtpField f = new OtpField(4);
        f.getInputField().insertText("123456789");
        assertEquals("1234", f.getText());
    }

    @FormTest
    void typingSkipsNonDigitsWhenNumeric() {
        OtpField f = new OtpField(4, true);
        f.getInputField().insertText("1a2b3c4d");
        assertEquals("1234", f.getText());
    }

    @FormTest
    void typingKeepsNonDigitsWhenNotNumeric() {
        OtpField f = new OtpField(4, false);
        f.getInputField().insertText("ab12");
        assertEquals("ab12", f.getText());
    }

    @FormTest
    void lineBreaksNeverEnterTheCode() {
        // a code pasted out of a message often carries the rest of the line
        OtpField f = new OtpField(4, false);
        f.getInputField().insertText("12\n34");
        assertEquals("1234", f.getText());
    }

    // ---- what the platform hands over -----------------------------------

    @FormTest
    void aCodeCommittedByThePlatformIsFilteredLikeATypedOne() {
        // The Android autofill path commits the whole value into the field. An
        // autofill service that keeps the message's separators hands over
        // something the user could never have typed, and a field left holding it
        // never reaches the length that completes it.
        OtpField f = new OtpField(6);
        AtomicInteger fired = new AtomicInteger();
        f.addCompleteListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.incrementAndGet();
            }
        });
        f.getInputField().commitText("123-456");
        assertEquals("123456", f.getText());
        assertTrue(f.isComplete());
        assertEquals(1, fired.get());
    }

    @FormTest
    void aCommittedCodeReplacesWhateverWasThereRatherThanAppending() {
        // the platform is answering "the value is this", so a half typed code is
        // replaced rather than prefixed onto the offer
        OtpField f = new OtpField(6);
        f.setText("99");
        EditField input = f.getInputField();
        input.setSelectionRange(0, input.getText().length());
        input.commitText("123456");
        assertEquals("123456", f.getText());
    }

    @FormTest
    void composedTextIsFilteredWhileItIsStillBeingComposed() {
        // Dictation, handwriting and an IME all build text as a composition before
        // committing it, and a composition writes to the document directly rather
        // than through the typed-text path. Unfiltered, a numeric code field would
        // hold letters for as long as the composition lasted.
        OtpField f = new OtpField(6);
        f.getInputField().setComposingText("12a3", 0);
        assertEquals("123", f.getText());
    }

    @FormTest
    void aCommitThatFinalizesACompositionIsFilteredToo() {
        // the commit that ends a composition replaces the composed range directly,
        // which is the one commit that never reaches the typed-text hook
        OtpField f = new OtpField(6);
        EditField input = f.getInputField();
        input.setComposingText("12", 0);
        input.commitText("12b345");
        assertEquals("12345", f.getText());
    }

    @FormTest
    void composedTextCannotOverfillTheField() {
        OtpField f = new OtpField(4);
        f.getInputField().setComposingText("123456789", 0);
        assertEquals("1234", f.getText());
    }

    // ---- tapping a box --------------------------------------------------

    @FormTest
    void tappingABoxPutsTheCaretInThatBox() {
        // The inherited hit test measures the field's own text layout, which sits at
        // the field's left edge and is never painted. A tap has to answer with the box
        // the user aimed at, or a correction lands on the wrong digit.
        OtpField f = new OtpField(6);
        Form form = new Form("t", BoxLayout.y());
        form.add(f);
        form.show();
        form.revalidate();
        f.setText("123456");
        form.revalidate();

        EditField input = f.getInputField();
        for (int i = 0; i < 6; i++) {
            TextField box = f.getBox(i);
            int x = box.getAbsoluteX() + box.getWidth() / 2;
            int y = box.getAbsoluteY() + box.getHeight() / 2;
            assertEquals(i, input.offsetAtPoint(x, y), "tap on box " + i);
        }
        TextField last = f.getBox(5);
        assertEquals(6, input.offsetAtPoint(last.getAbsoluteX() + last.getWidth() + 40,
                last.getAbsoluteY() + 1), "a tap past the last box means the end");
    }

    @FormTest
    void tappingAnEmptyBoxMeansTheEndOfWhatWasEntered() {
        // the caller assigns this offset to the caret without clamping it, so an
        // offset past the text would put the caret outside the document
        OtpField f = new OtpField(6);
        Form form = new Form("t", BoxLayout.y());
        form.add(f);
        form.show();
        form.revalidate();
        f.setText("12");
        form.revalidate();

        TextField box = f.getBox(5);
        assertEquals(2, f.getInputField().offsetAtPoint(box.getAbsoluteX() + 1,
                box.getAbsoluteY() + 1));
    }

    // ---- completion ---------------------------------------------------

    @FormTest
    void completionFiresOnceWhenTheLastBoxFills() {
        OtpField f = new OtpField(3);
        AtomicInteger fired = new AtomicInteger();
        f.addCompleteListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.incrementAndGet();
            }
        });
        EditField input = f.getInputField();
        input.insertText("1");
        input.insertText("2");
        assertEquals(0, fired.get(), "must not fire until the last box is filled");
        input.insertText("3");
        assertEquals(1, fired.get());
        assertTrue(f.isComplete());
    }

    @FormTest
    void completionFiresAgainAfterTheCodeIsCorrected() {
        OtpField f = new OtpField(3);
        AtomicInteger fired = new AtomicInteger();
        f.addCompleteListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.incrementAndGet();
            }
        });
        f.setText("123");
        assertEquals(1, fired.get());
        f.setText("12");
        assertFalse(f.isComplete());
        assertEquals(1, fired.get());
        f.setText("124");
        assertEquals(2, fired.get(), "a corrected code is a new attempt");
    }

    @FormTest
    void removedListenerStopsFiring() {
        OtpField f = new OtpField(3);
        AtomicInteger fired = new AtomicInteger();
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.incrementAndGet();
            }
        };
        f.addCompleteListener(l);
        f.removeCompleteListener(l);
        f.setText("123");
        assertEquals(0, fired.get());
    }
}
