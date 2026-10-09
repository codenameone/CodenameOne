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
package com.codename1.desktopcompat.org.fife.ui.rtextarea;

import com.codename1.desktopcompat.org.fife.ui.rsyntaxtextarea.DocumentRange;

/// What a [SearchEngine] operation did: the range it selected, how many
/// occurrences it found or replaced, and how many it marked.
public class SearchResult implements Comparable<SearchResult> {

    private DocumentRange matchRange;
    private int count;
    private int markedCount;
    private boolean wrapped;

    public SearchResult() {
        this(null, 0, 0);
    }

    public SearchResult(DocumentRange range, int count, int markedCount) {
        this.matchRange = range;
        this.count = count;
        this.markedCount = markedCount;
    }

    @Override
    public int compareTo(SearchResult other) {
        if (other == null) {
            return 1;
        }
        if (other == this) {
            return 0;
        }
        int diff = count - other.count;
        if (diff != 0) {
            return diff;
        }
        diff = markedCount - other.markedCount;
        if (diff != 0) {
            return diff;
        }
        if (matchRange == null) {
            return other.matchRange == null ? 0 : -1;
        }
        return matchRange.compareTo(other.matchRange);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof SearchResult) {
            return compareTo((SearchResult) other) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = count + markedCount;
        if (matchRange != null) {
            hash += matchRange.hashCode();
        }
        return hash;
    }

    public int getCount() {
        return count;
    }

    public int getMarkedCount() {
        return markedCount;
    }

    public DocumentRange getMatchRange() {
        return matchRange;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setMarkedCount(int markedCount) {
        this.markedCount = markedCount;
    }

    public void setMatchRange(DocumentRange range) {
        this.matchRange = range;
    }

    public void setWrapped(boolean wrapped) {
        this.wrapped = wrapped;
    }

    /// Whether the search reached the end of the text and went on from
    /// the other end.
    public boolean isWrapped() {
        return wrapped;
    }

    @Override
    public String toString() {
        return "[SearchResult: count=" + count + ", markedCount=" + markedCount
                + ", matchRange=" + matchRange + "]";
    }

    /// Whether anything was found or replaced.
    public boolean wasFound() {
        return count > 0;
    }
}
