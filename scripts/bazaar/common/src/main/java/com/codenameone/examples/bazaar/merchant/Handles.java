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
package com.codenameone.examples.bazaar.merchant;

/// URL handles made from titles: ASCII letters and digits, folded to lower
/// case by hand (never `toLowerCase()`, which is locale sensitive), every run
/// of anything else one dash.
public final class Handles {
    private Handles() {
    }

    public static String of(String title) {
        StringBuilder b = new StringBuilder();
        boolean dash = false;
        for (int iter = 0; title != null && iter < title.length(); iter++) {
            char c = title.charAt(iter);
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + ('a' - 'A'));
            }
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                if (dash && b.length() > 0) {
                    b.append('-');
                }
                dash = false;
                b.append(c);
            } else {
                dash = true;
            }
        }
        return b.length() == 0 ? "item" : b.toString();
    }
}
