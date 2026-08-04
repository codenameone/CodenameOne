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
package com.codename1.security.shield.spi;

import com.codename1.security.shield.PinSet;
import com.codename1.security.shield.ShieldConfig;
import com.codename1.security.shield.ShieldException;
import com.codename1.security.shield.ShieldSignal;
import com.codename1.security.shield.ShieldToken;

/// The service-provider seam between the public shield API and the attestation engine that
/// implements it.
///
/// Codename One ships an inert default that reports itself unavailable, so an app written against
/// [com.codename1.security.shield.AppShield] compiles and runs everywhere. A build entitled to the
/// enterprise engine has a real implementation registered by the build server before
/// `Display.init`, via [ShieldEngineRegistry#setEngine(ShieldEngine)].
///
/// #### What must never move into the framework
///
/// The split is only worth anything if the engine keeps the parts an attacker would want to reach.
/// An implementation must own, and must never delegate to open framework code:
///
/// - challenge and nonce generation -- predictable nonces make replay possible;
/// - any key material, and any code that touches it;
/// - the pin **comparison** and the decision to fail a request. The framework may hold a
///   [PinSet]; patching the framework's copy must not be enough to disable pinning;
/// - the detection heuristics themselves. Published heuristics are bypassed heuristics, so the
///   framework only ever sees finished [ShieldSignal] results -- and only the ones the engine
///   chooses to publish;
/// - interpretation of the raw platform attestation. Those responses go to the verifying service,
///   which the attacker does not control, rather than being judged on the device.
///
/// The security property this preserves is not "the app refuses to make the call" -- an attacker
/// who controls the device can always strip a header. It is that the customer's backend refuses to
/// *serve* a request without a valid, unexpired, service-signed token, which a substituted engine
/// cannot mint.
public interface ShieldEngine {

    /// A stable name for diagnostics, for example `unprotected`, `simulator` or the enterprise
    /// engine's own identifier.
    String getName();

    /// True when this engine can actually attest. False for the inert default, which is how
    /// [com.codename1.security.shield.AppShield#isProtected()] is answered.
    boolean isAvailable();

    /// Called once from [com.codename1.security.shield.AppShield#init(ShieldConfig)]. Must not
    /// block on the network; do warm-up work on a background thread.
    void initialize(EngineContext ctx, ShieldConfig config);

    /// Obtains a token, blocking until it has one or fails. Called on a network thread, never the
    /// EDT.
    ///
    /// @param bindingData request data to bind the token to, or null for a plain time-limited
    ///        token. A bound token is only valid for the request whose data was supplied.
    /// @throws ShieldException carrying the [com.codename1.security.shield.ShieldStatus] that
    ///         explains whether the failure was about reaching the service or about this device
    ShieldToken fetchToken(String bindingData) throws ShieldException;

    /// The cached token, without contacting the service. Returns null when nothing is cached.
    ///
    /// Must never block: callers are typically on the EDT, deciding whether they can decorate a
    /// request right now.
    ShieldToken getCachedToken();

    /// Decides whether a certificate chain is acceptable for a host.
    ///
    /// Must be purely local and non-blocking: on iOS this is invoked synchronously from the TLS
    /// delegate thread while the handshake is held open, so any network call or blocking wait here
    /// deadlocks the connection.
    ///
    /// Returns true when the host is not pinned -- "no opinion" must never read as a mismatch.
    ///
    /// @param spkiDigests base64 SHA-256 digests of each chain certificate's public key info
    /// @param certDigests whole-certificate digests, for engines that pin those instead
    boolean verifyPins(String host, String[] spkiDigests, String[] certDigests);

    /// The pin set currently in force, never null. May be [PinSet#EMPTY].
    PinSet getPinSet();

    /// The runtime self-protection observations this engine wants reported. May legitimately be a
    /// subset of what it detected.
    ShieldSignal[] collectSignals();

    /// Discards any cached token, forcing the next fetch to go to the service. Called when a
    /// backend rejects a token, which usually means the device's attestation state is stale.
    void invalidate();

    /// Releases resources. Called when the app is shutting down.
    void shutdown();
}
