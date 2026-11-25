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

import com.codenameone.examples.bazaar.api.RateQuoteDto;
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
/// - **CARRIER**: priced live by the store's carrier
///   ([com.codenameone.examples.bazaar.carrier.CarrierProvider]) for the
///   cart's weight and the address, one option per service. `service` is a
///   comma separated list of the services to offer (empty offers them all),
///   `markupBps` is added to the carrier's price (in hundredths of a
///   percent, half up) and `priceCents` is a flat handling fee on top. When
///   the carrier cannot answer, the rate offers `fallbackCents` instead if
///   that is above 0, and otherwise nothing: the zone's other rates still
///   stand, so a carrier outage never blocks checkout.
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
    public static final String CARRIER = "CARRIER";
    /// Separates a carrier rate's id from the service in an option id.
    public static final char SERVICE_SEPARATOR = '~';
    private static final long BPS = 10000L;
    public static final String REST_OF_WORLD = "*";

    private ShippingCalculator() {
    }

    /// One way the order can reach the customer.
    public static final class Option {
        public final ShippingRate rate;
        public final long priceCents;
        /// What the shopper picks: the rate's id, or for a carrier rate the
        /// rate's id, [#SERVICE_SEPARATOR] and the carrier's service code.
        public final String id;
        public final String name;
        /// The carrier service, or empty for a rate the store prices.
        public final String service;
        public final int etaDaysMin;
        public final int etaDaysMax;

        Option(ShippingRate rate, long priceCents) {
            this(rate, priceCents, rate.getId(), rate.getName(), "", rate.getEtaDaysMin(),
                    rate.getEtaDaysMax());
        }

        /// This option under another name and price, as a checkout function
        /// renames or reprices it; everything else is kept.
        public Option with(String newName, long newPriceCents) {
            return new Option(rate, newPriceCents, id, newName, service, etaDaysMin, etaDaysMax);
        }

        Option(ShippingRate rate, long priceCents, String id, String name, String service,
                int etaDaysMin, int etaDaysMax) {
            this.rate = rate;
            this.priceCents = priceCents;
            this.id = id;
            this.name = name;
            this.service = service;
            this.etaDaysMin = etaDaysMin;
            this.etaDaysMax = etaDaysMax;
        }
    }

    /// Asks the carrier what a CARRIER rate's services cost for this cart
    /// and address. Answers null when the carrier cannot be reached or
    /// refused, never throws.
    public interface CarrierQuotes {
        List<RateQuoteDto> quotes(ShippingRate rate, int weightGrams);
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
        return options(zones, rates, locations, country, hasPoint, lat, lng, subtotalCents,
                weightGrams, freeShipping, null);
    }

    /// [#options], with `carrier` pricing the CARRIER rates; without one
    /// they offer their fallback.
    public static List<Option> options(List<ShippingZone> zones, List<ShippingRate> rates,
            Map<String, Location> locations, String country, boolean hasPoint, double lat,
            double lng, long subtotalCents, int weightGrams, boolean freeShipping,
            CarrierQuotes carrier) {
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
            if (CARRIER.equals(kind)) {
                carrierOptions(out, rate, carrier == null ? null : carrier.quotes(rate,
                        weightGrams), freeShipping);
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

    private static void carrierOptions(List<Option> out, ShippingRate rate,
            List<RateQuoteDto> quotes, boolean freeShipping) {
        int before = out.size();
        for (int iter = 0; quotes != null && iter < quotes.size(); iter++) {
            RateQuoteDto q = quotes.get(iter);
            if (q == null || q.service() == null || q.service().length() == 0 || q.cents() < 0
                    || !offersService(rate.getService(), q.service())) {
                continue;
            }
            long price = carrierPrice(q.cents(), rate.getMarkupBps(), rate.getPriceCents());
            if (price < 0) {
                continue;
            }
            String title = q.title() == null || q.title().length() == 0 ? q.service() : q.title();
            String name = rate.getName().length() > 0 ? rate.getName() + " - " + title : title;
            int days = Math.max(0, q.days());
            out.add(new Option(rate, freeShipping ? 0 : price, rate.getId() + SERVICE_SEPARATOR
                    + q.service(), name, q.service(), days, days));
        }
        if (out.size() == before && rate.getFallbackCents() > 0) {
            // The carrier did not answer (or offered none of the services):
            // the merchant's fallback price keeps checkout open.
            out.add(new Option(rate, freeShipping ? 0 : rate.getFallbackCents()));
        }
    }

    /// The carrier's `cents` with `markupBps` added half up and `handlingCents`
    /// on top; -1 when the sum would overflow.
    public static long carrierPrice(long cents, int markupBps, long handlingCents) {
        try {
            long base = Math.max(0, cents);
            long markup = Math.multiplyExact(base, (long) Math.max(0, markupBps));
            markup = Math.addExact(markup, BPS / 2) / BPS;
            return Math.addExact(Math.addExact(base, markup), Math.max(0, handlingCents));
        } catch (ArithmeticException overflow) {
            return -1;
        }
    }

    /// Whether the comma separated `list` (empty = every service) names
    /// `service`, ignoring ASCII case and spaces.
    public static boolean offersService(String list, String service) {
        if (list == null || list.trim().length() == 0) {
            return true;
        }
        int start = 0;
        while (true) {
            int comma = list.indexOf(',', start);
            int end = comma < 0 ? list.length() : comma;
            if (list.substring(start, end).trim().equalsIgnoreCase(service)) {
                return true;
            }
            if (comma < 0) {
                return false;
            }
            start = comma + 1;
        }
    }

    /// The physical lines of a cart that share a shipping profile: their
    /// subtotal after discounts and their weight. The empty profile is the
    /// store's general one, which every product is in until it is assigned.
    public static final class Group {
        public final String profileId;
        public final long subtotalCents;
        public final int weightGrams;

        public Group(String profileId, long subtotalCents, int weightGrams) {
            this.profileId = profileId == null ? "" : profileId;
            this.subtotalCents = subtotalCents;
            this.weightGrams = weightGrams;
        }
    }

    /// The zones of one shipping profile; the empty id is the general profile.
    public static List<ShippingZone> zonesOf(List<ShippingZone> zones, String profileId) {
        String want = profileId == null ? "" : profileId;
        List<ShippingZone> out = new ArrayList<ShippingZone>();
        for (int iter = 0; zones != null && iter < zones.size(); iter++) {
            String has = zones.get(iter).getProfileId();
            if (want.equals(has == null ? "" : has)) {
                out.add(zones.get(iter));
            }
        }
        return out;
    }

    /// The options for a cart whose lines fall into shipping profiles
    /// (`groups`, the general profile first when the cart has any of it).
    ///
    /// The first group is priced as [#options] prices a whole cart, over the
    /// zones of its own profile, and the shopper chooses among its options.
    /// Every other group is priced the same way over its profile's zones and
    /// adds its cheapest delivery option to each of those choices, so a cart
    /// of a sofa and a lamp pays the oversized rate plus the general rate it
    /// picked. A group whose profile has no rate for the address makes every
    /// delivery option go away -- the cart cannot be sent there -- but pickup
    /// stays: the shopper collects the whole order, for the pickup price
    /// alone. A free-shipping discount makes every group cost nothing.
    public static List<Option> combined(List<ShippingZone> zones, List<ShippingRate> rates,
            Map<String, Location> locations, String country, boolean hasPoint, double lat,
            double lng, List<Group> groups, boolean freeShipping, CarrierQuotes carrier) {
        if (groups == null || groups.isEmpty()) {
            return new ArrayList<Option>();
        }
        Group first = groups.get(0);
        List<Option> primary = options(zonesOf(zones, first.profileId), rates, locations, country,
                hasPoint, lat, lng, first.subtotalCents, first.weightGrams, freeShipping, carrier);
        if (groups.size() == 1) {
            return primary;
        }
        long extra = 0;
        int slowest = 0;
        boolean blocked = false;
        for (int iter = 1; iter < groups.size() && !blocked; iter++) {
            Group g = groups.get(iter);
            List<Option> own = options(zonesOf(zones, g.profileId), rates, locations, country,
                    hasPoint, lat, lng, g.subtotalCents, g.weightGrams, freeShipping, carrier);
            Option best = null;
            for (int k = 0; k < own.size(); k++) {
                Option o = own.get(k);
                if (!PICKUP.equals(o.rate.getKind())
                        && (best == null || o.priceCents < best.priceCents)) {
                    best = o;
                }
            }
            if (best == null) {
                blocked = true;
            } else {
                extra = saturatingAdd(extra, best.priceCents);
                slowest = Math.max(slowest, best.etaDaysMax);
            }
        }
        List<Option> out = new ArrayList<Option>();
        for (int iter = 0; iter < primary.size(); iter++) {
            Option o = primary.get(iter);
            if (PICKUP.equals(o.rate.getKind())) {
                out.add(o);
            } else if (!blocked) {
                out.add(new Option(o.rate, saturatingAdd(o.priceCents, extra), o.id, o.name,
                        o.service, o.etaDaysMin, Math.max(o.etaDaysMax, slowest)));
            }
        }
        return out;
    }

    private static long saturatingAdd(long a, long b) {
        long sum = a + b;
        return sum < a ? Long.MAX_VALUE : sum;
    }

    /// The option for `rateId` (an [Option#id]), or null when the address
    /// does not get it.
    /// `o` named `name`, for a pickup the checkout titles by its location.
    public static Option renamed(Option o, String name) {
        return new Option(o.rate, o.priceCents, o.id, name, o.service, o.etaDaysMin,
                o.etaDaysMax);
    }

    public static Option find(List<Option> options, String rateId) {
        if (rateId == null) {
            return null;
        }
        for (int iter = 0; iter < options.size(); iter++) {
            if (rateId.equals(options.get(iter).id)) {
                return options.get(iter);
            }
        }
        return null;
    }
}
