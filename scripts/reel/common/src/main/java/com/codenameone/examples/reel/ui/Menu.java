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
package com.codenameone.examples.reel.ui;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.reel.api.AccountDto;
import com.codenameone.examples.reel.api.HouseholdProfileDto;
import com.codenameone.examples.reel.net.Account;
import com.codenameone.examples.reel.net.Session;

/// The menu of a home screen: who is signed in, which of the app's four modes
/// this is, the places that mode has, and the way to another mode.
///
/// Each mode fills in its own places with [#item]; nothing here knows what a
/// viewer's or a creator's are. What is the same for all of them is the top --
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

    /// @param mode the mode `form` is the home of: `Nav.VIEWER` and the others
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
        panel.setUIID("SvMenu");
        panel.setSafeArea(true);
        panel.setGrabsPointerEvents(true);
        panel.setName("menu");
        build();
        // What is left of the screen beside the menu: dimmed, and a tap on it
        // puts the menu away.
        Button dim = new Button("", "SvScrim");
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
        AccountDto user = Session.user();
        Container top = new Container(BoxLayout.y());
        top.setUIID("SvMenuTop");
        Container who = new Container(new BorderLayout());
        Container names = new Container(BoxLayout.y());
        HouseholdProfileDto watching = Nav.VIEWER.equals(mode) ? Account.profile() : null;
        String shown = watching != null ? watching.name() : user.name();
        names.add(Ui.label(shown, "SvMenuName"));
        names.add(Ui.label(user.email(), "SvMenuDetail"));
        who.add(BorderLayout.WEST, Ui.avatar(shown));
        who.add(BorderLayout.CENTER, names);
        top.add(who);
        top.add(FlowLayout.encloseIn(Ui.badge(Nav.ADMIN.equals(mode) ? "Admin"
                : Nav.MODERATION.equals(mode) ? "Moderation"
                : Nav.STUDIO.equals(mode) ? "Studio" : "Watching", "SvModeBadge")));

        Container list = new Container(BoxLayout.y());
        list.setScrollableY(true);
        list.setScrollVisible(false);
        list.add(places);
        if (Nav.VIEWER.equals(mode) && Account.profiles().size() > 1) {
            list.add(entry(FontImage.MATERIAL_SWITCH_ACCOUNT, "Switch profile", "menu-profiles",
                    Nav::switchProfile));
        }
        boolean header = false;
        String[] modes = {Nav.VIEWER, Nav.STUDIO, Nav.MODERATION, Nav.ADMIN};
        char[] icons = {FontImage.MATERIAL_MOVIE, FontImage.MATERIAL_VIDEO_CALL, FontImage.MATERIAL_GAVEL,
                FontImage.MATERIAL_INSIGHTS};
        String[] labels = {"Watch", "Studio", "Moderation", "Administration"};
        for (int iter = 0; iter < modes.length; iter++) {
            if (!modes[iter].equals(mode) && Nav.allowed(user, modes[iter])) {
                if (!header) {
                    list.add(Ui.label("Switch to", "SvMenuLabel"));
                    header = true;
                }
                list.add(other(icons[iter], labels[iter], modes[iter]));
            }
        }

        Container bottom = new Container(BoxLayout.y());
        bottom.setUIID("SvMenuBottom");
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
        Button entry = new Button(text, "SvMenuItem");
        entry.setName(name);
        Ui.icon(entry, icon, 3.8f);
        entry.addActionListener(e -> {
            close();
            go.run();
        });
        return entry;
    }
}
