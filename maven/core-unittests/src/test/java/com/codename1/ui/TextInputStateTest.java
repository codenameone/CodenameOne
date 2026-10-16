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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextInputStateTest {
    @Test
    void legacyRangeGettersRemainOrderedWhileAppleRetainsDirection() {
        TextInputState state = new TextInputState("backward", 8, 0, 1, 3);
        assertEquals(0, state.getSelectionStart());
        assertEquals(8, state.getSelectionEnd());
        assertEquals(8, state.getSelectionAnchor());
        assertEquals(0, state.getSelectionFocus());
        assertFalse(state.isCaret());
        assertEquals(1, state.getComposingStart());
        assertEquals(3, state.getComposingEnd());
        // Android deletion clamps end >= start, and DOM selection setters require this order.
        assertEquals("backward", state.getText().substring(state.getSelectionStart(),
                Math.max(state.getSelectionStart(), state.getSelectionEnd())));
    }

    @Test
    void forwardAndCollapsedSnapshotsKeepTheirExistingRange() {
        TextInputState forward = new TextInputState("text", 1, 4);
        assertEquals(1, forward.getSelectionStart());
        assertEquals(4, forward.getSelectionEnd());
        assertEquals(1, forward.getSelectionAnchor());
        assertEquals(4, forward.getSelectionFocus());
        TextInputState caret = new TextInputState("text", 2, 2);
        assertEquals(2, caret.getSelectionStart());
        assertEquals(2, caret.getSelectionEnd());
        assertTrue(caret.isCaret());
    }
}
