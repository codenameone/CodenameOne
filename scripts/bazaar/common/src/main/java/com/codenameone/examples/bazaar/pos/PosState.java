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
package com.codenameone.examples.bazaar.pos;

import com.codename1.appkit.Net;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.mapping.Mappers;
import com.codenameone.examples.bazaar.api.CollectionDto;
import com.codenameone.examples.bazaar.api.PosCatalogDto;
import com.codenameone.examples.bazaar.api.PosSaleDto;
import com.codenameone.examples.bazaar.api.ParkedCartDto;
import com.codenameone.examples.bazaar.api.PosGridDto;
import com.codenameone.examples.bazaar.api.PosPolicyDto;
import com.codenameone.examples.bazaar.api.PosSessionDto;
import com.codenameone.examples.bazaar.api.ShiftDto;
import com.codenameone.examples.bazaar.merchant.MerchantState;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.shop.Money;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Everything the register holds, kept outside the screens: a section's
/// content is made afresh each time it is shown, and the whole home screen
/// is rebuilt when the window is resized, but a sale in progress, the cashier
/// at the till and the sales waiting to upload must survive both.
///
/// What must survive a restart with the network down is stored with
/// `Storage`, which every target has -- the browser included -- and which
/// needs no engine to load: the register chosen on this device, the cashier's
/// session and shift, the catalog snapshot, the collections and the outbox.
/// The catalog is a few hundred items and the outbox at most a few hundred
/// sales, so one value each, rewritten whole, is plenty.
public final class PosState {
    private static final String STORE = "bazaar.pos.store";
    private static final String STORE_NAME = "bazaar.pos.storeName";
    private static final String STORE_HANDLE = "bazaar.pos.storeHandle";
    private static final String CURRENCY = "bazaar.pos.currency";
    private static final String REGISTER = "bazaar.pos.register";
    private static final String REGISTER_NAME = "bazaar.pos.registerName";
    private static final String LOCATION = "bazaar.pos.location";

    private static final PosCart CART = new PosCart();
    private static final List<Runnable> CART_LISTENERS = new ArrayList<Runnable>();
    private static final List<Runnable> STATUS_LISTENERS = new ArrayList<Runnable>();

    private static PosCatalogDto catalog;
    private static String catalogFor = "";
    /// Collection id to its title and its product ids, in display order.
    private static final Map<String, String> COLLECTION_TITLES = new LinkedHashMap<String, String>();
    private static final Map<String, List<String>> COLLECTIONS = new LinkedHashMap<String, List<String>>();
    private static PosSessionDto session;
    private static boolean sessionLoaded;
    private static Outbox outbox;
    private static String outboxFor = "";

    private PosState() {
    }

    // ---------------------------------------------------------------- device

    public static String storeId() {
        return Preferences.get(STORE, "");
    }

    public static String storeName() {
        return Preferences.get(STORE_NAME, "");
    }

    public static String storeHandle() {
        return Preferences.get(STORE_HANDLE, "");
    }

    public static String registerId() {
        return Preferences.get(REGISTER, "");
    }

    public static String registerName() {
        return Preferences.get(REGISTER_NAME, "");
    }

    public static String locationId() {
        return Preferences.get(LOCATION, "");
    }

    public static boolean hasRegister() {
        return storeId().length() > 0 && registerId().length() > 0;
    }

    /// Remembers the register this device is, and forgets the cashier.
    public static void useRegister(String storeId, String storeName, String handle,
            String currency, String registerId, String registerName, String locationId) {
        Preferences.set(STORE, storeId);
        Preferences.set(STORE_NAME, storeName == null ? "" : storeName);
        Preferences.set(STORE_HANDLE, handle == null ? "" : handle);
        Preferences.set(CURRENCY, currency == null ? "" : currency);
        Preferences.set(REGISTER, registerId);
        Preferences.set(REGISTER_NAME, registerName == null ? "" : registerName);
        Preferences.set(LOCATION, locationId == null ? "" : locationId);
        setSession(null);
        catalog = null;
        catalogFor = "";
        outbox = null;
        CART.clear();
        fireCart();
    }

    /// Forgets the register, for choosing another one.
    public static void forgetRegister() {
        Preferences.set(REGISTER, "");
        setSession(null);
        fireStatus();
    }

    public static String currency() {
        String c = catalog != null && catalog.currency() != null && catalog.currency().length() > 0
                ? catalog.currency() : Preferences.get(CURRENCY, "");
        return c.length() == 0 ? MerchantState.currency() : c;
    }

