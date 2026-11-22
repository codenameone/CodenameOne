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

import com.codenameone.examples.bazaar.api.NameCountDto;
import java.util.ArrayList;
import java.util.List;

/// The category row above a store's products: which categories (product
/// types) it offers and what a tap on one does. Plain, so the rules are
/// tested without the UI.
public final class Categories {
    /// The most categories the row shows; the rest are a search away.
    public static final int MAX = 8;

    private Categories() {
    }

    /// The categories to offer, from the store's type facet: the ones with
    /// products, the biggest first (ties by name), at most `max`, and none
    /// at all when there is only one -- a row with a single choice filters
    /// nothing.
    public static List<String> choices(List<NameCountDto> types, int max) {
        List<NameCountDto> kept = new ArrayList<NameCountDto>();
        for (int iter = 0; types != null && iter < types.size(); iter++) {
            NameCountDto t = types.get(iter);
            if (t != null && t.count() > 0 && t.name() != null && t.name().length() > 0) {
                int at = 0;
                while (at < kept.size() && before(kept.get(at), t)) {
                    at++;
                }
                kept.add(at, t);
            }
        }
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < kept.size() && iter < max; iter++) {
            out.add(kept.get(iter).name());
        }
        if (out.size() < 2) {
            out.clear();
        }
        return out;
    }

    /// The category after a tap on `tapped` (null for "All"): tapping the
    /// chosen one again, or "All", clears the filter.
    public static String tap(String current, String tapped) {
        if (tapped == null || tapped.equals(current)) {
            return null;
        }
        return tapped;
    }

    /// Whether the chip for `name` (null for "All") shows as chosen.
    public static boolean chosen(String current, String name) {
        return name == null ? current == null : name.equals(current);
    }

    private static boolean before(NameCountDto a, NameCountDto b) {
        if (a.count() != b.count()) {
            return a.count() > b.count();
        }
        return a.name().compareTo(b.name()) <= 0;
    }
}
