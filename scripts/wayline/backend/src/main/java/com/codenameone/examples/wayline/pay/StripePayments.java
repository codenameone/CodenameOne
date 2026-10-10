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
package com.codenameone.examples.wayline.pay;

import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codenameone.examples.wayline.api.CardDto;
import com.codenameone.examples.wayline.geo.UrlText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Payments by Stripe, over its REST API. No SDK: five requests, form encoded,
/// with the account's secret key as a bearer token.
///
/// - **A customer** is made the first time a rider adds a card.
/// - **A card is added on Stripe's own page.** [#startCardSetup] opens a
///   Checkout Session in `setup` mode and hands back its address; the app
///   opens it in the browser. The card is typed there, so its number never
///   reaches the app or this server, and neither falls under the card
///   industry's rules for systems that handle one.
/// - **The setup is read back** when the app says the user returned: the
///   session's SetupIntent names the PaymentMethod Stripe saved, and its brand
///   and last four digits are all this server keeps beside its id.
/// - **A ride is charged** with a PaymentIntent that is `off_session` and
///   confirmed in the same request, because the rider is in a car and not at a
///   payment form.
/// - **A refund** returns a charge to the card.
///
/// The secret key is a setting of the server (`wayline.payments.stripe.secret`)
/// and is used for the `Authorization` header and nothing else: it is never
/// logged and never part of an error.
///
/// Everything Stripe-specific is in [#form] strings and the reading of the
/// answers. The network is behind [Transport], which is what lets the tests
/// hold both to canned answers without a key.
public class StripePayments implements PaymentProvider {
    private static final String API = "https://api.stripe.com";

    private final Transport transport;
    private final String returnUrl;

    /// @param secret the account's secret key
    /// @param returnUrl the address of this server's `/pay/return` page, which
    ///     Stripe sends the browser to when the user is done
    public StripePayments(String secret, String returnUrl) {
        this(new WebTransport(secret), returnUrl);
    }

    public StripePayments(Transport transport, String returnUrl) {
        this.transport = transport;
        this.returnUrl = returnUrl;
    }

    @Override
    public String name() {
        return "stripe";
    }

    @Override
    public String createCustomer(String username, String displayName) throws IOException {
        Map made = ask("POST", "/v1/customers", form("email", username, "name", displayName,
                "metadata[wayline_user]", username), "");
        return id(made);
    }

    @Override
    public Setup startCardSetup(String customer, String setupId) throws IOException {
        Map session = ask("POST", "/v1/checkout/sessions", form("mode", "setup",
                "customer", customer, "payment_method_types[0]", "card",
                "client_reference_id", setupId,
                "success_url", returnUrl + "?result=done",
                "cancel_url", returnUrl + "?result=cancelled"), "");
        Setup setup = new Setup();
        setup.hosted = true;
        setup.reference = id(session);
        setup.url = text(session, "url");
        if (setup.url.length() == 0) {
            throw new IOException("The payment processor returned no page to open");
        }
        return setup;
    }

    @Override
    public Card finishCardSetup(String customer, String reference, CardDto typed)
            throws IOException {
        Map session = ask("GET", "/v1/checkout/sessions/" + UrlText.encode(reference)
                + "?expand%5B%5D=setup_intent.payment_method", null, "");
        Object intent = session.get("setup_intent");
        Object method = intent instanceof Map ? ((Map) intent).get("payment_method") : null;
        if (!"complete".equals(text(session, "status")) || !(intent instanceof Map)
                || !"succeeded".equals(text((Map) intent, "status")) || !(method instanceof Map)) {
            throw new PaymentDeclinedException("The card was not added. Try again.");
        }
        // The session is looked up by a reference this server stored for this
        // rider, so this cannot differ -- unless something is very wrong, in
        // which case the card is not attached to anybody.
        if (!customer.equals(text(session, "customer"))) {
            throw new IOException("The payment processor's session is for another customer");
        }
        Card card = new Card();
        card.token = id((Map) method);
        Object details = ((Map) method).get("card");
        if (details instanceof Map) {
            card.brand = brand(text((Map) details, "brand"));
            card.last4 = text((Map) details, "last4");
            card.expMonth = (int) number((Map) details, "exp_month");
            card.expYear = (int) number((Map) details, "exp_year");
        }
        return card;
    }