    public static String money(long cents) {
        return Money.format(cents, currency(), "USD");
    }

    // ---------------------------------------------------------------- session

    /// Who made the sale: the cashier signed in with their PIN, or "" for
    /// the device's own account.
    public static String staffUsername() {
        PosSessionDto s = session();
        return s == null || s.username() == null ? "" : s.username();
    }

    /// The cashier signed in at this register, or null.
    public static PosSessionDto session() {
        if (!sessionLoaded) {
            sessionLoaded = true;
            Object stored = read(key("session"));
            if (stored instanceof String) {
                try {
                    session = Mappers.fromJson((String) stored, PosSessionDto.class);
                } catch (RuntimeException bad) {
                    session = null;
                }
            }
        }
        return session;
    }

    public static void setSession(PosSessionDto s) {
        session = s;
        sessionLoaded = true;
        if (s == null) {
            Storage.getInstance().deleteStorageFile(key("session"));
        } else {
            write(key("session"), Mappers.toJson(s));
        }
        fireStatus();
    }

    /// The open shift, or null.
    public static ShiftDto shift() {
        PosSessionDto s = session();
        ShiftDto shift = s == null ? null : s.shift();
        return shift != null && "OPEN".equalsIgnoreCase(shift.status()) ? shift : null;
    }

    public static String shiftId() {
        ShiftDto s = shift();
        return s == null ? "" : s.id();
    }

    /// The shift as the server now has it (null once closed).
    public static void setShift(ShiftDto shift) {
        PosSessionDto s = session();
        if (s == null) {
            return;
        }
        setSession(new PosSessionDto(s.registerId(), s.storeId(), s.locationId(), s.username(),
                s.displayName(), s.role(), shift));
    }

    // ---------------------------------------------------------------- catalog

    public static PosCatalogDto catalog() {
        String reg = registerId();
        if (!reg.equals(catalogFor)) {
            catalogFor = reg;
            catalog = null;
            Object stored = read(key("catalog"));
            if (stored instanceof String) {
                try {
                    catalog = Mappers.fromJson((String) stored, PosCatalogDto.class);
                } catch (RuntimeException bad) {
                    catalog = null;
                }
            }
            loadCollections();
        }
        return catalog;
    }

    static void setCatalog(PosCatalogDto c) {
        catalog = c;
        catalogFor = registerId();
        if (c != null) {
            write(key("catalog"), Mappers.toJson(c));
        }
        fireCart();
    }

    /// Asks for the catalog, sending the version held, and keeps the answer
    /// merged with the sales not yet uploaded. `done` runs either way.
    public static void refreshCatalog(final Runnable done) {
        if (!hasRegister()) {
            if (done != null) {
                done.run();
            }
            return;
        }
        PosCatalogDto held = catalog();
        String version = held == null || held.version() == null ? "" : held.version();
        Api.pos().catalog(storeId(), registerId(), version, Net.to(fresh -> {
            PosSync.reachable(true);
            setCatalog(Snapshot.merge(held, fresh, outbox().unsyncedUnits()));
            if (done != null) {
                done.run();
            }
        }, (status, message) -> {
            if (status <= 0) {
                PosSync.reachable(false);
            }
            if (done != null) {
                done.run();
            }
        }));
        refreshCollections();
    }

    /// The collections, as tabs over the product grid.
    public static Map<String, String> collectionTitles() {
        catalog();
        return new LinkedHashMap<String, String>(COLLECTION_TITLES);
    }

    public static Map<String, List<String>> collections() {
        catalog();
        return COLLECTIONS;
    }

    private static void refreshCollections() {
        String handle = storeHandle();
        if (handle.length() == 0) {
            return;
        }
        Api.catalog().collections(handle, Net.to(list -> {
            COLLECTION_TITLES.clear();
            COLLECTIONS.clear();
            StringBuilder out = new StringBuilder();
            for (int iter = 0; list != null && iter < list.size(); iter++) {
                CollectionDto c = list.get(iter);
                List<String> ids = c.productIds() == null ? new ArrayList<String>() : c.productIds();
                COLLECTION_TITLES.put(c.id(), c.title());
                COLLECTIONS.put(c.id(), new ArrayList<String>(ids));
                out.append(flat(c.id())).append('\t').append(flat(c.title())).append('\t');
                for (int p = 0; p < ids.size(); p++) {
                    out.append(p == 0 ? "" : ",").append(ids.get(p));
                }
                out.append('\n');
            }
            write(key("collections"), out.toString());
            fireCart();
        }, (status, message) -> { }));
    }

