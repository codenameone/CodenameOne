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
package com.codenameone.examples.bazaar.pos;

import com.codenameone.examples.bazaar.api.DiscountDto;
import com.codenameone.examples.bazaar.api.PosCatalogDto;
import com.codenameone.examples.bazaar.api.PosItemDto;
import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.api.TaxRateDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The offline catalog and how it changes. Pure: no storage, no display.
///
/// The server answers a register that already has the current version with
/// `unchanged` and empty lists; that answer must never replace what is held.
/// A new version knows nothing of the sales this register has not uploaded
/// yet, so their units are taken off it again. After each sale the units
/// sold come off the snapshot at once, so the grid shows what is left.
public final class Snapshot {
    private Snapshot() {
    }

    /// What to hold after asking for `fresh` while holding `held`.
    public static PosCatalogDto merge(PosCatalogDto held, PosCatalogDto fresh,
            Map<String, Integer> unsynced) {
        if (fresh == null || fresh.unchanged()) {
            return held;
        }
        return less(fresh, unsynced);
    }

    /// `catalog` with the units of `lines` sold.
    public static PosCatalogDto sold(PosCatalogDto catalog, List<PosLineDto> lines) {
        Map<String, Integer> units = new HashMap<String, Integer>();
        for (int iter = 0; lines != null && iter < lines.size(); iter++) {
            PosLineDto l = lines.get(iter);
            Integer before = units.get(l.variantId());
            units.put(l.variantId(), Integer.valueOf((before == null ? 0 : before.intValue())
                    + l.quantity()));
        }
        return less(catalog, units);
    }

    /// `catalog` with `units` per variant taken off what is available. Stock
    /// may go negative: a sale at the counter is never refused for stock.
    public static PosCatalogDto less(PosCatalogDto catalog, Map<String, Integer> units) {
        if (catalog == null || units == null || units.isEmpty() || catalog.items() == null) {
            return catalog;
        }
        List<PosItemDto> items = new ArrayList<PosItemDto>();
        for (int iter = 0; iter < catalog.items().size(); iter++) {
            PosItemDto i = catalog.items().get(iter);
            Integer n = units.get(i.variantId());
            if (n == null || !i.trackInventory()) {
                items.add(i);
            } else {
                items.add(new PosItemDto(i.variantId(), i.productId(), i.title(),
                        i.variantTitle(), i.sku(), i.barcode(), i.priceCents(),
                        i.compareAtCents(), i.taxable(), i.trackInventory(),
                        i.available() - n.intValue(), i.photoId()));
            }
        }
        return new PosCatalogDto(catalog.version(), false, catalog.storeId(),
                catalog.registerId(), catalog.locationId(), catalog.currency(),
                catalog.taxInclusive(), catalog.taxCountry(), catalog.taxRegion(), items,
                catalog.taxRates() == null ? new ArrayList<TaxRateDto>() : catalog.taxRates(),
                catalog.discounts() == null ? new ArrayList<DiscountDto>() : catalog.discounts());
    }

    /// The item of `variantId`, or null.
    public static PosItemDto item(PosCatalogDto catalog, String variantId) {
        if (catalog == null || catalog.items() == null) {
            return null;
        }
        for (int iter = 0; iter < catalog.items().size(); iter++) {
            if (catalog.items().get(iter).variantId().equals(variantId)) {
                return catalog.items().get(iter);
            }
        }
        return null;
    }

    /// The variants of each product, products in catalog order.
    public static List<List<PosItemDto>> products(List<PosItemDto> items) {
        List<List<PosItemDto>> out = new ArrayList<List<PosItemDto>>();
        Map<String, List<PosItemDto>> by = new HashMap<String, List<PosItemDto>>();
        for (int iter = 0; items != null && iter < items.size(); iter++) {
            PosItemDto i = items.get(iter);
            List<PosItemDto> group = by.get(i.productId());
            if (group == null) {
                group = new ArrayList<PosItemDto>();
                by.put(i.productId(), group);
                out.add(group);
            }
            group.add(i);
        }
        return out;
    }

    /// Items whose title, variant, SKU or barcode contains `q`, ignoring the
    /// case of ASCII letters; in a collection when `productIds` is not null.
    public static List<PosItemDto> filter(List<PosItemDto> items, String q,
            List<String> productIds) {
        List<PosItemDto> out = new ArrayList<PosItemDto>();
        String query = q == null ? "" : q.trim();
        for (int iter = 0; items != null && iter < items.size(); iter++) {
            PosItemDto i = items.get(iter);
            if (productIds != null && !productIds.contains(i.productId())) {
                continue;
            }
            if (query.length() == 0 || contains(i.title(), query) || contains(i.variantTitle(), query)
                    || contains(i.sku(), query) || contains(i.barcode(), query)) {
                out.add(i);
            }
        }
        return out;
    }

    static boolean contains(String text, String q) {
        if (text == null) {
            return false;
        }
        int max = text.length() - q.length();
        for (int iter = 0; iter <= max; iter++) {
            if (text.regionMatches(true, iter, q, 0, q.length())) {
                return true;
            }
        }
        return false;
    }
}
