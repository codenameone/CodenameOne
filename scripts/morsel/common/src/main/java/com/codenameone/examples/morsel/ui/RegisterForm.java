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
package com.codenameone.examples.morsel.ui;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.morsel.Telemetry;
import com.codenameone.examples.morsel.api.RegisterDto;
import com.codenameone.examples.morsel.AppConfig;
import com.codenameone.examples.morsel.net.Api;
import com.codenameone.examples.morsel.net.Net;
import com.codenameone.examples.morsel.net.Session;

/// Creates an account, signs in with it and goes on to verify the phone.
///
/// Everyone who registers can delivery. Someone who wants to drive says so here
/// and is taken on to the application; the courier role comes when an admin
/// approves it. Nobody registers as an admin either: that role is granted by
/// another admin, from the admin screens.
public final class RegisterForm {
    private RegisterForm() {
    }

    public static void show(Form previous) {
        final Form form = Ui.form("Create an account", "Register");
        Ui.back(form, previous);
        Container page = Ui.page();
        final TextField name = Ui.field(page, "Name", "name", "What couriers will call you");
        final TextField email = Ui.field(page, "E-mail", "email", "you@example.com",
                TextArea.EMAILADDR);
        final TextField phone = Ui.field(page, "Mobile number", "phone", "+1 555 010 0000",
                TextArea.PHONENUMBER);
        final TextField password = Ui.field(page, "Password", "password",
                "At least 8 characters", TextArea.PASSWORD);

        // Riding is open to everyone the moment they have an account. Onboarding
        // is asked for here and granted later, by an admin who has read the
        // application this choice leads to.
        final String[] wants = {Nav.CUSTOMER};
        page.add(Ui.label("I want to", "WlLabel"));
        page.add(Ui.choice("want", new String[] {"Delivery", "Drive"},
                new String[] {Nav.CUSTOMER, Nav.COURIER}, wants[0], value -> wants[0] = value));
        page.add(Ui.text("Couriers apply after signing up: a few details about you and your "
                + "car, and five photos. You can delivery in the meantime.", "WlMuted"));

        final SpanLabel error = Ui.text("", "WlError");
        error.setName("error");
        final Button create = Ui.primary("Create account", "submit", null);
        create.addActionListener(e -> {
            final boolean onboarding = Nav.COURIER.equals(wants[0]);
            final RegisterDto request = new RegisterDto(email.getText().trim(), password.getText(),
                    name.getText().trim(), phone.getText().trim(), false, null, null, onboarding,
                    null);
            final Net.Failed failed = (status, message) -> {
                create.setEnabled(true);
                error.setText(message);
                Ui.refresh(form);
            };
            create.setEnabled(false);
            error.setText("");
            // The server checks every field and says what is wrong with the
            // first that fails, so nothing is validated twice.
            Api.account().register(request, Net.to(user -> Session.signIn(request.email(),
                    request.password(), () -> {
                        // Someone who came to drive starts on the courier's side,
                        // which for them is the application.
                        AppConfig.setMode(onboarding ? Nav.COURIER : Nav.CUSTOMER, Session.owner(request.email()));
                        Telemetry.registered(onboarding);
                        VerifyPhoneForm.show(Nav::enter);
                    }, failed), failed));
        });
        page.add(error).add(create);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
