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
package com.codenameone.examples.bazaar.pricing;

import java.util.ArrayList;
import java.util.List;

/// One line of a cart or an order as the calculators see it: what it is, what
/// one unit costs and how many there are. The calculators fill in
/// [#discountCents] and [#taxCents].
public final class PriceLine {
    public final String key;
    public final String productId;
    public final String variantId;
    public final long unitCents;
    public final int quantity;
    public final int weightGrams;
    public final boolean taxable;
    /// The collections the product is in, for discounts aimed at one.
    public final List<String> collectionIds = new ArrayList<String>();
    public long discountCents;
    public long taxCents;
    /// The rate the line was taxed at, in basis points; written with
    /// [#taxCents].
    public long taxBasisPoints;

    public PriceLine(String key, String productId, String variantId, long unitCents,
            int quantity, int weightGrams, boolean taxable) {
        this.key = key;
        this.productId = productId;
        this.variantId = variantId;
        this.unitCents = unitCents;
        this.quantity = quantity;
        this.weightGrams = weightGrams;
        this.taxable = taxable;
    }

    /// Price times quantity, before any discount.
    public long grossCents() {
        return unitCents * quantity;
    }

    /// What is left to pay for the line after its discounts.
    public long netCents() {
        return grossCents() - discountCents;
    }

    public static long subtotal(List<PriceLine> lines) {
        long sum = 0;
        for (int iter = 0; iter < lines.size(); iter++) {
            sum += lines.get(iter).grossCents();
        }
        return sum;
    }

    public static int weight(List<PriceLine> lines) {
        int sum = 0;
        for (int iter = 0; iter < lines.size(); iter++) {
            PriceLine line = lines.get(iter);
            sum += line.weightGrams * line.quantity;
        }
        return sum;
    }
}
