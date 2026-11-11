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

package com.codename1.impl.ios;

import com.codename1.junit.FormTest;
import com.codename1.ui.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The "nextButton" and "doneButton" client properties on iOS: which component
/// the keyboard moves to, and whether ending an edit fires the Done listener.
/// The defaults must stay exactly what they were before the properties existed.
class TextEditUtilKeyboardActionTest extends com.codename1.junit.UITestBase {
    private static final int RETURN_OR_NEXT = -2;
    private static final int DONE = -3;

    @AfterEach
    void clearNext() {
        TextEditUtil.setNextEditComponent(null);
        TextEditUtil.setExplicitKeyboardAction(false);
    }

    @Test
    void endCodesAreTheOnesNativeCodeSends() throws Exception {
        assertEquals(RETURN_OR_NEXT, TextEditUtil.ENDED_BY_RETURN_OR_NEXT);
        assertEquals(DONE, TextEditUtil.ENDED_BY_DONE);

        // The native side sends literals, so hold them to the constants here:
        // the Done button alone sends ENDED_BY_DONE, Next and Return the other.
        File source = new File("../../Ports/iOSPort/nativeSources/METALView.m");
        assertTrue(source.isFile(), "METALView.m not found from " + new File(".").getAbsolutePath());
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        String done = method(text, "-(void) keyboardDoneClicked {");
        String next = method(text, "-(void) keyboardNextClicked {");
        String ret = method(text, "- (BOOL)textFieldShouldReturn:(UITextField *)theTextField {");
        assertTrue(done.contains("stringEdit(YES, " + DONE + ","));
        assertFalse(done.contains("stringEdit(YES, " + RETURN_OR_NEXT + ","));
        assertTrue(next.contains("stringEdit(YES, " + RETURN_OR_NEXT + ","));
        assertFalse(next.contains("stringEdit(YES, " + DONE + ","));
        assertTrue(ret.contains("stringEdit(YES, " + RETURN_OR_NEXT + ","));
        assertFalse(ret.contains("stringEdit(YES, " + DONE + ","));
    }

    /// The text of an Objective-C method: from its signature to the next line
    /// that closes a top-level body.
    private static String method(String text, String signature) {
        int start = text.indexOf(signature);
        assertTrue(start >= 0, "no method " + signature);
        int end = text.indexOf("\n}", start);
        assertTrue(end > start, "unterminated method " + signature);
        return text.substring(start, end);
    }

    private static class NativeStub extends IOSNative {
        @Override public boolean isAsyncEditMode() { return false; }
        @Override void setNativeEditingComponentVisible(boolean b) { }
    }

    private static class Port extends IOSImplementation {
        @Override public int getDisplayWidth() { return 800; }
        @Override public int getDisplayHeight() { return 600; }
    }

    /// Ends an edit of field the way native code does and answers how many
    /// times its Done listener ran. Drives IOSImplementation.editingUpdate
    /// itself, so the wiring is held and not only the helpers it calls.
    private int endEdit(Port port, TextField field, TextField following, int endCode) throws Exception {
        final int[] fired = new int[1];
        field.setDoneListener(e -> fired[0]++);
        TextEditUtil.setNextEditComponent(TextEditUtil.nextFor(field, following));
        Field editing = IOSImplementation.class.getDeclaredField("currentEditing");
        editing.setAccessible(true);
        editing.set(port, field);
        IOSImplementation.editingUpdate("42", endCode, true);
        flushSerialCalls();
        assertNull(editing.get(port), "the edit must have ended");
        assertEquals("42", field.getText());
        return fired[0];
    }

    private static boolean editNext() throws Exception {
        Field editNext = IOSImplementation.class.getDeclaredField("editNext");
        editNext.setAccessible(true);
        return editNext.getBoolean(null);
    }

    @FormTest
    void editingUpdateFiresDoneForEveryEndingExceptAnExplicitNext() throws Exception {
        IOSNative oldNative = IOSImplementation.nativeInstance;
        IOSImplementation oldInstance = IOSImplementation.instance;
        Port port = new Port();
        try {
            IOSImplementation.nativeInstance = new NativeStub();
            IOSImplementation.instance = port;
            TextField following = new TextField();

            // no property: both codes fire and both advance, as the one code did
            assertEquals(1, endEdit(port, new TextField(), following, RETURN_OR_NEXT));
            assertTrue(editNext());
            assertEquals(1, endEdit(port, new TextField(), following, DONE));
            assertTrue(editNext());
            assertEquals(0, endEdit(port, new TextField(), following, -1));
            assertFalse(editNext());

            // nextButton: advancing does not finish, the Done button does
            TextField next = new TextField();
            next.putClientProperty("nextButton", Boolean.TRUE);
            assertEquals(0, endEdit(port, next, following, RETURN_OR_NEXT));
            assertTrue(editNext());
            assertEquals(1, endEdit(port, next, following, DONE));
            assertEquals(1, endEdit(port, next, null, RETURN_OR_NEXT));

            // doneButton: fires, and there is nowhere to advance to
            TextField done = new TextField();
            done.putClientProperty("doneButton", Boolean.TRUE);
            assertEquals(1, endEdit(port, done, following, RETURN_OR_NEXT));
            assertTrue(TextEditUtil.isLastEditComponent());
        } finally {
            IOSImplementation.nativeInstance = oldNative;
            IOSImplementation.instance = oldInstance;
        }
    }

