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
import com.codename1.backend.HttpSession;
import java.util.ArrayList;
import java.util.List;

/// Sign-out. On by default: `POST /logout` ends the session, forgets who was
/// signed in and redirects to `/login?logout`.
///
/// ```java
/// http.logout(logout -> logout
///         .logoutUrl("/signout")
///         .logoutSuccessUrl("/")
///         .deleteCookies("remember"));
/// ```
///
/// The request must be a POST while CSRF protection is on, so that a link on
/// another site cannot sign a user out; with it off, any method does.
public final class LogoutConfigurer extends SecurityConfigurer {
    private String logoutUrl = "/logout";
    private RequestMatcher logoutRequestMatcher;
    private String logoutSuccessUrl;
    private LogoutSuccessHandler logoutSuccessHandler;
    private boolean invalidateHttpSession = true;
    private boolean clearAuthentication = true;
    private final List<String> cookies = new ArrayList<String>();
    private final List<LogoutHandler> handlers = new ArrayList<LogoutHandler>();
    private boolean permitAll;

    LogoutConfigurer() {
    }

    /// The path that signs out.
    public LogoutConfigurer logoutUrl(String logoutUrl) {
        this.logoutUrl = FormLoginConfigurer.path(logoutUrl, "logoutUrl");
        return this;
    }

    /// The requests that sign out, in place of the path and its method rule.
    public LogoutConfigurer logoutRequestMatcher(RequestMatcher logoutRequestMatcher) {
        this.logoutRequestMatcher = logoutRequestMatcher;
        return this;
    }

    /// Where a signed-out user goes.
    public LogoutConfigurer logoutSuccessUrl(String logoutSuccessUrl) {
        this.logoutSuccessUrl = FormLoginConfigurer.path(logoutSuccessUrl, "logoutSuccessUrl");
        return this;
    }

    /// Answers the sign-out itself, instead of the redirect.
    public LogoutConfigurer logoutSuccessHandler(LogoutSuccessHandler logoutSuccessHandler) {
        this.logoutSuccessHandler = logoutSuccessHandler;
        return this;
    }

    /// Whether signing out ends the session; true unless changed.
    public LogoutConfigurer invalidateHttpSession(boolean invalidateHttpSession) {
        this.invalidateHttpSession = invalidateHttpSession;
        return this;
    }

    /// Whether signing out forgets who was signed in; true unless changed.
    public LogoutConfigurer clearAuthentication(boolean clearAuthentication) {
        this.clearAuthentication = clearAuthentication;
        return this;
    }

    /// Cookies to delete at sign-out, by name; each must have been set with the
    /// path `/`.
    public LogoutConfigurer deleteCookies(String... cookieNamesToClear) {
        for (String name : cookieNamesToClear) {
            cookies.add(CsrfFilter.requireName(name, "cookie name"));
        }
        return this;
    }

    /// Something more to undo at sign-out; runs before the session ends.
    public LogoutConfigurer addLogoutHandler(LogoutHandler logoutHandler) {
        if (logoutHandler == null) {
            throw new IllegalArgumentException("logoutHandler cannot be null");
        }
        handlers.add(logoutHandler);
        return this;
    }

    /// Lets everyone reach the page a signed-out user is sent to.
    public LogoutConfigurer permitAll() {
        this.permitAll = true;
        return this;
    }

    private String successUrl(HttpSecurity http) {
        if (logoutSuccessUrl != null) {
            return logoutSuccessUrl;
        }
        FormLoginConfigurer form = http.getConfigurer(FormLoginConfigurer.class);
        return (form == null ? "/login" : form.getLoginPage()) + "?logout";
    }

    @Override
    public void init(HttpSecurity http) {
        if (permitAll) {
            http.permit(AntPathRequestMatcher.antMatcher("GET",
                    FormLoginConfigurer.pathOnly(successUrl(http))));
            http.permit(matcher(http));
        }
    }

    private RequestMatcher matcher(HttpSecurity http) {
        if (logoutRequestMatcher != null) {
            return logoutRequestMatcher;
        }
        if (http.getConfigurer(CsrfConfigurer.class) != null) {
            return AntPathRequestMatcher.antMatcher("POST", logoutUrl);
        }
        return RequestMatchers.anyOf(AntPathRequestMatcher.antMatcher("GET", logoutUrl),
                AntPathRequestMatcher.antMatcher("POST", logoutUrl),
                AntPathRequestMatcher.antMatcher("PUT", logoutUrl),
                AntPathRequestMatcher.antMatcher("DELETE", logoutUrl));
    }

    @Override
    public void configure(HttpSecurity http) {
        List<LogoutHandler> all = new ArrayList<LogoutHandler>(handlers);
        if (!cookies.isEmpty()) {
            all.add(new CookieClearing(cookies.toArray(new String[cookies.size()])));
        }
        all.add(new ContextClearing(http.getConfigurer(CsrfConfigurer.class) != null,
                invalidateHttpSession, clearAuthentication, http.resolveSecurityContextRepository(),
                http.sessionCreationPolicy() == SessionCreationPolicy.STATELESS));
        LogoutSuccessHandler success = logoutSuccessHandler != null ? logoutSuccessHandler
                : new SimpleUrlLogoutSuccessHandler(successUrl(http));
        http.addFilter(new LogoutFilter(matcher(http), all, success), LogoutFilter.class);
    }

    /// Tells the client to drop cookies, each set with the path `/`.
    private static final class CookieClearing implements LogoutHandler {
        private final String[] names;

        CookieClearing(String[] names) {
            this.names = names;
        }

        @Override
        public void logout(HttpServer.Request request, Authentication authentication) {
            SecurityExchange exchange = SecurityExchange.of(request);
            if (exchange == null) {
                return;
            }
            for (String name : names) {
                exchange.addResponseHeader("Set-Cookie", name + "=; Path=/; Max-Age=0"
                        + (exchange.isSecure() ? "; Secure" : ""));
            }
        }
    }

    /// Forgets the CSRF token and who was signed in, and ends the session.
    private static final class ContextClearing implements LogoutHandler {
        private final boolean csrf;
        private final boolean invalidate;
        private final boolean clear;
        private final SecurityContextRepository repository;
        private final boolean stateless;

        ContextClearing(boolean csrf, boolean invalidate, boolean clear,
                        SecurityContextRepository repository, boolean stateless) {
            this.csrf = csrf;
            this.invalidate = invalidate;
            this.clear = clear;
            this.repository = repository;
            this.stateless = stateless;
        }

        @Override
        public void logout(HttpServer.Request request, Authentication authentication) {
            if (csrf) {
                CsrfFilter.clear(request);
            }
            SecurityContext empty = SecurityContextHolder.createEmptyContext();
            if (clear) {
                SecurityContextHolder.setContext(empty);
            }
            if (stateless) {
                return;
            }
            HttpSession session = request.getSession(false);
            if (session == null) {
                return;
            }
            if (invalidate) {
                session.invalidate();
            } else if (clear) {
                repository.saveContext(empty, request);
            }
        }
    }
}
