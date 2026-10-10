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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.wayline.Text;
import com.codenameone.examples.wayline.api.PricingDto;
import com.codenameone.examples.wayline.api.RideRequestDto;
import com.codenameone.examples.wayline.domain.Pricing;
import com.codenameone.examples.wayline.geo.Geo;

import java.io.IOException;

/// What a ride costs.
///
/// A base fare, plus so much a kilometre and so much a minute, with a floor;
/// the larger products multiply all of it, and so does a surge. On top of the
/// fare the rider pays a service fee, and out of it the service keeps a
/// commission; the rest is the driver's.
///
/// Money is whole cents in a `long` from end to end: a fare is never a floating
/// point number, here or in the app.
///
/// The prices start as settings (`wayline.fare.*`) and are the admin's to
/// change, which stores them in the database. They are read from there for
/// every quote and not kept in memory, so a change made through one instance
/// of the server is the price on all of them.
@Component
public class Fares {
    public static final String STANDARD = "standard";
    public static final String COMFORT = "comfort";
    public static final String XL = "xl";
    /// Cheapest first, which is the order a quote lists them in.
    public static final String[] PRODUCTS = {STANDARD, COMFORT, XL};

    /// How much longer than the straight line a road route is taken to be when
    /// the app did not send one.
    static final double ROAD_FACTOR = 1.3d;
    /// City driving, for a duration when the app did not send one: 30 km/h.
    static final double METERS_PER_SECOND = 8.33d;

    private final PricingRepository prices;
    private final PricingDto defaults;

    public Fares(PricingRepository prices,
            @Value("${wayline.currency:USD}") String currency,
            @Value("${wayline.fare.baseCents:250}") int baseCents,
            @Value("${wayline.fare.perKmCents:120}") int perKmCents,
            @Value("${wayline.fare.perMinuteCents:30}") int perMinuteCents,
            @Value("${wayline.fare.minimumCents:500}") int minimumCents,
            @Value("${wayline.fare.serviceFeePercent:10}") int serviceFeePercent,
            @Value("${wayline.fare.commissionPercent:20}") int commissionPercent) {
        this.prices = prices;
        defaults = new PricingDto();
        defaults.currency = currency;
        defaults.baseFare = baseCents;
        defaults.perKm = perKmCents;
        defaults.perMinute = perMinuteCents;
        defaults.minimumFare = minimumCents;
        defaults.serviceFeePercent = serviceFeePercent;
        defaults.commissionPercent = commissionPercent;
        defaults.surgeMultiplier = 1d;
        defaults.comfortMultiplier = 1.35d;
        defaults.xlMultiplier = 1.7d;
    }

    public String currency() {
        return defaults.currency;
    }

    /// The prices in force: the admin's when they have set any, the settings'
    /// otherwise.
    @Transactional(readOnly = true)
    public PricingDto pricing() throws IOException {
        return toDto(prices.find());
    }

    private PricingDto toDto(Pricing row) {
        PricingDto dto = new PricingDto();
        // The currency is a setting and stays one. Rides already taken are in
        // it, and changing it would not convert them.
        dto.currency = defaults.currency;
        if (row == null) {
            dto.baseFare = defaults.baseFare;
            dto.perKm = defaults.perKm;
            dto.perMinute = defaults.perMinute;
            dto.minimumFare = defaults.minimumFare;
            dto.serviceFeePercent = defaults.serviceFeePercent;
            dto.commissionPercent = defaults.commissionPercent;
            dto.surgeMultiplier = defaults.surgeMultiplier;
            dto.comfortMultiplier = defaults.comfortMultiplier;
            dto.xlMultiplier = defaults.xlMultiplier;
            return dto;
        }
        dto.baseFare = row.baseCents;
        dto.perKm = row.perKmCents;
        dto.perMinute = row.perMinuteCents;
        dto.minimumFare = row.minimumCents;
        dto.serviceFeePercent = row.feePercent;
        dto.commissionPercent = row.commissionPercent;
        dto.surgeMultiplier = row.surge;
        dto.comfortMultiplier = row.comfort;
        dto.xlMultiplier = row.xl;
        return dto;
    }

