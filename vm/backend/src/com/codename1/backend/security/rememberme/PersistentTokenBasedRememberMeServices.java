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
package com.codename1.backend.security.rememberme;

import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.LogoutHandler;
import com.codename1.backend.security.RememberMeAuthenticationToken;
import com.codename1.backend.security.SecurityExchange;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.core.userdetails.UsernameNotFoundException;
import java.io.IOException;

/// Remember-me with a series and a rotating token.
///
/// The cookie is `series:token`, both random. The server keeps the series and
/// a SHA-256 of the token. Presenting the cookie signs the user in and replaces
/// the token. For ten seconds after rotation, parallel requests can still use
/// the immediately preceding token without rotating again or overwriting the
/// winning response's cookie. The grace is stored with the token, so it also
/// works across servers sharing a repository. An older or unrelated token
/// deletes every remembered sign-in of that user as a possible cookie theft.
///
/// A cookie issued by a sign-in that passed a second factor is recorded as
/// such, and only such a cookie signs in a user who has one; see
/// [com.codename1.backend.security.MfaConfigurer].
///
/// The cookie is `HttpOnly`, `SameSite=Lax` unless changed, and `Secure` when
/// the request that set it was.
public final class PersistentTokenBasedRememberMeServices
        implements RememberMeServices, LogoutHandler {
    /// The cookie's name unless changed.
    public static final String DEFAULT_COOKIE_NAME = "remember-me";
    /// The form field that asks to be remembered unless changed.
    public static final String DEFAULT_PARAMETER = "remember-me";
    /// Two weeks, as in Spring Security.
    public static final int TWO_WEEKS_S = 1209600;
    /// What the series of a cookie issued after a second factor starts with.
    /// The series is the server's own record -- a cookie cannot name one that
    /// was not issued -- so how a sign-in was made is kept there, and every
    /// [PersistentTokenRepository] keeps it without knowing. A `.` is in no
    /// series otherwise.
    private static final String AFTER_SECOND_FACTOR = "2f.";
    private static final long ROTATION_GRACE_MILLIS = 10000L;

    private final String key;
    private final UserDetailsService userDetailsService;
    private final PersistentTokenRepository tokenRepository;
    private String cookieName = DEFAULT_COOKIE_NAME;
    private String parameter = DEFAULT_PARAMETER;
    private int tokenValiditySeconds = TWO_WEEKS_S;
    private boolean alwaysRemember;
    private Boolean useSecureCookie;
    private String sameSite = "Lax";
    private Clock clock = Clock.SYSTEM;

    /// @param key what identifies the tokens this makes; any text
    public PersistentTokenBasedRememberMeServices(String key, UserDetailsService userDetailsService,
                                                  PersistentTokenRepository tokenRepository) {
        if (key == null || key.length() == 0 || userDetailsService == null
                || tokenRepository == null) {
            throw new IllegalArgumentException("A key, a UserDetailsService and a "
                    + "PersistentTokenRepository are required");
        }
        this.key = key;
        this.userDetailsService = userDetailsService;
        this.tokenRepository = tokenRepository;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = name(cookieName, "cookieName");
    }

    public void setParameter(String parameter) {
        this.parameter = name(parameter, "parameter");
    }

    /// How long a cookie that is not used stays good; two weeks unless set.
    public void setTokenValiditySeconds(int tokenValiditySeconds) {
        if (tokenValiditySeconds < 1) {
            throw new IllegalArgumentException("tokenValiditySeconds must be positive");
        }
        this.tokenValiditySeconds = tokenValiditySeconds;
    }

    /// Remembers every user who signs in, whether or not they asked.
    public void setAlwaysRemember(boolean alwaysRemember) {
        this.alwaysRemember = alwaysRemember;
    }

    /// Whether the cookie is `Secure`; null, the default, for "when the request
    /// was".
    public void setUseSecureCookie(Boolean useSecureCookie) {
        this.useSecureCookie = useSecureCookie;
    }

    /// The cookie's `SameSite`: `Lax`, `Strict` or `None`; null for none.
    public void setSameSite(String sameSite) {
        if (sameSite != null && !"Lax".equals(sameSite) && !"Strict".equals(sameSite)
                && !"None".equals(sameSite)) {
            throw new IllegalArgumentException("SameSite is Lax, Strict or None: " + sameSite);
        }
        this.sameSite = sameSite;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public String getCookieName() {
        return cookieName;
    }

    public String getParameter() {
        return parameter;
    }

    private static String name(String value, String what) {
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException(what + " cannot be null or empty");
        }
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_')) {
                throw new IllegalArgumentException(what + " \"" + value + "\" may hold letters, "
                        + "digits, - and _ only");
            }
        }
        return value;
    }

    // ------------------------------------------------------------ recognizing

    @Override
    public Authentication autoLogin(HttpServer.Request request) {
        String cookie = request.getCookie(cookieName);
        if (cookie == null || cookie.length() == 0) {
            return null;
        }
        int colon = cookie.indexOf(':');
        if (colon <= 0 || colon == cookie.length() - 1 || cookie.length() > 200
                || cookie.indexOf(':', colon + 1) >= 0) {
            cancelCookie();
            return null;
        }
        String series = cookie.substring(0, colon);
        String presented = hash(cookie.substring(colon + 1));
        PersistentRememberMeToken stored = tokenRepository.getTokenForSeries(series);
        if (stored == null) {
            cancelCookie();
            return null;
        }
        long now = clock.currentTimeMillis();
        if (!sameHash(presented, stored.getTokenHash()) && !previousWithinGrace(stored, presented, now)) {
            return stolen(stored);
        }
        if (stored.getLastUsed() + tokenValiditySeconds * 1000L < now) {
            tokenRepository.removeToken(series);
            cancelCookie();
            return null;
        }
        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(stored.getUsername());
        } catch (UsernameNotFoundException gone) {
            user = null;
        }
        if (user == null || !user.isEnabled() || !user.isAccountNonLocked()
                || !user.isAccountNonExpired() || !user.isCredentialsNonExpired()) {
            // An account that could not sign in is not signed in by its cookie.
            tokenRepository.removeToken(series);
            cancelCookie();
            return null;
        }
        if (!withinGrace(stored, now)) {
            String next = random();
            if (!tokenRepository.updateToken(series, stored.getTokenHash(), hash(next), now)) {
                PersistentRememberMeToken winner = tokenRepository.getTokenForSeries(series);
                if (winner == null) {
                    cancelCookie();
                    return null;
                }
                if (!stored.getUsername().equals(winner.getUsername())
                        || !previousWithinGrace(winner, presented, clock.currentTimeMillis())) {
                    return stolen(stored);
                }
                // The other response sends the new cookie. Sending this request's
                // old cookie (or cancelling it) would race with that response.
            } else {
                setCookie(series + ":" + next);
            }
        }
        return new RememberMeAuthenticationToken(key, user, user.getAuthorities(),
                series.startsWith(AFTER_SECOND_FACTOR));
    }

    private static boolean withinGrace(PersistentRememberMeToken stored, long now) {
        return stored.getPreviousTokenHash() != null && now >= stored.getLastUsed()
                && now - stored.getLastUsed() < ROTATION_GRACE_MILLIS;
    }

    private static boolean previousWithinGrace(PersistentRememberMeToken stored, String presented, long now) {
        return withinGrace(stored, now) && sameHash(presented, stored.getPreviousTokenHash());
    }

    private Authentication stolen(PersistentRememberMeToken stored) {
        tokenRepository.removeUserTokens(stored.getUsername());
        cancelCookie();
        System.err.println("cn1: a remember-me cookie of " + stored.getUsername() + " was "
                + "presented with a token that had already been used, which means it was "
                + "copied; every remembered sign-in of that user has been deleted");
        return null;
    }

    // ---------------------------------------------------------------- issuing

    /// Whether `request` asks for the user to be remembered: this is set to
    /// remember always, the request's exchange says so, or the form field is
    /// `true`, `on`, `yes` or `1`.
    public boolean rememberMeRequested(HttpServer.Request request) {
        if (alwaysRemember) {
            return true;
        }
        SecurityExchange exchange = SecurityExchange.current();
        if (exchange != null && Boolean.TRUE.equals(exchange.getAttribute(REQUESTED_ATTRIBUTE))) {
            return true;
        }
        String value;
        try {
            value = request.param(parameter);
        } catch (RuntimeException malformed) {
            value = null;
        }
        return value != null && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on")
                || value.equalsIgnoreCase("yes") || "1".equals(value));
    }

    @Override
    public void loginSuccess(HttpServer.Request request, Authentication successfulAuthentication) {
        if (successfulAuthentication == null || !rememberMeRequested(request)) {
            return;
        }
        SecurityExchange exchange = SecurityExchange.current();
        boolean secondFactor = exchange != null
                && Boolean.TRUE.equals(exchange.getAttribute(SECOND_FACTOR_ATTRIBUTE));
        String series = secondFactor ? AFTER_SECOND_FACTOR + random() : random();
        String token = random();
        tokenRepository.createNewToken(new PersistentRememberMeToken(
                successfulAuthentication.getName(), series, hash(token),
                clock.currentTimeMillis()));
        setCookie(series + ":" + token);
    }

    @Override
    public void loginFail(HttpServer.Request request) {
        String cookie = request.getCookie(cookieName);
        if (cookie != null) {
            int colon = cookie.indexOf(':');
            if (colon > 0 && cookie.length() <= 200 && cookie.indexOf(':', colon + 1) < 0) {
                // A cookie withdrawn by a later authentication check (such as MFA)
                // must remain unusable even if the client ignores Set-Cookie.
                tokenRepository.removeToken(cookie.substring(0, colon));
            }
            cancelCookie();
        }
    }

    /// Signing out forgets the user in every browser they were remembered in,
    /// as Spring Security does, and withdraws this browser's cookie.
    @Override
    public void logout(HttpServer.Request request, Authentication authentication) {
        if (authentication != null) {
            tokenRepository.removeUserTokens(authentication.getName());
        }
        if (request.getCookie(cookieName) != null) {
            cancelCookie();
        }
    }

    // ----------------------------------------------------------------- cookie

    private void setCookie(String value) {
        writeCookie(value, tokenValiditySeconds);
    }

    private void cancelCookie() {
        writeCookie("", 0);
    }

    private void writeCookie(String value, int maxAge) {
        SecurityExchange exchange = SecurityExchange.current();
        if (exchange == null) {
            // Not under a chain: there is no response to put a cookie on.
            return;
        }
        boolean secure = useSecureCookie != null ? useSecureCookie.booleanValue()
                : exchange.isSecure();
        StringBuilder cookie = new StringBuilder(cookieName).append('=').append(value)
                .append("; Path=/; Max-Age=").append(maxAge).append("; HttpOnly");
        if (secure) {
            cookie.append("; Secure");
        }
        if (sameSite != null) {
            cookie.append("; SameSite=").append(sameSite);
        }
        exchange.addResponseHeader("Set-Cookie", cookie.toString());
    }

    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();

    /// 144 random bits as 24 characters a cookie can carry unquoted.
    private static String random() {
        byte[] bytes;
        try {
            bytes = Crypto.randomBytes(18);
        } catch (IOException err) {
            throw new IllegalStateException("No random source: " + err.getMessage(), err);
        }
        StringBuilder out = new StringBuilder(24);
        for (int iter = 0 ; iter < bytes.length ; iter += 3) {
            int bits = ((bytes[iter] & 0xff) << 16) | ((bytes[iter + 1] & 0xff) << 8)
                    | (bytes[iter + 2] & 0xff);
            out.append(ALPHABET[(bits >> 18) & 63]).append(ALPHABET[(bits >> 12) & 63])
               .append(ALPHABET[(bits >> 6) & 63]).append(ALPHABET[bits & 63]);
        }
        return out.toString();
    }

    private static byte[] ascii(String value) {
        byte[] out = new byte[value.length()];
        for (int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte) value.charAt(iter);
        }
        return out;
    }

    /// The SHA-256 of a token, in hex: what the repository keeps.
    public static String hash(String token) {
        byte[] digest = Crypto.sha256(ascii(token));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append("0123456789abcdef".charAt((b >> 4) & 15))
                    .append("0123456789abcdef".charAt(b & 15));
        }
        return hex.toString();
    }

    private static boolean sameHash(String a, String b) {
        return Crypto.equalsConstantTime(ascii(a), ascii(b));
    }
}