    private static void loadCollections() {
        COLLECTION_TITLES.clear();
        COLLECTIONS.clear();
        Object stored = read(key("collections"));
        if (!(stored instanceof String)) {
            return;
        }
        String text = (String) stored;
        int start = 0;
        while (start < text.length()) {
            int end = text.indexOf('\n', start);
            if (end < 0) {
                end = text.length();
            }
            String line = text.substring(start, end);
            start = end + 1;
            int a = line.indexOf('\t');
            int b = a < 0 ? -1 : line.indexOf('\t', a + 1);
            if (b < 0) {
                continue;
            }
            String id = line.substring(0, a);
            List<String> ids = new ArrayList<String>();
            String rest = line.substring(b + 1);
            int s = 0;
            while (s < rest.length()) {
                int comma = rest.indexOf(',', s);
                if (comma < 0) {
                    comma = rest.length();
                }
                if (comma > s) {
                    ids.add(rest.substring(s, comma));
                }
                s = comma + 1;
            }
            COLLECTION_TITLES.put(id, line.substring(a + 1, b));
            COLLECTIONS.put(id, ids);
        }
    }

    // ---------------------------------------------------------------- outbox

    public static Outbox outbox() {
        String reg = registerId();
        if (outbox == null || !reg.equals(outboxFor)) {
            outboxFor = reg;
            outbox = new Outbox(new Outbox.Persist() {
                @Override
                public String read(String k) {
                    Object o = PosState.read(key(k));
                    return o instanceof String ? (String) o : null;
                }

                @Override
                public void write(String k, String value) {
                    PosState.write(key(k), value);
                }
            }, new Outbox.Codec() {
                @Override
                public String encode(PosSaleDto sale) {
                    return Mappers.toJson(sale);
                }

                @Override
                public PosSaleDto decode(String text) {
                    try {
                        return Mappers.fromJson(text, PosSaleDto.class);
                    } catch (RuntimeException bad) {
                        return null;
                    }
                }
            });
        }
        return outbox;
    }

    /// Records a finished sale: outbox first, then the stock it took.
    public static Outbox.Entry recordSale(PosSaleDto sale) {
        Outbox.Entry e = outbox().add(sale);
        PosCatalogDto c = catalog();
        if (c != null) {
            setCatalog(Snapshot.sold(c, sale.lines()));
        }
        fireStatus();
        PosSync.kick();
        return e;
    }

    // ---------------------------------------------------------------- cart

    public static PosCart cart() {
        return CART;
    }

    /// The sale priced now.
    public static PosPricing.Totals totals() {
        return CART.totals(catalog(), collections(), System.currentTimeMillis());
    }

    public static void onCart(Runnable r) {
        if (!CART_LISTENERS.contains(r)) {
            CART_LISTENERS.add(r);
        }
    }

    public static void offCart(Runnable r) {
        CART_LISTENERS.remove(r);
    }

    public static void fireCart() {
        List<Runnable> copy = new ArrayList<Runnable>(CART_LISTENERS);
        for (int iter = 0; iter < copy.size(); iter++) {
            copy.get(iter).run();
        }
    }

    public static void onStatus(Runnable r) {
        if (!STATUS_LISTENERS.contains(r)) {
            STATUS_LISTENERS.add(r);
        }
    }

    public static void offStatus(Runnable r) {
        STATUS_LISTENERS.remove(r);
    }

    public static void fireStatus() {
        List<Runnable> copy = new ArrayList<Runnable>(STATUS_LISTENERS);
        for (int iter = 0; iter < copy.size(); iter++) {
            copy.get(iter).run();
        }
    }

    // ----------------------------------------------------------------- policy

    /// The tip presets of the store's policy as last fetched, in basis
    /// points; empty when never fetched.
    public static long[] tipPresets() {
        Object stored = read(key("tips"));
        return parseBp(stored instanceof String ? (String) stored : "");
    }

    /// Keeps the store's point-of-sale policy for offline use.
    /// Caches the store's policy: the tip presets and everything the
    /// approval and layaway decisions read, so they hold offline.
    public static void policy(PosPolicyDto p) {
        if (p == null) {
            return;
        }
        StringBuilder b = new StringBuilder();
        long[] all = {p.tip1Bp(), p.tip2Bp(), p.tip3Bp()};
        for (int iter = 0; iter < all.length; iter++) {
            if (all[iter] > 0) {
                b.append(b.length() > 0 ? "," : "").append(all[iter]);
            }
        }
        write(key("tips"), b.toString());
        write(key("policy"), PosRules.encode(p));
    }

