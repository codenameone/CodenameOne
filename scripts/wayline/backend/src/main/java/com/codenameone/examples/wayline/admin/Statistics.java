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
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.account.Moderation;
import com.codenameone.examples.wayline.api.AdminStatsDto;
import com.codenameone.examples.wayline.api.DayStatsDto;
import com.codenameone.examples.wayline.api.DriverDto;
import com.codenameone.examples.wayline.api.NameCountDto;
import com.codenameone.examples.wayline.api.RideStates;
import com.codenameone.examples.wayline.api.StatsSeriesDto;
import com.codenameone.examples.wayline.api.UserDetailDto;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.driver.Drivers;
import com.codenameone.examples.wayline.driving.Applications;
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
/// ([Days]); [StatisticsRepository#timeline] says why that division is not left
/// to a query. A sample's ninety days fit in memory. A service whose do not
/// keeps a table of daily totals, and this class is where it would be read.
@Component
public class Statistics {
    static final int DEFAULT_DAYS = 14;
    static final int MAX_DAYS = 90;
    private static final int TOP_DRIVERS = 5;
    private static final String[] STATES = {RideStates.REQUESTED, RideStates.OFFERED,
        RideStates.ACCEPTED, RideStates.ARRIVED, RideStates.IN_PROGRESS, RideStates.COMPLETED,
        RideStates.CANCELLED_BY_RIDER, RideStates.CANCELLED_BY_DRIVER,
        RideStates.CANCELLED_BY_ADMIN, RideStates.NO_DRIVERS};

    private static final String PAID = "PAID";
    private static final String[] ACTIVE = {RideStates.REQUESTED, RideStates.OFFERED,
        RideStates.ACCEPTED, RideStates.ARRIVED, RideStates.IN_PROGRESS};

    private final StatisticsRepository numbers;
    private final Accounts accounts;
    private final Drivers drivers;
    private final Moderation moderation;
    private final Fares fares;
    private final Applications applications;

    public Statistics(StatisticsRepository numbers, Accounts accounts, Drivers drivers,
            Moderation moderation, Fares fares, Applications applications) {
        this.numbers = numbers;
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
        AdminStatsDto stats = new AdminStatsDto();
        stats.currency = fares.currency();
        stats.users = (int) numbers.profiles();
        // Who holds a role is the sign-in store's to say and not a table of
        // this application's, so the drivers are counted from the accounts as
        // the accounts service describes them.
        List<UserDto> everyone = accounts.all();
        for (int iter = 0; iter < everyone.size(); iter++) {
            if (everyone.get(iter).driver) {
                stats.drivers++;
            }
        }
        stats.driversOnline = (int) numbers.driversOnline(now - Matcher.FRESH_MILLIS);
        stats.ridesToday = (int) numbers.ridesSince(Days.start(now));
        stats.ridesActive = (int) numbers.ridesIn(ACTIVE);
        stats.ridesCompleted = (int) numbers.ridesIn(new String[] {RideStates.COMPLETED});
        stats.revenueCents = numbers.fares(RideStates.COMPLETED, PAID);
        return stats;
    }

