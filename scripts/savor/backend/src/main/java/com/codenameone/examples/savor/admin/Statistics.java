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
package com.codenameone.examples.savor.admin;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.savor.Days;
import com.codenameone.examples.savor.Rows;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.account.Moderation;
import com.codenameone.examples.savor.api.AdminStatsDto;
import com.codenameone.examples.savor.api.DayStatsDto;
import com.codenameone.examples.savor.api.CourierDto;
import com.codenameone.examples.savor.api.NameCountDto;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.api.StatsSeriesDto;
import com.codenameone.examples.savor.api.UserDetailDto;
import com.codenameone.examples.savor.api.UserDto;
import com.codenameone.examples.savor.domain.CourierState;
import com.codenameone.examples.savor.courier.Couriers;
import com.codenameone.examples.savor.courier.Earnings;
import com.codenameone.examples.savor.onboarding.Applications;
import com.codenameone.examples.savor.repository.CourierStateRepository;
import com.codenameone.examples.savor.repository.ProfileRepository;
import com.codenameone.examples.savor.repository.DeliveryEventRepository;
import com.codenameone.examples.savor.repository.DeliveryRepository;
import com.codenameone.examples.savor.delivery.Fares;
import com.codenameone.examples.savor.delivery.Matcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The numbers behind the admin's dashboard and charts, and the account of one
/// user.
///
/// Whatever can be counted by a column is counted by the database: deliveries by
/// state, by product and by courier, and every sum of money. The charts by day
/// and by hour of the day are counted here instead, from one row per delivery of
/// the period. Times are stored as milliseconds and a day is a division
/// ([Days]); [DeliveryRepository#timeline] says why that division is not left
/// to a query. A sample's ninety days fit in memory. A service whose do not
/// keeps a table of daily totals, and this class is where it would be read.
@Component
public class Statistics {
    static final int DEFAULT_DAYS = 14;
    static final int MAX_DAYS = 90;
    private static final int TOP_COURIERS = 5;
    private static final DeliveryState[] STATES = {DeliveryState.REQUESTED, DeliveryState.OFFERED,
        DeliveryState.ACCEPTED, DeliveryState.ARRIVED, DeliveryState.IN_PROGRESS, DeliveryState.COMPLETED,
        DeliveryState.CANCELLED_BY_CUSTOMER, DeliveryState.CANCELLED_BY_COURIER,
        DeliveryState.CANCELLED_BY_ADMIN, DeliveryState.NO_COURIERS};

    private static final String PAID = "PAID";
    private static final DeliveryState[] ACTIVE = {DeliveryState.REQUESTED, DeliveryState.OFFERED,
        DeliveryState.ACCEPTED, DeliveryState.ARRIVED, DeliveryState.IN_PROGRESS};

    private final ProfileRepository profiles;
    private final CourierStateRepository cars;
    private final DeliveryRepository deliveries;
    private final DeliveryEventRepository events;
    private final Accounts accounts;
    private final Couriers couriers;
    private final Moderation moderation;
    private final Fares fares;
    private final Applications applications;
    private final Earnings earnings;

    public Statistics(ProfileRepository profiles, CourierStateRepository cars,
            DeliveryRepository deliveries, DeliveryEventRepository events, Accounts accounts,
            Couriers couriers, Moderation moderation, Fares fares, Applications applications,
            Earnings earnings) {
        this.earnings = earnings;
        this.profiles = profiles;
        this.cars = cars;
        this.deliveries = deliveries;
        this.events = events;
        this.applications = applications;
        this.accounts = accounts;
        this.couriers = couriers;
        this.moderation = moderation;
        this.fares = fares;
    }

