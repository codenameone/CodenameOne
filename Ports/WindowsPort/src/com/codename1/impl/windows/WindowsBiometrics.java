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
package com.codename1.impl.windows;

import com.codename1.security.AuthenticationOptions;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.BiometricType;
import com.codename1.security.Biometrics;
import com.codename1.util.AsyncResource;
import java.util.ArrayList;
import java.util.List;

/**
 * Windows Hello biometric authentication, backed by the WinRT
 * {@code UserConsentVerifier} (face / fingerprint / PIN). Availability maps to
 * {@link #isSupported()} / {@link #canAuthenticate()}; {@link #authenticate} runs
 * the system Hello prompt off the EDT and completes the {@link AsyncResource} with
 * the verification result. When the port is built without WinRT the native layer
 * reports {@code DeviceNotPresent}, so this reports unsupported honestly.
 */
class WindowsBiometrics extends Biometrics {
    // UserConsentVerifierAvailability values.
    private static final int AVAILABLE = 0;
    private static final int DEVICE_NOT_PRESENT = 1;

    @Override
    public boolean isSupported() {
        // Any value other than DeviceNotPresent means the hardware exists.
        return WindowsNative.biometricAvailability() != DEVICE_NOT_PRESENT;
    }

    @Override
    public boolean canAuthenticate() {
        return WindowsNative.biometricAvailability() == AVAILABLE;
    }

    @Override
    public List<BiometricType> getAvailableBiometrics() {
        List<BiometricType> out = new ArrayList<BiometricType>();
        if (isSupported()) {
            // UserConsentVerifier does not expose the underlying modality (it may
            // be face, fingerprint or PIN), so report it as a STRONG authenticator.
            out.add(BiometricType.STRONG);
        }
        return out;
    }

    @Override
    public AsyncResource<Boolean> authenticate(final AuthenticationOptions opts) {
        final AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        if (!isSupported()) {
            result.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                    "Windows Hello is not available on this device"));
            return result;
        }
        final String reason = opts != null && opts.getReason() != null ? opts.getReason()
                : (opts != null && opts.getTitle() != null ? opts.getTitle() : "Authenticate");
        // The Hello prompt is modal and blocking; run it off the EDT.
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean verified = WindowsNative.biometricAuthenticate(reason);
                if (verified) {
                    result.complete(Boolean.TRUE);
                } else {
                    result.error(new BiometricException(BiometricError.AUTHENTICATION_FAILED,
                            "Windows Hello verification was not successful"));
                }
            }
        }, "cn1-windows-hello");
        t.setDaemon(true);
        t.start();
        return result;
    }
}
