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
package com.codenameone.developerguide.backend.wayline;

import com.codename1.backend.annotations.Component;
import com.codename1.orm.session.JpqlQuery;
import com.codename1.orm.session.Session;

// tag::wayline-repository[]
/// Where rides are read from and written to. It holds the queries and
/// nothing else.
@Component
public class RideRepository {
    public static final String RIDER = "rider";
    public static final String DRIVER = "driver";

    private static final String[] ACTIVE = {RideStates.REQUESTED, RideStates.OFFERED,
        RideStates.ACCEPTED, RideStates.ARRIVED, RideStates.IN_PROGRESS};

    private final Session session;

    public RideRepository(Session session) {
        this.session = session;
    }

    /// The ride of this id, or null when there is none.
    public Ride find(String id) {
        return session.find(Ride.class, id);
    }

    public void add(Ride ride) {
        session.persist(ride);
    }

    /// The ride a rider has under way, or null.
    public Ride activeForRider(String rider) {
        return session.query(Ride.class).eq("rider", rider).in("state", (Object[]) ACTIVE).first();
    }
    // end::wayline-repository[]

    // tag::wayline-ride-move[]
    /// Moves a ride to `to` if it is in one of the states `from`, and answers
    /// how many rows that changed: 1 for the caller that made the move, 0 for
    /// every caller that came second.
    public int move(String id, String[] from, String to, long now, String ownerField,
            String owner, String newDriver, Long newOfferExpiresAt, boolean offerOpen) {
        StringBuilder text = new StringBuilder("update Ride r set r.state = :to, "
                + "r.updatedAt = :now");
        if (newDriver != null) {
            text.append(", r.driver = :newDriver");
        }
        if (newOfferExpiresAt != null) {
            text.append(", r.offerExpiresAt = :newExpiry");
        }
        text.append(" where r.id = :id and r.state in :from");
        boolean owned = RIDER.equals(ownerField) || DRIVER.equals(ownerField);
        if (owned) {
            text.append(RIDER.equals(ownerField) ? " and r.rider = :owner"
                    : " and r.driver = :owner");
        }
        if (offerOpen) {
            text.append(" and r.offerExpiresAt >= :now");
        }
        JpqlQuery<Object> update = session.createQuery(text.toString())
                .setParameter("to", to).setParameter("now", Long.valueOf(now))
                .setParameter("id", id).setParameter("from", from);
        if (newDriver != null) {
            update.setParameter("newDriver", newDriver);
        }
        if (newOfferExpiresAt != null) {
            update.setParameter("newExpiry", newOfferExpiresAt);
        }
        if (owned) {
            update.setParameter("owner", owner);
        }
        return update.executeUpdate();
    }
    // end::wayline-ride-move[]
}
