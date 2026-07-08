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

import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.SecureStorage;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.HashMap;
import java.util.Map;

/**
 * iOS backing for {@link SecureStorage} backed by the system Keychain. Reads
 * present a Touch ID / Face ID prompt courtesy of
 * {@code kSecUseOperationPrompt}; writes are performed via
 * {@code SecItemAdd} without a separate prompt (Apple does not enforce one
 * because the user could simply enrol a new fingerprint in Settings to
 * bypass it). Set the {@code ios.Fingerprint.addPassword.prompt} display
 * property to enable a write-side prompt for parity with the legacy cn1lib.
 */
public final class IOSSecureStorage extends SecureStorage {

    private static final Map<Integer, AsyncResource<?>> REQUESTS =
            new HashMap<Integer, AsyncResource<?>>();
    private static int nextRequestId = 1;

    static {
        // Prevents the iOS VM optimizer from eliding these callbacks.
        nativeStorageStringResult(-1, null);
        nativeStorageBooleanResult(-1, false);
        nativeStorageError(-1, 0, null);
    }

    private final IOSNative nativeInstance;
    private String accessGroup;

    IOSSecureStorage(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
    }

    @Override
    public boolean set(String account, String value) {
        if (account == null) {
            return false;
        }
        try {
            return nativeInstance.secureStorageSetPlain(account, value == null ? "" : value);
        } catch (Throwable err) {
            return false;
        }
    }

    @Override
    public String get(String account) {
        if (account == null) {
            return null;
        }
        try {
            return nativeInstance.secureStorageGetPlain(account);
        } catch (Throwable err) {
            return null;
        }
    }

    @Override
    public boolean remove(String account) {
        if (account == null) {
            return false;
        }
        try {
            return nativeInstance.secureStorageRemovePlain(account);
        } catch (Throwable err) {
            return false;
        }
    }

    @Override
    public void setKeychainAccessGroup(String group) {
        this.accessGroup = (group != null && group.length() == 0) ? null : group;
        nativeInstance.setSecureStorageAccessGroup(this.accessGroup);
    }

    @Override
    public AsyncResource<String> get(String reason, String account) {
        AsyncResource<String> r = new AsyncResource<String>();
        int rid;
        synchronized (REQUESTS) {
            rid = nextRequestId++;
            REQUESTS.put(Integer.valueOf(rid), r);
        }
        nativeInstance.secureStorageGet(rid, reason == null ? "Authenticate" : reason, account);
        return r;
    }

    @Override
    public AsyncResource<Boolean> set(String reason, String account, String value) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        int rid;
        synchronized (REQUESTS) {
            rid = nextRequestId++;
            REQUESTS.put(Integer.valueOf(rid), r);
        }
        nativeInstance.secureStorageSet(rid, reason == null ? "Authenticate" : reason, account, value);
        return r;
    }

    @Override
    public AsyncResource<Boolean> remove(String reason, String account) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        int rid;
        synchronized (REQUESTS) {
            rid = nextRequestId++;
            REQUESTS.put(Integer.valueOf(rid), r);
        }
        nativeInstance.secureStorageRemove(rid, reason == null ? "Authenticate" : reason, account);
        return r;
    }

    // ---- Native callbacks (do not rename) ----------------------------------

    /** Called from native on a successful {@link #get(String, String)}. */
    public static void nativeStorageStringResult(final int requestId, final String value) {
        final AsyncResource<?> rr = take(requestId);
        if (rr == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        final AsyncResource<String> r = (AsyncResource<String>) rr;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!r.isDone()) {
                    r.complete(value);
                }
            }
        });
    }

    /** Called from native on a successful {@link #set(String, String, String)} / {@link #remove(String, String)}. */
    public static void nativeStorageBooleanResult(final int requestId, final boolean ok) {
        final AsyncResource<?> rr = take(requestId);
        if (rr == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        final AsyncResource<Boolean> r = (AsyncResource<Boolean>) rr;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!r.isDone()) {
                    r.complete(Boolean.valueOf(ok));
                }
            }
        });
    }

    /** Called from native on any failure. {@code errorCode} is an LAError or OSStatus value. */
    public static void nativeStorageError(final int requestId, final int errorCode, final String msg) {
        final AsyncResource<?> r = take(requestId);
        if (r == null) {
            return;
        }
        final BiometricError mapped = mapStorageError(errorCode);
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!r.isDone()) {
                    r.error(new BiometricException(mapped, msg == null ? mapped.name() : msg));
                }
            }
        });
    }

    private static AsyncResource<?> take(int requestId) {
        synchronized (REQUESTS) {
            return REQUESTS.remove(Integer.valueOf(requestId));
        }
    }

    private static BiometricError mapStorageError(int code) {
        // Reuse the LAError mapping (negative values) and the dominant
        // OSStatus values returned by Security.framework.
        switch (code) {
            case -128:  // errSecUserCanceled
                return BiometricError.USER_CANCELED;
            case -25291: // errSecNotAvailable
                return BiometricError.NOT_AVAILABLE;
            case -25300: // errSecItemNotFound
                return BiometricError.UNKNOWN;
            case -25308: // errSecInteractionNotAllowed
                return BiometricError.LOCKED_OUT;
            case -34018: // errSecMissingEntitlement
                return BiometricError.UNKNOWN;
            case -2:    // LAErrorUserCancel
                return BiometricError.USER_CANCELED;
            case -7:    // LAErrorBiometryNotEnrolled
                return BiometricError.NOT_ENROLLED;
            case -8:    // LAErrorBiometryLockout
                return BiometricError.LOCKED_OUT;
            default:
                return BiometricError.UNKNOWN;
        }
    }
}
