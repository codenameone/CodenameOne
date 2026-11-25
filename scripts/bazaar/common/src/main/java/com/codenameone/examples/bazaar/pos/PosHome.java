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
import com.codename1.components.InfiniteProgress;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.OpenShiftDto;
import com.codenameone.examples.bazaar.api.PinSignInDto;
import com.codenameone.examples.bazaar.api.PosRegisterDto;
import com.codenameone.examples.bazaar.api.PosSessionDto;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.api.StoreRoleDto;
import com.codenameone.examples.bazaar.merchant.MerchantState;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The Register section: whatever the till needs next. A device that is no
/// register yet picks its store and register (remembered on the device); a
/// register with nobody at it asks for a PIN; a cashier with no shift open
/// counts the float in; and then it sells ([SellScreen]).
final class PosHome {
    private static PosHome current;
    private final Container root = new Container(new BorderLayout());
    private Component stage;

    private PosHome() {
        root.setName("pos-home");
        root.setUIID("PosRoot");
    }

    static Component create() {
        PosSync.start();
        if (current != null) {
            PosUi.detach(current.root);
        }
        current = new PosHome();
        current.next();
        return current.root;
    }

    /// Moves the till on to whatever it needs now.
    static void refresh() {
        if (current != null) {
            current.next();
        }
    }

    private void set(Component c) {
        if (stage != null) {
            PosUi.detach(stage);
            if (stage instanceof Container) {
                SellScreen.left((Container) stage);
            }
        }
        root.removeAll();
        stage = c;
        root.add(BorderLayout.CENTER, c);
        root.revalidateLater();
    }

    private void next() {
        if (!PosState.hasRegister()) {
            chooseStore();
        } else if (PosState.session() == null) {
            set(pin());
        } else if (PosState.shift() == null) {
            set(openShift());
        } else {
            set(SellScreen.create());
            PosState.refreshCatalog(null);
            PosState.refreshPolicy();
            PosState.refreshGrid();
        }
    }

    // -------------------------------------------------------------- setup

    private Container page(String title) {
        Container page = new Container(BoxLayout.y());
        page.setUIID("PosPage");
        page.setScrollableY(true);
        page.add(PosUi.header(title, null));
        return page;
    }

    private void chooseStore() {
        final Container page = page("Set up this register");
        page.add(new InfiniteProgress());
        set(page);
        MerchantState.whenReady(() -> {
            List<StoreRoleDto> stores = MerchantState.stores();
            if (stores.size() == 1) {
                chooseRegister(stores.get(0).store());
                return;
            }
            page.removeComponent(page.getComponentAt(1));
            page.add(PosUi.label("Which store is this?", "PosMuted"));
            for (int iter = 0; iter < stores.size(); iter++) {
                final StoreDto s = stores.get(iter).store();
                Button b = PosUi.icon(FontImage.MATERIAL_STORE, s.name(), "pos-store-" + s.handle(),
                        "PosChoice", e -> chooseRegister(s));
                b.setShouldLocalize(false);
                page.add(b);
            }
            page.revalidateLater();
        });
    }

    private void chooseRegister(final StoreDto store) {
        final Container page = page("Choose a register");
        page.add(PosUi.data(store.name(), "PosMuted"));
        page.add(new InfiniteProgress());
        set(page);
        final Map<String, String> places = new HashMap<String, String>();
        Api.stores().locations(store.id(), Net.to(locs -> {
            for (int iter = 0; locs != null && iter < locs.size(); iter++) {
                LocationDto l = locs.get(iter);
                places.put(l.id(), l.name());
            }
            Api.pos().registers(store.id(), Net.to(regs -> {
                page.removeComponent(page.getComponentAt(2));
                int shown = 0;
                String lastPlace = null;
                for (int iter = 0; regs != null && iter < regs.size(); iter++) {
                    final PosRegisterDto r = regs.get(iter);
                    if (!r.active()) {
                        continue;
                    }
                    String place = places.get(r.locationId());
                    place = place == null ? "" : place;
                    if (!place.equals(lastPlace)) {
                        page.add(PosUi.data(place, "PosGroup"));
                        lastPlace = place;
                    }
                    Button b = PosUi.icon(FontImage.MATERIAL_POINT_OF_SALE, r.name(),
                            "pos-register-" + iter, "PosChoice", e -> {
                                PosState.useRegister(store.id(), store.name(), store.handle(),
                                        store.currency(), r.id(), r.name(), r.locationId());
                                PosState.refreshCatalog(PosHome::prewarm);
                                next();
                            });
                    b.setShouldLocalize(false);
                    page.add(b);
                    shown++;
                }
                if (shown == 0) {
                    page.add(Ui.empty(FontImage.MATERIAL_POINT_OF_SALE, "No registers yet",
                            "An owner or manager adds registers in the store's settings."));
                }
                page.revalidateLater();
            }, Ui::fail));
        }, Ui::fail));
    }

    // -------------------------------------------------------------- PIN

