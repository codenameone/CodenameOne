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
package com.codenameone.examples.wayline.admin;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.wayline.Days;
import com.codenameone.examples.wayline.Rows;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.account.Moderation;
import com.codenameone.examples.wayline.api.AdminStatsDto;
import com.codenameone.examples.wayline.api.DayStatsDto;
import com.codenameone.examples.wayline.api.DriverDto;
import com.codenameone.examples.wayline.api.NameCountDto;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.api.StatsSeriesDto;
import com.codenameone.examples.wayline.api.UserDetailDto;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.driver.Drivers;
import com.codenameone.examples.wayline.driving.Applications;
import com.codenameone.examples.wayline.repository.DriverStateRepository;
import com.codenameone.examples.wayline.repository.ProfileRepository;
import com.codenameone.examples.wayline.repository.RideEventRepository;
import com.codenameone.examples.wayline.repository.RideRepository;
import com.codenameone.examples.wayline.ride.Fares;
import com.codenameone.examples.wayline.ride.Matcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The numbers behind the admin's dashboard and charts, and the account of one
/// user.
///
/// Whatever can be counted by a column is counted by the database: rides by
/// state, by product and by driver, and every sum of money. The charts by day
/// and by hour of the day are counted here instead, from one row per ride of
/// the period. Times are stored as milliseconds and a day is a division
/// ([Days]); [RideRepository#timeline] says why that division is not left
/// to a query. A sample's ninety days fit in memory. A service whose do not
/// keeps a table of daily totals, and this class is where it would be read.
@Component
public class Statistics {
    static final int DEFAULT_DAYS = 14;
    static final int MAX_DAYS = 90;
    private static final int TOP_DRIVERS = 5;
    private static final RideState[] STATES = {RideState.REQUESTED, RideState.OFFERED,
        RideState.ACCEPTED, RideState.ARRIVED, RideState.IN_PROGRESS, RideState.COMPLETED,
        RideState.CANCELLED_BY_RIDER, RideState.CANCELLED_BY_DRIVER,
        RideState.CANCELLED_BY_ADMIN, RideState.NO_DRIVERS};

    private static final String PAID = "PAID";
    private static final RideState[] ACTIVE = {RideState.REQUESTED, RideState.OFFERED,
        RideState.ACCEPTED, RideState.ARRIVED, RideState.IN_PROGRESS};

    private final ProfileRepository profiles;
    private final DriverStateRepository cars;
    private final RideRepository rides;
    private final RideEventRepository events;
    private final Accounts accounts;
    private final Drivers drivers;
    private final Moderation moderation;
    private final Fares fares;
    private final Applications applications;

    public Statistics(ProfileRepository profiles, DriverStateRepository cars,
            RideRepository rides, RideEventRepository events, Accounts accounts,
            Drivers drivers, Moderation moderation, Fares fares, Applications applications) {
        this.profiles = profiles;
        this.cars = cars;
        this.rides = rides;
        this.events = events;
        this.applications = applications;
        this.accounts = accounts;
        this.drivers = drivers;
        this.moderation = moderation;
        this.fares = fares;
    }

    /// The handful of totals at the top of the admin's dashboard.
    @Transactional(readOnly = true)
    public AdminStatsDto dashboard() throws IOException {
        long now = System.currentTimeMillis();
        int users = (int) profiles.count();
        // Who holds a role is the sign-in store's to say and not a table of
        // this application's, so the drivers are counted from the accounts as
        // the accounts service describes them.
        int driving = 0;
        List<UserDto> everyone = accounts.all();
        for (int iter = 0; iter < everyone.size(); iter++) {
            if (everyone.get(iter).driver()) {
                driving++;
            }
        }
        int online = (int) cars.countByOnlineTrueAndLastSeenGreaterThanEqual(
                now - Matcher.FRESH_MILLIS);
        int today = (int) rides.countByRequestedAtGreaterThanEqual(Days.start(now));
        int active = (int) rides.countByStateIn(ACTIVE);
        int done = (int) rides.countByStateIn(new RideState[] {RideState.COMPLETED});
        long revenue = Rows.number(rides.fares(RideState.COMPLETED, PAID));
        return new AdminStatsDto(users, driving, online, today, active, done, revenue,
                fares.currency());
    }

