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
package com.codename1.impl.linux;

import com.codename1.ui.TextInputState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LinuxTextInputTest {
    @Test
    void gtkCharacterOffsetsBecomeUtf16WithoutSplittingSurrogatePairs() {
        String text = "A\ud83d\ude00e\u0301Z";
        assertEquals(3, LinuxTextInput.moveCodePoints(text, 0, 2));
        assertEquals(1, LinuxTextInput.moveCodePoints(text, 3, -1));
        assertEquals(5, LinuxTextInput.moveCodePoints(text, 3, 2));
        assertEquals(3, LinuxTextInput.moveCodePoints(text, 5, -2));
    }

    @Test
    void surroundingRequestsClampToTheAvailableSnapshot() {
        String text = "A\ud83d\ude00B";
        assertEquals(0, LinuxTextInput.moveCodePoints(text, 4, Integer.MIN_VALUE));
        assertEquals(4, LinuxTextInput.moveCodePoints(text, 0, Integer.MAX_VALUE));
        assertEquals(0, LinuxTextInput.moveCodePoints("", 0, 1));
    }

    @Test
    void oversizedDeletionBeforeCaretCannotEraseTextAfterIt() {
        TextInputState state = new TextInputState("A\ud83d\ude00B", 3, 3);
        assertArrayEquals(new int[] {2, 0}, LinuxTextInput.surroundingDeletion(state, -1, 1));
        assertArrayEquals(new int[] {3, 0}, LinuxTextInput.surroundingDeletion(state, -100, 100));
        assertArrayEquals(new int[] {2, 1}, LinuxTextInput.surroundingDeletion(state, -1, 2));
        assertNull(LinuxTextInput.surroundingDeletion(state, 1, 1));
    }

    @Test
    void absentOrStaleHandlesCannotInvokeNativeStateChanges() {
        LinuxTextInput input = new LinuxTextInput();
        input.stop(null);
        input.stop(new Object());
        input.update(new Object(), new TextInputState("ignored", 0, 0));
        input.update(null, null);
        assertThrows(IllegalArgumentException.class, () -> input.start(null, null));
    }
}
