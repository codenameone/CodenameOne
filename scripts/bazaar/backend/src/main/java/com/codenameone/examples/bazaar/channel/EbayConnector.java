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

import com.codename1.backend.Base64;
import com.codename1.backend.Json;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A connector over an eBay-style seller REST API: an OAuth refresh-token
/// grant for the access token, the inventory API's bulk price and quantity
/// update keyed by SKU and offer id, the fulfilment API's order search by
/// creation date, and a shipping fulfilment per shipment. What it sends:
///
/// - **Token**: `POST {identity}/identity/v1/oauth2/token`, Basic
///   `client-id:client-secret`, form `grant_type=refresh_token`, the refresh
///   token and the scopes; reads `access_token` and `expires_in`.
/// - **Listing**: when the listing has no offer id yet,
///   `GET {api}/sell/inventory/v1/offer?sku=` reads `offers[0].offerId`; then
///   `POST {api}/sell/inventory/v1/bulk_update_price_quantity` with one
///   request carrying `shipToLocationAvailability.quantity` and the offer's
///   `availableQuantity` and `price {value, currency}`; reads
///   `responses[0].statusCode`.
/// - **Orders**: `GET {api}/sell/fulfillment/v1/order?filter=creationdate:[iso..]`
///   and its `next` pages (only ones on the same API host), reading
///   `orderId`, `creationDate`, `pricingSummary`, `buyer`, `lineItems` and the
///   first `fulfillmentStartInstructions[].shippingStep.shipTo`.
/// - **Shipment**: `POST {api}/sell/fulfillment/v1/order/{id}/shipping_fulfillment`
///   with `lineItems [{lineItemId, quantity}]`, `shippedDate`,
///   `shippingCarrierCode` and `trackingNumber`.
///
/// Check the field names against the marketplace's current reference before
/// connecting a real account; the shape above is what this class implements
/// and what its tests hold it to. Notifications are verified as
/// [SignedBodies] (`sha256=` HMAC under the channel's secret), the form a
/// relay in front of the marketplace's own notification scheme signs with.
public class EbayConnector implements MarketplaceConnector {
    public static final String KIND = "EBAY";
    static final String SCOPES = "https://api.ebay.com/oauth/api_scope/sell.inventory "
            + "https://api.ebay.com/oauth/api_scope/sell.fulfillment";
    static final int MAX_PAGES = 10;

    private final String apiBase;
    private final String identityBase;
    private final String clientId;
    private final String clientSecret;
    private final String marketplaceId;
    private final ChannelHttp http;

    public EbayConnector(String apiBase, String identityBase, String clientId,
            String clientSecret, String marketplaceId, ChannelHttp http) {
        this.apiBase = trimSlash(apiBase);
        this.identityBase = trimSlash(identityBase);
        this.clientId = clientId == null ? "" : clientId;
        this.clientSecret = clientSecret == null ? "" : clientSecret;
        this.marketplaceId = marketplaceId == null || marketplaceId.length() == 0 ? "EBAY_US"
                : marketplaceId;
        this.http = http;
    }

    private static String trimSlash(String s) {
        String v = s == null ? "" : s.trim();
        return v.endsWith("/") ? v.substring(0, v.length() - 1) : v;
    }

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public boolean configured() {
        return clientId.length() > 0 && clientSecret.length() > 0 && apiBase.startsWith("https://");
    }

    @Override
    public Token refresh(String refreshToken) throws ChannelException {
        if (refreshToken == null || refreshToken.length() == 0) {
            throw new ChannelException(401, false, "No refresh token; connect the account again");
        }
        try {
            List<String> headers = new ArrayList<String>();
            headers.add("Authorization: Basic " + Base64.encode((clientId + ":" + clientSecret)
                    .getBytes("UTF-8")));
            headers.add("Content-Type: application/x-www-form-urlencoded");
            String form = "grant_type=refresh_token&refresh_token="
                    + URLEncoder.encode(refreshToken, "UTF-8") + "&scope="
                    + URLEncoder.encode(SCOPES, "UTF-8");
            ChannelHttp.Reply r = http.send("POST", identityBase + "/identity/v1/oauth2/token",
                    headers, form);
            if (r.status != 200) {
                // A refused refresh token is not fixed by trying again.
                throw new ChannelException(r.status, r.status >= 500 || r.status == 429,
                        "The token refresh answered " + r.status);
            }
            Map m = Json.parseObject(r.body);
            String access = text(m.get("access_token"));
            long seconds = number(m.get("expires_in"));
            if (access.length() == 0) {
                throw new ChannelException(r.status, true, "The token refresh had no token");
            }
            return new Token(access, System.currentTimeMillis()
                    + Math.max(60L, seconds) * 1000L);
        } catch (IOException e) {
            throw new ChannelException(0, true, "The token refresh failed: " + e.getMessage());
        }
    }

