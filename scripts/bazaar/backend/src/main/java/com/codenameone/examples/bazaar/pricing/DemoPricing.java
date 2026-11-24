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

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.ShippingRate;
import com.codenameone.examples.bazaar.domain.ShippingZone;
import com.codenameone.examples.bazaar.domain.TaxRate;
import com.codenameone.examples.bazaar.repository.DiscountRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.ShippingRateRepository;
import com.codenameone.examples.bazaar.repository.ShippingZoneRepository;
import com.codenameone.examples.bazaar.repository.TaxRateRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/// Sample shipping, tax and discounts for a demo store; for development and
/// tests only, never called in production.
///
/// [#seed] gives a store four zones (Domestic US, Canada, Europe, Rest of
/// world) with a rate of every kind among them, a few US state sales taxes
/// and EU VAT rates, and three codes: WELCOME10 (10% off, once per
/// customer), FREESHIP (free shipping over $75) and BUNDLE (buy 2, get 1
/// free). Pickup and local delivery use the store's first location and are
/// left out when it has none. Seeding a store twice does nothing the second
/// time.
@Component
public class DemoPricing {
    private final ShippingZoneRepository zones;
    private final ShippingRateRepository rates;
    private final TaxRateRepository taxes;
    private final DiscountRepository discounts;
    private final LocationRepository locations;

    public DemoPricing(ShippingZoneRepository zones, ShippingRateRepository rates,
            TaxRateRepository taxes, DiscountRepository discounts, LocationRepository locations) {
        this.zones = zones;
        this.rates = rates;
        this.taxes = taxes;
        this.discounts = discounts;
        this.locations = locations;
    }

    /// The tax a demo register at the counter charges: the store's first
    /// rate, which `Pos.addRegister` also falls back to. A register with no
    /// tax country charges no tax at all, and the demo registers used to be
    /// seeded that way, so the counter sold tax free while the store had
    /// rates.
    public TaxRate counterTax(String storeId) {
        List<TaxRate> all = taxes.findByStoreId(storeId);
        return all.isEmpty() ? null : all.get(0);
    }

    @Transactional(rollbackFor = IOException.class)
    public void seed(String storeId) throws IOException {
        if (!zones.findByStoreIdOrderByPositionAsc(storeId).isEmpty()) {
            return;
        }
        List<Location> at = locations.findByStoreIdOrderByPositionAsc(storeId);
        Location home = at.isEmpty() ? null : at.get(0);

        String us = zone(storeId, "Domestic US", "US", 0);
        rate(storeId, us, "Standard", ShippingCalculator.WEIGHT, 599, 0, 2000, "USPS", 3, 5, null, 0, 0);
        rate(storeId, us, "Standard (heavy)", ShippingCalculator.WEIGHT, 1299, 2000, 0, "USPS", 3, 6,
                null, 0, 1);
        rate(storeId, us, "Express", ShippingCalculator.FLAT, 1999, 0, 0, "UPS", 1, 2, null, 0, 2);
        rate(storeId, us, "Free over $75", ShippingCalculator.FREE_OVER, 0, 7500, 0, "USPS", 4, 7,
                null, 0, 3);
        if (home != null) {
            rate(storeId, us, "Same-day local delivery", ShippingCalculator.LOCAL, 499, 0, 0, "", 0, 0,
                    home.getId(), 15000, 4);
            rate(storeId, "", "Pick up in store", ShippingCalculator.PICKUP, 0, 0, 0, "", 0, 0,
                    home.getId(), 0, 5);
        }
        String ca = zone(storeId, "Canada", "CA", 1);
        rate(storeId, ca, "Standard", ShippingCalculator.PRICE, 1499, 0, 10000, "Canada Post", 5, 9,
                null, 0, 0);
        rate(storeId, ca, "Standard", ShippingCalculator.PRICE, 799, 10000, 0, "Canada Post", 5, 9,
                null, 0, 1);
        String eu = zone(storeId, "Europe",
                "AT,BE,BG,HR,CY,CZ,DK,EE,FI,FR,DE,GR,HU,IE,IT,LV,LT,LU,MT,NL,PL,PT,RO,SK,SI,ES,SE",
                2);
        rate(storeId, eu, "International", ShippingCalculator.FLAT, 2499, 0, 0, "DHL", 6, 12, null, 0, 0);
        String world = zone(storeId, "Rest of world", ShippingCalculator.REST_OF_WORLD, 3);
        rate(storeId, world, "International", ShippingCalculator.FLAT, 3499, 0, 0, "DHL", 8, 20, null,
                0, 0);

        tax(storeId, "US", "CA", "California sales tax", 725, false);
        tax(storeId, "US", "NY", "New York sales tax", 400, true);
        tax(storeId, "US", "TX", "Texas sales tax", 625, true);
        tax(storeId, "US", "WA", "Washington sales tax", 650, true);
        tax(storeId, "CA", "", "GST", 500, true);
        tax(storeId, "DE", "", "VAT", 1900, true);
        tax(storeId, "FR", "", "VAT", 2000, true);
        tax(storeId, "NL", "", "VAT", 2100, true);
        tax(storeId, "IE", "", "VAT", 2300, true);

        discount(storeId, "WELCOME10", "10% off your first order", DiscountEngine.PERCENT, 1000, 0,
                0, 0, true, false);
        discount(storeId, "FREESHIP", "Free shipping over $75", DiscountEngine.FREE_SHIPPING, 0,
                7500, 0, 0, false, true);
        discount(storeId, "BUNDLE", "Buy 2, get 1 free", DiscountEngine.BUY_X_GET_Y, 0, 0, 2, 1,
                false, false);
    }

