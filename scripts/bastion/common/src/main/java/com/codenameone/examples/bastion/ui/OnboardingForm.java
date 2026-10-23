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
package com.codenameone.examples.bastion.ui;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.bastion.Prefs;
import com.codenameone.examples.bastion.api.RecoveryCodesDto;
import com.codenameone.examples.bastion.api.RegisterDto;
import com.codenameone.examples.bastion.api.TotpCodeDto;
import com.codenameone.examples.bastion.api.TotpEnrollmentDto;
import com.codenameone.examples.bastion.api.TotpStartDto;
import com.codenameone.examples.bastion.api.UserDto;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;
import com.codenameone.examples.bastion.net.Session;
import com.codenameone.examples.bastion.security.Keys;
import com.codenameone.examples.bastion.security.Lock;
import com.codenameone.examples.bastion.security.Protect;
import java.util.List;

/// A new customer, from nothing to a phone that signs their payments.
///
/// The steps, each a screen, each but the first skippable and offered again
/// from Settings:
///
/// 1. **Register**: name, e-mail and a password the rules below accept --
///    the same rule as the server's (`Passwords.MINIMUM`), checked as it is
///    typed so the server's refusal is never the first the user hears of it.
/// 2. **Sign in**, at once, keeping the server session for step 4.
/// 3. **An authenticator app**: the TOTP secret as a QR code and as text,
///    confirmed by a first code, then the recovery codes -- shown once, to
///    write down (never copied: see `Protect`).
/// 4. **A passkey**, through the platform's passkey sheet.
/// 5. **Biometric unlock**, if the device has one: a choice, not a default.
/// 6. **This device's signing key**, registered while the sign-in is fresh
///    enough for the server to accept it.
///
/// Every screen with a secret on it blocks screenshots.
public final class OnboardingForm {
    /// The server's rule (`Passwords` in the backend).
    static final int MIN_PASSWORD = 12;
    static final int MAX_PASSWORD = 200;

    private OnboardingForm() {
    }

    /// The registration screen; `signedIn` runs at the end of onboarding.
    public static void show(final Runnable back, final Runnable signedIn) {
        Form form = Ui.form("Open an account", back);
        form.add(Ui.title("Bank with Bastion"));
        form.add(Ui.muted(Lang.tr("Takes two minutes. You will set up a second factor and this "
                + "phone's signing key as you go.")));
        final TextField name = Ui.field("Your name", TextArea.ANY);
        final TextField email = Ui.field("E-mail", TextArea.EMAILADDR);
        final TextField password = Ui.field("Password", TextArea.PASSWORD);
        final TextField repeat = Ui.field("Password again", TextArea.PASSWORD);
        final Label ruleLength = new Label("", "BnRule");
        final Label ruleMatch = new Label("", "BnRule");
        final Label ruleEmail = new Label("", "BnRule");
        final Label error = Ui.error();
        final Button go = Ui.primary("Create account");
        final Runnable check = () -> {
            String[] problems = passwordProblems(password.getText(), repeat.getText(), email.getText());
            rule(ruleLength, Lang.tr("At least {0} characters", String.valueOf(MIN_PASSWORD)),
                    problems[0] == null);
            rule(ruleMatch, Lang.tr("Both passwords match"), problems[1] == null);
            rule(ruleEmail, Lang.tr("Not your e-mail address"), problems[2] == null);
            go.setEnabled(problems[0] == null && problems[1] == null && problems[2] == null
                    && name.getText().trim().length() > 0 && email.getText().indexOf('@') > 0);
        };
        password.addDataChangedListener((type, index) -> check.run());
        repeat.addDataChangedListener((type, index) -> check.run());
        email.addDataChangedListener((type, index) -> check.run());
        name.addDataChangedListener((type, index) -> check.run());
        check.run();
        form.addAll(name, email, password, repeat, ruleLength, ruleMatch, ruleEmail, error, go);
        SpanLabel tip = new SpanLabel(Lang.tr("A few unrelated words make a password that is long "
                + "and easy to remember."));
        tip.setTextUIID("BnMuted");
        form.add(tip);
        go.addActionListener(evt -> {
            go.setEnabled(false);
            Ui.showError(error, null);
            final String address = Session.accountName(email.getText());
            final String secret = password.getText();
            Api.account().register(new RegisterDto(address, secret, name.getText().trim()),
                    Net.to((UserDto user) -> {
                        // Kept for one passkey ceremony in step 4.
                        Session.keepSessionForPasskey();
                        Session.signIn(address, secret, pending -> {
                            // A brand-new account has no second factor yet.
                            Session.endPasskeySession();
                            Ui.showError(error, Lang.tr("Sign in to continue."));
                        }, () -> {
                            password.setText("");
                            repeat.setText("");
                            totpStep(signedIn);
                        }, (status, message) -> {
                            go.setEnabled(true);
                            Ui.showError(error, Lang.tr(message));
                        });
                    }, (status, message) -> {
                        go.setEnabled(true);
                        Ui.showError(error, Lang.tr(message));
                    }));
        });
        Ui.show(form, true);
    }

