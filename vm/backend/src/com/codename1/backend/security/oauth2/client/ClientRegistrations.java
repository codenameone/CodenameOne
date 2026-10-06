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

import com.codename1.backend.Config;
import com.codename1.backend.security.oauth2.core.ClientAuthenticationMethod;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.security.oauth2.jwt.JwtDecoders;
import com.codename1.backend.security.oauth2.jwt.RemoteJwkSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Registrations read from the server's configuration, and from an issuer's
/// metadata.
///
/// ```
/// cn1.security.oauth2.client.registration.google.client-id=...
/// cn1.security.oauth2.client.registration.google.client-secret=...
///
/// cn1.security.oauth2.client.registration.acme.client-id=web
/// cn1.security.oauth2.client.registration.acme.client-secret=...
/// cn1.security.oauth2.client.registration.acme.scope=openid,profile,email
/// cn1.security.oauth2.client.registration.acme.provider=acme-id
/// cn1.security.oauth2.client.provider.acme-id.issuer-uri=https://id.example.com
/// ```
///
/// A registration's `provider` names either one of [CommonOAuth2Provider] or a
/// `cn1.security.oauth2.client.provider.<id>` block; when it is not set, the
/// registration's own id is tried as both. Under a registration:
/// `client-id`, `client-secret`, `client-authentication-method`
/// (`client_secret_basic`, `client_secret_post`, `none`), `scope`,
/// `redirect-uri`, `client-name`, `response-mode` and `provider`. Under a
/// provider: `issuer-uri`, `authorization-uri`, `token-uri`, `user-info-uri`,
/// `user-emails-uri`, `jwk-set-uri`, `user-name-attribute`, and
/// `authorization-response-iss-parameter-supported` (`true` for a provider
/// whose endpoints are named here and that sends `iss` with its answers; one
/// read from its issuer's metadata says so itself).
///
/// Sign in with Apple is declared in code, as a
/// [ClientRegistrationRepository] bean, because its secret is a token signed
/// with a key and not a setting -- see [AppleClientSecret] -- and a server
/// that does not sign in with Apple should not carry what signs one.
public final class ClientRegistrations {
    /// The prefix of every registration's settings.
    public static final String REGISTRATION = "cn1.security.oauth2.client.registration.";
    /// The prefix of every provider's settings.
    public static final String PROVIDER = "cn1.security.oauth2.client.provider.";

    private ClientRegistrations() {
    }

