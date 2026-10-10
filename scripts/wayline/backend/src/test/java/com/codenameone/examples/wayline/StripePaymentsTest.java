/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline;

import com.codenameone.examples.wayline.pay.PaymentDeclinedException;
import com.codenameone.examples.wayline.pay.PaymentProvider;
import com.codenameone.examples.wayline.pay.StripePayments;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The Stripe provider, held to what it sends and what it makes of the
/// answers. The network is replaced by a script of canned answers, so this
/// needs no key and reaches nobody: what it proves is the requests' shape and
/// the reading of Stripe's JSON, which is all of the provider there is.
class StripePaymentsTest {
    private static final String RETURN = "https://wayline.example/pay/return";

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
        assertEquals("cus_123", stripe.createCustomer("ada@wayline.example", "Ada & Co"));
        assertEquals("POST /v1/customers", script.asked.get(0));
        String form = script.forms.get(0);
        // Escaped, name and value both: an ampersand in a name must not start
        // another field.
        has(form, "email=ada%40wayline.example");
        has(form, "name=Ada%20%26%20Co");
        has(form, "metadata%5Bwayline_user%5D=ada%40wayline.example");
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
        has(form, "success_url=https%3A%2F%2Fwayline.example%2Fpay%2Freturn%3Fresult%3Ddone");
        has(form, "cancel_url=https%3A%2F%2Fwayline.example%2Fpay%2Freturn%3Fresult%3Dcancelled");

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
    void aRideIsChargedOnceWithNobodyAtTheScreen() throws Exception {
        Script script = new Script().reply(200, "{\"id\":\"pi_1\",\"object\":\"payment_intent\","
                + "\"status\":\"succeeded\",\"amount\":1840}");
        StripePayments stripe = new StripePayments(script, RETURN);
        assertEquals("pi_1", stripe.charge("ride-77", "cus_123", "pm_1", 1840L, "USD",
                "Wayline ride"));
        assertEquals("POST /v1/payment_intents", script.asked.get(0));
        String form = script.forms.get(0);
        has(form, "amount=1840");
        has(form, "currency=usd");
        has(form, "customer=cus_123");
        has(form, "payment_method=pm_1");
        has(form, "off_session=true");
        has(form, "confirm=true");
        has(form, "description=Wayline%20ride");
        has(form, "metadata%5Bwayline_charge%5D=ride-77");
        // The key that makes a charge sent twice a charge made once.
        assertEquals("ride-77", script.keys.get(0));
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
        assertEquals("re_1", stripe.refund("pi_1", 1840L));
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
}
