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
package com.codename1.backend.security.oauth2.client.credentials;

import com.codename1.backend.Base64;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The OAuth2 client credentials grant (RFC 6749 section 4.4): this server
/// authenticates as itself and receives a token for its own account.
///
/// ```java
/// AccessTokenProvider billing = new AccessTokenProvider(
///         ClientCredentialsGrant.builder("https://auth.example.com/oauth/token", "orders")
///                 .clientSecret(config.get("billing.secret"))
///                 .scopes("invoices:write")
///                 .build());
/// Web.request("POST", url, Arrays.asList("Authorization: "
///         + billing.getToken().toAuthorizationHeader()), body);
/// ```
///
/// The client authenticates with a secret in an HTTP Basic header (the default,
/// `client_secret_basic`), with a secret in the form (`client_secret_post`), or
/// with a JWT signed by its own key (`private_key_jwt`, RFC 7523 section 2.2),
/// which sends no shared secret at all.
public final class ClientCredentialsGrant implements AccessTokenGrant {
    /// `client_assertion_type` of a `private_key_jwt` client.
    public static final String JWT_BEARER_ASSERTION_TYPE =
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    /// How the client proves who it is.
    public enum Authentication {
        CLIENT_SECRET_BASIC,
        CLIENT_SECRET_POST,
        PRIVATE_KEY_JWT
    }

    private final TokenEndpoint endpoint;
    private final String clientId;
    private final String clientSecret;
    private final Authentication authentication;
    private final JwtAssertion assertion;
    private final Set<String> scopes;
    private final String audience;
    private final String resource;

    private ClientCredentialsGrant(Builder b) {
        this.endpoint = new TokenEndpoint(b.tokenUri, b.transport, b.clock);
        this.clientId = b.clientId;
        this.clientSecret = b.clientSecret;
        this.authentication = b.authentication;
        this.assertion = b.signingKey == null ? null : new JwtAssertion(b.signingKey);
        this.scopes = b.scopes;
        this.audience = b.audience;
        this.resource = b.resource;
    }

    /// @param tokenUri the authorization server's token endpoint
    /// @param clientId this server's client id there
    public static Builder builder(String tokenUri, String clientId) {
        return new Builder(tokenUri, clientId);
    }

    @Override
    public AccessToken fetch() {
        Map<String, Object> form = new LinkedHashMap<String, Object>();
        form.put("grant_type", "client_credentials");
        if (!scopes.isEmpty()) {
            form.put("scope", OAuth2Parameters.scopes(scopes));
        }
        if (audience != null) {
            form.put("audience", audience);
        }
        if (resource != null) {
            form.put("resource", resource);
        }
        List<String> headers = new ArrayList<String>();
        if (authentication == Authentication.CLIENT_SECRET_BASIC) {
            // RFC 6749 section 2.3.1: both halves form-encoded before the colon.
            headers.add("Authorization: Basic " + Base64.encode(OAuth2Parameters.utf8(
                    OAuth2Parameters.encode(clientId) + ":"
                            + OAuth2Parameters.encode(clientSecret))));
        } else if (authentication == Authentication.CLIENT_SECRET_POST) {
            form.put("client_id", clientId);
            form.put("client_secret", clientSecret);
        } else {
            long now = endpoint.getClock().currentTimeMillis() / 1000L;
            form.put("client_id", clientId);
            form.put("client_assertion_type", JWT_BEARER_ASSERTION_TYPE);
            // RFC 7523 section 3: iss and sub are the client, aud the endpoint.
            form.put("client_assertion", assertion.sign(clientId, clientId, endpoint.getUri(),
                    null, now, 300));
        }
        return endpoint.exchange(OAuth2Parameters.format(form), headers);
    }

    @Override
    public String toString() {
        return "ClientCredentialsGrant[" + TokenEndpoint.describe(endpoint.getUri()) + ", client="
                + clientId + ", " + authentication + "]";
    }

    public static final class Builder {
        private final String tokenUri;
        private final String clientId;
        private String clientSecret;
        private Authentication authentication;
        private Jwk signingKey;
        private Set<String> scopes = new LinkedHashSet<String>();
        private String audience;
        private String resource;
        private TokenEndpoint.Transport transport;
        private Clock clock;

        Builder(String tokenUri, String clientId) {
            if (clientId == null || clientId.length() == 0) {
                throw new IllegalArgumentException("A client id is required");
            }
            this.tokenUri = tokenUri;
            this.clientId = clientId;
        }

        /// Authenticates with a secret in an HTTP Basic header.
        public Builder clientSecret(String secret) {
            return clientSecret(secret, Authentication.CLIENT_SECRET_BASIC);
        }

        /// Authenticates with a secret, in a header or in the form.
        public Builder clientSecret(String secret, Authentication how) {
            if (secret == null || secret.length() == 0) {
                throw new IllegalArgumentException("A client secret is required");
            }
            if (how == Authentication.PRIVATE_KEY_JWT) {
                throw new IllegalArgumentException("private_key_jwt takes a key, not a secret");
            }
            this.clientSecret = secret;
            this.authentication = how;
            this.signingKey = null;
            return this;
        }

        /// Authenticates with a JWT signed by `privateKey` (`private_key_jwt`).
        /// Give the key the id the authorization server registered it under with
        /// [Jwk#withKeyId(String)].
        public Builder privateKeyJwt(Jwk privateKey) {
            JwtAssertion.algorithmFor(privateKey);
            if (!privateKey.isPrivate()) {
                throw new IllegalArgumentException("private_key_jwt signs with a private key");
            }
            this.signingKey = privateKey;
            this.authentication = Authentication.PRIVATE_KEY_JWT;
            this.clientSecret = null;
            return this;
        }

        public Builder scopes(String... scopes) {
            return scopes(Arrays.asList(scopes));
        }

        public Builder scopes(Collection<String> scopes) {
            this.scopes = new LinkedHashSet<String>(scopes);
            return this;
        }

        /// An `audience` parameter, which Auth0 and Okta use to name the API the
        /// token is for. Not part of RFC 6749; omitted unless set.
        public Builder audience(String audience) {
            this.audience = audience;
            return this;
        }

        /// A `resource` parameter (RFC 8707). Omitted unless set.
        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public Builder transport(TokenEndpoint.Transport transport) {
            this.transport = transport;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public ClientCredentialsGrant build() {
            if (authentication == null) {
                throw new IllegalStateException("The client authenticates with clientSecret(...) "
                        + "or privateKeyJwt(...)");
            }
            return new ClientCredentialsGrant(this);
        }
    }
}
