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

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.net.Session;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.Layouts;
import com.codenameone.examples.wayline.ui.Nav;
import com.codenameone.examples.wayline.ui.SettingsForm;
import com.codenameone.examples.wayline.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The admin console on a wide screen: a tablet on its side, a desktop window,
/// a browser.
///
/// One screen, in three parts. The places an admin goes are always in view
/// down the side, where a phone keeps them in a drawer. Beside them is the
/// place that is open: the dashboard, or a list. And beside a list is what was
/// picked from it -- an application, an account, a ride, a receipt -- so that
/// working through a list does not mean going back to it each time.
///
/// The parts themselves are the phone's. Each is written against [Panes], and
/// the ones here put a list and its detail side by side where the phone's
/// make each a screen.
///
/// On a desktop the window has a menu bar, and the places are in it too, each
/// with a key. Where the platform's own theme is installed the console wears
/// it: see [Layouts#dress(Container)].
final class Console {
    private static final int POLL_MILLIS = 5000;
    private static final String HOME = "home";
    private static final String FLEET = "fleet";
    private static final float SIDE_MM = 56f;
    private static final float MASTER_MM = 100f;

    /// The places, in the order they are listed: an id, and what it is called.
    private static final String[][] PLACES = {
        {HOME, "Dashboard"}, {"applications", "Applications"}, {"users", "People"},
        {"rides", "Rides"}, {"payments", "Payments"}, {FLEET, "Live map"},
        {"pricing", "Pricing"}};
    private static final char[] ICONS = {FontImage.MATERIAL_INSIGHTS,
        FontImage.MATERIAL_ASSIGNMENT_IND, FontImage.MATERIAL_GROUP,
        FontImage.MATERIAL_RECEIPT_LONG, FontImage.MATERIAL_PAYMENTS, FontImage.MATERIAL_MAP,
        FontImage.MATERIAL_SELL};

    /// The place that was open when the window stopped being wide, or the
    /// device was turned: where the console opens when it is wide again.
    private static String resume = HOME;

    private final Form form = new Form("", new BorderLayout());
    private final Container side;
    private final Container body = new Container(new BorderLayout());
    private final Button[] items = new Button[PLACES.length];
    private final Wide panes = new Wide();
    private final Dashboard dashboard;
    /// Whether the window, and not the app, has the title bar and the menu.
    private final boolean window;
    private String place = HOME;

    Console() {
        form.setUIID("WlForm");
        form.setName("Admin");
        Ui.watch(form);
        form.setScrollable(false);
        window = CN.isDesktop() && form.getToolbar().getParent() == null;
        if (!window) {
            // Everything a title bar would say is down the side.
            form.getToolbar().hideToolbar();
        }
        dashboard = new Dashboard(true, new Dashboard.Places() {
            @Override
            public void applications() {
                go("applications");
            }

            @Override
            public void flagged() {
                mark("users");
                UsersForm.show(panes, UsersForm.FLAGGED);
            }
        });
        side = side();
        Container screen = new Container(new BorderLayout());
        screen.setSafeArea(true);
        screen.add(BorderLayout.WEST, side);
        screen.add(BorderLayout.CENTER, body);
        form.add(BorderLayout.CENTER, screen);
        if (window) {
            menus();
        }
        form.addShowListener(e -> {
            if (!Layouts.wide()) {
                // It was left wide, for the map or the settings, and the
                // window was made narrow while it was away.
                narrowed();
                return;
            }
            if (HOME.equals(place)) {
                dashboard.shown();
            }
        });
        form.addSizeChangedListener(e -> dashboard.fit());
        Layouts.onChange(form, this::narrowed);
        UITimer.timer(POLL_MILLIS, true, form, () -> {
            if (HOME.equals(place)) {
                dashboard.load();
            }
        });
    }

    Form form() {
        return form;
    }

    /// Fills the console in, at the place it was last on.
    void start() {
        dashboard.start();
        String to = resume;
        resume = HOME;
        go(to);
    }

    private void narrowed() {
        resume = place;
        Nav.home();
    }

    /// The place a narrow console should open, having just stopped being a
    /// wide one; null when there is none, which is the dashboard.
    static String resumed() {
        String to = resume;
        resume = HOME;
        return HOME.equals(to) ? null : to;
    }

