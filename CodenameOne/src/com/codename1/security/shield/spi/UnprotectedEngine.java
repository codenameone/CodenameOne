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

import com.codename1.security.DeviceIntegrity;
import com.codename1.security.shield.PinSet;
import com.codename1.security.shield.ShieldConfig;
import com.codename1.security.shield.ShieldException;
import com.codename1.security.shield.ShieldSignal;
import com.codename1.security.shield.ShieldStatus;
import com.codename1.security.shield.ShieldToken;
import java.util.Vector;

/// The engine used when no attestation engine was registered -- an open-source build, a build not
/// entitled to the enterprise engine, or a unit test.
///
/// The contract it implements is the degradation promise the public API makes: an app written
/// against the shield must run everywhere, and must never fail closed just because attestation is
/// unavailable.
///
/// - `fetchToken` **completes**, with a [ShieldStatus#UNPROTECTED] failure. It never hangs and
///   never throws synchronously, so callers written for the real engine follow their normal error
///   path instead of deadlocking.
/// - `verifyPins` returns true. There is no pin set to enforce, and reporting "no opinion" as a
///   mismatch would break every request.
/// - Nothing here can block a request.
///
/// It is not entirely inert: [#collectSignals()] still reports what the free platform checks
/// found, so an app can react to a rooted device without an enterprise entitlement.
final class UnprotectedEngine implements ShieldEngine {

    static final UnprotectedEngine INSTANCE = new UnprotectedEngine();

    private UnprotectedEngine() {
    }

    public String getName() {
        return "unprotected";
    }

    public boolean isAvailable() {
        return false;
    }

    public void initialize(EngineContext ctx, ShieldConfig config) {
        if (ctx != null) {
            ctx.log("AppShield: no attestation engine registered; running unprotected. "
                    + "Tokens are not issued and certificate pins are not enforced.");
        }
    }

    public ShieldToken fetchToken(String bindingData) throws ShieldException {
        throw new ShieldException(ShieldStatus.UNPROTECTED,
                "This build has no attestation engine, so no token can be issued.");
    }

    public ShieldToken getCachedToken() {
        return null;
    }

    public boolean verifyPins(String host, String[] spkiDigests, String[] certDigests) {
        return true;
    }

    public PinSet getPinSet() {
        return PinSet.EMPTY;
    }

    public ShieldSignal[] collectSignals() {
        String[] reasons;
        try {
            reasons = DeviceIntegrity.getCompromiseReasons();
        } catch (Throwable t) {
            // Never let a platform probe break the caller; an absent signal is
            // strictly better than a crashed app.
            return new ShieldSignal[0];
        }
        if (reasons == null || reasons.length == 0) {
            return new ShieldSignal[0];
        }
        Vector out = new Vector();
        for (int i = 0; i < reasons.length; i++) {
            ShieldSignal s = toSignal(reasons[i]);
            if (s != null) {
                out.addElement(s);
            }
        }
        ShieldSignal[] arr = new ShieldSignal[out.size()];
        out.copyInto(arr);
        return arr;
    }

    private static ShieldSignal toSignal(String reason) {
        if (reason == null) {
            return null;
        }
        if ("root".equals(reason)) {
            return new ShieldSignal(ShieldSignal.ROOT, 70, null);
        }
        if ("jailbreak".equals(reason)) {
            return new ShieldSignal(ShieldSignal.JAILBREAK, 70, null);
        }
        if ("frida".equals(reason)) {
            return new ShieldSignal(ShieldSignal.HOOK, 90, "frida");
        }
        if ("emulator".equals(reason)) {
            // Low severity on purpose: every developer's device is an emulator.
            // The service weighs it against the other signals.
            return new ShieldSignal(ShieldSignal.EMULATOR, 30, null);
        }
        return new ShieldSignal(reason, 50, null);
    }

    public void invalidate() {
    }

    public void shutdown() {
    }
}
