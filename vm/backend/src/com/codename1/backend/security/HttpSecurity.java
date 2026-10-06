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

import com.codename1.backend.Config;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.ratelimit.RateLimitKeyResolver;
import com.codename1.backend.security.ratelimit.RateLimiter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Builds one [SecurityFilterChain]. A `@Bean` method that returns a chain
/// declares a parameter of this type and is handed a new one:
///
/// ```java
/// @Bean
/// SecurityFilterChain web(HttpSecurity http) {
///     http.authorizeHttpRequests(auth -> auth
///             .requestMatchers("/public/**").permitAll()
///             .anyRequest().authenticated())
///         .formLogin(Customizer.withDefaults())
///         .httpBasic(Customizer.withDefaults());
///     return http.build();
/// }
/// ```
///
/// Out of the box a chain guards every request, writes the security headers,
/// keeps who is signed in in the HTTP session, protects that session against
/// CSRF and gives a request nobody signed in for an anonymous authentication.
/// It has no way of signing in and no authorization rules until it is given
/// them.
///
/// Everything else is there only when the chain asks for it, and a server
/// carries the code of only what its chains ask for: [#formLogin] -- which
/// brings sign-out and the memory of where a request was going with it --
/// [#httpBasic], [#oauth2Login], [#oauth2ResourceServer],
/// [#authorizationServer], [#apiKey], [#rateLimit], [#rememberMe], [#mfa],
/// [#webAuthn], [#logout] and [#requestCache]. A server that only
/// verifies tokens has no login page, no password hashing and no user store in
/// it. This makes two departures from Spring Security. The first: a chain
/// without `formLogin` has no `POST /logout` until it calls [#logout].
///
/// The second: a chain whose session policy is
/// [SessionCreationPolicy#STATELESS] keeps nothing a forged request could
/// ride on, and has no CSRF filter unless [#csrf] asks for one. One that takes
/// HTTP Basic credentials from browsers should ask.
///
/// Users come from the application's beans: a
/// [UserDetailsService] and, if there is one, a
/// [com.codename1.backend.security.crypto.PasswordEncoder] and a
/// [com.codename1.backend.security.core.userdetails.UserDetailsPasswordService]
/// -- or [AuthenticationProvider] beans, or an [AuthenticationManager] bean. A
/// chain can also be told directly, with [#userDetailsService],
/// [#authenticationProvider] or [#authenticationManager].
public final class HttpSecurity {
    /// The name of the one user a server with no user store has, when
    /// [#USER_PASSWORD] is set; `user` unless set.
    public static final String USER_NAME = "cn1.security.user.name";
    /// That user's password, as it would be stored; one written without an
    /// `{id}` is taken as `{noop}`, which verifies on a development profile only.
    public static final String USER_PASSWORD = "cn1.security.user.password";
    /// That user's roles, separated by commas.
    public static final String USER_ROLES = "cn1.security.user.roles";
    /// The most passwords checked at one time; a sign-in beyond that is
    /// answered 503 at once. No bound unless set. See
    /// [DaoAuthenticationProvider#setMaxConcurrentPasswordChecks].
    public static final String PASSWORD_MAX_CONCURRENT = "cn1.security.password.maxConcurrent";