    /// The last `days` days, today included and last. Fewer than one is taken
    /// as the default and more than [#MAX_DAYS] as that many.
    @Transactional(readOnly = true)
    public StatsSeriesDto series(int days) throws IOException {
        int span = days < 1 ? DEFAULT_DAYS : Math.min(days, MAX_DAYS);
        long today = Days.start(System.currentTimeMillis());
        long since = today - (span - 1) * Days.MILLIS;
        // Each day's figures, added up here and made into a DayStatsDto at the end.
        int[] dayRides = new int[span];
        int[] dayCompleted = new int[span];
        int[] dayCancelled = new int[span];
        long[] dayRevenue = new long[span];
        int[] dayNewUsers = new int[span];
        int[] byHour = new int[24];
        // By day and by hour, counted here from one row per ride.
        Map<String, Long> requestedAt = new HashMap<String, Long>();
        List<Object[]> timeline = rides.timeline(since);
        for (int iter = 0; iter < timeline.size(); iter++) {
            Object[] ride = timeline.get(iter);
            long at = whole(ride[1]);
            RideState state = Rows.state(ride[2]);
            requestedAt.put(Rows.text(ride[0]), Long.valueOf(at));
            int index = (int) ((at - since) / Days.MILLIS);
            byHour[(int) (at % Days.MILLIS / 3600000L)]++;
            if (index < 0 || index >= span) {
                continue;
            }
            dayRides[index]++;
            if (RideState.COMPLETED == state) {
                dayCompleted[index]++;
                if (PAID.equals(ride[4])) {
                    dayRevenue[index] += whole(ride[3]);
                }
            } else if (cancelled(state)) {
                dayCancelled[index]++;
            }
        }
        // By state, by product and by driver, counted by the database.
        int[] byState = new int[STATES.length];
        long completed = 0L;
        long finished = 0L;
        List<Object[]> states = rides.countByState(since);
        for (int iter = 0; iter < states.size(); iter++) {
            RideState state = Rows.state(states.get(iter)[0]);
            long count = whole(states.get(iter)[1]);
            for (int s = 0; s < STATES.length; s++) {
                if (STATES[s] == state) {
                    byState[s] += (int) count;
                }
            }
            if (RideState.COMPLETED == state) {
                completed += count;
                finished += count;
            } else if (cancelled(state) || RideState.NO_DRIVERS == state) {
                finished += count;
            }
        }
        int[] byProduct = new int[Fares.PRODUCTS.length];
        List<Object[]> products = rides.countByProduct(since);
        for (int iter = 0; iter < products.size(); iter++) {
            for (int p = 0; p < Fares.PRODUCTS.length; p++) {
                if (Fares.PRODUCTS[p].equals(products.get(iter)[0])) {
                    byProduct[p] += (int) whole(products.get(iter)[1]);
                }
            }
        }
        Map<String, int[]> completedBy = new HashMap<String, int[]>();
        List<Object[]> perDriver = rides.countByDriver(since, RideState.COMPLETED);
        for (int iter = 0; iter < perDriver.size(); iter++) {
            completedBy.put(Rows.text(perDriver.get(iter)[0]),
                    new int[] {(int) whole(perDriver.get(iter)[1])});
        }
        long fareSum = Rows.number(rides.faresSince(since, RideState.COMPLETED));
        List<Integer> hours = new ArrayList<Integer>();
        for (int iter = 0; iter < byHour.length; iter++) {
            hours.add(Integer.valueOf(byHour[iter]));
        }
        List<NameCountDto> statesSeen = new ArrayList<NameCountDto>();
        for (int iter = 0; iter < STATES.length; iter++) {
            // A state no ride is in is left out; a chart has no slice for it.
            if (byState[iter] > 0) {
                statesSeen.add(new NameCountDto(STATES[iter].name(), byState[iter]));
            }
        }
        List<NameCountDto> productsSold = new ArrayList<NameCountDto>();
        for (int iter = 0; iter < Fares.PRODUCTS.length; iter++) {
            productsSold.add(new NameCountDto(Fares.PRODUCTS[iter], byProduct[iter]));
        }
        // Tested before dividing, and not left to an exception: dividing a
        // whole number by zero does not raise one on every runtime this runs on.
        long averageFare = completed == 0L ? 0L : fareSum / completed;
        double completionRate = finished == 0L ? 0d
                : Math.round(completed * 100d / finished) / 100d;

        List<Long> signUps = profiles.signUps(since);
        for (int iter = 0; iter < signUps.size(); iter++) {
            int index = (int) ((signUps.get(iter).longValue() - since) / Days.MILLIS);
            if (index >= 0 && index < span) {
                dayNewUsers[index]++;
            }
        }
        List<DayStatsDto> daily = new ArrayList<DayStatsDto>();
        for (int iter = 0; iter < span; iter++) {
            daily.add(new DayStatsDto(Days.iso(since + iter * Days.MILLIS), dayRides[iter],
                    dayCompleted[iter], dayCancelled[iter], dayRevenue[iter],
                    dayNewUsers[iter]));
        }

        // How long a rider waited for a driver to take the ride: each time one
        // was accepted, set against when it was asked for.
        List<Object[]> waits = events.reached(since, RideState.ACCEPTED);
        long waited = 0L;
        int answered = 0;
        for (int iter = 0; iter < waits.size(); iter++) {
            Long asked = requestedAt.get(Rows.text(waits.get(iter)[0]));
            if (asked != null) {
                waited += Math.max(0L, whole(waits.get(iter)[1]) - asked.longValue());
                answered++;
            }
        }
        double averageWaitSeconds = answered == 0 ? 0d
                : Math.round(waited / 100d / answered) / 10d;

        List<DriverDto> topDrivers = new ArrayList<DriverDto>();
        for (int place = 0; place < TOP_DRIVERS && !completedBy.isEmpty(); place++) {
            String best = null;
            for (Map.Entry<String, int[]> entry : completedBy.entrySet()) {
                int[] top = best == null ? null : completedBy.get(best);
                // Ties go to the name that sorts first, so the order is the
                // same every time the chart is drawn.
                if (top == null || entry.getValue()[0] > top[0] || (entry.getValue()[0] == top[0]
                        && entry.getKey().compareTo(best) < 0)) {
                    best = entry.getKey();
                }
            }
            int rode = completedBy.remove(best)[0];
            if (accounts.exists(best)) {
                // Within the period, which is what the chart is of.
                topDrivers.add(drivers.describe(best, rode));
            }
        }
        return new StatsSeriesDto(daily, hours, statesSeen, productsSold, topDrivers,
                averageFare, averageWaitSeconds, completionRate, applications.waiting(),
                (int) profiles.countByFlaggedTrue(), fares.currency());
    }

