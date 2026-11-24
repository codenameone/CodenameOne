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

/// A review comment anchored to a range of the document: from an offset in one paragraph to an
/// offset in the same or a later one. Paragraphs are named by id, so the anchor survives edits
/// elsewhere.
public final class Comment {
    /// The comment's id, unique within the document.
    public final String id;

    /// Who wrote it.
    public final String author;

    /// When, as an ISO 8601 date and time, or empty.
    public final String date;

    /// The comment's text; paragraphs are separated by newlines.
    public final String text;

    /// Whether the discussion is marked done.
    public final boolean resolved;

    /// The id of the paragraph the range starts in.
    public final String startId;

    /// The offset in that paragraph.
    public final int start;

    /// The id of the paragraph the range ends in.
    public final String endId;

    /// The offset in that paragraph, exclusive.
    public final int end;

    /// Creates a comment.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `author`: the author, or null
    ///
    /// - `date`: the date, or null
    ///
    /// - `text`: the text, or null
    ///
    /// - `resolved`: whether it is done
    ///
    /// - `startId`: the paragraph the range starts in
    ///
    /// - `start`: the offset there
    ///
    /// - `endId`: the paragraph the range ends in
    ///
    /// - `end`: the offset there, exclusive
    public Comment(String id, String author, String date, String text, boolean resolved,
            String startId, int start, String endId, int end) {
        if (id == null || id.length() == 0 || startId == null || endId == null) {
            throw new IllegalArgumentException("Comment id and anchors are required");
        }
        this.id = id;
        this.author = author == null ? "" : author;
        this.date = date == null ? "" : date;
        this.text = text == null ? "" : text;
        this.resolved = resolved;
        this.startId = startId;
        this.start = Math.max(0, start);
        this.endId = endId;
        this.end = Math.max(0, end);
    }

    /// A copy marked resolved or open.
    ///
    /// #### Parameters
    ///
    /// - `value`: whether it is done
    ///
    /// #### Returns
    ///
    /// the comment
    public Comment withResolved(boolean value) {
        return new Comment(id, author, date, text, value, startId, start, endId, end);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Comment)) {
            return false;
        }
        Comment c = (Comment) o;
        return id.equals(c.id) && author.equals(c.author) && date.equals(c.date) && text.equals(c.text)
                && resolved == c.resolved && startId.equals(c.startId) && start == c.start
                && endId.equals(c.endId) && end == c.end;
    }

    @Override
    public int hashCode() {
        return id.hashCode() * 31 + text.hashCode();
    }
}
