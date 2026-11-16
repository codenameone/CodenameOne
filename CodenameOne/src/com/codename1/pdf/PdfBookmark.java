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
package com.codename1.pdf;

import java.util.ArrayList;
import java.util.List;

/// An entry of the document outline, the table of contents a reader shows
/// in its side panel. Created by [PdfDocument#addBookmark(String, PdfPage, float)]
/// or, for a nested entry, by [#addChild(String, PdfPage, float)].
@com.codename1.impl.SharedWithBackend
public final class PdfBookmark {
    final String title;
    final PdfPage page;
    final float y;
    final List<PdfBookmark> children = new ArrayList<PdfBookmark>();
    int objectNumber;

    PdfBookmark(String title, PdfPage page, float y) {
        this.title = title;
        this.page = page;
        this.y = y;
    }

    /// Adds a nested entry that jumps to `y` points from the top of `page`.
    public PdfBookmark addChild(String title, PdfPage page, float y) {
        if (page.document() != this.page.document()) { // NOPMD CompareObjectsWithEquals - the same document object is required
            throw new IllegalArgumentException("The page belongs to another document");
        }
        PdfBookmark b = new PdfBookmark(title, page, y);
        children.add(b);
        return b;
    }

    /// The text shown for this entry.
    public String getTitle() {
        return title;
    }
}
