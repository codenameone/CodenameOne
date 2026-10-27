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
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// The frame every home screen shares: a bar with the menu button, a title
/// and the screen's own actions, the content under it, and the menu over
/// both. A mode builds one, adds its places to [#menu()] and puts its pages
/// in with [#show(String, Component)].
public final class Shell {
    private final Form form;
    private final Menu menu;
    private final Container body = new Container(new BorderLayout());
    private final Container actions = new Container(BoxLayout.x());
    private final com.codename1.ui.Label title = new com.codename1.ui.Label("", "SvBarTitle");
    private final Container middle = new Container(new BorderLayout());
    private final String mode;

    public Shell(String name, String mode) {
        form = Ui.home(name);
        this.mode = mode;
        menu = new Menu(form, mode);
        Container bar = new Container(new BorderLayout());
        bar.setUIID("SvBar");
        bar.setSafeArea(true);
        bar.add(BorderLayout.WEST, menu.flatButton());
        title.setName("title");
        middle.add(BorderLayout.WEST, title);
        bar.add(BorderLayout.CENTER, middle);
        bar.add(BorderLayout.EAST, actions);
        Container frame = new Container(new BorderLayout());
        frame.add(BorderLayout.NORTH, bar);
        frame.add(BorderLayout.CENTER, body);
        form.add(frame);
    }

    public Form form() {
        return form;
    }

    public Menu menu() {
        return menu;
    }

    /// Adds a button to the bar.
    public Shell action(Button b) {
        actions.add(b);
        return this;
    }

    /// Replaces the page under the bar.
    public void show(String heading, Component page) {
        title.setText(Lang.tr(heading));
        body.removeAll();
        body.add(BorderLayout.CENTER, page);
        form.revalidate();
    }

    /// Call once everything is added: the menu goes on top.
    ///
    /// On a wide screen a viewer also gets the places in the bar itself, the
    /// way a streaming site on a desktop shows them: a menu behind a button is
    /// a phone's answer to having no room, and a desktop window has room. The
    /// menu stays, for the modes, Settings and signing out.
    public void done() {
        if (Layouts.wide() && Nav.VIEWER.equals(mode)) {
            Container nav = new Container(BoxLayout.x());
            nav.setUIID("RlNav");
            java.util.List<Object[]> links = menu.links();
            for (int iter = 0; iter < links.size(); iter++) {
                final Object[] link = links.get(iter);
                Object go = link[2];
                if (!(go instanceof Runnable)) {
                    continue;
                }
                final Runnable run = (Runnable) go;
                Button b = new Button(Lang.tr(String.valueOf(link[0])), iter == 0 ? "RlNavPicked" : "RlNavLink");
                b.setName("nav-" + link[1]);
                b.addActionListener(e -> run.run());
                nav.add(b);
            }
            middle.add(BorderLayout.CENTER, nav);
        }
        menu.install();
    }
}
