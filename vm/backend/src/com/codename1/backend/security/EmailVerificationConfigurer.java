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
package com.codename1.backend.security;

import com.codename1.backend.security.mail.EmailVerificationService;

/// The address a mailed verification link opens.
///
/// ```java
/// http.emailVerification(Customizer.withDefaults());
/// ```
///
/// `GET /verify-email?token=...` is a page with one button, and pressing it --
/// a `POST` -- records the address as proved and sends the user to
/// `/?verified`; a link that is no longer good is answered 400. Opening the
/// link proves nothing by itself, because a mail gateway opens every link in a
/// mail before its reader does. The address is open to everyone: a link is
/// often opened on another device than the one that is signed in.
///
/// The application mails the link itself, where it takes the address, with
/// [#linkUrl] as where it leads:
///
/// ```java
/// verification.send(username, email, "https://example.com/verify-email");
/// ```
///
/// The work is an [EmailVerificationService]'s: the one given here, or the
/// application's bean of that type.
public final class EmailVerificationConfigurer extends SecurityConfigurer {
    private EmailVerificationService service;
    private String confirmUrl = "/verify-email";
    private String successUrl = "/?verified";

    EmailVerificationConfigurer() {
    }

    /// What checks the link, in place of the application's bean.
    public EmailVerificationConfigurer service(EmailVerificationService service) {
        this.service = service;
        return this;
    }

    /// The path the link opens; `/verify-email` unless set.
    public EmailVerificationConfigurer confirmUrl(String confirmUrl) {
        this.confirmUrl = Responses.path(confirmUrl, "confirmUrl");
        return this;
    }

    /// Where the user is sent once the address is proved; `/?verified` unless
    /// set.
    public EmailVerificationConfigurer successUrl(String successUrl) {
        this.successUrl = Responses.path(successUrl, "successUrl");
        return this;
    }

    /// The whole address to give
    /// [EmailVerificationService#send(String, String, String)]: the server's
    /// address from `cn1.security.base-url` and the path the link opens.
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: the setting is missing
    public String linkUrl() {
        String base = PasswordResetConfigurer.base(getBuilder(), null);
        if (base == null) {
            throw new IllegalStateException("A link cannot be mailed without knowing the "
                    + "address this server is reached at. Set "
                    + PasswordResetConfigurer.BASE_URL + "=https://your.host in "
                    + "application.properties.");
        }
        return base + confirmUrl;
    }

    @Override
    public void init(HttpSecurity http) {
        EmailVerificationService verification = service != null ? service
                : http.getSharedObject(EmailVerificationService.class);
        if (verification == null) {
            throw new IllegalStateException("emailVerification() needs an "
                    + "EmailVerificationService, and this application has none. Declare a bean "
                    + "of that type, or call service(...) on the emailVerification() "
                    + "configurer.");
        }
        http.addFilter(new EmailVerificationFilter(verification, confirmUrl, successUrl),
                HttpSecurity.ORDER_EMAIL_VERIFICATION);
        http.permit(AntPathRequestMatcher.antMatcher("GET", confirmUrl));
        http.permit(AntPathRequestMatcher.antMatcher("POST", confirmUrl));
    }
}
