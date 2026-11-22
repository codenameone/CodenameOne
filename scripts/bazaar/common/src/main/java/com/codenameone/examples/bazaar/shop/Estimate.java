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

import com.codenameone.examples.bazaar.api.AppliedDiscountDto;
import com.codenameone.examples.bazaar.api.CartDto;
import java.util.ArrayList;
import java.util.List;

/// The lines of a cart's summary -- subtotal, discounts, shipping, tax,
/// total, gift card, amount due -- as the server priced them. The app never
/// adds anything up itself: it only decides which of the server's numbers to
/// show and how. Pure; the captions are English keys the screen translates.
public final class Estimate {
    /// A plain line.
    public static final int PLAIN = 0;
    /// Money taken off: drawn in the discount colour, with a minus.
    public static final int SAVING = 1;
    /// The total and the amount due.
    public static final int TOTAL = 2;
    /// Words rather than an amount ("Free", "At checkout").
    public static final int NOTE = 3;

    /// One line of the summary.
    public static final class Row {
        public final String caption;
        public final String detail;
        public final long cents;
        public final int kind;
        public final String note;

        Row(String caption, String detail, long cents, int kind, String note) {
            this.caption = caption;
            this.detail = detail;
            this.cents = cents;
            this.kind = kind;
            this.note = note;
        }
    }

    private Estimate() {
    }

    public static List<Row> rows(CartDto cart) {
        List<Row> rows = new ArrayList<Row>();
        if (cart == null) {
            return rows;
        }
        rows.add(new Row("Subtotal", null, cart.subtotalCents(), PLAIN, null));
        List<AppliedDiscountDto> discounts = cart.discounts();
        if (discounts != null && !discounts.isEmpty()) {
            for (AppliedDiscountDto discount : discounts) {
                if (discount.amountCents() > 0) {
                    rows.add(new Row("Discount", label(discount.code(), discount.title()),
                            -discount.amountCents(), SAVING, null));
                }
            }
        } else if (cart.discountCents() > 0) {
            rows.add(new Row("Discount", cart.discountCode(), -cart.discountCents(), SAVING,
                    null));
        }
        if (cart.shippingRateId() == null || cart.shippingRateId().length() == 0) {
            rows.add(new Row("Shipping", null, 0, NOTE,
                    cart.freeShipping() ? "Free" : "At checkout"));
        } else if (cart.shippingCents() == 0) {
            rows.add(new Row("Shipping", null, 0, NOTE, "Free"));
        } else {
            rows.add(new Row("Shipping", null, cart.shippingCents(), PLAIN, null));
        }
        if (cart.taxInclusive()) {
            rows.add(new Row("Tax included", null, cart.taxCents(), PLAIN, null));
        } else {
            rows.add(new Row("Tax", null, cart.taxCents(), PLAIN, null));
        }
        rows.add(new Row("Total", null, cart.totalCents(), TOTAL, null));
        if (cart.giftCardCents() > 0) {
            rows.add(new Row("Gift card", cart.giftCardCode(), -cart.giftCardCents(), SAVING,
                    null));
        }
        if (cart.storeCreditCents() > 0) {
            rows.add(new Row("Store credit", null, -cart.storeCreditCents(), SAVING, null));
        }
        if (cart.giftCardCents() > 0 || cart.storeCreditCents() > 0) {
            rows.add(new Row("Due now", null, cart.dueCents(), TOTAL, null));
        }
        return rows;
    }

    /// How many units the cart holds, for the badge on the cart tab.
    public static int units(CartDto cart) {
        if (cart == null || cart.lines() == null) {
            return 0;
        }
        int units = 0;
        for (int iter = 0; iter < cart.lines().size(); iter++) {
            units += Math.max(0, cart.lines().get(iter).quantity());
        }
        return units;
    }

    private static String label(String code, String title) {
        if (code != null && code.length() > 0) {
            return code;
        }
        return title;
    }
}
