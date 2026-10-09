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
package com.codename1.impl.migration;

import com.codename1.migration.MigrationSet;
import java.util.ArrayList;
import java.util.List;

/// The migration sets an application registered, in the order they run: library sets first, the
/// application's own `default` set last, so application tables may reference library ones.
///
/// Filled once at start-up by generated bootstrap code and by libraries, before anything opens a
/// database.
///
/// Internal migration runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class MigrationRegistry {
    private static final List<MigrationSet> SETS = new ArrayList<MigrationSet>();

    private MigrationRegistry() {
    }

    /// Registers a set, replacing one of the same name.
    public static void register(MigrationSet set) {
        for (int i = 0; i < SETS.size(); i++) {
            if (SETS.get(i).getName().equals(set.getName())) {
                SETS.set(i, set);
                return;
            }
        }
        if (set.isDefault()) {
            SETS.add(set);
            return;
        }
        int at = SETS.size();
        if (at > 0 && SETS.get(at - 1).isDefault()) {
            at--;
        }
        SETS.add(at, set);
    }

    /// Removes a set; used by tests that register their own.
    public static void unregister(String name) {
        for (int i = 0; i < SETS.size(); i++) {
            if (SETS.get(i).getName().equals(name)) {
                SETS.remove(i);
                return;
            }
        }
    }

    /// The registered sets in run order.
    public static MigrationSet[] sets() {
        return SETS.toArray(new MigrationSet[SETS.size()]);
    }

    /// The set registered under a name, or null.
    public static MigrationSet find(String name) {
        for (MigrationSet set : SETS) {
            if (set.getName().equals(name)) {
                return set;
            }
        }
        return null;
    }

    /// Whether nothing is registered.
    public static boolean isEmpty() {
        return SETS.isEmpty();
    }
}
