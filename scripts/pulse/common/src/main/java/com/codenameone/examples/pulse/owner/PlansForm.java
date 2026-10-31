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
package com.codenameone.examples.pulse.owner;

import com.codename1.components.Switch;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.pulse.api.PlanDto;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.ui.Lang;
import com.codenameone.examples.pulse.ui.Ui;
import java.util.List;

/// The membership plans the studio sells, and an editor for their price and
/// terms. A plan is never deleted -- members are on it -- only retired.
public final class PlansForm {
    static final String[] KINDS = {"MONTHLY", "ANNUAL", "CLASS_PACK", "DROP_IN"};
    private static final String[] KIND_NAMES = {"Monthly", "Annual", "Class pack", "Drop-in"};

    private PlansForm() {
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Plans and pricing", "OwnerPlans");
        Ui.back(form, previous);
        final Container list = Ui.list("plans");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.owner().plans(Net.to(plans -> fill(form, list, plans, load[0]),
                Ui.failed(list, load[0])));
        Ui.pull(list, load[0]);
        form.add(BorderLayout.CENTER, list);
        form.add(BorderLayout.SOUTH, Ui.dock(Ui.primary("New plan", "newPlan",
                e -> edit(form, null, load[0]))));
        form.show();
        load[0].run();
    }

    private static void fill(final Form form, Container list, List<PlanDto> plans,
            final Runnable load) {
        list.removeAll();
        for (int iter = 0; plans != null && iter < plans.size(); iter++) {
            final PlanDto plan = plans.get(iter);
            list.add(Ui.tap(Ui.row(FontImage.MATERIAL_CARD_MEMBERSHIP, plan.name(),
                    Ui.money(plan.priceCents(), plan.currency()) + Ui.DOT + kindName(plan.kind()),
                    plan.active() ? null : Ui.badge(Lang.tr("Retired"), "SvBadgeBad")),
                    "plan-" + iter, e -> edit(form, plan, load)));
        }
        Ui.refresh(form);
    }

    static String kindName(String kind) {
        for (int iter = 0; iter < KINDS.length; iter++) {
            if (KINDS[iter].equals(kind)) {
                return Lang.tr(KIND_NAMES[iter]);
            }
        }
        return kind;
    }

    private static void edit(final Form previous, final PlanDto plan, final Runnable saved) {
        final Form form = Ui.form(plan == null ? "New plan" : plan.name(), "PlanEditor");
        Ui.back(form, previous);
        Container page = Ui.page();
        final TextField name = Ui.field(page, "Name", "name", "Unlimited monthly");
        final TextField about = Ui.field(page, "Description", "description",
                "What the plan includes");
        final String[] kind = {plan == null ? KINDS[0] : plan.kind()};
        page.add(Ui.label("Kind", "SvLabel"));
        String[] names = new String[KINDS.length];
        for (int iter = 0; iter < names.length; iter++) {
            names[iter] = Lang.tr(KIND_NAMES[iter]);
        }
        page.add(Ui.choice("kind", names, KINDS, kind[0], value -> kind[0] = value, 2));
        final TextField price = Ui.field(page, "Price", "price", "49.00", TextArea.DECIMAL);
        final TextField months = Ui.field(page, "Months per period", "months", "1",
                TextArea.NUMERIC);
        final TextField credits = Ui.field(page, "Classes included (0 for unlimited)", "credits",
                "0", TextArea.NUMERIC);
        final boolean[] offPeak = {plan != null && plan.offPeakOnly()};
        final boolean[] active = {plan == null || plan.active()};
        Switch offPeakSwitch = Ui.toggle("offPeak", offPeak[0], on -> offPeak[0] = on);
        Switch activeSwitch = Ui.toggle("active", active[0], on -> active[0] = on);
        page.add(Ui.row((char) 0, Lang.tr("Off-peak only"), null, offPeakSwitch));
        page.add(Ui.row((char) 0, Lang.tr("On sale"), null, activeSwitch));
        if (plan != null) {
            name.setText(plan.name());
            about.setText(plan.description());
            price.setText(cents(plan.priceCents()));
            months.setText(String.valueOf(plan.periodMonths()));
            credits.setText(String.valueOf(plan.classCredits()));
        }
        page.add(Ui.primary("Save", "save", e -> {
            long priceCents = parseCents(price.getText());
            if (name.getText().trim().length() == 0 || priceCents < 0) {
                Ui.say(Lang.tr("A plan needs a name and a price."));
                return;
            }
            PlanDto wanted = new PlanDto(plan == null ? null : plan.id(), name.getText().trim(),
                    about.getText().trim(), kind[0], priceCents,
                    plan == null ? null : plan.currency(), number(months.getText(), 1),
                    number(credits.getText(), 0), plan == null ? 0 : plan.minAge(),
                    plan == null ? 0 : plan.maxAge(), offPeak[0], active[0]);
            Api.owner().savePlan(wanted, Net.to(done -> {
                Ui.say(Lang.tr("Plan saved"));
                previous.showBack();
                saved.run();
            }, Ui::fail));
        }));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    /// `cents` written as whole units and two decimals.
    static String cents(long cents) {
        long part = cents % 100;
        return (cents / 100) + "." + (part < 10 ? "0" : "") + part;
    }

    /// A price typed in units, in cents; -1 when it is not a price.
    static long parseCents(String text) {
        String value = text == null ? "" : text.trim();
        if (value.length() == 0) {
            return -1;
        }
        try {
            return Math.round(Double.parseDouble(value) * 100);
        } catch (NumberFormatException wrong) {
            return -1;
        }
    }

    static int number(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException wrong) {
            return fallback;
        }
    }
}
