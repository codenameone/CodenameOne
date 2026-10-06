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
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetailsPasswordService;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoderFactories;
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
/// Out of the box a chain guards every request, protects against CSRF, writes
/// the security headers, keeps who is signed in in the HTTP session, remembers
/// where an anonymous request was going and signs out at `POST /logout`. It has
/// no way of signing in and no authorization rules until it is given them.
///
/// Users come from the application's beans: a
/// [UserDetailsService] and, if there is one, a [PasswordEncoder] and a
/// [UserDetailsPasswordService] -- or [AuthenticationProvider] beans, or an
/// [AuthenticationManager] bean. A chain can also be told directly, with
/// [#userDetailsService], [#authenticationProvider] or [#authenticationManager].
public final class HttpSecurity {
    /// The name of the one user a server with no user store has, when
    /// [#USER_PASSWORD] is set; `user` unless set.
    public static final String USER_NAME = "cn1.security.user.name";
    /// That user's password, as it would be stored; one written without an
    /// `{id}` is taken as `{noop}`, which verifies on a development profile only.
    public static final String USER_PASSWORD = "cn1.security.user.password";
    /// That user's roles, separated by commas.
    public static final String USER_ROLES = "cn1.security.user.roles";

    private final Config config;
    private final Object[] beans;
    private final Map<Class<?>, Object> sharedObjects = new HashMap<Class<?>, Object>();
    private final Map<Class<?>, SecurityConfigurer> configurers =
            new LinkedHashMap<Class<?>, SecurityConfigurer>();
    private final Map<Class<?>, Integer> filterOrder = new HashMap<Class<?>, Integer>();
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

    HttpSecurity(Config config, Object[] beans) {
        this.config = config;
        this.beans = beans == null ? new Object[0] : beans.clone();
        // The order the filters of a chain run in. The gaps are where the
        // filters of the sign-in mechanisms that are not here yet belong:
        // between CSRF and the form login for a redirect to another identity
        // provider, between the form login and HTTP Basic for a bearer token.
        filterOrder.put(SecurityContextHolderFilter.class, Integer.valueOf(100));
        filterOrder.put(HeaderWriterFilter.class, Integer.valueOf(200));
        filterOrder.put(CsrfFilter.class, Integer.valueOf(400));
        filterOrder.put(LogoutFilter.class, Integer.valueOf(500));
        filterOrder.put(UsernamePasswordAuthenticationFilter.class, Integer.valueOf(1000));
        filterOrder.put(DefaultLoginPageGeneratingFilter.class, Integer.valueOf(1100));
        filterOrder.put(BasicAuthenticationFilter.class, Integer.valueOf(1500));
        filterOrder.put(AnonymousAuthenticationFilter.class, Integer.valueOf(2000));
        filterOrder.put(ExceptionTranslationFilter.class, Integer.valueOf(2400));
        filterOrder.put(AuthorizationFilter.class, Integer.valueOf(2500));
        // What every chain has until it says otherwise.
        apply(new SecurityContextConfigurer());
        apply(new HeadersConfigurer());
        apply(new CsrfConfigurer());
        apply(new LogoutConfigurer());
        apply(new SessionManagementConfigurer());
        apply(new RequestCacheConfigurer());
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
        customizer.customize(getOrApply(AuthorizeHttpRequestsConfigurer.class).getRegistry());
        return this;
    }

    /// Sign-in through an HTML form; see [FormLoginConfigurer].
    public HttpSecurity formLogin(Customizer<FormLoginConfigurer> customizer) {
        customizer.customize(getOrApply(FormLoginConfigurer.class));
        return this;
    }

    /// Sign-in with HTTP Basic credentials; see [HttpBasicConfigurer].
    public HttpSecurity httpBasic(Customizer<HttpBasicConfigurer> customizer) {
        customizer.customize(getOrApply(HttpBasicConfigurer.class));
        return this;
    }

    /// Sign-out; see [LogoutConfigurer].
    public HttpSecurity logout(Customizer<LogoutConfigurer> customizer) {
        customizer.customize(getOrApply(LogoutConfigurer.class));
        return this;
    }

    /// CSRF protection; see [CsrfConfigurer].
    public HttpSecurity csrf(Customizer<CsrfConfigurer> customizer) {
        customizer.customize(getOrApply(CsrfConfigurer.class));
        return this;
    }

    /// The use of the HTTP session; see [SessionManagementConfigurer].
    public HttpSecurity sessionManagement(Customizer<SessionManagementConfigurer> customizer) {
        customizer.customize(getOrApply(SessionManagementConfigurer.class));
        return this;
    }

