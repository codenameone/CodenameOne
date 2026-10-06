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

import com.codename1.backend.security.oauth2.core.AuthorizationGrantType;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/// A client this authorization server issues tokens to.
///
/// ```java
/// RegisteredClient app = RegisteredClient.withId("mobile")
///         .clientId("acme-app")
///         .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)   // public: PKCE
///         .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
///         .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
///         .redirectUri("com.acme.app:/oauth2redirect")
///         .scope("openid").scope("profile").scope("orders:read")
///         .build();
/// ```
///
/// A redirect address is matched whole, character for character: there are no
/// wildcards. The one exception is RFC 8252's: an `http` address on
/// `127.0.0.1` or `[::1]` may be asked for with any port, because a native
/// application cannot know which one it will get.
///
/// A secret is stored as its [com.codename1.backend.security.crypto.PasswordEncoder]
/// wrote it, never as it was issued.
public final class RegisteredClient {
    private final String id;
    private final String clientId;
    private final String clientSecret;
    private final String clientName;
    private final Set<ClientAuthenticationMethod> clientAuthenticationMethods;
    private final Set<AuthorizationGrantType> authorizationGrantTypes;
    private final Set<String> redirectUris;
    private final Set<String> scopes;
    private final Set<String> resources;
    private final ClientSettings clientSettings;
    private final TokenSettings tokenSettings;

    private RegisteredClient(Builder b) {
        this.id = b.id;
        this.clientId = b.clientId;
        this.clientSecret = b.clientSecret;
        this.clientName = b.clientName == null ? b.clientId : b.clientName;
        this.clientAuthenticationMethods = Collections.unmodifiableSet(
                new LinkedHashSet<ClientAuthenticationMethod>(b.methods));
        this.authorizationGrantTypes = Collections.unmodifiableSet(
                new LinkedHashSet<AuthorizationGrantType>(b.grants));
        this.redirectUris = Collections.unmodifiableSet(new LinkedHashSet<String>(b.redirectUris));
        this.scopes = Collections.unmodifiableSet(new LinkedHashSet<String>(b.scopes));
        this.resources = Collections.unmodifiableSet(new LinkedHashSet<String>(b.resources));
        this.clientSettings = b.clientSettings;
        this.tokenSettings = b.tokenSettings;
    }

    /// A client stored under `id`, which is the server's own name for it and
    /// never changes; the `clientId` is what the client calls itself.
    public static Builder withId(String id) {
        return new Builder(id);
    }

    public String getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    /// The encoded secret, or null for a client without one.
    public String getClientSecret() {
        return clientSecret;
    }

    public String getClientName() {
        return clientName;
    }

    public Set<ClientAuthenticationMethod> getClientAuthenticationMethods() {
        return clientAuthenticationMethods;
    }

