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

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.net.Session;
import com.codenameone.examples.bazaar.ui.Nav;
import com.codenameone.examples.bazaar.ui.Sections;
import com.codenameone.examples.bazaar.ui.Shell;
import com.codenameone.examples.bazaar.ui.SignInGate;
import com.codenameone.examples.bazaar.ui.Ui;

/// Puts the shopper's screens in the place of the Shop mode's placeholders,
/// and moves between them.
public final class ShopSections {
    private static boolean installed;

    private ShopSections() {
    }

    /// Called once, before the first home screen is built.
    public static void install() {
        if (installed) {
            // Nav.launch runs again on every sign-in; one badge listener.
            return;
        }
        installed = true;
        Sections.content(Nav.SHOP, Sections.HOME, HomePane::new);
        Sections.content(Nav.SHOP, Sections.SEARCH, SearchPane::new);
        Sections.content(Nav.SHOP, Sections.CART, CartPane::new);
        // The orders and the account are an account's: a guest is asked to
        // sign in there, and nowhere else until the checkout.
        Sections.content(Nav.SHOP, Sections.ORDERS, () -> Nav.guest()
                ? guest("shop-orders", "Track every order",
                        "Sign in to see your orders from every store, and follow each "
                        + "parcel from the warehouse to your door.")
                : new OrdersPane());
        Sections.content(Nav.SHOP, Sections.ACCOUNT, () -> Nav.guest()
                ? guest("shop-account", "Your Bazaar account",
                        "Browse and fill your cart as a guest. Sign in to check out faster, "
                        + "follow stores and keep your addresses and wishlist.")
                : new AccountPane());
        // The cart's badge follows the cart on whatever home screen is up.
        ShopState.onCart(() -> {
            Shell shell = Shell.current();
            if (shell != null && Nav.SHOP.equals(shell.mode())) {
                ShopUi.badge(shell.form());
            }
        });
    }

    private static Component guest(String name, String title, String why) {
        Container page = Ui.page();
        page.setName(name);
        page.add(SignInGate.prompt(CN.getCurrentForm(), title, why));
        return page;
    }

    /// Opens the search, with `collection` (a handle) as its only filter
    /// when given.
    static void search(String collection) {
        if (collection != null) {
            ShopState.query().clearFilters();
            ShopState.query().collection = collection;
        }
        show(Sections.SEARCH);
    }

    /// A collection from the storefront: the search, filtered to it.
    static void collection(String handle, String title) {
        ShopState.query().clearFilters();
        ShopState.query().collection = handle;
        // The banner names the collection; the handle is for the server.
        ShopState.query().collectionTitle = title;
        show(Sections.SEARCH);
    }

    /// Goes to section `id` of the shop, back on the home screen if a pushed
    /// screen (a product, the checkout) is up.
    static void show(String id) {
        Shell shell = Shell.current();
        if (shell == null || !Nav.SHOP.equals(shell.mode())) {
            return;
        }
        Form home = shell.form();
        if (CN.getCurrentForm() != home) {
            home.showBack();
        }
        shell.show(id);
        ShopUi.badge(home);
    }

    /// The address of a product on the web storefront, for sharing and for
    /// "Copy link": `/s/<store>/products/<product>`, the storefront's own
    /// path. The store is the product's (`storeId`) when the app knows it --
    /// a product opened from Discover need not be the current store's.
    static String link(String storeId, String productHandle) {
        StoreDto owner = storeId == null ? null : ShopState.known(storeId);
        if (owner == null) {
            owner = ShopState.store();
        }
        String store = owner == null ? "" : owner.handle();
        String base = Session.serverUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/s/" + store + "/products/" + productHandle;
    }

    /// Shares a product: the platform's share sheet where there is one (the
    /// Web Share API in a browser that has it), otherwise the link is copied
    /// and a toast says so.
    static void share(Component from, String storeId, String title, String handle) {
        String url = link(storeId, handle);
        if (CN.isNativeShareSupported()) {
            // The link first: the browser port splits "<url> <words>" into
            // navigator.share's url and text, and a phone shares it as is.
            // (Never the link as the image argument: Android reads that as a
            // file.)
            Display.getInstance().share(url + " " + title, null, null,
                    new Rectangle(from.getAbsoluteX(), from.getAbsoluteY(),
                            from.getWidth(), from.getHeight()));
            return;
        }
        Display.getInstance().copyToClipboard(url);
        Ui.say("Link copied");
    }
}
