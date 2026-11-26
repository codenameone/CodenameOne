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
package com.codenameone.examples.bazaar.customer;

/// The arithmetic of spending store credit, with no database: which grants a
/// payment takes from and how much of each.
///
/// Credit is spent first in, first out by expiry: the grant that lapses
/// soonest goes first, grants that never lapse go last, and two grants that
/// lapse together go in the order they were given. A grant that has already
/// lapsed (its expiry is at or before `now`) pays nothing, whether or not the
/// expiry job has swept it yet.
public final class CreditMath {
    private CreditMath() {
    }

    /// Whether a grant expiring at `expiresAt` (0 for never) can pay at `now`.
    public static boolean usable(long remaining, long expiresAt, long now) {
        return remaining > 0 && (expiresAt <= 0 || expiresAt > now);
    }

    /// What can be spent now across the grants.
    public static long available(long[] remaining, long[] expiresAt, long now) {
        long sum = 0;
        for (int iter = 0; iter < remaining.length; iter++) {
            if (usable(remaining[iter], expiresAt[iter], now)) {
                sum += remaining[iter];
            }
        }
        return sum;
    }

    /// The order the grants are spent in, as indexes.
    public static int[] order(long[] expiresAt, long[] createdAt) {
        int n = expiresAt.length;
        int[] idx = new int[n];
        for (int iter = 0; iter < n; iter++) {
            idx[iter] = iter;
        }
        // Insertion sort: a customer holds a handful of grants.
        for (int i = 1; i < n; i++) {
            int cur = idx[i];
            int j = i - 1;
            while (j >= 0 && before(cur, idx[j], expiresAt, createdAt)) {
                idx[j + 1] = idx[j];
                j--;
            }
            idx[j + 1] = cur;
        }
        return idx;
    }

    private static boolean before(int a, int b, long[] expiresAt, long[] createdAt) {
        long ea = expiresAt[a] <= 0 ? Long.MAX_VALUE : expiresAt[a];
        long eb = expiresAt[b] <= 0 ? Long.MAX_VALUE : expiresAt[b];
        if (ea != eb) {
            return ea < eb;
        }
        return createdAt[a] < createdAt[b];
    }

    /// How much of `want` each grant pays, by index, or null when the usable
    /// grants together hold less than `want`.
    public static long[] plan(long[] remaining, long[] expiresAt, long[] createdAt, long now,
            long want) {
        long[] parts = new long[remaining.length];
        if (want <= 0) {
            return parts;
        }
        if (available(remaining, expiresAt, now) < want) {
            return null;
        }
        int[] idx = order(expiresAt, createdAt);
        long left = want;
        for (int k = 0; k < idx.length && left > 0; k++) {
            int i = idx[k];
            if (!usable(remaining[i], expiresAt[i], now)) {
                continue;
            }
            long take = Math.min(left, remaining[i]);
            parts[i] = take;
            left -= take;
        }
        return parts;
    }
}
