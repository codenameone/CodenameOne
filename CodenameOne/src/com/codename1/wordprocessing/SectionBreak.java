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
package com.codename1.wordprocessing;

/// Starts a new section: the blocks after it, up to the next break, are laid out on the page
/// this break gives, with its margins, orientation and columns. The blocks before the first
/// break use the document's own page.
///
/// A break that does not start a new page (`continuous`) only changes the columns; the text
/// before it is balanced across its own columns first, as a word processor does, so a page can
/// carry a one-column heading over three-column text.
public final class SectionBreak implements Block {
    /// The block id.
    public final String id;

    /// The page of the section that starts here.
    public final PageSpec page;

    /// Whether the section starts on a new page; false continues on the current one.
    public final boolean newPage;

    /// Creates a break.
    ///
    /// #### Parameters
    ///
    /// - `id`: the block id
    ///
    /// - `page`: the page of the section that starts here, or null for A4
    ///
    /// - `newPage`: whether the section starts on a new page
    public SectionBreak(String id, PageSpec page, boolean newPage) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("Section break id is required");
        }
        this.id = id;
        this.page = page == null ? PageSpec.A4 : page;
        this.newPage = newPage;
    }

    /// The block id.
    ///
    /// #### Returns
    ///
    /// the id
    @Override
    public String id() {
        return id;
    }

    /// Two breaks are equal when their id, page and start are.
    ///
    /// #### Parameters
    ///
    /// - `o`: the other object
    ///
    /// #### Returns
    ///
    /// true when equal
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SectionBreak)) {
            return false;
        }
        SectionBreak b = (SectionBreak) o;
        return id.equals(b.id) && page.equals(b.page) && newPage == b.newPage;
    }

    /// A hash consistent with `equals`.
    ///
    /// #### Returns
    ///
    /// the hash
    @Override
    public int hashCode() {
        return id.hashCode() * 31 + page.hashCode() + (newPage ? 1 : 0);
    }
}
