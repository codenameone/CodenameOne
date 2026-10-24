/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.parley;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Reading the rows a repository answers for a select of several values,
/// `select d.platform, count(d) ...`, which come back as `Object[]`.
///
/// What a column holds is checked and never cast blindly: a count is a `Long`
/// on one database and an `Integer` on another, and a name is read as text
/// whatever type the column came back as.
public final class Rows {
    private Rows() {
    }

    /// A column as a whole number; 0 for null or anything that is not a
    /// number.
    public static long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    /// A timestamp column as epoch milliseconds. Anything but an `Instant`
    /// means the query selected something other than a timestamp, which is
    /// a bug to see rather than a zero to chart.
    public static long millis(Object value) {
        if (value instanceof Instant) {
            return ((Instant) value).toEpochMilli();
        }
        throw new IllegalStateException("Expected a java.time.Instant column but got "
                + (value == null ? "null" : value.getClass().getName()));
    }

    /// A column as text; empty for null.
    public static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /// Rows of `{name, count}` as a map from the name to the count.
    public static Map<String, Long> counts(List<Object[]> rows) {
        Map<String, Long> out = new HashMap<String, Long>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Object[] row = rows.get(iter);
            out.put(text(row[0]), Long.valueOf(number(row[1])));
        }
        return out;
    }

    /// A single number a repository answers, which is null for a sum or a
    /// maximum of no rows.
    public static long number(Long value) {
        return value == null ? 0L : value.longValue();
    }
}
