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

import com.codename1.backend.Config;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codenameone.examples.savor.account.DemoAccounts;
import com.codenameone.examples.savor.api.CourierStatusDto;
import com.codenameone.examples.savor.api.DeliveryDto;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.courier.Couriers;
import com.codenameone.examples.savor.geo.Geo;
import java.io.IOException;
import java.util.List;

/// One courier the server rides itself on a development server, so that a
/// customer alone can watch an order come all the way: it takes offers, rides
/// to the restaurant, waits for the food, and rides to the door, reporting
/// where it is as a phone would.
///
/// It steps aside -- goes off line -- whenever a real courier is on line and
/// free, so a demo with a courier's phone in hand gets the offers. Off on
/// every other profile, the `test` one included.
@Component
public class DemoFleet {
    /// Where it waits between orders: the waterfront, among the restaurants.
    static final double HOME_LAT = 37.7990;
    static final double HOME_LNG = -122.4020;
    /// Metres per tick, about 45 km/h at one tick every two seconds.
    static final double STEP_METERS = 25d;
    static final double THERE_METERS = 30d;

    private final Couriers couriers;
    private final Deliveries deliveries;
    private final Matcher matcher;
    private final boolean on;
    private final String courier = DemoAccounts.FOURTH_COURIER;
    private double lat = HOME_LAT;
    private double lng = HOME_LNG;

    public DemoFleet(Couriers couriers, Deliveries deliveries, Matcher matcher, Config config) {
        this.couriers = couriers;
        this.deliveries = deliveries;
        this.matcher = matcher;
        on = config.isDevelopmentProfile() && !"test".equals(config.getProfile());
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 10000, lock = "savor-demo-fleet")
    public void ride() throws IOException {
        if (!on) {
            return;
        }
        DeliveryDto job = deliveries.activeForCourier(courier);
        DeliveryState state = job == null ? DeliveryState.NONE : job.state();
        if (DeliveryState.NONE == state && someoneElse()) {
            couriers.report(courier, new CourierStatusDto(false, lat, lng, 0d));
            return;
        }
        double heading = 0d;
        try {
            if (DeliveryState.OFFERED == state) {
                deliveries.accept(courier, job.id());
            } else if (DeliveryState.ACCEPTED == state) {
                heading = toward(job.pickupLat(), job.pickupLng());
                if (near(job.pickupLat(), job.pickupLng())) {
                    deliveries.arrived(courier, job.id());
                }
            } else if (DeliveryState.ARRIVED == state) {
                // Refused until the kitchen says the food is ready.
                deliveries.start(courier, job.id());
            } else if (DeliveryState.IN_PROGRESS == state) {
                heading = toward(job.dropoffLat(), job.dropoffLng());
                if (near(job.dropoffLat(), job.dropoffLng())) {
                    deliveries.complete(courier, job.id());
                }
            }
        } catch (ResponseStatusException notYet) {
            // The kitchen is not done, or the order was cancelled; next tick.
        }
        couriers.report(courier, new CourierStatusDto(true, lat, lng, heading));
    }

    /// Whether a real courier is on line and free near the restaurants.
    private boolean someoneElse() throws IOException {
        List<Matcher.Candidate> free = matcher.nearby(HOME_LAT, HOME_LNG, 50000d);
        for (int iter = 0; iter < free.size(); iter++) {
            if (!courier.equals(free.get(iter).username)) {
                return true;
            }
        }
        return false;
    }

    private boolean near(double toLat, double toLng) {
        return Geo.meters(lat, lng, toLat, toLng) <= THERE_METERS;
    }

    /// One step toward a point, and the heading of it in degrees.
    private double toward(double toLat, double toLng) {
        double meters = Geo.meters(lat, lng, toLat, toLng);
        double dLat = toLat - lat;
        double dLng = toLng - lng;
        double heading = (Math.toDegrees(Math.atan2(dLng * Math.cos(Math.toRadians(lat)), dLat))
                + 360d) % 360d;
        if (meters <= STEP_METERS) {
            lat = toLat;
            lng = toLng;
        } else {
            double share = STEP_METERS / meters;
            lat += dLat * share;
            lng += dLng * share;
        }
        return heading;
    }
}
