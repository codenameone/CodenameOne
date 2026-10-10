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
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.ReasonDto;
import com.codenameone.examples.wayline.api.ReceiptDto;
import com.codenameone.examples.wayline.api.TipDto;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.Ui;

/// What a ride cost, how it was paid, and -- for the rider -- the chance to
/// leave a tip.
///
/// Every number here is the server's, in cents, and is shown as it came. The
/// one sum the app does is the tip a percentage stands for, which it then sends
/// as an amount: what "15%" means is decided where the rider can see it.
public final class ReceiptForm {
    private static final int[] PERCENTS = {10, 15, 20};

    private ReceiptForm() {
    }

    /// Fetches the rider's receipt for `rideId` and shows it.
    public static void open(final Form previous, String rideId) {
        Api.payments().receipt(rideId, Net.to(receipt -> show(previous, receipt, false),
                Ui::fail));
    }

    /// @param admin true for an admin, who may return the money and may not tip
    public static void show(final Form previous, final ReceiptDto receipt, final boolean admin) {
        final Form form = Ui.form("Receipt", "Receipt");
        Ui.back(form, previous);
        Container page = page(receipt, admin);
        actions(form, previous, page, receipt, admin);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    /// What a receipt says, without what can be done about it: for a caller
    /// with its own place to show one, which the admin console is.
    ///
    /// @param admin true to name the rider, which an admin needs and a rider does not
    public static Container page(final ReceiptDto receipt, final boolean admin) {
        Container page = Ui.page();
        final String currency = receipt.currency;

        Label total = Ui.plain(Ui.money(receipt.total, currency), "WlAmount");
        total.setName("total");
        page.add(total);
        Label status = Ui.badge(status(receipt.status), badge(receipt.status));
        status.setName("paymentStatus");
        page.add(FlowLayout.encloseCenter(status));
        page.add(FlowLayout.encloseCenter(Ui.plain(
                receipt.paidAt > 0 ? Ui.when(receipt.paidAt) : "", "WlMuted")));

        Ui.section(page, "Trip");
        Container trip = new Container(BoxLayout.y());
        trip.add(Ui.row(FontImage.MATERIAL_TRIP_ORIGIN, receipt.pickupName, null, null));
        trip.add(Ui.row(FontImage.MATERIAL_PLACE, receipt.dropoffName,
                Ui.distance(receipt.distanceMeters) + Ui.DOT
                        + Ui.minutes(receipt.durationSeconds), null));
        if (receipt.driverName != null && receipt.driverName.length() > 0) {
            trip.add(Ui.row(FontImage.MATERIAL_PERSON, receipt.driverName, receipt.vehicle,
                    null));
        }
        if (admin && receipt.riderUsername != null && receipt.riderUsername.length() > 0) {
            trip.add(Ui.row(FontImage.MATERIAL_HAIL, receipt.riderUsername, "Rider", null));
        }
        page.add(trip);

        Ui.section(page, "Fare");
        Container fare = new Container(BoxLayout.y());
        fare.setUIID("WlPanel");
        fare.add(Ui.fact("Base fare", Ui.money(receipt.baseFare, currency)));
        fare.add(Ui.fact("Distance", Ui.money(receipt.distanceFare, currency)));
        fare.add(Ui.fact("Time", Ui.money(receipt.timeFare, currency)));
        fare.add(Ui.fact("Service fee", Ui.money(receipt.serviceFee, currency)));
        if (receipt.discount > 0) {
            fare.add(Ui.fact("Discount", "-" + Ui.money(receipt.discount, currency)));
        }
        if (receipt.tip > 0) {
            Container tipped = Ui.fact("Tip", Ui.money(receipt.tip, currency));
            tipped.getComponentAt(1).setName("tipPaid");
            fare.add(tipped);
        }
        fare.add(Ui.fact("Total", Ui.money(receipt.total, currency), "WlFactTotal"));
        fare.add(Ui.fact("Paid with", "cash".equals(receipt.status) ? Lang.tr("Cash")
                : receipt.methodLabel));
        page.add(fare);
        return page;
    }

    private static void actions(final Form form, final Form previous, Container page,
            final ReceiptDto receipt, final boolean admin) {
        final String currency = receipt.currency;
        boolean settled = "paid".equals(receipt.status) || "cash".equals(receipt.status);
        if (!admin && settled && receipt.tip == 0) {
            tip(form, previous, page, receipt);
        }
        if (admin && "paid".equals(receipt.status)) {
            page.add(Ui.danger("Refund this ride", "refund", e -> Ui.reason(form,
                    "Refund", Lang.tr("{0} goes back to the card it came from.",
                            Ui.money(receipt.total, currency)), "Refund",
                    why -> {
                        ReasonDto reason = new ReasonDto();
                        reason.reason = why;
                        Api.admin().refundRide(receipt.rideId, reason, Net.to(refunded -> {
                            Telemetry.rideRefunded("receipt");
                            show(previous, refunded, true);
                        }, Ui::fail));
                    })));
        }
    }

    /// The tip: three shares of the fare to tap, or an amount to type.
    private static void tip(final Form form, final Form previous, Container page,
            final ReceiptDto receipt) {
        Ui.section(page, "Add a tip");
        page.add(Ui.text(receipt.driverName == null || receipt.driverName.length() == 0
                ? Lang.tr("All of it goes to your driver.")
                : Lang.tr("All of it goes to {0}.", receipt.driverName), "WlMuted"));
        final long fare = receipt.baseFare + receipt.distanceFare + receipt.timeFare;
        String[] labels = new String[PERCENTS.length];
        String[] values = new String[PERCENTS.length];
        for (int iter = 0; iter < PERCENTS.length; iter++) {
            long cents = Math.max(100L, Math.round(fare * PERCENTS[iter] / 100d));
            values[iter] = String.valueOf(cents);
            labels[iter] = PERCENTS[iter] + "%  " + Ui.money(cents, receipt.currency);
        }
        final TextField custom = new TextField("", Lang.tr("Another amount"), 8, TextArea.DECIMAL);
        custom.setUIID("WlField");
        custom.getHintLabel().setUIID("WlFieldHint");
        custom.setName("tipCustom");
        final long[] chosen = {0};
        Container presets = Ui.choice("tip", labels, values, "", value -> {
            chosen[0] = Long.parseLong(value);
            custom.setText("");
        });
        final String[] amounts = values;
        // The amounts are numbers; nothing in them is a word to look up.
        for (int iter = 0; iter < presets.getComponentCount(); iter++) {
            ((Button) presets.getComponentAt(iter)).setShouldLocalize(false);
        }
        page.add(presets);
        page.add(custom);
        page.add(Ui.primary("Send tip", "sendTip", e -> {
            long cents = custom.getText().trim().length() > 0 ? cents(custom.getText())
                    : chosen[0];
            if (cents <= 0) {
                Ui.say("Choose an amount for the tip");
                return;
            }
            TipDto tip = new TipDto();
            tip.amount = cents;
            // Which of the shares it was, when it was one of them.
            int share = 0;
            for (int iter = 0; iter < amounts.length; iter++) {
                if (custom.getText().trim().length() == 0
                        && amounts[iter].equals(String.valueOf(cents))) {
                    share = PERCENTS[iter];
                }
            }
            final int reported = share;
            Api.payments().tip(receipt.rideId, tip, Net.to(tipped -> {
                Telemetry.tipped("receipt", reported);
                Ui.say("Thank you. Your tip was sent.");
                show(previous, tipped, false);
            }, Ui::fail));
        }));
    }

    /// An amount typed with or without a decimal point, in cents; 0 for
    /// anything that is not an amount.
    static long cents(String typed) {
        String text = typed.trim().replace(',', '.');
        int point = text.indexOf('.');
        String whole = point < 0 ? text : text.substring(0, point);
        String part = point < 0 ? "" : text.substring(point + 1);
        if (part.length() > 2) {
            part = part.substring(0, 2);
        }
        while (part.length() < 2) {
            part += "0";
        }
        if (Cards.digits(whole).length() != whole.length()
                || Cards.digits(part).length() != part.length() || whole.length() > 6) {
            return 0;
        }
        return (whole.length() == 0 ? 0 : Long.parseLong(whole)) * 100 + Long.parseLong(part);
    }

    /// A payment's state, in words.
    public static String status(String status) {
        return "paid".equals(status) ? "Paid" : "cash".equals(status) ? "Paid in cash"
                : "refunded".equals(status) ? "Refunded" : "failed".equals(status)
                ? "Payment failed" : "Payment pending";
    }

    public static String badge(String status) {
        return "paid".equals(status) || "cash".equals(status) ? "WlBadgeGood"
                : "failed".equals(status) ? "WlBadgeBad" : "WlBadgeWarn";
    }
}
