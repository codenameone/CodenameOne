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
package com.codenameone.examples.wayline.admin;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.ReasonDto;
import com.codenameone.examples.wayline.api.ReceiptDto;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.pay.ReceiptForm;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.Ui;
import java.util.List;

/// What was charged, newest first, and for each whether it went through. A
/// payment opens as its receipt, which is where an admin returns the money.
public final class PaymentsForm {
    private PaymentsForm() {
    }

    static void show(Form previous) {
        show(Panes.phone(previous));
    }

    static void show(final Panes panes) {
        final Container list = Ui.list("payments");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.admin().payments(Net.to(found -> fill(panes, list, found),
                Ui.failed(list, () -> load[0].run())));
        panes.list("Payments", "Payments", Panes.listed(null, list), load[0]);
    }

    /// A payment, opened: its receipt, and the way to return the money.
    private static void open(final Panes panes, final ReceiptDto payment) {
        Container page = ReceiptForm.page(payment, true);
        if ("paid".equals(payment.status())) {
            page.add(Ui.danger("Refund this ride", "refund", e -> panes.reason("Refund",
                    Lang.tr("{0} goes back to the card it came from.",
                            Ui.money(payment.total(), payment.currency())), "Refund", why -> {
                        ReasonDto reason = new ReasonDto(why);
                        Api.admin().refundRide(payment.rideId(), reason, Net.to(refunded -> {
                            Telemetry.rideRefunded("payments");
                            open(panes, refunded);
                        }, Ui::fail));
                    })));
        }
        panes.detail("Receipt", "Receipt", page);
    }

    private static void fill(final Panes panes, Container list, List<ReceiptDto> payments) {
        list.removeAll();
        if (payments == null || payments.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_PAYMENTS, "No payments yet",
                    "A ride shows up here when it is paid for."));
        }
        for (int iter = 0; payments != null && iter < payments.size(); iter++) {
            final ReceiptDto payment = payments.get(iter);
            Container value = new Container(BoxLayout.y());
            value.add(Ui.plain(Ui.money(payment.total(), payment.currency()), "WlRowValue"));
            value.add(Ui.badge(ReceiptForm.status(payment.status()),
                    ReceiptForm.badge(payment.status())));
            String who = payment.riderUsername() == null ? "" : payment.riderUsername();
            String how = "cash".equals(payment.status()) ? Lang.tr("Cash") : payment.methodLabel();
            final Container picked = Ui.row("cash".equals(payment.status())
                    ? FontImage.MATERIAL_PAYMENTS : FontImage.MATERIAL_CREDIT_CARD,
                    payment.dropoffName(),
                    who + (who.length() > 0 && how != null && how.length() > 0 ? Ui.DOT : "")
                            + (how == null ? "" : how), value);
            list.add(Ui.tap(picked, "payment-" + iter, e -> {
                panes.picked(picked);
                open(panes, payment);
            }));
        }
        panes.refresh();
    }
}
