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
        List<String> parts = split(value, '\n');
        for (int iter = 0; iter < parts.size(); iter++) {
            if (parts.get(iter).trim().length() > 0) {
                out.add(parts.get(iter).trim());
            }
        }
        return out;
    }

    // The compiled server's class library has no regular expressions, so no
    // String.split or replaceAll: these are the cuts the server needs, by hand.

    /// `value` cut at every `separator`, as `String.split` cuts it: inner
    /// empty pieces kept, trailing ones dropped.
    public static List<String> split(String value, char separator) {
        List<String> out = new ArrayList<String>();
        int from = 0;
        for (;;) {
            int at = value.indexOf(separator, from);
            if (at < 0) {
                out.add(value.substring(from));
                while (out.size() > 1 && out.get(out.size() - 1).length() == 0) {
                    out.remove(out.size() - 1);
                }
                return out;
            }
            out.add(value.substring(from, at));
            from = at + 1;
        }
    }

    /// The runs of ASCII letters and digits in `folded` (already lower case),
    /// in order; everything else separates them.
    public static List<String> words(String folded) {
        List<String> out = new ArrayList<String>();
        int start = -1;
        for (int iter = 0; iter <= folded.length(); iter++) {
            char c = iter < folded.length() ? folded.charAt(iter) : ' ';
            boolean word = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (word && start < 0) {
                start = iter;
            } else if (!word && start >= 0) {
                out.add(folded.substring(start, iter));
                start = -1;
            }
        }
        return out;
    }

    /// The UTC calendar day of `millis` as `yyyy-MM-dd`, computed rather than
    /// formatted: the compiled runtime's date formatting has no time zones.
    public static String utcDay(long millis) {
        long days = Math.floorDiv(millis, 86400000L);
        // Howard Hinnant's days-to-civil, for the proleptic Gregorian calendar.
        long z = days + 719468;
        long era = Math.floorDiv(z, 146097);
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        long y = yoe + era * 400 + (m <= 2 ? 1 : 0);
        return y + "-" + (m < 10 ? "0" : "") + m + "-" + (d < 10 ? "0" : "") + d;
    }
}
