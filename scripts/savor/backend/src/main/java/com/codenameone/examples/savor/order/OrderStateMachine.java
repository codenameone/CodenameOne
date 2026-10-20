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
package com.codenameone.examples.savor.order;

import com.codenameone.examples.savor.api.OrderState;

/// The moves an order may make, as the states each one may start from. Every
/// move is a conditional update of the order's row on these sources, so the
/// database enforces the rule and settles a race in the same statement.
///
/// An order is coupled to its delivery, which has the same id: the delivery is
/// created when the kitchen accepts, the order is picked up in the same
/// transaction as the courier starts the delivery, and it is delivered in the
/// same transaction as the delivery completes.
public final class OrderStateMachine {
    private static final OrderState[] NONE = {};
    private static final OrderState[] FROM_PLACED = {OrderState.PLACED};
    private static final OrderState[] FROM_ACCEPTED = {OrderState.ACCEPTED};
    private static final OrderState[] FROM_READY = {OrderState.READY};
    private static final OrderState[] FROM_PICKED_UP = {OrderState.PICKED_UP};
    private static final OrderState[] IN_THE_KITCHEN = {OrderState.ACCEPTED, OrderState.READY};
    private static final OrderState[] NOT_DELIVERED = {OrderState.PLACED, OrderState.ACCEPTED,
        OrderState.READY, OrderState.PICKED_UP};

    private OrderStateMachine() {
    }

    /// The states an order may move to `to` from; empty when nothing does.
    public static OrderState[] sources(OrderState to) {
        if (OrderState.ACCEPTED == to || OrderState.REJECTED == to
                || OrderState.CANCELLED_BY_CUSTOMER == to) {
            return FROM_PLACED;
        }
        if (OrderState.READY == to) {
            return FROM_ACCEPTED;
        }
        if (OrderState.PICKED_UP == to) {
            return FROM_READY;
        }
        if (OrderState.DELIVERED == to) {
            return FROM_PICKED_UP;
        }
        if (OrderState.NO_COURIERS == to) {
            return IN_THE_KITCHEN;
        }
        if (OrderState.CANCELLED_BY_ADMIN == to) {
            return NOT_DELIVERED;
        }
        return NONE;
    }

    public static boolean allowed(OrderState from, OrderState to) {
        OrderState[] sources = sources(to);
        for (int iter = 0; iter < sources.length; iter++) {
            if (sources[iter] == from) {
                return true;
            }
        }
        return false;
    }
}
