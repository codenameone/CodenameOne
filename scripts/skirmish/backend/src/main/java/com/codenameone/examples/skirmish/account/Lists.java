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
package com.codenameone.examples.skirmish.account;

import java.util.ArrayList;
import java.util.List;

/// Comma-separated id lists, the way a profile stores its deck and items.
public final class Lists {
    private Lists() {
    }

    public static String join(String[] items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(items[i]);
        }
        return sb.toString();
    }

    public static String join(List<String> items) {
        return join(items.toArray(new String[items.size()]));
    }

    public static List<String> split(String csv) {
        List<String> out = new ArrayList<String>();
        if (csv == null || csv.length() == 0) {
            return out;
        }
        int start = 0;
        for (int i = 0; i <= csv.length(); i++) {
            if (i == csv.length() || csv.charAt(i) == ',') {
                if (i > start) {
                    out.add(csv.substring(start, i));
                }
                start = i + 1;
            }
        }
        return out;
    }

    public static boolean contains(String csv, String id) {
        return split(csv).contains(id);
    }
}
