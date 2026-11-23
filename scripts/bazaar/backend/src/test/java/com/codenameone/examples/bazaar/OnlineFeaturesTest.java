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

import static org.junit.jupiter.api.Assertions.assertNull;
import com.codenameone.examples.bazaar.api.FraudRuleDto;
import com.codenameone.examples.bazaar.risk.Fraud;
import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.CheckoutFieldDto;
import com.codenameone.examples.bazaar.api.CheckoutOptionsDto;
import com.codenameone.examples.bazaar.api.CheckoutSettingsDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.FieldAnswerDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.api.TipPresetDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.CheckoutSettings;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.checkout.ExpressCheckouts;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pay.PaymentProvider;
import com.codenameone.examples.bazaar.pay.PaymentTypes;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.pay.SimulatedPayments;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Express wallets, payment methods that finish later, tips and checkout
/// fields, all against the simulated processor.
@BackendTest
class OnlineFeaturesTest {
    @Autowired
    private com.codenameone.examples.bazaar.risk.Fraud fraud;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private StoreRepository storeRows;
    @Autowired
    private Inventory inventory;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private Orders orders;
    @Autowired
    private Payments payments;
    @Autowired
    private CheckoutSettings settings;
    @Autowired
    private ExpressCheckouts express;
    @Autowired
    private com.codenameone.examples.bazaar.ops.Reports reports;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;

    private static final AddressDto NY = Shop.NY;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 20, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private SimulatedPayments sim() {
        return (SimulatedPayments) payments.provider();
    }

    private static int status(Throwable t) {
        return t instanceof ResponseStatusException ? ((ResponseStatusException) t).getStatus()
                : -1;
    }

    private String rate(Shop s, String user, String name) throws Exception {
        CartDto c = carts.estimate(s.storeId, user, null, new EstimateDto(NY, null));
        for (ShippingOptionDto o : c.shippingOptions()) {
            if (name.equals(o.name())) {
                return o.rateId();
            }
        }
        throw new AssertionError("no " + name + " rate");
    }

    private CheckoutSettingsDto save(Shop s, List<String> methods, boolean tips,
            List<CheckoutFieldDto> fields) throws Exception {
        return settings.save(s.owner, s.storeId, new CheckoutSettingsDto(methods, tips,
                Arrays.asList(new TipPresetDto(1500, 0), new TipPresetDto(0, 500)), fields));
    }

    private OrderDto place(Shop s, String user, int qty, int tip, List<FieldAnswerDto> answers,
            Checkouts.Via via) throws Exception {
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, qty));
        String card = via == null ? (String) Rest.addCard(mvc, user, Rest.VISA).get("id") : null;
        return checkouts.place(user, s.storeId, Ids.next(), new PlaceOrderDto(NY,
                rate(s, user, "Express"), card, user + "@example.com", "", null, tip, answers),
                null, null, null, via);
    }


    // ================================================== manual payment methods

    @Test
    void cashOnDeliveryIsPlacedUnpaidAndTheMerchantMarksItPaid() throws Exception {
        Shop s = shop("of-cod");
        save(s, Arrays.asList("cod", "bank_transfer"), false,
                Collections.<CheckoutFieldDto>emptyList());
        CheckoutOptionsDto us = settings.options(s.storeId, "simulated", "USD", "US", 1000, "");
        assertEquals(3, us.methods().size());
        Checkouts.Via via = Checkouts.Via.method("bank_transfer");
        OrderDto o = place(s, People.customer(accounts, "of-cod-a"), 2, 0, null, via);
        assertEquals(Orders.PENDING, o.financial());
        assertNull(via.intent);
        assertEquals(18, inventory.available(s.variantId, s.locationId));
        OrderDto paid = orders.markPaid(s.owner, s.storeId, o.id());
        assertEquals(Orders.PAID, paid.financial());
        assertEquals(paid.totalCents(), paid.tenders().get(0).amountCents());
    }

    @Test
    void aManualMethodTheStoreDidNotEnableIsRefused() throws Exception {
        Shop s = shop("of-cod-off");
        save(s, Arrays.asList("klarna"), false, Collections.<CheckoutFieldDto>emptyList());
        try {
            place(s, People.customer(accounts, "of-cod-off-a"), 1, 0, null,
                    Checkouts.Via.method("cod"));
            throw new AssertionError("placed");
        } catch (ResponseStatusException e) {
            assertEquals(400, e.getStatus());
        }
    }

    // ======================================================= fraud rules

    @Test
    void aTotalRuleHoldsAndACancelRuleCancels() throws Exception {
        Shop s = shop("of-rule");
        List<FraudRuleDto> list = fraud.addRule(s.owner, s.storeId,
                new FraudRuleDto(null, Fraud.TOTAL_OVER, 1, Fraud.HOLD));
        assertEquals(1, list.size());
        OrderDto held = place(s, People.customer(accounts, "of-rule-a"), 1, 0, null, null);
        assertEquals(Fraud.HELD, fraud.detail(s.owner, s.storeId, held.id()).status());
        fraud.removeRule(s.owner, s.storeId, list.get(0).id());
        fraud.addRule(s.owner, s.storeId,
                new FraudRuleDto(null, Fraud.ORDERS_PER_HOUR, 2, Fraud.CANCEL));
        String b = People.customer(accounts, "of-rule-b");
        OrderDto first = place(s, b, 1, 0, null, null);
        assertEquals(Fraud.CLEAR, fraud.detail(s.owner, s.storeId, first.id()).status());
        OrderDto second = place(s, b, 1, 0, null, null);
        assertEquals(Fraud.CANCELLED, fraud.detail(s.owner, s.storeId, second.id()).status());
        assertEquals(Orders.CANCELLED, orders.detail(s.owner, s.storeId, second.id()).status());
    }

    @Test
    void rulesAreCheckedAndManagedByStaffOnly() throws Exception {
        Shop s = shop("of-rule-chk");
        try {
            fraud.addRule(s.owner, s.storeId, new FraudRuleDto(null, "NOPE", 1, Fraud.HOLD));
            throw new AssertionError("added");
        } catch (ResponseStatusException e) {
            assertEquals(400, e.getStatus());
        }
        try {
            fraud.addRule(People.customer(accounts, "of-rule-x"), s.storeId,
                    new FraudRuleDto(null, Fraud.COUNTRY_MISMATCH, 0, Fraud.HOLD));
            throw new AssertionError("added");
        } catch (ResponseStatusException e) {
            assertTrue(e.getStatus() == 403 || e.getStatus() == 404);
        }
        assertEquals(1, fraud.addRule(s.owner, s.storeId,
                new FraudRuleDto(null, Fraud.COUNTRY_MISMATCH, 0, Fraud.CANCEL)).size());
    }
}
