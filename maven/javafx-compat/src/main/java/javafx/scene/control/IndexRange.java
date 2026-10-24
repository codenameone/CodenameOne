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
package javafx.scene.control;

import javafx.beans.NamedArg;

/// A range of positions between two indexes, the start included and the
/// end not.
public final class IndexRange {

    /// What separates the two numbers in the text form of a range.
    public static final String VALUE_DELIMITER = ",";

    private final int start;
    private final int end;

    /// Creates a range.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the end is before the start
    public IndexRange(@NamedArg("start") int start, @NamedArg("end") int end) {
        if (end < start) {
            throw new IllegalArgumentException("The end of a range must not be before its start");
        }
        this.start = start;
        this.end = end;
    }

    /// Creates a copy of a range.
    public IndexRange(@NamedArg("range") IndexRange range) {
        this.start = range.start;
        this.end = range.end;
    }

    /// Returns the first index of the range.
    public int getStart() {
        return start;
    }

    /// Returns the index after the last one of the range.
    public int getEnd() {
        return end;
    }

    /// Returns the number of indexes in the range.
    public int getLength() {
        return end - start;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof IndexRange) {
            IndexRange other = (IndexRange) object;
            return start == other.start && end == other.end;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * start + end;
    }

    @Override
    public String toString() {
        return start + VALUE_DELIMITER + " " + end;
    }

    /// Creates a range from two indexes in either order.
    public static IndexRange normalize(int v1, int v2) {
        return new IndexRange(Math.min(v1, v2), Math.max(v1, v2));
    }

    /// Parses a range written as `start,end`.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the text is not two integers
    ///   separated by a comma
    public static IndexRange valueOf(String value) {
        if (value == null) {
            throw new IllegalArgumentException("A range is two numbers separated by a comma");
        }
        int comma = value.indexOf(',');
        if (comma < 0 || value.indexOf(',', comma + 1) >= 0) {
            throw new IllegalArgumentException("A range is two numbers separated by a comma");
        }
        int s = Integer.parseInt(value.substring(0, comma).trim());
        int e = Integer.parseInt(value.substring(comma + 1).trim());
        return normalize(s, e);
    }
}
