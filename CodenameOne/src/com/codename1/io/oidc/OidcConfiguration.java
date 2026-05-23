/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.io.oidc;

import java.util.Map;

/// The subset of an OpenID Connect provider's `.well-known/openid-configuration`
/// document that [OidcClient] cares about. Construct directly when you already
/// know the endpoints, or obtain via [OidcClient#discover(String)] which fetches
/// and parses the document.
///
/// All fields are immutable after construction. Use [#newBuilder()] to start
/// from a blank slate; use [#newBuilder(OidcConfiguration)] to derive one from
/// an existing instance.
///
/// @since 8.0
public final class OidcConfiguration {

    private final String issuer;
    private final String authorizationEndpoint;
    private final String tokenEndpoint;
    private final String userInfoEndpoint;
    private final String revocationEndpoint;
    private final String endSessionEndpoint;
    private final String jwksUri;

    private OidcConfiguration(Builder b) {
        this.issuer = b.issuer;
        this.authorizationEndpoint = b.authorizationEndpoint;
        this.tokenEndpoint = b.tokenEndpoint;
        this.userInfoEndpoint = b.userInfoEndpoint;
        this.revocationEndpoint = b.revocationEndpoint;
        this.endSessionEndpoint = b.endSessionEndpoint;
        this.jwksUri = b.jwksUri;
    }

    /// Builds an [OidcConfiguration] from a parsed discovery JSON document.
    /// Only the fields this client needs are extracted; anything else is ignored.
    public static OidcConfiguration fromDiscoveryJson(Map<String, Object> json) {
        if (json == null) {
            throw new IllegalArgumentException("json must not be null");
        }
        Builder b = new Builder();
        b.issuer = stringOrNull(json.get("issuer"));
        b.authorizationEndpoint = stringOrNull(json.get("authorization_endpoint"));
        b.tokenEndpoint = stringOrNull(json.get("token_endpoint"));
        b.userInfoEndpoint = stringOrNull(json.get("userinfo_endpoint"));
        b.revocationEndpoint = stringOrNull(json.get("revocation_endpoint"));
        b.endSessionEndpoint = stringOrNull(json.get("end_session_endpoint"));
        b.jwksUri = stringOrNull(json.get("jwks_uri"));
        return b.build();
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    public String getRevocationEndpoint() {
        return revocationEndpoint;
    }

    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(OidcConfiguration source) {
        Builder b = new Builder();
        b.issuer = source.issuer;
        b.authorizationEndpoint = source.authorizationEndpoint;
        b.tokenEndpoint = source.tokenEndpoint;
        b.userInfoEndpoint = source.userInfoEndpoint;
        b.revocationEndpoint = source.revocationEndpoint;
        b.endSessionEndpoint = source.endSessionEndpoint;
        b.jwksUri = source.jwksUri;
        return b;
    }

    private static String stringOrNull(Object o) {
        return o instanceof String ? (String) o : null;
    }

    /// Fluent builder for [OidcConfiguration].
    public static final class Builder {
        private String issuer;
        private String authorizationEndpoint;
        private String tokenEndpoint;
        private String userInfoEndpoint;
        private String revocationEndpoint;
        private String endSessionEndpoint;
        private String jwksUri;

        public Builder issuer(String v) {
            this.issuer = v;
            return this;
        }

        public Builder authorizationEndpoint(String v) {
            this.authorizationEndpoint = v;
            return this;
        }

        public Builder tokenEndpoint(String v) {
            this.tokenEndpoint = v;
            return this;
        }

        public Builder userInfoEndpoint(String v) {
            this.userInfoEndpoint = v;
            return this;
        }

        public Builder revocationEndpoint(String v) {
            this.revocationEndpoint = v;
            return this;
        }

        public Builder endSessionEndpoint(String v) {
            this.endSessionEndpoint = v;
            return this;
        }

        public Builder jwksUri(String v) {
            this.jwksUri = v;
            return this;
        }

        public OidcConfiguration build() {
            if (authorizationEndpoint == null) {
                throw new IllegalStateException("authorizationEndpoint is required");
            }
            return new OidcConfiguration(this);
        }
    }
}
