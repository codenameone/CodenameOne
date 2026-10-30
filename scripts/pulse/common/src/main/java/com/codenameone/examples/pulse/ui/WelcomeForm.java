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

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.pulse.AppConfig;

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
        Ui.icon(icon, FontImage.MATERIAL_MONITOR_HEART, 7f);
        mark.add(BorderLayout.WEST, icon);
        Beat beat = new Beat("SvBeat", true);
        beat.setPreferredH(com.codename1.ui.CN.convertToPixels(9));
        mark.add(BorderLayout.CENTER, beat);
        hero.add(mark);
        hero.add(Ui.label("Pulse", "SvWelcomeTitle"));
        hero.add(Ui.text("Your studio in your pocket.", "SvWelcomeTag"));
        hero.add(feature(FontImage.MATERIAL_EVENT_AVAILABLE,
                "Book classes for you and your family in two taps"));
        hero.add(feature(FontImage.MATERIAL_QR_CODE,
                "Walk in with a pass on your phone or your watch"));
        hero.add(feature(FontImage.MATERIAL_EMOJI_EVENTS,
                "Earn points and badges for every class you show up to"));

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
