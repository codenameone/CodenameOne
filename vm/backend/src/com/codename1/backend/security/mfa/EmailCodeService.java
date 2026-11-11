/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.SecuritySchema;
import com.codename1.backend.security.mail.EmailAddressResolver;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;

/// A code mailed to the user as the second step of signing in.
///
/// ```java
/// @Bean
/// EmailCodeService emailCodes(DataSource dataSource, MailSender mail) {
///     return new EmailCodeService(new JdbcEmailCodeRepository(dataSource), mail);
/// }
/// ```
///
/// With the bean declared, `http.mfa(...)` asks every user for whom
/// [#enable] was called for a code after their password, and mails it. A user
/// who has an authenticator app too is asked for either, and the mail is sent
/// when they ask for it.
///
/// A code is six digits, works once and for ten minutes, and is given up on
/// after five wrong tries: those five in a million are what protect it, so
/// the count is kept with the code and is the same count on every server.
/// Asking for another code does not start the count again: it is carried to
/// the new code until a code is entered right or runs out, and once the five
/// are used no code is mailed until then: the user waits out what is left of
/// the ten minutes, and asking again does not make the wait longer. Only a hash of the code is
/// kept. Another is not mailed within half a minute of the last.
///
/// A mailed code is as strong as the mailbox it goes to, and weaker than an
/// authenticator app or a passkey: offer it as the easy option, not the only
/// one.
///
/// The mail is plain by default. Name a template to send the application's
/// own: it is rendered with `code`, `username` and `minutes`.
public final class EmailCodeService {
    private final EmailCodeRepository repository;
    private final MailSender mail;
    private EmailAddressResolver addresses = EmailAddressResolver.USERNAME;
    private Clock clock = Clock.SYSTEM;
    private int validitySeconds = 600;
    private int resendSeconds = 30;
    private int maxAttempts = 5;
    private String subject = "Your sign-in code";
    private String template;

    public EmailCodeService(EmailCodeRepository repository, MailSender mail) {
        if (repository == null || mail == null) {
            throw new IllegalArgumentException("A mailed code needs a store and a mail sender; "
                    + "neither can be null");
        }
        this.repository = repository;
        this.mail = mail;
    }

    /// Where each user's mail goes. Without one, a user name that is an
    /// address is used and other users are sent nothing.
    public EmailCodeService addressResolver(EmailAddressResolver addressResolver) {
        this.addresses = addressResolver == null ? EmailAddressResolver.USERNAME
                : addressResolver;
        return this;
    }

    /// The clock expiry is measured by; a test sets its own.
    public EmailCodeService clock(Clock clock) {
        this.clock = clock;
        return this;
    }

    /// How long a code works; 600 seconds unless set.
    public EmailCodeService validitySeconds(int validitySeconds) {
        if (validitySeconds < 1) {
            throw new IllegalArgumentException("validitySeconds must be positive");
        }
        this.validitySeconds = validitySeconds;
        return this;
    }

    /// How long after one mail the next may be sent; 30 seconds unless set.
    public EmailCodeService resendSeconds(int resendSeconds) {
        this.resendSeconds = resendSeconds < 0 ? 0 : resendSeconds;
        return this;
    }

    /// How many tries a user is given, over however many codes, before one
    /// is entered right or the last runs out; 5 unless set.
    public EmailCodeService maxAttempts(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        this.maxAttempts = maxAttempts;
        return this;
    }

    /// The subject of the mail.
    public EmailCodeService subject(String subject) {
        this.subject = subject;
        return this;
    }

    /// The template the mail is rendered from, such as `mail/sign-in-code`, in
    /// place of the plain mail.
    public EmailCodeService template(String view) {
        this.template = view;
        return this;
    }

    /// Whether `username` signs in with a mailed code.
    public boolean isEnabled(String username) {
        EmailCode held = repository.find(username);
        return held != null && held.isEnabled();
    }

    /// Makes `username` sign in with a mailed code from now on. Call it once
    /// the user's address is known to be theirs.
    public void enable(String username) {
        repository.setEnabled(username, true);
    }

    /// Stops asking `username` for a mailed code.
    public void disable(String username) {
        repository.setEnabled(username, false);
    }

