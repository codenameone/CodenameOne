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
package com.codenameone.examples.bazaar.merchant;

import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderLineDto;
import java.util.Map;

/// What a refund comes to, shown live in the refund dialog as the merchant
/// picks quantities.
///
/// The same arithmetic as the server's `Orders.lineRefund`: a unit's share of
/// the line after discounts, rounded half to even, plus its share of the tax
/// when the tax was added on top of the price. The server has the last word --
/// this is the number the dialog promises before it asks. Pure logic.
public final class RefundMath {
    private RefundMath() {
    }

    /// Units of a line not yet refunded.
    public static int refundable(OrderLineDto line) {
        return Math.max(0, line.quantity() - line.refundedQty());
    }

    /// `quantity` units' share of `line`.
    public static long line(OrderLineDto line, int quantity, boolean taxInclusive) {
        if (quantity <= 0 || line.quantity() <= 0) {
            return 0;
        }
        long net = line.priceCents() * line.quantity() - line.discountCents();
        long out = divideHalfEven(net * quantity, line.quantity());
        if (!taxInclusive) {
            out += divideHalfEven(line.taxCents() * quantity, line.quantity());
        }
        return out;
    }

    /// The whole refund: the chosen units of each line (keyed by line id,
    /// clamped to what is left), plus shipping (clamped to the order's), never
    /// more than what the order has left to refund.
    public static long total(OrderDto order, Map<String, Integer> quantities,
            long shippingCents, boolean taxInclusive) {
        long amount = 0;
        for (int iter = 0; order.lines() != null && iter < order.lines().size(); iter++) {
            OrderLineDto l = order.lines().get(iter);
            Integer q = quantities == null ? null : quantities.get(l.id());
            int units = q == null ? 0 : Math.min(q.intValue(), refundable(l));
            amount += line(l, units, taxInclusive);
        }
        amount += Math.max(0, Math.min(shippingCents, order.shippingCents()));
        long left = order.totalCents() - order.refundedCents();
        return Math.max(0, Math.min(amount, left));
    }

    /// `value / divisor`, an exact half going to the even neighbour. Zero for
    /// a divisor of zero: ParparVM would not throw.
    public static long divideHalfEven(long value, long divisor) {
        if (divisor == 0) {
            return 0;
        }
        boolean negative = (value < 0) != (divisor < 0);
        long a = Math.abs(value);
        long d = Math.abs(divisor);
        long q = a / d;
        long r = a % d;
        long twice = r * 2;
        if (twice > d || (twice == d && (q & 1) == 1)) {
            q++;
        }
        return negative ? -q : q;
    }
}
