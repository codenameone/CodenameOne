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

import com.codenameone.examples.bazaar.api.PosCatalogDto;
import com.codenameone.examples.bazaar.api.PosCustomerDto;
import com.codenameone.examples.bazaar.api.PosItemDto;
import com.codenameone.examples.bazaar.api.PosLineDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The sale being rung up. Pure: no display, no storage.
///
/// A line is a catalog item, a quantity and the register's own discount on
/// it -- a price typed over, a percent off each unit or an amount off each
/// unit. The whole sale can have a percent or an amount off too. None of
/// these exist in the upload, so [#lines] turns them into the unit price
/// each line is sold at, and the total is priced from exactly those lines.
/// A unit price never drops below one cent, because 0 means "the catalog
/// price" to the server.
public final class PosCart {
    /// One line of the cart.
    public static final class Item {
        public final PosItemDto item;
        public int quantity;
        /// A price typed over the catalog's, or -1.
        public long overrideCents = -1;
        /// Off each unit: basis points, or cents. At most one is set.
        public long percentBp;
        public long amountCents;

        Item(PosItemDto item, int quantity) {
            this.item = item;
            this.quantity = quantity;
        }

        /// The unit price after the line's own discount, before the sale's.
        public long unitCents() {
            long base = overrideCents >= 0 ? overrideCents : item.priceCents();
            long unit = base;
            if (percentBp > 0) {
                unit = base - PosPricing.basisPoints(base, Math.min(10000L, percentBp));
            } else if (amountCents > 0) {
                unit = base - amountCents;
            }
            return Math.max(1, unit);
        }

        public boolean discounted() {
            return overrideCents >= 0 || percentBp > 0 || amountCents > 0;
        }
    }

    private final List<Item> items = new ArrayList<Item>();
    private long orderPercentBp;
    private long orderAmountCents;
    private String code = "";
    private PosCustomerDto customer;
    private String note = "";

    public List<Item> items() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int units() {
        int n = 0;
        for (int iter = 0; iter < items.size(); iter++) {
            n += items.get(iter).quantity;
        }
        return n;
    }

    /// Adds `quantity` of `item`, onto its line when it already has one
    /// without a discount of its own.
    public Item add(PosItemDto item, int quantity) {
        if (item == null || quantity <= 0) {
            return null;
        }
        for (int iter = 0; iter < items.size(); iter++) {
            Item i = items.get(iter);
            if (i.item.variantId().equals(item.variantId()) && !i.discounted()) {
                i.quantity += quantity;
                return i;
            }
        }
        Item added = new Item(item, quantity);
        items.add(added);
        return added;
    }

    /// Sets the quantity of line `index`; 0 or less removes it.
    public void setQuantity(int index, int quantity) {
        if (index < 0 || index >= items.size()) {
            return;
        }
        if (quantity <= 0) {
            items.remove(index);
        } else {
            items.get(index).quantity = quantity;
        }
    }

    public void remove(int index) {
        setQuantity(index, 0);
    }

    /// A percent (basis points) off each unit of line `index`.
    public void linePercent(int index, long bp) {
        if (index >= 0 && index < items.size()) {
            Item i = items.get(index);
            i.percentBp = Math.max(0, Math.min(10000L, bp));
            i.amountCents = 0;
        }
    }

    /// An amount off each unit of line `index`.
    public void lineAmount(int index, long cents) {
        if (index >= 0 && index < items.size()) {
            Item i = items.get(index);
            i.amountCents = Math.max(0, cents);
            i.percentBp = 0;
        }
    }

    public void linePrice(int index, long cents) {
        if (index >= 0 && index < items.size()) {
            items.get(index).overrideCents = cents < 0 ? -1 : cents;
        }
    }

    public void orderPercent(long bp) {
        orderPercentBp = Math.max(0, Math.min(10000L, bp));
        orderAmountCents = 0;
    }

    public void orderAmount(long cents) {
        orderAmountCents = Math.max(0, cents);
        orderPercentBp = 0;
    }

    public long orderPercentBp() {
        return orderPercentBp;
    }

    public long orderAmountCents() {
        return orderAmountCents;
    }

    public String code() {
        return code;
    }

    public void setCode(String code) {
        this.code = code == null ? "" : code.trim();
    }

    public PosCustomerDto customer() {
        return customer;
    }

    public void setCustomer(PosCustomerDto customer) {
        this.customer = customer;
    }

    public String note() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? "" : note;
    }

    public void clear() {
        items.clear();
        orderPercentBp = 0;
        orderAmountCents = 0;
        code = "";
        customer = null;
        note = "";
    }

    /// What the line totals come to before the sale's own discount.
    public long beforeOrderDiscount() {
        long sum = 0;
        for (int iter = 0; iter < items.size(); iter++) {
            sum += items.get(iter).unitCents() * items.get(iter).quantity;
        }
        return sum;
    }

    /// The unit price each line is sold at, with the sale's own discount
    /// spread over the lines by value and taken off each unit. A share that
    /// does not divide by the line's quantity is rounded down per unit, so
    /// the customer gets what the lines say and never a cent more than asked.
    public long[] unitPrices() {
        int n = items.size();
        long[] units = new long[n];
        long[] weights = new long[n];
        for (int iter = 0; iter < n; iter++) {
            units[iter] = items.get(iter).unitCents();
            weights[iter] = units[iter] * items.get(iter).quantity;
        }
        long gross = beforeOrderDiscount();
        long off = orderPercentBp > 0 ? PosPricing.basisPoints(gross, orderPercentBp)
                : Math.min(orderAmountCents, gross);
        if (off <= 0) {
            return units;
        }
        long[] share = PosPricing.shares(weights, off);
        for (int iter = 0; iter < n; iter++) {
            int q = Math.max(1, items.get(iter).quantity);
            units[iter] = Math.max(1, units[iter] - share[iter] / q);
        }
        return units;
    }

    /// The lines to price, at the prices they will be uploaded at.
    public List<PosPricing.Line> pricingLines(Map<String, List<String>> collections) {
        long[] units = unitPrices();
        List<PosPricing.Line> out = new ArrayList<PosPricing.Line>();
        for (int iter = 0; iter < items.size(); iter++) {
            PosItemDto it = items.get(iter).item;
            out.add(new PosPricing.Line(it.variantId(), it.productId(), items.get(iter).quantity,
                    units[iter], it.taxable(),
                    PosPricing.collectionsOf(collections, it.productId())));
        }
        return out;
    }

    /// The sale priced against `catalog`.
    public PosPricing.Totals totals(PosCatalogDto catalog, Map<String, List<String>> collections,
            long now) {
        List<PosPricing.Line> lines = pricingLines(collections);
        if (catalog == null) {
            return PosPricing.price(lines, null, code, null, null, null, false, now);
        }
        return PosPricing.price(lines, catalog.discounts(), code, catalog.taxRates(),
                catalog.taxCountry(), catalog.taxRegion(), catalog.taxInclusive(), now);
    }

    /// The lines as uploaded. A line sold at the catalog price still sends
    /// that price: the snapshot may be older than the server's catalog, and
    /// the customer pays what the register showed.
    public List<PosLineDto> uploadLines() {
        long[] units = unitPrices();
        List<PosLineDto> out = new ArrayList<PosLineDto>();
        for (int iter = 0; iter < items.size(); iter++) {
            out.add(new PosLineDto(items.get(iter).item.variantId(), items.get(iter).quantity,
                    units[iter]));
        }
        return out;
    }
}
