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
package com.codenameone.examples.bazaar.web.account;

/// One row of an account page: an order, a line, a timeline entry, a review, a
/// session. Which fields a row fills depends on the list it is in; the rest stay
/// empty. Public fields, because compiled views read fields.
public class AccountItem {
    public String id = "";
    public String title = "";
    public String detail = "";
    public String meta = "";
    public String status = "";
    public String amount = "";
    public String url = "";
    /// Where the row's form posts.
    public String action = "";
    /// A second link or form target (a store handle, an "add to cart" path).
    public String extra = "";
    /// A quantity as text, and the most a form may ask for.
    public String quantity = "";
    public String max = "";
    /// A rating, as text ("1".."5").
    public String rating = "";
    public boolean flag;
    public boolean flag2;
    public boolean disabled;
}
