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

import java.util.List;
import java.util.Map;

/// The storefront end to end: a guest fills a cart, signs in, checks out with a
/// discount; the owner sees the order, buys a (simulated) label and ships it; and
/// the shopper's tracking then reads shipped.
public class ShopFlowTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        String sid = storeId();
        assertTrue(sid.length() > 0, "the demo store is there");

        // A guest browses and fills a cart; the server hands back its token.
        Actor guest = Actor.guest();
        String variant = variantInStock(guest);
        Map<String, Object> cart = guest.post("/api/shop/cart/" + sid + "/lines",
                "{\"variantId\":\"" + variant + "\",\"quantity\":2}");
        String cartToken = Actor.text(cart, "token");
        assertTrue(cartToken.length() > 0, "a guest cart has a token");

        // Signs in through the app, and the guest cart becomes the account's.
        signIn(CUSTOMER, "home-shop");
        Actor shopper = Actor.signIn(CUSTOMER).with("X-Cart-Token", cartToken);
        cart = shopper.post("/api/cart/" + sid + "/merge", null);
        assertTrue(!Actor.maps(cart.get("lines")).isEmpty(), "the merged cart keeps the lines");

        cart = shopper.post("/api/shop/cart/" + sid + "/discount", "{\"code\":\"WELCOME10\"}");
        assertTrue(Actor.num(cart, "discountCents") > 0, "the discount takes money off");

        String address = "{\"name\":\"Sam Shopper\",\"line1\":\"1 Market St\",\"city\":\"San Francisco\","
                + "\"region\":\"CA\",\"postal\":\"94105\",\"country\":\"US\",\"phone\":\"\",\"lat\":0,\"lng\":0}";
        cart = shopper.post("/api/shop/cart/" + sid + "/estimate", "{\"address\":" + address + "}");
        String rate = null;
        for (Object o : Actor.maps(cart.get("shippingOptions"))) {
            Map<String, Object> option = (Map<String, Object>) o;
            String kind = Actor.text(option, "kind");
            if (!"PICKUP".equals(kind) && !"LOCAL".equals(kind)) {
                rate = Actor.text(option, "rateId");
                break;
            }
        }
        assertNotNull(rate, "a shipping rate is offered for the address");

        String key = "e2e-" + System.currentTimeMillis();
        Map<String, Object> order = shopper.with("Idempotency-Key", key).post("/api/checkout/" + sid,
                "{\"address\":" + address + ",\"shippingRateId\":\"" + rate + "\",\"email\":\""
                + CUSTOMER + "\",\"idempotencyKey\":\"" + key + "\"}");
        String orderId = Actor.text(order, "id");
        assertTrue(orderId.length() > 0, "the order is placed (" + shopper.status + ")");
        assertTrue(Actor.num(order, "discountCents") > 0, "the order carries the discount");

        // The owner sees it, packs it with a label from the simulated carrier, ships it.
        Actor owner = Actor.signIn(MERCHANT);
        boolean listed = false;
        for (Object o : owner.list("/api/merchant/stores/" + sid + "/orders")) {
            listed |= orderId.equals(Actor.text((Map<String, Object>) o, "id"));
        }
        assertTrue(listed, "the owner sees the order");
        Map<String, Object> detail = owner.get("/api/merchant/stores/" + sid + "/orders/" + orderId);
        StringBuilder lines = new StringBuilder();
        for (Object o : Actor.maps(detail.get("lines"))) {
            Map<String, Object> line = (Map<String, Object>) o;
            lines.append(lines.length() == 0 ? "" : ",").append("{\"orderLineId\":\"")
                    .append(Actor.text(line, "id")).append("\",\"quantity\":")
                    .append(Actor.num(line, "quantity")).append('}');
        }
        List<Object> locations = Actor.maps(owner.get("/api/merchant/stores/" + sid + "/locations").get("root"));
        assertFalse(locations.isEmpty(), "the store has a location");
        String location = Actor.text((Map<String, Object>) locations.get(0), "id");
        Map<String, Object> parcel = owner.post("/api/merchant/stores/" + sid + "/orders/" + orderId
                + "/fulfillments", "{\"locationId\":\"" + location + "\",\"method\":\"SHIP\",\"lines\":["
                + lines + "]}");
        String parcelId = Actor.text(parcel, "id");
        assertTrue(parcelId.length() > 0, "a fulfilment is made (" + owner.status + ")");
        List<Object> rates = Actor.maps(owner.get("/api/merchant/stores/" + sid + "/fulfillments/"
                + parcelId + "/rates").get("root"));
        assertFalse(rates.isEmpty(), "the carrier quotes the parcel");
        String service = Actor.text((Map<String, Object>) rates.get(0), "service");
        parcel = owner.post("/api/merchant/stores/" + sid + "/fulfillments/" + parcelId + "/label",
                "{\"service\":\"" + service + "\"}");
        assertTrue(Actor.text(parcel, "trackingNumber").length() > 0, "the label has a tracking number");
        parcel = owner.post("/api/merchant/stores/" + sid + "/fulfillments/" + parcelId + "/ship", null);
        assertEqual("SHIPPED", Actor.text(parcel, "status"));

        // The shopper's tracking says it is on its way.
        boolean shipped = false;
        for (Object o : Actor.signIn(CUSTOMER).list("/api/orders/" + orderId + "/tracking")) {
            shipped |= "SHIPPED".equals(Actor.text((Map<String, Object>) o, "status"));
        }
        assertTrue(shipped, "the shopper's tracking reads shipped");
        signOut();
        return true;
    }
}
