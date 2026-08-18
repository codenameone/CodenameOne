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
import com.codename1.ui.CN;
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
    /**
     * A named subclass so the test can reach the protected helper the desktop ports call. Naming
     * it here rather than testing through a port keeps the rule where both ports share it.
     */
    private static final class NamespaceProbe extends SecureStorage {
        static String namespace() {
            return applicationNamespace();
        }
    }

    @Test
    void theApplicationNamespaceComesFromThePackage() {
        // What separates two native desktop applications. Their Storage is one directory under the
        // user account, so without this both reach the same entry: one reads the other's managed
        // database key, and forgetting it in either removes the other's only copy.
        CN.setProperty("package_name", "com.example.notes");
        CN.setProperty("AppName", "Notes");
        assertEquals("com.example.notes", NamespaceProbe.namespace(),
                "the package is the identity the installer and the store agree on");

        // A build with no package still has to be separated from other applications, so the
        // display name stands in.
        CN.setProperty("package_name", null);
        assertEquals("Notes", NamespaceProbe.namespace(), "the display name is the fallback");

        // Whatever it is has to survive being part of a storage name.
        CN.setProperty("package_name", "com.example/notes v2");
        assertEquals("com.example_notes_v2", NamespaceProbe.namespace(),
                "anything a storage name cannot carry is replaced rather than dropped");

        // Neither stamped. A constant, rather than a name that looks unique and is not.
        CN.setProperty("package_name", null);
        CN.setProperty("AppName", null);
        assertEquals("cn1app", NamespaceProbe.namespace(),
                "a build that stamped neither still gets a stable answer");
    }

}
