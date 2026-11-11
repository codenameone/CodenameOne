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

import com.codename1.ui.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
    void endCodesAreTheOnesNativeCodeSends() {
        // METALView.m sends these literals; changing a constant alone breaks it.
        assertTrue(TextEditUtil.ENDED_BY_RETURN_OR_NEXT == RETURN_OR_NEXT);
        assertTrue(TextEditUtil.ENDED_BY_DONE == DONE);
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
