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

/// The sales pipeline's columns, the moves between them, and the
/// purchase-intent score of a cart -- one table for the server, which
/// enforces it, and the app, which uses it to refuse a drop before asking.
///
/// A cart is in Browsing while nobody can be reached about it (no email, no
/// account), in Checkout started once it carries either, and in Reminded once
/// the recovery mail went. An order is in Paid until something of it is
/// packed, then Fulfilling, Shipped, Delivered; an order with an open return
/// request is in Returns whatever its parcels did.
///
/// The moves are the ones that do something real: a cart that reached
/// checkout can be reminded (the recovery mail), and an order moves one step
/// right through fulfilment (packs, ships with the cheapest label, marks
/// delivered). Nothing moves left, and a shopper's own steps -- paying,
/// asking for a return -- are not the merchant's to drag.
public final class SalesPipeline {
    public static final String BROWSING = "BROWSING";
    public static final String CHECKOUT = "CHECKOUT";
    public static final String REMINDED = "REMINDED";
    public static final String PAID = "PAID";
    public static final String FULFILLING = "FULFILLING";
    public static final String SHIPPED = "SHIPPED";
    public static final String DELIVERED = "DELIVERED";
    public static final String RETURNS = "RETURNS";

    /// Card kinds.
    public static final String CART = "CART";
    public static final String ORDER = "ORDER";

    private static final String[] COLUMNS = {BROWSING, CHECKOUT, REMINDED, PAID, FULFILLING,
        SHIPPED, DELIVERED, RETURNS};
    private static final String[] TITLES = {"Browsing", "Checkout started", "Reminded",
        "Paid", "Fulfilling", "Shipped", "Delivered", "Return requested"};

    /// `from` to `to` pairs a merchant may drag.
    private static final String[][] MOVES = {{CHECKOUT, REMINDED}, {PAID, FULFILLING},
        {FULFILLING, SHIPPED}, {SHIPPED, DELIVERED}};

    /// Two views further apart than this are two visits, and the time
    /// between them is not time on a product.
    public static final long SESSION_GAP_MILLIS = 30L * 60000L;

    private SalesPipeline() {
    }

    /// Every column id, left to right.
    public static String[] columns() {
        String[] out = new String[COLUMNS.length];
        System.arraycopy(COLUMNS, 0, out, 0, COLUMNS.length);
        return out;
    }

    /// The English title of a column (translated where it is shown), or the
    /// id itself for one that is not a column.
    public static String title(String column) {
        int at = indexOf(column);
        return at < 0 ? column : TITLES[at];
    }

