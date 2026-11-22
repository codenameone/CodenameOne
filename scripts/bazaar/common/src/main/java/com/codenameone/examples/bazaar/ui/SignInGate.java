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
package com.codenameone.examples.bazaar.ui;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.AppConfig;

/// Where a guest is asked to sign in: only on the way to something that is
/// an account's -- the checkout, following a store, the orders, the account
/// itself. Browsing, search and the cart never ask.
///
/// Once signed in (or registered), the errand that asked is carried on with
/// when the account shops; staff and administrators land on their own home.
public final class SignInGate {
    private SignInGate() {
    }

    /// Runs `then` at once when someone is signed in; otherwise explains
    /// `why` and offers to sign in, running `then` afterwards.
    public static void require(Form previous, String title, String why, Runnable then) {
        if (!Nav.guest()) {
            then.run();
            return;
        }
        Nav.afterSignIn(then);
        final Form form = Ui.form("", "SignInGate");
        Ui.back(form, previous);
        Container page = Ui.page();
        page.setName("sign-in-gate");
        page.add(prompt(form, title, why));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    /// Plain sign-in, from Account or the menu: nothing to carry on with.
    public static void signIn(Form previous) {
        Nav.afterSignIn(null);
        SignInForm.show(previous);
    }

    /// The icon, the words and the two ways in, for a screen of its own or
    /// the guest's Account tab.
    public static Container prompt(final Form form, String title, String why) {
        Container box = new Container(BoxLayout.y());
        box.setUIID("ShopGate");
        Label icon = new Label("", "ShopGateIcon");
        Ui.icon(icon, FontImage.MATERIAL_LOCK_OPEN, 7f);
        box.add(icon);
        box.add(Ui.label(title, "ShopGateTitle"));
        box.add(Ui.text(why, "ShopGateText"));
        box.add(Ui.primary("Sign in", "signIn", e -> SignInForm.show(form)));
        box.add(Ui.secondary("Create an account", "register", e -> RegisterForm.show(form)));
        if (AppConfig.serverSwitchable()) {
            box.add(Ui.link("Server address", "server", e -> ServerForm.show(form)));
        }
        return box;
    }
}
