/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.ride;

import com.codenameone.examples.wayline.api.RideState;

/// Which state a ride may move to from which.
///
/// ```
/// REQUESTED -> OFFERED -> ACCEPTED -> ARRIVED -> IN_PROGRESS -> COMPLETED
///     ^           |
///     +-----------+   a driver passes, or lets the offer lapse
/// ```
///
/// The rider can cancel until the trip starts, the driver once they have taken
/// it and until it starts, and a search that finds nobody ends in NO_DRIVERS.
/// An admin can cancel a ride at any point before it ends.
/// Nothing leaves a finished state.
///
/// This table is the only place the rules are written. A move is made by one
/// `UPDATE ... WHERE state IN (the states it may come from)`, so the database
/// applies the rule and settles a race in the same statement: of two drivers
/// accepting at once, or a rider cancelling as a driver accepts, one statement
/// changes a row and the other changes none.
public final class RideStateMachine {
    private static final RideState[] NONE = {};
    private static final RideState[] FROM_REQUESTED = {RideState.REQUESTED};
    private static final RideState[] FROM_OFFERED = {RideState.OFFERED};
    private static final RideState[] FROM_ACCEPTED = {RideState.ACCEPTED};
    private static final RideState[] FROM_ARRIVED = {RideState.ARRIVED};
    private static final RideState[] FROM_IN_PROGRESS = {RideState.IN_PROGRESS};
    private static final RideState[] BEFORE_THE_TRIP = {RideState.REQUESTED, RideState.OFFERED,
        RideState.ACCEPTED, RideState.ARRIVED};
    private static final RideState[] UNDER_WAY = {RideState.REQUESTED, RideState.OFFERED,
        RideState.ACCEPTED, RideState.ARRIVED, RideState.IN_PROGRESS};
    private static final RideState[] TAKEN_NOT_STARTED = {RideState.ACCEPTED, RideState.ARRIVED};

    private RideStateMachine() {
    }

    /// The states a ride may be in to move to `to`. Empty for a state nothing
    /// moves to.
    public static RideState[] sources(RideState to) {
        if (RideState.OFFERED == to) {
            return FROM_REQUESTED;
        }
        if (RideState.REQUESTED == to || RideState.ACCEPTED == to) {
            return FROM_OFFERED;
        }
        if (RideState.ARRIVED == to) {
            return FROM_ACCEPTED;
        }
        if (RideState.IN_PROGRESS == to) {
            return FROM_ARRIVED;
        }
        if (RideState.COMPLETED == to) {
            return FROM_IN_PROGRESS;
        }
        if (RideState.CANCELLED_BY_RIDER == to) {
            return BEFORE_THE_TRIP;
        }
        if (RideState.CANCELLED_BY_DRIVER == to) {
            return TAKEN_NOT_STARTED;
        }
        if (RideState.CANCELLED_BY_ADMIN == to) {
            return UNDER_WAY;
        }
        if (RideState.NO_DRIVERS == to) {
            return FROM_REQUESTED;
        }
        return NONE;
    }

    public static boolean allowed(RideState from, RideState to) {
        RideState[] sources = sources(to);
        for (int iter = 0; iter < sources.length; iter++) {
            if (sources[iter] == from) {
                return true;
            }
        }
        return false;
    }
}
