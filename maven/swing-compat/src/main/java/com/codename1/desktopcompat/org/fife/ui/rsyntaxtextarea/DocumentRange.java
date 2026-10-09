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
package com.codename1.desktopcompat.org.fife.ui.rsyntaxtextarea;

/// A range of a document: from a start offset up to, and not including,
/// an end offset.
public class DocumentRange implements Comparable<DocumentRange> {

    private int startOffs;
    private int endOffs;

    public DocumentRange(int startOffs, int endOffs) {
        set(startOffs, endOffs);
    }

    @Override
    public int compareTo(DocumentRange other) {
        if (other == null) {
            return 1;
        }
        int diff = startOffs - other.startOffs;
        if (diff != 0) {
            return diff;
        }
        return endOffs - other.endOffs;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof DocumentRange) {
            return compareTo((DocumentRange) other) == 0;
        }
        return false;
    }

    public int getEndOffset() {
        return endOffs;
    }

    public int getStartOffset() {
        return startOffs;
    }

    @Override
    public int hashCode() {
        return startOffs + endOffs;
    }

    public boolean isZeroLength() {
        return startOffs == endOffs;
    }

    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the start is negative or after
    ///   the end
    public void set(int start, int end) {
        if (start < 0 || end < 0) {
            throw new IllegalArgumentException("start and end must be >= 0 (" + start + "-" + end + ")");
        }
        if (end < start) {
            throw new IllegalArgumentException("'end' cannot be less than 'start' (" + start + "-" + end + ")");
        }
        this.startOffs = start;
        this.endOffs = end;
    }

    @Override
    public String toString() {
        return "[DocumentRange: " + startOffs + "-" + endOffs + "]";
    }

    /// Moves this range by `amount` and answers this range.
    public DocumentRange translate(int amount) {
        startOffs += amount;
        endOffs += amount;
        return this;
    }
}
