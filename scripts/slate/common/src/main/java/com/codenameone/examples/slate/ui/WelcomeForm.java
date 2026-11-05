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
package com.codenameone.examples.slate.ui;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.slate.AppConfig;

/// What a signed-out user sees.
public final class WelcomeForm {
    private WelcomeForm() {
    }

    private static Container feature(char glyph, String text) {
        Container row = new Container(new BorderLayout(), "SvWelcomeFeature");
        Label icon = new Label("", "SvWelcomeFeatureIcon");
        Ui.icon(icon, glyph, 3.6f);
        row.add(BorderLayout.WEST, icon);
        row.add(BorderLayout.CENTER, Ui.text(text, "SvWelcomeFeatureText"));
        return row;
    }

    public static void show() {
        final Form form = Ui.form("", "Welcome");
        form.getContentPane().setUIID("SvWelcome");
        form.getToolbar().hideToolbar();

        Container hero = new Container(BoxLayout.y());
        hero.setUIID("SvWelcome");
        hero.setSafeArea(true);
        Container mark = new Container(new BorderLayout(), "SvInkRow");
        Label icon = new Label("", "SvWelcomeIcon");
        Ui.icon(icon, FontImage.MATERIAL_EVENT_AVAILABLE, 7f);
        mark.add(BorderLayout.WEST, icon);
        hero.add(mark);
        hero.add(Ui.label("Slate", "SvWelcomeTitle"));
        hero.add(Ui.text("Book the people you rely on.", "SvWelcomeTag"));
        hero.add(feature(FontImage.MATERIAL_EVENT_AVAILABLE,
                "See every free time, and only the times that are really free"));
        hero.add(feature(FontImage.MATERIAL_SYNC,
                "Checked against your own calendar before you pick"));
        hero.add(feature(FontImage.MATERIAL_NOTIFICATIONS,
                "In your calendar, with a reminder, the moment it is booked"));

        Container actions = Ui.page();
        actions.setScrollableY(false);
        actions.setUIID("SvWelcomeActions");
        actions.setSafeArea(true);
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
