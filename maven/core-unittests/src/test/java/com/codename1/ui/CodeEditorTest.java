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
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deep coverage for {@link CodeEditor} including the asynchronous {@link CodeCompletionProvider} flow.
 * Tests drive the editor against the deterministic native editor backend of the test implementation so
 * the command/query channel and the completion request/response cycle (with JSON serialization) are
 * fully exercised without a real web view.
 */
class CodeEditorTest extends UITestBase {

    private void pump() {
        for (int i = 0; i < 6; i++) {
            flushSerialCalls();
        }
    }

    private CodeEditor showNativeEditor() {
        implementation.setEditorNativePeerSupported(true);
        CodeEditor editor = new CodeEditor();
        Form f = new Form("code", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        pump();
        return editor;
    }

    private List<String> cmds() {
        return implementation.getEditorCommands();
    }

    @FormTest
    void testNativeBackendTypeIsCode() {
        CodeEditor editor = showNativeEditor();
        assertTrue(editor.isNativeEditor());
        assertTrue(editor.isEditorReady());
        assertEquals("code", implementation.getLastEditorType());
    }

    @FormTest
    void testLanguageThemeLineNumbersTabSize() {
        CodeEditor editor = showNativeEditor();
        editor.setLanguage("java");
        editor.setTheme("dark");
        editor.setShowLineNumbers(false);
        editor.setTabSize(2);
        assertEquals("java", editor.getLanguage());
        assertEquals("dark", editor.getTheme());
        assertFalse(editor.isShowLineNumbers());
        assertEquals(2, editor.getTabSize());
        List<String> c = cmds();
        assertTrue(c.contains("setLanguage:java"));
        assertTrue(c.contains("setTheme:dark"));
        assertTrue(c.contains("setLineNumbers:0"));
        assertTrue(c.contains("setTabSize:2"));
    }

    @FormTest
    void testReadOnlyMapsToEditable() {
        CodeEditor editor = showNativeEditor();
        editor.setReadOnly(true);
        assertTrue(editor.isReadOnly());
        assertFalse(editor.isEditable());
        assertTrue(cmds().contains("setEditable:0"));
        editor.setReadOnly(false);
        assertFalse(editor.isReadOnly());
        assertTrue(cmds().contains("setEditable:1"));
    }

    @FormTest
    void testSetTextAndInsert() {
        CodeEditor editor = showNativeEditor();
        editor.setText("public class A {}");
        editor.insertAtCursor("// note");
        assertTrue(cmds().contains("setText:public class A {}"));
        assertTrue(cmds().contains("insertText:// note"));
    }

    @FormTest
    void testConstructorWithLanguageAndText() {
        implementation.setEditorNativePeerSupported(true);
        CodeEditor editor = new CodeEditor("kotlin", "fun main() {}");
        Form f = new Form("code", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        pump();
        assertEquals("kotlin", editor.getLanguage());
        assertTrue(cmds().contains("setLanguage:kotlin"));
        assertTrue(cmds().contains("setText:fun main() {}"));
    }

    @FormTest
    void testGetTextQueryRoundTrip() {
        implementation.setEditorNativePeerSupported(true);
        implementation.setEditorQueryResponder(q -> q.equals("getText:") ? "the code" : null);
        CodeEditor editor = showNativeEditor();
        AtomicReference<String> result = new AtomicReference<>();
        editor.getText(result::set);
        assertEquals("the code", result.get());
    }

    @FormTest
    void testGetCursorPositionParsesInteger() {
        implementation.setEditorNativePeerSupported(true);
        implementation.setEditorQueryResponder(q -> q.equals("getCursor:") ? "42" : null);
        CodeEditor editor = showNativeEditor();
        AtomicReference<Integer> result = new AtomicReference<>();
        editor.getCursorPosition(result::set);
        assertEquals(Integer.valueOf(42), result.get());
    }

    @FormTest
    void testCursorPositionDefaultsToZeroOnGarbage() {
        implementation.setEditorNativePeerSupported(true);
        implementation.setEditorQueryResponder(q -> "not-a-number");
        CodeEditor editor = showNativeEditor();
        AtomicReference<Integer> result = new AtomicReference<>();
        editor.getCursorPosition(result::set);
        assertEquals(Integer.valueOf(0), result.get());
    }

    @FormTest
    void testCompletionProviderEnableDisable() {
        CodeEditor editor = showNativeEditor();
        assertNull(editor.getCompletionProvider());
        CodeCompletionProvider p = (ed, code, cursor, results) -> results.onSucess(new ArrayList<>());
        editor.setCompletionProvider(p);
        assertSame(p, editor.getCompletionProvider());
        assertTrue(cmds().contains("setCompletionEnabled:1"));
        editor.setCompletionProvider(null);
        assertTrue(cmds().contains("setCompletionEnabled:0"));
    }

    @FormTest
    void testCompletionRequestFlowProducesJson() {
        implementation.setEditorNativePeerSupported(true);
        implementation.setEditorQueryResponder(q -> q.equals("getText:") ? "Sys" : null);
        CodeEditor editor = showNativeEditor();

        AtomicReference<String> seenCode = new AtomicReference<>();
        AtomicInteger seenCursor = new AtomicInteger(-1);
        editor.setCompletionProvider((ed, code, cursor, results) -> {
            seenCode.set(code);
            seenCursor.set(cursor);
            List<CodeCompletion> list = new ArrayList<>();
            list.add(new CodeCompletion("System", "System").setType("class").setDetail("java.lang"));
            list.add(new CodeCompletion("System.out", "System.out"));
            results.onSucess(list);
        });

        // the native peer would post this when the user triggers completion: reqId 7, cursor 3
        editor.fireEditorEvent("complete", "7:3");

        assertEquals("Sys", seenCode.get());
        assertEquals(3, seenCursor.get());

        String last = implementation.getLastEditorCommand();
        assertNotNull(last);
        assertTrue(last.startsWith("showCompletions:7:["), "got: " + last);
        assertTrue(last.contains("\"d\":\"System\""));
        assertTrue(last.contains("\"i\":\"System\""));
        assertTrue(last.contains("\"t\":\"class\""));
        assertTrue(last.contains("\"x\":\"java.lang\""));
        assertTrue(last.contains("\"d\":\"System.out\""));
    }

    @FormTest
    void testCompletionJsonEscaping() {
        implementation.setEditorNativePeerSupported(true);
        implementation.setEditorQueryResponder(q -> "");
        CodeEditor editor = showNativeEditor();
        editor.setCompletionProvider((ed, code, cursor, results) -> {
            List<CodeCompletion> list = new ArrayList<>();
            list.add(new CodeCompletion("a\"b", "x\ny\\z"));
            results.onSucess(list);
        });
        editor.fireEditorEvent("complete", "1:0");
        String last = implementation.getLastEditorCommand();
        assertNotNull(last);
        assertTrue(last.contains("a\\\"b"), "display quote should be escaped: " + last);
        assertTrue(last.contains("x\\ny\\\\z"), "insert newline/backslash should be escaped: " + last);
    }

    @FormTest
    void testCompletionWithoutProviderIsNoOp() {
        CodeEditor editor = showNativeEditor();
        int before = cmds().size();
        editor.fireEditorEvent("complete", "1:0");
        // no provider -> no showCompletions command emitted
        for (String c : cmds()) {
            assertFalse(c.startsWith("showCompletions:"));
        }
        // and it must not throw / emit anything new
        assertEquals(before, cmds().size());
    }

    @FormTest
    void testChangeEventStillDispatchesThroughOverride() {
        CodeEditor editor = showNativeEditor();
        AtomicInteger count = new AtomicInteger();
        ActionListener l = e -> count.incrementAndGet();
        editor.addChangeListener(l);
        editor.fireEditorEvent("change", null);
        assertEquals(1, count.get());
    }

    @FormTest
    void testPureBackendUsedWhenNoNativePeer() {
        // no native peer -> pure Codename One code engine
        CodeEditor editor = new CodeEditor("java", "class A {}");
        Form f = new Form("code", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        pump();
        assertFalse(editor.isNativeEditor());
        assertTrue(editor.isEditorReady());
        assertNull(editor.getInternalBrowser());
        final java.util.concurrent.atomic.AtomicReference<String> text =
                new java.util.concurrent.atomic.AtomicReference<String>();
        editor.getText(new com.codename1.util.SuccessCallback<String>() {
            public void onSucess(String v) {
                text.set(v);
            }
        });
        pump();
        assertEquals("class A {}", text.get());
    }

    @FormTest
    void testBrowserFallbackUsedWithoutLowLevelTextInput() {
        implementation.setTextInputSupported(false);
        CodeEditor editor = new CodeEditor("java", "class A {}");
        Form f = new Form("code", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        pump();
        assertFalse(editor.isNativeEditor());
        assertNotNull(editor.getInternalBrowser());
    }
}
