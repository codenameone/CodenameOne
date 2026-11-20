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
package com.codenameone.examples.bazaar;

import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.ShippingRate;
import com.codenameone.examples.bazaar.domain.ShippingZone;
import com.codenameone.examples.bazaar.domain.TaxRate;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.pricing.Money;
import com.codenameone.examples.bazaar.pricing.PriceLine;
import com.codenameone.examples.bazaar.pricing.ShippingCalculator;
import com.codenameone.examples.bazaar.pricing.TaxCalculator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The three calculators, with no database: discounts, tax and shipping.
class PricingTest {
    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Set<String> NONE = Collections.<String>emptySet();

    private static PriceLine line(String key, long unit, int qty) {
        return new PriceLine(key, "p-" + key, "v-" + key, unit, qty, 100, true);
    }

    private static Discount discount(String kind, long amount) {
        Discount d = new Discount();
        d.setId("d-" + kind + amount);
        d.setCode("CODE" + amount);
        d.setKind(kind);
        d.setAmount(amount);
        d.setAppliesTo(DiscountEngine.ALL);
        d.setActive(true);
        return d;
    }

    private static Discount auto(String kind, long amount) {
        Discount d = discount(kind, amount);
        d.setCode("");
        d.setAutomatic(true);
        return d;
    }

    private static long sumDiscounts(List<PriceLine> lines) {
        long sum = 0;
        for (int iter = 0; iter < lines.size(); iter++) {
            assertTrue(lines.get(iter).discountCents >= 0);
            assertTrue(lines.get(iter).netCents() >= 0);
            sum += lines.get(iter).discountCents;
        }
        return sum;
    }

    private static DiscountEngine.Result code(List<PriceLine> lines, Discount d) {
        return DiscountEngine.apply(lines, null, d, d.getCode(), NOW, NONE, 0);
    }

    // ------------------------------------------------------------------ money

    @Test
    void halfEvenRounding() {
        assertEquals(2, Money.divideHalfEven(5, 2));
        assertEquals(4, Money.divideHalfEven(7, 2));
        assertEquals(2, Money.divideHalfEven(3, 2));
        assertEquals(2, Money.divideHalfEven(5, 2));
        assertEquals(-2, Money.divideHalfEven(-5, 2));
        assertEquals(3, Money.divideHalfEven(8, 3));
        assertEquals(0, Money.divideHalfEven(10, 0));
        // 15% of 10 cents is 1.5 -> 2; of 30 cents is 4.5 -> 4.
        assertEquals(2, Money.basisPoints(10, 1500));
        assertEquals(4, Money.basisPoints(30, 1500));
    }

    @Test
    void listMatchingIgnoresCaseAndSpaces() {
        assertTrue(Money.listHas("US, ca ,MX", "CA"));
        assertTrue(Money.listHas("*", "*"));
        assertFalse(Money.listHas("USA", "US"));
        assertFalse(Money.listHas(null, "US"));
    }

    // -------------------------------------------------------------- discounts

    @Test
    void percentOffRoundsHalfEven() {
        List<PriceLine> lines = Arrays.asList(line("a", 1999, 1));
        DiscountEngine.Result r = code(lines, discount(DiscountEngine.PERCENT, 1000));
        // 10% of 19.99 is 1.999 -> 2.00
        assertEquals(200, r.totalCents);
        assertEquals(1799, lines.get(0).netCents());
        assertNull(r.codeProblem);
    }

    @Test
    void percentIsSpreadExactlyWithRemainderCents() {
        List<PriceLine> lines = Arrays.asList(line("a", 100, 1), line("b", 100, 1),
                line("c", 100, 1));
        DiscountEngine.Result r = code(lines, discount(DiscountEngine.FIXED, 100));
        assertEquals(100, r.totalCents);
        assertEquals(100, sumDiscounts(lines));
        // 33.33 each: the leftover cent goes to the first line on a tie.
        assertEquals(34, lines.get(0).discountCents);
        assertEquals(33, lines.get(1).discountCents);
        assertEquals(33, lines.get(2).discountCents);
    }

