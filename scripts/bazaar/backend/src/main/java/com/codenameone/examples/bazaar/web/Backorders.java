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
package com.codenameone.examples.bazaar.web;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.Rows;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Which lines ship later because they were sold past zero.
///
/// An order line keeps the units it could not be given from stock
/// (`order_line.backordered_qty`, written when the order was placed); a cart
/// line has no such record yet, so it is worked out the way the checkout
/// will: the variant continues selling when out of stock and what is
/// available at every location is less than the quantity. The pages show
/// either as "ships when back in stock".
@Component
public class Backorders {
    private final DataSource db;

    public Backorders(DataSource db) {
        this.db = db;
    }

    /// The backordered units of each line of an order that has any, by line id.
    public Map<String, Integer> ofOrder(String orderId) throws IOException {
        Map<String, Integer> out = new HashMap<String, Integer>();
        List rows = db.query("SELECT id, backordered_qty FROM order_line WHERE order_id = ? AND "
                + "backordered_qty > 0", new Object[] {orderId});
        for (int iter = 0; iter < rows.size(); iter++) {
            Map r = (Map) rows.get(iter);
            out.put(Rows.text(r.get("id")),
                    Integer.valueOf((int) Rows.number(r.get("backordered_qty"))));
        }
        return out;
    }

    /// Whether `qty` units of a variant would be sold past zero: it continues
    /// selling when out of stock and fewer are available than that.
    public boolean wouldBackorder(String variantId, int qty) throws IOException {
        if (variantId == null || qty <= 0) {
            return false;
        }
        Map v = db.queryOne("SELECT allow_backorder FROM variant WHERE id = ?",
                new Object[] {variantId});
        if (v == null || Rows.number(v.get("allow_backorder")) == 0) {
            return false;
        }
        Map s = db.queryOne("SELECT SUM(on_hand - committed) AS free FROM inventory_level WHERE "
                + "variant_id = ?", new Object[] {variantId});
        long free = s == null ? 0 : Rows.number(s.get("free"));
        return free < qty;
    }
}
