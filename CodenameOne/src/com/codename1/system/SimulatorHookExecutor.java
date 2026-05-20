/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.system;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// Cross-platform registry of named actions that the JavaSE simulator exposes.
///
/// The simulator scans cn1libs (and the app itself) for
/// `META-INF/codenameone/simulator-hooks.properties` files; each hook
/// declared there is registered here under `namespace:itemN` keys. The
/// JavaSE port hooks into [com.codename1.ui.CN#execute(String)] /
/// [com.codename1.ui.CN#canExecute(String)] so cross-platform code (such
/// as a CN1 UnitTest under `common/`) can invoke a hook via the same
/// `execute` it would use for any other URL, with no JavaSE-only import.
///
/// On Android, iOS, JavaScript and other production targets this registry
/// is always empty, so `execute` returns `false` and `isRegistered` is
/// always `false` -- the "running outside a simulator" signal.
///
/// Hooks can be menu-backed (shipped with a label and visible in the
/// simulator's menu bar) or API-only (no label, callable by URL only).
/// Tests that want behavioral coverage of cn1lib internals lean on the
/// API-only form so the menu UX stays focused on actions a human would
/// click.
public final class SimulatorHookExecutor {

    private static volatile Map<String, Runnable> hooks = Collections.emptyMap();

    private SimulatorHookExecutor() {}

    /// Invokes the action registered under `hookId`. Returns `true`
    /// if a hook with that id was found and dispatched, `false`
    /// otherwise. Invocation is delegated to whatever the registering code
    /// configured (the JavaSE port wraps each hook in
    /// `Display.callSeriallyAndWait`, so menu actions and tests run on the
    /// CN1 EDT and the call is synchronous from off-EDT callers).
    ///
    /// #### Parameters
    ///
    /// - `hookId`: opaque id of the form `namespace:hook` (the exact
    ///   value the hook author chose in the properties file).
    public static boolean execute(String hookId) {
        if (hookId == null) {
            return false;
        }
        Runnable r = hooks.get(hookId);
        if (r == null) {
            return false;
        }
        r.run();
        return true;
    }

    /// Returns `true` if a hook with the given id is registered.
    /// Useful for tests that want to skip themselves gracefully when running
    /// on a platform that doesn't expose the relevant cn1lib hook.
    public static boolean isRegistered(String hookId) {
        return hookId != null && hooks.containsKey(hookId);
    }

    /// Diagnostic view of every registered id. Returns an unmodifiable
    /// snapshot -- never null. Intended for tests/inspectors; ordinary app
    /// code shouldn't need this.
    public static Collection<String> registeredIds() {
        return Collections.unmodifiableCollection(hooks.keySet());
    }

    /// Replaces the entire registry. The JavaSE port calls this every time
    /// it rebuilds the simulator menu (e.g., after a reload). On non-simulator
    /// targets nothing calls it and the registry stays empty.
    public static void register(Map<String, Runnable> registered) {
        if (registered == null || registered.isEmpty()) {
            hooks = Collections.emptyMap();
            return;
        }
        hooks = Collections.unmodifiableMap(new HashMap<String, Runnable>(registered));
    }
}
