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
package com.codenameone.examples.parley.admin;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.parley.ui.Layouts;
import com.codenameone.examples.parley.ui.Menu;
import com.codenameone.examples.parley.ui.Nav;
import com.codenameone.examples.parley.ui.Ui;

/// Administering: what needs an answer, how the service is doing right now and
/// over the last weeks, and the way to everything an admin looks after.
///
/// Everything here is behind `/api/admin`, which the server opens to the
/// `ADMIN` role and to nobody else. That is where the protection is. This
/// screen is reached only when `/api/me` says the user is an admin, but that is
/// so the others are not shown something useless -- not what keeps them out.
///
/// The [Dashboard] comes first, with the places under it and in a drawer,
/// each a screen of its own.
public class AdminForm implements Nav.Home {
    private static final int POLL_MILLIS = 5000;

    private final Form form;
    private final Dashboard dashboard;

    public AdminForm() {
        form = Ui.home("Admin");
        final Menu menu = new Menu(form, Nav.ADMIN);
        dashboard = new Dashboard(new Dashboard.Places() {
            @Override
            public void reports() {
                ReportsForm.show(form);
            }

            @Override
            public void flagged() {
                UsersForm.show(form, UsersForm.FLAGGED);
            }
        });
        menu.item(FontImage.MATERIAL_INSIGHTS, "Dashboard", "home", () -> { });
        menu.item(FontImage.MATERIAL_REPORT, "Reports", "reports", () -> ReportsForm.show(form));
        menu.item(FontImage.MATERIAL_GROUP, "People", "users", () -> UsersForm.show(form, ""));
        menu.item(FontImage.MATERIAL_LINK, "Bridges", "bridges", () -> BridgesForm.show(form));

        // The mode is said beside the title: an admin who also chats should
        // never have to wonder which app this is.
        Container bar = new Container(new BorderLayout());
        bar.setUIID("PlHomeBar");
        bar.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(menu.flatButton()));
        bar.add(BorderLayout.CENTER, FlowLayout.encloseLeftMiddle(
                Ui.label("Operations", "PlHeading")));
        Label admin = Ui.badge("Admin", "PlModeBadge");
        admin.getAllStyles().setMargin(0, 0, 0, 0);
        bar.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(admin));

        Container page = dashboard.page();
        Ui.section(page, "Manage");
        Container manage = new Container(BoxLayout.y());
        manage.add(Ui.tap(Ui.row(FontImage.MATERIAL_REPORT, "Reports",
                "What people reported, and what was done", null), "reports",
                e -> ReportsForm.show(form)));
        manage.add(Ui.tap(Ui.row(FontImage.MATERIAL_GROUP, "People",
                "Roles, flags, and blocking an account", null), "users",
                e -> UsersForm.show(form, "")));
        manage.add(Ui.tap(Ui.row(FontImage.MATERIAL_LINK, "Bridges",
                "Chats linked to Telegram or WhatsApp", null), "bridges",
                e -> BridgesForm.show(form)));
        page.add(manage);

        Container screen = new Container(new BorderLayout());
        screen.setUIID("PlForm");
        screen.setSafeArea(true);
        screen.add(BorderLayout.NORTH, bar);
        screen.add(BorderLayout.CENTER, page);
        form.add(screen);
        menu.install();
        Layouts.dress(form);
        form.addShowListener(e -> dashboard.shown());
        UITimer.timer(POLL_MILLIS, true, form, dashboard::load);
    }

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        dashboard.start();
    }

    public void show() {
        start();
        form.show();
    }
}
