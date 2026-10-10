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
import com.codenameone.examples.wayline.api.PhoneCodeDto;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.net.Session;

/// Proves the phone number is the user's, which the server asks for before the
/// first ride.
///
/// The code is the server's: it makes it up, texts it, and is the only one who
/// checks it. The app never sees the code unless the server is a development
/// one with no SMS provider configured, which sends it back so the flow can be
/// tried without a phone.
public final class VerifyPhoneForm {
    private VerifyPhoneForm() {
    }

    /// @param after where to go once the number is verified, or the user
    ///     decides to leave it for later
    public static void show(final Runnable after) {
        final Form form = Ui.form("Verify your number", "VerifyPhone");
        UserDto user = Session.user();
        Container page = Ui.page();
        page.add(Ui.text(user == null || user.phone == null
                ? Lang.tr("We sent a six-digit code to your phone.")
                : Lang.tr("We sent a six-digit code to {0}.", user.phone), "WlText"));
        final SpanLabel demo = Ui.text("", "WlNotice");
        demo.setName("demoCode");
        demo.setHidden(true);
        page.add(demo);
        final TextField code = Ui.field(page, "Code", "code", "000000", TextArea.NUMERIC);
        final SpanLabel error = Ui.text("", "WlError");
        error.setName("error");
        final Button verify = Ui.primary("Verify", "submit", null);
        verify.addActionListener(e -> {
            PhoneCodeDto entered = new PhoneCodeDto();
            entered.code = code.getText().trim();
            verify.setEnabled(false);
            error.setText("");
            Api.account().verifyPhone(entered, Net.to(verified -> {
                Session.setUser(verified);
                Telemetry.phoneVerified();
                after.run();
            }, (status, message) -> {
                verify.setEnabled(true);
                error.setText(message);
                Ui.refresh(form);
            }));
        });
        final Runnable send = () -> Api.account().startPhoneVerification(Net.to(challenge -> {
                    if (challenge.demoCode != null && challenge.demoCode.length() > 0) {
                        demo.setText(Lang.tr("This is a development server, so no text was "
                                + "sent. Your code is {0}.", challenge.demoCode));
                        demo.setHidden(false);
                        Ui.refresh(form);
                    }
                }, (status, message) -> {
                    error.setText(message);
                    Ui.refresh(form);
                }));
        page.add(error).add(verify);
        page.add(Ui.link("Send a new code", "resend", e -> send.run()));
        page.add(Ui.link("Not now", "skip", e -> after.run()));
        form.add(BorderLayout.CENTER, page);
        form.show();
        send.run();
    }
}
