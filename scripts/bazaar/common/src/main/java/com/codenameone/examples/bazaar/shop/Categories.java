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

import com.codenameone.examples.bazaar.api.CategoryCountDto;
import java.util.ArrayList;
import java.util.List;

/// The category row above a store's products: which top-level taxonomy
/// categories it offers and what a tap on one does -- the same categories the
/// web storefront filters by. Plain, so the rules are tested without the UI.
public final class Categories {
    /// The most categories the row shows; the rest are a search away.
    public static final int MAX = 8;

    private Categories() {
    }

    /// The categories to offer: the ones with products, the biggest first
    /// (ties by name), at most `max`, and none at all when there is only one
    /// -- a row with a single choice filters nothing.
    public static List<CategoryCountDto> choices(List<CategoryCountDto> found, int max) {
        List<CategoryCountDto> kept = new ArrayList<CategoryCountDto>();
        for (int iter = 0; found != null && iter < found.size(); iter++) {
            CategoryCountDto t = found.get(iter);
            if (t != null && t.id() > 0 && t.count() > 0 && t.name() != null
                    && t.name().length() > 0) {
                int at = 0;
                while (at < kept.size() && before(kept.get(at), t)) {
                    at++;
                }
                kept.add(at, t);
            }
        }
        List<CategoryCountDto> out = new ArrayList<CategoryCountDto>();
        for (int iter = 0; iter < kept.size() && iter < max; iter++) {
            out.add(kept.get(iter));
        }
        if (out.size() < 2) {
            out.clear();
        }
        return out;
    }

    /// The category after a tap on `tapped` (0 for "All"): tapping the
    /// chosen one again, or "All", clears the filter.
    public static int tap(int current, int tapped) {
        return tapped == current ? 0 : tapped;
    }

    /// Whether the chip for `id` (0 for "All") shows as chosen.
    public static boolean chosen(int current, int id) {
        return current == id;
    }

    private static boolean before(CategoryCountDto a, CategoryCountDto b) {
        if (a.count() != b.count()) {
            return a.count() > b.count();
        }
        return a.name().compareTo(b.name()) <= 0;
    }
}
