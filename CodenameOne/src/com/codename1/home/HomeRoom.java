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
package com.codename1.home;

/// A room in a [HomeStructure]: HomeKit's `HMRoom`, a Google Home room.
///
/// An immutable snapshot. Rooms cannot be created, renamed or deleted through
/// this API -- the graph is read-only in this release -- so a room only ever
/// arrives from the platform.
public final class HomeRoom {

    private final String id;
    private final String name;
    private final String structureId;

    /// Creates a room snapshot. Called by the ports and by the local home.
    ///
    /// #### Parameters
    ///
    /// - `id`: the room identifier
    ///
    /// - `name`: the user-visible name, or `null` for none
    ///
    /// - `structureId`: the structure this room belongs to
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `id` is `null` or empty
    public HomeRoom(String id, String name, String structureId) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("room id is required");
        }
        this.id = id;
        this.name = name == null ? "" : name;
        this.structureId = structureId;
    }

    /// The identifier this room is addressed by.
    ///
    /// #### Returns
    ///
    /// the identifier, never `null`
    public String getId() {
        return id;
    }

    /// The user-visible name, empty when the room has none. The user's own
    /// text; treat it as untrusted beyond display.
    ///
    /// #### Returns
    ///
    /// the name, never `null`
    public String getName() {
        return name;
    }

    /// The structure this room belongs to.
    ///
    /// #### Returns
    ///
    /// the structure identifier, or `null` when unknown
    public String getStructureId() {
        return structureId;
    }

    @Override
    public String toString() {
        return "Room[" + id + (name.length() > 0 ? " " + name : "") + "]";
    }
}