    private final Config config;
    private final Object[] beans;
    private final Map<Class<?>, Object> sharedObjects = new HashMap<Class<?>, Object>();
    private final Map<Class<?>, SecurityConfigurer> configurers =
            new LinkedHashMap<Class<?>, SecurityConfigurer>();
    /// The place of each of the layer's own filters, by class NAME. A name, and
    /// the numbers below, rather than the classes: naming a filter class here
    /// would put every sign-in mechanism into every server with a chain.
    private final Map<String, Integer> filterOrder = new HashMap<String, Integer>();
    private static final String PACKAGE = "com.codename1.backend.security.";
    static final int ORDER_RATE_LIMIT = 50;
    static final int ORDER_SECURITY_CONTEXT = 100;
    static final int ORDER_HEADERS = 200;
    static final int ORDER_CSRF = 400;
    static final int ORDER_LOGOUT = 500;
    static final int ORDER_OAUTH2_AUTHORIZATION_REQUEST = 600;
    static final int ORDER_AUTHORIZATION_SERVER = 700;
    static final int ORDER_OAUTH2_LOGIN = 900;
    static final int ORDER_WEBAUTHN_LOGIN = 950;
    static final int ORDER_FORM_LOGIN = 1000;
    static final int ORDER_SECOND_FACTOR = 1050;
    static final int ORDER_LOGIN_PAGE = 1100;
    static final int ORDER_API_KEY = 1200;
    static final int ORDER_BEARER_TOKEN = 1300;
    static final int ORDER_BASIC = 1500;
    static final int ORDER_REMEMBER_ME = 1700;
    static final int ORDER_ANONYMOUS = 2000;
    static final int ORDER_AUTHENTICATED_RATE_LIMIT = 2100;
    static final int ORDER_EXCEPTION_TRANSLATION = 2400;
    static final int ORDER_AUTHORIZATION_SERVER_USER = 2450;
    static final int ORDER_AUTHORIZATION = 2500;
    static final int ORDER_WEBAUTHN_REGISTRATION = 2600;
    /// {Integer order, SecurityFilter}, in the order they were added.
    private final List<Object[]> filters = new ArrayList<Object[]>();
    private final List<RequestMatcher> permitted = new ArrayList<RequestMatcher>();
    private final List<AuthenticationProvider> providers = new ArrayList<AuthenticationProvider>();
    private RequestMatcher requestMatcher = AnyRequestMatcher.INSTANCE;
    private AuthenticationManager authenticationManager;
    private AuthenticationManager explicitAuthenticationManager;
    private boolean authenticationManagerResolved;
    private UserDetailsService userDetailsService;
    private boolean built;
    private boolean redirectsToSignIn;
    private boolean csrfAsked;
    private String loginPage = "/login";
    private RequestCacheSource requestCacheSource;
    /// The parts a chain took out with disable(), which nothing puts back.
    private final List<Class<?>> disabled = new ArrayList<Class<?>>();

    /// Where a chain's [RequestCache] comes from, when it has one: the
    /// configurer, seen through a type that does not name it.
    interface RequestCacheSource {
        RequestCache resolve(SessionCreationPolicy policy, boolean redirects);
    }

    HttpSecurity(Config config, Object[] beans) {
        this.config = config;
        this.beans = beans == null ? new Object[0] : beans.clone();
        // The order the filters of a chain run in. The gaps are where the
        // filters of the sign-in mechanisms that are not here yet belong:
        // between CSRF and the form login for a redirect to another identity
        // provider, between HTTP Basic and the anonymous filter for a cookie.
        filterOrder.put(PACKAGE + "RateLimitFilter", Integer.valueOf(ORDER_RATE_LIMIT));
        filterOrder.put(PACKAGE + "SecurityContextHolderFilter",
                Integer.valueOf(ORDER_SECURITY_CONTEXT));
        filterOrder.put(PACKAGE + "HeaderWriterFilter", Integer.valueOf(ORDER_HEADERS));
        filterOrder.put(PACKAGE + "CsrfFilter", Integer.valueOf(ORDER_CSRF));
        filterOrder.put(PACKAGE + "LogoutFilter", Integer.valueOf(ORDER_LOGOUT));
        filterOrder.put(PACKAGE + "OAuth2AuthorizationRequestRedirectFilter",
                Integer.valueOf(ORDER_OAUTH2_AUTHORIZATION_REQUEST));
        filterOrder.put(PACKAGE + "OAuth2AuthorizationServerFilter",
                Integer.valueOf(ORDER_AUTHORIZATION_SERVER));
        filterOrder.put(PACKAGE + "OAuth2LoginAuthenticationFilter",
                Integer.valueOf(ORDER_OAUTH2_LOGIN));
        filterOrder.put(PACKAGE + "WebAuthnAuthenticationFilter",
                Integer.valueOf(ORDER_WEBAUTHN_LOGIN));
        filterOrder.put(PACKAGE + "UsernamePasswordAuthenticationFilter",
                Integer.valueOf(ORDER_FORM_LOGIN));
        filterOrder.put(PACKAGE + "SecondFactorAuthenticationFilter",
                Integer.valueOf(ORDER_SECOND_FACTOR));
        filterOrder.put(PACKAGE + "DefaultLoginPageGeneratingFilter",
                Integer.valueOf(ORDER_LOGIN_PAGE));
        filterOrder.put(PACKAGE + "ApiKeyAuthenticationFilter", Integer.valueOf(ORDER_API_KEY));
        filterOrder.put(PACKAGE + "BearerTokenAuthenticationFilter",
                Integer.valueOf(ORDER_BEARER_TOKEN));
        filterOrder.put(PACKAGE + "BasicAuthenticationFilter", Integer.valueOf(ORDER_BASIC));
        filterOrder.put(PACKAGE + "RememberMeAuthenticationFilter",
                Integer.valueOf(ORDER_REMEMBER_ME));
        filterOrder.put(PACKAGE + "AnonymousAuthenticationFilter",
                Integer.valueOf(ORDER_ANONYMOUS));
        filterOrder.put(PACKAGE + "ExceptionTranslationFilter",
                Integer.valueOf(ORDER_EXCEPTION_TRANSLATION));
        filterOrder.put(PACKAGE + "OAuth2AuthorizationEndpointFilter",
                Integer.valueOf(ORDER_AUTHORIZATION_SERVER_USER));
        filterOrder.put(PACKAGE + "AuthorizationFilter", Integer.valueOf(ORDER_AUTHORIZATION));
        // After the rules, as the one filter that is: registering a passkey is
        // something a signed-in user does, and the rules are what say who is.
        filterOrder.put(PACKAGE + "WebAuthnRegistrationFilter",
                Integer.valueOf(ORDER_WEBAUTHN_REGISTRATION));
        // What every chain has until it says otherwise.
        apply(new SecurityContextConfigurer());
        apply(new HeadersConfigurer());
        apply(new CsrfConfigurer());
        apply(new SessionManagementConfigurer());
        apply(new AnonymousConfigurer());
        apply(new ExceptionHandlingConfigurer());
    }

