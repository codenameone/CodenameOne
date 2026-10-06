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
import com.codename1.io.oidc.OidcDeviceAuthorization;
import com.codename1.io.oidc.OidcException;

import java.util.Map;

/// The device grant from both ends. The app is the device -- it asks for a code
/// and polls with `OidcClient` -- and it is also the user at another screen, who
/// signs in and approves the code on the server's verification page.
///
/// Three devices are started so no step has to out-wait the server's polling
/// interval: one is approved and polled by the client, one is polled twice in a
/// row to hear `authorization_pending` and then `slow_down`, and one is denied.
public class BackendDeviceGrantTest extends BackendAuthTestBase {
    private OidcClient client;
    private OidcDeviceAuthorization device;
    private String deviceCode;

    @Override
    protected void defineSteps() {
        if (skippedOnBrowser()) {
            return;
        }
        client = newClient(new MemoryTokenStore()).setScopes("openid", SCOPE_READ);
        step(() -> {
            final int id = currentStep();
            client.requestDeviceAuthorization()
                    .ready(started -> {
                        device = started;
                        if (expect(started.getUserCode() != null && started.getUserCode().length() == 9,
                                "the user code is " + started.getUserCode())
                                && expect(started.getVerificationUri() != null
                                && started.getVerificationUri().startsWith(baseUrl()),
                                "the verification address is " + started.getVerificationUri()
                                + ", not under " + baseUrl())
                                && expect(started.getInterval() > 0 && !started.isExpired(),
                                "interval " + started.getInterval())) {
                            proceed(id);
                        }
                    })
                    .except(err -> failStep("the device authorization request failed: " + err));
        });
        step(() -> {
            final int id = currentStep();
            decide(device.getUserCode(), "approve", "Device approved", () -> proceed(id));
        });
        // The client waits one interval, asks once, and has its tokens.
        step(() -> {
            final int id = currentStep();
            client.pollDeviceToken(device)
                    .ready(tokens -> new Call("GET", "/api/secure/whoami")
                            .bearer(tokens.getAccessToken()).send(call -> {
                                if (expect(call.code == 200 && call.text()
                                        .indexOf("\"name\":\"" + USER + "\"") >= 0,
                                        "the device's token answered " + call.code + " " + call.text())
                                        && expect(tokens.getRefreshToken() != null,
                                        "the device got no refresh token")) {
                                    proceed(id);
                                }
                            }))
                    .except(err -> failStep("polling for the approved device failed: " + err));
        });
        // A second device nobody approves: pending, and then told to slow down.
        step(() -> {
            final int id = currentStep();
            start(started -> {
                deviceCode = (String) started.get("device_code");
                poll(deviceCode, first -> {
                    if (expect(first.code == 400 && OidcException.AUTHORIZATION_PENDING
                            .equals(error(first)), "an unapproved device answered " + first.code
                            + " " + first.text())) {
                        poll(deviceCode, second -> {
                            if (expect(second.code == 400 && OidcException.SLOW_DOWN
                                    .equals(error(second)), "a device polling at once answered "
                                    + second.code + " " + second.text())) {
                                proceed(id);
                            }
                        });
                    }
                });
            });
        });
        // A third the user refuses.
        step(() -> {
            final int id = currentStep();
            start(started -> {
                final String code = (String) started.get("device_code");
                decide((String) started.get("user_code"), "deny", null, () -> poll(code, denied -> {
                    if (expect(denied.code == 400 && OidcException.ACCESS_DENIED.equals(error(denied)),
                            "a refused device answered " + denied.code + " " + denied.text())) {
                        proceed(id);
                    }
                }));
            });
        });
    }

    private interface Started {
        void got(Map<String, Object> answer);
    }

    private void start(final Started then) {
        new Call("POST", "/oauth2/device_authorization")
                .form("client_id", CLIENT_ID).form("scope", "openid " + SCOPE_READ)
                .send(call -> {
                    Map<String, Object> json = call.json();
                    if (expect(call.code == 200 && json != null && json.get("device_code") != null,
                            "device authorization answered " + call.code + " " + call.text())) {
                        then.got(json);
                    }
                });
    }

    private void poll(String code, Answer then) {
        new Call("POST", "/oauth2/token")
                .form("grant_type", DEVICE_GRANT).form("device_code", code)
                .form("client_id", CLIENT_ID).send(then);
    }

    private static String error(Call call) {
        Object error = field(call.json(), "error");
        return error == null ? null : error.toString();
    }

    /// The user's side: sign in, look the code up, and answer the question the
    /// page asks. The page binds its question to the session with a ticket, so the
    /// second request must carry the first one's cookie and the ticket it showed.
    private void decide(final String userCode, final String decision, final String expected,
            final Runnable then) {
        new Call("POST", "/oauth2/device_verification").basic(USER, USER_PASSWORD)
                .form("user_code", userCode).send(asked -> {
                    String page = asked.text();
                    String marker = "name=\"ticket\" value=\"";
                    int at = page.indexOf(marker);
                    if (!expect(asked.code == 200 && at > 0, "looking the code up answered "
                            + asked.code + " " + page)
                            || !expect(asked.session != null, "the verification page set no session")) {
                        return;
                    }
                    String ticket = page.substring(at + marker.length(),
                            page.indexOf('"', at + marker.length()));
                    new Call("POST", "/oauth2/device_verification").basic(USER, USER_PASSWORD)
                            .cookie(asked.session)
                            .form("user_code", userCode).form("ticket", ticket)
                            .form("decision", decision).send(decided -> {
                                if (expect(decided.code == 200 && (expected == null
                                        || decided.text().indexOf(expected) >= 0),
                                        "the decision answered " + decided.code + " " + decided.text())) {
                                    then.run();
                                }
                            });
                });
    }
}
