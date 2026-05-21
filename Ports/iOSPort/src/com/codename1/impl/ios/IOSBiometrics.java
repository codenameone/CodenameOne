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

import com.codename1.security.AuthenticationOptions;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.BiometricType;
import com.codename1.security.Biometrics;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * iOS backing for {@link Biometrics}, wrapping {@code LAContext} from
 * {@code LocalAuthentication.framework}.
 *
 * <p>The native side dispatches results back via the static
 * {@link #nativeAuthSuccess(int)} / {@link #nativeAuthError(int, int, String)}
 * methods on this class. To stop the ParparVM dead-code eliminator from
 * stripping these (no Java caller exists), the static initializer invokes
 * each with no-op values --- the same idiom used by the original
 * FingerprintScanner cn1lib.</p>
 */
public final class IOSBiometrics extends Biometrics {

    static {
        // Prevents the iOS VM optimizer from eliding these callbacks.
        nativeAuthSuccess(-1);
        nativeAuthError(-1, 0, null);
    }

    // Map request id -> pending AsyncResource. Static because the native
    // callback path doesn't carry an instance reference.
    private static final Map<Integer, AsyncResource<Boolean>> REQUESTS =
            new HashMap<Integer, AsyncResource<Boolean>>();
    private static int nextRequestId = 1;

    private IOSNative nativeInstance;

    IOSBiometrics(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
    }

    @Override
    public boolean isSupported() {
        return nativeInstance.isBiometricsSupported();
    }

    @Override
    public boolean canAuthenticate() {
        return nativeInstance.canAuthenticateBiometric();
    }

    @Override
    public List<BiometricType> getAvailableBiometrics() {
        int mask = nativeInstance.getAvailableBiometricTypes();
        List<BiometricType> out = new ArrayList<BiometricType>();
        if ((mask & 1) != 0) {
            out.add(BiometricType.FINGERPRINT);
        }
        if ((mask & 2) != 0) {
            out.add(BiometricType.FACE);
        }
        return out;
    }

    @Override
    public AsyncResource<Boolean> authenticate(AuthenticationOptions opts) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        if (!nativeInstance.isBiometricsSupported()) {
            r.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                    "Biometrics not available on this device"));
            return r;
        }
        String reason = opts == null || opts.getReason() == null
                ? "Authenticate" : opts.getReason();
        int rid;
        synchronized (REQUESTS) {
            rid = nextRequestId++;
            REQUESTS.put(Integer.valueOf(rid), r);
        }
        nativeInstance.authenticateBiometric(rid, reason);
        return r;
    }

    @Override
    public boolean stopAuthentication() {
        synchronized (REQUESTS) {
            if (REQUESTS.isEmpty()) {
                return false;
            }
        }
        nativeInstance.stopBiometricAuthentication();
        return true;
    }

    // ---- Callbacks invoked from native code (do not rename) ----------------

    /** Called from native when the LAContext.evaluatePolicy succeeds. */
    public static void nativeAuthSuccess(final int requestId) {
        final AsyncResource<Boolean> r = take(requestId);
        if (r == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!r.isDone()) {
                    r.complete(Boolean.TRUE);
                }
            }
        });
    }

    /** Called from native when evaluatePolicy fails or is cancelled. */
    public static void nativeAuthError(final int requestId, final int errorCode, final String msg) {
        final AsyncResource<Boolean> r = take(requestId);
        if (r == null) {
            return;
        }
        final BiometricError mapped = mapLAError(errorCode);
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!r.isDone()) {
                    r.error(new BiometricException(mapped, msg == null ? mapped.name() : msg));
                }
            }
        });
    }

    private static AsyncResource<Boolean> take(int requestId) {
        synchronized (REQUESTS) {
            return REQUESTS.remove(Integer.valueOf(requestId));
        }
    }

    /**
     * Maps numeric {@code LAError} codes (passed across the JNI boundary) to
     * our typed enum. The native side uses the canonical
     * {@code LAErrorCode} values verbatim.
     */
    private static BiometricError mapLAError(int code) {
        switch (code) {
            case -1:  // LAErrorAuthenticationFailed
                return BiometricError.AUTHENTICATION_FAILED;
            case -2:  // LAErrorUserCancel
                return BiometricError.USER_CANCELED;
            case -3:  // LAErrorUserFallback (user chose PIN/passcode)
                return BiometricError.USER_CANCELED;
            case -4:  // LAErrorSystemCancel
                return BiometricError.SYSTEM_CANCELED;
            case -5:  // LAErrorPasscodeNotSet
                return BiometricError.PASSCODE_NOT_SET;
            case -6:  // LAErrorTouchIDNotAvailable / LAErrorBiometryNotAvailable
                return BiometricError.NOT_AVAILABLE;
            case -7:  // LAErrorTouchIDNotEnrolled / LAErrorBiometryNotEnrolled
                return BiometricError.NOT_ENROLLED;
            case -8:  // LAErrorTouchIDLockout / LAErrorBiometryLockout
                return BiometricError.LOCKED_OUT;
            case -9:  // LAErrorAppCancel
                return BiometricError.SYSTEM_CANCELED;
            case -10: // LAErrorInvalidContext
                return BiometricError.UNKNOWN;
            default:
                return BiometricError.UNKNOWN;
        }
    }
}