    /// One account, with what it has done and what was done to it.
    @Transactional(readOnly = true)
    public UserDetailDto user(String username) throws IOException {
        UserDto user = accounts.describe(username);
        int taken = (int) rides.countByRiderOrDriver(username, username);
        int cancellations = (int) rides.countByRiderAndStateOrDriverAndState(username,
                RideState.CANCELLED_BY_RIDER, username, RideState.CANCELLED_BY_DRIVER);
        DriverState car = username == null || username.length() == 0
                || username.length() > 190 ? null : cars.findById(username);
        // A rating is a driver's; an account that has never driven has none.
        double rating = car == null ? 0d : car.rating();
        long spent = sum(rides.spent(username, RideState.COMPLETED, PAID));
        long earned = sum(rides.earned(username, RideState.COMPLETED));
        return new UserDetailDto(user, taken, cancellations, rating, spent, earned,
                moderation.events(username));
    }

    /// Whether a ride in `state` was called off, by whoever did it.
    private static boolean cancelled(RideState state) {
        return RideState.CANCELLED_BY_RIDER == state || RideState.CANCELLED_BY_DRIVER == state
                || RideState.CANCELLED_BY_ADMIN == state;
    }

    /// A count or a sum as a query answers it.
    private static long whole(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static long sum(Object[] values) {
        long total = 0L;
        for (int iter = 0; values != null && iter < values.length; iter++) {
            total += whole(values[iter]);
        }
        return total;
    }
}
