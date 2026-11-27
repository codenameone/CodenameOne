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
import java.util.List;

/// Where an order is on its way, as the steps of a timeline, and what the
/// shopper may still do to it. Pure: the screen draws the steps.
public final class OrderSteps {
    public static final String[] STEPS = {"Placed", "Confirmed", "Shipped", "Delivered"};

    private OrderSteps() {
    }

    /// How many of [#STEPS] are done, 1 to 4; 0 for a cancelled order.
    public static int reached(OrderDto order) {
        if (order == null || "CANCELLED".equals(order.status())) {
            return 0;
        }
        int reached = 1;
        String financial = order.financial();
        if ("AUTHORIZED".equals(financial) || "PAID".equals(financial)
                || "PARTIALLY_REFUNDED".equals(financial) || "REFUNDED".equals(financial)) {
            reached = 2;
        }
        if ("PARTIAL".equals(order.fulfilment()) || "FULFILLED".equals(order.fulfilment())
                || shipped(order.fulfillments())) {
            reached = 3;
        }
        if (delivered(order.fulfillments()) && "FULFILLED".equals(order.fulfilment())) {
            reached = 4;
        }
        return reached;
    }

    /// Whether the shopper may still cancel: open, nothing sent yet.
    public static boolean cancellable(OrderDto order) {
        return order != null && "OPEN".equals(order.status())
                && "UNFULFILLED".equals(order.fulfilment())
                && !shipped(order.fulfillments());
    }

    /// The word for an order's state on a list row.
    public static String label(OrderDto order) {
        if (order == null) {
            return "";
        }
        if ("CANCELLED".equals(order.status())) {
            return "Cancelled";
        }
        if ("REFUNDED".equals(order.financial())) {
            return "Refunded";
        }
        return STEPS[Math.max(0, Math.min(STEPS.length - 1, reached(order) - 1))];
    }

    private static boolean shipped(List<FulfillmentSummaryDto> fulfillments) {
        if (fulfillments == null) {
            return false;
        }
        for (FulfillmentSummaryDto f : fulfillments) {
            String s = f.status();
            if ("SHIPPED".equals(s) || "IN_TRANSIT".equals(s) || "OUT_FOR_DELIVERY".equals(s)
                    || "DELIVERED".equals(s) || "PICKED_UP".equals(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean delivered(List<FulfillmentSummaryDto> fulfillments) {
        if (fulfillments == null || fulfillments.isEmpty()) {
            return false;
        }
        for (FulfillmentSummaryDto f : fulfillments) {
            if (!"DELIVERED".equals(f.status()) && !"PICKED_UP".equals(f.status())
                    && !"CANCELLED".equals(f.status())) {
                return false;
            }
        }
        return true;
    }
}
