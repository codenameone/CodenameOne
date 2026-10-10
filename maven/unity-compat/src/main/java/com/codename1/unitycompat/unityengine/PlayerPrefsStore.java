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
package com.codename1.unitycompat.unityengine;

/// Where [PlayerPrefs] keeps what it is given.
///
/// Unity saves preferences in a place of the platform's own -- the
/// registry, a property list, shared preferences. The runtime core cannot
/// name Codename One's equivalent, because the core also runs where there
/// is no Codename One at all: under a plain JVM and through ParparVM with
/// nothing but `System.out`. So the storage is an interface. With none
/// installed, preferences live in memory and last as long as the process;
/// the game view installs one that keeps them in the device's preferences.
///
/// A value is an `Integer`, a `Float` or a `String`, and a store hands
/// back the same kind it was given: Unity remembers which of the three a
/// key was set as, and reading it as another answers the default.
public interface PlayerPrefsStore {
    /// The value kept under `key`, or null.
    java.lang.Object get(String key);

    void set(String key, java.lang.Object value);

    void delete(String key);

    void deleteAll();

    /// Writes anything not yet written. Unity does this on quitting; a
    /// store that writes as it goes has nothing to do.
    void save();
}
