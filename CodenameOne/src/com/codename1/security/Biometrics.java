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
package com.codename1.security;

import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.List;

/**
 * Entry point for biometric authentication (Touch ID, Face ID, fingerprint,
 * Android BiometricPrompt). Obtain the platform implementation via
 * {@link #getInstance()}; the returned subclass is owned by the active port.
 *
 * <p>A typical unlock flow:</p>
 *
 * <pre>{@code
 * Biometrics b = Biometrics.getInstance();
 * if (!b.canAuthenticate()) {
 *     // Fall back to password
 *     return;
 * }
 * b.authenticate("Unlock your account").onResult((success, err) -> {
 *     if (err != null) {
 *         BiometricError code = ((BiometricException) err).getError();
 *         // branch on code
 *     } else {
 *         // success
 *     }
 * });
 * }</pre>
 *
 * <p>{@link #authenticate(AuthenticationOptions)} returns an
 * {@link AsyncResource} whose failure path completes with a
 * {@link BiometricException} so callers can branch on the typed
 * {@link BiometricError}.</p>
 *
 * <p>This class is the parallel of Flutter's {@code local_auth} surface.
 * On platforms without biometric support (desktop simulator with the
 * "Available" simulator menu item unchecked, or older Android devices),
 * {@link #canAuthenticate()} returns {@code false} and
 * {@link #authenticate(AuthenticationOptions)} completes with
 * {@link BiometricError#NOT_AVAILABLE}.</p>
 */
public abstract class Biometrics {

    private static Biometrics fallback;

    /** Subclasses are constructed by the port; not for application use. */
    protected Biometrics() {
    }

    /**
     * Returns the platform-specific singleton owned by the current port.
     * Ports that do not implement biometrics get a no-op fallback that
     * reports {@link BiometricError#NOT_AVAILABLE}.
     */
    public static Biometrics getInstance() {
        Biometrics b = Display.getInstance().getBiometrics();
        if (b != null) {
            return b;
        }
        if (fallback == null) {
            fallback = new StubBiometrics();
        }
        return fallback;
    }

    /**
     * Returns {@code true} when biometric hardware exists on the device,
     * regardless of whether the user has enrolled biometrics. Combine with
     * {@link #canAuthenticate()} to gate UI affordances: show the "Use
     * biometrics" toggle when {@code isSupported()} is true, but only invoke
     * {@link #authenticate(AuthenticationOptions)} when {@code canAuthenticate()}
     * is also true.
     */
    public abstract boolean isSupported();

    /**
     * Returns {@code true} when the device is ready to authenticate right now:
     * hardware present, at least one biometric enrolled, and not in a
     * locked-out state.
     */
    public abstract boolean canAuthenticate();

    /**
     * Lists the biometric modalities currently enrolled. On iOS this is
     * {@link BiometricType#FINGERPRINT} or {@link BiometricType#FACE}; on
     * Android the list may contain {@link BiometricType#IRIS} as well, and
     * Android API 30+ adds {@link BiometricType#STRONG} / {@link BiometricType#WEAK}
     * authenticator class tags.
     *
     * @return an empty list when nothing is enrolled or the device is unsupported
     */
    public abstract List<BiometricType> getAvailableBiometrics();

    /**
     * Prompts the user to authenticate. The returned {@link AsyncResource}
     * completes with {@code true} on success, or with a
     * {@link BiometricException} on failure (consult
     * {@link BiometricException#getError()} for the typed code).
     *
     * @param opts non-null configuration; {@link AuthenticationOptions#setReason(String)}
     *             should be set
     */
    public abstract AsyncResource<Boolean> authenticate(AuthenticationOptions opts);

    /**
     * Convenience for {@code authenticate(new AuthenticationOptions().setReason(reason))}.
     */
    public AsyncResource<Boolean> authenticate(String reason) {
        return authenticate(new AuthenticationOptions().setReason(reason));
    }

    /**
     * Cancels an in-flight {@link #authenticate(AuthenticationOptions)} call
     * if one is running. The pending {@link AsyncResource} completes with
     * {@link BiometricError#USER_CANCELED}.
     *
     * @return {@code true} when a call was cancelled; {@code false} when no
     *         authentication was pending
     */
    public abstract boolean stopAuthentication();
}
