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

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.pulse.Telemetry;
import com.codenameone.examples.pulse.api.RegisterDto;
import com.codenameone.examples.pulse.AppConfig;
import com.codenameone.examples.pulse.net.Api;
import com.codenameone.examples.pulse.net.Net;
import com.codenameone.examples.pulse.net.Session;

/// Creates an account, signs in with it and goes on to verify the phone.
///
/// Everyone who registers is a member, free to look at the timetable and to
/// join a plan. Coaching, the front desk and owning the studio are roles the
/// owner grants; nobody registers into them.
public final class RegisterForm {
    private RegisterForm() {
    }

    public static void show(Form previous) {
        final Form form = Ui.form("Create an account", "Register");
        Ui.back(form, previous);
        Container page = Ui.page();
        final TextField name = Ui.field(page, "Name", "name", "What your coaches will call you");
        final TextField email = Ui.field(page, "E-mail", "email", "you@example.com",
                TextArea.EMAILADDR);
        final TextField phone = Ui.field(page, "Mobile number", "phone", "+1 555 010 0000",
                TextArea.PHONENUMBER);
        final TextField password = Ui.field(page, "Password", "password",
                "At least 8 characters", TextArea.PASSWORD);

        final TextField birth = Ui.field(page, "Date of birth", "birthDate", "YYYY-MM-DD");
        page.add(Ui.text("Some classes are for an age band, so the timetable needs to know. "
                + "Children are added later, from Household.", "SvMuted"));

        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        final Button create = Ui.primary("Create account", "submit", null);
        create.addActionListener(e -> {
            final RegisterDto request = new RegisterDto(email.getText().trim(), password.getText(),
                    name.getText().trim(), phone.getText().trim(), birth.getText().trim(), null);
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
                        AppConfig.setMode(Nav.MEMBER, Session.owner(request.email()));
                        Telemetry.registered();
                        VerifyPhoneForm.show(Nav::enter);
                    }, failed), failed));
        });
        page.add(error).add(create);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
