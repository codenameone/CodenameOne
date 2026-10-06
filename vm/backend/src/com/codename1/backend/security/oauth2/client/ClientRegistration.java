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

import com.codename1.backend.security.oauth2.core.AuthorizationGrantType;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/// This server as a client of one identity provider: the id and secret the
/// provider issued it, the scopes it asks for, and where the provider's
/// endpoints are.
///
/// ```java
/// ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
///         .clientId(id).clientSecret(secret).build();
/// ClientRegistration own = ClientRegistration.withRegistrationId("acme")
///         .clientId("web").clientSecret(secret)
///         .issuerUri("https://id.example.com")
///         .scope("openid", "profile", "email").build();
/// ```
///
/// A registration that names an `issuerUri` and not the endpoints has them read
/// from the issuer's metadata the first time a user signs in through it.
public final class ClientRegistration {
    /// The `response_mode` that has the provider answer with a form the browser
    /// posts, rather than a redirect.
    public static final String FORM_POST = "form_post";

    /// Where a client secret comes from when it is not a fixed text: Sign in
    /// with Apple's is a token this server signs.
    public interface ClientSecretSupplier {
        /// The secret to send now.
        String getClientSecret(ClientRegistration registration) throws IOException;
    }

    private final String registrationId;
    private final String clientId;
    private final String clientSecret;
    private final ClientSecretSupplier clientSecretSupplier;
    private final ClientAuthenticationMethod clientAuthenticationMethod;
    private final AuthorizationGrantType authorizationGrantType;
    private final String redirectUri;
    private final Set<String> scopes;
    private final String clientName;
    private final String responseMode;
    private final ProviderDetails providerDetails;

    private ClientRegistration(Builder b) {
        this.registrationId = b.registrationId;
        this.clientId = b.clientId;
        this.clientSecret = b.clientSecret;
        this.clientSecretSupplier = b.clientSecretSupplier;
        this.clientAuthenticationMethod = b.clientAuthenticationMethod != null
                ? b.clientAuthenticationMethod
                : b.clientSecret == null && b.clientSecretSupplier == null
                        ? ClientAuthenticationMethod.NONE
                        : ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
        this.authorizationGrantType = AuthorizationGrantType.AUTHORIZATION_CODE;
        this.redirectUri = b.redirectUri;
        this.scopes = Collections.unmodifiableSet(new LinkedHashSet<String>(b.scopes));
        this.clientName = b.clientName == null ? b.registrationId : b.clientName;
        this.responseMode = b.responseMode;
        this.providerDetails = new ProviderDetails(b);
    }

    /// A registration under this id, which is the last part of the two paths a
    /// sign-in through it uses.
    public static Builder withRegistrationId(String registrationId) {
        return new Builder(registrationId);
    }

