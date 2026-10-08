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

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.rememberme.InMemoryTokenRepositoryImpl;
import com.codename1.backend.security.rememberme.PersistentTokenBasedRememberMeServices;
import com.codename1.backend.security.rememberme.PersistentTokenRepository;
import com.codename1.backend.security.rememberme.RememberMeServices;

/// Remember-me: a cookie that signs a returning user in.
///
/// ```java
/// http.formLogin(Customizer.withDefaults())
///     .rememberMe(remember -> remember
///             .tokenRepository(new JdbcTokenRepository(dataSource))
///             .tokenValiditySeconds(30 * 24 * 3600));
/// ```
///
/// A user who ticks `remember-me` on the login form is given the cookie, and
/// is signed in by it on a later visit without a session. They are then
/// authenticated but not *fully*: `fullyAuthenticated()` in the authorization
/// rules, and `isFullyAuthenticated()` in a `@PreAuthorize`, refuse them until
/// they sign in again. Signing out deletes the cookie and forgets the user in
/// every browser.
///
/// On a chain with a second factor -- [HttpSecurity#mfa] -- the cookie of a
/// user who has one signs them in only if it was issued by a sign-in that
/// passed it. One issued for a password alone, before the user enrolled, is
/// withdrawn when it is presented, and the user signs in again; see
/// [MfaConfigurer].
///
/// Tokens are kept by a [PersistentTokenRepository]: the one given here, the
/// application's bean of that type, or one in memory -- which forgets everyone
/// when the server restarts, and is no use to a deployment of several
/// processes. Users are found through the chain's [UserDetailsService].
///
/// See [PersistentTokenBasedRememberMeServices] for the cookie itself.
public final class RememberMeConfigurer extends SecurityConfigurer {
    private RememberMeServices services;
    private PersistentTokenRepository tokenRepository;
    private UserDetailsService userDetailsService;
    private String key = "cn1-remember-me";
    private String cookieName = PersistentTokenBasedRememberMeServices.DEFAULT_COOKIE_NAME;
    private String parameter = PersistentTokenBasedRememberMeServices.DEFAULT_PARAMETER;
    private int tokenValiditySeconds = PersistentTokenBasedRememberMeServices.TWO_WEEKS_S;
    private boolean alwaysRemember;
    private Boolean useSecureCookie;
    private String sameSite = "Lax";
    private RememberMeServices resolved;

    RememberMeConfigurer() {
    }

    /// Services of the application's own, in place of everything else here.
    public RememberMeConfigurer rememberMeServices(RememberMeServices rememberMeServices) {
        this.services = rememberMeServices;
        return this;
    }

    /// Where tokens are kept.
    public RememberMeConfigurer tokenRepository(PersistentTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
        return this;
    }

