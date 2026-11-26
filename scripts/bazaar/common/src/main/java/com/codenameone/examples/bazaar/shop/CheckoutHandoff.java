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

import com.codenameone.examples.bazaar.api.CheckoutFieldDto;
import com.codenameone.examples.bazaar.api.CheckoutOptionsDto;
import com.codenameone.examples.bazaar.api.PaymentTypeDto;

import java.util.List;

/// Decides when the app's checkout hands a shopper to the storefront's
/// hosted checkout page. The app pays by card; wallets (Apple Pay, Google
/// Pay), pay-later and bank methods, tips and the store's own checkout
/// questions are taken on that page, which runs the same server flow.
///
/// Plain logic with no UI, so it is tested without a display.
public final class CheckoutHandoff {
    private CheckoutHandoff() {
    }

    /// Whether the store asks a question the shopper must answer, which the
    /// app's own form cannot place an order without.
    public static boolean required(CheckoutOptionsDto o) {
        if (o == null || o.fields() == null) {
            return false;
        }
        List<CheckoutFieldDto> fields = o.fields();
        for (int iter = 0; iter < fields.size(); iter++) {
            if (fields.get(iter).required()) {
                return true;
            }
        }
        return false;
    }

    /// Whether the hosted page offers anything the app does not: a method
    /// besides the card, a tip, or a checkout question.
    public static boolean offersMore(CheckoutOptionsDto o) {
        if (o == null || o.webCheckoutUrl() == null || o.webCheckoutUrl().length() == 0) {
            return false;
        }
        if (o.tips() != null && !o.tips().isEmpty()) {
            return true;
        }
        if (o.fields() != null && !o.fields().isEmpty()) {
            return true;
        }
        List<PaymentTypeDto> methods = o.methods();
        if (methods != null) {
            for (int iter = 0; iter < methods.size(); iter++) {
                if (!"card".equals(methods.get(iter).type())) {
                    return true;
                }
            }
        }
        return false;
    }
}
