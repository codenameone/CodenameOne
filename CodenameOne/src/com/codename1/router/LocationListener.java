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
package com.codename1.router;

/// Receives notifications when the Router's current `Location` changes.
///
/// Listeners run on the EDT, after the Form transition has been initiated but
/// before it has completed. For after-transition hooks, use Form#onShowCompleted.
///
/// #### Since 8.0
public interface LocationListener {

    /// What kind of change produced the new location.
    enum Kind {
        /// A `push` added a new entry on top.
        PUSH,
        /// A `pop` removed the top entry; current is the entry beneath.
        POP,
        /// A `replace` swapped the top entry without changing depth.
        REPLACE,
        /// The router was reset/initialized to a starting location.
        RESET
    }

    /// Called after the Router commits a navigation. `previous` is null on the very
    /// first RESET event.
    void onLocationChanged(Location previous, Location current, Kind kind);
}
