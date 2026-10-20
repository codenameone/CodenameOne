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
package com.codenameone.examples.savor;

import com.codenameone.examples.savor.pay.PaymentDeclinedException;
import com.codenameone.examples.savor.pay.PaymentProvider;
import com.codenameone.examples.savor.pay.StripePayments;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The Stripe provider, held to what it sends and what it makes of the
/// answers. The network is replaced by a script of canned answers, so this
/// needs no key and reaches nobody: what it proves is the requests' shape and
/// the reading of Stripe's JSON, which is all of the provider there is.
class StripePaymentsTest {
    private static final String RETURN = "https://savor.example/pay/return";

    /// Answers each request with the next canned reply and keeps what was
    /// asked.
    private static final class Script implements StripePayments.Transport {
        final List<String> asked = new ArrayList<String>();
        final List<String> forms = new ArrayList<String>();
        final List<String> keys = new ArrayList<String>();
        private final List<StripePayments.Reply> replies = new ArrayList<StripePayments.Reply>();

        Script reply(int status, String body) {
            replies.add(new StripePayments.Reply(status, body));
            return this;
        }

        @Override
        public StripePayments.Reply send(String method, String path, String form,
                String idempotencyKey) throws IOException {
            asked.add(method + " " + path);
            forms.add(form);
            keys.add(idempotencyKey);
            if (replies.isEmpty()) {
                throw new IOException("Nothing more was expected to be asked");
            }
            return replies.remove(0);
        }
    }

    private static void has(String form, String pair) {
        assertTrue(("&" + form + "&").indexOf("&" + pair + "&") >= 0, pair + " in " + form);
    }

    @Test
    void aCustomerIsMadeWithTheAccountsName() throws Exception {
        Script script = new Script().reply(200, "{\"id\":\"cus_123\",\"object\":\"customer\"}");
        StripePayments stripe = new StripePayments(script, RETURN);
        assertEquals("stripe", stripe.name());
        assertEquals("cus_123", stripe.createCustomer("ada@savor.example", "Ada & Co"));
        assertEquals("POST /v1/customers", script.asked.get(0));
        String form = script.forms.get(0);
        // Escaped, name and value both: an ampersand in a name must not start
        // another field.
        has(form, "email=ada%40savor.example");
        has(form, "name=Ada+%26+Co");
        has(form, "metadata%5Bsavor_user%5D=ada%40savor.example");
        // The same request made twice makes one customer: the same key, which
        // does not spell the address out.
        String key = script.keys.get(0);
        assertTrue(key.startsWith("customer-") && key.indexOf("ada") < 0, key);
        script.reply(200, "{\"id\":\"cus_123\",\"object\":\"customer\"}");
        stripe.createCustomer("ada@savor.example", "Ada & Co");
        assertEquals(key, script.keys.get(1));
        script.reply(200, "{\"id\":\"cus_124\",\"object\":\"customer\"}");
        stripe.createCustomer("ada@savor.example", "Ada");
        assertTrue(!key.equals(script.keys.get(2)), "another name is another request");
    }

