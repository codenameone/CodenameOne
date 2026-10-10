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

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codenameone.examples.wayline.domain.DriverApplication;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.driver.DriverRepository;
import com.codenameone.examples.wayline.geo.Geo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Finds the drivers near a point.
///
/// No spatial index and no spatial database: the query asks for the drivers on
/// line inside a box of latitude and longitude around the point, which an
/// ordinary index answers, and the handful it returns are measured and sorted
/// here. That runs unchanged on SQLite, PostgreSQL and MySQL, and it is enough
/// until one city has more drivers on line than fit in a query result -- at
/// which point the box becomes a geohash column and nothing else changes.
///
/// The box does not wrap around the 180th meridian. A service operating across
/// it would need a second box.
@Component
public class Matcher {
    /// A driver not heard from for this long is not there, whatever their
    /// status says: the app reports its position every few seconds while on
    /// line, and a phone that lost its signal cannot go off line.
    public static final long FRESH_MILLIS = 60000L;
    /// How far to look at first, and how far once the first look found nobody.
    static final double NEAR_METERS = 3000d;
    static final double FAR_METERS = 8000d;
    /// How long a ride waits before the search widens.
    static final long WIDEN_AFTER_MILLIS = 20000L;

    private final DriverRepository drivers;
    private final RideRepository rides;
    private final JdbcUserDetailsManager users;

    public Matcher(DriverRepository drivers, RideRepository rides, JdbcUserDetailsManager users) {
        this.drivers = drivers;
        this.rides = rides;
        this.users = users;
    }

    /// A driver who could be offered a ride: where the car is, what kind it
    /// is, and how far it is from the point that was asked about.
    public static final class Candidate {
        public final String username;
        public final double lat;
        public final double lng;
        public final double heading;
        /// The kind of car, as the driver's application has it.
        public final String product;
        public final double meters;
        /// The driver's state as it was read, for whoever wants more of it.
        public final DriverState state;

        Candidate(DriverState state, String product, double meters) {
            this.state = state;
            this.username = state.username;
            this.lat = state.lat;
            this.lng = state.lng;
            this.heading = state.heading;
            this.product = product;
            this.meters = meters;
        }
    }

    /// The radius to search for a ride that has been waiting `waitedMillis`.
    static double radius(long waitedMillis) {
        return waitedMillis < WIDEN_AFTER_MILLIS ? NEAR_METERS : FAR_METERS;
    }

    /// The drivers on line and free within `radiusMeters` of a point, nearest
    /// first, of any kind.
    @Transactional(readOnly = true)
    public List<Candidate> nearby(double lat, double lng, double radiusMeters)
            throws IOException {
        return nearby(lat, lng, radiusMeters, null, false, false, false);
    }

    /// The drivers a ride could be offered to: on line, free, in reach, and
    /// fit for it.
    ///
    /// A driver is offered rides only while their application stands approved
    /// and their account is not blocked. Both are read here, each time, and
    /// not trusted to have been put right elsewhere: an approval withdrawn or
    /// an account blocked takes a driver out of matching at once, whatever
    /// their state still says. The approval is part of the query. Whether the
    /// account is blocked is the sign-in's to say, and its tables are its own,
    /// so each of the handful the query answers is asked about there.
    ///
    /// @param product the kind of car the ride asks for, or null for any
    /// @param womenOnly whether the rider asked for a woman driver
    /// @param wheelchair whether the ride needs a car that takes a wheelchair
    /// @param pets whether the ride needs a driver who takes pets
    @Transactional(readOnly = true)
    public List<Candidate> nearby(double lat, double lng, double radiusMeters, String product,
            boolean womenOnly, boolean wheelchair, boolean pets) throws IOException {
        double dLat = radiusMeters / Geo.METERS_PER_DEGREE;
        double shrink = Math.max(0.01d, Math.cos(Math.toRadians(lat)));
        double dLng = radiusMeters / (Geo.METERS_PER_DEGREE * shrink);
        List<DriverState> boxed = drivers.freeInBox(System.currentTimeMillis() - FRESH_MILLIS,
                lat - dLat, lat + dLat, lng - dLng, lng + dLng, product, womenOnly, wheelchair,
                pets);
        List<DriverState> inReach = new ArrayList<DriverState>();
        List<String> names = new ArrayList<String>();
        for (int iter = 0; iter < boxed.size(); iter++) {
            DriverState state = boxed.get(iter);
            if (Geo.meters(lat, lng, state.lat, state.lng) > radiusMeters) {
                // Inside the box but outside the circle: a corner of it.
                continue;
            }
            if (!enabled(state.username)) {
                continue;
            }
            inReach.add(state);
            names.add(state.username);
        }
        List<DriverApplication> applications = drivers.applications(names);
        List<Candidate> near = new ArrayList<Candidate>();
        for (int iter = 0; iter < inReach.size(); iter++) {
            DriverState state = inReach.get(iter);
            String kind = null;
            for (int a = 0; a < applications.size(); a++) {
                if (state.username.equals(applications.get(a).username)) {
                    kind = applications.get(a).product;
                    break;
                }
            }
            if (kind == null) {
                // The application went between the two queries, and the
                // approval with it.
                continue;
            }
            double meters = Geo.meters(lat, lng, state.lat, state.lng);
            int at = 0;
            while (at < near.size() && near.get(at).meters <= meters) {
                at++;
            }
            near.add(at, new Candidate(state, kind, meters));
        }
        return near;
    }

    /// Whether the account of this name exists and has not been blocked.
    private boolean enabled(String username) {
        return users.userExists(username) && users.loadUserByUsername(username).isEnabled();
    }

    /// The nearest free driver fit for a ride who has not already passed on it,
    /// or null when there is none in reach.
    @Transactional(readOnly = true)
    public String pick(Ride ride, long waitedMillis) throws IOException {
        List<Candidate> near = nearby(ride.pickupLat, ride.pickupLng, radius(waitedMillis),
                ride.product, ride.womenOnly, ride.wheelchair, ride.pets);
        if (near.isEmpty()) {
            return null;
        }
        List<String> passed = rides.passedOn(ride.id);
        for (int iter = 0; iter < near.size(); iter++) {
            String driver = near.get(iter).username;
            if (!passed.contains(driver)) {
                return driver;
            }
        }
        return null;
    }
}