    /// The last `days` days, today included and last. Fewer than one is taken
    /// as the default and more than [#MAX_DAYS] as that many.
    @Transactional(readOnly = true)
    public StatsSeriesDto series(int days) throws IOException {
        int span = days < 1 ? DEFAULT_DAYS : Math.min(days, MAX_DAYS);
        long today = Days.start(System.currentTimeMillis());
        long since = today - (span - 1) * Days.MILLIS;
        StatsSeriesDto dto = new StatsSeriesDto();
        dto.currency = fares.currency();
        dto.days = new ArrayList<DayStatsDto>();
        for (int iter = 0; iter < span; iter++) {
            DayStatsDto day = new DayStatsDto();
            day.date = Days.iso(since + iter * Days.MILLIS);
            dto.days.add(day);
        }
        int[] byHour = new int[24];
        // By day and by hour, counted here from one row per ride.
        Map<String, Long> requestedAt = new HashMap<String, Long>();
        List<Object[]> rides = numbers.timeline(since);
        for (int iter = 0; iter < rides.size(); iter++) {
            Object[] ride = rides.get(iter);
            long at = whole(ride[1]);
            String state = (String) ride[2];
            requestedAt.put((String) ride[0], Long.valueOf(at));
            int index = (int) ((at - since) / Days.MILLIS);
            DayStatsDto day = index >= 0 && index < span ? dto.days.get(index) : null;
            byHour[(int) (at % Days.MILLIS / 3600000L)]++;
            if (day == null) {
                continue;
            }
            day.rides++;
            if (RideStates.COMPLETED.equals(state)) {
                day.completed++;
                if (PAID.equals(ride[4])) {
                    day.revenue += whole(ride[3]);
                }
            } else if (state.startsWith("CANCELLED")) {
                day.cancelled++;
            }
        }
        // By state, by product and by driver, counted by the database.
        int[] byState = new int[STATES.length];
        long completed = 0L;
        long finished = 0L;
        List<Object[]> states = numbers.countByState(since);
        for (int iter = 0; iter < states.size(); iter++) {
            String state = (String) states.get(iter)[0];
            long count = whole(states.get(iter)[1]);
            for (int s = 0; s < STATES.length; s++) {
                if (STATES[s].equals(state)) {
                    byState[s] += (int) count;
                }
            }
            if (RideStates.COMPLETED.equals(state)) {
                completed += count;
                finished += count;
            } else if (state.startsWith("CANCELLED") || RideStates.NO_DRIVERS.equals(state)) {
                finished += count;
            }
        }
        int[] byProduct = new int[Fares.PRODUCTS.length];
        List<Object[]> products = numbers.countByProduct(since);
        for (int iter = 0; iter < products.size(); iter++) {
            for (int p = 0; p < Fares.PRODUCTS.length; p++) {
                if (Fares.PRODUCTS[p].equals(products.get(iter)[0])) {
                    byProduct[p] += (int) whole(products.get(iter)[1]);
                }
            }
        }
        Map<String, int[]> completedBy = new HashMap<String, int[]>();
        List<Object[]> perDriver = numbers.countByDriver(since, RideStates.COMPLETED);
        for (int iter = 0; iter < perDriver.size(); iter++) {
            completedBy.put((String) perDriver.get(iter)[0],
                    new int[] {(int) whole(perDriver.get(iter)[1])});
        }
        long fareSum = numbers.faresSince(since, RideStates.COMPLETED);
        dto.byHour = new ArrayList<Integer>();
        for (int iter = 0; iter < byHour.length; iter++) {
            dto.byHour.add(Integer.valueOf(byHour[iter]));
        }
        dto.byState = new ArrayList<NameCountDto>();
        for (int iter = 0; iter < STATES.length; iter++) {
            // A state no ride is in is left out; a chart has no slice for it.
            if (byState[iter] > 0) {
                dto.byState.add(named(STATES[iter], byState[iter]));
            }
        }
        dto.byProduct = new ArrayList<NameCountDto>();
        for (int iter = 0; iter < Fares.PRODUCTS.length; iter++) {
            dto.byProduct.add(named(Fares.PRODUCTS[iter], byProduct[iter]));
        }
        // Tested before dividing, and not left to an exception: dividing a
        // whole number by zero does not raise one on every runtime this runs on.
        dto.averageFare = completed == 0L ? 0L : fareSum / completed;
        dto.completionRate = finished == 0L ? 0d
                : Math.round(completed * 100d / finished) / 100d;

        List<Long> signUps = numbers.signUps(since);
        for (int iter = 0; iter < signUps.size(); iter++) {
            int index = (int) ((signUps.get(iter).longValue() - since) / Days.MILLIS);
            if (index >= 0 && index < span) {
                dto.days.get(index).newUsers++;
            }
        }

        // How long a rider waited for a driver to take the ride: each time one
        // was accepted, set against when it was asked for.
        List<Object[]> waits = numbers.reached(since, RideStates.ACCEPTED);
        long waited = 0L;
        int answered = 0;
        for (int iter = 0; iter < waits.size(); iter++) {
            Long asked = requestedAt.get((String) waits.get(iter)[0]);
            if (asked != null) {
                waited += Math.max(0L, whole(waits.get(iter)[1]) - asked.longValue());
                answered++;
            }
        }
        dto.averageWaitSeconds = answered == 0 ? 0d
                : Math.round(waited / 100d / answered) / 10d;

        dto.topDrivers = new ArrayList<DriverDto>();
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
                DriverDto driver = drivers.describe(best);
                // Within the period, which is what the chart is of.
                driver.rides = rode;
                dto.topDrivers.add(driver);
            }
        }
        dto.pendingApplications = applications.waiting();
        dto.flaggedUsers = (int) numbers.flaggedProfiles();
        return dto;
    }

    /// One account, with what it has done and what was done to it.
    @Transactional(readOnly = true)
    public UserDetailDto user(String username) throws IOException {
        UserDetailDto dto = new UserDetailDto();
        dto.user = accounts.describe(username);
        dto.rides = (int) numbers.ridesOf(username);
        dto.cancellations = (int) numbers.cancellationsOf(username,
                RideStates.CANCELLED_BY_RIDER, RideStates.CANCELLED_BY_DRIVER);
        DriverState car = numbers.driver(username);
        // A rating is a driver's; an account that has never driven has none.
        dto.rating = car == null ? 0d : car.rating();
        dto.spent = sum(numbers.spent(username, RideStates.COMPLETED, PAID));
        dto.earned = sum(numbers.earned(username, RideStates.COMPLETED));
        dto.events = moderation.events(username);
        return dto;
    }

    private static NameCountDto named(String name, int count) {
        NameCountDto dto = new NameCountDto();
        dto.name = name;
        dto.count = count;
        return dto;
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
