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
package com.codenameone.examples.bazaar.web.commerce;

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;

/// Reading the account pages' form fields: trimmed text and whole numbers,
/// with a 400 the page can show for anything that is not one.
final class AdminForms {
    private AdminForms() {
    }

    /// A form or query field, trimmed; empty when missing or malformed.
    static String field(HttpServer.Request request, String name) {
        String v;
        try {
            v = request.param(name);
        } catch (RuntimeException malformed) {
            v = null;
        }
        return v == null ? "" : v.trim();
    }

    /// A whole number, possibly negative; `fallback` when empty.
    static int number(String text, int fallback, String what) {
        String t = text == null ? "" : text.trim();
        if (t.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException bad) {
            throw new ResponseStatusException(400, "Write " + what + " as a whole number");
        }
    }
}