    @Test
    void allocationFollowsLineValue() {
        List<PriceLine> lines = Arrays.asList(line("a", 300, 1), line("b", 100, 1));
        code(lines, discount(DiscountEngine.FIXED, 101));
        assertEquals(76, lines.get(0).discountCents);
        assertEquals(25, lines.get(1).discountCents);
    }

    @Test
    void fixedNeverGoesBelowZero() {
        List<PriceLine> lines = Arrays.asList(line("a", 500, 1), line("b", 250, 2));
        DiscountEngine.Result r = code(lines, discount(DiscountEngine.FIXED, 5000));
        assertEquals(1000, r.totalCents);
        assertEquals(0, lines.get(0).netCents());
        assertEquals(0, lines.get(1).netCents());
    }

    @Test
    void buyTwoGetOneDiscountsTheCheapest() {
        List<PriceLine> lines = Arrays.asList(line("a", 3000, 1), line("b", 1000, 1),
                line("c", 2000, 1));
        Discount bogo = discount(DiscountEngine.BUY_X_GET_Y, 0);
        bogo.setBuyQty(2);
        bogo.setGetQty(1);
        DiscountEngine.Result r = code(lines, bogo);
        assertEquals(1000, r.totalCents);
        assertEquals(0, lines.get(0).discountCents);
        assertEquals(1000, lines.get(1).discountCents);
        assertEquals(0, lines.get(2).discountCents);
    }

    @Test
    void buyTwoGetOneCountsEveryFullGroup() {
        // Seven units of one price: two full groups, two free units.
        List<PriceLine> lines = Arrays.asList(line("a", 500, 7));
        Discount bogo = discount(DiscountEngine.BUY_X_GET_Y, 0);
        bogo.setBuyQty(2);
        bogo.setGetQty(1);
        assertEquals(1000, code(lines, bogo).totalCents);
    }

    @Test
    void buyOneGetOneHalfOff() {
        List<PriceLine> lines = Arrays.asList(line("a", 999, 2));
        Discount bogo = discount(DiscountEngine.BUY_X_GET_Y, 5000);
        bogo.setBuyQty(1);
        bogo.setGetQty(1);
        // Half of 9.99 is 4.995 -> 5.00 (half even: 499.5 -> 500).
        assertEquals(500, code(lines, bogo).totalCents);
    }

    @Test
    void buyXGetYNeedsAFullGroup() {
        List<PriceLine> lines = Arrays.asList(line("a", 500, 2));
        Discount bogo = discount(DiscountEngine.BUY_X_GET_Y, 0);
        bogo.setBuyQty(2);
        bogo.setGetQty(1);
        assertEquals(0, code(lines, bogo).totalCents);
    }

    @Test
    void minimumSubtotal() {
        List<PriceLine> lines = Arrays.asList(line("a", 1000, 1));
        Discount d = discount(DiscountEngine.FIXED, 500);
        d.setMinSubtotalCents(5000);
        DiscountEngine.Result r = code(lines, d);
        assertEquals(0, r.totalCents);
        assertNotNull(r.codeProblem);
    }

    @Test
    void scheduleAndLimits() {
        Discount d = discount(DiscountEngine.FIXED, 100);
        d.setStartsAt(NOW.plusSeconds(60));
        assertNotNull(DiscountEngine.problem(d, 1000, NOW, NONE));
        d.setStartsAt(NOW.minusSeconds(60));
        d.setEndsAt(NOW);
        assertNotNull(DiscountEngine.problem(d, 1000, NOW, NONE));
        d.setEndsAt(NOW.plusSeconds(60));
        assertNull(DiscountEngine.problem(d, 1000, NOW, NONE));
        d.setUsageLimit(3);
        d.setUsedCount(3);
        assertNotNull(DiscountEngine.problem(d, 1000, NOW, NONE));
        d.setUsedCount(2);
        assertNull(DiscountEngine.problem(d, 1000, NOW, NONE));
        d.setOncePerCustomer(true);
        Set<String> used = new HashSet<String>();
        used.add(d.getId());
        assertNotNull(DiscountEngine.problem(d, 1000, NOW, used));
        d.setActive(false);
        assertNotNull(DiscountEngine.problem(d, 1000, NOW, NONE));
    }

