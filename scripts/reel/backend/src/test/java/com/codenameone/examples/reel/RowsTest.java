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
package com.codenameone.examples.reel;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The hand-written cuts the compiled server uses in place of regular
/// expressions, held to what String.split and SimpleDateFormat answered.
class RowsTest {
    @Test
    void splitCutsLikeStringSplit() {
        assertEquals(Arrays.asList("a", "", "b"), Rows.split("a::b", ':'));
        assertEquals(Arrays.asList("a", "b"), Rows.split("a:b::", ':'));
        assertEquals(Arrays.asList(""), Rows.split("", ':'));
        assertEquals(Arrays.asList("#EXTM3U", "v720.m3u8"), Rows.split("#EXTM3U\nv720.m3u8\n", '\n'));
    }

    @Test
    void wordsAreTheRunsOfLettersAndDigits() {
        assertEquals(Arrays.asList("big", "buck", "bunny", "2008"), Rows.words("  big-buck, bunny (2008)!"));
        assertEquals(Arrays.asList(), Rows.words("--"));
    }

    @Test
    void utcDayIsTheCalendarDay() {
        assertEquals("1970-01-01", Rows.utcDay(0L));
        assertEquals("1969-12-31", Rows.utcDay(-1L));
        assertEquals("2026-03-01", Rows.utcDay(20513L * 86400000L));
        assertEquals("2024-02-29", Rows.utcDay(19782L * 86400000L + 86399999L));
    }
}
