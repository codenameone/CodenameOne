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
package com.codenameone.examples.savor.api;

/// The states of an order, as [OrderDto#state] carries them and the server stores
/// them: by name, on the wire and in the database column.
///
/// The courier's part runs beside it, as the order's [DeliveryDto]: it is
/// looked for from the moment the restaurant accepts, so that someone is at the
/// counter about when the food is.
public enum OrderState {
    /// No order. Never stored; it answers "is there one?".
    NONE,
    /// Paid for and sent; the restaurant has not answered.
    PLACED,
    /// The restaurant took it and is making it.
    ACCEPTED,
    /// Made, and waiting at the counter for the courier.
    READY,
    /// With the courier, on the way.
    PICKED_UP,
    DELIVERED,
    /// The restaurant turned it down.
    REJECTED,
    CANCELLED_BY_CUSTOMER,
    /// Stopped by an admin, at any point before it was delivered.
    CANCELLED_BY_ADMIN,
    /// Made or not, nobody could be found to bring it.
    NO_COURIERS;

    /// Whether the order is still going on.
    public boolean isActive() {
        return this == PLACED || this == ACCEPTED || this == READY || this == PICKED_UP;
    }

    /// Whether the order ended without reaching the customer.
    public boolean isCancelled() {
        return this == REJECTED || this == CANCELLED_BY_CUSTOMER || this == CANCELLED_BY_ADMIN
                || this == NO_COURIERS;
    }
}
