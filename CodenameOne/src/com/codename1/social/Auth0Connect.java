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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.io.oidc.OidcClient;
import com.codename1.io.oidc.OidcException;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.io.webauthn.PublicKeyCredential;
import com.codename1.io.webauthn.PublicKeyCredentialCreationOptions;
import com.codename1.io.webauthn.PublicKeyCredentialRequestOptions;
import com.codename1.io.webauthn.WebAuthnClient;
import com.codename1.util.AsyncResource;
import com.codename1.util.StringUtil;
import com.codename1.util.SuccessCallback;
import com.codename1.util.regex.StringReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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

    /// Signs the user in with an existing passkey via Auth0's WebAuthn grant.
    /// Routes through Auth0's `/passkey/challenge` + `/oauth/token` endpoints
    /// (`grant_type=urn:okta:params:oauth:grant-type:webauthn`).
    ///
    /// Requires the Auth0 tenant to have *Passkeys* enabled and the
    /// application to have the *WebAuthn* grant type allowed. The user must
    /// already have at least one passkey enrolled (use the standard `signIn`
    /// flow first and have the user enroll via Auth0's hosted page, or call
    /// [#registerPasskey(String, String, String, String, String...)] for a
    /// new account).
    ///
    /// `realm` is the Auth0 *Connection* name (most often
    /// `"Username-Password-Authentication"`).
    ///
    /// Available iOS 16+ and Android API 28+ via the system passkey
    /// providers. Fails fast with
    /// [com.codename1.io.webauthn.WebAuthnException#NOT_IMPLEMENTED] on
    /// platforms that don't have a WebAuthn implementation.
    ///
    public AsyncResource<OidcTokens> signInWithPasskey(final String clientId,
                                                       final String realm,
                                                       final String... scopes) {
        final AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        if (domain == null) {
            out.error(new IllegalStateException(
                    "Auth0Connect requires withDomain(\"your-tenant.region.auth0.com\")"));
            return out;
        }
        final String scopeArg = scopes == null || scopes.length == 0
                ? "openid email profile offline_access"
                : joinScopes(scopes);
        Map<String, String> body = new HashMap<String, String>();
        body.put("client_id", clientId);
        body.put("realm", realm == null ? "Username-Password-Authentication" : realm);
        postJson("https://" + domain + "/passkey/challenge", body)
                .ready(new SuccessCallback<Map<String, Object>>() {
                    @Override
                    public void onSucess(Map<String, Object> challenge) {
                        runPasskeyAssertion(clientId, scopeArg, challenge, out);
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

    /// Enrolls a brand-new passkey credential for the given Auth0 user. The
    /// account is created on first registration (if the connection allows
    /// signup), or attached to an existing passwordless account by email.
    ///
    /// The flow is:
    /// 1. POST `/passkey/register` with `client_id`, `realm`,
    ///    `user_profile`. Response includes registration options.
    /// 2. Run [WebAuthnClient#create] with those options.
    /// 3. POST `/oauth/token` to swap the authenticator response for tokens.
    ///
    public AsyncResource<OidcTokens> registerPasskey(final String clientId,
                                                     final String realm,
                                                     final String email,
                                                     final String displayName,
                                                     final String... scopes) {
        final AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        if (domain == null) {
            out.error(new IllegalStateException(
                    "Auth0Connect requires withDomain(\"your-tenant.region.auth0.com\")"));
            return out;
        }
        final String scopeArg = scopes == null || scopes.length == 0
                ? "openid email profile offline_access"
                : joinScopes(scopes);
        StringBuilder userProfile = new StringBuilder("{");
        boolean first = true;
        if (email != null) {
            userProfile.append("\"email\":").append(jsonString(email));
            first = false;
        }
        if (displayName != null) {
            if (!first) {
                userProfile.append(',');
            }
            userProfile.append("\"name\":").append(jsonString(displayName));
        }
        userProfile.append('}');
        Map<String, String> body = new HashMap<String, String>();
        body.put("client_id", clientId);
        body.put("realm", realm == null ? "Username-Password-Authentication" : realm);
        body.put("user_profile", userProfile.toString());
        postJson("https://" + domain + "/passkey/register", body)
                .ready(new SuccessCallback<Map<String, Object>>() {
                    @Override
                    public void onSucess(Map<String, Object> registration) {
                        runPasskeyRegistration(clientId, scopeArg, registration, out);
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

    private void runPasskeyAssertion(final String clientId,
                                     final String scopes,
                                     Map<String, Object> challenge,
                                     final AsyncResource<OidcTokens> out) {
        Object authSession = challenge.get("auth_session");
        Object authnParams = challenge.get("authn_params_public_key");
        if (authSession == null || !(authnParams instanceof Map)) {
            out.error(new OidcException(OidcException.INVALID_GRANT,
                    "Auth0 /passkey/challenge response missing auth_session / authn_params_public_key"));
            return;
        }
        String optionsJson;
        try {
            optionsJson = mapToJson((Map<?, ?>) authnParams);
        } catch (Throwable t) {
            out.error(new OidcException(OidcException.INVALID_GRANT,
                    "Could not serialise WebAuthn request options: " + t.getMessage(), t));
            return;
        }
        final String authSessionStr = authSession.toString();
        WebAuthnClient.getInstance()
                .get(PublicKeyCredentialRequestOptions.fromJson(optionsJson))
                .ready(new SuccessCallback<PublicKeyCredential>() {
                    @Override
                    public void onSucess(PublicKeyCredential cred) {
                        exchangePasskeyForToken(clientId, scopes, authSessionStr,
                                cred.toJson(), out);
                    }
                })
                .except(new SuccessCallback<Throwable>() {
                    @Override
                    public void onSucess(Throwable err) {
                        out.error(err);
                    }
                });
    }

    private void runPasskeyRegistration(final String clientId,
                                        final String scopes,
                                        Map<String, Object> registration,
                                        final AsyncResource<OidcTokens> out) {
        Object authSession = registration.get("auth_session");
        Object authnParams = registration.get("authn_params_public_key");
        if (authSession == null || !(authnParams instanceof Map)) {
            out.error(new OidcException(OidcException.INVALID_GRANT,
                    "Auth0 /passkey/register response missing auth_session / authn_params_public_key"));
            return;
        }
        String optionsJson;
        try {
            optionsJson = mapToJson((Map<?, ?>) authnParams);
        } catch (Throwable t) {
            out.error(new OidcException(OidcException.INVALID_GRANT,
                    "Could not serialise WebAuthn creation options: " + t.getMessage(), t));
            return;
        }
        final String authSessionStr = authSession.toString();
        WebAuthnClient.getInstance()
                .create(PublicKeyCredentialCreationOptions.fromJson(optionsJson))
                .ready(new SuccessCallback<PublicKeyCredential>() {
                    @Override
                    public void onSucess(PublicKeyCredential cred) {
                        exchangePasskeyForToken(clientId, scopes, authSessionStr,
                                cred.toJson(), out);
                    }
                })
                .except(new SuccessCallback<Throwable>() {
                    @Override
                    public void onSucess(Throwable err) {
                        out.error(err);
                    }
                });
    }

    private void exchangePasskeyForToken(String clientId,
                                         String scope,
                                         String authSession,
                                         String authnResponseJson,
                                         final AsyncResource<OidcTokens> out) {
        final ConnectionRequest req = new ConnectionRequest() {
            @Override
            protected void readResponse(InputStream input) throws IOException {
                byte[] body = Util.readInputStream(input);
                String json = StringUtil.newString(body);
                Map<String, Object> parsed = new JSONParser().parseJSON(new StringReader(json));
                if (parsed == null) {
                    out.error(new OidcException(OidcException.INVALID_GRANT,
                            "Auth0 /oauth/token returned an empty body"));
                    return;
                }
                if (parsed.get("error") != null) {
                    Object desc = parsed.get("error_description");
                    out.error(new OidcException(parsed.get("error").toString(),
                            desc != null ? desc.toString() : null));
                    return;
                }
                OidcTokens tokens = OidcTokens.fromTokenResponse(parsed, null);
                setAccessToken(tokens.toAccessToken());
                out.complete(tokens);
            }

            @Override
            protected void handleException(Exception err) {
                out.error(new OidcException(OidcException.TRANSPORT_ERROR,
                        "Auth0 passkey token exchange failed: " + err.getMessage(), err));
            }
        };
        req.setUrl("https://" + domain + "/oauth/token");
        req.setPost(true);
        req.setReadResponseForErrors(true);
        req.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        req.addRequestHeader("Accept", "application/json");
        req.addArgument("grant_type", "urn:okta:params:oauth:grant-type:webauthn");
        req.addArgument("client_id", clientId);
        req.addArgument("scope", scope);
        if (audience != null) {
            req.addArgument("audience", audience);
        }
        req.addArgument("auth_session", authSession);
        req.addArgument("authn_response", authnResponseJson);
        NetworkManager.getInstance().addToQueue(req);
    }

    private static AsyncResource<Map<String, Object>> postJson(final String url,
                                                               final Map<String, String> body) {
        final AsyncResource<Map<String, Object>> out = new AsyncResource<Map<String, Object>>();
        ConnectionRequest req = new ConnectionRequest() {
            @Override
            protected void readResponse(InputStream input) throws IOException {
                byte[] data = Util.readInputStream(input);
                String json = StringUtil.newString(data);
                Map<String, Object> parsed = new JSONParser().parseJSON(new StringReader(json));
                if (parsed == null) {
                    out.error(new OidcException(OidcException.INVALID_GRANT,
                            "Auth0 returned an empty body for " + url));
                    return;
                }
                if (parsed.get("error") != null) {
                    Object desc = parsed.get("error_description");
                    out.error(new OidcException(parsed.get("error").toString(),
                            desc != null ? desc.toString() : null));
                    return;
                }
                out.complete(parsed);
            }

            @Override
            protected void handleException(Exception err) {
                out.error(new OidcException(OidcException.TRANSPORT_ERROR,
                        "Auth0 request to " + url + " failed: " + err.getMessage(), err));
            }
        };
        req.setUrl(url);
        req.setPost(true);
        req.setReadResponseForErrors(true);
        req.addRequestHeader("Content-Type", "application/json");
        req.addRequestHeader("Accept", "application/json");
        req.setRequestBody(mapToFlatJson(body));
        NetworkManager.getInstance().addToQueue(req);
        return out;
    }

    private static String joinScopes(String[] scopes) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < scopes.length; i++) {
            if (i > 0) {
                b.append(' ');
            }
            b.append(scopes[i]);
        }
        return b.toString();
    }

    /// Serialises a parsed JSON sub-tree back into a JSON string. We need this
    /// because the Auth0 challenge response embeds the W3C options as a nested
    /// object, but [PublicKeyCredentialRequestOptions#fromJson] takes a string.
    private static String mapToJson(Map<?, ?> map) {
        StringBuilder b = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) {
                b.append(',');
            }
            first = false;
            b.append(jsonString(e.getKey().toString())).append(':');
            appendValue(b, e.getValue());
        }
        return b.append('}').toString();
    }

    private static void appendValue(StringBuilder b, Object v) {
        if (v == null) {
            b.append("null");
        } else if (v instanceof Map) {
            b.append(mapToJson((Map<?, ?>) v));
        } else if (v instanceof java.util.Collection) {
            b.append('[');
            boolean first = true;
            for (Object item : (java.util.Collection<?>) v) {
                if (!first) {
                    b.append(',');
                }
                first = false;
                appendValue(b, item);
            }
            b.append(']');
        } else if (v instanceof Number || v instanceof Boolean) {
            b.append(v.toString());
        } else {
            b.append(jsonString(v.toString()));
        }
    }

    /// Flat string-to-string JSON object serializer used for the small
    /// Auth0 request bodies. `user_profile` arrives as a JSON-shaped string
    /// and is emitted unquoted so the server receives a real JSON object.
    private static String mapToFlatJson(Map<String, String> map) {
        StringBuilder b = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!first) {
                b.append(',');
            }
            first = false;
            b.append(jsonString(e.getKey())).append(':');
            String val = e.getValue();
            // `user_profile` is already a JSON object; everything else is a
            // plain string. We detect this by the leading brace.
            if (val != null && val.length() > 0 && val.charAt(0) == '{') {
                b.append(val);
            } else {
                b.append(jsonString(val == null ? "" : val));
            }
        }
        return b.append('}').toString();
    }

    private static String jsonString(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8).append('"');
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n");  break;
                case '\r': out.append("\\r");  break;
                case '\t': out.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        out.append("\\u");
                        for (int p = hex.length(); p < 4; p++) {
                            out.append('0');
                        }
                        out.append(hex);
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append('"').toString();
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
