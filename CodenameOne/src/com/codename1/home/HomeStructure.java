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

/// One home: HomeKit's `HMHome`, a Google Home structure.
///
/// A user can have several -- a house and a holiday flat -- so
/// [SmartHome#getStructures()] returns a list and [#isPrimary()] marks the one
/// their ecosystem app opens by default. An app that only ever wants one
/// should use the primary rather than the first.
///
/// An immutable snapshot; see [Accessory] for why.
public final class HomeStructure {

    private final String id;
    private final String name;
    private final boolean primary;
    private final boolean owner;
    private final boolean sceneAuthoringSupported;
    private final List<HomeRoom> rooms;
    private final List<HomeZone> zones;
    private final List<Accessory> accessories;
    private final List<Scene> scenes;

    /// Creates a structure snapshot. Called by the ports and by the local
    /// home; application code receives these rather than building them.
    ///
    /// #### Parameters
    ///
    /// - `id`: the structure identifier
    ///
    /// - `name`: the user-visible name, or `null` for none
    ///
    /// - `primary`: whether this is the user's default home
    ///
    /// - `owner`: whether the user owns this home rather than being a guest
    ///
    /// - `sceneAuthoringSupported`: whether scenes can be created and deleted
    ///   here
    ///
    /// - `rooms`: its rooms; `null` becomes empty
    ///
    /// - `zones`: its zones; `null` becomes empty, which is what every
    ///   backend but HomeKit produces
    ///
    /// - `accessories`: its accessories; `null` becomes empty
    ///
    /// - `scenes`: its scenes; `null` becomes empty
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `id` is `null` or empty
    public HomeStructure(String id, String name, boolean primary,
            boolean owner, boolean sceneAuthoringSupported,
            List<HomeRoom> rooms, List<HomeZone> zones,
            List<Accessory> accessories, List<Scene> scenes) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("structure id is required");
        }
        this.id = id;
        this.name = name == null ? "" : name;
        this.primary = primary;
        this.owner = owner;
        this.sceneAuthoringSupported = sceneAuthoringSupported;
        this.rooms = freezeRooms(rooms);
        this.zones = freezeZones(zones);
        this.accessories = freezeAccessories(accessories);
        this.scenes = freezeScenes(scenes);
    }

    private static List<HomeRoom> freezeRooms(List<HomeRoom> in) {
        if (in == null || in.isEmpty()) {
            return Collections.<HomeRoom>emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<HomeRoom>(in));
    }

    private static List<HomeZone> freezeZones(List<HomeZone> in) {
        if (in == null || in.isEmpty()) {
            return Collections.<HomeZone>emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<HomeZone>(in));
    }

    private static List<Accessory> freezeAccessories(List<Accessory> in) {
        if (in == null || in.isEmpty()) {
            return Collections.<Accessory>emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<Accessory>(in));
    }

    private static List<Scene> freezeScenes(List<Scene> in) {
        if (in == null || in.isEmpty()) {
            return Collections.<Scene>emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<Scene>(in));
    }

    /// The identifier this structure is addressed by.
    ///
    /// #### Returns
    ///
    /// the identifier, never `null`
    public String getId() {
        return id;
    }

    /// The user-visible name, empty when the structure has none. The user's
    /// own text; treat it as untrusted beyond display.
    ///
    /// #### Returns
    ///
    /// the name, never `null`
    public String getName() {
        return name;
    }

    /// Whether this is the user's default home.
    ///
    /// **Always `false` on iOS.** Apple deprecated
    /// `HMHomeManager.primaryHome` in iOS 16.1 as "no longer supported" and
    /// shipped nothing to replace it, so the platform genuinely cannot say
    /// which home the user thinks of as theirs. The iOS port reports `false`
    /// for every home rather than guessing, and
    /// [SmartHome#getPrimaryStructure()] falls back to the first -- which is
    /// at least not a claim about what the user prefers.
    ///
    /// #### Returns
    ///
    /// `true` for the primary structure
    public boolean isPrimary() {
        return primary;
    }

    /// Whether the user owns this home rather than having been invited to it.
    ///
    /// A guest's permissions vary by home and by accessory, so a write can
    /// still fail with [HomeError#UNAUTHORIZED] in a home they own; this is
    /// worth surfacing mainly so an app can explain why a control it offered
    /// did not work.
    ///
    /// #### Returns
    ///
    /// `true` when the user is the owner
    public boolean isOwner() {
        return owner;
    }

    /// Whether scenes can be created and deleted in this home through
    /// [SmartHome#createScene(HomeStructure, java.lang.String, java.util.List)]
    /// and [SmartHome#deleteScene(Scene)].
    ///
    /// `false` on backends that will run a scene but not author one, and for a
    /// guest without permission. Check it before offering a "save this as a
    /// scene" button.
    ///
    /// #### Returns
    ///
    /// `true` when scenes can be authored here
    public boolean isSceneAuthoringSupported() {
        return sceneAuthoringSupported;
    }

    /// The rooms in this home.
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<HomeRoom> getRooms() {
        return rooms;
    }

    /// The zones in this home.
    ///
    /// **Empty on every backend but HomeKit**, which is the only one with the
    /// concept; see [HomeZone].
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<HomeZone> getZones() {
        return zones;
    }

    /// Every accessory in this home, in every room and in none.
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<Accessory> getAccessories() {
        return accessories;
    }

    /// The scenes in this home.
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<Scene> getScenes() {
        return scenes;
    }

    /// One room by identifier.
    ///
    /// #### Parameters
    ///
    /// - `roomId`: the identifier to look up, or `null`
    ///
    /// #### Returns
    ///
    /// the room, or `null` when this home has no such room
    public HomeRoom getRoom(String roomId) {
        if (roomId == null) {
            return null;
        }
        for (HomeRoom room : rooms) {
            if (roomId.equals(room.getId())) {
                return room;
            }
        }
        return null;
    }

    /// One accessory by identifier.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the identifier to look up, or `null`
    ///
    /// #### Returns
    ///
    /// the accessory, or `null` when this home has no such accessory
    public Accessory getAccessory(String accessoryId) {
        if (accessoryId == null) {
            return null;
        }
        for (Accessory a : accessories) {
            if (accessoryId.equals(a.getId())) {
                return a;
            }
        }
        return null;
    }

    /// The accessories in one room.
    ///
    /// #### Parameters
    ///
    /// - `roomId`: the room to filter by; `null` selects the accessories that
    ///   are in no room at all, which is a real state on both backends and
    ///   easy to lose a device in
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<Accessory> getAccessoriesInRoom(String roomId) {
        List<Accessory> out = new ArrayList<Accessory>();
        for (Accessory a : accessories) {
            if (roomId == null ? a.getRoomId() == null
                    : roomId.equals(a.getRoomId())) {
                out.add(a);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /// Every accessory in this home that exposes a trait.
    ///
    /// #### Parameters
    ///
    /// - `trait`: the trait to look for, or `null`
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<Accessory> getAccessoriesSupporting(Trait trait) {
        if (trait == null) {
            return Collections.<Accessory>emptyList();
        }
        List<Accessory> out = new ArrayList<Accessory>();
        for (Accessory a : accessories) {
            if (a.supports(trait)) {
                out.add(a);
            }
        }
        return Collections.unmodifiableList(out);
    }

    @Override
    public String toString() {
        return "Home[" + id + (name.length() > 0 ? " " + name : "")
                + (primary ? " primary" : "") + " " + accessories.size()
                + " accessories]";
    }
}
