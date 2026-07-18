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
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Form;
import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;
import com.codename1.ui.layouts.BorderLayout;
import static org.junit.jupiter.api.Assertions.*;

/// Regression coverage for normalized offsets, rich-model round trips, undo/redo and touch scrolling.
class EditorRegressionTest extends UITestBase {
    private static class Host implements EditorHost {
        int starts;
        int stops;

        public boolean isTextInputSupported() { return true; }
        public Object startTextInput(TextInputClient client, TextInputConfig config) {
            starts++;
            return this;
        }
        public void updateTextInputState(Object handle, TextInputState state) { }
        public void stopTextInput(Object handle) { stops++; }
        public void editorChanged() { }
        public void fireEditorEvent(String type, String value) { }
    }

    private static final class ExposedView extends EditorView {
        ExposedView(EditorHost host) { super(host, true); }
        int verticalScroll() { return getVerticalScroll(); }
        int maximumVerticalScroll() { return getMaximumVerticalScroll(); }
    }

    private <T extends EditorView> T show(T view) {
        Form form = new Form("Editor", new BorderLayout());
        form.add(BorderLayout.CENTER, view);
        form.show();
        for (int i = 0; i < 4; i++) {
            flushSerialCalls();
        }
        return view;
    }

    @FormTest
    void replacementNormalizesBeforeUpdatingCaretAndHistory() {
        EditorView view = show(new EditorView(new Host(), true));
        view.commitText("a\r\nb\rc");
        assertEquals("a\nb\nc", view.getText());
        assertEquals(view.getText().length(), view.getCaretOffset());
        view.onKeyCommand(TextInputClient.KEY_BACKSPACE, 0);
        assertEquals("a\nb\n", view.getText());
        view.performUndo();
        assertEquals("a\nb\nc", view.getText());

        RichView rich = new RichView(new Host());
        rich.commitText("a\r\nb");
        assertEquals(rich.getDocument().length(), rich.getInlineStyles().length());
        assertEquals(rich.getDocument().length(), rich.getLinkRuns().size());
        assertEquals(rich.getDocument().length(), rich.getImageSources().size());
    }

    @FormTest
    void richHtmlFragmentsLinksAndImagesRoundTrip() {
        RichPureEditor editor = new RichPureEditor(new Host(), "richtext");
        RichView view = (RichView) editor.view();
        show(view);
        editor.cmd("setHtml", "<p>start </p>");
        view.setSelectionRange(view.getText().length(), view.getText().length());
        editor.cmd("insertHtml", "<b>bold</b> <a href=\"https://example.com/a?x=1&amp;y=2\">link</a>");
        editor.cmd("insertImage", "gen:2x3:ff0000");
        String html = editor.query("getHtml", null);
        assertTrue(html.contains("<b>bold</b>"), html);
        assertTrue(html.contains("href=\"https://example.com/a?x=1&amp;y=2\""), html);
        assertTrue(html.contains("<img src=\"gen:2x3:ff0000\">"), html);
    }

    @FormTest
    void createLinkPersistsItsTarget() {
        RichPureEditor editor = new RichPureEditor(new Host(), "richtext");
        RichView view = (RichView) editor.view();
        show(view);
        editor.cmd("setHtml", "<p>Codename One</p>");
        view.setSelectionRange(0, 8);
        editor.cmd("createLink", "https://www.codenameone.com/");
        String html = editor.query("getHtml", null);
        assertTrue(html.contains("href=\"https://www.codenameone.com/\""), html);
    }

    @FormTest
    void undoRedoRestoreRichFormattingAndParagraphs() {
        RichPureEditor editor = new RichPureEditor(new Host(), "richtext");
        RichView view = (RichView) editor.view();
        show(view);
        editor.cmd("setHtml", "<h1><b>Head</b></h1><p>Body</p>");
        String original = editor.query("getHtml", null);
        assertTrue(original.contains("<h1><b>Head</b></h1>"), original);
        view.setSelectionRange(0, 5); // formatted text plus the paragraph separator
        view.commitText("");
        view.performUndo();
        assertEquals(original, editor.query("getHtml", null));
        view.performRedo();
        assertFalse(editor.query("getHtml", null).contains("<b>Head</b>"));
    }

    @FormTest
    void touchDragScrollsWithoutMovingCaret() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        boolean wasDesktop = impl.isDesktop();
        impl.setDesktop(false);
        try {
            ExposedView view = show(new ExposedView(new Host()));
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < 240; i++) {
                text.append("line ").append(i).append('\n');
            }
            view.setText(text.toString());
            view.setSelectionRange(0, 0);
            assertFalse(com.codename1.ui.Display.getInstance().isDesktop());
            assertTrue(view.maximumVerticalScroll() > 0,
                    "precondition: long document must exceed viewport; height=" + view.getHeight()
                            + ", preferred=" + view.getPreferredH());
            int x = view.getAbsoluteX() + 20;
            int y = view.getAbsoluteY() + Math.max(60, view.getHeight() - 40);
            view.pointerPressed(x, y);
            view.pointerDragged(x, y - 120);
            view.pointerReleased(x, y - 120);
            assertTrue(view.verticalScroll() > 0,
                    "vertical swipe must scroll the document; max=" + view.maximumVerticalScroll());
            assertEquals(0, view.getCaretOffset(), "scroll gesture must restore the pre-press caret");
        } finally {
            impl.setDesktop(wasDesktop);
        }
    }

    @FormTest
    void blurStopsTextInputAndClearsFocus() {
        Host host = new Host();
        EditorView view = show(new EditorView(host, true));
        view.requestFocus();
        assertTrue(host.starts > 0);
        view.blur();
        assertNull(view.getComponentForm().getFocused());
        assertTrue(host.stops > 0);
    }
}
