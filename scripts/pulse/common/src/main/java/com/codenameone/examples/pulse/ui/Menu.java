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
package com.codenameone.examples.pulse.ui;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.pulse.api.UserDto;
import com.codenameone.examples.pulse.net.Session;
import java.util.ArrayList;
import java.util.List;

/// The menu of a home screen: who is signed in, which of the app's four modes
/// this is, the places that mode has, and the way to another mode.
///
/// Each mode fills in its own places with [#item]; nothing here knows what a
/// member's or the front desk's are. What is the same for all of them is the top --
/// the person and the mode -- and the bottom: the other modes the account's
/// roles open, Settings, and signing out. An account with one role is offered
/// no other mode, because it has none.
///
/// It slides in over the screen as a layer of the screen's own, not as the
/// title bar's side menu: the home screens have no title bar, and on a desktop
/// the title bar's menu is handed to the window's.
///
/// On a wide window -- a tablet on its side, a desktop, a browser -- there is
/// room to keep it in view, so it is docked down the side of the home screen
/// and of every screen opened from it, with the place that is open marked, and
/// the button that opens it is not shown. See [Layouts#wide()].
public final class Menu {
    /// The menu of the home screen showing on a wide window, which every
    /// screen opened from it docks beside its own content; null on a phone
    /// and while nobody is signed in.
    private static Menu docked;
    /// The width of the docked menu, in millimetres.
    private static final float SIDE_MM = 58f;
    /// The name of the place last opened from the docked menu.
    private static String picked;
    /// The client property that marks a screen whose menu is docked.
    private static final String DOCKED = "pulse.docked";
    private final Form form;
    private final String mode;
    /// The mode's places as they were added: icon, words, name and what each does.
    private final List<Object[]> items = new ArrayList<Object[]>();
    private final Container layer = new Container(new BorderLayout());
    private final Container places = new Container(BoxLayout.y());
    private final Container panel;