    /// Where a remembered user is looked up, in place of the chain's users.
    public RememberMeConfigurer userDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
        return this;
    }

    /// What identifies the tokens of this chain.
    public RememberMeConfigurer key(String key) {
        this.key = key;
        return this;
    }

    /// The cookie's name; `remember-me` unless set.
    public RememberMeConfigurer rememberMeCookieName(String rememberMeCookieName) {
        this.cookieName = rememberMeCookieName;
        return this;
    }

    /// The form field that asks to be remembered; `remember-me` unless set.
    public RememberMeConfigurer rememberMeParameter(String rememberMeParameter) {
        this.parameter = rememberMeParameter;
        return this;
    }

    /// How long an unused cookie stays good; two weeks unless set.
    public RememberMeConfigurer tokenValiditySeconds(int tokenValiditySeconds) {
        this.tokenValiditySeconds = tokenValiditySeconds;
        return this;
    }

    /// Remembers every user who signs in, whether or not they asked.
    public RememberMeConfigurer alwaysRemember(boolean alwaysRemember) {
        this.alwaysRemember = alwaysRemember;
        return this;
    }

    /// Whether the cookie is `Secure`; when the request was, unless set.
    public RememberMeConfigurer useSecureCookie(boolean useSecureCookie) {
        this.useSecureCookie = Boolean.valueOf(useSecureCookie);
        return this;
    }

    /// The cookie's `SameSite`: `Lax` unless set; null for none.
    public RememberMeConfigurer sameSite(String sameSite) {
        this.sameSite = sameSite;
        return this;
    }

    /// The services of this chain: what a sign-in the application completes
    /// itself calls `loginSuccess` on. Available once the chain is being built.
    public RememberMeServices getRememberMeServices() {
        return resolve(getBuilder());
    }

    private RememberMeServices resolve(HttpSecurity http) {
        if (resolved != null) {
            return resolved;
        }
        if (services != null) {
            resolved = services;
            return resolved;
        }
        RememberMeServices bean = http.getSharedObject(RememberMeServices.class);
        if (bean != null) {
            resolved = bean;
            return resolved;
        }
        UserDetailsService users = userDetailsService != null ? userDetailsService
                : PasswordAuthentication.users(http);
        if (users == null) {
            throw new IllegalStateException("rememberMe() needs to look a remembered user up, "
                    + "and this application has no users. Declare a UserDetailsService bean, or "
                    + "call userDetailsService(...) on the rememberMe() configurer.");
        }
        PersistentTokenRepository tokens = tokenRepository != null ? tokenRepository
                : http.getSharedObject(PersistentTokenRepository.class);
        if (tokens == null) {
            tokens = new InMemoryTokenRepositoryImpl();
        }
        PersistentTokenBasedRememberMeServices made = new PersistentTokenBasedRememberMeServices(
                key, users, tokens);
        made.setCookieName(cookieName);
        made.setParameter(parameter);
        made.setTokenValiditySeconds(tokenValiditySeconds);
        made.setAlwaysRemember(alwaysRemember);
        made.setUseSecureCookie(useSecureCookie);
        made.setSameSite(sameSite);
        resolved = made;
        return resolved;
    }

    @Override
    public void init(HttpSecurity http) {
        RememberMeServices remember = resolve(http);
        http.setSharedObject(RememberMeServices.class, remember);
        if (remember instanceof PersistentTokenBasedRememberMeServices) {
            http.rememberMeParameter(((PersistentTokenBasedRememberMeServices) remember)
                    .getParameter());
        }
        http.addSignInListener(new Remembering(remember));
        LogoutConfigurer logout = http.getConfigurer(LogoutConfigurer.class);
        if (logout != null && remember instanceof LogoutHandler) {
            logout.addLogoutHandler((LogoutHandler) remember);
        }
    }

    /// Tells the chain's remember-me services of each sign-in.
    private static final class Remembering implements SessionSignIn.Listener {
        private final RememberMeServices remember;

        Remembering(RememberMeServices remember) {
            this.remember = remember;
        }

        @Override
        public boolean requested(HttpServer.Request request) {
            return remember instanceof PersistentTokenBasedRememberMeServices
                    && ((PersistentTokenBasedRememberMeServices) remember)
                            .rememberMeRequested(request);
        }

        @Override
        public void success(HttpServer.Request request, Authentication authentication,
                            boolean requested, boolean secondFactor) {
            SecurityExchange exchange = SecurityExchange.of(request);
            if (exchange != null) {
                if (requested) {
                    // The request that completes a sign-in in two steps is not
                    // the one that carried the checkbox.
                    exchange.setAttribute(RememberMeServices.REQUESTED_ATTRIBUTE, Boolean.TRUE);
                }
                // Always written, true or not: a sign-in by password alone
                // must not inherit what an earlier one on this exchange said.
                exchange.setAttribute(RememberMeServices.SECOND_FACTOR_ATTRIBUTE,
                        secondFactor ? Boolean.TRUE : null);
            }
            remember.loginSuccess(request, authentication);
        }

        @Override
        public void failure(HttpServer.Request request) {
            remember.loginFail(request);
        }
    }

    @Override
    public void configure(HttpSecurity http) {
        http.addFilter(new RememberMeAuthenticationFilter(resolve(http),
                http.resolveSecurityContextRepository(), http.sessionAuthentication(),
                http.secondFactorPolicy()),
                HttpSecurity.ORDER_REMEMBER_ME);
    }
}
