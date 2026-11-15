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
package com.codenameone.examples.brickyard;

/// Time, in epoch milliseconds, in one place so the tests can reason about it.
public final class Clock {
    public static final long MINUTE = 60L * 1000;
    public static final long DAY = 24L * 60 * MINUTE;
    private static final long YEAR = 31556952000L;

    private Clock() {
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    public static long dayOf(long millis) {
        return millis - millis % DAY;
    }

    /// The calendar year `millis` falls in, to the day: close enough for an age
    /// gate, which only ever compares it with a birth year.
    public static int yearOf(long millis) {
        return 1970 + (int) (millis / YEAR);
    }
}
