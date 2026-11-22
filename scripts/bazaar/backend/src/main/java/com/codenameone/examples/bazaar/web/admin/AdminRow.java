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
package com.codenameone.examples.bazaar.web.admin;

/// One row of a back-office list or form: an order line, a variant, a stock
/// level, a timeline entry. Public fields, because compiled views read fields;
/// money and dates are formatted before they get here.
public class AdminRow {
    public String id = "";
    public String title = "";
    public String detail = "";
    public String meta = "";
    public String status = "";
    public String amount = "";
    public String url = "";
    /// Where the row's own form posts.
    public String action = "";
    /// Form field values, as text.
    public String quantity = "";
    public String max = "";
    public String price = "";
    public String sku = "";
    /// The names of the row's form fields (`qty_<lineId>`, `v0_price`).
    public String field = "";
    public String field2 = "";
    public String field3 = "";
    public String field4 = "";
    /// A variant's cost per item as typed, and its margin and markup in words.
    public String cost = "";
    public String margin = "";
    /// A variant's customs fields: HS code and country of origin.
    public String hs = "";
    public String origin = "";
    /// A variant's unit pricing: the measure sold ("750"), its unit, the
    /// reference quantity and unit, and the unit price it comes to.
    public String measure = "";
    public String unit = "";
    public String reference = "";
    public String referenceUnit = "";
    public String unitPrice = "";
    public boolean flag;
}
