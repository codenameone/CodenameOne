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
package com.codenameone.examples.bazaar.net;

/// What the sign-in reads from the server about a second factor, kept apart
/// from [Session] so it can be tested without a network or a screen.
public final class SecondFactor {
    private SecondFactor() {
    }

    /// Whether the password step was held for a second factor: the installed
    /// app sees the redirect to the code page, the app in a browser follows
    /// it and reads the page it ends on (answered 401, `secondFactor`
    /// `required`).
    public static boolean wanted(boolean web, int status, String location, String secondFactor) {
        return web ? status == 401 && "required".equals(secondFactor)
                : location != null && location.indexOf("second-factor") >= 0;
    }

    /// Whether the code step failed: anything but a 200, or a redirect back to
    /// the code page with `error`.
    public static boolean refused(int status, String location) {
        return status != 200 || location != null && location.indexOf("error") >= 0;
    }

    /// The code as typed, with the spaces an authenticator app shows in the
    /// middle of it taken out. Dashes stay: a recovery code has them.
    public static String clean(String typed) {
        if (typed == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < typed.length(); iter++) {
            char c = typed.charAt(iter);
            if (c != ' ' && c != '\t') {
                b.append(c);
            }
        }
        return b.toString();
    }
}
