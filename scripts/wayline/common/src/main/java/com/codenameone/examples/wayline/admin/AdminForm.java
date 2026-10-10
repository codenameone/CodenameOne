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

import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.wayline.ui.Layouts;
import com.codenameone.examples.wayline.ui.Menu;
import com.codenameone.examples.wayline.ui.Nav;
import com.codenameone.examples.wayline.ui.Ui;

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
        menu.item(FontImage.MATERIAL_RECEIPT_LONG, "Rides", "rides",
                () -> AdminRidesForm.show(form));
        menu.item(FontImage.MATERIAL_PAYMENTS, "Payments", "payments",
                () -> PaymentsForm.show(form));
        menu.item(FontImage.MATERIAL_MAP, "Live map", "fleet", this::fleet);
        menu.item(FontImage.MATERIAL_SELL, "Pricing", "pricing", () -> PricingForm.show(form));

        // The bar a title bar would be, with the menu where the map screens
        // have it, and the mode said beside the title: an admin who also rides
        // should never have to wonder which app this is.
        Container bar = new Container(new BorderLayout());
        bar.setUIID("WlHomeBar");
        bar.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(menu.flatButton()));
        bar.add(BorderLayout.CENTER, FlowLayout.encloseLeftMiddle(
                Ui.label("Operations", "WlHeading")));
        // In the menu the badge stands under a name and keeps a gap above it;
        // here it is alone on the bar's centre line.
        Label admin = Ui.badge("Admin", "WlModeBadge");
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
        manage.add(Ui.tap(Ui.row(FontImage.MATERIAL_RECEIPT_LONG, "Rides",
                "The most recent, in every state", null), "rides",
                e -> AdminRidesForm.show(form)));
        manage.add(Ui.tap(Ui.row(FontImage.MATERIAL_PAYMENTS, "Payments",
                "What was charged, and refunds", null), "payments",
                e -> PaymentsForm.show(form)));
        manage.add(Ui.tap(Ui.row(FontImage.MATERIAL_MAP, "Live map",
                "Where every car online is", null), "fleet", e -> fleet()));
        manage.add(Ui.tap(Ui.row(FontImage.MATERIAL_SELL, "Pricing",
                "What a ride costs, and the service's share", null), "pricing",
                e -> PricingForm.show(form)));
        page.add(manage);

        Container screen = new Container(new BorderLayout());
        // The strip the status bar is drawn in is the bar's colour, not
        // whatever is behind the screen.
        screen.setUIID("WlForm");
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
        } else if ("rides".equals(place)) {
            AdminRidesForm.show(form);
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
