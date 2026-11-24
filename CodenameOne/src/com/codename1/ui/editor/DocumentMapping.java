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

import com.codename1.util.Base64;
import com.codename1.util.StringUtil;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Document;
import com.codename1.wordprocessing.EmbeddedComponent;
import com.codename1.wordprocessing.EmbeddedComponents;
import com.codename1.wordprocessing.Field;
import com.codename1.wordprocessing.Inline;
import com.codename1.wordprocessing.Note;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.Table;

import java.util.ArrayList;
import java.util.List;

/// Maps between the editor's flat model and a word processing `Document`, directly: Markdown
/// and RTF are read and written by `com.codename1.wordprocessing`, and an intermediate HTML
/// string would lose what HTML parsing normalizes away (whitespace between styled runs).
///
/// The editor holds less than a document. Tables become one line per row with cells separated
/// by tabs, a note becomes its mark in brackets, a field its result; nothing a document says
/// is dropped as text.
final class DocumentMapping {
    /// The font name the word processing model gives code.
    private static final String MONOSPACE = "Monospace";

    private DocumentMapping() {
    }

    static HtmlImporter.Result toEditor(Document d) {
        Builder out = new Builder();
        for (int i = 0; i < d.size(); i++) {
            Block block = d.get(i);
            if (block instanceof Paragraph) {
                paragraph(out, (Paragraph) block);
            } else if (block instanceof Table) {
                table(out, (Table) block);
            } else if (block instanceof Picture) {
                out.startBlock(new RichBlocks.BlockAttr());
                out.appendImage(source((Picture) block), null);
            }
        }
        return out.result();
    }

    private static void paragraph(Builder out, Paragraph p) {
        RichBlocks.BlockAttr attr = new RichBlocks.BlockAttr();
        if (p.isHeading()) {
            attr.type = RichBlocks.H1 + Math.max(0, Math.min(5, p.headingLevel() - 1));
        } else if ("Quote".equals(p.style)) {
            attr.type = RichBlocks.BLOCKQUOTE;
        } else if ("Code".equals(p.style)) {
            attr.type = RichBlocks.PRE;
        }
        if (Paragraph.BULLET.equals(p.list)) {
            attr.listType = RichBlocks.LIST_UNORDERED;
            attr.indent = p.listLevel;
        } else if (Paragraph.NUMBERED.equals(p.list)) {
            attr.listType = RichBlocks.LIST_ORDERED;
            attr.indent = p.listLevel;
        }
        attr.align = Paragraph.CENTER.equals(p.align) ? RichBlocks.ALIGN_CENTER
                : Paragraph.RIGHT.equals(p.align) ? RichBlocks.ALIGN_RIGHT : RichBlocks.ALIGN_LEFT;
        // a hard line break inside a paragraph is a new editor line in the same block
        RichText text = p.text;
        boolean heading = p.isHeading();
        out.startBlock(attr);
        for (int r = 0; r < text.runCount(); r++) {
            com.codename1.wordprocessing.TextStyle s = text.runStyle(r);
            if (s.revision != null && s.revision.isDeletion()) {
                continue;
            }
            String link = s.link.length() > 0 ? s.link : null;
            Inline object = text.runObject(r);
            if (object instanceof Picture) {
                out.appendImage(source((Picture) object), link);
            } else if (object instanceof EmbeddedComponent) {
                out.appendImage(EmbeddedComponents.toSource((EmbeddedComponent) object), link);
            } else if (object instanceof Note) {
                Note n = (Note) object;
                out.appendText("[" + (n.mark == null ? n.displayText() : n.mark) + "]",
                        style(s, link != null, heading), link, attr);
            } else if (object instanceof Field) {
                out.appendText(((Field) object).result, style(s, link != null, heading), link, attr);
            } else {
                out.appendText(text.runText(r), style(s, link != null, heading), link, attr);
            }
        }
    }