    // ---------------------------------------------------------------- the DSL

    /// Limits the chain to the requests whose path matches any of these Ant
    /// patterns; see [AntPathRequestMatcher]. A chain without one guards every
    /// request, and must then be the last in `@Order`.
    public HttpSecurity securityMatcher(String... patterns) {
        if (patterns == null || patterns.length == 0) {
            throw new IllegalArgumentException("At least one pattern is required");
        }
        RequestMatcher[] matchers = new RequestMatcher[patterns.length];
        for (int iter = 0 ; iter < patterns.length ; iter++) {
            matchers[iter] = new AntPathRequestMatcher(patterns[iter]);
        }
        return securityMatcher(matchers.length == 1 ? matchers[0] : RequestMatchers.anyOf(matchers));
    }

    /// Limits the chain to the requests `requestMatcher` matches.
    public HttpSecurity securityMatcher(RequestMatcher requestMatcher) {
        if (requestMatcher == null) {
            throw new IllegalArgumentException("requestMatcher cannot be null");
        }
        this.requestMatcher = requestMatcher;
        return this;
    }

    /// The authorization rules; see [AuthorizeHttpRequestsConfigurer].
    public HttpSecurity authorizeHttpRequests(Customizer<
            AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry> customizer) {
        AuthorizeHttpRequestsConfigurer configurer = getConfigurer(
                AuthorizeHttpRequestsConfigurer.class);
        if (configurer == null) {
            configurer = new AuthorizeHttpRequestsConfigurer();
            apply(configurer);
        }
        customizer.customize(configurer.getRegistry());
        return this;
    }

    /// Sign-in through an HTML form; see [FormLoginConfigurer]. Brings sign-out
    /// ([#logout]) and the memory of where a request was going
    /// ([#requestCache]) with it, unless the chain has turned those off.
    public HttpSecurity formLogin(Customizer<FormLoginConfigurer> customizer) {
        FormLoginConfigurer configurer = getConfigurer(FormLoginConfigurer.class);
        if (configurer == null) {
            configurer = new FormLoginConfigurer();
            apply(configurer);
            sessionMechanism();
        }
        customizer.customize(configurer);
        return this;
    }

    /// Sign-in with HTTP Basic credentials; see [HttpBasicConfigurer].
    public HttpSecurity httpBasic(Customizer<HttpBasicConfigurer> customizer) {
        HttpBasicConfigurer configurer = getConfigurer(HttpBasicConfigurer.class);
        if (configurer == null) {
            configurer = new HttpBasicConfigurer();
            apply(configurer);
        }
        customizer.customize(configurer);
        return this;
    }

    /// Sign-in with a bearer token that is a JWT; see
    /// [OAuth2ResourceServerConfigurer].
    public HttpSecurity oauth2ResourceServer(Customizer<OAuth2ResourceServerConfigurer> customizer) {
        OAuth2ResourceServerConfigurer configurer = getConfigurer(OAuth2ResourceServerConfigurer.class);
        if (configurer == null) {
            configurer = new OAuth2ResourceServerConfigurer();
            apply(configurer);
        }
        customizer.customize(configurer);
        return this;
    }

    /// Sign-in through another identity provider, with OAuth2 or OpenID
    /// Connect; see [OAuth2LoginConfigurer]. Brings sign-out and the memory of
    /// where a request was going with it, as [#formLogin] does.
    public HttpSecurity oauth2Login(Customizer<OAuth2LoginConfigurer> customizer) {
        OAuth2LoginConfigurer configurer = getConfigurer(OAuth2LoginConfigurer.class);
        if (configurer == null) {
            configurer = new OAuth2LoginConfigurer();
            apply(configurer);
            sessionMechanism();
        }
        customizer.customize(configurer);
        return this;
    }

