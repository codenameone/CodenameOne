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
package com.codenameone.examples.bazaar.ops;

import java.util.ArrayList;
import java.util.List;

/// A generic marketplace export, read into products and variants.
///
/// The header names the columns, in any order: `listing_id`, `title`,
/// `description`, `brand`, `category`, `color`, `size`, `sku`, `price` and
/// `quantity`; `title`, `sku` and `price` are required. Rows sharing a
/// `listing_id` (or, without one, a title) become one product, each row a
/// variant with color and size as its options.
///
/// [#plan] is the one reading both the dry run and the import use, so the
/// preview cannot disagree with what is imported: a row with an error is
/// reported with its line number and left out, and the rest go ahead.
public final class MarketplaceRows {
    public static final int MAX_ROWS = 2000;
    static final String[] COLUMNS = {"listing_id", "title", "description", "brand", "category",
        "color", "size", "sku", "price", "quantity"};

    private MarketplaceRows() {
    }

    /// One variant to create.
    public static final class Item {
        public String color = "";
        public String size = "";
        public String sku = "";
        public long priceCents;
        public int quantity;
        public int line;
    }

    /// One product to create.
    public static final class Listing {
        public String key = "";
        public String title = "";
        public String description = "";
        public String brand = "";
        public String category = "";
        public final List<Item> items = new ArrayList<Item>();
    }

    /// What a file amounts to.
    public static final class Plan {
        public int rows;
        public final List<Listing> listings = new ArrayList<Listing>();
        /// `{line, message}` per refused row; line 1 is the header.
        public final List<Object[]> errors = new ArrayList<Object[]>();

        void error(int line, String message) {
            errors.add(new Object[] {Integer.valueOf(line), message});
        }
    }

    /// Reads RFC 4180 CSV: quoted fields may hold commas, quotes ("") and
    /// line breaks; CRLF and LF both end a record.
    public static List<List<String>> csv(String text) {
        List<List<String>> out = new ArrayList<List<String>>();
        if (text == null) {
            return out;
        }
        List<String> row = new ArrayList<String>();
        StringBuilder f = new StringBuilder();
        boolean quoted = false;
        boolean any = false;
        int n = text.length();
        for (int iter = 0; iter < n; iter++) {
            char c = text.charAt(iter);
            if (quoted) {
                if (c == '"') {
                    if (iter + 1 < n && text.charAt(iter + 1) == '"') {
                        f.append('"');
                        iter++;
                    } else {
                        quoted = false;
                    }
                } else {
                    f.append(c);
                }
                continue;
            }
            if (c == '"') {
                quoted = true;
                any = true;
            } else if (c == ',') {
                row.add(f.toString());
                f.setLength(0);
                any = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && iter + 1 < n && text.charAt(iter + 1) == '\n') {
                    iter++;
                }
                if (any || f.length() > 0) {
                    row.add(f.toString());
                    out.add(row);
                }
                row = new ArrayList<String>();
                f.setLength(0);
                any = false;
            } else {
                f.append(c);
                any = true;
            }
        }
        if (any || f.length() > 0) {
            row.add(f.toString());
            out.add(row);
        }
        return out;
    }

    /// `12.5`, `12.50` or `$12` as cents; -1 for anything else.
    public static long cents(String text) {
        if (text == null) {
            return -1;
        }
        String t = text.trim();
        if (t.startsWith("$")) {
            t = t.substring(1);
        }
        if (t.length() == 0 || t.length() > 12) {
            return -1;
        }
        long whole = 0;
        long frac = 0;
        int fracDigits = -1;
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c == '.' && fracDigits < 0) {
                fracDigits = 0;
            } else if (c >= '0' && c <= '9') {
                if (fracDigits < 0) {
                    whole = whole * 10 + (c - '0');
                } else if (fracDigits < 2) {
                    frac = frac * 10 + (c - '0');
                    fracDigits++;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        }
        if (fracDigits == 1) {
            frac *= 10;
        }
        return whole * 100 + frac;
    }

    private static int intOf(String text) {
        if (text == null || text.trim().length() == 0) {
            return 0;
        }
        String t = text.trim();
        if (t.length() > 7) {
            return -1;
        }
        int v = 0;
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }

    private static String cell(List<String> row, int index) {
        return index < 0 || index >= row.size() ? "" : row.get(index).trim();
    }

    /// The products and variants `text` describes, and every refused row.
    public static Plan plan(String text) {
        Plan p = new Plan();
        List<List<String>> rows = csv(text);
        if (rows.isEmpty()) {
            p.error(1, "The file is empty");
            return p;
        }
        List<String> head = rows.get(0);
        int[] at = new int[COLUMNS.length];
        for (int c = 0; c < COLUMNS.length; c++) {
            at[c] = -1;
            for (int h = 0; h < head.size(); h++) {
                if (COLUMNS[c].equalsIgnoreCase(head.get(h).trim())) {
                    at[c] = h;
                }
            }
        }
        if (at[1] < 0 || at[7] < 0 || at[8] < 0) {
            p.error(1, "The header needs title, sku and price columns");
            return p;
        }
        if (rows.size() - 1 > MAX_ROWS) {
            p.error(1, "At most " + MAX_ROWS + " rows at a time");
            return p;
        }
        List<String> skus = new ArrayList<String>();
        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            int line = r + 1;
            p.rows++;
            String title = cell(row, at[1]);
            String sku = cell(row, at[7]);
            long price = cents(cell(row, at[8]));
            int qty = intOf(cell(row, at[9]));
            if (title.length() == 0) {
                p.error(line, "No title");
                continue;
            }
            if (title.length() > 200) {
                p.error(line, "The title is longer than 200 characters");
                continue;
            }
            if (sku.length() == 0 || sku.length() > 64) {
                p.error(line, "No SKU, or one longer than 64 characters");
                continue;
            }
            if (skus.contains(sku)) {
                p.error(line, "SKU " + sku + " appears twice");
                continue;
            }
            if (price < 0) {
                p.error(line, "The price is not an amount: " + cell(row, at[8]));
                continue;
            }
            if (qty < 0) {
                p.error(line, "The quantity is not a whole number");
                continue;
            }
            skus.add(sku);
            String key = cell(row, at[0]);
            if (key.length() == 0) {
                key = "title:" + title;
            }
            Listing l = null;
            for (int iter = 0; iter < p.listings.size(); iter++) {
                if (p.listings.get(iter).key.equals(key)) {
                    l = p.listings.get(iter);
                }
            }
            if (l == null) {
                l = new Listing();
                l.key = key;
                l.title = title;
                l.description = cell(row, at[2]);
                l.brand = cell(row, at[3]);
                l.category = cell(row, at[4]);
                p.listings.add(l);
            }
            Item it = new Item();
            it.color = cell(row, at[5]);
            it.size = cell(row, at[6]);
            it.sku = sku;
            it.priceCents = price;
            it.quantity = qty;
            it.line = line;
            l.items.add(it);
        }
        return p;
    }
}
