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
package com.codenameone.examples.bazaar.pay;

import com.codename1.backend.Json;
import java.io.IOException;
import java.util.Map;

/// Stripe Terminal's server-driven integration over Stripe's REST API, with
/// no SDK. A reader is registered to a Terminal location
/// (`bazaar.payments.stripe.terminal-location`), a `card_present` payment
/// intent is opened with automatic capture, and
/// `POST /v1/terminal/readers/{reader}/process_payment_intent` puts it on the
/// reader. What happened is read back from the intent, and from the reader's
/// `action` while the intent still waits: a failed action carries the reason.
///
/// The secret key and the transport are [StripePayments]'s, so the same
/// canned replies drive the tests.
public class StripeTerminal implements CardTerminal {
    private final StripePayments.Transport transport;
    private final String location;

    /// @param location the Terminal location (`tml_...`) new readers are registered to
    public StripeTerminal(StripePayments.Transport transport, String location) {
        this.transport = transport;
        this.location = location == null ? "" : location;
    }

    /// The real network, with the account's secret key.
    public static StripeTerminal live(String secret, String location) {
        return new StripeTerminal(new StripePayments.WebTransport(secret), location);
    }

    @Override
    public String name() {
        return "stripe";
    }

    @Override
    public Reader register(String registrationCode, String label) throws IOException {
        if (location.length() == 0) {
            throw new IOException("bazaar.payments.stripe.terminal-location is not set");
        }
        Map reader = ask("POST", "/v1/terminal/readers", StripePayments.form(
                "registration_code", registrationCode, "label", label, "location", location),
                null);
        return new Reader(id(reader), text(reader, "serial_number"));
    }

    @Override
    public void remove(String readerRef) throws IOException {
        StripePayments.Reply reply = transport.send("DELETE", "/v1/terminal/readers/"
                + readerRef, null, null);
        if (reply.status != 404 && (reply.status < 200 || reply.status >= 300)) {
            throw new IOException("The payment processor answered " + reply.status);
        }
    }

    @Override
    public String createIntent(String key, long amountCents, String currency,
            String description) throws IOException {
        Map intent = ask("POST", "/v1/payment_intents", StripePayments.form(
                "amount", String.valueOf(amountCents),
                "currency", StripePayments.lowerAscii(currency),
                "payment_method_types[]", "card_present",
                "capture_method", "automatic",
                "description", description,
                "metadata[bazaar_card_payment]", key), "terminal-" + key);
        return id(intent);
    }

    @Override
    public void process(String readerRef, String intentRef) throws IOException {
        ask("POST", "/v1/terminal/readers/" + readerRef + "/process_payment_intent",
                StripePayments.form("payment_intent", intentRef), null);
    }

    @Override
    public Outcome status(String readerRef, String intentRef) throws IOException {
        Map intent = ask("GET", "/v1/payment_intents/" + intentRef
                + "?expand%5B%5D=latest_charge", null, null);
        String state = text(intent, "status");
        if ("succeeded".equals(state)) {
            return new Outcome(SUCCEEDED, "", cardLabel(intent));
        }
        if ("canceled".equals(state)) {
            return new Outcome(CANCELED, "The payment was cancelled", "");
        }
        // Still waiting for a card: the reader's action says whether it is
        // in progress or already failed (a decline leaves the intent open).
        Map reader = ask("GET", "/v1/terminal/readers/" + readerRef, null, null);
        Object action = reader.get("action");
        if (action instanceof Map) {
            Map a = (Map) action;
            Object processing = a.get("process_payment_intent");
            boolean ours = processing instanceof Map
                    && intentRef.equals(text((Map) processing, "payment_intent"));
            if (ours && "failed".equals(text(a, "status"))) {
                String why = text(a, "failure_message");
                return new Outcome(FAILED, why.length() > 0 ? why : "The card was not accepted",
                        "");
            }
            if (ours) {
                return new Outcome(PENDING, "", "");
            }
        }
        Object error = intent.get("last_payment_error");
        if (error instanceof Map) {
            String why = text((Map) error, "message");
            return new Outcome(FAILED, why.length() > 0 ? why : "The card was not accepted", "");
        }
        return new Outcome(PENDING, "", "");
    }

    @Override
    public void cancel(String readerRef, String intentRef) throws IOException {
        StripePayments.Reply reply = transport.send("POST", "/v1/terminal/readers/" + readerRef
                + "/cancel_action", "", null);
        if (reply.status >= 500) {
            throw new IOException("The payment processor answered " + reply.status);
        }
        // Left open, the intent could still be paid on the next tap.
        transport.send("POST", "/v1/payment_intents/" + intentRef + "/cancel", "",
                "terminal-cancel-" + intentRef);
    }

    /// `Visa 4242` from the charge's `card_present` details, when expanded.
    private static String cardLabel(Map intent) {
        Object charge = intent.get("latest_charge");
        if (!(charge instanceof Map)) {
            return "Card";
        }
        Object details = ((Map) charge).get("payment_method_details");
        Object present = details instanceof Map ? ((Map) details).get("card_present") : null;
        if (!(present instanceof Map)) {
            return "Card";
        }
        String brand = text((Map) present, "brand");
        String last4 = text((Map) present, "last4");
        String name = brand.length() == 0 ? "Card"
                : Character.toUpperCase(brand.charAt(0)) + brand.substring(1);
        return last4.length() == 0 ? name : name + " " + last4;
    }

    private Map ask(String method, String path, String form, String key) throws IOException {
        StripePayments.Reply reply = transport.send(method, path, form, key);
        Map body;
        try {
            body = Json.parseObject(reply.body == null ? "" : reply.body);
        } catch (IOException unreadable) {
            throw new IOException("The payment processor answered " + reply.status
                    + " with something that is not JSON");
        }
        if (reply.status >= 200 && reply.status < 300) {
            return body;
        }
        Object error = body.get("error");
        if (error instanceof Map) {
            String message = text((Map) error, "message");
            throw new IOException(message.length() > 0 ? message
                    : "The payment processor answered " + reply.status);
        }
        throw new IOException("The payment processor answered " + reply.status);
    }

    private static String id(Map object) throws IOException {
        String id = text(object, "id");
        if (id.length() == 0) {
            throw new IOException("The payment processor's answer has no id");
        }
        return id;
    }

    private static String text(Map object, String key) {
        Object value = object.get(key);
        return value instanceof String ? (String) value : "";
    }
}