    private String zone(String storeId, String name, String countries, int position)
            throws IOException {
        ShippingZone z = new ShippingZone();
        z.setId(Ids.next());
        z.setStoreId(storeId);
        z.setName(name);
        z.setCountries(countries);
        z.setPosition(position);
        zones.insert(z);
        return z.getId();
    }

    private void rate(String storeId, String zoneId, String name, String kind, long price,
            long min, long max, String carrier, int etaMin, int etaMax, String locationId,
            int radius, int position) throws IOException {
        ShippingRate r = new ShippingRate();
        r.setId(Ids.next());
        r.setZoneId(zoneId);
        r.setStoreId(storeId);
        r.setName(name);
        r.setKind(kind);
        r.setPriceCents(price);
        r.setMinValue(min);
        r.setMaxValue(max);
        r.setCarrier(carrier);
        r.setService(name);
        r.setEtaDaysMin(etaMin);
        r.setEtaDaysMax(etaMax);
        r.setLocationId(locationId == null ? "" : locationId);
        r.setRadiusMeters(radius);
        r.setPosition(position);
        rates.insert(r);
    }

    private void tax(String storeId, String country, String region, String name, int bp,
            boolean shipping) throws IOException {
        TaxRate t = new TaxRate();
        t.setId(Ids.next());
        t.setStoreId(storeId);
        t.setCountry(country);
        t.setRegion(region);
        t.setName(name);
        t.setBasisPoints(bp);
        t.setAppliesToShipping(shipping);
        taxes.insert(t);
    }

    private void discount(String storeId, String code, String title, String kind, long amount,
            long min, int buy, int get, boolean once, boolean combinable) throws IOException {
        Discount d = new Discount();
        d.setId(Ids.next());
        d.setStoreId(storeId);
        d.setCode(code);
        d.setTitle(title);
        d.setKind(kind);
        d.setAmount(amount);
        d.setMinSubtotalCents(min);
        d.setBuyQty(buy);
        d.setGetQty(get);
        d.setAppliesTo(DiscountEngine.ALL);
        d.setTargetId("");
        d.setOncePerCustomer(once);
        d.setCombinable(combinable);
        d.setActive(true);
        d.setCreatedAt(Instant.now());
        discounts.insert(d);
    }
}
