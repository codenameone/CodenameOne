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

/// Clipboard content with both a plain-text fallback and optional rich representations. Platform
/// ports may return this from `Display#getPasteDataFromClipboard()` when the native clipboard exposes
/// multiple MIME types. Applications may also pass it to `Display#copyToClipboard(Object)` for rich
/// round-tripping inside the application.
public final class RichTextClipboardData extends ClipboardContent {

    /// Creates clipboard data with a required plain-text fallback.
    public RichTextClipboardData(String plainText) {
        setData(MIME_TEXT, plainText == null ? "" : plainText);
    }

    public String getPlainText() {
        return getText(MIME_TEXT);
    }

    public RichTextClipboardData setPlainText(String value) {
        setData(MIME_TEXT, value == null ? "" : value);
        return this;
    }

    public String getHtml() {
        return getText(MIME_HTML);
    }

    public RichTextClipboardData setHtml(String value) {
        setData(MIME_HTML, value);
        return this;
    }

    public String getRtf() {
        return getText(MIME_RTF);
    }

    public RichTextClipboardData setRtf(String value) {
        setData(MIME_RTF, value);
        return this;
    }

    public String getMarkdown() {
        return getText(MIME_MARKDOWN);
    }

    public RichTextClipboardData setMarkdown(String value) {
        setData(MIME_MARKDOWN, value);
        return this;
    }

    public String getAsciiDoc() {
        return getText(MIME_ASCIIDOC);
    }

    public RichTextClipboardData setAsciiDoc(String value) {
        setData(MIME_ASCIIDOC, value);
        return this;
    }

    /// Returns the richest available format, preferring native rich formats over lightweight markup.
    public RichTextFormat getPreferredFormat() {
        if (getHtml() != null && getHtml().length() > 0) {
            return RichTextFormat.HTML;
        }
        if (getRtf() != null && getRtf().length() > 0) {
            return RichTextFormat.RTF;
        }
        if (getMarkdown() != null && getMarkdown().length() > 0) {
            return RichTextFormat.MARKDOWN;
        }
        if (getAsciiDoc() != null && getAsciiDoc().length() > 0) {
            return RichTextFormat.ASCIIDOC;
        }
        return RichTextFormat.PLAIN_TEXT;
    }

    /// Returns the content for the requested format.
    public String getContent(RichTextFormat format) {
        if (format == RichTextFormat.HTML) {
            return getHtml();
        }
        if (format == RichTextFormat.RTF) {
            return getRtf();
        }
        if (format == RichTextFormat.MARKDOWN) {
            return getMarkdown();
        }
        if (format == RichTextFormat.ASCIIDOC) {
            return getAsciiDoc();
        }
        return getPlainText();
    }

    @Override
    public String toString() {
        return getPlainText();
    }
}
