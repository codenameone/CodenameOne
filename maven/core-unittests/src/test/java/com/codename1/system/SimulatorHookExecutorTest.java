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
package com.codename1.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the process-global {@link SimulatorHookExecutor} registry:
 * registration, dispatch (and its return value), id queries, the unmodifiable
 * id snapshot, and the empty/null registration that clears the registry. The
 * registry is reset after each test so methods stay order-independent.
 */
class SimulatorHookExecutorTest {

    @AfterEach
    void clearRegistry() {
        SimulatorHookExecutor.register(null);
    }

    @Test
    void executeNullIdReturnsFalse() {
        assertFalse(SimulatorHookExecutor.execute(null));
    }

    @Test
    void executeUnknownIdReturnsFalse() {
        assertFalse(SimulatorHookExecutor.execute("nope:missing"));
    }

    @Test
    void registeredHookRunsAndReturnsTrue() {
        final AtomicInteger runs = new AtomicInteger();
        Map<String, Runnable> hooks = new HashMap<String, Runnable>();
        hooks.put("demo:reload", new Runnable() {
            public void run() {
                runs.incrementAndGet();
            }
        });
        SimulatorHookExecutor.register(hooks);

        assertTrue(SimulatorHookExecutor.execute("demo:reload"));
        assertEquals(1, runs.get());
    }

    @Test
    void isRegisteredReflectsRegistration() {
        assertFalse(SimulatorHookExecutor.isRegistered("demo:x"));
        assertFalse(SimulatorHookExecutor.isRegistered(null));
        Map<String, Runnable> hooks = new HashMap<String, Runnable>();
        hooks.put("demo:x", new Runnable() {
            public void run() {
            }
        });
        SimulatorHookExecutor.register(hooks);
        assertTrue(SimulatorHookExecutor.isRegistered("demo:x"));
    }

    @Test
    void registeredIdsIsAnUnmodifiableSnapshotContainingTheKeys() {
        Map<String, Runnable> hooks = new HashMap<String, Runnable>();
        hooks.put("a:one", noop());
        hooks.put("a:two", noop());
        SimulatorHookExecutor.register(hooks);

        Collection<String> ids = SimulatorHookExecutor.registeredIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains("a:one"));
        assertTrue(ids.contains("a:two"));
        assertThrows(UnsupportedOperationException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                ids.add("a:three");
            }
        });
    }

    @Test
    void registerNullOrEmptyClearsTheRegistry() {
        Map<String, Runnable> hooks = new HashMap<String, Runnable>();
        hooks.put("a:one", noop());
        SimulatorHookExecutor.register(hooks);
        assertFalse(SimulatorHookExecutor.registeredIds().isEmpty());

        SimulatorHookExecutor.register(null);
        assertTrue(SimulatorHookExecutor.registeredIds().isEmpty());

        SimulatorHookExecutor.register(hooks);
        SimulatorHookExecutor.register(new HashMap<String, Runnable>());
        assertTrue(SimulatorHookExecutor.registeredIds().isEmpty());
    }

    @Test
    void registryIsADefensiveCopy() {
        Map<String, Runnable> hooks = new HashMap<String, Runnable>();
        hooks.put("a:one", noop());
        SimulatorHookExecutor.register(hooks);
        // Mutating the caller's map after registration must not change the registry.
        hooks.put("a:two", noop());
        assertFalse(SimulatorHookExecutor.isRegistered("a:two"));
    }

    private static Runnable noop() {
        return new Runnable() {
            public void run() {
            }
        };
    }
}
