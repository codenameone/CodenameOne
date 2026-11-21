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
import com.codename1.mapping.Mappers;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.ParkedCartDto;
import com.codenameone.examples.bazaar.api.PosCatalogDto;
import com.codenameone.examples.bazaar.api.PosCustomerDto;
import com.codenameone.examples.bazaar.api.PosItemDto;
import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;
import java.util.UUID;

/// Parked sales: a cart put aside while the next customer is served, and
/// picked up again later. Parking, resuming and discarding all happen on the
/// device first ([ParkedCarts]) and reach the server when it answers, so the
/// register parks and resumes with the network down.
final class ParkedPane {
    private static boolean sending;

    private ParkedPane() {
    }

    /// Asks for a label and parks the current cart under it.
    static void parkCurrent() {
        final PosCart cart = PosState.cart();
        if (cart.isEmpty()) {
            return;
        }
        TextField label = new TextField("", Lang.tr("Label, e.g. a name"), 20, TextField.ANY);
        label.setUIID("PosField");
        label.setName("pos-park-label");
        Command park = new Command(Lang.tr("Park"));
        if (Dialog.show(Lang.tr("Park this sale"), label, park, new Command(Lang.tr("Cancel")))
                != park) {
            return;
        }
        String text = label.getText().trim();
        if (text.length() == 0) {
            text = Lang.tr("Sale of {0} items", cart.units());
        }
        ParkedCartDto dto = new ParkedCartDto("", UUID.randomUUID().toString(),
                cut(text, 80), cart.customer() == null ? "" : cart.customer().id(),
                PosState.staffUsername(), cut(cart.note(), 500), cart.uploadLines(),
                ParkedCarts.PARKED, System.currentTimeMillis());
        PosState.parked().park(dto, cart.customer() == null ? null
                : Mappers.toJson(cart.customer()));
        cart.clear();
        PosState.fireCart();
        PosState.fireStatus();
        sync();
    }

    static Component create() {
        final Container root = new Container(new BorderLayout());
        root.setName("pos-parked");
        root.setUIID("PosRoot");
        root.add(BorderLayout.NORTH, PosUi.header("Parked sales", null));
        final Container page = new Container(BoxLayout.y());
        page.setScrollableY(true);
        page.setUIID("PosPage");
        root.add(BorderLayout.CENTER, page);
        fill(page);
        PosUi.live(root, () -> fill(page), false);
        sync();
        return root;
    }

    private static void fill(final Container page) {
        page.removeAll();
        List<ParkedCarts.Entry> held = PosState.parked().held();
        if (held.isEmpty()) {
            page.add(Ui.empty(FontImage.MATERIAL_PAUSE_CIRCLE, "Nothing parked",
                    "Park a sale from the cart to serve the next customer."));
        }
        for (int iter = 0; iter < held.size(); iter++) {
            final ParkedCarts.Entry e = held.get(iter);
            ParkedCartDto c = e.cart;
            Container row = new Container(new BorderLayout());
            row.setUIID("PosCartRow");
            row.setName("pos-parked-" + iter);
            Container text = new Container(BoxLayout.y());
            text.add(PosUi.data(c.label(), "PosCartTitle"));
            text.add(PosUi.data(Lang.tr("{0} items", units(c)) + Ui.DOT + PosState.money(total(c))
                    + (e.parkSent ? "" : Ui.DOT + Lang.tr("not synced")), "PosCartDetail"));
            row.add(BorderLayout.CENTER, text);
            Container acts = new Container(new GridLayout(1, 2));
            acts.add(PosUi.icon(FontImage.MATERIAL_PLAY_ARROW, "Resume", "pos-parked-resume-" + iter,
                    "PosChoice", ev -> resume(e)));
            acts.add(PosUi.icon(FontImage.MATERIAL_DELETE, "Discard", "pos-parked-discard-" + iter,
                    "PosChoice", ev -> {
                        if (Dialog.show(Lang.tr("Discard"), Lang.tr("Discard this parked sale?"),
                                Lang.tr("Discard"), Lang.tr("Keep"))) {
                            PosState.parked().discard(e.clientId());
                            PosState.fireStatus();
                            sync();
                        }
                    }));
            row.add(BorderLayout.EAST, acts);
            page.add(row);
        }
        page.revalidateLater();
    }

