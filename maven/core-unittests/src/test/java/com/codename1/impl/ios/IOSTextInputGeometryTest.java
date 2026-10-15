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
        int sequence;
        String text;
        @Override void startTextInput(int constraint, boolean correct, boolean caps, boolean multi,
                String text, int start, int end, int action) { }
        @Override void updateTextInputState(String text, int start, int end, int x, int y, int w, int h, int seq) { sequence = seq; this.text = text; }
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
    @FormTest
    void delayedKeyboardHideRestoresItsOwnerAndPreservesTheNextFormsPadding() {
        IOSNative oldNative = IOSImplementation.nativeInstance;
        IOSImplementation oldInstance = IOSImplementation.instance;
        Port port = new Port();
        Client a = new Client();
        Client b = new Client();
        try {
            IOSImplementation.nativeInstance = new NativeStub();
            IOSImplementation.instance = port;
            Form first = new Form(new BorderLayout());
            first.setFormBottomPaddingEditingMode(true);
            first.add(BorderLayout.CENTER, a);
            Form second = new Form(new BorderLayout());
            second.setFormBottomPaddingEditingMode(true);
            second.add(BorderLayout.CENTER, b);
            first.getContentPane().getStyle().setPaddingUnitBottom(com.codename1.ui.plaf.Style.UNIT_TYPE_DIPS);
            first.getContentPane().getStyle().setPadding(Component.BOTTOM, 21);
            second.getContentPane().getStyle().setPadding(Component.BOTTOM, 35);
            first.show();
            flushSerialCalls();
            port.startTextInput(a, a.getConfig());
            IOSImplementation.keyboardWillBeShown();
            flushSerialCalls();
            assertEquals(150, first.getContentPane().getStyle().getPaddingBottom());
            IOSImplementation.keyboardWillBeHidden(); // queued against first
            second.show();
            port.startTextInput(b, b.getConfig());
            IOSImplementation.keyboardWillBeShown();
            flushSerialCalls();
            assertEquals(21, first.getContentPane().getStyle().getPaddingFloatValue(false, Component.BOTTOM));
            assertEquals(com.codename1.ui.plaf.Style.UNIT_TYPE_DIPS,
                    first.getContentPane().getStyle().getPaddingUnit()[Component.BOTTOM]);
            assertEquals(150, second.getContentPane().getStyle().getPaddingBottom());
            IOSImplementation.keyboardWillBeHidden();
            IOSImplementation.keyboardWillBeShown(); // same form, a newer notification
            flushSerialCalls();
            assertEquals(150, second.getContentPane().getStyle().getPaddingBottom());
            IOSImplementation.keyboardWillBeHidden();
            flushSerialCalls();
            assertEquals(35, second.getContentPane().getStyle().getPaddingBottom());
        } finally {
            port.stopTextInput(a);
            port.stopTextInput(b);
            IOSImplementation.nativeInstance = oldNative;
            IOSImplementation.instance = oldInstance;
        }
    }

    @FormTest
    void commandAcknowledgementFollowsTheFinalStateAndIgnoresReplacedSessions() {
        IOSNative oldNative = IOSImplementation.nativeInstance;
        IOSImplementation oldInstance = IOSImplementation.instance;
        final Port port = new Port();
        final NativeStub stub = new NativeStub();
        final int[] calls = new int[1];
        EditorView client = new EditorView(null, false) {
            @Override public void onKeyCommand(int command, int modifiers) {
                calls[0]++;
                setText("intermediate");
                port.updateTextInputState(this, getEditingState());
                assertNotEquals(40, stub.sequence, "intermediate pushes cannot release the native barrier");
                setText("final rich paste");
            }
        };
        Client replacement = new Client();
        try {
            IOSImplementation.nativeInstance = stub;
            IOSImplementation.instance = port;
            port.startTextInput(client, client.getConfig());
            IOSImplementation.tiKeyCommandWithSequence(TextInputClient.KEY_PASTE, 0, 40);
            flushSerialCalls();
            assertEquals(1, calls[0]);
            assertEquals(40, stub.sequence);
            assertEquals("final rich paste", stub.text);
            IOSImplementation.tiKeyCommandWithSequence(TextInputClient.KEY_CUT, 0, 41);
            port.startTextInput(replacement, replacement.getConfig());
            flushSerialCalls();
            assertEquals(1, calls[0]);
        } finally {
            port.stopTextInput(client);
            port.stopTextInput(replacement);
            IOSImplementation.nativeInstance = oldNative;
            IOSImplementation.instance = oldInstance;
        }
    }

    @FormTest
    void keyboardHideRelayoutsSavedWindowAfterInputStopsWithStyleEventsSuppressed() {
        IOSNative oldNative = IOSImplementation.nativeInstance;
        IOSImplementation oldInstance = IOSImplementation.instance;
        Port port = new Port();
        Client client = new Client();
        implementation.setMultiWindowSupported(true);
        Window window = new Window("Stopped editor", new BorderLayout());
        try {
            IOSImplementation.nativeInstance = new NativeStub();
            IOSImplementation.instance = port;
            Form main = new Form("Main", new BorderLayout());
            main.show();
            window.add(BorderLayout.CENTER, client);
            window.show();
            window.setWindowContentSize(400, 600);
            flushSerialCalls();
            com.codename1.ui.plaf.Style style = window.getContentPane().getUnselectedStyle();
            style.setPaddingUnitBottom(com.codename1.ui.plaf.Style.UNIT_TYPE_DIPS);
            style.setPadding(Component.BOTTOM, 3);
            window.revalidate();
            flushSerialCalls();
            int heightBefore = client.getHeight();
            port.startTextInput(client, client.getConfig());
            IOSImplementation.keyboardWillBeShown();
            flushSerialCalls();
            assertTrue(client.getHeight() < heightBefore, "keyboard must shrink the viewport first");
            style.setSuppressChangeEvents(true);
            IOSImplementation.keyboardWillBeHidden();
            port.stopTextInput(client); // ends the session before the queued hide runs
            flushSerialCalls();
            assertEquals(3f, style.getPaddingFloatValue(false, Component.BOTTOM));
            assertEquals(com.codename1.ui.plaf.Style.UNIT_TYPE_DIPS,
                    style.getPaddingUnit()[Component.BOTTOM]);
            assertEquals(heightBefore, client.getHeight(),
                    "the saved window must regain its viewport without a style-change event");
        } finally {
            window.getContentPane().getUnselectedStyle().setSuppressChangeEvents(false);
            port.stopTextInput(client);
            window.dispose();
            IOSImplementation.nativeInstance = oldNative;
            IOSImplementation.instance = oldInstance;
        }
    }

    @FormTest
    void secondaryWindowKeyboardReservesItsOwnViewportAndLeavesMainFormAlone() {
        IOSNative oldNative = IOSImplementation.nativeInstance;
        IOSImplementation oldInstance = IOSImplementation.instance;
        Port port = new Port();
        Client client = new Client();
        implementation.setMultiWindowSupported(true);
        Window window = new Window("Secondary editor", new BorderLayout());
        try {
            IOSImplementation.nativeInstance = new NativeStub();
            IOSImplementation.instance = port;
            Form main = new Form("Main", new BorderLayout());
            main.setFormBottomPaddingEditingMode(true);
            main.getContentPane().getStyle().setPadding(Component.BOTTOM, 37);
            main.show();
            window.add(BorderLayout.CENTER, client);
            window.show();
            window.setWindowContentSize(400, 600);
            flushSerialCalls();
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < 80; i++) text.append("window keyboard caret\n");
            client.setText(text.toString());
            client.setSelectionRange(client.getTextLength(), client.getTextLength());
            assertNull(client.getComponentForm());
            assertSame(window, client.getTopLevelContainer());
            int heightBefore = client.getHeight();
            int mainPadding = main.getContentPane().getStyle().getPaddingBottom();
            port.startTextInput(client, client.getConfig());
            IOSImplementation.keyboardWillBeShown();
            flushSerialCalls();
            assertEquals(mainPadding, main.getContentPane().getStyle().getPaddingBottom());
            assertEquals(0, port.getInvisibleAreaUnderVKB(), "another scene must not obscure the main form");
            assertTrue(client.getHeight() <= heightBefore - 140, "secondary content must shrink above its keyboard");
            assertTrue(client.revealCount > 0);
            int[] caret = client.getCaretRect();
            assertTrue(caret[1] + caret[3] <= window.getAbsoluteY() + window.getHeight() - 150);
            IOSImplementation.keyboardWillBeHidden();
            flushSerialCalls();
            assertEquals(heightBefore, client.getHeight());
        } finally {
            port.stopTextInput(client);
            window.dispose();
            IOSImplementation.nativeInstance = oldNative;
            IOSImplementation.instance = oldInstance;
        }
    }

}
