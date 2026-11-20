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

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/// Reviews and the wishlist migration's parsing, without a display.
class ReviewLogicTest {
    @Test
    void averageAndShares() {
        assertEquals("4.3", ProductReviews.average(43));
        assertEquals("5.0", ProductReviews.average(50));
        assertEquals(0.0, ProductReviews.share(3, 0), 0.0001);
        assertEquals(0.25, ProductReviews.share(1, 4), 0.0001);
    }

    @Test
    void oldWishlistIdsSplit() {
        assertEquals(Arrays.asList("a", "b"), ShopState.splitIds("a,,b,"));
        assertEquals(0, ShopState.splitIds("").size());
        assertEquals(0, ShopState.splitIds(null).size());
    }
}
