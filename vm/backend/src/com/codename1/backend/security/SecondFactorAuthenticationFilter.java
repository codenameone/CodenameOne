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
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.mfa.RecoveryCodeService;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.security.ratelimit.RateLimitKeyResolver;
import com.codename1.backend.security.ratelimit.RateLimitKeys;
import com.codename1.backend.security.ratelimit.RateLimiter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Asks a user who has a second factor for it, between their password being
/// accepted and their being signed in.
///
/// It is the chain's [SecondFactorPolicy]: handed an authentication whose first
/// factor has passed, it signs nobody in. It notes in the session who is
/// waiting -- a marker that is not a security context, and that lasts five
/// minutes unless set otherwise -- and redirects to the page that asks for the
/// code. Until the code arrives every request of that session is anonymous.
///
/// It is also the filter that takes the code, at `POST /login/mfa` unless
/// changed, in the field `code`: a one-time code from the user's authenticator
/// app, or one of their recovery codes. A right one completes the sign-in
/// through the chain's [SessionSignIn] -- the session id changes, the context
/// is stored, remember-me is issued if it was asked for at the first step, and
/// the user goes where they were going.
///
/// Wrong codes are counted twice: for the user, whatever address and session
/// they come from, and for the user at the client's network. Too many of
/// either are answered 429 until time has passed. See [MfaConfigurer] for the
/// numbers, and for what the two counts together do and do not promise.
public final class SecondFactorAuthenticationFilter implements SecurityFilter, SecondFactorPolicy {
    /// The session attribute the pending sign-in is kept under.
    public static final String PENDING = "CN1_SECURITY_SECOND_FACTOR_PENDING";

    private final TotpService totp;
    private final RecoveryCodeService recoveryCodes;
    private final String page;
    private final boolean servePage;
    private final String processingUrl;
    private final String codeParameter;
    private final long pendingMillis;
    /// Counts a user's wrong one-time codes from everywhere; null to count none.
    private final RateLimiter attempts;
    /// Counts wrong codes for a user at one network, one-time codes and
    /// recovery codes apart; null to count none.
    private final RateLimiter addressAttempts;
    private final RateLimitKeyResolver network = RateLimitKeys.clientNetwork();
    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler expiredHandler;
    private final Clock clock;
    private SessionSignIn signIn;
    private HttpSessionSecurityContextRepository kinds;

    SecondFactorAuthenticationFilter(TotpService totp, RecoveryCodeService recoveryCodes,
            String page, boolean servePage, String processingUrl, String codeParameter,
            long pendingMillis, RateLimiter attempts, RateLimiter addressAttempts,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler expiredHandler, Clock clock) {
        this.totp = totp;
        this.recoveryCodes = recoveryCodes;
        this.page = page;
        this.servePage = servePage;
        this.processingUrl = processingUrl;
        this.codeParameter = codeParameter;
        this.pendingMillis = pendingMillis;
        this.attempts = attempts;
        this.addressAttempts = addressAttempts;
        this.successHandler = successHandler;
        this.expiredHandler = expiredHandler;
        this.clock = clock;
    }

    /// Given once the chain's sign-in exists, which is after this is installed
    /// as its policy.
    void signIn(SessionSignIn signIn) {
        this.signIn = signIn;
    }

    /// The chain's session repository, when it has one: what knows how to keep
    /// the kind of a sign-in while it waits here, so that a user who came
    /// through another provider is that provider's user after the code too.
    void kinds(HttpSessionSecurityContextRepository repository) {
        this.kinds = repository;
    }

    // ------------------------------------------------------------ the policy

