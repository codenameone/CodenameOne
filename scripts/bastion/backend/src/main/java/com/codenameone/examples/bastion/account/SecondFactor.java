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
package com.codenameone.examples.bastion.account;

import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.security.mfa.RecoveryCodeService;
import com.codename1.backend.security.mfa.TotpEnrollment;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.bastion.Caller;
import com.codenameone.examples.bastion.api.RecoveryCodesDto;
import com.codenameone.examples.bastion.api.TotpEnrollmentDto;
import com.codenameone.examples.bastion.audit.Audit;

import java.io.IOException;

/// Adding an authenticator app to an account, and the recovery codes that
/// stand in for it.
///
/// Once an account has a confirmed app, `http.mfa(...)` in
/// [com.codenameone.examples.bastion.SecurityConfig] holds every password
/// sign-in back until a code from it is posted to `/login/mfa`. The codes are
/// checked, used up and counted by [TotpService]; nothing here sees one at
/// sign-in.
@Component
public class SecondFactor {
    private final TotpService totp;
    private final RecoveryCodeService recoveryCodes;
    private final Audit audit;

    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;
    private final SignInGuard guard;
    private final long freshSeconds;

    public SecondFactor(TotpService totp, RecoveryCodeService recoveryCodes, Audit audit,
            JdbcUserDetailsManager users, PasswordEncoder encoder, SignInGuard guard,
            @Value("${bastion.device.fresh-minutes:10}") String freshMinutes) {
        this.users = users;
        this.encoder = encoder;
        this.guard = guard;
        this.totp = totp;
        this.recoveryCodes = recoveryCodes;
        this.audit = audit;
        this.freshSeconds = Long.parseLong(freshMinutes.trim()) * 60L;
    }

    /// Enrolling an app is a step-up: once it is on, every sign-in needs its
    /// codes, so a stolen token must not be able to put the attacker's app on
    /// the account and lock the customer out. A sign-in made with a second
    /// factor or a passkey (`amr` holding `mfa`, `otp` or `hwk`) is enough by
    /// itself. A password-only one must be fresh (as for a device) AND the
    /// request must carry the password: freshness alone would let a token
    /// stolen in its first minutes enrol, which is exactly the window a
    /// phishing proxy works in. A wrong password counts toward the lockout.
    private void requireStepUp(String username, String password) throws IOException {
        java.util.List<String> how = Caller.authMethods();
        if (how.contains("mfa") || how.contains("otp") || how.contains("hwk")) {
            return;
        }
        long age = System.currentTimeMillis() / 1000L - Caller.authTime();
        if (Caller.authTime() < 0 || age > freshSeconds || age < -60L) {
            audit.append(username, "TOTP_REFUSED", username, "sign-in not fresh");
            throw Refused.with(403, "sign-in-first", "Sign in first");
        }
        if (password == null
                || !encoder.matches(password, users.loadUserByUsername(username).getPassword())) {
            guard.failed(username);
            audit.append(username, "TOTP_REFUSED", username, "password");
            throw Refused.with(403, "enter-password-add-app", "Enter your password to add an authenticator app");
        }
    }

    /// Makes a secret for the caller's app. Refused when they already have
    /// one: starting over replaces the secret and turns the second factor off
    /// until it is confirmed again, which would let somebody holding only an
    /// access token strip the account of its second factor.
    public TotpEnrollmentDto start(String username, String password) throws IOException {
        if (totp.isEnabled(username)) {
            throw Refused.with(409, "account-already-has-authenticator-app", "This account already has an authenticator app");
        }
        requireStepUp(username, password);
        TotpEnrollment enrollment = totp.beginEnrollment(username);
        audit.append(username, "TOTP_STARTED", username, "");
        return new TotpEnrollmentDto(enrollment.getSecret(), enrollment.getOtpauthUri());
    }

    /// Confirms the app with a code it shows, and makes the recovery codes.
    public RecoveryCodesDto confirm(String username, String code, String password)
            throws IOException {
        requireStepUp(username, password);
        if (code == null || !totp.confirmEnrollment(username, code)) {
            audit.append(username, "TOTP_CONFIRM_FAILED", username, "");
            throw Refused.with(400, "code-not-app-shows", "That code is not the one your app shows");
        }
        audit.append(username, "TOTP_ENROLLED", username, "");
        return new RecoveryCodesDto(recoveryCodes.generate(username));
    }

    /// New recovery codes in place of the old, for a caller who proves they
    /// still have the app.
    public RecoveryCodesDto renew(String username, String code) throws IOException {
        if (!totp.isEnabled(username)) {
            throw Refused.with(409, "add-authenticator-app", "Add an authenticator app first");
        }
        if (code == null || !totp.verify(username, code)) {
            audit.append(username, "RECOVERY_CODES_REFUSED", username, "");
            throw Refused.with(403, "code-not-app-shows", "That code is not the one your app shows");
        }
        audit.append(username, "RECOVERY_CODES_RENEWED", username, "");
        return new RecoveryCodesDto(recoveryCodes.generate(username));
    }
}
