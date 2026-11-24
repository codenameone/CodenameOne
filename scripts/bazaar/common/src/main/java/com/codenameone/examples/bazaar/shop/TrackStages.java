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

import com.codenameone.examples.bazaar.api.FulfillmentSummaryDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import java.util.ArrayList;
import java.util.List;

/// The six stages of the order-tracking screen, from placed to delivered,
/// and the in-app banner each new one is announced with. Finer than
/// [OrderSteps], whose four steps label the order lists and pick the badge
/// colours; pure, so the screen only draws what this decides.
public final class TrackStages {
    public static final int PLACED = 0;
    public static final int PAID = 1;
    public static final int PACKED = 2;
    public static final int SHIPPED = 3;
    public static final int OUT_FOR_DELIVERY = 4;
    public static final int DELIVERED = 5;
    /// What [#reached] answers for a cancelled order.
    public static final int CANCELLED = -1;

    public static final String[] LABELS = {"Placed", "Paid", "Packed", "Shipped",
        "Out for delivery", "Delivered"};

    /// The banner title of each stage, reached while the shopper watches.
    private static final String[] BANNERS = {"Order placed", "Payment confirmed",
        "Your order is packed", "Your order has shipped", "Out for delivery today",
        "Delivered!"};

    private TrackStages() {
    }

    /// The stage `order` has reached.
    public static int reached(OrderDto order) {
        if (order == null) {
            return PLACED;
        }
        List<String> parcels = new ArrayList<String>();
        List<FulfillmentSummaryDto> fs = order.fulfillments();
        for (int iter = 0; fs != null && iter < fs.size(); iter++) {
            parcels.add(fs.get(iter).status());
        }
        return reached(order.status(), order.financial(), parcels);
    }

    /// The stage of an order with this status, financial status and parcels:
    /// the furthest any parcel got, Delivered only when every parcel that was
    /// not cancelled arrived.
    public static int reached(String status, String financial, List<String> parcels) {
        if ("CANCELLED".equals(status)) {
            return CANCELLED;
        }
        int at = PLACED;
        if ("AUTHORIZED".equals(financial) || "PAID".equals(financial)
                || "PARTIALLY_REFUNDED".equals(financial) || "REFUNDED".equals(financial)) {
            at = PAID;
        }
        if (parcels == null || parcels.isEmpty()) {
            return at;
        }
        int furthest = at;
        boolean allArrived = true;
        boolean any = false;
        for (int iter = 0; iter < parcels.size(); iter++) {
            String s = parcels.get(iter);
            if ("CANCELLED".equals(s)) {
                continue;
            }
            any = true;
            int p = parcel(s);
            if (p != DELIVERED) {
                allArrived = false;
            }
            furthest = Math.max(furthest, Math.min(p, OUT_FOR_DELIVERY));
        }
        if (!any) {
            return at;
        }
        // A parcel on its way means the order was paid, whatever the
        // financial status says (a card is captured when the goods ship).
        return allArrived ? DELIVERED : Math.max(furthest, PACKED);
    }

    private static int parcel(String s) {
        if ("DELIVERED".equals(s) || "PICKED_UP".equals(s)) {
            return DELIVERED;
        }
        if ("OUT_FOR_DELIVERY".equals(s) || "READY_FOR_PICKUP".equals(s)) {
            return OUT_FOR_DELIVERY;
        }
        if ("SHIPPED".equals(s) || "IN_TRANSIT".equals(s)) {
            return SHIPPED;
        }
        return PACKED;
    }

    /// The share of the way from placed to delivered, in thousandths: what
    /// the progress line and the parcel on the route are drawn at.
    public static int progress(int stage) {
        if (stage <= PLACED) {
            return 0;
        }
        return Math.min(1000, stage * 1000 / DELIVERED);
    }

    /// The banner for moving from `before` to `after`, or null when nothing
    /// worth announcing happened (no change, a step back, a cancellation).
    public static String banner(int before, int after) {
        if (after <= before || after < 0 || after >= BANNERS.length) {
            return null;
        }
        return BANNERS[after];
    }
}
