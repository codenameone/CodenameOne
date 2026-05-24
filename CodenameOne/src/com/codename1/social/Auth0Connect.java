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

/// Sign-in via an Auth0 tenant. Auth0 is a fully OpenID-Connect compliant
/// provider so this class is a very thin convenience over
/// [com.codename1.io.oidc.OidcClient] -- it just builds the issuer URL from
/// the tenant domain and configures sensible defaults.
///
/// ```java
/// Auth0Connect.getInstance()
///     .withDomain("dev-xyz.us.auth0.com")
///     .signIn(
///         "YOUR_AUTH0_CLIENT_ID",
///         "com.example.app:/oauth2redirect",
///         "openid", "email", "profile")
///     .ready(new SuccessCallback<OidcTokens>() { ... });
/// ```
///
/// To request an Auth0 *audience* (so the access token can be used against
/// your custom API) pass it via [#withAudience(String)] before calling
/// [#signIn(String, String, String...)].
///
/// @since 7.1
public final class Auth0Connect extends Login {

    private static Auth0Connect INSTANCE;
    private String domain;
    private String audience;

    private Auth0Connect() {}

    public static synchronized Auth0Connect getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Auth0Connect();
        }
        return INSTANCE;
    }

    /// Auth0 tenant domain (e.g. `"dev-xyz.us.auth0.com"`). Do not include
    /// the protocol -- it is always `https://`.
    public Auth0Connect withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    /// Optional `audience` parameter for API authorization. When set, the
    /// access token issued by Auth0 will be a JWT valid against your API
    /// identifier instead of the default opaque token.
    public Auth0Connect withAudience(String audience) {
        this.audience = audience;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public String getAudience() {
        return audience;
    }

    @Override
    public boolean isNativeLoginSupported() {
        return false;
    }

    @Override
    protected boolean validateToken(String token) {
        return token != null && token.length() > 0;
    }

    public AsyncResource<OidcTokens> signIn(final String clientId,
                                            final String redirectUri,
                                            final String... scopes) {
        if (domain == null) {
            AsyncResource<OidcTokens> err = new AsyncResource<OidcTokens>();
            err.error(new IllegalStateException(
                    "Auth0Connect requires withDomain(\"your-tenant.region.auth0.com\")"));
            return err;
        }
        final AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        OidcClient.discover("https://" + domain)
                .ready(new SuccessCallback<OidcClient>() {
                    @Override
                    public void onSucess(OidcClient client) {
                        client.setClientId(clientId)
                                .setRedirectUri(redirectUri)
                                .setScopes(scopes != null && scopes.length > 0
                                        ? scopes
                                        : new String[] {"openid", "email",
                                                "profile", "offline_access"});
                        if (audience != null) {
                            client.setAuthorizationParameters("audience", audience);
                        }
                        client.authorize()
                                .ready(new SuccessCallback<OidcTokens>() {
                                    @Override
                                    public void onSucess(OidcTokens t) {
                                        setAccessToken(t.toAccessToken());
                                        out.complete(t);
                                    }
                                })
                                .except(new SuccessCallback<Throwable>() {
                                    @Override
                                    public void onSucess(Throwable err) {
                                        out.error(err);
                                    }
                                });
                    }
                })
                .except(new SuccessCallback<Throwable>() {
                    @Override
                    public void onSucess(Throwable err) {
                        out.error(err);
                    }
                });
        return out;
    }
}
