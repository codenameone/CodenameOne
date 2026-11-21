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
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.LayawayCancelDto;
import com.codenameone.examples.bazaar.api.LayawayDto;
import com.codenameone.examples.bazaar.api.LayawayPaymentDto;
import com.codenameone.examples.bazaar.api.LayawayRequestDto;
import com.codenameone.examples.bazaar.api.PosPolicyDto;
import com.codenameone.examples.bazaar.api.PosTenderDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.UUID;

/// Layaways: the sale on the register put aside for its customer on a
/// deposit, paid off over time, and handed over once the balance is zero.
/// Every step needs the network: the goods are reserved on the server.
final class LayawayPane {
    private LayawayPane() {
    }

    static Component create() {
        final Container root = new Container(new BorderLayout());
        root.setName("pos-layaways");
        root.setUIID("PosRoot");
        root.add(BorderLayout.NORTH, PosUi.header("Layaways", null));
        final Container page = new Container(BoxLayout.y());
        page.setScrollableY(true);
        page.setUIID("PosPage");
        root.add(BorderLayout.CENTER, page);
        list(page);
        return root;
    }

    private static void list(final Container page) {
        page.removeAll();
        page.add(PosUi.icon(FontImage.MATERIAL_SAVINGS, "Lay away the current sale",
                "pos-layaway-new", "PosPay", e -> start(page)));
        page.add(PosUi.label("Looking...", "PosMuted"));
        page.revalidateLater();
        Api.layaways().layaways(PosState.storeId(), Net.to(list -> {
            page.removeComponent(page.getComponentAt(1));
            for (int iter = 0; list != null && iter < list.size(); iter++) {
                final LayawayDto l = list.get(iter);
                Container row = new Container(new BorderLayout());
                row.setUIID("PosCartRow");
                row.add(BorderLayout.CENTER, PosUi.data(Lang.tr("Order #{0}", l.orderNumber())
                        + Ui.DOT + l.status(), "PosCartTitle"));
                row.add(BorderLayout.EAST, PosUi.data(Lang.tr("Owes {0}",
                        PosState.money(PosRules.balance(l.totalCents(), l.paidCents()))),
                        "PosCartDetail"));
                com.codename1.ui.Button lead = new com.codename1.ui.Button();
                lead.setName("pos-layaway-" + iter);
                lead.addActionListener(e -> show(page, l));
                row.add(BorderLayout.SOUTH, lead);
                lead.setHidden(true);
                row.setLeadComponent(lead);
                page.add(row);
            }
            if (list == null || list.isEmpty()) {
                page.add(Ui.empty(FontImage.MATERIAL_SAVINGS, "No layaways",
                        "Add a customer to a sale and lay it away on a deposit."));
            }
            page.revalidateLater();
        }, (s, m) -> Ui.fail(s, s <= 0 ? "Layaways need the network" : m)));
    }

    /// Lays the cart away: a customer, and a deposit of at least the
    /// policy's share of the total.
    private static void start(final Container page) {
        final PosCart cart = PosState.cart();
        if (cart.isEmpty()) {
            Ui.say("Ring up the items first");
            return;
        }
        if (cart.customer() == null) {
            Ui.say("A layaway is for a customer: add one to the sale");
            return;
        }
        final long total = PosState.totals().totalCents;
        PosPolicyDto p = PosState.policy();
        final long min = PosRules.minimumDeposit(total, p == null ? 0 : p.layawayDepositBp());
        SellScreen.askAmount(Lang.tr("Deposit, at least {0}", PosState.money(min)), false, cents -> {
            if (cents < Math.max(1, min) || cents > total) {
                Ui.say(Lang.tr("The deposit is between {0} and {1}", PosState.money(Math.max(1,
                        min)), PosState.money(total)));
                return;
            }
            String kind = pickKind();
            if (kind == null) {
                return;
            }
            LayawayRequestDto req = new LayawayRequestDto(UUID.randomUUID().toString(),
                    PosState.shiftId(), cart.customer().id(), cart.uploadLines(),
                    new PosTenderDto(kind, cents, ""), -1);
            Api.layaways().create(PosState.storeId(), PosState.registerId(), req, Net.to(l -> {
                cart.clear();
                PosState.fireCart();
                PosState.refreshCatalog(null);
                show(page, l);
            }, (s, m) -> Ui.fail(s, s <= 0 ? "Layaways need the network" : m)));
        });
    }