    /// Makes this server an OAuth2 authorization server and OpenID Connect
    /// provider: the one that issues tokens; see
    /// [AuthorizationServerConfigurer]. How a user signs in to it is whatever
    /// else the chain declares.
    public HttpSecurity authorizationServer(Customizer<AuthorizationServerConfigurer> customizer) {
        AuthorizationServerConfigurer configurer = getConfigurer(
                AuthorizationServerConfigurer.class);
        if (configurer == null) {
            configurer = new AuthorizationServerConfigurer();
            apply(configurer);
        }
        customizer.customize(configurer);
        return this;
    }

    /// A cookie that signs a returning user in; see [RememberMeConfigurer].
    /// Brings sign-out with it, as [#formLogin] does.
    public HttpSecurity rememberMe(Customizer<RememberMeConfigurer> customizer) {
        RememberMeConfigurer configurer = getConfigurer(RememberMeConfigurer.class);
        if (configurer == null) {
            configurer = new RememberMeConfigurer();
            apply(configurer);
            sessionMechanism();
        }
        customizer.customize(configurer);
        return this;
    }

    /// A second factor at sign-in; see [MfaConfigurer]. Brings sign-out with
    /// it, as [#formLogin] does.
    public HttpSecurity mfa(Customizer<MfaConfigurer> customizer) {
        MfaConfigurer configurer = getConfigurer(MfaConfigurer.class);
        if (configurer == null) {
            configurer = new MfaConfigurer();
            apply(configurer);
            sessionMechanism();
        }
        customizer.customize(configurer);
        return this;
    }

    /// Passkeys: registering one for a user who is signed in, and signing in
    /// with one; see [WebAuthnConfigurer]. Brings sign-out with it, as
    /// [#formLogin] does.
    public HttpSecurity webAuthn(Customizer<WebAuthnConfigurer> customizer) {
        WebAuthnConfigurer configurer = getConfigurer(WebAuthnConfigurer.class);
        if (configurer == null) {
            configurer = new WebAuthnConfigurer();
            apply(configurer);
            sessionMechanism();
        }
        customizer.customize(configurer);
        return this;
    }

    /// Sign-in with an API key; see [ApiKeyConfigurer].
    public HttpSecurity apiKey(Customizer<ApiKeyConfigurer> customizer) {
        ApiKeyConfigurer configurer = getConfigurer(ApiKeyConfigurer.class);
        if (configurer == null) {
            configurer = new ApiKeyConfigurer();
            apply(configurer);
        }
        customizer.customize(configurer);
        return this;
    }

    /// Limits how often the requests `matcher` matches may be made under one
    /// key, and answers 429 with `Retry-After` beyond that.
    ///
    /// ```java
    /// http.rateLimit(AntPathRequestMatcher.antMatcher("/login"),
    ///         RateLimitKeys.clientAddress(), new InMemoryRateLimiter(5, 60));
    /// http.rateLimit("/api/**", RateLimitKeys.firstOf(RateLimitKeys.apiKeyId(),
    ///         RateLimitKeys.principal()), new InMemoryRateLimiter(600, 60));
    /// ```
    ///
    /// A limit keyed by something the request has from the start -- its
    /// client's address, its session -- is applied before anything else in the
    /// chain. One keyed by who signed in is applied once that is known, and does
    /// not apply to a request nobody signed in for. Rules are consulted in the
    /// order given, and a request counts against every rule that matches it up
    /// to the one that refuses it.
    ///
    /// Two rules given the same limiter share its counts for any key they have
    /// in common; give each its own unless that is what is meant.
    ///
    /// @param keyResolver which key a request counts under; see
    /// [com.codename1.backend.security.ratelimit.RateLimitKeys]
    /// @param limiter what counts; null for the application's one
    /// [RateLimiter] bean. [com.codename1.backend.security.ratelimit.InMemoryRateLimiter]
    /// counts in this process alone.
    public HttpSecurity rateLimit(RequestMatcher matcher, RateLimitKeyResolver keyResolver,
                                  RateLimiter limiter) {
        RateLimitConfigurer configurer = getConfigurer(RateLimitConfigurer.class);
        if (configurer == null) {
            configurer = new RateLimitConfigurer();
            apply(configurer);
        }
        configurer.add(matcher, keyResolver, limiter);
        return this;
    }

