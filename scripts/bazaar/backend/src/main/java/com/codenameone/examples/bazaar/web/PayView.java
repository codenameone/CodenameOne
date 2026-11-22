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

import java.util.ArrayList;
import java.util.List;

/// The page a checkout link opens (`templates/pay.html`): an order the store
/// prepared -- a draft or an unpaid invoice -- with what it holds, what it
/// costs, and the card form that pays it. Once paid the same page says so.
public final class PayView {
    public int number;
    public String email = "";
    public String storeName = "";
    public String storeUrl = "";
    public List<CartLineView> lines = new ArrayList<CartLineView>();
    public String subtotal = "";
    public String discount = "";
    public String shipping = "";
    public String tax = "";
    public String total = "";
    /// What is left to pay: the total less gift cards and earlier part payments.
    public String due = "";
    public String expires = "";
    public String action = "";
    public String cardError = "";
    /// False when the store's processor takes no card typed on this page.
    public boolean typedCards;
    public boolean paid;
}
