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
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.oauth2.client.AuthorizationRequestRepository;
import com.codename1.backend.security.oauth2.client.ClientRegistration;
import com.codename1.backend.security.oauth2.client.ClientRegistrationRepository;
import com.codename1.backend.security.oauth2.client.ClientRegistrations;
import com.codename1.backend.security.oauth2.client.CookieOAuth2AuthorizationRequestRepository;
import com.codename1.backend.security.oauth2.client.DefaultAuthorizationCodeTokenResponseClient;
import com.codename1.backend.security.oauth2.client.DefaultOAuth2AuthorizationRequestResolver;
import com.codename1.backend.security.oauth2.client.DefaultOAuth2UserService;
import com.codename1.backend.security.oauth2.client.HttpSessionOAuth2AuthorizationRequestRepository;
import com.codename1.backend.security.oauth2.client.InMemoryClientRegistrationRepository;
import com.codename1.backend.security.oauth2.client.OAuth2AccessTokenResponseClient;
import com.codename1.backend.security.oauth2.client.OAuth2AuthorizationRequestResolver;
import com.codename1.backend.security.oauth2.client.OAuth2User;
import com.codename1.backend.security.oauth2.client.OAuth2UserRequest;
import com.codename1.backend.security.oauth2.client.OAuth2UserService;
import com.codename1.backend.security.oauth2.client.OidcIdTokenDecoderFactory;
import com.codename1.backend.security.oauth2.client.OidcUser;
import com.codename1.backend.security.oauth2.client.OidcUserRequest;
import com.codename1.backend.security.oauth2.client.OidcUserService;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Sign-in through another identity provider, with OAuth2 or OpenID Connect.
///
/// ```java
/// http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
///     .oauth2Login(Customizer.withDefaults());
/// ```
///
/// with, in `application.properties`,
///
/// ```
/// cn1.security.oauth2.client.registration.google.client-id=...
/// cn1.security.oauth2.client.registration.google.client-secret=...
/// ```
///
/// The providers are the application's
/// [ClientRegistrationRepository] bean, or the registrations the configuration
/// declares; see [ClientRegistrations] and
/// [com.codename1.backend.security.oauth2.client.CommonOAuth2Provider].
///
/// A sign-in starts at `GET /oauth2/authorization/{registrationId}`, which
/// sends the browser to the provider, and ends at
/// `/login/oauth2/code/{registrationId}`, where the provider sends it back.
/// Every request carries a `state`, a PKCE challenge and, with OpenID Connect,
/// a `nonce`; the answer is taken only when it repeats the `state` this
/// browser was given, the code is exchanged with the PKCE verifier, and an ID
/// token is accepted only as
/// [OidcIdTokenDecoderFactory] describes and with that `nonce` in it.
///
/// Who the provider's user is here is the [OAuth2UserService]'s to say; see
/// [#userService] and [#oidcUserService], and
/// [com.codename1.backend.security.oauth2.client.LinkingOAuth2UserService] for
/// tying them to the application's own users. The sign-in then ends the way
/// every sign-in to a session does -- see [SessionSignIn] -- so a second factor
/// and remember-me apply to it.
///
/// A signed-in user goes back to the page that asked for the sign-in, or to
/// [#defaultSuccessUrl]: nothing the provider or the request says chooses the
/// address. A refused one goes to `/login?error`, and is told nothing of why;
/// the reason, as an OAuth2 error code, is the
/// [AuthenticationFailureHandler]'s to read from the exception.
///
/// A chain that serves no login page of its own serves a plain one at
/// `GET /login` with a link per provider, and sends a request that must sign
/// in straight to the provider when there is only one.
public final class OAuth2LoginConfigurer extends SecurityConfigurer {
    private ClientRegistrationRepository registrations;
    private OAuth2AuthorizationRequestResolver resolver;
    private AuthorizationRequestRepository requestRepository;
    private OAuth2AccessTokenResponseClient tokenClient;
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> userService;
    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;
    private OidcIdTokenDecoderFactory decoders;
    private String loginPage;
    private String authorizationBaseUri =
            DefaultOAuth2AuthorizationRequestResolver.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;
    private String callbackBaseUri = "/login/oauth2/code";
    private String defaultSuccessUrl = "/";
    private boolean alwaysUseDefaultSuccessUrl;
    private String failureUrl;
    private AuthenticationSuccessHandler successHandler;
    private AuthenticationFailureHandler failureHandler;
    private boolean permitAll;
    private final EntryPoint entryPoint = new EntryPoint();

