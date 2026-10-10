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

import com.codenameone.examples.wayline.api.RideStates;

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
    private static final String[] NONE = {};
    private static final String[] FROM_REQUESTED = {RideStates.REQUESTED};
    private static final String[] FROM_OFFERED = {RideStates.OFFERED};
    private static final String[] FROM_ACCEPTED = {RideStates.ACCEPTED};
    private static final String[] FROM_ARRIVED = {RideStates.ARRIVED};
    private static final String[] FROM_IN_PROGRESS = {RideStates.IN_PROGRESS};
    private static final String[] BEFORE_THE_TRIP = {RideStates.REQUESTED, RideStates.OFFERED,
        RideStates.ACCEPTED, RideStates.ARRIVED};
    private static final String[] UNDER_WAY = {RideStates.REQUESTED, RideStates.OFFERED,
        RideStates.ACCEPTED, RideStates.ARRIVED, RideStates.IN_PROGRESS};
    private static final String[] TAKEN_NOT_STARTED = {RideStates.ACCEPTED, RideStates.ARRIVED};

    private RideStateMachine() {
    }

    /// The states a ride may be in to move to `to`. Empty for a state nothing
    /// moves to.
    public static String[] sources(String to) {
        if (RideStates.OFFERED.equals(to)) {
            return FROM_REQUESTED;
        }
        if (RideStates.REQUESTED.equals(to) || RideStates.ACCEPTED.equals(to)) {
            return FROM_OFFERED;
        }
        if (RideStates.ARRIVED.equals(to)) {
            return FROM_ACCEPTED;
        }
        if (RideStates.IN_PROGRESS.equals(to)) {
            return FROM_ARRIVED;
        }
        if (RideStates.COMPLETED.equals(to)) {
            return FROM_IN_PROGRESS;
        }
        if (RideStates.CANCELLED_BY_RIDER.equals(to)) {
            return BEFORE_THE_TRIP;
        }
        if (RideStates.CANCELLED_BY_DRIVER.equals(to)) {
            return TAKEN_NOT_STARTED;
        }
        if (RideStates.CANCELLED_BY_ADMIN.equals(to)) {
            return UNDER_WAY;
        }
        if (RideStates.NO_DRIVERS.equals(to)) {
            return FROM_REQUESTED;
        }
        return NONE;
    }

    public static boolean allowed(String from, String to) {
        String[] sources = sources(to);
        for (int iter = 0; iter < sources.length; iter++) {
            if (sources[iter].equals(from)) {
                return true;
            }
        }
        return false;
    }
}
