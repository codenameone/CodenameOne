/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.pay;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.wayline.Prefs;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.PaymentMethodDto;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.Ui;
import java.util.List;

/// The ways the rider can pay: their cards, and cash, which is always there.
///
/// The list is the server's. A card is added by the payment provider and what
/// comes back is a brand and four digits; the number itself is never kept by
/// this app, and by the server only long enough to hand it over.
///
/// The same screen answers "which of these, for this ride?": opened with
/// [#pick], a tap on a method chooses it and goes back.
public final class WalletForm {
    public interface Picked {
        void picked(PaymentMethodDto method);
    }

    private final Form form;
    private final Form previous;
    private final Picked picked;
    private final String chosen;
    private final Container list = new Container(BoxLayout.y());

    private WalletForm(Form previous, String chosen, Picked picked) {
        this.previous = previous;
        this.picked = picked;
        this.chosen = chosen;
        form = Ui.form(picked == null ? "Wallet" : "Pay with", "Wallet");
        Ui.back(form, previous);
        Container page = Ui.page();
        page.add(Ui.text(picked == null
                ? "Tap a card to make it the one that pays unless you say otherwise."
                : "Choose how to pay for this ride.", "WlMuted"));
        list.setName("methods");
        page.add(list);
        page.add(Ui.secondary("Add a card", "addCard",
                e -> CardForm.show(form, () -> {
                    form.showBack();
                    load();
                })));
        form.add(BorderLayout.CENTER, page);
    }

    /// The wallet, to look after.
    public static void show(Form previous) {
        WalletForm wallet = new WalletForm(previous, null, null);
        wallet.form.show();
        wallet.load();
    }

    /// The wallet, to choose from for one ride.
    ///
    /// @param chosen the id of the method the ride has now
    public static void pick(Form previous, String chosen, Picked picked) {
        WalletForm wallet = new WalletForm(previous, chosen, picked);
        wallet.form.show();
        wallet.load();
    }

    /// The name of a method, as a rider would say it.
    public static String label(PaymentMethodDto method) {
        if (method == null || "cash".equals(method.kind)) {
            return Lang.tr("Cash");
        }
        return method.label == null || method.label.length() == 0
                ? method.brand + " " + method.last4 : method.label;
    }

    private void load() {
        Api.payments().methods(Net.to(this::fill, Ui::fail));
    }

    private void fill(List<PaymentMethodDto> methods) {
        list.removeAll();
        for (int iter = 0; methods != null && iter < methods.size(); iter++) {
            final PaymentMethodDto method = methods.get(iter);
            boolean cash = "cash".equals(method.kind);
            boolean marked = picked == null ? method.isDefault : method.id.equals(chosen);
            Container row = new Container(new BorderLayout());
            row.setUIID(marked ? "WlOptionPicked" : "WlOption");
            Label icon = new Label("", "WlOptionIcon");
            Ui.icon(icon, cash ? FontImage.MATERIAL_PAYMENTS
                    : FontImage.MATERIAL_CREDIT_CARD, 4f);
            row.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(icon));
            Container lines = new Container(BoxLayout.y());
            lines.add(Ui.plain(label(method), "WlRowTitle"));
            lines.add(Ui.plain(cash ? Lang.tr("Pay the driver directly")
                    : Lang.tr("Expires {0}", expiry(method)), "WlRowDetail"));
            row.add(BorderLayout.CENTER, lines);
            Container end = new Container(new FlowLayout(Label.RIGHT, Label.CENTER));
            if (method.isDefault) {
                end.add(Ui.badge("Default", "WlBadgeGood"));
            }
            row.add(BorderLayout.EAST, end);
            Ui.tap(row, cash ? "method-cash" : "method-" + iter, e -> choose(method));
            list.add(row);
            if (!cash && picked == null) {
                // Its own line under the card, so that a tap meant for the card
                // is never a tap on Remove.
                Button remove = Ui.link("Remove this card", "remove-" + iter, e ->
                        Api.payments().remove(method.id, Net.to(gone -> {
                            Telemetry.cardRemoved();
                            if (method.id.equals(Prefs.paymentMethod())) {
                                Prefs.setPaymentMethod(Prefs.PAYMENT_CASH);
                            }
                            load();
                        }, Ui::fail)));
                list.add(FlowLayout.encloseRight(remove));
            }
        }
        Ui.refresh(form);
    }

    private void choose(final PaymentMethodDto method) {
        if (picked != null) {
            previous.showBack();
            picked.picked(method);
            return;
        }
        Api.payments().makeDefault(method.id, Net.to(made -> {
            Prefs.setPaymentMethod(method.id);
            load();
        }, Ui::fail));
    }

    private static String expiry(PaymentMethodDto method) {
        int year = method.expYear % 100;
        return (method.expMonth < 10 ? "0" : "") + method.expMonth + "/"
                + (year < 10 ? "0" : "") + year;
    }
}
