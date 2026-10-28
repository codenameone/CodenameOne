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

import com.codename1.backend.security.Clock;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/// The JWT bearer grant of RFC 7523 section 2.1: this server signs an assertion
/// with a service account's private key and exchanges it for an access token.
/// No client secret exists; the key is the credential.
///
/// It is how Google service accounts authenticate (see
/// [GoogleServiceAccountCredentials]), and Salesforce, DocuSign and others accept
/// it the same way. Every exchange signs a new assertion, valid for
/// `assertionLifetimeSeconds` (one hour by default, the most Google accepts) and
/// carrying a fresh `jti`.
public final class JwtBearerGrant implements AccessTokenGrant {
    /// The `grant_type` of this grant.
    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";

    private final TokenEndpoint endpoint;
    private final JwtAssertion assertion;
    private final String issuer;
    private final String subject;
    private final String audience;
    private final Set<String> scopes;
    private final boolean scopesInAssertion;
    private final long assertionLifetimeSeconds;

    private JwtBearerGrant(Builder b) {
        this.endpoint = new TokenEndpoint(b.tokenUri, b.transport, b.clock);
        this.assertion = new JwtAssertion(b.signingKey);
        this.issuer = b.issuer;
        this.subject = b.subject == null ? b.issuer : b.subject;
        this.audience = b.audience == null ? b.tokenUri : b.audience;
        this.scopes = b.scopes;
        this.scopesInAssertion = b.scopesInAssertion;
        this.assertionLifetimeSeconds = b.assertionLifetimeSeconds;
    }

    /// @param tokenUri the token endpoint the assertion is exchanged at
    /// @param issuer `iss`: the service account, such as its client email
    /// @param signingKey the service account's private key; its id, if it has
    /// one, goes into the header as `kid`
    public static Builder builder(String tokenUri, String issuer, Jwk signingKey) {
        return new Builder(tokenUri, issuer, signingKey);
    }

    @Override
    public AccessToken fetch() {
        long now = endpoint.getClock().currentTimeMillis() / 1000L;
        String signed = assertion.sign(issuer, subject, audience,
                scopesInAssertion ? scopes : null, now, assertionLifetimeSeconds);
        Map<String, Object> form = new LinkedHashMap<String, Object>();
        form.put("grant_type", GRANT_TYPE);
        form.put("assertion", signed);
        if (!scopesInAssertion && !scopes.isEmpty()) {
            form.put("scope", OAuth2Parameters.scopes(scopes));
        }
        return endpoint.exchange(OAuth2Parameters.format(form), null);
    }

    @Override
    public String toString() {
        return "JwtBearerGrant[" + TokenEndpoint.describe(endpoint.getUri()) + ", iss=" + issuer
                + ", sub=" + subject + "]";
    }

    public static final class Builder {
        private final String tokenUri;
        private final String issuer;
        private final Jwk signingKey;
        private String subject;
        private String audience;
        private Set<String> scopes = new LinkedHashSet<String>();
        private boolean scopesInAssertion;
        private long assertionLifetimeSeconds = 3600;
        private TokenEndpoint.Transport transport;
        private Clock clock;

        Builder(String tokenUri, String issuer, Jwk signingKey) {
            if (issuer == null || issuer.length() == 0) {
                throw new IllegalArgumentException("An assertion needs an issuer");
            }
            JwtAssertion.algorithmFor(signingKey);
            this.tokenUri = tokenUri;
            this.issuer = issuer;
            this.signingKey = signingKey;
        }

        /// `sub`: whom the token acts for. The issuer itself unless set; for a
        /// Google Workspace domain-wide delegation, the user being impersonated.
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        /// `aud`: the token endpoint unless set.
        public Builder audience(String audience) {
            this.audience = audience;
            return this;
        }

        public Builder scopes(String... scopes) {
            return scopes(Arrays.asList(scopes));
        }

        public Builder scopes(Collection<String> scopes) {
            this.scopes = new LinkedHashSet<String>(scopes);
            return this;
        }

        /// Where the scopes go: a `scope` form parameter (false, the default,
        /// as RFC 7521 section 4.1 and RFC 6749 section 3.3 define it) or a
        /// `scope` claim in the assertion (true, as Google expects; the Google
        /// builder sets it).
        public Builder scopesInAssertion(boolean inAssertion) {
            this.scopesInAssertion = inAssertion;
            return this;
        }

        public Builder assertionLifetimeSeconds(long seconds) {
            if (seconds < 1) {
                throw new IllegalArgumentException("An assertion lives at least a second");
            }
            this.assertionLifetimeSeconds = seconds;
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

        public JwtBearerGrant build() {
            return new JwtBearerGrant(this);
        }
    }
}
