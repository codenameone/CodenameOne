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
import com.codenameone.examples.bazaar.api.SavedAddressDto;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.net.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    /// The store something was last added to the cart of: see [#cartStore].
    private static final String CART_STORE_KEY = "bazaar.shop.cartStore";
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
        useStore(chosen, false);
    }

    /// [#useStore], keeping the search when `keepSearch`: a product opened
    /// from a search of every store enters its store, and coming back must
    /// find the search the shopper typed, not every product of every store.
    private static void useStore(StoreDto chosen, boolean keepSearch) {
        boolean changed = store == null || chosen == null || !store.id().equals(chosen.id());
        store = chosen;
        if (chosen != null) {
            Preferences.set(STORE_KEY, chosen.handle());
        }
        if (changed) {
            cart = null;
            if (!keepSearch) {
                QUERY.clearFilters();
                QUERY.q = "";
            }
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

    // ------------------------------------------------------- across stores

    private static final Map<String, StoreDto> KNOWN = new HashMap<String, StoreDto>();
    private static boolean inStore;

    /// Remembers `stores` by id, so a product from the feed can name its
    /// store's currency and open in it.
    public static void know(List<StoreDto> stores) {
        for (int iter = 0; stores != null && iter < stores.size(); iter++) {
            KNOWN.put(stores.get(iter).id(), stores.get(iter));
        }
    }

    /// A store seen in a listing by id, or null.
    public static StoreDto known(String storeId) {
        return storeId == null ? null : KNOWN.get(storeId);
    }

    /// `cents` in the currency of the store `storeId`, the current store's
    /// when it is not known.
    static String money(String storeId, long cents) {
        StoreDto s = known(storeId);
        return s == null ? money(cents) : Money.format(cents, s.currency(), "USD");
    }

    /// Whether the home tab shows the current store's page rather than the
    /// discovery feed across every store.
    public static boolean inStore() {
        return inStore && store != null;
    }

    public static void setInStore(boolean on) {
        inStore = on;
    }

    /// Makes the store of `storeId` the current one, when it is known and is
    /// not already: a product of another store is opened, and its cart is the
    /// one it goes into.
    static void enter(String storeId) {
        enter(storeId, null);
    }

    /// [#enter], then `done` once the store is the current one. A store no
    /// listing has shown yet -- one opened after the home screen fetched the
    /// list, or reached from a search before home was ever shown -- is
    /// looked up first; `done` is not run when it cannot be found.
    static void enter(final String storeId, final Runnable done) {
        enter(storeId, done, null);
    }

    /// [#enter], running `missing` instead of `done` when the store cannot
    /// be found.
    static void enter(final String storeId, final Runnable done, final Runnable missing) {
        switch (entering(store == null ? null : store.id(), storeId, known(storeId) != null)) {
            case ENTER_STAY:
                run(done);
                return;
            case ENTER_SWITCH:
                useStore(known(storeId), true);
                run(done);
                return;
            case ENTER_LOOKUP:
                Api.stores().stores(Net.to(list -> {
                    know(list);
                    StoreDto s = known(storeId);
                    if (s != null) {
                        if (store == null || !store.id().equals(s.id())) {
                            useStore(s, true);
                        }
                        run(done);
                    } else {
                        run(missing);
                    }
                }, (status, message) -> run(missing)));
                return;
            default:
                run(missing);
                return;
        }
    }

    static final int ENTER_NONE = 0;
    static final int ENTER_STAY = 1;
    static final int ENTER_SWITCH = 2;
    static final int ENTER_LOOKUP = 3;

    /// What [#enter] does for a product of `wanted` while `current` is the
    /// current store (null for none), `known` saying whether a listing has
    /// shown `wanted`. An unknown store is looked up, never ignored: ignored,
    /// the shopper stayed in the last store (or in none) and the product went
    /// into that store's cart -- or into no cart at all.
    static int entering(String current, String wanted, boolean known) {
        if (wanted == null || wanted.length() == 0) {
            return ENTER_NONE;
        }
        if (wanted.equals(current)) {
            return ENTER_STAY;
        }
        return known ? ENTER_SWITCH : ENTER_LOOKUP;
    }

    // ------------------------------------------------------------- follows

    private static final List<String> FOLLOWED = new ArrayList<String>();
    private static boolean followsLoaded;

    public static boolean follows(String storeId) {
        ensureOwner();
        return FOLLOWED.contains(storeId);
    }

    /// The followed stores' ids, newest first.
    public static List<String> followed() {
        ensureOwner();
        return new ArrayList<String>(FOLLOWED);
    }

    /// Fetches the follow list once per account; a guest follows nobody.
    public static void loadFollows(final Runnable done) {
        ensureOwner();
        if (Session.user() == null || followsLoaded) {
            run(done);
            return;
        }
        final String owner = Session.owner();
        Api.discover().following(Net.to(list -> {
            if (sameOwner(owner)) {
                setFollowed(list);
                followsLoaded = true;
            }
            run(done);
        }, (status, message) -> run(done)));
    }

    private static void setFollowed(List<StoreDto> list) {
        FOLLOWED.clear();
        for (int iter = 0; list != null && iter < list.size(); iter++) {
            FOLLOWED.add(list.get(iter).id());
        }
        know(list);
    }

    /// Follows `s`, or stops; changes at once and goes back if the server
    /// refuses. Signed-in callers only.
    public static void toggleFollow(final StoreDto s, final Runnable done) {
        ensureOwner();
        final boolean now = !FOLLOWED.remove(s.id());
        if (now) {
            FOLLOWED.add(0, s.id());
        }
        run(done);
        Net.Failed undo = (status, message) -> {
            if (now) {
                FOLLOWED.remove(s.id());
            } else if (!FOLLOWED.contains(s.id())) {
                FOLLOWED.add(0, s.id());
            }
            run(done);
        };
        if (now) {
            Api.discover().follow(s.id(), Net.to(ShopState::setFollowed, undo));
        } else {
            Api.discover().unfollow(s.id(), Net.to(ShopState::setFollowed, undo));
        }
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

    /// Adopts a guest cart's `token` for the current store -- the cart
    /// reminder mail's link carries it -- so the next [#loadCart] fetches
    /// that cart, or merges it into the account's when someone is signed in.
    static void adoptToken(String token) {
        if (store != null && token != null && token.length() > 0) {
            Preferences.set(TOKEN_KEY + store.id(), token);
        }
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

    /// The id of the store whose cart the Cart section shows. The cart is
    /// kept per store, and the current store follows every product opened;
    /// showing the current one alone, a shopper who filled a cart in one
    /// store, signed in and looked at a product of another found the Cart
    /// section empty, with their cart still on the server. Showing the
    /// remembered one always, a shopper who had ordered in one store and was
    /// browsing another was yanked back to the first one's empty cart.
    ///
    /// So: the current store's cart when it has lines; else the remembered
    /// store's (`remembered`, the one last added to) when its cart has lines
    /// -- or is still to be fetched, `rememberedLines` below 0 -- else the
    /// current store. Null when there is neither.
    static String cartStore(String current, int currentLines, String remembered,
            int rememberedLines) {
        boolean hasCurrent = current != null && current.length() > 0;
        if (hasCurrent && currentLines > 0) {
            return current;
        }
        if (remembered != null && remembered.length() > 0 && !remembered.equals(current)
                && rememberedLines != 0) {
            return remembered;
        }
        return hasCurrent ? current : null;
    }

    /// The number of lines in `c`; 0 for none.
    static int lines(CartDto c) {
        return c == null || c.lines() == null ? 0 : c.lines().size();
    }

    /// Fetches the cart [#cartStore] picks, entering its store when that is
    /// the remembered one, and returning to the current store when the
    /// remembered cart turns out empty or its store gone.
    public static void loadShownCart(final Runnable done) {
        final String current = store == null ? null : store.id();
        final String remembered = Preferences.get(CART_STORE_KEY, "");
        if (current == null) {
            shownCart(null, 0, remembered, done);
            return;
        }
        loadCart(() -> shownCart(current, lines(cart), remembered, done));
    }

    private static void shownCart(final String current, final int currentLines,
            final String remembered, final Runnable done) {
        String wanted = cartStore(current, currentLines, remembered, -1);
        if (wanted == null || wanted.equals(current)) {
            run(done);
            return;
        }
        final Runnable back = () -> {
            // Nothing there: forget it and show the current store's cart.
            Preferences.delete(CART_STORE_KEY);
            if (current == null) {
                run(done);
            } else {
                enter(current, () -> loadCart(done), done);
            }
        };
        enter(remembered, () -> loadCart(() -> {
            if (remembered.equals(cartStore(current, currentLines, remembered, lines(cart)))) {
                run(done);
            } else {
                back.run();
            }
        }), back);
    }

    /// Forgets the store last added to ([#cartStore]): after an order is
    /// placed, whose cart is now empty, and on sign-out, so the next account
    /// on the device does not inherit it.
    public static void forgetCartStore() {
        Preferences.delete(CART_STORE_KEY);
    }

    /// The id of the store last added to; empty for none.
    public static String cartStoreId() {
        return Preferences.get(CART_STORE_KEY, "");
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

    /// Adds `quantity` of a variant of a product of the store `storeId` to
    /// that store's cart, entering the store first when it is not the
    /// current one ([#enter]).
    public static void addTo(String storeId, final String variantId, final int quantity,
            final Runnable done, final Net.Failed failed) {
        if (storeId == null || storeId.length() == 0) {
            add(variantId, quantity, done, failed);
            return;
        }
        enter(storeId, () -> add(variantId, quantity, done, failed), () -> {
            // The store could not be found or the lookup failed: the caller
            // still hears of it, to re-enable its button and say so.
            if (failed != null) {
                failed.failed(0, "That store could not be opened");
            }
        });
    }

    /// Adds `quantity` of a variant to the cart.
    public static void add(String variantId, int quantity, final Runnable done,
            Net.Failed failed) {
        if (store == null) {
            return;
        }
        final String into = store.id();
        Api.cart().add(into, token(), new CartLineChangeDto(variantId, quantity),
                Net.to(changed -> {
                    Preferences.set(CART_STORE_KEY, into);
                    setCart(changed);
                    run(done);
                }, failed));
    }

    // -------------------------------------------- wishlist and addresses

    // The account's wishlist and address book live on the server; these are
    // the copies the screens read synchronously, kept for the owner they
    // were fetched for and changed optimistically.
    private static String accountOwner;
    private static boolean synced;
    private static final List<String> WISHED = new ArrayList<String>();
    private static final List<SavedAddressDto> SAVED = new ArrayList<SavedAddressDto>();

    private static void ensureOwner() {
        String owner = Session.owner();
        if (owner == null ? accountOwner != null : !owner.equals(accountOwner)) {
            accountOwner = owner;
            synced = false;
            followsLoaded = false;
            FOLLOWED.clear();
            WISHED.clear();
            SAVED.clear();
        }
    }

    /// Fetches the wishlist and address book from the server, after first
    /// moving up anything an older version kept on this device. The device
    /// copy is cleared only once the server has taken every entry.
    public static void syncAccount(final Runnable done) {
        ensureOwner();
        if (Session.user() == null) {
            run(done);
            return;
        }
        final String owner = Session.owner();
        migrateWishes(owner, () -> migrateAddresses(owner, () ->
                Api.wishlist().list(Net.to(items -> {
                    if (sameOwner(owner)) {
                        WISHED.clear();
                        for (int iter = 0; items != null && iter < items.size(); iter++) {
                            WISHED.add(items.get(iter).productId());
                        }
                    }
                    Api.addresses().list(Net.to(list -> {
                        if (sameOwner(owner)) {
                            SAVED.clear();
                            if (list != null) {
                                SAVED.addAll(list);
                            }
                        }
                        run(done);
                    }, (status, message) -> run(done)));
                }, (status, message) -> run(done)))));
    }

    /// Syncs once per signed-in account; later calls do nothing.
    public static void syncIfNeeded(Runnable done) {
        ensureOwner();
        if (synced || Session.user() == null) {
            run(done);
            return;
        }
        synced = true;
        syncAccount(done);
    }

    private static boolean sameOwner(String owner) {
        return owner != null && owner.equals(Session.owner());
    }

    /// Splits the comma separated ids an older version stored.
    static List<String> splitIds(String saved) {
        List<String> ids = new ArrayList<String>();
        int start = 0;
        while (saved != null && start < saved.length()) {
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

    private static void migrateWishes(final String owner, final Runnable next) {
        final List<String> local = splitIds(Preferences.get(WISH_KEY + owner, ""));
        if (local.isEmpty()) {
            next.run();
            return;
        }
        final int[] left = {local.size()};
        final boolean[] failed = {false};
        for (int iter = 0; iter < local.size(); iter++) {
            Api.wishlist().add(local.get(iter), Net.to(item -> {
                if (--left[0] == 0) {
                    if (!failed[0]) {
                        Preferences.delete(WISH_KEY + owner);
                    }
                    next.run();
                }
            }, (status, message) -> {
                // A product that is gone answers 404; that entry is done.
                failed[0] |= status != 404;
                if (--left[0] == 0) {
                    if (!failed[0]) {
                        Preferences.delete(WISH_KEY + owner);
                    }
                    next.run();
                }
            }));
        }
    }

    private static void migrateAddresses(final String owner, final Runnable next) {
        final List<AddressDto> local = Addresses.decode(Preferences.get(ADDRESS_KEY + owner, ""));
        if (local.isEmpty()) {
            next.run();
            return;
        }
        final int[] left = {local.size()};
        final boolean[] failed = {false};
        for (int iter = 0; iter < local.size(); iter++) {
            Api.addresses().add(new SavedAddressDto(null, local.get(iter), false), Net.to(saved -> {
                if (--left[0] == 0) {
                    if (!failed[0]) {
                        Preferences.delete(ADDRESS_KEY + owner);
                    }
                    next.run();
                }
            }, (status, message) -> {
                failed[0] = true;
                if (--left[0] == 0) {
                    next.run();
                }
            }));
        }
    }

    /// The products this account saved for later, newest first.
    public static List<String> wishlist() {
        ensureOwner();
        return new ArrayList<String>(WISHED);
    }

    public static boolean wished(String productId) {
        ensureOwner();
        return WISHED.contains(productId);
    }

    /// Saves `productId` for later, or takes it off the list; answers whether
    /// it is on the list now. The list changes at once and goes back if the
    /// server refuses.
    public static boolean toggleWish(final String productId) {
        ensureOwner();
        final boolean now = !WISHED.remove(productId);
        if (now) {
            WISHED.add(0, productId);
            Api.wishlist().add(productId, Net.to(item -> { }, (status, message) -> {
                WISHED.remove(productId);
            }));
        } else {
            Api.wishlist().remove(productId, Net.to(items -> { }, (status, message) -> {
                if (!WISHED.contains(productId)) {
                    WISHED.add(0, productId);
                }
            }));
        }
        return now;
    }

    /// The address book, default first.
    public static List<AddressDto> addresses() {
        ensureOwner();
        List<AddressDto> out = new ArrayList<AddressDto>();
        for (int iter = 0; iter < SAVED.size(); iter++) {
            if (SAVED.get(iter).isDefault()) {
                out.add(0, SAVED.get(iter).address());
            } else {
                out.add(SAVED.get(iter).address());
            }
        }
        return out;
    }

    /// Adds `address` to the book unless it is already there.
    public static void rememberAddress(AddressDto address) {
        ensureOwner();
        for (int iter = 0; iter < SAVED.size(); iter++) {
            if (Addresses.same(SAVED.get(iter).address(), address)) {
                return;
            }
        }
        final SavedAddressDto pending = new SavedAddressDto(null, address, SAVED.isEmpty());
        SAVED.add(pending);
        Api.addresses().add(pending, Net.to(saved -> {
            int at = SAVED.indexOf(pending);
            if (at >= 0) {
                SAVED.set(at, saved);
            }
        }, (status, message) -> SAVED.remove(pending)));
    }

    public static void forgetAddress(AddressDto address) {
        ensureOwner();
        for (int iter = SAVED.size() - 1; iter >= 0; iter--) {
            SavedAddressDto s = SAVED.get(iter);
            if (Addresses.same(s.address(), address)) {
                SAVED.remove(iter);
                if (s.id() != null) {
                    Api.addresses().remove(s.id(), Net.to(list -> {
                        SAVED.clear();
                        SAVED.addAll(list);
                    }, (status, message) -> { }));
                }
            }
        }
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
