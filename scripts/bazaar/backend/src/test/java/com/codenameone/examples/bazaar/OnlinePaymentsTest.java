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
class OnlinePaymentsTest {
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

    private long paymentCents(String orderId) throws Exception {
        return Rows.number(db.queryOne("SELECT amount_cents FROM payment WHERE order_id = ?",
                new Object[] {orderId}).get("amount_cents"));
    }

    // ================================================================= tips

    @Test
    void aTipIsChargedBesideTheTotalAndNeverTaxedOrDiscounted() throws Exception {
        Shop s = shop("op-tip");
        save(s, Collections.<String>emptyList(), true, Collections.<CheckoutFieldDto>emptyList());
        String plain = People.customer(accounts, "op-tip-a");
        String tipper = People.customer(accounts, "op-tip-b");
        OrderDto without = place(s, plain, 2, 0, null, null);
        OrderDto with = place(s, tipper, 2, 1, null, null);
        // 15% of $20.00 of goods.
        assertEquals(300, with.tipCents());
        assertEquals(0, without.tipCents());
        assertEquals(without.taxCents(), with.taxCents());
        assertEquals(without.totalCents(), with.totalCents());
        assertEquals(with.totalCents() + 300, paymentCents(with.id()));
        OrderDto fixed = place(s, People.customer(accounts, "op-tip-c"), 1, 2, null, null);
        assertEquals(500, fixed.tipCents());
        try {
            place(s, People.customer(accounts, "op-tip-d"), 1, 3, null, null);
            fail("a tip the store does not offer");
        } catch (ResponseStatusException e) {
            assertEquals(400, e.getStatus());
        }
    }

    @Test
    void tipsAreOffMeansOnlyNoTip() throws Exception {
        Shop s = shop("op-notip");
        try {
            place(s, People.customer(accounts, "op-notip-a"), 1, 1, null, null);
            fail("tips are off");
        } catch (ResponseStatusException e) {
            assertEquals(400, e.getStatus());
        }
    }

