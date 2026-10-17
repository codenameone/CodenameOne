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

import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.savor.ui.Layouts;
import com.codenameone.examples.savor.ui.Menu;
import com.codenameone.examples.savor.ui.Nav;
import com.codenameone.examples.savor.ui.Ui;

/// Administering: what needs an answer, how the service is doing right now and
/// over the last weeks, and the way to everything an admin looks after.
///
/// Everything here is behind `/api/admin`, which the server opens to the
/// `ADMIN` role and to nobody else. That is where the protection is. This
/// screen is reached only when `/api/me` says the user is an admin, but that is
/// so the others are not shown something useless -- not what keeps them out.
///
/// It is laid out for the room there is. This is the phone's: the [Dashboard]
/// with the places under it and in a drawer, each a screen of its own. On a
/// wide screen -- a tablet on its side, a desktop, a browser -- it is the
/// [Console] instead, with the places down the side and a list beside what was
/// picked from it. Which one is decided when the screen is built, and a window
/// resized across the line, or a device turned, builds it again.
public class AdminForm implements Nav.Home {
    private static final int POLL_MILLIS = 5000;

    private final Console console;
    private final Form form;
    private Dashboard dashboard;

    public AdminForm() {
        if (Layouts.wide()) {
            console = new Console();
            form = console.form();
            return;
        }
        console = null;
        form = Ui.home("Admin");
        final Menu menu = new Menu(form, Nav.ADMIN);
        dashboard = new Dashboard(false, new Dashboard.Places() {
            @Override
            public void applications() {
                ApplicationsForm.show(form);
            }

            @Override
            public void flagged() {
                UsersForm.show(form, UsersForm.FLAGGED);
            }
        });
        menu.item(FontImage.MATERIAL_INSIGHTS, "Dashboard", "home", () -> { });
        menu.item(FontImage.MATERIAL_ASSIGNMENT_IND, "Applications", "applications",
                () -> ApplicationsForm.show(form));
        menu.item(FontImage.MATERIAL_GROUP, "People", "users", () -> UsersForm.show(form, ""));
        menu.item(FontImage.MATERIAL_RECEIPT_LONG, "Deliveries", "deliveries",
                () -> AdminDeliveriesForm.show(form));
        menu.item(FontImage.MATERIAL_PAYMENTS, "Payments", "payments",
                () -> PaymentsForm.show(form));
        menu.item(FontImage.MATERIAL_MAP, "Live map", "fleet", this::fleet);
        menu.item(FontImage.MATERIAL_SELL, "Pricing", "pricing", () -> PricingForm.show(form));

        // The bar a title bar would be, with the menu where the map screens
        // have it, and the mode said beside the title: an admin who also deliveries
        // should never have to wonder which app this is.
        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvHomeBar");
        bar.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(menu.flatButton()));
        bar.add(BorderLayout.CENTER, FlowLayout.encloseLeftMiddle(
                Ui.label("Operations", "SvHeading")));
        // In the menu the badge stands under a name and keeps a gap above it;
        // here it is alone on the bar's centre line.
        Label admin = Ui.badge("Admin", "SvModeBadge");
        admin.getAllStyles().setMargin(0, 0, 0, 0);
        bar.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(admin));

        Container page = dashboard.page();
        Ui.section(page, "Manage");
        Container manage = new Container(BoxLayout.y());
        manage.add(Ui.tap(Ui.row(FontImage.MATERIAL_ASSIGNMENT_IND, "Applications",
                "People who asked to drive, and their papers", null), "applications",
                e -> ApplicationsForm.show(form)));
        manage.add(Ui.tap(Ui.row(FontImage.MATERIAL_GROUP, "People",
                "Roles, flags, and blocking an account", null), "users",
                e -> UsersForm.show(form, "")));
        manage.add(Ui.tap(Ui.row(FontImage.MATERIAL_RECEIPT_LONG, "Deliveries",
                "The most recent, in every state", null), "deliveries",
                e -> AdminDeliveriesForm.show(form)));
        manage.add(Ui.tap(Ui.row(FontImage.MATERIAL_PAYMENTS, "Payments",
                "What was charged, and refunds", null), "payments",
                e -> PaymentsForm.show(form)));
        manage.add(Ui.tap(Ui.row(FontImage.MATERIAL_MAP, "Live map",
                "Where every car online is", null), "fleet", e -> fleet()));
        manage.add(Ui.tap(Ui.row(FontImage.MATERIAL_SELL, "Pricing",
                "What a delivery costs, and the service's share", null), "pricing",
                e -> PricingForm.show(form)));
        page.add(manage);

        Container screen = new Container(new BorderLayout());
        // The strip the status bar is drawn in is the bar's colour, not
        // whatever is behind the screen.
        screen.setUIID("SvForm");
        screen.setSafeArea(true);
        screen.add(BorderLayout.NORTH, bar);
        screen.add(BorderLayout.CENTER, page);
        form.add(screen);
        menu.install();
        // A narrow window on a desktop is still a desktop's.
        Layouts.dress(form);
        form.addShowListener(e -> {
            if (Layouts.wide()) {
                // Come back to from a screen that was made wide meanwhile.
                CN.callSerially(Nav::home);
                return;
            }
            dashboard.shown();
        });
        Layouts.onChange(form, Nav::home);
        UITimer.timer(POLL_MILLIS, true, form, dashboard::load);
    }

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        if (console != null) {
            console.start();
            return;
        }
        dashboard.start();
        // The window was a console a moment ago, on one of its places: that
        // place is where the admin still is.
        final String place = Console.resumed();
        if (place != null) {
            CN.callSerially(() -> open(place));
        }
    }

    public void show() {
        start();
        form.show();
    }

    private void open(String place) {
        if ("applications".equals(place)) {
            ApplicationsForm.show(form);
        } else if ("users".equals(place)) {
            UsersForm.show(form, "");
        } else if ("deliveries".equals(place)) {
            AdminDeliveriesForm.show(form);
        } else if ("payments".equals(place)) {
            PaymentsForm.show(form);
        } else if ("pricing".equals(place)) {
            PricingForm.show(form);
        }
    }

    private void fleet() {
        new FleetForm(form).show();
    }
}
