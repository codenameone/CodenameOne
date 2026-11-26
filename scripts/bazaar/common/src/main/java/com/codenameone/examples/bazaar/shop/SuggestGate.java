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
package com.codenameone.examples.bazaar.shop;

/// Which predictive-search answer may still be shown. Every request takes a
/// ticket; an answer is shown only while its ticket is the newest, so a slow
/// answer for "mu" never replaces the one for "mug" that came back first.
/// Clearing the field (or searching) invalidates every ticket out. EDT only.
final class SuggestGate {
    /// The fewest characters worth asking about.
    static final int MIN_CHARS = 2;

    private int newest;

    /// True when `text` is long enough to ask about.
    static boolean worthAsking(String text) {
        return text != null && text.trim().length() >= MIN_CHARS;
    }

    /// A ticket for a request about to go out.
    int issue() {
        return ++newest;
    }

    /// Drops every answer still on its way.
    void invalidate() {
        newest++;
    }

    /// True when the answer for `ticket` is the one to show.
    boolean current(int ticket) {
        return ticket == newest;
    }
}
