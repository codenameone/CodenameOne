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
package com.codenameone.examples.bazaar.docs;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The Code 128 width table was typed in by hand, so it is checked by its
/// structure: 107 distinct patterns, each of eleven modules (the stop
/// thirteen), with an even number of bar modules as the symbology requires.
class Code128PatternsTest {
    @Test
    void patternTableIsWellFormed() {
        assertEquals(107, Code128.PATTERNS.length);
        Set<String> seen = new HashSet<String>();
        for (int iter = 0; iter < Code128.PATTERNS.length; iter++) {
            String p = Code128.PATTERNS[iter];
            assertTrue(seen.add(p), "duplicate pattern at " + iter);
            int modules = 0;
            int bars = 0;
            for (int w = 0; w < p.length(); w++) {
                int width = p.charAt(w) - '0';
                assertTrue(width >= 1 && width <= 4, "width at " + iter);
                modules += width;
                if (w % 2 == 0) {
                    bars += width;
                }
            }
            assertEquals(iter == Code128.STOP ? 13 : 11, modules, "modules at " + iter);
            assertEquals(0, bars % 2, "bar modules at " + iter);
        }
    }
}
