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
package com.codename1.health.sensors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A discovered health sensor: what it is, what it advertises, and how
/// strong its signal was.
public final class HealthSensor {

    private final String id;
    private final String name;
    private final List<HealthSensorProfile> profiles;
    private final int rssi;

    /// Creates a descriptor. Called by [HealthSensors] during discovery.
    HealthSensor(String id, String name, List<HealthSensorProfile> profiles,
            int rssi) {
        this.id = id;
        this.name = name;
        List<HealthSensorProfile> copy =
                new ArrayList<HealthSensorProfile>();
        if (profiles != null) {
            copy.addAll(profiles);
        }
        this.profiles = copy;
        this.rssi = rssi;
    }

    /// A stable identifier for this device.
    ///
    /// Persist it and pass it to
    /// [HealthSensors#connect(String,HealthSensorProfile,SensorSessionOptions)]
    /// to reconnect to the user's own strap on a later launch without
    /// making them pick it out of a scan list again.
    ///
    /// The value is platform-scoped: a MAC address on Android and an
    /// opaque per-installation UUID on iOS. It is stable on one device but
    /// meaningless on another, so do not sync it between a user's phone
    /// and tablet.
    public String getId() {
        return id;
    }

    /// The advertised device name, or null.
    public String getName() {
        return name;
    }

    /// The health profiles this device advertised. A dual-purpose device
    /// such as a bike computer may report several.
    public List<HealthSensorProfile> getProfiles() {
        return Collections.unmodifiableList(profiles);
    }

    /// The received signal strength in dBm at discovery. Useful for
    /// sorting a picker so the strap the user is wearing appears first.
    public int getRssi() {
        return rssi;
    }

    /// `true` when this device advertised `profile`.
    public boolean supports(HealthSensorProfile profile) {
        return profiles.contains(profile);
    }

    @Override
    public String toString() {
        return "HealthSensor[" + (name == null ? id : name) + " "
                + profiles + "]";
    }
}
