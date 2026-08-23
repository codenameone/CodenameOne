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
package com.codename1.nearby.companion;

/// A device this app is associated with.
///
/// An association outlives the app: it is stored by the OS, survives
/// restarts, and is what makes background presence notifications and
/// permission-free scanning possible. It ends when the app calls
/// [CompanionDevices#disassociate], when the user revokes it in system
/// settings, or when the app is uninstalled.
public final class CompanionDevice {

    private final String id;
    private final String displayName;
    private final String address;
    private final CompanionProfile profile;
    private final boolean present;

    /// Ports construct these; application code reads them from
    /// [CompanionDevices].
    ///
    /// #### Parameters
    ///
    /// - `id`: the platform's association id
    /// - `displayName`: the name to show a user, never null
    /// - `address`: the device address, or null when the platform withholds
    ///   it
    /// - `profile`: the profile the association was made under
    /// - `present`: whether the device is in range right now
    public CompanionDevice(String id, String displayName, String address,
            CompanionProfile profile, boolean present) {
        this.id = id;
        this.displayName = displayName == null ? "" : displayName;
        this.address = address;
        this.profile = profile == null ? CompanionProfile.GENERIC : profile;
        this.present = present;
    }

    /// The association id, stable across app restarts. This is what
    /// [CompanionDevices#disassociate] and
    /// [CompanionDevices#startObservingPresence] take, and what to persist.
    public String getId() {
        return id;
    }

    /// The name to show a user. Never null, occasionally empty where the
    /// device advertises none.
    public String getDisplayName() {
        return displayName;
    }

    /// The device address, or `null` where the platform does not hand it
    /// out. Where it is present it matches
    /// `com.codename1.bluetooth.BluetoothDevice#getAddress()`, so it can be
    /// passed to `BluetoothLE.getPeripheral(String)` to open a GATT
    /// connection to the associated device.
    ///
    /// Android returns the MAC address for a Bluetooth association. iOS
    /// returns the per-app accessory identifier.
    public String getAddress() {
        return address;
    }

    /// The profile this association was made under.
    public CompanionProfile getProfile() {
        return profile;
    }

    /// Whether the device was in range when this record was produced.
    ///
    /// This is a snapshot, not a live value -- re-read it from
    /// [CompanionDevices#getAssociations()], or watch
    /// [PresenceListener] for changes. Platforms that do not track presence
    /// report `false`.
    public boolean isPresent() {
        return present;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompanionDevice)) {
            return false;
        }
        CompanionDevice d = (CompanionDevice) o;
        return id == null ? d.id == null : id.equals(d.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "CompanionDevice[" + id + ", " + displayName
                + ", profile=" + profile + ", present=" + present + "]";
    }
}