    @Override
    public String charge(String key, String customer, String token, long amountCents,
            String currency, String description) throws IOException {
        Map intent = ask("POST", "/v1/payment_intents", form("amount", String.valueOf(amountCents),
                "currency", asciiLower(currency), "customer", customer, "payment_method", token,
                "off_session", "true", "confirm", "true", "description", description,
                "metadata[wayline_charge]", key), key);
        if (!"succeeded".equals(text(intent, "status"))) {
            // Most often `requires_action`: the bank wants the cardholder to
            // approve it, and there is nobody at a screen to do so.
            throw new PaymentDeclinedException("The card needs its owner's approval");
        }
        return id(intent);
    }

    @Override
    public String refund(String chargeReference, long amountCents) throws IOException {
        Map refund = ask("POST", "/v1/refunds", form("payment_intent", chargeReference,
                "amount", String.valueOf(amountCents)), "refund-" + chargeReference);
        String status = text(refund, "status");
        if (!"succeeded".equals(status) && !"pending".equals(status)) {
            throw new IOException("The refund was not accepted: " + status);
        }
        return id(refund);
    }

    /// Makes a request and returns the object in the answer.
    ///
    /// A card Stripe refuses is answered with status 402 and an error of type
    /// `card_error`, whose message is written for the cardholder; that becomes
    /// a [PaymentDeclinedException]. Any other failure is a fault of ours or
    /// theirs and is reported by status and code only.
    private Map ask(String method, String path, String form, String key) throws IOException {
        Reply reply = transport.send(method, path, form, key);
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
            Map details = (Map) error;
            if ("card_error".equals(text(details, "type"))) {
                String message = text(details, "message");
                throw new PaymentDeclinedException(message.length() > 0 ? message
                        : "Your card was declined");
            }
            throw new IOException("The payment processor answered " + reply.status + " "
                    + text(details, "type") + " " + text(details, "code"));
        }
        throw new IOException("The payment processor answered " + reply.status);
    }

    /// A form body from names and values in turn. Both are escaped: a name
    /// such as `metadata[wayline_user]` has brackets in it.
    static String form(String... pairs) throws IOException {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter + 1 < pairs.length; iter += 2) {
            if (out.length() > 0) {
                out.append('&');
            }
            out.append(UrlText.encode(pairs[iter])).append('=')
                    .append(UrlText.encode(pairs[iter + 1] == null ? "" : pairs[iter + 1]));
        }
        return out.toString();
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

    private static long number(Map object, String key) {
        Object value = object.get(key);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    /// Stripe names brands in lower case (`visa`, `mastercard`, `amex`); this is
    /// the name with its first letter raised, which is how a person writes it.
    private static String brand(String name) {
        if (name.length() == 0) {
            return "Card";
        }
        char first = name.charAt(0);
        if (first >= 'a' && first <= 'z') {
            first = (char) (first - ('a' - 'A'));
        }
        return first + name.substring(1);
    }

    /// Stripe wants a currency code in lower case. Folded by hand, because
    /// `toLowerCase()` follows the server's locale and a currency code must not.
    private static String asciiLower(String text) {
        char[] chars = text.toCharArray();
        for (int iter = 0; iter < chars.length; iter++) {
            if (chars[iter] >= 'A' && chars[iter] <= 'Z') {
                chars[iter] = (char) (chars[iter] + ('a' - 'A'));
            }
        }
        return new String(chars);
    }

    /// The network, as this class uses it: one request to the API, one answer.
    public interface Transport {
        /// @param path the path and query, from `/v1`
        /// @param form the form-encoded body, or null for a request without one
        /// @param idempotencyKey sent as `Idempotency-Key` when not empty, so a
        ///     request repeated with the same key is carried out once
        Reply send(String method, String path, String form, String idempotencyKey)
                throws IOException;
    }

    /// An answer: its status and its body, whatever the status.
    public static final class Reply {
        public final int status;
        public final String body;

        public Reply(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    /// The real network: HTTPS to `api.stripe.com`, as [Web] makes it.
    static final class WebTransport implements Transport {
        private final String secret;

        WebTransport(String secret) {
            this.secret = secret;
        }

        @Override
        public Reply send(String method, String path, String form, String idempotencyKey)
                throws IOException {
            List headers = new ArrayList();
            headers.add("Authorization: Bearer " + secret);
            if (form != null) {
                headers.add("Content-Type: application/x-www-form-urlencoded");
            }
            if (idempotencyKey != null && idempotencyKey.length() > 0) {
                headers.add("Idempotency-Key: " + idempotencyKey);
            }
            Web.Result answer = Web.request(method, API + path, headers,
                    form == null ? null : form.getBytes("UTF-8"));
            return new Reply(answer.getStatus(), answer.getBodyAsString());
        }
    }
}
