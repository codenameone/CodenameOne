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
package com.codenameone.examples.bastion.api;

/// How the server words a refusal so the app can say it in the user's
/// language: a stable code, the values the sentence names, and the English
/// sentence for any client that does not know the code.
///
/// ```
/// (payee-already-list) That payee is already on your list
/// (address-wrong-asset|BTC) That address is for BTC
/// ```
///
/// The code is what the app looks up (`error.<code>` in its bundle, whose
/// English entry is the sentence with `{0}`, `{1}`... where the values go);
/// it never changes once shipped, while the English may be reworded freely. A
/// value never contains `|` or `)`: they are asset ids, statuses and numbers.
/// Parentheses, not brackets: a body that starts with `[` reads as JSON to
/// a client that sniffs it.
///
/// Read with `indexOf` and `substring` only, since the app's class library
/// has no `String.split`.
public final class Refusal {
    private Refusal() {
    }

    /// The wire form of a refusal. `english` is the sentence with `{0}`...
    /// for `values`, filled in here.
    public static String format(String code, String english, Object... values) {
        StringBuilder out = new StringBuilder("(").append(code);
        String[] texts = new String[values.length];
        for (int iter = 0; iter < values.length; iter++) {
            texts[iter] = String.valueOf(values[iter]);
            out.append('|').append(texts[iter]);
        }
        return out.append(") ").append(fill(english, texts)).toString();
    }

    /// The code of a refusal body, or null when the body is not one.
    public static String code(String body) {
        int end = end(body);
        if (end < 0) {
            return null;
        }
        int bar = body.indexOf('|');
        return body.substring(1, bar > 0 && bar < end ? bar : end);
    }

    /// The values a refusal names, in order.
    public static String[] values(String body) {
        int end = end(body);
        if (end < 0) {
            return new String[0];
        }
        int count = 0;
        for (int iter = 1; iter < end; iter++) {
            if (body.charAt(iter) == '|') {
                count++;
            }
        }
        String[] out = new String[count];
        int at = body.indexOf('|');
        for (int iter = 0; iter < count; iter++) {
            int next = body.indexOf('|', at + 1);
            if (next < 0 || next > end) {
                next = end;
            }
            out[iter] = body.substring(at + 1, next);
            at = next;
        }
        return out;
    }

    /// The English sentence of a refusal, or the whole body when it is not one.
    public static String english(String body) {
        int end = end(body);
        if (end < 0) {
            return body;
        }
        return body.substring(Math.min(body.length(), end + 2));
    }

    /// `pattern` with each `{n}` replaced by `values[n]`.
    public static String fill(String pattern, String[] values) {
        String text = pattern;
        for (int iter = 0; iter < values.length; iter++) {
            String mark = "{" + iter + "}";
            int at = text.indexOf(mark);
            while (at >= 0) {
                text = text.substring(0, at) + values[iter] + text.substring(at + mark.length());
                at = text.indexOf(mark, at + values[iter].length());
            }
        }
        return text;
    }

    private static int end(String body) {
        if (body == null || body.length() < 3 || body.charAt(0) != '(') {
            return -1;
        }
        int end = body.indexOf(')');
        // A code has no spaces, so a bracket after the first space is part of
        // a sentence, not the end of a code.
        int space = body.indexOf(' ');
        return end > 1 && (space < 0 || space > end) ? end : -1;
    }
}