    OAuth2LoginConfigurer() {
    }

    /// The providers, in place of the application's bean and the configuration.
    public OAuth2LoginConfigurer clientRegistrationRepository(
            ClientRegistrationRepository clientRegistrationRepository) {
        this.registrations = clientRegistrationRepository;
        return this;
    }

    /// The application's own login page: a path it serves, with a link to
    /// `/oauth2/authorization/{registrationId}` for each provider.
    public OAuth2LoginConfigurer loginPage(String loginPage) {
        this.loginPage = Responses.path(loginPage, "loginPage");
        getBuilder().loginPage(this.loginPage);
        return this;
    }

    /// What decides that a request starts a sign-in and what is asked of the
    /// provider; see [DefaultOAuth2AuthorizationRequestResolver].
    public OAuth2LoginConfigurer authorizationRequestResolver(
            OAuth2AuthorizationRequestResolver authorizationRequestResolver) {
        this.resolver = authorizationRequestResolver;
        return this;
    }

    /// The path a sign-in starts at, before the registration's id;
    /// `/oauth2/authorization` unless set.
    public OAuth2LoginConfigurer authorizationEndpointBaseUri(String baseUri) {
        this.authorizationBaseUri = Responses.path(baseUri, "authorizationEndpointBaseUri");
        return this;
    }

    /// The path a provider answers at, before the registration's id;
    /// `/login/oauth2/code` unless set. A registration's redirect address has
    /// to agree with it.
    public OAuth2LoginConfigurer redirectionEndpointBaseUri(String baseUri) {
        this.callbackBaseUri = Responses.path(baseUri, "redirectionEndpointBaseUri");
        return this;
    }

    /// Where a request waits while the user is at the provider, for every
    /// provider. Unless set, the session -- and a signed cookie for a provider
    /// that answers with a posted form; see
    /// [CookieOAuth2AuthorizationRequestRepository].
    public OAuth2LoginConfigurer authorizationRequestRepository(
            AuthorizationRequestRepository authorizationRequestRepository) {
        this.requestRepository = authorizationRequestRepository;
        return this;
    }

    /// What exchanges the code for tokens.
    public OAuth2LoginConfigurer accessTokenResponseClient(
            OAuth2AccessTokenResponseClient accessTokenResponseClient) {
        this.tokenClient = accessTokenResponseClient;
        return this;
    }

    /// What makes the user of a provider without OpenID Connect.
    public OAuth2LoginConfigurer userService(
            OAuth2UserService<OAuth2UserRequest, OAuth2User> userService) {
        this.userService = userService;
        return this;
    }

    /// What makes the user of an OpenID Connect provider.
    public OAuth2LoginConfigurer oidcUserService(
            OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService) {
        this.oidcUserService = oidcUserService;
        return this;
    }

    /// What verifies ID tokens.
    public OAuth2LoginConfigurer idTokenDecoderFactory(OidcIdTokenDecoderFactory factory) {
        this.decoders = factory;
        return this;
    }

    /// Where a user goes after signing in when no page asked for the sign-in;
    /// `/` unless set.
    public OAuth2LoginConfigurer defaultSuccessUrl(String defaultSuccessUrl) {
        return defaultSuccessUrl(defaultSuccessUrl, false);
    }

    /// @param alwaysUse go there even when a page asked for the sign-in
    public OAuth2LoginConfigurer defaultSuccessUrl(String defaultSuccessUrl, boolean alwaysUse) {
        this.defaultSuccessUrl = Responses.path(defaultSuccessUrl, "defaultSuccessUrl");
        this.alwaysUseDefaultSuccessUrl = alwaysUse;
        return this;
    }