    /// Opens one of the places.
    private void go(String id) {
        if (FLEET.equals(id)) {
            // A map wants the whole window.
            new FleetForm(form).show();
            return;
        }
        mark(id);
        if ("applications".equals(id)) {
            ApplicationsForm.show(panes);
        } else if ("users".equals(id)) {
            UsersForm.show(panes, "");
        } else if ("rides".equals(id)) {
            AdminRidesForm.show(panes);
        } else if ("payments".equals(id)) {
            PaymentsForm.show(panes);
        } else if ("pricing".equals(id)) {
            PricingForm.show(panes);
        } else {
            panes.clear();
            body.removeAll();
            body.add(BorderLayout.CENTER, dashboard.page());
            title("Dashboard");
            Ui.viewed("Admin");
            panes.refresh();
            if (form.isVisible() && CN.getCurrentForm() == form) {
                dashboard.shown();
            }
        }
    }

    /// Shows `id` as the place that is open.
    private void mark(String id) {
        place = id;
        for (int iter = 0; iter < items.length; iter++) {
            items[iter].setUIID(PLACES[iter][0].equals(id) ? "WlSidePicked" : "WlSideItem");
        }
        Layouts.dress(side);
        // An icon is drawn in the colour of the style it was made for.
        for (int iter = 0; iter < items.length; iter++) {
            Ui.icon(items[iter], ICONS[iter], 3.4f);
        }
    }

    private void title(String title) {
        if (window) {
            form.setTitle(Lang.tr(title));
        }
    }

    private Container side() {
        Container panel = new Container(new BorderLayout()) {
            @Override
            protected Dimension calcPreferredSize() {
                Dimension size = super.calcPreferredSize();
                size.setWidth(CN.convertToPixels(SIDE_MM));
                return size;
            }
        };
        panel.setUIID("WlSide");
        panel.setName("side");
        UserDto user = Session.user();
        Container who = new Container(BoxLayout.y());
        who.add(Ui.plain(user.displayName, "WlSideName"));
        who.add(Ui.plain(user.username, "WlSideDetail"));

        Container list = new Container(BoxLayout.y());
        list.setScrollableY(true);
        list.setScrollVisible(false);
        for (int iter = 0; iter < PLACES.length; iter++) {
            final String id = PLACES[iter][0];
            items[iter] = entry(ICONS[iter], PLACES[iter][1], "nav-" + id, () -> go(id));
            list.add(items[iter]);
        }
        // The other modes this account's roles open. Everyone may ride.
        list.add(Ui.label("Switch to", "WlSideLabel"));
        list.add(entry(FontImage.MATERIAL_HAIL, "Riding", "switch-" + Nav.RIDER,
                () -> Nav.switchTo(Nav.RIDER)));
        if (user.driver) {
            list.add(entry(FontImage.MATERIAL_DIRECTIONS_CAR, "Driving", "switch-" + Nav.DRIVER,
                    () -> Nav.switchTo(Nav.DRIVER)));
        }

        Container bottom = new Container(BoxLayout.y());
        bottom.add(entry(FontImage.MATERIAL_SETTINGS, "Settings", "menu-settings",
                () -> SettingsForm.show(form)));
        bottom.add(entry(FontImage.MATERIAL_LOGOUT, "Sign out", "menu-signOut", Nav::signOut));

        panel.add(BorderLayout.NORTH, who);
        panel.add(BorderLayout.CENTER, list);
        panel.add(BorderLayout.SOUTH, bottom);
        return panel;
    }

    private static Button entry(char icon, String text, String name, final Runnable go) {
        Button entry = new Button(text, "WlSideItem");
        entry.setName(name);
        Ui.icon(entry, icon, 3.4f);
        entry.addActionListener(e -> go.run());
        return entry;
    }

    /// The window's menu bar: the places under Go, each with a key, and
    /// Settings and signing out where each platform keeps them. Called only
    /// where the window has a menu bar, which is where commands put in the
    /// title bar's side menu are shown.
    private void menus() {
        for (int iter = 0; iter < PLACES.length; iter++) {
            final String id = PLACES[iter][0];
            Command go = Command.create(Lang.tr(PLACES[iter][1]), null, e -> go(id));
            go.setDesktopMenu(Lang.tr("Go"));
            go.setDesktopShortcut((char) ('1' + iter));
            form.getToolbar().addCommandToSideMenu(go);
        }
        Command settings = Command.create(Lang.tr("Settings"), null,
                e -> SettingsForm.show(form));
        settings.setDesktopMenu(Command.DESKTOP_MENU_PREFERENCES);
        settings.setDesktopShortcut(',');
        form.getToolbar().addCommandToSideMenu(settings);
        Command signOut = Command.create(Lang.tr("Sign out"), null, e -> Nav.signOut());
        signOut.setDesktopMenu(Command.DESKTOP_MENU_FILE);
        form.getToolbar().addCommandToSideMenu(signOut);
    }

