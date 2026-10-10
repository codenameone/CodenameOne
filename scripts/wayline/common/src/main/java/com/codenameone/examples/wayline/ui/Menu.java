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
package com.codenameone.examples.wayline.ui;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.net.Session;

/// The menu of a home screen: who is signed in, which of the app's three modes
/// this is, the places that mode has, and the way to another mode.
///
/// Each mode fills in its own places with [#item]; nothing here knows what a
/// rider's or a driver's are. What is the same for all of them is the top --
/// the person and the mode -- and the bottom: the other modes the account's
/// roles open, Settings, and signing out. An account with one role is offered
/// no other mode, because it has none.
///
/// It slides in over the screen as a layer of the screen's own, not as the
/// title bar's side menu: the home screens have no title bar, and on a desktop
/// the title bar's menu is handed to the window's.
public final class Menu {
    private final Form form;
    private final String mode;
    private final Container layer = new Container(new BorderLayout());
    private final Container places = new Container(BoxLayout.y());
    private final Container panel;

    /// @param mode the mode `form` is the home of: `Nav.RIDER` and the others
    public Menu(Form form, String mode) {
        this.form = form;
        this.mode = mode;
        panel = new Container(new BorderLayout()) {
            @Override
            protected Dimension calcPreferredSize() {
                // Most of a phone's width, and no wider than a menu wants to
                // be on a tablet or a desktop.
                Dimension size = super.calcPreferredSize();
                size.setWidth(Math.min(CN.getDisplayWidth() * 5 / 6, CN.convertToPixels(78f)));
                return size;
            }
        };
        panel.setUIID("WlMenu");
        panel.setSafeArea(true);
        panel.setGrabsPointerEvents(true);
        panel.setName("menu");
        build();
        // What is left of the screen beside the menu: dimmed, and a tap on it
        // puts the menu away.
        Button dim = new Button("", "WlScrim");
        dim.setName("menu-dim");
        dim.addActionListener(e -> close());
        layer.add(BorderLayout.WEST, panel);
        layer.add(BorderLayout.CENTER, dim);
        layer.setHidden(true);
        layer.setVisible(false);
    }

    /// Adds the menu to the screen, closed, as its top layer. Call it after
    /// the screen's own content has been added.
    public void install() {
        form.add(layer);
    }

    /// The round button that opens the menu, for the screen to place.
    public Button button() {
        return Ui.round(FontImage.MATERIAL_MENU, "menu-open", e -> open());
    }

    /// The button that opens the menu, for a screen with a bar and no map.
    public Button flatButton() {
        return Ui.flat(FontImage.MATERIAL_MENU, "menu-open", e -> open());
    }

    /// Adds one of the mode's own places.
    ///
    /// @param name the button's name is `menu-` and this
    public Menu item(char icon, String text, String name, final Runnable go) {
        places.add(entry(icon, text, "menu-" + name, go));
        return this;
    }

    public void open() {
        if (!layer.isHidden()) {
            return;
        }
        layer.setHidden(false);
        layer.setVisible(true);
        form.getContentPane().revalidate();
        // Laid out where it belongs; started from off the edge it comes from,
        // which is the right-hand one in a right-to-left language.
        panel.setX(panel.isRTL() ? layer.getWidth() : -panel.getWidth());
        layer.animateLayout(200);
    }

    public void close() {
        if (layer.isHidden()) {
            return;
        }
        layer.setHidden(true);
        layer.setVisible(false);
        form.getContentPane().revalidate();
    }

    private void build() {
        UserDto user = Session.user();
        Container top = new Container(BoxLayout.y());
        top.setUIID("WlMenuTop");
        Container who = new Container(new BorderLayout());
        Container names = new Container(BoxLayout.y());
        names.add(Ui.label(user.displayName, "WlMenuName"));
        names.add(Ui.label(user.username, "WlMenuDetail"));
        who.add(BorderLayout.WEST, Ui.avatar(user.displayName));
        who.add(BorderLayout.CENTER, names);
        top.add(who);
        // Which of the three apps this is, said in words at the top of each.
        top.add(FlowLayout.encloseIn(Ui.badge(Nav.ADMIN.equals(mode) ? "Admin mode"
                : Nav.DRIVER.equals(mode) ? "Driver mode" : "Rider mode", "WlModeBadge")));

        Container list = new Container(BoxLayout.y());
        list.setScrollableY(true);
        list.setScrollVisible(false);
        list.add(places);
        // The other modes, for an account whose roles open more than one.
        // Everyone may ride; driving and administering are granted.
        if (user.driver || user.admin || !Nav.RIDER.equals(mode)) {
            list.add(Ui.label("Switch to", "WlMenuLabel"));
            if (!Nav.RIDER.equals(mode)) {
                list.add(other(FontImage.MATERIAL_HAIL, "Riding", Nav.RIDER));
            }
            if (user.driver && !Nav.DRIVER.equals(mode)) {
                list.add(other(FontImage.MATERIAL_DIRECTIONS_CAR, "Driving", Nav.DRIVER));
            }
            if (user.admin && !Nav.ADMIN.equals(mode)) {
                list.add(other(FontImage.MATERIAL_INSIGHTS, "Administration", Nav.ADMIN));
            }
        }

        Container bottom = new Container(BoxLayout.y());
        bottom.setUIID("WlMenuBottom");
        bottom.add(entry(FontImage.MATERIAL_SETTINGS, "Settings", "menu-settings",
                () -> SettingsForm.show(form)));
        bottom.add(entry(FontImage.MATERIAL_LOGOUT, "Sign out", "menu-signOut", Nav::signOut));

        panel.add(BorderLayout.NORTH, top);
        panel.add(BorderLayout.CENTER, list);
        panel.add(BorderLayout.SOUTH, bottom);
    }

    private Button other(char icon, String text, final String to) {
        return entry(icon, text, "switch-" + to, () -> Nav.switchTo(to));
    }

    private Button entry(char icon, String text, String name, final Runnable go) {
        Button entry = new Button(text, "WlMenuItem");
        entry.setName(name);
        Ui.icon(entry, icon, 3.8f);
        entry.addActionListener(e -> {
            close();
            go.run();
        });
        return entry;
    }
}
