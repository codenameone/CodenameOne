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
package com.codenameone.examples.wayline.admin;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.PricingDto;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.Ui;

/// What a ride costs: the parts of a fare, the service's share, and what the
/// roomier cars and the busy hours multiply it by.
///
/// A change is for the rides asked for from then on; a ride already quoted
/// keeps its price. The server refuses numbers that could only be a mistake,
/// and says which.
public final class PricingForm {
    private PricingForm() {
    }

    static void show(final Form previous) {
        show(Panes.phone(previous));
    }

    static void show(final Panes panes) {
        final Container page = Ui.page();
        panes.page("Pricing", "Pricing", page);
        Api.admin().pricing(Net.to(pricing -> fill(panes, page, pricing), Ui::fail));
    }

    private static void fill(final Panes panes, final Container page, final PricingDto pricing) {
        page.removeAll();
        Ui.section(page, Lang.tr("The fare, in {0}", pricing.currency));
        final TextField base = money(page, "Base fare", "baseFare", pricing.baseFare, null);
        Container pair = new Container(new GridLayout(1, 2));
        Container left = side("WlPairLeft");
        Container right = side("WlPairRight");
        final TextField perKm = money(left, "Per kilometre", "perKm", pricing.perKm, null);
        final TextField perMinute = money(right, "Per minute", "perMinute", pricing.perMinute,
                null);
        pair.add(left).add(right);
        page.add(pair);
        final TextField minimum = money(page, "Minimum fare", "minimumFare", pricing.minimumFare,
                null);

        Ui.section(page, "The service's share");
        pair = new Container(new GridLayout(1, 2));
        left = side("WlPairLeft");
        right = side("WlPairRight");
        final TextField fee = number(left, "Service fee, %", "serviceFeePercent",
                pricing.serviceFeePercent);
        final TextField commission = number(right, "Commission, %", "commissionPercent",
                pricing.commissionPercent);
        pair.add(left).add(right);
        page.add(pair);
        page.add(Ui.text("The fee is added to the fare and paid by the rider. The commission "
                + "is kept from the fare; the rest is the driver's.", "WlMuted"));

        Ui.section(page, "Multipliers");
        final TextField surge = number(page, "Surge, while demand is high", "surgeMultiplier",
                pricing.surgeMultiplier);
        pair = new Container(new GridLayout(1, 2));
        left = side("WlPairLeft");
        right = side("WlPairRight");
        final TextField comfort = number(left, "Comfort", "comfortMultiplier",
                pricing.comfortMultiplier);
        final TextField xl = number(right, "XL", "xlMultiplier", pricing.xlMultiplier);
        pair.add(left).add(right);
        page.add(pair);

        final SpanLabel error = Ui.text("", "WlError");
        error.setName("error");
        final Button save = Ui.primary("Save prices", "submit", null);
        save.addActionListener(e -> {
            PricingDto wanted = new PricingDto();
            wanted.currency = pricing.currency;
            wanted.baseFare = cents(base);
            wanted.perKm = cents(perKm);
            wanted.perMinute = cents(perMinute);
            wanted.minimumFare = cents(minimum);
            wanted.serviceFeePercent = value(fee);
            wanted.commissionPercent = value(commission);
            wanted.surgeMultiplier = value(surge);
            wanted.comfortMultiplier = value(comfort);
            wanted.xlMultiplier = value(xl);
            save.setEnabled(false);
            error.setText("");
            Api.admin().savePricing(wanted, Net.to(saved -> {
                Telemetry.pricingChanged();
                Ui.say("The new prices are in force");
                fill(panes, page, saved);
            }, (status, message) -> {
                save.setEnabled(true);
                error.setText(message);
                panes.refresh();
            }));
        });
        page.add(error).add(save);
        panes.refresh();
    }

    private static Container side(String uiid) {
        Container side = new Container(BoxLayout.y());
        side.setUIID(uiid);
        return side;
    }

    private static TextField money(Container to, String caption, String name, long cents,
            String hint) {
        TextField field = Ui.field(to, caption, name, hint == null ? "0.00" : hint,
                TextArea.DECIMAL);
        long part = cents % 100;
        field.setText((cents / 100) + "." + (part < 10 ? "0" : "") + part);
        return field;
    }

    private static TextField number(Container to, String caption, String name, double value) {
        TextField field = Ui.field(to, caption, name, "1.0", TextArea.DECIMAL);
        // Two decimal places are all a percentage or a multiplier needs.
        long hundredths = Math.round(value * 100d);
        long part = hundredths % 100;
        field.setText((hundredths / 100) + "." + (part < 10 ? "0" : "") + part);
        return field;
    }

    private static long cents(TextField field) {
        return Math.round(value(field) * 100d);
    }

    /// The number typed, or -1 for anything that is not one, which the server
    /// then refuses by name.
    private static double value(TextField field) {
        String text = field.getText().trim().replace(',', '.');
        boolean point = false;
        if (text.length() == 0 || text.length() > 12) {
            return -1;
        }
        for (int iter = 0; iter < text.length(); iter++) {
            char letter = text.charAt(iter);
            if (letter == '.' && !point) {
                point = true;
            } else if (letter < '0' || letter > '9') {
                return -1;
            }
        }
        return ".".equals(text) ? -1 : Double.parseDouble(text);
    }
}
