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
package com.codenameone.examples.papercut.run;

import com.codename1.backend.ResponseStatusException;

/// The three ways to play, and the periods a board covers.
public final class Modes {
    public static final String CLASSIC = "CLASSIC";
    public static final String FOLD = "FOLD";
    public static final String DAILY = "DAILY";

    public static final String ALL = "ALL";
    public static final String WEEK = "WEEK";
    public static final String TODAY = "TODAY";
    public static final String FRIENDS = "FRIENDS";

    private Modes() {
    }

    /// `value` as one of the modes, compared without folding case by locale.
    public static String mode(String value) {
        String[] all = {CLASSIC, FOLD, DAILY};
        for (String m : all) {
            if (m.equalsIgnoreCase(value)) {
                return m;
            }
        }
        throw new ResponseStatusException(400, "Mode is CLASSIC, FOLD or DAILY");
    }

    public static String period(String value) {
        if (value == null || value.length() == 0) {
            return ALL;
        }
        String[] all = {ALL, WEEK, TODAY, FRIENDS};
        for (String p : all) {
            if (p.equalsIgnoreCase(value)) {
                return p;
            }
        }
        throw new ResponseStatusException(400, "Period is ALL, WEEK, TODAY or FRIENDS");
    }
}
