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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.ConnectionRequest.SSLCertificate;
import com.codename1.io.Log;
import com.codename1.io.NetworkGuard;
import com.codename1.io.NetworkManager;
import com.codename1.io.WebSocket;
import com.codename1.security.DeviceIntegrity;
import com.codename1.security.shield.AppShield;
import com.codename1.security.shield.FailureMode;
import com.codename1.security.shield.HostPolicy;
import com.codename1.security.shield.PinSet;
import com.codename1.security.shield.ShieldConfig;
import com.codename1.security.shield.ShieldException;
import com.codename1.security.shield.ShieldListener;
import com.codename1.security.shield.ShieldSignal;
import com.codename1.security.shield.ShieldSignals;
import com.codename1.security.shield.ShieldStatus;
import com.codename1.security.shield.ShieldToken;
import com.codename1.ui.BrowserComponent;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import java.io.IOException;
import java.util.Hashtable;

/** Compiled source snippets for the App Shield guide chapter. */
public class AppShieldSnippets {

    /** Startup: register the hosts, then make requests exactly as before. */
    public void init() {
        // tag::shield-init[]
        AppShield.init(new ShieldConfig()
                // Money moves here, so refuse the request when no token can be obtained.
                .protect("api.mybank.example", HostPolicy.ENFORCED)
                // Everything else under the domain gets a token when one is available.
                .protect("*.mybank.example", HostPolicy.PROTECTED));

        // From here on nothing changes at the call sites. A request to a protected
        // host is given its token and its certificate check on the network thread.
        ConnectionRequest r = new ConnectionRequest("https://api.mybank.example/transfer", true);
        NetworkManager.getInstance().addToQueueAndWait(r);
        // end::shield-init[]
    }

    /** The same registration, with the default failure mode moved to fail-closed. */
    public void failClosedByDefault() {
        // tag::shield-fail-closed-default[]
        AppShield.init(new ShieldConfig()
                .defaultFailureMode(FailureMode.CLOSED)
                // Registered the short way, so it picks up the default above.
                .protect("api.mybank.example")
                // And this one opts back out, because an outage here should degrade
                // rather than block the app.
                .protect("images.mybank.example", HostPolicy.PROTECTED));
        // end::shield-fail-closed-default[]
    }

    /** A backend the app only learns about after login. */
    public void addHostAtRuntime(String region) {
        // tag::shield-runtime-host[]
        // Exact host or a "*." wildcard, resolved the way the configured ones are:
        // the most specific rule wins, and a runtime registration beats a configured
        // one for the same pattern.
        AppShield.addProtectedHost(region + ".api.mybank.example", HostPolicy.ENFORCED);
        AppShield.addProtectedHost("*.cdn.mybank.example", HostPolicy.PROTECTED);
        // end::shield-runtime-host[]
    }

    /** Reacting to what the service says about this device. */
    public void listen() {
        // tag::shield-listener[]
        AppShield.addListener(new ShieldListener() {
            public void statusChanged(ShieldStatus status) {
                if (status.isSuccess()) {
                    return;
                }
                if (status.isTransient()) {
                    // The service could not be reached. This is a bad connection, not a
                    // bad device -- treating it like a rejection is how an app locks out
                    // users on a train.
                    Log.p("AppShield: attestation unavailable (" + status.getId() + ")");
                    return;
                }
                // REJECTED or PIN_MISMATCH: the service evaluated this device and
                // declined, so degrade rather than retry.
                disableTransfers();
            }

            public void signalRaised(ShieldSignal signal) {
                // Informational. The server decides what a signal means -- an emulator
                // signal is normal on a developer's machine.
                Log.p("AppShield signal " + signal.getId() + " severity "
                        + signal.getSeverity());
            }

            public void tokenRefreshed(ShieldToken token) {
            }
        });
        // end::shield-listener[]
    }

