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

import com.codename1.appkit.Net;
import com.codename1.components.SpanLabel;
import com.codename1.qrcode.QrCode;
import com.codename1.qrcode.QrCodeImage;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.TwoFactorCodeDto;
import com.codenameone.examples.bazaar.api.TwoFactorDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.net.SecondFactor;
import com.codenameone.examples.bazaar.net.Session;

import java.util.List;

/// Two-factor sign-in for the account: turning it on with an authenticator
/// app (the QR code, then a code from the app), the recovery codes it answers
/// once, and turning it off with the password. And the screen a sign-in shows
/// when the account asks for a code.
public final class TwoFactorForm {
    private TwoFactorForm() {
    }

    /// Makes the sign-in ask for a code on a screen of its own, over
    /// `previous`.
    public static void askCodesOver(final Form previous) {
        Session.setCodePrompt((code, cancelled) -> askCode(previous, code, cancelled));
    }

    private static void askCode(final Form previous, final Net.Ok<String> code,
            final Runnable cancelled) {
        final Form form = Ui.form("Authenticator code", "TwoFactorCode");
        // One answer per prompt: a double tap on Continue (or Back after it)
        // would hand the waiting request a second answer.
        final boolean[] answered = new boolean[1];
        Ui.back(form, () -> {
            if (answered[0]) {
                return;
            }
            answered[0] = true;
            previous.showBack();
            cancelled.run();
        });
        Container page = Ui.page();
        page.add(Ui.text("Enter the code your authenticator app shows, or one of your recovery "
                + "codes.", "SvText"));
        final TextField field = Ui.field(page, "Code", "code", "123456", TextArea.ANY);
        page.add(Ui.primary("Continue", "submitCode", e -> {
            if (answered[0]) {
                return;
            }
            answered[0] = true;
            previous.showBack();
            code.got(SecondFactor.clean(field.getText()));
        }));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    public static void show(final Form previous) {
        final Form form = Ui.form("Two-factor sign-in", "TwoFactor");
        Ui.back(form, previous);
        final Container page = Ui.page();
        form.add(BorderLayout.CENTER, page);
        form.show();
        Api.twoFactor().status(Net.to(status -> render(form, page, status), Ui::fail));
    }

    private static void render(final Form form, final Container page, TwoFactorDto status) {
        page.removeAll();
        if (status.enabled()) {
            page.add(Ui.callout(com.codename1.ui.FontImage.MATERIAL_VERIFIED_USER,
                    "Two-factor sign-in is on", Lang.tr("{0} recovery codes left.",
                            status.recoveryLeft())));
            final TextField password = Ui.field(page, "Password", "password", "",
                    TextArea.PASSWORD);
            page.add(Ui.secondary("New recovery codes", "newRecoveryCodes", e ->
                    Api.twoFactor().recovery(new TwoFactorCodeDto("", password.getText()),
                            Net.to(fresh -> codes(form, page, fresh), Ui::fail))));
            List<String> requiredBy = status.requiredBy();
            if (requiredBy != null && !requiredBy.isEmpty()) {
                StringBuilder names = new StringBuilder();
                for (int iter = 0; iter < requiredBy.size(); iter++) {
                    names.append(iter == 0 ? "" : ", ").append(requiredBy.get(iter));
                }
                page.add(Ui.text(Lang.tr("Required by {0}, so it cannot be turned off.",
                        names.toString()), "SvText"));
            } else {
                page.add(Ui.danger("Turn off", "disableTwoFactor", e ->
                        Api.twoFactor().disable(new TwoFactorCodeDto("", password.getText()),
                                Net.to(off -> render(form, page, off), Ui::fail))));
            }
        } else {
            page.add(Ui.text("Ask for a code from an authenticator app as well as the password "
                    + "whenever this account signs in.", "SvText"));
            page.add(Ui.primary("Set up", "startTwoFactor", e ->
                    Api.twoFactor().start(Net.to(started -> enrol(form, page, started),
                            Ui::fail))));
        }
        Ui.refresh(form);
    }

    private static void enrol(final Form form, final Container page, TwoFactorDto started) {
        page.removeAll();
        page.add(Ui.text("Scan this with your authenticator app, then enter the code it shows.",
                "SvText"));
        Container qr = new Container(new BorderLayout());
        qr.add(BorderLayout.CENTER, new Label(QrCodeImage.create(QrCode.encode(
                started.otpauthUri(), QrCode.ECC_MEDIUM), 4), "TwoFactorQr"));
        page.add(qr);
        page.add(Ui.fact("Or type this key", started.secret()));
        final TextField code = Ui.field(page, "Code", "code", "123456", TextArea.NUMERIC);
        final SpanLabel error = Ui.text("", "SvError");
        page.add(error);
        final Button confirm = Ui.primary("Turn on", "confirmTwoFactor", null);
        confirm.addActionListener(e -> Api.twoFactor().confirm(new TwoFactorCodeDto(
                SecondFactor.clean(code.getText()), ""), Net.to(on -> codes(form, page, on),
                        (status, message) -> {
                            error.setText(Lang.tr(message));
                            Ui.refresh(form);
                        })));
        page.add(confirm);
        Ui.refresh(form);
    }

    private static void codes(final Form form, final Container page, TwoFactorDto on) {
        page.removeAll();
        page.add(Ui.callout(com.codename1.ui.FontImage.MATERIAL_KEY, "Your recovery codes",
                "Keep these somewhere safe. Each signs in once without the app, and they are "
                + "not shown again."));
        Container list = new Container(BoxLayout.y());
        List<String> codes = on.recoveryCodes();
        for (int iter = 0; codes != null && iter < codes.size(); iter++) {
            list.add(Ui.label(codes.get(iter), "TwoFactorRecoveryCode"));
        }
        page.add(list);
        page.add(Ui.primary("Done", "doneTwoFactor", e -> Api.twoFactor().status(
                Net.to(status -> render(form, page, status), Ui::fail))));
        Ui.refresh(form);
    }
}
