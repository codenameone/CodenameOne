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
package com.codenameone.examples.bazaar.pricing;

import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.ShippingRate;
import com.codenameone.examples.bazaar.domain.ShippingZone;
import com.codenameone.examples.bazaar.geo.Geo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The delivery options a cart has for an address.
///
/// Pure. The zone is the first, by position, whose comma separated
/// `countries` names the address's country; failing that the first that
/// lists `*`, the rest of the world. Its rates, by kind:
///
/// - **FLAT**: `priceCents`, always offered.
/// - **WEIGHT**: offered when the cart weighs at least `minValue` grams and
///   less than `maxValue` (0 = no upper bound).
/// - **PRICE**: the same tiers over the subtotal after discounts, in cents.
/// - **FREE_OVER**: free, offered once that subtotal reaches `minValue`.
/// - **LOCAL**: delivery from `locationId`, offered when the address is
///   within `radiusMeters` of it ([Geo#meters]).
///
/// **PICKUP** rates are offered whatever the address, from any zone of the
/// store, when their location is active: the customer comes to the shop.
/// A free-shipping discount makes every option but pickup cost nothing.
public final class ShippingCalculator {
    public static final String FLAT = "FLAT";
    public static final String WEIGHT = "WEIGHT";
    public static final String PRICE = "PRICE";
    public static final String FREE_OVER = "FREE_OVER";
    public static final String PICKUP = "PICKUP";
    public static final String LOCAL = "LOCAL";
    public static final String REST_OF_WORLD = "*";

    private ShippingCalculator() {
    }

    /// One way the order can reach the customer.
    public static final class Option {
        public final ShippingRate rate;
        public final long priceCents;

        Option(ShippingRate rate, long priceCents) {
            this.rate = rate;
            this.priceCents = priceCents;
        }
    }

    /// The zone serving `country`, or null.
    public static ShippingZone zone(List<ShippingZone> zones, String country) {
        if (zones == null || country == null || country.length() == 0) {
            return null;
        }
        ShippingZone world = null;
        for (int iter = 0; iter < zones.size(); iter++) {
            ShippingZone zone = zones.get(iter);
            if (Money.listHas(zone.getCountries(), country)) {
                return zone;
            }
            if (world == null && Money.listHas(zone.getCountries(), REST_OF_WORLD)) {
                world = zone;
            }
        }
        return world;
    }

    private static boolean inTier(long value, ShippingRate rate) {
        return value >= rate.getMinValue() && (rate.getMaxValue() <= 0 || value < rate.getMaxValue());
    }

    /// The options, in zone order (pickup last), cheapest price per rate.
    ///
    /// @param zones the store's zones, by position
    /// @param rates the store's rates, by position
    /// @param locations the store's locations by id, for pickup and local
    /// @param subtotalCents the subtotal after discounts
    /// @param hasPoint whether `lat`/`lng` are a real point
    public static List<Option> options(List<ShippingZone> zones, List<ShippingRate> rates,
            Map<String, Location> locations, String country, boolean hasPoint, double lat,
            double lng, long subtotalCents, int weightGrams, boolean freeShipping) {
        List<Option> out = new ArrayList<Option>();
        ShippingZone zone = zone(zones, country);
        List<Option> pickups = new ArrayList<Option>();
        for (int iter = 0; iter < rates.size(); iter++) {
            ShippingRate rate = rates.get(iter);
            String kind = rate.getKind();
            if (PICKUP.equals(kind)) {
                Location at = locations == null ? null : locations.get(rate.getLocationId());
                if (at != null && at.isActive()) {
                    pickups.add(new Option(rate, Math.max(0, rate.getPriceCents())));
                }
                continue;
            }
            if (zone == null || !zone.getId().equals(rate.getZoneId())) {
                continue;
            }
            long price = Math.max(0, rate.getPriceCents());
            boolean offered;
            if (FLAT.equals(kind)) {
                offered = true;
            } else if (WEIGHT.equals(kind)) {
                offered = inTier(weightGrams, rate);
            } else if (PRICE.equals(kind)) {
                offered = inTier(subtotalCents, rate);
            } else if (FREE_OVER.equals(kind)) {
                offered = subtotalCents >= rate.getMinValue();
                price = 0;
            } else if (LOCAL.equals(kind)) {
                Location from = locations == null ? null : locations.get(rate.getLocationId());
                offered = hasPoint && from != null && from.isActive()
                        && Geo.meters(lat, lng, from.getLat(), from.getLng()) <= rate.getRadiusMeters();
            } else {
                offered = false;
            }
            if (offered) {
                out.add(new Option(rate, freeShipping ? 0 : price));
            }
        }
        out.addAll(pickups);
        return out;
    }

    /// The option for `rateId`, or null when the address does not get it.
    public static Option find(List<Option> options, String rateId) {
        if (rateId == null) {
            return null;
        }
        for (int iter = 0; iter < options.size(); iter++) {
            if (rateId.equals(options.get(iter).rate.getId())) {
                return options.get(iter);
            }
        }
        return null;
    }
}
