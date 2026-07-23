/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.bluetooth.le;

import java.util.ArrayList;
import java.util.List;

/// Options for a BLE scan started via
/// [BluetoothLE#startScan(ScanSettings, ScanListener)]. Fluent setters
/// return `this` for chaining.
public class ScanSettings {

    private ScanMode scanMode = ScanMode.BALANCED;
    private boolean allowDuplicates;
    private ArrayList<ScanFilter> filters;

    /// The power/latency trade-off; defaults to [ScanMode#BALANCED].
    public ScanSettings setScanMode(ScanMode mode) {
        this.scanMode = mode == null ? ScanMode.BALANCED : mode;
        return this;
    }

    /// When `false` (the default) each device is reported once per scan;
    /// when `true` every advertisement sighting is reported -- required
    /// for RSSI tracking and beacon monitoring.
    public ScanSettings setAllowDuplicates(boolean allow) {
        this.allowDuplicates = allow;
        return this;
    }

    /// Adds a filter; multiple filters are OR-combined. A scan without
    /// filters reports every advertising device.
    public ScanSettings addFilter(ScanFilter filter) {
        if (filter != null) {
            if (filters == null) {
                filters = new ArrayList<ScanFilter>();
            }
            filters.add(filter);
        }
        return this;
    }

    /// The configured scan mode.
    public ScanMode getScanMode() {
        return scanMode;
    }

    /// Whether duplicate sightings are reported.
    public boolean isAllowDuplicates() {
        return allowDuplicates;
    }

    /// The filters added via [#addFilter(ScanFilter)]; empty means
    /// match-all.
    public List<ScanFilter> getFilters() {
        return filters == null
                ? new ArrayList<ScanFilter>() : new ArrayList<ScanFilter>(filters);
    }

    /// `true` when the given result passes this settings object's filter
    /// set (no filters == match all). Used by the core scan
    /// demultiplexer.
    public boolean matches(ScanResult result) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        int size = filters.size();
        for (int i = 0; i < size; i++) {
            if (filters.get(i).matches(result)) {
                return true;
            }
        }
        return false;
    }
}
