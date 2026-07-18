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
import com.codename1.ui.events.ActionSource;
import com.codename1.ui.layouts.BorderLayout;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/// Coverage for the pure {@link EditField}: text round-trip, single/multi-line behavior, the data and
/// action listeners and the platform input configuration.
class EditFieldTest extends UITestBase {

    private EditField show(EditField f) {
        implementation.setTextInputSupported(true);
        Form form = new Form("ef", new BorderLayout());
        form.add(BorderLayout.CENTER, f);
        form.show();
        for (int i = 0; i < 6; i++) {
            flushSerialCalls();
        }
        return f;
    }

    @FormTest
    void textRoundTrips() {
        EditField f = show(new EditField("hello"));
        assertEquals("hello", f.getText());
        f.setText("world");
        assertEquals("world", f.getText());
        f.setSelectionRange(5, 5);
        f.commitText("!");
        assertEquals("world!", f.getText());
    }

    @FormTest
    void hintAndConstraint() {
        EditField f = show(new EditField("", "Email", TextArea.EMAILADDR));
        assertEquals("Email", f.getHint());
        assertEquals(TextArea.EMAILADDR, f.getConstraint());
        assertEquals(TextArea.EMAILADDR, f.getConfig().getConstraint());
        assertFalse(f.getConfig().isMultiline(), "single line by default");
    }

    @FormTest
    void singleLineReturnFiresActionWithoutInsertingNewline() {
        EditField f = show(new EditField("abc"));
        AtomicInteger actions = new AtomicInteger();
        f.addActionListener(e -> actions.incrementAndGet());
        f.setSelectionRange(3, 3);
        f.commitText("\n");
        assertEquals("abc", f.getText(), "no newline inserted on a single-line field");
        assertEquals(1, actions.get(), "return key fired the action");
    }

    @FormTest
    void singleLinePasteKeepsFirstLineOnly() {
        EditField f = show(new EditField(""));
        f.commitText("one\ntwo\nthree");
        assertEquals("onetwothree", f.getText(), "line breaks stripped from a pasted value");
    }

    @FormTest
    void multiLineInsertsNewline() {
        EditField f = show(new EditField(""));
        f.setSingleLineTextArea(false);
        assertTrue(f.getConfig().isMultiline());
        f.commitText("a");
        f.commitText("\n");
        f.commitText("b");
        assertEquals("a\nb", f.getText());
    }

    @FormTest
    void editorActionOnSingleLineFiresAction() {
        EditField f = show(new EditField("x"));
        AtomicInteger actions = new AtomicInteger();
        f.addActionListener(e -> actions.incrementAndGet());
        // simulate the soft keyboard's Done button
        f.onEditorAction(TextInputConfig.ACTION_DONE);
        assertEquals(1, actions.get());
    }

    @FormTest
    void dataChangedListenerFires() {
        EditField f = show(new EditField(""));
        AtomicInteger changes = new AtomicInteger();
        f.addDataChangedListener((type, index) -> changes.incrementAndGet());
        f.commitText("h");
        f.commitText("i");
        assertTrue(changes.get() >= 2, "data changed fired per edit, got " + changes.get());
    }

    @FormTest
    void editableToggle() {
        EditField f = show(new EditField("abc"));
        assertTrue(f.isEditable());
        f.setEditable(false);
        assertFalse(f.isEditable());
        f.commitText("z");
        assertEquals("abc", f.getText(), "no edit while non-editable");
    }

    @FormTest
    void implementsCommonTextInterfaces() {
        EditField f = new EditField("abc");
        // usable wherever the framework expects a TextHolder or an ActionSource
        TextHolder holder = f;
        assertEquals("abc", holder.getText());
        holder.setText("def");
        assertEquals("def", f.getText());
        ActionSource<?> source = f;
        assertNotNull(source, "EditField is an ActionSource");
    }

    @FormTest
    void multiLineConstructorStartsMultiLine() {
        EditField f = show(new EditField(4, 30));
        assertFalse(f.isSingleLineTextArea(), "(rows, columns) constructor is multi-line");
        assertTrue(f.getConfig().isMultiline());
        assertEquals(4, f.getRows());
        assertEquals(30, f.getColumns());
        f.commitText("a");
        f.commitText("\n");
        f.commitText("b");
        assertEquals("a\nb", f.getText(), "newlines kept in a multi-line field");
    }

    @FormTest
    void textFieldStyleConstructorStaysSingleLine() {
        EditField f = show(new EditField("hi", "Name", 12, TextArea.EMAILADDR));
        assertTrue(f.isSingleLineTextArea(), "(text, hint, columns, constraint) constructor is single-line");
        assertEquals("hi", f.getText());
        assertEquals("Name", f.getHint());
        assertEquals(12, f.getColumns());
        assertEquals(TextArea.EMAILADDR, f.getConstraint());
    }
}
