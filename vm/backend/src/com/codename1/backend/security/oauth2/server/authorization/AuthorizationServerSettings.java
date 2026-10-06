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
package com.codename1.backend.security.oauth2.server.authorization;

/// Who the authorization server is and where its endpoints are.
///
/// ```java
/// http.authorizationServer(as -> as.settings(AuthorizationServerSettings.builder()
///         .issuer("https://id.example.com").build()));
/// ```
///
/// The issuer is the address clients reach this server at, with no trailing
/// slash; it is the `iss` of every token and the base of every address in the
/// metadata. It can also be given as `cn1.security.authorizationserver.issuer`.
/// Outside a development profile it must be given one way or the other: an
/// issuer read off a request is whatever the request's `Host` header said. On
/// a development profile, with neither, it is taken from the request.
public final class AuthorizationServerSettings {
    /// The setting that holds the issuer.
    public static final String ISSUER = "cn1.security.authorizationserver.issuer";
    /// The setting that holds the default audience; see
    /// [Builder#defaultAudience].
    public static final String AUDIENCE = "cn1.security.authorizationserver.audience";

    private final String issuer;
    private final String defaultAudience;
    private final String authorizationEndpoint;
    private final String tokenEndpoint;
    private final String jwkSetEndpoint;
    private final String tokenRevocationEndpoint;
    private final String deviceAuthorizationEndpoint;
    private final String deviceVerificationEndpoint;
    private final String oidcUserInfoEndpoint;

    private AuthorizationServerSettings(Builder b) {
        this.issuer = b.issuer;
        this.defaultAudience = b.defaultAudience;
        this.authorizationEndpoint = b.authorizationEndpoint;
        this.tokenEndpoint = b.tokenEndpoint;
        this.jwkSetEndpoint = b.jwkSetEndpoint;
        this.tokenRevocationEndpoint = b.tokenRevocationEndpoint;
        this.deviceAuthorizationEndpoint = b.deviceAuthorizationEndpoint;
        this.deviceVerificationEndpoint = b.deviceVerificationEndpoint;
        this.oidcUserInfoEndpoint = b.oidcUserInfoEndpoint;
    }

    public static Builder builder() {
        return new Builder();
    }

    /// The issuer, or null to read it from the configuration.
    public String getIssuer() {
        return issuer;
    }

    /// The `aud` of an access token whose request named no `resource`, or
    /// null for the configured one, and the issuer when there is none.
    public String getDefaultAudience() {
        return defaultAudience;
    }

    /// `/oauth2/authorize` unless set.
    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    /// `/oauth2/token` unless set.
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    /// `/oauth2/jwks` unless set.
    public String getJwkSetEndpoint() {
        return jwkSetEndpoint;
    }

    /// `/oauth2/revoke` unless set.
    public String getTokenRevocationEndpoint() {
        return tokenRevocationEndpoint;
    }

    /// `/oauth2/device_authorization` unless set.
    public String getDeviceAuthorizationEndpoint() {
        return deviceAuthorizationEndpoint;
    }

    /// `/oauth2/device_verification` unless set.
    public String getDeviceVerificationEndpoint() {
        return deviceVerificationEndpoint;
    }

    /// `/userinfo` unless set.
    public String getOidcUserInfoEndpoint() {
        return oidcUserInfoEndpoint;
    }

    /// Builds an [AuthorizationServerSettings].
    public static final class Builder {
        private String issuer;
        private String defaultAudience;
        private String authorizationEndpoint = "/oauth2/authorize";
        private String tokenEndpoint = "/oauth2/token";
        private String jwkSetEndpoint = "/oauth2/jwks";
        private String tokenRevocationEndpoint = "/oauth2/revoke";
        private String deviceAuthorizationEndpoint = "/oauth2/device_authorization";
        private String deviceVerificationEndpoint = "/oauth2/device_verification";
        private String oidcUserInfoEndpoint = "/userinfo";

        Builder() {
        }

        private static String path(String value, String what) {
            if (value == null || !value.startsWith("/") || value.startsWith("//")
                    || value.indexOf('?') >= 0 || value.indexOf('#') >= 0) {
                throw new IllegalArgumentException(what + " must be a path on this server, "
                        + "starting with one / and without a query: " + value);
            }
            return value;
        }

        public Builder issuer(String issuer) {
            this.issuer = validIssuer(issuer);
            return this;
        }

        /// The resource server an access token is for when the request that
        /// had it issued named none: its `aud` (RFC 9068). Also given as
        /// `cn1.security.authorizationserver.audience`. Unless set either
        /// way it is the issuer, which suits a server that issues tokens for
        /// its own API.
        ///
        /// A request names a resource server with the `resource` parameter
        /// (RFC 8707), and the token is then for that one instead. Who the
        /// token was issued to is its `client_id` claim, never its audience.
        public Builder defaultAudience(String defaultAudience) {
            if (!RegisteredClient.isResource(defaultAudience)) {
                throw new IllegalArgumentException("An audience is an absolute address with "
                        + "no fragment: " + defaultAudience);
            }
            this.defaultAudience = defaultAudience;
            return this;
        }

        public Builder authorizationEndpoint(String path) {
            this.authorizationEndpoint = path(path, "authorizationEndpoint");
            return this;
        }

        public Builder tokenEndpoint(String path) {
            this.tokenEndpoint = path(path, "tokenEndpoint");
            return this;
        }

        public Builder jwkSetEndpoint(String path) {
            this.jwkSetEndpoint = path(path, "jwkSetEndpoint");
            return this;
        }

        public Builder tokenRevocationEndpoint(String path) {
            this.tokenRevocationEndpoint = path(path, "tokenRevocationEndpoint");
            return this;
        }

        public Builder deviceAuthorizationEndpoint(String path) {
            this.deviceAuthorizationEndpoint = path(path, "deviceAuthorizationEndpoint");
            return this;
        }

        public Builder deviceVerificationEndpoint(String path) {
            this.deviceVerificationEndpoint = path(path, "deviceVerificationEndpoint");
            return this;
        }

        public Builder oidcUserInfoEndpoint(String path) {
            this.oidcUserInfoEndpoint = path(path, "oidcUserInfoEndpoint");
            return this;
        }

        public AuthorizationServerSettings build() {
            return new AuthorizationServerSettings(this);
        }
    }

    /// `issuer`, required to be an `http` or `https` address with no query, no
    /// fragment and no trailing slash, as RFC 8414 asks of one.
    public static String validIssuer(String issuer) {
        if (issuer == null || !(issuer.startsWith("https://") || issuer.startsWith("http://"))
                || issuer.indexOf('?') >= 0 || issuer.indexOf('#') >= 0 || issuer.endsWith("/")
                || issuer.indexOf(' ') >= 0 || issuer.length() < 9) {
            throw new IllegalArgumentException("An issuer is an http or https address with no "
                    + "query, no fragment and no trailing slash: " + issuer);
        }
        return issuer;
    }
}