    private static String pickKind() {
        return Dialog.show(Lang.tr("Paid by"), Lang.tr("How is it paid?"), Lang.tr("Cash"),
                Lang.tr("Card")) ? Tenders.CASH : Tenders.CARD;
    }

    /// The balance of one layaway, its payments, and what can be done next.
    static void show(final Container page, final LayawayDto l) {
        page.removeAll();
        page.add(PosUi.data(Lang.tr("Order #{0}", l.orderNumber()) + Ui.DOT + l.status(),
                "PosTitle"));
        long balance = PosRules.balance(l.totalCents(), l.paidCents());
        page.add(Ui.fact("Total", PosState.money(l.totalCents())));
        page.add(Ui.fact("Paid", PosState.money(l.paidCents()) + "  ("
                + PosRules.percentPaid(l.totalCents(), l.paidCents()) + "%)"));
        page.add(Ui.fact("Balance", PosState.money(balance)));
        for (int iter = 0; l.payments() != null && iter < l.payments().size(); iter++) {
            LayawayPaymentDto p = l.payments().get(iter);
            page.add(PosUi.data(TenderSheet.kindName(p.kind()) + Ui.DOT
                    + PosState.money(p.amountCents()), "PosCartDetail"));
        }
        boolean open = "OPEN".equalsIgnoreCase(l.status());
        if (open && balance > 0) {
            Container acts = new Container(new GridLayout(1, 2));
            acts.add(PosUi.icon(FontImage.MATERIAL_PAYMENTS, "Take a payment", "pos-layaway-pay",
                    "PosChoice", e -> pay(page, l)));
            acts.add(PosUi.icon(FontImage.MATERIAL_CANCEL, "Cancel", "pos-layaway-cancel",
                    "PosChoice", e -> cancel(page, l)));
            page.add(acts);
        }
        page.add(Ui.link("All layaways", "pos-layaway-back", e -> list(page)));
        page.revalidateLater();
    }

    private static void pay(final Container page, final LayawayDto l) {
        final long owed = PosRules.balance(l.totalCents(), l.paidCents());
        SellScreen.askAmount(Lang.tr("Payment, up to {0}", PosState.money(owed)), false, cents -> {
            long amount = PosRules.payment(cents, l.totalCents(), l.paidCents());
            if (amount <= 0) {
                return;
            }
            String kind = pickKind();
            Api.layaways().pay(PosState.storeId(), l.id(), new PosTenderDto(kind, amount, ""),
                    Net.to(fresh -> show(page, fresh), (s, m) -> Ui.fail(s, s <= 0
                            ? "Layaways need the network" : m)));
        });
    }

    private static void cancel(final Container page, final LayawayDto l) {
        long back = PosRules.cancelRefund(l.totalCents(), l.paidCents(), l.cancelFeeBp());
        if (!Dialog.show(Lang.tr("Cancel layaway"), Lang.tr("Give back {0} in cash and return "
                + "the items to stock?", PosState.money(back)), Lang.tr("Cancel layaway"),
                Lang.tr("Keep"))) {
            return;
        }
        Api.layaways().cancel(PosState.storeId(), l.id(), new LayawayCancelDto(PosState.shiftId(),
                Tenders.CASH, "Cancelled at the counter"), Net.to(fresh -> {
                    PosState.refreshCatalog(null);
                    show(page, fresh);
                }, (s, m) -> Ui.fail(s, s <= 0 ? "Layaways need the network" : m)));
    }
}
