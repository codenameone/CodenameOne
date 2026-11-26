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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/// The publishing schedule's UTC times, typed and shown.
class UtcTimesTest {
    @Test
    void readsAndWritesTheTypedForm() {
        // 2026-03-01 14:05 UTC.
        long t = 1772373900000L;
        assertEquals("2026-03-01 14:05", UtcTimes.format(t));
        assertEquals(t, UtcTimes.parse("2026-03-01 14:05"));
        assertEquals(t, UtcTimes.parse(" 2026-03-01T14:05 "));
        assertEquals(1772323200000L, UtcTimes.parse("2026-03-01"));
        assertEquals(951782400000L, UtcTimes.parse("2000-02-29"));
        assertEquals("2000-02-29 00:00", UtcTimes.format(951782400000L));
    }

    @Test
    void emptyIsNoTimeAndNonsenseIsRefused() {
        assertEquals("", UtcTimes.format(0));
        assertEquals(0, UtcTimes.parse(""));
        assertEquals(0, UtcTimes.parse(null));
        assertEquals(-1, UtcTimes.parse("2026-02-30"));
        assertEquals(-1, UtcTimes.parse("2025-02-29 10:00"));
        assertEquals(-1, UtcTimes.parse("2026-03-01 24:00"));
        assertEquals(-1, UtcTimes.parse("01/03/2026"));
        assertEquals(-1, UtcTimes.parse("2026-3-1 1:05"));
    }
}
