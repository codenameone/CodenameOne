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
package com.codenameone.examples.wayline.driver;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.wayline.Days;
import com.codenameone.examples.wayline.account.ProfileRepository;
import com.codenameone.examples.wayline.api.DriverDto;
import com.codenameone.examples.wayline.api.DriverStatusDto;
import com.codenameone.examples.wayline.api.EarningsDto;
import com.codenameone.examples.wayline.domain.DriverApplication;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.driving.Applications;
import com.codenameone.examples.wayline.geo.Geo;
import com.codenameone.examples.wayline.live.LiveHub;
import com.codenameone.examples.wayline.ride.Fares;
import com.codenameone.examples.wayline.ride.Matcher;
import com.codenameone.examples.wayline.ride.RideRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Drivers' live state and what they have earned.
@Component
public class Drivers {
    private static final long DAY_MILLIS = 86400000L;

    private final DriverRepository drivers;
    private final RideRepository rides;
    private final ProfileRepository profiles;
    private final LiveHub live;
    private final Fares fares;
    private final Applications applications;

    public Drivers(DriverRepository drivers, RideRepository rides, ProfileRepository profiles,
            LiveHub live, Fares fares, Applications applications) {
        this.drivers = drivers;
        this.rides = rides;
        this.profiles = profiles;
        this.applications = applications;
        this.live = live;
        this.fares = fares;
    }

    /// Records whether a driver is on line and where the car is. The app sends
    /// this every few seconds while on line; it is both the position riders
    /// follow and the sign the driver is still there.
    ///
    /// The rider being driven and the admin's fleet view are told once the
    /// report is stored, and not before: what they are told sends them here to
    /// read it.
    public DriverDto report(String driver, DriverStatusDto status) throws IOException {
        if (status == null || (status.online && !Geo.isPoint(status.lat, status.lng))) {
            throw new ResponseStatusException(400, "Going on line needs the car's position");
        }
        Report made = store(driver, status);
        DriverDto dto = made.driver;
        if (made.rideId != null) {
            live.driverMoved(made.rider, made.rideId, dto.lat, dto.lng, dto.heading);
        }
        live.fleetChanged(dto.username, dto.online, dto.lat, dto.lng, dto.heading);
        return dto;
    }

    /// A report as it was stored, and the ride the driver is on, if any. Not
    /// private: the class the build writes to begin and commit the transaction
    /// of [#store] has to be able to name what it answers.
    static final class Report {
        DriverDto driver;
        String rideId;
        String rider;
    }

    @Transactional
    private Report store(String driver, DriverStatusDto status) throws IOException {
        // The role lets a driver into this part of the API; the approval is
        // what lets them take rides. They are granted together, but they are
        // kept apart, and it is the approval that is asked for here.
        if (status.online && !applications.approved(driver)) {
            throw new ResponseStatusException(403, "Your application to drive is not approved");
        }
        Ride ride = rides.takenBy(driver);
        if (!status.online && ride != null) {
            throw new ResponseStatusException(409, "Finish the ride before going off line");
        }
        long now = System.currentTimeMillis();
        DriverState state = drivers.find(driver);
        if (state != null && state.online) {
            // The time since the last report was time on line, unless it was
            // so long ago that the app had plainly stopped reporting.
            long since = now - state.lastSeen;
            if (since > 0L && since < Matcher.FRESH_MILLIS) {
                drivers.addHours(driver, Days.number(now), since);
            }
        }
        boolean first = state == null;
        if (first) {
            state = new DriverState();
            state.username = driver;
        }
        state.online = status.online;
        state.lastSeen = now;
        // Off line keeps the last position: there is none to report. A first
        // report stores whatever came with it.
        if (status.online || first) {
            state.lat = status.lat;
            state.lng = status.lng;
            state.heading = status.heading;
        }
        if (first) {
            drivers.add(state);
        }
        Report made = new Report();
        made.driver = view(driver, state);
        if (ride != null) {
            made.rideId = ride.id;
            made.rider = ride.rider;
        }
        return made;
    }

    @Transactional(readOnly = true)
    public DriverDto describe(String driver) throws IOException {
        return view(driver, drivers.find(driver));
    }

