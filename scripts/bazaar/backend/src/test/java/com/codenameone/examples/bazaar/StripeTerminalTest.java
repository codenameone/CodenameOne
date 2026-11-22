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

import com.codenameone.examples.bazaar.pay.CardTerminal;
import com.codenameone.examples.bazaar.pay.StripePayments;
import com.codenameone.examples.bazaar.pay.StripeTerminal;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The Stripe Terminal calls of a server-driven integration, against canned
/// answers: what is sent, in what order, and how each answer is read.
class StripeTerminalTest {
    /// Answers each request with the first canned reply whose key its
    /// method and path start with, and records what was asked.
    private static final class Script implements StripePayments.Transport {
        final List<String[]> answers = new ArrayList<String[]>();
        final List<String> asked = new ArrayList<String>();

        Script on(String methodAndPath, int status, String body) {
            answers.add(new String[] {methodAndPath, Integer.toString(status), body});
            return this;
        }

        @Override
        public StripePayments.Reply send(String method, String path, String form,
                String idempotencyKey) throws IOException {
            String key = method + " " + path;
            asked.add(key + (form == null ? "" : " " + form)
                    + (idempotencyKey == null || idempotencyKey.length() == 0 ? ""
                        : " key=" + idempotencyKey));
            for (int iter = 0; iter < answers.size(); iter++) {
                String[] a = answers.get(iter);
                if (key.startsWith(a[0])) {
                    return new StripePayments.Reply(Integer.parseInt(a[1]), a[2]);
                }
            }
            return new StripePayments.Reply(404, "{\"error\":{\"message\":\"No such thing\"}}");
        }
    }

    @Test
    void pairingSendsTheCodeLabelAndLocation() throws Exception {
        Script s = new Script().on("POST /v1/terminal/readers", 200,
                "{\"id\":\"tmr_123\",\"serial_number\":\"WSC-1\"}");
        CardTerminal.Reader r = new StripeTerminal(s, "tml_9").register("puppies-plug",
                "Front desk");
        assertEquals("tmr_123", r.reference);
        assertEquals("WSC-1", r.serial);
        String sent = s.asked.get(0);
        assertTrue(sent.contains("registration_code=puppies-plug"), sent);
        assertTrue(sent.contains("label=Front+desk") || sent.contains("label=Front%20desk"),
                sent);
        assertTrue(sent.contains("location=tml_9"), sent);
    }

    @Test
    void aRefusedCodeIsAnError() {
        Script s = new Script().on("POST /v1/terminal/readers", 400,
                "{\"error\":{\"message\":\"Invalid registration code\"}}");
        IOException e = assertThrows(IOException.class,
                () -> new StripeTerminal(s, "tml_9").register("wrong", "Front"));
        assertTrue(e.getMessage().contains("Invalid registration code"), e.getMessage());
    }

    @Test
    void aPaymentIsACardPresentIntentHandedToTheReader() throws Exception {
        Script s = new Script()
                .on("POST /v1/payment_intents", 200, "{\"id\":\"pi_1\",\"status\":"
                    + "\"requires_payment_method\"}")
                .on("POST /v1/terminal/readers/tmr_1/process_payment_intent", 200,
                    "{\"id\":\"tmr_1\",\"action\":{\"status\":\"in_progress\"}}");
        StripeTerminal t = new StripeTerminal(s, "tml_9");
        assertEquals("pi_1", t.createIntent("cp42", 1040, "USD", "Sale at Front"));
        t.process("tmr_1", "pi_1");
        String intent = s.asked.get(0);
        assertTrue(intent.contains("amount=1040"), intent);
        assertTrue(intent.contains("currency=usd"), intent);
        assertTrue(intent.contains("card_present"), intent);
        assertTrue(intent.contains("key=terminal-cp42"), intent);
        assertTrue(s.asked.get(1).contains("payment_intent=pi_1"), s.asked.get(1));
    }

    @Test
    void theStatusReadsTheIntentThenTheReader() throws Exception {
        Script paid = new Script().on("GET /v1/payment_intents/pi_1", 200,
                "{\"id\":\"pi_1\",\"status\":\"succeeded\",\"latest_charge\":{"
                + "\"payment_method_details\":{\"card_present\":{\"brand\":\"visa\","
                + "\"last4\":\"4242\"}}}}");
        CardTerminal.Outcome ok = new StripeTerminal(paid, "tml_9").status("tmr_1", "pi_1");
        assertEquals(CardTerminal.SUCCEEDED, ok.status);
        assertEquals("Visa 4242", ok.cardLabel);

        Script declined = new Script()
                .on("GET /v1/payment_intents/pi_1", 200, "{\"id\":\"pi_1\",\"status\":"
                    + "\"requires_payment_method\"}")
                .on("GET /v1/terminal/readers/tmr_1", 200, "{\"id\":\"tmr_1\",\"action\":{"
                    + "\"status\":\"failed\",\"failure_message\":\"Card declined\","
                    + "\"process_payment_intent\":{\"payment_intent\":\"pi_1\"}}}");
        CardTerminal.Outcome no = new StripeTerminal(declined, "tml_9").status("tmr_1", "pi_1");
        assertEquals(CardTerminal.FAILED, no.status);
        assertEquals("Card declined", no.message);

        Script waiting = new Script()
                .on("GET /v1/payment_intents/pi_1", 200, "{\"id\":\"pi_1\",\"status\":"
                    + "\"requires_payment_method\"}")
                .on("GET /v1/terminal/readers/tmr_1", 200, "{\"id\":\"tmr_1\",\"action\":{"
                    + "\"status\":\"in_progress\",\"process_payment_intent\":{"
                    + "\"payment_intent\":\"pi_1\"}}}");
        assertEquals(CardTerminal.PENDING, new StripeTerminal(waiting, "tml_9")
                .status("tmr_1", "pi_1").status);
    }

    @Test
    void cancellingClearsTheReaderAndTheIntent() throws Exception {
        Script s = new Script()
                .on("POST /v1/terminal/readers/tmr_1/cancel_action", 200, "{}")
                .on("POST /v1/payment_intents/pi_1/cancel", 200, "{\"status\":\"canceled\"}");
        new StripeTerminal(s, "tml_9").cancel("tmr_1", "pi_1");
        assertEquals(2, s.asked.size());
        assertTrue(s.asked.get(0).startsWith("POST /v1/terminal/readers/tmr_1/cancel_action"));
        assertTrue(s.asked.get(1).startsWith("POST /v1/payment_intents/pi_1/cancel"));
    }
}
