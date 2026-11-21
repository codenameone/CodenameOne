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

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.carrier.CarrierRates;
import com.codenameone.examples.bazaar.carrier.SimulatedCarrier;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.domain.ShippingRate;
import com.codenameone.examples.bazaar.domain.ShippingZone;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.pricing.ShippingCalculator;
import com.codenameone.examples.bazaar.repository.ShippingRateRepository;
import com.codenameone.examples.bazaar.repository.ShippingZoneRepository;
import com.codenameone.examples.bazaar.store.Stores;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Carrier-calculated rates at checkout: the simulated carrier prices each
/// offered service with the merchant's markup and handling fee, the chosen
/// one is charged, a service or rate the address was not offered is
/// refused at place time, quotes are cached per cart and address, and a
/// carrier outage falls back to the configured price instead of blocking
/// checkout.
@BackendTest
class CarrierCheckoutTest {
    /// An address the simulated carrier refuses to price.
    static final AddressDto DOWN = new AddressDto("Ada Lovelace", "1 Main St", "", "Albany",
            "NY", SimulatedCarrier.OUTAGE_POSTAL, "US", "", 42.65, -73.75);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private DataSource db;
    @Autowired
    private Inventory inventory;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private ShippingZoneRepository zones;
    @Autowired
    private ShippingRateRepository rates;
    @Autowired
    private CarrierRates carrierRates;

    /// A CARRIER rate in the shop's US zone: Ground and Express, 10% markup,
    /// 1.00 handling, 15.00 when the carrier is down.
    private String carrierRate(Shop s) throws Exception {
        String us = null;
        List<ShippingZone> all = zones.findByStoreIdOrderByPositionAsc(s.storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if ("US".equals(all.get(iter).getCountries())) {
                us = all.get(iter).getId();
            }
        }
        assertNotNull(us);
        ShippingRate r = new ShippingRate();
        r.setId(Ids.next());
        r.setStoreId(s.storeId);
        r.setZoneId(us);
        r.setName("Live");
        r.setKind(ShippingCalculator.CARRIER);
        r.setPriceCents(100);
        r.setMarkupBps(1000);
        r.setFallbackCents(1500);
        r.setCarrier(SimulatedCarrier.NAME);
        r.setService("GROUND, express");
        r.setPosition(10);
        rates.insert(r);
        return r.getId();
    }

    private static ShippingOptionDto option(CartDto c, String id) {
        for (ShippingOptionDto o : c.shippingOptions()) {
            if (o.rateId().equals(id)) {
                return o;
            }
        }
        return null;
    }

    private long shippingOf(OrderDto o) throws Exception {
        Map row = db.queryOne("SELECT shipping_cents, shipping_title FROM store_order WHERE id = ?",
                new Object[] {o.id()});
        return Rows.number(row.get("shipping_cents"));
    }

    @Test
    void checkoutOffersAndChargesTheCarriersRateWithMarkupAndHandling() throws Exception {
        Shop s = new Shop("carrier-ok", 20, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        String rate = carrierRate(s);
        String user = People.customer(accounts, "carrier-ok-buyer");
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 2));
        int calls = carrierRates.calls();
        CartDto c = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
        // 700 g from a location with no coordinates: the simulated carrier
        // takes 500 km, so Ground is 495 + 90 + 5 * 25 = 710 cents, every time.
        ShippingOptionDto ground = option(c, rate + "~GROUND");
        assertNotNull(ground, "Ground is offered");
        assertEquals(ShippingCalculator.carrierPrice(710, 1000, 100), ground.priceCents());
        assertEquals(881, ground.priceCents(), "710 + 10% (71) + 100 handling");
        assertEquals("Live - Ground", ground.name());
        assertEquals(ShippingCalculator.CARRIER, ground.kind());
        assertNotNull(option(c, rate + "~EXPRESS"), "Express is offered");
        assertNull(option(c, rate + "~OVERNIGHT"), "a service the merchant left out is not");
        assertNull(option(c, rate), "no fallback while the carrier answers");
        assertTrue(option(c, rate + "~GROUND") != null && c.shippingOptions().size() > 2,
                "the zone's own rates stay alongside");

        // The same cart and address reuse the cached quote.
        carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, rate + "~GROUND"));
        assertEquals(calls + 1, carrierRates.calls(), "one carrier call per cart and address");

        // A service that was not offered is refused at place time; the
        // client never sends an amount, the server re-prices the choice.
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        ResponseStatusException refused = assertThrows(ResponseStatusException.class,
                () -> checkouts.place(user, s.storeId, Ids.next(), new PlaceOrderDto(Shop.NY,
                    rate + "~OVERNIGHT", (String) card.get("id"), user + "@example.com", "",
                    null)));
        assertEquals(409, refused.getStatus());
        assertThrows(ResponseStatusException.class, () -> checkouts.place(user, s.storeId,
                Ids.next(), new PlaceOrderDto(Shop.NY, rate, (String) card.get("id"),
                    user + "@example.com", "", null)), "the fallback is not offered either");

        OrderDto placed = checkouts.place(user, s.storeId, Ids.next(), new PlaceOrderDto(Shop.NY,
                rate + "~GROUND", (String) card.get("id"), user + "@example.com", "", null));
        assertEquals(881, shippingOf(placed));
    }

    @Test
    void aCarrierOutageFallsBackInsteadOfBlockingCheckout() throws Exception {
        Shop s = new Shop("carrier-down", 20, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        String rate = carrierRate(s);
        String user = People.customer(accounts, "carrier-down-buyer");
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        CartDto c = carts.estimate(s.storeId, user, null, new EstimateDto(DOWN, null));
        assertNull(option(c, rate + "~GROUND"), "the carrier did not answer");
        ShippingOptionDto fallback = option(c, rate);
        assertNotNull(fallback, "the configured fallback is offered");
        assertEquals(1500, fallback.priceCents());
        assertTrue(c.shippingOptions().size() > 1, "the zone's other rates are still offered");

        Map card = Rest.addCard(mvc, user, Rest.VISA);
        assertThrows(ResponseStatusException.class, () -> checkouts.place(user, s.storeId,
                Ids.next(), new PlaceOrderDto(DOWN, rate + "~GROUND", (String) card.get("id"),
                    user + "@example.com", "", null)), "a quote the carrier never gave is refused");
        OrderDto placed = checkouts.place(user, s.storeId, Ids.next(), new PlaceOrderDto(DOWN,
                rate, (String) card.get("id"), user + "@example.com", "", null));
        assertEquals(1500, shippingOf(placed));
    }

    @Test
    void carrierPricingRoundsHalfUpAndGuardsOverflow() {
        assertEquals(1100, ShippingCalculator.carrierPrice(1000, 1000, 0));
        assertEquals(2, ShippingCalculator.carrierPrice(1, 5000, 0), "half a cent rounds up");
        assertEquals(-1, ShippingCalculator.carrierPrice(Long.MAX_VALUE / 2, 100000, 0));
        assertTrue(ShippingCalculator.offersService("", "GROUND"));
        assertTrue(ShippingCalculator.offersService("ground , Express", "EXPRESS"));
        assertTrue(!ShippingCalculator.offersService("GROUND", "OVERNIGHT"));
    }
}
