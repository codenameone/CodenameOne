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

import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.security.AntPathRequestMatcher;
import com.codename1.backend.security.AuthenticationFailureHandler;
import com.codename1.backend.security.AuthenticationSuccessHandler;
import com.codename1.backend.security.RequestMatcher;
import com.codename1.backend.security.SecondFactorAuthenticationFilter;
import com.codename1.backend.security.SecurityFilter;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.mfa.RecoveryCodeService;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntity;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntityRepository;
import com.codename1.backend.security.webauthn.UserCredentialRepository;
import com.codenameone.examples.bastion.Refused;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.LoginFailure;
import com.codenameone.examples.bastion.repository.LoginFailureRepository;

import java.io.IOException;
import java.util.Map;

/// What happens around a sign-in: the audit record of each one, and the
/// lockout after repeated wrong passwords.
///
/// ## The lockout
///
/// The rate limit on `/login` counts by client address, which an attacker
/// with many addresses walks around. This counts by account: after
/// `bastion.lockout.attempts` wrong passwords (5) within
/// `bastion.lockout.window-seconds` (15 minutes), the account refuses
/// password sign-ins for `bastion.lockout.seconds` (15 minutes), the right
/// password included. The security layer has no lockout of its own -- it
/// honours `account_non_locked`, but nothing sets it -- and a permanent lock
/// there would need an administrator to lift every time, so the count and the
/// lock live in a table of this application's.
///
/// The trade every lockout makes: anyone who knows a customer's e-mail can
/// keep them from signing in with a password, five guesses every fifteen
/// minutes. That is why the lock is temporary and why it covers only
/// passwords -- a passkey still signs the customer in, and an attacker
/// cannot lock that out. It also says, to whoever is guessing, that the
/// account exists; registration says as much already.
///
/// ## The strongest factor an account has is the one it signs in with
///
/// An account is only as safe as the weakest way into it, so enrolling a
/// stronger factor retires the weaker ones. With a passkey registered the
/// password stops being a way in: `POST /login` is refused (403,
/// `"stronger":"passkey"`) before the password is looked at, and the code
/// step that follows a password takes a recovery code and nothing else -- a
/// six-digit code from the authenticator app is refused the same way. That
/// leaves one documented way back for a customer whose passkey is gone: their
/// password and one of their recovery codes, each of which works once, and
/// the sign-in is in the audit log as `password+totp` with the passkey still
/// on the account. An account with a passkey and no authenticator app has no
/// recovery codes, and its way back is the bank, which removes the passkey
/// after checking who is asking. The refusal says the account has a passkey
/// to anyone who knows its e-mail, the same trade the lockout makes.
@Component
public class SignInGuard {
    private static final RequestMatcher PASSWORD_SIGN_IN =
            AntPathRequestMatcher.antMatcher("POST", "/login");
    private static final RequestMatcher SECOND_FACTOR =
            AntPathRequestMatcher.antMatcher("POST", "/login/mfa");

    private final LoginFailureRepository failures;
    private final JdbcUserDetailsManager users;
    private final Audit audit;
    private final UserCredentialRepository passkeys;
    private final PublicKeyCredentialUserEntityRepository passkeyHolders;
    private final TotpService totp;
    private final int attempts;
    private final long windowMillis;
    private final long lockMillis;

    public SignInGuard(LoginFailureRepository failures, JdbcUserDetailsManager users,
            Audit audit, UserCredentialRepository passkeys,
            PublicKeyCredentialUserEntityRepository passkeyHolders, TotpService totp,
            @Value("${bastion.lockout.attempts:5}") int attempts,
            @Value("${bastion.lockout.window-seconds:900}") int windowSeconds,
            @Value("${bastion.lockout.seconds:900}") int lockSeconds) {
        this.failures = failures;
        this.users = users;
        this.audit = audit;
        this.passkeys = passkeys;
        this.passkeyHolders = passkeyHolders;
        this.totp = totp;
        this.attempts = attempts;
        this.windowMillis = windowSeconds * 1000L;
        this.lockMillis = lockSeconds * 1000L;
    }

    /// The name a sign-in was attempted under, the way it is stored. Variants
    /// of case and spacing are one account, so they are one count.
    static String attempted(HttpServer.Request request) {
        String name;
        try {
            name = request.param("username");
        } catch (RuntimeException malformed) {
            name = null;
        }
        return name == null ? "" : Accounts.asciiLower(name.trim());
    }

    /// Runs before the password is looked at, and refuses a locked account
    /// without looking: a lock that still checked the password would tell the
    /// guesser when they were right.
    public SecurityFilter lockout() {
        return (request, chain) -> {
            if (PASSWORD_SIGN_IN.matches(request)) {
                String username = attempted(request);
                if (username.length() > 0 && locked(username)) {
                    audit.append(username, "SIGNIN_LOCKED", username, "password");
                    return new HttpServer.Response(423, "application/json",
                            "{\"signedIn\":false,\"locked\":true}".getBytes("UTF-8"))
                            .header("Cache-Control", "no-store");
                }
                // A passkey account takes a password only on the way to a
                // recovery code, which needs an authenticator app behind it.
                if (username.length() > 0 && hasPasskey(username) && !totp.isEnabled(username)) {
                    audit.append(username, "SIGNIN_REFUSED", username,
                            "password: the account has a passkey");
                    return json(403, "{\"signedIn\":false,\"stronger\":\"passkey\"}");
                }
            }
            return chain.doFilter(request);
        };
    }

