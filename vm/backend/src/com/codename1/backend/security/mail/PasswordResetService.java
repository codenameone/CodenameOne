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

import com.codename1.backend.mail.Mail;
import com.codename1.backend.mail.MailSender;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.core.userdetails.UserDetailsPasswordService;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.core.userdetails.UsernameNotFoundException;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.rememberme.PersistentTokenRepository;
import java.io.IOException;

/// Resets a forgotten password by mail.
///
/// ```java
/// @Bean
/// PasswordResetService passwordReset(OneTimeTokenService tokens, MailSender mail,
///         JdbcUserDetailsManager users, PasswordEncoder encoder) {
///     return new PasswordResetService(tokens, mail, users, users, encoder);
/// }
/// ```
///
/// [#request] mails a link that works once and for half an hour; [#reset]
/// takes the token from that link and the new password. `http.passwordReset()`
/// puts both behind pages; an application with pages of its own calls the two
/// methods from its controllers.
///
/// [#request] returns the same way whether or not there is such a user, and
/// whether or not the mail could be sent, so that asking tells nobody which
/// accounts exist. The mail is handed to the server's mail queue, so the call
/// never waits for a mail server. For an account that exists it still makes a
/// token and stores it, which somebody timing many requests could measure:
/// limit how often it can be called. The pages do, a controller of the
/// application's own has to.
///
/// A reset removes the user's remember-me sign-ins when the service is given
/// their store. A session that is already signed in stays so until it ends.
///
/// The mail is plain by default. Name a template to send the application's
/// own: it is rendered with `link`, `username` and `minutes`.
public final class PasswordResetService {
    /// The purpose of the tokens this service gives out.
    public static final String PURPOSE = "password-reset";

    private final OneTimeTokenService tokens;
    private final MailSender mail;
    private final UserDetailsService users;
    private final UserDetailsPasswordService passwords;
    private final PasswordEncoder encoder;
    private EmailAddressResolver addresses = EmailAddressResolver.USERNAME;
    private PersistentTokenRepository rememberMe;
    private int validityMinutes = 30;
    private int minimumLength = 8;
    private String subject = "Reset your password";
    private String template;

    public PasswordResetService(OneTimeTokenService tokens, MailSender mail,
            UserDetailsService users, UserDetailsPasswordService passwords,
            PasswordEncoder encoder) {
        if (tokens == null || mail == null || users == null || passwords == null
                || encoder == null) {
            throw new IllegalArgumentException("A password reset needs tokens, a mail sender, "
                    + "the users, their password store and an encoder; none can be null");
        }
        this.tokens = tokens;
        this.mail = mail;
        this.users = users;
        this.passwords = passwords;
        this.encoder = encoder;
    }

    /// Where each user's mail goes. Without one, a user name that is an
    /// address is used and other users are sent nothing.
    public PasswordResetService addressResolver(EmailAddressResolver addressResolver) {
        this.addresses = addressResolver == null ? EmailAddressResolver.USERNAME
                : addressResolver;
        return this;
    }

    /// The remember-me sign-ins to remove when a password is reset.
    public PasswordResetService rememberMeTokens(PersistentTokenRepository repository) {
        this.rememberMe = repository;
        return this;
    }

    /// How long a link works; 30 minutes unless set.
    public PasswordResetService validityMinutes(int validityMinutes) {
        if (validityMinutes < 1) {
            throw new IllegalArgumentException("validityMinutes must be positive");
        }
        this.validityMinutes = validityMinutes;
        return this;
    }

    /// The shortest password [#reset] takes; 8 unless set.
    public PasswordResetService minimumLength(int minimumLength) {
        this.minimumLength = minimumLength < 1 ? 1 : minimumLength;
        return this;
    }

    /// The subject of the mail.
    public PasswordResetService subject(String subject) {
        this.subject = subject;
        return this;
    }

    /// The template the mail is rendered from, such as `mail/password-reset`,
    /// in place of the plain mail.
    public PasswordResetService template(String view) {
        this.template = view;
        return this;
    }

    /// The shortest password [#reset] takes.
    public int getMinimumLength() {
        return minimumLength;
    }

    /// Mails `username` a link to `resetUrl` with a `token` parameter added.
    /// Does nothing, and says nothing, when there is no such user, the
    /// account is disabled or it has no address.
    ///
    /// #### Parameters
    ///
    /// - `username`: the account, as the person asking typed it
    /// - `resetUrl`: the whole address of the page that takes the new
    ///   password, `https://example.com/reset-password`. Build it from the
    ///   server's configured address, never from the request's `Host` header:
    ///   that header is the caller's to choose, and the link is mailed to
    ///   somebody else.
    public void request(String username, String resetUrl) {
        if (username == null || username.length() == 0 || username.length() > 320) {
            return;
        }
        UserDetails user;
        try {
            user = users.loadUserByUsername(username);
        } catch (UsernameNotFoundException none) {
            return;
        }
        if (user == null || !user.isEnabled()) {
            return;
        }
        String address = addresses.emailFor(user.getUsername());
        if (address == null || !Mail.isAddress(address)) {
            return;
        }
        String token = tokens.create(PURPOSE, user.getUsername(), validityMinutes * 60);
        String link = MailText.link(resetUrl, token);
        try {
            Mail.Builder message = Mail.to(address).subject(subject);
            if (template != null) {
                message.template(template, new Model().addAttribute("link", link)
                        .addAttribute("username", user.getUsername())
                        .addAttribute("minutes", Integer.valueOf(validityMinutes)));
            } else {
                String[] lines = {
                    "Somebody asked to reset the password of your account.",
                    "Open this link within " + validityMinutes + " minutes to choose a new one:",
                    "If this was not you, ignore this mail: your password has not changed."};
                message.text(MailText.text(lines, link)).html(MailText.html(lines, link,
                        "Reset password"));
            }
            mail.send(message.build());
        } catch (IOException err) {
            // Not thrown: an error for the accounts that exist tells them apart.
            MailText.notSent("password reset", err);
        } catch (RuntimeException err) {
            MailText.notSent("password reset", err);
        }
    }

    /// Whether `password` is one [#reset] would take.
    public boolean isAcceptable(String password) {
        return password != null && password.length() >= minimumLength
                && password.length() <= 1024;
    }

    /// Sets the password of the user `token` was mailed to. False when the
    /// token is not good -- wrong, expired or used -- or the password is too
    /// short. A password that is too short leaves the token good for another
    /// try. A token is used up before the password is stored, so that it
    /// cannot set two: if the store then fails, this throws what the store
    /// threw and the user asks for another link.
    public boolean reset(String token, String newPassword) {
        if (!isAcceptable(newPassword)) {
            return false;
        }
        String username = tokens.consume(PURPOSE, token);
        if (username == null) {
            return false;
        }
        UserDetails user;
        try {
            user = users.loadUserByUsername(username);
        } catch (UsernameNotFoundException gone) {
            return false;
        }
        if (user == null || !user.isEnabled()) {
            return false;
        }
        passwords.updatePassword(user, encoder.encode(newPassword));
        if (rememberMe != null) {
            rememberMe.removeUserTokens(user.getUsername());
        }
        return true;
    }
}