    /// What is wrong with a password, one slot per rule (null when the rule
    /// holds): length, the repeat, and being the e-mail address itself.
    static String[] passwordProblems(String password, String repeat, String email) {
        String[] out = new String[3];
        int length = password == null ? 0 : password.length();
        if (length < MIN_PASSWORD || length > MAX_PASSWORD) {
            out[0] = "length";
        }
        if (length == 0 || !password.equals(repeat)) {
            out[1] = "repeat";
        }
        String account = Session.accountName(email);
        if (length > 0 && account != null && account.length() > 0) {
            String folded = Session.accountName(password);
            int at = account.indexOf('@');
            String local = at > 0 ? account.substring(0, at) : account;
            if (folded.equals(account) || (local.length() >= 4 && folded.indexOf(local) >= 0)) {
                out[2] = "email";
            }
        }
        return out;
    }

    private static void rule(Label label, String text, boolean met) {
        label.setText((met ? "+ " : "- ") + text);
        label.setUIID(met ? "BnRuleMet" : "BnRule");
    }

    // ------------------------------------------------------- authenticator

    private static void totpStep(final Runnable signedIn) {
        totp(() -> passkeyStep(signedIn), () -> passkeyStep(signedIn));
    }

    /// Turns the authenticator app on from Settings.
    public static void totp(final Runnable done) {
        totp(done, done);
    }

    private static void totp(final Runnable done, final Runnable skipped) {
        // Adding an app is a step-up: after a sign-in made with the password
        // alone the server wants the password again, so a stolen token cannot
        // put somebody else's app on the account (SecondFactor on the server).
        // Asked once here and sent with both calls, then dropped.
        String password = null;
        // Matches SecondFactor on the server: a passkey counts only when the
        // authenticator verified the user (`hwk` with `user`).
        if (!Session.signedInWith("mfa") && !Session.signedInWith("otp")
                && !(Session.signedInWith("hwk") && Session.signedInWith("user"))) {
            TextField field = new TextField("", Lang.tr("Password"), 20, TextArea.PASSWORD);
            field.setUIID("BnField");
            Command ok = new Command(Lang.tr("Continue"));
            if (Dialog.show(Lang.tr("Confirm it is you"),
                    // Enclosed, or the dialog stretches the one-line field to its
                    // whole body.
                    com.codename1.ui.layouts.BoxLayout.encloseY(field), ok,
                    new Command(Lang.tr("Cancel"))) != ok) {
                skipped.run();
                return;
            }
            password = field.getText();
        }
        final String confirmedPassword = password;
        final Form form = Ui.form("Authenticator app", null);
        form.add(Ui.title("Add a second factor"));
        form.add(Ui.muted(Lang.tr("Scan this with an authenticator app. From now on a sign-in with "
                + "your password also asks for the code it shows.")));
        final Container content = new Container(BoxLayout.y());
        form.add(content);
        Button later = Ui.secondary("Not now");
        later.addActionListener(evt -> skipped.run());
        form.add(later);
        Ui.show(form, true);
        Ui.loading(content);
        Api.account().startTotp(new TotpStartDto(confirmedPassword),
                Net.to((TotpEnrollmentDto enrollment) ->
                showSecret(content, enrollment, confirmedPassword, done),
                (status, message) -> Ui.failed(content, message, () -> totp(done, skipped))));
    }