    /// Runs before the second-factor filter: a password accepted for an account
    /// with a passkey is finished only by a recovery code (see the class
    /// comment). The pending sign-in is left as it is, so the recovery code
    /// can still follow it.
    public SecurityFilter passkeyFirst() {
        return (request, chain) -> {
            if (SECOND_FACTOR.matches(request)) {
                HttpSession session = request.getSession(false);
                Object pending = session == null ? null
                        : session.getAttribute(SecondFactorAuthenticationFilter.PENDING);
                Object name = pending instanceof Map ? ((Map) pending).get("name") : null;
                String code;
                try {
                    code = request.param("code");
                } catch (RuntimeException malformed) {
                    code = null;
                }
                if (name instanceof String && hasPasskey((String) name)
                        && !RecoveryCodeService.isCodeShaped(code)) {
                    String username = Accounts.asciiLower((String) name);
                    audit.append(username, "SIGNIN_REFUSED", username,
                            "totp: the account has a passkey");
                    return json(403, "{\"signedIn\":false,\"stronger\":\"passkey\","
                            + "\"recovery\":true}");
                }
            }
            return chain.doFilter(request);
        };
    }

    /// Whether `username` has registered a passkey.
    public boolean hasPasskey(String username) {
        PublicKeyCredentialUserEntity holder = passkeyHolders.findByUsername(username);
        return holder != null && !passkeys.findByUserId(holder.getId()).isEmpty();
    }

    /// Answers a completed sign-in -- a password with no second factor, a
    /// password and a code, or a passkey -- and records it. `how` goes into
    /// the record.
    public AuthenticationSuccessHandler signedIn(final String how) {
        return (request, authentication) -> {
            String username = authentication.getName();
            cleared(username);
            audit.append(username, "SIGNIN", username, how);
            return json(200, "{\"signedIn\":true,\"authenticated\":true}");
        };
    }

    /// Answers a refused sign-in, and counts it against the account.
    public AuthenticationFailureHandler refused(final String how) {
        return (request, failure) -> {
            String username = "password".equals(how) ? attempted(request) : "";
            boolean known = username.length() > 0 && users.userExists(username);
            if (known) {
                try {
                    failed(username);
                } catch (IOException | RuntimeException race) {
                    // failed() already retries a lost first insert; anything
                    // still thrown is the database, and failing the answer
                    // would cost more than the one uncounted guess.
                    System.err.println("bastion: lockout count failed: " + race);
                }
            }
            // A made-up name is recorded as one: the actor column holds only
            // names of real accounts, so a search by actor is a customer's
            // history and nothing else.
            audit.append(known ? username : "unknown", "SIGNIN_FAILED",
                    username.length() > 0 ? username : "-",
                    how + ": " + failure.getClass().getSimpleName());
            return json(401, "{\"signedIn\":false,\"authenticated\":false}");
        };
    }

    /// The password asked for again inside a signed-in session (adding an
    /// app, a device, revoking one), held to the same lockout as the sign-in
    /// form: a locked account is refused before the password is looked at,
    /// a wrong one counts, a right one wipes the count. A missing password is
    /// a client that has not asked yet, not a guess, so it is not counted.
    /// `refusal` is what (`code`, English) a wrong or missing one answers.
    public void recheck(String username, String password, PasswordEncoder encoder,
            String event, String code, String english) throws IOException {
        if (locked(username)) {
            audit.append(username, event, username, "account locked");
            throw Refused.with(423, "account-locked",
                    "Too many wrong passwords: this account is locked for a while");
        }
        if (password == null || password.length() == 0) {
            audit.append(username, event, username, "no password");
            throw Refused.with(403, code, english);
        }
        if (!encoder.matches(password, users.loadUserByUsername(username).getPassword())) {
            failed(username);
            audit.append(username, event, username, "password");
            throw Refused.with(403, code, english);
        }
        cleared(username);
    }

    @Transactional(readOnly = true)
    public boolean locked(String username) throws IOException {
        LoginFailure row = failures.findById(username);
        return row != null && row.getLockedUntil() > System.currentTimeMillis();
    }

    /// One more wrong password for `username`; locks the account on the last
    /// one the window allows.
    ///
    /// Counted by the database, not read, incremented and written back: two
    /// wrong passwords at once each read the same count, and one of them was
    /// lost -- a guesser sending in parallel got more guesses than the window
    /// allows. The row is made first (a duplicate-key loss to a concurrent
    /// first failure is fine: the row exists either way), then the window
    /// restart, the increment and the lock are each a single UPDATE on it.
    public void failed(String username) throws IOException {
        if (!exists(username)) {
            try {
                create(username);
            } catch (IOException | RuntimeException race) {
                if (!exists(username)) {
                    throw race;
                }
            }
        }
        countOne(username);
    }

    @Transactional(readOnly = true)
    private boolean exists(String username) throws IOException {
        return failures.existsById(username);
    }

    @Transactional(rollbackFor = Exception.class)
    private void create(String username) throws IOException {
        LoginFailure row = new LoginFailure();
        row.setUsername(username);
        row.setFirstAt(System.currentTimeMillis());
        failures.insert(row);
    }

    @Transactional(rollbackFor = Exception.class)
    private void countOne(String username) throws IOException {
        long now = System.currentTimeMillis();
        failures.restartWindow(username, now, now - windowMillis);
        failures.increment(username);
        if (failures.lockIfReached(username, attempts, now, now + lockMillis) == 1) {
            audit.append(username, "ACCOUNT_LOCKED", username,
                    attempts + " wrong passwords; " + lockMillis / 1000 + "s");
        }
    }

    /// A completed sign-in wipes what was counted.
    @Transactional(rollbackFor = Exception.class)
    public void cleared(String username) throws IOException {
        if (failures.existsById(username)) {
            failures.deleteById(username);
        }
    }

    private static HttpServer.Response json(int status, String body) throws IOException {
        return new HttpServer.Response(status, "application/json", body.getBytes("UTF-8"))
                .header("Cache-Control", "no-store");
    }
}
