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
package com.codename1.appkit.server.security;

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.AntPathRequestMatcher;
import com.codename1.backend.security.DisabledException;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.HttpStatusReturningLogoutSuccessHandler;
import com.codename1.backend.security.LogoutFilter;
import com.codename1.backend.security.SecurityExchange;
import com.codename1.backend.security.SecurityFilter;
import com.codename1.backend.security.SecurityFilterChain;
import com.codename1.backend.security.SessionCreationPolicy;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoderFactories;
import com.codename1.backend.security.oauth2.core.AuthorizationGrantType;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.JwtAudienceValidator;
import com.codename1.backend.security.oauth2.jwt.JwtClaimValidator;
import com.codename1.backend.security.oauth2.jwt.JwtIssuerValidator;
import com.codename1.backend.security.oauth2.jwt.JwtValidators;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerKeys;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerSettings;
import com.codename1.backend.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.JdbcRegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClient;
import com.codename1.backend.security.oauth2.server.authorization.RegisteredClientRepository;
import com.codename1.backend.security.oauth2.server.resource.JwtAuthenticationConverter;
import com.codename1.backend.security.ratelimit.InMemoryRateLimiter;
import com.codename1.backend.security.ratelimit.RateLimitKeys;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/// The sign-in half of an app's server: accounts, the app as an OAuth client,
/// the authorization server, and how the API reads the token it issues.
///
/// The server is two things at once, with a filter chain for each:
///
/// - **An authorization server.** The app signs a user in here -- e-mail and
///   password, then the authorization-code flow with PKCE -- and gets a
///   short-lived access token and a refresh token. Nothing but this server ever
///   sees the password. [#signIn] builds its chain.
/// - **An API.** Everything under `/api` takes that access token as a bearer
///   token, keeps no session, and decides from the account's roles what the
///   caller may do. The application writes that chain, since its routes and
///   roles are its own, from [#tokenDecoder] and [#accountRoles].
///
/// Nothing here is a bean by itself. The application's `@Configuration` returns
/// these from its own `@Bean` methods, so it names its client and audience in
/// one place and replaces any piece by writing that method differently.
public final class AccountSecurity {
    /// Where the authorization code is sent back to. The app makes the requests
    /// of the flow itself and reads the code out of the redirect, so this is
    /// never opened; it is the loopback address because that is the one address
    /// nobody else can register a listener on. An app that signs in through the
    /// system browser adds its own address scheme here with `redirectUri`.
    public static final String LOOPBACK_REDIRECT = "http://127.0.0.1/callback";
    /// Where the code is sent back to when the app runs in a browser: a path on
    /// this server, which answers with the code it was given. See
    /// [#withWebRedirect(RegisteredClient, String, String)].
    public static final String WEB_REDIRECT_PATH = "/signin/code";
    /// The key of the issuer setting: the address tokens name as their issuer.
    /// Empty on a development profile, where the server answers on whatever
    /// address it was reached by.
    public static final String ISSUER_KEY = "cn1.security.authorizationserver.issuer";

    private AccountSecurity() {
    }

    /// How passwords are stored: the delegating encoder, which records the
    /// scheme with each hash so a stronger one can be adopted later.
    public static PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /// The accounts, in the framework's own user tables.
    public static JdbcUserDetailsManager users(DataSource db) {
        return new JdbcUserDetailsManager(db);
    }

    /// The app as the authorization server's one client, registered on first
    /// start.
    ///
    /// @param id the row id of the registration
    /// @param clientId the id the app signs in as. A public client: it holds no
    ///     secret, since anything shipped inside an app can be read out of it,
    ///     and proves itself with PKCE instead.
    /// @param clientName what the sign-in pages call the app
    /// @param issuer the [#ISSUER_KEY] setting
    public static RegisteredClientRepository clients(DataSource db, final String id,
            final String clientId, String clientName, final String issuer) throws IOException {
        final JdbcRegisteredClientRepository clients = new JdbcRegisteredClientRepository(db);
        if (clients.findByClientId(clientId) == null) {
            clients.save(RegisteredClient.withId(id).clientId(clientId)
                    .clientName(clientName)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri(LOOPBACK_REDIRECT)
                    .scope("openid").scope("profile")
                    .build());
        }
        // The app in a browser is the same client with one more address: this
        // server's own. It is worked out and not stored, because on a
        // development profile the server has no one address to store -- and a
        // row written by an earlier version would not have it either.
        return new RegisteredClientRepository() {
            @Override
            public void save(RegisteredClient client) {
                clients.save(client);
            }

            @Override
            public RegisteredClient findByClientId(String wanted) {
                return withWebRedirect(clients.findByClientId(wanted), clientId, issuer);
            }

            @Override
            public RegisteredClient findById(String wanted) {
                return withWebRedirect(clients.findById(wanted), clientId, issuer);
            }
        };
    }

