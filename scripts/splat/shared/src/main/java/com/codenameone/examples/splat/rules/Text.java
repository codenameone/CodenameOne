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
package com.codenameone.examples.splat.rules;

import java.util.ArrayList;
import java.util.List;

/// String helpers. `String.split` is a regex on the JDK and absent from the
/// device's class library, so the rules split by hand.
public final class Text {
    private Text() {
    }

    /// Splits on single spaces, dropping empty pieces.
    public static List<String> words(String s) {
        List<String> out = new ArrayList<String>();
        int start = -1;
        for (int i = 0; i <= s.length(); i++) {
            boolean sep = i == s.length() || s.charAt(i) == ' ';
            if (sep) {
                if (start >= 0) {
                    out.add(s.substring(start, i));
                    start = -1;
                }
            } else if (start < 0) {
                start = i;
            }
        }
        return out;
    }

    /// Exactly `count` integers separated by spaces.
    public static int[] ints(String s, int count) {
        List<String> w = words(s);
        if (w.size() != count) {
            throw new IllegalArgumentException("expected " + count + " values: " + s);
        }
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            out[i] = Integer.parseInt(w.get(i));
        }
        return out;
    }

    /// The pieces of `s` between occurrences of `sep`, empty ones kept.
    public static List<String> split(String s, char sep) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == sep) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out;
    }
}
