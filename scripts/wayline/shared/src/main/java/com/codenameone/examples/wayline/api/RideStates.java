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
package com.codenameone.examples.wayline.api;

/// The states of a ride, as they are written in [RideDto#state].
///
/// Strings and not an enum, on purpose: a state is a word on the wire and in a
/// database column, and both sides compare it as one. An app built before a
/// state was added then meets a word it does not know instead of failing to
/// decode the whole ride.
public final class RideStates {
    /// No ride. Never stored; it answers "is there one?".
    public static final String NONE = "NONE";
    /// Asked for; no driver has been offered it yet.
    public static final String REQUESTED = "REQUESTED";
    /// Offered to one driver, who has a few seconds to answer.
    public static final String OFFERED = "OFFERED";
    /// A driver took it and is on the way to the pickup.
    public static final String ACCEPTED = "ACCEPTED";
    /// The driver is at the pickup.
    public static final String ARRIVED = "ARRIVED";
    /// The rider is in the car.
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED_BY_RIDER = "CANCELLED_BY_RIDER";
    public static final String CANCELLED_BY_DRIVER = "CANCELLED_BY_DRIVER";
    /// Stopped by an admin, at any point before it completed.
    public static final String CANCELLED_BY_ADMIN = "CANCELLED_BY_ADMIN";
    /// Nobody was near enough, or nobody took it.
    public static final String NO_DRIVERS = "NO_DRIVERS";

    private RideStates() {
    }

    /// Whether a ride in `state` is still going on: someone is waiting for a
    /// driver, or a driver is involved and the trip is not over.
    public static boolean isActive(String state) {
        return REQUESTED.equals(state) || OFFERED.equals(state) || ACCEPTED.equals(state)
                || ARRIVED.equals(state) || IN_PROGRESS.equals(state);
    }
}
