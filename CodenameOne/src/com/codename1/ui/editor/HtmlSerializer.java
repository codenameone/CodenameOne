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

/// Serializes the pure rich text editor model (text + `InlineStyles` + `RichBlocks`) back into an HTML
/// string for `RichTextArea#getHtml`. The output is model canonical rather than byte identical, which
/// matches how a `contenteditable` surface normalized HTML in the previous backend.
public final class HtmlSerializer {
    private HtmlSerializer() {
    }

    /// Serializes the model into HTML.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the text document
    ///
    /// - `inline`: the inline style model
    ///
    /// - `blocks`: the block attribute model
    public static String serialize(EditorDocument doc, InlineStyles inline, RichBlocks blocks) {
        StringBuilder sb = new StringBuilder();
        String currentList = null;
        int lineCount = doc.getLineCount();
        for (int p = 0; p < lineCount; p++) {
            RichBlocks.BlockAttr b = blocks.get(p);
            String desiredList = b.listType == RichBlocks.LIST_ORDERED ? "ol"
                    : b.listType == RichBlocks.LIST_UNORDERED ? "ul" : null;
            if (!eq(desiredList, currentList)) {
                if (currentList != null) {
                    sb.append("</").append(currentList).append(">");
                }
                if (desiredList != null) {
                    sb.append("<").append(desiredList).append(">");
                }
                currentList = desiredList;
            }
            String tag = desiredList != null ? "li" : tagForType(b.type);
            sb.append("<").append(tag).append(alignAttr(b.align)).append(indentAttr(b.indent)).append(">");
            appendInline(sb, doc, inline, p);
            sb.append("</").append(tag).append(">");
        }
        if (currentList != null) {
            sb.append("</").append(currentList).append(">");
        }
        return sb.toString();
    }

    private static void appendInline(StringBuilder sb, EditorDocument doc, InlineStyles inline, int paragraph) {
        int start = doc.getLineStart(paragraph);
        String text = doc.getLineText(paragraph);
        int col = 0;
        while (col < text.length()) {
            TextStyle st = inline.styleAt(start + col);
            int end = col + 1;
            while (end < text.length() && inline.styleAt(start + end).equals(st)) {
                end++;
            }
            appendRun(sb, st, text.substring(col, end));
            col = end;
        }
    }

    private static void appendRun(StringBuilder sb, TextStyle st, String text) {
        StringBuilder span = new StringBuilder();
        if (st.getForeColor() >= 0) {
            span.append("color:").append(css(st.getForeColor())).append(";");
        }
        if (st.getHighlight() >= 0) {
            span.append("background-color:").append(css(st.getHighlight())).append(";");
        }
        if (st.getFontSizeLevel() > 0) {
            span.append("font-size:").append(fontSizePercent(st.getFontSizeLevel())).append("%;");
        }
        boolean hasSpan = span.length() > 0;
        if (hasSpan) {
            sb.append("<span style=\"").append(span).append("\">");
        }
        if (st.isBold()) {
            sb.append("<b>");
        }
        if (st.isItalic()) {
            sb.append("<i>");
        }
        if (st.isUnderline()) {
            sb.append("<u>");
        }
        if (st.isStrike()) {
            sb.append("<s>");
        }
        sb.append(escape(text));
        if (st.isStrike()) {
            sb.append("</s>");
        }
        if (st.isUnderline()) {
            sb.append("</u>");
        }
        if (st.isItalic()) {
            sb.append("</i>");
        }
        if (st.isBold()) {
            sb.append("</b>");
        }
        if (hasSpan) {
            sb.append("</span>");
        }
    }

    private static String tagForType(int type) {
        if (type >= RichBlocks.H1 && type <= RichBlocks.H1 + 5) {
            return "h" + (type - RichBlocks.H1 + 1);
        }
        if (type == RichBlocks.PRE) {
            return "pre";
        }
        if (type == RichBlocks.BLOCKQUOTE) {
            return "blockquote";
        }
        return "p";
    }

    private static String alignAttr(int align) {
        if (align == RichBlocks.ALIGN_CENTER) {
            return " style=\"text-align:center\"";
        }
        if (align == RichBlocks.ALIGN_RIGHT) {
            return " style=\"text-align:right\"";
        }
        return "";
    }

    private static String indentAttr(int indent) {
        if (indent > 0) {
            return " data-indent=\"" + indent + "\"";
        }
        return "";
    }

    private static int fontSizePercent(int level) {
        switch (level) {
            case 1:
                return 70;
            case 2:
                return 85;
            case 4:
                return 115;
            case 5:
                return 150;
            case 6:
                return 200;
            case 7:
                return 250;
            default:
                return 100;
        }
    }

    private static String css(int rgb) {
        String s = Integer.toHexString(rgb & 0xffffff);
        while (s.length() < 6) {
            s = "0" + s;
        }
        return "#" + s;
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\uFFFC') {
                // object-replacement char for an inline image; images are not serialized to HTML yet
                continue;
            }
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