    /// A builder that starts as a copy of `registration`.
    public static Builder withClientRegistration(ClientRegistration registration) {
        Builder b = new Builder(registration.registrationId);
        b.clientId = registration.clientId;
        b.clientSecret = registration.clientSecret;
        b.clientSecretSupplier = registration.clientSecretSupplier;
        b.clientAuthenticationMethod = registration.clientAuthenticationMethod;
        b.redirectUri = registration.redirectUri;
        b.scopes.addAll(registration.scopes);
        b.clientName = registration.clientName;
        b.responseMode = registration.responseMode;
        ProviderDetails p = registration.providerDetails;
        b.authorizationUri = p.authorizationUri;
        b.tokenUri = p.tokenUri;
        b.userInfoUri = p.userInfoUri;
        b.userNameAttributeName = p.userNameAttributeName;
        b.jwkSetUri = p.jwkSetUri;
        b.issuerUri = p.issuerUri;
        b.issuerTemplate = p.issuerTemplate;
        b.idTokenAlgorithms = p.idTokenAlgorithms;
        return b;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public String getClientId() {
        return clientId;
    }

    /// The fixed secret, or null when there is none or it is supplied; see
    /// [#resolveClientSecret].
    public String getClientSecret() {
        return clientSecret;
    }

    /// The secret to send now: the fixed one, or what the supplier gives.
    public String resolveClientSecret() throws IOException {
        return clientSecretSupplier != null ? clientSecretSupplier.getClientSecret(this)
                : clientSecret;
    }

    public ClientAuthenticationMethod getClientAuthenticationMethod() {
        return clientAuthenticationMethod;
    }

    public AuthorizationGrantType getAuthorizationGrantType() {
        return authorizationGrantType;
    }

    /// Where the provider sends the user back: an address, or a template with
    /// `{baseUrl}` and `{registrationId}` in it.
    public String getRedirectUri() {
        return redirectUri;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    /// What a login page calls this provider.
    public String getClientName() {
        return clientName;
    }

    /// [#FORM_POST], or null for the provider's default, a redirect.
    public String getResponseMode() {
        return responseMode;
    }

    public ProviderDetails getProviderDetails() {
        return providerDetails;
    }

    @Override
    public String toString() {
        return "ClientRegistration[" + registrationId + ", clientId=" + clientId + "]";
    }

    /// Where the provider is.
    public static final class ProviderDetails {
        private final String authorizationUri;
        private final String tokenUri;
        private final String userInfoUri;
        private final String userNameAttributeName;
        private final String jwkSetUri;
        private final String issuerUri;
        private final String issuerTemplate;
        private final String[] idTokenAlgorithms;

        ProviderDetails(Builder b) {
            this.authorizationUri = b.authorizationUri;
            this.tokenUri = b.tokenUri;
            this.userInfoUri = b.userInfoUri;
            this.userNameAttributeName = b.userNameAttributeName;
            this.jwkSetUri = b.jwkSetUri;
            this.issuerUri = b.issuerUri;
            this.issuerTemplate = b.issuerTemplate;
            this.idTokenAlgorithms = b.idTokenAlgorithms;
        }

        public String getAuthorizationUri() {
            return authorizationUri;
        }

        public String getTokenUri() {
            return tokenUri;
        }

        /// Where the user's attributes are read from, or null.
        public String getUserInfoUri() {
            return userInfoUri;
        }

        /// The attribute that names the user; `sub` for an OpenID Connect
        /// provider that says nothing else.
        public String getUserNameAttributeName() {
            return userNameAttributeName;
        }

        /// Where the keys that sign ID tokens are published, or null.
        public String getJwkSetUri() {
            return jwkSetUri;
        }

        /// The `iss` every ID token must carry, or null.
        public String getIssuerUri() {
            return issuerUri;
        }

        /// For a provider that issues under one address per tenant: the issuer
        /// with `{tenantid}` where the token's `tid` claim goes. Null otherwise.
        public String getIssuerTemplate() {
            return issuerTemplate;
        }

        /// The names of the algorithms an ID token may be signed with, or null
        /// for what the metadata says, and RS256 and ES256 when there is none.
        public String[] getIdTokenAlgorithms() {
            return idTokenAlgorithms == null ? null : idTokenAlgorithms.clone();
        }
    }

    /// Builds a [ClientRegistration].
    public static final class Builder {
        private final String registrationId;
        private String clientId;
        private String clientSecret;
        private ClientSecretSupplier clientSecretSupplier;
        private ClientAuthenticationMethod clientAuthenticationMethod;
        private String redirectUri = "{baseUrl}/login/oauth2/code/{registrationId}";
        private final Set<String> scopes = new LinkedHashSet<String>();
        private String clientName;
        private String responseMode;
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String userNameAttributeName;
        private String jwkSetUri;
        private String issuerUri;
        private String issuerTemplate;
        private String[] idTokenAlgorithms;

        Builder(String registrationId) {
            if (registrationId == null || registrationId.length() == 0) {
                throw new IllegalArgumentException("registrationId cannot be empty");
            }
            for (int iter = 0 ; iter < registrationId.length() ; iter++) {
                char c = registrationId.charAt(iter);
                if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                        || c == '-' || c == '_' || c == '.')) {
                    throw new IllegalArgumentException("A registrationId is part of a path, and "
                            + "is made of letters, digits, '-', '_' and '.': " + registrationId);
                }
            }
            this.registrationId = registrationId;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        /// A secret made when it is needed; see [AppleClientSecret].
        public Builder clientSecretSupplier(ClientSecretSupplier supplier) {
            this.clientSecretSupplier = supplier;
            return this;
        }

        /// [ClientAuthenticationMethod#CLIENT_SECRET_BASIC] unless set, and
        /// [ClientAuthenticationMethod#NONE] for a client without a secret.
        public Builder clientAuthenticationMethod(ClientAuthenticationMethod method) {
            this.clientAuthenticationMethod = method;
            return this;
        }

        /// `{baseUrl}/login/oauth2/code/{registrationId}` unless set. Give the
        /// whole address in production: `{baseUrl}` is read from the request,
        /// and what the provider has on record is one fixed address.
        public Builder redirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        public Builder scope(String... scope) {
            this.scopes.clear();
            if (scope != null) {
                this.scopes.addAll(Arrays.asList(scope));
            }
            return this;
        }

        public Builder scope(Collection<String> scope) {
            this.scopes.clear();
            if (scope != null) {
                this.scopes.addAll(scope);
            }
            return this;
        }

        public Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        /// [ClientRegistration#FORM_POST], or null.
        public Builder responseMode(String responseMode) {
            this.responseMode = responseMode;
            return this;
        }

        public Builder authorizationUri(String authorizationUri) {
            this.authorizationUri = authorizationUri;
            return this;
        }

        public Builder tokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
            return this;
        }

        public Builder userInfoUri(String userInfoUri) {
            this.userInfoUri = userInfoUri;
            return this;
        }

        public Builder userNameAttributeName(String userNameAttributeName) {
            this.userNameAttributeName = userNameAttributeName;
            return this;
        }

        public Builder jwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
            return this;
        }

        public Builder issuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
            return this;
        }

        /// See [ProviderDetails#getIssuerTemplate].
        public Builder issuerTemplate(String issuerTemplate) {
            this.issuerTemplate = issuerTemplate;
            return this;
        }

        /// See [ProviderDetails#getIdTokenAlgorithms]: names such as `RS256`.
        public Builder idTokenAlgorithms(String... names) {
            this.idTokenAlgorithms = names == null || names.length == 0 ? null : names.clone();
            return this;
        }

        public ClientRegistration build() {
            if (clientId == null || clientId.length() == 0) {
                throw new IllegalArgumentException("The registration " + registrationId
                        + " has no clientId");
            }
            if (redirectUri == null || redirectUri.length() == 0) {
                throw new IllegalArgumentException("The registration " + registrationId
                        + " has no redirectUri");
            }
            if (issuerUri == null && (authorizationUri == null || tokenUri == null)) {
                throw new IllegalArgumentException("The registration " + registrationId
                        + " says where neither its provider's endpoints nor its issuer are: "
                        + "set authorizationUri and tokenUri, or issuerUri");
            }
            if (scopes.contains("openid") && issuerUri == null && issuerTemplate == null) {
                throw new IllegalArgumentException("The registration " + registrationId
                        + " asks for openid and names no issuer to hold the ID token to: set "
                        + "issuerUri");
            }
            if (OAuth2Parameters.scopes(scopes).indexOf('"') >= 0) {
                throw new IllegalArgumentException("A scope cannot contain a quote");
            }
            return new ClientRegistration(this);
        }
    }
}
