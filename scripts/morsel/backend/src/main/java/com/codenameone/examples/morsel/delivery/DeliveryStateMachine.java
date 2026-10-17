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
package com.codenameone.examples.morsel.delivery;

import com.codenameone.examples.morsel.api.DeliveryState;

/// Which state a delivery may move to from which.
///
/// ```
/// REQUESTED -> OFFERED -> ACCEPTED -> ARRIVED -> IN_PROGRESS -> COMPLETED
///     ^           |
///     +-----------+   a courier passes, or lets the offer lapse
/// ```
///
/// The customer can cancel until the trip starts, the courier once they have taken
/// it and until it starts, and a search that finds nobody ends in NO_COURIERS.
/// An admin can cancel a delivery at any point before it ends.
/// Nothing leaves a finished state.
///
/// This table is the only place the rules are written. A move is made by one
/// `UPDATE ... WHERE state IN (the states it may come from)`, so the database
/// applies the rule and settles a race in the same statement: of two couriers
/// accepting at once, or a customer cancelling as a courier accepts, one statement
/// changes a row and the other changes none.
public final class DeliveryStateMachine {
    private static final DeliveryState[] NONE = {};
    private static final DeliveryState[] FROM_REQUESTED = {DeliveryState.REQUESTED};
    private static final DeliveryState[] FROM_OFFERED = {DeliveryState.OFFERED};
    private static final DeliveryState[] FROM_ACCEPTED = {DeliveryState.ACCEPTED};
    private static final DeliveryState[] FROM_ARRIVED = {DeliveryState.ARRIVED};
    private static final DeliveryState[] FROM_IN_PROGRESS = {DeliveryState.IN_PROGRESS};
    private static final DeliveryState[] BEFORE_THE_TRIP = {DeliveryState.REQUESTED, DeliveryState.OFFERED,
        DeliveryState.ACCEPTED, DeliveryState.ARRIVED};
    private static final DeliveryState[] UNDER_WAY = {DeliveryState.REQUESTED, DeliveryState.OFFERED,
        DeliveryState.ACCEPTED, DeliveryState.ARRIVED, DeliveryState.IN_PROGRESS};
    private static final DeliveryState[] TAKEN_NOT_STARTED = {DeliveryState.ACCEPTED, DeliveryState.ARRIVED};

    private DeliveryStateMachine() {
    }

    /// The states a delivery may be in to move to `to`. Empty for a state nothing
    /// moves to.
    public static DeliveryState[] sources(DeliveryState to) {
        if (DeliveryState.OFFERED == to) {
            return FROM_REQUESTED;
        }
        if (DeliveryState.REQUESTED == to || DeliveryState.ACCEPTED == to) {
            return FROM_OFFERED;
        }
        if (DeliveryState.ARRIVED == to) {
            return FROM_ACCEPTED;
        }
        if (DeliveryState.IN_PROGRESS == to) {
            return FROM_ARRIVED;
        }
        if (DeliveryState.COMPLETED == to) {
            return FROM_IN_PROGRESS;
        }
        if (DeliveryState.CANCELLED_BY_CUSTOMER == to) {
            return BEFORE_THE_TRIP;
        }
        if (DeliveryState.CANCELLED_BY_COURIER == to) {
            return TAKEN_NOT_STARTED;
        }
        if (DeliveryState.CANCELLED_BY_ADMIN == to) {
            return UNDER_WAY;
        }
        if (DeliveryState.NO_COURIERS == to) {
            return FROM_REQUESTED;
        }
        return NONE;
    }

    public static boolean allowed(DeliveryState from, DeliveryState to) {
        DeliveryState[] sources = sources(to);
        for (int iter = 0; iter < sources.length; iter++) {
            if (sources[iter] == from) {
                return true;
            }
        }
        return false;
    }
}
