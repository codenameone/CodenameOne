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
package com.codenameone.examples.reel.viewer;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.reel.Prefs;
import com.codenameone.examples.reel.Telemetry;
import com.codenameone.examples.reel.api.InvoiceDto;
import com.codenameone.examples.reel.api.PlanDto;
import com.codenameone.examples.reel.api.SubscribeDto;
import com.codenameone.examples.reel.api.SubscriptionDto;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.net.Session;
import com.codenameone.examples.reel.pay.WalletForm;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Ui;

/// The plans, the account's subscription, and changing it: subscribe (with a
/// coupon), switch plan, cancel at period end, resume. A first plan with a
/// trial needs a card on file all the same; the server charges it when the
/// trial ends.
public final class PlansForm {
    private PlansForm() {
    }

    public static void show(Form previous) {
        final Form form = Ui.form("Plans", "Plans");
        Ui.back(form, previous);
        final Container page = Ui.list("plans");
        form.add(BorderLayout.CENTER, page);
        form.show();
        load(form, page);
    }

    private static void load(final Form form, final Container page) {
        Api.billing().subscription(Net.to(sub -> Api.billing().plans(Net.to(plans -> {
            page.removeAll();
            Container top = Ui.page();
            if (sub != null && sub.planCode() != null && sub.planCode().length() > 0) {
                top.add(current(form, page, sub));
            }
            final TextField coupon = Ui.field(top, "Coupon", "coupon", "Optional");
            page.add(top);
            for (int iter = 0; plans != null && iter < plans.size(); iter++) {
                page.add(plan(form, page, plans.get(iter), sub, coupon));
            }
            page.revalidate();
        }, Ui.failed(page, () -> load(form, page)))), Ui.failed(page, () -> load(form, page))));
    }

    private static Container current(final Form form, final Container page, final SubscriptionDto sub) {
        Container box = Ui.card();
        box.add(Ui.label(sub.planName(), "SvTitle"));
        String state = "TRIALING".equals(sub.status()) ? Lang.tr("Trial until {0}", Ui.day(sub.periodEndsAt()))
                : sub.cancelAtPeriodEnd() ? Lang.tr("Ends {0}", Ui.day(sub.periodEndsAt()))
                : "PAST_DUE".equals(sub.status()) ? Lang.tr("Payment failed; we will retry")
                : Lang.tr("Renews {0}", Ui.day(sub.periodEndsAt()));
        box.add(Ui.label(state + Ui.DOT + Ui.money(sub.priceCents(), sub.currency()), "SvMuted"));
        if (sub.discountPercent() > 0) {
            box.add(Ui.badge(Lang.tr("{0}% off", String.valueOf(sub.discountPercent())), "SvBadgeGood"));
        }
        if ("CANCELED".equals(sub.status())) {
            return box;
        }
        if (sub.cancelAtPeriodEnd()) {
            box.add(Ui.secondary("Keep my plan", "resume", e -> Api.billing().resume(Net.to(s -> load(form, page),
                    Ui::fail))));
        } else {
            box.add(Ui.link("Cancel at the end of the period", "cancel", e -> Api.billing().cancel(
                    Net.to(s -> load(form, page), Ui::fail))));
        }
        return box;
    }

    private static Container plan(final Form form, final Container page, final PlanDto p, SubscriptionDto sub,
            final TextField coupon) {
        Container box = Ui.card();
        box.setName("plan-" + p.code());
        box.add(Ui.label(p.name(), "SvTitle"));
        box.add(Ui.label(Ui.money(p.priceCents(), p.currency()) + " / " + Lang.tr(p.interval()), "SvText"));
        box.add(Ui.text(p.description(), "SvMuted"));
        box.add(Ui.row(FontImage.MATERIAL_HD, Lang.tr("Up to {0}p", p.maxQuality()), null, null));
        box.add(Ui.row(FontImage.MATERIAL_DEVICES, Lang.tr("{0} screens at once", String.valueOf(p.maxStreams())),
                null, null));
        if (p.downloads()) {
            box.add(Ui.row(FontImage.MATERIAL_DOWNLOAD, "Downloads", null, null));
        }
        if (p.adSupported()) {
            box.add(Ui.row(FontImage.MATERIAL_CAMPAIGN, "With ads", null, null));
        }
        boolean mine = sub != null && p.code().equals(sub.planCode()) && !"CANCELED".equals(sub.status());
        if (mine) {
            box.add(Ui.badge("Your plan", "SvBadgeGood"));
            return box;
        }
        String label = p.trialDays() > 0 && (sub == null || sub.planCode() == null || sub.planCode().length() == 0)
                ? Lang.tr("Start {0}-day trial", String.valueOf(p.trialDays())) : "Choose";
        final SpanLabel error = Ui.text("", "SvError");
        box.add(error);
        box.add(Ui.primary(label, "subscribe-" + p.code(), e -> {
            String card = Prefs.paymentMethod();
            Api.billing().subscribe(new SubscribeDto(p.code(), coupon.getText().trim(),
                    card.length() == 0 ? null : card), Net.to(s -> {
                        Telemetry.subscribed(p.code(), "TRIALING".equals(s.status()));
                        Session.refreshUser(() -> load(form, page), Ui::fail);
                    }, (status, message) -> {
                        error.setText(message);
                        page.revalidate();
                        if (status == 400 && message != null && message.indexOf("card") >= 0) {
                            WalletForm.show(form);
                        }
                    }));
        }));
        return box;
    }

    /// Every charge and refund on the account.
    public static void invoices(Form previous) {
        final Form form = Ui.form("Billing history", "Invoices");
        Ui.back(form, previous);
        final Container page = Ui.list("invoices");
        form.add(BorderLayout.CENTER, page);
        form.show();
        Api.billing().invoices(Net.to(list -> {
            page.removeAll();
            if (list == null || list.isEmpty()) {
                page.add(Ui.empty(FontImage.MATERIAL_RECEIPT_LONG, "No invoices yet", null));
            }
            for (int iter = 0; list != null && iter < list.size(); iter++) {
                InvoiceDto i = list.get(iter);
                page.add(Ui.row(FontImage.MATERIAL_RECEIPT, i.description(), Ui.day(i.createdAt()) + Ui.DOT
                        + Lang.tr(i.status()), Ui.label(Ui.money(i.amountCents(), i.currency()), "SvRowValue")));
            }
            page.revalidate();
        }, Ui.failed(page, null)));
    }
}
