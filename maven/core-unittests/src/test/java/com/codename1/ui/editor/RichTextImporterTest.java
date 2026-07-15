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

import com.codename1.ui.RichTextFormat;
import com.codename1.ui.RichTextClipboardData;
import com.codename1.ui.ClipboardContent;
import com.codename1.ui.Display;
import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RichTextImporterTest extends UITestBase {

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
        void copy() { copySelection(); }
    }

    private static HtmlImporter.Result parse(String value, RichTextFormat format) {
        return RichTextImporter.parse(value, format);
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
        boolean unordered = false;
        for (RichBlocks.BlockAttr block : result.getBlocks()) {
            unordered |= block.listType == RichBlocks.LIST_UNORDERED;
        }
        assertTrue(unordered);
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

    @Test
    void markdownAndAsciiDocSerializeBackWithoutHtml() {
        HtmlImporter.Result markdown = RichTextImporter.parse(
                "# Title\n\nA **bold** `code` [link](https://example.com)\n\n- one",
                RichTextFormat.MARKDOWN);
        String markdownOut = serialize(markdown, RichTextFormat.MARKDOWN);
        assertTrue(markdownOut.contains("# Title"));
        assertTrue(markdownOut.contains("**bold**"));
        assertTrue(markdownOut.contains("`code`"));
        assertTrue(markdownOut.contains("[link](https://example.com)"));
        assertTrue(markdownOut.contains("- one"));
        assertFalse(markdownOut.contains("<p>"));

        HtmlImporter.Result asciidoc = RichTextImporter.parse(
                "= Title\n\nA *bold* +code+ [underline]#under# [line-through]#gone# https://example.com[link]\n\n. one",
                RichTextFormat.ASCIIDOC);
        String asciidocOut = serialize(asciidoc, RichTextFormat.ASCIIDOC);
        assertTrue(asciidocOut.contains("= Title"));
        assertTrue(asciidocOut.contains("*bold*"));
        assertTrue(asciidocOut.contains("+code+"));
        assertTrue(asciidocOut.contains("[underline]#under#"));
        assertTrue(asciidocOut.contains("[line-through]#gone#"));
        assertTrue(asciidocOut.contains("https://example.com[link]"));
        assertTrue(asciidocOut.contains(". one"));
        assertFalse(asciidocOut.contains("<p>"));
    }

    @Test
    void richCopyPublishesNegotiableNativeRepresentations() {
        PasteView view = new PasteView();
        HtmlImporter.Result result = HtmlImporter.parse("<p><b>bold</b> <a href=\"https://example.com\">link</a></p>");
        view.importContent(result.getText(), result.getStyles(), result.getBlocks(), result.getLinks(),
                RichPureEditor.loadImages(result.getImageSources()), result.getImageSources());
        view.setSelectionRange(0, view.getTextLength());
        view.copy();
        ClipboardContent clipboard = Display.getInstance().getClipboardContent();
        assertNotNull(clipboard);
        assertNotNull(clipboard.getText(ClipboardContent.MIME_TEXT));
        assertTrue(clipboard.getText(ClipboardContent.MIME_HTML).contains("<b>bold</b>"));
        assertTrue(clipboard.getText(ClipboardContent.MIME_RTF).startsWith("{\\rtf1"));
        assertTrue(clipboard.getText(ClipboardContent.MIME_MARKDOWN).contains("**bold**"));
        assertTrue(clipboard.getText(ClipboardContent.MIME_ASCIIDOC).contains("*bold*"));
    }

    @Test
    void lightweightFormatsRoundTripEscapesAndLinkedImages() {
        String markdownSource = "[![image](logo.png)](https://example.com) and literal \\*asterisk\\*";
        HtmlImporter.Result markdown = RichTextImporter.parse(markdownSource, RichTextFormat.MARKDOWN);
        int image = markdown.getText().indexOf('\uFFFC');
        assertTrue(image >= 0);
        assertEquals("logo.png", markdown.getImageSources().get(image));
        assertEquals("https://example.com", markdown.getLinks().get(image));
        assertEquals(markdownSource, serialize(markdown, RichTextFormat.MARKDOWN));

        String asciidocSource = "https://example.com[image:logo.png[]] and literal \\*asterisk\\*";
        HtmlImporter.Result asciidoc = RichTextImporter.parse(asciidocSource, RichTextFormat.ASCIIDOC);
        image = asciidoc.getText().indexOf('\uFFFC');
        assertTrue(image >= 0);
        assertEquals("logo.png", asciidoc.getImageSources().get(image));
        assertEquals("https://example.com", asciidoc.getLinks().get(image));
        assertEquals(asciidocSource, serialize(asciidoc, RichTextFormat.ASCIIDOC));
    }

    private static String serialize(HtmlImporter.Result result, RichTextFormat format) {
        EditorDocument doc = new EditorDocument(result.getText());
        InlineStyles inline = new InlineStyles(doc.length());
        for (int i = 0; i < doc.length(); i++) {
            inline.setAt(i, result.getStyles().get(i));
        }
        RichBlocks blocks = new RichBlocks(doc.getLineCount());
        for (int i = 0; i < blocks.count(); i++) {
            RichBlocks.BlockAttr source = result.getBlocks().get(i);
            RichBlocks.BlockAttr target = blocks.get(i);
            target.type = source.type;
            target.listType = source.listType;
        }
        return RichTextSerializer.serialize(doc, inline, blocks, result.getLinks(),
                result.getImageSources(), format);
    }
}
