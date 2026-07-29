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

import com.codename1.security.SecureStorage;
import com.codename1.security.shield.ShieldSignal;
import com.codename1.util.AsyncResource;

/// The narrow set of framework services lent to a [ShieldEngine].
///
/// Handed to the engine rather than reached for, so the engine never needs access to the platform
/// implementation object. That keeps the framework's implementation accessor package-private, and
/// it keeps the list of things an engine can do small enough to review.
public interface EngineContext {

    /// Non-prompting secure storage, for the attestation key identifier and cached tokens. On
    /// device this is the platform keychain or keystore.
    SecureStorage getSecureStorage();

    /// Requests a raw platform attestation (Play Integrity or App Attest) bound to the nonce.
    ///
    /// The result is opaque and must be forwarded to the verifying service; the engine must not
    /// try to interpret it on the device, because a device the attacker controls can be made to
    /// produce any interpretation.
    AsyncResource<String> requestPlatformAttestation(String nonce);

    /// True when the platform provides attestation and this build bundled it.
    boolean isPlatformAttestationSupported();

    /// Clears cached platform attestation state, forcing a fresh hardware key on the next request.
    /// Used when the service reports that the device's attestation key is unknown to it.
    void resetPlatformAttestation();

    /// Acknowledges that the verifying service recorded the attested key, releasing the client to use
    /// cheap assertions from here on. Call it once the service has accepted an attestation token; until
    /// then the platform refuses to assert against a key the service cannot yet resolve.
    void confirmPlatformAttestation(String keyId);

    /// Platform-detected compromise reasons, such as `root` or `frida`.
    String[] getPlatformCompromiseReasons();

    /// Component identifiers of the accessibility services currently enabled.
    String[] getEnabledAccessibilityServices();

    /// Digests of the certificates the running app is actually signed with, for comparison against
    /// what it was built with. Empty where the platform cannot report it.
    ///
    /// Not exposed as public framework API: nothing in an app needs this, and publishing it would
    /// only tell an attacker exactly which value to fake.
    String[] getAppSignerDigests();

    /// A build-stamped property, such as the build key or the per-build hardening manifest.
    String getProperty(String key, String defaultValue);

    /// Writes to the framework log.
    void log(String message);

    /// Publishes an observation to [com.codename1.security.shield.ShieldSignals], where the app
    /// can see it and from where it is offered to the service on the next token fetch.
    void publishSignal(ShieldSignal signal);
}
