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

/// The confirmation page of an order: what was bought, where it goes, what
/// it cost, its shipments as they are tracked, and the invoice.
public final class OrderView {
    public int number;
    public String email = "";
    public String status = "";
    public String placed = "";
    public String shipTo = "";
    public String shippingTitle = "";
    public List<CartLineView> lines = new ArrayList<CartLineView>();
    public String subtotal = "";
    public String discount = "";
    public String shipping = "";
    public String tax = "";
    public String total = "";
    public String payment = "";
    /// The tip, or empty for none.
    public String tip = "";
    /// The store's checkout fields as the shopper answered them: each
    /// [CheckoutView.Option#label] and its answer in `value`.
    public List<CheckoutView.Option> fields = new ArrayList<CheckoutView.Option>();
    /// The order waits for a payment method that finishes in the browser
    /// (a bank redirect, pay later): the processor's form, or with the
    /// simulated processor its two buttons.
    public boolean payPending;
    public boolean paySimulated;
    public String payAction = "";
    public String payKey = "";
    public String paySecret = "";
    public String payReturn = "";
    public String invoiceUrl = "";
    public String storeUrl = "";
    public List<LinkView> tracking = new ArrayList<LinkView>();
    /// The order's downloads, once it is paid.
    public List<DownloadView> downloads = new ArrayList<DownloadView>();
}
