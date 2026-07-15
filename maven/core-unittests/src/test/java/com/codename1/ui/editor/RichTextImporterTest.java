/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.editor;

import com.codename1.ui.RichTextFormat;
import com.codename1.ui.RichTextClipboardData;
import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RichTextImporterTest {

    private static final class NoopHost implements EditorHost {
        public boolean isTextInputSupported() { return false; }
        public Object startTextInput(TextInputClient client, TextInputConfig config) { return null; }
        public void updateTextInputState(Object handle, TextInputState state) { }
        public void stopTextInput(Object handle) { }
        public void editorChanged() { }
        public void fireEditorEvent(String type, String value) { }
    }

    private static final class PasteView extends RichView {
        PasteView() { super(new NoopHost()); }
        void paste(Object data) { pasteClipboardData(data); }
    }

    private static HtmlImporter.Result parse(String value, RichTextFormat format) {
        return HtmlImporter.parse(RichTextImporter.toHtml(value, format));
    }

    @Test
    void markdownPreservesBlocksInlineStylesLinksAndImages() {
        HtmlImporter.Result result = parse("# Title\n\nA **bold** [link](https://example.com)\n\n- one\n- two\n\n![alt](pic.png)",
                RichTextFormat.MARKDOWN);
        assertTrue(result.getText().contains("Title"));
        assertEquals(RichBlocks.H1, result.getBlocks().get(0).type);
        int bold = result.getText().indexOf("bold");
        assertTrue(result.getStyles().get(bold).isBold());
        int link = result.getText().indexOf("link");
        assertEquals("https://example.com", result.getLinks().get(link));
        assertTrue(result.getBlocks().get(2).listType == RichBlocks.LIST_UNORDERED
                || result.getBlocks().get(1).listType == RichBlocks.LIST_UNORDERED);
        assertTrue(result.getImageSources().contains("pic.png"));
    }

    @Test
    void asciidocPreservesHeadingInlineStylesLinkAndList() {
        HtmlImporter.Result result = parse("= Heading\n\nA *bold* https://example.com[link]\n\n. first\n. second",
                RichTextFormat.ASCIIDOC);
        assertEquals(RichBlocks.H1, result.getBlocks().get(0).type);
        assertTrue(result.getStyles().get(result.getText().indexOf("bold")).isBold());
        assertEquals("https://example.com", result.getLinks().get(result.getText().indexOf("link")));
        boolean ordered = false;
        for (RichBlocks.BlockAttr block : result.getBlocks()) {
            ordered |= block.listType == RichBlocks.LIST_ORDERED;
        }
        assertTrue(ordered);
    }

    @Test
    void rtfPreservesStylesParagraphsUnicodeAndSkipsMetadata() {
        String rtf = "{\\rtf1\\ansi{\\fonttbl{\\f0 Arial;}}Plain {\\b bold} {\\i italic} "
                + "{\\ul under} {\\strike gone}\\par Unicode \\u8212? end}";
        HtmlImporter.Result result = parse(rtf, RichTextFormat.RTF);
        assertFalse(result.getText().contains("Arial"));
        assertTrue(result.getText().contains("Plain bold italic under gone"));
        assertTrue(result.getText().contains("Unicode \u2014 end"));
        assertTrue(result.getStyles().get(result.getText().indexOf("bold")).isBold());
        assertTrue(result.getStyles().get(result.getText().indexOf("italic")).isItalic());
        assertTrue(result.getStyles().get(result.getText().indexOf("under")).isUnderline());
        assertTrue(result.getStyles().get(result.getText().indexOf("gone")).isStrike());
        assertTrue(result.getBlocks().size() >= 2);
    }

    @Test
    void plainTextIsEscapedBeforeHtmlImport() {
        HtmlImporter.Result result = parse("<tag> & value\nnext", RichTextFormat.PLAIN_TEXT);
        assertEquals("<tag> & value\nnext", result.getText());
    }

    @Test
    void richViewPastePrefersRichRepresentationAndRecognizesRtf() {
        PasteView view = new PasteView();
        view.paste(new RichTextClipboardData("plain").setHtml("<b>rich</b>"));
        assertEquals("rich", view.getText());
        assertTrue(view.getInlineStyles().styleAt(0).isBold());

        view.setText("");
        view.paste("{\\rtf1\\ansi {\\i RTF}}");
        assertEquals("RTF", view.getText().trim());
        assertTrue(view.getInlineStyles().styleAt(view.getText().indexOf("RTF")).isItalic());
    }
}
