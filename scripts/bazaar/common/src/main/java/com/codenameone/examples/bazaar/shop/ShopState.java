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

import com.codename1.appkit.Net;
import com.codename1.io.Preferences;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.net.Session;
import java.util.ArrayList;
import java.util.List;

/// What the shopper has chosen, kept outside every screen: the store, the
/// cart, the search. Screens are rebuilt whenever a section is opened or the
/// window crosses the wide line, so nothing a shopper chose may live in a
/// component.
///
/// The cart of a store is the server's; the device keeps only the guest token
/// that names it before anyone signs in, and hands it to `merge` once someone
/// has, so what a guest put in the cart follows them into their account.
public final class ShopState {
    private static final String STORE_KEY = "bazaar.shop.store";
    private static final String TOKEN_KEY = "bazaar.shop.cart.";
    private static final String WISH_KEY = "bazaar.shop.wish.";
    private static final String ADDRESS_KEY = "bazaar.shop.addresses.";

    private static StoreDto store;
    private static CartDto cart;
    private static final SearchQuery QUERY = new SearchQuery();
    private static final List<Runnable> CART_LISTENERS = new ArrayList<Runnable>();
    private static final List<Runnable> STORE_LISTENERS = new ArrayList<Runnable>();

    private ShopState() {
    }

    // ----------------------------------------------------------------- store

    public static StoreDto store() {
        return store;
    }

    /// The handle of the store chosen last on this device; empty for none.
    public static String rememberedHandle() {
        return Preferences.get(STORE_KEY, "");
    }

    /// The shopper is in `chosen` now: remembered, the cart and the search of
    /// the last store dropped.
    public static void useStore(StoreDto chosen) {
        boolean changed = store == null || chosen == null || !store.id().equals(chosen.id());
        store = chosen;
        if (chosen != null) {
            Preferences.set(STORE_KEY, chosen.handle());
        }
        if (changed) {
            cart = null;
            QUERY.clearFilters();
            QUERY.q = "";
            fire(STORE_LISTENERS);
            fire(CART_LISTENERS);
            loadCart(null);
        }
    }

    /// Opens the remembered store without asking, for a section shown before
    /// the storefront ever was (the app reopened on the cart). Nothing
    /// happens when none is remembered; the storefront asks.
    public static void restoreStore() {
        final String handle = rememberedHandle();
        if (store != null || handle.length() == 0 || restoring) {
            return;
        }
        restoring = true;
        Api.stores().store(handle, Net.to(found -> {
            restoring = false;
            if (store == null && found != null && "ACTIVE".equals(found.status())) {
                useStore(found);
            }
        }, (status, message) -> restoring = false));
    }

    private static boolean restoring;

    public static void onStore(Runnable listener) {
        if (!STORE_LISTENERS.contains(listener)) {
            STORE_LISTENERS.add(listener);
        }
    }

    public static void offStore(Runnable listener) {
        STORE_LISTENERS.remove(listener);
    }

    /// The store's currency, or null for the server's.
    public static String currency() {
        return store == null ? null : store.currency();
    }

    /// `cents` in the store's currency (every store names one).
    public static String money(long cents) {
        return Money.format(cents, currency(), "USD");
    }

    public static SearchQuery query() {
        return QUERY;
    }

    // ------------------------------------------------------------------ cart

    public static CartDto cart() {
        return cart;
    }

    public static int units() {
        return Estimate.units(cart);
    }

    public static void onCart(Runnable listener) {
        if (!CART_LISTENERS.contains(listener)) {
            CART_LISTENERS.add(listener);
        }
    }

    public static void offCart(Runnable listener) {
        CART_LISTENERS.remove(listener);
    }

    /// The guest token of the current store's cart, or null.
    static String token() {
        if (store == null) {
            return null;
        }
        String token = Preferences.get(TOKEN_KEY + store.id(), "");
        return token.length() == 0 ? null : token;
    }