    /// [#rateLimit(RequestMatcher, RateLimitKeyResolver, RateLimiter)] for the
    /// requests whose path matches an Ant pattern.
    public HttpSecurity rateLimit(String pattern, RateLimitKeyResolver keyResolver,
                                  RateLimiter limiter) {
        return rateLimit(new AntPathRequestMatcher(pattern), keyResolver, limiter);
    }

    /// Sign-out; see [LogoutConfigurer]. A chain with [#formLogin] has it
    /// already; any other chain has none until it calls this.
    public HttpSecurity logout(Customizer<LogoutConfigurer> customizer) {
        LogoutConfigurer configurer = getConfigurer(LogoutConfigurer.class);
        if (configurer == null) {
            configurer = new LogoutConfigurer();
            apply(configurer);
        }
        customizer.customize(configurer);
        return this;
    }

    /// CSRF protection; see [CsrfConfigurer]. On for every chain that keeps a
    /// session; a [SessionCreationPolicy#STATELESS] chain has it only when it
    /// calls this.
    public HttpSecurity csrf(Customizer<CsrfConfigurer> customizer) {
        CsrfConfigurer configurer = getConfigurer(CsrfConfigurer.class);
        if (configurer == null) {
            configurer = new CsrfConfigurer();
            apply(configurer);
        }
        csrfAsked = true;
        customizer.customize(configurer);
        return this;
    }

    /// The use of the HTTP session; see [SessionManagementConfigurer].
    public HttpSecurity sessionManagement(Customizer<SessionManagementConfigurer> customizer) {
        SessionManagementConfigurer configurer = getConfigurer(SessionManagementConfigurer.class);
        if (configurer == null) {
            configurer = new SessionManagementConfigurer();
            apply(configurer);
        }
        customizer.customize(configurer);
        return this;
    }

    /// The security headers; see [HeadersConfigurer].
    public HttpSecurity headers(Customizer<HeadersConfigurer> customizer) {
        HeadersConfigurer configurer = getConfigurer(HeadersConfigurer.class);
        if (configurer == null) {
            configurer = new HeadersConfigurer();
            apply(configurer);
        }
        customizer.customize(configurer);
        return this;
    }

    /// Where an anonymous request's address is remembered; see
    /// [RequestCacheConfigurer].
    public HttpSecurity requestCache(Customizer<RequestCacheConfigurer> customizer) {
        customizer.customize(requestCacheConfigurer());
        return this;
    }

    /// The answers to a request that must sign in or is denied; see
    /// [ExceptionHandlingConfigurer].
    public HttpSecurity exceptionHandling(Customizer<ExceptionHandlingConfigurer> customizer) {
        ExceptionHandlingConfigurer configurer = getConfigurer(ExceptionHandlingConfigurer.class);
        if (configurer == null) {
            configurer = new ExceptionHandlingConfigurer();
            apply(configurer);
        }
        customizer.customize(configurer);
        return this;
    }

    /// Where who is signed in is kept; see [SecurityContextConfigurer].
    public HttpSecurity securityContext(Customizer<SecurityContextConfigurer> customizer) {
        SecurityContextConfigurer configurer = getConfigurer(SecurityContextConfigurer.class);
        if (configurer == null) {
            configurer = new SecurityContextConfigurer();
            apply(configurer);
        }
        customizer.customize(configurer);
        return this;
    }

    /// The authentication of a request nobody signed in for; see
    /// [AnonymousConfigurer].
    public HttpSecurity anonymous(Customizer<AnonymousConfigurer> customizer) {
        AnonymousConfigurer configurer = getConfigurer(AnonymousConfigurer.class);
        if (configurer == null) {
            configurer = new AnonymousConfigurer();
            apply(configurer);
        }
        customizer.customize(configurer);
        return this;
    }

    /// Applies a configurer of the application's or a library's own, and lets
    /// `customizer` set it up.
    public <C extends SecurityConfigurer> HttpSecurity with(C configurer, Customizer<C> customizer) {
        if (configurer == null) {
            throw new IllegalArgumentException("configurer cannot be null");
        }
        apply(configurer);
        if (customizer != null) {
            customizer.customize(configurer);
        }
        return this;
    }

    /// The configurer of this class applied to the chain, or null.
    @SuppressWarnings("unchecked")
    public <C extends SecurityConfigurer> C getConfigurer(Class<C> type) {
        return (C) configurers.get(type);
    }

    /// The [AuthenticationManager] the chain's sign-in filters use, in place of
    /// what the application's beans would give.
    public HttpSecurity authenticationManager(AuthenticationManager authenticationManager) {
        if (authenticationManager == null) {
            throw new IllegalArgumentException("authenticationManager cannot be null");
        }
        this.explicitAuthenticationManager = authenticationManager;
        return this;
    }

