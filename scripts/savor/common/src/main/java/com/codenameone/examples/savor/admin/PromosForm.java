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

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.savor.api.PromoDto;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Ui;

import java.util.List;

/// The promo codes a customer can type at checkout: a share off, or an
/// amount off, above a smallest order. A code is switched off, never removed,
/// so the orders that used it still say what it was.
final class PromosForm {
    private final Panes panes;
    private final Container list = Ui.list("promos");

    private PromosForm(Panes panes) {
        this.panes = panes;
        Container top = new Container(BoxLayout.y());
        top.setUIID("SvPage");
        top.add(Ui.secondary("New promo", "new-promo", e -> edit(null)));
        Ui.pull(list, this::load);
        panes.list("Promos", "Promos", Panes.listed(top, list), this::load);
    }

    static void show(Form previous) {
        show(Panes.phone(previous));
    }

    static void show(Panes panes) {
        new PromosForm(panes);
    }

    private void load() {
        Api.admin().promos(Net.to(this::fill, Ui.failed(list, this::load)));
    }

    private void fill(List<PromoDto> found) {
        list.removeAll();
        if (found == null || found.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_SELL, "No promos", null));
        }
        for (int iter = 0; found != null && iter < found.size(); iter++) {
            final PromoDto promo = found.get(iter);
            Container row = Ui.row(FontImage.MATERIAL_SELL, promo.code(), describe(promo),
                    Ui.badge(promo.active() ? Lang.tr("On") : Lang.tr("Off"),
                            promo.active() ? "SvBadgeGood" : "SvBadgeBad"));
            final Container picked = row;
            list.add(Ui.tap(row, "promo-" + promo.code(), e -> {
                panes.picked(picked);
                edit(promo);
            }));
        }
        panes.refresh();
    }

    static String describe(PromoDto promo) {
        String off = promo.percentOff() > 0 ? Lang.tr("{0}% off", String.valueOf(promo.percentOff()))
                : Lang.tr("{0} off", Ui.money(promo.amountOffCents(), null));
        return promo.minimumCents() > 0 ? off + Ui.DOT
                + Lang.tr("over {0}", Ui.money(promo.minimumCents(), null)) : off;
    }

    private void edit(final PromoDto promo) {
        Container page = Ui.page();
        final TextField code = Ui.field(page, "Code", "promo-code", "WELCOME10");
        final TextField percent = Ui.field(page, "Percent off", "promo-percent", "10");
        percent.setConstraint(TextArea.NUMERIC);
        final TextField amount = Ui.field(page, "Or an amount off", "promo-amount", "5.00");
        amount.setConstraint(TextArea.DECIMAL);
        final TextField minimum = Ui.field(page, "Smallest order", "promo-minimum", "15.00");
        minimum.setConstraint(TextArea.DECIMAL);
        final boolean[] active = {promo == null || promo.active()};
        page.add(Ui.row((char) 0, Lang.tr("Active"), null, Ui.toggle("promo-active", active[0],
                value -> active[0] = value)));
        if (promo != null) {
            code.setText(promo.code());
            code.setEditable(false);
            percent.setText(String.valueOf(promo.percentOff()));
            amount.setText(amount(promo.amountOffCents()));
            minimum.setText(amount(promo.minimumCents()));
        }
        final Label error = new Label("", "SvError");
        page.add(error);
        page.add(Ui.primary("Save", "save-promo", e -> {
            int share = number(percent.getText());
            long off = cents(amount.getText());
            String typed = code.getText().trim();
            if (typed.length() == 0 || share < 0 || share > 100 || off < 0
                    || share == 0 && off == 0) {
                error.setText(Lang.tr("A code, and either a percent or an amount off"));
                page.revalidate();
                return;
            }
            Api.admin().savePromo(new PromoDto(typed, share, share > 0 ? 0 : off,
                    Math.max(0, cents(minimum.getText())), active[0]), Net.to(saved -> {
                        Ui.say("Saved");
                        panes.home();
                    }, Ui::fail));
        }));
        panes.detail(promo == null ? Lang.tr("New promo") : promo.code(), "PromoEdit", page);
    }

    private static String amount(long cents) {
        long part = cents % 100;
        return cents / 100 + "." + (part < 10 ? "0" : "") + part;
    }

    private static int number(String text) {
        try {
            return text.trim().length() == 0 ? 0 : Integer.parseInt(text.trim());
        } catch (NumberFormatException wrong) {
            return -1;
        }
    }

    private static long cents(String text) {
        String clean = text.trim().replace(',', '.');
        if (clean.length() == 0) {
            return 0;
        }
        int dot = clean.indexOf('.');
        try {
            if (dot < 0) {
                return Long.parseLong(clean) * 100;
            }
            String part = (clean.substring(dot + 1) + "00").substring(0, 2);
            return Long.parseLong(dot == 0 ? "0" : clean.substring(0, dot)) * 100
                    + Long.parseLong(part);
        } catch (NumberFormatException wrong) {
            return -1;
        }
    }
}
