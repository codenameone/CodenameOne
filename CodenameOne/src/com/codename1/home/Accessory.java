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

/// One physical device in the home: HomeKit's `HMAccessory`, a Google Home
/// device, one Matter node.
///
/// #### An immutable snapshot, not a live handle
///
/// Every getter here reads a field. Nothing calls into the platform, so
/// nothing here can block, fail, or care which thread you are on.
///
/// The alternative -- a proxy over the platform's own live object -- was
/// rejected: `HMAccessory` is a mutable Objective-C object whose properties
/// are only safe to touch on the main queue, so every getter would have been a
/// cross-boundary call with a threading rule attached, and under ParparVM an
/// expensive one. Reading six properties to lay out a row would be six hops.
///
/// The cost is that a snapshot goes stale. When the topology moves -- an
/// accessory added, removed, renamed, moved between rooms, or its reachability
/// flipping -- a [HomeStructureListener] fires and you fetch again. Trait
/// *values* are not part of the snapshot at all; read them with
/// [SmartHome#read(TraitReadRequest)] or watch them with a
/// [TraitSubscription].
public final class Accessory {

    private final String id;
    private final String name;
    private final String roomId;
    private final AccessoryCategory category;
    private final String manufacturer;
    private final String model;
    private final String firmwareVersion;
    private final boolean reachable;
    private final String bridgeAccessoryId;
    private final List<AccessoryService> services;

