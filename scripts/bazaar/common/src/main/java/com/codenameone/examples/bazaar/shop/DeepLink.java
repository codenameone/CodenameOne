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
package com.codenameone.examples.bazaar.shop;

import com.codename1.appkit.Net;
import com.codename1.ui.CN;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Sections;

/// The page the app was opened on in a browser: `/app/?store=<handle>` opens
/// that store, and `&product=<handle>` that product of it. The storefront's
/// "Open the app" link and the cart reminder mail (`SiteUrls.appCart` on the
/// server) both send it. Read once, on the first home screen; outside a
/// browser the property is absent and nothing happens.
public final class DeepLink {
    private static boolean done;

    private DeepLink() {
    }

    /// Opens what the address names, once per run.
    public static void apply() {
        if (done) {
            return;
        }
        done = true;
        String search = CN.getProperty("browser.window.location.search", null);
        final String store = param(search, "store");
        if (store == null) {
            return;
        }
        final String product = param(search, "product");
        Api.stores().store(store, Net.to(s -> {
            if (s == null) {
                return;
            }
            ShopState.useStore(s);
            ShopState.setInStore(true);
            ShopSections.show(Sections.HOME);
            if (product != null) {
                Api.catalog().productByHandle(store, product, Net.to(p -> {
                    if (p != null) {
                        ProductForm.show(p.id(), p.title());
                    }
                }, (status, message) -> { }));
            }
        }, (status, message) -> { }));
    }

    /// The value of `name` in a query string such as `?a=1&b=2`, when it is a
    /// handle (lower-case letters, digits and dashes); null otherwise.
    static String param(String search, String name) {
        if (search == null) {
            return null;
        }
        String key = name + "=";
        int at = 0;
        while (at < search.length()) {
            int end = search.indexOf('&', at);
            if (end < 0) {
                end = search.length();
            }
            int start = search.charAt(at) == '?' ? at + 1 : at;
            if (search.startsWith(key, start)) {
                String value = search.substring(start + key.length(), end);
                return handle(value) ? value : null;
            }
            at = end + 1;
        }
        return null;
    }

    private static boolean handle(String value) {
        if (value.length() == 0 || value.length() > 110) {
            return false;
        }
        for (int iter = 0; iter < value.length(); iter++) {
            char c = value.charAt(iter);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '-')) {
                return false;
            }
        }
        return true;
    }
}
