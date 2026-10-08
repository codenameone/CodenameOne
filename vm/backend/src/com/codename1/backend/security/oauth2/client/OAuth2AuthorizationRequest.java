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

import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/// One user being sent to an identity provider: where to, and the three values
/// that tie the provider's answer back to this browser -- the `state`, the
/// `nonce` the ID token must repeat, and the PKCE verifier whose hash went with
/// the request. It is kept, between the redirect out and the answer coming
/// back, by an [AuthorizationRequestRepository].
public final class OAuth2AuthorizationRequest {
    private final String registrationId;
    private final String authorizationUri;
    private final String clientId;
    private final String redirectUri;
    private final Set<String> scopes;
    private final String state;
    private final String nonce;
    private final String codeVerifier;
    private final Map<String, Object> additionalParameters;

    private OAuth2AuthorizationRequest(Builder b) {
        this.registrationId = b.registrationId;
        this.authorizationUri = b.authorizationUri;
        this.clientId = b.clientId;
        this.redirectUri = b.redirectUri;
        this.scopes = Collections.unmodifiableSet(new LinkedHashSet<String>(b.scopes));
        this.state = b.state;
        this.nonce = b.nonce;
        this.codeVerifier = b.codeVerifier;
        this.additionalParameters = Collections.unmodifiableMap(
                new LinkedHashMap<String, Object>(b.additionalParameters));
    }

    /// A request through `registration`, with a new `state`, `nonce` and PKCE
    /// verifier of 256 random bits each.
    public static Builder from(ClientRegistration registration, String redirectUri) {
        Builder b = new Builder();
        b.registrationId = registration.getRegistrationId();
        b.authorizationUri = registration.getProviderDetails().getAuthorizationUri();
        b.clientId = registration.getClientId();
        b.redirectUri = redirectUri;
        b.scopes.addAll(registration.getScopes());
        b.state = OAuth2Parameters.random(32);
        b.codeVerifier = OAuth2Parameters.random(32);
        if (registration.getScopes().contains("openid")) {
            b.nonce = OAuth2Parameters.random(32);
        }
        if (registration.getResponseMode() != null) {
            b.additionalParameters.put("response_mode", registration.getResponseMode());
        }
        return b;
    }

    /// A builder that starts as a copy of `request`.
    public static Builder from(OAuth2AuthorizationRequest request) {
        Builder b = new Builder();
        b.registrationId = request.registrationId;
        b.authorizationUri = request.authorizationUri;
        b.clientId = request.clientId;
        b.redirectUri = request.redirectUri;
        b.scopes.addAll(request.scopes);
        b.state = request.state;
        b.nonce = request.nonce;
        b.codeVerifier = request.codeVerifier;
        b.additionalParameters.putAll(request.additionalParameters);
        return b;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public String getClientId() {
        return clientId;
    }

    /// The address the provider is asked to answer at, which the exchange of
    /// the code must repeat exactly.
    public String getRedirectUri() {
        return redirectUri;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public String getState() {
        return state;
    }

    /// The value the ID token must repeat, or null when no ID token is asked
    /// for.
    public String getNonce() {
        return nonce;
    }

    /// The PKCE verifier. It never leaves this server except in the exchange
    /// of the code; the request carries its SHA-256.
    public String getCodeVerifier() {
        return codeVerifier;
    }

    /// What the request carries besides the parameters OAuth2 defines.
    public Map<String, Object> getAdditionalParameters() {
        return additionalParameters;
    }

    /// The address the browser is sent to.
    public String getAuthorizationRequestUri() {
        Map<String, Object> query = new LinkedHashMap<String, Object>();
        query.put("response_type", "code");
        query.put("client_id", clientId);
        if (!scopes.isEmpty()) {
            query.put("scope", OAuth2Parameters.scopes(scopes));
        }
        query.put("state", state);
        query.put("redirect_uri", redirectUri);
        if (nonce != null) {
            query.put("nonce", nonce);
        }
        query.put("code_challenge", OAuth2Parameters.sha256(codeVerifier));
        query.put("code_challenge_method", "S256");
        for (Map.Entry<String, Object> extra : additionalParameters.entrySet()) {
            if (!query.containsKey(extra.getKey())) {
                query.put(extra.getKey(), extra.getValue());
            }
        }
        return OAuth2Parameters.append(authorizationUri, query);
    }

    /// The request as a map of text, numbers and lists: what a session, or a
    /// cookie, can keep.
    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("registrationId", registrationId);
        out.put("authorizationUri", authorizationUri);
        out.put("clientId", clientId);
        out.put("redirectUri", redirectUri);
        out.put("scope", OAuth2Parameters.scopes(scopes));
        out.put("state", state);
        if (nonce != null) {
            out.put("nonce", nonce);
        }
        out.put("codeVerifier", codeVerifier);
        Map<String, Object> extra = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> e : additionalParameters.entrySet()) {
            extra.put(e.getKey(), String.valueOf(e.getValue()));
        }
        out.put("additional", extra);
        return out;
    }

    /// The request [#toMap] wrote, or null when `stored` is not one.
    public static OAuth2AuthorizationRequest fromMap(Map stored) {
        if (stored == null) {
            return null;
        }
        Builder b = new Builder();
        b.registrationId = text(stored, "registrationId");
        b.authorizationUri = text(stored, "authorizationUri");
        b.clientId = text(stored, "clientId");
        b.redirectUri = text(stored, "redirectUri");
        b.scopes.addAll(OAuth2Parameters.scopes(text(stored, "scope")));
        b.state = text(stored, "state");
        b.nonce = text(stored, "nonce");
        b.codeVerifier = text(stored, "codeVerifier");
        Object extra = stored.get("additional");
        if (extra instanceof Map) {
            for (Object entry : ((Map) extra).entrySet()) {
                Map.Entry e = (Map.Entry) entry;
                b.additionalParameters.put(String.valueOf(e.getKey()), e.getValue());
            }
        }
        if (b.registrationId == null || b.authorizationUri == null || b.clientId == null
                || b.redirectUri == null || b.state == null || b.codeVerifier == null) {
            return null;
        }
        return new OAuth2AuthorizationRequest(b);
    }

    private static String text(Map stored, String name) {
        Object value = stored.get(name);
        return value instanceof String ? (String) value : null;
    }

    /// Builds an [OAuth2AuthorizationRequest].
    public static final class Builder {
        private String registrationId;
        private String authorizationUri;
        private String clientId;
        private String redirectUri;
        private final Set<String> scopes = new LinkedHashSet<String>();
        private String state;
        private String nonce;
        private String codeVerifier;
        private final Map<String, Object> additionalParameters =
                new LinkedHashMap<String, Object>();

        Builder() {
        }

        public Builder scopes(Collection<String> scopes) {
            this.scopes.clear();
            this.scopes.addAll(scopes);
            return this;
        }

        /// One more parameter of the request: `prompt`, `login_hint`,
        /// `access_type`. It cannot replace one OAuth2 defines.
        public Builder additionalParameter(String name, Object value) {
            if (value == null) {
                additionalParameters.remove(name);
            } else {
                additionalParameters.put(name, value);
            }
            return this;
        }

        public OAuth2AuthorizationRequest build() {
            return new OAuth2AuthorizationRequest(this);
        }
    }
}