    /// One more provider for this chain, asked before those found among the
    /// application's beans.
    public HttpSecurity authenticationProvider(AuthenticationProvider authenticationProvider) {
        if (authenticationProvider == null) {
            throw new IllegalArgumentException("authenticationProvider cannot be null");
        }
        requireUnresolved();
        providers.add(authenticationProvider);
        return this;
    }

    /// The users of this chain, in place of the application's
    /// [UserDetailsService] bean.
    public HttpSecurity userDetailsService(UserDetailsService userDetailsService) {
        if (userDetailsService == null) {
            throw new IllegalArgumentException("userDetailsService cannot be null");
        }
        requireUnresolved();
        this.userDetailsService = userDetailsService;
        return this;
    }

    private void requireUnresolved() {
        // A configurer may still add one from its init(); once the sign-in
        // filters have asked for the manager it is made, and adding is too late.
        if (authenticationManagerResolved) {
            throw new IllegalStateException("The chain's AuthenticationManager has already "
                    + "been made; add providers and users before the chain is built, or from "
                    + "a configurer's init()");
        }
    }

    /// Adds `filter` just before the filter of class `beforeFilter`.
    public HttpSecurity addFilterBefore(SecurityFilter filter,
                                        Class<? extends SecurityFilter> beforeFilter) {
        return addFilterAtOffsetOf(filter, -1, beforeFilter);
    }

    /// Adds `filter` just after the filter of class `afterFilter`.
    public HttpSecurity addFilterAfter(SecurityFilter filter,
                                       Class<? extends SecurityFilter> afterFilter) {
        return addFilterAtOffsetOf(filter, 1, afterFilter);
    }

    /// Adds `filter` at the place of the filter of class `atFilter`, beside it
    /// rather than instead of it: which of the two runs first is not defined.
    public HttpSecurity addFilterAt(SecurityFilter filter, Class<? extends SecurityFilter> atFilter) {
        return addFilterAtOffsetOf(filter, 0, atFilter);
    }

    private HttpSecurity addFilterAtOffsetOf(SecurityFilter filter, int offset,
                                             Class<? extends SecurityFilter> registered) {
        if (filter == null) {
            throw new IllegalArgumentException("filter cannot be null");
        }
        Integer position = filterOrder.get(registered.getName());
        if (position == null) {
            throw new IllegalArgumentException("The Filter class " + registered.getName()
                    + " does not have a registered order: name one of the chain's own filter "
                    + "classes, or a filter this HttpSecurity was already given");
        }
        int order = position.intValue() + offset;
        filters.add(new Object[] {Integer.valueOf(order), filter});
        // So that a later filter can be placed relative to this one.
        String name = filter.getClass().getName();
        if (!filterOrder.containsKey(name)) {
            filterOrder.put(name, Integer.valueOf(order));
        }
        return this;
    }

    /// Adds one of the layer's own filters at its place: one of the ORDER_
    /// numbers, or a number between two of them for a part of a library's.
    void addFilter(SecurityFilter filter, int order) {
        filters.add(new Object[] {Integer.valueOf(order), filter});
    }

    /// Something the parts of a chain share, by its class: one set with
    /// [#setSharedObject], or else the one bean of the application that is an
    /// instance of `sharedType`. Null when there is none, or more than one bean.
    @SuppressWarnings("unchecked")
    public <C> C getSharedObject(Class<C> sharedType) {
        Object set = sharedObjects.get(sharedType);
        if (set != null) {
            return (C) set;
        }
        Object found = null;
        for (Object bean : beans) {
            if (bean != null && sharedType.isInstance(bean)) {
                if (found != null) {
                    return null;
                }
                found = bean;
            }
        }
        return (C) found;
    }

    /// Shares `object` with the parts of this chain under `sharedType`.
    public <C> void setSharedObject(Class<C> sharedType, C object) {
        sharedObjects.put(sharedType, object);
    }

    /// The configuration of the server the chain is built for.
    public Config getConfig() {
        return config;
    }

