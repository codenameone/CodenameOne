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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.wayline.Text;
import com.codenameone.examples.wayline.api.PricingDto;
import com.codenameone.examples.wayline.api.RideRequestDto;
import com.codenameone.examples.wayline.domain.Pricing;
import com.codenameone.examples.wayline.geo.Geo;
import com.codenameone.examples.wayline.repository.PricingRepository;

import java.io.IOException;
import java.time.Instant;

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
        defaults = new PricingDto(currency, baseCents, perKmCents, perMinuteCents, minimumCents,
                serviceFeePercent, commissionPercent, 1d, 1.35d, 1.7d);
    }

    public String currency() {
        return defaults.currency();
    }

    /// The prices in force: the admin's when they have set any, the settings'
    /// otherwise.
    @Transactional(readOnly = true)
    public PricingDto pricing() throws IOException {
        return toDto(prices.findById(PricingRepository.ONLY));
    }

    private PricingDto toDto(Pricing row) {
        // The row a migration created and nobody has saved over yet.
        if (row == null || Instant.EPOCH.equals(row.getUpdatedAt())) {
            return defaults;
        }
        // The currency is a setting and stays one. Rides already taken are in
        // it, and changing it would not convert them.
        return new PricingDto(defaults.currency(), row.getBaseCents(), row.getPerKmCents(),
                row.getPerMinuteCents(), row.getMinimumCents(), row.getFeePercent(),
                row.getCommissionPercent(), row.getSurge(), row.getComfort(), row.getXl());
    }

    /// Changes the prices, for the rides asked for from now on. A ride already
    /// asked for keeps the price it was quoted. Answers 400 for prices that
    /// could only be a mistake.
    @Transactional
    public PricingDto save(String by, PricingDto wanted) throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "No prices to save");
        }
        amount(wanted.baseFare(), "the base fare");
        amount(wanted.perKm(), "the price per kilometre");
        amount(wanted.perMinute(), "the price per minute");
        amount(wanted.minimumFare(), "the minimum fare");
        between(wanted.serviceFeePercent(), 0d, 50d, "the service fee");
        between(wanted.commissionPercent(), 0d, 90d, "the commission");
        between(wanted.surgeMultiplier(), 1d, 5d, "the surge multiplier");
        between(wanted.comfortMultiplier(), 1d, 5d, "the comfort multiplier");
        between(wanted.xlMultiplier(), 1d, 5d, "the XL multiplier");
        // The row is always there (V6 creates it), so this only ever updates
        // it. Inserting it on the first save was a race: two admins saving
        // before anyone had both inserted id 1, and one of them failed.
        if (prices.change(PricingRepository.ONLY.intValue(), wanted.baseFare(), wanted.perKm(),
                wanted.perMinute(), wanted.minimumFare(), wanted.serviceFeePercent(),
                wanted.commissionPercent(), wanted.surgeMultiplier(),
                wanted.comfortMultiplier(), wanted.xlMultiplier(), Instant.now(),
                by) == 0) {
            throw new IllegalStateException("The pricing row is missing; the V6 migration "
                    + "creates it");
        }
        return new PricingDto(defaults.currency(), wanted.baseFare(), wanted.perKm(),
                wanted.perMinute(), wanted.minimumFare(), wanted.serviceFeePercent(),
                wanted.commissionPercent(), wanted.surgeMultiplier(), wanted.comfortMultiplier(),
                wanted.xlMultiplier());
    }

    /// The length of the ride to price, in metres.
    ///
    /// The app sends the length of the route it drew, and that is the better
    /// number -- but it is also a number the caller chose, and the fare follows
    /// from it. So it is believed only between the straight line and four times
    /// it; outside that the server prices from its own estimate.
    public double distanceMeters(RideRequestDto request) {
        double straight = Geo.meters(request.pickupLat(), request.pickupLng(), request.dropoffLat(),
                request.dropoffLng());
        if (request.distanceMeters() >= straight && request.distanceMeters() <= straight * 4) {
            return request.distanceMeters();
        }
        return straight * ROAD_FACTOR;
    }

    /// The duration to price, in seconds; believed on the same terms.
    public double durationSeconds(RideRequestDto request, double distanceMeters) {
        double estimate = distanceMeters / METERS_PER_SECOND;
        if (distanceMeters == request.distanceMeters() && request.durationSeconds() >= estimate / 4
                && request.durationSeconds() <= estimate * 4) {
            return request.durationSeconds();
        }
        return estimate;
    }

    /// A ride of this length and duration, taken as `product`, at `pricing`.
    public static Price price(PricingDto pricing, double distanceMeters, double durationSeconds,
            String product) {
        double times = pricing.surgeMultiplier();
        if (COMFORT.equals(product)) {
            times *= pricing.comfortMultiplier();
        } else if (XL.equals(product)) {
            times *= pricing.xlMultiplier();
        }
        Price price = new Price();
        price.base = Math.round(pricing.baseFare() * times);
        price.distance = Math.round(distanceMeters / 1000d * pricing.perKm() * times);
        price.time = Math.round(durationSeconds / 60d * pricing.perMinute() * times);
        price.fare = price.base + price.distance + price.time;
        long minimum = Math.round(pricing.minimumFare() * times);
        if (price.fare < minimum) {
            // The floor is a larger base fare, so the three parts still add up
            // to what is charged.
            price.base += minimum - price.fare;
            price.fare = minimum;
        }
        price.fee = Math.round(price.fare * pricing.serviceFeePercent() / 100d);
        return price;
    }

    /// What the service keeps of a fare.
    public static long commission(PricingDto pricing, long fareCents) {
        return Math.round(fareCents * pricing.commissionPercent() / 100d);
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
