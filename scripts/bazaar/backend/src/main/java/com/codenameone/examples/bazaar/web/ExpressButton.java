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

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.checkout.ExpressCheckouts;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.web.security.WebHeaders;

import java.io.IOException;

/// Puts the express checkout button on a page: the product page, the cart
/// and the checkout. With Stripe it is Stripe's payment request button (Apple
/// Pay or Google Pay, whichever the browser has), and the page's policy is
/// widened for Stripe.js ([WebHeaders#allowStripe]) -- on these pages only.
/// With the simulated processor it is a plain button that completes the same
/// server flow with a demo address, and the policy stays as it is.
@Component
public class ExpressButton {
    private final ExpressCheckouts express;
    private final Payments payments;

    public ExpressButton(ExpressCheckouts express, Payments payments) {
        this.express = express;
        this.payments = payments;
    }

    /// Fills `meta`'s express fields for `store` when express checkout is
    /// offered there; `country` is the sheet's first guess (empty: the US) and
    /// `cents` its first total in the store's currency (0 until the sheet
    /// prices an address, on a product page).
    public void decorate(PageMeta meta, Store store, String country, long cents)
            throws IOException {
        if (!express.offered(store)) {
            return;
        }
        meta.express = "/s/" + SiteUrls.encode(store.getHandle()) + "/express";
        meta.expressKey = payments.provider().publicKey();
        meta.expressCountry = country != null && country.length() == 2 ? country : "US";
        meta.expressCurrency = store.getCurrency();
        meta.expressAmount = String.valueOf(Math.max(0, cents));
        if (stripe(payments.provider().name())) {
            WebHeaders.allowStripe();
        }
    }

    /// Whether `provider` is the one whose script the storefront may load.
    static boolean stripe(String provider) {
        return "stripe".equals(provider);
    }
}
