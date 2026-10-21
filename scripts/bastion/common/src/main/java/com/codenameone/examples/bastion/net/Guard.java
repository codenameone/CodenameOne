/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.net;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkGuard;
import com.codename1.io.NetworkManager;
import com.codename1.io.Log;
import com.codename1.security.shield.AppShield;
import com.codename1.security.shield.HostPolicy;
import com.codename1.security.shield.PinSet;
import com.codename1.security.shield.ShieldConfig;
import com.codenameone.examples.bastion.AppConfig;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/// The one gate every request of the app goes through, installed before the
/// first request is made.
///
/// - **No cleartext.** A plain `http` request is refused before a connection
///   is opened, unless it is to the development server on this machine
///   ([AppConfig#isDevelopmentServer]).
/// - **A host allow-list.** A request to a host not in
///   [AppConfig#ALLOWED_HOSTS] is refused, so nothing -- a redirect, a link in
///   a message, a server typed in the settings -- can take the bearer token or
///   a signed request somewhere else.
/// - **Certificate pinning.** A host in [AppConfig#PINS] must present a chain
///   with one of its pinned keys, or the request fails before its body is
///   written.
/// - **AppShield.** The attestation token and the pins the attestation service
///   publishes over the air, by delegating to the shield's own guard. Without
///   the enterprise engine (any local build) the shield is inert and says so.
///
/// `NetworkManager` takes one guard and seals the slot, which is why this is
/// installed before `AppShield.init`: the shield then finds the slot taken,
/// and this guard calls the shield's from its own methods instead.
public final class Guard implements NetworkGuard {
    private static Guard installed;
    private final PinSet pins = new PinSet(AppConfig.pinTable(), 1, 0, 0);
    // Counted on the network thread, read on the EDT.
    private final AtomicInteger refused = new AtomicInteger();
    private final AtomicInteger pinFailures = new AtomicInteger();

    private Guard() {
    }

    /// Installs the guard and starts the shield. Called once, before the first
    /// request.
    public static void install() {
        if (installed != null) {
            return;
        }
        installed = new Guard();
        try {
            NetworkManager.setNetworkGuard(installed);
        } catch (IllegalStateException alreadyInstalled) {
            Log.p("Bastion: a network guard was already installed; requests are not guarded");
        }
        ShieldConfig shield = new ShieldConfig()
                // Every API host carries the attestation token and fails closed
                // when there is a pin set and it does not match.
                .collectSignals(true);
        for (String host : AppConfig.ALLOWED_HOSTS) {
            shield.protect(host, HostPolicy.ENFORCED);
        }
        AppShield.init(shield);
    }

    public static Guard get() {
        return installed;
    }

    /// Why `url` may not be requested, or null when it may.
    public static String refusal(String url) {
        if (AppConfig.isDevelopmentServer(url)) {
            return null;
        }
        String host = AppConfig.hostOf(url);
        if (host == null) {
            return "not an http(s) address";
        }
        if (!AppConfig.isHttps(url)) {
            return "cleartext http to " + host;
        }
        if (!AppConfig.isAllowedHost(host)) {
            return host + " is not on the allow-list";
        }
        return null;
    }

    /// The pins this build carries.
    public PinSet pins() {
        return pins;
    }

    /// How many requests this guard refused since start: shown in the
    /// Security Center.
    public int refusedCount() {
        return refused.get();
    }

    public int pinFailureCount() {
        return pinFailures.get();
    }

    private void count(boolean pin) {
        (pin ? pinFailures : refused).incrementAndGet();
    }

    @Override
    public void beforeRequest(ConnectionRequest request) throws IOException {
        String why = refusal(request.getUrl());
        if (why != null) {
            count(false);
            throw new IOException("Refused by the network policy: " + why);
        }
        NetworkGuard shield = AppShield.getNetworkGuard();
        if (shield != null) {
            shield.beforeRequest(request);
        }
    }

    @Override
    public boolean isCertificateCheckRequired(String url) {
        String host = AppConfig.hostOf(url);
        if (host != null && pins.isEnforcedFor(host)) {
            return true;
        }
        NetworkGuard shield = AppShield.getNetworkGuard();
        return shield != null && shield.isCertificateCheckRequired(url);
    }

    @Override
    public void checkCertificates(ConnectionRequest request,
            ConnectionRequest.SSLCertificate[] certificates) throws IOException {
        String host = AppConfig.hostOf(request.getUrl());
        // An empty chain is "not seen" (the iOS TLS cache), not a mismatch:
        // the same never-brick rule the shield follows.
        if (host != null && pins.isEnforcedFor(host) && certificates != null
                && certificates.length > 0) {
            String[] spki = new String[certificates.length];
            for (int iter = 0; iter < spki.length; iter++) {
                spki[iter] = certificates[iter].getPublicKeyDigest();
            }
            if (!pins.matches(host, spki)) {
                count(true);
                throw new IOException("The certificate of " + host
                        + " matched none of its pins");
            }
        }
        NetworkGuard shield = AppShield.getNetworkGuard();
        if (shield != null) {
            shield.checkCertificates(request, certificates);
        }
    }

    @Override
    public String[] interestingResponseHeaders() {
        NetworkGuard shield = AppShield.getNetworkGuard();
        return shield == null ? new String[0] : shield.interestingResponseHeaders();
    }

    @Override
    public void afterResponse(ConnectionRequest request, int responseCode, String[] headers) {
        NetworkGuard shield = AppShield.getNetworkGuard();
        if (shield != null) {
            shield.afterResponse(request, responseCode, headers);
        }
    }
}
