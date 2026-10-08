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

import com.codename1.security.Base32;
import com.codename1.security.Hash;
import com.codename1.security.Otp;

/// A one-time code computed on the device, accepted by the server. The user has a
/// second factor, so a correct password only gets as far as `/login/mfa`; the code
/// from `com.codename1.security.Otp` -- the class the server verifies with --
/// finishes the sign-in. The same code is then refused for a second sign-in: a
/// code is good once.
///
/// The server remembers the last code it accepted for as long as it runs, so a
/// suite run twice inside thirty seconds against one server would have its first
/// code refused as a replay. The test then offers the next time step's code, which
/// is inside the server's allowed clock drift and newer than anything it has seen.
public class BackendTotpTest extends BackendAuthTestBase {
    private long now;
    private String accepted;
    /// The session of the sign-in in progress: half signed in after the password,
    /// replaced by the server once the code is accepted.
    private String session;

    @Override
    protected void defineSteps() {
        if (skippedOnBrowser()) {
            return;
        }
        accepted = null;
        step(() -> {
            final int id = currentStep();
            password(() -> {
                now = System.currentTimeMillis();
                offer(code(now), first -> {
                    if ("/account/me".equals(path(first.location))) {
                        keep(first);
                        accepted = code(now);
                        proceed(id);
                        return;
                    }
                    // Refused: either this server accepted the same code moments ago,
                    // or something is wrong. The next step's code tells them apart.
                    offer(code(now + 30000L), second -> {
                        if (expect(second.code == 302 && "/account/me".equals(path(second.location)),
                                "neither this time step's code nor the next was accepted: "
                                + first.code + " " + first.location + ", then " + second.code
                                + " " + second.location)) {
                            keep(second);
                            accepted = code(now + 30000L);
                            proceed(id);
                        }
                    });
                });
            });
        });
        // The session is the user's now.
        step(() -> {
            final int id = currentStep();
            new Call("GET", "/account/me").unfollowed().cookie(session).send(call -> {
                if (expect(call.code == 200 && call.text().indexOf("\"name\":\"" + MFA_USER + "\"") >= 0,
                        "after the second factor /account/me answered " + call.code + " "
                        + call.text() + " " + call.location)) {
                    proceed(id);
                }
            });
        });
        // A second sign-in, offered the code that was just used.
        step(() -> {
            final int id = currentStep();
            password(() -> offer(accepted, replayed -> {
                if (!expect(replayed.code == 302 && path(replayed.location) != null
                        && path(replayed.location).startsWith("/login/mfa"),
                        "a replayed code answered " + replayed.code + " " + replayed.location)) {
                    return;
                }
                new Call("GET", "/account/me").unfollowed().cookie(session).send(call -> {
                    if (expect(call.code == 302, "a sign-in that stopped at the second factor "
                            + "reached /account/me: " + call.code + " " + call.text())) {
                        proceed(id);
                    }
                });
            }));
        });
    }

    private static String code(long at) {
        return Otp.totp(Base32.decode(MFA_SECRET), at, 30, 6, Hash.SHA1);
    }

    /// The first step: the password, which must lead to the second factor's page.
    private void password(final Runnable then) {
        new Call("POST", "/login").unfollowed()
                .form("username", MFA_USER).form("password", MFA_USER_PASSWORD).send(call -> {
                    if (expect(call.code == 302 && "/login/mfa".equals(path(call.location)),
                            "the password step answered " + call.code + " " + call.location)
                            && expect(call.session != null, "the password step set no session")) {
                        session = call.session;
                        then.run();
                    }
                });
    }

    private void offer(String code, Answer then) {
        new Call("POST", "/login/mfa").unfollowed().cookie(session).form("code", code).send(then);
    }

    /// The server replaces the session when the second factor is accepted.
    private void keep(Call accepted) {
        if (accepted.session != null) {
            session = accepted.session;
        }
    }

    /// The path and query of a redirect, whether the server wrote it whole or relative.
    private static String path(String location) {
        if (location == null) {
            return null;
        }
        int scheme = location.indexOf("://");
        if (scheme < 0) {
            return location;
        }
        int slash = location.indexOf('/', scheme + 3);
        return slash < 0 ? "/" : location.substring(slash);
    }
}
