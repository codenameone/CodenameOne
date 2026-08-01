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
package com.codename1.health;

/// Where a sample came from -- which app wrote it and, when known, which
/// device produced it.
///
/// This matters more than it looks. When a phone and a watch both record
/// steps for the same walk, the store holds two overlapping sets of
/// samples, and every platform counts the walk twice: aggregation is done
/// in shared code from raw samples, so HealthKit's own de-duplicating
/// statistics engine is not in play. Filtering a query by source via
/// [AggregateQuery#addSource(String)] is the way to avoid double-counting
/// -- see the warning on [AggregateQuery].
public final class HealthSource {

    private final String bundleId;
    private final String name;
    private final String deviceName;
    private final String deviceModel;
    private final String deviceManufacturer;

    /// Creates a source descriptor. Only `bundleId` is required; the rest
    /// may be null where the platform does not report them.
    public HealthSource(String bundleId, String name, String deviceName,
            String deviceModel, String deviceManufacturer) {
        this.bundleId = bundleId;
        this.name = name;
        this.deviceName = deviceName;
        this.deviceModel = deviceModel;
        this.deviceManufacturer = deviceManufacturer;
    }

    /// The writing app's bundle identifier (iOS) or package name
    /// (Android). This is the value to pass to
    /// [AggregateQuery#addSource(String)].
    public String getBundleId() {
        return bundleId;
    }

    /// The writing app's display name, or null.
    public String getName() {
        return name;
    }

    /// The producing device's name, or null -- for example the user's
    /// watch. Frequently null for manually entered data.
    public String getDeviceName() {
        return deviceName;
    }

    /// The producing device's model, or null.
    public String getDeviceModel() {
        return deviceModel;
    }

    /// The producing device's manufacturer, or null.
    public String getDeviceManufacturer() {
        return deviceManufacturer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HealthSource)) {
            return false;
        }
        HealthSource other = (HealthSource) o;
        return bundleId == null ? other.bundleId == null
                : bundleId.equals(other.bundleId);
    }

    @Override
    public int hashCode() {
        return bundleId == null ? 0 : bundleId.hashCode();
    }

    @Override
    public String toString() {
        return name == null ? String.valueOf(bundleId)
                : name + " (" + bundleId + ")";
    }
}