    /// The security headers; see [HeadersConfigurer].
    public HttpSecurity headers(Customizer<HeadersConfigurer> customizer) {
        customizer.customize(getOrApply(HeadersConfigurer.class));
        return this;
    }

    /// Where an anonymous request's address is remembered; see
    /// [RequestCacheConfigurer].
    public HttpSecurity requestCache(Customizer<RequestCacheConfigurer> customizer) {
        customizer.customize(getOrApply(RequestCacheConfigurer.class));
        return this;
    }

    /// The answers to a request that must sign in or is denied; see
    /// [ExceptionHandlingConfigurer].
    public HttpSecurity exceptionHandling(Customizer<ExceptionHandlingConfigurer> customizer) {
        customizer.customize(getOrApply(ExceptionHandlingConfigurer.class));
        return this;
    }

    /// Where who is signed in is kept; see [SecurityContextConfigurer].
    public HttpSecurity securityContext(Customizer<SecurityContextConfigurer> customizer) {
        customizer.customize(getOrApply(SecurityContextConfigurer.class));
        return this;
    }

    /// The authentication of a request nobody signed in for; see
    /// [AnonymousConfigurer].
    public HttpSecurity anonymous(Customizer<AnonymousConfigurer> customizer) {
        customizer.customize(getOrApply(AnonymousConfigurer.class));
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
        Integer position = filterOrder.get(registered);
        if (position == null) {
            throw new IllegalArgumentException("The Filter class " + registered.getName()
                    + " does not have a registered order: name one of the chain's own filter "
                    + "classes, or a filter this HttpSecurity was already given");
        }
        int order = position.intValue() + offset;
        filters.add(new Object[] {Integer.valueOf(order), filter});
        // So that a later filter can be placed relative to this one.
        if (!filterOrder.containsKey(filter.getClass())) {
            filterOrder.put(filter.getClass(), Integer.valueOf(order));
        }
        return this;
    }

    /// Adds one of the chain's own filters at its own place.
    void addFilter(SecurityFilter filter, Class<? extends SecurityFilter> type) {
        filters.add(new Object[] {filterOrder.get(type), filter});
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
        if (sharedType == AuthenticationManager.class) { //NOPMD CompareObjectsWithEquals - a class
            return (C) resolveAuthenticationManager();
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
        configurers.remove(type);
    }

    @SuppressWarnings("unchecked")
    private <C extends SecurityConfigurer> C getOrApply(Class<C> type) {
        SecurityConfigurer existing = configurers.get(type);
        if (existing != null) {
            return (C) existing;
        }
        SecurityConfigurer created;
        // Spelled out: this runtime builds nothing by reflection.
        if (type == AuthorizeHttpRequestsConfigurer.class) { //NOPMD CompareObjectsWithEquals - classes
            created = new AuthorizeHttpRequestsConfigurer();
        } else if (type == FormLoginConfigurer.class) { //NOPMD CompareObjectsWithEquals
            created = new FormLoginConfigurer();
        } else if (type == HttpBasicConfigurer.class) { //NOPMD CompareObjectsWithEquals
            created = new HttpBasicConfigurer();
        } else if (type == LogoutConfigurer.class) { //NOPMD CompareObjectsWithEquals
            created = new LogoutConfigurer();
        } else if (type == CsrfConfigurer.class) { //NOPMD CompareObjectsWithEquals
            created = new CsrfConfigurer();
        } else if (type == SessionManagementConfigurer.class) { //NOPMD CompareObjectsWithEquals
            created = new SessionManagementConfigurer();
        } else if (type == HeadersConfigurer.class) { //NOPMD CompareObjectsWithEquals
            created = new HeadersConfigurer();
        } else if (type == RequestCacheConfigurer.class) { //NOPMD CompareObjectsWithEquals
            created = new RequestCacheConfigurer();
        } else if (type == ExceptionHandlingConfigurer.class) { //NOPMD CompareObjectsWithEquals
            created = new ExceptionHandlingConfigurer();
        } else if (type == SecurityContextConfigurer.class) { //NOPMD CompareObjectsWithEquals
            created = new SecurityContextConfigurer();
        } else if (type == AnonymousConfigurer.class) { //NOPMD CompareObjectsWithEquals
            created = new AnonymousConfigurer();
        } else {
            throw new IllegalArgumentException("Not a built-in configurer: " + type.getName());
        }
        apply(created);
        return (C) created;
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
                    : context.resolve(sessionCreationPolicy());
            sharedObjects.put(SecurityContextRepository.class, shared);
        }
        return shared;
    }