    public OAuth2LoginConfigurer successHandler(AuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
        return this;
    }

    /// Where a refused sign-in goes; the login page with `?error` unless set.
    public OAuth2LoginConfigurer failureUrl(String failureUrl) {
        this.failureUrl = Responses.path(failureUrl, "failureUrl");
        return this;
    }

    /// What answers a refused sign-in. The exception it is given is an
    /// [com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException]
    /// whose error says why.
    public OAuth2LoginConfigurer failureHandler(AuthenticationFailureHandler failureHandler) {
        this.failureHandler = failureHandler;
        return this;
    }

    /// Lets everyone reach the login page and the failure page, whatever the
    /// authorization rules say. Needed with a `loginPage` of the application's.
    public OAuth2LoginConfigurer permitAll() {
        this.permitAll = true;
        return this;
    }

    private String page(HttpSecurity http) {
        return loginPage != null ? loginPage : http.loginPage();
    }

    private String failure(HttpSecurity http) {
        return failureUrl != null ? failureUrl : page(http) + "?error";
    }

    private ClientRegistrationRepository registrations(HttpSecurity http) {
        if (registrations == null) {
            registrations = http.getSharedObject(ClientRegistrationRepository.class);
        }
        if (registrations == null) {
            List<ClientRegistration> declared;
            try {
                declared = ClientRegistrations.fromConfig(http.getConfig());
            } catch (IOException err) {
                throw new IllegalStateException("The client registrations could not be read "
                        + "from the configuration: " + err.getMessage(), err);
            }
            if (declared.isEmpty()) {
                throw new IllegalStateException("oauth2Login() needs to be told which providers "
                        + "users sign in through: declare a ClientRegistrationRepository bean, "
                        + "or set " + ClientRegistrations.REGISTRATION + "<id>.client-id");
            }
            registrations = new InMemoryClientRegistrationRepository(declared);
        }
        return registrations;
    }

