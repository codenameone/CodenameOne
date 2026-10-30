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
package com.codenameone.examples.pulse.desk;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.pulse.Telemetry;
import com.codenameone.examples.pulse.api.WalkInDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;

/// Someone at the desk with no membership: a guest of a member, a day pass, or
/// a free trial. They are recorded as a lead, so the owner's campaigns can
/// follow up -- which is why the e-mail is asked for.
public final class WalkInForm {
    private WalkInForm() {
    }

    public static void show(final Form previous, final String locationId, final Runnable done) {
        final Form form = Ui.form("Walk-in", "WalkIn");
        Ui.back(form, previous);
        Container page = Ui.page();
        final TextField name = Ui.field(page, "Name", "name", "");
        final TextField email = Ui.field(page, "E-mail", "email", "for the follow-up",
                TextArea.EMAILADDR);
        final TextField phone = Ui.field(page, "Mobile number", "phone", "optional",
                TextArea.PHONENUMBER);
        final String[] kind = {"DAY_PASS"};
        page.add(Ui.label("Visit", "SvLabel"));
        page.add(Ui.choice("kind", new String[] {Lang.tr("Day pass"), Lang.tr("Trial"),
            Lang.tr("Guest")}, new String[] {"DAY_PASS", "TRIAL", "GUEST"}, kind[0],
                value -> kind[0] = value));
        final TextField amount = Ui.field(page, "Amount", "amount", "0.00", TextArea.DECIMAL);
        final String[] paidBy = {"card"};
        page.add(Ui.label("Paid by", "SvLabel"));
        page.add(Ui.choice("paidBy", new String[] {Lang.tr("Card"), Lang.tr("Cash"),
            Lang.tr("Free")}, new String[] {"card", "cash", "none"}, paidBy[0],
                value -> paidBy[0] = value));
        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        final Button save = Ui.primary("Let them in", "submit", null);
        save.addActionListener(e -> {
            save.setEnabled(false);
            long cents = "none".equals(paidBy[0]) ? 0 : cents(amount.getText());
            Api.desk().walkIn(new WalkInDto(null, locationId, name.getText().trim(),
                    email.getText().trim(), phone.getText().trim(), kind[0], cents, paidBy[0], 0),
                    Net.to(saved -> {
                        Telemetry.walkIn(kind[0]);
                        Ui.say(Lang.tr("{0} is in", saved.name()));
                        previous.showBack();
                        done.run();
                    }, (status, message) -> {
                        save.setEnabled(true);
                        error.setText(message);
                        Ui.refresh(form);
                    }));
        });
        page.add(error).add(save);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    /// "12.5" to 1250; anything unreadable is nothing.
    static long cents(String typed) {
        String text = typed == null ? "" : typed.trim();
        long whole = 0;
        long fraction = 0;
        int fractionDigits = -1;
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c == '.' || c == ',') {
                if (fractionDigits >= 0) {
                    return 0;
                }
                fractionDigits = 0;
            } else if (c >= '0' && c <= '9') {
                if (fractionDigits < 0) {
                    whole = whole * 10 + (c - '0');
                } else if (fractionDigits < 2) {
                    fraction = fraction * 10 + (c - '0');
                    fractionDigits++;
                }
            } else {
                return 0;
            }
        }
        if (fractionDigits == 1) {
            fraction *= 10;
        }
        return whole * 100 + fraction;
    }
}
