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
package com.codenameone.examples.splat.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.splat.net.Session;

/// Signing in, and the way to the practice arena without an account.
public final class SignInForm {
    private SignInForm() {
    }

    public static void show() {
        Form f = Ui.form("", "signin");
        f.setLayout(new com.codename1.ui.layouts.LayeredLayout());
        f.setScrollable(false);
        f.setUIID("MenuForm");
        f.add(new com.codenameone.examples.splat.play.OperatorStage("op-swat", "semi",
                CN.getDisplayWidth() > CN.getDisplayHeight() ? 0.9f : 0.55f));
        Label shade = new Label();
        shade.setUIID("MenuShade");
        f.add(shade);
        f.getToolbar().hideToolbar();
        Container col = Ui.column();
        col.add(Ui.label("SPLAT", "Logo"));
        col.add(Ui.label("TEAM PAINTBALL. TACTICAL. ALLEGEDLY.", "Tagline"));
        final TextField email = field("Email", TextField.EMAILADDR, "email");
        final TextField password = field("Password", TextField.PASSWORD, "password");
        // Fields and both buttons share one panel, so they share one width.
        // The guest run and the server switch sit inside the panel too, so
        // nothing reads over the backdrop's track.
        Button guest = Ui.button("PRACTICE VS BOTS -- NO ACCOUNT NEEDED", "LinkButton", "practiceButton",
                e -> MatchForm.practice("lot7:TEAM_PAINT", SignInForm::show));
        Container panel = Ui.panel(email, password,
                Ui.primary("SIGN IN", "signInButton", e -> {
                    Session.signIn(email.getText().trim(), password.getText(),
                            () -> Ui.later(Nav::signedIn), Ui::failed);
                }),
                Ui.secondary("ENLIST", "registerButton", e -> RegisterForm.show()), guest);
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

    static TextField field(String hint, int constraint, String name) {
        TextField t = new TextField("", hint, 20, constraint);
        t.setUIID("GameField");
        t.setName(name);
        return t;
    }
}