    /// The chain. An [HttpSecurity] builds one chain, once.
    public DefaultSecurityFilterChain build() {
        if (built) {
            throw new IllegalStateException("This HttpSecurity has already built its chain. "
                    + "Every SecurityFilterChain bean method is handed its own; declare the "
                    + "parameter on each.");
        }
        built = true;
        List<SecurityConfigurer> all = new ArrayList<SecurityConfigurer>(configurers.values());
        for (SecurityConfigurer configurer : all) {
            configurer.init(this);
        }
        // A configurer applied by another's init() takes part too.
        all = new ArrayList<SecurityConfigurer>(configurers.values());
        for (SecurityConfigurer configurer : all) {
            configurer.configure(this);
        }
        // By place, and within one place in the order they were added.
        List<Object[]> sorted = new ArrayList<Object[]>();
        for (Object[] entry : filters) {
            int order = ((Integer) entry[0]).intValue();
            int at = sorted.size();
            while (at > 0 && ((Integer) sorted.get(at - 1)[0]).intValue() > order) {
                at--;
            }
            sorted.add(at, entry);
        }
        List<SecurityFilter> chain = new ArrayList<SecurityFilter>(sorted.size());
        for (Object[] entry : sorted) {
            chain.add((SecurityFilter) entry[1]);
        }
        return new DefaultSecurityFilterChain(requestMatcher, chain);
    }

    // ------------------------------------------------- what the parts ask for

    private void apply(SecurityConfigurer configurer) {
        configurer.attach(this);
        configurers.put(configurer.getClass(), configurer);
    }

    void removeConfigurer(Class<?> type) {
        SecurityConfigurer removed = configurers.remove(type);
        if (removed != null && removed == requestCacheSource) { //NOPMD CompareObjectsWithEquals - the object itself
            requestCacheSource = null;
        }
        if (!disabled.contains(type)) {
            disabled.add(type);
        }
    }

    /// What a way of signing in that keeps the user in a session brings with
    /// it: sign-out, and the memory of where a request was going. Called by
    /// the DSL method of such a mechanism, so a chain without one names neither.
    void sessionMechanism() {
        if (getConfigurer(LogoutConfigurer.class) == null
                && !disabled.contains(LogoutConfigurer.class)) {
            apply(new LogoutConfigurer());
        }
        if (requestCacheSource == null && !disabled.contains(RequestCacheConfigurer.class)) {
            requestCacheConfigurer();
        }
    }

    private RequestCacheConfigurer requestCacheConfigurer() {
        RequestCacheConfigurer configurer = getConfigurer(RequestCacheConfigurer.class);
        if (configurer == null) {
            configurer = new RequestCacheConfigurer();
            apply(configurer);
            requestCacheSource = configurer;
        }
        return configurer;
    }

    private final List<SessionSignIn.Listener> signInListeners =
            new ArrayList<SessionSignIn.Listener>();
    private SecondFactorPolicy secondFactorPolicy;
    /// How a mechanism of this chain signs a user in to a session; see
    /// [SessionSignIn]. Asked for from a configurer's configure(), once every
    /// part has said in its init() what it adds to a sign-in.
    ///
    /// Kept among the shared objects rather than in a field of its own: a
    /// field of that type is enough to put the class into a server whose
    /// chains sign nobody in to a session -- measured, in one that only
    /// verifies tokens.
    SessionSignIn signIn() {
        Object shared = sharedObjects.get(SessionSignIn.class);
        if (shared instanceof SessionSignIn) {
            return (SessionSignIn) shared;
        }
        SessionSignIn made = new SessionSignIn(resolveSecurityContextRepository(),
                sessionAuthentication(), signInListeners, secondFactorPolicy);
        sharedObjects.put(SessionSignIn.class, made);
        return made;
    }

    /// Something to tell of every sign-in; from a configurer's init().
    void addSignInListener(SessionSignIn.Listener listener) {
        signInListeners.add(listener);
    }

    /// What holds a sign-in back for a second factor; from a configurer's
    /// init(). A chain has one.
    void secondFactorPolicy(SecondFactorPolicy policy) {
        if (secondFactorPolicy != null && policy != secondFactorPolicy) { //NOPMD CompareObjectsWithEquals - the object itself
            throw new IllegalStateException("This chain already has a SecondFactorPolicy");
        }
        secondFactorPolicy = policy;
    }

    private final List<AuthenticationCodec> authenticationCodecs =
            new ArrayList<AuthenticationCodec>();

    /// Keeps one more kind of [Authentication] in the session as itself; see
    /// [AuthenticationCodec]. Called before the chain is built, or from a
    /// configurer's `init`: the ways of signing in that have a kind of their
    /// own -- [#oauth2Login], [#webAuthn] -- each call it for theirs.
    public HttpSecurity authenticationCodec(AuthenticationCodec codec) {
        if (codec == null) {
            throw new IllegalArgumentException("codec cannot be null");
        }
        if (sharedObjects.get(SecurityContextRepository.class) != null) {
            throw new IllegalStateException("The chain's SecurityContextRepository has already "
                    + "been made; add codecs before the chain is built, or from a "
                    + "configurer's init()");
        }
        authenticationCodecs.add(codec);
        return this;
    }

