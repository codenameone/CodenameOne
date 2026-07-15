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
package com.codename1.ui;

/// Clipboard content with both a plain-text fallback and optional rich representations. Platform
/// ports may return this from `Display#getPasteDataFromClipboard()` when the native clipboard exposes
/// multiple MIME types. Applications may also pass it to `Display#copyToClipboard(Object)` for rich
/// round-tripping inside the application.
public final class RichTextClipboardData {
    private String plainText;
    private String html;
    private String rtf;
    private String markdown;
    private String asciidoc;

    /// Creates clipboard data with a required plain-text fallback.
    public RichTextClipboardData(String plainText) {
        this.plainText = plainText == null ? "" : plainText;
    }

    public String getPlainText() {
        return plainText;
    }

    public RichTextClipboardData setPlainText(String value) {
        plainText = value == null ? "" : value;
        return this;
    }

    public String getHtml() {
        return html;
    }

    public RichTextClipboardData setHtml(String value) {
        html = value;
        return this;
    }

    public String getRtf() {
        return rtf;
    }

    public RichTextClipboardData setRtf(String value) {
        rtf = value;
        return this;
    }

    public String getMarkdown() {
        return markdown;
    }

    public RichTextClipboardData setMarkdown(String value) {
        markdown = value;
        return this;
    }

    public String getAsciiDoc() {
        return asciidoc;
    }

    public RichTextClipboardData setAsciiDoc(String value) {
        asciidoc = value;
        return this;
    }

    /// Returns the richest available format, preferring native rich formats over lightweight markup.
    public RichTextFormat getPreferredFormat() {
        if (html != null && html.length() > 0) {
            return RichTextFormat.HTML;
        }
        if (rtf != null && rtf.length() > 0) {
            return RichTextFormat.RTF;
        }
        if (markdown != null && markdown.length() > 0) {
            return RichTextFormat.MARKDOWN;
        }
        if (asciidoc != null && asciidoc.length() > 0) {
            return RichTextFormat.ASCIIDOC;
        }
        return RichTextFormat.PLAIN_TEXT;
    }

    /// Returns the content for the requested format.
    public String getContent(RichTextFormat format) {
        if (format == RichTextFormat.HTML) {
            return html;
        }
        if (format == RichTextFormat.RTF) {
            return rtf;
        }
        if (format == RichTextFormat.MARKDOWN) {
            return markdown;
        }
        if (format == RichTextFormat.ASCIIDOC) {
            return asciidoc;
        }
        return plainText;
    }

    @Override
    public String toString() {
        return plainText;
    }
}
