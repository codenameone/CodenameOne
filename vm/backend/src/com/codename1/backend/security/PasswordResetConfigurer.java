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

import com.codename1.backend.security.mail.PasswordResetService;
import com.codename1.backend.security.ratelimit.InMemoryRateLimiter;
import com.codename1.backend.security.ratelimit.RateLimiter;

/// The pages of a password reset by mail.
///
/// ```java
/// http.formLogin(Customizer.withDefaults())
///     .passwordReset(Customizer.withDefaults());
/// ```
///
/// ```properties
/// cn1.security.base-url=https://example.com
/// ```
///
/// `GET /forgot-password` asks for a user name and `POST /forgot-password`
/// mails that user a link, answering the same whether or not there is such a
/// user. The link opens `GET /reset-password`, which asks for the new
/// password, and `POST /reset-password` sets it and sends the user to the
/// sign-in page. All four are open to everyone, whatever the authorization
/// rules say.
///
/// The work is a [PasswordResetService]'s: the one given here, or the
/// application's bean of that type.
///
/// #### The address in the link
///
/// The link is mailed to somebody other than whoever asked for it, so it is
/// never built from the request's `Host` header, which the caller chooses.
/// It starts with `cn1.security.base-url`, or what [#baseUrl] is given. With
/// neither, a request to `localhost` gets a link to itself, which is what a
/// developer's machine needs, and any other request is refused with an error
/// that names the setting.
///
/// #### How often
///
/// Asking is counted for the client's network and for the name asked about,
/// 5 in 15 minutes each unless [#attemptLimiter] says otherwise, and more are
/// answered 429. The count for a name is kept whether or not there is such a
/// user, so it tells nobody which there are; it is what stops one person's
/// mailbox being filled from many addresses.
public final class PasswordResetConfigurer extends SecurityConfigurer {
    /// The setting that holds the address the server is reached at from
    /// outside, `https://example.com`, with no path.
    public static final String BASE_URL = "cn1.security.base-url";

    private PasswordResetService service;
    private String requestUrl = "/forgot-password";
    private String resetUrl = "/reset-password";
    private String baseUrl;
    private boolean servePages = true;
    private RateLimiter attempts;
    private boolean attemptsGiven;

    PasswordResetConfigurer() {
    }

    /// What mails the link and sets the password, in place of the
    /// application's bean.
    public PasswordResetConfigurer service(PasswordResetService service) {
        this.service = service;
        return this;
    }

    /// Where a reset is asked for; `/forgot-password` unless set.
    public PasswordResetConfigurer requestUrl(String requestUrl) {
        this.requestUrl = Responses.path(requestUrl, "requestUrl");
        return this;
    }

    /// Where the mailed link leads and the new password is posted;
    /// `/reset-password` unless set.
    public PasswordResetConfigurer resetUrl(String resetUrl) {
        this.resetUrl = Responses.path(resetUrl, "resetUrl");
        return this;
    }

    /// The address the server is reached at from outside, in place of the
    /// `cn1.security.base-url` setting.
    public PasswordResetConfigurer baseUrl(String baseUrl) {
        this.baseUrl = checkBase(baseUrl, "baseUrl");
        return this;
    }

    /// False leaves the two `GET` pages to the application, which then posts
    /// the fields `username`, and `token`, `password` and `confirm`, to the
    /// same paths.
    public PasswordResetConfigurer servePages(boolean servePages) {
        this.servePages = servePages;
        return this;
    }

    /// What counts requests for a link, in place of the application's
    /// [RateLimiter] bean and of the count kept in this process; null to
    /// count none.
    public PasswordResetConfigurer attemptLimiter(RateLimiter attemptLimiter) {
        this.attempts = attemptLimiter;
        this.attemptsGiven = true;
        return this;
    }

    static String checkBase(String value, String what) {
        if (value == null) {
            return null;
        }
        String url = value.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        boolean scheme = url.regionMatches(true, 0, "https://", 0, 8)
                || url.regionMatches(true, 0, "http://", 0, 7);
        if (!scheme || url.indexOf('/', url.indexOf("//") + 2) >= 0 || url.indexOf('?') >= 0
                || url.indexOf('#') >= 0) {
            throw new IllegalArgumentException(what + " must be the server's address and "
                    + "nothing after it, such as https://example.com: " + value);
        }
        return url;
    }

    /// The configured address, or null when requests to localhost alone are
    /// served.
    static String base(HttpSecurity http, String given) {
        if (given != null) {
            return given;
        }
        try {
            String set = http.getConfig() == null ? null : http.getConfig().get(BASE_URL, null);
            return set == null || set.trim().length() == 0 ? null : checkBase(set, BASE_URL);
        } catch (java.io.IOException err) {
            throw new IllegalStateException(BASE_URL + " could not be read from the "
                    + "configuration: " + err.getMessage(), err);
        }
    }

    @Override
    public void init(HttpSecurity http) {
        PasswordResetService reset = service != null ? service
                : http.getSharedObject(PasswordResetService.class);
        if (reset == null) {
            throw new IllegalStateException("passwordReset() needs a PasswordResetService, and "
                    + "this application has none. Declare a bean of that type, or call "
                    + "service(...) on the passwordReset() configurer.");
        }
        RateLimiter counting = attempts;
        if (!attemptsGiven) {
            RateLimiter bean = http.uniqueSharedObject(RateLimiter.class,
                    "The password reset's limit", "passwordReset().attemptLimiter(...)");
            RateLimiter derived = bean == null ? null : bean.derive("password-reset", 5, 900);
            counting = derived != null ? derived : bean != null ? bean
                    : new InMemoryRateLimiter(5, 900);
        }
        http.addFilter(new PasswordResetFilter(reset, requestUrl, resetUrl,
                base(http, baseUrl), servePages, counting, http.loginPage()),
                HttpSecurity.ORDER_PASSWORD_RESET);
        // A generated login page offers the way here, and says so when a
        // reset sends the user back to it.
        http.passwordResetPage(requestUrl);
        http.permit(AntPathRequestMatcher.antMatcher("GET", requestUrl));
        http.permit(AntPathRequestMatcher.antMatcher("POST", requestUrl));
        http.permit(AntPathRequestMatcher.antMatcher("GET", resetUrl));
        http.permit(AntPathRequestMatcher.antMatcher("POST", resetUrl));
    }
}