    @Test
    void aCustomerIsDeletedAndOneAlreadyGoneCountsAsDeleted() throws Exception {
        Script script = new Script().reply(200, "{\"id\":\"cus_123\",\"deleted\":true}")
                .reply(404, "{\"error\":{\"type\":\"invalid_request_error\","
                        + "\"code\":\"resource_missing\"}}")
                .reply(500, "{}");
        StripePayments stripe = new StripePayments(script, RETURN);
        stripe.forgetCustomer("cus_123");
        assertEquals("DELETE /v1/customers/cus_123", script.asked.get(0));
        assertEquals(null, script.forms.get(0));
        // Asked again after an answer that was lost.
        stripe.forgetCustomer("cus_123");
        // Anything else is not a deletion, and the closure it is part of fails.
        try {
            stripe.forgetCustomer("cus_123");
            throw new AssertionError("A processor that failed must not read as deleted");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().indexOf("500") >= 0, expected.getMessage());
        }
    }

    @Test
    void aRemovedCardIsDetachedAndOneAlreadyDetachedCountsAsDetached() throws Exception {
        Script script = new Script()
                .reply(200, "{\"id\":\"pm_1\",\"object\":\"payment_method\",\"customer\":null}")
                // Gone with its customer.
                .reply(404, "{\"error\":{\"type\":\"invalid_request_error\","
                        + "\"code\":\"resource_missing\"}}")
                // Asked again after an answer that was lost: Stripe refuses to
                // detach a card attached to nobody, and the card says so.
                .reply(400, "{\"error\":{\"type\":\"invalid_request_error\"}}")
                .reply(200, "{\"id\":\"pm_1\",\"object\":\"payment_method\",\"customer\":null}")
                // Refused while the card is still this customer's: not detached.
                .reply(400, "{\"error\":{\"type\":\"invalid_request_error\"}}")
                .reply(200, "{\"id\":\"pm_1\",\"object\":\"payment_method\","
                        + "\"customer\":\"cus_123\"}")
                .reply(500, "{}")
                .reply(500, "{}");
        StripePayments stripe = new StripePayments(script, RETURN);
        stripe.detachCard("cus_123", "pm_1");
        assertEquals("POST /v1/payment_methods/pm_1/detach", script.asked.get(0));
        stripe.detachCard("cus_123", "pm_1");
        stripe.detachCard("cus_123", "pm_1");
        assertEquals("GET /v1/payment_methods/pm_1", script.asked.get(3));
        for (int iter = 0; iter < 2; iter++) {
            try {
                stripe.detachCard("cus_123", "pm_1");
                throw new AssertionError("A card still attached must not read as detached");
            } catch (IOException expected) {
                assertTrue(expected.getMessage().indexOf("detaching") >= 0,
                        expected.getMessage());
            }
        }
    }

    @Test
    void aCardIsAddedOnAHostedPageAndReadBack() throws Exception {
        Script script = new Script()
                .reply(200, "{\"id\":\"cs_test_1\",\"object\":\"checkout.session\","
                        + "\"url\":\"https://checkout.stripe.com/c/pay/cs_test_1\"}")
                .reply(200, "{\"id\":\"cs_test_1\",\"status\":\"complete\","
                        + "\"customer\":\"cus_123\",\"setup_intent\":{\"id\":\"seti_1\","
                        + "\"status\":\"succeeded\",\"payment_method\":{\"id\":\"pm_1\","
                        + "\"type\":\"card\",\"card\":{\"brand\":\"visa\",\"last4\":\"4242\","
                        + "\"exp_month\":8,\"exp_year\":2031}}}}");
        StripePayments stripe = new StripePayments(script, RETURN);
        PaymentProvider.Setup setup = stripe.startCardSetup("cus_123", "setup9");
        assertTrue(setup.hosted);
        assertEquals("cs_test_1", setup.reference);
        assertEquals("https://checkout.stripe.com/c/pay/cs_test_1", setup.url);
        assertEquals("POST /v1/checkout/sessions", script.asked.get(0));
        String form = script.forms.get(0);
        has(form, "mode=setup");
        has(form, "customer=cus_123");
        has(form, "payment_method_types%5B0%5D=card");
        has(form, "client_reference_id=setup9");
        has(form, "success_url=https%3A%2F%2Fsavor.example%2Fpay%2Freturn%3Fresult%3Ddone");
        has(form, "cancel_url=https%3A%2F%2Fsavor.example%2Fpay%2Freturn%3Fresult%3Dcancelled");

        // No card is sent: it was typed on the processor's page.
        PaymentProvider.Card card = stripe.finishCardSetup("cus_123", "cs_test_1", null);
        assertEquals("GET /v1/checkout/sessions/cs_test_1"
                + "?expand%5B%5D=setup_intent.payment_method", script.asked.get(1));
        assertEquals(null, script.forms.get(1));
        assertEquals("pm_1", card.token);
        assertEquals("Visa", card.brand);
        assertEquals("4242", card.last4);
        assertEquals(8, card.expMonth);
        assertEquals(2031, card.expYear);
    }

    @Test
    void aSetupNotFinishedOrSomebodyElsesSavesNoCard() throws Exception {
        // The user closed the page: the session is still open, with no card.
        Script script = new Script().reply(200, "{\"id\":\"cs_test_2\",\"status\":\"open\","
                + "\"customer\":\"cus_123\",\"setup_intent\":null}");
        try {
            new StripePayments(script, RETURN).finishCardSetup("cus_123", "cs_test_2", null);
            fail("An unfinished setup saved a card");
        } catch (PaymentDeclinedException expected) {
            assertTrue(expected.getMessage().indexOf("not added") > 0);
        }
        script = new Script().reply(200, "{\"id\":\"cs_test_3\",\"status\":\"complete\","
                + "\"customer\":\"cus_999\",\"setup_intent\":{\"status\":\"succeeded\","
                + "\"payment_method\":{\"id\":\"pm_9\",\"card\":{\"brand\":\"visa\","
                + "\"last4\":\"1111\",\"exp_month\":1,\"exp_year\":2030}}}}");
        try {
            new StripePayments(script, RETURN).finishCardSetup("cus_123", "cs_test_3", null);
            fail("Another customer's session saved a card");
        } catch (PaymentDeclinedException wrong) {
            fail("That is a fault, not a refusal");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().indexOf("another customer") > 0);
        }
        // A page to open is the whole point of starting one.
        script = new Script().reply(200, "{\"id\":\"cs_test_4\"}");
        try {
            new StripePayments(script, RETURN).startCardSetup("cus_123", "setup1");
            fail("A setup with no page was started");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().indexOf("no page") > 0);
        }
    }

    @Test
    void aDeliveryIsChargedOnceWithNobodyAtTheScreen() throws Exception {
        Script script = new Script().reply(200, "{\"id\":\"pi_1\",\"object\":\"payment_intent\","
                + "\"status\":\"succeeded\",\"amount\":1840}");
        StripePayments stripe = new StripePayments(script, RETURN);
        assertEquals("pi_1", stripe.charge("delivery-77", "cus_123", "pm_1", 1840L, "USD",
                "Savor delivery"));
        assertEquals("POST /v1/payment_intents", script.asked.get(0));
        String form = script.forms.get(0);
        has(form, "amount=1840");
        has(form, "currency=usd");
        has(form, "customer=cus_123");
        has(form, "payment_method=pm_1");
        has(form, "off_session=true");
        has(form, "confirm=true");
        has(form, "description=Savor+delivery");
        has(form, "metadata%5Bsavor_charge%5D=delivery-77");
        // The key that makes a charge sent twice a charge made once.
        assertEquals("delivery-77", script.keys.get(0));
    }

    @Test
    void aChargeTheBankWantsApprovedOrRefusesIsADecline() throws Exception {
        Script script = new Script().reply(200, "{\"id\":\"pi_2\","
                + "\"status\":\"requires_action\"}");
        try {
            new StripePayments(script, RETURN).charge("k1", "cus_123", "pm_1", 500L, "USD", "x");
            fail("A charge awaiting approval was taken as made");
        } catch (PaymentDeclinedException expected) {
            assertTrue(expected.getMessage().indexOf("approval") > 0);
        }
        // What Stripe answers for a card that is refused: 402, and words
        // written for the cardholder.
        script = new Script().reply(402, "{\"error\":{\"type\":\"card_error\","
                + "\"code\":\"card_declined\",\"decline_code\":\"insufficient_funds\","
                + "\"message\":\"Your card has insufficient funds.\"}}");
        try {
            new StripePayments(script, RETURN).charge("k2", "cus_123", "pm_1", 500L, "USD", "x");
            fail("A declined charge was taken as made");
        } catch (PaymentDeclinedException expected) {
            assertEquals("Your card has insufficient funds.", expected.getMessage());
        }
        // Anything else is a fault, reported by status and code and without
        // the processor's message, which can quote the request.
        script = new Script().reply(401, "{\"error\":{\"type\":\"invalid_request_error\","
                + "\"code\":\"api_key_invalid\",\"message\":\"Invalid API Key provided: sk_x\"}}");
        try {
            new StripePayments(script, RETURN).charge("k3", "cus_123", "pm_1", 500L, "USD", "x");
            fail("A request the processor rejected was taken as made");
        } catch (PaymentDeclinedException wrong) {
            fail("That is a fault, not a refusal");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().indexOf("401") > 0, expected.getMessage());
            assertTrue(expected.getMessage().indexOf("api_key_invalid") > 0);
            assertEquals(-1, expected.getMessage().indexOf("sk_x"));
        }
        script = new Script().reply(502, "<html>Bad gateway</html>");
        try {
            new StripePayments(script, RETURN).charge("k4", "cus_123", "pm_1", 500L, "USD", "x");
            fail("An answer that is not JSON was taken as a charge");
        } catch (PaymentDeclinedException wrong) {
            fail("That is a fault, not a refusal");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().indexOf("502") > 0, expected.getMessage());
        }
    }

    @Test
    void aRefundReturnsTheChargeOnce() throws Exception {
        Script script = new Script()
                .reply(200, "{\"id\":\"re_1\",\"object\":\"refund\",\"status\":\"succeeded\"}")
                .reply(200, "{\"id\":\"re_2\",\"object\":\"refund\",\"status\":\"failed\"}");
        StripePayments stripe = new StripePayments(script, RETURN);
        PaymentProvider.Refund made = stripe.refund("pi_1", 1840L);
        assertEquals("re_1", made.reference);
        assertFalse(made.pending);
        assertEquals("POST /v1/refunds", script.asked.get(0));
        has(script.forms.get(0), "payment_intent=pi_1");
        has(script.forms.get(0), "amount=1840");
        assertEquals("refund-pi_1", script.keys.get(0));
        try {
            stripe.refund("pi_1", 1840L);
            fail("A refund that failed was taken as made");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().indexOf("failed") > 0);
        }
    }

    @Test
    void aPendingRefundIsAskedAboutUntilItEnds() throws Exception {
        Script script = new Script()
                .reply(200, "{\"id\":\"re_3\",\"object\":\"refund\",\"status\":\"pending\"}")
                .reply(200, "{\"id\":\"re_4\",\"object\":\"refund\","
                        + "\"status\":\"requires_action\"}")
                .reply(200, "{\"id\":\"re_3\",\"object\":\"refund\",\"status\":\"pending\"}")
                .reply(200, "{\"id\":\"re_3\",\"object\":\"refund\",\"status\":\"succeeded\"}")
                .reply(200, "{\"id\":\"re_4\",\"object\":\"refund\",\"status\":\"canceled\"}")
                .reply(200, "{\"id\":\"re_5\",\"object\":\"refund\",\"status\":\"failed\"}");
        StripePayments stripe = new StripePayments(script, RETURN);
        // Taken, but not yet money back: neither is a refund to record as made.
        PaymentProvider.Refund pending = stripe.refund("pi_3", 900L);
        assertEquals("re_3", pending.reference);
        assertTrue(pending.pending);
        assertTrue(stripe.refund("pi_4", 900L).pending);
        assertEquals(PaymentProvider.REFUND_PENDING, stripe.refundStatus("re_3"));
        assertEquals("GET /v1/refunds/re_3", script.asked.get(2));
        assertEquals(PaymentProvider.REFUND_SUCCEEDED, stripe.refundStatus("re_3"));
        // Canceled returned no more money than failed did.
        assertEquals(PaymentProvider.REFUND_FAILED, stripe.refundStatus("re_4"));
        assertEquals(PaymentProvider.REFUND_FAILED, stripe.refundStatus("re_5"));
    }
}
