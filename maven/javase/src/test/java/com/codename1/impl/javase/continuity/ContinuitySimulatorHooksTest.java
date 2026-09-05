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
package com.codename1.impl.javase.continuity;

import com.codename1.impl.continuity.LocalContinuityBridge;
import com.codename1.impl.javase.ContinuitySimulatorHooks;
import com.codename1.impl.javase.JavaSEPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The "synced store unsupported" simulation has to disable the STORE, not only its answer about
 * itself.
 *
 * <p>The framework deliberately stopped gating store calls on {@code isSyncedStoreSupported()}: on
 * iOS the store is local and works whether or not the build is entitled to sync it. That left this
 * hook overriding the predicate alone and nothing else, so the simulation kept a fully working
 * store -- and an application that ignores {@code isSupported()} kept its setting here while
 * losing it on Android, which is the exact failure the menu item exists to reproduce.</p>
 */
class ContinuitySimulatorHooksTest {

    @AfterEach
    void restoreTheCapablePlatform() {
        ContinuitySimulatorHooks.makeEverythingSupported();
    }

    @Test
    void theUnsupportedStoreSimulationAlsoDisablesTheOperations() throws Exception {
        // Asserted on what the installed bridge OVERRIDES rather than on values it stores. The
        // store persists through Storage, which this harness has no initialised runtime for, so a
        // value-level check could not run -- and worse, "put returned false" there would be
        // ambiguous between "this platform has no store" and "there was nowhere to write", which
        // is precisely the distinction the test exists to make.
        // The control is the SIBLING hook, not the plain bridge. makeEverythingSupported()
        // installs LocalContinuityBridge itself, which of course declares the store methods --
        // it is the implementation -- so comparing against it proved nothing and said so when it
        // fired. makeContinuationUnsupported() installs the same SHAPE, an anonymous subclass
        // overriding one predicate, and it must NOT touch the store.
        ContinuitySimulatorHooks.makeContinuationUnsupported();
        Class<?> otherHook = JavaSEPort.getSimulatedContinuity().getClass();

        ContinuitySimulatorHooks.makeTheSyncedStoreUnsupported();
        LocalContinuityBridge unsupported = JavaSEPort.getSimulatedContinuity();

        assertFalse(unsupported.isSyncedStoreSupported(),
                "the hook did not make the store report itself unsupported");

        Class<?> off = unsupported.getClass();
        String[] operations = {"syncedStorePut", "syncedStoreGet", "syncedStoreRemove",
            "syncedStoreKeys"};
        Class<?>[][] signatures = {
            {String.class, String.class}, {String.class}, {String.class}, {}
        };
        for (int i = 0; i < operations.length; i++) {
            assertTrue(declares(off, operations[i], signatures[i]),
                    operations[i] + " is inherited from the working store, so a platform with no "
                            + "synced store still keeps and returns values -- an application that "
                            + "ignores isSupported() passes here and loses its setting on Android");
            // The control: a hook about a DIFFERENT capability must not override them, or the
            // check above would be satisfied by any anonymous bridge at all.
            assertFalse(declares(otherHook, operations[i], signatures[i]),
                    "the continuation hook overrides " + operations[i] + " as well, so the "
                            + "assertion above is true of any hook and distinguishes nothing");
        }
    }

    private static boolean declares(Class<?> c, String name, Class<?>[] args) {
        try {
            c.getDeclaredMethod(name, args);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
