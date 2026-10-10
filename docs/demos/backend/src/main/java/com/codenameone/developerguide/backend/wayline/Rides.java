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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;

import java.io.IOException;

/// The part of the sample's ride service these examples call.
@Component
public class Rides {
    private final RideRepository rides;
    private final long offerMillis = 15000L;

    public Rides(RideRepository rides) {
        this.rides = rides;
    }

    FareQuoteDto quote(RideRequestDto request) throws IOException {
        return new FareQuoteDto();
    }

    RideDto request(String rider, RideRequestDto request) throws IOException {
        return new RideDto();
    }

    // tag::wayline-service-readonly[]
    @Transactional(readOnly = true)
    public RideDto activeForRider(String rider) throws IOException {
        Ride ride = rides.activeForRider(rider);
        return ride == null ? none() : toDto(ride);
    }
    // end::wayline-service-readonly[]

    // tag::wayline-service-transaction[]
    public RideDto cancelByRider(String rider, String id) throws IOException {
        Change change = cancelAsRider(rider, id);
        // The driver it was offered to or who had taken it is told as well.
        return tell(change);
    }

    @Transactional
    private Change cancelAsRider(String rider, String id) throws IOException {
        Ride before = owned(id, RideRepository.RIDER, rider);
        move(id, RideStates.CANCELLED_BY_RIDER, rider, RideRepository.RIDER, null, null);
        return new Change(id, RideStates.CANCELLED_BY_RIDER, rider, before.driver, "",
                view(id, rider));
    }
    // end::wayline-service-transaction[]

    // tag::wayline-dispatch-assign[]
    /// Offers a waiting ride to the driver a dispatcher picked. The driver
    /// still accepts or declines it on their own screen, and an offer nobody
    /// answers goes back to waiting like any other.
    public RideDto assign(String dispatcher, String id, String driver) throws IOException {
        return tell(assignTo(dispatcher, id, driver));
    }

    @Transactional
    private Change assignTo(String dispatcher, String id, String driver) throws IOException {
        Ride ride = rides.find(id);
        if (ride == null) {
            throw new ResponseStatusException(404, "No such ride");
        }
        Long lapses = Long.valueOf(System.currentTimeMillis() + offerMillis);
        if (!tryMove(id, RideStates.OFFERED, dispatcher, null, driver, lapses)) {
            throw new ResponseStatusException(409, "That ride is no longer waiting");
        }
        return new Change(id, RideStates.OFFERED, ride.rider, driver, "", view(id, dispatcher));
    }
    // end::wayline-dispatch-assign[]

    /// What a committed change was, for telling the people concerned.
    static final class Change {
        final String id;
        final String state;
        final String rider;
        final String driver;
        final String also;
        final RideDto view;

        Change(String id, String state, String rider, String driver, String also, RideDto view) {
            this.id = id;
            this.state = state;
            this.rider = rider;
            this.driver = driver;
            this.also = also;
            this.view = view;
        }
    }

    /// Tells the people concerned about a change that has been committed; the
    /// sample pushes it down the live channel here.
    private RideDto tell(Change change) {
        return change == null ? null : change.view;
    }

    /// Moves a ride to `to`, or answers 409 when its state does not allow it.
    private void move(String id, String to, String actor, String ownerField, String newDriver,
            Long newOfferExpiresAt) throws IOException {
        if (!tryMove(id, to, actor, ownerField, newDriver, newOfferExpiresAt)) {
            throw new ResponseStatusException(409, "The ride cannot be changed that way now");
        }
    }

    // tag::wayline-ride-trymove[]
    private boolean tryMove(String id, String to, String actor, String ownerField,
            String newDriver, Long newOfferExpiresAt) throws IOException {
        String[] sources = RideStateMachine.sources(to);
        if (sources.length == 0) {
            return false;
        }
        // An offer that has lapsed cannot be taken, even in the moment before
        // the sweep takes it back.
        boolean offerOpen = RideStates.ACCEPTED.equals(to);
        if (rides.move(id, sources, to, System.currentTimeMillis(), ownerField, actor, newDriver,
                newOfferExpiresAt, offerOpen) != 1) {
            return false;
        }
        event(id, to, actor);
        return true;
    }
    // end::wayline-ride-trymove[]

    /// Records the change in the ride's history; the sample writes a
    /// `RideEvent` here.
    private void event(String id, String state, String actor) throws IOException {
    }

    private Ride owned(String id, String field, String actor) {
        Ride ride = rides.find(id);
        if (ride == null || !actor.equals(RideRepository.RIDER.equals(field) ? ride.rider
                : ride.driver)) {
            throw new ResponseStatusException(404, "No such ride");
        }
        return ride;
    }

    private RideDto view(String id, String viewer) throws IOException {
        return toDto(rides.find(id));
    }

    private static RideDto none() {
        return new RideDto();
    }

    private static RideDto toDto(Ride ride) {
        RideDto dto = new RideDto();
        dto.id = ride.id;
        dto.state = ride.state;
        dto.driver = ride.driver;
        return dto;
    }
}
