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

import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.SignedTokens;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.saml2.InMemoryRelyingPartyRegistrationRepository;
import com.codename1.backend.security.saml2.RelyingPartyRegistration;
import com.codename1.backend.security.saml2.RelyingPartyRegistrationRepository;
import com.codename1.backend.security.saml2.RelyingPartyRegistrations;
import com.codename1.backend.security.saml2.Saml2AuthenticatedPrincipal;
import com.codename1.backend.security.saml2.Saml2Authentication;
import com.codename1.backend.security.saml2.Saml2AuthenticationConverter;
import com.codename1.backend.security.saml2.Saml2ResponseValidator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Sign-in through a SAML 2.0 identity provider: Okta, Microsoft Entra ID,
/// ADFS, Ping, Google Workspace, Keycloak, Shibboleth.
///
/// ```java
/// http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
///     .saml2Login(Customizer.withDefaults());
/// ```
///
/// with, in `application.properties`,
///
/// ```
/// cn1.security.saml2.relyingparty.registration.okta.assertingparty.metadata-uri=https://...
/// ```
///
/// The identity providers are the application's
/// [RelyingPartyRegistrationRepository] bean or the registrations the
/// configuration declares; see [RelyingPartyRegistrations].
///
/// A sign-in starts at `GET /saml2/authenticate/{registrationId}`, which sends
/// the browser to the identity provider with an AuthnRequest, and ends at
/// `POST /login/saml2/sso/{registrationId}`, where the identity provider posts
/// its response. This server's metadata, for the identity provider's
/// administrator, is at `GET /saml2/service-provider-metadata/{registrationId}`.
/// How a response is judged is [Saml2ResponseValidator]'s; who the user is
/// here is the [#authenticationConverter]'s, which by default gives every
/// signed-in user `ROLE_USER`.
///
/// An assertion encrypted to a registration's decryption credential is
/// decrypted; see [com.codename1.backend.security.saml2.RelyingPartyRegistration.Builder#decryptionCredential].
/// Signing out ends the session here only, unless `http.saml2Logout(...)`
/// is declared as well; see [Saml2LogoutConfigurer].
///
/// The cookie that holds a pending request is signed with
/// `cn1.security.saml2.cookie-secret` (at least 32 characters); unset, a key
/// is made per process, and a response that reaches another server than the
/// one that sent the request is then taken as unsolicited -- set it when
/// several servers share the work.
public final class Saml2LoginConfigurer extends SecurityConfigurer {
    /// The setting that holds the key of the pending-request cookie.
    public static final String SECRET = "cn1.security.saml2.cookie-secret";

    private RelyingPartyRegistrationRepository registrations;
    private Saml2AuthenticationConverter converter;
    private String loginPage;
    private String authenticationRequestBaseUri = "/saml2/authenticate";
    private String loginProcessingBaseUri = "/login/saml2/sso";
    private String metadataBaseUri = "/saml2/service-provider-metadata";
    private String defaultSuccessUrl = "/";
    private boolean alwaysUseDefaultSuccessUrl;
    private String failureUrl;
    private AuthenticationSuccessHandler successHandler;
    private AuthenticationFailureHandler failureHandler;
    private boolean permitAll;
    private final EntryPoint entryPoint = new EntryPoint();

    Saml2LoginConfigurer() {
    }

    /// The identity providers, in place of the application's bean and the configuration.
    public Saml2LoginConfigurer relyingPartyRegistrationRepository(
            RelyingPartyRegistrationRepository repository) {
        this.registrations = repository;
        return this;
    }

    /// Who a signed-in user is here; see [Saml2AuthenticationConverter].
    public Saml2LoginConfigurer authenticationConverter(Saml2AuthenticationConverter converter) {
        this.converter = converter;
        return this;
    }

    /// The application's own login page, with a link to
    /// `/saml2/authenticate/{registrationId}` for each identity provider.
    public Saml2LoginConfigurer loginPage(String loginPage) {
        this.loginPage = Responses.path(loginPage, "loginPage");
        getBuilder().loginPage(this.loginPage);
        return this;
    }

