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

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.Customizer;
import com.codename1.backend.security.oauth2.jwt.RemoteJwkSet;

/// Starts a sign-in for a `GET` of `/oauth2/authorization/{registrationId}`.
///
/// ```java
/// DefaultOAuth2AuthorizationRequestResolver resolver =
///         new DefaultOAuth2AuthorizationRequestResolver(registrations);
/// resolver.setAuthorizationRequestCustomizer(request ->
///         request.additionalParameter("prompt", "select_account"));
/// http.oauth2Login(oauth2 -> oauth2.authorizationRequestResolver(resolver));
/// ```
public final class DefaultOAuth2AuthorizationRequestResolver
        implements OAuth2AuthorizationRequestResolver {
    /// The path a sign-in starts at, before the registration's id.
    public static final String DEFAULT_AUTHORIZATION_REQUEST_BASE_URI = "/oauth2/authorization";

    private final ClientRegistrationRepository registrations;
    private final String baseUri;
    private Customizer<OAuth2AuthorizationRequest.Builder> customizer;
    private RemoteJwkSet.Fetcher fetcher = RemoteJwkSet.WEB;
    private final java.util.Map<String, ClientRegistration> resolved =
            new java.util.HashMap<String, ClientRegistration>();

    public DefaultOAuth2AuthorizationRequestResolver(ClientRegistrationRepository registrations) {
        this(registrations, DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
    }

    /// @param authorizationRequestBaseUri the path before the registration's id
    public DefaultOAuth2AuthorizationRequestResolver(ClientRegistrationRepository registrations,
                                                     String authorizationRequestBaseUri) {
        if (registrations == null || authorizationRequestBaseUri == null
                || !authorizationRequestBaseUri.startsWith("/")) {
            throw new IllegalArgumentException("The registrations and a path are required");
        }
        this.registrations = registrations;
        this.baseUri = authorizationRequestBaseUri.endsWith("/") ? authorizationRequestBaseUri
                : authorizationRequestBaseUri + "/";
    }

    /// Changes every request before it is sent: adds a parameter, narrows the
    /// scopes.
    public void setAuthorizationRequestCustomizer(
            Customizer<OAuth2AuthorizationRequest.Builder> customizer) {
        this.customizer = customizer;
    }

    /// What reads an issuer's metadata; for tests.
    public void setFetcher(RemoteJwkSet.Fetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServer.Request request) {
        if (!"GET".equals(request.getMethod())) {
            return null;
        }
        String path = request.pathFrom(0);
        if (!path.startsWith(baseUri) || path.indexOf('/', baseUri.length()) >= 0) {
            return null;
        }
        ClientRegistration registration = registrations.findByRegistrationId(
                path.substring(baseUri.length()));
        if (registration == null) {
            return null;
        }
        registration = resolved(registration);
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(
                registration, redirectUri(request, registration));
        if (customizer != null) {
            customizer.customize(builder);
        }
        return builder.build();
    }

    private ClientRegistration resolved(ClientRegistration registration) {
        synchronized (resolved) {
            ClientRegistration known = resolved.get(registration.getRegistrationId());
            if (known != null) {
                return known;
            }
        }
        // Outside the lock: this reads the issuer's metadata over the network.
        ClientRegistration complete = ClientRegistrations.resolve(registration, fetcher);
        synchronized (resolved) {
            resolved.put(registration.getRegistrationId(), complete);
        }
        return complete;
    }

    /// The registration's redirect address with `{baseUrl}` and
    /// `{registrationId}` filled in.
    public static String redirectUri(HttpServer.Request request,
                                     ClientRegistration registration) {
        String template = registration.getRedirectUri();
        template = replace(template, "{registrationId}", registration.getRegistrationId());
        if (template.indexOf("{baseUrl}") >= 0) {
            String host = request.getHeader("Host");
            if (host == null || !isAuthority(host)) {
                throw new IllegalArgumentException("The request names no host to build the "
                        + "redirect address of " + registration.getRegistrationId() + " from; "
                        + "give the registration a whole redirectUri");
            }
            template = replace(template, "{baseUrl}",
                    (request.isSecure() ? "https://" : "http://") + host);
        }
        return template;
    }

    private static boolean isAuthority(String host) {
        if (host.length() == 0 || host.length() > 255) {
            return false;
        }
        for (int iter = 0 ; iter < host.length() ; iter++) {
            char c = host.charAt(iter);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == ':' || c == '[' || c == ']')) {
                return false;
            }
        }
        return true;
    }

    private static String replace(String text, String what, String with) {
        int at = text.indexOf(what);
        return at < 0 ? text : text.substring(0, at) + with + text.substring(at + what.length());
    }
}
