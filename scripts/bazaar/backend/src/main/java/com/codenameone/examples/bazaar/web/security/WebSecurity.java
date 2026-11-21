/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.web.security;

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Order;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.security.AntPathRequestMatcher;
import com.codename1.backend.security.CsrfFilter;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.SavedRequestAwareAuthenticationSuccessHandler;
import com.codename1.backend.security.SecurityExchange;
import com.codename1.backend.security.SecurityFilter;
import com.codename1.backend.security.SecurityFilterChain;
import com.codename1.backend.security.SessionCreationPolicy;
import com.codename1.backend.security.mfa.JdbcTotpRepository;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.security.ratelimit.RateLimitKeys;
import com.codenameone.examples.bazaar.Roles;

import java.io.IOException;

/// The browser-native web app's security: three chains beside the app's own two
/// ([com.codenameone.examples.bazaar.SecurityConfig]).
///
/// - **Assets** (`/static/**`): a file whose name carries its content hash
///   (`app.1a2b3c4d.css`) never changes, so it is cached for a year as
///   immutable; anything else keeps the server's `no-cache`.
/// - **Signed-in pages** (`/auth/**`, `/account/**`, `/admin/**`,
///   `/platform/**`, checkout): a session cookie from a password form (and an
///   authenticator code once enrolled), CSRF tokens on every state-changing
///   request, the session id replaced at sign-in, sign-out by POST only, and the
///   password guarded twice -- per address by rate ([Throttle] `signin`) and per
///   account by failures ([Throttle] `signin-failures`, a lockout).
/// - **Storefront** (`/`, `/s/**`, `/track/**`, `/consent/**`): public, no
///   session needed to read; the cart forms keep their same-site check.
///
/// Every page of the last two gets [WebHeaders] -- a nonce CSP, HSTS,
/// `nosniff`, a referrer policy, a permissions policy and no framing -- and
/// [PageEtags], which answers a repeated request for unchanged HTML with 304.
///
/// The paths are not the app's: `/login`, `/logout` and `/signin/code` belong
/// to the app's authorization-server chain, which answers JSON and keeps CSRF
/// off for the app's own screens. A web page signs in at `/auth/signin`.
@Configuration
public class WebSecurity {
    public static final String SIGN_IN = "/auth/signin";
    public static final String SECOND_FACTOR = "/auth/2fa";
    public static final String SIGN_OUT = "/auth/signout";

    /// Authenticator-app codes, kept sealed in the security schema's table.
    @Bean
    public TotpService totp(DataSource db, Config config) {
        return new TotpService(JdbcTotpRepository.fromConfig(db, config), "Bazaar");
    }

    @Bean
    @Order(0)
    public SecurityFilterChain assetChain(HttpSecurity http) throws IOException {
        http.securityMatcher("/static/**")
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(c -> c.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(new ImmutableAssets(), CsrfFilter.class);
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain webAppChain(HttpSecurity http, TotpService totp, final Throttle throttle,
            @Value("${bazaar.web.hsts:true}") boolean hsts) throws IOException {
        final SavedRequestAwareAuthenticationSuccessHandler saved =
                new SavedRequestAwareAuthenticationSuccessHandler();
        saved.setDefaultTargetUrl("/account");
        http.securityMatcher("/auth/**", "/account", "/account/**", "/admin", "/admin/**",
                    "/platform", "/platform/**", "/s/*/checkout", "/s/*/checkout/**")
            .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
            .addFilterBefore(new WebHeaders(hsts), CsrfFilter.class)
            .addFilterBefore(new PageEtags(), CsrfFilter.class)
            .addFilterBefore(lockout(throttle), CsrfFilter.class)
            .rateLimit(AntPathRequestMatcher.antMatcher("POST", SIGN_IN),
                    RateLimitKeys.clientAddress(), throttle.limiter("signin"))
            .rateLimit(AntPathRequestMatcher.antMatcher("POST", "/s/*/checkout/**"),
                    RateLimitKeys.firstOf(RateLimitKeys.principal(), RateLimitKeys.clientAddress()),
                    throttle.limiter("checkout"))
            .rateLimit(AntPathRequestMatcher.antMatcher("POST", "/auth/sms/**"),
                    RateLimitKeys.clientAddress(), throttle.limiter("sms"))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/platform", "/platform/**").hasRole(Roles.ADMIN)
                    .requestMatchers("/admin", "/admin/**").hasRole(Roles.MERCHANT)
                    .anyRequest().authenticated())
            .formLogin(form -> form
                    .loginPage(SIGN_IN)
                    .loginProcessingUrl(SIGN_IN)
                    .successHandler((request, who) -> {
                        throttle.succeed("signin-failures", key(request));
                        return saved.onAuthenticationSuccess(request, who);
                    })
                    .failureHandler((request, refused) -> {
                        throttle.fail("signin-failures", key(request));
                        return HttpServer.Response.text(303, "")
                                .header("Location", SIGN_IN + "?error=1")
                                .header("Cache-Control", "no-store");
                    }))
            .mfa(mfa -> mfa.totpService(totp)
                    .secondFactorPage(SECOND_FACTOR)
                    .processingUrl(SECOND_FACTOR)
                    .defaultSuccessUrl("/account"))
            .logout(logout -> logout
                    .logoutRequestMatcher(AntPathRequestMatcher.antMatcher("POST", SIGN_OUT))
                    .logoutSuccessUrl("/"));
        return http.build();
    }

    @Bean
    @Order(4)
    public SecurityFilterChain shopChain(HttpSecurity http,
            @Value("${bazaar.web.hsts:true}") boolean hsts) throws IOException {
        http.securityMatcher("/", "/s/**", "/track/**", "/consent", "/consent/**")
            .csrf(c -> c.disable())
            .addFilterBefore(new WebHeaders(hsts), CsrfFilter.class)
            .addFilterBefore(new PageEtags(), CsrfFilter.class)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /// The account a sign-in attempt names, folded, as the lockout's key.
    static String key(HttpServer.Request request) {
        String u;
        try {
            u = request.param("username");
        } catch (RuntimeException malformed) {
            u = null;
        }
        return u == null ? "" : Ascii.lower(u.trim());
    }

    /// Refuses a password for an account with too many wrong ones lately --
    /// before the password is even looked at, so a right guess during the
    /// lockout is no different from a wrong one.
    static SecurityFilter lockout(final Throttle throttle) {
        return (request, chain) -> {
            if ("POST".equals(request.getMethod()) && SIGN_IN.equals(request.pathFrom(0))
                    && throttle.lockout("signin-failures", key(request))) {
                SecurityExchange.current().setResponseHeader("Cache-Control", "no-store");
                return HttpServer.Response.text(303, "").header("Location", SIGN_IN + "?locked=1");
            }
            return chain.doFilter(request);
        };
    }
}
