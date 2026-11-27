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
package com.codenameone.examples.bazaar.web;

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.ExpressCheckouts;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.security.Throttle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The browser half of express checkout ([ExpressCheckouts]), driven by
/// `pay.js` from the wallet button on the product page, the cart and the
/// checkout. Every call is a same-site form post answered with JSON, never
/// cached, over the visitor's session cart, and rate limited with the
/// checkout (`Throttle` rule `checkout`).
///
/// - `POST /s/{handle}/express/start` -- opens the intent; from the product
///   page `variantId` and `quantity` first put that item in the cart.
/// - `POST /s/{handle}/express/{id}/quote` -- `country`, `region`, `postal`,
///   `city` and `rateId` as the sheet now shows them.
/// - `POST /s/{handle}/express/{id}/confirm` -- the address, `email` and
///   `rateId` the wallet confirmed with; answers the order page.
/// - `POST /s/{handle}/express/{id}/simulate` -- the development wallet,
///   with the simulated processor only.
@Controller
public class ExpressPages {
    private final Stores stores;
    private final Carts carts;
    private final ExpressCheckouts express;
    private final Throttle throttle;
    private final StoreOrderRepository orderRows;

    public ExpressPages(Stores stores, Carts carts, ExpressCheckouts express, Throttle throttle,
            StoreOrderRepository orderRows) {
        this.stores = stores;
        this.carts = carts;
        this.express = express;
        this.throttle = throttle;
        this.orderRows = orderRows;
    }

    @PostMapping("/s/{handle}/express/start")
    public HttpServer.Response start(@PathVariable("handle") String handle,
            HttpServer.Request request) throws IOException {
        try {
            Store store = guard(handle, request);
            String variant = StorefrontPages.param(request, "variantId");
            if (variant != null) {
                int qty = number(StorefrontPages.param(request, "quantity"), 1);
                carts.add(store.getId(), null, token(request, store),
                        new CartLineChangeDto(variant, qty));
            }
            Cart cart = cart(store, request);
            return json(express.start(payer(cart), store, cart), store, null);
        } catch (ResponseStatusException e) {
            return refused(e);
        }
    }

    @PostMapping("/s/{handle}/express/{id}/quote")
    public HttpServer.Response quote(@PathVariable("handle") String handle,
            @PathVariable("id") String id, HttpServer.Request request) throws IOException {
        try {
            Store store = guard(handle, request);
            Cart cart = cart(store, request);
            AddressDto partial = address(request);
            return json(express.quote(payer(cart), id, cart, partial,
                    StorefrontPages.param(request, "rateId")), store, null);
        } catch (ResponseStatusException e) {
            return refused(e);
        }
    }

    @PostMapping("/s/{handle}/express/{id}/confirm")
    public HttpServer.Response confirm(@PathVariable("handle") String handle,
            @PathVariable("id") String id, HttpServer.Request request) throws IOException {
        try {
            Store store = guard(handle, request);
            Cart cart = cart(store, request);
            ExpressCheckouts.Session done = express.confirm(payer(cart), id, cart,
                    address(request), StorefrontPages.param(request, "email"),
                    StorefrontPages.param(request, "rateId"));
            return json(done, store, done.order == null ? null : SiteUrls.order(store.getHandle(),
                    done.order.number(), CheckoutPages.linkToken(orderRows.findById(
                        done.order.id()))));
        } catch (ResponseStatusException e) {
            return refused(e);
        }
    }

    @PostMapping("/s/{handle}/express/{id}/simulate")
    public HttpServer.Response simulate(@PathVariable("handle") String handle,
            @PathVariable("id") String id, HttpServer.Request request) throws IOException {
        try {
            Store store = guard(handle, request);
            Cart cart = cart(store, request);
            String email = StorefrontPages.param(request, "email");
            ExpressCheckouts.Session done = express.simulate(payer(cart), id, cart,
                    email == null ? "wallet-shopper@example.com" : email);
            return json(done, store, SiteUrls.order(store.getHandle(), done.order.number(),
                    CheckoutPages.linkToken(orderRows.findById(done.order.id()))));
        } catch (ResponseStatusException e) {
            return refused(e);
        }
    }

    private Store guard(String handle, HttpServer.Request request) throws IOException {
        Storefront.sameSite(request);
        throttle.check("checkout", Throttle.client(request));
        return stores.activeStore(handle);
    }

    private static String token(HttpServer.Request request, Store store) {
        return StorefrontPages.token(request, store, false);
    }

    private Cart cart(Store store, HttpServer.Request request) throws IOException {
        Cart cart = carts.cart(store.getId(), null, token(request, store));
        StorefrontPages.remember(request, store, cart.getToken());
        return cart;
    }

    /// The shopper: the account when signed in, otherwise the session cart's
    /// guest name, as the HTML checkout keys them.
    private static String payer(Cart cart) {
        String me = CheckoutPages.signedIn();
        return me != null ? me : CheckoutPages.GUEST + cart.getId();
    }

    private static AddressDto address(HttpServer.Request request) {
        return new AddressDto(text(request, "name"), text(request, "line1"),
                text(request, "line2"), text(request, "city"), text(request, "region"),
                text(request, "postal"), text(request, "country"), text(request, "phone"), 0, 0);
    }

    private static String text(HttpServer.Request request, String name) {
        String v = StorefrontPages.param(request, name);
        if (v == null) {
            return "";
        }
        v = v.trim();
        return v.length() > 200 ? v.substring(0, 200) : v;
    }

    private static int number(String v, int fallback) {
        if (v == null || v.length() == 0 || v.length() > 3) {
            return fallback;
        }
        int n = 0;
        for (int iter = 0; iter < v.length(); iter++) {
            char c = v.charAt(iter);
            if (c < '0' || c > '9') {
                return fallback;
            }
            n = n * 10 + c - '0';
        }
        return n <= 0 ? fallback : n;
    }

    private static HttpServer.Response json(ExpressCheckouts.Session s, Store store, String url) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", s.id);
        if (s.clientSecret.length() > 0) {
            out.put("clientSecret", s.clientSecret);
        }
        out.put("currency", s.currency);
        out.put("total", Long.valueOf(s.totalCents));
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        item(items, "Subtotal", s.subtotalCents - s.discountCents);
        item(items, "Shipping", s.shippingCents);
        item(items, "Tax", s.taxCents);
        out.put("items", items);
        List<Map<String, Object>> options = new ArrayList<Map<String, Object>>();
        for (int iter = 0; iter < s.options.size(); iter++) {
            ExpressCheckouts.Option o = s.options.get(iter);
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", o.id);
            m.put("label", o.label);
            m.put("amount", Long.valueOf(o.cents));
            options.add(m);
        }
        out.put("options", options);
        out.put("rateId", s.rateId);
        if (url != null) {
            out.put("url", url);
        }
        return HttpServer.Response.json(200, Json.write(out)).header("Cache-Control", "no-store");
    }

    private static void item(List<Map<String, Object>> items, String label, long cents) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("label", label);
        m.put("amount", Long.valueOf(cents));
        items.add(m);
    }

    private static HttpServer.Response refused(ResponseStatusException e) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("error", e.getReason() == null ? "Refused" : e.getReason());
        return HttpServer.Response.json(e.getStatus(), Json.write(out))
                .header("Cache-Control", "no-store");
    }
}
