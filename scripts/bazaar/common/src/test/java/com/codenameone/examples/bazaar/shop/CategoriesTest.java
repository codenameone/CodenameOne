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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.bazaar.api.NameCountDto;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategoriesTest {
    @Test
    void biggestCategoriesFirstEmptyOnesDropped() {
        List<String> c = Categories.choices(Arrays.asList(new NameCountDto("Mugs", 3),
                new NameCountDto("Bowls", 7), new NameCountDto("Ghost", 0),
                new NameCountDto("Vases", 3), new NameCountDto("", 4)), Categories.MAX);
        assertEquals(Arrays.asList("Bowls", "Mugs", "Vases"), c);
    }

    @Test
    void capsTheRowAndHidesASingleChoice() {
        assertEquals(Arrays.asList("A", "B"), Categories.choices(Arrays.asList(
                new NameCountDto("A", 5), new NameCountDto("B", 4), new NameCountDto("C", 1)), 2));
        assertTrue(Categories.choices(Collections.singletonList(new NameCountDto("A", 5)), 8)
                .isEmpty());
        assertTrue(Categories.choices(null, 8).isEmpty());
    }

    @Test
    void tappingChoosesAndTappingAgainClears() {
        assertEquals("Mugs", Categories.tap(null, "Mugs"));
        assertEquals("Bowls", Categories.tap("Mugs", "Bowls"));
        assertNull(Categories.tap("Mugs", "Mugs"));
        assertNull(Categories.tap("Mugs", null));
        assertTrue(Categories.chosen(null, null));
        assertFalse(Categories.chosen("Mugs", null));
        assertTrue(Categories.chosen("Mugs", "Mugs"));
    }
}
