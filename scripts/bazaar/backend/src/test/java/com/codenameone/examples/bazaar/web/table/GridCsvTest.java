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
package com.codenameone.examples.bazaar.web.table;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The CSV formula guard leaves plain numbers alone and still guards formulas.
class GridCsvTest {
    @Test
    void plainNumbersAreNotGuarded() {
        assertTrue(Grid.plainNumber("-12.50"));
        assertTrue(Grid.plainNumber("7"));
        assertFalse(Grid.plainNumber("-"));
        assertFalse(Grid.plainNumber("-1."));
        assertFalse(Grid.plainNumber("1.2.3"));
        assertFalse(Grid.plainNumber("-1+1"));
        assertEquals("-12.50", Grid.csvCell("-12.50"));
        assertEquals("'-1+cmd", Grid.csvCell("-1+cmd"));
        assertEquals("'=SUM(A1)", Grid.csvCell("=SUM(A1)"));
    }
}
