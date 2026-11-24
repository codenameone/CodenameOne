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
package com.codenameone.examples.bazaar.shop;

import com.codenameone.examples.bazaar.api.RewardsDto;

/// The rules of the shopper's rewards: which badges the server's counts
/// earn, how far along the tier ring is, the daily-drop streak and which
/// product the day's drop is. Pure; the server keeps the counts and the
/// points, and the Account tab and the order screens draw what this decides.
public final class Rewards {
    public static final int FIRST_ORDER = 0;
    public static final int REVIEWER = 1;
    public static final int REGULAR = 2;
    public static final int EXPLORER = 3;

    /// Orders that make a regular.
    public static final int REGULAR_ORDERS = 5;
    /// Stores an explorer has bought from. The demo has two stores; a third
    /// would make this 3.
    public static final int EXPLORER_STORES = 2;

    public static final String[] TITLES = {"First order", "Reviewer", "Regular", "Explorer"};
    public static final String[] HINTS = {"Place your first order", "Review something you bought",
        "Place five orders", "Shop from two stores"};

    private Rewards() {
    }

    /// Which badges `r` has earned, by index ([#FIRST_ORDER] ...).
    public static boolean[] badges(RewardsDto r) {
        if (r == null) {
            return new boolean[TITLES.length];
        }
        return badges(r.orders(), r.reviews(), r.stores());
    }

    /// [#badges] from the counts themselves.
    public static boolean[] badges(int orders, int reviews, int stores) {
        boolean[] out = new boolean[TITLES.length];
        out[FIRST_ORDER] = orders >= 1;
        out[REVIEWER] = reviews >= 1;
        out[REGULAR] = orders >= REGULAR_ORDERS;
        out[EXPLORER] = stores >= EXPLORER_STORES;
        return out;
    }

    /// How far toward its badge each count is, in thousandths.
    public static int badgeProgress(int badge, int orders, int reviews, int stores) {
        int have;
        int need;
        switch (badge) {
            case FIRST_ORDER:
                have = orders;
                need = 1;
                break;
            case REVIEWER:
                have = reviews;
                need = 1;
                break;
            case REGULAR:
                have = orders;
                need = REGULAR_ORDERS;
                break;
            default:
                have = stores;
                need = EXPLORER_STORES;
                break;
        }
        return Math.max(0, Math.min(1000, have * 1000 / need));
    }

    /// The first badge `after` has that `before` did not, or -1.
    public static int unlocked(boolean[] before, boolean[] after) {
        for (int iter = 0; after != null && iter < after.length; iter++) {
            if (after[iter] && (before == null || iter >= before.length || !before[iter])) {
                return iter;
            }
        }
        return -1;
    }

    /// The tier ring's fill in thousandths: qualifying of qualifying plus
    /// what is left to the next tier. Full at the top tier (nothing left).
    public static int tierProgress(long qualifying, long toNext) {
        if (toNext <= 0) {
            return qualifying > 0 ? 1000 : 0;
        }
        if (qualifying <= 0) {
            return 0;
        }
        return (int) Math.min(1000L, qualifying * 1000L / (qualifying + toNext));
    }

    /// The streak after opening the daily drop on day `today` (days since the
    /// epoch), when it was last opened on `last` with streak `streak`: the
    /// same day keeps it, the next day adds one, a missed day starts over.
    public static int streak(long last, long today, int streak) {
        if (last == today) {
            return Math.max(1, streak);
        }
        if (last == today - 1) {
            return streak + 1;
        }
        return 1;
    }

    /// Which of `count` products is day `day`'s drop: a different one each
    /// day, the same all day, cycling through all of them.
    public static int dropIndex(long day, int count) {
        if (count <= 0) {
            return -1;
        }
        // A stride that shares no factor with the count visits every product.
        long step = 7;
        while (gcd(step, count) != 1) {
            step++;
        }
        long at = (day * step) % count;
        return (int) (at < 0 ? at + count : at);
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    /// The points an amount in cents earns where a point is given per whole
    /// unit of `unitCents`, as the stores' default programme does.
    public static long points(long cents, int pointsPerUnit, long unitCents) {
        if (cents <= 0 || pointsPerUnit <= 0 || unitCents <= 0) {
            return 0;
        }
        return cents * pointsPerUnit / unitCents;
    }
}
