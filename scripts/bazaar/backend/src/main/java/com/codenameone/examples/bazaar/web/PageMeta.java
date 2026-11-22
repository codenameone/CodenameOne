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
    /// Express checkout (Apple Pay, Google Pay) on this page: the endpoint
    /// `pay.js` talks to, or empty when it is not offered ([ExpressButton]).
    public String express = "";
    /// The processor's browser key; empty shows the development wallet.
    public String expressKey = "";
    public String expressCountry = "";
    public String expressCurrency = "";
    /// The sheet's first total in cents, before an address is priced.
    public String expressAmount = "0";
    public String title = "Bazaar";
    /// The interface language ([StoreText]): the `lang` of the page.
    public String lang = "en";
    /// `rtl` for a right-to-left language, else `ltr`.
    public String dir = "ltr";
    /// The language picker ([StoreText#options]): `url` is the code.
    public java.util.List<LinkView> langs = new java.util.ArrayList<LinkView>();
    /// Where the picker returns to (this page's address).
    public String langBack = "/";
    /// The shown language's own name, on the header's language button.
    public String langName = "English";
    /// The market picker ([StorefrontMarkets.Choice#picker]): `url` is the
    /// market code, empty for the store's own prices. Empty when the store
    /// sells in one currency only.
    public java.util.List<LinkView> markets = new java.util.ArrayList<LinkView>();
    /// The currency the page is priced in, on the header's market button.
    public String marketName = "";
    /// Where the hosted Codename One app is served.
    public String appUrl = "/app/";
    /// The interface text in [#lang], by key: `${page.t['cart.title']}`.
    public java.util.Map<String, String> t = StoreText.table("en");
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
    /// App slot snippets by place ([com.codenameone.examples.bazaar.web.slots.Slots]):
    /// only what this visitor consented to and this page allows.
    public java.util.List<com.codenameone.examples.bazaar.web.slots.SlotView> headSlots =
            new java.util.ArrayList<com.codenameone.examples.bazaar.web.slots.SlotView>();
    public java.util.List<com.codenameone.examples.bazaar.web.slots.SlotView> bodySlots =
            new java.util.ArrayList<com.codenameone.examples.bazaar.web.slots.SlotView>();
    public java.util.List<com.codenameone.examples.bazaar.web.slots.SlotView> priceSlots =
            new java.util.ArrayList<com.codenameone.examples.bazaar.web.slots.SlotView>();
    public java.util.List<com.codenameone.examples.bazaar.web.slots.SlotView> cartSlots =
            new java.util.ArrayList<com.codenameone.examples.bazaar.web.slots.SlotView>();
    public java.util.List<com.codenameone.examples.bazaar.web.slots.SlotView> thanksSlots =
            new java.util.ArrayList<com.codenameone.examples.bazaar.web.slots.SlotView>();
    /// Whether to show the cookie consent banner: the store has optional
    /// snippets and the visitor has not answered.
    public boolean consentAsk;
    /// Where the consent form returns to (this page's path).
    public String consentBack = "/";
    /// The store's privacy and cookie policy page, or empty.
    public String privacyUrl = "";
    /// The store's handle when the floating chat is offered (every storefront
    /// page), or empty off a store. The page carries no per-visitor chat
    /// state: storefront pages are cached and shared, so the widget reads the
    /// visitor's conversation over its own no-store calls (`chat.js`).
    public String chatStore = "";
    /// The theme classes on the page's body (palette, font, corner style), or empty.
    public String themeClass = "";
    /// The store logo's address, or empty.
    public String logoUrl = "";
    /// The store's main menu, two levels deep.
    public java.util.List<MenuView> menu = new java.util.ArrayList<MenuView>();
    /// The footer's links: the footer menu, then the published policies.
    public java.util.List<LinkView> footerLinks = new java.util.ArrayList<LinkView>();
    /// The home page's sections in the theme's order.
    public java.util.List<String> sections = new java.util.ArrayList<String>();
    /// Whether this is a signed theme preview (never cached, never indexed).
    public boolean preview;
}