    @Override
    public HttpServer.Response intercept(HttpServer.Request request,
            Authentication authentication, boolean rememberMe) {
        if (authentication == null || !totp.isEnabled(authentication.getName())) {
            return null;
        }
        Map<String, Object> pending = new LinkedHashMap<String, Object>();
        pending.put("name", authentication.getName());
        List<String> authorities = new ArrayList<String>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            authorities.add(authority.getAuthority());
        }
        pending.put("authorities", authorities);
        pending.put("expires", Long.valueOf(clock.currentTimeMillis() + pendingMillis));
        pending.put("remember", Boolean.valueOf(rememberMe));
        if (kinds != null) {
            // Kept as the repository would keep it, under a name of the
            // marker's own: nothing reads this as a security context.
            pending.put("authentication", kinds.toMap(authentication));
        }
        HttpSession session = request.getSession(true);
        // Whatever the session held of an earlier user is not this one's, and
        // an id handed out before a password was accepted is not kept either.
        session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        session.changeSessionId();
        session.setAttribute(PENDING, pending);
        return Responses.redirect(page);
    }

    @Override
    public boolean requires(Authentication authentication) {
        return authentication != null && totp.isEnabled(authentication.getName());
    }

    /// A passkey that verified the user signed them in. Whoever ran the count
    /// of wrong codes up was not them, or no longer matters: it is forgotten,
    /// so their next one-time code is not refused for somebody else's guesses.
    @Override
    public void satisfied(HttpServer.Request request, Authentication authentication) {
        if (authentication != null && totp.isEnabled(authentication.getName())) {
            forget(asciiLower(authentication.getName()), from(request));
        }
    }

    /// The client's network as the counts name it.
    private String from(HttpServer.Request request) {
        String key = network.resolve(request);
        return key == null ? "net:none" : key;
    }

    /// Forgets what was counted against `who`: their own count, and the two
    /// kept for them at `from`. The counts for them at other networks run out
    /// by themselves; there is no telling which those are.
    private void forget(String who, String from) {
        if (attempts != null) {
            attempts.reset("mfa:" + who);
        }
        if (addressAttempts != null) {
            addressAttempts.reset("mfa:" + who + "|" + from);
            addressAttempts.reset("mfa-recovery:" + who + "|" + from);
        }
    }

    // ------------------------------------------------------------ the filter

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        String path = SecurityExchange.path(request);
        if (servePage && "GET".equals(request.getMethod()) && page.equals(path)) {
            return page(request);
        }
        if (!"POST".equals(request.getMethod()) || !processingUrl.equals(path)) {
            return chain.doFilter(request);
        }
        HttpSession session = request.getSession(false);
        Object stored = session == null ? null : session.getAttribute(PENDING);
        if (!(stored instanceof Map)) {
            return expired(request, session);
        }
        Map pending = (Map) stored;
        Object name = pending.get("name");
        Object expires = pending.get("expires");
        if (!(name instanceof String) || !(expires instanceof Number)
                || ((Number) expires).longValue() < clock.currentTimeMillis()) {
            return expired(request, session);
        }
        String user = (String) name;
        String code = Responses.param(request, codeParameter);
        String who = asciiLower(user);
        String from = from(request);
        // What is written as a recovery code is counted apart from the one-time
        // codes, and only at this network: it is the way in for a user whose
        // one-time codes somebody else has used up the attempts at, so those
        // attempts must not close it. A recovery code is one of 31^10; guesses
        // at it need no count for the user as a whole to be hopeless.
        boolean recovery = recoveryCodes != null && RecoveryCodeService.isCodeShaped(code);
        // Counted before the code is looked at, so that guesses sent together
        // cannot each be let through as the last one allowed; a right one hands
        // its counts back below. At the network first: a client that has used
        // its share up there is refused without touching the user's count,
        // which is what stops one client spending what another needs.
        String here = (recovery ? "mfa-recovery:" : "mfa:") + who + "|" + from;
        if (addressAttempts != null && !addressAttempts.tryAcquire(here)) {
            return tooMany(addressAttempts.retryAfterSeconds(here));
        }
        // Under the user and not the session or the address: whoever has the
        // password can start as many sessions from as many places as they
        // like, and must not get a fresh allowance with each.
        String counted = "mfa:" + who;
        if (!recovery && attempts != null && !attempts.tryAcquire(counted)) {
            return tooMany(attempts.retryAfterSeconds(counted));
        }
        boolean accepted = code != null && (totp.verify(user, code)
                || (recoveryCodes != null && recoveryCodes.consume(user, code)));
        if (!accepted) {
            return Responses.redirect(page + "?error");
        }
        // Only wrong codes stay counted, and a user who got in starts afresh.
        forget(who, from);
        session.removeAttribute(PENDING);
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        Object listed = pending.get("authorities");
        if (listed instanceof List) {
            for (Object authority : (List) listed) {
                if (authority instanceof String && ((String) authority).length() > 0) {
                    authorities.add(new SimpleGrantedAuthority((String) authority));
                }
            }
        }
        Authentication authentication = null;
        Object kept = pending.get("authentication");
        if (kinds != null && kept instanceof Map) {
            Authentication made = kinds.fromMap((Map) kept);
            // The one that waited, and no other: the name the code was checked
            // against is the name that signs in.
            if (made != null && user.equals(made.getName())) {
                authentication = made;
            }
        }
        if (authentication == null) {
            authentication = UsernamePasswordAuthenticationToken.authenticated(
                    new User(user, "", authorities), null, authorities);
        }
        SessionSignIn complete = signIn;
        if (complete == null) {
            throw new IllegalStateException("The second factor filter was not given the "
                    + "chain's sign-in; it is installed by http.mfa(...)");
        }
        return complete.complete(request, authentication,
                Boolean.TRUE.equals(pending.get("remember")), successHandler, true);
    }

    private static HttpServer.Response tooMany(long wait) {
        return Responses.status(429, "Too Many Requests")
                .header("Retry-After", String.valueOf(wait < 1 ? 1 : wait));
    }

    /// `value` with `A` to `Z` folded, by hand: a name is compared this way by
    /// the stores, and `toLowerCase()` follows the device's locale.
    private static String asciiLower(String value) {
        StringBuilder sb = null;
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if (c >= 'A' && c <= 'Z') {
                if (sb == null) {
                    sb = new StringBuilder(value);
                }
                sb.setCharAt(iter, (char) (c + ('a' - 'A')));
            }
        }
        return sb == null ? value : sb.toString();
    }

    private HttpServer.Response expired(HttpServer.Request request, HttpSession session)
            throws Exception {
        if (session != null && session.getAttribute(PENDING) != null) {
            session.removeAttribute(PENDING);
        }
        return expiredHandler.onAuthenticationFailure(request,
                new InsufficientAuthenticationException("The sign-in was not completed in time; "
                        + "start again"));
    }

    private HttpServer.Response page(HttpServer.Request request) {
        boolean error = request.queryParam("error") != null;
        CsrfToken token = CsrfFilter.getToken(request);
        StringBuilder html = new StringBuilder(1024);
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html.append("<title>Enter your code</title>\n</head>\n<body>\n");
        html.append("<form method=\"post\" action=\"");
        html.append(Responses.escape(processingUrl));
        html.append("\">\n<h2>Enter your code</h2>\n");
        if (error) {
            html.append("<p role=\"alert\">That code was not accepted</p>\n");
        }
        html.append("<p><label for=\"code\">The code from your authenticator app, or a "
                + "recovery code</label>\n<input type=\"text\" id=\"code\" name=\"");
        html.append(Responses.escape(codeParameter));
        html.append("\" required autofocus autocomplete=\"one-time-code\" "
                + "inputmode=\"numeric\"></p>\n");
        if (token != null) {
            html.append("<input type=\"hidden\" name=\"")
                .append(Responses.escape(token.getParameterName())).append("\" value=\"")
                .append(Responses.escape(token.getToken())).append("\">\n");
        }
        html.append("<button type=\"submit\">Continue</button>\n</form>\n</body>\n</html>\n");
        return Responses.html(200, html.toString());
    }
}
