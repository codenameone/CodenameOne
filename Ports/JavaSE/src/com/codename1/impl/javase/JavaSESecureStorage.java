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
package com.codename1.impl.javase;

import com.codename1.security.AuthenticationOptions;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.Biometrics;
import com.codename1.security.SecureStorage;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncResult;

/**
 * Simulator backing for {@link SecureStorage}. Reads gate behind the
 * {@link Biometrics} prompt (which the simulator menu controls); writes
 * persist to {@code java.util.prefs} so values survive a JVM restart.
 */
public final class JavaSESecureStorage extends SecureStorage {

    private static final String NODE = "com.codename1.simulator.secureStorage";
    private final java.util.prefs.Preferences prefs;
    private final JavaSEBiometrics biometrics;

    JavaSESecureStorage(JavaSEBiometrics biometrics) {
        this.biometrics = biometrics;
        this.prefs = java.util.prefs.Preferences.userRoot().node(NODE);
    }

    @Override
    public AsyncResource<String> get(final String reason, final String account) {
        final AsyncResource<String> result = new AsyncResource<String>();
        final String stored = prefs.get(account, null);
        if (stored == null) {
            result.error(new BiometricException(BiometricError.UNKNOWN,
                    "No secure storage entry for account: " + account));
            return result;
        }
        AsyncResource<Boolean> auth = biometrics.authenticate(
                new AuthenticationOptions().setReason(reason));
        auth.onResult(new AsyncResult<Boolean>() {
            @Override
            public void onReady(Boolean ok, Throwable err) {
                if (err != null) {
                    result.error(err);
                } else {
                    result.complete(stored);
                }
            }
        });
        return result;
    }

    @Override
    public AsyncResource<Boolean> set(final String reason, final String account, final String value) {
        final AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        if (!biometrics.canAuthenticate()) {
            result.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                    "Simulator: biometrics not enabled for secure storage write"));
            return result;
        }
        prefs.put(account, value);
        result.complete(Boolean.TRUE);
        return result;
    }

    @Override
    public AsyncResource<Boolean> remove(String reason, String account) {
        AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        prefs.remove(account);
        result.complete(Boolean.TRUE);
        return result;
    }

    @Override
    public void setKeychainAccessGroup(String group) {
        // No-op in the simulator.
    }
}