    /// The app's registration with [#WEB_REDIRECT_PATH] on this server added
    /// to the addresses a code may be sent back to.
    ///
    /// A browser will not show a page the answer to a request that was
    /// redirected to another origin, so the app running in one cannot read its
    /// code off a redirect to the loopback address the way the installed app
    /// does. It asks for the code to be sent to this server instead, which
    /// hands it straight back from a page the application serves there.
    ///
    /// "This server" is the configured issuer where there is one, and is then
    /// the only web address accepted. With none -- a development profile -- it
    /// is the address the request arrived at, the same rule
    /// [#issuedAtThisAddress] holds a token to: a code is only ever sent back
    /// to the server the browser was already talking to.
    public static RegisteredClient withWebRedirect(RegisteredClient client, String clientId,
            String issuer) {
        if (client == null || !clientId.equals(client.getClientId())) {
            return client;
        }
        String here = here(issuer);
        if (here == null) {
            return client;
        }
        RegisteredClient.Builder copy = RegisteredClient.withId(client.getId())
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .clientSettings(client.getClientSettings())
                .tokenSettings(client.getTokenSettings())
                .redirectUri(here + WEB_REDIRECT_PATH);
        for (ClientAuthenticationMethod method : client.getClientAuthenticationMethods()) {
            copy.clientAuthenticationMethod(method);
        }
        for (AuthorizationGrantType grant : client.getAuthorizationGrantTypes()) {
            copy.authorizationGrantType(grant);
        }
        for (String uri : client.getRedirectUris()) {
            copy.redirectUri(uri);
        }
        for (String scope : client.getScopes()) {
            copy.scope(scope);
        }
        for (String resource : client.getResources()) {
            copy.resource(resource);
        }
        return copy.build();
    }