    @Override
    public PushResult upsert(Session session, ListingPush push) throws ChannelException {
        try {
            String offerId = push.externalId;
            if (offerId == null || offerId.length() == 0) {
                ChannelHttp.Reply r = http.send("GET", apiBase + "/sell/inventory/v1/offer?sku="
                        + URLEncoder.encode(push.sku, "UTF-8"), headers(session, null), null);
                if (r.status != 200) {
                    throw ChannelException.of(r.status, "The offer lookup");
                }
                List offers = list(Json.parseObject(r.body).get("offers"));
                if (offers.isEmpty()) {
                    throw new ChannelException(404, false, "No offer on the marketplace for SKU "
                            + push.sku + "; list it there first");
                }
                offerId = text(((Map) offers.get(0)).get("offerId"));
            }
            Map price = new HashMap();
            price.put("value", Cents.format(push.priceCents));
            price.put("currency", push.currency);
            Map offer = new HashMap();
            offer.put("offerId", offerId);
            offer.put("availableQuantity", Integer.valueOf(push.quantity));
            offer.put("price", price);
            List offerList = new ArrayList();
            offerList.add(offer);
            Map avail = new HashMap();
            avail.put("quantity", Integer.valueOf(push.quantity));
            Map req = new HashMap();
            req.put("sku", push.sku);
            req.put("shipToLocationAvailability", avail);
            req.put("offers", offerList);
            List requests = new ArrayList();
            requests.add(req);
            Map body = new HashMap();
            body.put("requests", requests);
            ChannelHttp.Reply r = http.send("POST", apiBase
                    + "/sell/inventory/v1/bulk_update_price_quantity",
                    headers(session, push.idempotencyKey), Json.write(body));
            if (r.status != 200 && r.status != 207) {
                throw ChannelException.of(r.status, "The price and quantity update");
            }
            List responses = list(Json.parseObject(r.body).get("responses"));
            int each = responses.isEmpty() ? 0
                    : (int) number(((Map) responses.get(0)).get("statusCode"));
            if (each < 200 || each > 299) {
                throw ChannelException.of(each, "The update of SKU " + push.sku);
            }
            return new PushResult(offerId, r.etag);
        } catch (IOException e) {
            throw new ChannelException(0, true, "The listing update failed: " + e.getMessage());
        }
    }

    @Override
    public List<ExternalOrder> orders(Session session, long sinceMillis) throws ChannelException {
        List<ExternalOrder> out = new ArrayList<ExternalOrder>();
        try {
            String url = apiBase + "/sell/fulfillment/v1/order?limit=50&filter="
                    + URLEncoder.encode("creationdate:[" + Instant.ofEpochMilli(sinceMillis)
                    + "..]", "UTF-8");
            for (int page = 0; page < MAX_PAGES && url != null; page++) {
                ChannelHttp.Reply r = http.send("GET", url, headers(session, null), null);
                if (r.status != 200) {
                    throw ChannelException.of(r.status, "The order search");
                }
                Map m = Json.parseObject(r.body);
                List orders = list(m.get("orders"));
                for (int iter = 0; iter < orders.size(); iter++) {
                    out.add(order((Map) orders.get(iter)));
                }
                String next = text(m.get("next"));
                // Follow only the API's own pages: the answer cannot send the
                // seller's token anywhere else.
                url = next.startsWith(apiBase + "/") ? next : null;
            }
            return out;
        } catch (IOException e) {
            throw new ChannelException(0, true, "The order search failed: " + e.getMessage());
        }
    }

