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
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Primary;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.TestConfiguration;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.analytics.Analytics;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.SeriesPointDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.checkout.IdempotencyKeys;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pay.PaymentRecovery;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.StoreClock;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The operational gaps a store meets once it takes real orders: its own time
/// zone, idempotency keys that expire and refuse a changed request, card
/// holds a crash left behind, and the daily reconciliation with the
/// processor. A crash is reproduced by putting the rows in the state the
/// server would have left them in at that point.
@BackendTest
class OpsGapsTest {
    /// 2026-01-15T00:00Z, 12:00Z and 2026-07-15T12:00Z.
    private static final long JAN15 = 1768435200000L;
    private static final long JAN15_NOON = 1768478400000L;
    private static final long JUL15_NOON = 1784116800000L;
    /// 2026-01-16T03:30Z: 22:30 on the 15th in New York.
    private static final long JAN16_0330 = 1768534200000L;
    private static final String NY = "America/New_York";

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
    private IdempotencyKeys keys;
    @Autowired
    private PaymentRecovery recovery;
    @Autowired
    private Payments payments;
    @Autowired
    private StoreClock clock;
    @Autowired
    private ScriptedPayments processor;

    @TestConfiguration
    static class Processor {
        @Bean
        @Primary
        ScriptedPayments scriptedPayments() {
            return new ScriptedPayments();
        }
    }

    // ------------------------------------------------------------ time zone

    @Test
    void zonesAreCheckedByShapeAndKeepDaylightSaving() {
        assertTrue(Days.isZone("UTC"));
        assertTrue(Days.isZone(NY));
        assertTrue(Days.isZone("America/Argentina/Buenos_Aires"));
        assertFalse(Days.isZone("Mars/Olympus"));
        assertFalse(Days.isZone("America/"));
        assertFalse(Days.isZone("GMT+5"));
        assertFalse(Days.isZone("America/New York"));
        assertEquals(0, Days.offset(Days.UTC, JAN15_NOON));
        assertEquals(-5 * 3600000, Days.offset(NY, JAN15_NOON), "standard time in January");
        assertEquals(-4 * 3600000, Days.offset(NY, JUL15_NOON), "daylight saving in July");
        assertEquals("2026-01-15", Days.localIso(NY, JAN16_0330));
        assertEquals("2026-01-16", Days.localIso(Days.UTC, JAN16_0330));
        assertEquals(22, Days.localHour(NY, JAN16_0330));
        assertEquals(JAN15 + 5 * 3600000L, Days.localDayStart(NY, JAN15 / Days.MILLIS));
    }

    @Test
    void anEveningSaleLandsOnThatEveningsBar() {
        StoreOrder late = new StoreOrder();
        late.setCreatedAt(Instant.ofEpochMilli(JAN16_0330));
        late.setSubtotalCents(2500);
        List<StoreOrder> period = new ArrayList<StoreOrder>();
        period.add(late);
        long from = Days.localDayStart(NY, JAN15 / Days.MILLIS);
        List<SeriesPointDto> local = Analytics.daily(period, from, from + 2 * Days.MILLIS, NY);
        assertEquals("2026-01-15", local.get(0).label());
        assertEquals(1, local.get(0).orders());
        assertEquals(2500L, local.get(0).salesCents());
        List<SeriesPointDto> utc = Analytics.daily(period, JAN15, JAN15 + 2 * Days.MILLIS,
                Days.UTC);
        assertEquals(0, utc.get(0).orders());
        assertEquals(1, utc.get(1).orders());
        assertEquals(1, Analytics.hourly(period, NY).get(22).orders());
        assertEquals(1, Analytics.hourly(period, Days.UTC).get(3).orders());
    }

    @Test
    void aStoreStartsInUtcAndItsManagersPickAZone() throws Exception {
        Shop s = new Shop("ops-zone", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        assertEquals(Days.UTC, clock.zoneOf(s.storeId));
        mvc.perform(People.asMerchant(get("/api/merchant/stores/" + s.storeId + "/time-zone"),
                s.owner)).andExpect(jsonPath("$.timeZone").value("UTC"));
        mvc.perform(People.asMerchant(Rest.body(post("/api/merchant/stores/" + s.storeId
                + "/time-zone"), "{\"timeZone\":\"Mars/Olympus\"}"), s.owner))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asMerchant(Rest.body(post("/api/merchant/stores/" + s.storeId
                + "/time-zone"), "{\"timeZone\":\"" + NY + "\"}"), s.owner))
                .andExpect(status().isOk());
        assertEquals(NY, clock.zoneOf(s.storeId));
        String stranger = People.customer(accounts, "ops-zone-stranger");
        assertTrue(Rest.statusOf(mvc, People.asMerchant(Rest.body(post("/api/merchant/stores/"
                + s.storeId + "/time-zone"), "{\"timeZone\":\"UTC\"}"), stranger)) >= 400);
    }