    @Test
    void productAndCollectionTargets() {
        PriceLine shirt = line("shirt", 2000, 1);
        PriceLine mug = line("mug", 1000, 1);
        mug.collectionIds.add("kitchen");
        List<PriceLine> lines = Arrays.asList(shirt, mug);
        Discount d = discount(DiscountEngine.PERCENT, 5000);
        d.setAppliesTo(DiscountEngine.COLLECTION);
        d.setTargetId("kitchen");
        assertEquals(500, code(lines, d).totalCents);
        assertEquals(0, shirt.discountCents);
        assertEquals(500, mug.discountCents);
        d.setAppliesTo(DiscountEngine.PRODUCT);
        d.setTargetId("p-shirt");
        assertEquals(1000, code(lines, d).totalCents);
        assertEquals(1000, shirt.discountCents);
        assertEquals(0, mug.discountCents);
    }

    @Test
    void combinableDiscountsStack() {
        List<PriceLine> lines = Arrays.asList(line("a", 10000, 1));
        Discount ten = auto(DiscountEngine.PERCENT, 1000);
        ten.setCombinable(true);
        Discount five = discount(DiscountEngine.FIXED, 500);
        five.setCombinable(true);
        DiscountEngine.Result r = DiscountEngine.apply(lines, Arrays.asList(ten), five,
                five.getCode(), NOW, NONE, 0);
        assertEquals(2, r.applied.size());
        // 10% of 100.00, then 5.00 more.
        assertEquals(1500, r.totalCents);
        assertNull(r.codeProblem);
    }

    @Test
    void theBetterExclusiveDiscountWins() {
        List<PriceLine> lines = Arrays.asList(line("a", 10000, 1));
        Discount auto = auto(DiscountEngine.PERCENT, 2000);
        Discount small = discount(DiscountEngine.FIXED, 500);
        DiscountEngine.Result r = DiscountEngine.apply(lines, Arrays.asList(auto), small,
                small.getCode(), NOW, NONE, 0);
        assertEquals(2000, r.totalCents);
        assertEquals(1, r.applied.size());
        assertNotNull(r.codeProblem, "the shopper is told the code did nothing");
        // A bigger code beats the automatic one.
        Discount big = discount(DiscountEngine.FIXED, 3000);
        r = DiscountEngine.apply(lines, Arrays.asList(auto), big, big.getCode(), NOW, NONE, 0);
        assertEquals(3000, r.totalCents);
        assertNull(r.codeProblem);
    }

    @Test
    void freeShippingIsWeighedAtTheShippingPrice() {
        List<PriceLine> lines = Arrays.asList(line("a", 10000, 1));
        Discount ship = discount(DiscountEngine.FREE_SHIPPING, 0);
        DiscountEngine.Result r = DiscountEngine.apply(lines, null, ship, ship.getCode(), NOW,
                NONE, 1500);
        assertTrue(r.freeShipping);
        assertEquals(0, r.totalCents);
        assertEquals(0, lines.get(0).discountCents);
    }

    @Test
    void unknownCodeIsReported() {
        List<PriceLine> lines = Arrays.asList(line("a", 1000, 1));
        DiscountEngine.Result r = DiscountEngine.apply(lines, null, null, "NOPE", NOW, NONE, 0);
        assertEquals("That code is not valid", r.codeProblem);
        assertEquals(0, r.totalCents);
    }

    // -------------------------------------------------------------------- tax

    private static TaxRate rate(String country, String region, int bp, boolean shipping) {
        TaxRate r = new TaxRate();
        r.setId(country + region + bp);
        r.setCountry(country);
        r.setRegion(region);
        r.setName(country + region);
        r.setBasisPoints(bp);
        r.setAppliesToShipping(shipping);
        return r;
    }

    private static final List<TaxRate> RATES = Arrays.asList(rate("US", "CA", 725, false),
            rate("US", "NY", 400, true), rate("US", "", 100, false), rate("DE", "", 1900, true),
            rate("CA", "", 500, true), rate("CA", "QC", 998, true), rate("CA", "QC", 500, true));

