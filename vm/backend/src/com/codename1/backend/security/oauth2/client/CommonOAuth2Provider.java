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

import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;

/// The settings of the identity providers most applications sign in through,
/// so that a registration needs only the id and secret the provider issued.
///
/// ```java
/// CommonOAuth2Provider.GITHUB.getBuilder("github").clientId(id).clientSecret(secret).build();
/// ```
public enum CommonOAuth2Provider {
    /// Google, with OpenID Connect.
    GOOGLE {
        @Override
        public ClientRegistration.Builder getBuilder(String registrationId) {
            return ClientRegistration.withRegistrationId(registrationId)
                    .clientName("Google")
                    .scope("openid", "profile", "email")
                    .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                    .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                    .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                    .issuerUri("https://accounts.google.com")
                    .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .userNameAttributeName("sub");
        }
    },
    /// GitHub, which is OAuth2 without OpenID Connect: the user is read from
    /// its API, and named by the numeric `id`.
    ///
    /// GitHub's user says nothing of whether an address is verified, and may
    /// show none at all. The verified ones are listed at `/user/emails`, which
    /// the `user:email` scope asked for here opens; see
    /// [ClientRegistration.ProviderDetails#getUserEmailsUri].
    GITHUB {
        @Override
        public ClientRegistration.Builder getBuilder(String registrationId) {
            return ClientRegistration.withRegistrationId(registrationId)
                    .clientName("GitHub")
                    .scope("read:user", "user:email")
                    .authorizationUri("https://github.com/login/oauth/authorize")
                    .tokenUri("https://github.com/login/oauth/access_token")
                    .userInfoUri("https://api.github.com/user")
                    .userEmailsUri("https://api.github.com/user/emails")
                    .userNameAttributeName("id");
        }
    },
    /// Microsoft's identity platform at its multi-tenant `common` endpoints,
    /// which accept accounts of any organisation and personal ones. Tokens are
    /// issued under the address of the account's own tenant, so the issuer is
    /// held to the tenant template rather than to one address; an application
    /// that admits one tenant only should also check the `tid` claim.
    MICROSOFT {
        @Override
        public ClientRegistration.Builder getBuilder(String registrationId) {
            String base = "https://login.microsoftonline.com/common";
            return ClientRegistration.withRegistrationId(registrationId)
                    .clientName("Microsoft")
                    .scope("openid", "profile", "email")
                    .authorizationUri(base + "/oauth2/v2.0/authorize")
                    .tokenUri(base + "/oauth2/v2.0/token")
                    .jwkSetUri(base + "/discovery/v2.0/keys")
                    .issuerTemplate("https://login.microsoftonline.com/{tenantid}/v2.0")
                    .userNameAttributeName("sub");
        }
    },
    /// Sign in with Apple. Apple answers with a form the browser posts, and its
    /// client secret is a token signed with the key Apple issued: give the
    /// builder a [ClientRegistration.Builder#clientSecretSupplier] made by
    /// [AppleClientSecret].
    APPLE {
        @Override
        public ClientRegistration.Builder getBuilder(String registrationId) {
            return ClientRegistration.withRegistrationId(registrationId)
                    .clientName("Apple")
                    .scope("openid", "name", "email")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .responseMode(ClientRegistration.FORM_POST)
                    .authorizationUri("https://appleid.apple.com/auth/authorize")
                    .tokenUri("https://appleid.apple.com/auth/token")
                    .jwkSetUri("https://appleid.apple.com/auth/keys")
                    .issuerUri("https://appleid.apple.com")
                    .userNameAttributeName("sub");
        }
    };

    /// A builder with the provider's settings in it, wanting the client id and
    /// secret.
    public abstract ClientRegistration.Builder getBuilder(String registrationId);

    /// The provider of this name, in any case, or null.
    public static CommonOAuth2Provider of(String name) {
        for (CommonOAuth2Provider provider : values()) {
            if (provider.name().equalsIgnoreCase(name)) {
                return provider;
            }
        }
        return null;
    }
}
