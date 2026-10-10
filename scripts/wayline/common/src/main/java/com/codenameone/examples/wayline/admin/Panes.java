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

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.wayline.ui.Layouts;
import com.codenameone.examples.wayline.ui.Nav;
import com.codenameone.examples.wayline.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// Where a part of the admin console shows what it has.
///
/// Every part is a list and what is opened from it: an application, an
/// account, a ride. On a phone each of those is a screen of its own, reached
/// from the one before and left with the back arrow. On a wide screen the list
/// stays where it is and what is opened from it is shown beside it. The parts
/// are written once, against this, and do not know which they are in:
///
/// - [#list] shows the list.
/// - [#detail] shows what was picked from it, in place of whatever was open.
/// - [#over] shows something on top of that: a document, the question "why?".
/// - [#back] leaves the last of those; [#home] leaves all of them.
///
/// [#phone(Form)] is the phone's. The wide one is [Console]'s.
abstract class Panes {
    /// Shows a part's list.
    ///
    /// @param name what a test waits for
    /// @param shown run each time the list comes into view, to fetch what is in it
    abstract void list(String title, String name, Component content, Runnable shown);

    /// Shows a part that is one page and has nothing to open: the prices.
    abstract void page(String title, String name, Component content);

    /// Shows what was picked from the list, in place of whatever was open.
    abstract void detail(String title, String name, Component content);

    /// Shows `content` on top of what is open.
    abstract void over(String title, String name, Component content);

    /// Leaves what is on top.
    abstract void back();

    /// Back to the list alone, which is fetched again.
    abstract void home();

    /// Lays out again what is showing, after it changed.
    abstract void refresh();

    /// The screen all of this is on, for something that needs one to return to.
    abstract Form form();

    /// Marks `row` as the one that is open, where the list stays in view.
    void picked(Component row) {
    }

    /// Asks why, on top of what is open. See [Ui#reason].
    void reason(String title, String text, String action, Ui.Reason reason) {
        over(title, "Reason", Ui.reasonPage(text, action, reason));
    }

    /// The panes of a phone: a screen each.
    ///
    /// @param previous where the back arrow of the list leads
    static Panes phone(final Form previous) {
        return new Panes() {
            private final List<Form> screens = new ArrayList<Form>();

            @Override
            void list(String title, String name, Component content, final Runnable shown) {
                Form form = Ui.form(title, name);
                Ui.back(form, previous);
                form.add(BorderLayout.CENTER, content);
                form.addShowListener(e -> shown.run());
                watch(form);
                screens.clear();
                screens.add(form);
                form.show();
            }

            @Override
            void page(String title, String name, Component content) {
                list(title, name, content, () -> { });
            }

            @Override
            void detail(String title, String name, Component content) {
                while (screens.size() > 1) {
                    screens.remove(screens.size() - 1);
                }
                over(title, name, content);
            }

            @Override
            void over(String title, String name, Component content) {
                Form form = Ui.form(title, name);
                Ui.back(form, this::back);
                form.add(BorderLayout.CENTER, content);
                watch(form);
                screens.add(form);
                form.show();
            }

            /// A screen made for a narrow window is of no use to a wide one:
            /// the console is built again, as the wide one. And on a desktop
            /// a narrow window still wears the desktop's look.
            private void watch(Form form) {
                Layouts.dress(form);
                Layouts.onChange(form, Nav::home);
            }

            @Override
            void back() {
                if (screens.size() > 1) {
                    screens.remove(screens.size() - 1);
                }
                form().showBack();
            }

            @Override
            void home() {
                while (screens.size() > 1) {
                    screens.remove(screens.size() - 1);
                }
                form().showBack();
            }

            @Override
            void refresh() {
                Layouts.dress(form());
                Ui.refresh(form());
            }

            @Override
            Form form() {
                return screens.get(screens.size() - 1);
            }
        };
    }

    /// A list with what filters it above it, as the content of [#list].
    static Container listed(Component filters, Component list) {
        Container content = new Container(new BorderLayout());
        if (filters != null) {
            content.add(BorderLayout.NORTH, filters);
        }
        content.add(BorderLayout.CENTER, list);
        return content;
    }
}
