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
package com.codenameone.examples.wayline.ride;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.wayline.Days;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.api.PricingDto;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.domain.DriverApplication;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.domain.Payment;
import com.codenameone.examples.wayline.domain.Payout;
import com.codenameone.examples.wayline.domain.PayoutAccount;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.domain.RideEvent;
import com.codenameone.examples.wayline.domain.RidePassed;
import com.codenameone.examples.wayline.geo.Geo;
import com.codenameone.examples.wayline.pay.Payments;
import com.codenameone.examples.wayline.repository.DriverApplicationRepository;
import com.codenameone.examples.wayline.repository.DriverStateRepository;
import com.codenameone.examples.wayline.repository.PaymentRepository;
import com.codenameone.examples.wayline.repository.PayoutAccountRepository;
import com.codenameone.examples.wayline.repository.PayoutRepository;
import com.codenameone.examples.wayline.repository.RideEventRepository;
import com.codenameone.examples.wayline.repository.RidePassedRepository;
import com.codenameone.examples.wayline.repository.RideRepository;

import java.io.IOException;
import java.time.Instant;

/// Two weeks of rides that were never taken, so that the admin's charts, a
/// driver's earnings and a rider's receipts have something in them the first
/// time a development server is opened.
///
/// Stored whole, as finished rides with their events and payments, in one
/// transaction: there is no way to take a ride last Tuesday through the API. The
/// numbers come from a fixed sequence and not from a random source, so every
/// server starts with the same fortnight, moved to end today.
@Component
public class DemoRides {
    private static final int DAYS = 14;
    private static final Object[][] PLACES = {
        {"Ferry Building", 37.7955, -122.3937},
        {"Pier 39", 37.8087, -122.4098},
        {"Coit Tower", 37.8024, -122.4058},
        {"Ghirardelli Square", 37.8059, -122.4229},
        {"Lombard Street", 37.8021, -122.4187},
        {"Palace of Fine Arts", 37.8029, -122.4484},
        {"Union Square", 37.7880, -122.4075},
        {"Oracle Park", 37.7786, -122.3893},
        {"Fort Mason", 37.8066, -122.4310},
        {"Transamerica Pyramid", 37.7952, -122.4028}};
    /// The hours rides are asked for at, the busy ones more than once.
    private static final int[] HOURS = {7, 8, 8, 9, 12, 13, 15, 17, 18, 18, 19, 20, 22, 23};

    private final RideRepository rides;
    private final RideEventRepository events;
    private final RidePassedRepository passes;
    private final PaymentRepository paid;
    private final DriverStateRepository cars;
    private final DriverApplicationRepository applications;
    private final PayoutAccountRepository payoutAccounts;
    private final PayoutRepository payouts;
    private final Fares fares;
    private final Payments payments;
    private long sequence = 20240229L;

    public DemoRides(RideRepository rides, RideEventRepository events,
            RidePassedRepository passes, PaymentRepository paid, DriverStateRepository cars,
            DriverApplicationRepository applications, PayoutAccountRepository payoutAccounts,
            PayoutRepository payouts, Fares fares, Payments payments) {
        this.rides = rides;
        this.events = events;
        this.passes = passes;
        this.paid = paid;
        this.cars = cars;
        this.applications = applications;
        this.payoutAccounts = payoutAccounts;
        this.payouts = payouts;
        this.fares = fares;
        this.payments = payments;
    }

