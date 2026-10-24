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
package com.codenameone.examples.savor.delivery;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.core.userdetails.UsernameNotFoundException;
import com.codenameone.examples.savor.Roles;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.domain.CourierApplication;
import com.codenameone.examples.savor.domain.CourierState;
import com.codenameone.examples.savor.domain.Delivery;
import com.codenameone.examples.savor.domain.DeliveryPassed;
import com.codenameone.examples.savor.onboarding.Applications;
import com.codenameone.examples.savor.geo.Geo;
import com.codenameone.examples.savor.repository.CourierApplicationRepository;
import com.codenameone.examples.savor.repository.CourierStateRepository;
import com.codenameone.examples.savor.repository.DeliveryPassedRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Finds the couriers near a point.
///
/// No spatial index and no spatial database: the query asks for the couriers on
/// line inside a box of latitude and longitude around the point, which an
/// ordinary index answers, and the handful it returns are measured and sorted
/// here. That runs unchanged on SQLite, PostgreSQL and MySQL, and it is enough
/// until one city has more couriers on line than fit in a query result -- at
/// which point the box becomes a geohash column and nothing else changes.
///
/// The box does not wrap around the 180th meridian. A service operating across
/// it would need a second box.
@Component
public class Matcher {
    /// A courier not heard from for this long is not there, whatever their
    /// status says: the app reports its position every few seconds while on
    /// line, and a phone that lost its signal cannot go off line.
    public static final long FRESH_MILLIS = 60000L;
    /// How far to look at first, and how far once the first look found nobody.
    static final double NEAR_METERS = 3000d;
    static final double FAR_METERS = 8000d;
    /// How long a delivery waits before the search widens.
    static final long WIDEN_AFTER_MILLIS = 20000L;

    /// The states of a delivery that keep its courier from being offered another.
    private static final DeliveryState[] WITH_DELIVERY = {DeliveryState.OFFERED, DeliveryState.ACCEPTED,
        DeliveryState.ARRIVED, DeliveryState.IN_PROGRESS};

    private final CourierStateRepository couriers;
    private final CourierApplicationRepository applications;
    private final DeliveryPassedRepository passes;
    private final JdbcUserDetailsManager users;

    public Matcher(CourierStateRepository couriers, CourierApplicationRepository applications,
            DeliveryPassedRepository passes,
            JdbcUserDetailsManager users) {
        this.couriers = couriers;
        this.applications = applications;
        this.passes = passes;
        this.users = users;
    }

    /// A courier who could be offered a delivery: where the car is, what kind it
    /// is, and how far it is from the point that was asked about.
    public static final class Candidate {
        public final String username;
        public final double lat;
        public final double lng;
        public final double heading;
        public final double meters;
        /// The courier's state as it was read, for whoever wants more of it.
        public final CourierState state;

        Candidate(CourierState state, double meters) {
            this.state = state;
            this.username = state.getUsername();
            this.lat = state.getLat();
            this.lng = state.getLng();
            this.heading = state.getHeading();
            this.meters = meters;
        }
    }

    /// The radius to search for a delivery that has been waiting `waitedMillis`.
    static double radius(long waitedMillis) {
        return waitedMillis < WIDEN_AFTER_MILLIS ? NEAR_METERS : FAR_METERS;
    }

    /// The couriers on line and free within `radiusMeters` of a point, nearest
    /// first, of any kind.
    @Transactional(readOnly = true)
    public List<Candidate> nearby(double lat, double lng, double radiusMeters)
            throws IOException {
        return free(lat, lng, radiusMeters);
    }

