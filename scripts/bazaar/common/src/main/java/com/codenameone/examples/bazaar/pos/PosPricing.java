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
import com.codenameone.examples.bazaar.api.TaxRateDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/// What a counter sale costs, worked out on the register so the cashier sees
/// a total with the network down. Pure: no display, no storage.
///
/// The server prices every uploaded sale again and its answer is the one
/// that counts; this mirrors its rules (the backend's `DiscountEngine` and
/// `TaxCalculator`) step for step so the two agree to the cent: banker's
/// rounding everywhere, a discount spread over lines by largest remainder,
/// the best of "every combinable discount together" and "one that does not
/// combine", and tax on what each line costs after its discount.
///
/// The register's own discounts -- a price typed over, a percent or an
/// amount off a line, an amount off the whole sale -- have no field in the
/// upload. They become the unit price the line is sold at ([Line#unitCents]),
/// so the server sees exactly the prices this total was computed from.
public final class PosPricing {
    public static final String PERCENT = "PERCENT";
    public static final String FIXED = "FIXED";
    public static final String BUY_X_GET_Y = "BUY_X_GET_Y";

    private PosPricing() {
    }

    /// A line as it will be uploaded: the unit price it is sold at, already
    /// net of the register's own discounts.
    public static final class Line {
        public final String variantId;
        public final String productId;
        public final int quantity;
        public final long unitCents;
        public final boolean taxable;
        /// Collections the product is in, for a discount aimed at one.
        public final List<String> collectionIds;
        /// Taken off by the store's discounts, written by [#price].
        public long discountCents;
        public long taxCents;

        public Line(String variantId, String productId, int quantity, long unitCents,
                boolean taxable, List<String> collectionIds) {
            this.variantId = variantId;
            this.productId = productId;
            this.quantity = quantity;
            this.unitCents = unitCents;
            this.taxable = taxable;
            this.collectionIds = collectionIds == null ? new ArrayList<String>() : collectionIds;
        }

        public long grossCents() {
            return unitCents * quantity;
        }

        public long netCents() {
            return grossCents() - discountCents;
        }
    }

    /// The sale's numbers.
    public static final class Totals {
        public long subtotalCents;
        public long discountCents;
        /// The part of [#discountCents] the store's automatic discounts
        /// gave: a promotion the store runs, never the clerk's to give, so it
        /// does not count towards the manager's discount threshold.
        public long automaticCents;
        public long taxCents;
        /// Whether the tax is inside the prices (and so adds nothing).
        public boolean taxInclusive;
        public long totalCents;
        /// Titles of the store discounts that applied.
        public final List<String> applied = new ArrayList<String>();
        /// Why the typed code did nothing, or null.
        public String codeProblem;
        public final List<Line> lines = new ArrayList<Line>();

        public long addedTaxCents() {
            return taxInclusive ? 0 : taxCents;
        }
    }

    /// Banker's rounding of `numerator / denominator`; 0 for a denominator
    /// that is not positive (ParparVM does not throw on a division by zero).
    public static long divideHalfEven(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        boolean negative = numerator < 0;
        long n = negative ? -numerator : numerator;
        long q = n / denominator;
        long r = n - q * denominator;
        long twice = r * 2;
        if (twice > denominator || (twice == denominator && (q & 1) == 1)) {
            q++;
        }
        return negative ? -q : q;
    }

    public static long basisPoints(long amount, long basisPoints) {
        return divideHalfEven(amount * basisPoints, 10000L);
    }

    /// The tax in `amount` at `bp`, inside the amount or on top of it.
    public static long tax(long amount, long bp, boolean inclusive) {
        if (amount <= 0 || bp <= 0) {
            return 0;
        }
        if (inclusive) {
            return divideHalfEven(amount * bp, 10000L + bp);
        }
        return basisPoints(amount, bp);
    }

    /// The rates for a place: the region's when it has any, else the
    /// country-wide ones.
    public static List<TaxRateDto> matching(List<TaxRateDto> rates, String country,
            String region) {
        List<TaxRateDto> exact = new ArrayList<TaxRateDto>();
        List<TaxRateDto> countryWide = new ArrayList<TaxRateDto>();
        if (rates == null || country == null || country.length() == 0) {
            return exact;
        }
        for (int iter = 0; iter < rates.size(); iter++) {
            TaxRateDto rate = rates.get(iter);
            if (!country.equalsIgnoreCase(rate.country())) {
                continue;
            }
            String r = rate.region() == null ? "" : rate.region();
            if (r.length() == 0) {
                countryWide.add(rate);
            } else if (region != null && r.equalsIgnoreCase(region.trim())) {
                exact.add(rate);
            }
        }
        return exact.isEmpty() ? countryWide : exact;
    }

