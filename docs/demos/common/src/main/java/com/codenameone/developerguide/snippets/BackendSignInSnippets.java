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
package com.codenameone.developerguide.snippets;

import com.codename1.io.NetworkManager;
import com.codename1.io.RequestAuthorizer;
import com.codename1.io.oidc.OidcClient;
import com.codename1.io.oidc.OidcDeviceAuthorization;
import com.codename1.io.oidc.OidcException;
import com.codename1.io.oidc.OidcRequestAuthorizer;
import com.codename1.io.oidc.SecureStorageTokenStore;
import com.codename1.io.rest.Response;
import com.codename1.io.rest.Rest;
import com.codename1.io.webauthn.PublicKeyCredentialCreationOptions;
import com.codename1.io.webauthn.PublicKeyCredentialRequestOptions;
import com.codename1.io.webauthn.WebAuthnClient;
import com.codename1.security.Base32;
import com.codename1.security.Otp;
import com.codename1.util.AsyncResource;
import com.codename1.io.oidc.OidcTokens;

public class BackendSignInSnippets {

    private static final String SERVER = "https://api.example.com";

    private OidcClient client;
    private OidcRequestAuthorizer authorizer;

    public void signIn() {
        // tag::backend-signin-authorize[]
        OidcClient.discover("https://api.example.com").ready(discovered -> {
            client = discovered
                    .setClientId("notes-app")
                    .setRedirectUri("com.example.notes:/oauth2redirect")
                    .setScopes("openid", "profile", "notes.read")
                    .setTokenStore(new SecureStorageTokenStore());
            client.authorize()
                    .ready(tokens -> showNotes())
                    .except(err -> showSignInFailed(err));
        });
        // end::backend-signin-authorize[]
    }

    public void tokenStores() {
        // tag::backend-signin-store[]
        // Encrypted by the platform, read without a prompt.
        client.setTokenStore(new SecureStorageTokenStore());

        // Behind Face ID or a fingerprint. Reading the tokens prompts the user.
        client.setTokenStore(new SecureStorageTokenStore()
                .requireBiometrics("Unlock your notes"));

        // Ordinary storage on a platform that has no secure storage.
        client.setTokenStore(new SecureStorageTokenStore()
                .allowPlainStorageFallback(true));
        // end::backend-signin-store[]
    }

    public void authorizeRequests() {
        // tag::backend-signin-authorizer[]
        authorizer = new OidcRequestAuthorizer(client).install("https://api.example.com");
        authorizer.addSignInRequiredListener((source, reason) -> showSignIn());
        authorizer.load().ready(saved -> {
            if (saved == null) {
                showSignIn();
            } else {
                showNotes();
            }
        });
        // end::backend-signin-authorizer[]
    }

    public void callTheApi() {
        // tag::backend-signin-call[]
        // No header to add: the authorizer covers every request under its base URL.
        Rest.get(SERVER + "/api/notes").acceptJson().fetchAsJsonMap(response -> {
            if (response.getResponseCode() == 200) {
                show(response.getResponseData());
            }
        });

        // A blocking call waits through the renewal too.
        Response<String> profile = Rest.get(SERVER + "/api/me").getAsString();
        // end::backend-signin-call[]
        show(profile);
    }

    public void perRequest(RequestAuthorizer other) {
        // tag::backend-signin-per-request[]
        // Another authorizer for one request.
        Rest.get("https://partner.example.org/feed").authorizer(other).fetchAsString(r -> show(r));

        // No authorizer for one request, not even the default.
        Rest.get(SERVER + "/api/public/status")
                .authorizer(RequestAuthorizer.NONE)
                .fetchAsString(r -> show(r));

        // A header you set yourself always wins.
        Rest.get(SERVER + "/api/notes").bearer("a-token-from-elsewhere").fetchAsString(r -> show(r));
        // end::backend-signin-per-request[]
    }

    public void signOut() {
        // tag::backend-signin-signout[]
        OidcTokens current = authorizer.getTokens();
        if (current != null && current.getRefreshToken() != null) {
            client.revoke(current.getRefreshToken());
        }
        authorizer.signOut();
        // end::backend-signin-signout[]
    }

    public void deviceGrant() {
        // tag::backend-signin-device[]
        client.requestDeviceAuthorization().ready(device -> {
            showCode(device.getUserCode(), device.getVerificationUri());
            AsyncResource<OidcTokens> waiting = client.pollDeviceToken(device);
            waiting.ready(tokens -> showNotes());
            waiting.except(err -> {
                if (err instanceof OidcException
                        && OidcException.EXPIRED_TOKEN.equals(((OidcException) err).getError())) {
                    showCodeExpired();
                } else {
                    showSignInFailed(err);
                }
            });
        });
        // end::backend-signin-device[]
    }

    public void registerPasskey() {
        // tag::backend-signin-passkey-register[]
        // The user is signed in, so the authorizer's token goes with both requests.
        String options = Rest.post(SERVER + "/webauthn/register/options").getAsString().getResponseData();
        WebAuthnClient.getInstance()
                .create(PublicKeyCredentialCreationOptions.fromJson(options))
                .ready(credential -> Rest.post(SERVER + "/webauthn/register")
                        .queryParam("label", "My phone")
                        .jsonContent()
                        .body(credential.toJson())
                        .fetchAsString(r -> show(r)));
        // end::backend-signin-passkey-register[]
    }

    public void signInWithPasskey() {
        // tag::backend-signin-passkey[]
        String options = Rest.post(SERVER + "/webauthn/authenticate/options").getAsString().getResponseData();
        WebAuthnClient.getInstance()
                .get(PublicKeyCredentialRequestOptions.fromJson(options))
                .ready(credential -> Rest.post(SERVER + "/login/webauthn")
                        .jsonContent()
                        .useBoolean(true)
                        .body(credential.toJson())
                        .fetchAsJsonMap(r -> {
                            if (Boolean.TRUE.equals(r.getResponseData().get("authenticated"))) {
                                // The answer set the session cookie, and later requests send it.
                                showNotes();
                            }
                        }));
        // end::backend-signin-passkey[]
    }

    public String totp(String base32Secret) {
        // tag::backend-signin-totp[]
        String code = Otp.totp(Base32.decode(base32Secret));
        // end::backend-signin-totp[]
        return code;
    }

    private void showNotes() {
    }

    private void showSignIn() {
    }

    private void showSignInFailed(Throwable err) {
    }

    private void showCode(String userCode, String verificationUri) {
    }

    private void showCodeExpired() {
    }

    private void show(Object value) {
    }
}
