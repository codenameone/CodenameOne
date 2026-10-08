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
package com.codename1.backend.security.oauth2.client;

import com.codename1.backend.Base64Url;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.security.SecurityExchange;
import com.codename1.backend.security.crypto.SignedTokens;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import java.io.IOException;

/// Keeps the request in a cookie of its own, for a provider that answers with
/// a form the browser posts ([ClientRegistration#FORM_POST]): Sign in with
/// Apple.
///
/// That answer is a `POST` from the provider's page, and a browser does not
/// send a `SameSite=Lax` cookie with one -- so the session, and a request kept
/// in it, are not there when the answer arrives. This cookie is
/// `SameSite=None; Secure; HttpOnly`, lasts five minutes, and uses a `__Host-`
/// name with `Path=/` and no `Domain`. Browsers therefore reject a sibling
/// subdomain's attempt to plant this cookie for the parent domain. It is
/// signed with HMAC-SHA256: a value this server did not
/// write, or wrote more than five minutes ago, is no request at all. Its
/// `state` is what ties the posted answer to this browser.
///
/// The cookie is signed, not encrypted. It holds the PKCE verifier, which the
/// user's own browser may read; what protects the exchange from that browser's
/// user is the client secret a `form_post` provider also requires.
///
/// Every process that may receive the answer needs the same key: set
/// `cn1.security.oauth2.client.cookie-secret`. Without it each process makes a
/// key of its own when it starts.
public final class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository {
    /// The setting that holds the key, as at least 32 characters of text.
    public static final String SECRET = "cn1.security.oauth2.client.cookie-secret";
    /// The cookie's name.
    public static final String COOKIE = "__Host-cn1_oauth2_authorization_request";
    private static final String PURPOSE = "oauth2-authorization-request";
    private static final long SECONDS = 300;

    private final SignedTokens tokens;

    /// @param secret at least 32 bytes
    /// @param callbackPath the callback route, beginning with `/`; the cookie always
    /// uses `Path=/`, as required for browser-enforced host binding
    public CookieOAuth2AuthorizationRequestRepository(byte[] secret, String callbackPath) {
        if (callbackPath == null || !callbackPath.startsWith("/")) {
            throw new IllegalArgumentException("callbackPath must begin with /");
        }
        this.tokens = new SignedTokens(secret);
    }

    /// The signer; for tests that move the clock.
    public SignedTokens getSignedTokens() {
        return tokens;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServer.Request request) {
        String subject = Base64Url.encode(OAuth2Parameters.utf8(
                Json.write(authorizationRequest.toMap())));
        write(tokens.create(PURPOSE, subject, SECONDS), SECONDS);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServer.Request request) {
        String cookie = request.getCookie(COOKIE);
        if (cookie == null || cookie.length() == 0) {
            return null;
        }
        write("", 0);
        String subject = tokens.verify(PURPOSE, cookie);
        if (subject == null) {
            return null;
        }
        try {
            return OAuth2AuthorizationRequest.fromMap(Json.parseObject(
                    OAuth2Parameters.string(Base64Url.decode(subject))));
        } catch (IOException malformed) {
            return null;
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    private void write(String value, long maxAge) {
        SecurityExchange exchange = SecurityExchange.current();
        if (exchange != null) {
            exchange.addResponseHeader("Set-Cookie", COOKIE + "=" + value + "; Path=/"
                    + "; Max-Age=" + maxAge + "; Secure; HttpOnly; SameSite=None");
        }
    }
}
