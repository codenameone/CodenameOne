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
package com.codenameone.examples.bazaar.channel;

import java.util.ArrayList;
import java.util.List;

/// One marketplace, as the sync engine ([ChannelSync]) uses it: the seller's
/// OAuth token refreshed, a listing's price and quantity written, orders read
/// since a time, a shipment told back, and a notification's signature
/// checked. Every network call of an implementation goes through a
/// [ChannelHttp], so tests answer them locally.
///
/// [EbayConnector] speaks an eBay-style REST shape; [SimulatedConnector] keeps
/// everything in memory and is what a channel uses when the server holds no
/// credentials for its marketplace.
public interface MarketplaceConnector {
    /// The channel kind this connector serves (`EBAY`, `SIMULATED`).
    String kind();

    /// Whether the server holds the marketplace application's credentials.
    boolean configured();

    /// A fresh access token for the seller's refresh token.
    Token refresh(String refreshToken) throws ChannelException;

    /// Writes one listing's quantity and price. Calling it twice with the same
    /// [ListingPush#idempotencyKey] has the effect of calling it once.
    PushResult upsert(Session session, ListingPush push) throws ChannelException;

    /// The orders created at or after `sinceMillis`, oldest first. The same
    /// order may come back on a later call; the engine imports it once.
    List<ExternalOrder> orders(Session session, long sinceMillis) throws ChannelException;

    /// Tells the marketplace that `shipment` left, with its carrier and tracking.
    void ship(Session session, Shipment shipment) throws ChannelException;

    /// Whether `signature` (as sent in [#SIGNATURE_HEADER]) signs `body` under
    /// the channel's secret. Checked before anything else is done with a
    /// notification.
    boolean verify(String secret, byte[] body, String signature);

    /// The header a notification's signature arrives in: `sha256=` and the
    /// HMAC-SHA256 of the raw body in hex.
    String SIGNATURE_HEADER = "X-Bazaar-Channel-Signature";

    /// The channel a call is for and its current access token.
    final class Session {
        public final String channelId;
        public final String accessToken;

        public Session(String channelId, String accessToken) {
            this.channelId = channelId;
            this.accessToken = accessToken;
        }
    }

    /// An access token and when it stops working (epoch millis).
    final class Token {
        public final String accessToken;
        public final long expiresAt;

        public Token(String accessToken, long expiresAt) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
        }
    }

    /// What one listing should say on the marketplace.
    final class ListingPush {
        public final String sku;
        public final String externalId;
        public final int quantity;
        public final long priceCents;
        public final String currency;
        public final String idempotencyKey;

        public ListingPush(String sku, String externalId, int quantity, long priceCents,
                String currency, String idempotencyKey) {
            this.sku = sku;
            this.externalId = externalId;
            this.quantity = quantity;
            this.priceCents = priceCents;
            this.currency = currency;
            this.idempotencyKey = idempotencyKey;
        }
    }

    /// The marketplace's id for the listing and its version tag, if any.
    final class PushResult {
        public final String externalId;
        public final String etag;

        public PushResult(String externalId, String etag) {
            this.externalId = externalId == null ? "" : externalId;
            this.etag = etag == null ? "" : etag;
        }
    }

    /// An order as the marketplace reports it; money in cents of `currency`.
    final class ExternalOrder {
        public String id = "";
        public long createdAt;
        public String currency = "";
        public String buyerEmail = "";
        public String shipName = "";
        public String line1 = "";
        public String line2 = "";
        public String city = "";
        public String region = "";
        public String postal = "";
        public String country = "";
        public long shippingCents;
        public long taxCents;
        public long totalCents;
        public final List<ExternalLine> lines = new ArrayList<ExternalLine>();
    }

    /// One line of an [ExternalOrder] or a [Shipment].
    final class ExternalLine {
        public final String lineId;
        public final String sku;
        public final int quantity;
        public final long unitCents;

        public ExternalLine(String lineId, String sku, int quantity, long unitCents) {
            this.lineId = lineId == null ? "" : lineId;
            this.sku = sku == null ? "" : sku;
            this.quantity = quantity;
            this.unitCents = unitCents;
        }
    }

    /// A shipment of a marketplace order.
    final class Shipment {
        public final String orderId;
        public final String carrier;
        public final String tracking;
        public final long shippedAt;
        public final List<ExternalLine> lines;

        public Shipment(String orderId, String carrier, String tracking, long shippedAt,
                List<ExternalLine> lines) {
            this.orderId = orderId;
            this.carrier = carrier;
            this.tracking = tracking;
            this.shippedAt = shippedAt;
            this.lines = lines;
        }
    }
}