    @Test
    void exclusiveTaxPerLineHalfEven() {
        List<PriceLine> lines = Arrays.asList(line("a", 1000, 1), line("b", 333, 3));
        TaxCalculator.Result r = TaxCalculator.apply(lines, 500, RATES, "us", "ca", false);
        // 10.00 * 7.25% = 0.725 -> 72 (half even); 9.99 * 7.25% = 72.4275 -> 72.
        assertEquals(72, lines.get(0).taxCents);
        assertEquals(72, lines.get(1).taxCents);
        assertEquals(144, r.linesTaxCents);
        assertEquals(0, r.shippingTaxCents, "California does not tax shipping here");
        assertEquals(144, r.addedCents());
    }

    @Test
    void shippingTaxedWhereTheRateSaysSo() {
        List<PriceLine> lines = Arrays.asList(line("a", 1000, 1));
        TaxCalculator.Result r = TaxCalculator.apply(lines, 1000, RATES, "US", "NY", false);
        assertEquals(40, r.linesTaxCents);
        assertEquals(40, r.shippingTaxCents);
        assertEquals(80, r.totalCents());
    }

    @Test
    void regionFallsBackToTheCountryRate() {
        List<PriceLine> lines = Arrays.asList(line("a", 1000, 1));
        TaxCalculator.Result r = TaxCalculator.apply(lines, 0, RATES, "US", "OR", false);
        assertEquals(10, r.linesTaxCents);
        assertEquals(1, TaxCalculator.matching(RATES, "US", "").size());
        assertEquals(0, TaxCalculator.matching(RATES, "JP", "").size());
    }

    @Test
    void regionalRatesAddUp() {
        List<PriceLine> lines = Arrays.asList(line("a", 10000, 1));
        TaxCalculator.Result r = TaxCalculator.apply(lines, 0, RATES, "CA", "QC", false);
        assertEquals(1498, r.basisPoints);
        assertEquals(1498, r.linesTaxCents);
    }

    @Test
    void inclusiveTaxIsInsideThePrice() {
        List<PriceLine> lines = Arrays.asList(line("a", 1190, 1), line("b", 1000, 1));
        TaxCalculator.Result r = TaxCalculator.apply(lines, 0, RATES, "DE", "", true);
        // 11.90 incl. 19% VAT holds 1.90 of tax; 10.00 holds 1.5966 -> 1.60.
        assertEquals(190, lines.get(0).taxCents);
        assertEquals(160, lines.get(1).taxCents);
        assertEquals(350, r.totalCents());
        assertEquals(0, r.addedCents(), "nothing is added on top");
    }

    @Test
    void taxComesAfterDiscountsAndSkipsUntaxable() {
        PriceLine taxed = line("a", 1000, 1);
        taxed.discountCents = 500;
        PriceLine food = new PriceLine("f", "p-f", "v-f", 1000, 1, 0, false);
        TaxCalculator.Result r = TaxCalculator.apply(Arrays.asList(taxed, food), 0, RATES,
                "US", "NY", false);
        assertEquals(20, taxed.taxCents);
        assertEquals(0, food.taxCents);
        assertEquals(20, r.linesTaxCents);
    }

    // --------------------------------------------------------------- shipping

    private static ShippingZone zone(String id, String countries) {
        ShippingZone z = new ShippingZone();
        z.setId(id);
        z.setCountries(countries);
        return z;
    }

    private static ShippingRate rate(String id, String zone, String kind, long price, long min,
            long max) {
        ShippingRate r = new ShippingRate();
        r.setId(id);
        r.setZoneId(zone);
        r.setKind(kind);
        r.setName(id);
        r.setPriceCents(price);
        r.setMinValue(min);
        r.setMaxValue(max);
        return r;
    }

