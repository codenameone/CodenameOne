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

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// What a signed-out user sees.
public final class WelcomeForm {
    private WelcomeForm() {
    }

    public static void show() {
        final Form form = Ui.form("", "Welcome");
        form.getToolbar().hideToolbar();

        Container hero = new Container(BoxLayout.y());
        hero.setUIID("WlHero");
        hero.setSafeArea(true);
        Label icon = new Label("", "WlHeroIcon");
        Ui.icon(icon, FontImage.MATERIAL_NEAR_ME, 10f);
        hero.add(icon);
        hero.add(Ui.label("Wayline", "WlHeroTitle"));
        hero.add(Ui.text("A ride when you need one, and a fair way to drive.", "WlHeroText"));

        Container actions = Ui.page();
        actions.setScrollableY(false);
        actions.add(Ui.primary("Sign in", "signIn", e -> SignInForm.show(form)));
        actions.add(Ui.secondary("Create an account", "register", e -> RegisterForm.show(form)));
        actions.add(Ui.link("Server address", "server", e -> ServerForm.show(form)));

        form.add(BorderLayout.CENTER, hero);
        form.add(BorderLayout.SOUTH, actions);
        form.show();
    }
}