    /// Whether the chain called [#csrf], rather than having it by default.
    boolean csrfAsked() {
        return csrfAsked;
    }

    /// The path of the login page a sign-in mechanism of this chain serves or
    /// names; `/login` until one says otherwise.
    String loginPage() {
        return loginPage;
    }

    void loginPage(String loginPage) {
        this.loginPage = loginPage;
    }

    /// {path, name} of each other way of signing in the login page links to.
    private final List<String[]> loginLinks = new ArrayList<String[]>();
    private boolean loginPageServed;

    /// A way of signing in that starts at a path of this server rather than
    /// with the login form: what a generated login page offers as a link. Told
    /// from a configurer's init().
    void loginLink(String path, String name) {
        loginLinks.add(new String[] {path, name});
    }

    List<String[]> loginLinks() {
        return loginLinks;
    }

    /// Says that a part of this chain serves the login page; from a
    /// configurer's init(). Another part that would serve one then does not.
    void servesLoginPage() {
        loginPageServed = true;
    }

    boolean loginPageServed() {
        return loginPageServed;
    }

    private String rememberMeParameter;

    /// The form field that asks to be remembered, when the chain remembers:
    /// what the generated login page offers as a checkbox. Null otherwise.
    String rememberMeParameter() {
        return rememberMeParameter;
    }

    void rememberMeParameter(String rememberMeParameter) {
        this.rememberMeParameter = rememberMeParameter;
    }

    /// Says that this chain sends a request that must sign in to a page it can
    /// come back from, so the address is worth remembering. A sign-in part that
    /// redirects calls this from its init().
    void redirectsToSignIn() {
        redirectsToSignIn = true;
    }

    /// A request the authorization rules must let through whatever they say.
    void permit(RequestMatcher matcher) {
        permitted.add(matcher);
    }

    List<RequestMatcher> permitted() {
        return permitted;
    }

    SessionCreationPolicy sessionCreationPolicy() {
        SessionManagementConfigurer session = getConfigurer(SessionManagementConfigurer.class);
        return session == null ? SessionCreationPolicy.IF_REQUIRED : session.getPolicy();
    }

    SessionAuthentication sessionAuthentication() {
        SessionManagementConfigurer session = getConfigurer(SessionManagementConfigurer.class);
        return session == null ? new SessionAuthentication(SessionAuthentication.CHANGE_SESSION_ID,
                false) : session.strategy();
    }

    SecurityContextRepository resolveSecurityContextRepository() {
        SecurityContextRepository shared = (SecurityContextRepository) sharedObjects.get(
                SecurityContextRepository.class);
        if (shared == null) {
            SecurityContextConfigurer context = getConfigurer(SecurityContextConfigurer.class);
            shared = context == null ? new NullSecurityContextRepository()
                    : context.resolve(sessionCreationPolicy(), authenticationCodecs);
            sharedObjects.put(SecurityContextRepository.class, shared);
        }
        return shared;
    }

    RequestCache resolveRequestCache() {
        RequestCache shared = (RequestCache) sharedObjects.get(RequestCache.class);
        if (shared == null) {
            shared = requestCacheSource == null ? new NullRequestCache()
                    : requestCacheSource.resolve(sessionCreationPolicy(), redirectsToSignIn);
            sharedObjects.put(RequestCache.class, shared);
        }
        return shared;
    }

    AccessDeniedHandler resolveAccessDeniedHandler() {
        ExceptionHandlingConfigurer handling = getConfigurer(ExceptionHandlingConfigurer.class);
        return handling == null ? new AccessDeniedHandlerImpl()
                : handling.resolveAccessDeniedHandler();
    }

    // ---- what PasswordAuthentication, which builds the manager of a chain that
    // checks passwords, reads and records. Kept apart from it so that a chain
    // that checks none carries no user store and no password hashing.

    AuthenticationManager explicitAuthenticationManager() {
        return explicitAuthenticationManager;
    }

    List<AuthenticationProvider> providers() {
        return providers;
    }

    UserDetailsService chosenUserDetailsService() {
        return userDetailsService;
    }

    Object[] beans() {
        return beans;
    }

    boolean authenticationManagerResolved() {
        return authenticationManagerResolved;
    }

    AuthenticationManager resolvedAuthenticationManager() {
        return authenticationManager;
    }

    void resolvedAuthenticationManager(AuthenticationManager manager) {
        authenticationManagerResolved = true;
        authenticationManager = manager;
    }
}
