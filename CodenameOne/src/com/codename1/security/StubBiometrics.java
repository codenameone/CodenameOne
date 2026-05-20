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

import com.codename1.util.AsyncResource;

import java.util.Collections;
import java.util.List;

/// No-op Biometrics returned by `CodenameOneImplementation` when a port has
/// not overridden `getBiometrics()`. Reports the device as unsupported and
/// fails every authentication with [BiometricError#NOT_AVAILABLE].
final class StubBiometrics extends Biometrics {

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public boolean canAuthenticate() {
        return false;
    }

    @Override
    public List<BiometricType> getAvailableBiometrics() {
        return Collections.emptyList();
    }

    @Override
    public AsyncResource<Boolean> authenticate(AuthenticationOptions opts) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                "Biometric authentication is not available on this platform"));
        return r;
    }

    @Override
    public boolean stopAuthentication() {
        return false;
    }
}