    // ------------------------------------------------------ idempotency keys

    @Test
    void aKeyLivesADayAndRefusesAChangedOrARunningRequest() throws Exception {
        String id = "ops-key-" + Ids.next();
        long now = 1000000000000L;
        assertTrue(keys.begin(id, "a", now));
        assertEquals(409, refusal(id, "a", now + 1000));
        assertEquals(422, refusal(id, "b", now + 1000));
        keys.finish(id, "order-1");
        assertFalse(keys.begin(id, "a", now + 2000), "a repeat of a finished request");
        assertEquals(422, refusal(id, "b", now + 2000));
        // Past its lifetime the key is forgotten: any request may use it.
        assertTrue(keys.begin(id, "b", now + IdempotencyKeys.LIFETIME_MILLIS + 1));

        // A request that died is taken over once it has run too long.
        String dead = "ops-key-" + Ids.next();
        assertTrue(keys.begin(dead, "a", now));
        assertTrue(keys.begin(dead, "a", now + IdempotencyKeys.IN_FLIGHT_MILLIS + 1));

        // The sweep forgets expired keys.
        String old = "ops-key-" + Ids.next();
        assertTrue(keys.begin(old, "a", now));
        keys.sweep(now + IdempotencyKeys.LIFETIME_MILLIS + 1);
        assertNull(db.queryOne("SELECT client_id FROM idempotency_key WHERE client_id = ?",
                new Object[] {old}));
    }

    private int refusal(String id, String fingerprint, long now) throws Exception {
        try {
            keys.begin(id, fingerprint, now);
            return 200;
        } catch (ResponseStatusException refused) {
            return refused.getStatus();
        }
    }

