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
package com.codename1.backend.security.mfa;

import com.codename1.backend.Crypto;
import com.codename1.backend.security.Clock;
import com.codename1.security.Base32;
import com.codename1.security.Hash;
import com.codename1.security.Otp;
import java.io.IOException;

/// Time-based one-time codes (RFC 6238): enrolling a user's authenticator app,
/// and checking the codes it shows.
///
/// ```java
/// @Bean
/// TotpService totp(DataSource dataSource, Config config) {
///     return new TotpService(JdbcTotpRepository.fromConfig(dataSource, config), "Acme");
/// }
/// ```
///
/// Enrolment is two steps, so that nobody is locked out by an app that was
/// never set up: [#beginEnrollment] makes a secret and returns it for the user
/// to scan, and [#confirmEnrollment] takes a code from the app. Only then does
/// the user have a second factor, and only then does a chain with
/// `http.mfa(...)` ask for it.
///
/// Codes are six digits over SHA-1 that change every 30 seconds, which is what
/// authenticator apps expect, and one step either side of the current one is
/// accepted to allow for a clock that is off. A code is good once: the step of
/// each accepted code is recorded, and a code of that step or an earlier one
/// is refused afterwards -- including the same code sent twice at once.
public final class TotpService {
    private final TotpRepository repository;
    private final String issuer;
    private int digits = 6;
    private int periodSeconds = 30;
    private int tolerance = 1;
    private String algorithm = Hash.SHA1;
    private Clock clock = Clock.SYSTEM;

    /// @param issuer the name an authenticator app shows beside the account;
    /// it may not contain a colon
    public TotpService(TotpRepository repository, String issuer) {
        if (repository == null) {
            throw new IllegalArgumentException("repository cannot be null");
        }
        if (issuer == null || issuer.length() == 0 || issuer.indexOf(':') >= 0) {
            throw new IllegalArgumentException("An issuer is required, without a colon");
        }
        this.repository = repository;
        this.issuer = issuer;
    }

    /// The number of digits in a code: 6 unless set, or 8.
    public void setDigits(int digits) {
        if (digits != 6 && digits != 8) {
            throw new IllegalArgumentException("A code has 6 or 8 digits");
        }
        this.digits = digits;
    }

    /// How long a code lasts; 30 seconds unless set.
    public void setPeriodSeconds(int periodSeconds) {
        if (periodSeconds < 1) {
            throw new IllegalArgumentException("periodSeconds must be positive");
        }
        this.periodSeconds = periodSeconds;
    }

    /// How many steps either side of the current one are accepted; 1 unless
    /// set. Zero accepts the current code only.
    public void setTolerance(int tolerance) {
        if (tolerance < 0 || tolerance > 10) {
            throw new IllegalArgumentException("tolerance is 0 to 10 steps");
        }
        this.tolerance = tolerance;
    }

    /// The hash: [Hash#SHA1] unless set. Most authenticator apps support no
    /// other.
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    /// Makes a new secret for `username` and returns what their app needs.
    /// Whatever they had before is replaced, and is not a second factor again
    /// until [#confirmEnrollment].
    public TotpEnrollment beginEnrollment(String username) {
        byte[] secret;
        try {
            secret = Crypto.randomBytes(20);
        } catch (IOException err) {
            throw new IllegalStateException("No random source: " + err.getMessage(), err);
        }
        repository.save(username, secret);
        return new TotpEnrollment(Base32.encode(secret), Otp.otpauthUri(issuer, username, secret,
                digits, periodSeconds, algorithm));
    }

    /// Finishes an enrolment with a code from the user's app.
    ///
    /// @return whether `code` was right; false too when no enrolment is waiting
    public boolean confirmEnrollment(String username, String code) {
        TotpCredential credential = repository.find(username);
        if (credential == null || credential.isConfirmed()) {
            return false;
        }
        long step = matchingStep(credential, code);
        return step >= 0 && repository.confirm(username, credential.getSecret(), step);
    }

    /// Whether `username` has a confirmed second factor.
    public boolean isEnabled(String username) {
        TotpCredential credential = repository.find(username);
        return credential != null && credential.isConfirmed();
    }

    /// Checks a code at sign-in, and uses it up.
    ///
    /// @return whether `code` is a current code of `username` that has not
    /// been used
    public boolean verify(String username, String code) {
        TotpCredential credential = repository.find(username);
        if (credential == null || !credential.isConfirmed()) {
            return false;
        }
        long step = matchingStep(credential, code);
        // The step is recorded by a statement that changes a row only when no
        // code of this step was accepted before; that count is the decision.
        return step >= 0 && repository.advance(username, credential.getSecret(), step);
    }

    /// Removes the second factor of `username`.
    ///
    /// @return whether they had one
    public boolean disable(String username) {
        return repository.delete(username);
    }

    /// The code `username`'s app shows now. For tests and for tools; a server
    /// has no other use for it.
    public String currentCode(String username) {
        TotpCredential credential = repository.find(username);
        if (credential == null) {
            return null;
        }
        return Otp.totp(credential.getSecret(), clock.currentTimeMillis(), periodSeconds, digits,
                algorithm);
    }

    /// The time step whose code `code` is, among those accepted now; -1 when
    /// it is none of them.
    private long matchingStep(TotpCredential credential, String code) {
        String digitsOnly = normalize(code);
        if (digitsOnly == null) {
            return -1;
        }
        byte[] presented = ascii(digitsOnly);
        byte[] secret = credential.getSecret();
        long current = (clock.currentTimeMillis() / 1000L) / periodSeconds;
        long matched = -1;
        // Every candidate is computed and compared, whichever matches.
        for (long step = current - tolerance ; step <= current + tolerance ; step++) {
            if (step < 0) {
                continue;
            }
            String expected = Otp.totp(secret, step * periodSeconds * 1000L, periodSeconds, digits,
                    algorithm);
            if (Crypto.equalsConstantTime(presented, ascii(expected)) && matched < 0) {
                matched = step;
            }
        }
        return matched;
    }

    /// `code` without the spaces people type into it, or null when what is
    /// left is not the right number of digits.
    private String normalize(String code) {
        if (code == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(digits);
        for (int iter = 0 ; iter < code.length() ; iter++) {
            char c = code.charAt(iter);
            if (c == ' ') {
                continue;
            }
            if (c < '0' || c > '9' || out.length() == digits) {
                return null;
            }
            out.append(c);
        }
        return out.length() == digits ? out.toString() : null;
    }

    private static byte[] ascii(String value) {
        byte[] out = new byte[value.length()];
        for (int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte) value.charAt(iter);
        }
        return out;
    }
}