    /// The path a sign-in starts at, before the registration's id.
    public Saml2LoginConfigurer authenticationRequestBaseUri(String baseUri) {
        this.authenticationRequestBaseUri = Responses.path(baseUri, "authenticationRequestBaseUri");
        return this;
    }

    /// The path responses are posted to, before the registration's id. The
    /// registrations' `acs.location` must agree with it.
    public Saml2LoginConfigurer loginProcessingBaseUri(String baseUri) {
        this.loginProcessingBaseUri = Responses.path(baseUri, "loginProcessingBaseUri");
        return this;
    }

    /// The path this server's metadata is served at, before the registration's id.
    public Saml2LoginConfigurer metadataBaseUri(String baseUri) {
        this.metadataBaseUri = Responses.path(baseUri, "metadataBaseUri");
        return this;
    }

    public Saml2LoginConfigurer defaultSuccessUrl(String defaultSuccessUrl) {
        return defaultSuccessUrl(defaultSuccessUrl, false);
    }

    /// @param alwaysUse go there even when a page asked for the sign-in
    public Saml2LoginConfigurer defaultSuccessUrl(String defaultSuccessUrl, boolean alwaysUse) {
        this.defaultSuccessUrl = Responses.path(defaultSuccessUrl, "defaultSuccessUrl");
        this.alwaysUseDefaultSuccessUrl = alwaysUse;
        return this;
    }

    public Saml2LoginConfigurer successHandler(AuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
        return this;
    }

    /// Where a refused sign-in goes; the login page with `?error` unless set.
    public Saml2LoginConfigurer failureUrl(String failureUrl) {
        this.failureUrl = Responses.path(failureUrl, "failureUrl");
        return this;
    }

    /// What answers a refused sign-in. The exception is a
    /// [com.codename1.backend.security.saml2.Saml2AuthenticationException]
    /// whose error code says why.
    public Saml2LoginConfigurer failureHandler(AuthenticationFailureHandler failureHandler) {
        this.failureHandler = failureHandler;
        return this;
    }

    /// Lets everyone reach the login page and the failure page.
    public Saml2LoginConfigurer permitAll() {
        this.permitAll = true;
        return this;
    }

    private String page(HttpSecurity http) {
        return loginPage != null ? loginPage : http.loginPage();
    }

    private String failure(HttpSecurity http) {
        return failureUrl != null ? failureUrl : page(http) + "?error";
    }

    RelyingPartyRegistrationRepository registrations(HttpSecurity http) {
        if (registrations == null) {
            registrations = http.getSharedObject(RelyingPartyRegistrationRepository.class);
        }
        if (registrations == null) {
            List<RelyingPartyRegistration> declared;
            try {
                declared = RelyingPartyRegistrations.fromConfig(http.getConfig());
            } catch (IOException err) {
                throw new IllegalStateException("The SAML registrations could not be read "
                        + "from the configuration: " + err.getMessage(), err);
            }
            if (declared.isEmpty()) {
                throw new IllegalStateException("saml2Login() needs to be told which identity "
                        + "providers users sign in through: declare a "
                        + "RelyingPartyRegistrationRepository bean, or set "
                        + RelyingPartyRegistrations.REGISTRATION
                        + "<id>.assertingparty.metadata-uri");
            }
            registrations = new InMemoryRelyingPartyRegistrationRepository(declared);
        }
        return registrations;
    }

