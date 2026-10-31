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
package com.codenameone.examples.dash.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.dash.net.Session;

/// Signing in, and the way to the practice arena without an account.
public final class SignInForm {
    private SignInForm() {
    }

    public static void show() {
        Form f = Ui.form("", "signin");
        f.setLayout(new com.codename1.ui.layouts.LayeredLayout());
        f.setScrollable(false);
        f.add(Kit.backdrop());
        f.getToolbar().hideToolbar();
        Container col = Ui.column();
        col.add(heroes());
        col.add(Ui.label("DASH", "Logo"));
        col.add(FlowLayout.encloseCenter(Ui.label("Run. Race. Build.", "Tagline")));
        final TextField email = field("Email", TextField.EMAILADDR, "email");
        final TextField password = field("Password", TextField.PASSWORD, "password");
        // Fields and both buttons share one panel, so they share one width.
        // The guest run and the server switch sit inside the panel too, so
        // nothing reads over the backdrop's track.
        Button guest = Ui.button("Try a level without an account", "LinkButton", "practiceButton",
                e -> RunForm.solo(com.codenameone.examples.dash.run.Levels.official(
                        com.codenameone.examples.dash.rules.Content.CAUSEWAY), 1L, "guest", null, SignInForm::show));
        Container panel = Ui.panel(email, password,
                Ui.primary("Sign in", "signInButton", e -> {
                    Session.signIn(email.getText().trim(), password.getText(),
                            () -> Ui.later(Nav::signedIn), Ui::failed);
                }),
                Ui.secondary("Create an account", "registerButton", e -> RegisterForm.show()), guest);
        // Which server the app talks to is a setting, not something every
        // player meets before signing in: it is under Settings, signed in.
        col.add(panel);
        Container safe = Ui.centeredColumn(col);
        safe.setSafeArea(true);
        Container front = new Container(BoxLayout.yCenter());
        front.setScrollableY(true);
        front.add(safe);
        f.add(front);
        f.show();
    }

    /// Three runners mid-stride, the middle one in front: the first thing a
    /// new player sees is who they will race.
    static Container heroes() {
        int h = CN.convertToPixels(26);
        Label left = new Label(Portraits.figure("runner-nova", h * 4 / 5), "SignInHero");
        Label mid = new Label(Portraits.figure("runner-dash", h), "SignInHero");
        Label right = new Label(Portraits.figure("runner-kite", h * 4 / 5), "SignInHero");
        Container c = new Container(new FlowLayout(Component.CENTER, Component.BOTTOM));
        c.add(left).add(mid).add(right);
        c.setName("heroes");
        return c;
    }

    static TextField field(String hint, int constraint, String name) {
        TextField t = new TextField("", hint, 20, constraint);
        t.setUIID("GameField");
        t.setName(name);
        return t;
    }
}
