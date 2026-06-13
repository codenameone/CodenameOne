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
package com.codename1.security;

import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the {@link SecureStorage} fallback base class returned on
 * platforms without biometric-gated keychain support (the test implementation
 * provides none). The biometric-gated async operations fail with
 * {@link BiometricError#NOT_AVAILABLE}; the quiet non-prompting overloads
 * return null / false; and {@code setKeychainAccessGroup} is a no-op.
 */
class SecureStorageTest extends UITestBase {

    private static Throwable errorOf(AsyncResource<?> r) {
        final AtomicReference<Throwable> err = new AtomicReference<Throwable>();
        r.except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                err.set(t);
            }
        });
        return err.get();
    }

    @Test
    void getInstanceReturnsStableFallbackSingleton() {
        SecureStorage a = SecureStorage.getInstance();
        SecureStorage b = SecureStorage.getInstance();
        assertNotNull(a);
        assertSame(a, b);
    }

    @Test
    void biometricGetFailsNotAvailable() {
        Throwable t = errorOf(SecureStorage.getInstance().get("reason", "account"));
        assertTrue(t instanceof BiometricException);
        assertEquals(BiometricError.NOT_AVAILABLE, ((BiometricException) t).getError());
    }

    @Test
    void biometricSetFailsNotAvailable() {
        Throwable t = errorOf(SecureStorage.getInstance().set("reason", "account", "value"));
        assertTrue(t instanceof BiometricException);
        assertEquals(BiometricError.NOT_AVAILABLE, ((BiometricException) t).getError());
    }

    @Test
    void biometricRemoveFailsNotAvailable() {
        Throwable t = errorOf(SecureStorage.getInstance().remove("reason", "account"));
        assertTrue(t instanceof BiometricException);
        assertEquals(BiometricError.NOT_AVAILABLE, ((BiometricException) t).getError());
    }

    @Test
    void quietOverloadsReturnNullAndFalse() {
        SecureStorage s = SecureStorage.getInstance();
        assertFalse(s.set("account", "value"));
        assertNull(s.get("account"));
        assertFalse(s.remove("account"));
    }

    @Test
    void setKeychainAccessGroupIsANoOp() {
        // Must not throw on the fallback base class.
        SecureStorage.getInstance().setKeychainAccessGroup("ABCDE12345.group.com.example.app");
        SecureStorage.getInstance().setKeychainAccessGroup(null);
    }
}
