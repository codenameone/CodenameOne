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
import com.codename1.junit.UITestBase;
import com.codename1.ui.*;
import com.codename1.ui.editor.EditorView;
import com.codename1.ui.layouts.BorderLayout;
import static org.junit.jupiter.api.Assertions.*;

class IOSTextInputGeometryTest extends UITestBase {
    private static class NativeStub extends IOSNative {
        int[] bounds;
        @Override void startTextInput(int constraint, boolean correct, boolean caps, boolean multi,
                String text, int start, int end, int action) { }
        @Override void updateTextInputState(String text, int start, int end, int x, int y, int w, int h, int seq) { }
        @Override void setTextInputBounds(int x, int y, int w, int h) { bounds = new int[]{x, y, w, h}; }
        @Override void stopTextInput() { }
        @Override public boolean isAsyncEditMode() { return false; }
        @Override public int getVKBHeight() { return 150; }
        @Override boolean isDirectToDrawable() { return false; }
    }
    private static class Port extends IOSImplementation {
        @Override public int getDisplayWidth() { return 800; }
        @Override public int getDisplayHeight() { return 600; }
        @Override public boolean hasPendingPaints() { return true; }
    }
    private static class Client extends EditorView {
        int revealCount;
        Client() { super(null, false); }
        @Override public void scrollRectToVisible(int x, int y, int w, int h, Component space) {
            revealCount++;
            super.scrollRectToVisible(x, y, w, h, space);
        }
    }

    @FormTest
    void paintRefreshesMovedResizedAndScrolledGeometryWithoutAnEdit() {
        IOSNative oldNative = IOSImplementation.nativeInstance;
        IOSImplementation oldInstance = IOSImplementation.instance;
        NativeStub nativeStub = new NativeStub();
        Port port = new Port();
        Client client = new Client();
        try {
            IOSImplementation.nativeInstance = nativeStub;
            IOSImplementation.instance = port;
            Form form = new Form(new BorderLayout());
            Container ancestor = new Container(new BorderLayout());
            ancestor.add(BorderLayout.CENTER, client);
            form.add(BorderLayout.CENTER, ancestor);
            form.show();
            flushSerialCalls();
            client.setText("one\ntwo\nthree\nfour\nfive");
            port.startTextInput(client, client.getConfig());
            int[] before = IOSImplementation.tiRectForOffset(1);
            ancestor.setY(ancestor.getY() + 45);
            client.setWidth(client.getWidth() - 30);
            client.setHeight(25);
            client.setSelectionRange(18, 18); // scrolls internally without pushing text state
            port.paintDirty();
            int[] after = IOSImplementation.tiRectForOffset(18);
            assertFalse(java.util.Arrays.equals(before, after));
            assertArrayEquals(client.rectForOffset(18), after);
            assertEquals(18, IOSImplementation.tiOffsetAtPoint(after[0], after[1] + 1));
            assertArrayEquals(new int[]{client.getAbsoluteX(), client.getAbsoluteY(),
                client.getWidth(), client.getHeight()}, nativeStub.bounds);
            port.stopTextInput(client);
            assertArrayEquals(new int[]{0, 0, 0, 0}, IOSImplementation.tiRectForOffset(18));
        } finally {
            port.stopTextInput(client);
            IOSImplementation.nativeInstance = oldNative;
            IOSImplementation.instance = oldInstance;
        }
    }

    @FormTest
    void defaultKeyboardModeRevalidatesAndRevealsPureClientWithoutLegacyEditor() {
        IOSNative oldNative = IOSImplementation.nativeInstance;
        IOSImplementation oldInstance = IOSImplementation.instance;
        Port port = new Port();
        Client client = new Client();
        final int[] layouts = new int[1];
        try {
            IOSImplementation.nativeInstance = new NativeStub();
            IOSImplementation.instance = port;
            Form form = new Form(new BorderLayout()) {
                @Override public void revalidate() { layouts[0]++; super.revalidate(); }
            };
            form.setFormBottomPaddingEditingMode(false);
            form.add(BorderLayout.CENTER, client);
            form.show();
            flushSerialCalls();
            StringBuilder lines = new StringBuilder();
            for (int i = 0; i < 80; i++) lines.append("keyboard caret\n");
            client.setText(lines.toString());
            client.setSelectionRange(client.getTextLength(), client.getTextLength());
            port.startTextInput(client, client.getConfig());
            form.setOverrideInvisibleAreaUnderVKB(150);
            int before = layouts[0];
            IOSImplementation.keyboardWillBeShown();
            flushSerialCalls();
            assertTrue(layouts[0] > before, "pure clients must revalidate without currentEditing");
            assertTrue(client.revealCount > 0, "the caret must be revealed in its ancestor viewport");
            int[] caret = client.getCaretRect();
            assertTrue(caret[1] + caret[3] <= form.getHeight() - 150,
                    "a full-height editor must scroll its own caret above the keyboard");
            assertArrayEquals(client.getCaretRect(), IOSImplementation.tiRectForOffset(client.getSelectionEnd()));
            port.stopTextInput(client);
        } finally {
            port.stopTextInput(client);
            IOSImplementation.nativeInstance = oldNative;
            IOSImplementation.instance = oldInstance;
        }
    }

    @FormTest
    void nativeSelectionRecapturesGeometryAndIgnoresReplacedSessions() {
        IOSNative oldNative = IOSImplementation.nativeInstance;
        IOSImplementation oldInstance = IOSImplementation.instance;
        Port port = new Port();
        Client client = new Client();
        Client replacement = new Client();
        try {
            IOSImplementation.nativeInstance = new NativeStub();
            IOSImplementation.instance = port;
            client.setText("one\ntwo\nthree\nfour");
            client.setWidth(200);
            client.setHeight(25);
            port.startTextInput(client, client.getConfig());
            IOSImplementation.tiSetSelection(17, 17, 1);
            flushSerialCalls();
            assertEquals(17, IOSImplementation.tiSelectionEnd());
            assertArrayEquals(client.rectForOffset(17), IOSImplementation.tiRectForOffset(17));
            IOSImplementation.tiSetSelection(0, 0, 2);
            replacement.setText("replacement");
            port.startTextInput(replacement, replacement.getConfig());
            flushSerialCalls();
            assertEquals(17, client.getSelectionEnd(), "queued old-session callbacks must not mutate it");
        } finally {
            port.stopTextInput(replacement);
            port.stopTextInput(client);
            IOSImplementation.nativeInstance = oldNative;
            IOSImplementation.instance = oldInstance;
        }
    }
}
