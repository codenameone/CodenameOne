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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/// The authorization rules of a chain: which requests need what.
///
/// ```java
/// http.authorizeHttpRequests(auth -> auth
///         .requestMatchers("/", "/css/**").permitAll()
///         .requestMatchers("/admin/**").hasRole("ADMIN")
///         .requestMatchers(AntPathRequestMatcher.antMatcher("DELETE", "/api/**"))
///                 .hasAuthority("api:delete")
///         .anyRequest().authenticated());
/// ```
///
/// The rules are asked in the order they are written and the first that
/// matches decides. A request no rule matches is denied.
public final class AuthorizeHttpRequestsConfigurer extends SecurityConfigurer {
    private final AuthorizationManagerRequestMatcherRegistry registry =
            new AuthorizationManagerRequestMatcherRegistry();
    /// {RequestMatcher, AuthorizationManager}, in the order they are asked.
    private final List<Object[]> mappings = new ArrayList<Object[]>();
    private boolean anyRequestConfigured;
    private List<RequestMatcher> unmapped;

    AuthorizeHttpRequestsConfigurer() {
    }

    /// The rules, to add to.
    public AuthorizationManagerRequestMatcherRegistry getRegistry() {
        return registry;
    }

    @Override
    public void configure(HttpSecurity http) {
        if (unmapped != null) {
            throw new IllegalStateException("An incomplete mapping was found for " + unmapped
                    + ". Try completing it with something like requestUrls().<something>."
                    + "hasRole('USER')");
        }
        if (mappings.isEmpty()) {
            throw new IllegalStateException("At least one mapping is required (for example, "
                    + "authorizeHttpRequests().anyRequest().authenticated())");
        }
        List<Object[]> rules = new ArrayList<Object[]>();
        // What a login or logout configurer opened with permitAll() comes first,
        // or the application's own anyRequest().authenticated() would close it.
        for (RequestMatcher open : http.permitted()) {
            rules.add(new Object[] {open, PERMIT_ALL});
        }
        rules.addAll(mappings);
        http.addFilter(new AuthorizationFilter(new Delegating(rules)), AuthorizationFilter.class);
    }

    private static final AuthorizationManager<RequestAuthorizationContext> PERMIT_ALL =
            new AuthorizationManager<RequestAuthorizationContext>() {
                @Override
                public AuthorizationDecision check(Supplier<Authentication> authentication,
                                                   RequestAuthorizationContext object) {
                    return new AuthorizationDecision(true);
                }
            };

    private static final AuthorizationManager<RequestAuthorizationContext> DENY_ALL =
            new AuthorizationManager<RequestAuthorizationContext>() {
                @Override
                public AuthorizationDecision check(Supplier<Authentication> authentication,
                                                   RequestAuthorizationContext object) {
                    return new AuthorizationDecision(false);
                }
            };

    /// The rules as one manager: the first matching rule decides.
    private static final class Delegating implements AuthorizationManager<HttpServer.Request> {
        private final RequestMatcher[] matchers;
        private final AuthorizationManager<RequestAuthorizationContext>[] managers;

        @SuppressWarnings("unchecked")
        Delegating(List<Object[]> rules) {
            matchers = new RequestMatcher[rules.size()];
            managers = new AuthorizationManager[rules.size()];
            for (int iter = 0 ; iter < matchers.length ; iter++) {
                matchers[iter] = (RequestMatcher) rules.get(iter)[0];
                managers[iter] = (AuthorizationManager<RequestAuthorizationContext>) rules.get(iter)[1];
            }
        }

        @Override
        public AuthorizationDecision check(Supplier<Authentication> authentication,
                                           HttpServer.Request request) {
            for (int iter = 0 ; iter < matchers.length ; iter++) {
                RequestMatcher.MatchResult result = matchers[iter].matcher(request);
                if (result.isMatch()) {
                    return managers[iter].check(authentication,
                            new RequestAuthorizationContext(request, result.getVariables()));
                }
            }
            return new AuthorizationDecision(false);
        }
    }

    /// Where rules are added: pick the requests, then say what they need.
    public final class AuthorizationManagerRequestMatcherRegistry {
        private AuthorizationManagerRequestMatcherRegistry() {
        }

