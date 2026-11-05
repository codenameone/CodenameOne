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
package com.codenameone.examples.slate.owner;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.slate.api.DashboardDto;
import com.codenameone.examples.slate.api.NameCountDto;
import com.codenameone.examples.slate.live.LiveChannel;
import com.codenameone.examples.slate.net.Api;
import com.codenameone.examples.slate.net.Net;
import com.codenameone.examples.slate.ui.Chart;
import com.codenameone.examples.slate.ui.Lang;
import com.codenameone.examples.slate.ui.Layouts;
import com.codenameone.examples.slate.ui.Menu;
import com.codenameone.examples.slate.ui.Nav;
import com.codenameone.examples.slate.ui.SettingsForm;
import com.codenameone.examples.slate.ui.StatRow;
import com.codenameone.examples.slate.ui.Ui;

import java.util.List;

// tag::slate-owner-dashboard[]
/// The owner's home: how the last weeks went -- booked, done, no-shows,
/// cancelled, takings and how full the providers' hours were -- and the way
/// into the day board, the catalogue and the booking rules.
public class OwnerForm extends LiveChannel.Adapter implements Nav.Home {
    private final Form form = Ui.home("Owner");
    private final Menu menu = new Menu(form, Nav.OWNER);
    private final Container page = Ui.page();

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        menu.item(FontImage.MATERIAL_DASHBOARD, "Dashboard", "dashboard", () -> form.showBack());
        menu.item(FontImage.MATERIAL_VIEW_WEEK, "Day board", "board", () -> BoardForm.show(form));
        menu.item(FontImage.MATERIAL_SPA, "Services", "services",
                () -> CatalogForm.show(form, CatalogForm.SERVICES));
        menu.item(FontImage.MATERIAL_PEOPLE, "Providers", "providers",
                () -> CatalogForm.show(form, CatalogForm.PROVIDERS));
        menu.item(FontImage.MATERIAL_PLACE, "Locations", "locations",
                () -> CatalogForm.show(form, CatalogForm.LOCATIONS));
        menu.item(FontImage.MATERIAL_CHAIR, "Rooms and equipment", "resources",
                () -> CatalogForm.show(form, CatalogForm.RESOURCES));
        menu.item(FontImage.MATERIAL_RULE, "Booking rules", "policy", () -> PolicyForm.show(form));

        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvHomeBar");
        bar.add(BorderLayout.WEST, menu.flatButton());
        bar.add(BorderLayout.CENTER, Ui.label("Dashboard", "SvTitle"));
        bar.add(BorderLayout.EAST, Ui.flat(FontImage.MATERIAL_SETTINGS, "Settings", "home-settings",
                e -> SettingsForm.show(form)));
        page.setName("dashboard");
        Ui.pull(page, this::load);
        Container shell = new Container(new BorderLayout());
        shell.add(BorderLayout.NORTH, bar);
        shell.add(BorderLayout.CENTER, page);
        form.add(shell);
        menu.install();
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            load();
        });
    }

    @Override
    public void appointmentChanged(String id, String state) {
        load();
    }

    private void load() {
        Api.owner().dashboard(Net.to(this::show, Ui::fail));
    }

    private void show(DashboardDto d) {
        if (!Nav.isHome(form)) {
            return;
        }
        page.removeAll();
        StatRow top = Ui.stats(3);
        top.stat(String.valueOf(d.booked()), "Booked", "stat-booked");
        top.stat(String.valueOf(d.completed()), "Done", "stat-completed");
        top.stat(Ui.moneyShort(d.revenueCents(), d.currency()), "Taken", "stat-revenue");
        page.add(top);
        StatRow second = Ui.stats(3);
        second.stat(d.utilisation() + "%", "Hours filled", "stat-utilisation");
        int shown = d.completed() + d.noShows();
        second.stat(shown == 0 ? "0%" : (d.noShows() * 100 / shown) + "%", "No-show rate",
                "stat-no-shows");
        second.stat(String.valueOf(d.cancelled()), "Cancelled", "stat-cancelled");
        page.add(second);
        Ui.section(page, "Bookings a day");
        double[] days = new double[d.bookedByDay().size()];
        for (int iter = 0; iter < days.length; iter++) {
            days[iter] = d.bookedByDay().get(iter).intValue();
        }
        page.add(Chart.of("chart-booked", days, Lang.tr("{0} days ago",
                String.valueOf(days.length - 1)), Lang.tr("Today")));
        String trend = trend(days);
        if (trend != null) {
            Label change = Ui.label(trend, "SvCaption");
            change.setName("booked-trend");
            page.add(change);
        }
        counts(page, "Most booked", d.topServices(), FontImage.MATERIAL_SPA);
        counts(page, "Busiest providers", d.providerLoad(), FontImage.MATERIAL_PERSON);
        page.add(Ui.primary("Open the day board", "open-board", e -> BoardForm.show(form)));
        Layouts.broaden(page);
        Ui.relayout(page);
    }

    /// The second half of the period against the first: is the business
    /// growing. Null while the first half has nothing to compare with.
    static String trend(double[] days) {
        int half = days.length / 2;
        double before = 0;
        double after = 0;
        for (int iter = 0; iter < half * 2; iter++) {
            if (iter < half) {
                before += days[days.length - half * 2 + iter];
            } else {
                after += days[days.length - half * 2 + iter];
            }
        }
        if (before <= 0) {
            return null;
        }
        long change = Math.round((after - before) * 100 / before);
        return Lang.tr("{0}% against the {1} days before", (change > 0 ? "+" : "") + change,
                String.valueOf(half));
    }

    /// Each row with a bar as long as its share of the busiest, so the
    /// ranking reads at a glance.
    private static void counts(Container page, String title, List<NameCountDto> rows, char icon) {
        if (rows.isEmpty()) {
            return;
        }
        Ui.section(page, title);
        Container list = new Container(BoxLayout.y());
        int most = 1;
        for (int iter = 0; iter < rows.size(); iter++) {
            most = Math.max(most, rows.get(iter).count());
        }
        for (int iter = 0; iter < rows.size(); iter++) {
            NameCountDto r = rows.get(iter);
            list.add(Ui.row(icon, r.name(), "", Ui.label(String.valueOf(r.count()), "SvValue")));
            list.add(new Chart.Meter(r.count() / (double) most));
        }
        page.add(list);
    }
}
// end::slate-owner-dashboard[]
