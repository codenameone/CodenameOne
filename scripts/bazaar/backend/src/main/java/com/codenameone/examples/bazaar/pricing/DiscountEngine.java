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

import com.codename1.backend.Json;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
/// - **More kinds.** [#TIERED] takes the percentage (or amount) of the highest
///   tier whose `min` the targeted lines reach; [#VOLUME] gives each product
///   the percentage of the highest quantity break its units reach; [#BUNDLE]
///   sells every complete set of its `items` for `price`. Their parameters are
///   the discount's `rules` JSON.
/// - **Who.** A discount whose `eligibility` is set (`TIER:<id>`,
///   `CLUB:<id>`) only applies when the caller's memberships include it.
/// - **Combination classes.** Once any candidate names a `discountClass`
///   (PRODUCT, ORDER or SHIPPING), stacking follows the classes instead: two
///   discounts may go together when each combines with the other's class
///   (a discount without a class combines with every class exactly when it is
///   `combinable`). Every set of mutually combinable candidates is weighed and
///   the one that saves most wins, the code winning a tie. Within a set,
///   product discounts apply first, then order discounts, then shipping.
public final class DiscountEngine {
    public static final String PERCENT = "PERCENT";
    public static final String FIXED = "FIXED";
    public static final String FREE_SHIPPING = "FREE_SHIPPING";
    public static final String BUY_X_GET_Y = "BUY_X_GET_Y";
    public static final String ALL = "ALL";
    public static final String COLLECTION = "COLLECTION";
    public static final String PRODUCT = "PRODUCT";
    public static final String TIERED = "TIERED";
    public static final String VOLUME = "VOLUME";
    public static final String BUNDLE = "BUNDLE";
    public static final String CLASS_PRODUCT = "PRODUCT";
    public static final String CLASS_ORDER = "ORDER";
    public static final String CLASS_SHIPPING = "SHIPPING";
    /// Under class rules every subset of candidates is weighed: at most this
    /// many, so at most 4096 sets.
    static final int MAX_WEIGHED = 12;

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
        return problem(d, subtotalCents, now, used, null);
    }

    /// [#problem(Discount, long, Instant, Set)] for a customer with
    /// `memberships` (`TIER:<id>`, `CLUB:<id>`); null is none.
    public static String problem(Discount d, long subtotalCents, Instant now, Set<String> used,
            Set<String> memberships) {
        if (d == null || !d.isActive()) {
            return "That code is not valid";
        }
        String who = d.getEligibility();
        if (who != null && who.length() > 0 && (memberships == null || !memberships.contains(who))) {
            return "That discount is for members";
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
        return apply(lines, automatic, code, typedCode, now, used, shippingCents, null);
    }

    /// [#apply(List, List, Discount, String, Instant, Set, long)] for a
    /// customer with `memberships`, which member-only discounts need.
    public static Result apply(List<PriceLine> lines, List<Discount> automatic, Discount code,
            String typedCode, Instant now, Set<String> used, long shippingCents,
            Set<String> memberships) {
        Result result = new Result();
        for (int iter = 0; iter < lines.size(); iter++) {
            lines.get(iter).discountCents = 0;
        }
        long subtotal = PriceLine.subtotal(lines);
        List<Discount> candidates = new ArrayList<Discount>();
        if (automatic != null) {
            for (int iter = 0; iter < automatic.size(); iter++) {
                Discount d = automatic.get(iter);
                if (d.isAutomatic() && problem(d, subtotal, now, used, memberships) == null) {
                    candidates.add(d);
                }
            }
        }
        boolean typed = typedCode != null && typedCode.trim().length() > 0;
        if (typed) {
            String why = code == null ? "That code is not valid"
                    : problem(code, subtotal, now, used, memberships);
            if (why == null) {
                candidates.add(code);
            } else {
                result.codeProblem = why;
            }
        }
        List<List<Discount>> choices = usesClasses(candidates)
                ? classChoices(candidates, typed ? code : null) : legacyChoices(candidates);
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

    /// The older rule: every combinable discount together, or one that does
    /// not combine on its own.
    private static List<List<Discount>> legacyChoices(List<Discount> candidates) {
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
        return choices;
    }

    static boolean usesClasses(List<Discount> candidates) {
        for (int iter = 0; iter < candidates.size(); iter++) {
            String c = candidates.get(iter).getDiscountClass();
            if (c != null && c.length() > 0) {
                return true;
            }
        }
        return false;
    }

    /// The class a discount competes in: its own, or for one without, the
    /// class its kind implies.
    public static String classOf(Discount d) {
        String c = d.getDiscountClass();
        if (c != null && c.length() > 0) {
            return c;
        }
        if (FREE_SHIPPING.equals(d.getKind())) {
            return CLASS_SHIPPING;
        }
        String to = d.getAppliesTo();
        boolean all = to == null || to.length() == 0 || ALL.equals(to);
        if (all && (FIXED.equals(d.getKind()) || TIERED.equals(d.getKind())
                || PERCENT.equals(d.getKind()))) {
            return CLASS_ORDER;
        }
        return CLASS_PRODUCT;
    }

    /// Whether `d` stacks with a discount of class `other`.
    public static boolean combinesWith(Discount d, String other) {
        String c = d.getDiscountClass();
        if (c == null || c.length() == 0) {
            return d.isCombinable();
        }
        if (CLASS_PRODUCT.equals(other)) {
            return d.isCombinesProduct();
        }
        if (CLASS_ORDER.equals(other)) {
            return d.isCombinesOrder();
        }
        if (CLASS_SHIPPING.equals(other)) {
            return d.isCombinesShipping();
        }
        return false;
    }

    /// Whether two discounts may apply to one cart.
    public static boolean compatible(Discount a, Discount b) {
        return combinesWith(a, classOf(b)) && combinesWith(b, classOf(a));
    }

    private static int rank(Discount d) {
        String c = classOf(d);
        return CLASS_PRODUCT.equals(c) ? 0 : CLASS_ORDER.equals(c) ? 1 : 2;
    }

    /// Every set of mutually compatible candidates, each in application
    /// order. The code is always weighed; past [#MAX_WEIGHED] the later
    /// automatic discounts are not.
    private static List<List<Discount>> classChoices(List<Discount> candidates, Discount code) {
        List<Discount> pool = new ArrayList<Discount>();
        if (code != null && candidates.contains(code)) {
            pool.add(code);
        }
        for (int iter = 0; iter < candidates.size() && pool.size() < MAX_WEIGHED; iter++) {
            if (candidates.get(iter) != code) {
                pool.add(candidates.get(iter));
            }
        }
        int n = pool.size();
        boolean[][] ok = new boolean[n][n];
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                ok[a][b] = compatible(pool.get(a), pool.get(b));
            }
        }
        List<List<Discount>> out = new ArrayList<List<Discount>>();
        for (int mask = 1; mask < (1 << n); mask++) {
            boolean fits = true;
            for (int a = 0; a < n && fits; a++) {
                if ((mask & (1 << a)) == 0) {
                    continue;
                }
                for (int b = a + 1; b < n && fits; b++) {
                    if ((mask & (1 << b)) != 0 && !ok[a][b]) {
                        fits = false;
                    }
                }
            }
            if (!fits) {
                continue;
            }
            List<Discount> set = new ArrayList<Discount>();
            for (int r = 0; r < 3; r++) {
                for (int a = 0; a < n; a++) {
                    if ((mask & (1 << a)) != 0 && rank(pool.get(a)) == r) {
                        set.add(pool.get(a));
                    }
                }
            }
            out.add(set);
        }
        return out;
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
        if (VOLUME.equals(kind)) {
            return volume(targets, d);
        }
        if (BUNDLE.equals(kind)) {
            return bundle(lines, d);
        }
        long amount;
        if (TIERED.equals(kind)) {
            amount = tiered(net, d);
        } else if (PERCENT.equals(kind)) {
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

    // ------------------------------------------------------------ rule kinds

    /// A discount's `rules` as a map; empty when there are none or they do
    /// not read.
    public static Map rules(Discount d) {
        String text = d.getRules();
        if (text == null || text.trim().length() == 0) {
            return new LinkedHashMap();
        }
        try {
            Map m = Json.parseObject(text);
            return m == null ? new LinkedHashMap() : m;
        } catch (IOException | RuntimeException unreadable) {
            return new LinkedHashMap();
        }
    }

    /// The entries of a list in `rules`, each a map; others are skipped.
    public static List<Map> entries(Map rules, String name) {
        List<Map> out = new ArrayList<Map>();
        Object v = rules.get(name);
        if (v instanceof List) {
            List l = (List) v;
            for (int iter = 0; iter < l.size(); iter++) {
                if (l.get(iter) instanceof Map) {
                    out.add((Map) l.get(iter));
                }
            }
        }
        return out;
    }

    /// A whole number in a rules map; `fallback` when absent or not a number.
    public static long number(Map m, String name, long fallback) {
        Object v = m.get(name);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        if (v instanceof String) {
            try {
                return Long.parseLong(((String) v).trim());
            } catch (NumberFormatException notNumber) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String text(Map m, String name) {
        Object v = m.get(name);
        return v == null ? "" : v.toString();
    }

    /// The highest tier `net` reaches: its basis points of `net`, or its cents.
    static long tiered(long net, Discount d) {
        List<Map> tiers = entries(rules(d), "tiers");
        Map best = null;
        long bestMin = -1;
        for (int iter = 0; iter < tiers.size(); iter++) {
            long min = number(tiers.get(iter), "min", -1);
            if (min >= 0 && min <= net && min > bestMin) {
                best = tiers.get(iter);
                bestMin = min;
            }
        }
        if (best == null) {
            return 0;
        }
        long bps = number(best, "bps", -1);
        if (bps >= 0) {
            return Money.basisPoints(net, Math.min(10000L, bps));
        }
        return Math.max(0, Math.min(number(best, "cents", 0), net));
    }

    /// Each product's lines get the percentage of the highest break their
    /// units together reach.
    private static long volume(List<PriceLine> targets, Discount d) {
        List<Map> breaks = entries(rules(d), "breaks");
        Map<String, List<PriceLine>> byProduct = new LinkedHashMap<String, List<PriceLine>>();
        for (int iter = 0; iter < targets.size(); iter++) {
            PriceLine line = targets.get(iter);
            List<PriceLine> group = byProduct.get(line.productId);
            if (group == null) {
                group = new ArrayList<PriceLine>();
                byProduct.put(line.productId, group);
            }
            group.add(line);
        }
        long total = 0;
        for (Map.Entry<String, List<PriceLine>> e : byProduct.entrySet()) {
            int qty = 0;
            long net = 0;
            for (int iter = 0; iter < e.getValue().size(); iter++) {
                qty += e.getValue().get(iter).quantity;
                net += e.getValue().get(iter).netCents();
            }
            long bps = 0;
            long bestQty = 0;
            for (int iter = 0; iter < breaks.size(); iter++) {
                long need = number(breaks.get(iter), "qty", 0);
                if (need > 0 && need <= qty && need > bestQty) {
                    bestQty = need;
                    bps = Math.max(0, Math.min(10000L, number(breaks.get(iter), "bps", 0)));
                }
            }
            long amount = Money.basisPoints(net, bps);
            if (amount > 0) {
                allocate(e.getValue(), Math.min(amount, net));
                total += Math.min(amount, net);
            }
        }
        return total;
    }

    /// Every complete set of the bundle's items sells for its `price`; the
    /// saving lands on the lines that make up the sets.
    private static long bundle(List<PriceLine> lines, Discount d) {
        Map r = rules(d);
        List<Map> items = entries(r, "items");
        long price = number(r, "price", -1);
        if (items.isEmpty() || price < 0) {
            return 0;
        }
        long sets = Long.MAX_VALUE;
        long regular = 0;
        List<PriceLine> involved = new ArrayList<PriceLine>();
        long involvedNet = 0;
        for (int iter = 0; iter < items.size(); iter++) {
            String variant = text(items.get(iter), "variantId");
            long need = Math.max(1, number(items.get(iter), "qty", 1));
            long have = 0;
            long unit = -1;
            for (int l = 0; l < lines.size(); l++) {
                PriceLine line = lines.get(l);
                if (variant.equals(line.variantId) && line.netCents() > 0) {
                    have += line.quantity;
                    unit = unit < 0 ? line.unitCents : Math.min(unit, line.unitCents);
                    if (!involved.contains(line)) {
                        involved.add(line);
                        involvedNet += line.netCents();
                    }
                }
            }
            if (unit < 0) {
                return 0;
            }
            sets = Math.min(sets, have / need);
            regular += unit * need;
        }
        if (sets <= 0 || sets == Long.MAX_VALUE || regular <= price) {
            return 0;
        }
        long amount = Math.min((regular - price) * sets, involvedNet);
        allocate(involved, amount);
        return amount;
    }
}
