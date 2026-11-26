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
import com.codename1.backend.Web;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.CardDto;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Payments by Stripe, over its REST API. No SDK: a few requests, form encoded,
/// with the account's secret key as a bearer token.
///
/// **This is an untested stub.** It has never been run against a live Stripe
/// account: the requests follow Stripe's published API, and the tests hold it
/// to canned answers through [Transport], but nothing here has seen a real
/// card, a bank asking its cardholder to approve a charge, or a refund settle.
/// Treat it as a starting point, and run it against a Stripe test-mode key
/// before any money depends on it.
///
/// Selected with `bazaar.payments.provider=stripe`.
///
/// - **A customer** is made the first time a customer adds a card.
/// - **A card is added on Stripe's own page.** [#startCardSetup] opens a
///   Checkout Session in `setup` mode and hands back its address; the app
///   opens it in the browser. The card is typed there, so its number never
///   reaches the app or this server, and neither falls under the card
///   industry's rules for systems that handle one.
/// - **The setup is read back** when the app says the user returned: the
///   session's SetupIntent names the PaymentMethod Stripe saved, and its brand
///   and last four digits are all this server keeps beside its id.
/// - **An order is charged** with a PaymentIntent that is `off_session` and
///   confirmed in the same request, because the customer is in a car and not at a
///   payment form.
/// - **A refund** returns a charge to the card.
/// - **A card removed** in the app is detached from the customer, so Stripe
///   no longer offers it for a charge.
///
/// The secret key is a setting of the server (`bazaar.payments.stripe.secret`)
/// and is used for the `Authorization` header and nothing else: it is never
/// logged and never part of an error.
///
/// Everything Stripe-specific is in [#form] strings and the reading of the
/// answers. The network is behind [Transport], which is what lets the tests
/// hold both to canned answers without a key.
///
/// Amounts go to Stripe exactly as stored: Stripe's `amount` is the
/// currency's minor unit, which is what every Bazaar amount already is
/// ([com.codenameone.examples.bazaar.ops.Amounts]). Nothing here multiplies
/// by 100, so a yen order of 1250 is sent as 1250 yen. Stripe further wants
/// a three-decimal currency's amount (BHD, KWD) to end in 0; that is not
/// rounded here, since charging other than the order's total would be worse
/// than the refusal Stripe answers with.
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
        // Two first cards started together both find the account without a
        // customer and both ask for one. Only one reference is kept, and the
        // other would be left at the processor with nothing pointing at it.
        // With the same key the second request is answered with the first
        // one's customer. The key is a digest, since the header is logged in
        // places an address should not be, and it covers the name too: the
        // processor refuses a key it has seen with other fields, and a customer
        // who renamed themselves between two attempts must not be refused.
        // The price is one orphan in a case that takes two mishaps: the
        // answer to the first attempt is lost, and the customer renames
        // themselves before trying again. The second customer is the one
        // kept; the first holds no card and no money, and is left at the
        // processor rather than looked up by address on every first card.
        String key = "customer-" + Ids.sha256(username + "\n" + displayName);
        Map made = ask("POST", "/v1/customers", form("email", username, "name", displayName,
                "metadata[bazaar_user]", username), key);
        return id(made);
    }

    @Override
    public void forgetCustomer(String customer) throws IOException {
        // Deleting a customer detaches its payment methods too. 404 is the
        // answer for one deleted by a request whose answer never arrived.
        Reply reply = transport.send("DELETE", "/v1/customers/"
                + URLEncoder.encode(customer, "UTF-8"), null, "");
        if (reply.status != 404 && (reply.status < 200 || reply.status >= 300)) {
            throw new IOException("The payment processor answered " + reply.status
                    + " to deleting a customer");
        }
    }

    // tag::bazaar-payment-hosted[]
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
    // end::bazaar-payment-hosted[]

    @Override
    public Card finishCardSetup(String customer, String reference, CardDto typed)
            throws IOException {
        Map session = ask("GET", "/v1/checkout/sessions/" + URLEncoder.encode(reference, "UTF-8")
                + "?expand%5B%5D=setup_intent.payment_method", null, "");
        Object intent = session.get("setup_intent");
        Object method = intent instanceof Map ? ((Map) intent).get("payment_method") : null;
        if (!"complete".equals(text(session, "status")) || !(intent instanceof Map)
                || !"succeeded".equals(text((Map) intent, "status")) || !(method instanceof Map)) {
            throw new PaymentDeclinedException("The card was not added. Try again.");
        }
        // The session is looked up by a reference this server stored for this
        // customer, so this cannot differ -- unless something is very wrong, in
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
    public void detachCard(String customer, String token) throws IOException {
        String path = "/v1/payment_methods/" + URLEncoder.encode(token, "UTF-8");
        Reply reply = transport.send("POST", path + "/detach", "", "");
        if ((reply.status >= 200 && reply.status < 300) || reply.status == 404) {
            // Detached, or no such card: 404 is the answer for one deleted
            // with its customer, or never there.
            return;
        }
        // Stripe refuses to detach a card that is attached to nobody, which
        // is what a second request finds after the first one's answer was
        // lost. Rather than read that from an error's wording, the card is
        // looked at: no longer this customer's, it is detached.
        Reply now = transport.send("GET", path, null, "");
        if (now.status == 404) {
            return;
        }
        if (now.status >= 200 && now.status < 300) {
            Map method;
            try {
                method = Json.parseObject(now.body == null ? "" : now.body);
            } catch (IOException unreadable) {
                method = null;
            }
            if (method != null && !customer.equals(text(method, "customer"))) {
                return;
            }
        }
        throw new IOException("The payment processor answered " + reply.status
                + " to detaching a card");
    }

    @Override
    public String charge(String key, String customer, String token, long amountCents,
            String currency, String description) throws IOException {
        Map intent = ask("POST", "/v1/payment_intents", form("amount", String.valueOf(amountCents),
                "currency", asciiLower(currency), "customer", customer, "payment_method", token,
                "off_session", "true", "confirm", "true", "description", description,
                "metadata[bazaar_charge]", key), key);
        if (!"succeeded".equals(text(intent, "status"))) {
            // Most often `requires_action`: the bank wants the cardholder to
            // approve it, and there is nobody at a screen to do so.
            throw new PaymentDeclinedException("The card needs its owner's approval");
        }
        return id(intent);
    }

    /// A PaymentIntent with `capture_method=manual`: confirmed now, it ends in
    /// `requires_capture` with the money held on the card.
    @Override
    public String authorize(String key, String customer, String token, long amountCents,
            String currency, String description) throws IOException {
        Map intent = ask("POST", "/v1/payment_intents", form("amount", String.valueOf(amountCents),
                "currency", asciiLower(currency), "customer", customer, "payment_method", token,
                "off_session", "true", "confirm", "true", "capture_method", "manual",
                "description", description, "metadata[bazaar_charge]", key), key);
        if (!"requires_capture".equals(text(intent, "status"))) {
            throw new PaymentDeclinedException("The card needs its owner's approval");
        }
        return id(intent);
    }

    /// Asked again after a lost answer, Stripe refuses to capture an intent
    /// twice; so the intent is read first and one already `succeeded` is done.
    @Override
    public void capture(String reference, long amountCents) throws IOException {
        String path = "/v1/payment_intents/" + URLEncoder.encode(reference, "UTF-8");
        if ("succeeded".equals(text(ask("GET", path, null, null), "status"))) {
            return;
        }
        Map intent = ask("POST", path + "/capture", form("amount_to_capture",
                String.valueOf(amountCents)), "capture-" + reference);
        if (!"succeeded".equals(text(intent, "status"))) {
            throw new IOException("The hold was not captured: " + text(intent, "status"));
        }
    }

    // ------------------------------------------------------------- intents

    private String publishableKey = "";

    /// The account's publishable key (`bazaar.payments.stripe.publishable-key`),
    /// which Stripe.js in the shopper's browser is started with. It is public
    /// by design; the secret key never leaves the server.
    public void setPublishableKey(String key) {
        publishableKey = key == null ? "" : key;
    }

    @Override
    public String publicKey() {
        return publishableKey;
    }

    /// A PaymentIntent the browser confirms with Stripe.js: the Payment
    /// Request Button for a wallet (`card`, held with `capture_method=manual`)
    /// or the Payment Element for one other method type, taken at once.
    /// `metadata[bazaar_order]` names the order for the webhook.
    @Override
    public Intent createIntent(String key, long amountCents, String currency, String methodType,
            boolean manualCapture, String orderId) throws IOException {
        Map intent = ask("POST", "/v1/payment_intents", form("amount",
                String.valueOf(amountCents), "currency", asciiLower(currency),
                "payment_method_types[]", methodType,
                "capture_method", manualCapture ? "manual" : "automatic",
                "metadata[bazaar_charge]", key, "metadata[bazaar_order]", orderId), key);
        return intentOf(intent, methodType);
    }

    @Override
    public void updateIntent(String reference, long amountCents) throws IOException {
        ask("POST", "/v1/payment_intents/" + URLEncoder.encode(reference, "UTF-8"),
                form("amount", String.valueOf(amountCents)), null);
    }

    @Override
    public Intent intent(String reference) throws IOException {
        return intentOf(ask("GET", "/v1/payment_intents/" + URLEncoder.encode(reference, "UTF-8"),
                null, null), "");
    }

    /// Stripe's intent statuses folded into the five the server tells apart:
    /// one still waiting on the shopper (`requires_action`,
    /// `requires_confirmation`) is open, and `canceled` failed.
    private static Intent intentOf(Map answer, String methodType) throws IOException {
        Intent out = new Intent();
        out.reference = id(answer);
        out.clientSecret = text(answer, "client_secret");
        out.amountCents = number(answer, "amount");
        out.currency = text(answer, "currency");
        out.methodType = methodType;
        String status = text(answer, "status");
        if (INTENT_HELD.equals(status) || INTENT_SUCCEEDED.equals(status)
                || INTENT_PROCESSING.equals(status)) {
            out.status = status;
        } else if ("canceled".equals(status)) {
            out.status = INTENT_FAILED;
        } else {
            out.status = INTENT_OPEN;
        }
        return out;
    }

    @Override
    public void release(String reference) throws IOException {
        String path = "/v1/payment_intents/" + URLEncoder.encode(reference, "UTF-8");
        if ("canceled".equals(text(ask("GET", path, null, null), "status"))) {
            return;
        }
        ask("POST", path + "/cancel", form(), "release-" + reference);
    }

    /// The key is the caller's (Payments.refundKey), one per refund intent:
    /// a retry of a refund whose answer was lost sends the same key, and
    /// Stripe replays its first answer instead of returning the money twice,
    /// while a second partial refund names a different key and moves money.
    @Override
    public Refund refund(String chargeReference, long amountCents, String key) throws IOException {
        Map answer = ask("POST", "/v1/refunds", form("payment_intent", chargeReference,
                "amount", String.valueOf(amountCents)), key);
        String status = refundState(text(answer, "status"));
        if (REFUND_FAILED.equals(status)) {
            throw new IOException("The refund was not accepted: " + text(answer, "status"));
        }
        Refund refund = new Refund();
        refund.reference = id(answer);
        // A pending refund can still fail or be canceled, so it is not yet a
        // refund to record as made: the caller keeps asking.
        refund.pending = REFUND_PENDING.equals(status);
        return refund;
    }

    @Override
    public String refundStatus(String refundReference) throws IOException {
        return refundState(text(ask("GET", "/v1/refunds/"
                + URLEncoder.encode(refundReference, "UTF-8"), null, null), "status"));
    }

    /// The PaymentIntents created in the window that hold or took money
    /// (`requires_capture`, `processing`, `succeeded`), a page of 100 at a
    /// time, for the daily reconciliation. Stripe's `created` is in seconds.
    @Override
    public List<ProcessorCharge> charges(long fromMillis, long toMillis) throws IOException {
        List<ProcessorCharge> out = new ArrayList<ProcessorCharge>();
        String after = "";
        for (int page = 0; page < 100; page++) {
            String path = "/v1/payment_intents?limit=100&created%5Bgte%5D=" + (fromMillis / 1000L)
                    + "&created%5Blt%5D=" + ((toMillis + 999L) / 1000L)
                    + (after.length() == 0 ? "" : "&starting_after="
                        + URLEncoder.encode(after, "UTF-8"));
            Map answer = ask("GET", path, null, null);
            Object data = answer.get("data");
            if (!(data instanceof List)) {
                break;
            }
            List rows = (List) data;
            for (int iter = 0; iter < rows.size(); iter++) {
                if (!(rows.get(iter) instanceof Map)) {
                    continue;
                }
                Map row = (Map) rows.get(iter);
                after = text(row, "id");
                String status = text(row, "status");
                if (!INTENT_HELD.equals(status) && !INTENT_PROCESSING.equals(status)
                        && !INTENT_SUCCEEDED.equals(status)) {
                    continue;
                }
                ProcessorCharge c = new ProcessorCharge();
                c.reference = after;
                c.amountCents = number(row, "amount");
                c.currency = upperAscii(text(row, "currency"));
                c.createdAt = number(row, "created") * 1000L;
                out.add(c);
            }
            if (!Boolean.TRUE.equals(answer.get("has_more")) || rows.isEmpty()) {
                break;
            }
        }
        return out;
    }

    private static String upperAscii(String text) {
        StringBuilder b = new StringBuilder(text.length());
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            b.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        return b.toString();
    }

    /// Stripe's five refund states as three. `requires_action` waits on the
    /// cardholder like `pending` waits on the bank, and `canceled` returned no
    /// more money than `failed` did. A state this does not know is taken as
    /// still pending: asked about again, it is never recorded as money back.
    private static String refundState(String stripe) {
        if ("succeeded".equals(stripe)) {
            return REFUND_SUCCEEDED;
        }
        if ("failed".equals(stripe) || "canceled".equals(stripe)) {
            return REFUND_FAILED;
        }
        return REFUND_PENDING;
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
    /// such as `metadata[bazaar_user]` has brackets in it. A space is written
    /// as `+`, which a form body allows.
    static String form(String... pairs) throws IOException {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter + 1 < pairs.length; iter += 2) {
            if (out.length() > 0) {
                out.append('&');
            }
            out.append(URLEncoder.encode(pairs[iter], "UTF-8")).append('=')
                    .append(URLEncoder.encode(pairs[iter + 1] == null ? "" : pairs[iter + 1],
                            "UTF-8"));
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
        return lowerAscii(text);
    }

    /// [#asciiLower] for [StripeTerminal], which speaks to the same API.
    static String lowerAscii(String text) {
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
