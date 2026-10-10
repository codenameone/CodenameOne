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

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.RegisterDto;
import com.codenameone.examples.wayline.AppConfig;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.net.Session;

/// Creates an account, signs in with it and goes on to verify the phone.
///
/// Everyone who registers can ride. Someone who wants to drive says so here
/// and is taken on to the application; the driver role comes when an admin
/// approves it. Nobody registers as an admin either: that role is granted by
/// another admin, from the admin screens.
public final class RegisterForm {
    private RegisterForm() {
    }

    public static void show(Form previous) {
        final Form form = Ui.form("Create an account", "Register");
        Ui.back(form, previous);
        Container page = Ui.page();
        final TextField name = Ui.field(page, "Name", "name", "What drivers will call you");
        final TextField email = Ui.field(page, "E-mail", "email", "you@example.com",
                TextArea.EMAILADDR);
        final TextField phone = Ui.field(page, "Mobile number", "phone", "+1 555 010 0000",
                TextArea.PHONENUMBER);
        final TextField password = Ui.field(page, "Password", "password",
                "At least 8 characters", TextArea.PASSWORD);

        // Riding is open to everyone the moment they have an account. Driving
        // is asked for here and granted later, by an admin who has read the
        // application this choice leads to.
        final String[] wants = {Nav.RIDER};
        page.add(Ui.label("I want to", "WlLabel"));
        page.add(Ui.choice("want", new String[] {"Ride", "Drive"},
                new String[] {Nav.RIDER, Nav.DRIVER}, wants[0], value -> wants[0] = value));
        page.add(Ui.text("Drivers apply after signing up: a few details about you and your "
                + "car, and five photos. You can ride in the meantime.", "WlMuted"));

        final SpanLabel error = Ui.text("", "WlError");
        error.setName("error");
        final Button create = Ui.primary("Create account", "submit", null);
        create.addActionListener(e -> {
            final RegisterDto request = new RegisterDto();
            request.displayName = name.getText().trim();
            request.email = email.getText().trim();
            request.phone = phone.getText().trim();
            request.password = password.getText();
            final boolean driving = Nav.DRIVER.equals(wants[0]);
            request.wantsToDrive = driving;
            final Net.Failed failed = (status, message) -> {
                create.setEnabled(true);
                error.setText(message);
                Ui.refresh(form);
            };
            create.setEnabled(false);
            error.setText("");
            // The server checks every field and says what is wrong with the
            // first that fails, so nothing is validated twice.
            Api.account().register(request, Net.to(user -> Session.signIn(request.email,
                    request.password, () -> {
                        // Someone who came to drive starts on the driver's side,
                        // which for them is the application.
                        AppConfig.setMode(driving ? Nav.DRIVER : Nav.RIDER, request.email);
                        Telemetry.registered(driving);
                        VerifyPhoneForm.show(Nav::enter);
                    }, failed), failed));
        });
        page.add(error).add(create);
        form.add(BorderLayout.CENTER, page);
        form.show();
    }
}