    private DriverDto view(String driver, DriverState state) {
        Profile profile = profiles.find(driver);
        if (profile == null) {
            throw new ResponseStatusException(404, "No such driver");
        }
        DriverDto dto = toDto(profile, drivers.application(driver), state,
                (int) rides.completedBy(driver));
        dto.acceptanceRate = acceptanceRate(rides.acceptedBy(driver), rides.passedBy(driver));
        return dto;
    }

    /// The share of the offers a driver answered that they took: 1 before they
    /// have answered any. An offer left to lapse counts as passed on.
    static double acceptanceRate(long accepted, long passed) {
        long offers = accepted + passed;
        return offers == 0L ? 1d : Math.round(accepted * 100d / offers) / 100d;
    }

    /// Every account that has driven or gone on line, for the admin's fleet
    /// view; those on line first.
    @Transactional(readOnly = true)
    public List<DriverDto> all() throws IOException {
        Map<String, Long> done = rides.completedByDriver();
        Map<String, Long> accepted = rides.acceptedByDriver();
        Map<String, Long> passed = rides.passedByDriver();
        Map<String, Profile> people = new HashMap<String, Profile>();
        List<Profile> profilesOfAll = drivers.profilesOfAll();
        for (int iter = 0; iter < profilesOfAll.size(); iter++) {
            people.put(profilesOfAll.get(iter).username, profilesOfAll.get(iter));
        }
        Map<String, DriverApplication> made = new HashMap<String, DriverApplication>();
        List<DriverApplication> applicationsOfAll = drivers.applicationsOfAll();
        for (int iter = 0; iter < applicationsOfAll.size(); iter++) {
            made.put(applicationsOfAll.get(iter).username, applicationsOfAll.get(iter));
        }
        List<DriverState> states = drivers.all();
        List<DriverDto> out = new ArrayList<DriverDto>();
        for (int iter = 0; iter < states.size(); iter++) {
            DriverState state = states.get(iter);
            Profile profile = people.get(state.username);
            if (profile == null) {
                // A state left behind by an account that is gone.
                continue;
            }
            Long rode = done.get(state.username);
            DriverDto dto = toDto(profile, made.get(state.username), state,
                    rode == null ? 0 : (int) rode.longValue());
            Long took = accepted.get(state.username);
            Long left = passed.get(state.username);
            dto.acceptanceRate = acceptanceRate(took == null ? 0L : took.longValue(),
                    left == null ? 0L : left.longValue());
            out.add(dto);
        }
        return out;
    }

    /// What a driver's completed, paid rides came to. "Today" is the day in
    /// UTC; a service in one city would use that city's midnight.
    @Transactional(readOnly = true)
    public EarningsDto earnings(String driver) throws IOException {
        long now = System.currentTimeMillis();
        EarningsDto dto = new EarningsDto();
        dto.currency = fares.currency();
        dto.todayCents = rides.paidSince(driver, now - now % DAY_MILLIS);
        dto.weekCents = rides.paidSince(driver, now - 7 * DAY_MILLIS);
        long[] total = rides.paidTotals(driver);
        dto.rides = (int) total[0];
        dto.totalCents = total[1];
        return dto;
    }

    /// @param application the account's application to drive, or null
    /// @param car the account's state, or null when it has never gone on line
    private static DriverDto toDto(Profile profile, DriverApplication application,
            DriverState car, int rides) {
        DriverDto dto = new DriverDto();
        dto.username = profile.username;
        dto.displayName = profile.displayName;
        dto.vehicle = profile.vehicle;
        dto.plate = profile.plate;
        dto.rides = rides;
        dto.approved = application != null && Applications.APPROVED.equals(application.status);
        // An account with no application has no product; the app is told the
        // standard one so that it always has a word it knows.
        String product = application == null ? "" : application.product;
        dto.product = product.length() == 0 ? Fares.STANDARD : product;
        dto.gender = profile.gender;
        dto.accessible = application != null && application.wheelchair;
        dto.flagged = profile.flagged;
        dto.acceptanceRate = 1d;
        dto.rating = 5d;
        if (car != null) {
            dto.lastSeen = car.lastSeen;
            // On line means said so and heard from lately; see Matcher.
            dto.online = car.online
                    && System.currentTimeMillis() - dto.lastSeen < Matcher.FRESH_MILLIS;
            dto.lat = car.lat;
            dto.lng = car.lng;
            dto.heading = car.heading;
            dto.rating = car.rating();
        }
        return dto;
    }
}