    private static void showSecret(final Container content, final TotpEnrollmentDto enrollment,
            final String password, final Runnable done) {
        content.removeAll();
        byte[] uri = utf8(enrollment.otpauthUri());
        // A URI too long for the encoder (a very long e-mail address) still
        // has the key below; the code is a convenience, the key the record.
        if (uri.length <= QrCode.MAX_BYTES) {
            Container qr = new Container(new FlowLayout(Container.CENTER));
            qr.add(new QrView(QrCode.encode(uri)));
            content.add(qr);
        }
        content.add(Ui.section("Or enter this key"));
        content.add(new Label(group(enrollment.secret()), "BnSecret"));
        Button copy = Ui.secondary("Copy key");
        copy.addActionListener(evt -> {
            Protect.copy(enrollment.secret());
            Ui.toast("Copied. The clipboard clears itself in a minute.");
        });
        Button open = Ui.secondary("Open authenticator");
        open.addActionListener(evt -> CN.execute(enrollment.otpauthUri()));
        content.add(Ui.across(copy, open));
        final TextField code = Ui.field("Six-digit code", TextArea.NUMERIC);
        final Label error = Ui.error();
        final Button confirm = Ui.primary("Turn on");
        content.addAll(code, error, confirm);
        confirm.addActionListener(evt -> {
            confirm.setEnabled(false);
            Api.account().confirmTotp(new TotpCodeDto(code.getText().trim(), password),
                    Net.to((RecoveryCodesDto codes) -> Session.refreshUser(
                            () -> recoveryCodes(codes.codes(), done),
                            (status, message) -> recoveryCodes(codes.codes(), done)),
                            (status, message) -> {
                                confirm.setEnabled(true);
                                code.setText("");
                                Ui.showError(error, Lang.tr(message));
                            }));
        });
        content.revalidate();
    }

    /// Makes a fresh set of recovery codes, the old ones void; asks for a
    /// current code first, as the server does.
    public static void newRecoveryCodes(final Runnable done) {
        TextField code = Ui.field("Six-digit code", TextArea.NUMERIC);
        Command ok = new Command(Lang.tr("Continue"));
        if (Dialog.show(Lang.tr("New recovery codes"), code, ok, new Command(Lang.tr("Cancel"))) != ok) {
            return;
        }
        Api.account().newRecoveryCodes(new TotpCodeDto(code.getText().trim(), null),
                Net.to((RecoveryCodesDto codes) -> Session.refreshUser(
                        () -> recoveryCodes(codes.codes(), done),
                        (status, message) -> recoveryCodes(codes.codes(), done)),
                        (status, message) -> Dialog.show(Lang.tr("Not changed"), Lang.tr(message),
                                Lang.tr("OK"), null)));
    }

    /// The recovery codes, shown this once: the server keeps only their
    /// hashes, so this is the only time anyone can read them.
    private static void recoveryCodes(final List<String> codes, final Runnable done) {
        Form form = Ui.form("Recovery codes", null);
        form.add(Ui.title("Save your recovery codes"));
        form.add(Ui.muted(Lang.tr("Each signs you in once if you lose your phone. Keep them "
                + "somewhere safe and offline. They will not be shown again.")));
        Container grid = new Container(BoxLayout.y(), "BnPanel");
        for (String one : codes) {
            grid.add(new Label(one, "BnSecret"));
        }
        form.add(grid);
        // Copied as a sensitive clip that clears itself (Protect#copy): long
        // enough to paste into a password manager, kept out of the keyboard's
        // history and the copy preview where the platform allows it.
        Button copy = Ui.secondary("Copy codes");
        copy.addActionListener(evt -> {
            StringBuilder all = new StringBuilder();
            for (String one : codes) {
                all.append(one).append('\n');
            }
            Protect.copy(all.toString());
            Ui.toast("Copied. The clipboard clears itself in a minute.");
        });
        form.add(copy);
        Button saved = Ui.primary("I have saved them");
        saved.addActionListener(evt -> done.run());
        form.add(saved);
        Ui.show(form, true);
    }

    // -------------------------------------------------------------- passkey