    @Test
    void checkoutAnswersARepeatAndRefusesTheSameKeyForAnotherOrder() throws Exception {
        Shop s = new Shop("ops-idem", 10, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        String user = People.customer(accounts, "ops-idem-buyer");
        PlaceOrderDto first = order(s, user, "");
        String key = "ops-idem-" + Ids.next();
        OrderDto placed = checkouts.place(user, s.storeId, key, first);
        assertEquals(placed.id(), checkouts.place(user, s.storeId, key, first).id());
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        PlaceOrderDto changed = new PlaceOrderDto(first.address(), first.shippingRateId(),
                first.paymentMethodId(), first.email(), "Leave it at the door", null, 0, null);
        ResponseStatusException refused = assertThrows(ResponseStatusException.class,
                () -> checkouts.place(user, s.storeId, key, changed));
        assertEquals(422, refused.getStatus());
    }

    /// A cart of one mug for `user`, shipped Express, paid by a new card.
    private PlaceOrderDto order(Shop s, String user, String note) throws Exception {
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, 1));
        CartDto c = carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
        String rate = null;
        for (ShippingOptionDto o : c.shippingOptions()) {
            if ("Express".equals(o.name())) {
                rate = o.rateId();
            }
        }
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        return new PlaceOrderDto(Shop.NY, rate, (String) card.get("id"), user + "@example.com",
                note, null, 0, null);
    }

    // ------------------------------------------------------- crash recovery

    /// The journal row checkout writes before asking for a hold, an hour old.
    private void attempt(String orderId, String key, String user) throws Exception {
        db.execute("DELETE FROM payment_attempt WHERE order_id = ?", new Object[] {orderId});
        db.execute("INSERT INTO payment_attempt (order_id, username, idem_key, customer, token, "
                + "amount_cents, currency, status, created_at) VALUES (?, ?, ?, 'cus', "
                + "'', 1000, 'USD', ?, ?)", new Object[] {orderId, user, key,
                    PaymentRecovery.STARTED, Long.valueOf(System.currentTimeMillis() - 3600000L)});
    }

    /// The hold the processor made under `key` before the crash.
    private void heldAtProcessor(String key) throws Exception {
        processor.authorize(key, "cus", "sim_pm_ops", 1000, "USD", "Bazaar order");
    }

    /// The order as it stood before checkout's last step wrote its payment.
    private void unfinish(String orderId) throws Exception {
        db.execute("UPDATE store_order SET financial = 'PENDING' WHERE id = ?",
                new Object[] {orderId});
        db.execute("DELETE FROM tender WHERE order_id = ? AND kind = 'CARD'",
                new Object[] {orderId});
    }

    private boolean journaled(String orderId) throws Exception {
        return db.queryOne("SELECT order_id FROM payment_attempt WHERE order_id = ?",
                new Object[] {orderId}) != null;
    }

    @Test
    void aCheckoutLeavesNoJournalBehindWhenItFinishes() throws Exception {
        Shop s = new Shop("ops-journal", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        OrderDto o = s.buy(People.customer(accounts, "ops-journal-buyer"), 1);
        assertFalse(journaled(o.id()));
    }

    @Test
    void aHoldRecordedBeforeTheCrashCompletesTheOrder() throws Exception {
        processor.reset();
        Shop s = new Shop("ops-complete", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        OrderDto o = s.buy(People.customer(accounts, "ops-complete-buyer"), 1);
        // Crash after the payment row, before the order recorded it.
        unfinish(o.id());
        attempt(o.id(), "k-" + o.id(), o.username());
        recovery.recover(System.currentTimeMillis());
        Map row = db.queryOne("SELECT financial FROM store_order WHERE id = ?",
                new Object[] {o.id()});
        assertEquals("AUTHORIZED", Rows.text(row.get("financial")));
        assertNotNull(db.queryOne("SELECT id FROM tender WHERE order_id = ? AND kind = 'CARD'",
                new Object[] {o.id()}));
        assertFalse(journaled(o.id()));
        assertEquals(0, processor.released.size(), "a completed order keeps its hold");
    }

    @Test
    void aHoldTheOrderNeverRecordedIsVoidedAndTheOrderTakenBack() throws Exception {
        processor.reset();
        Shop s = new Shop("ops-void", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        OrderDto o = s.buy(People.customer(accounts, "ops-void-buyer"), 1);
        // Crash after the processor held the money, before the payment row.
        db.execute("DELETE FROM payment WHERE order_id = ?", new Object[] {o.id()});
        unfinish(o.id());
        String key = "k-void-" + o.id();
        heldAtProcessor(key);
        attempt(o.id(), key, o.username());
        recovery.recover(System.currentTimeMillis());
        assertNull(db.queryOne("SELECT id FROM store_order WHERE id = ?", new Object[] {o.id()}),
                "the order is taken back, as a declined checkout leaves it");
        assertTrue(processor.released.contains("sim_ch_" + key), "the orphaned hold is let go");
        assertFalse(journaled(o.id()));
        // The stock came back: all five can be had again.
        Shop again = s;
        String buyer = People.customer(accounts, "ops-void-second");
        assertNotNull(again.buy(buyer, 5));
    }

    @Test
    void aHoldWithNoOrderIsVoidedAndAFreshAttemptIsLeftAlone() throws Exception {
        processor.reset();
        String orphan = Ids.next();
        String key = "k-orphan-" + orphan;
        heldAtProcessor(key);
        attempt(orphan, key, "nobody");
        String fresh = Ids.next();
        attempt(fresh, "k-fresh-" + fresh, "nobody");
        db.execute("UPDATE payment_attempt SET created_at = ? WHERE order_id = ?",
                new Object[] {Long.valueOf(System.currentTimeMillis()), fresh});
        recovery.recover(System.currentTimeMillis());
        assertTrue(processor.released.contains("sim_ch_" + key));
        assertFalse(journaled(orphan));
        assertTrue(journaled(fresh), "a checkout still within its grace is not touched");
        assertFalse(processor.released.contains("sim_ch_k-fresh-" + fresh));
        db.execute("DELETE FROM payment_attempt WHERE order_id = ?", new Object[] {fresh});
    }

    @Test
    void noCardTokenIsKeptAndAnAttemptThatHeldNothingReleasesNothing() throws Exception {
        processor.reset();
        Shop s = new Shop("ops-token", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        OrderDto o = s.buy(People.customer(accounts, "ops-token-buyer"), 1);
        // Checkout writes the journal row without the card's token.
        db.execute("DELETE FROM payment_attempt WHERE order_id = ?", new Object[] {o.id()});
        String orphan = Ids.next();
        String key = "k-never-" + orphan;
        attempt(orphan, key, "nobody");
        recovery.recover(System.currentTimeMillis());
        assertFalse(journaled(orphan));
        assertFalse(processor.released.contains("sim_ch_" + key),
                "nothing was held under the key, and nothing is held now to be let go");
        assertNull(processor.findHold(key, "cus", 0), "recovery never asks for a hold again");
    }

    @Test
    void checkoutWritesNoTokenIntoTheJournal() throws Exception {
        processor.reset();
        Shop s = new Shop("ops-notoken", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        String buyer = People.customer(accounts, "ops-notoken-buyer");
        final String[] seen = new String[1];
        processor.duringAuthorize = new Runnable() {
            @Override
            public void run() {
                try {
                    Map row = db.queryOne("SELECT token FROM payment_attempt WHERE username = ?",
                            new Object[] {buyer});
                    seen[0] = row == null ? null : Rows.text(row.get("token"));
                } catch (IOException e) {
                    seen[0] = e.getMessage();
                }
            }
        };
        s.buy(buyer, 1);
        processor.duringAuthorize = null;
        assertEquals("", seen[0], "the journal row exists while the hold is asked for, tokenless");
    }

    @Test
    void aClaimLeftByAReconcilerThatDiedIsTakenOverAfterTheLease() throws Exception {
        processor.reset();
        String orphan = Ids.next();
        String key = "k-stranded-" + orphan;
        heldAtProcessor(key);
        attempt(orphan, key, "nobody");
        long now = System.currentTimeMillis();
        // A pass claimed it and the server stopped before it settled.
        db.execute("UPDATE payment_attempt SET status = ?, claimed_at = ? WHERE order_id = ?",
                new Object[] {PaymentRecovery.RECONCILING, Long.valueOf(now - 60000L), orphan});
        recovery.recover(now);
        assertTrue(journaled(orphan), "a claim within its lease is left to its pass");
        assertFalse(processor.released.contains("sim_ch_" + key));
        recovery.recover(now + PaymentRecovery.LEASE_MILLIS);
        assertFalse(journaled(orphan), "past the lease the row is taken over and settled");
        assertTrue(processor.released.contains("sim_ch_" + key));
    }

    @Test
    void aRetriedCheckoutNeverReplacesARowTheReconcilerHolds() throws Exception {
        processor.reset();
        Shop s = new Shop("ops-held", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        OrderDto o = s.buy(People.customer(accounts, "ops-held-buyer"), 1);
        db.execute("DELETE FROM payment WHERE order_id = ?", new Object[] {o.id()});
        attempt(o.id(), "k-held-" + o.id(), o.username());
        long claim = System.currentTimeMillis();
        db.execute("UPDATE payment_attempt SET status = ?, claimed_at = ? WHERE order_id = ?",
                new Object[] {PaymentRecovery.RECONCILING, Long.valueOf(claim), o.id()});
        ResponseStatusException refused = assertThrows(ResponseStatusException.class,
                () -> payments.authorize(o.id(), o.username(), null, 1000, "USD", true));
        assertEquals(409, refused.getStatus());
        Map row = db.queryOne("SELECT status, claimed_at, idem_key FROM payment_attempt "
                + "WHERE order_id = ?", new Object[] {o.id()});
        assertEquals(PaymentRecovery.RECONCILING, Rows.text(row.get("status")));
        assertEquals(claim, Rows.number(row.get("claimed_at")));
        assertEquals("k-held-" + o.id(), Rows.text(row.get("idem_key")));
        db.execute("DELETE FROM payment_attempt WHERE order_id = ?", new Object[] {o.id()});
    }

    // ------------------------------------------------------- reconciliation

    @Test
    void theDailyReportMatchesTheProcessorAndNamesWhatOnlyItHas() throws Exception {
        processor.reset();
        Shop s = new Shop("ops-recon", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        s.buy(People.customer(accounts, "ops-recon-buyer"), 1);
        String stray = processor.authorize("ops-stray-" + Ids.next(), "cus", "sim_pm_ops", 700L,
                "USD", "Lost");
        PaymentRecovery.DayReport r = recovery.reconcileDay(System.currentTimeMillis());
        assertTrue(r.listed);
        assertTrue(r.matched >= 1);
        assertTrue(r.providerOnly >= 1);
        assertTrue(r.details.indexOf("processor only: " + stray) >= 0);
        Map kept = db.queryOne("SELECT provider_only FROM payment_reconciliation WHERE "
                + "report_day = ?", new Object[] {r.day});
        assertEquals((long) r.providerOnly, Rows.number(kept.get("provider_only")));
    }
}
