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

/// The states of a delivery, as [DeliveryDto#state] carries them and the server stores
/// them: by name, on the wire and in the database column.
public enum DeliveryState {
    /// No delivery. Never stored; it answers "is there one?".
    NONE,
    /// Asked for; no courier has been offered it yet.
    REQUESTED,
    /// Offered to one courier, who has a few seconds to answer.
    OFFERED,
    /// A courier took it and is on the way to the pickup.
    ACCEPTED,
    /// The courier is at the pickup.
    ARRIVED,
    /// The customer is in the car.
    IN_PROGRESS,
    COMPLETED,
    CANCELLED_BY_CUSTOMER,
    CANCELLED_BY_COURIER,
    /// Stopped by an admin, at any point before it completed.
    CANCELLED_BY_ADMIN,
    /// Nobody was near enough, or nobody took it.
    NO_COURIERS;

    /// Whether a delivery in this state is still going on: someone is waiting for a
    /// courier, or a courier is involved and the trip is not over.
    public boolean isActive() {
        return this == REQUESTED || this == OFFERED || this == ACCEPTED || this == ARRIVED
                || this == IN_PROGRESS;
    }

    /// The state called `name`, or null for a name that is none of them -- one
    /// a newer server sends, say. Unlike `valueOf` it never throws, so a
    /// caller reading a name off the wire needs no `catch`.
    public static DeliveryState named(String name) {
        DeliveryState[] all = values();
        for (int iter = 0; iter < all.length; iter++) {
            if (all[iter].name().equals(name)) {
                return all[iter];
            }
        }
        return null;
    }
}
