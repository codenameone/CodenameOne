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

import com.codename1.backend.security.crypto.SignedTokens;
import com.codename1.backend.security.saml2.RelyingPartyRegistrationRepository;

/// SAML 2.0 Single Logout, front channel, for the users `saml2Login` signs in:
///
/// ```java
/// http.saml2Login(Customizer.withDefaults())
///     .saml2Logout(Customizer.withDefaults());
/// ```
///
/// - **Signing out here** (the chain's ordinary sign-out, `POST /logout`)
///   ends the session, then sends the browser to the identity provider with a
///   signed `LogoutRequest`, so the user is signed out there and of the other
///   applications it signed them in to. The provider answers at
///   `/logout/saml2/slo/{registrationId}`, and the browser goes on to the
///   chain's sign-out page.
/// - **Signing out elsewhere**: the identity provider sends a signed
///   `LogoutRequest` to `/logout/saml2/slo/{registrationId}`; the session of
///   the user it names ends, and the browser is sent back with a signed
///   `LogoutResponse`.
///
/// A registration takes part when it has a signing credential and the
/// identity provider's logout address (from its metadata, or
/// `assertingparty.singlelogout.url`); for any other, signing out ends the
/// session here alone, as without this configurer. This server's metadata
/// lists its logout address once this is declared.
///
/// Prefer the HTTP-Redirect binding for the identity provider's logout
/// address. A logout the identity provider starts arrives as a cross-site
/// request, and only a top-level GET carries the `SameSite=Lax` session
/// cookie: a request that arrives by POST finds no session, is answered with
/// success, and the session here lives on to its expiry. That is how browsers
/// treat cookies, and Spring Security has the same limit. SOAP (back
/// channel) logout is not supported.
public final class Saml2LogoutConfigurer extends SecurityConfigurer {
    private String logoutBaseUri = "/logout/saml2/slo";

    Saml2LogoutConfigurer() {
    }

    /// The path logout messages arrive at, before the registration's id. The
    /// registrations' `singlelogout.url` must agree with it.
    public Saml2LogoutConfigurer logoutBaseUri(String baseUri) {
        this.logoutBaseUri = Responses.path(baseUri, "logoutBaseUri");
        return this;
    }

    @Override
    public void init(HttpSecurity http) {
        final Saml2LoginConfigurer login = http.getConfigurer(Saml2LoginConfigurer.class);
        if (login == null) {
            throw new IllegalStateException("saml2Logout() needs saml2Login(): it ends the "
                    + "sessions saml2Login() starts");
        }
        http.saml2LogoutBase(logoutBaseUri);
        // Both bindings: the identity provider redirects or posts its
        // messages, from its own origin, with no CSRF token of ours. What
        // admits one is its signature.
        CsrfConfigurer csrf = http.getConfigurer(CsrfConfigurer.class);
        if (csrf != null) {
            csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("POST",
                    logoutBaseUri + "/*"));
        }
        http.permit(AntPathRequestMatcher.antMatcher("GET", logoutBaseUri + "/*"));
        http.permit(AntPathRequestMatcher.antMatcher("POST", logoutBaseUri + "/*"));
    }

    @Override
    public void configure(HttpSecurity http) {
        Saml2LoginConfigurer login = http.getConfigurer(Saml2LoginConfigurer.class);
        RelyingPartyRegistrationRepository repository = login.registrations(http);
        SignedTokens tokens = new SignedTokens(Saml2LoginConfigurer.cookieSecret(http));
        Saml2LogoutFilter filter = new Saml2LogoutFilter(logoutBaseUri + "/", repository,
                tokens, http.logoutHooks(), http.loginPage() + "?logout");
        http.logoutHooks().wrappers.add(new RelyingPartyInitiated(filter));
        http.addFilter(filter, HttpSecurity.ORDER_SAML2_LOGOUT);
    }

    /// Hands the chain's own sign-out to the identity provider once it ends here.
    private static final class RelyingPartyInitiated implements LogoutHooks.Wrapper {
        private final Saml2LogoutFilter filter;

        RelyingPartyInitiated(Saml2LogoutFilter filter) {
            this.filter = filter;
        }

        @Override
        public LogoutSuccessHandler wrap(LogoutSuccessHandler next) {
            return filter.relyingPartyInitiated(next);
        }
    }
}