    private static ExternalOrder order(Map m) {
        ExternalOrder o = new ExternalOrder();
        o.id = text(m.get("orderId"));
        try {
            o.createdAt = Instant.parse(text(m.get("creationDate"))).toEpochMilli();
        } catch (RuntimeException unreadable) {
            o.createdAt = 0;
        }
        Map pricing = map(m.get("pricingSummary"));
        Map total = map(pricing.get("total"));
        o.currency = text(total.get("currency"));
        o.totalCents = Cents.parse(total.get("value"));
        o.shippingCents = Cents.parse(map(pricing.get("deliveryCost")).get("value"));
        o.taxCents = Cents.parse(map(pricing.get("tax")).get("value"));
        Map buyer = map(m.get("buyer"));
        o.buyerEmail = text(map(buyer.get("buyerRegistrationAddress")).get("email"));
        List starts = list(m.get("fulfillmentStartInstructions"));
        if (!starts.isEmpty()) {
            Map to = map(map(map(starts.get(0)).get("shippingStep")).get("shipTo"));
            o.shipName = text(to.get("fullName"));
            Map a = map(to.get("contactAddress"));
            o.line1 = text(a.get("addressLine1"));
            o.line2 = text(a.get("addressLine2"));
            o.city = text(a.get("city"));
            o.region = text(a.get("stateOrProvince"));
            o.postal = text(a.get("postalCode"));
            o.country = text(a.get("countryCode"));
            if (o.buyerEmail.length() == 0) {
                o.buyerEmail = text(to.get("email"));
            }
        }
        List items = list(m.get("lineItems"));
        for (int iter = 0; iter < items.size(); iter++) {
            Map li = map(items.get(iter));
            int qty = (int) Math.max(0, Math.min(10000, number(li.get("quantity"))));
            long cost = Cents.parse(map(li.get("lineItemCost")).get("value"));
            // lineItemCost is the line's total; the unit price divides it
            // (guarded: a zero quantity line is kept and refused later).
            long unit = qty > 0 ? cost / qty : cost;
            o.lines.add(new ExternalLine(text(li.get("lineItemId")), text(li.get("sku")), qty,
                    unit));
        }
        return o;
    }

    @Override
    public void ship(Session session, Shipment s) throws ChannelException {
        try {
            List items = new ArrayList();
            for (int iter = 0; iter < s.lines.size(); iter++) {
                Map li = new HashMap();
                li.put("lineItemId", s.lines.get(iter).lineId);
                li.put("quantity", Integer.valueOf(s.lines.get(iter).quantity));
                items.add(li);
            }
            Map body = new HashMap();
            body.put("lineItems", items);
            body.put("shippedDate", Instant.ofEpochMilli(s.shippedAt).toString());
            body.put("shippingCarrierCode", s.carrier);
            body.put("trackingNumber", s.tracking);
            ChannelHttp.Reply r = http.send("POST", apiBase + "/sell/fulfillment/v1/order/"
                    + URLEncoder.encode(s.orderId, "UTF-8") + "/shipping_fulfillment",
                    headers(session, null), Json.write(body));
            if (r.status != 200 && r.status != 201) {
                throw ChannelException.of(r.status, "The shipment");
            }
        } catch (IOException e) {
            throw new ChannelException(0, true, "The shipment failed: " + e.getMessage());
        }
    }

    @Override
    public boolean verify(String secret, byte[] body, String signature) {
        return SignedBodies.verify(secret, body, signature);
    }

    private List<String> headers(Session session, String idempotencyKey) {
        List<String> h = new ArrayList<String>();
        h.add("Authorization: Bearer " + session.accessToken);
        h.add("Content-Type: application/json");
        h.add("Accept: application/json");
        h.add("X-EBAY-C-MARKETPLACE-ID: " + marketplaceId);
        if (idempotencyKey != null && idempotencyKey.length() > 0) {
            h.add("Idempotency-Key: " + idempotencyKey);
        }
        return h;
    }

    static String text(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    static long number(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        long c = Cents.parse(o);
        return c / 100;
    }

    static Map map(Object o) {
        return o instanceof Map ? (Map) o : new HashMap();
    }

    static List list(Object o) {
        return o instanceof List ? (List) o : new ArrayList();
    }
}
