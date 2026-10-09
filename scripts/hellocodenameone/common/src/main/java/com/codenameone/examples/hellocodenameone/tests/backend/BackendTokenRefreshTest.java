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
package com.codenameone.examples.hellocodenameone.tests.backend;

import com.codename1.io.oidc.OidcClient;
import com.codename1.io.oidc.OidcException;
import com.codename1.io.oidc.OidcRequestAuthorizer;
import com.codename1.io.oidc.OidcTokens;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A refused access token, end to end. The token the authorizer holds is replaced
/// with one the server did not sign, which is what an expired one looks like to
/// the caller: a 401. The generated client's call must come back 200 all the same,
/// with the refresh token exchanged behind it; the server rotates the refresh
/// token, so the old one must then be refused; and when the refresh token itself
/// is bad the call delivers the 401, the tokens are dropped and the application is
/// told to sign in again.
public class BackendTokenRefreshTest extends BackendAuthTestBase {
    private MemoryTokenStore store;
    private OidcClient client;
    private OidcRequestAuthorizer authorizer;
    private OidcTokens issued;
    private SecureNotesApi api;
    private int signInRequired;

    @Override
    protected void defineSteps() {
        if (skippedOnBrowser()) {
            return;
        }
        authorizer = null;
        signInRequired = 0;
        step(() -> {
            final int id = currentStep();
            signIn("openid " + SCOPE_READ, tokens -> {
                issued = tokens;
                store = new MemoryTokenStore();
                client = newClient(store);
                authorizer = new OidcRequestAuthorizer(client).install(securedBase());
                authorizer.addSignInRequiredListener((source, reason) -> signInRequired++);
                authorizer.setTokens(withAccessToken(tokens, forged(tokens.getAccessToken())));
                api = SecureNotesApi.of(baseUrl());
                proceed(id);
            });
        });
        // The token is refused, renewed, and the call sent again -- all before the
        // callback, which sees one answer.
        step(() -> {
            final int id = currentStep();
            final int[] answers = new int[1];
            api.notes(r -> {
                answers[0]++;
                List<NoteDto> notes = r == null ? null : r.getResponseData();
                OidcTokens now = authorizer.getTokens();
                if (expect(answers[0] == 1, "the call was answered " + answers[0] + " times")
                        && expect(r != null && r.getResponseCode() == 200 && notes != null
                        && notes.size() >= 3, "after a refused token the call answered "
                        + (r == null ? null : Integer.valueOf(r.getResponseCode())))
                        && expect(now != null && !forged(issued.getAccessToken())
                        .equals(now.getAccessToken()) && !issued.getAccessToken()
                        .equals(now.getAccessToken()), "the access token was not renewed")
                        && expect(now.getRefreshToken() != null
                        && !issued.getRefreshToken().equals(now.getRefreshToken()),
                        "the refresh token did not rotate")
                        && expect(store.saved != null && now.getRefreshToken()
                        .equals(store.saved.getRefreshToken()), "the renewed tokens were not stored")
                        && expect(signInRequired == 0, "a successful renewal asked for a sign-in")) {
                    proceed(id);
                }
            });
        });
        // Bad again, and this time with nothing to renew it: the refresh token the
        // server already replaced. The call delivers its 401 and the session ends.
        step(() -> {
            final int id = currentStep();
            authorizer.setTokens(withAccessToken(issued, forged(issued.getAccessToken())));
            api.whoami(r -> {
                if (expect(r != null && r.getResponseCode() == 401, "with a spent refresh token "
                        + "the call answered " + (r == null ? null : Integer.valueOf(r.getResponseCode())))
                        && expect(signInRequired == 1, "the sign-in listener ran "
                        + signInRequired + " times")
                        && expect(!authorizer.isSignedIn(), "the refused tokens were kept")
                        && expect(store.saved == null && store.clears > 0,
                        "the refused tokens were left in the store")) {
                    proceed(id);
                }
            });
        });
        // What the server said about that refresh token, asked directly.
        step(() -> {
            final int id = currentStep();
            client.refresh(issued.getRefreshToken())
                    .ready(tokens -> failStep("a spent refresh token was exchanged again"))
                    .except(err -> {
                        String code = err instanceof OidcException
                                ? ((OidcException) err).getError() : String.valueOf(err);
                        if (expect("invalid_grant".equals(code), "a spent refresh token answered "
                                + code)) {
                            uninstall(authorizer);
                            proceed(id);
                        }
                    });
        });
    }

    /// The same token set with another access token.
    private static OidcTokens withAccessToken(OidcTokens tokens, String access) {
        Map<String, Object> json = new HashMap<String, Object>(tokens.getRawResponse());
        json.put("access_token", access);
        // Keep the issued identity: refresh must prove it is still the same subject.
        // Only the access token is corrupted to exercise the 401 renewal path.
        return OidcTokens.fromTokenResponse(json, tokens.getRefreshToken());
    }
}
