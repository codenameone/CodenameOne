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
package com.codenameone.examples.savor.pay;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.savor.Prefs;
import com.codenameone.examples.savor.Telemetry;
import com.codenameone.examples.savor.api.PaymentMethodDto;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Ui;
import java.util.List;

/// The ways the customer can pay: their cards, and cash, which is always there.
///
/// The list is the server's. A card is added by the payment provider and what
/// comes back is a brand and four digits; the number itself is never kept by
/// this app, and by the server only long enough to hand it over.
///
/// The same screen answers "which of these, for this delivery?": opened with
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
                : "Choose how to pay for this delivery.", "SvMuted"));
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

    /// The wallet, to choose from for one delivery.
    ///
    /// @param chosen the id of the method the delivery has now
    public static void pick(Form previous, String chosen, Picked picked) {
        WalletForm wallet = new WalletForm(previous, chosen, picked);
        wallet.form.show();
        wallet.load();
    }

    /// The name of a method, as a customer would say it.
    public static String label(PaymentMethodDto method) {
        if (method == null || "cash".equals(method.kind())) {
            return Lang.tr("Cash");
        }
        return method.label() == null || method.label().length() == 0
                ? method.brand() + " " + method.last4() : method.label();
    }

    private void load() {
        Api.payments().methods(Net.to(this::fill, Ui::fail));
    }

    private void fill(List<PaymentMethodDto> methods) {
        list.removeAll();
        for (int iter = 0; methods != null && iter < methods.size(); iter++) {
            final PaymentMethodDto method = methods.get(iter);
            boolean cash = "cash".equals(method.kind());
            boolean marked = picked == null ? method.isDefault() : method.id().equals(chosen);
            Container row = new Container(new BorderLayout());
            row.setUIID(marked ? "SvOptionPicked" : "SvOption");
            Label icon = new Label("", "SvOptionIcon");
            Ui.icon(icon, cash ? FontImage.MATERIAL_PAYMENTS
                    : FontImage.MATERIAL_CREDIT_CARD, 4f);
            row.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(icon));
            Container lines = new Container(BoxLayout.y());
            lines.add(Ui.plain(label(method), "SvRowTitle"));
            lines.add(Ui.plain(cash ? Lang.tr("Pay the courier directly")
                    : Lang.tr("Expires {0}", expiry(method)), "SvRowDetail"));
            row.add(BorderLayout.CENTER, lines);
            Container end = new Container(new FlowLayout(Label.RIGHT, Label.CENTER));
            if (method.isDefault()) {
                end.add(Ui.badge("Default", "SvBadgeGood"));
            }
            row.add(BorderLayout.EAST, end);
            Ui.tap(row, cash ? "method-cash" : "method-" + iter, e -> choose(method));
            list.add(row);
            if (!cash && picked == null) {
                // Its own line under the card, so that a tap meant for the card
                // is never a tap on Remove.
                Button remove = Ui.link("Remove this card", "remove-" + iter, e ->
                        Api.payments().remove(method.id(), Net.to(gone -> {
                            Telemetry.cardRemoved();
                            if (method.id().equals(Prefs.paymentMethod())) {
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
        Api.payments().makeDefault(method.id(), Net.to(made -> {
            Prefs.setPaymentMethod(method.id());
            load();
        }, Ui::fail));
    }

    private static String expiry(PaymentMethodDto method) {
        int year = method.expYear() % 100;
        return (method.expMonth() < 10 ? "0" : "") + method.expMonth() + "/"
                + (year < 10 ? "0" : "") + year;
    }
}