    /// The discount a typed code names, or null.
    public static DiscountDto byCode(List<DiscountDto> discounts, String code) {
        if (discounts == null || code == null || code.trim().length() == 0) {
            return null;
        }
        String c = code.trim();
        for (int iter = 0; iter < discounts.size(); iter++) {
            DiscountDto d = discounts.get(iter);
            if (d.code() != null && d.code().equalsIgnoreCase(c)) {
                return d;
            }
        }
        return null;
    }

    /// Why `d` cannot be used now, or null. Once-per-customer is the
    /// server's to judge: the register cannot know the customer's history.
    public static String problem(DiscountDto d, long subtotal, long now) {
        if (d == null || !d.active()) {
            return "That code is not valid";
        }
        if (d.startsAt() > 0 && now < d.startsAt()) {
            return "That code is not valid yet";
        }
        if (d.endsAt() > 0 && now >= d.endsAt()) {
            return "That code has expired";
        }
        if (d.usageLimit() > 0 && d.usedCount() >= d.usageLimit()) {
            return "That code has been used up";
        }
        if (subtotal < d.minSubtotalCents()) {
            return "Spend a little more to use that code";
        }
        return null;
    }

    /// Prices `lines` (writing their discount and tax) with the store's
    /// discounts, the typed code and the register's tax.
    public static Totals price(List<Line> lines, List<DiscountDto> discounts, String code,
            List<TaxRateDto> rates, String country, String region, boolean inclusive, long now) {
        Totals t = new Totals();
        t.lines.addAll(lines);
        t.taxInclusive = inclusive;
        long subtotal = 0;
        for (int iter = 0; iter < lines.size(); iter++) {
            lines.get(iter).discountCents = 0;
            subtotal += lines.get(iter).grossCents();
        }
        t.subtotalCents = subtotal;
        List<DiscountDto> candidates = new ArrayList<DiscountDto>();
        for (int iter = 0; discounts != null && iter < discounts.size(); iter++) {
            DiscountDto d = discounts.get(iter);
            if (d.automatic() && problem(d, subtotal, now) == null) {
                candidates.add(d);
            }
        }
        boolean typed = code != null && code.trim().length() > 0;
        DiscountDto named = typed ? byCode(discounts, code) : null;
        if (typed) {
            String why = named == null ? "That code is not valid" : problem(named, subtotal, now);
            if (why == null) {
                if (!candidates.contains(named)) {
                    candidates.add(named);
                }
            } else {
                t.codeProblem = why;
            }
        }
        List<List<DiscountDto>> choices = new ArrayList<List<DiscountDto>>();
        List<DiscountDto> together = new ArrayList<DiscountDto>();
        for (int iter = 0; iter < candidates.size(); iter++) {
            DiscountDto d = candidates.get(iter);
            if (d.combinable()) {
                together.add(d);
            } else {
                choices.add(Collections.singletonList(d));
            }
        }
        if (!together.isEmpty()) {
            choices.add(together);
        }
        List<DiscountDto> best = null;
        long bestValue = -1;
        boolean bestHasCode = false;
        for (int iter = 0; iter < choices.size(); iter++) {
            List<DiscountDto> choice = choices.get(iter);
            long value = run(lines, choice, null);
            boolean hasCode = typed && named != null && choice.contains(named);
            if (value > bestValue || (value == bestValue && hasCode && !bestHasCode)) {
                best = choice;
                bestValue = value;
                bestHasCode = hasCode;
            }
        }
        if (best != null) {
            t.automaticCents = automatic(lines, best);
            t.discountCents = run(lines, best, t.applied);
        } else {
            clear(lines);
        }
        if (typed && t.codeProblem == null && !bestHasCode) {
            t.codeProblem = "A better discount already applies";
        }
        List<TaxRateDto> apply = matching(rates, country, region);
        long bp = 0;
        for (int iter = 0; iter < apply.size(); iter++) {
            bp += Math.max(0, apply.get(iter).basisPoints());
        }
        for (int iter = 0; iter < lines.size(); iter++) {
            Line line = lines.get(iter);
            line.taxCents = line.taxable ? tax(line.netCents(), bp, inclusive) : 0;
            t.taxCents += line.taxCents;
        }
        t.totalCents = t.subtotalCents - t.discountCents + t.addedTaxCents();
        return t;
    }

    private static long run(List<Line> lines, List<DiscountDto> chosen, List<String> titles) {
        clear(lines);
        long saved = 0;
        for (int iter = 0; iter < chosen.size(); iter++) {
            DiscountDto d = chosen.get(iter);
            long amount = applyOne(lines, d);
            saved += amount;
            if (titles != null && amount > 0) {
                titles.add(d.title() == null || d.title().length() == 0 ? d.code() : d.title());
            }
        }
        return saved;
    }

    /// What the automatic discounts of `chosen` give when `chosen` is run
    /// in order, as [#run] runs it.
    private static long automatic(List<Line> lines, List<DiscountDto> chosen) {
        clear(lines);
        long auto = 0;
        for (int iter = 0; iter < chosen.size(); iter++) {
            long amount = applyOne(lines, chosen.get(iter));
            if (chosen.get(iter).automatic()) {
                auto += amount;
            }
        }
        return auto;
    }

