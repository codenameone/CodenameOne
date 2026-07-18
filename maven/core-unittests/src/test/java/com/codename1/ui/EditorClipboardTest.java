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
import com.codename1.ui.editor.RichView;
import com.codename1.ui.layouts.BorderLayout;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Verifies the generic clipboard flows through the editors: text, HTML and -- for the rich editor --
/// pasting an image inserts an inline image.
class EditorClipboardTest extends UITestBase {

    private RichView showRich() {
        implementation.setTextInputSupported(true);
        implementation.setEditorNativePeerSupported(false);
        RichTextArea editor = new RichTextArea("<p>hi</p>");
        Form f = new Form("rt", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        for (int i = 0; i < 8; i++) {
            flushSerialCalls();
        }
        return (RichView) editor.getComponentAt(0);
    }

    @FormTest
    void pastingAnImageInsertsAnInlineImage() {
        RichView view = showRich();
        int before = imageCount(view.getImageSources());
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 1, 2, 3, 4};
        Display.getInstance().copyToClipboard(
                new ClipboardContent().setData(ClipboardContent.MIME_PNG, png));
        view.setSelectionRange(view.getTextLength(), view.getTextLength());
        view.onKeyCommand(TextInputClient.KEY_PASTE, 0);
        List<String> sources = view.getImageSources();
        assertEquals(before + 1, imageCount(sources), "one image inserted");
        boolean hasDataUri = false;
        for (String s : sources) {
            if (s != null && s.startsWith("data:image/png;base64,")) {
                hasDataUri = true;
            }
        }
        assertTrue(hasDataUri, "pasted image kept as a self-contained data URI");
    }

    @FormTest
    void pastingHtmlInsertsRichContent() {
        RichView view = showRich();
        Display.getInstance().copyToClipboard(
                new ClipboardContent().setData(ClipboardContent.MIME_HTML, "<b>bold</b>"));
        view.setSelectionRange(view.getTextLength(), view.getTextLength());
        view.onKeyCommand(TextInputClient.KEY_PASTE, 0);
        assertTrue(view.getText().contains("bold"), "html paste inserted its text");
    }

    @FormTest
    void pastingPlainTextIntoPlainEditor() {
        implementation.setTextInputSupported(true);
        CodeEditor editor = new CodeEditor("text", "");
        Form f = new Form("c", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        for (int i = 0; i < 8; i++) {
            flushSerialCalls();
        }
        EditorView view = (EditorView) editor.getComponentAt(0);
        Display.getInstance().copyToClipboard(
                new ClipboardContent().setData(ClipboardContent.MIME_TEXT, "hello"));
        view.onKeyCommand(TextInputClient.KEY_PASTE, 0);
        assertEquals("hello", view.getText());
    }

    private static int imageCount(List<String> sources) {
        int n = 0;
        for (String s : sources) {
            if (s != null && s.length() > 0) {
                n++;
            }
        }
        return n;
    }
}