    /// Mails `username` a new code, which replaces any that was waiting.
    /// False, and nothing sent, when the user does not sign in with a mailed
    /// code, the last was mailed too recently, the tries were used up and
    /// the code they were used on has not run out, the user has no address,
    /// or the mail could not be handed over.
    public boolean send(String username) {
        if (username == null) {
            return false;
        }
        String address = addresses.emailFor(username);
        if (address == null || !Mail.isAddress(address)) {
            return false;
        }
        long now = clock.currentTimeMillis();
        String code = newCode();
        String hash = hash(username, code);
        // The store decides whether one is due, in the step that keeps the
        // code: two requests at once would each find none sent and each mail
        // one.
        if (!repository.saveCode(username, hash, now + validitySeconds * 1000L, now,
                now - resendSeconds * 1000L, maxAttempts)) {
            return false;
        }
        int minutes = (validitySeconds + 59) / 60;
        try {
            Mail.Builder message = Mail.to(address).subject(subject);
            if (template != null) {
                message.template(template, new Model().addAttribute("code", code)
                        .addAttribute("username", username)
                        .addAttribute("minutes", Integer.valueOf(minutes)));
            } else {
                message.text("Your sign-in code is " + code + "\r\n\r\nIt works for " + minutes
                        + " minutes. If you are not signing in, somebody has your password: "
                        + "change it.\r\n");
            }
            mail.send(message.build());
            return true;
        } catch (IOException err) {
            return notSent(username, hash, err);
        } catch (RuntimeException err) {
            // A template that throws, an address the mail refuses: the
            // sign-in goes on without this mail, it does not end in an error.
            return notSent(username, hash, err);
        }
    }

    private boolean notSent(String username, String hash, Exception err) {
        System.err.println("cn1: a sign-in code mail was not sent: " + err.getMessage());
        // Nobody has the code, so it is not left to be guessed at.
        repository.clear(username, hash, false);
        return false;
    }

    /// Whether `code` is the one mailed to `username`. A right code is used
    /// up; a wrong one is counted against the code that is waiting.
    public boolean verify(String username, String code) {
        String digits = normalize(code);
        if (username == null || digits == null) {
            return false;
        }
        EmailCode held = repository.find(username);
        if (held == null || !held.isEnabled() || held.getCodeHash().length() == 0
                || held.getExpiresAt() < clock.currentTimeMillis()) {
            return false;
        }
        // Counted before it is compared, so that guesses sent together cannot
        // each be the last one allowed.
        if (!repository.countAttempt(username, maxAttempts)) {
            return false;
        }
        String hash = hash(username, digits);
        if (!Crypto.equalsConstantTime(WireBuffer.utf8(hash),
                WireBuffer.utf8(held.getCodeHash()))) {
            return false;
        }
        return repository.clear(username, hash, true);
    }

    /// Whether `code` is written as a mailed code is: six digits.
    public static boolean isCodeShaped(String code) {
        return normalize(code) != null;
    }

    private static String normalize(String code) {
        if (code == null) {
            return null;
        }
        StringBuilder digits = new StringBuilder(6);
        for (int iter = 0 ; iter < code.length() ; iter++) {
            char c = code.charAt(iter);
            if (c == ' ' || c == '-') {
                continue;
            }
            if (c < '0' || c > '9' || digits.length() == 6) {
                return null;
            }
            digits.append(c);
        }
        return digits.length() == 6 ? digits.toString() : null;
    }

    /// Six digits, each as likely as the next.
    private static String newCode() {
        try {
            // A value that would favour the low codes is passed over.
            long limit = 0x100000000L - 0x100000000L % 1000000L;
            while (true) {
                byte[] r = Crypto.randomBytes(4);
                long value = ((r[0] & 0xffL) << 24) | ((r[1] & 0xffL) << 16)
                        | ((r[2] & 0xffL) << 8) | (r[3] & 0xffL);
                if (value < limit) {
                    String text = String.valueOf(1000000L + value % 1000000L);
                    return text.substring(1);
                }
            }
        } catch (IOException err) {
            throw new IllegalStateException("No random source: " + err.getMessage(), err);
        }
    }

    /// Bound to the user, so that one user's row is no use against another.
    private static String hash(String username, String code) {
        return Base64Url.encode(Crypto.sha256(WireBuffer.utf8(
                SecuritySchema.usernameKey(username) + ":" + code)));
    }
}