    public Set<AuthorizationGrantType> getAuthorizationGrantTypes() {
        return authorizationGrantTypes;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    /// Every scope the client may be granted.
    public Set<String> getScopes() {
        return scopes;
    }

    /// The resource servers the client may ask for tokens for, each by the
    /// address it is named with in a token's `aud`; empty when the client was
    /// registered with none, and may then name any. See [Builder#resource].
    public Set<String> getResources() {
        return resources;
    }

    public ClientSettings getClientSettings() {
        return clientSettings;
    }

    public TokenSettings getTokenSettings() {
        return tokenSettings;
    }

    /// Whether the client is a public one: it authenticates with nothing, and
    /// so must prove with PKCE that the code it redeems is the one it asked for.
    public boolean isPublic() {
        return clientAuthenticationMethods.contains(ClientAuthenticationMethod.NONE);
    }

    @Override
    public String toString() {
        return "RegisteredClient[" + id + ", clientId=" + clientId + "]";
    }

    /// Whether `resource` can name a resource server: an absolute URI with no
    /// fragment, as RFC 8707 section 2 requires of the parameter.
    public static boolean isResource(String resource) {
        if (resource == null || resource.length() > 2048 || resource.indexOf('#') >= 0) {
            return false;
        }
        int colon = resource.indexOf(':');
        if (colon <= 0 || colon == resource.length() - 1) {
            return false;
        }
        for (int iter = 0 ; iter < resource.length() ; iter++) {
            char c = resource.charAt(iter);
            if (c <= 0x20 || c >= 0x7f || c == '"' || c == '\\') {
                return false;
            }
            if (iter < colon && !((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (iter > 0 && ((c >= '0' && c <= '9') || c == '+' || c == '-'
                    || c == '.')))) {
                return false;
            }
        }
        return true;
    }

    /// Builds a [RegisteredClient].
    public static final class Builder {
        private final String id;
        private String clientId;
        private String clientSecret;
        private String clientName;
        private final Set<ClientAuthenticationMethod> methods =
                new LinkedHashSet<ClientAuthenticationMethod>();
        private final Set<AuthorizationGrantType> grants =
                new LinkedHashSet<AuthorizationGrantType>();
        private final Set<String> redirectUris = new LinkedHashSet<String>();
        private final Set<String> scopes = new LinkedHashSet<String>();
        private final Set<String> resources = new LinkedHashSet<String>();
        private ClientSettings clientSettings = ClientSettings.builder().build();
        private TokenSettings tokenSettings = TokenSettings.builder().build();

        Builder(String id) {
            if (id == null || id.length() == 0) {
                throw new IllegalArgumentException("id cannot be empty");
            }
            this.id = id;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        /// The secret as the encoder wrote it: `encoder.encode(secret)`.
        public Builder clientSecret(String encodedClientSecret) {
            this.clientSecret = encodedClientSecret == null || encodedClientSecret.length() == 0
                    ? null : encodedClientSecret;
            return this;
        }

        public Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public Builder clientAuthenticationMethod(ClientAuthenticationMethod method) {
            this.methods.add(method);
            return this;
        }

        public Builder authorizationGrantType(AuthorizationGrantType grantType) {
            this.grants.add(grantType);
            return this;
        }

        public Builder redirectUri(String redirectUri) {
            this.redirectUris.add(redirectUri);
            return this;
        }

        public Builder scope(String scope) {
            this.scopes.add(scope);
            return this;
        }

        /// A resource server this client may ask for tokens for, named as the
        /// client names it in the `resource` parameter of a request (RFC
        /// 8707): an absolute address with no fragment, compared whole.
        ///
        /// An access token's `aud` is what the request asked for with
        /// `resource`, or the server's default audience when it asked for
        /// nothing; see [AuthorizationServerSettings.Builder#defaultAudience].
        /// A client registered with resources is refused any other with
        /// `invalid_target`. One registered with none may name any: the
        /// audience limits where a token is accepted, and a client that could
        /// not be trusted to choose it should be given a list.
        public Builder resource(String resource) {
            this.resources.add(resource);
            return this;
        }

        public Builder clientSettings(ClientSettings clientSettings) {
            this.clientSettings = clientSettings;
            return this;
        }

        public Builder tokenSettings(TokenSettings tokenSettings) {
            this.tokenSettings = tokenSettings;
            return this;
        }

        public RegisteredClient build() {
            if (clientId == null || clientId.length() == 0) {
                throw new IllegalArgumentException("clientId cannot be empty");
            }
            if (grants.isEmpty()) {
                throw new IllegalArgumentException("The client " + clientId
                        + " has no authorizationGrantTypes");
            }
            if (methods.isEmpty()) {
                methods.add(clientSecret == null ? ClientAuthenticationMethod.NONE
                        : ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
            }
            boolean secretMethod = methods.contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    || methods.contains(ClientAuthenticationMethod.CLIENT_SECRET_POST);
            if (secretMethod && clientSecret == null) {
                throw new IllegalArgumentException("The client " + clientId
                        + " authenticates with a secret and has none");
            }
            if (grants.contains(AuthorizationGrantType.CLIENT_CREDENTIALS) && !secretMethod) {
                throw new IllegalArgumentException("The client " + clientId + " asks for "
                        + "client_credentials, which a client without a secret cannot use: "
                        + "there would be nothing to tell it from anyone else");
            }
            if (grants.contains(AuthorizationGrantType.AUTHORIZATION_CODE)
                    && redirectUris.isEmpty()) {
                throw new IllegalArgumentException("The client " + clientId
                        + " asks for authorization_code and has no redirectUris");
            }
            for (String uri : redirectUris) {
                if (uri == null || uri.indexOf(':') <= 0 || uri.indexOf('#') >= 0
                        || uri.indexOf('*') >= 0 || uri.indexOf(' ') >= 0) {
                    throw new IllegalArgumentException("A redirect address is a whole address "
                            + "with no fragment and no wildcard: " + uri);
                }
            }
            for (String resource : resources) {
                if (!isResource(resource)) {
                    throw new IllegalArgumentException("A resource is an absolute address "
                            + "with no fragment: " + resource);
                }
            }
            for (String scope : scopes) {
                if (scope == null || scope.length() == 0 || scope.indexOf(' ') >= 0
                        || scope.indexOf('"') >= 0 || scope.indexOf('\\') >= 0) {
                    throw new IllegalArgumentException("A scope is a word without spaces, "
                            + "quotes or backslashes: " + scope);
                }
            }
            return new RegisteredClient(this);
        }
    }
}