    /// Writes the fortnight, shared out among these riders and drivers. The
    /// drivers are approved ones, and each ride is of its driver's kind of car.
    ///
    /// @param untilToday false ends the fortnight yesterday. A test that
    ///     compares a chart with a picture of it asks for that: rides of today
    ///     are moved back to before now, so today's bars would depend on the
    ///     hour the server was started at.
    @Transactional
    public void seed(String[] riders, String[] drivers, boolean untilToday) throws IOException {
        long now = System.currentTimeMillis();
        long today = Days.start(now) - (untilToday ? 0L : Days.MILLIS);
        PricingDto pricing = fares.pricing();
        long[] stars = new long[drivers.length];
        long[] rated = new long[drivers.length];
        long[] earnedEarlier = new long[drivers.length];
        long[] lastSeen = new long[drivers.length];
        String[] products = new String[drivers.length];
        for (int iter = 0; iter < drivers.length; iter++) {
            DriverApplication made = applications.findById(drivers[iter]);
            products[iter] = made == null ? Fares.STANDARD : made.getProduct();
        }
        int count = 0;
        for (int day = DAYS - 1; day >= 0; day--) {
            int asked = 3 + next(4);
            for (int number = 0; number < asked; number++, count++) {
                int riderIndex = next(riders.length);
                int driverIndex = next(drivers.length);
                String rider = riders[riderIndex];
                String driver = drivers[driverIndex];
                int from = next(PLACES.length);
                int to = (from + 1 + next(PLACES.length - 1)) % PLACES.length;
                double meters = Geo.meters(real(from, 1), real(from, 2), real(to, 1), real(to, 2))
                        * Fares.ROAD_FACTOR;
                double seconds = meters / Fares.METERS_PER_SECOND;
                Fares.Price price = Fares.price(pricing, meters, seconds, products[driverIndex]);
                long wait = (20 + next(100)) * 1000L;
                long pickup = (120 + next(240)) * 1000L;
                long whole = wait + pickup + (long) (seconds * 1000d);
                long at = today - day * Days.MILLIS + HOURS[next(HOURS.length)] * 3600000L
                        + next(3600) * 1000L;
                // Nothing here happens in the future: a ride that would end
                // after now is moved back until it has ended.
                at = Math.min(at, now - whole - 60000L);

                int roll = next(20);
                RideState state = roll < 15 ? RideState.COMPLETED
                        : roll == 17 ? RideState.CANCELLED_BY_DRIVER
                        : roll == 18 ? RideState.NO_DRIVERS : RideState.CANCELLED_BY_RIDER;
                boolean completed = RideState.COMPLETED == state;
                // A rider who cancels does it before a driver is found half the
                // time, and after one has taken the ride the other half.
                boolean taken = completed || RideState.CANCELLED_BY_DRIVER == state
                        || (RideState.CANCELLED_BY_RIDER == state && next(2) == 0);
                String[] method = next(3) == 0 ? new String[] {Payments.CASH, Payments.CASH_LABEL}
                        : payments.choose(rider, null);
                boolean cash = Payments.CASH.equals(method[0]);
                long tip = completed && !cash && next(3) == 0 ? price.fare * 15L / 100L : 0L;
                int given = 0;
                if (completed && next(5) != 0) {
                    int liked = next(10);
                    given = liked == 0 ? 3 : liked < 4 ? 4 : 5;
                    stars[driverIndex] += given;
                    rated[driverIndex]++;
                }
                long ended = completed ? at + whole : taken ? at + wait + pickup / 2L
                        : RideState.NO_DRIVERS == state ? at + 90000L : at + 30000L;
                String id = Ids.next();
                Ride ride = new Ride();
                ride.setId(id);
                ride.setState(state);
                ride.setRider(rider);
                ride.setDriver(taken ? driver : "");
                ride.setPickupLat(real(from, 1));
                ride.setPickupLng(real(from, 2));
                ride.setPickupAddress(String.valueOf(PLACES[from][0]));
                ride.setDropoffLat(real(to, 1));
                ride.setDropoffLng(real(to, 2));
                ride.setDropoffAddress(String.valueOf(PLACES[to][0]));
                ride.setFareCents(price.fare);
                ride.setCurrency(fares.currency());
                ride.setDistanceMeters(meters);
                ride.setDurationSeconds(seconds);
                ride.setRequestedAt(Instant.ofEpochMilli(at));
                ride.setUpdatedAt(Instant.ofEpochMilli(ended));
                ride.setSearchUntil(Instant.ofEpochMilli(at + 90000L));
                ride.setStars(given);
                ride.setPaymentStatus(completed ? Payments.PAID : Payments.PENDING);
                ride.setProduct(products[driverIndex]);
                ride.setMethodId(method[0]);
                ride.setMethodLabel(method[1]);
                ride.setBaseCents(price.base);
                ride.setDistanceCents(price.distance);
                ride.setTimeCents(price.time);
                ride.setFeeCents(price.fee);
                ride.setTipCents(tip);
                ride.setDriverCents(price.fare - Fares.commission(pricing, price.fare));
                rides.insert(ride);
                event(id, RideState.REQUESTED, rider, at);
                if (taken) {
                    event(id, RideState.OFFERED, "", at + 1000L);
                    event(id, RideState.ACCEPTED, driver, at + wait);
                    if (count % 7 == 3 && drivers.length > 1) {
                        // Now and then the first driver asked let it go by.
                        RidePassed passed = new RidePassed();
                        passed.setRideId(id);
                        passed.setDriver(drivers[(driverIndex + 1) % drivers.length]);
                        passes.insert(passed);
                    }
                }
                if (completed) {
                    event(id, RideState.ARRIVED, driver, at + wait + pickup);
                    event(id, RideState.IN_PROGRESS, driver, at + wait + pickup + 60000L);
                    event(id, state, driver, ended);
                    Payment payment = new Payment();
                    payment.setRideId(id);
                    payment.setAmountCents(price.fare + price.fee);
                    payment.setCurrency(fares.currency());
                    payment.setStatus(Payments.PAID);
                    payment.setProviderRef(cash ? Payments.CASH : "sim_ch_ride-" + id);
                    if (tip > 0L) {
                        // A tip counts as paid -- in the driver's earnings,
                        // and for a refund -- once its charge has a reference.
                        payment.setTipRef("sim_ch_tip-" + id);
                    }
                    payment.setCreatedAt(Instant.ofEpochMilli(ended));
                    paid.insert(payment);
                    lastSeen[driverIndex] = Math.max(lastSeen[driverIndex], ended);
                    if (ended < now - 7L * Days.MILLIS) {
                        earnedEarlier[driverIndex] += price.fare
                                - Fares.commission(pricing, price.fare) + tip;
                    }
                } else {
                    event(id, state, RideState.CANCELLED_BY_DRIVER == state ? driver
                            : RideState.NO_DRIVERS == state ? "" : rider, ended);
                }
            }
        }
        for (int iter = 0; iter < drivers.length; iter++) {
            // Off line, parked where the last ride might have ended, with the
            // rating those rides gave.
            DriverState car = cars.findById(drivers[iter]);
            boolean first = car == null;
            if (first) {
                car = new DriverState();
                car.setUsername(drivers[iter]);
            }
            car.setLat(real(iter % PLACES.length, 1));
            car.setLng(real(iter % PLACES.length, 2));
            car.setLastSeen(Instant.ofEpochMilli(lastSeen[iter]));
            car.setRatingSum(stars[iter]);
            car.setRatingCount(rated[iter]);
            if (first) {
                cars.insert(car);
            }
            if (earnedEarlier[iter] > 0L) {
                // What the first week earned was cashed out at the end of it.
                PayoutAccount account = new PayoutAccount();
                account.setUsername(drivers[iter]);
                account.setHolder(drivers[iter]);
                account.setBankName("Bay Credit Union");
                account.setLast4("67" + (10 + iter));
                payoutAccounts.insert(account);
                Payout payout = new Payout();
                payout.setId(Ids.next());
                payout.setDriver(drivers[iter]);
                payout.setAmountCents(earnedEarlier[iter]);
                payout.setStatus("paid");
                payout.setCreatedAt(Instant.ofEpochMilli(now - 7L * Days.MILLIS));
                payout.setDestination("Bay Credit Union ****67" + (10 + iter));
                payouts.insert(payout);
            }
        }
    }

    private void event(String ride, RideState state, String actor, long at) throws IOException {
        RideEvent event = new RideEvent();
        event.setId(Ids.next());
        event.setRideId(ride);
        event.setState(state);
        event.setActor(actor);
        event.setCreatedAt(Instant.ofEpochMilli(at));
        events.insert(event);
    }

    private static double real(int place, int column) {
        return ((Double) PLACES[place][column]).doubleValue();
    }

    /// The next number of the sequence, from 0 to `below` less one: a linear
    /// congruential generator, which is all a made-up fortnight needs and is
    /// the same on every runtime.
    private int next(int below) {
        sequence = (sequence * 1103515245L + 12345L) & 0x7fffffffL;
        return (int) ((sequence >> 8) % below);
    }
}
