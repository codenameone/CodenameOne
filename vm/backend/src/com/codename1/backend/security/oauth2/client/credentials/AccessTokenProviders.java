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

import com.codename1.backend.Config;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.KeyFiles;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import java.io.IOException;
import java.util.Set;

/// Builds an [AccessTokenProvider] from configuration, so the credential lives in
/// a secret store or the environment instead of in code.
///
/// Every key is under `cn1.security.oauth2.client.credentials.<name>.`:
///
/// | key | meaning |
/// |---|---|
/// | `grant` | `client_credentials`, `jwt_bearer` or `google_service_account` |
/// | `token-uri` | the token endpoint (not used by `google_service_account`) |
/// | `client-id` | the client id (`client_credentials`) |
/// | `client-secret` | the secret, sent as HTTP Basic unless `client-authentication` says `post` |
/// | `client-authentication` | `basic`, `post` or `private_key_jwt` |
/// | `key-file` | PEM private key: `private_key_jwt`, or the `jwt_bearer` signing key |
/// | `key-id` | the `kid` the key is registered under |
/// | `issuer`, `subject`, `audience` | `jwt_bearer` claims; `audience` is also sent by `client_credentials` |
/// | `google-json` | path of a Google service-account key file |
/// | `scopes` | space- or comma-separated scopes |
/// | `scopes-in-assertion` | `jwt_bearer`: true sends the scopes as an assertion claim instead of a form parameter (false) |
/// | `refresh-skew` | seconds before expiry a token stops being used (60) |
/// | `max-backoff` | longest pause between failed exchanges, in seconds (60) |
///
/// ```java
/// @Bean
/// AccessTokenProvider billing(Config config) throws IOException {
///     return AccessTokenProviders.fromConfig(config, "billing");
/// }
/// ```
public final class AccessTokenProviders {
    /// The prefix of every key.
    public static final String PREFIX = "cn1.security.oauth2.client.credentials.";

    private AccessTokenProviders() {
    }

    /// @throws IOException when a file cannot be read or the configuration is
    /// incomplete; the message names the missing key, never a value
    public static AccessTokenProvider fromConfig(Config config, String name) throws IOException {
        String p = PREFIX + name + ".";
        String grant = config.get(p + "grant");
        if (grant == null) {
            throw new IOException(p + "grant is not set");
        }
        Set<String> scopes = OAuth2Parameters.scopes(config.get(p + "scopes", "").replace(',', ' '));
        AccessTokenGrant built;
        if ("client_credentials".equals(grant)) {
            ClientCredentialsGrant.Builder b = ClientCredentialsGrant.builder(
                    required(config, p + "token-uri"), required(config, p + "client-id"))
                    .scopes(scopes).audience(config.get(p + "audience"));
            String how = config.get(p + "client-authentication", "basic");
            if ("private_key_jwt".equals(how)) {
                b.privateKeyJwt(key(config, p));
            } else if ("post".equals(how)) {
                b.clientSecret(required(config, p + "client-secret"),
                        ClientCredentialsGrant.Authentication.CLIENT_SECRET_POST);
            } else if ("basic".equals(how)) {
                b.clientSecret(required(config, p + "client-secret"));
            } else {
                throw new IOException(p + "client-authentication is basic, post or "
                        + "private_key_jwt, not " + how);
            }
            built = b.build();
        } else if ("jwt_bearer".equals(grant)) {
            built = JwtBearerGrant.builder(required(config, p + "token-uri"),
                    required(config, p + "issuer"), key(config, p))
                    .subject(config.get(p + "subject")).audience(config.get(p + "audience"))
                    .scopes(scopes)
                    .scopesInAssertion(config.getBoolean(p + "scopes-in-assertion", false))
                    .build();
        } else if ("google_service_account".equals(grant)) {
            built = GoogleServiceAccountCredentials.fromFile(required(config, p + "google-json"))
                    .builder().subject(config.get(p + "subject")).scopes(scopes).build();
        } else {
            throw new IOException(p + "grant is client_credentials, jwt_bearer or "
                    + "google_service_account, not " + grant);
        }
        AccessTokenProvider provider = new AccessTokenProvider(built);
        provider.setRefreshSkewSeconds(config.getInt(p + "refresh-skew", 60));
        provider.setMaxBackoffSeconds(config.getInt(p + "max-backoff", 60));
        return provider;
    }

    private static Jwk key(Config config, String p) throws IOException {
        Jwk key = Jwk.ofPrivateKey(KeyFiles.readPrivateKey(required(config, p + "key-file")));
        String keyId = config.get(p + "key-id");
        return keyId == null ? key : key.withKeyId(keyId);
    }

    private static String required(Config config, String key) throws IOException {
        String value = config.get(key);
        if (value == null || value.length() == 0) {
            throw new IOException(key + " is not set");
        }
        return value;
    }
}
