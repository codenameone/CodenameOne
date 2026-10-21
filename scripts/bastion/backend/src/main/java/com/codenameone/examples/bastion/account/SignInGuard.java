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
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.security.AntPathRequestMatcher;
import com.codename1.backend.security.AuthenticationFailureHandler;
import com.codename1.backend.security.AuthenticationSuccessHandler;
import com.codename1.backend.security.RequestMatcher;
import com.codename1.backend.security.SecurityFilter;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.LoginFailure;
import com.codenameone.examples.bastion.repository.LoginFailureRepository;

import java.io.IOException;

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
@Component
public class SignInGuard {
    private static final RequestMatcher PASSWORD_SIGN_IN =
            AntPathRequestMatcher.antMatcher("POST", "/login");

    private final LoginFailureRepository failures;
    private final JdbcUserDetailsManager users;
    private final Audit audit;
    private final int attempts;
    private final long windowMillis;
    private final long lockMillis;

    public SignInGuard(LoginFailureRepository failures, JdbcUserDetailsManager users,
            Audit audit,
            @Value("${bastion.lockout.attempts:5}") int attempts,
            @Value("${bastion.lockout.window-seconds:900}") int windowSeconds,
            @Value("${bastion.lockout.seconds:900}") int lockSeconds) {
        this.failures = failures;
        this.users = users;
        this.audit = audit;
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
            }
            return chain.doFilter(request);
        };
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
                    // Two first failures at once both insert the row, and one
                    // insert loses on the key. That costs the count one guess
                    // out of five; failing the answer would cost more.
                    System.err.println("bastion: lockout count lost a race: " + race);
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

    @Transactional(readOnly = true)
    public boolean locked(String username) throws IOException {
        LoginFailure row = failures.findById(username);
        return row != null && row.getLockedUntil() > System.currentTimeMillis();
    }

    /// One more wrong password for `username`; locks the account on the last
    /// one the window allows.
    @Transactional(rollbackFor = Exception.class)
    public void failed(String username) throws IOException {
        long now = System.currentTimeMillis();
        LoginFailure row = failures.findById(username);
        boolean fresh = row == null;
        if (fresh) {
            row = new LoginFailure();
            row.setUsername(username);
        }
        if (fresh || now - row.getFirstAt() > windowMillis) {
            row.setFailures(0);
            row.setFirstAt(now);
        }
        row.setFailures(row.getFailures() + 1);
        if (row.getFailures() >= attempts) {
            row.setLockedUntil(now + lockMillis);
            row.setFailures(0);
            row.setFirstAt(now);
            audit.append(username, "ACCOUNT_LOCKED", username,
                    attempts + " wrong passwords; " + lockMillis / 1000 + "s");
        }
        if (fresh) {
            failures.insert(row);
        } else {
            failures.save(row);
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
