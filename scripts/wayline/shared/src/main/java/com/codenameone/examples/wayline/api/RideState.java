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
