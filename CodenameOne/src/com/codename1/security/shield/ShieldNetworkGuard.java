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
package com.codename1.security.shield;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.Log;
import com.codename1.io.NetworkGuard;
import com.codename1.security.shield.spi.ShieldEngineRegistry;
import java.io.IOException;

/// Connects [AppShield] to the network stack.
///
/// This is what makes [ShieldConfig#protect(String, HostPolicy)] mean anything: without a guard
/// installed, registering a host would configure a policy that nothing ever consults, and tokens
/// would only ever be attached by an app calling [AppShield#attach(ConnectionRequest)] by hand.
/// Installed once from [AppShield#init(ShieldConfig)].
///
/// Package-private: apps configure behaviour through [ShieldConfig], and an app-supplied guard
/// could only weaken this one.
final class ShieldNetworkGuard implements NetworkGuard {

    @Override
    public void beforeRequest(ConnectionRequest request) throws IOException {
        // Also clears the header when the host is not protected, which is what
        // stops a token following a cross-host redirect.
        AppShield.attach(request);
    }

    @Override
    public boolean isCertificateCheckRequired(String url) {
        String host = AppShield.hostOf(url);
        if (host == null || !AppShield.policyFor(host).isEnforcePins()) {
            return false;
        }
        // Only ask for the richer certificate details when a pin set actually
        // covers this host -- collecting them has a per-connection cost, and an
        // unpinned host must be left completely alone.
        return AppShield.getPinSet().isEnforcedFor(host);
    }

    @Override
    public void checkCertificates(ConnectionRequest request,
            ConnectionRequest.SSLCertificate[] certificates) throws IOException {
        String host = AppShield.hostOf(request.getUrl());
        if (host == null || !AppShield.policyFor(host).isEnforcePins()) {
            return;
        }
        String[] spki = new String[certificates == null ? 0 : certificates.length];
        String[] certs = new String[spki.length];
        for (int i = 0; i < spki.length; i++) {
            spki[i] = certificates[i].getPublicKeyDigest();
            certs[i] = certificates[i].getFingerprint();
        }
        boolean ok;
        try {
            // Local and non-blocking by contract: on iOS this runs on the TLS
            // delegate thread with the handshake held open.
            ok = ShieldEngineRegistry.getEngine().verifyPins(host, spki, certs);
        } catch (Throwable t) {
            // A crash in pin comparison must not fail closed by accident. A real
            // mismatch is reported as false, not thrown.
            Log.e(t);
            return;
        }
        if (!ok) {
            throw new ShieldException(ShieldStatus.PIN_MISMATCH,
                    "The certificate chain presented by " + host
                    + " matched none of its configured pins");
        }
    }

    @Override
    public String[] interestingResponseHeaders() {
        return new String[] {AppShield.REJECT_HEADER};
    }

    @Override
    public void afterResponse(ConnectionRequest request, int responseCode, String[] headers) {
        if (responseCode != 401 && responseCode != 403) {
            return;
        }
        String host = AppShield.hostOf(request.getUrl());
        if (host == null || !AppShield.policyFor(host).isAttachToken()) {
            return;
        }
        // Only when the backend says it was the *token* it rejected. A protected
        // API usually also carries ordinary user authorization, so an expired
        // login or a plain permission denial is a 401/403 that has nothing to do
        // with attestation -- re-attesting on every one of those would push a
        // client into rate limiting precisely while it is already failing to log
        // in.
        if (headers == null || headers.length == 0 || headers[0] == null) {
            return;
        }
        AppShield.invalidateToken();
    }
}
