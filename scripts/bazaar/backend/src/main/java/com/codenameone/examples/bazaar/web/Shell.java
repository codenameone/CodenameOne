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

import com.codenameone.examples.bazaar.web.slots.SlotView;

import java.util.ArrayList;
import java.util.List;

/// The frame every signed-in web page is drawn in (`templates/shell.html`):
/// the account pages, the back office and the platform admin share it, and so
/// share one stylesheet, one script, one set of keyboard shortcuts and one
/// way of telling the user what just happened.
///
/// Public fields, because compiled views read fields and JavaBean getters.
/// Built by [WebPages], never by a controller by hand.
public class Shell {
    /// "account", "admin" or "platform": the `data-area` of the page.
    public String area = "account";
    public String title = "Bazaar";
    /// The CSP nonce of this response, printed on its style and script elements.
    public String nonce = "";
    /// The brand in the top bar, and where it links.
    public String brand = "Bazaar";
    public String home = "/";
    /// The signed-in person, as shown; empty when nobody is.
    public String user = "";
    public boolean signedIn;
    public boolean merchant;
    public boolean admin;
    /// The sidebar.
    public List<NavItem> nav = new ArrayList<NavItem>();
    /// The stores this person works at, for the back office's switcher.
    public List<NavItem> stores = new ArrayList<NavItem>();
    /// The page's heading (often the title without the brand).
    public String heading = "";
    /// One message from the previous request ("Saved", "Order refunded").
    public String toast = "";
    /// One error to announce, shown in the alert region.
    public String error = "";
    /// App slot content for the page's body end (scripts load from data attributes).
    public List<SlotView> slots = new ArrayList<SlotView>();
    /// The current store in the back office, empty elsewhere.
    public String storeId = "";
    public String storeName = "";
}
