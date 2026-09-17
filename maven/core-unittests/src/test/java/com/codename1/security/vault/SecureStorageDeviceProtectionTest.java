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
package com.codename1.security.vault;

import com.codename1.junit.UITestBase;
import com.codename1.security.SecureStorage;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecureStorageDeviceProtectionTest extends UITestBase {
    private static final String WINNER =
            "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20";

    private static class RacingStore extends SecureStorage {
        String value;
        String publishOnProbe;
        String publishOnCreate;
        int state = ENTRY_ABSENT;
        int creates;
        int reads;

        @Override
        public String get(String account) {
            reads++;
            return value;
        }

        @Override
        public int entryState(String account) {
            if (publishOnProbe != null) {
                value = publishOnProbe;
                publishOnProbe = null;
                state = ENTRY_PRESENT;
            }
            return state;
        }

        @Override
        public String setIfAbsent(String account, String candidate) {
            creates++;
            if (publishOnCreate != null) value = publishOnCreate;
            if (value == null) value = candidate;
            state = ENTRY_PRESENT;
            return value;
        }
    }

    private static SecureStorageDeviceProtection device(final RacingStore store) {
        return new SecureStorageDeviceProtection() {
            @Override
            protected SecureStorage storage() {
                return store;
            }
        };
    }

    private static VaultError errorOf(AsyncResource<?> result) {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> result.get());
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof VaultException) return ((VaultException) cause).getError();
        }
        throw new AssertionError("Expected a VaultException", failure);
    }

    @Test
    void ensureAndWrapAdoptAKeyCreatedBetweenTheReadAndProbe() {
        for (boolean wrap : new boolean[] {false, true}) {
            RacingStore store = new RacingStore();
            store.publishOnProbe = WINNER;
            SecureStorageDeviceProtection device = device(store);
            if (wrap) {
                byte[] message = {9, 8, 7};
                byte[] aad = {1, 2};
                byte[] sealed = device.wrap("shared", message, aad).get();
                assertArrayEquals(message, SecureEnvelope.parse(sealed).open(Bytes.fromHex(WINNER), aad));
            } else {
                assertTrue(device.ensureKey("shared").get());
            }
            assertEquals(2, store.reads, "must read the concurrently published key");
            assertEquals(0, store.creates, "the winning key must not be replaced");
            assertEquals(WINNER, store.value);
        }
    }

    @Test
    void unreadableOrUnknownKeysAreNeverReplaced() {
        for (int state : new int[] {SecureStorage.ENTRY_PRESENT, SecureStorage.ENTRY_UNKNOWN}) {
            RacingStore store = new RacingStore();
            store.state = state;
            assertEquals(VaultError.TEMPORARILY_UNREADABLE, errorOf(device(store).ensureKey("shared")));
            assertEquals(0, store.creates);
            assertNull(store.value);
        }
    }

    @Test
    void aCorruptConcurrentWinnerIsReportedWithoutReplacingIt() {
        RacingStore store = new RacingStore();
        store.publishOnProbe = "not a key";
        assertEquals(VaultError.CORRUPT, errorOf(device(store).ensureKey("shared")));
        assertEquals(0, store.creates);
        assertEquals("not a key", store.value);
    }

    @Test
    void aLaterCreatorStillConvergesThroughTheAtomicCreate() {
        RacingStore store = new RacingStore();
        store.publishOnCreate = WINNER;
        byte[] message = {4, 5, 6};
        byte[] aad = {3, 2};
        byte[] sealed = device(store).wrap("shared", message, aad).get();
        assertArrayEquals(message, SecureEnvelope.parse(sealed).open(Bytes.fromHex(WINNER), aad));
        assertEquals(1, store.creates);
        assertEquals(WINNER, store.value);
    }
}
