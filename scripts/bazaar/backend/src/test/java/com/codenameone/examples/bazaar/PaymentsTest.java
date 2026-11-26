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
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.account.DemoAccounts;
import com.codenameone.examples.bazaar.api.ReceiptDto;
import com.codenameone.examples.bazaar.domain.Payment;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.repository.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Paying: the cards an account keeps, and the money of an order -- held at
/// checkout, captured, voided, refunded in parts. Against the simulated
/// processor, which is what a server with no key configured runs. Orders are
/// bare ids here: payments know nothing else of them.
@BackendTest
class PaymentsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Payments payments;
    @Autowired
    private DataSource db;
    @Autowired
    private PaymentRepository paymentRows;

    private MockRequestBuilder admin(MockRequestBuilder request) {
        return People.asAdmin(request, DemoAccounts.ADMIN);
    }

    private long setupsOf(String username) throws Exception {
        Map row = db.queryOne("SELECT COUNT(*) AS n FROM payment_setup WHERE username = ?",
                new Object[] {username});
        return Rows.number(row.get("n"));
    }

    @Test
    void openSetupsAreCappedAndTheFinishedAndAbandonedAreSwept() throws Exception {
        String customer = People.customer(accounts, "setup-looper");
        // Five open at once is as many as an account may have; each is a
        // session made at the processor.
        String first = null;
        for (int iter = 0; iter < 5; iter++) {
            Map setup = Rest.json(mvc, People.asCustomer(post("/api/payments/methods/setup"),
                    customer));
            first = first == null ? (String) setup.get("id") : first;
        }
        mvc.perform(People.asCustomer(post("/api/payments/methods/setup"), customer))
                .andExpect(status().is(429));
        // Finishing one makes room for another.
        mvc.perform(People.asCustomer(Rest.body(post("/api/payments/methods/setup/" + first
                        + "/complete"), Rest.card(Rest.VISA)), customer))
                .andExpect(status().isOk());
        String last = (String) Rest.json(mvc, People.asCustomer(
                post("/api/payments/methods/setup"), customer)).get("id");
        assertEquals(6L, setupsOf(customer));
        // So does an hour going by: abandoned setups do not count.
        db.execute("UPDATE payment_setup SET created_at = ? WHERE username = ? AND id <> ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 90L * 60L * 1000L),
                    customer, last});
        Rest.json(mvc, People.asCustomer(post("/api/payments/methods/setup"), customer));

        // The sweep deletes the finished one, and an abandoned one once it is
        // two hours old; younger ones, and the open ones, stay.
        payments.sweepSetups();
        assertEquals(6L, setupsOf(customer));
        db.execute("UPDATE payment_setup SET created_at = ? WHERE username = ? AND created_at < ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 3L * 60L * 60L * 1000L),
                    customer, Long.valueOf(System.currentTimeMillis() - 60L * 60L * 1000L)});
        payments.sweepSetups();
        assertEquals(2L, setupsOf(customer));
        mvc.perform(People.asCustomer(Rest.body(post("/api/payments/methods/setup/" + last
                        + "/complete"), Rest.card(Rest.MASTERCARD)), customer))
                .andExpect(status().isOk());
    }

    @Test
    void cardsAreSavedChosenAndRemoved() throws Exception {
        String customer = People.customer(accounts, "card-holder");
        String other = People.customer(accounts, "card-other");
        mvc.perform(People.asCustomer(get("/api/payments/config"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("simulated"))
                .andExpect(jsonPath("$.currency").value("USD"));
        // Cash, always, and the default until there is a card.
        mvc.perform(People.asCustomer(get("/api/payments/methods"), customer))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("cash"))
                .andExpect(jsonPath("$[0].kind").value("cash"))
                .andExpect(jsonPath("$[0].isDefault").value(true));

        // A card that is refused is not saved, and says why; the same setup
        // then takes a card that is good.
        Map setup = Rest.json(mvc, People.asCustomer(post("/api/payments/methods/setup"), customer));
        assertEquals(Boolean.FALSE, setup.get("hosted"));
        String complete = "/api/payments/methods/setup/" + setup.get("id") + "/complete";
        mvc.perform(People.asCustomer(Rest.body(post(complete), Rest.card(Rest.DECLINED)), customer))
                .andExpect(status().is(402));
        mvc.perform(People.asCustomer(Rest.body(post(complete), Rest.card("4242424242424241")),
                        customer))
                .andExpect(status().is(402));
        // Somebody else cannot finish it for them.
        mvc.perform(People.asCustomer(Rest.body(post(complete), Rest.card(Rest.VISA)), other))
                .andExpect(status().isNotFound());
        mvc.perform(People.asCustomer(get("/api/payments/methods"), customer))
                .andExpect(jsonPath("$.length()").value(1));
        Map visa = Rest.json(mvc, People.asCustomer(Rest.body(post(complete),
                Rest.card(Rest.VISA)), customer));
        assertEquals("card", visa.get("kind"));
        assertEquals("Visa", visa.get("brand"));
        assertEquals("4242", visa.get("last4"));
        assertEquals("Visa 4242", visa.get("label"));
        assertEquals(12L, Rest.number(visa, "expMonth"));
        // The first card saved is the one to pay with.
        assertEquals(Boolean.TRUE, visa.get("isDefault"));
        // A setup is used once.
        mvc.perform(People.asCustomer(Rest.body(post(complete), Rest.card(Rest.VISA)), customer))
                .andExpect(status().isNotFound());

        Map second = Rest.addCard(mvc, customer, Rest.MASTERCARD);
        assertEquals("Mastercard", second.get("brand"));
        assertEquals(Boolean.FALSE, second.get("isDefault"));
        String visaId = (String) visa.get("id");
        String secondId = (String) second.get("id");
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + secondId + "/default"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
        mvc.perform(People.asCustomer(get("/api/payments/methods"), customer))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(visaId))
                .andExpect(jsonPath("$[0].isDefault").value(false))
                .andExpect(jsonPath("$[1].isDefault").value(true))
                .andExpect(jsonPath("$[2].id").value("cash"))
                .andExpect(jsonPath("$[2].isDefault").value(false));

        // A card is its owner's: nobody else sees it, chooses it or removes it.
        mvc.perform(People.asCustomer(get("/api/payments/methods"), other))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + visaId + "/default"), other))
                .andExpect(status().isNotFound());
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + visaId + "/remove"), other))
                .andExpect(status().isNotFound());
        try {
            payments.authorize(Ids.next(), other, visaId, 1000L, "USD");
            throw new AssertionError("someone else's card was accepted");
        } catch (ResponseStatusException refused) {
            assertEquals(400, refused.getStatus());
        }

        // Removing the default leaves cash as the default; cash stays.
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + secondId + "/remove"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secondId));
        mvc.perform(People.asCustomer(get("/api/payments/methods"), customer))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].id").value("cash"))
                .andExpect(jsonPath("$[1].isDefault").value(true));
        mvc.perform(People.asCustomer(post("/api/payments/methods/cash/remove"), customer))
                .andExpect(status().isBadRequest());
        mvc.perform(People.asCustomer(post("/api/payments/methods/cash/default"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void theCardLimitHoldsForSetupsStartedWhileThereWasRoom() throws Exception {
        String customer = People.customer(accounts, "card-collector");
        for (int iter = 0; iter < 7; iter++) {
            Rest.addCard(mvc, customer, Rest.VISA);
        }
        // Two setups started with room for one card.
        Map first = Rest.json(mvc, People.asCustomer(post("/api/payments/methods/setup"), customer));
        Map second = Rest.json(mvc, People.asCustomer(post("/api/payments/methods/setup"), customer));
        mvc.perform(People.asCustomer(Rest.body(post("/api/payments/methods/setup/"
                        + first.get("id") + "/complete"), Rest.card(Rest.MASTERCARD)), customer))
                .andExpect(status().isOk());
        mvc.perform(People.asCustomer(Rest.body(post("/api/payments/methods/setup/"
                        + second.get("id") + "/complete"), Rest.card(Rest.MASTERCARD)), customer))
                .andExpect(status().isConflict());
        // Cash and eight cards.
        mvc.perform(People.asCustomer(get("/api/payments/methods"), customer))
                .andExpect(jsonPath("$.length()").value(9));
    }

    @Test
    void anOrderIsHeldCapturedAndRefundedInParts() throws Exception {
        String customer = People.customer(accounts, "order-payer");
        String visa = (String) Rest.addCard(mvc, customer, Rest.VISA).get("id");
        String order = Ids.next();
        String hold = payments.authorize(order, customer, visa, 5000L, "USD");
        assertTrue(hold.length() > 0);
        // Asked again, the same hold, and no second payment.
        assertEquals(hold, payments.authorize(order, customer, visa, 5000L, "USD"));
        // A card that is holding money cannot be removed.
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + visa + "/remove"), customer))
                .andExpect(status().isConflict());
        assertEquals(Payments.AUTHORIZED, Rest.json(mvc, People.asCustomer(
                get("/api/payments/receipts/" + order), customer)).get("status"));

        assertEquals(Payments.PAID, payments.capture(order));
        // Capturing twice takes nothing more.
        assertEquals(Payments.PAID, payments.capture(order));
        // A captured payment cannot be voided.
        assertEquals(Payments.PAID, payments.voidOrder(order));

        ReceiptDto part = payments.refund(order, 2000L, "one item returned");
        assertEquals(Payments.PARTIALLY_REFUNDED, part.status());
        assertEquals(2000L, part.refunded());
        try {
            payments.refund(order, 4000L, "too much");
            throw new AssertionError("more than was left was refunded");
        } catch (ResponseStatusException refused) {
            assertEquals(400, refused.getStatus());
        }
        ReceiptDto rest = payments.refund(order, 0L, "the rest");
        assertEquals(Payments.REFUNDED, rest.status());
        assertEquals(5000L, rest.refunded());

        // The receipt is the payer's and an admin's, nobody else's.
        String other = People.customer(accounts, "order-snoop");
        mvc.perform(People.asCustomer(get("/api/payments/receipts/" + order), other))
                .andExpect(status().isNotFound());
        List mine = Rest.list(mvc, People.asCustomer(get("/api/payments/receipts"), customer));
        assertEquals(1, mine.size());
        mvc.perform(admin(get("/api/admin/payments"))).andExpect(status().isOk());
        // Once the hold is gone the card can go too.
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + visa + "/remove"), customer))
                .andExpect(status().isOk());
    }

    @Test
    void aCancelledOrderLetsItsHoldGo() throws Exception {
        String customer = People.customer(accounts, "order-canceller");
        String visa = (String) Rest.addCard(mvc, customer, Rest.VISA).get("id");
        String order = Ids.next();
        payments.authorize(order, customer, visa, 1200L, "USD");
        assertEquals(Payments.VOIDED, payments.voidOrder(order));
        assertEquals(Payments.VOIDED, payments.capture(order));
        try {
            payments.refund(order, 0L, "nothing was taken");
            throw new AssertionError("a voided payment was refunded");
        } catch (ResponseStatusException refused) {
            assertEquals(409, refused.getStatus());
        }
        // Cash holds nothing and is taken at the counter.
        String cash = Ids.next();
        assertEquals(Payments.CASH, payments.authorize(cash, customer, Payments.CASH, 800L, "USD"));
        assertEquals(Payments.PAID, payments.capture(cash));
    }

    @Test
    void aCardRefusedWhenChargedFailsThePaymentAndFreesTheCard() throws Exception {
        String customer = People.customer(accounts, "order-declined");
        String failing = (String) Rest.addCard(mvc, customer, Rest.FAILS_WHEN_CHARGED).get("id");
        String order = Ids.next();
        payments.authorize(order, customer, failing, 1500L, "USD");
        assertEquals(Payments.FAILED, payments.capture(order));
        mvc.perform(People.asCustomer(post("/api/payments/methods/" + failing + "/remove"), customer))
                .andExpect(status().isOk());
    }

    @Test
    void theHostedSetupEndsOnAPageThatSaysSo() throws Exception {
        String done = mvc.perform(get("/pay/return?result=done")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(done.indexOf("Card added") > 0, done);
        String cancelled = mvc.perform(get("/pay/return?result=%3Cscript%3E"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        // Nothing from the address is written into the page.
        assertTrue(cancelled.indexOf("No card was added") > 0, cancelled);
        assertEquals(-1, cancelled.indexOf("<script"));
    }

    @Test
    void aSeededHoldIsCapturedAndRefundedWithoutTheProcessor() throws Exception {
        // DemoStores seeds holds under Payments.SEEDED. With Stripe configured
        // a call about such a reference fails, so none may reach the provider.
        String order = Ids.next();
        String ref = Payments.SEEDED + order;
        assertFalse(Payments.atProcessor(ref));
        assertTrue(Payments.atProcessor("pi_123"));
        Payment row = new Payment();
        row.setOrderId(order);
        row.setUsername("seeded@example.com");
        row.setMethodId("card");
        row.setMethodLabel("Visa ending 4242");
        row.setAmountCents(2500L);
        row.setCurrency("USD");
        row.setStatus(Payments.AUTHORIZED);
        row.setProviderRef(ref);
        row.setCreatedAt(java.time.Instant.now());
        paymentRows.insert(row);
        assertEquals(Payments.PAID, payments.capture(order));
        ReceiptDto receipt = payments.refund(order, 0L, "seeded");
        assertTrue(receipt != null);
        assertEquals(Payments.REFUNDED, paymentRows.findById(order).getStatus());
    }
}