    @Test
    void theTipIsInTheFinanceReport() throws Exception {
        Shop s = shop("op-tiprep");
        save(s, Collections.<String>emptyList(), true, Collections.<CheckoutFieldDto>emptyList());
        place(s, People.customer(accounts, "op-tiprep-a"), 2, 1, null, null);
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        String tomorrow = java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1).toString();
        com.codenameone.examples.bazaar.api.ReportDto r = reports.run(s.owner, s.storeId,
                new com.codenameone.examples.bazaar.api.ReportSpecDto("Tips", "channel", "",
                    "tips", "", "", today, tomorrow));
        assertEquals(300, r.total());
    }

    // =============================================================== fields

    @Test
    void checkoutFieldsAreCheckedAndKeptOnTheOrder() throws Exception {
        Shop s = shop("op-fld");
        CheckoutSettingsDto saved = save(s, Collections.<String>emptyList(), false, Arrays.asList(
                new CheckoutFieldDto(null, "Gift message", CheckoutSettings.TEXT, "", false),
                new CheckoutFieldDto(null, "Leave at door", CheckoutSettings.CHECKBOX, "", false),
                new CheckoutFieldDto(null, "Delivery window", CheckoutSettings.SELECT,
                    "Morning\nEvening", true)));
        assertEquals(3, saved.fields().size());
        String gift = saved.fields().get(0).id();
        String door = saved.fields().get(1).id();
        String window = saved.fields().get(2).id();
        try {
            place(s, People.customer(accounts, "op-fld-a"), 1, 0, null, null);
            fail("a required field left out");
        } catch (ResponseStatusException e) {
            assertEquals(400, e.getStatus());
        }
        try {
            place(s, People.customer(accounts, "op-fld-b"), 1, 0, Arrays.asList(
                    new FieldAnswerDto(window, "Midnight")), null);
            fail("not one of the choices");
        } catch (ResponseStatusException e) {
            assertEquals(400, e.getStatus());
        }
        OrderDto o = place(s, People.customer(accounts, "op-fld-c"), 1, 0, Arrays.asList(
                new FieldAnswerDto(gift, "Happy birthday"), new FieldAnswerDto(door, "on"),
                new FieldAnswerDto(window, "Evening")), null);
        OrderDto again = orders.detail(s.owner, s.storeId, o.id());
        assertEquals(3, again.fields().size());
        assertEquals("Gift message", again.fields().get(0).label());
        assertEquals("Happy birthday", again.fields().get(0).value());
        assertEquals("yes", again.fields().get(1).value());
        assertEquals("Evening", again.fields().get(2).value());
    }

    @Test
    void settingsAreTheOwnersAndValidated() throws Exception {
        Shop s = shop("op-set");
        try {
            save(s, Arrays.asList("bitcoin"), false, Collections.<CheckoutFieldDto>emptyList());
            fail("unknown method");
        } catch (ResponseStatusException e) {
            assertEquals(400, e.getStatus());
        }
        String stranger = People.customer(accounts, "op-set-x");
        try {
            settings.read(stranger, s.storeId);
            fail("not their store");
        } catch (ResponseStatusException e) {
            assertTrue(e.getStatus() == 403 || e.getStatus() == 404);
        }
        save(s, Arrays.asList("klarna", "ideal"), false, Collections.<CheckoutFieldDto>emptyList());
        CheckoutOptionsDto us = settings.options(s.storeId, "simulated", "USD", "US", 1000,
                "/s/op-set/checkout");
        assertEquals(PaymentTypes.CARD, us.methods().get(0).type());
        assertEquals(2, us.methods().size());
        assertEquals("klarna", us.methods().get(1).type());
        CheckoutOptionsDto nl = settings.options(s.storeId, "simulated", "EUR", "NL", 1000, "");
        assertEquals(3, nl.methods().size());
    }

    // ======================================================= async methods

    private MockResponse webhook(String body) throws Exception {
        return mvc.perform(Rest.body(post("/api/payments/webhooks/payments"), body)
                .header("X-Bazaar-Signature", "sha256=" + Tokens.sign("test-dispute-secret",
                    body))).andReturn().getResponse();
    }

    private String event(String orderId) {
        return "{\"id\":\"evt-" + orderId + "\",\"type\":\"payment.updated\",\"orderId\":\""
                + orderId + "\"}";
    }

    @Test
    void aLaterMethodLeavesTheOrderPendingUntilTheWebhookSaysPaid() throws Exception {
        Shop s = shop("op-bnpl");
        save(s, Arrays.asList("klarna"), false, Collections.<CheckoutFieldDto>emptyList());
        Checkouts.Via via = Checkouts.Via.method("klarna");
        OrderDto o = place(s, People.customer(accounts, "op-bnpl-a"), 2, 0, null, via);
        assertEquals(Orders.PENDING, o.financial());
        assertNotNull(via.intent);
        assertTrue(via.intent.clientSecret.length() > 0);
        assertEquals(18, inventory.available(s.variantId, s.locationId));
        // Unsigned is refused; signed but not yet settled changes nothing.
        assertEquals(401, mvc.perform(Rest.body(post("/api/payments/webhooks/payments"),
                event(o.id()))).andReturn().getResponse().getStatus());
        assertEquals(200, webhook(event(o.id())).getStatus());
        assertEquals(Orders.PENDING, orders.detail(s.owner, s.storeId, o.id()).financial());
        sim().complete(via.intent.reference, true);
        sim().settle(via.intent.reference, true);
        assertEquals(200, webhook(event(o.id())).getStatus());
        assertEquals(200, webhook(event(o.id())).getStatus());
        OrderDto paid = orders.detail(s.owner, s.storeId, o.id());
        assertEquals(Orders.PAID, paid.financial());
        assertEquals(1, paid.tenders().size());
        assertEquals(paid.totalCents(), paid.tenders().get(0).amountCents());
    }

    @Test
    void aFailedLaterPaymentCancelsTheOrderAndReleasesTheStock() throws Exception {
        Shop s = shop("op-fail");
        save(s, Arrays.asList("klarna"), false, Collections.<CheckoutFieldDto>emptyList());
        Checkouts.Via via = Checkouts.Via.method("klarna");
        OrderDto o = place(s, People.customer(accounts, "op-fail-a"), 3, 0, null, via);
        assertEquals(17, inventory.available(s.variantId, s.locationId));
        sim().complete(via.intent.reference, false);
        assertEquals(200, webhook(event(o.id())).getStatus());
        OrderDto gone = orders.detail(s.owner, s.storeId, o.id());
        assertEquals(Orders.CANCELLED, gone.status());
        assertEquals(20, inventory.available(s.variantId, s.locationId));
    }

    @Test
    void aMethodTheStoreDoesNotOfferIsRefused() throws Exception {
        Shop s = shop("op-off");
        save(s, Arrays.asList("ideal"), false, Collections.<CheckoutFieldDto>emptyList());
        try {
            // iDEAL is euros in the Netherlands; this store sells dollars in New York.
            place(s, People.customer(accounts, "op-off-a"), 1, 0, null,
                    Checkouts.Via.method("ideal"));
            fail("not offered here");
        } catch (ResponseStatusException e) {
            assertEquals(400, e.getStatus());
        }
        assertEquals(20, inventory.available(s.variantId, s.locationId));
    }

    // ============================================================== express

    private Cart cartOf(Shop s, String user) throws Exception {
        return carts.cart(s.storeId, user, null);
    }

    @Test
    void anExpressCheckoutReQuotesAsTheSheetChanges() throws Exception {
        Shop s = shop("op-exp");
        String user = People.customer(accounts, "op-exp-a");
        Store store = storeRows.findById(s.storeId);
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 2));
        ExpressCheckouts.Session started = express.start(user, store, cartOf(s, user));
        assertEquals(2000, started.totalCents);
        assertTrue(started.clientSecret.length() > 0);
        AddressDto partial = new AddressDto("", "", "", "Albany", "NY", "12207", "US", "", 0, 0);
        ExpressCheckouts.Session there = express.quote(user, started.id, cartOf(s, user),
                partial, null);
        assertFalse(there.options.isEmpty());
        assertTrue(there.taxCents > 0);
        assertEquals(there.subtotalCents - there.discountCents + there.shippingCents
                + there.taxCents, there.totalCents);
        String fast = rate(s, user, "Express");
        ExpressCheckouts.Session faster = express.quote(user, started.id, cartOf(s, user),
                partial, fast);
        assertEquals(fast, faster.rateId);
        assertTrue(faster.shippingCents >= there.shippingCents);
        String ref = Rows.text(db.queryOne("SELECT provider_ref FROM express_checkout WHERE id = ?",
                new Object[] {started.id}).get("provider_ref"));
        PaymentProvider.Intent intent = sim().intent(ref);
        assertEquals(faster.totalCents, intent.amountCents);
        // Another shopper cannot look at it.
        String other = People.customer(accounts, "op-exp-b");
        try {
            express.quote(other, started.id, cartOf(s, other), partial, null);
            fail("someone else's");
        } catch (ResponseStatusException e) {
            assertEquals(404, e.getStatus());
        }
    }

    @Test
    void confirmingTwicePlacesOneOrderOnTheHold() throws Exception {
        Shop s = shop("op-conf");
        String user = People.customer(accounts, "op-conf-a");
        Store store = storeRows.findById(s.storeId);
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        ExpressCheckouts.Session started = express.start(user, store, cartOf(s, user));
        Cart cart = cartOf(s, user);
        ExpressCheckouts.Session quoted = express.quote(user, started.id, cart, NY, null);
        String ref = Rows.text(db.queryOne("SELECT provider_ref FROM express_checkout WHERE id = ?",
                new Object[] {started.id}).get("provider_ref"));
        try {
            express.confirm(user, started.id, cart, NY, user + "@example.com", quoted.rateId);
            fail("the wallet has not approved yet");
        } catch (ResponseStatusException e) {
            assertEquals(402, e.getStatus());
        }
        sim().complete(ref, true);
        ExpressCheckouts.Session first = express.confirm(user, started.id, cart, NY,
                user + "@example.com", quoted.rateId);
        ExpressCheckouts.Session second = express.confirm(user, started.id, cart, NY,
                user + "@example.com", quoted.rateId);
        assertNotNull(first.order);
        assertEquals(first.order.id(), second.order.id());
        assertEquals(Orders.AUTHORIZED, first.order.financial());
        assertEquals(quoted.totalCents, first.order.totalCents());
        assertEquals(1, Rows.number(db.queryOne("SELECT COUNT(*) AS n FROM store_order "
                + "WHERE store_id = ?", new Object[] {s.storeId}).get("n")));
        Map pay = db.queryOne("SELECT provider_ref, status FROM payment WHERE order_id = ?",
                new Object[] {first.order.id()});
        assertEquals(ref, Rows.text(pay.get("provider_ref")));
        assertEquals(19, inventory.available(s.variantId, s.locationId));
    }

    @Test
    void aHoldThatNoLongerMatchesTheCartPlacesNothing() throws Exception {
        Shop s = shop("op-drift");
        String user = People.customer(accounts, "op-drift-a");
        Store store = storeRows.findById(s.storeId);
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        ExpressCheckouts.Session started = express.start(user, store, cartOf(s, user));
        ExpressCheckouts.Session quoted = express.quote(user, started.id, cartOf(s, user), NY,
                null);
        String ref = Rows.text(db.queryOne("SELECT provider_ref FROM express_checkout WHERE id = ?",
                new Object[] {started.id}).get("provider_ref"));
        sim().complete(ref, true);
        // The cart grew in another tab after the sheet was priced.
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        try {
            express.confirm(user, started.id, cartOf(s, user), NY, user + "@example.com",
                    quoted.rateId);
            fail("the hold is for one mug");
        } catch (ResponseStatusException e) {
            assertEquals(409, e.getStatus());
        }
        assertEquals(20, inventory.available(s.variantId, s.locationId));
    }

    @Test
    void theDevelopmentWalletGoesThroughTheSameFlow() throws Exception {
        Shop s = shop("op-sim");
        String user = People.customer(accounts, "op-sim-a");
        Store store = storeRows.findById(s.storeId);
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        ExpressCheckouts.Session started = express.start(user, store, cartOf(s, user));
        ExpressCheckouts.Session done = express.simulate(user, started.id, cartOf(s, user),
                user + "@example.com");
        assertNotNull(done.order);
        assertEquals(Orders.AUTHORIZED, done.order.financial());
    }

    @Test
    void expressIsNotOfferedWhenAFieldIsRequired() throws Exception {
        Shop s = shop("op-req");
        save(s, Collections.<String>emptyList(), false, Arrays.asList(
                new CheckoutFieldDto(null, "PO number", CheckoutSettings.TEXT, "", true)));
        assertFalse(express.offered(storeRows.findById(s.storeId)));
        List<String> none = new ArrayList<String>();
        assertTrue(none.isEmpty());
    }
}
