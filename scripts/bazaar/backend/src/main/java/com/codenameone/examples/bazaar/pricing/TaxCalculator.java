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

import com.codenameone.examples.bazaar.domain.TaxRate;

import java.util.ArrayList;
import java.util.List;

/// Sales tax and VAT on a cart.
///
/// Pure. The rates that apply are those of the address's country and region
/// (a US state, say); when the store has none for the region, those of the
/// country with an empty region; several matching rates add up. Country and
/// region compare ignoring ASCII case.
///
/// - **Exclusive pricing** (`Store.taxInclusive` false, the US way): tax is
///   added on top, `net * rate`.
/// - **Inclusive pricing** (the EU way): prices already contain the tax, and
///   the part of `net` that is tax is `net * rate / (1 + rate)`. Nothing is
///   added to the total.
/// - Tax is worked out **per line**, after the line's discounts, and rounded
///   half to even on each line; the cart's tax is the sum of the lines (plus
///   shipping). A line whose variant is not taxable pays none.
/// - Shipping is taxed by the matching rates marked `appliesToShipping` only.
public final class TaxCalculator {
    private TaxCalculator() {
    }

    /// What [#apply] worked out.
    public static final class Result {
        /// The basis points the lines were taxed at.
        public long basisPoints;
        public long linesTaxCents;
        public long shippingTaxCents;
        /// Whether the tax is inside the prices rather than on top.
        public boolean inclusive;
        /// Names of the rates used, for the receipt.
        public final List<String> names = new ArrayList<String>();

        public long totalCents() {
            return linesTaxCents + shippingTaxCents;
        }

        /// What the tax adds to the total: nothing when prices include it.
        public long addedCents() {
            return inclusive ? 0 : totalCents();
        }
    }

    /// The rates that apply to an address; see the class comment.
    public static List<TaxRate> matching(List<TaxRate> rates, String country, String region) {
        List<TaxRate> exact = new ArrayList<TaxRate>();
        List<TaxRate> countryWide = new ArrayList<TaxRate>();
        if (rates == null || country == null) {
            return exact;
        }
        for (int iter = 0; iter < rates.size(); iter++) {
            TaxRate rate = rates.get(iter);
            if (!country.equalsIgnoreCase(rate.getCountry())) {
                continue;
            }
            String r = rate.getRegion() == null ? "" : rate.getRegion();
            if (r.length() == 0) {
                countryWide.add(rate);
            } else if (region != null && r.equalsIgnoreCase(region.trim())) {
                exact.add(rate);
            }
        }
        return exact.isEmpty() ? countryWide : exact;
    }

    /// The tax in `amount` cents at `basisPoints`, inclusive or not, rounded
    /// half to even.
    public static long tax(long amount, long basisPoints, boolean inclusive) {
        if (amount <= 0 || basisPoints <= 0) {
            return 0;
        }
        if (inclusive) {
            return Money.divideHalfEven(amount * basisPoints, 10000L + basisPoints);
        }
        return Money.basisPoints(amount, basisPoints);
    }

    /// Taxes the lines (writing [PriceLine#taxCents]) and the shipping.
    public static Result apply(List<PriceLine> lines, long shippingCents, List<TaxRate> rates,
            String country, String region, boolean inclusive) {
        Result result = new Result();
        result.inclusive = inclusive;
        List<TaxRate> apply = matching(rates, country, region);
        long bp = 0;
        long shippingBp = 0;
        for (int iter = 0; iter < apply.size(); iter++) {
            TaxRate rate = apply.get(iter);
            bp += Math.max(0, rate.getBasisPoints());
            if (rate.isAppliesToShipping()) {
                shippingBp += Math.max(0, rate.getBasisPoints());
            }
            result.names.add(rate.getName());
        }
        result.basisPoints = bp;
        for (int iter = 0; iter < lines.size(); iter++) {
            PriceLine line = lines.get(iter);
            line.taxCents = line.taxable ? tax(line.netCents(), bp, inclusive) : 0;
            result.linesTaxCents += line.taxCents;
        }
        result.shippingTaxCents = tax(shippingCents, shippingBp, inclusive);
        return result;
    }
}
