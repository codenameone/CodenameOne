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

import com.codename1.io.oidc.OidcRequestAuthorizer;
import com.codename1.io.oidc.OidcTokens;

import java.util.List;

/// Signing in to the backend and calling what it guards: no token is a 401 with a
/// challenge; an authorization code with PKCE becomes tokens; a generated
/// `@RestClient` reaches the secured API through the request authorizer with no
/// header written anywhere; method security answers by who the token is for; and
/// a scope the token lacks is a 403.
///
/// The notes it reads are rows a migration of the server inserted, so this is also
/// the device's proof that the server -- translated or on the JVM -- migrated its
/// database at start-up.
public class BackendAuthTest extends BackendAuthTestBase {
    private OidcRequestAuthorizer authorizer;
    private OidcTokens tokens;
    private SecureNotesApi api;

    @Override
    protected void defineSteps() {
        authorizer = null;
        tokens = null;
        step(() -> {
            final int id = currentStep();
            new Call("GET", "/api/secure/notes").send(call -> {
                // The header is the browser's to see on the browser port, where the
                // server would have to expose it to a page.
                if (expect(call.code == 401, "no token answered " + call.code)
                        && expect(isBrowser() || (call.challenge != null
                        && call.challenge.startsWith("Bearer")),
                        "the 401 challenged with " + call.challenge)) {
                    proceed(id);
                }
            });
        });
        if (skippedOnBrowser()) {
            return;
        }
        step(() -> {
            final int id = currentStep();
            signIn("openid profile " + SCOPE_READ, issued -> {
                tokens = issued;
                String name = issued.getIdToken() == null ? null : issued.getSubject();
                if (expect(issued.getRefreshToken() != null, "no refresh token was issued")
                        && expect(USER.equals(name), "the ID token is for " + name)
                        && expect(("openid profile " + SCOPE_READ).equals(issued.getScope()),
                        "granted scope " + issued.getScope())
                        && expect(!issued.isExpired(), "the tokens arrived expired")) {
                    MemoryTokenStore store = new MemoryTokenStore();
                    authorizer = new OidcRequestAuthorizer(newClient(store)).install(securedBase());
                    authorizer.setTokens(issued);
                    api = SecureNotesApi.of(baseUrl());
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            api.notes(r -> {
                List<NoteDto> notes = r == null ? null : r.getResponseData();
                if (expect(r != null && r.getResponseCode() == 200, "notes answered "
                        + (r == null ? null : Integer.valueOf(r.getResponseCode())))
                        && expect(notes != null && notes.size() >= 3
                        && "first".equals(notes.get(0).title)
                        && "seeded by migration V2".equals(notes.get(0).body),
                        "the migrated rows came back as " + describe(notes))) {
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            api.whoami(r -> {
                String body = r == null ? null : r.getResponseData();
                if (expect(body != null && body.indexOf("\"name\":\"" + USER + "\"") >= 0
                        && body.indexOf("SCOPE_" + SCOPE_READ) >= 0, "whoami answered " + body)) {
                    proceed(id);
                }
            });
        });
        // Method security: the service method wants the read scope AND the caller's
        // own name in the path.
        step(() -> {
            final int id = currentStep();
            api.summary(USER, r -> {
                String body = r == null ? null : r.getResponseData();
                if (expect(r != null && r.getResponseCode() == 200 && body != null
                        && body.indexOf("\"owner\":\"" + USER + "\"") >= 0,
                        "the caller's own summary answered " + body)) {
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            new Call("GET", "/api/secure/summary/" + MFA_USER)
                    .bearer(tokens.getAccessToken()).send(call -> {
                        if (expect(call.code == 403, "another user's summary answered "
                                + call.code + " " + call.text())) {
                            proceed(id);
                        }
                    });
        });
        // A scope the token was never granted.
        step(() -> {
            final int id = currentStep();
            new Call("GET", "/api/secure/admin")
                    .bearer(tokens.getAccessToken()).send(call -> {
                        if (expect(call.code == 403, "the write scope's route answered " + call.code)
                                && expect(call.challenge != null
                                && call.challenge.indexOf("insufficient_scope") >= 0,
                                "the 403 challenged with " + call.challenge)) {
                            proceed(id);
                        }
                    });
        });
        // The same refusal through the generated client: a 403 is not a 401, so it
        // is delivered as it is and nothing is renewed.
        step(() -> {
            final int id = currentStep();
            final String before = authorizer.getTokens().getAccessToken();
            api.admin(r -> {
                if (expect(r != null && r.getResponseCode() == 403, "the typed admin call answered "
                        + (r == null ? null : Integer.valueOf(r.getResponseCode())))
                        && expect(before.equals(authorizer.getTokens().getAccessToken()),
                        "a 403 renewed the token")) {
                    uninstall(authorizer);
                    proceed(id);
                }
            });
        });
    }

    private static String describe(List<NoteDto> notes) {
        if (notes == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder("[");
        for (int iter = 0; iter < notes.size(); iter++) {
            b.append(iter == 0 ? "" : ", ").append(notes.get(iter).title);
        }
        return b.append(']').toString();
    }
}
