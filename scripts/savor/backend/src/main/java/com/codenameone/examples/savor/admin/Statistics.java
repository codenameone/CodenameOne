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
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.api.StatsSeriesDto;
import com.codenameone.examples.savor.api.UserDetailDto;
import com.codenameone.examples.savor.api.UserDto;
import com.codenameone.examples.savor.domain.CourierState;
import com.codenameone.examples.savor.domain.Restaurant;
import com.codenameone.examples.savor.courier.Couriers;
import com.codenameone.examples.savor.courier.Earnings;
import com.codenameone.examples.savor.onboarding.Applications;
import com.codenameone.examples.savor.repository.CourierStateRepository;
import com.codenameone.examples.savor.repository.ProfileRepository;
import com.codenameone.examples.savor.repository.CustomerOrderRepository;
import com.codenameone.examples.savor.repository.DeliveryEventRepository;
import com.codenameone.examples.savor.repository.DeliveryRepository;
import com.codenameone.examples.savor.repository.RestaurantRepository;
import com.codenameone.examples.savor.order.Quotes;
import com.codenameone.examples.savor.delivery.Matcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
/// ([Days]); [CustomerOrderRepository#timeline] says why that division is not left
/// to a query. A sample's ninety days fit in memory. A service whose do not
/// keeps a table of daily totals, and this class is where it would be read.
@Component
public class Statistics {
    static final int DEFAULT_DAYS = 14;
    static final int MAX_DAYS = 90;
    private static final int TOP_COURIERS = 5;
    private static final OrderState[] STATES = OrderState.values();

    private static final String PAID = "PAID";
    private static final OrderState[] ACTIVE = {OrderState.PLACED, OrderState.ACCEPTED,
        OrderState.READY, OrderState.PICKED_UP};

    private final ProfileRepository profiles;
    private final CourierStateRepository cars;
    private final DeliveryRepository deliveries;
    private final CustomerOrderRepository orders;
    private final DeliveryEventRepository events;
    private final Accounts accounts;
    private final Couriers couriers;
    private final Moderation moderation;
    private final Quotes fares;
    private final Applications applications;
    private final Earnings earnings;
    private final RestaurantRepository restaurants;

