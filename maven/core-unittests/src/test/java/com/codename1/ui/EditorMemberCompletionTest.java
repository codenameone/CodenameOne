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
import com.codename1.ui.editor.EditorView;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/// Verifies that typing a member-access dot triggers a completion request (so a provider can supply
/// members), which the browser-era editor did but the pure editor previously suppressed.
class EditorMemberCompletionTest extends UITestBase {

    @FormTest
    void dotTriggersCompletionRequest() {
        implementation.setTextInputSupported(true);
        final AtomicInteger requests = new AtomicInteger();
        final AtomicReference<Integer> lastCursor = new AtomicReference<Integer>(-1);
        CodeEditor editor = new CodeEditor("java", "");
        editor.setCompletionProvider(new CodeCompletionProvider() {
            public void getCompletions(CodeEditor ed, String code, int cursor,
                    SuccessCallback<List<CodeCompletion>> results) {
                requests.incrementAndGet();
                lastCursor.set(cursor);
                List<CodeCompletion> out = new ArrayList<CodeCompletion>();
                // pretend the receiver resolves to a type with these members
                out.add(new CodeCompletion("getText()", "getText()").setType("member"));
                out.add(new CodeCompletion("setText(String)", "setText(").setType("member"));
                results.onSucess(out);
            }
        });
        Form f = new Form("m", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        for (int i = 0; i < 8; i++) {
            flushSerialCalls();
        }
        EditorView v = (EditorView) editor.getComponentAt(0);
        v.commitText("b");
        for (int i = 0; i < 3; i++) {
            flushSerialCalls();
        }
        int afterIdent = requests.get();
        assertTrue(afterIdent >= 1, "identifier char requests completion");

        v.commitText(".");
        for (int i = 0; i < 3; i++) {
            flushSerialCalls();
        }
        assertTrue(requests.get() > afterIdent, "the member-access dot requests completion");
        assertEquals(2, lastCursor.get().intValue(), "provider sees the cursor after 'b.'");
    }
}