    /// @param mode the mode `form` is the home of: `Nav.MEMBER` and the others
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
        build(panel, places);
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
        if (Layouts.wide()) {
            docked = this;
            picked = items.isEmpty() ? null : (String) items.get(0)[2];
            dock(form);
            return;
        }
        form.add(layer);
    }

    /// Docks the menu of the home screen down the side of `screen`, on a wide
    /// window while someone is signed in; does nothing anywhere else. A
    /// screen whose content is laid out in borders gets it as its west, any
    /// other has its content moved beside it.
    public static void dock(Form screen) {
        if (docked == null || !Layouts.wide()
                || Boolean.TRUE.equals(screen.getClientProperty(DOCKED))) {
            return;
        }
        screen.putClientProperty(DOCKED, Boolean.TRUE);
        // A screen that is one of the places marks it; one opened from a
        // place -- a member, an account -- keeps the place it came from.
        for (int iter = 0; iter < docked.items.size(); iter++) {
            Object[] item = docked.items.get(iter);
            if (Lang.tr((String) item[1]).equals(screen.getTitle())) {
                picked = (String) item[2];
            }
        }
        Container side = docked.side();
        if (screen instanceof Screen) {
            ((Screen) screen).dockBeside(side);
            return;
        }
        // A home screen: its content is layered, and built once.
        Container content = screen.getContentPane();
        Container rest = new Container(new com.codename1.ui.layouts.LayeredLayout());
        while (content.getComponentCount() > 0) {
            com.codename1.ui.Component child = content.getComponentAt(0);
            content.removeComponent(child);
            rest.add(child);
        }
        Container both = new Container(new BorderLayout());
        both.add(BorderLayout.WEST, side);
        both.add(BorderLayout.CENTER, rest);
        content.add(both);
    }

    /// Forgets the docked menu: nobody is signed in any more.
    public static void undock() {
        docked = null;
        picked = null;
    }

    /// A fresh copy of the menu for the side of a screen, with the place that
    /// is open marked.
    private Container side() {
        Container dock = new Container(new BorderLayout()) {
            @Override
            protected Dimension calcPreferredSize() {
                // As wide on every screen, whatever its longest entry.
                Dimension size = super.calcPreferredSize();
                size.setWidth(CN.convertToPixels(SIDE_MM));
                return size;
            }
        };
        dock.setUIID("SvMenuDock");
        dock.setName("menu");
        Container docks = new Container(BoxLayout.y());
        for (int iter = 0; iter < items.size(); iter++) {
            Object[] item = items.get(iter);
            final String name = (String) item[2];
            final Runnable go = (Runnable) item[3];
            // Marked before the place opens, so that its screen docks with it marked.
            Button entry = entry(((Character) item[0]).charValue(), (String) item[1],
                    "menu-" + name, () -> {
                        picked = name;
                        go.run();
                    });
            if (name.equals(picked)) {
                entry.setUIID("SvMenuItemPicked");
            }
            docks.add(entry);
        }
        build(dock, docks);
        return dock;
    }

    /// The round button that opens the menu, for the screen to place.
    public Button button() {
        Button button = Ui.round(FontImage.MATERIAL_MENU, "menu-open", e -> open());
        button.setAccessibilityText(Lang.tr("Menu"));
        return button;
    }

    /// The button that opens the menu, for a screen with a bar and no map.
    public Button flatButton() {
        Button button = Ui.flat(FontImage.MATERIAL_MENU, "Menu", "menu-open", e -> open());
        // Docked, the menu is in view and there is nothing to open.
        button.setHidden(Layouts.wide());
        return button;
    }

    /// Adds one of the mode's own places.
    ///
    /// @param name the button's name is `menu-` and this
    public Menu item(char icon, String text, String name, final Runnable go) {
        places.add(entry(icon, text, "menu-" + name, go));
        items.add(new Object[] {Character.valueOf(icon), text, name, go});
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

    private void build(Container panel, Container places) {
        UserDto user = Session.user();
        Container top = new Container(BoxLayout.y());
        top.setUIID("SvMenuTop");
        Container who = new Container(new BorderLayout());
        Container names = new Container(BoxLayout.y());
        names.add(Ui.label(user.displayName(), "SvMenuName"));
        names.add(Ui.label(user.username(), "SvMenuDetail"));
        who.add(BorderLayout.WEST, Ui.avatar(user.displayName()));
        who.add(BorderLayout.CENTER, names);
        top.add(who);
        // Which of the three apps this is, said in words at the top of each.
        top.add(FlowLayout.encloseIn(Ui.badge(Nav.OWNER.equals(mode) ? "Owner mode"
                : Nav.TRAINER.equals(mode) ? "Trainer mode"
                : Nav.DESK.equals(mode) ? "Front desk" : "Member", "SvModeBadge")));

        Container list = new Container(BoxLayout.y());
        list.setScrollableY(true);
        list.setScrollVisible(false);
        list.add(places);
        // The other modes, for an account whose roles open more than one.
        // Every account is a member; coaching, the desk and owning are granted.
        if (user.trainer() || user.desk() || user.owner() || !Nav.MEMBER.equals(mode)) {
            list.add(Ui.label("Switch to", "SvMenuLabel"));
            if (!Nav.MEMBER.equals(mode)) {
                list.add(other(FontImage.MATERIAL_FITNESS_CENTER, "Member", Nav.MEMBER));
            }
            if (user.desk() && !Nav.DESK.equals(mode)) {
                list.add(other(FontImage.MATERIAL_QR_CODE_SCANNER, "Front desk", Nav.DESK));
            }
            if (user.trainer() && !Nav.TRAINER.equals(mode)) {
                list.add(other(FontImage.MATERIAL_SPORTS, "Coaching", Nav.TRAINER));
            }
            if (user.owner() && !Nav.OWNER.equals(mode)) {
                list.add(other(FontImage.MATERIAL_INSIGHTS, "Studio owner", Nav.OWNER));
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
