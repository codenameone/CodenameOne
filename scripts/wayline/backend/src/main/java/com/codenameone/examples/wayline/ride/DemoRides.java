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
import com.codenameone.examples.wayline.Days;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.api.PricingDto;
import com.codenameone.examples.wayline.api.RideStates;
import com.codenameone.examples.wayline.domain.DriverApplication;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.domain.Payment;
import com.codenameone.examples.wayline.domain.Payout;
import com.codenameone.examples.wayline.domain.PayoutAccount;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.domain.RideEvent;
import com.codenameone.examples.wayline.domain.RidePassed;
import com.codenameone.examples.wayline.driver.DriverRepository;
import com.codenameone.examples.wayline.geo.Geo;
import com.codenameone.examples.wayline.pay.Payments;

import java.io.IOException;

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
    private final DriverRepository cars;
    private final Fares fares;
    private final Payments payments;
    private long sequence = 20240229L;

    public DemoRides(RideRepository rides, DriverRepository cars, Fares fares,
            Payments payments) {
        this.rides = rides;
        this.cars = cars;
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
            DriverApplication made = cars.application(drivers[iter]);
            products[iter] = made == null ? Fares.STANDARD : made.product;
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
                String state = roll < 15 ? RideStates.COMPLETED
                        : roll == 17 ? RideStates.CANCELLED_BY_DRIVER
                        : roll == 18 ? RideStates.NO_DRIVERS : RideStates.CANCELLED_BY_RIDER;
                boolean completed = RideStates.COMPLETED.equals(state);
                // A rider who cancels does it before a driver is found half the
                // time, and after one has taken the ride the other half.
                boolean taken = completed || RideStates.CANCELLED_BY_DRIVER.equals(state)
                        || (RideStates.CANCELLED_BY_RIDER.equals(state) && next(2) == 0);
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
                        : RideStates.NO_DRIVERS.equals(state) ? at + 90000L : at + 30000L;
                String id = Ids.next();
                Ride ride = new Ride();
                ride.id = id;
                ride.state = state;
                ride.rider = rider;
                ride.driver = taken ? driver : "";
                ride.pickupLat = real(from, 1);
                ride.pickupLng = real(from, 2);
                ride.pickupAddress = String.valueOf(PLACES[from][0]);
                ride.dropoffLat = real(to, 1);
                ride.dropoffLng = real(to, 2);
                ride.dropoffAddress = String.valueOf(PLACES[to][0]);
                ride.fareCents = price.fare;
                ride.currency = fares.currency();
                ride.distanceMeters = meters;
                ride.durationSeconds = seconds;
                ride.requestedAt = at;
                ride.updatedAt = ended;
                ride.searchUntil = at + 90000L;
                ride.stars = given;
                ride.paymentStatus = completed ? Payments.PAID : Payments.PENDING;
                ride.product = products[driverIndex];
                ride.methodId = method[0];
                ride.methodLabel = method[1];
                ride.baseCents = price.base;
                ride.distanceCents = price.distance;
                ride.timeCents = price.time;
                ride.feeCents = price.fee;
                ride.tipCents = tip;
                ride.driverCents = price.fare - Fares.commission(pricing, price.fare);
                rides.add(ride);
                event(id, RideStates.REQUESTED, rider, at);
                if (taken) {
                    event(id, RideStates.OFFERED, "", at + 1000L);
                    event(id, RideStates.ACCEPTED, driver, at + wait);
                    if (count % 7 == 3 && drivers.length > 1) {
                        // Now and then the first driver asked let it go by.
                        RidePassed passed = new RidePassed();
                        passed.rideId = id;
                        passed.driver = drivers[(driverIndex + 1) % drivers.length];
                        rides.add(passed);
                    }
                }
                if (completed) {
                    event(id, RideStates.ARRIVED, driver, at + wait + pickup);
                    event(id, RideStates.IN_PROGRESS, driver, at + wait + pickup + 60000L);
                    event(id, state, driver, ended);
                    Payment payment = new Payment();
                    payment.rideId = id;
                    payment.amountCents = price.fare + price.fee;
                    payment.currency = fares.currency();
                    payment.status = Payments.PAID;
                    payment.providerRef = cash ? Payments.CASH : "sim_ch_ride-" + id;
                    payment.createdAt = ended;
                    rides.add(payment);
                    lastSeen[driverIndex] = Math.max(lastSeen[driverIndex], ended);
                    if (ended < now - 7L * Days.MILLIS) {
                        earnedEarlier[driverIndex] += price.fare
                                - Fares.commission(pricing, price.fare) + tip;
                    }
                } else {
                    event(id, state, RideStates.CANCELLED_BY_DRIVER.equals(state) ? driver
                            : RideStates.NO_DRIVERS.equals(state) ? "" : rider, ended);
                }
            }
        }
        for (int iter = 0; iter < drivers.length; iter++) {
            // Off line, parked where the last ride might have ended, with the
            // rating those rides gave.
            DriverState car = cars.find(drivers[iter]);
            boolean first = car == null;
            if (first) {
                car = new DriverState();
                car.username = drivers[iter];
            }
            car.lat = real(iter % PLACES.length, 1);
            car.lng = real(iter % PLACES.length, 2);
            car.lastSeen = lastSeen[iter];
            car.ratingSum = stars[iter];
            car.ratingCount = rated[iter];
            if (first) {
                cars.add(car);
            }
            if (earnedEarlier[iter] > 0L) {
                // What the first week earned was cashed out at the end of it.
                PayoutAccount account = new PayoutAccount();
                account.username = drivers[iter];
                account.holder = drivers[iter];
                account.bankName = "Bay Credit Union";
                account.last4 = "67" + (10 + iter);
                cars.add(account);
                Payout payout = new Payout();
                payout.id = Ids.next();
                payout.driver = drivers[iter];
                payout.amountCents = earnedEarlier[iter];
                payout.status = "paid";
                payout.createdAt = now - 7L * Days.MILLIS;
                payout.destination = "Bay Credit Union ****67" + (10 + iter);
                cars.add(payout);
            }
        }
    }

    private void event(String ride, String state, String actor, long at) throws IOException {
        RideEvent event = new RideEvent();
        event.id = Ids.next();
        event.rideId = ride;
        event.state = state;
        event.actor = actor;
        event.createdAt = at;
        rides.add(event);
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
