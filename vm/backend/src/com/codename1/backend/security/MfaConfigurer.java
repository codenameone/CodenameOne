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
package com.codename1.backend.security;

import com.codename1.backend.security.mfa.RecoveryCodeService;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.security.ratelimit.InMemoryRateLimiter;
import com.codename1.backend.security.ratelimit.RateLimiter;

/// A second factor at sign-in.
///
/// ```java
/// http.formLogin(Customizer.withDefaults())
///     .mfa(mfa -> mfa.totpService(totp).recoveryCodeService(recoveryCodes));
/// ```
///
/// A user with a confirmed authenticator app -- see [TotpService] -- is no
/// longer signed in by their password alone. Their password is accepted, the
/// request stays anonymous, and they are sent to `/login/mfa`, which asks for a
/// code; posting a right one there signs them in, exactly as the password
/// would have without this. A user who has not enrolled signs in as before.
///
/// With nothing set the chain serves a plain page at `GET /login/mfa` and
/// takes the code at `POST /login/mfa` in the field `code`. Naming a
/// `secondFactorPage` hands the page to the application. Either way both are
/// open to everyone, whatever the authorization rules say: the user on them
/// is, as far as the chain knows, nobody yet.
///
/// The codes come from the [TotpService] given here or the application's bean
/// of that type, and recovery codes are accepted when there is a
/// [RecoveryCodeService] the same way. Five attempts are allowed in five
/// minutes for each pending sign-in, counted in this process; see
/// [#attemptLimiter] to count across processes or to change the limit.
///
/// What holds a sign-in back is the chain's [SecondFactorPolicy], which every
/// sign-in that ends in a session consults; see [SessionSignIn].
public final class MfaConfigurer extends SecurityConfigurer {
    private TotpService totp;
    private RecoveryCodeService recoveryCodes;
    private String page = "/login/mfa";
    private boolean customPage;
    private String processingUrl;
    private String codeParameter = "code";
    private int pendingSeconds = 300;
    private RateLimiter attempts = new InMemoryRateLimiter(5, 300);
    private String defaultSuccessUrl = "/";
    private AuthenticationSuccessHandler successHandler;
    private Clock clock = Clock.SYSTEM;
    private SecondFactorAuthenticationFilter filter;

    MfaConfigurer() {
    }

    /// What checks one-time codes, in place of the application's bean.
    public MfaConfigurer totpService(TotpService totpService) {
        this.totp = totpService;
        return this;
    }

    /// What checks recovery codes, in place of the application's bean.
    public MfaConfigurer recoveryCodeService(RecoveryCodeService recoveryCodeService) {
        this.recoveryCodes = recoveryCodeService;
        return this;
    }

    /// The application's own page that asks for the code: a path it serves.
    public MfaConfigurer secondFactorPage(String secondFactorPage) {
        this.page = Responses.path(secondFactorPage, "secondFactorPage");
        this.customPage = true;
        return this;
    }

    /// Where the code is posted; the page's path unless set.
    public MfaConfigurer processingUrl(String processingUrl) {
        this.processingUrl = Responses.path(processingUrl, "processingUrl");
        return this;
    }

    /// The form field the code is in; `code` unless set.
    public MfaConfigurer codeParameter(String codeParameter) {
        this.codeParameter = CsrfFilter.requireName(codeParameter, "codeParameter");
        return this;
    }

    /// How long a user has to enter their code after their password was
    /// accepted; 300 seconds unless set.
    public MfaConfigurer pendingValiditySeconds(int pendingValiditySeconds) {
        if (pendingValiditySeconds < 1) {
            throw new IllegalArgumentException("pendingValiditySeconds must be positive");
        }
        this.pendingSeconds = pendingValiditySeconds;
        return this;
    }

    /// What counts attempts, each pending sign-in under a key of its own; null
    /// to count none.
    public MfaConfigurer attemptLimiter(RateLimiter attemptLimiter) {
        this.attempts = attemptLimiter;
        return this;
    }

    /// Where a user goes after the code when no page asked for the sign-in.
    public MfaConfigurer defaultSuccessUrl(String defaultSuccessUrl) {
        this.defaultSuccessUrl = Responses.path(defaultSuccessUrl, "defaultSuccessUrl");
        return this;
    }

    /// Answers a completed sign-in itself, instead of the redirect.
    public MfaConfigurer successHandler(AuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
        return this;
    }

    /// The clock the pending sign-in's lifetime is read from; for tests.
    public MfaConfigurer clock(Clock clock) {
        this.clock = clock;
        return this;
    }

    private String processing() {
        return processingUrl != null ? processingUrl : page;
    }

    @Override
    public void init(HttpSecurity http) {
        TotpService codes = totp != null ? totp : http.getSharedObject(TotpService.class);
        if (codes == null) {
            throw new IllegalStateException("mfa() needs something to check codes with, and "
                    + "this application has none. Declare a TotpService bean, or call "
                    + "totpService(...) on the mfa() configurer.");
        }
        RecoveryCodeService recovery = recoveryCodes != null ? recoveryCodes
                : http.getSharedObject(RecoveryCodeService.class);
        AuthenticationSuccessHandler success = successHandler;
        if (success == null) {
            SavedRequestAwareAuthenticationSuccessHandler saved =
                    new SavedRequestAwareAuthenticationSuccessHandler();
            saved.setDefaultTargetUrl(defaultSuccessUrl);
            saved.setRequestCache(new Deferred(http));
            success = saved;
        }
        filter = new SecondFactorAuthenticationFilter(codes, recovery, page, !customPage,
                processing(), codeParameter, pendingSeconds * 1000L, attempts, success,
                new SimpleUrlAuthenticationFailureHandler(http.loginPage() + "?error"), clock);
        http.secondFactorPolicy(filter);
        http.redirectsToSignIn();
        // Whoever is on these is nobody yet, so nothing can be asked of them.
        http.permit(AntPathRequestMatcher.antMatcher("GET", page));
        http.permit(AntPathRequestMatcher.antMatcher("POST", processing()));
    }

    @Override
    public void configure(HttpSecurity http) {
        filter.signIn(http.signIn());
        http.addFilter(filter, HttpSecurity.ORDER_SECOND_FACTOR);
    }

    /// The chain's request cache, asked for when it is first needed: at init()
    /// the chain has not settled which it has.
    private static final class Deferred implements RequestCache {
        private final HttpSecurity http;

        Deferred(HttpSecurity http) {
            this.http = http;
        }

        @Override
        public void saveRequest(com.codename1.backend.HttpServer.Request request) {
            http.resolveRequestCache().saveRequest(request);
        }

        @Override
        public String getRequest(com.codename1.backend.HttpServer.Request request) {
            return http.resolveRequestCache().getRequest(request);
        }

        @Override
        public void removeRequest(com.codename1.backend.HttpServer.Request request) {
            http.resolveRequestCache().removeRequest(request);
        }
    }
}
