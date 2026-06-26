/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.impl.ios.sim.child;

import com.codename1.impl.ios.sim.bridge.BridgeRegistry;
import com.codename1.security.AuthenticationOptions;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.BiometricType;
import com.codename1.security.Biometrics;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulator backing for {@link Biometrics} in the RPC simulator - the parity
 * equivalent of {@code JavaSEBiometrics}. State lives in {@link BridgeRegistry}
 * (mutated by the shell's Simulate &gt; Biometric Simulation menu); each
 * {@link #authenticate(AuthenticationOptions)} pops a CN1 dialog mimicking the
 * OS prompt so the developer sees the trigger fire and steps through outcomes.
 */
public final class SimBiometrics extends Biometrics {

    private volatile AsyncResource<Boolean> pending;

    @Override
    public boolean isSupported() {
        return BridgeRegistry.isBiometricAvailable();
    }

    @Override
    public boolean canAuthenticate() {
        return BridgeRegistry.isBiometricAvailable()
                && (BridgeRegistry.isBiometricFaceEnrolled()
                || BridgeRegistry.isBiometricTouchEnrolled()
                || BridgeRegistry.isBiometricIrisEnrolled());
    }

    @Override
    public List<BiometricType> getAvailableBiometrics() {
        List<BiometricType> out = new ArrayList<BiometricType>();
        if (!BridgeRegistry.isBiometricAvailable()) {
            return out;
        }
        if (BridgeRegistry.isBiometricTouchEnrolled()) {
            out.add(BiometricType.FINGERPRINT);
        }
        if (BridgeRegistry.isBiometricFaceEnrolled()) {
            out.add(BiometricType.FACE);
        }
        if (BridgeRegistry.isBiometricIrisEnrolled()) {
            out.add(BiometricType.IRIS);
        }
        return out;
    }

    @Override
    public AsyncResource<Boolean> authenticate(AuthenticationOptions opts) {
        final AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        if (!BridgeRegistry.isBiometricAvailable()) {
            result.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                    "Simulator: Biometric Simulation -> Available is unchecked"));
            return result;
        }
        if (!BridgeRegistry.isBiometricFaceEnrolled()
                && !BridgeRegistry.isBiometricTouchEnrolled()
                && !BridgeRegistry.isBiometricIrisEnrolled()) {
            result.error(new BiometricException(BiometricError.NOT_ENROLLED,
                    "Simulator: no biometric modality enrolled"));
            return result;
        }
        pending = result;
        final String reason = opts == null || opts.getReason() == null
                ? "Authenticate" : opts.getReason();
        final String title = opts == null || opts.getTitle() == null
                ? "Biometric Authentication" : opts.getTitle();
        CN.callSerially(new Runnable() {
            public void run() {
                // a modal CN1 prompt mimicking the OS biometric sheet
                boolean ok = Dialog.show(title,
                        reason + "\n\nSimulator: next outcome = " + outcome(),
                        "Authenticate", "Cancel");
                completePending(result, ok ? outcome() : "CANCEL");
            }
        });
        return result;
    }

    @Override
    public boolean stopAuthentication() {
        final AsyncResource<Boolean> r = pending;
        if (r == null) {
            return false;
        }
        completePending(r, "CANCEL");
        return true;
    }

    private static String outcome() {
        String o = BridgeRegistry.getBiometricOutcome();
        return o == null ? "SUCCEED" : o;
    }

    private void completePending(AsyncResource<Boolean> result, String outcome) {
        if (pending != result) {
            return;
        }
        pending = null;
        if (result.isDone()) {
            return;
        }
        if ("SUCCEED".equals(outcome)) {
            result.complete(Boolean.TRUE);
        } else if ("FAIL".equals(outcome)) {
            result.error(new BiometricException(BiometricError.AUTHENTICATION_FAILED,
                    "Simulator: simulated authentication failure"));
        } else if ("CANCEL".equals(outcome)) {
            result.error(new BiometricException(BiometricError.USER_CANCELED,
                    "Simulator: user cancelled"));
        } else if ("LOCKED_OUT".equals(outcome)) {
            result.error(new BiometricException(BiometricError.LOCKED_OUT,
                    "Simulator: locked out"));
        } else if ("PERMANENTLY_LOCKED_OUT".equals(outcome)) {
            result.error(new BiometricException(BiometricError.PERMANENTLY_LOCKED_OUT,
                    "Simulator: permanently locked out"));
        } else if ("NOT_ENROLLED".equals(outcome)) {
            result.error(new BiometricException(BiometricError.NOT_ENROLLED,
                    "Simulator: no biometric enrolled"));
        } else if ("PASSCODE_NOT_SET".equals(outcome)) {
            result.error(new BiometricException(BiometricError.PASSCODE_NOT_SET,
                    "Simulator: passcode not set"));
        } else {
            result.error(new BiometricException(BiometricError.UNKNOWN));
        }
    }
}