    @Override
    public void init(HttpSecurity http) {
        http.redirectsToSignIn();
        http.authenticationCodec(new Saml2AuthenticationCodec());
        RelyingPartyRegistrationRepository repository = registrations(http);
        if (repository instanceof InMemoryRelyingPartyRegistrationRepository) {
            for (RelyingPartyRegistration r
                    : ((InMemoryRelyingPartyRegistrationRepository) repository).getRegistrations()) {
                http.loginLink(authenticationRequestBaseUri + "/" + r.getRegistrationId(),
                        r.getRegistrationId());
            }
        }
        ExceptionHandlingConfigurer handling = http.getConfigurer(ExceptionHandlingConfigurer.class);
        if (handling != null) {
            handling.defaultAuthenticationEntryPointFor(entryPoint,
                    RequestMatchers.not(RequestMatchers.header("X-Requested-With", "XMLHttpRequest")));
        }
        // The identity provider's page posts the response: it can carry no CSRF
        // token of ours. What binds it to this browser is the signed cookie
        // naming the request, and the signature on the response itself.
        CsrfConfigurer csrf = http.getConfigurer(CsrfConfigurer.class);
        if (csrf != null) {
            csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("POST",
                    loginProcessingBaseUri + "/*"));
        }
        // The metadata is public by design: the identity provider's
        // administrator, or the provider itself, fetches it unauthenticated.
        http.permit(AntPathRequestMatcher.antMatcher("GET", metadataBaseUri + "/*"));
        if (permitAll) {
            http.permit(AntPathRequestMatcher.antMatcher("GET", page(http)));
            http.permit(AntPathRequestMatcher.antMatcher("GET",
                    Responses.pathOnly(failure(http))));
        }
    }

    @Override
    public void configure(HttpSecurity http) {
        RelyingPartyRegistrationRepository repository = registrations(http);
        AuthenticationSuccessHandler success = successHandler;
        if (success == null) {
            SavedRequestAwareAuthenticationSuccessHandler saved =
                    new SavedRequestAwareAuthenticationSuccessHandler();
            saved.setDefaultTargetUrl(defaultSuccessUrl);
            saved.setAlwaysUseDefaultTargetUrl(alwaysUseDefaultSuccessUrl);
            saved.setRequestCache(http.resolveRequestCache());
            success = saved;
        }
        AuthenticationFailureHandler failed = failureHandler != null ? failureHandler
                : new SimpleUrlAuthenticationFailureHandler(failure(http));
        SignedTokens tokens = new SignedTokens(cookieSecret(http));
        http.addFilter(new Saml2WebSsoAuthenticationRequestFilter(
                authenticationRequestBaseUri + "/", metadataBaseUri + "/", repository, tokens,
                failed, http.saml2LogoutBase() != null),
                HttpSecurity.ORDER_SAML2_AUTHENTICATION_REQUEST);
        http.addFilter(new Saml2WebSsoAuthenticationFilter(loginProcessingBaseUri + "/",
                repository, tokens, new Saml2ResponseValidator(),
                converter != null ? converter : DEFAULT_CONVERTER, success, failed,
                http.signIn()), HttpSecurity.ORDER_SAML2_LOGIN);
        boolean ownPage = loginPage == null && !http.loginPageServed();
        List<String[]> links = new ArrayList<String[]>(http.loginLinks());
        if (loginPage != null || http.loginPageServed() || links.size() != 1) {
            entryPoint.target = page(http);
        } else {
            entryPoint.target = links.get(0)[0];
        }
        if (ownPage) {
            http.addLoginLinksPage();
        }
    }

    private static final Saml2AuthenticationConverter DEFAULT_CONVERTER =
            new Saml2AuthenticationConverter() {
                @Override
                public Saml2Authentication convert(Saml2AuthenticatedPrincipal principal) {
                    return new Saml2Authentication(principal, Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_USER")));
                }
            };

    static byte[] cookieSecret(HttpSecurity http) {
        try {
            String configured = http.getConfig().get(SECRET);
            if (configured != null) {
                byte[] secret = OAuth2Parameters.utf8(configured);
                if (secret.length < 32) {
                    throw new IllegalStateException(SECRET + " must be at least 32 characters");
                }
                return secret;
            }
            return Crypto.randomBytes(32);
        } catch (IOException err) {
            throw new IllegalStateException("No key for the SAML request cookie: "
                    + err.getMessage(), err);
        }
    }

    /// Sends a request that must sign in to the login page, or to the one
    /// identity provider.
    private static final class EntryPoint implements AuthenticationEntryPoint {
        private String target = "/login";

        @Override
        public com.codename1.backend.HttpServer.Response commence(
                com.codename1.backend.HttpServer.Request request,
                AuthenticationException authException) {
            return Responses.redirect(target);
        }
    }
}
