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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A named grouping of rooms -- "upstairs", "the annexe": HomeKit's `HMZone`.
///
/// **HomeKit only.** No other backend has the concept, and
/// [HomeStructure#getZones()] returns an empty list on them rather than
/// synthesizing groupings out of room names. A guess about which rooms a user
/// thinks of as upstairs is not information, and an app that laid out its
/// navigation around invented zones would look broken on the platform that has
/// real ones.
///
/// An immutable snapshot.
public final class HomeZone {

    private final String id;
    private final String name;
    private final List<String> roomIds;

    /// Creates a zone snapshot. Called by the iOS port.
    ///
    /// #### Parameters
    ///
    /// - `id`: the zone identifier
    ///
    /// - `name`: the user-visible name, or `null` for none
    ///
    /// - `roomIds`: the rooms in this zone; `null` becomes empty
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `id` is `null` or empty
    public HomeZone(String id, String name, List<String> roomIds) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("zone id is required");
        }
        this.id = id;
        this.name = name == null ? "" : name;
        if (roomIds == null || roomIds.isEmpty()) {
            this.roomIds = Collections.<String>emptyList();
        } else {
            this.roomIds = Collections.unmodifiableList(
                    new ArrayList<String>(roomIds));
        }
    }

    /// The identifier this zone is addressed by.
    ///
    /// #### Returns
    ///
    /// the identifier, never `null`
    public String getId() {
        return id;
    }

    /// The user-visible name, empty when the zone has none.
    ///
    /// #### Returns
    ///
    /// the name, never `null`
    public String getName() {
        return name;
    }

    /// The rooms in this zone.
    ///
    /// #### Returns
    ///
    /// an immutable list of room identifiers, possibly empty
    public List<String> getRoomIds() {
        return roomIds;
    }

    @Override
    public String toString() {
        return "Zone[" + id + (name.length() > 0 ? " " + name : "") + " "
                + roomIds.size() + " rooms]";
    }
}