    /// Changes the prices, for the rides asked for from now on. A ride already
    /// asked for keeps the price it was quoted. Answers 400 for prices that
    /// could only be a mistake.
    @Transactional
    public PricingDto save(String by, PricingDto wanted) throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "No prices to save");
        }
        amount(wanted.baseFare, "the base fare");
        amount(wanted.perKm, "the price per kilometre");
        amount(wanted.perMinute, "the price per minute");
        amount(wanted.minimumFare, "the minimum fare");
        between(wanted.serviceFeePercent, 0d, 50d, "the service fee");
        between(wanted.commissionPercent, 0d, 90d, "the commission");
        between(wanted.surgeMultiplier, 1d, 5d, "the surge multiplier");
        between(wanted.comfortMultiplier, 1d, 5d, "the comfort multiplier");
        between(wanted.xlMultiplier, 1d, 5d, "the XL multiplier");
        Pricing row = prices.find();
        boolean first = row == null;
        if (first) {
            row = new Pricing();
        }
        row.baseCents = wanted.baseFare;
        row.perKmCents = wanted.perKm;
        row.perMinuteCents = wanted.perMinute;
        row.minimumCents = wanted.minimumFare;
        row.feePercent = wanted.serviceFeePercent;
        row.commissionPercent = wanted.commissionPercent;
        row.surge = wanted.surgeMultiplier;
        row.comfort = wanted.comfortMultiplier;
        row.xl = wanted.xlMultiplier;
        row.updatedAt = System.currentTimeMillis();
        row.updatedBy = by;
        if (first) {
            prices.add(row);
        }
        return toDto(row);
    }

    /// The length of the ride to price, in metres.
    ///
    /// The app sends the length of the route it drew, and that is the better
    /// number -- but it is also a number the caller chose, and the fare follows
    /// from it. So it is believed only between the straight line and four times
    /// it; outside that the server prices from its own estimate.
    public double distanceMeters(RideRequestDto request) {
        double straight = Geo.meters(request.pickupLat, request.pickupLng, request.dropoffLat,
                request.dropoffLng);
        if (request.distanceMeters >= straight && request.distanceMeters <= straight * 4) {
            return request.distanceMeters;
        }
        return straight * ROAD_FACTOR;
    }

    /// The duration to price, in seconds; believed on the same terms.
    public double durationSeconds(RideRequestDto request, double distanceMeters) {
        double estimate = distanceMeters / METERS_PER_SECOND;
        if (distanceMeters == request.distanceMeters && request.durationSeconds >= estimate / 4
                && request.durationSeconds <= estimate * 4) {
            return request.durationSeconds;
        }
        return estimate;
    }

    /// A ride of this length and duration, taken as `product`, at `pricing`.
    public static Price price(PricingDto pricing, double distanceMeters, double durationSeconds,
            String product) {
        double times = pricing.surgeMultiplier;
        if (COMFORT.equals(product)) {
            times *= pricing.comfortMultiplier;
        } else if (XL.equals(product)) {
            times *= pricing.xlMultiplier;
        }
        Price price = new Price();
        price.base = Math.round(pricing.baseFare * times);
        price.distance = Math.round(distanceMeters / 1000d * pricing.perKm * times);
        price.time = Math.round(durationSeconds / 60d * pricing.perMinute * times);
        price.fare = price.base + price.distance + price.time;
        long minimum = Math.round(pricing.minimumFare * times);
        if (price.fare < minimum) {
            // The floor is a larger base fare, so the three parts still add up
            // to what is charged.
            price.base += minimum - price.fare;
            price.fare = minimum;
        }
        price.fee = Math.round(price.fare * pricing.serviceFeePercent / 100d);
        return price;
    }

    /// What the service keeps of a fare.
    public static long commission(PricingDto pricing, long fareCents) {
        return Math.round(fareCents * pricing.commissionPercent / 100d);
    }

    /// The product a request names; standard when it names none. Answers 400
    /// for one that does not exist.
    public static String product(String value) {
        return Text.oneOf(value, PRODUCTS, STANDARD, "the kind of ride");
    }

    /// The parts of a price, in cents. `fare` is the first three added up, and
    /// `fee` is paid on top of it.
    public static final class Price {
        public long base;
        public long distance;
        public long time;
        public long fare;
        public long fee;
    }

    private static void amount(long cents, String what) {
        if (cents < 0L || cents > 1000000L) {
            throw new ResponseStatusException(400, "That is not an amount for " + what);
        }
    }

    private static void between(double value, double low, double high, String what) {
        // Written so that NaN, which compares false to everything, is refused.
        if (!(value >= low && value <= high)) {
            throw new ResponseStatusException(400, "That is not a value for " + what);
        }
    }
}