    private static void clear(List<Line> lines) {
        for (int iter = 0; iter < lines.size(); iter++) {
            lines.get(iter).discountCents = 0;
        }
    }

    static boolean targets(DiscountDto d, Line line) {
        String to = d.appliesTo();
        if (to == null || to.length() == 0 || "ALL".equals(to)) {
            return true;
        }
        if ("PRODUCT".equals(to)) {
            return d.targetId() != null && d.targetId().equals(line.productId);
        }
        if ("COLLECTION".equals(to)) {
            return line.collectionIds.contains(d.targetId());
        }
        return false;
    }

    private static long applyOne(List<Line> lines, DiscountDto d) {
        List<Line> targets = new ArrayList<Line>();
        long net = 0;
        for (int iter = 0; iter < lines.size(); iter++) {
            Line line = lines.get(iter);
            if (targets(d, line) && line.netCents() > 0) {
                targets.add(line);
                net += line.netCents();
            }
        }
        if (targets.isEmpty() || net <= 0) {
            return 0;
        }
        String kind = d.kind();
        if (BUY_X_GET_Y.equals(kind)) {
            return buyXGetY(targets, d);
        }
        long amount;
        if (PERCENT.equals(kind)) {
            amount = basisPoints(net, Math.max(0, Math.min(10000L, d.amount())));
        } else if (FIXED.equals(kind)) {
            amount = Math.max(0, Math.min(d.amount(), net));
        } else {
            return 0;
        }
        allocate(targets, amount);
        return amount;
    }

    /// Spreads `amount` over `lines` by what each still costs, largest
    /// remainder first, so the shares add up exactly.
    public static void allocate(List<Line> lines, long amount) {
        long[] nets = new long[lines.size()];
        for (int iter = 0; iter < nets.length; iter++) {
            nets[iter] = lines.get(iter).netCents();
        }
        long[] share = shares(nets, amount);
        for (int iter = 0; iter < nets.length; iter++) {
            lines.get(iter).discountCents += share[iter];
        }
    }

    /// `amount` split in proportion to `weights` by largest remainder. All
    /// of each weight when the amount covers them; nothing for no weight.
    public static long[] shares(long[] weights, long amount) {
        int count = weights.length;
        long[] share = new long[count];
        long net = 0;
        for (int iter = 0; iter < count; iter++) {
            net += weights[iter];
        }
        if (amount <= 0 || net <= 0) {
            return share;
        }
        if (amount >= net) {
            for (int iter = 0; iter < count; iter++) {
                share[iter] = weights[iter];
            }
            return share;
        }
        long[] rest = new long[count];
        long given = 0;
        for (int iter = 0; iter < count; iter++) {
            long product = amount * weights[iter];
            share[iter] = product / net;
            rest[iter] = product % net;
            given += share[iter];
        }
        long left = amount - given;
        while (left > 0) {
            int pick = -1;
            for (int iter = 0; iter < count; iter++) {
                if (rest[iter] >= 0 && (pick < 0 || rest[iter] > rest[pick])) {
                    pick = iter;
                }
            }
            if (pick < 0) {
                break;
            }
            share[pick]++;
            rest[pick] = -1;
            left--;
        }
        return share;
    }

    private static long buyXGetY(List<Line> targets, DiscountDto d) {
        int buy = Math.max(0, d.buyQty());
        int get = Math.max(0, d.getQty());
        if (get == 0) {
            return 0;
        }
        long pct = d.amount() <= 0 ? 10000L : Math.min(10000L, d.amount());
        List<Line> units = new ArrayList<Line>();
        for (int iter = 0; iter < targets.size(); iter++) {
            Line line = targets.get(iter);
            for (int u = 0; u < line.quantity; u++) {
                units.add(line);
            }
        }
        Collections.sort(units, new Comparator<Line>() {
            @Override
            public int compare(Line a, Line b) {
                return a.unitCents < b.unitCents ? 1 : a.unitCents > b.unitCents ? -1 : 0;
            }
        });
        int group = buy + get;
        int groups = group <= 0 ? 0 : units.size() / group;
        long total = 0;
        for (int g = 0; g < groups; g++) {
            for (int u = g * group + buy; u < (g + 1) * group; u++) {
                Line line = units.get(u);
                long off = Math.min(basisPoints(line.unitCents, pct), line.netCents());
                line.discountCents += off;
                total += off;
            }
        }
        return total;
    }

    /// Collections of `productId` in a map of collection id to product ids.
    public static List<String> collectionsOf(Map<String, List<String>> byCollection,
            String productId) {
        List<String> out = new ArrayList<String>();
        if (byCollection == null || productId == null) {
            return out;
        }
        for (Map.Entry<String, List<String>> e : byCollection.entrySet()) {
            if (e.getValue() != null && e.getValue().contains(productId)) {
                out.add(e.getKey());
            }
        }
        return out;
    }
}