    private static void passkeyStep(final Runnable signedIn) {
        if (!Session.canAddPasskey()) {
            Session.endPasskeySession();
            biometricStep(signedIn);
            return;
        }
        Form form = Ui.form("Passkey", null);
        form.add(Ui.title("Sign in with a passkey"));
        form.add(Ui.muted(Lang.tr("A passkey is kept by your phone and unlocked by your face, "
                + "fingerprint or screen lock. It cannot be phished, and replaces both the "
                + "password and the code.")));
        final Label error = Ui.error();
        final Button create = Ui.primary("Create a passkey");
        create.addActionListener(evt -> {
            create.setEnabled(false);
            Session.addPasskey(Lang.tr("Bastion on {0}", CN.getPlatformName()),
                    () -> biometricStep(signedIn), (status, message) -> {
                        // The kept session is gone either way; Settings can't
                        // retry without a new sign-in, so move on.
                        Ui.showError(error, Lang.tr(message));
                        create.setEnabled(false);
                    });
        });
        Button next = Ui.secondary("Continue");
        next.addActionListener(evt -> {
            Session.endPasskeySession();
            biometricStep(signedIn);
        });
        form.addAll(error, create, next);
        Ui.show(form, false);
    }

    // ------------------------------------------------------------ biometric

    private static void biometricStep(final Runnable signedIn) {
        if (!Lock.biometricsAvailable()) {
            pinStep(signedIn);
            return;
        }
        Form form = Ui.form("Unlock", null);
        form.add(Ui.title("Unlock with your face or fingerprint?"));
        form.add(Ui.muted(Lang.tr("Bastion locks itself when you leave it. With this on, the "
                + "phone keeps the key to your session and payments where only your biometric "
                + "releases it. The device passcode is never accepted.")));
        final Label error = Ui.error();
        Button yes = Ui.primary("Use biometrics");
        yes.addActionListener(evt -> UnlockSetup.biometric(() -> pinStep(signedIn), error));
        Button no = Ui.secondary("Not now");
        no.addActionListener(evt -> {
            Keys.disableBiometric();
            pinStep(signedIn);
        });
        form.addAll(error, yes, no);
        Ui.show(form, false);
    }

    // ------------------------------------------------------------------ PIN

    private static void pinStep(final Runnable signedIn) {
        if (Keys.pinEnabled()) {
            deviceStep(signedIn);
            return;
        }
        Form form = Ui.form("Unlock PIN", null);
        form.add(Ui.title("Choose an unlock PIN"));
        form.add(Ui.muted(Lang.tr("For when a biometric is not available or you would rather "
                + "not use one: {0} to {1} digits, not your card PIN. {2} wrong tries in a row "
                + "erase the keys on this device.", String.valueOf(Keys.MIN_PIN),
                String.valueOf(Keys.MAX_PIN), String.valueOf(Keys.PIN_ATTEMPTS))));
        final Label error = Ui.error();
        final UnlockSetup.PinFields fields = new UnlockSetup.PinFields();
        Button set = Ui.primary("Set PIN");
        set.addActionListener(evt -> fields.save(() -> deviceStep(signedIn), error));
        Button no = Ui.secondary("Not now");
        no.addActionListener(evt -> deviceStep(signedIn));
        form.addAll(error, fields.pin, fields.again, set, no);
        Ui.show(form, true);
    }

    // --------------------------------------------------------------- device

    private static void deviceStep(final Runnable signedIn) {
        final Runnable finish = () -> {
            UserDto user = Session.user();
            Prefs.setOnboarded(user == null ? null : user.username());
            signedIn.run();
        };
        if (DeviceSetup.registered()) {
            finish.run();
            return;
        }
        Form form = Ui.form("This device", null);
        form.add(Ui.title("Let this phone sign your payments"));
        form.add(Ui.muted(Lang.tr("Bastion makes a key on this phone and gives the bank only its "
                + "public half. Payments to other people and crypto withdrawals must carry its "
                + "signature, so a stolen password alone cannot move your money.")));
        final Label error = Ui.error();
        final Button register = Ui.primary("Register this device");
        register.addActionListener(evt -> {
            register.setEnabled(false);
            DeviceSetup.register(finish, (status, message) -> {
                register.setEnabled(true);
                Ui.showError(error, Lang.tr(message));
            });
        });
        Button later = Ui.secondary("Later");
        later.addActionListener(evt -> finish.run());
        form.addAll(error, register, later);
        Ui.show(form, false);
    }

    /// A base32 secret in groups of four, as authenticator apps show it.
    static String group(String secret) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < secret.length(); iter++) {
            if (iter > 0 && iter % 4 == 0) {
                out.append(' ');
            }
            out.append(secret.charAt(iter));
        }
        return out.toString();
    }

    private static byte[] utf8(String text) {
        try {
            return text.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException impossible) {
            return text.getBytes();
        }
    }
}
