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
package com.codename1.continuity;

import com.codename1.continuity.sync.SyncedStore;
import com.codename1.junit.EdtTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A port that implements nothing.
 *
 * <p>This is the ordinary case for Android, the desktop and the browser, and it is the case that
 * has to stay boring: an app that references the continuity API and runs where the platform
 * carries nothing between devices must get honest answers, not exceptions. Every entry point is
 * exercised here precisely because none of them is interesting.</p>
 *
 * <p>Note what is NOT unsupported on such a port: saving and restoring state on the device itself.
 * That half is pure storage and has no bridge behind it at all, which is why it is tested
 * elsewhere rather than here.</p>
 */
public class ContinuityDegradationTest extends UITestBase {

    @BeforeEach
    public void noBridge() {
        Continuity.reset();
        Continuity.setBridge(new NullContinuityBridge());
        Continuity.enable();
    }

    @AfterEach
    public void clear() {
        Continuity.reset();
    }

    @EdtTest
    public void everyCapabilityQueryAnswersFalselyRatherThanThrowing() {
        assertFalse(Continuity.isContinuationSupported());
        assertFalse(SyncedStore.isSupported());
    }

    @EdtTest
    public void publishingAContinuationIsAnInertNoOp() {
        Continuity.setTitle("Something");
        Continuity.checkpoint();
    }

    @EdtTest
    public void theSyncedStoreAnswersWithTheDefaultAndKeepsNothing() {
        assertFalse(SyncedStore.put("sortOrder", "byDate"));
        assertEquals("byName", SyncedStore.get("sortOrder", "byName"));
        SyncedStore.remove("sortOrder");
        assertArrayEquals(new String[0], SyncedStore.keys());
    }

    /**
     * The argument checks are NOT part of the degradation.
     *
     * <p>A null key is a programming error wherever it happens, and letting it pass silently on
     * the ports where the store does nothing means it is found for the first time on the one port
     * where it does something.</p>
     */
    @EdtTest
    public void argumentMistakesStillFailOnAPortWithNoStore() {
        assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() {
                        SyncedStore.get(null, "x");
                    }
                });
        assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() {
                        SyncedStore.put("k", null);
                    }
                });
    }

    /**
     * A bridge that throws from everything, which is what a port mid-failure looks like.
     *
     * <p>The framework runs on housekeeping paths -- a navigation, a suspend -- so an exception
     * escaping one of them takes down a flow that has nothing to do with continuity.</p>
     */
    @EdtTest
    public void aBridgeThatThrowsFromEverythingDoesNotEscape() {
        Continuity.setBridge(new ThrowingContinuityBridge());

        assertFalse(Continuity.isContinuationSupported());
        assertFalse(SyncedStore.isSupported());
        assertFalse(SyncedStore.put("k", "v"));
        assertEquals("d", SyncedStore.get("k", "d"));
        SyncedStore.remove("k");
        assertArrayEquals(new String[0], SyncedStore.keys());
        Continuity.checkpoint();
        Continuity.disable();
    }

    /** Reports nothing supported and records nothing. */
    static class NullContinuityBridge implements com.codename1.continuity.spi.ContinuityBridge {
        public void setCallback(com.codename1.continuity.spi.ContinuityCallback callback) {
        }

        public boolean isContinuationSupported() {
            return false;
        }

        public void publishContinuation(String activityType, String title,
                java.util.Map<String, Object> userInfo) {
            throw new IllegalStateException("must not be called when unsupported");
        }

        public void clearContinuation() {
        }

        public boolean isSyncedStoreSupported() {
            return false;
        }

        public void syncedStorePut(String key, String value) {
            throw new IllegalStateException("must not be called when unsupported");
        }

        public String syncedStoreGet(String key) {
            throw new IllegalStateException("must not be called when unsupported");
        }

        public void syncedStoreRemove(String key) {
            throw new IllegalStateException("must not be called when unsupported");
        }

        public String[] syncedStoreKeys() {
            throw new IllegalStateException("must not be called when unsupported");
        }
    }

    /** Throws from every method, including the capability queries. */
    static class ThrowingContinuityBridge implements com.codename1.continuity.spi.ContinuityBridge {
        public void setCallback(com.codename1.continuity.spi.ContinuityCallback callback) {
            throw new IllegalStateException("boom");
        }

        public boolean isContinuationSupported() {
            throw new IllegalStateException("boom");
        }

        public void publishContinuation(String activityType, String title,
                java.util.Map<String, Object> userInfo) {
            throw new IllegalStateException("boom");
        }

        public void clearContinuation() {
            throw new IllegalStateException("boom");
        }

        public boolean isSyncedStoreSupported() {
            throw new IllegalStateException("boom");
        }

        public void syncedStorePut(String key, String value) {
            throw new IllegalStateException("boom");
        }

        public String syncedStoreGet(String key) {
            throw new IllegalStateException("boom");
        }

        public void syncedStoreRemove(String key) {
            throw new IllegalStateException("boom");
        }

        public String[] syncedStoreKeys() {
            throw new IllegalStateException("boom");
        }
    }
}
