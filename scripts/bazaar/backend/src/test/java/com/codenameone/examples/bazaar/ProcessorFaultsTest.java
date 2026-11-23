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
import com.codenameone.examples.bazaar.pay.Payments;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/// Paying, against a processor that fails when a test says so: what is left
/// behind when a request to it fails part way, alone or overlapping another.
/// The service is called directly, on the test's thread, so the request a
/// test lines up inside another runs exactly where it is put.
@BackendTest
class ProcessorFaultsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Payments payments;
    @Autowired
    private ScriptedPayments processor;
    @Autowired
    private DataSource db;

    @TestConfiguration
    static class Processor {
        @Bean
        @Primary
        ScriptedPayments scriptedPayments() {
            return new ScriptedPayments();
        }
    }

    /// The status `call` was refused with, or 200 when it was not.
    private static int refusal(Runnable call) {
        try {
            call.run();
            return 200;
        } catch (ResponseStatusException refused) {
            return refused.getStatus();
        }
    }

    private Runnable refund(final String orderId) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    payments.refund(orderId, 0L, "Overlapping");
                } catch (IOException err) {
                    throw new IllegalStateException(err);
                }
            }
        };
    }

    private Runnable remove(final String customer, final String card) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    payments.remove(customer, card);
                } catch (IOException err) {
                    throw new IllegalStateException(err);
                }
            }
        };
    }

    @Test
    void aRemovedCardIsDetachedAtTheProcessorFirst() throws Exception {
        processor.reset();
        String customer = People.customer(accounts, "card-detacher");
        String card = (String) Rest.addCard(mvc, customer, Rest.VISA).get("id");
        String token = (String) db.queryOne("SELECT token FROM payment_method WHERE id = ?",
                new Object[] {card}).get("token");

        // A processor that cannot be reached keeps the card: deleted here
        // alone, it would stay chargeable there with nothing pointing at it.
        processor.detachFails = true;
        assertEquals(502, refusal(remove(customer, card)));
        mvc.perform(People.asCustomer(get("/api/payments/methods"), customer))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(card));
        assertEquals(0, processor.detached.size());

        processor.detachFails = false;
        assertEquals(200, refusal(remove(customer, card)));
        assertEquals(1, processor.detached.size());
        assertEquals(token, processor.detached.get(0));
        mvc.perform(People.asCustomer(get("/api/payments/methods"), customer))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void aCardTheLimitRefusesIsDetachedAtTheProcessor() throws Exception {
        processor.reset();
        String customer = People.customer(accounts, "card-overflow");
        for (int iter = 0; iter < 7; iter++) {
            Rest.addCard(mvc, customer, Rest.VISA);
        }
        // Two setups started with room for one card, and a third.
        Map first = Rest.json(mvc, People.asCustomer(post("/api/payments/methods/setup"), customer));
        Map second = Rest.json(mvc, People.asCustomer(post("/api/payments/methods/setup"), customer));
        Map third = Rest.json(mvc, People.asCustomer(post("/api/payments/methods/setup"), customer));
        mvc.perform(People.asCustomer(Rest.body(post("/api/payments/methods/setup/"
                        + first.get("id") + "/complete"), Rest.card(Rest.MASTERCARD)), customer))
                .andExpect(status().isOk());
        assertEquals(0, processor.detached.size());

        // The processor attached the second card before the limit refused it;
        // with no row here, it is detached there, and no card that was kept.
        mvc.perform(People.asCustomer(Rest.body(post("/api/payments/methods/setup/"
                        + second.get("id") + "/complete"), Rest.card(Rest.MASTERCARD)), customer))
                .andExpect(status().isConflict());
        assertEquals(1, processor.detached.size());
        Map kept = db.queryOne("SELECT COUNT(*) AS n FROM payment_method WHERE token = ?",
                new Object[] {processor.detached.get(0)});
        assertEquals(0L, Rows.number(kept.get("n")));

        // A detach that fails is logged, and the customer still hears the limit.
        processor.detachFails = true;
        mvc.perform(People.asCustomer(Rest.body(post("/api/payments/methods/setup/"
                        + third.get("id") + "/complete"), Rest.card(Rest.MASTERCARD)), customer))
                .andExpect(status().isConflict());
        assertEquals(1, processor.detached.size());
        mvc.perform(People.asCustomer(get("/api/payments/methods"), customer))
                .andExpect(jsonPath("$.length()").value(9));
        processor.reset();
    }

    @Test
    void aRefundThatFailsGivesItsClaimBackAndRefusesOneThatOverlapsIt() throws Exception {
        processor.reset();
        String customer = People.customer(accounts, "overlap-customer");
        String card = (String) Rest.addCard(mvc, customer, Rest.VISA).get("id");
        final String order = Ids.next();
        payments.authorize(order, customer, card, 3000L, "USD");
        assertEquals(Payments.PAID, payments.capture(order));

        // While a refund is at the processor a second one is refused: the
        // first has claimed the payment. The first then fails without an
        // answer, and gives the claim back.
        processor.duringRefund = new Runnable() {
            @Override
            public void run() {
                assertEquals(409, refusal(refund(order)));
            }
        };
        processor.refundFailures.add(Boolean.TRUE);
        assertEquals(502, refusal(refund(order)));
        assertEquals(Payments.PAID, payments.receipt(customer, false, order).status());

        // Asked again, it goes through.
        assertEquals(200, refusal(refund(order)));
        assertEquals(Payments.REFUNDED, payments.receipt(customer, false, order).status());
        processor.reset();
    }

    @Test
    void aRefundAskedAgainAfterALostAnswerSendsTheSameKey() throws Exception {
        processor.reset();
        String customer = People.customer(accounts, "refund-retrier");
        String card = (String) Rest.addCard(mvc, customer, Rest.VISA).get("id");
        final String order = Ids.next();
        payments.authorize(order, customer, card, 3000L, "USD");
        assertEquals(Payments.PAID, payments.capture(order));

        // The processor made the refund but the answer was lost: asked again,
        // it must see the same key, or it would return the money twice.
        processor.refundFailures.add(Boolean.TRUE);
        assertEquals(502, refusal(partial(order, 1000L)));
        assertEquals(200, refusal(partial(order, 1000L)));
        assertEquals(2, processor.keys.size());
        assertEquals(processor.keys.get(0), processor.keys.get(1));

        // A second partial refund of the same amount is a new refund.
        assertEquals(200, refusal(partial(order, 1000L)));
        assertEquals(3, processor.keys.size());
        assertNotEquals(processor.keys.get(1), processor.keys.get(2));
        assertEquals(2000L, payments.receipt(customer, false, order).refunded());
        processor.reset();
    }

    @Test
    void aRefundLeftClaimedIsFinishedUnderItsOwnKey() throws Exception {
        processor.reset();
        String customer = People.customer(accounts, "refund-stranded");
        String card = (String) Rest.addCard(mvc, customer, Rest.VISA).get("id");
        final String order = Ids.next();
        payments.authorize(order, customer, card, 3000L, "USD");
        assertEquals(Payments.PAID, payments.capture(order));
        String ref = (String) db.queryOne("SELECT provider_ref FROM payment WHERE order_id = ?",
                new Object[] {order}).get("provider_ref");

        // The server stopped after claiming a refund of 1200: the row stays
        // REFUNDING until the sweep asks again with the same key.
        db.execute("UPDATE payment SET status = 'REFUNDING', pending_refund_cents = 1200, "
                + "refund_claimed_at = 1 WHERE order_id = ?", new Object[] {order});
        payments.resumeRefunds();
        assertEquals(Payments.PARTIALLY_REFUNDED, payments.receipt(customer, false, order).status());
        assertEquals(1200L, payments.receipt(customer, false, order).refunded());
        assertEquals(Payments.refundKey(ref, 0L, 1200L), processor.keys.get(0));

        // One the processor refuses goes back to where it was.
        db.execute("UPDATE payment SET status = 'REFUNDING', pending_refund_cents = 500, "
                + "refund_claimed_at = 1 WHERE order_id = ?", new Object[] {order});
        processor.refundFailures.add(Boolean.TRUE);
        payments.resumeRefunds();
        assertEquals(Payments.PARTIALLY_REFUNDED, payments.receipt(customer, false, order).status());
        assertEquals(1200L, payments.receipt(customer, false, order).refunded());
        processor.reset();
    }

    private Runnable partial(final String orderId, final long cents) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    payments.refund(orderId, cents, "Part");
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }
}