    /// Refuses a request that changes something and was sent by a page of
    /// another site.
    ///
    /// A browser names the site a request came from in `Origin` on every
    /// request that is not a plain read, and a page cannot change or drop it.
    /// The installed app is not a browser and sends none, so a request without
    /// one is let through: there is no cookie jar of somebody else's for it to
    /// be riding on.
    public static SecurityFilter sameOrigin(final String issuer) {
        return (request, chain) -> {
            String method = request.getMethod();
            if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
                return chain.doFilter(request);
            }
            String origin = request.getHeader("Origin");
            if (origin == null || origin.length() == 0) {
                return chain.doFilter(request);
            }
            String here = here(issuer);
            if (here != null && sameOrigin(origin, here)) {
                return chain.doFilter(request);
            }
            return new HttpServer.Response(403, "application/json",
                    "{\"error\":\"cross_origin\"}".getBytes("UTF-8"))
                    .header("Cache-Control", "no-store");
        };
    }

    /// This server's address: the configured issuer, and with none -- a
    /// development profile -- the address the request in hand arrived at. Null
    /// outside a request that says.
    ///
    /// Configure the issuer in production. Without one, the trusted origin and
    /// the web redirect follow the request's `Host` header, which a proxy that
    /// forwards it unchanged lets the client choose.
    public static String here(String issuer) {
        if (issuer != null && issuer.length() > 0) {
            // "https://x/" and "https://x" are one server; paths are appended.
            return issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
        }
        SecurityExchange exchange = SecurityExchange.current();
        String host = exchange == null ? null : exchange.getRequest().getHeader("Host");
        if (host == null || host.length() == 0) {
            return null;
        }
        return (exchange.isSecure() ? "https://" : "http://") + host;
    }

    /// Whether `origin` is the origin of `address`: the same scheme, host and
    /// port, whatever path the address goes on to name. Scheme and host are
    /// compared without regard to case, and without folding it -- that would
    /// depend on the language the server runs in.
    public static boolean sameOrigin(String origin, String address) {
        int path = address.indexOf('/', address.indexOf("://") + 3);
        String root = path < 0 ? address : address.substring(0, path);
        return origin.equalsIgnoreCase(root);
    }

    /// The grants signed-in devices hold, in the authorization server's table.
    public static OAuth2AuthorizationService authorizations(DataSource db) {
        return new JdbcOAuth2AuthorizationService(db);
    }

    /// The keys tokens are signed with, from the server's configuration.
    public static JwkSource signingKeys(Config config) throws IOException {
        return AuthorizationServerKeys.load(config);
    }

    /// The authorization server's chain: the sign-in form, signing out, and
    /// the OAuth endpoints. Return it from an `@Order(1)` bean, ahead of the
    /// API's chain.
    ///
    /// @param issuer the [#ISSUER_KEY] setting
    /// @param signInsPerMinute how many sign-in attempts one address may make a
    ///     minute; ten is right for people, tests signing many in raise it
    /// @param audience whom the access tokens are for; the API accepts no token
    ///     issued for anything else
    public static SecurityFilterChain signIn(HttpSecurity http, final String issuer,
            int signInsPerMinute, String audience) throws IOException {
        http.securityMatcher("/oauth2/**", "/.well-known/**", "/userinfo", "/login", "/login/**",
                    "/logout")
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            // No CSRF token: the app posts the sign-in form itself, from its own
            // screen, and would have to fetch a token first to send one. What a
            // token is for still has to be done, because the app also runs in a
            // browser, where the session is a cookie the browser keeps and would
            // send along with a form another site submitted -- signing its
            // visitor in as somebody else. Two things stand in for it. The
            // session cookie is `SameSite=Lax`, the server's default, so it does
            // not travel with another site's request; and [#sameOrigin] below
            // refuses any request that changes something and says it came from
            // another site, which is the one a forged sign-in is. A server that
            // signs people in through a page it serves keeps CSRF on.
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(sameOrigin(issuer), LogoutFilter.class)
            // Signing out is a POST and nothing else -- with CSRF off the
            // default takes a GET too, which an image tag on any page can send
            // -- and is answered with a status, for the reason signing in is.
            .logout(logout -> logout
                    .logoutRequestMatcher(AntPathRequestMatcher.antMatcher("POST", "/logout"))
                    .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(204)))
            // And it is answered with a status, not a redirect to a page:
            // the app in a browser cannot see a redirect or the cookie, only
            // how the request ended, so that has to be the answer. The session
            // cookie is set either way; the installed app reads it, a browser
            // keeps it.
            .formLogin(form -> form
                    .successHandler((request, authentication) -> new HttpServer.Response(200,
                            "application/json", "{\"signedIn\":true}".getBytes("UTF-8"))
                            .header("Cache-Control", "no-store"))
                    .failureHandler((request, refused) -> new HttpServer.Response(401,
                            "application/json", "{\"signedIn\":false}".getBytes("UTF-8"))
                            .header("Cache-Control", "no-store")))
            // Guessing passwords is the attack on this chain. Ten tries a minute
            // from one address, unless the application's setting says otherwise:
            // the end-to-end tests are many people signing in from one address,
            // and their profile raises it.
            .rateLimit("/login", RateLimitKeys.clientAddress(),
                    new InMemoryRateLimiter(signInsPerMinute, 60))
            .authorizationServer(server -> server
                    .settings(AuthorizationServerSettings.builder()
                            .defaultAudience(audience).build()));
        return http.build();
    }

    /// Reads and checks an access token this server issued for `audience`.
    public static DefaultJwtDecoder tokenDecoder(JwkSource signingKeys, String issuer,
            String audience) {
        DefaultJwtDecoder decoder = DefaultJwtDecoder.withJwkSource(signingKeys).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtAudienceValidator(audience),
                issuer != null && issuer.length() > 0 ? new JwtIssuerValidator(issuer)
                        : new JwtClaimValidator("iss", AccountSecurity::issuedAtThisAddress)));
        return decoder;
    }

    /// Turns a token into the account's roles, read from the account on every
    /// request rather than from the token.
    public static JwtAuthenticationConverter accountRoles(final JdbcUserDetailsManager users) {
        // The token says who the caller is and nothing about what they may do.
        // The roles are read from the account on every request instead of being
        // copied into the token, for one reason: an admin who withdraws a role
        // or suspends an account means now, not when the token runs out. It
        // costs a lookup by primary key per request.
        JwtAuthenticationConverter roles = new JwtAuthenticationConverter();
        roles.setJwtGrantedAuthoritiesConverter(jwt -> {
            String name = jwt.getSubject();
            if (name == null || !users.userExists(name)) {
                throw new DisabledException("No such account");
            }
            UserDetails account = users.loadUserByUsername(name);
            if (!account.isEnabled() || !account.isAccountNonLocked()) {
                throw new DisabledException("This account is suspended");
            }
            Collection<GrantedAuthority> held = new ArrayList<GrantedAuthority>();
            held.addAll(account.getAuthorities());
            return held;
        });
        return roles;
    }

    /// Whether a token's issuer is the address this request came to.
    ///
    /// Used only when no issuer is configured, which the server allows only on
    /// a development profile: it then issues tokens in the name of whatever
    /// address each request arrived at, so that is the name to hold a token to.
    public static boolean issuedAtThisAddress(Object issuer) {
        SecurityExchange exchange = SecurityExchange.current();
        if (exchange == null || !(issuer instanceof String)) {
            return false;
        }
        String host = exchange.getRequest().getHeader("Host");
        return host != null && ((exchange.isSecure() ? "https://" : "http://") + host)
                .equals(issuer);
    }
}
