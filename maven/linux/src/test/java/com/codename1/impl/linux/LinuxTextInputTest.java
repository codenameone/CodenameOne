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
import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
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
    private static class Client implements TextInputClient {
        String text = "original";
        int start = 0, end = 8, markedStart = -1, markedEnd = -1;
        int finished;
        public TextInputState getEditingState() { return new TextInputState(text, start, end, markedStart, markedEnd); }
        public String getTextRange(int a, int b) { return text.substring(a, b); }
        public void setComposingText(String value, int caret) {
            int a = markedStart < 0 ? Math.min(start, end) : markedStart;
            int b = markedStart < 0 ? Math.max(start, end) : markedEnd;
            text = text.substring(0, a) + value + text.substring(b);
            markedStart = a;
            markedEnd = a + value.length();
            start = end = a + caret;
        }
        public void commitText(String value) { setComposingText(value, value.length()); markedStart = markedEnd = -1; }
        public void finishComposing() { finished++; markedStart = markedEnd = -1; }
        public void setSelectionRange(int a, int b) { start = a; end = b; }
        public int getTextLength() { return text.length(); }
        public int[] getCaretRect() { return new int[]{0, 0, 1, 1}; }
        public int[] rectForOffset(int offset) { return getCaretRect(); }
        public int[] selectionRects(int a, int b) { return new int[0]; }
        public int offsetAtPoint(int x, int y) { return end; }
        public TextInputConfig getConfig() { return new TextInputConfig(); }
        public void deleteSurroundingText(int a, int b) { }
        public void onKeyCommand(int c, int m) { }
        public void onEditorAction(int a) { }
        public void inputFocusGained() { }
        public void inputFocusLost() { }
        public void replaceRange(int a, int b, String value) { }
    }

    @Test
    void canceledEmptyPreeditRestoresOriginalSelectedText() {
        Client client = new Client();
        client.setSelectionRange(8, 0);
        LinuxTextInput.Composition composition = new LinuxTextInput.Composition(client);
        composition.preedit("replacement", 3);
        composition.preedit("", 0);
        composition.cancel();
        composition.cancel();
        assertEquals("original", client.text);
        assertEquals(8, client.start);
        assertEquals(0, client.end);
        assertEquals(1, client.finished);
    }

    @Test
    void commitWorksBeforeOrAfterGtkPreeditEnd() {
        for (boolean endFirst : new boolean[]{false, true}) {
            Client client = new Client();
            LinuxTextInput.Composition composition = new LinuxTextInput.Composition(client);
            composition.preedit("marked", 6);
            composition.preedit("", 0);
            if (endFirst) { composition.cancel(); }
            composition.commit("committed");
            composition.preedit("", 0);
            composition.cancel();
            composition.commit("!");
            assertEquals("committed!", client.text);
        }
    }

    @Test
    void richClientsReceiveCancellationInsteadOfTextOnlyRollback() {
        class RichClient extends Client implements TextInputClient.CompositionCancellation {
            int cancellations;
            public void cancelComposing() { cancellations++; }
        }
        RichClient client = new RichClient();
        LinuxTextInput.Composition composition = new LinuxTextInput.Composition(client);
        composition.preedit("", 0);
        composition.cancel();
        assertEquals(0, client.cancellations);
        composition.preedit("marked", 6);
        composition.cancel();
        assertEquals(1, client.cancellations);
        assertEquals(0, client.finished);
    }

}
