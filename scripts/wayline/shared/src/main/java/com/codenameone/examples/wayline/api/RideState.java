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
package com.codenameone.examples.wayline.api;

/// The states of a ride, as [RideDto#state] carries them and the server stores
/// them: by name, on the wire and in the database column.
public enum RideState {
    /// No ride. Never stored; it answers "is there one?".
    NONE,
    /// Asked for; no driver has been offered it yet.
    REQUESTED,
    /// Offered to one driver, who has a few seconds to answer.
    OFFERED,
    /// A driver took it and is on the way to the pickup.
    ACCEPTED,
    /// The driver is at the pickup.
    ARRIVED,
    /// The rider is in the car.
    IN_PROGRESS,
    COMPLETED,
    CANCELLED_BY_RIDER,
    CANCELLED_BY_DRIVER,
    /// Stopped by an admin, at any point before it completed.
    CANCELLED_BY_ADMIN,
    /// Nobody was near enough, or nobody took it.
    NO_DRIVERS;

    /// Whether a ride in this state is still going on: someone is waiting for a
    /// driver, or a driver is involved and the trip is not over.
    public boolean isActive() {
        return this == REQUESTED || this == OFFERED || this == ACCEPTED || this == ARRIVED
                || this == IN_PROGRESS;
    }

    /// The state called `name`, or null for a name that is none of them -- one
    /// a newer server sends, say. Unlike `valueOf` it never throws, so a
    /// caller reading a name off the wire needs no `catch`.
    public static RideState named(String name) {
        RideState[] all = values();
        for (int iter = 0; iter < all.length; iter++) {
            if (all[iter].name().equals(name)) {
                return all[iter];
            }
        }
        return null;
    }
}
