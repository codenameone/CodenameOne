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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.bazaar.api.CategoryCountDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategoriesTest {
    private static List<String> names(List<CategoryCountDto> list) {
        List<String> out = new ArrayList<String>();
        for (CategoryCountDto c : list) {
            out.add(c.name());
        }
        return out;
    }

    @Test
    void offersTheCategoriesWithProductsBiggestFirst() {
        List<CategoryCountDto> c = Categories.choices(Arrays.asList(
                new CategoryCountDto(1, "Mugs", 3), new CategoryCountDto(2, "Bowls", 7),
                new CategoryCountDto(3, "Ghost", 0), new CategoryCountDto(4, "Vases", 3),
                new CategoryCountDto(5, "", 4), new CategoryCountDto(0, "Nowhere", 9)),
                Categories.MAX);
        assertEquals(Arrays.asList("Bowls", "Mugs", "Vases"), names(c));
        assertEquals(2, c.get(0).id());
    }

    @Test
    void capsTheRowAndDropsARowOfOne() {
        assertEquals(Arrays.asList("A", "B"), names(Categories.choices(Arrays.asList(
                new CategoryCountDto(1, "A", 5), new CategoryCountDto(2, "B", 4),
                new CategoryCountDto(3, "C", 1)), 2)));
        assertTrue(Categories.choices(Collections.singletonList(
                new CategoryCountDto(1, "A", 5)), 8).isEmpty());
        assertTrue(Categories.choices(null, 8).isEmpty());
    }

    @Test
    void aTapChoosesAndASecondTapOrAllClears() {
        assertEquals(7, Categories.tap(0, 7));
        assertEquals(9, Categories.tap(7, 9));
        assertEquals(0, Categories.tap(7, 7));
        assertEquals(0, Categories.tap(7, 0));
        assertTrue(Categories.chosen(0, 0));
        assertFalse(Categories.chosen(7, 0));
        assertTrue(Categories.chosen(7, 7));
    }
}
