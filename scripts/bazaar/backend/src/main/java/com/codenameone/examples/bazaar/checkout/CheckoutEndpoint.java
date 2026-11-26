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
package com.codenameone.examples.bazaar.checkout;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.CheckoutApiServer;
import com.codenameone.examples.bazaar.api.CheckoutOptionsDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;

/// The server's half of `CheckoutApi`.
@Component
public class CheckoutEndpoint implements CheckoutApiServer {
    private final Checkouts checkouts;
    private final CheckoutSettings settings;
    private final com.codenameone.examples.bazaar.cart.Carts carts;
    private final com.codenameone.examples.bazaar.cart.Quoter quoter;
    private final com.codenameone.examples.bazaar.repository.StoreRepository stores;
    private final com.codenameone.examples.bazaar.pay.Payments payments;
    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.functions.CheckoutFunctions functions;

    public CheckoutEndpoint(Checkouts checkouts, CheckoutSettings settings,
            com.codenameone.examples.bazaar.cart.Carts carts,
            com.codenameone.examples.bazaar.cart.Quoter quoter,
            com.codenameone.examples.bazaar.repository.StoreRepository stores,
            com.codenameone.examples.bazaar.pay.Payments payments) {
        this.checkouts = checkouts;
        this.settings = settings;
        this.carts = carts;
        this.quoter = quoter;
        this.stores = stores;
        this.payments = payments;
    }

    @Override
    public OrderDto place(String storeId, String key, PlaceOrderDto body) throws Exception {
        return checkouts.place(Caller.name(), storeId, key, body);
    }

    @Override
    public CheckoutOptionsDto options(String storeId, String country) throws Exception {
        com.codenameone.examples.bazaar.domain.Store store = storeId == null ? null
                : stores.findById(storeId);
        if (store == null || !com.codenameone.examples.bazaar.store.Stores.ACTIVE.equals(store.getStatus())) {
            throw new com.codename1.backend.ResponseStatusException(404, "No such store");
        }
        String who = Caller.name();
        com.codenameone.examples.bazaar.cart.Quote quote = quoter.price(
                carts.cart(storeId, who, null), null, null, who);
        String c = country == null ? "" : country.trim();
        com.codenameone.examples.bazaar.api.CheckoutOptionsDto offered = settings.options(storeId,
                payments.provider().name(), store.getCurrency(), c.length() == 2 ? upper(c) : "",
                quote.subtotalCents - quote.discountCents(),
                com.codenameone.examples.bazaar.web.SiteUrls.checkout(store.getHandle()));
        // Installed apps' payment functions hide and reorder what is offered.
        java.util.List<com.codenameone.examples.bazaar.functions.CheckoutFunctions.Active> fns =
                functions.active(storeId);
        if (fns.isEmpty()) {
            return offered;
        }
        return new com.codenameone.examples.bazaar.api.CheckoutOptionsDto(offered.provider(),
                functions.payments(fns, quote, offered.methods(), c.length() == 2 ? upper(c) : "",
                        who), offered.tips(), offered.fields(), offered.webCheckoutUrl());
    }

    /// Two letters upper case, folded by hand (never `toUpperCase()`, which
    /// follows the locale).
    static String upper(String code) {
        StringBuilder b = new StringBuilder(code.length());
        for (int iter = 0; iter < code.length(); iter++) {
            char ch = code.charAt(iter);
            b.append(ch >= 'a' && ch <= 'z' ? (char) (ch - 32) : ch);
        }
        return b.toString();
    }
}
