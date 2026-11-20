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

import com.codenameone.examples.bazaar.domain.Discount;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/// Works out which discounts a cart gets and spreads each over its lines.
///
/// Pure: no database, no clock (the caller passes `now`), nothing remembered
/// between calls. The rules:
///
/// - **Kinds.** [#PERCENT] takes `amount` basis points (1500 = 15%) off the
///   lines it applies to, rounded half to even. [#FIXED] takes `amount` cents,
///   never more than those lines cost. [#FREE_SHIPPING] makes delivery free
///   and touches no line. [#BUY_X_GET_Y]: of every `buyQty + getQty` units of
///   the lines it applies to, the `getQty` cheapest are discounted by
///   `amount` basis points, or are free when `amount` is 0.
/// - **Who it applies to.** `appliesTo` is ALL, COLLECTION (lines whose
///   product is in collection `targetId`) or PRODUCT (product `targetId`).
/// - **When.** Active, between `startsAt` and `endsAt` (either may be unset),
///   under its `usageLimit` (0 = no limit), not already used by this customer
///   when `oncePerCustomer`, and the cart's subtotal at least
///   `minSubtotalCents`.
/// - **Stacking.** Every automatic discount and at most one code compete.
///   Those marked `combinable` stack with each other; one that is not stands
///   alone. Whichever choice saves the customer most wins, the code winning a
///   tie.
/// - **Allocation.** A discount is spread over its lines in proportion to what
///   each still costs; the cents left over by rounding down go one each to the
///   lines with the largest remainders, the earlier line on a tie. So the
///   lines always add up to the discount exactly, and no line ever goes below
///   zero.
public final class DiscountEngine {
    public static final String PERCENT = "PERCENT";
    public static final String FIXED = "FIXED";
    public static final String FREE_SHIPPING = "FREE_SHIPPING";
    public static final String BUY_X_GET_Y = "BUY_X_GET_Y";
    public static final String ALL = "ALL";
    public static final String COLLECTION = "COLLECTION";
    public static final String PRODUCT = "PRODUCT";

    private DiscountEngine() {
    }

    /// One discount the cart got, and what it saved.
    public static final class Applied {
        public final Discount discount;
        public final long amountCents;
        public final boolean freeShipping;

        Applied(Discount discount, long amountCents, boolean freeShipping) {
            this.discount = discount;
            this.amountCents = amountCents;
            this.freeShipping = freeShipping;
        }
    }

    /// What [#apply] decided.
    public static final class Result {
        public final List<Applied> applied = new ArrayList<Applied>();
        public boolean freeShipping;
        /// The cents taken off the lines (free shipping not included).
        public long totalCents;
        /// Why the code the customer typed did nothing, or null.
        public String codeProblem;
    }

    /// Why a discount cannot be used now, or null when it can.
    ///
    /// @param used the ids of the discounts this customer has already used
    /// A schedule bound is unset when null or the epoch, the entity's default.
    private static boolean set(Instant at) {
        return at != null && at.toEpochMilli() > 0;
    }

    public static String problem(Discount d, long subtotalCents, Instant now, Set<String> used) {
        if (d == null || !d.isActive()) {
            return "That code is not valid";
        }
        if (set(d.getStartsAt()) && now.isBefore(d.getStartsAt())) {
            return "That code is not valid yet";
        }
        if (set(d.getEndsAt()) && !now.isBefore(d.getEndsAt())) {
            return "That code has expired";
        }
        if (d.getUsageLimit() > 0 && d.getUsedCount() >= d.getUsageLimit()) {
            return "That code has been used up";
        }
        if (d.isOncePerCustomer() && used != null && used.contains(d.getId())) {
            return "That code was already used on your account";
        }
        if (subtotalCents < d.getMinSubtotalCents()) {
            return "Spend a little more to use that code";
        }
        return null;
    }

    /// Picks and applies the discounts. Writes each line's
    /// [PriceLine#discountCents].
    ///
    /// @param automatic the store's automatic discounts
    /// @param code the discount the typed code names, or null
    /// @param typedCode what the customer typed, or null for none
    /// @param shippingCents what delivery would cost, to weigh free shipping
    public static Result apply(List<PriceLine> lines, List<Discount> automatic, Discount code,
            String typedCode, Instant now, Set<String> used, long shippingCents) {
        Result result = new Result();
        for (int iter = 0; iter < lines.size(); iter++) {
            lines.get(iter).discountCents = 0;
        }
        long subtotal = PriceLine.subtotal(lines);
        List<Discount> candidates = new ArrayList<Discount>();
        if (automatic != null) {
            for (int iter = 0; iter < automatic.size(); iter++) {
                Discount d = automatic.get(iter);
                if (d.isAutomatic() && problem(d, subtotal, now, used) == null) {
                    candidates.add(d);
                }
            }
        }
        boolean typed = typedCode != null && typedCode.trim().length() > 0;
        if (typed) {
            String why = code == null ? "That code is not valid" : problem(code, subtotal, now, used);
            if (why == null) {
                candidates.add(code);
            } else {
                result.codeProblem = why;
            }
        }
        // The choices: every combinable discount together, or one that does
        // not combine on its own.
        List<List<Discount>> choices = new ArrayList<List<Discount>>();
        List<Discount> together = new ArrayList<Discount>();
        for (int iter = 0; iter < candidates.size(); iter++) {
            Discount d = candidates.get(iter);
            if (d.isCombinable()) {
                together.add(d);
            } else {
                choices.add(Collections.singletonList(d));
            }
        }
        if (!together.isEmpty()) {
            choices.add(together);
        }
        List<Discount> best = null;
        long bestValue = -1;
        boolean bestHasCode = false;
        for (int iter = 0; iter < choices.size(); iter++) {
            List<Discount> choice = choices.get(iter);
            long value = run(lines, choice, shippingCents, null);
            boolean hasCode = typed && code != null && choice.contains(code);
            if (value > bestValue || (value == bestValue && hasCode && !bestHasCode)) {
                best = choice;
                bestValue = value;
                bestHasCode = hasCode;
            }
        }
        if (best != null) {
            run(lines, best, shippingCents, result);
        } else {
            clear(lines);
        }
        if (typed && result.codeProblem == null && !bestHasCode) {
            result.codeProblem = "A better discount already applies";
        }
        return result;
    }

