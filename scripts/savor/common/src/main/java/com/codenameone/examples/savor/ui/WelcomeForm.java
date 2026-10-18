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
package com.codenameone.examples.savor.ui;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.savor.AppConfig;

/// What a signed-out user sees.
public final class WelcomeForm {
    private WelcomeForm() {
    }

    public static void show() {
        final Form form = Ui.form("", "Welcome");
        form.getToolbar().hideToolbar();

        Container hero = new Container(BoxLayout.y());
        hero.setUIID("SvHero");
        hero.setSafeArea(true);
        Label icon = new Label("", "SvHeroIcon");
        Ui.icon(icon, FontImage.MATERIAL_RESTAURANT_MENU, 10f);
        hero.add(icon);
        hero.add(Ui.label("Savor", "SvHeroTitle"));
        hero.add(Ui.text("Good food from places nearby, brought to your door.", "SvHeroText"));

        Container actions = Ui.page();
        actions.setScrollableY(false);
        actions.add(Ui.primary("Sign in", "signIn", e -> SignInForm.show(form)));
        actions.add(Ui.secondary("Create an account", "register", e -> RegisterForm.show(form)));
        if (AppConfig.serverSwitchable()) {
            actions.add(Ui.link("Server address", "server", e -> ServerForm.show(form)));
        }

        form.add(BorderLayout.CENTER, hero);
        form.add(BorderLayout.SOUTH, actions);
        form.show();
    }
}
