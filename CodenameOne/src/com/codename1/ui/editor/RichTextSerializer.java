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
import com.codename1.wordprocessing.Document;
import com.codename1.wordprocessing.RichTextDocuments;

import java.util.List;

/// Serializes the native rich-text model directly to its requested interchange format.
public final class RichTextSerializer {
    private RichTextSerializer() {
    }

    /// Serializes a native editor document into the requested interchange format.
    public static String serialize(EditorDocument doc, InlineStyles inline, RichBlocks blocks,
            List<String> links, List<String> imageSources, RichTextFormat format) {
        if (format == RichTextFormat.HTML) {
            return HtmlSerializer.serialize(doc, inline, blocks, links, imageSources);
        }
        if (format == RichTextFormat.MARKDOWN || format == RichTextFormat.RTF) {
            // written by com.codename1.wordprocessing, the one Markdown and RTF writer
            Document d = DocumentMapping.fromEditor(doc, inline, blocks, links, imageSources);
            if (format == RichTextFormat.RTF) {
                return RichTextDocuments.writeRtf(d);
            }
            // a document ends its last block with a newline; editor content does not
            String md = RichTextDocuments.writeMarkdown(d);
            int end = md.length();
            while (end > 0 && md.charAt(end - 1) == '\n') {
                end--;
            }
            return md.substring(0, end);
        }
        if (format == RichTextFormat.ASCIIDOC) {
            return serializeAsciiDoc(doc, inline, blocks, links, imageSources);
        }
        return doc.getText().replace("\uFFFC", "");
    }

    private static String serializeAsciiDoc(EditorDocument doc, InlineStyles inline, RichBlocks blocks,
            List<String> links, List<String> imageSources) {
        StringBuilder out = new StringBuilder();
        boolean inPre = false;
        for (int line = 0; line < doc.getLineCount(); line++) {
            RichBlocks.BlockAttr block = blocks.get(line);
            boolean pre = block.type == RichBlocks.PRE;
            if (pre != inPre) {
                if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
                    out.append('\n');
                }
                out.append("----\n");
                inPre = pre;
            }
            if (line > 0 && out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
                out.append('\n');
            }
            if (!pre) {
                appendBlockPrefix(out, block);
            }
            appendInline(out, doc, inline, links, imageSources, line, pre);
        }
        if (inPre) {
            if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
                out.append('\n');
            }
            out.append("----");
        }
        return out.toString();
    }

    private static void appendBlockPrefix(StringBuilder out, RichBlocks.BlockAttr block) {
        for (int i = 0; i < block.indent; i++) {
            out.append("  ");
        }
        if (block.listType == RichBlocks.LIST_ORDERED) {
            out.append(". ");
        } else if (block.listType == RichBlocks.LIST_UNORDERED) {
            out.append("* ");
        } else if (block.type >= RichBlocks.H1 && block.type <= RichBlocks.H1 + 5) {
            char marker = '=';
            int level = block.type - RichBlocks.H1 + 1;
            for (int i = 0; i < level; i++) {
                out.append(marker);
            }
            out.append(' ');
        } else if (block.type == RichBlocks.BLOCKQUOTE) {
            out.append("> ");
        }
    }

    private static void appendInline(StringBuilder out, EditorDocument doc, InlineStyles inline,
            List<String> links, List<String> imageSources, int line, boolean literal) {
        int start = doc.getLineStart(line);
        String text = doc.getLineText(line);
        int column = 0;
        while (column < text.length()) {
            int offset = start + column;
            String source = valueAt(imageSources, offset);
            String href = valueAt(links, offset);
            if (text.charAt(column) == '\uFFFC' && source != null) {
                String image = "image:" + source + "[]";
                if (href != null) {
                    out.append(href + "[" + image + "]");
                } else {
                    out.append(image);
                }
                column++;
                continue;
            }
            TextStyle style = inline.styleAt(offset);
            int end = column + 1;
            while (end < text.length() && text.charAt(end) != '\uFFFC'
                    && style.equals(inline.styleAt(start + end))
                    && equal(href, valueAt(links, start + end))) {
                end++;
            }
            String value = text.substring(column, end);
            if (!literal) {
                if (style.isMonospace()) {
                    value = "+" + escapeInlineCode(value) + "+";
                } else {
                    value = escapeMarkup(value);
                }
                if (style.isStrike()) {
                    value = "[line-through]#" + value + "#";
                }
                if (style.isUnderline()) {
                    value = "[underline]#" + value + "#";
                }
                if (style.isItalic()) {
                    value = "_" + value + "_";
                }
                if (style.isBold()) {
                    value = "*" + value + "*";
                }
                if (href != null) {
                    value = href + "[" + value + "]";
                }
            }
            out.append(value);
            column = end;
        }
    }

    private static String escapeMarkup(String value) {
        String specials = "\\*_[#+";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (specials.indexOf(c) >= 0) {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String escapeInlineCode(String value) {
        char delimiter = '+';
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == delimiter || c == '\\') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String valueAt(List<String> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private static boolean equal(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