    RequestCache resolveRequestCache() {
        RequestCache shared = (RequestCache) sharedObjects.get(RequestCache.class);
        if (shared == null) {
            RequestCacheConfigurer cache = getConfigurer(RequestCacheConfigurer.class);
            shared = cache == null ? new NullRequestCache()
                    : cache.resolve(sessionCreationPolicy(), redirectsToSignIn);
            sharedObjects.put(RequestCache.class, shared);
        }
        return shared;
    }

    AccessDeniedHandler resolveAccessDeniedHandler() {
        ExceptionHandlingConfigurer handling = getConfigurer(ExceptionHandlingConfigurer.class);
        return handling == null ? new AccessDeniedHandlerImpl()
                : handling.resolveAccessDeniedHandler();
    }

    /// The manager a sign-in filter authenticates with.
    AuthenticationManager requireAuthenticationManager(String what) {
        AuthenticationManager manager = resolveAuthenticationManager();
        if (manager == null) {
            throw new IllegalStateException(what + " needs something to check credentials "
                    + "against, and this application has none. Declare a UserDetailsService "
                    + "bean, an AuthenticationProvider bean or an AuthenticationManager bean, "
                    + "call userDetailsService(...) on the HttpSecurity, or set "
                    + USER_PASSWORD + ".");
        }
        return manager;
    }

    private AuthenticationManager resolveAuthenticationManager() {
        if (authenticationManagerResolved) {
            return authenticationManager;
        }
        authenticationManagerResolved = true;
        if (explicitAuthenticationManager != null) {
            authenticationManager = explicitAuthenticationManager;
            return authenticationManager;
        }
        List<AuthenticationProvider> all = new ArrayList<AuthenticationProvider>(providers);
        for (Object bean : beans) {
            if (bean instanceof AuthenticationProvider && !all.contains(bean)) {
                all.add((AuthenticationProvider) bean);
            }
        }
        AuthenticationManager parent = null;
        int managers = 0;
        for (Object bean : beans) {
            if (bean instanceof AuthenticationManager) {
                parent = (AuthenticationManager) bean;
                managers++;
            }
        }
        if (managers != 1) {
            parent = null;
        }
        UserDetailsService users = userDetailsService;
        if (users == null && all.isEmpty() && parent == null) {
            // The application's user store, when it has exactly one and no
            // provider of its own: as Spring Boot wires it.
            users = getSharedObject(UserDetailsService.class);
            if (users == null) {
                users = configuredUser();
            }
        }
        if (users != null) {
            DaoAuthenticationProvider dao = new DaoAuthenticationProvider(users);
            PasswordEncoder encoder = getSharedObject(PasswordEncoder.class);
            dao.setPasswordEncoder(encoder != null ? encoder
                    : PasswordEncoderFactories.createDelegatingPasswordEncoder());
            UserDetailsPasswordService passwords = getSharedObject(UserDetailsPasswordService.class);
            if (passwords == null && users instanceof UserDetailsPasswordService) {
                passwords = (UserDetailsPasswordService) users;
            }
            dao.setUserDetailsPasswordService(passwords);
            all.add(dao);
        }
        if (all.isEmpty()) {
            authenticationManager = parent;
        } else {
            authenticationManager = new ProviderManager(all, parent);
        }
        return authenticationManager;
    }

    /// The one user `cn1.security.user.*` describes, or null.
    private UserDetailsService configuredUser() {
        if (config == null) {
            return null;
        }
        String password;
        String name;
        String roles;
        try {
            password = config.get(USER_PASSWORD);
            name = config.get(USER_NAME, "user");
            roles = config.get(USER_ROLES, "");
        } catch (java.io.IOException err) {
            throw new IllegalStateException(err.getMessage(), err);
        }
        if (password == null || password.length() == 0) {
            return null;
        }
        if (!(password.startsWith("{") && password.indexOf('}') > 1)) {
            password = "{noop}" + password;
        }
        List<String> granted = new ArrayList<String>();
        int start = 0;
        while (start <= roles.length()) {
            int comma = roles.indexOf(',', start);
            int end = comma < 0 ? roles.length() : comma;
            String role = roles.substring(start, end).trim();
            if (role.length() > 0) {
                granted.add(role);
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        return new InMemoryUserDetailsManager(User.withUsername(name).password(password)
                .roles(granted.toArray(new String[granted.size()])).build());
    }
}