    /// The registrations the configuration declares; empty when it declares
    /// none.
    ///
    /// - `IllegalArgumentException`: when one of them is incomplete
    public static List<ClientRegistration> fromConfig(Config config) throws IOException {
        List<String> ids = new ArrayList<String>();
        for (Object key : config.keys()) {
            String name = String.valueOf(key);
            if (name.startsWith(REGISTRATION)) {
                int dot = name.indexOf('.', REGISTRATION.length());
                String id = dot < 0 ? null : name.substring(REGISTRATION.length(), dot);
                if (id != null && id.length() > 0 && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        List<ClientRegistration> out = new ArrayList<ClientRegistration>();
        for (String id : ids) {
            out.add(fromConfig(config, id));
        }
        return out;
    }

    private static ClientRegistration fromConfig(Config config, String id) throws IOException {
        String prefix = REGISTRATION + id + ".";
        String providerId = config.get(prefix + "provider", id);
        CommonOAuth2Provider common = CommonOAuth2Provider.of(providerId);
        ClientRegistration.Builder b = common != null ? common.getBuilder(id)
                : ClientRegistration.withRegistrationId(id);
        b.clientId(config.get(prefix + "client-id"));
        String secret = config.get(prefix + "client-secret");
        if (secret != null && secret.length() > 0) {
            b.clientSecret(secret);
        }
        String method = config.get(prefix + "client-authentication-method");
        if (method != null) {
            b.clientAuthenticationMethod(new ClientAuthenticationMethod(method));
        }
        String scope = config.get(prefix + "scope");
        if (scope != null) {
            b.scope(OAuth2Parameters.scopes(scope));
        }
        String value = config.get(prefix + "redirect-uri");
        if (value != null) {
            b.redirectUri(value);
        }
        value = config.get(prefix + "client-name");
        if (value != null) {
            b.clientName(value);
        }
        value = config.get(prefix + "response-mode");
        if (value != null) {
            b.responseMode(value);
        }
        String provider = PROVIDER + providerId + ".";
        value = config.get(provider + "issuer-uri");
        if (value != null) {
            b.issuerUri(value);
        }
        value = config.get(provider + "authorization-uri");
        if (value != null) {
            b.authorizationUri(value);
        }
        value = config.get(provider + "token-uri");
        if (value != null) {
            b.tokenUri(value);
        }
        value = config.get(provider + "user-info-uri");
        if (value != null) {
            b.userInfoUri(value);
        }
        value = config.get(provider + "user-emails-uri");
        if (value != null) {
            b.userEmailsUri(value);
        }
        value = config.get(provider + "jwk-set-uri");
        if (value != null) {
            b.jwkSetUri(value);
        }
        value = config.get(provider + "user-name-attribute");
        if (value != null) {
            b.userNameAttributeName(value);
        }
        value = config.get(provider + "authorization-response-iss-parameter-supported");
        if (value != null) {
            b.authorizationResponseIssParameterSupported("true".equals(value.trim()));
        }
        return b.build();
    }

    /// A builder with the endpoints of `issuer` read from its metadata now.
    ///
    /// - `IllegalArgumentException`: when the metadata cannot be read or names
    /// another issuer
    public static ClientRegistration.Builder fromIssuerLocation(String issuer) {
        return discover(ClientRegistration.withRegistrationId(host(issuer)).issuerUri(issuer)
                .clientId("unset").build(), RemoteJwkSet.WEB);
    }

    /// `registration` with the endpoints it does not name read from its
    /// issuer's metadata; itself when it names them all.
    public static ClientRegistration resolve(ClientRegistration registration,
                                             RemoteJwkSet.Fetcher fetcher) {
        ClientRegistration.ProviderDetails p = registration.getProviderDetails();
        boolean oidc = registration.getScopes().contains("openid");
        if (p.getAuthorizationUri() != null && p.getTokenUri() != null
                && (!oidc || p.getJwkSetUri() != null)) {
            return registration;
        }
        if (p.getIssuerUri() == null) {
            throw new IllegalArgumentException("The registration "
                    + registration.getRegistrationId() + " names neither a jwkSetUri nor an "
                    + "issuerUri to read one from");
        }
        return discover(registration, fetcher).build();
    }

    private static ClientRegistration.Builder discover(ClientRegistration registration,
                                                       RemoteJwkSet.Fetcher fetcher) {
        ClientRegistration.ProviderDetails p = registration.getProviderDetails();
        Map metadata = JwtDecoders.metadata(p.getIssuerUri(), fetcher);
        ClientRegistration.Builder b = ClientRegistration.withClientRegistration(registration);
        if (p.getAuthorizationUri() == null) {
            b.authorizationUri(text(metadata, "authorization_endpoint"));
        }
        if (p.getTokenUri() == null) {
            b.tokenUri(text(metadata, "token_endpoint"));
        }
        if (p.getJwkSetUri() == null) {
            b.jwkSetUri(text(metadata, "jwks_uri"));
        }
        if (p.getUserInfoUri() == null) {
            b.userInfoUri(text(metadata, "userinfo_endpoint"));
        }
        if (Boolean.TRUE.equals(metadata.get("authorization_response_iss_parameter_supported"))) {
            b.authorizationResponseIssParameterSupported(true);
        }
        if (p.getIdTokenAlgorithms() == null) {
            Object listed = metadata.get("id_token_signing_alg_values_supported");
            if (listed instanceof List) {
                List<String> names = new ArrayList<String>();
                for (Object name : (List) listed) {
                    if (name instanceof String) {
                        names.add((String) name);
                    }
                }
                b.idTokenAlgorithms(names.toArray(new String[names.size()]));
            }
        }
        return b;
    }

    private static String text(Map metadata, String name) {
        Object value = metadata.get(name);
        return value instanceof String && ((String) value).length() > 0 ? (String) value : null;
    }

    private static String host(String issuer) {
        StringBuilder sb = new StringBuilder();
        int start = issuer.indexOf("://");
        for (int iter = start < 0 ? 0 : start + 3 ; iter < issuer.length() ; iter++) {
            char c = issuer.charAt(iter);
            if (c == '/' || c == ':') {
                break;
            }
            sb.append((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' ? c : '_');
        }
        return sb.length() == 0 ? "issuer" : sb.toString();
    }
}
