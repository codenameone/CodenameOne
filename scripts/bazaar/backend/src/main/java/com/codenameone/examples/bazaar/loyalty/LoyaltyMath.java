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
package com.codenameone.examples.bazaar.loyalty;

import com.codenameone.examples.bazaar.domain.LoyaltyProgram;
import com.codenameone.examples.bazaar.domain.LoyaltyTier;

import java.util.List;

/// The arithmetic of points, with no database: what a purchase earns, what
/// points are worth, how much of an order's points a refund takes back, and
/// which tier a member stands in. Every division is guarded; the compiled
/// runtime does not throw on a division by zero.
public final class LoyaltyMath {
    public static final String DOWN = "DOWN";
    public static final String NEAREST = "NEAREST";
    public static final String UP = "UP";

    private LoyaltyMath() {
    }

    /// The points `cents` earns: `pointsPerUnit` for every `unitCents`,
    /// rounded as the programme says. Nothing for a negative amount.
    public static long points(long cents, LoyaltyProgram p) {
        if (cents <= 0 || p.getUnitCents() <= 0 || p.getPointsPerUnit() <= 0) {
            return 0;
        }
        long numerator = cents * p.getPointsPerUnit();
        long unit = p.getUnitCents();
        long whole = numerator / unit;
        long rest = numerator % unit;
        if (UP.equals(p.getRounding())) {
            return rest > 0 ? whole + 1 : whole;
        }
        if (NEAREST.equals(p.getRounding())) {
            return rest * 2 >= unit ? whole + 1 : whole;
        }
        return whole;
    }

    /// Why `points` cannot be redeemed, or null when they can: at least the
    /// minimum, a whole number of rewards, and no more than the balance.
    public static String redeemProblem(long points, long balance, LoyaltyProgram p) {
        if (p == null || !p.isActive()) {
            return "The store has no points programme";
        }
        if (p.getPointsPerReward() <= 0 || p.getRewardCents() <= 0) {
            return "Points cannot be redeemed here";
        }
        if (points <= 0 || points < p.getMinRedeemPoints()) {
            return "Redeem at least " + Math.max(p.getMinRedeemPoints(), p.getPointsPerReward())
                    + " points";
        }
        if (points % p.getPointsPerReward() != 0) {
            return "Redeem points in steps of " + p.getPointsPerReward();
        }
        if (points > balance) {
            return "Not enough points";
        }
        return null;
    }

    /// What `points` are worth, in whole rewards.
    public static long value(long points, LoyaltyProgram p) {
        if (p.getPointsPerReward() <= 0 || points <= 0) {
            return 0;
        }
        return points / p.getPointsPerReward() * p.getRewardCents();
    }

    /// How many of an order's `earned` points a refund leaves reversed in
    /// all, once `refundedCents` of `paidCents` went back: in proportion,
    /// rounded down, and all of them when everything was refunded. Asking
    /// again with the same total answers the same number, which is what
    /// makes a reversal happen once per refund.
    public static long reversedFor(long earned, long paidCents, long refundedCents) {
        if (earned <= 0 || refundedCents <= 0) {
            return 0;
        }
        if (paidCents <= 0 || refundedCents >= paidCents) {
            return earned;
        }
        return earned * refundedCents / paidCents;
    }

    /// The tier `qualifying` reaches: the one with the highest threshold at
    /// or below it, or null.
    public static LoyaltyTier tierFor(long qualifying, List<LoyaltyTier> tiers) {
        LoyaltyTier best = null;
        for (int iter = 0; iter < tiers.size(); iter++) {
            LoyaltyTier t = tiers.get(iter);
            if (t.getThreshold() <= qualifying && (best == null
                    || t.getThreshold() > best.getThreshold())) {
                best = t;
            }
        }
        return best;
    }

    /// The next tier above `qualifying`, or null at the top.
    public static LoyaltyTier nextTier(long qualifying, List<LoyaltyTier> tiers) {
        LoyaltyTier best = null;
        for (int iter = 0; iter < tiers.size(); iter++) {
            LoyaltyTier t = tiers.get(iter);
            if (t.getThreshold() > qualifying && (best == null
                    || t.getThreshold() < best.getThreshold())) {
                best = t;
            }
        }
        return best;
    }
}
