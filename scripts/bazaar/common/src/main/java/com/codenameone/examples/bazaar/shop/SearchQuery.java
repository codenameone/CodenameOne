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

/// What the shopper is looking for: the words, the filters and the order.
/// One instance lives in [ShopState] so the search keeps its filters when the
/// user goes to a product and back, or the window is resized and the screen
/// rebuilt. Pure.
public final class SearchQuery {
    public String q = "";
    public String collection;
    public String type;
    /// A top-level taxonomy category id, 0 for any.
    public int category;
    public String vendor;
    public String tag;
    /// Cents, or -1 for no bound.
    public long min = -1;
    public long max = -1;
    /// One of the server's sort keys: `relevance`, `newest`, `price`,
    /// `-price`, `title`, `rating`.
    public String sort = "relevance";

    public static final String[] SORTS = {"relevance", "newest", "price", "-price", "rating"};
    public static final String[] SORT_LABELS = {"Best match", "Newest", "Price: low to high",
        "Price: high to low", "Top rated"};

    /// How many filters are on, for the badge on the Filters button. The
    /// words and the order are not filters.
    public int active() {
        int count = 0;
        count += empty(collection) ? 0 : 1;
        count += empty(type) ? 0 : 1;
        count += empty(vendor) ? 0 : 1;
        count += empty(tag) ? 0 : 1;
        count += min >= 0 || max >= 0 ? 1 : 0;
        return count;
    }

    public void clearFilters() {
        collection = null;
        type = null;
        vendor = null;
        tag = null;
        min = -1;
        max = -1;
    }

    /// The words, or null when there are none: an absent query parameter.
    public String words() {
        String t = q == null ? "" : q.trim();
        return t.length() == 0 ? null : t;
    }

    public String minParam() {
        return min < 0 ? null : String.valueOf(min);
    }

    public String maxParam() {
        return max < 0 ? null : String.valueOf(max);
    }

    /// The order to ask for: relevance only means something with words.
    public String sortParam() {
        if ("relevance".equals(sort) && words() == null) {
            return null;
        }
        return sort;
    }

    /// Flips `value` on or off for a filter that holds one value.
    public static String toggle(String current, String value) {
        return value != null && value.equals(current) ? null : value;
    }

    private static boolean empty(String s) {
        return s == null || s.length() == 0;
    }

    /// The category as the `category` parameter, null for any.
    public String categoryParam() {
        return category > 0 ? String.valueOf(category) : null;
    }
}