    @Test
    void bothEndCodesAreKeyboardActionsAndNothingElseIs() {
        assertTrue(TextEditUtil.endedOnKeyboardAction(RETURN_OR_NEXT));
        assertTrue(TextEditUtil.endedOnKeyboardAction(DONE));
        assertFalse(TextEditUtil.endedOnKeyboardAction(-1));
        assertFalse(TextEditUtil.endedOnKeyboardAction(0));
        assertFalse(TextEditUtil.endedOnKeyboardAction(7));
    }

    @Test
    void theExplicitFlagIsWhatNativeCodeReadsBack() {
        assertFalse(TextEditUtil.isExplicitKeyboardAction());
        TextField field = new TextField();
        field.putClientProperty("nextButton", Boolean.TRUE);
        TextEditUtil.setExplicitKeyboardAction(TextEditUtil.choosesKeyboardAction(field));
        assertTrue(TextEditUtil.isExplicitKeyboardAction());
        TextEditUtil.setExplicitKeyboardAction(TextEditUtil.choosesKeyboardAction(new TextField()));
        assertFalse(TextEditUtil.isExplicitKeyboardAction());
    }

    @Test
    void aPlainFieldKeepsItsSuccessorAndFiresOnEveryEnding() {
        TextField field = new TextField();
        TextField following = new TextField();
        assertSame(following, TextEditUtil.nextFor(field, following));
        assertFalse(TextEditUtil.choosesKeyboardAction(field));

        TextEditUtil.setNextEditComponent(following);
        assertTrue(TextEditUtil.firesDoneEvent(RETURN_OR_NEXT, field));
        assertTrue(TextEditUtil.firesDoneEvent(DONE, field));
        TextEditUtil.setNextEditComponent(null);
        assertTrue(TextEditUtil.firesDoneEvent(RETURN_OR_NEXT, field));
    }

    @Test
    void doneButtonHasNoSuccessorAndFires() {
        TextField field = new TextField();
        field.putClientProperty("doneButton", Boolean.TRUE);
        assertNull(TextEditUtil.nextFor(field, new TextField()));
        assertTrue(TextEditUtil.choosesKeyboardAction(field));

        TextEditUtil.setNextEditComponent(TextEditUtil.nextFor(field, new TextField()));
        assertTrue(TextEditUtil.isLastEditComponent());
        assertTrue(TextEditUtil.firesDoneEvent(RETURN_OR_NEXT, field));
        assertTrue(TextEditUtil.firesDoneEvent(DONE, field));
    }

    @Test
    void nextButtonMovesOnWithoutFiringButItsDoneButtonStillFires() {
        TextField field = new TextField();
        TextField following = new TextField();
        field.putClientProperty("nextButton", Boolean.TRUE);
        assertSame(following, TextEditUtil.nextFor(field, following));
        assertTrue(TextEditUtil.choosesKeyboardAction(field));

        TextEditUtil.setNextEditComponent(following);
        assertFalse(TextEditUtil.firesDoneEvent(RETURN_OR_NEXT, field));
        assertTrue(TextEditUtil.firesDoneEvent(DONE, field));
    }

    @Test
    void nextButtonWithNothingFollowingIsAPlainLastField() {
        TextField field = new TextField();
        field.putClientProperty("nextButton", Boolean.TRUE);
        TextEditUtil.setNextEditComponent(TextEditUtil.nextFor(field, null));
        assertTrue(TextEditUtil.isLastEditComponent());
        assertTrue(TextEditUtil.firesDoneEvent(RETURN_OR_NEXT, field));
    }

    @Test
    void doneButtonWinsOverNextButton() {
        TextField field = new TextField();
        field.putClientProperty("nextButton", Boolean.TRUE);
        field.putClientProperty("doneButton", Boolean.TRUE);
        TextEditUtil.setNextEditComponent(TextEditUtil.nextFor(field, new TextField()));
        assertTrue(TextEditUtil.isLastEditComponent());
        assertTrue(TextEditUtil.firesDoneEvent(RETURN_OR_NEXT, field));
    }

    @Test
    void anEditThatDidNotEndOnTheKeyboardNeverFires() {
        TextField field = new TextField();
        assertFalse(TextEditUtil.firesDoneEvent(-1, field));
        assertFalse(TextEditUtil.firesDoneEvent(0, field));
        assertFalse(TextEditUtil.firesDoneEvent(RETURN_OR_NEXT, null));
    }
}