    private static void resume(ParkedCarts.Entry e) {
        PosCart cart = PosState.cart();
        if (!cart.isEmpty() && !Dialog.show(Lang.tr("Resume"),
                Lang.tr("The sale on the register is replaced. Resume anyway?"), Lang.tr("Resume"),
                Lang.tr("Keep"))) {
            return;
        }
        ParkedCartDto c = PosState.parked().resume(e.clientId());
        if (c == null) {
            return;
        }
        restore(cart, c, PosState.catalog());
        cart.setCustomer(customerOf(PosState.parked().customerOf(c.clientId())));
        PosState.fireCart();
        PosState.fireStatus();
        sync();
        PosSections.show(com.codenameone.examples.bazaar.ui.Sections.REGISTER);
    }

    /// Fills `cart` with the parked lines; a line whose item left the
    /// catalog comes back as a custom line at its parked price.
    static void restore(PosCart cart, ParkedCartDto c, PosCatalogDto catalog) {
        cart.clear();
        cart.setNote(c.note());
        for (int iter = 0; c.lines() != null && iter < c.lines().size(); iter++) {
            PosLineDto l = c.lines().get(iter);
            PosItemDto item = find(catalog, l.variantId());
            if (item == null) {
                String title = l.title() == null || l.title().length() == 0 ? Lang.tr("Item")
                        : l.title();
                cart.add(PosCart.customItem(title, l.unitCents(), l.taxable()), l.quantity());
                continue;
            }
            int before = cart.items().size();
            cart.add(item, l.quantity());
            if (l.overridden() && cart.items().size() > before) {
                cart.linePrice(cart.items().size() - 1, l.unitCents());
            }
        }
    }

    /// The customer stored with a hold, or null when it was for nobody or
    /// was parked by a register that did not store one.
    static PosCustomerDto customerOf(String text) {
        if (text == null || text.length() == 0) {
            return null;
        }
        try {
            return Mappers.fromJson(text, PosCustomerDto.class);
        } catch (RuntimeException bad) {
            return null;
        }
    }

    private static PosItemDto find(PosCatalogDto catalog, String variantId) {
        if (catalog == null || catalog.items() == null || variantId == null
                || variantId.length() == 0) {
            return null;
        }
        for (int iter = 0; iter < catalog.items().size(); iter++) {
            if (variantId.equals(catalog.items().get(iter).variantId())) {
                return catalog.items().get(iter);
            }
        }
        return null;
    }

    static int units(ParkedCartDto c) {
        int n = 0;
        for (int iter = 0; c.lines() != null && iter < c.lines().size(); iter++) {
            n += c.lines().get(iter).quantity();
        }
        return n;
    }

    static long total(ParkedCartDto c) {
        long n = 0;
        for (int iter = 0; c.lines() != null && iter < c.lines().size(); iter++) {
            n += c.lines().get(iter).unitCents() * c.lines().get(iter).quantity();
        }
        return n;
    }

    // ---------------------------------------------------------------- sync
    /// Sends the next parked cart change the server has not heard of, one
    /// at a time; [PosSync] calls this on its tick, so changes made offline
    /// go out once the network is back.
    static void sync() {
        if (sending || !PosState.hasRegister() || Api.posTools() == null) {
            return;
        }
        final ParkedCarts carts = PosState.parked();
        List<ParkedCarts.Entry> due = carts.due();
        if (due.isEmpty()) {
            return;
        }
        final ParkedCarts.Entry e = due.get(0);
        final String what = e.due();
        sending = true;
        String store = PosState.storeId();
        String reg = PosState.registerId();
        if ("PARK".equals(what)) {
            Api.posTools().park(store, reg, e.cart, Net.to(server -> {
                sending = false;
                carts.parked(e.clientId(), server);
                PosState.fireStatus();
                sync();
            }, (status, message) -> failed(carts, e, status)));
        } else {
            Net.Ok<ParkedCartDto> ok = server -> {
                sending = false;
                carts.moved(e.clientId());
                PosState.fireStatus();
                sync();
            };
            if ("RESUME".equals(what)) {
                Api.posTools().resume(store, reg, e.serverId, Net.to(ok,
                        (status, message) -> failed(carts, e, status)));
            } else {
                Api.posTools().discard(store, reg, e.serverId, Net.to(ok,
                        (status, message) -> failed(carts, e, status)));
            }
        }
    }

    private static void failed(ParkedCarts carts, ParkedCarts.Entry e, int status) {
        sending = false;
        if (status == 409 || status == 404 || status == 400) {
            // The server will never take this one (already moved elsewhere,
            // or gone): stop sending it so the queue behind it moves.
            if (!e.parkSent) {
                carts.parked(e.clientId(), new ParkedCartDto("", e.clientId(), "", "", "", "",
                        null, ParkedCarts.DISCARDED, 0));
            } else {
                carts.refused(e.clientId(), "");
            }
            PosState.fireStatus();
        }
    }

    private static String cut(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