    /// The cached policy, or null before the register first fetched it.
    public static PosPolicyDto policy() {
        Object stored = read(key("policy"));
        return stored instanceof String ? PosRules.decode((String) stored) : null;
    }

    /// Fetches the policy when the register opens; a register offline keeps
    /// the one it cached.
    public static void refreshPolicy() {
        if (!hasRegister() || Api.posTools() == null) {
            return;
        }
        Api.posTools().policy(storeId(), Net.to(p -> policy(p), (status, message) -> { }));
    }

    /// The staff role of the signed-in session, "" when nobody is signed in.
    public static String role() {
        PosSessionDto s = session();
        return s == null || s.role() == null ? "" : s.role();
    }

    // ------------------------------------------------------------------ grid
    /// The cached smart grid of this register's location, or null.
    public static PosGridDto grid() {
        Object stored = read(key("grid"));
        if (stored instanceof String) {
            try {
                return Mappers.fromJson((String) stored, PosGridDto.class);
            } catch (RuntimeException bad) {
                return null;
            }
        }
        return null;
    }

    /// Fetches the location's grid and caches it for offline use.
    public static void refreshGrid() {
        if (!hasRegister() || locationId().length() == 0 || Api.posTools() == null) {
            return;
        }
        Api.posTools().grid(storeId(), locationId(), Net.to(g -> {
            if (g != null) {
                write(key("grid"), Mappers.toJson(g));
                fireCart();
            }
        }, (status, message) -> { }));
    }

    // ---------------------------------------------------------------- parked
    private static ParkedCarts parked;
    private static String parkedFor = "";

    /// This register's parked carts, kept on the device until synced.
    public static ParkedCarts parked() {
        String reg = registerId();
        if (parked == null || !reg.equals(parkedFor)) {
            parkedFor = reg;
            parked = new ParkedCarts(new Outbox.Persist() {
                @Override
                public String read(String k) {
                    Object o = PosState.read(key(k));
                    return o instanceof String ? (String) o : null;
                }

                @Override
                public void write(String k, String value) {
                    PosState.write(key(k), value);
                }
            }, new ParkedCarts.Codec() {
                @Override
                public String encode(ParkedCartDto cart) {
                    return Mappers.toJson(cart);
                }

                @Override
                public ParkedCartDto decode(String text) {
                    try {
                        return Mappers.fromJson(text, ParkedCartDto.class);
                    } catch (RuntimeException bad) {
                        return null;
                    }
                }
            });
        }
        return parked;
    }

    static long[] parseBp(String text) {
        List<Long> out = new ArrayList<Long>();
        long n = 0;
        boolean digits = false;
        for (int iter = 0; iter <= text.length(); iter++) {
            char ch = iter == text.length() ? ',' : text.charAt(iter);
            if (ch >= '0' && ch <= '9' && n < 100000) {
                n = n * 10 + (ch - '0');
                digits = true;
            } else if (ch == ',') {
                if (digits && n > 0 && n <= 10000) {
                    out.add(Long.valueOf(n));
                }
                n = 0;
                digits = false;
            }
        }
        long[] bp = new long[out.size()];
        for (int iter = 0; iter < bp.length; iter++) {
            bp[iter] = out.get(iter).longValue();
        }
        return bp;
    }

    // ---------------------------------------------------------------- goal

    private static ShiftGoal goal;

    /// The open shift's sales against its target, counted on this register.
    static ShiftGoal goal() {
        if (goal == null) {
            Object o = read(key("goal"));
            goal = ShiftGoal.load(o instanceof String ? (String) o : null);
        }
        ShiftDto s = shift();
        if (s != null) {
            goal.begin(s.id(), s.cashSalesCents() + s.cardSalesCents() + s.otherSalesCents());
        }
        return goal;
    }

    /// Counts a finished sale toward the goal; answers the milestone it
    /// crossed (25, 50, 75 or 100), or 0.
    static int recordGoal(long cents) {
        int crossed = goal().record(cents);
        write(key("goal"), goal.save());
        return crossed;
    }

    // ---------------------------------------------------------------- storage

    private static String key(String name) {
        return "pos-" + registerId() + "-" + name;
    }

    static Object read(String name) {
        Storage s = Storage.getInstance();
        return s.exists(name) ? s.readObject(name) : null;
    }

    static void write(String name, String value) {
        Storage.getInstance().writeObject(name, value);
    }

    private static String flat(String s) {
        return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ');
    }
}
