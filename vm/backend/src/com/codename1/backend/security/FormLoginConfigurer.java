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

/// Sign-in through an HTML form.
///
/// ```java
/// http.formLogin(form -> form
///         .loginPage("/signin")
///         .defaultSuccessUrl("/home")
///         .permitAll());
/// ```
///
/// With nothing set, the chain serves a plain login page at `GET /login`, takes
/// the form at `POST /login` with the fields `username` and `password`, sends a
/// signed-in user back to the page that asked -- or to `/` -- and a refused one
/// to `/login?error`. Naming a `loginPage` hands the page to the application:
/// the chain then serves none, and takes the form at that same path.
public final class FormLoginConfigurer extends SecurityConfigurer {
    private String loginPage = "/login";
    private boolean customLoginPage;
    private String loginProcessingUrl;
    private String usernameParameter = "username";
    private String passwordParameter = "password";
    private String defaultSuccessUrl = "/";
    private boolean alwaysUseDefaultSuccessUrl;
    private String failureUrl;
    private AuthenticationSuccessHandler successHandler;
    private AuthenticationFailureHandler failureHandler;
    private boolean permitAll;

    FormLoginConfigurer() {
    }

    /// The application's own login page: a path the application serves.
    public FormLoginConfigurer loginPage(String loginPage) {
        this.loginPage = Responses.path(loginPage, "loginPage");
        this.customLoginPage = true;
        // Told to the chain at once: sign-out reads it for its redirect.
        getBuilder().loginPage(this.loginPage);
        return this;
    }

    /// Where the form is posted; the login page's path unless set.
    public FormLoginConfigurer loginProcessingUrl(String loginProcessingUrl) {
        this.loginProcessingUrl = Responses.path(loginProcessingUrl, "loginProcessingUrl");
        return this;
    }

    public FormLoginConfigurer usernameParameter(String usernameParameter) {
        this.usernameParameter = CsrfFilter.requireName(usernameParameter, "usernameParameter");
        return this;
    }

    public FormLoginConfigurer passwordParameter(String passwordParameter) {
        this.passwordParameter = CsrfFilter.requireName(passwordParameter, "passwordParameter");
        return this;
    }

    /// Where a user goes after signing in when no page asked for the sign-in.
    public FormLoginConfigurer defaultSuccessUrl(String defaultSuccessUrl) {
        return defaultSuccessUrl(defaultSuccessUrl, false);
    }

    /// @param alwaysUse go there even when a page asked for the sign-in
    public FormLoginConfigurer defaultSuccessUrl(String defaultSuccessUrl, boolean alwaysUse) {
        this.defaultSuccessUrl = Responses.path(defaultSuccessUrl, "defaultSuccessUrl");
        this.alwaysUseDefaultSuccessUrl = alwaysUse;
        return this;
    }

    /// Where a refused sign-in goes; the login page with `?error` unless set.
    public FormLoginConfigurer failureUrl(String failureUrl) {
        this.failureUrl = Responses.path(failureUrl, "failureUrl");
        return this;
    }

    /// Answers a sign-in itself, instead of the redirects.
    public FormLoginConfigurer successHandler(AuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
        return this;
    }

    /// Answers a refused sign-in itself, instead of the redirect.
    public FormLoginConfigurer failureHandler(AuthenticationFailureHandler failureHandler) {
        this.failureHandler = failureHandler;
        return this;
    }

    /// Lets everyone reach the login page, the form's target and the failure
    /// page, whatever the authorization rules say. Needed with a `loginPage` of
    /// the application's: without it `anyRequest().authenticated()` redirects
    /// the login page to itself.
    public FormLoginConfigurer permitAll() {
        this.permitAll = true;
        return this;
    }

    String getLoginPage() {
        return loginPage;
    }

    private String processingUrl() {
        return loginProcessingUrl != null ? loginProcessingUrl : loginPage;
    }

    private String failure() {
        return failureUrl != null ? failureUrl : loginPage + "?error";
    }

    @Override
    public void init(HttpSecurity http) {
        http.redirectsToSignIn();
        ExceptionHandlingConfigurer handling = http.getConfigurer(ExceptionHandlingConfigurer.class);
        if (handling != null) {
            // A person's browser is sent to the login page; a script -- which
            // cannot use one -- is left to HTTP Basic's challenge when there is one.
            handling.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint(loginPage),
                    RequestMatchers.not(RequestMatchers.header("X-Requested-With", "XMLHttpRequest")));
        }
        if (permitAll) {
            http.permit(AntPathRequestMatcher.antMatcher("GET", loginPage));
            http.permit(AntPathRequestMatcher.antMatcher("POST", processingUrl()));
            http.permit(AntPathRequestMatcher.antMatcher("GET", Responses.pathOnly(failure())));
        }
    }

    @Override
    public void configure(HttpSecurity http) {
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
                : new SimpleUrlAuthenticationFailureHandler(failure());
        http.addFilter(new UsernamePasswordAuthenticationFilter(
                AntPathRequestMatcher.antMatcher("POST", processingUrl()), usernameParameter,
                passwordParameter, PasswordAuthentication.require(http, "formLogin()"), success,
                failed, http.signIn()),
                HttpSecurity.ORDER_FORM_LOGIN);
        if (!customLoginPage) {
            http.addFilter(new DefaultLoginPageGeneratingFilter(loginPage, processingUrl(),
                    usernameParameter, passwordParameter, http.rememberMeParameter()),
                    HttpSecurity.ORDER_LOGIN_PAGE);
        }
    }
}
