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
package com.codenameone.examples.morsel.delivery;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.morsel.Days;
import com.codenameone.examples.morsel.Ids;
import com.codenameone.examples.morsel.api.PricingDto;
import com.codenameone.examples.morsel.api.DeliveryState;
import com.codenameone.examples.morsel.domain.CourierApplication;
import com.codenameone.examples.morsel.domain.CourierState;
import com.codenameone.examples.morsel.domain.Payment;
import com.codenameone.examples.morsel.domain.Payout;
import com.codenameone.examples.morsel.domain.PayoutAccount;
import com.codenameone.examples.morsel.domain.Delivery;
import com.codenameone.examples.morsel.domain.DeliveryEvent;
import com.codenameone.examples.morsel.domain.DeliveryPassed;
import com.codenameone.examples.morsel.geo.Geo;
import com.codenameone.examples.morsel.pay.Payments;
import com.codenameone.examples.morsel.repository.CourierApplicationRepository;
import com.codenameone.examples.morsel.repository.CourierStateRepository;
import com.codenameone.examples.morsel.repository.PaymentRepository;
import com.codenameone.examples.morsel.repository.PayoutAccountRepository;
import com.codenameone.examples.morsel.repository.PayoutRepository;
import com.codenameone.examples.morsel.repository.DeliveryEventRepository;
import com.codenameone.examples.morsel.repository.DeliveryPassedRepository;
import com.codenameone.examples.morsel.repository.DeliveryRepository;

import java.io.IOException;

/// Two weeks of deliveries that were never taken, so that the admin's charts, a
/// courier's earnings and a customer's receipts have something in them the first
/// time a development server is opened.
///
/// Stored whole, as finished deliveries with their events and payments, in one
/// transaction: there is no way to take a delivery last Tuesday through the API. The
/// numbers come from a fixed sequence and not from a random source, so every
/// server starts with the same fortnight, moved to end today.
@Component
public class DemoDeliveries {
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
    /// The hours deliveries are asked for at, the busy ones more than once.
    private static final int[] HOURS = {7, 8, 8, 9, 12, 13, 15, 17, 18, 18, 19, 20, 22, 23};

    private final DeliveryRepository deliveries;
    private final DeliveryEventRepository events;
    private final DeliveryPassedRepository passes;
    private final PaymentRepository paid;
    private final CourierStateRepository cars;
    private final CourierApplicationRepository applications;
    private final PayoutAccountRepository payoutAccounts;
    private final PayoutRepository payouts;
    private final Fares fares;
    private final Payments payments;
    private long sequence = 20240229L;

