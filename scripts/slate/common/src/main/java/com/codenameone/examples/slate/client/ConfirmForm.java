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
package com.codenameone.examples.slate.client;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.slate.Telemetry;
import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.CancelDto;
import com.codenameone.examples.slate.api.ConfirmDto;
import com.codenameone.examples.slate.api.PaymentMethodDto;
import com.codenameone.examples.slate.api.PolicyDto;
import com.codenameone.examples.slate.api.ServiceDto;
import com.codenameone.examples.slate.net.Api;
import com.codenameone.examples.slate.net.Net;
import com.codenameone.examples.slate.pay.WalletForm;
import com.codenameone.examples.slate.ui.Lang;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Ui;

import java.util.List;

/// The last step: a held start, what it costs, and how it is paid.
///
/// The start is the client's for as long as the hold lasts -- ten minutes by
/// default -- and nobody else can book it meanwhile. Leaving the screen lets
/// it go at once, so a client who changes their mind does not keep a slot
/// from the next one for the rest of the hold.
public final class ConfirmForm {
    private final Form form;
    private final AppointmentDto held;
    private final Label countdown = new Label("", "SvCaption");
    private final Label method = new Label("", "SvText");
    private final Container page = Ui.page();
    private PaymentMethodDto chosen;
    private PolicyDto policy;
    private UITimer ticking;
    private boolean done;

    private ConfirmForm(final Form previous, AppointmentDto held, ServiceDto service) {
        this.held = held;
        form = Ui.form("Confirm", "Confirm");
        Ui.back(form, () -> {
            letGo();
            previous.showBack();
        });
        countdown.setName("countdown");
        page.add(Ui.label(held.serviceName(), "SvTitle"));
        page.add(Ui.text(Ui.weekday(held.start()) + Ui.DOT + Times.clock(held.start()) + " - "
                + Times.clock(held.end()), "SvHeading"));
        String who = Appointments.with(held);
        if (who.length() > 0) {
            page.add(Ui.text(who, "SvText"));
        }
        page.add(Ui.text(held.online() ? Lang.tr("Online") : held.locationName(), "SvText"));
        page.add(countdown);

        Ui.section(page, "Payment");
        page.add(Ui.fact("Price", Ui.money(held.priceCents(), held.currency())));
        final Container deposit = new Container(com.codename1.ui.layouts.BoxLayout.y());
        page.add(deposit);
        if (held.depositCents() > 0) {
            method.setName("method");
            page.add(Ui.tap(Ui.row(FontImage.MATERIAL_CREDIT_CARD, "Pay the deposit with", null, method),
                    "pick-method", e -> WalletForm.pick(form, chosen == null ? "" : chosen.id(), picked -> {
                        chosen = picked;
                        showMethod();
                        form.showBack();
                    })));
            Api.payments().methods(Net.to(this::defaultMethod, Ui::fail));
        }
        Api.business().policy(Net.to(p -> {
            policy = p;
            deposit.removeAll();
            deposit.add(Ui.text(terms(p), "SvSmall"));
            Ui.relayout(deposit);
        }, Ui::fail));

        final TextArea note = new TextArea("", 3, 24);
        note.setUIID("SvField");
        note.setName("note");
        note.setHint(service != null && service.intakePrompt() != null
                && service.intakePrompt().length() > 0 ? service.intakePrompt()
                : Lang.tr("Anything to tell them before you arrive?"));
        Ui.section(page, "Note");
        page.add(note);
        page.add(Ui.primary("Confirm booking", "confirm", e -> confirm(note.getText())));
        Layouts.broaden(page);
        form.add(BorderLayout.CENTER, page);
    }

    public static void show(Form previous, AppointmentDto held, ServiceDto service) {
        ConfirmForm confirm = new ConfirmForm(previous, held, service);
        confirm.form.show();
        confirm.tick();
    }

    /// The cancellation and no-show terms, as the business set them.
    private String terms(PolicyDto p) {
        StringBuilder out = new StringBuilder();
        if (held.depositCents() > 0) {
            String deposit = Ui.money(held.depositCents(), held.currency());
            // Whole sentences, not a sentence and a tail: a translation may
            // need the words in another order.
            out.append(held.start() - System.currentTimeMillis() > 6 * Times.DAY
                    ? Lang.tr("A deposit of {0} is held on your card a few days before the appointment.",
                            deposit)
                    : Lang.tr("A deposit of {0} is held on your card now.", deposit)).append(' ');
        }
        if (p.cancelHours() > 0) {
            out.append(Lang.tr("Cancel free of charge up to {0} hours before.",
                    String.valueOf(p.cancelHours())));
            if (held.depositCents() > 0) {
                out.append(' ').append(Lang.tr("Later than that, the deposit is kept."));
            }
        }
        if (p.noShowFeePercent() > 0) {
            out.append(' ').append(Lang.tr("Not coming at all costs {0}% of the price.",
                    String.valueOf(p.noShowFeePercent())));
        }
        return out.toString();
    }

    private void defaultMethod(List<PaymentMethodDto> methods) {
        for (int iter = 0; iter < methods.size(); iter++) {
            if (methods.get(iter).isDefault()) {
                chosen = methods.get(iter);
            }
        }
        if (chosen == null && !methods.isEmpty()) {
            chosen = methods.get(0);
        }
        showMethod();
    }

    private void showMethod() {
        method.setText(chosen == null ? Lang.tr("Add a card") : WalletForm.label(chosen));
        Ui.relayout(page);
    }

    /// The minutes left on the hold, said once a second.
    private void tick() {
        ticking = UITimer.timer(1000, true, form, () -> {
            long left = held.holdExpiresAt() - System.currentTimeMillis();
            if (left <= 0) {
                ticking.cancel();
                countdown.setText(Lang.tr("The hold ran out. Go back and pick the time again."));
            } else {
                long seconds = left / 1000;
                countdown.setText(Lang.tr("Held for you for {0}:{1}", String.valueOf(seconds / 60),
                        seconds % 60 < 10 ? "0" + seconds % 60 : String.valueOf(seconds % 60)));
            }
            countdown.getParent().revalidateLater();
        });
    }

    private void confirm(String note) {
        String methodId = held.depositCents() > 0 && chosen != null ? chosen.id() : "";
        Api.appointments().confirm(held.id(), new ConfirmDto(methodId, note, null), Net.to(booked -> {
            done = true;
            if (ticking != null) {
                ticking.cancel();
            }
            Telemetry.booked(booked.depositCents() > 0);
            BookedForm.show(booked, policy);
        }, Ui::fail));
    }

    /// The client went back: the hold is given up rather than left to run out.
    private void letGo() {
        if (done) {
            return;
        }
        if (ticking != null) {
            ticking.cancel();
        }
        Api.appointments().cancel(held.id(), new CancelDto("Changed my mind"),
                Net.to(gone -> { }, (status, message) -> { }));
    }
}