    /// Takes what the server says the cart is now.
    public static void setCart(CartDto changed) {
        cart = changed;
        if (changed != null && store != null && Session.user() == null && changed.token() != null
                && changed.token().length() > 0) {
            // Only a guest keeps a token: a signed-in cart is the account's.
            Preferences.set(TOKEN_KEY + store.id(), changed.token());
        }
        fire(CART_LISTENERS);
    }

    /// Fetches the cart of the current store. A guest cart left from before
    /// sign-in is merged into the account's first, and its token forgotten.
    public static void loadCart(final Runnable done) {
        if (store == null) {
            return;
        }
        final String storeId = store.id();
        final String token = token();
        if (token != null && Session.user() != null) {
            Api.cart().merge(storeId, token, Net.to(merged -> {
                Preferences.delete(TOKEN_KEY + storeId);
                if (store != null && storeId.equals(store.id())) {
                    setCart(merged);
                }
                run(done);
            }, (status, message) -> {
                // A token the server no longer knows: start afresh.
                if (status == 404 || status == 410) {
                    Preferences.delete(TOKEN_KEY + storeId);
                }
                get(storeId, done);
            }));
            return;
        }
        get(storeId, done);
    }

    private static void get(final String storeId, final Runnable done) {
        Api.cart().get(storeId, token(), Net.to(fetched -> {
            if (store != null && storeId.equals(store.id())) {
                setCart(fetched);
            }
            run(done);
        }, (status, message) -> run(done)));
    }

    /// Adds `quantity` of a variant to the cart.
    public static void add(String variantId, int quantity, final Runnable done,
            Net.Failed failed) {
        if (store == null) {
            return;
        }
        Api.cart().add(store.id(), token(), new CartLineChangeDto(variantId, quantity),
                Net.to(changed -> {
                    setCart(changed);
                    run(done);
                }, failed));
    }

    // -------------------------------------------------------------- wishlist

    /// The products saved for later on this device, by the signed-in owner.
    /// The server has no wishlist yet, so it is kept here.
    public static List<String> wishlist() {
        List<String> ids = new ArrayList<String>();
        String saved = Preferences.get(WISH_KEY + Session.owner(), "");
        int start = 0;
        while (start < saved.length()) {
            int end = saved.indexOf(',', start);
            if (end < 0) {
                end = saved.length();
            }
            if (end > start) {
                ids.add(saved.substring(start, end));
            }
            start = end + 1;
        }
        return ids;
    }

    public static boolean wished(String productId) {
        return wishlist().contains(productId);
    }

    /// Saves `productId` for later, or takes it off the list; answers whether
    /// it is on the list now.
    public static boolean toggleWish(String productId) {
        List<String> ids = wishlist();
        boolean now = !ids.remove(productId);
        if (now) {
            ids.add(0, productId);
        }
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < ids.size() && iter < 100; iter++) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(ids.get(iter));
        }
        Preferences.set(WISH_KEY + Session.owner(), out.toString());
        return now;
    }

    // ------------------------------------------------------------- addresses

    public static List<AddressDto> addresses() {
        return Addresses.decode(Preferences.get(ADDRESS_KEY + Session.owner(), ""));
    }

    public static void rememberAddress(AddressDto address) {
        Preferences.set(ADDRESS_KEY + Session.owner(),
                Addresses.encode(Addresses.remember(addresses(), address)));
    }

    public static void forgetAddress(AddressDto address) {
        List<AddressDto> kept = new ArrayList<AddressDto>();
        for (AddressDto a : addresses()) {
            if (!Addresses.same(a, address)) {
                kept.add(a);
            }
        }
        Preferences.set(ADDRESS_KEY + Session.owner(), Addresses.encode(kept));
    }

    private static void fire(List<Runnable> listeners) {
        List<Runnable> copy = new ArrayList<Runnable>(listeners);
        for (Runnable r : copy) {
            r.run();
        }
    }

    private static void run(Runnable r) {
        if (r != null) {
            r.run();
        }
    }
}