    /// The couriers a delivery could be offered to: on line, free, and in reach
    /// of the restaurant.
    ///
    /// A courier is offered deliveries only while their application stands approved
    /// and their account is not blocked. Both are read here, each time, and
    /// not trusted to have been put right elsewhere: an approval withdrawn or
    /// an account blocked takes a courier out of matching at once, whatever
    /// their state still says. The approval is part of the query. Whether the
    /// account is blocked is the sign-in's to say, and its tables are its own,
    /// so each courier in reach is asked about there.
    @Transactional(readOnly = true)
    private List<Candidate> free(double lat, double lng, double radiusMeters) throws IOException {
        double dLat = radiusMeters / Geo.METERS_PER_DEGREE;
        double shrink = Math.max(0.01d, Math.cos(Math.toRadians(lat)));
        double dLng = radiusMeters / (Geo.METERS_PER_DEGREE * shrink);
        List<CourierState> boxed = couriers.freeInBox(Instant.now().minusMillis(FRESH_MILLIS),
                lat - dLat, lat + dLat, lng - dLng, lng + dLng, Applications.APPROVED, WITH_DELIVERY);
        List<CourierState> inReach = new ArrayList<CourierState>();
        List<String> names = new ArrayList<String>();
        for (int iter = 0; iter < boxed.size(); iter++) {
            CourierState state = boxed.get(iter);
            if (Geo.meters(lat, lng, state.getLat(), state.getLng()) > radiusMeters) {
                // Inside the box but outside the circle: a corner of it.
                continue;
            }
            inReach.add(state);
            names.add(state.getUsername());
        }
        if (inReach.isEmpty()) {
            return new ArrayList<Candidate>();
        }
        Map<String, CourierApplication> cars = new HashMap<String, CourierApplication>();
        List<CourierApplication> rows = applications.findByUsernameIn(names);
        for (int iter = 0; iter < rows.size(); iter++) {
            cars.put(rows.get(iter).getUsername(), rows.get(iter));
        }
        List<Candidate> near = new ArrayList<Candidate>();
        for (int iter = 0; iter < inReach.size(); iter++) {
            CourierState state = inReach.get(iter);
            CourierApplication car = cars.get(state.getUsername());
            if (car == null || !Applications.APPROVED.equals(car.getStatus())) {
                // The application went between the two queries, or its
                // approval was withdrawn: the row stays, rejected.
                continue;
            }
            if (!enabled(state.getUsername())) {
                continue;
            }
            double meters = Geo.meters(lat, lng, state.getLat(), state.getLng());
            int at = 0;
            while (at < near.size() && near.get(at).meters <= meters) {
                at++;
            }
            near.add(at, new Candidate(state, meters));
        }
        return near;
    }

    /// Whether `courier` is still one a delivery could be offered to: on line and
    /// heard from lately, free, in reach, approved, not blocked, and fit for
    /// still approved. [#pick] read all of that in a transaction of its
    /// own, and any of it can have changed before the offer is made; the
    /// offer asks again once it holds the courier.
    @Transactional(readOnly = true)
    public boolean fits(Delivery delivery, String courier, long waitedMillis) throws IOException {
        List<Candidate> near = free(delivery.getPickupLat(), delivery.getPickupLng(),
                radius(waitedMillis));
        for (int iter = 0; iter < near.size(); iter++) {
            if (courier.equals(near.get(iter).username)) {
                return true;
            }
        }
        return false;
    }

    /// Whether the account of this name exists and has not been blocked.
    ///
    /// Read once: an account closed between asking whether it exists and
    /// loading it would otherwise throw, and turn a customer's request into a
    /// server error over a courier who is simply no longer there.
    boolean enabled(String username) {
        try {
            return users.loadUserByUsername(username).isEnabled();
        } catch (UsernameNotFoundException gone) {
            return false;
        }
    }

    /// Whether the account exists, is not blocked and still has the courier's
    /// role, read once as [#enabled] is.
    boolean mayDeliver(String username) {
        try {
            UserDetails user = users.loadUserByUsername(username);
            if (!user.isEnabled()) {
                return false;
            }
            for (GrantedAuthority authority : user.getAuthorities()) {
                if (("ROLE_" + Roles.COURIER).equals(authority.getAuthority())) {
                    return true;
                }
            }
            return false;
        } catch (UsernameNotFoundException gone) {
            return false;
        }
    }

    /// The nearest free courier fit for a delivery who has not already passed on it,
    /// or null when there is none in reach.
    @Transactional(readOnly = true)
    public String pick(Delivery delivery, long waitedMillis) throws IOException {
        List<Candidate> near = free(delivery.getPickupLat(), delivery.getPickupLng(),
                radius(waitedMillis));
        if (near.isEmpty()) {
            return null;
        }
        List<DeliveryPassed> rows = passes.findByDeliveryId(delivery.getId());
        List<String> passed = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            passed.add(rows.get(iter).getCourier());
        }
        for (int iter = 0; iter < near.size(); iter++) {
            String courier = near.get(iter).username;
            if (!passed.contains(courier)) {
                return courier;
            }
        }
        return null;
    }
}
