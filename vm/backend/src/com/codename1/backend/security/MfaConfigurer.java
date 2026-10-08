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
/// #### How many guesses, and who can be kept out
///
/// Wrong codes are counted, and too many are answered 429. Three numbers
/// decide how many:
///
/// - `cn1.security.mfa.attempts`: wrong one-time codes for one user, from
///   every address and session together. 5 unless set.
/// - `cn1.security.mfa.attemptsPerAddress`: how many of those one client
///   network may use up, and how many wrong recovery codes it may try for that
///   user. Half of `attempts` rounded up unless set: 3.
/// - `cn1.security.mfa.attemptsWindowSeconds`: the window both are counted
///   in. 300 unless set.
///
/// A client network is the client's address, an IPv6 address counting by its
/// first 64 bits; see
/// [com.codename1.backend.security.ratelimit.RateLimitKeys#clientNetwork].
/// A code is counted before it is looked at and a right one hands its counts
/// back, so only wrong ones stay counted. A sign-in that completes -- by a
/// one-time code, a recovery code, or a passkey that verified the user --
/// clears what was counted against that user.
///
/// What that gives somebody who has a user's password and not their second
/// factor, with the numbers above:
///
/// - **At most 5 wrong one-time codes in a window, in total.** Signing in again
///   with the password, a new session or another address buys none: the count
///   is the user's. At most 3 of the 5 from one network.
/// - **Recovery codes: at most 3 wrong ones in a window from each network**,
///   with no total for the user. A code is ten characters out of 31, and a
///   user has ten: a guess is right once in 8 x 10^13.
/// - **They cannot keep the user out from one network.** When they have used
///   their 3 there, 2 of the user's 5 are left for everybody else, and a
///   right code needs one.
/// - **From two networks or more they can use all 5**, and one-time codes are
///   then refused for that user from everywhere until the window has run. The
///   user still signs in with a recovery code, which has its own count at
///   their own network, or with a passkey, which is not counted at all; either
///   clears the count. The attacker can run it up again, so this lasts until
///   the password is changed -- and a run of 429s for one user is the sign
///   that it has to be.
/// - **At the user's own network they can use up both counts**, the one-time
///   codes' and the recovery codes'. A passkey is then the way in. A server
///   behind a proxy it has not been told to believe sees one address for every
///   client, which makes every client the user's own network: set
///   `cn1.server.forwardHeaders`.
///
/// `attemptsPerAddress` equal to `attempts` gives the old trade back: any one
/// client that knows the password can keep the user out. Larger is refused.
///
/// The window is whatever the limiter means by one. The count kept in the
/// process hands attempts back evenly -- after 5 at once, one every minute --
/// and a [com.codename1.backend.security.ratelimit.JdbcRateLimiter] counts 5
/// from the first of each window.
///
/// #### Where the attempts are counted
///
/// Where they are counted depends on what the application declares. With
/// nothing, in this process. With one [RateLimiter] bean, wherever that bean
/// counts -- a [com.codename1.backend.security.ratelimit.JdbcRateLimiter]
/// makes it one count for every process -- in two limiters the bean derives
/// for the purpose, with the limits above and not the bean's own; see
/// [RateLimiter#derive]. A limiter given to [#attemptLimiter], or a bean that
/// derives none, counts by its own limit, and setting the keys as well is
/// then refused when the chain is built, since they would decide nothing.
/// One limiter has one limit, so it cannot hold a client network to less than
/// the user's total: give [#attemptLimiter(RateLimiter, RateLimiter)] two to
/// keep the guarantees above. Clearing a count needs [RateLimiter#reset]; with
/// a limiter that has none, a right code costs an attempt and nothing is
/// cleared.
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
    /// The setting that holds how many wrong one-time codes a user has in one
    /// window, from everywhere together; 5 unless set.
    public static final String ATTEMPTS = "cn1.security.mfa.attempts";
    /// The setting that holds how many of those one client network may use
    /// up, and how many wrong recovery codes it may try for the user; half of
    /// [#ATTEMPTS], rounded up, unless set.
    public static final String ATTEMPTS_PER_ADDRESS = "cn1.security.mfa.attemptsPerAddress";
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
    private RateLimiter addressAttempts;
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

    /// What counts attempts, in place of the application's [RateLimiter] bean
    /// and of the count kept in this process; null to count none.
    ///
    /// The one limiter counts a user's attempts and their attempts at each
    /// client network, under keys of their own and by its one limit. A client
    /// network is then allowed all of a user's attempts, and one client that
    /// knows the password can keep the user out; see
    /// [#attemptLimiter(RateLimiter, RateLimiter)].
    public MfaConfigurer attemptLimiter(RateLimiter attemptLimiter) {
        return attemptLimiter(attemptLimiter, attemptLimiter);
    }

    /// What counts attempts, as two limits: `perUser` a user's wrong one-time
    /// codes from everywhere, and `perAddress` their wrong codes at one client
    /// network -- one-time codes and recovery codes apart.
    ///
    /// Give `perAddress` the smaller limit. What is left of `perUser` when one
    /// network has used its share up is what everybody else, the user
    /// included, still has.
    /// @param perUser the limiter for a user's total, or null to count none
    /// @param perAddress the limiter for a user at one network, or null to
    ///     count none
    public MfaConfigurer attemptLimiter(RateLimiter perUser, RateLimiter perAddress) {
        this.attempts = perUser;
        this.addressAttempts = perAddress;
        this.attemptsGiven = true;
        return this;
    }

    /// What counts attempts for this chain, as {a user's total, a user at one
    /// network}: the limiters given; or two the application's [RateLimiter]
    /// bean derives, sized by the configuration; or that bean itself, twice,
    /// when it derives none; or two counts in this process when the
    /// application has no such bean. Several beans and no one `@Primary` among
    /// them is refused, never counted around.
    private RateLimiter[] attempts(HttpSecurity http) {
        try {
            com.codename1.backend.Config config = http.getConfig();
            boolean configured = config.get(ATTEMPTS, null) != null
                    || config.get(ATTEMPTS_PER_ADDRESS, null) != null
                    || config.get(ATTEMPTS_WINDOW, null) != null;
            int permits = config.getInt(ATTEMPTS, 5);
            int window = config.getInt(ATTEMPTS_WINDOW, 300);
            if (permits < 1 || window < 1) {
                throw new IllegalStateException(ATTEMPTS + " and " + ATTEMPTS_WINDOW
                        + " must each be at least 1");
            }
            // Half, rounded up: what one network cannot use is what the others have.
            int perAddress = config.getInt(ATTEMPTS_PER_ADDRESS, permits - permits / 2);
            if (perAddress < 1 || perAddress > permits) {
                throw new IllegalStateException(ATTEMPTS_PER_ADDRESS + " is " + perAddress
                        + "; it must be at least 1 and no more than " + ATTEMPTS + ", which is "
                        + permits + ": one client network cannot use more attempts than the "
                        + "user has.");
            }
            String counted = "the limiter given to attemptLimiter(...)";
            RateLimiter[] limiters = {attempts, addressAttempts};
            if (!attemptsGiven) {
                RateLimiter bean = http.uniqueSharedObject(RateLimiter.class,
                        "The second factor's attempt limit", "mfa().attemptLimiter(...)");
                if (bean == null) {
                    return new RateLimiter[] {new InMemoryRateLimiter(permits, window),
                        new InMemoryRateLimiter(perAddress, window)};
                }
                RateLimiter derived = bean.derive("mfa", permits, window);
                RateLimiter derivedPerAddress = derived == null ? null
                        : bean.derive("mfa-address", perAddress, window);
                if (derived != null && derivedPerAddress != null) {
                    return new RateLimiter[] {derived, derivedPerAddress};
                }
                limiters = new RateLimiter[] {bean, bean};
                counted = "the application's RateLimiter bean, which derives no limiter";
            }
            if (configured) {
                throw new IllegalStateException(ATTEMPTS + " and " + ATTEMPTS_WINDOW
                        + " would decide nothing: attempts are counted by " + counted
                        + ", by a limit of its own. Remove the settings, or give that limiter "
                        + "the numbers.");
            }
            return limiters;
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
        RateLimiter[] counting = attempts(http);
        filter = new SecondFactorAuthenticationFilter(codes, recovery, page, !customPage,
                processing(), codeParameter, pendingSeconds * 1000L, counting[0], counting[1],
                success,
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