    /// Creates an accessory snapshot. Called by the ports and by the local
    /// home; application code receives these rather than building them.
    ///
    /// #### Parameters
    ///
    /// - `id`: the accessory identifier, unique within the backend
    ///
    /// - `name`: the user-visible name, or `null` for none
    ///
    /// - `roomId`: the room it is in, or `null` when it is in none
    ///
    /// - `category`: roughly what it is; `null` becomes
    ///   [AccessoryCategory#OTHER]
    ///
    /// - `manufacturer`: the maker, or `null`
    ///
    /// - `model`: the model name, or `null`
    ///
    /// - `firmwareVersion`: the firmware version, or `null`
    ///
    /// - `reachable`: whether the platform could talk to it when the snapshot
    ///   was taken
    ///
    /// - `bridgeAccessoryId`: the bridge it sits behind, or `null` when it
    ///   talks to the platform directly
    ///
    /// - `services`: its functional endpoints; `null` becomes empty
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `id` is `null` or empty
    public Accessory(String id, String name, String roomId,
            AccessoryCategory category, String manufacturer, String model,
            String firmwareVersion, boolean reachable,
            String bridgeAccessoryId, List<AccessoryService> services) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("accessory id is required");
        }
        this.id = id;
        this.name = name == null ? "" : name;
        this.roomId = roomId;
        this.category = category == null ? AccessoryCategory.OTHER : category;
        this.manufacturer = manufacturer == null ? "" : manufacturer;
        this.model = model == null ? "" : model;
        this.firmwareVersion = firmwareVersion == null ? "" : firmwareVersion;
        this.reachable = reachable;
        this.bridgeAccessoryId = bridgeAccessoryId;
        if (services == null || services.isEmpty()) {
            this.services = Collections.<AccessoryService>emptyList();
        } else {
            this.services = Collections.unmodifiableList(
                    new ArrayList<AccessoryService>(services));
        }
    }

    /// The identifier this accessory is addressed by.
    ///
    /// Unique across the whole backend, not merely within its structure, so a
    /// read or a write needs only this and a service id.
    ///
    /// Stable for the life of the process on every backend, and stable across
    /// launches wherever the platform provides a stable identifier -- which
    /// both do today. [SmartHome#areIdsPersistent()] is the honest answer for
    /// a given backend, and is what to check before persisting one as a user's
    /// favourite.
    ///
    /// #### Returns
    ///
    /// the identifier, never `null`
    public String getId() {
        return id;
    }

    /// The user-visible name, empty when the accessory has none.
    ///
    /// This is the user's own text, from their ecosystem app. Treat it as
    /// untrusted for anything beyond display.
    ///
    /// #### Returns
    ///
    /// the name, never `null`
    public String getName() {
        return name;
    }

    /// The room this accessory is in.
    ///
    /// #### Returns
    ///
    /// the room identifier, or `null` when it is not assigned to one
    public String getRoomId() {
        return roomId;
    }

    /// Roughly what this accessory is, for icons and grouping. Read the
    /// services to decide what it can do.
    ///
    /// #### Returns
    ///
    /// the category, never `null`
    public AccessoryCategory getCategory() {
        return category;
    }

    /// The manufacturer, empty when unknown.
    ///
    /// #### Returns
    ///
    /// the manufacturer, never `null`
    public String getManufacturer() {
        return manufacturer;
    }

    /// The model name, empty when unknown.
    ///
    /// #### Returns
    ///
    /// the model, never `null`
    public String getModel() {
        return model;
    }

    /// The firmware version, empty when unknown.
    ///
    /// #### Returns
    ///
    /// the firmware version, never `null`
    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    /// Whether the platform could talk to this accessory when the snapshot
    /// was taken.
    ///
    /// A snapshot's answer, so it can be out of date; the current answer
    /// arrives through [StructureChangeKind#REACHABILITY_CHANGED]. An
    /// operation on an unreachable accessory fails with
    /// [HomeError#ACCESSORY_UNREACHABLE], which is worth handling even when
    /// this said `true`.
    ///
    /// #### Returns
    ///
    /// `true` when the accessory was reachable
    public boolean isReachable() {
        return reachable;
    }

    /// Whether this accessory sits behind a bridge rather than talking to the
    /// platform directly.
    ///
    /// Worth surfacing because a bridge going offline takes every accessory
    /// behind it with it, and "twelve lights stopped responding" is much
    /// easier to explain when you can name the one device that actually
    /// failed.
    ///
    /// #### Returns
    ///
    /// `true` when [#getBridgeAccessoryId()] names a bridge
    public boolean isBridged() {
        return bridgeAccessoryId != null && bridgeAccessoryId.length() > 0;
    }

    /// The bridge this accessory sits behind.
    ///
    /// #### Returns
    ///
    /// the bridge's accessory id, or `null` when it is not bridged
    public String getBridgeAccessoryId() {
        return bridgeAccessoryId;
    }

    /// Every functional endpoint of this accessory.
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<AccessoryService> getServices() {
        return services;
    }

    /// One service by identifier.
    ///
    /// #### Parameters
    ///
    /// - `serviceId`: the identifier to look up, or `null`
    ///
    /// #### Returns
    ///
    /// the service, or `null` when this accessory has no such service
    public AccessoryService getService(String serviceId) {
        if (serviceId == null) {
            return null;
        }
        for (int i = 0; i < services.size(); i++) {
            AccessoryService s = services.get(i);
            if (serviceId.equals(s.getId())) {
                return s;
            }
        }
        return null;
    }

    /// The accessory's main service -- what a UI showing one control for the
    /// whole device should drive.
    ///
    /// Falls back to the first service when none is flagged primary, and to
    /// `null` only when there are no services at all.
    ///
    /// #### Returns
    ///
    /// the primary service, or `null`
    public AccessoryService getPrimaryService() {
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).isPrimary()) {
                return services.get(i);
            }
        }
        return services.isEmpty() ? null : services.get(0);
    }

    /// Every service on this accessory that exposes a trait.
    ///
    /// More than one for a device with repeated endpoints -- the two halves of
    /// a two-gang switch both expose [Trait#ON_OFF] -- which is exactly why a
    /// write names a service rather than an accessory.
    ///
    /// #### Parameters
    ///
    /// - `trait`: the trait to look for, or `null`
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<AccessoryService> getServicesSupporting(Trait trait) {
        if (trait == null || services.isEmpty()) {
            return Collections.<AccessoryService>emptyList();
        }
        List<AccessoryService> out = new ArrayList<AccessoryService>();
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).supports(trait)) {
                out.add(services.get(i));
            }
        }
        return Collections.unmodifiableList(out);
    }

    /// Whether any service on this accessory exposes a trait.
    ///
    /// #### Parameters
    ///
    /// - `trait`: the trait to test, or `null`
    ///
    /// #### Returns
    ///
    /// `true` when at least one service has it
    public boolean supports(Trait trait) {
        if (trait == null) {
            return false;
        }
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).supports(trait)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return category.name() + "[" + id
                + (name.length() > 0 ? " " + name : "")
                + (reachable ? "" : " unreachable") + "]";
    }
}
