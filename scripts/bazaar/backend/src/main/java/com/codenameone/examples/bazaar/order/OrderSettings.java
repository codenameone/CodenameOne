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
package com.codenameone.examples.bazaar.order;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.OrderSettingsDto;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.util.Map;

/// A store's order settings: whether closed orders archive themselves, and
/// the rules self-serve returns are held to. A store that never saved any
/// gets [#DEFAULTS]: no auto-archiving, a 30 day window and no fees.
@Component
public class OrderSettings {
    /// The return window a store starts with, in days.
    public static final int WINDOW_DAYS = 30;
    public static final OrderSettingsDto DEFAULTS = new OrderSettingsDto(false, WINDOW_DAYS, 0,
            0, "", "");
    /// The longest window a store may set, a year.
    static final int MAX_WINDOW = 365;
    /// At most half the goods' value as a restocking fee.
    static final int MAX_FEE_BP = 5000;

    private final DataSource db;
    private final StoreAccess access;

    public OrderSettings(DataSource db, StoreAccess access) {
        this.db = db;
        this.access = access;
    }

    /// The store's settings, for its staff.
    @Transactional(readOnly = true)
    public OrderSettingsDto read(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.ORDERS, false, StoreAccess.ANYONE);
        return of(storeId);
    }

    /// The store's settings, with no access check: for the code applying them.
    @Transactional(readOnly = true)
    public OrderSettingsDto of(String storeId) throws IOException {
        Map r = db.queryOne("SELECT * FROM order_settings WHERE store_id = ?",
                new Object[] {storeId});
        if (r == null) {
            return DEFAULTS;
        }
        return new OrderSettingsDto(Rows.number(r.get("auto_archive")) != 0,
                (int) Rows.number(r.get("return_window_days")),
                (int) Rows.number(r.get("restocking_fee_bp")),
                Rows.number(r.get("return_shipping_cents")),
                Rows.text(r.get("final_sale_products")),
                Rows.text(r.get("final_sale_collections")));
    }

    @Transactional(rollbackFor = IOException.class)
    public OrderSettingsDto save(String username, String storeId, OrderSettingsDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, true, StoreAccess.MANAGERS);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Send the settings");
        }
        if (wanted.returnWindowDays() < 0 || wanted.returnWindowDays() > MAX_WINDOW) {
            throw new ResponseStatusException(400, "The return window is 0 to " + MAX_WINDOW
                    + " days");
        }
        if (wanted.restockingFeeBp() < 0 || wanted.restockingFeeBp() > MAX_FEE_BP) {
            throw new ResponseStatusException(400, "The restocking fee is 0% to 50%");
        }
        if (wanted.returnShippingCents() < 0 || wanted.returnShippingCents() > 100000L) {
            throw new ResponseStatusException(400, "The return shipping fee is 0 to 1000");
        }
        String products = ids(wanted.finalSaleProducts(), "product", storeId);
        String collections = ids(wanted.finalSaleCollections(), "collection", storeId);
        db.execute("DELETE FROM order_settings WHERE store_id = ?", new Object[] {storeId});
        db.execute("INSERT INTO order_settings (store_id, auto_archive, return_window_days, "
                + "restocking_fee_bp, return_shipping_cents, final_sale_products, "
                + "final_sale_collections, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[] {storeId, Integer.valueOf(wanted.autoArchive() ? 1 : 0),
                    Integer.valueOf(wanted.returnWindowDays()),
                    Integer.valueOf(wanted.restockingFeeBp()),
                    Long.valueOf(wanted.returnShippingCents()), products, collections,
                    Long.valueOf(System.currentTimeMillis())});
        return of(storeId);
    }

    /// Whether a product is final sale: listed itself, or in a final-sale
    /// collection.
    @Transactional(readOnly = true)
    public boolean finalSale(OrderSettingsDto s, String productId) throws IOException {
        if (productId == null || productId.length() == 0) {
            return false;
        }
        if (listed(s.finalSaleProducts(), productId)) {
            return true;
        }
        if (s.finalSaleCollections().length() == 0) {
            return false;
        }
        java.util.List rows = db.query("SELECT collection_id FROM collection_product WHERE "
                + "product_id = ?", new Object[] {productId});
        for (int iter = 0; iter < rows.size(); iter++) {
            if (listed(s.finalSaleCollections(), Rows.text(((Map) rows.get(iter))
                    .get("collection_id")))) {
                return true;
            }
        }
        return false;
    }

    /// `restockingFeeBp` of `valueCents`, half up, with the multiplication
    /// guarded: a fee never exceeds the value it is taken from.
    public static long restockingFee(long valueCents, int feeBp) {
        if (valueCents <= 0 || feeBp <= 0) {
            return 0;
        }
        long fee;
        try {
            fee = Math.addExact(Math.multiplyExact(valueCents, (long) feeBp), 5000L) / 10000L;
        } catch (ArithmeticException tooBig) {
            fee = valueCents / 10000L * feeBp;
        }
        return Math.min(fee, valueCents);
    }

    static boolean listed(String csv, String id) {
        if (csv == null || id == null || id.length() == 0) {
            return false;
        }
        int start = 0;
        for (int iter = 0; iter <= csv.length(); iter++) {
            if (iter == csv.length() || csv.charAt(iter) == ',') {
                if (csv.substring(start, iter).trim().equals(id)) {
                    return true;
                }
                start = iter + 1;
            }
        }
        return false;
    }

    /// The ids of a comma separated list, each checked to be the store's.
    private String ids(String csv, String table, String storeId) throws IOException {
        StringBuilder out = new StringBuilder();
        if (csv == null) {
            return "";
        }
        int start = 0;
        for (int iter = 0; iter <= csv.length(); iter++) {
            if (iter == csv.length() || csv.charAt(iter) == ',') {
                String id = csv.substring(start, iter).trim();
                start = iter + 1;
                if (id.length() == 0) {
                    continue;
                }
                Map r = id.length() > 32 ? null : db.queryOne("SELECT store_id FROM " + table
                        + " WHERE id = ?", new Object[] {id});
                if (r == null || !storeId.equals(Rows.text(r.get("store_id")))) {
                    throw new ResponseStatusException(400, "No such " + table + ": " + id);
                }
                if (!listed(out.toString(), id)) {
                    out.append(out.length() == 0 ? "" : ",").append(id);
                }
            }
        }
        if (out.length() > 2000) {
            throw new ResponseStatusException(400, "Too many final-sale " + table + "s");
        }
        return out.toString();
    }
}