    public DemoDeliveries(DeliveryRepository deliveries, DeliveryEventRepository events,
            DeliveryPassedRepository passes, PaymentRepository paid, CourierStateRepository cars,
            CourierApplicationRepository applications, PayoutAccountRepository payoutAccounts,
            PayoutRepository payouts, Fares fares, Payments payments) {
        this.deliveries = deliveries;
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

    /// Writes the fortnight, shared out among these customers and couriers. The
    /// couriers are approved ones, and each delivery is of its courier's kind of car.
    ///
    /// @param untilToday false ends the fortnight yesterday. A test that
    ///     compares a chart with a picture of it asks for that: deliveries of today
    ///     are moved back to before now, so today's bars would depend on the
    ///     hour the server was started at.
    @Transactional
    public void seed(String[] customers, String[] couriers, boolean untilToday) throws IOException {
        long now = System.currentTimeMillis();
        long today = Days.start(now) - (untilToday ? 0L : Days.MILLIS);
        PricingDto pricing = fares.pricing();
        long[] stars = new long[couriers.length];
        long[] rated = new long[couriers.length];
        long[] earnedEarlier = new long[couriers.length];
        long[] lastSeen = new long[couriers.length];
        String[] products = new String[couriers.length];
        for (int iter = 0; iter < couriers.length; iter++) {
            CourierApplication made = applications.findById(couriers[iter]);
            products[iter] = made == null ? Fares.STANDARD : made.getProduct();
        }
        int count = 0;
        for (int day = DAYS - 1; day >= 0; day--) {
            int asked = 3 + next(4);
            for (int number = 0; number < asked; number++, count++) {
                int customerIndex = next(customers.length);
                int courierIndex = next(couriers.length);
                String customer = customers[customerIndex];
                String courier = couriers[courierIndex];
                int from = next(PLACES.length);
                int to = (from + 1 + next(PLACES.length - 1)) % PLACES.length;
                double meters = Geo.meters(real(from, 1), real(from, 2), real(to, 1), real(to, 2))
                        * Fares.ROAD_FACTOR;
                double seconds = meters / Fares.METERS_PER_SECOND;
                Fares.Price price = Fares.price(pricing, meters, seconds, products[courierIndex]);
                long wait = (20 + next(100)) * 1000L;
                long pickup = (120 + next(240)) * 1000L;
                long whole = wait + pickup + (long) (seconds * 1000d);
                long at = today - day * Days.MILLIS + HOURS[next(HOURS.length)] * 3600000L
                        + next(3600) * 1000L;
                // Nothing here happens in the future: a delivery that would end
                // after now is moved back until it has ended.
                at = Math.min(at, now - whole - 60000L);

                int roll = next(20);
                DeliveryState state = roll < 15 ? DeliveryState.COMPLETED
                        : roll == 17 ? DeliveryState.CANCELLED_BY_COURIER
                        : roll == 18 ? DeliveryState.NO_COURIERS : DeliveryState.CANCELLED_BY_CUSTOMER;
                boolean completed = DeliveryState.COMPLETED == state;
                // A customer who cancels does it before a courier is found half the
                // time, and after one has taken the delivery the other half.
                boolean taken = completed || DeliveryState.CANCELLED_BY_COURIER == state
                        || (DeliveryState.CANCELLED_BY_CUSTOMER == state && next(2) == 0);
                String[] method = next(3) == 0 ? new String[] {Payments.CASH, Payments.CASH_LABEL}
                        : payments.choose(customer, null);
                boolean cash = Payments.CASH.equals(method[0]);
                long tip = completed && !cash && next(3) == 0 ? price.fare * 15L / 100L : 0L;
                int given = 0;
                if (completed && next(5) != 0) {
                    int liked = next(10);
                    given = liked == 0 ? 3 : liked < 4 ? 4 : 5;
                    stars[courierIndex] += given;
                    rated[courierIndex]++;
                }
                long ended = completed ? at + whole : taken ? at + wait + pickup / 2L
                        : DeliveryState.NO_COURIERS == state ? at + 90000L : at + 30000L;
                String id = Ids.next();
                Delivery delivery = new Delivery();
                delivery.setId(id);
                delivery.setState(state);
                delivery.setCustomer(customer);
                delivery.setCourier(taken ? courier : "");
                delivery.setPickupLat(real(from, 1));
                delivery.setPickupLng(real(from, 2));
                delivery.setPickupAddress(String.valueOf(PLACES[from][0]));
                delivery.setDropoffLat(real(to, 1));
                delivery.setDropoffLng(real(to, 2));
                delivery.setDropoffAddress(String.valueOf(PLACES[to][0]));
                delivery.setFareCents(price.fare);
                delivery.setCurrency(fares.currency());
                delivery.setDistanceMeters(meters);
                delivery.setDurationSeconds(seconds);
                delivery.setRequestedAt(at);
                delivery.setUpdatedAt(ended);
                delivery.setSearchUntil(at + 90000L);
                delivery.setStars(given);
                delivery.setPaymentStatus(completed ? Payments.PAID : Payments.PENDING);
                delivery.setProduct(products[courierIndex]);
                delivery.setMethodId(method[0]);
                delivery.setMethodLabel(method[1]);
                delivery.setBaseCents(price.base);
                delivery.setDistanceCents(price.distance);
                delivery.setTimeCents(price.time);
                delivery.setFeeCents(price.fee);
                delivery.setTipCents(tip);
                delivery.setCourierCents(price.fare - Fares.commission(pricing, price.fare));
                deliveries.insert(delivery);
                event(id, DeliveryState.REQUESTED, customer, at);
                if (taken) {
                    event(id, DeliveryState.OFFERED, "", at + 1000L);
                    event(id, DeliveryState.ACCEPTED, courier, at + wait);
                    if (count % 7 == 3 && couriers.length > 1) {
                        // Now and then the first courier asked let it go by.
                        DeliveryPassed passed = new DeliveryPassed();
                        passed.setDeliveryId(id);
                        passed.setCourier(couriers[(courierIndex + 1) % couriers.length]);
                        passes.insert(passed);
                    }
                }
                if (completed) {
                    event(id, DeliveryState.ARRIVED, courier, at + wait + pickup);
                    event(id, DeliveryState.IN_PROGRESS, courier, at + wait + pickup + 60000L);
                    event(id, state, courier, ended);
                    Payment payment = new Payment();
                    payment.setDeliveryId(id);
                    payment.setAmountCents(price.fare + price.fee);
                    payment.setCurrency(fares.currency());
                    payment.setStatus(Payments.PAID);
                    payment.setProviderRef(cash ? Payments.CASH : "sim_ch_delivery-" + id);
                    if (tip > 0L) {
                        // A tip counts as paid -- in the courier's earnings,
                        // and for a refund -- once its charge has a reference.
                        payment.setTipRef("sim_ch_tip-" + id);
                    }
                    payment.setCreatedAt(ended);
                    paid.insert(payment);
                    lastSeen[courierIndex] = Math.max(lastSeen[courierIndex], ended);
                    if (ended < now - 7L * Days.MILLIS) {
                        earnedEarlier[courierIndex] += price.fare
                                - Fares.commission(pricing, price.fare) + tip;
                    }
                } else {
                    event(id, state, DeliveryState.CANCELLED_BY_COURIER == state ? courier
                            : DeliveryState.NO_COURIERS == state ? "" : customer, ended);
                }
            }
        }
        for (int iter = 0; iter < couriers.length; iter++) {
            // Off line, parked where the last delivery might have ended, with the
            // rating those deliveries gave.
            CourierState car = cars.findById(couriers[iter]);
            boolean first = car == null;
            if (first) {
                car = new CourierState();
                car.setUsername(couriers[iter]);
            }
            car.setLat(real(iter % PLACES.length, 1));
            car.setLng(real(iter % PLACES.length, 2));
            car.setLastSeen(lastSeen[iter]);
            car.setRatingSum(stars[iter]);
            car.setRatingCount(rated[iter]);
            if (first) {
                cars.insert(car);
            }
            if (earnedEarlier[iter] > 0L) {
                // What the first week earned was cashed out at the end of it.
                PayoutAccount account = new PayoutAccount();
                account.setUsername(couriers[iter]);
                account.setHolder(couriers[iter]);
                account.setBankName("Bay Credit Union");
                account.setLast4("67" + (10 + iter));
                payoutAccounts.insert(account);
                Payout payout = new Payout();
                payout.setId(Ids.next());
                payout.setCourier(couriers[iter]);
                payout.setAmountCents(earnedEarlier[iter]);
                payout.setStatus("paid");
                payout.setCreatedAt(now - 7L * Days.MILLIS);
                payout.setDestination("Bay Credit Union ****67" + (10 + iter));
                payouts.insert(payout);
            }
        }
    }

    private void event(String delivery, DeliveryState state, String actor, long at) throws IOException {
        DeliveryEvent event = new DeliveryEvent();
        event.setId(Ids.next());
        event.setDeliveryId(delivery);
        event.setState(state);
        event.setActor(actor);
        event.setCreatedAt(at);
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