    /// The handful of totals at the top of the admin's dashboard.
    @Transactional(readOnly = true)
    public AdminStatsDto dashboard() throws IOException {
        long now = System.currentTimeMillis();
        int users = (int) profiles.count();
        // Who holds a role is the sign-in store's to say and not a table of
        // this application's, so the couriers are counted from the accounts as
        // the accounts service describes them.
        int onboarding = 0;
        List<UserDto> everyone = accounts.all();
        for (int iter = 0; iter < everyone.size(); iter++) {
            if (everyone.get(iter).courier()) {
                onboarding++;
            }
        }
        int online = (int) cars.countByOnlineTrueAndLastSeenGreaterThanEqual(
                now - Matcher.FRESH_MILLIS);
        int today = (int) deliveries.countByRequestedAtGreaterThanEqual(Days.start(now));
        int active = (int) deliveries.countByStateIn(ACTIVE);
        int done = (int) deliveries.countByStateIn(new DeliveryState[] {DeliveryState.COMPLETED});
        long revenue = Rows.number(deliveries.fares(DeliveryState.COMPLETED, PAID));
        return new AdminStatsDto(users, onboarding, online, today, active, done, revenue,
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
        int[] dayDeliveries = new int[span];
        int[] dayCompleted = new int[span];
        int[] dayCancelled = new int[span];
        long[] dayRevenue = new long[span];
        int[] dayNewUsers = new int[span];
        int[] byHour = new int[24];
        // By day and by hour, counted here from one row per delivery.
        Map<String, Long> requestedAt = new HashMap<String, Long>();
        List<Object[]> timeline = deliveries.timeline(since);
        for (int iter = 0; iter < timeline.size(); iter++) {
            Object[] delivery = timeline.get(iter);
            long at = whole(delivery[1]);
            DeliveryState state = Rows.state(delivery[2]);
            requestedAt.put(Rows.text(delivery[0]), Long.valueOf(at));
            int index = (int) ((at - since) / Days.MILLIS);
            byHour[(int) (at % Days.MILLIS / 3600000L)]++;
            if (index < 0 || index >= span) {
                continue;
            }
            dayDeliveries[index]++;
            if (DeliveryState.COMPLETED == state) {
                dayCompleted[index]++;
                if (PAID.equals(delivery[4])) {
                    dayRevenue[index] += whole(delivery[3]);
                }
            } else if (cancelled(state)) {
                dayCancelled[index]++;
            }
        }
        // By state, by product and by courier, counted by the database.
        int[] byState = new int[STATES.length];
        long completed = 0L;
        long finished = 0L;
        List<Object[]> states = deliveries.countByState(since);
        for (int iter = 0; iter < states.size(); iter++) {
            DeliveryState state = Rows.state(states.get(iter)[0]);
            long count = whole(states.get(iter)[1]);
            for (int s = 0; s < STATES.length; s++) {
                if (STATES[s] == state) {
                    byState[s] += (int) count;
                }
            }
            if (DeliveryState.COMPLETED == state) {
                completed += count;
                finished += count;
            } else if (cancelled(state) || DeliveryState.NO_COURIERS == state) {
                finished += count;
            }
        }
        int[] byProduct = new int[Fares.PRODUCTS.length];
        List<Object[]> products = deliveries.countByProduct(since);
        for (int iter = 0; iter < products.size(); iter++) {
            for (int p = 0; p < Fares.PRODUCTS.length; p++) {
                if (Fares.PRODUCTS[p].equals(products.get(iter)[0])) {
                    byProduct[p] += (int) whole(products.get(iter)[1]);
                }
            }
        }
        Map<String, int[]> completedBy = new HashMap<String, int[]>();
        List<Object[]> perCourier = deliveries.countByCourier(since, DeliveryState.COMPLETED);
        for (int iter = 0; iter < perCourier.size(); iter++) {
            completedBy.put(Rows.text(perCourier.get(iter)[0]),
                    new int[] {(int) whole(perCourier.get(iter)[1])});
        }
        long fareSum = Rows.number(deliveries.faresSince(since, DeliveryState.COMPLETED));
        List<Integer> hours = new ArrayList<Integer>();
        for (int iter = 0; iter < byHour.length; iter++) {
            hours.add(Integer.valueOf(byHour[iter]));
        }
        List<NameCountDto> statesSeen = new ArrayList<NameCountDto>();
        for (int iter = 0; iter < STATES.length; iter++) {
            // A state no delivery is in is left out; a chart has no slice for it.
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
            daily.add(new DayStatsDto(Days.iso(since + iter * Days.MILLIS), dayDeliveries[iter],
                    dayCompleted[iter], dayCancelled[iter], dayRevenue[iter],
                    dayNewUsers[iter]));
        }

        // How long a customer waited for a courier to take the delivery: each time one
        // was accepted, set against when it was asked for.
        List<Object[]> waits = events.reached(since, DeliveryState.ACCEPTED);
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

        List<CourierDto> topCouriers = new ArrayList<CourierDto>();
        for (int place = 0; place < TOP_COURIERS && !completedBy.isEmpty(); place++) {
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
                topCouriers.add(couriers.describe(best, rode));
            }
        }
        return new StatsSeriesDto(daily, hours, statesSeen, productsSold, topCouriers,
                averageFare, averageWaitSeconds, completionRate, applications.waiting(),
                (int) profiles.countByFlaggedTrue(), fares.currency());
    }

    /// One account, with what it has done and what was done to it.
    @Transactional(readOnly = true)
    public UserDetailDto user(String username) throws IOException {
        UserDto user = accounts.describe(username);
        int taken = (int) deliveries.countByCustomerOrCourier(username, username);
        int cancellations = (int) deliveries.countByCustomerAndStateOrCourierAndState(username,
                DeliveryState.CANCELLED_BY_CUSTOMER, username, DeliveryState.CANCELLED_BY_COURIER);
        CourierState car = username == null || username.length() == 0
                || username.length() > 190 ? null : cars.findById(username);
        // A rating is a courier's; an account that has never driven has none.
        double rating = car == null ? 0d : car.rating();
        long spent = sum(deliveries.spent(username, DeliveryState.COMPLETED, PAID));
        // What the courier's own screens call earned, from the same sum: a
        // card delivery whose charge failed, is pending or was refunded, and a
        // tip whose charge has not come back, are not money the courier has.
        long earned = earnings.earned(username);
        return new UserDetailDto(user, taken, cancellations, rating, spent, earned,
                moderation.events(username));
    }

    /// Whether a delivery in `state` was called off, by whoever did it.
    private static boolean cancelled(DeliveryState state) {
        return DeliveryState.CANCELLED_BY_CUSTOMER == state || DeliveryState.CANCELLED_BY_COURIER == state
                || DeliveryState.CANCELLED_BY_ADMIN == state;
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