    private static void table(Builder out, Table t) {
        for (int row = 0; row < t.rows(); row++) {
            out.startBlock(new RichBlocks.BlockAttr());
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < t.columns(); col++) {
                if (col > 0) {
                    line.append('\t');
                }
                if (!t.covered(row, col)) {
                    line.append(t.cellText(row, col).replace('\n', ' '));
                }
            }
            out.appendText(line.toString(), TextStyle.DEFAULT, null, null);
        }
    }

    private static TextStyle style(com.codename1.wordprocessing.TextStyle s, boolean link,
            boolean heading) {
        // a heading block is bold already; the readers make its text bold so a page shows it
        TextStyle out = TextStyle.DEFAULT.withBold(s.bold && !heading).withItalic(s.italic)
                .withStrike(s.strike).withMonospace(MONOSPACE.equals(s.font));
        if (!link) {
            // a reader underlines and colors a link so a page shows it; the editor paints its
            // links itself, and keeping those would write them back as formatting
            out = out.withUnderline(s.underline);
            if (s.color != com.codename1.wordprocessing.TextStyle.DEFAULT.color) {
                out = out.withForeColor(s.color);
            }
        }
        if (s.background != com.codename1.wordprocessing.TextStyle.NO_BACKGROUND) {
            out = out.withHighlight(s.background);
        }
        return out;
    }

    private static String source(Picture p) {
        if (p.isLinked()) {
            return p.source;
        }
        return "data:" + p.mimeType + ";base64," + Base64.encodeNoNewline(p.data());
    }

    static Document fromEditor(EditorDocument doc, InlineStyles inline, RichBlocks blocks,
            List<String> links, List<String> imageSources) {
        List<Block> out = new ArrayList<Block>();
        int ids = 0;
        for (int line = 0; line < doc.getLineCount(); line++) {
            RichBlocks.BlockAttr attr = blocks.get(line);
            int start = doc.getLineStart(line);
            String text = doc.getLineText(line);
            RichText.Builder b = new RichText.Builder();
            for (int column = 0; column < text.length();) {
                int offset = start + column;
                String href = valueAt(links, offset);
                String source = valueAt(imageSources, offset);
                com.codename1.wordprocessing.TextStyle style = style(inline.styleAt(offset), href);
                if (text.charAt(column) == '\uFFFC') {
                    Inline object = source == null ? null : image(source, "i" + (++ids));
                    if (object != null) {
                        b.append(object, style);
                    }
                    column++;
                    continue;
                }
                int end = column + 1;
                while (end < text.length() && text.charAt(end) != '\uFFFC'
                        && inline.styleAt(start + end).equals(inline.styleAt(offset))
                        && equal(href, valueAt(links, start + end))) {
                    end++;
                }
                b.append(text.substring(column, end), style);
                column = end;
            }
            out.add(paragraph("p" + (++ids), attr, b.build()));
        }
        return new Document(out, null, "", ids + 1L);
    }

    private static Paragraph paragraph(String id, RichBlocks.BlockAttr attr, RichText text) {
        String style = "Normal";
        if (attr.type >= RichBlocks.H1 && attr.type <= RichBlocks.H1 + 5) {
            style = "Heading" + (attr.type - RichBlocks.H1 + 1);
        } else if (attr.type == RichBlocks.BLOCKQUOTE) {
            style = "Quote";
        } else if (attr.type == RichBlocks.PRE) {
            style = "Code";
        }
        String list = attr.listType == RichBlocks.LIST_ORDERED ? Paragraph.NUMBERED
                : attr.listType == RichBlocks.LIST_UNORDERED ? Paragraph.BULLET : Paragraph.NO_LIST;
        String align = attr.align == RichBlocks.ALIGN_CENTER ? Paragraph.CENTER
                : attr.align == RichBlocks.ALIGN_RIGHT ? Paragraph.RIGHT : Paragraph.LEFT;
        int level = list.length() == 0 ? 0 : Math.max(0, Math.min(8, attr.indent));
        return new Paragraph(id, style, align, list, level, 0, 0, 0, 0, 1.15f, false, false, text);
    }

    private static com.codename1.wordprocessing.TextStyle style(TextStyle s, String href) {
        com.codename1.wordprocessing.TextStyle out = com.codename1.wordprocessing.TextStyle.DEFAULT
                .withBold(s.isBold()).withItalic(s.isItalic()).withUnderline(s.isUnderline())
                .withStrike(s.isStrike());
        if (s.isMonospace()) {
            out = out.withFont(MONOSPACE);
        }
        if (s.getForeColor() >= 0) {
            out = out.withColor(s.getForeColor());
        }
        if (s.getHighlight() >= 0) {
            out = out.withBackground(s.getHighlight());
        }
        return href == null ? out : out.withLink(href);
    }

    private static Inline image(String source, String id) {
        EmbeddedComponent component = EmbeddedComponents.fromSource(source);
        if (component != null) {
            return component;
        }
        if (source.startsWith("data:")) {
            int comma = source.indexOf(',');
            int semi = source.indexOf(";base64");
            byte[] data = comma < 0 || semi < 0 || semi > comma ? null
                    : Base64.decode(StringUtil.getBytes(source.substring(comma + 1)));
            return data == null || data.length == 0 ? null
                    : new Picture(id, data, source.substring(5, semi), 144, 144, null);
        }
        return Picture.linked(id, source, 144, 144, null);
    }

    private static String valueAt(List<String> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private static boolean equal(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /// Accumulates the flat model a `HtmlImporter.Result` holds.
    private static final class Builder {
        @SuppressWarnings("PMD.AvoidStringBufferField")
        private final StringBuilder text = new StringBuilder();
        private final List<TextStyle> styles = new ArrayList<TextStyle>();
        private final List<RichBlocks.BlockAttr> blocks = new ArrayList<RichBlocks.BlockAttr>();
        private final List<String> links = new ArrayList<String>();
        private final List<String> imageSources = new ArrayList<String>();

        void startBlock(RichBlocks.BlockAttr attr) {
            if (!blocks.isEmpty()) {
                add('\n', TextStyle.DEFAULT, null, null);
            }
            blocks.add(attr);
        }

        /// Appends text; a line break in it starts another line carrying `attr`.
        void appendText(String value, TextStyle style, String href, RichBlocks.BlockAttr attr) {
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '\n') {
                    startBlock(copy(attr));
                } else {
                    add(c, style, href, null);
                }
            }
        }

        void appendImage(String source, String href) {
            add('\uFFFC', TextStyle.DEFAULT, href, source);
        }

        private void add(char c, TextStyle style, String href, String source) {
            text.append(c);
            styles.add(style);
            links.add(href);
            imageSources.add(source);
        }

        private static RichBlocks.BlockAttr copy(RichBlocks.BlockAttr a) {
            RichBlocks.BlockAttr b = new RichBlocks.BlockAttr();
            if (a != null) {
                b.type = a.type;
                b.align = a.align;
                b.listType = a.listType;
                b.indent = a.indent;
            }
            return b;
        }

        HtmlImporter.Result result() {
            if (blocks.isEmpty()) {
                blocks.add(new RichBlocks.BlockAttr());
            }
            return new HtmlImporter.Result(text.toString(), styles, blocks, links, imageSources,
                    true);
        }
    }
}
