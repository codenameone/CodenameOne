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
import com.codenameone.examples.wayline.Rows;
import com.codenameone.examples.wayline.api.DriverDto;
import com.codenameone.examples.wayline.api.DriverStatusDto;
import com.codenameone.examples.wayline.api.EarningsDto;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.domain.DriverApplication;
import com.codenameone.examples.wayline.domain.DriverHours;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.driving.Applications;
import com.codenameone.examples.wayline.geo.Geo;
import com.codenameone.examples.wayline.live.LiveHub;
import com.codenameone.examples.wayline.ride.Fares;
import com.codenameone.examples.wayline.repository.DriverApplicationRepository;
import com.codenameone.examples.wayline.repository.DriverHoursRepository;
import com.codenameone.examples.wayline.repository.DriverStateRepository;
import com.codenameone.examples.wayline.repository.ProfileRepository;
import com.codenameone.examples.wayline.repository.RideEventRepository;
import com.codenameone.examples.wayline.repository.RidePassedRepository;
import com.codenameone.examples.wayline.repository.RideRepository;
import com.codenameone.examples.wayline.ride.Matcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Drivers' live state and what they have earned.
@Component
public class Drivers {
    private static final long DAY_MILLIS = 86400000L;

    /// The states of a ride a driver has taken and has yet to finish.
    private static final RideState[] TAKEN = {RideState.ACCEPTED, RideState.ARRIVED,
        RideState.IN_PROGRESS};

    private final DriverStateRepository drivers;
    private final DriverHoursRepository hours;
    private final DriverApplicationRepository made;
    private final RideRepository rides;
    private final RideEventRepository events;
    private final RidePassedRepository passes;
    private final ProfileRepository profiles;
    private final LiveHub live;
    private final Fares fares;
    private final Applications applications;

    public Drivers(DriverStateRepository drivers, DriverHoursRepository hours,
            DriverApplicationRepository made, RideRepository rides, RideEventRepository events,
            RidePassedRepository passes, ProfileRepository profiles, LiveHub live, Fares fares,
            Applications applications) {
        this.drivers = drivers;
        this.hours = hours;
        this.made = made;
        this.rides = rides;
        this.events = events;
        this.passes = passes;
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
        if (status == null || (status.online() && !Geo.isPoint(status.lat(), status.lng()))) {
            throw new ResponseStatusException(400, "Going on line needs the car's position");
        }
        Report made = store(driver, status);
        DriverDto dto = made.driver;
        if (made.rideId != null) {
            live.driverMoved(made.rider, made.rideId, dto.lat(), dto.lng(), dto.heading());
        }
        live.fleetChanged(dto.username(), dto.online(), dto.lat(), dto.lng(), dto.heading());
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
        // Two reports from one driver take turns, before anything is read: read together,
        // both would count the same stretch since the last one as time on
        // line. The clock is read after the wait, so reports are also stored
        // in the order they were let through and none moves the time back.
        //
        // A driver's first report has no state row to hold, so two first
        // reports would both find none and both insert one under the same
        // name. They take turns on the profile instead, the order blocking an
        // account takes them in, and the second then finds the first's row.
        if (drivers.hold(driver) == 0) {
            profiles.hold(driver);
        }
        // The role lets a driver into this part of the API; the approval is
        // what lets them take rides. They are granted together, but they are
        // kept apart, and it is the approval that is asked for here.
        if (status.online() && !applications.approved(driver)) {
            throw new ResponseStatusException(403, "Your application to drive is not approved");
        }
        Ride ride = rides.findFirstByDriverAndStateIn(driver, TAKEN);
        if (!status.online() && ride != null) {
            throw new ResponseStatusException(409, "Finish the ride before going off line");
        }
        long now = System.currentTimeMillis();
        DriverState state = drivers.findById(driver);
        // The time since the last report was time on line, unless it was so
        // long ago that the app had plainly stopped reporting. It is counted
        // here, from the row as it was, and added last, once the row has been
        // changed. Adding it is a bulk update that leaves the session as it
        // is, so the row read here stays managed and its change is written
        // either way; a statement marked clearAutomatically would detach it,
        // and a change made after that would be lost.
        long since = state != null && state.isOnline() ? now - state.getLastSeen() : 0L;
        boolean first = state == null;
        if (first) {
            state = new DriverState();
            state.setUsername(driver);
        }
        state.setOnline(status.online());
        state.setLastSeen(now);
        // Off line keeps the last position: there is none to report. A first
        // report stores whatever came with it.
        if (status.online() || first) {
            state.setLat(status.lat());
            state.setLng(status.lng());
            state.setHeading(status.heading());
        }
        if (first) {
            drivers.insert(state);
        }
        if (since > 0L && since < Matcher.FRESH_MILLIS) {
            // Cut at midnight: a stretch on line across it belongs half to
            // each day, not all to the new one.
            long[] days = Days.split(now - since, now);
            for (int iter = 0; iter < days.length; iter += 2) {
                addHours(driver, days[iter], days[iter + 1]);
            }
        }
        Report made = new Report();
        made.driver = view(driver, state);
        if (ride != null) {
            made.rideId = ride.getId();
            made.rider = ride.getRider();
        }
        return made;
    }

    @Transactional(readOnly = true)
    public DriverDto describe(String driver) throws IOException {
        return view(driver, drivers.findById(driver));
    }

