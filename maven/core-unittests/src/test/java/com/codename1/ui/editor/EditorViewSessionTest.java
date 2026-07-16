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

package com.codename1.ui.editor;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;
import com.codename1.ui.layouts.BorderLayout;
import static org.junit.jupiter.api.Assertions.*;

/// Regression tests for the platform text-input session lifecycle: the session must survive the
/// component being detached and re-attached (layout rebuilds remove a focused editor WITHOUT firing
/// focusLost), must not exist for read-only editors, and a finalized IME composition must undo as a
/// single unit.
class EditorViewSessionTest extends UITestBase {

    private static final class CountingHost implements EditorHost {
        int starts;
        int stops;
        TextInputClient active;

        public boolean isTextInputSupported() {
            return true;
        }

        public Object startTextInput(TextInputClient client, TextInputConfig config) {
            starts++;
            active = client;
            return client;
        }

        public void updateTextInputState(Object handle, TextInputState state) {
        }

        public void stopTextInput(Object handle) {
            if (handle == active) {
                stops++;
                active = null;
            }
        }

        public void editorChanged() {
        }

        public void fireEditorEvent(String type, String value) {
        }
    }

    private Form form;

    private EditorView show(CountingHost host, String text) {
        EditorView v = new EditorView(host, true);
        form = new Form("session", new BorderLayout());
        form.add(BorderLayout.CENTER, v);
        form.show();
        for (int i = 0; i < 4; i++) {
            flushSerialCalls();
        }
        v.setText(text);
        return v;
    }

    @FormTest
    void removingFocusedEditorReleasesTheSessionAndRefocusRestartsIt() {
        CountingHost host = new CountingHost();
        EditorView v = show(host, "hello");
        v.requestFocus();
        flushSerialCalls();
        assertEquals(1, host.starts, "focus gain binds the input session");
        assertSame(v, host.active);

        // a layout rebuild detaches the editor without any focusLost event
        form.getContentPane().removeComponent(v);
        flushSerialCalls();
        assertNull(host.active, "detaching the editor must release the platform session");
        assertEquals(1, host.stops);

        // re-attach and focus again: the session must rebind rather than being blocked by a
        // stale handle (the click-away-then-back-to-revive bug)
        form.getContentPane().add(BorderLayout.CENTER, v);
        form.revalidate();
        v.requestFocus();
        flushSerialCalls();
        assertEquals(2, host.starts, "refocusing after re-attach rebinds the session");
        assertSame(v, host.active);
    }

    @FormTest
    void readOnlyEditorDoesNotBindASession() {
        CountingHost host = new CountingHost();
        EditorView v = new EditorView(host, true);
        v.setEditableState(false);
        form = new Form("session", new BorderLayout());
        form.add(BorderLayout.CENTER, v);
        form.show();
        for (int i = 0; i < 4; i++) {
            flushSerialCalls();
        }
        v.setText("hello");
        v.requestFocus();
        flushSerialCalls();
        assertEquals(0, host.starts, "read-only editors must not summon the keyboard");

        // re-enabling while focused brings the session up; disabling tears it down
        v.setEditableState(true);
        assertEquals(1, host.starts);
        v.setEditableState(false);
        assertEquals(1, host.stops);
    }

    @FormTest
    void finalizedCompositionUndoesAsASingleUnit() {
        CountingHost host = new CountingHost();
        EditorView v = show(host, "world");
        v.setSelectionRange(0, 0);
        // compose in two steps then commit; the whole composition must be ONE undo unit
        v.setComposingText("a", 1);
        v.setComposingText("ab", 2);
        v.commitText("ab");
        assertEquals("abworld", v.getText());
        v.performUndo();
        assertEquals("world", v.getText(), "undo removes the whole committed composition");
        v.performRedo();
        assertEquals("abworld", v.getText(), "redo restores the committed composition");
    }

    @FormTest
    void compositionOverSelectionUndoesBackToTheSelection() {
        CountingHost host = new CountingHost();
        EditorView v = show(host, "xyz");
        v.setSelectionRange(0, 2);
        v.setComposingText("Q", 1);
        v.finishComposing();
        assertEquals("Qz", v.getText());
        v.performUndo();
        assertEquals("xyz", v.getText(), "undo restores the text the composition replaced");
    }
}
