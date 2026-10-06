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
/// [RecoveryCodeService] the same way.
///
/// Attempts at the code are counted for each user, whichever session they
/// come from: signing in again with the password does not buy more guesses.
/// Five are allowed in five minutes unless `cn1.security.mfa.attempts` and
/// `cn1.security.mfa.attemptsWindowSeconds` say otherwise.
///
/// Where they are counted depends on what the application declares. With
/// nothing, in this process. With one [RateLimiter] bean, wherever that bean
/// counts -- a [com.codename1.backend.security.ratelimit.JdbcRateLimiter]
/// makes it one count for every process -- in a limiter the bean derives for
/// the purpose, with the limit above and not the bean's own; see
/// [RateLimiter#derive]. A limiter given to [#attemptLimiter], or a bean that
/// derives none, counts by its own limit, and setting the two keys as well is
/// then refused when the chain is built, since they would decide nothing.
///
/// With several [RateLimiter] beans the one marked `@Primary` is the one, as it
/// would be for an injection. With none marked, or more than one, the chain is
/// refused when it is built, with a message that names the beans: counting in
/// this process instead would look exactly like the shared count the
/// application asked for, and not be it. Mark one `@Primary`, or inject the
/// one meant -- `@Qualifier` names it -- and give it to [#attemptLimiter].
///
/// What holds a sign-in back is the chain's [SecondFactorPolicy], which every
/// sign-in that ends in a session consults; see [SessionSignIn].
///
/// #### Every way of presenting a first factor
///
/// A second factor that one mechanism of the chain skipped would be optional,
/// so each mechanism has a rule for a user who has one:
///
/// - [HttpSecurity#formLogin] and [HttpSecurity#oauth2Login]: the sign-in is
///   held back for the code.
/// - [HttpSecurity#webAuthn]: a passkey whose authenticator verified the user
///   is two factors in one step and signs in; one that did not is held back.
/// - [HttpSecurity#httpBasic]: refused, with a 401 that says why once the
///   password has been checked. Credentials sent with every request have no
///   second step to present a code in. A chain can exempt it, by name:
///   [HttpBasicConfigurer#secondFactorExempt].
/// - [HttpSecurity#rememberMe]: the cookie signs the user in only when the
///   sign-in that issued it passed the second factor. Any other cookie of
///   theirs -- one from before they enrolled -- is withdrawn.
/// - [HttpSecurity#oauth2ResourceServer] and [HttpSecurity#apiKey]: accepted.
///   A token or a key is not a user signing in: nobody is there to type a
///   code, and it stands for a sign-in that already happened -- the one that
///   had the token issued, which went through this policy if it was made
///   here -- or for a decision of whoever minted the key. What limits them is
///   their own lifetime and revocation.
///
/// A filter or provider of the application's own that makes a request a
/// user's is outside all of this; it can ask [SecondFactorPolicy#requires].
public final class MfaConfigurer extends SecurityConfigurer {
    /// The setting that holds how many attempts at a code a user has in one
    /// window; 5 unless set.
    public static final String ATTEMPTS = "cn1.security.mfa.attempts";
    /// The setting that holds the length of that window in seconds; 300
    /// unless set.
    public static final String ATTEMPTS_WINDOW = "cn1.security.mfa.attemptsWindowSeconds";

    private TotpService totp;
    private RecoveryCodeService recoveryCodes;
    private String page = "/login/mfa";
    private boolean customPage;
    private String processingUrl;
    private String codeParameter = "code";
    private int pendingSeconds = 300;
    private RateLimiter attempts;
    private boolean attemptsGiven;
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

    /// What counts attempts, each user under a key of their own, in place of
    /// the application's [RateLimiter] bean and of the count kept in this
    /// process; null to count none.
    public MfaConfigurer attemptLimiter(RateLimiter attemptLimiter) {
        this.attempts = attemptLimiter;
        this.attemptsGiven = true;
        return this;
    }

    /// What counts attempts for this chain: the limiter given; or one the
    /// application's [RateLimiter] bean derives, sized by the configuration;
    /// or that bean itself when it derives none; or a count in this process
    /// when the application has no such bean. Several beans and no one
    /// `@Primary` among them is refused, never counted around.
    private RateLimiter attempts(HttpSecurity http) {
        try {
            com.codename1.backend.Config config = http.getConfig();
            boolean configured = config.get(ATTEMPTS, null) != null
                    || config.get(ATTEMPTS_WINDOW, null) != null;
            int permits = config.getInt(ATTEMPTS, 5);
            int window = config.getInt(ATTEMPTS_WINDOW, 300);
            if (permits < 1 || window < 1) {
                throw new IllegalStateException(ATTEMPTS + " and " + ATTEMPTS_WINDOW
                        + " must each be at least 1");
            }
            RateLimiter limiter = attempts;
            String counted = "the limiter given to attemptLimiter(...)";
            if (!attemptsGiven) {
                RateLimiter bean = http.uniqueSharedObject(RateLimiter.class,
                        "The second factor's attempt limit", "mfa().attemptLimiter(...)");
                if (bean == null) {
                    return new InMemoryRateLimiter(permits, window);
                }
                RateLimiter derived = bean.derive("mfa", permits, window);
                if (derived != null) {
                    return derived;
                }
                limiter = bean;
                counted = "the application's RateLimiter bean, which derives no limiter";
            }
            if (configured) {
                throw new IllegalStateException(ATTEMPTS + " and " + ATTEMPTS_WINDOW
                        + " would decide nothing: attempts are counted by " + counted
                        + ", by a limit of its own. Remove the settings, or give that limiter "
                        + "the numbers.");
            }
            return limiter;
        } catch (java.io.IOException err) {
            throw new IllegalStateException("The second factor's attempt limit could not be "
                    + "read from the configuration: " + err.getMessage(), err);
        }
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
                processing(), codeParameter, pendingSeconds * 1000L, attempts(http), success,
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
        SecurityContextRepository repository = http.resolveSecurityContextRepository();
        if (repository instanceof HttpSessionSecurityContextRepository) {
            filter.kinds((HttpSessionSecurityContextRepository) repository);
        }
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
