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
package com.codenameone.examples.reel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Reading the rows of an aggregate query, whose columns come back as whatever
/// number type the database driver chose, and the comma lists some columns hold.
public final class Rows {
    private Rows() {
    }

    public static long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    public static long number(Long value) {
        return value == null ? 0L : value.longValue();
    }

    public static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /// The first column of each row as a key and the second as its count.
    public static Map<String, Long> counts(List<Object[]> rows) {
        Map<String, Long> out = new HashMap<String, Long>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Object[] row = rows.get(iter);
            out.put(text(row[0]), Long.valueOf(number(row[1])));
        }
        return out;
    }

    /// A comma-separated column as a list, without empty entries.
    public static List<String> split(String value) {
        List<String> out = new ArrayList<String>();
        if (value == null || value.length() == 0) {
            return out;
        }
        int start = 0;
        while (start <= value.length()) {
            int comma = value.indexOf(',', start);
            int end = comma < 0 ? value.length() : comma;
            String part = value.substring(start, end).trim();
            if (part.length() > 0) {
                out.add(part);
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        return out;
    }

    /// A list as a comma-separated column. Commas inside an entry are dropped,
    /// since they would split it on the way back.
    public static String join(List<String> values) {
        StringBuilder out = new StringBuilder();
        if (values == null) {
            return "";
        }
        for (int iter = 0; iter < values.size(); iter++) {
            String v = values.get(iter);
            if (v == null) {
                continue;
            }
            v = v.replace(",", "").trim();
            if (v.length() == 0) {
                continue;
            }
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(v);
        }
        return out.toString();
    }

    /// Comma-separated integers, skipping anything that is not one.
    public static List<Integer> ints(String value) {
        List<Integer> out = new ArrayList<Integer>();
        List<String> parts = split(value);
        for (int iter = 0; iter < parts.size(); iter++) {
            try {
                out.add(Integer.valueOf(Integer.parseInt(parts.get(iter))));
            } catch (NumberFormatException skipped) {
                // not a number: not an answer
            }
        }
        return out;
    }

    public static String joinInts(List<Integer> values) {
        List<String> parts = new ArrayList<String>();
        if (values != null) {
            for (int iter = 0; iter < values.size(); iter++) {
                if (values.get(iter) != null) {
                    parts.add(String.valueOf(values.get(iter).intValue()));
                }
            }
        }
        return join(parts);
    }

    /// A list as one entry per line, for entries that may hold commas (the
    /// options of a quiz question). Line breaks inside an entry become spaces.
    public static String joinLines(List<String> values) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; values != null && iter < values.size(); iter++) {
            String v = values.get(iter) == null ? "" : values.get(iter).replace('\r', ' ').replace('\n', ' ').trim();
            if (v.length() == 0) {
                continue;
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(v);
        }
        return out.toString();
    }

    public static List<String> splitLines(String value) {
        List<String> out = new ArrayList<String>();
        if (value == null || value.length() == 0) {
            return out;
        }
        String[] parts = value.split("\n");
        for (int iter = 0; iter < parts.length; iter++) {
            if (parts[iter].trim().length() > 0) {
                out.add(parts[iter].trim());
            }
        }
        return out;
    }
}
