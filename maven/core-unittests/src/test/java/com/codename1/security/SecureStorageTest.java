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

        // Whatever it is has to survive being part of a storage name, and two identities that
        // differ have to stay different: folding every unsupported character onto one replacement
        // made "com.acme.foo$bar" and "com.acme.foo_bar" the same namespace, which is two
        // applications sharing the store this is here to keep apart.
        CN.setProperty("package_name", "com.example/notes v2");
        assertEquals("com.example%002fnotes%0020v2", NamespaceProbe.namespace(),
                "anything a storage name cannot carry is escaped rather than folded away");

        CN.setProperty("package_name", "com.acme.foo$bar");
        String dollar = NamespaceProbe.namespace();
        CN.setProperty("package_name", "com.acme.foo_bar");
        String underscore = NamespaceProbe.namespace();
        assertNotEquals(dollar, underscore, "two identities that differ keep different namespaces");

        // Including one that already contains the escape character.
        CN.setProperty("package_name", "a%0020b");
        String literal = NamespaceProbe.namespace();
        CN.setProperty("package_name", "a b");
        assertNotEquals(literal, NamespaceProbe.namespace(),
                "an identity that spells the escape is not the identity it escapes to");

        // Neither stamped. A constant, rather than a name that looks unique and is not.
        CN.setProperty("package_name", null);
        CN.setProperty("AppName", null);
        assertEquals("cn1app", NamespaceProbe.namespace(),
                "a build that stamped neither still gets a stable answer");
    }

    /** A store with no create-if-absent of its own, which is what the base class assumes. */
    private static class RecordingStore extends SecureStorage {
        final java.util.Map<String, String> entries = new java.util.HashMap<String, String>();

        int writes;

        @Override
        public boolean set(String account, String value) {
            writes++;
            entries.put(account, value);
            return true;
        }

        @Override
        public String get(String account) {
            return entries.get(account);
        }

        @Override
        public int entryState(String account) {
            return entries.containsKey(account) ? ENTRY_PRESENT : ENTRY_ABSENT;
        }
    }

    @Test
    void creatingAnEntryThatIsAlreadyThereTakesTheStoredValue() {
        // The property a first-time managed key depends on. Two processes can both find nothing
        // stored -- Android components with their own android:process, or two runs of a desktop
        // build -- and if the loser overwrites the winner, the database is encrypted under a key
        // that no longer exists anywhere.
        RecordingStore store = new RecordingStore();
        assertEquals("first", store.setIfAbsent("cn1.db.key.shared", "first"),
                "the first caller stores its own value");

        assertEquals("first", store.setIfAbsent("cn1.db.key.shared", "second"),
                "the second caller is handed the value that won, not its own");
        assertEquals("first", store.get("cn1.db.key.shared"), "and the store still holds it");
        assertEquals(1, store.writes, "the loser must not write at all");
    }

    @Test
    void creatingAnEntryReportsWhatTheStoreEndedUpWith() {
        // A store that already holds something unreadable-to-us answers with it rather than
        // reporting the value this caller wanted to write.
        RecordingStore store = new RecordingStore();
        store.entries.put("cn1.db.key.shared", "not a key we wrote");
        assertEquals("not a key we wrote", store.setIfAbsent("cn1.db.key.shared", "ours"));
        assertEquals(0, store.writes);
    }

    @Test
    void creatingAnEntryTheStoreCannotHoldAnswersNull() {
        // A refusal has to be distinguishable from success: ManagedKeys turns this into
        // KEY_UNAVAILABLE rather than opening a database with a key nothing kept.
        SecureStorage refuses = new SecureStorage() {
            @Override
            public boolean set(String account, String value) {
                return false;
            }

            @Override
            public String get(String account) {
                return null;
            }

            @Override
            public int entryState(String account) {
                return ENTRY_ABSENT;
            }
        };
        assertNull(refuses.setIfAbsent("cn1.db.key.shared", "ours"));
    }

}