        /// The requests whose path matches any of these Ant patterns; see
        /// [AntPathRequestMatcher].
        public AuthorizedUrl requestMatchers(String... patterns) {
            if (patterns == null || patterns.length == 0) {
                throw new IllegalArgumentException("At least one pattern is required");
            }
            RequestMatcher[] matchers = new RequestMatcher[patterns.length];
            for (int iter = 0 ; iter < patterns.length ; iter++) {
                matchers[iter] = new AntPathRequestMatcher(patterns[iter]);
            }
            return requestMatchers(matchers);
        }

        /// The requests any of these matchers match.
        public AuthorizedUrl requestMatchers(RequestMatcher... matchers) {
            if (matchers == null || matchers.length == 0) {
                throw new IllegalArgumentException("At least one matcher is required");
            }
            if (anyRequestConfigured) {
                throw new IllegalStateException("Can't configure requestMatchers after anyRequest");
            }
            List<RequestMatcher> chosen = new ArrayList<RequestMatcher>();
            for (RequestMatcher matcher : matchers) {
                if (matcher == null) {
                    throw new IllegalArgumentException("matchers cannot contain null values");
                }
                chosen.add(matcher);
            }
            return chain(chosen);
        }

        /// Every request the rules before this one did not match. The last rule.
        public AuthorizedUrl anyRequest() {
            if (anyRequestConfigured) {
                throw new IllegalStateException("Can't configure anyRequest after itself");
            }
            anyRequestConfigured = true;
            List<RequestMatcher> chosen = new ArrayList<RequestMatcher>();
            chosen.add(AnyRequestMatcher.INSTANCE);
            return chain(chosen);
        }

        private AuthorizedUrl chain(List<RequestMatcher> chosen) {
            if (unmapped != null) {
                throw new IllegalStateException("An incomplete mapping was found for " + unmapped
                        + ". Say what those requests need before choosing more.");
            }
            unmapped = chosen;
            return new AuthorizedUrl(chosen);
        }

        private AuthorizationManagerRequestMatcherRegistry add(List<RequestMatcher> chosen,
                AuthorizationManager<RequestAuthorizationContext> manager) {
            if (manager == null) {
                throw new IllegalArgumentException("manager cannot be null");
            }
            for (RequestMatcher matcher : chosen) {
                mappings.add(new Object[] {matcher, manager});
            }
            unmapped = null;
            return this;
        }
    }

    /// What the chosen requests need.
    public final class AuthorizedUrl {
        private final List<RequestMatcher> matchers;

        private AuthorizedUrl(List<RequestMatcher> matchers) {
            this.matchers = matchers;
        }

        /// Anyone, signed in or not.
        public AuthorizationManagerRequestMatcherRegistry permitAll() {
            return access(PERMIT_ALL);
        }

        /// Nobody.
        public AuthorizationManagerRequestMatcherRegistry denyAll() {
            return access(DENY_ALL);
        }

        /// Anyone who signed in.
        public AuthorizationManagerRequestMatcherRegistry authenticated() {
            return access(AuthenticatedAuthorizationManager.<RequestAuthorizationContext>authenticated());
        }

        /// Only callers who did not sign in.
        public AuthorizationManagerRequestMatcherRegistry anonymous() {
            return access(AuthenticatedAuthorizationManager.<RequestAuthorizationContext>anonymous());
        }

        /// A caller with the role: `hasRole("ADMIN")` asks for the authority
        /// `ROLE_ADMIN`.
        public AuthorizationManagerRequestMatcherRegistry hasRole(String role) {
            return access(AuthorityAuthorizationManager.<RequestAuthorizationContext>hasRole(role));
        }

        /// A caller with any one of the roles.
        public AuthorizationManagerRequestMatcherRegistry hasAnyRole(String... roles) {
            return access(AuthorityAuthorizationManager.<RequestAuthorizationContext>hasAnyRole(roles));
        }

        /// A caller with the authority, as it is written.
        public AuthorizationManagerRequestMatcherRegistry hasAuthority(String authority) {
            return access(AuthorityAuthorizationManager
                    .<RequestAuthorizationContext>hasAuthority(authority));
        }

        /// A caller with any one of the authorities.
        public AuthorizationManagerRequestMatcherRegistry hasAnyAuthority(String... authorities) {
            return access(AuthorityAuthorizationManager
                    .<RequestAuthorizationContext>hasAnyAuthority(authorities));
        }

        /// Whoever `manager` grants.
        public AuthorizationManagerRequestMatcherRegistry access(
                AuthorizationManager<RequestAuthorizationContext> manager) {
            return registry.add(matchers, manager);
        }
    }
}
