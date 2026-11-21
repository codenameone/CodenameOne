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

/// What every page has: its title and description, the addresses search
/// engines and link previews read, and the store it belongs to (none for the
/// platform pages).
public final class PageMeta {
    /// The CSP nonce of this response (see web.security.WebHeaders).
    public String nonce = com.codenameone.examples.bazaar.web.security.WebHeaders.currentNonce();
    public String title = "Bazaar";
    public String description = "";
    /// The absolute canonical address; also sent as a `Link` header, since a
    /// compiled view keeps link-element addresses static.
    public String canonical = "";
    /// The absolute address of the picture a link preview shows, or empty.
    public String image = "";
    /// `website` or `product`.
    public String type = "website";
    /// The store accent as `#rrggbb`.
    public String accent = "#4f46e5";
    /// The store name in the header, or `Bazaar`.
    public String brand = "Bazaar";
    /// Where the brand in the header links.
    public String home = "/";
    /// The cart page, or empty on platform pages.
    public String cartUrl = "";
    /// The search action, or empty on platform pages.
    public String searchUrl = "";
    /// The search words to show again in the box.
    public String query = "";
    /// The schema.org JSON-LD for this page, or empty.
    public String jsonLd = "";
    /// Whether search engines should leave the page out of their index.
    public boolean noindex;
}
