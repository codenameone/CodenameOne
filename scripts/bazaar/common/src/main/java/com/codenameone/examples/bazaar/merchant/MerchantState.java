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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.io.Preferences;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.api.StoreRoleDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.shop.Money;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The store the merchant is working on, out of every store they are staff
/// of. Every merchant screen reads the store from here and is told when the
/// owner switches stores.
public final class MerchantState {
    private static final String REMEMBERED = "bazaar.merchant.store";

    private static final List<StoreRoleDto> STORES = new ArrayList<StoreRoleDto>();
    private static final List<Runnable> LISTENERS = new ArrayList<Runnable>();
    private static StoreDto store;
    private static String role = "";
    private static boolean loading;
    private static final List<Runnable> WAITING = new ArrayList<Runnable>();

    private MerchantState() {
    }

    /// The store being worked on, null until `load` has answered.
    public static StoreDto store() {
        return store;
    }

    /// Its id, "" before a store is known so a request is harmless.
    public static String storeId() {
        return store == null ? "" : store.id();
    }

    /// The merchant's role in the current store: OWNER, MANAGER, CASHIER or
    /// FULFILMENT.
    public static String role() {
        return role;
    }

    /// Whether the merchant may change settings, staff and money matters.
    public static boolean manages() {
        return "OWNER".equals(role) || "MANAGER".equals(role);
    }

    public static List<StoreRoleDto> stores() {
        return new ArrayList<StoreRoleDto>(STORES);
    }

    public static String currency() {
        return store == null || store.currency() == null ? "USD" : store.currency();
    }

    public static String money(long cents) {
        return Money.format(cents, currency(), "USD");
    }

    /// Runs `ready` once the merchant's stores are known: at once when they
    /// are, otherwise after one request shared by every screen that asks.
    public static void whenReady(Runnable ready) {
        if (store != null) {
            ready.run();
            return;
        }
        WAITING.add(ready);
        if (loading) {
            return;
        }
        loading = true;
        Api.stores().myStores(Net.to(list -> {
            loading = false;
            STORES.clear();
            STORES.addAll(list);
            String wanted = Preferences.get(REMEMBERED, "");
            StoreRoleDto chosen = STORES.isEmpty() ? null : STORES.get(0);
            for (int iter = 0; iter < STORES.size(); iter++) {
                if (STORES.get(iter).store().id().equals(wanted)) {
                    chosen = STORES.get(iter);
                }
            }
            if (chosen != null) {
                store = chosen.store();
                role = chosen.role();
            }
            List<Runnable> run = new ArrayList<Runnable>(WAITING);
            WAITING.clear();
            if (store == null) {
                Ui.say("You are not staff of any store yet");
                return;
            }
            for (int iter = 0; iter < run.size(); iter++) {
                run.get(iter).run();
            }
        }, (status, message) -> {
            loading = false;
            WAITING.clear();
            Ui.fail(status, message);
        }));
    }

    /// Switches to another of the merchant's stores and tells every screen.
    public static void use(StoreRoleDto chosen) {
        store = chosen.store();
        role = chosen.role();
        Preferences.set(REMEMBERED, store.id());
        fire();
    }

    /// The current store as the server has just saved it.
    public static void updated(StoreDto saved) {
        if (saved == null) {
            return;
        }
        store = saved;
        for (int iter = 0; iter < STORES.size(); iter++) {
            if (STORES.get(iter).store().id().equals(saved.id())) {
                STORES.set(iter, new StoreRoleDto(saved, STORES.get(iter).role()));
            }
        }
        fire();
    }

    public static void onStore(Runnable listener) {
        if (!LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void offStore(Runnable listener) {
        LISTENERS.remove(listener);
    }

    /// Forgets everything; the next merchant to sign in loads afresh.
    public static void reset() {
        STORES.clear();
        store = null;
        role = "";
    }

    private static void fire() {
        List<Runnable> copy = new ArrayList<Runnable>(LISTENERS);
        for (int iter = 0; iter < copy.size(); iter++) {
            copy.get(iter).run();
        }
    }
}
