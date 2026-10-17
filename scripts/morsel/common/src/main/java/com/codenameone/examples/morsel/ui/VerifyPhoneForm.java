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
import com.codenameone.examples.morsel.api.PhoneCodeDto;
import com.codenameone.examples.morsel.api.UserDto;
import com.codenameone.examples.morsel.net.Api;
import com.codenameone.examples.morsel.net.Net;
import com.codenameone.examples.morsel.net.Session;

/// Proves the phone number is the user's, which the server asks for before the
/// first delivery.
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
        page.add(Ui.text(user == null || user.phone() == null
                ? Lang.tr("We sent a six-digit code to your phone.")
                : Lang.tr("We sent a six-digit code to {0}.", user.phone()), "WlText"));
        final SpanLabel demo = Ui.text("", "WlNotice");
        demo.setName("demoCode");
        demo.setHidden(true);
        page.add(demo);
        final TextField code = Ui.field(page, "Code", "code", "000000", TextArea.NUMERIC);
        final SpanLabel error = Ui.text("", "WlError");
        error.setName("error");
        final Button verify = Ui.primary("Verify", "submit", null);
        verify.addActionListener(e -> {
            PhoneCodeDto entered = new PhoneCodeDto(code.getText().trim());
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
                    if (challenge.demoCode() != null && challenge.demoCode().length() > 0) {
                        demo.setText(Lang.tr("This is a development server, so no text was "
                                + "sent. Your code is {0}.", challenge.demoCode()));
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
