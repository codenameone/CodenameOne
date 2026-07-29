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

import com.codename1.io.Log;
import com.codename1.security.DeviceIntegrity;
import com.codename1.security.SecureStorage;
import com.codename1.security.shield.ShieldSignal;
import com.codename1.security.shield.ShieldSignals;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

/// The framework-backed [EngineContext] handed to an engine at initialization.
///
/// Every method is defensive: an engine runs early in startup, often on a device in an unusual
/// state, and a platform probe that throws must degrade to an empty answer rather than take the
/// app down before it has drawn a frame.
final class DefaultEngineContext implements EngineContext {

    static final DefaultEngineContext INSTANCE = new DefaultEngineContext();

    private DefaultEngineContext() {
    }

    @Override
    public SecureStorage getSecureStorage() {
        return SecureStorage.getInstance();
    }

    @Override
    public AsyncResource<String> requestPlatformAttestation(String nonce) {
        return DeviceIntegrity.requestIntegrityToken(nonce);
    }

    @Override
    public boolean isPlatformAttestationSupported() {
        try {
            return DeviceIntegrity.isAttestationSupported();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void resetPlatformAttestation() {
        try {
            DeviceIntegrity.resetAttestation();
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    @Override
    public void confirmPlatformAttestation() {
        try {
            DeviceIntegrity.confirmAttestation();
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    @Override
    public String[] getPlatformCompromiseReasons() {
        try {
            String[] r = DeviceIntegrity.getCompromiseReasons();
            return r == null ? new String[0] : r;
        } catch (Throwable t) {
            return new String[0];
        }
    }

    @Override
    public String[] getEnabledAccessibilityServices() {
        try {
            String[] r = DeviceIntegrity.getEnabledAccessibilityServices();
            return r == null ? new String[0] : r;
        } catch (Throwable t) {
            return new String[0];
        }
    }

    @Override
    public String[] getAppSignerDigests() {
        try {
            String[] r = Display.getInstance().getAppSignerDigests();
            return r == null ? new String[0] : r;
        } catch (Throwable t) {
            return new String[0];
        }
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        try {
            return Display.getInstance().getProperty(key, defaultValue);
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    @Override
    public void log(String message) {
        Log.p(message);
    }

    @Override
    public void publishSignal(ShieldSignal signal) {
        ShieldSignals.add(signal);
    }
}