    private static List<String> ids(List<ShippingCalculator.Option> options) {
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < options.size(); iter++) {
            out.add(options.get(iter).rate.getId() + "=" + options.get(iter).priceCents);
        }
        return out;
    }

    private static final List<ShippingZone> ZONES = Arrays.asList(zone("us", "US"),
            zone("eu", "DE, FR"), zone("world", "*"));

    private List<ShippingRate> rates() {
        List<ShippingRate> out = new ArrayList<ShippingRate>();
        out.add(rate("flat", "us", ShippingCalculator.FLAT, 999, 0, 0));
        out.add(rate("light", "us", ShippingCalculator.WEIGHT, 500, 0, 1000));
        out.add(rate("heavy", "us", ShippingCalculator.WEIGHT, 1500, 1000, 0));
        out.add(rate("cheap", "us", ShippingCalculator.PRICE, 800, 0, 5000));
        out.add(rate("dear", "us", ShippingCalculator.PRICE, 400, 5000, 0));
        out.add(rate("free", "us", ShippingCalculator.FREE_OVER, 999, 7500, 0));
        ShippingRate local = rate("local", "us", ShippingCalculator.LOCAL, 300, 0, 0);
        local.setLocationId("shop");
        local.setRadiusMeters(5000);
        out.add(local);
        ShippingRate pickup = rate("pickup", "", ShippingCalculator.PICKUP, 0, 0, 0);
        pickup.setLocationId("shop");
        out.add(pickup);
        out.add(rate("eu", "eu", ShippingCalculator.FLAT, 2500, 0, 0));
        out.add(rate("world", "world", ShippingCalculator.FLAT, 3500, 0, 0));
        return out;
    }

    private static Map<String, Location> shop() {
        Location l = new Location();
        l.setId("shop");
        l.setActive(true);
        l.setLat(40.0);
        l.setLng(-74.0);
        Map<String, Location> out = new HashMap<String, Location>();
        out.put("shop", l);
        return out;
    }

    @Test
    void tiersByWeightAndPrice() {
        List<ShippingCalculator.Option> o = ShippingCalculator.options(ZONES, rates(), shop(), "US",
                false, 0, 0, 2000, 400, false);
        assertEquals(Arrays.asList("flat=999", "light=500", "cheap=800", "pickup=0"), ids(o));
        o = ShippingCalculator.options(ZONES, rates(), shop(), "US", false, 0, 0, 8000, 1000,
                false);
        assertEquals(Arrays.asList("flat=999", "heavy=1500", "dear=400", "free=0", "pickup=0"),
                ids(o));
    }

    @Test
    void zonesByCountryAndRestOfWorld() {
        assertEquals("eu", ShippingCalculator.zone(ZONES, "fr").getId());
        assertEquals("world", ShippingCalculator.zone(ZONES, "JP").getId());
        assertNull(ShippingCalculator.zone(Arrays.asList(zone("us", "US")), "JP"));
        List<ShippingCalculator.Option> o = ShippingCalculator.options(ZONES, rates(), shop(), "JP",
                false, 0, 0, 2000, 400, false);
        assertEquals(Arrays.asList("world=3500", "pickup=0"), ids(o));
    }

    @Test
    void localDeliveryWithinTheRadius() {
        // About 1.1 km north of the shop.
        List<ShippingCalculator.Option> near = ShippingCalculator.options(ZONES, rates(), shop(),
                "US", true, 40.01, -74.0, 2000, 400, false);
        assertTrue(ids(near).contains("local=300"));
        // About 11 km away.
        List<ShippingCalculator.Option> far = ShippingCalculator.options(ZONES, rates(), shop(),
                "US", true, 40.1, -74.0, 2000, 400, false);
        assertFalse(ids(far).contains("local=300"));
        // No point, no local delivery.
        assertFalse(ids(ShippingCalculator.options(ZONES, rates(), shop(), "US", false, 0, 0, 2000,
                400, false)).contains("local=300"));
    }

    @Test
    void freeShippingZeroesDeliveryButNotPickupPrice() {
        List<ShippingRate> rs = rates();
        rs.get(7).setPriceCents(100);
        List<ShippingCalculator.Option> o = ShippingCalculator.options(ZONES, rs, shop(), "US",
                false, 0, 0, 2000, 400, true);
        assertEquals(Arrays.asList("flat=0", "light=0", "cheap=0", "pickup=100"), ids(o));
        assertEquals("light", ShippingCalculator.find(o, "light").rate.getId());
        assertNull(ShippingCalculator.find(o, "heavy"));
    }

    @Test
    void inactivePickupLocationIsNotOffered() {
        Map<String, Location> closed = shop();
        closed.get("shop").setActive(false);
        List<ShippingCalculator.Option> o = ShippingCalculator.options(ZONES, rates(), closed, "US",
                true, 40.0, -74.0, 2000, 400, false);
        assertFalse(ids(o).contains("pickup=0"));
        assertFalse(ids(o).contains("local=300"));
    }
}