    /** Asking for a token directly, for a path the guard does not cover. */
    public void fetchToken() {
        // tag::shield-fetch-token[]
        AsyncResource<ShieldToken> pending = AppShield.fetchToken();
        pending.ready(new SuccessCallback<ShieldToken>() {
            public void onSucess(ShieldToken token) {
                sendToMyBackend(token.getValue());
            }
        });
        pending.except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable error) {
                ShieldStatus status = ((ShieldException) error).getStatus();
                Log.p("AppShield: no token (" + status.getId() + ")");
            }
        });
        // end::shield-fetch-token[]
    }

    /** Binding a token to the request it was minted for. */
    public void fetchBoundToken(String payload) {
        // tag::shield-bound-token[]
        // The digest travels in the token, so a token lifted off this request and
        // replayed on another one does not verify against the other one's body.
        AppShield.fetchToken(sha256(payload)).ready(new SuccessCallback<ShieldToken>() {
            public void onSucess(ShieldToken token) {
                sendToMyBackend(token.getValue());
            }
        });
        // end::shield-bound-token[]
    }

    /** Decorating a request by hand, off the EDT. */
    public void attachByHand() throws IOException {
        // tag::shield-attach[]
        ConnectionRequest r = new ConnectionRequest("https://api.mybank.example/transfer", true);
        try {
            // Blocks on the attestation round trip, so never on the EDT. In normal use
            // the guard does this for you on the network thread.
            AppShield.attach(r);
        } catch (ShieldException err) {
            if (ShieldStatus.PIN_MISMATCH.equals(err.getStatus())) {
                // The chain presented for a pinned host matched no pin. Nothing was
                // sent, and this is the one status worth surfacing to the user: it
                // usually means the connection is being intercepted.
                warnAboutInterception();
                return;
            }
            // A fail-closed host with no token. Retry later if the status is transient.
            Log.e(err);
            return;
        }
        NetworkManager.getInstance().addToQueueAndWait(r);
        // end::shield-attach[]
    }

    /** Rejection feedback from a backend that verifies the token itself. */
    public void handleBackendRejection() {
        // tag::shield-reject-header[]
        ConnectionRequest r = new ConnectionRequest("https://api.mybank.example/transfer", true) {
            @Override
            protected void readHeaders(Object connection) throws IOException {
                // Your middleware sets this header when the ATTESTATION was the problem,
                // as opposed to the user's own credentials. Without it a 401 or 403 is
                // ambiguous, and an app re-attests its way through a wrong password.
                if (getHeader(connection, AppShield.REJECT_HEADER) != null) {
                    AppShield.invalidateToken();
                }
            }
        };
        NetworkManager.getInstance().addToQueueAndWait(r);
        // end::shield-reject-header[]
    }

    /** What is pinned right now. */
    public void inspectPins() {
        // tag::shield-pins[]
        PinSet pins = AppShield.getPinSet();
        if (!pins.isEnforcedFor("api.mybank.example")) {
            // No published pins for this host yet, or the set has hard-expired on a
            // long-offline device. Pinning is not enforced, and the app still works.
            Log.p("AppShield: no pins in force (version " + pins.getVersion() + ")");
        }
        // end::shield-pins[]
    }

    /** Reporting something only the app can notice. */
    public void reportSignal() {
        // tag::shield-custom-signal[]
        // Severity is 0-100. The service applies the policy; the client only reports.
        ShieldSignals.add("serverStateMismatch", 60,
                "balance disagreed with the ledger after a retry");

        // The framework's own detections arrive the same way.
        for (ShieldSignal s : AppShield.getSignals()) {
            if (ShieldSignal.HOOK.equals(s.getId())) {
                requireStepUpAuth();
            }
        }
        // end::shield-custom-signal[]
    }

    /** Paths that do not go through ConnectionRequest. */
    public void otherTransports(BrowserComponent browser, String token) {
        // tag::shield-other-transports[]
        // BrowserComponent: the INITIAL navigation only. Requests the loaded page makes
        // itself are invisible to the framework and cannot be given a token or pinned.
        Hashtable headers = AppShield.headersFor("https://api.mybank.example/statement");
        browser.setURL("https://api.mybank.example/statement", headers);

        // WebSocket: the handshake carries the token. Emitted on Android, desktop,
        // Windows and Linux; silently dropped on iOS and in the browser, whose platform
        // sockets expose no way to add a header.
        WebSocket.build("wss://api.mybank.example/stream")
                .header("X-CN1-Attest", token)
                .connect();
        // end::shield-other-transports[]
    }

    /** Keeping your own guard without displacing the shield's. */
    public void composeGuard() {
        // tag::shield-compose-guard[]
        // NetworkManager holds exactly one guard and seals the slot, so an app with its
        // own has to delegate rather than replace -- calling attach() by hand instead
        // would restore the token and silently drop the certificate callbacks that
        // enforce pinning.
        final NetworkGuard shield = AppShield.getNetworkGuard();
        NetworkManager.setNetworkGuard(new NetworkGuard() {
            public void beforeRequest(ConnectionRequest r) throws IOException {
                r.addRequestHeader("X-My-Trace", newTraceId());
                shield.beforeRequest(r);
            }

            public boolean isCertificateCheckRequired(String url) {
                return shield.isCertificateCheckRequired(url);
            }

            public void checkCertificates(ConnectionRequest r, SSLCertificate[] chain)
                    throws IOException {
                shield.checkCertificates(r, chain);
            }

            public String[] interestingResponseHeaders() {
                return shield.interestingResponseHeaders();
            }

            public void afterResponse(ConnectionRequest r, int code, String[] headers) {
                shield.afterResponse(r, code, headers);
            }
        });
        // end::shield-compose-guard[]
    }

    /** Telling the client that the backend has registered this iOS key. */
    public void confirmAttestation(String keyId) {
        // tag::shield-confirm-attestation[]
        // iOS App Attest is attest-once, assert-many: the FIRST token carries the public
        // key, every later one is an assertion naming it. Call this once your backend
        // has stored that key, or a device whose registration call was lost keeps
        // sending assertions for a key the server never saw.
        DeviceIntegrity.confirmAttestation(keyId);
        // end::shield-confirm-attestation[]
    }

    // --- fixtures, not part of any snippet ------------------------------

    private void disableTransfers() {
    }

    private void warnAboutInterception() {
    }

    private void requireStepUpAuth() {
    }

    private void sendToMyBackend(String token) {
    }

    private String newTraceId() {
        return "trace";
    }

    private String sha256(String value) {
        return com.codename1.util.Base64.encodeNoNewline(value.getBytes());
    }
}