    public Statistics(ProfileRepository profiles, CourierStateRepository cars,
            DeliveryRepository deliveries, CustomerOrderRepository orders,
            DeliveryEventRepository events, Accounts accounts,
            Couriers couriers, Moderation moderation, Quotes fares, Applications applications,
            Earnings earnings, RestaurantRepository restaurants) {
        this.earnings = earnings;
        this.restaurants = restaurants;
        this.profiles = profiles;
        this.cars = cars;
        this.deliveries = deliveries;
        this.orders = orders;
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
        int today = (int) orders.countByPlacedAtGreaterThanEqual(Days.start(now));
        int active = (int) orders.countByStateIn(ACTIVE);
        int done = (int) orders.countByStateIn(new OrderState[] {OrderState.DELIVERED});
        long revenue = Rows.number(orders.totals(OrderState.DELIVERED, PAID));
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
        int[] dayOrders = new int[span];
        int[] dayDelivered = new int[span];
        int[] dayCancelled = new int[span];
        long[] dayRevenue = new long[span];
        int[] dayNewUsers = new int[span];
        int[] byHour = new int[24];
        // By day, by hour and by state, counted here from one row per order;
        // and the time from placing to the door, of each one that got there.
        int[] byState = new int[STATES.length];
        long delivered = 0L;
        long finished = 0L;
        long totalSum = 0L;
        long tookMillis = 0L;
        List<Object[]> timeline = orders.timeline(since);
        for (int iter = 0; iter < timeline.size(); iter++) {
            Object[] order = timeline.get(iter);
            long at = whole(order[1]);
            OrderState state = Rows.orderState(order[2]);
            byHour[(int) (at % Days.MILLIS / 3600000L)]++;
            if (state != null) {
                byState[state.ordinal()]++;
            }
            int index = (int) ((at - since) / Days.MILLIS);
            boolean arrived = OrderState.DELIVERED == state;
            if (arrived) {
                delivered++;
                finished++;
                totalSum += whole(order[3]);
                tookMillis += Math.max(0L, whole(order[5]) - at);
            } else if (ended(state)) {
                finished++;
            }
            if (index < 0 || index >= span) {
                continue;
            }
            dayOrders[index]++;
            if (arrived) {
                dayDelivered[index]++;
                if (PAID.equals(order[4])) {
                    dayRevenue[index] += whole(order[3]);
                }
            } else if (ended(state)) {
                dayCancelled[index]++;
            }
        }
        Map<String, int[]> completedBy = new HashMap<String, int[]>();
        List<Object[]> perCourier = deliveries.countByCourier(since, DeliveryState.COMPLETED);
        for (int iter = 0; iter < perCourier.size(); iter++) {
            completedBy.put(Rows.text(perCourier.get(iter)[0]),
                    new int[] {(int) whole(perCourier.get(iter)[1])});
        }
        List<Integer> hours = new ArrayList<Integer>();
        for (int iter = 0; iter < byHour.length; iter++) {
            hours.add(Integer.valueOf(byHour[iter]));
        }
        List<NameCountDto> statesSeen = new ArrayList<NameCountDto>();
        for (int iter = 0; iter < STATES.length; iter++) {
            // A state no order is in is left out; a chart has no slice for it.
            if (byState[iter] > 0) {
                statesSeen.add(new NameCountDto(STATES[iter].name(), byState[iter]));
            }
        }
        List<NameCountDto> byRestaurant = new ArrayList<NameCountDto>();
        List<Object[]> kitchens = orders.countByRestaurant(since);
        for (int iter = 0; iter < kitchens.size(); iter++) {
            // Counted by id, and named as the restaurant is called now: two
            // kitchens may share a name, and one may have been renamed.
            Restaurant kitchen = restaurants.findById(Rows.text(kitchens.get(iter)[0]));
            byRestaurant.add(new NameCountDto(kitchen == null ? Rows.text(kitchens.get(iter)[0])
                    : kitchen.getName(), (int) whole(kitchens.get(iter)[1])));
        }
        Collections.sort(byRestaurant, (a, b) -> Integer.compare(b.count(), a.count()));
        // Tested before dividing, and not left to an exception: dividing a
        // whole number by zero does not raise one on every runtime this runs on.
        long averageOrder = delivered == 0L ? 0L : totalSum / delivered;
        double averageDeliveryMinutes = delivered == 0L ? 0d
                : Math.round(tookMillis / 6000d / delivered) / 10d;
        double completionRate = finished == 0L ? 0d
                : Math.round(delivered * 100d / finished) / 100d;

        List<Long> signUps = profiles.signUps(since);
        for (int iter = 0; iter < signUps.size(); iter++) {
            int index = (int) ((signUps.get(iter).longValue() - since) / Days.MILLIS);
            if (index >= 0 && index < span) {
                dayNewUsers[index]++;
            }
        }
        List<DayStatsDto> daily = new ArrayList<DayStatsDto>();
        for (int iter = 0; iter < span; iter++) {
            daily.add(new DayStatsDto(Days.iso(since + iter * Days.MILLIS), dayOrders[iter],
                    dayDelivered[iter], dayCancelled[iter], dayRevenue[iter],
                    dayNewUsers[iter]));
        }

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
        return new StatsSeriesDto(daily, hours, statesSeen, byRestaurant, topCouriers,
                averageOrder, averageDeliveryMinutes, completionRate, applications.waiting(),
                (int) profiles.countByFlaggedTrue(), fares.currency());
    }

    /// One account, with what it has done and what was done to it.
    @Transactional(readOnly = true)
    public UserDetailDto user(String username) throws IOException {
        UserDto user = accounts.describe(username);
        int ordered = (int) orders.countByCustomer(username);
        int carried = (int) deliveries.countByCourier(username);
        // Called off by this account: as a customer, and as a courier who
        // gave a delivery back.
        int cancellations = (int) (orders.countByCustomerAndState(username,
                OrderState.CANCELLED_BY_CUSTOMER) + events.countByActorAndState(username,
                DeliveryState.CANCELLED_BY_COURIER));
        CourierState car = username == null || username.length() == 0
                || username.length() > 190 ? null : cars.findById(username);
        // A rating is a courier's; an account that has never delivered has none.
        double rating = car == null ? 0d : car.rating();
        long spent = Rows.number(orders.spent(username, OrderState.DELIVERED, PAID));
        // What the courier's own screens call earned, from the same sum: a
        // card delivery whose charge failed, is pending or was refunded, and a
        // tip whose charge has not come back, are not money the courier has.
        long earned = earnings.earned(username);
        return new UserDetailDto(user, ordered, carried, cancellations, rating, spent, earned,
                moderation.events(username));
    }

    /// Whether an order in `state` ended without reaching the door.
    private static boolean ended(OrderState state) {
        return OrderState.REJECTED == state || OrderState.CANCELLED_BY_CUSTOMER == state
                || OrderState.CANCELLED_BY_ADMIN == state || OrderState.NO_COURIERS == state;
    }

    /// A count or a sum as a query answers it.
    private static long whole(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

}
