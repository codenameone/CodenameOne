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

import com.codename1.backend.Config;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.PaymentRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;

import java.io.IOException;
import java.util.Map;

/// Settles orders left payment pending by a payment method that finishes
/// after the shopper leaves ([Payments#startIntent]).
///
/// The processor's events arrive at [#PATH], signed with HMAC-SHA256 over the
/// raw body under `bazaar.payments.webhook-secret` in `X-Bazaar-Signature`
/// (`sha256=<hex>`), as the dispute events are. An event only names the
/// order: what happened is asked of the processor itself
/// ([Payments#settleIntent]), so a forged or replayed event can at worst make
/// the server look again. Two event shapes are read -- the server's own
/// (`{"id", "type", "orderId"}`) and Stripe's (`data.object.metadata.bazaar_order`)
/// -- and a repeated one changes nothing.
///
/// - **Paid**: the order moves from PENDING to PAID with its tender, and goes
///   on to fulfilment like any other.
/// - **Failed**: the order is cancelled, which releases its reservations and
///   gives back the gift card and the discount uses it held.
@Component
public class PaymentEvents {
    public static final String PATH = "/api/payments/webhooks/payments";

    private final Payments payments;
    private final Orders orders;
    private final StoreOrderRepository orderRows;
    private final PaymentRepository paymentRows;
    private final String secret;

    public PaymentEvents(Payments payments, Orders orders, StoreOrderRepository orderRows,
            PaymentRepository paymentRows, Config config) throws IOException {
        this.payments = payments;
        this.orders = orders;
        this.orderRows = orderRows;
        this.paymentRows = paymentRows;
        this.secret = config.get("bazaar.payments.webhook-secret", "");
    }

    /// Whether `signature` (`sha256=<hex>`) signs `body`.
    boolean signed(byte[] body, String signature) throws IOException {
        if (secret.length() == 0 || signature == null || !signature.startsWith("sha256=")) {
            return false;
        }
        return Tokens.verify(secret, new String(body, "UTF-8"), signature.substring(7));
    }

    /// Applies a signed event; answers what became of the order's payment, or
    /// an empty string for an event of another kind.
    public String webhook(byte[] body, String signature) throws IOException {
        if (body == null || body.length > 65536 || !signed(body, signature)) {
            throw new ResponseStatusException(401, "Bad signature");
        }
        Map event;
        try {
            event = Json.parseObject(new String(body, "UTF-8"));
        } catch (IOException | RuntimeException bad) {
            throw new ResponseStatusException(400, "Not JSON");
        }
        String type = text(event.get("type"));
        if (!type.startsWith("payment.") && !type.startsWith("payment_intent.")) {
            return "";
        }
        String orderId = text(event.get("orderId"));
        Object data = event.get("data");
        if (orderId.length() == 0 && data instanceof Map) {
            Object object = ((Map) data).get("object");
            if (object instanceof Map) {
                Object meta = ((Map) object).get("metadata");
                if (meta instanceof Map) {
                    orderId = text(((Map) meta).get("bazaar_order"));
                }
            }
        }
        if (orderId.length() == 0 || orderId.length() > 32 || orderRows.findById(orderId) == null) {
            // An express checkout's intent carries no order until it is
            // confirmed; its event has nothing to settle.
            return "";
        }
        return settle(orderId);
    }

    /// Asks the processor how the order's payment went and applies it: what
    /// the webhook does, and the simulated bank page's buttons.
    public String settle(String orderId) throws IOException {
        String status = payments.settleIntent(orderId);
        StoreOrder order = orderRows.findById(orderId);
        if (order == null) {
            return status;
        }
        com.codenameone.examples.bazaar.domain.Payment row = paymentRows.findById(orderId);
        String label = row == null ? "the processor" : row.getMethodLabel();
        if (Payments.PAID.equals(status) || Payments.AUTHORIZED.equals(status)) {
            orders.settledOnline(orderId, status, label);
        } else if (Payments.FAILED.equals(status) && Orders.OPEN.equals(order.getStatus())
                && Orders.PENDING.equals(order.getFinancial())) {
            orders.event(orderId, "PAYMENT_FAILED", label + " payment failed", "processor");
            orders.cancelOrder(order, "processor");
        }
        return status;
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : "";
    }
}
