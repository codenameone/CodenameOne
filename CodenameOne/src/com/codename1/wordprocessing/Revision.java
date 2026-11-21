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

/// A tracked change on a run of text: who inserted or deleted it and when. A deleted run stays in
/// the text until the revision is accepted or rejected, and readers of the document's plain text
/// skip it.
public final class Revision {
    /// Text that was inserted.
    public static final String INSERT = "insert";

    /// Text that was deleted.
    public static final String DELETE = "delete";

    /// The revision's id.
    public final String id;

    /// `INSERT` or `DELETE`.
    public final String kind;

    /// Who made the change.
    public final String author;

    /// When, as an ISO 8601 date and time, or empty.
    public final String date;

    /// Creates a revision.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id, or null
    ///
    /// - `kind`: `INSERT` or `DELETE`; anything else means `INSERT`
    ///
    /// - `author`: the author, or null
    ///
    /// - `date`: the date, or null
    public Revision(String id, String kind, String author, String date) {
        this.id = id == null ? "" : id;
        this.kind = DELETE.equals(kind) ? DELETE : INSERT;
        this.author = author == null ? "" : author;
        this.date = date == null ? "" : date;
    }

    /// Whether this is a deletion.
    ///
    /// #### Returns
    ///
    /// true for `DELETE`
    public boolean isDeletion() {
        return DELETE.equals(kind);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Revision)) {
            return false;
        }
        Revision r = (Revision) o;
        return id.equals(r.id) && kind.equals(r.kind) && author.equals(r.author) && date.equals(r.date);
    }

    @Override
    public int hashCode() {
        return (id.hashCode() * 31 + kind.hashCode()) * 31 + author.hashCode();
    }
}