    /// Applies `chosen` in order and answers what it saves, free shipping
    /// valued at `shippingCents`. Leaves the lines' discounts set; records
    /// what was applied when `into` is not null.
    private static long run(List<PriceLine> lines, List<Discount> chosen, long shippingCents,
            Result into) {
        clear(lines);
        long saved = 0;
        boolean free = false;
        for (int iter = 0; iter < chosen.size(); iter++) {
            Discount d = chosen.get(iter);
            if (FREE_SHIPPING.equals(d.getKind())) {
                if (!free) {
                    saved += Math.max(0, shippingCents);
                }
                free = true;
                if (into != null) {
                    into.applied.add(new Applied(d, 0, true));
                    into.freeShipping = true;
                }
                continue;
            }
            long amount = applyOne(lines, d);
            saved += amount;
            if (into != null) {
                into.applied.add(new Applied(d, amount, false));
                into.totalCents += amount;
            }
        }
        return saved;
    }

    private static void clear(List<PriceLine> lines) {
        for (int iter = 0; iter < lines.size(); iter++) {
            lines.get(iter).discountCents = 0;
        }
    }

    /// Whether `d` applies to `line`.
    public static boolean targets(Discount d, PriceLine line) {
        String to = d.getAppliesTo();
        if (to == null || to.length() == 0 || ALL.equals(to)) {
            return true;
        }
        if (PRODUCT.equals(to)) {
            return d.getTargetId() != null && d.getTargetId().equals(line.productId);
        }
        if (COLLECTION.equals(to)) {
            return line.collectionIds.contains(d.getTargetId());
        }
        return false;
    }

    /// Takes one discount off the lines it targets; answers the cents taken.
    private static long applyOne(List<PriceLine> lines, Discount d) {
        List<PriceLine> targets = new ArrayList<PriceLine>();
        long net = 0;
        for (int iter = 0; iter < lines.size(); iter++) {
            PriceLine line = lines.get(iter);
            if (targets(d, line) && line.netCents() > 0) {
                targets.add(line);
                net += line.netCents();
            }
        }
        if (targets.isEmpty() || net <= 0) {
            return 0;
        }
        String kind = d.getKind();
        if (BUY_X_GET_Y.equals(kind)) {
            return buyXGetY(targets, d);
        }
        long amount;
        if (PERCENT.equals(kind)) {
            amount = Money.basisPoints(net, Math.max(0, Math.min(10000L, d.getAmount())));
        } else if (FIXED.equals(kind)) {
            amount = Math.max(0, Math.min(d.getAmount(), net));
        } else {
            return 0;
        }
        allocate(targets, amount);
        return amount;
    }

    /// Spreads `amount` over `lines` by what each still costs, exactly; see
    /// the class comment. `amount` must not exceed their total.
    public static void allocate(List<PriceLine> lines, long amount) {
        long net = 0;
        for (int iter = 0; iter < lines.size(); iter++) {
            net += lines.get(iter).netCents();
        }
        if (amount <= 0 || net <= 0) {
            return;
        }
        if (amount >= net) {
            for (int iter = 0; iter < lines.size(); iter++) {
                PriceLine line = lines.get(iter);
                line.discountCents += line.netCents();
            }
            return;
        }
        int count = lines.size();
        long[] share = new long[count];
        long[] rest = new long[count];
        long given = 0;
        for (int iter = 0; iter < count; iter++) {
            long product = amount * lines.get(iter).netCents();
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
        for (int iter = 0; iter < count; iter++) {
            lines.get(iter).discountCents += share[iter];
        }
    }

    /// Of every `buy + get` units, most expensive first, the `get` cheapest
    /// are discounted. Each unit's discount lands on its own line.
    private static long buyXGetY(List<PriceLine> targets, Discount d) {
        int buy = Math.max(0, d.getBuyQty());
        int get = Math.max(0, d.getGetQty());
        if (get == 0) {
            return 0;
        }
        long pct = d.getAmount() <= 0 ? 10000L : Math.min(10000L, d.getAmount());
        List<PriceLine> units = new ArrayList<PriceLine>();
        for (int iter = 0; iter < targets.size(); iter++) {
            PriceLine line = targets.get(iter);
            for (int u = 0; u < line.quantity; u++) {
                units.add(line);
            }
        }
        // Stable: equal prices keep cart order.
        Collections.sort(units, (a, b) -> Long.compare(b.unitCents, a.unitCents));
        int group = buy + get;
        int groups = units.size() / group;
        long total = 0;
        for (int g = 0; g < groups; g++) {
            for (int u = g * group + buy; u < (g + 1) * group; u++) {
                PriceLine line = units.get(u);
                long off = Math.min(Money.basisPoints(line.unitCents, pct), line.netCents());
                line.discountCents += off;
                total += off;
            }
        }
        return total;
    }
}
