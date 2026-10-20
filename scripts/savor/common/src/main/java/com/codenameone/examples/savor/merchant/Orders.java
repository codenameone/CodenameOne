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
package com.codenameone.examples.savor.merchant;

import com.codenameone.examples.savor.api.OrderDto;
import com.codenameone.examples.savor.api.OrderLineDto;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.ui.Lang;

/// An order as a kitchen reads it.
final class Orders {
    private Orders() {
    }

    /// The last few characters of an id: enough to tell tickets apart at the pass.
    static String shortId(String id) {
        return id == null || id.length() <= 5 ? String.valueOf(id) : id.substring(id.length() - 5);
    }

    /// `2 x Margherita, 1 x Tiramisu`.
    static String summary(OrderDto order) {
        StringBuilder text = new StringBuilder();
        for (OrderLineDto line : order.lines()) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(line.quantity()).append(" x ").append(line.name());
        }
        return text.toString();
    }

    static String state(OrderState state) {
        if (state == null) {
            return "";
        }
        switch (state) {
            case PLACED:
                return Lang.tr("New");
            case ACCEPTED:
                return Lang.tr("Cooking");
            case READY:
                return Lang.tr("Ready");
            case PICKED_UP:
                return Lang.tr("Picked up");
            case DELIVERED:
                return Lang.tr("Delivered");
            case REJECTED:
                return Lang.tr("Rejected");
            default:
                return Lang.tr("Cancelled");
        }
    }

    static String badge(OrderState state) {
        if (state == OrderState.PLACED) {
            return "SvBadgeWarn";
        }
        return state != null && state.isCancelled() ? "SvBadgeBad" : "SvBadgeGood";
    }
}
