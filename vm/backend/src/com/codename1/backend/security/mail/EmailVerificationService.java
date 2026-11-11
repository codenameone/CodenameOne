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
package com.codename1.backend.security.mail;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.AuthorizationDecision;
import com.codename1.backend.security.AuthorizationManager;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.RequestAuthorizationContext;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;
import java.util.function.Supplier;

/// Proves that an address is a user's, by mailing it a link.
///
/// ```java
/// verification.send(username, "ada@example.com", "https://example.com/verify-email");
/// // ... the link is opened
/// String username = verification.confirm(token);     // null unless it is good
/// ```
///
/// `http.emailVerification()` adds the address the link opens. An address is
/// proved once and stays so until [#send] is called with another one.
///
/// The service is an [EmailAddressResolver] too: it answers a user's proved
/// address, so a password reset or a sign-in code goes to an address the user
/// has shown to be theirs.
///
/// ```java
/// http.authorizeHttpRequests(a -> a
///         .requestMatchers("/billing/**").access(verification.verified())
///         .anyRequest().authenticated());
/// ```
///
/// The mail is plain by default. Name a template to send the application's
/// own: it is rendered with `link`, `username` and `minutes`.
public final class EmailVerificationService implements EmailAddressResolver {
    /// The purpose of the tokens this service gives out.
    public static final String PURPOSE = "email-verification";

    private final OneTimeTokenService tokens;
    private final MailSender mail;
    private final VerifiedEmailRepository repository;
    private Clock clock = Clock.SYSTEM;
    private int validityMinutes = 24 * 60;
    private String subject = "Confirm your email address";
    private String template;

    public EmailVerificationService(OneTimeTokenService tokens, MailSender mail,
            VerifiedEmailRepository repository) {
        if (tokens == null || mail == null || repository == null) {
            throw new IllegalArgumentException("Verifying an address needs tokens, a mail "
                    + "sender and a store; none can be null");
        }
        this.tokens = tokens;
        this.mail = mail;
        this.repository = repository;
    }

    /// The clock the moment of proof is read from; a test sets its own.
    public EmailVerificationService clock(Clock clock) {
        this.clock = clock;
        return this;
    }

    /// How long a link works; a day unless set.
    public EmailVerificationService validityMinutes(int validityMinutes) {
        if (validityMinutes < 1) {
            throw new IllegalArgumentException("validityMinutes must be positive");
        }
        this.validityMinutes = validityMinutes;
        return this;
    }

    /// The subject of the mail.
    public EmailVerificationService subject(String subject) {
        this.subject = subject;
        return this;
    }

    /// The template the mail is rendered from, such as `mail/verify-email`, in
    /// place of the plain mail.
    public EmailVerificationService template(String view) {
        this.template = view;
        return this;
    }

    /// Records `email` as the address `username` claims and mails it a link
    /// to `confirmUrl` with a `token` parameter added. Whatever address the
    /// user had proved is no longer theirs until this one is.
    ///
    /// #### Parameters
    ///
    /// - `confirmUrl`: the whole address the link opens,
    ///   `https://example.com/verify-email`
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: `email` is not an address
    /// - `IOException`: the mail could not be handed over
    public void send(String username, String email, String confirmUrl) throws IOException {
        if (username == null || !Mail.isAddress(email)) {
            throw new IllegalArgumentException("Not an address a mail can be sent to");
        }
        // The token first, and the address recorded with a proof made from
        // it: the address is proved only by the link that was mailed to it.
        // A token belongs to a user, not to an address, so without the proof
        // a link mailed to one address would prove whichever the user claimed
        // last -- one that is somebody else's.
        String token = tokens.create(PURPOSE, username, validityMinutes * 60);
        repository.savePending(username, email, proof(token));
        String link = MailText.link(confirmUrl, token);
        Mail.Builder message = Mail.to(email).subject(subject);
        if (template != null) {
            message.template(template, new Model().addAttribute("link", link)
                    .addAttribute("username", username)
                    .addAttribute("minutes", Integer.valueOf(validityMinutes)));
        } else {
            String[] lines = {
                // Named, so that somebody who gave another person's address
                // for an account of their own is not confirmed by a reader
                // who takes the mail for one about theirs.
                "This address was given for the account \"" + username + "\".",
                "Open this link to confirm that it is yours:",
                "If that is not your account, ignore this mail."};
            message.text(MailText.text(lines, link)).html(MailText.html(lines, link,
                    "Confirm address"));
        }
        mail.send(message.build());
    }

    /// Takes the token of a link. Answers the user whose address is now
    /// proved, or null when the token is not good.
    public String confirm(String token) {
        String username = tokens.consume(PURPOSE, token);
        if (username == null || !repository.markVerified(username, proof(token),
                clock.currentTimeMillis())) {
            return null;
        }
        return username;
    }

    /// What ties an address to the one link mailed to it: a hash of the
    /// token under a label of its own, so it is neither the token nor the
    /// hash the token is kept under.
    private static String proof(String token) {
        return Base64Url.encode(Crypto.sha256(WireBuffer.utf8("email-proof:" + token)));
    }

    /// Whether `username` has proved their address.
    public boolean isVerified(String username) {
        return repository.isVerified(username);
    }

    /// The address `username` has proved, or null.
    public String getVerifiedEmail(String username) {
        return repository.findVerifiedEmail(username);
    }

    /// Forgets `username`'s address and any link mailed to it.
    public void forget(String username) {
        tokens.revoke(PURPOSE, username);
        repository.delete(username);
    }

    /// The address `username` has proved; failing that, the user name where
    /// it is an address.
    @Override
    public String emailFor(String username) {
        String proved = getVerifiedEmail(username);
        return proved != null ? proved : EmailAddressResolver.USERNAME.emailFor(username);
    }

    /// Grants a request to a signed-in user who has proved their address.
    public AuthorizationManager<RequestAuthorizationContext> verified() {
        return new Verified(this);
    }

    private static final class Verified
            implements AuthorizationManager<RequestAuthorizationContext> {
        private final EmailVerificationService service;

        Verified(EmailVerificationService service) {
            this.service = service;
        }

        @Override
        public AuthorizationDecision check(Supplier<Authentication> authentication,
                RequestAuthorizationContext object) {
            Authentication who = authentication.get();
            return new AuthorizationDecision(who != null && who.isAuthenticated()
                    && who.getName() != null && service.isVerified(who.getName()));
        }
    }
}