    @Override
    public void init(HttpSecurity http) {
        http.redirectsToSignIn();
        // A later request sees the provider's user, not a name alone.
        http.authenticationCodec(new OAuth2AuthenticationCodec());
        ClientRegistrationRepository repository = registrations(http);
        boolean formPost = false;
        if (repository instanceof InMemoryClientRegistrationRepository) {
            for (ClientRegistration registration
                    : ((InMemoryClientRegistrationRepository) repository).getRegistrations()) {
                http.loginLink(authorizationBaseUri + "/" + registration.getRegistrationId(),
                        registration.getClientName());
                formPost |= ClientRegistration.FORM_POST.equals(registration.getResponseMode());
            }
        } else {
            // Not a list that can be read here: any of them might.
            formPost = true;
        }
        ExceptionHandlingConfigurer handling = http.getConfigurer(ExceptionHandlingConfigurer.class);
        if (handling != null) {
            handling.defaultAuthenticationEntryPointFor(entryPoint,
                    RequestMatchers.not(RequestMatchers.header("X-Requested-With", "XMLHttpRequest")));
        }
        if (formPost) {
            // A provider's posted answer comes from the provider's page: it can
            // carry no CSRF token of ours. What it must carry instead is the
            // state this browser was given, in a cookie only we can have signed.
            CsrfConfigurer csrf = http.getConfigurer(CsrfConfigurer.class);
            if (csrf != null) {
                csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("POST",
                        callbackBaseUri + "/*"));
            }
        }
        if (permitAll) {
            http.permit(AntPathRequestMatcher.antMatcher("GET", page(http)));
            http.permit(AntPathRequestMatcher.antMatcher("GET",
                    Responses.pathOnly(failure(http))));
        }
    }

    @Override
    public void configure(HttpSecurity http) {
        ClientRegistrationRepository repository = registrations(http);
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
        AuthorizationRequestRepository session = requestRepository != null ? requestRepository
                : new HttpSessionOAuth2AuthorizationRequestRepository();
        AuthorizationRequestRepository posted = requestRepository != null ? requestRepository
                : new CookieOAuth2AuthorizationRequestRepository(cookieSecret(http),
                        callbackBaseUri);
        OAuth2AuthorizationRequestResolver starts = resolver != null ? resolver
                : new DefaultOAuth2AuthorizationRequestResolver(repository, authorizationBaseUri);
        http.addFilter(new OAuth2AuthorizationRequestRedirectFilter(starts, session, posted,
                failed), HttpSecurity.ORDER_OAUTH2_AUTHORIZATION_REQUEST);
        http.addFilter(new OAuth2LoginAuthenticationFilter(callbackBaseUri + "/", repository,
                session, posted,
                tokenClient != null ? tokenClient : new DefaultAuthorizationCodeTokenResponseClient(),
                userService != null ? userService : new DefaultOAuth2UserService(),
                oidcUserService != null ? oidcUserService : new OidcUserService(),
                decoders != null ? decoders : new OidcIdTokenDecoderFactory(),
                success, failed, http.signIn()), HttpSecurity.ORDER_OAUTH2_LOGIN);
        boolean ownPage = loginPage == null && !http.loginPageServed();
        List<String[]> links = new ArrayList<String[]>(http.loginLinks());
        if (loginPage != null || http.loginPageServed() || links.size() != 1) {
            entryPoint.target = page(http);
        } else {
            // One provider and no other way in: the page would be one link.
            entryPoint.target = links.get(0)[0];
        }
        if (ownPage) {
            http.addFilter(new LoginLinksPage(http.loginPage(), links),
                    HttpSecurity.ORDER_LOGIN_PAGE);
        }
    }

    private static byte[] cookieSecret(HttpSecurity http) {
        try {
            String configured = http.getConfig().get(
                    CookieOAuth2AuthorizationRequestRepository.SECRET);
            if (configured != null) {
                byte[] secret = OAuth2Parameters.utf8(configured);
                if (secret.length < 32) {
                    throw new IllegalStateException(
                            CookieOAuth2AuthorizationRequestRepository.SECRET
                            + " must be at least 32 characters");
                }
                return secret;
            }
            return Crypto.randomBytes(32);
        } catch (IOException err) {
            throw new IllegalStateException("No key for the authorization request cookie: "
                    + err.getMessage(), err);
        }
    }

    /// Sends a request that must sign in to the login page, or to the one
    /// provider. Where to is known once the whole chain has been declared.
    private static final class EntryPoint implements AuthenticationEntryPoint {
        private String target = "/login";

        @Override
        public HttpServer.Response commence(HttpServer.Request request,
                                            AuthenticationException authException) {
            return Responses.redirect(target);
        }
    }

    /// The login page of a chain whose only ways in are other providers: a
    /// link to each.
    private static final class LoginLinksPage implements SecurityFilter {
        private final String loginPage;
        private final List<String[]> links;

        LoginLinksPage(String loginPage, List<String[]> links) {
            this.loginPage = loginPage;
            this.links = links;
        }

        @Override
        public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
                throws Exception {
            if (!"GET".equals(request.getMethod())
                    || !loginPage.equals(SecurityExchange.path(request))) {
                return chain.doFilter(request);
            }
            StringBuilder page = new StringBuilder(512);
            page.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n")
                    .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                    .append("<title>Please sign in</title>\n</head>\n<body>\n<h2>Please sign in</h2>\n");
            if (request.queryParam("error") != null) {
                page.append("<p role=\"alert\">The sign-in did not complete</p>\n");
            }
            if (request.queryParam("logout") != null) {
                page.append("<p role=\"status\">You have been signed out</p>\n");
            }
            for (String[] link : links) {
                page.append("<p><a href=\"").append(Responses.escape(link[0]))
                    .append("\">Sign in with ").append(Responses.escape(link[1]))
                    .append("</a></p>\n");
            }
            page.append("</body>\n</html>\n");
            return Responses.html(200, page.toString());
        }
    }
}
