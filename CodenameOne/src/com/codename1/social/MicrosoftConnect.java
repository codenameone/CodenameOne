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
package com.codename1.social;

import com.codename1.io.oidc.OidcClient;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

/// Sign-in with a Microsoft account (personal, work, or school) backed by
/// Microsoft Entra ID (formerly Azure Active Directory). Wraps
/// [com.codename1.io.oidc.OidcClient] against Microsoft's
/// `v2.0/.well-known/openid-configuration` endpoint.
///
/// On iOS and Android, where Microsoft ships the MSAL SDK with broker
/// integration (Microsoft Authenticator, Company Portal), this class still
/// uses the system browser flow -- MSAL's broker is only available when the
/// app embeds the native MSAL SDK and is configured for the conditional
/// access scenarios that require it. For 95% of Codename One apps the
/// system-browser flow is the right answer, and lets the same code work in
/// the simulator and on the web port.
///
/// ```java
/// MicrosoftConnect.getInstance()
///     .withTenant("common")             // or your tenant GUID
///     .signIn(
///         "YOUR_CLIENT_ID",
///         "com.example.app:/oauth2redirect",
///         "openid", "email", "profile", "User.Read")
///     .ready(new SuccessCallback<OidcTokens>() {
///         public void onSucess(OidcTokens t) { ... }
///     });
/// ```
///
/// @since 8.0
public final class MicrosoftConnect extends Login {

    /// "common" -- accepts personal, work, and school accounts. Use this for
    /// most multi-tenant apps. Pass a tenant GUID to restrict to a single
    /// Entra ID tenant; pass "organizations" for work/school only;
    /// "consumers" for personal only.
    public static final String COMMON_TENANT = "common";

    private static MicrosoftConnect INSTANCE;
    private String tenant = COMMON_TENANT;

    private MicrosoftConnect() {}

    public static synchronized MicrosoftConnect getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MicrosoftConnect();
        }
        return INSTANCE;
    }

    /// Picks the Entra ID tenant to target. Pass [#COMMON_TENANT],
    /// `"organizations"`, `"consumers"`, or a tenant GUID / verified domain
    /// (e.g. `"contoso.onmicrosoft.com"`).
    public MicrosoftConnect withTenant(String tenant) {
        this.tenant = tenant != null ? tenant : COMMON_TENANT;
        return this;
    }

    public String getTenant() {
        return tenant;
    }

    @Override
    public boolean isNativeLoginSupported() {
        return false;
    }

    @Override
    protected boolean validateToken(String token) {
        return token != null && token.length() > 0;
    }

    /// Drives a full authorization-code-with-PKCE sign-in through the system
    /// browser and resolves with the issued tokens.
    public AsyncResource<OidcTokens> signIn(final String clientId,
                                            final String redirectUri,
                                            final String... scopes) {
        final AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        String issuer = "https://login.microsoftonline.com/" + tenant + "/v2.0";
        OidcClient.discover(issuer)
                .ready(new DiscoveredCallback(this, clientId, redirectUri, scopes, out))
                .except(new ErrorCallback(out));
        return out;
    }

    /// Static so SpotBugs SIC_INNER_SHOULD_BE_STATIC_ANON stays quiet.
    /// `host` is passed explicitly because [Login#setAccessToken] is an
    /// instance method.
    private static final class DiscoveredCallback implements SuccessCallback<OidcClient> {
        private final MicrosoftConnect host;
        private final String clientId;
        private final String redirectUri;
        private final String[] scopes;
        private final AsyncResource<OidcTokens> out;

        DiscoveredCallback(MicrosoftConnect host, String clientId, String redirectUri,
                           String[] scopes, AsyncResource<OidcTokens> out) {
            this.host = host;
            this.clientId = clientId;
            this.redirectUri = redirectUri;
            this.scopes = scopes;
            this.out = out;
        }

        public void onSucess(OidcClient client) {
            client.setClientId(clientId)
                    .setRedirectUri(redirectUri)
                    .setScopes(scopes != null && scopes.length > 0
                            ? scopes
                            : new String[] {"openid", "email", "profile",
                                    "offline_access"});
            client.authorize()
                    .ready(new AuthorizedCallback(host, out))
                    .except(new ErrorCallback(out));
        }
    }

    private static final class AuthorizedCallback implements SuccessCallback<OidcTokens> {
        private final MicrosoftConnect host;
        private final AsyncResource<OidcTokens> out;

        AuthorizedCallback(MicrosoftConnect host, AsyncResource<OidcTokens> out) {
            this.host = host;
            this.out = out;
        }

        public void onSucess(OidcTokens t) {
            host.setAccessToken(t.toAccessToken());
            out.complete(t);
        }
    }

    private static final class ErrorCallback implements SuccessCallback<Throwable> {
        private final AsyncResource<OidcTokens> out;

        ErrorCallback(AsyncResource<OidcTokens> out) {
            this.out = out;
        }

        public void onSucess(Throwable err) {
            out.error(err);
        }
    }
}