    /// One thing shown in the detail's place.
    private static final class Shown {
        final String title;
        final String name;
        final Component content;

        Shown(String title, String name, Component content) {
            this.title = title;
            this.name = name;
            this.content = content;
        }
    }

    /// The panes of a wide screen: the list on the left of the room, and what
    /// is opened from it on the right.
    private final class Wide extends Panes {
        private final List<Shown> open = new ArrayList<Shown>();
        private final Container detail = new Container(new BorderLayout());
        private Runnable shown;
        private Component picked;

        void clear() {
            open.clear();
            shown = null;
            picked = null;
        }

        @Override
        void list(String title, String name, Component content, Runnable shown) {
            clear();
            this.shown = shown;
            Container master = new Container(new BorderLayout()) {
                @Override
                protected Dimension calcPreferredSize() {
                    Dimension size = super.calcPreferredSize();
                    // A list is as wide as a phone, and never most of the room.
                    size.setWidth(Math.min(CN.convertToPixels(MASTER_MM),
                            body.getWidth() > 0 ? body.getWidth() / 2 : Integer.MAX_VALUE));
                    return size;
                }
            };
            master.setUIID("WlMaster");
            master.setName(name);
            master.add(BorderLayout.CENTER, content);
            body.removeAll();
            body.add(BorderLayout.WEST, master);
            body.add(BorderLayout.CENTER, detail);
            title(title);
            Ui.viewed(name);
            show();
            shown.run();
        }

        @Override
        void page(String title, String name, Component content) {
            clear();
            open.add(new Shown(title, name, content));
            body.removeAll();
            body.add(BorderLayout.CENTER, detail);
            title(title);
            show();
        }

        @Override
        void detail(String title, String name, Component content) {
            open.clear();
            over(title, name, content);
        }

        @Override
        void over(String title, String name, Component content) {
            open.add(new Shown(title, name, content));
            show();
        }

        @Override
        void back() {
            if (!open.isEmpty()) {
                open.remove(open.size() - 1);
            }
            show();
        }

        @Override
        void home() {
            open.clear();
            picked = null;
            show();
            if (shown != null) {
                shown.run();
            }
        }

        @Override
        void refresh() {
            Layouts.dress(form);
            Ui.refresh(form);
        }

        @Override
        Form form() {
            return form;
        }

        @Override
        void picked(Component row) {
            if (picked != null) {
                picked.setUIID("WlRow");
            }
            picked = row;
            row.setUIID("WlRowPicked");
        }

        /// Puts what is on top in the detail's place, or says that nothing is
        /// picked.
        private void show() {
            detail.removeAll();
            if (open.isEmpty()) {
                Container none = Ui.empty(FontImage.MATERIAL_TOUCH_APP, "Nothing selected",
                        "Pick a row from the list to see it here.");
                none.setName("nothing");
                detail.add(BorderLayout.CENTER, none);
            } else {
                Shown top = open.get(open.size() - 1);
                Ui.viewed(top.name);
                Container bar = new Container(new BorderLayout());
                bar.setUIID("WlPaneBar");
                // Something shown over the detail is left the way it is on a
                // phone: back. The detail itself has nowhere to go back to.
                if (open.size() > 1) {
                    Button back = Ui.flat(FontImage.MATERIAL_ARROW_BACK, "pane-back",
                            e -> back());
                    bar.add(BorderLayout.WEST, back);
                }
                bar.add(BorderLayout.CENTER, Ui.label(top.title, "WlPaneTitle"));
                Container holder = new Container(new BorderLayout());
                holder.setName(top.name);
                // It may still be in the holder it was shown in before
                // something was put over it.
                top.content.remove();
                holder.add(BorderLayout.CENTER, top.content);
                detail.add(BorderLayout.NORTH, bar);
                detail.add(BorderLayout.CENTER, holder);
            }
            refresh();
        }
    }
}
