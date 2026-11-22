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

/// The product page.
public final class ProductView {
    public String id = "";
    /// The product page's local path, the variant form's target.
    public String url = "";
    public String title = "";
    public String vendor = "";
    public String description = "";
    public String price = "";
    public String compareAt = "";
    /// The price as a plain decimal (`12.50`), for structured data.
    public String amount = "";
    public String currency = "";
    /// A banner suggesting the market of the visitor's country, or empty.
    public String geoNote = "";
    /// The market code that banner's button chooses ([MarketChoice]).
    public String geoUrl = "";
    /// The language the text is translated to, or empty.
    public String lang = "";
    /// That checkout charges the store's currency, when another is shown.
    public String marketNote = "";
    /// The product's storefront metafields, as `{name, value}` pairs.
    public java.util.List<String[]> specs = new java.util.ArrayList<String[]>();
    public boolean available;
    /// The chosen variant sells ahead of stock.
    public boolean preorder;
    public String availability = "";
    /// The chosen variant id, for the add-to-cart form.
    public String variantId = "";
    public String variantTitle = "";
    public boolean hasVariants;
    public String rating = "";
    public int ratingCount;
    public String stars = "";
    public java.util.List<Art> gallery = new java.util.ArrayList<Art>();
    public java.util.List<VariantView> variants = new java.util.ArrayList<VariantView>();
    /// The selling plans the product can be subscribed on: `id` the plan,
    /// `title` its cadence and saving, `price` the chosen variant's price on
    /// it. Empty when it is sold only once.
    public java.util.List<VariantView> plans = new java.util.ArrayList<VariantView>();
    public java.util.List<ReviewView> reviews = new java.util.ArrayList<ReviewView>();
    public java.util.List<CardView> related = new java.util.ArrayList<CardView>();
}
