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
package com.codenameone.examples.savor.admin;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.savor.Telemetry;
import com.codenameone.examples.savor.api.PricingDto;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Ui;

/// What a delivery costs: the parts of a fare, the service's share, and what the
/// roomier cars and the busy hours multiply it by.
///
/// A change is for the deliveries asked for from then on; a delivery already quoted
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
        Ui.section(page, Lang.tr("Delivery, in {0}", pricing.currency()));
        Container pair = new Container(new GridLayout(1, 2));
        Container left = side("SvPairLeft");
        Container right = side("SvPairRight");
        final TextField base = money(left, "Delivery fee", "baseCents", pricing.baseCents(), null);
        final TextField perKm = money(right, "Per kilometre", "perKmCents", pricing.perKmCents(),
                null);
        pair.add(left).add(right);
        page.add(pair);
        final TextField surge = number(page, "Surge, while demand is high", "surge",
                pricing.surge());
        page.add(Ui.text("The delivery fee is the courier's, all of it.", "SvMuted"));

        Ui.section(page, "Small orders");
        pair = new Container(new GridLayout(1, 2));
        left = side("SvPairLeft");
        right = side("SvPairRight");
        final TextField small = money(left, "Below", "smallOrderCents", pricing.smallOrderCents(),
                null);
        final TextField smallFee = money(right, "Fee", "smallOrderFeeCents",
                pricing.smallOrderFeeCents(), null);
        pair.add(left).add(right);
        page.add(pair);

        Ui.section(page, "The service's share");
        pair = new Container(new GridLayout(1, 2));
        left = side("SvPairLeft");
        right = side("SvPairRight");
        final TextField fee = number(left, "Service fee, %", "serviceFeePercent",
                pricing.serviceFeePercent());
        final TextField commission = number(right, "Commission, %", "commissionPercent",
                pricing.commissionPercent());
        pair.add(left).add(right);
        page.add(pair);
        final TextField tax = number(page, "Tax, %", "taxPercent", pricing.taxPercent());
        page.add(Ui.text("The service fee is a share of the food, paid by the customer. The "
                + "commission is kept from what the restaurant is paid.", "SvMuted"));

        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        final Button save = Ui.primary("Save prices", "submit", null);
        save.addActionListener(e -> {
            PricingDto wanted = new PricingDto(pricing.currency(), cents(base), cents(perKm),
                    value(surge), cents(small), cents(smallFee), value(fee), value(tax),
                    value(commission));
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