    /// The position of a column, or -1.
    public static int indexOf(String column) {
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            if (COLUMNS[iter].equals(column)) {
                return iter;
            }
        }
        return -1;
    }

    /// True when a card in `from` may be dragged to `to`.
    public static boolean canMove(String from, String to) {
        if (from == null || to == null) {
            return false;
        }
        for (int iter = 0; iter < MOVES.length; iter++) {
            if (MOVES[iter][0].equals(from) && MOVES[iter][1].equals(to)) {
                return true;
            }
        }
        return false;
    }

    /// Where a card in `from` can go, or null when it stays put.
    public static String next(String from) {
        for (int iter = 0; iter < MOVES.length; iter++) {
            if (MOVES[iter][0].equals(from)) {
                return MOVES[iter][1];
            }
        }
        return null;
    }

    /// True for a column whose cards are carts.
    public static boolean isCartColumn(String column) {
        return BROWSING.equals(column) || CHECKOUT.equals(column) || REMINDED.equals(column);
    }

    /// The column of a cart that still has items in it.
    public static String cartColumn(boolean reachable, boolean reminded) {
        if (reminded) {
            return REMINDED;
        }
        return reachable ? CHECKOUT : BROWSING;
    }

    /// The pipeline column of an order from its fulfilment board column
    /// (`TO_PACK`, `PACKED`, `SHIPPED`, `DELIVERED`) and whether a return
    /// request of it is open. Null when the order is on no column.
    public static String orderColumn(String fulfilmentColumn, boolean returnOpen) {
        if (returnOpen) {
            return RETURNS;
        }
        if ("TO_PACK".equals(fulfilmentColumn)) {
            return PAID;
        }
        if ("PACKED".equals(fulfilmentColumn)) {
            return FULFILLING;
        }
        if ("SHIPPED".equals(fulfilmentColumn)) {
            return SHIPPED;
        }
        if ("DELIVERED".equals(fulfilmentColumn)) {
            return DELIVERED;
        }
        return null;
    }

    /// What a visitor did, the inputs of [#score].
    public static final class Signals {
        /// Product pages opened.
        public int views;
        /// Visits: runs of views with no gap longer than [#SESSION_GAP_MILLIS].
        public int visits;
        /// Minutes between views within a visit, summed: time on products.
        public int minutes;
        /// What is in the cart, in the store's minor unit.
        public long valueCents;
        /// Earlier orders of the shopper at this store.
        public int priorOrders;
    }

    /// Visits and minutes from view times, oldest first.
    public static Signals fromViews(long[] viewedAt, long valueCents, int priorOrders) {
        Signals s = new Signals();
        s.valueCents = valueCents;
        s.priorOrders = priorOrders;
        s.views = viewedAt == null ? 0 : viewedAt.length;
        long millis = 0;
        for (int iter = 0; iter < s.views; iter++) {
            if (iter == 0 || viewedAt[iter] - viewedAt[iter - 1] > SESSION_GAP_MILLIS) {
                s.visits++;
            } else {
                millis += viewedAt[iter] - viewedAt[iter - 1];
            }
        }
        s.minutes = (int) (millis / 60000L);
        return s;
    }

    /// The purchase-intent score, 0 to 100, from rules a merchant can check:
    ///
    /// - views: 3 points each, at most 30;
    /// - repeat visits: 5 points for each visit after the first, at most 20;
    /// - time on products: a point a minute, at most 20;
    /// - cart value: 15 from 100.00, 10 from 50.00, 5 for anything;
    /// - a returning customer: 15.
    public static int score(Signals s) {
        int total = 0;
        int[] parts = parts(s);
        for (int iter = 0; iter < parts.length; iter++) {
            total += parts[iter];
        }
        return Math.min(100, total);
    }

    private static int[] parts(Signals s) {
        int views = Math.min(30, Math.max(0, s.views) * 3);
        int visits = Math.min(20, Math.max(0, s.visits - 1) * 5);
        int minutes = Math.min(20, Math.max(0, s.minutes));
        int value = s.valueCents >= 10000 ? 15 : s.valueCents >= 5000 ? 10
                : s.valueCents > 0 ? 5 : 0;
        int returning = s.priorOrders > 0 ? 15 : 0;
        return new int[] {views, visits, minutes, value, returning};
    }

    /// Why the score is what it is: a code and a number for each rule that
    /// added something, the largest first (`views:7`, `visits:3`,
    /// `minutes:12`, `value:12000`, `returning:2`).
    public static List<String> reasons(Signals s) {
        int[] parts = parts(s);
        String[] codes = {"views:" + s.views, "visits:" + s.visits, "minutes:" + s.minutes,
            "value:" + s.valueCents, "returning:" + s.priorOrders};
        List<String> out = new ArrayList<String>();
        boolean[] used = new boolean[parts.length];
        for (int round = 0; round < parts.length; round++) {
            int best = -1;
            for (int iter = 0; iter < parts.length; iter++) {
                if (!used[iter] && parts[iter] > 0 && (best < 0 || parts[iter] > parts[best])) {
                    best = iter;
                }
            }
            if (best < 0) {
                break;
            }
            used[best] = true;
            out.add(codes[best]);
        }
        return out;
    }

    /// The code of a reason (`views` of `views:7`).
    public static String reasonCode(String reason) {
        int colon = reason == null ? -1 : reason.indexOf(':');
        return colon < 0 ? reason : reason.substring(0, colon);
    }

    /// The number of a reason (7 of `views:7`), 0 when it has none.
    public static long reasonValue(String reason) {
        int colon = reason == null ? -1 : reason.indexOf(':');
        if (colon < 0) {
            return 0;
        }
        try {
            return Long.parseLong(reason.substring(colon + 1));
        } catch (NumberFormatException bad) {
            return 0;
        }
    }

    /// A word for the score: `high` from 70, `medium` from 40, else `low`.
    public static String band(int score) {
        return score >= 70 ? "high" : score >= 40 ? "medium" : "low";
    }
}
