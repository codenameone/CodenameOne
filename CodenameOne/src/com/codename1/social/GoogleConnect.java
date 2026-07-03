/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.social;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Oauth2;
import com.codename1.io.oidc.OidcClient;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

import java.util.Arrays;
import java.util.Hashtable;

/// Sign-in-with-Google for Codename One.
///
/// As of 2025 Google replaced the legacy Sign-In SDK with the Google Identity
/// Services (GIS) family of APIs. GIS encourages the OAuth 2.0 authorization
/// code flow with PKCE driven from the system browser -- exactly what
/// [com.codename1.io.oidc.OidcClient] does. New apps should call
/// [#signIn(String, String, String[])] which goes through that modern path
/// and works on every Codename One platform without a native SDK dependency.
///
/// The older [#doLogin()] / [#nativelogin()] path remains for source
/// compatibility, and on iOS / Android it still delegates to the native
/// implementation provided by the port (see `Ports/iOSPort` and
/// `Ports/Android`). On other platforms the legacy path also now goes
/// through `OidcClient` instead of the deprecated [Oauth2] in-app WebView.
///
/// @author Chen
public class GoogleConnect extends Login {

    /// Google's well-known OIDC issuer.
    public static final String GOOGLE_ISSUER = "https://accounts.google.com";

    private static final String tokenURL = "https://www.googleapis.com/oauth2/v3/token";
    private static final Object INSTANCE_LOCK = new Object();
    static Class<?> implClass;
    private static GoogleConnect instance;


    GoogleConnect() {
        setOauth2URL("https://accounts.google.com/o/oauth2/auth");
        setScope("profile email");
    }

    /// Gets the GoogleConnect singleton instance
    /// .
    ///
    /// #### Returns
    ///
    /// the GoogleConnect instance
    public static GoogleConnect getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                if (implClass != null) {
                    try {
                        instance = (GoogleConnect) implClass.newInstance();
                    } catch (Throwable t) {
                        instance = new GoogleConnect();
                    }
                } else {
                    instance = new GoogleConnect();
                }
            }
            return instance;
        }
    }

    static void setImplClass(Class<?> implClass) {
        GoogleConnect.implClass = implClass;
    }

    @Override
    public boolean isNativeLoginSupported() {
        return false;
    }

    @Override
    protected Oauth2 createOauth2() {
        Hashtable params = new Hashtable();
        params.put("approval_prompt", "force");
        params.put("access_type", "offline");

        return new Oauth2(oauth2URL, clientId, redirectURI, scope, tokenURL, clientSecret, params);
    }

    /// Modern Google sign-in. Goes through the Google Identity Services OIDC
    /// endpoints with PKCE, using the system browser. Works the same on every
    /// platform (iOS, Android, JavaSE, Web) provided the platform port wires
    /// the system browser native interface; otherwise it falls back to an
    /// in-app browser window.
    ///
    /// #### Parameters
    ///
    /// - `clientId`: OAuth 2.0 client ID issued in Google Cloud Console.
    ///   Use the *iOS / Android* client for the matching native build, or the
    ///   *Web* client when running in the simulator / web port.
    /// - `redirectUri`: Redirect URI registered for that client. Custom
    ///   schemes (`com.example.app:/oauth2redirect`) for mobile; HTTPS
    ///   for web.
    /// - `scopes`: OAuth scopes to request -- include `openid email profile`
    ///   to get an ID token plus user metadata, plus any Google API scopes
    ///   you need.
    ///
    /// #### Returns
    ///
    /// An [AsyncResource] resolving to the [OidcTokens] for the signed-in
    /// user.
    ///
    public AsyncResource<OidcTokens> signIn(final String clientId,
                                            final String redirectUri,
                                            final String... scopes) {
        final AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        OidcClient.discover(GOOGLE_ISSUER)
                .ready(new SuccessCallback<OidcClient>() {
                    @Override
                    public void onSucess(OidcClient client) {
                        client.setClientId(clientId)
                                .setRedirectUri(redirectUri)
                                .setScopes(scopes != null && scopes.length > 0
                                        ? scopes
                                        : new String[] {"openid", "email", "profile"})
                                // `access_type=offline` is Google-specific and is needed
                                // to get a refresh token; `prompt=consent` forces the
                                // refresh-token grant on subsequent sign-ins.
                                .setAuthorizationParameters(
                                        "access_type", "offline",
                                        "prompt", "consent");
                        client.authorize()
                                .ready(new SuccessCallback<OidcTokens>() {
                                    @Override
                                    public void onSucess(OidcTokens tokens) {
                                        setAccessToken(tokens.toAccessToken());
                                        out.complete(tokens);
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

    @Override
    protected boolean validateToken(String token) {
        //make a call to the API if the return value is 40X the token is not 
        //valid anymore
        final boolean[] retval = new boolean[1];
        retval[0] = true;
        ConnectionRequest req = new ValidateTokenConnectionRequest(retval);
        req.setPost(false);
        req.setUrl("https://www.googleapis.com/plus/v1/people/me");
        req.addRequestHeader("Authorization", "Bearer " + token);
        NetworkManager.getInstance().addToQueueAndWait(req);
        return retval[0];

    }

    private static class ValidateTokenConnectionRequest extends ConnectionRequest {
        private final boolean[] retval;

        public ValidateTokenConnectionRequest(boolean[] retval) {
            this.retval = retval;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof ValidateTokenConnectionRequest)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            ValidateTokenConnectionRequest that = (ValidateTokenConnectionRequest) o;
            return Arrays.equals(retval, that.retval);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + Arrays.hashCode(retval);
            return result;
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            //access token not valid anymore
            if (code >= 400 && code <= 410) {
                retval[0] = false;
                return;
            }
            super.handleErrorResponseCode(code, message);
        }

    }
}
