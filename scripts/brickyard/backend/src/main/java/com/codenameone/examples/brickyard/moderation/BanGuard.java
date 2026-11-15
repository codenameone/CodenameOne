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
package com.codenameone.examples.brickyard.moderation;

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.SecurityContextHolder;
import com.codename1.backend.security.SecurityFilter;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2Authorization;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2AuthorizationService;
import java.util.LinkedHashMap;
import java.util.Map;

/// The filters that make a ban global. A banned account cannot sign in,
/// cannot trade a refresh token or an authorization code for a new access
/// token, and cannot change anything under `/api` -- every request but a
/// read, apart from filing an appeal. Each refusal says why and for how long.
public final class BanGuard {
    /// The one write a banned account may still make.
    public static final String APPEALS = "/api/me/appeals";

    private BanGuard() {
    }

    /// For the API chain, after the bearer token is read.
    public static SecurityFilter api(final Sanctions sanctions) {
        return (request, chain) -> {
            String method = request.getMethod();
            if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)
                    || APPEALS.equals(path(request))) {
                return chain.doFilter(request);
            }
            Authentication who = SecurityContextHolder.getContext().getAuthentication();
            String why = who == null || !who.isAuthenticated() ? null : sanctions.banMessage(who.getName());
            if (why != null) {
                Map<String, Object> body = new LinkedHashMap<String, Object>();
                body.put("status", Integer.valueOf(403));
                body.put("error", "banned");
                body.put("message", why);
                return HttpServer.Response.jsonValue(403, body);
            }
            return chain.doFilter(request);
        };
    }

    /// For the sign-in chain: the password form and the token endpoint.
    public static SecurityFilter signIn(final Sanctions sanctions, final OAuth2AuthorizationService grants) {
        return (request, chain) -> {
            if (!"POST".equals(request.getMethod())) {
                return chain.doFilter(request);
            }
            String path = path(request);
            String why = null;
            if ("/login".equals(path)) {
                String username = form(request).get("username");
                why = username == null ? null : sanctions.banMessage(username.trim());
                if (why != null) {
                    Map<String, Object> body = new LinkedHashMap<String, Object>();
                    body.put("signedIn", Boolean.FALSE);
                    body.put("error", "banned");
                    body.put("message", why);
                    return HttpServer.Response.jsonValue(403, body).header("Cache-Control", "no-store");
                }
            } else if ("/oauth2/token".equals(path)) {
                Map<String, String> form = form(request);
                String grant = form.get("grant_type");
                String principal = null;
                if ("refresh_token".equals(grant)) {
                    principal = principalOf(grants, OAuth2AuthorizationService.REFRESH_TOKEN,
                            form.get("refresh_token"));
                } else if ("authorization_code".equals(grant)) {
                    principal = principalOf(grants, OAuth2AuthorizationService.CODE, form.get("code"));
                }
                why = principal == null ? null : sanctions.banMessage(principal);
                if (why != null) {
                    // The token endpoint's own error shape, so an OAuth client
                    // reads it as a refused grant and signs the user out.
                    Map<String, Object> body = new LinkedHashMap<String, Object>();
                    body.put("error", "invalid_grant");
                    body.put("error_description", why);
                    return HttpServer.Response.jsonValue(400, body).header("Cache-Control", "no-store");
                }
            }
            return chain.doFilter(request);
        };
    }

    private static String principalOf(OAuth2AuthorizationService grants, String kind, String presented) {
        if (presented == null || presented.length() == 0) {
            return null;
        }
        OAuth2AuthorizationService.StoredToken stored = grants.findToken(kind, OAuth2Parameters.sha256(presented));
        if (stored == null) {
            return null;
        }
        OAuth2Authorization grant = grants.findById(stored.getAuthorizationId());
        return grant == null ? null : grant.getPrincipalName();
    }

    private static Map<String, String> form(HttpServer.Request request) {
        String body = request.getBody();
        return OAuth2Parameters.parse(body == null ? "" : body);
    }

    private static String path(HttpServer.Request request) {
        String target = request.getTarget();
        int q = target == null ? -1 : target.indexOf('?');
        return q < 0 ? target : target.substring(0, q);
    }
}
