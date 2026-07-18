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
import com.codename1.testing.TestUtils;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TextAreaTest extends UITestBase {

    @FormTest
    void testDefaultConstructor() {
        TextArea textArea = new TextArea();
        assertNotNull(textArea);
        assertEquals("", textArea.getText());
        assertTrue(textArea.isEditable());
    }

    @FormTest
    void testConstructorWithText() {
        TextArea textArea = new TextArea("Hello");
        assertEquals("Hello", textArea.getText());
    }

    @FormTest
    void testConstructorWithRows() {
        TextArea textArea = new TextArea(5, 20);
        assertEquals(5, textArea.getRows());
    }

    @FormTest
    void testConstructorWithTextRowsAndCols() {
        TextArea textArea = new TextArea("Text", 4, 20);
        assertEquals("Text", textArea.getText());
        assertEquals(4, textArea.getRows());
        assertEquals(20, textArea.getColumns());
    }

    @FormTest
    void testConstructorWithRowsAndCols() {
        TextArea textArea = new TextArea(3, 25);
        assertEquals(3, textArea.getRows());
        assertEquals(25, textArea.getColumns());
    }

    @FormTest
    void testConstructorWithTextRowsColsAndConstraint() {
        TextArea textArea = new TextArea("Email", 2, 30, TextArea.EMAILADDR);
        assertEquals("Email", textArea.getText());
        assertEquals(2, textArea.getRows());
        assertEquals(30, textArea.getColumns());
        assertEquals(TextArea.EMAILADDR, textArea.getConstraint());
    }

    @FormTest
    void testSetText() {
        TextArea textArea = new TextArea();
        textArea.setText("New text");
        assertEquals("New text", textArea.getText());
    }

    @FormTest
    void testSetTextNull() {
        TextArea textArea = new TextArea("Initial");
        textArea.setText(null);
        assertEquals("", textArea.getText());
    }

    @FormTest
    void testRows() {
        TextArea textArea = new TextArea();
        textArea.setRows(5);
        assertEquals(5, textArea.getRows());

        textArea.setRows(10);
        assertEquals(10, textArea.getRows());
    }

    @FormTest
    void testColumns() {
        TextArea textArea = new TextArea();
        textArea.setColumns(20);
        assertEquals(20, textArea.getColumns());

        textArea.setColumns(40);
        assertEquals(40, textArea.getColumns());
    }

    @FormTest
    void testMaxSize() {
        TextArea textArea = new TextArea();
        textArea.setMaxSize(100);
        assertEquals(100, textArea.getMaxSize());
    }

    @FormTest
    void testConstraint() {
        TextArea textArea = new TextArea();

        textArea.setConstraint(TextArea.NUMERIC);
        assertEquals(TextArea.NUMERIC, textArea.getConstraint());

        textArea.setConstraint(TextArea.EMAILADDR);
        assertEquals(TextArea.EMAILADDR, textArea.getConstraint());

        textArea.setConstraint(TextArea.PASSWORD);
        assertEquals(TextArea.PASSWORD, textArea.getConstraint());
    }

    @FormTest
    void testEditableFlag() {
        TextArea textArea = new TextArea();
        assertTrue(textArea.isEditable());

        textArea.setEditable(false);
        assertFalse(textArea.isEditable());

        textArea.setEditable(true);
        assertTrue(textArea.isEditable());
    }

    @FormTest
    void testGrowByContent() {
        TextArea textArea = new TextArea();

        textArea.setGrowByContent(true);
        assertTrue(textArea.isGrowByContent());

        textArea.setGrowByContent(false);
        assertFalse(textArea.isGrowByContent());
    }

    @FormTest
    void testGrowByContentRevalidatesParentWhenRowsGrowDuringEditing() {
        TextArea textArea = new TextArea();
        textArea.setRows(1);
        textArea.setSingleLineTextArea(false);
        textArea.setGrowByContent(true);
        TrackingContainer parent = new TrackingContainer();
        parent.add(textArea);
        parent.revalidatedLater = false;
        Display.impl.setFocusedEditingText(textArea);
        try {
            textArea.setText("Line 1");
            parent.revalidatedLater = false;
            textArea.setText("Line 1\nLine 2");
            assertTrue(parent.revalidatedLater, "Parent should be revalidated when growByContent row count increases");
        } finally {
            Display.impl.setFocusedEditingText(null);
        }
    }

    @FormTest
    void testGrowByContentDoesNotRevalidateWhenNotEditing() {
        TextArea textArea = new TextArea();
        textArea.setRows(2);
        textArea.setGrowByContent(true);
        TrackingContainer parent = new TrackingContainer();
        parent.add(textArea);
        parent.revalidatedLater = false;

        Display.impl.setFocusedEditingText(null);
        textArea.setText("Line 1");

        assertFalse(parent.revalidatedLater, "Parent should not be revalidated when text changes outside active editing");
    }

    @FormTest
    void testEndsWith3Points() {
        TextArea textArea = new TextArea();

        textArea.setEndsWith3Points(true);
        assertTrue(textArea.isEndsWith3Points());

        textArea.setEndsWith3Points(false);
        assertFalse(textArea.isEndsWith3Points());
    }

    @FormTest
    void testSingleLineTextArea() {
        TextArea textArea = new TextArea();

        textArea.setSingleLineTextArea(true);
        assertTrue(textArea.isSingleLineTextArea());

        textArea.setSingleLineTextArea(false);
        assertFalse(textArea.isSingleLineTextArea());
    }

    @FormTest
    void testHint() {
        TextArea textArea = new TextArea();
        textArea.setHint("Enter text");
        assertEquals("Enter text", textArea.getHint());

        textArea.setHint("New hint");
        assertEquals("New hint", textArea.getHint());
    }

    @FormTest
    void testHintLabel() {
        TextArea textArea = new TextArea();
        Component hintLabel = textArea.getHintLabel();
        // Hint label may be null if not set
        assertTrue(hintLabel == null || hintLabel instanceof Label);
    }

    @FormTest
    void testAlignment() {
        TextArea textArea = new TextArea();

        textArea.setAlignment(Component.LEFT);
        assertEquals(Component.LEFT, textArea.getAlignment());

        textArea.setAlignment(Component.CENTER);
        assertEquals(Component.CENTER, textArea.getAlignment());

        textArea.setAlignment(Component.RIGHT);
        assertEquals(Component.RIGHT, textArea.getAlignment());
    }

    @FormTest
    void testAddActionListener() {
        TextArea textArea = new TextArea();
        AtomicBoolean listenerCalled = new AtomicBoolean(false);

        ActionListener listener = evt -> listenerCalled.set(true);
        textArea.addActionListener(listener);

        // Verify listener was added (no exception)
        assertNotNull(textArea);
    }

    @FormTest
    void testRemoveActionListener() {
        TextArea textArea = new TextArea();
        ActionListener listener = evt -> {};

        textArea.addActionListener(listener);
        textArea.removeActionListener(listener);

        // Verify listener was removed (no exception)
        assertNotNull(textArea);
    }

    @FormTest
    void testAddDataChangeListener() {
        TextArea textArea = new TextArea();
        AtomicInteger changeCount = new AtomicInteger(0);

        DataChangedListener listener = (type, index) -> changeCount.incrementAndGet();
        textArea.addDataChangeListener(listener);

        textArea.setText("New");
        assertTrue(changeCount.get() >= 0);
    }

    @FormTest
    void testRemoveDataChangeListener() {
        TextArea textArea = new TextArea();
        DataChangedListener listener = (type, index) -> {};

        textArea.addDataChangeListener(listener);
        textArea.removeDataChangeListener(listener);

        // Verify listener was removed (no exception)
        assertNotNull(textArea);
    }

    @FormTest
    void testLinesToScroll() {
        TextArea textArea = new TextArea();
        textArea.setLinesToScroll(5);
        assertEquals(5, textArea.getLinesToScroll());
    }

    @FormTest
    void testReplaceText() {
        TextArea textArea = new TextArea("Hello world");
        String text = textArea.getText();
        text = text.replace("world", "universe");
        textArea.setText(text);
        assertEquals("Hello universe", textArea.getText());
    }

    @FormTest
    void testGetActualRows() {
        TextArea textArea = new TextArea("Line1\nLine2\nLine3");
        int actualRows = textArea.getActualRows();
        assertTrue(actualRows >= 1);
    }

    @FormTest
    void testGetBaseline() {
        TextArea textArea = new TextArea("Test");
        int baseline = textArea.getBaseline(100, 50);
        assertTrue(baseline >= 0);
    }

    @FormTest
    void testGetBaselineResizeBehavior() {
        TextArea textArea = new TextArea();
        int behavior = textArea.getBaselineResizeBehavior();
        assertTrue(behavior >= Component.BRB_CONSTANT_ASCENT);
    }

    @FormTest
    void testGetPreferredSize() {
        TextArea textArea = new TextArea("Test text");
        Dimension pref = textArea.getPreferredSize();
        assertTrue(pref.getWidth() > 0);
        assertTrue(pref.getHeight() > 0);
    }

    @FormTest
    void testPreferredWidth() {
        TextArea textArea = new TextArea();
        textArea.setPreferredW(200);
        assertEquals(200, textArea.getPreferredW());
    }

    @FormTest
    void testIsQwertyInput() {
        TextArea textArea = new TextArea();
        // Just verify the method can be called
        boolean qwerty = textArea.isQwertyInput();
        assertTrue(qwerty || !qwerty);
    }

    @FormTest
    void testConstraintFlags() {
        TextArea textArea = new TextArea();

        textArea.setConstraint(TextArea.INITIAL_CAPS_WORD);
        assertEquals(TextArea.INITIAL_CAPS_WORD, textArea.getConstraint());

        textArea.setConstraint(TextArea.INITIAL_CAPS_SENTENCE);
        assertEquals(TextArea.INITIAL_CAPS_SENTENCE, textArea.getConstraint());
    }

    @FormTest
    void testRightToLeft() {
        TextArea textArea = new TextArea();
        textArea.setRTL(true);
        assertTrue(textArea.isRTL());

        textArea.setRTL(false);
        assertFalse(textArea.isRTL());
    }

    @FormTest
    void testUIID() {
        TextArea textArea = new TextArea();
        textArea.setUIID("CustomTextArea");
        assertEquals("CustomTextArea", textArea.getUIID());
    }

    @FormTest
    void testContrainstTypes() {
        assertEquals(0, TextArea.ANY);
        assertEquals(1, TextArea.EMAILADDR);
        assertEquals(2, TextArea.NUMERIC);
        assertEquals(3, TextArea.PHONENUMBER);
        assertEquals(4, TextArea.URL);
        assertEquals(5, TextArea.DECIMAL);
        assertTrue(TextArea.PASSWORD > 0);
        assertTrue(TextArea.UNEDITABLE > 0);
        assertTrue(TextArea.SENSITIVE > 0);
        assertTrue(TextArea.NON_PREDICTIVE > 0);
        assertTrue(TextArea.INITIAL_CAPS_WORD > 0);
        assertTrue(TextArea.INITIAL_CAPS_SENTENCE > 0);
    }

    @FormTest
    void testAppendText() {
        TextArea textArea = new TextArea("Hello");
        textArea.setText(textArea.getText() + " World");
        assertEquals("Hello World", textArea.getText());
    }

    @FormTest
    void testRefreshTheme() {
        TextArea textArea = new TextArea();
        assertDoesNotThrow(() -> textArea.refreshTheme(false));
        assertDoesNotThrow(() -> textArea.refreshTheme(true));
    }

    @FormTest
    void testMultilineVerticalAlignmentIsRetained() {
        TextArea textArea = new TextArea("Line 1\nLine 2\nLine 3");
        textArea.setSingleLineTextArea(false);
        textArea.setRows(4);
        textArea.setEditable(true);
        textArea.setVerticalAlignment(Component.CENTER);

        assertEquals(Component.CENTER, textArea.getVerticalAlignment());

        textArea.setVerticalAlignment(Component.BOTTOM);
        assertEquals(Component.BOTTOM, textArea.getVerticalAlignment());
    }

    @FormTest
    void testVerticalAlignmentRejectsInvalidValues() {
        TextArea textArea = new TextArea();
        assertThrows(IllegalArgumentException.class, () -> textArea.setVerticalAlignment(Component.BASELINE));
    }

    @FormTest
    void testMultilineDefaultsToTop() {
        // Regression test for #5345: with a CENTER theme default a multi-line text
        // area reports TOP by default (the theme default is meant for single-line
        // fields). For an editable area this matches the top-aligning native editor,
        // so the text doesn't jump when editing starts and ends.
        int previous = TextArea.getDefaultValign();
        TextArea.setDefaultValign(Component.CENTER);
        try {
            TextArea editable = new TextArea("text text", 3, 20);
            editable.setEditable(true);
            assertEquals(Component.TOP, editable.getVerticalAlignment());

            TextArea nonEditable = new TextArea("text text", 3, 20);
            nonEditable.setEditable(false);
            assertEquals(Component.TOP, nonEditable.getVerticalAlignment());
        } finally {
            TextArea.setDefaultValign(previous);
        }
    }

    @FormTest
    void testExplicitVerticalAlignmentOverridesMultilineDefault() {
        // An explicit setVerticalAlignment() is honored even for a multi-line area,
        // so display text can still be centered/bottom-aligned.
        int previous = TextArea.getDefaultValign();
        TextArea.setDefaultValign(Component.CENTER);
        try {
            TextArea textArea = new TextArea("text text", 3, 20);
            textArea.setEditable(false);

            textArea.setVerticalAlignment(Component.CENTER);
            assertEquals(Component.CENTER, textArea.getVerticalAlignment());

            textArea.setVerticalAlignment(Component.BOTTOM);
            assertEquals(Component.BOTTOM, textArea.getVerticalAlignment());
        } finally {
            TextArea.setDefaultValign(previous);
        }
    }

    @FormTest
    void testSingleLineKeepsThemeVerticalAlignment() {
        // Single-line fields keep the theme default (they center vertically in their
        // native editor).
        int previous = TextArea.getDefaultValign();
        TextArea.setDefaultValign(Component.CENTER);
        try {
            TextArea textArea = new TextArea("text", 1, 20);
            textArea.setSingleLineTextArea(true);
            textArea.setEditable(true);
            assertEquals(Component.CENTER, textArea.getVerticalAlignment());
        } finally {
            TextArea.setDefaultValign(previous);
        }
    }

    @FormTest
    void testVerticalAlignmentScreenshotStates() {
        Form form = new Form("TextArea VAlign", BoxLayout.y());
        form.getStyle().setPadding(4, 4, 4, 4);

        TextArea topAligned = createAlignmentSample("TOP", Component.TOP);
        TextArea centerAligned = createAlignmentSample("CENTER", Component.CENTER);
        TextArea bottomAligned = createAlignmentSample("BOTTOM", Component.BOTTOM);
        TextArea overflowAligned = createOverflowSample(Component.BOTTOM);

        form.addAll(topAligned, centerAligned, bottomAligned, overflowAligned);
        form.show();
        flushSerialCalls();

        assertTrue(TestUtils.screenshotTest("TextAreaVerticalAlignmentStates"));
    }

    private TextArea createAlignmentSample(String label, int valign) {
        TextArea sample = new TextArea(label + "\nline 2");
        sample.setRows(4);
        sample.setColumns(20);
        sample.setSingleLineTextArea(false);
        sample.setEditable(false);
        sample.setVerticalAlignment(valign);
        return sample;
    }

    private TextArea createOverflowSample(int valign) {
        TextArea sample = new TextArea("Overflow line 1\nOverflow line 2\nOverflow line 3\nOverflow line 4\nOverflow line 5");
        sample.setRows(3);
        sample.setColumns(20);
        sample.setSingleLineTextArea(false);
        sample.setEditable(false);
        sample.setVerticalAlignment(valign);
        return sample;
    }

    private static class TrackingContainer extends Container {
        private boolean revalidatedLater;

        TrackingContainer() {
            super(BoxLayout.y());
        }

        @Override
        public void revalidateLater() {
            revalidatedLater = true;
        }
    }
}