    /// A driver with `rides` given rather than counted: the rides of a period
    /// the caller is describing, not of all time.
    @Transactional(readOnly = true)
    public DriverDto describe(String driver, int rides) throws IOException {
        return view(driver, drivers.findById(driver), rides);
    }

    private DriverDto view(String driver, DriverState state) {
        return view(driver, state, (int) rides.countByDriverAndState(driver,
                RideState.COMPLETED));
    }

    private DriverDto view(String driver, DriverState state, int completed) {
        Profile profile = profiles.findById(driver);
        if (profile == null) {
            throw new ResponseStatusException(404, "No such driver");
        }
        return toDto(profile, made.findById(driver), state, completed,
                acceptanceRate(events.countByActorAndState(driver, RideState.ACCEPTED),
                        passes.countByDriver(driver)));
    }

    /// Adds time on line to a driver's day. The addition is the database's,
    /// so reports that overlap are both counted.
    private void addHours(String driver, long dayNumber, long millis) {
        if (hours.addMillis(driver, dayNumber, millis) == 0) {
            DriverHours day = new DriverHours();
            day.setUsername(driver);
            day.setDayNumber(dayNumber);
            day.setMillis(millis);
            hours.insert(day);
        }
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
        Map<String, Long> done = Rows.counts(rides.countByDriver(RideState.COMPLETED));
        Map<String, Long> accepted = Rows.counts(events.countByActor(RideState.ACCEPTED));
        Map<String, Long> passed = Rows.counts(passes.countByDriver());
        Map<String, Profile> people = new HashMap<String, Profile>();
        List<Profile> profilesOfAll = profiles.drivers();
        for (int iter = 0; iter < profilesOfAll.size(); iter++) {
            people.put(profilesOfAll.get(iter).getUsername(), profilesOfAll.get(iter));
        }
        Map<String, DriverApplication> forms = new HashMap<String, DriverApplication>();
        List<DriverApplication> applicationsOfAll = made.ofDrivers();
        for (int iter = 0; iter < applicationsOfAll.size(); iter++) {
            forms.put(applicationsOfAll.get(iter).getUsername(), applicationsOfAll.get(iter));
        }
        List<DriverState> states = drivers.onLineFirst();
        List<DriverDto> out = new ArrayList<DriverDto>();
        for (int iter = 0; iter < states.size(); iter++) {
            DriverState state = states.get(iter);
            Profile profile = people.get(state.getUsername());
            if (profile == null) {
                // A state left behind by an account that is gone.
                continue;
            }
            Long rode = done.get(state.getUsername());
            Long took = accepted.get(state.getUsername());
            Long left = passed.get(state.getUsername());
            out.add(toDto(profile, forms.get(state.getUsername()), state,
                    rode == null ? 0 : (int) rode.longValue(),
                    acceptanceRate(took == null ? 0L : took.longValue(),
                            left == null ? 0L : left.longValue())));
        }
        return out;
    }

    /// What a driver's completed, paid rides earned them: their share of
    /// each fare, without tips. "Today" is the day in UTC; a service in one
    /// city would use that city's midnight.
    @Transactional(readOnly = true)
    public EarningsDto earnings(String driver) throws IOException {
        long now = System.currentTimeMillis();
        // The day's trips and the day's money from one query, so the header
        // that shows them together counts the rides that made the sum.
        Object[] day = rides.paidTotalsSince(driver, now - now % DAY_MILLIS);
        int todayCount = day == null ? 0 : (int) Rows.number(day[0]);
        long today = day == null ? 0L : Rows.number(day[1]);
        long week = Rows.number(rides.paidSince(driver, now - 7 * DAY_MILLIS));
        Object[] total = rides.paidTotals(driver);
        int count = total == null ? 0 : (int) Rows.number(total[0]);
        long all = total == null ? 0L : Rows.number(total[1]);
        return new EarningsDto(today, week, all, count, todayCount, fares.currency());
    }

    /// @param application the account's application to drive, or null
    /// @param car the account's state, or null when it has never gone on line
    private static DriverDto toDto(Profile profile, DriverApplication application,
            DriverState car, int rides, double acceptanceRate) {
        boolean approved = application != null
                && Applications.APPROVED.equals(application.getStatus());
        // An account with no application has no product; the app is told the
        // standard one so that it always has a word it knows.
        String product = application == null ? "" : application.getProduct();
        boolean accessible = application != null && application.isWheelchair();
        boolean online = false;
        double lat = 0d;
        double lng = 0d;
        double heading = 0d;
        double rating = 5d;
        long lastSeen = 0L;
        if (car != null) {
            lastSeen = car.getLastSeen();
            // On line means said so and heard from lately; see Matcher.
            online = car.isOnline()
                    && System.currentTimeMillis() - lastSeen < Matcher.FRESH_MILLIS;
            lat = car.getLat();
            lng = car.getLng();
            heading = car.getHeading();
            rating = car.rating();
        }
        return new DriverDto(profile.getUsername(), profile.getDisplayName(),
                profile.getVehicle(), profile.getPlate(), online, lat, lng, heading, rating,
                lastSeen, rides, approved, product.length() == 0 ? Fares.STANDARD : product,
                profile.getGender(), accessible, acceptanceRate, profile.isFlagged());
    }
}