    private Container pin() {
        Container page = new Container(new BorderLayout());
        page.setUIID("PosPage");
        page.add(BorderLayout.NORTH, PosUi.header("Sign in to the register", null));
        Container box = new Container(BoxLayout.y());
        box.setUIID("PosPinBox");
        box.add(PosUi.data(PosState.storeName() + Ui.DOT + PosState.registerName(), "PosMuted"));
        final Label dots = new Label("", "PosPinDots");
        dots.setShouldLocalize(false);
        dots.setName("pos-pin-dots");
        final Label message = new Label("", "PosError");
        message.setName("pos-pin-message");
        final TextField who = new TextField("", Lang.tr("Staff e-mail (blank for you)"), 20,
                TextField.EMAILADDR);
        who.setUIID("PosField");
        who.setName("pos-pin-user");
        final StringBuilder digits = new StringBuilder();
        final Runnable show = () -> {
            StringBuilder d = new StringBuilder();
            for (int iter = 0; iter < 6; iter++) {
                d.append(iter < digits.length() ? "\u25cf " : "\u25cb ");
            }
            dots.setText(d.toString().trim());
            dots.getParent().revalidateLater();
        };
        final Runnable submit = () -> {
            if (digits.length() < 4) {
                message.setText(Lang.tr("A PIN has 4 to 6 digits"));
                return;
            }
            String pin = digits.toString();
            digits.setLength(0);
            show.run();
            message.setText(Lang.tr("Checking..."));
            Api.pos().signIn(PosState.storeId(), PosState.registerId(),
                    new PinSignInDto(who.getText().trim(), pin), Net.to(s -> {
                        PosState.setSession(s);
                        next();
                    }, (status, text) -> {
                        message.setText(status == 423
                                ? Lang.tr("Too many wrong PINs. Try again in 15 minutes.")
                                : status == 401 || status == 403 ? Lang.tr("That PIN is not right")
                                : status <= 0 ? Lang.tr("The register is offline. A PIN needs the network.")
                                : Lang.tr(text));
                        message.getParent().revalidateLater();
                    }));
        };
        box.add(dots);
        box.add(message);
        Container pad = PosUi.pad("pos-pin", "OK", c -> {
            if (c == '\b') {
                if (digits.length() > 0) {
                    digits.setLength(digits.length() - 1);
                }
            } else if (c == '\n') {
                submit.run();
                return;
            } else if (digits.length() < 6) {
                digits.append(c);
            }
            message.setText("");
            show.run();
        });
        box.add(pad);
        box.add(who);
        Container links = new Container(new FlowLayout(Component.CENTER));
        links.add(Ui.link("Use another register", "pos-change-register", e -> {
            PosState.forgetRegister();
            next();
        }));
        box.add(links);
        Container center = new Container(new FlowLayout(Component.CENTER));
        center.setScrollableY(true);
        center.setScrollVisible(false);
        center.add(box);
        page.add(BorderLayout.CENTER, center);
        dots.setText("\u25cb \u25cb \u25cb \u25cb \u25cb \u25cb");
        return page;
    }

    // -------------------------------------------------------------- float

    private Container openShift() {
        Container page = new Container(new BorderLayout());
        page.setUIID("PosPage");
        PosSessionDto s = PosState.session();
        page.add(BorderLayout.NORTH, PosUi.header("Open a shift", Ui.link("Sign out",
                "pos-signout", e -> {
                    PosState.setSession(null);
                    next();
                })));
        Container box = new Container(BoxLayout.y());
        box.setUIID("PosPinBox");
        box.add(PosUi.data(Lang.tr("Hello, {0}", s == null ? "" : s.displayName()), "PosTitle"));
        box.add(PosUi.label("Count the float into the drawer", "PosMuted"));
        final long[] cents = {0};
        final Label amount = PosUi.data(PosState.money(0), "PosAmount");
        amount.setName("pos-float");
        box.add(amount);
        box.add(PosUi.pad("pos-float", "00", c -> {
            if (c == '\n') {
                cents[0] = PosUi.typed(PosUi.typed(cents[0], '0'), '0');
            } else {
                cents[0] = PosUi.typed(cents[0], c);
            }
            amount.setText(PosState.money(cents[0]));
            amount.getParent().revalidateLater();
        }));
        final Button open = PosUi.icon(FontImage.MATERIAL_LOCK_OPEN, "Open shift", "pos-open-shift",
                "PosPay", null);
        open.addActionListener(e -> {
            open.setEnabled(false);
            Api.pos().openShift(PosState.storeId(), PosState.registerId(),
                    new OpenShiftDto(cents[0], ""), Net.to(shift -> {
                        PosState.setShift(shift);
                        next();
                    }, (status, text) -> {
                        open.setEnabled(true);
                        Ui.fail(status, status <= 0 ? "Opening a shift needs the network" : text);
                    }));
        });
        // The button stays pinned under the pad, outside the scrolling area,
        // so a short phone screen can never push it out of reach.
        Container center = new Container(new FlowLayout(Component.CENTER));
        center.setScrollableY(true);
        center.setScrollVisible(false);
        center.add(box);
        page.add(BorderLayout.CENTER, center);
        Container south = new Container(new BorderLayout());
        south.setUIID("PosPinFooter");
        south.add(BorderLayout.CENTER, open);
        page.add(BorderLayout.SOUTH, south);
        return page;
    }

    /// Readies every product photo of the catalog while the cashier enters
    /// the PIN and counts the float, so the product grid opens on photos.
    /// Drawn placeholder tiles showed for a second before them otherwise.
    private static void prewarm() {
        com.codenameone.examples.bazaar.api.PosCatalogDto c = PosState.catalog();
        if (c == null || c.items() == null) {
            return;
        }
        for (int iter = 0; iter < c.items().size(); iter++) {
            com.codenameone.examples.bazaar.ui.ProductArt.prefetch(c.items().get(iter).photoId());
        }
    }
}
