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
package com.codename1.impl.ios;

import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.HashMap;
import java.util.Map;

/**
 * iOS backing for App Attest (DeviceCheck.framework), surfaced through
 * {@link com.codename1.security.DeviceIntegrity#requestIntegrityToken(String)}.
 *
 * <p>The native side dispatches results back via the static
 * {@link #nativeAttestSuccess(int, String)} / {@link #nativeAttestError(int, String)}
 * methods. As with {@link IOSBiometrics}, the static initializer invokes each
 * with no-op values so the ParparVM dead-code eliminator does not strip the
 * native callback targets (no Java caller exists).</p>
 */
final class IOSDeviceIntegrity {

    static {
        // Prevents the iOS VM optimizer from eliding these native callbacks.
        nativeAttestSuccess(-1, null);
        nativeAttestError(-1, null);
    }

    private static final Map<Integer, AsyncResource<String>> REQUESTS =
            new HashMap<Integer, AsyncResource<String>>();
    private static int nextRequestId = 1;

    private final IOSNative nativeInstance;

    IOSDeviceIntegrity(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
    }

    boolean isSupported() {
        return nativeInstance.isAppAttestSupported();
    }

    AsyncResource<String> requestToken(String nonce) {
        AsyncResource<String> r = new AsyncResource<String>();
        if (!nativeInstance.isAppAttestSupported()) {
            r.error(new UnsupportedOperationException(
                    "App Attest is not supported on this device"));
            return r;
        }
        int rid;
        synchronized (REQUESTS) {
            rid = nextRequestId++;
            REQUESTS.put(Integer.valueOf(rid), r);
        }
        nativeInstance.requestAppAttestToken(rid, nonce);
        return r;
    }

    // ---- Callbacks invoked from native code (do not rename) ----------------

    /** Called from native when attestation succeeds with the opaque token. */
    public static void nativeAttestSuccess(final int requestId, final String token) {
        final AsyncResource<String> r = take(requestId);
        if (r == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!r.isDone()) {
                    r.complete(token);
                }
            }
        });
    }

    /** Called from native when key generation or attestation fails. */
    public static void nativeAttestError(final int requestId, final String msg) {
        final AsyncResource<String> r = take(requestId);
        if (r == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!r.isDone()) {
                    r.error(new RuntimeException(msg == null ? "App Attest failed" : msg));
                }
            }
        });
    }

    private static AsyncResource<String> take(int requestId) {
        synchronized (REQUESTS) {
            return REQUESTS.remove(Integer.valueOf(requestId));
        }
    }
}
