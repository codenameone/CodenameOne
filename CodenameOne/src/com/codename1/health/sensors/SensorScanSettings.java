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

/// Which sensors to look for and for how long.
public final class SensorScanSettings {

    private final List<HealthSensorProfile> profiles =
            new ArrayList<HealthSensorProfile>();
    private int timeoutMillis = 15000;
    private boolean lowPower;

    /// Looks for devices advertising this profile. Add several to scan for
    /// more than one kind at once; adding none scans for every profile
    /// this framework understands.
    public SensorScanSettings addProfile(HealthSensorProfile profile) {
        if (profile != null && !profiles.contains(profile)) {
            profiles.add(profile);
        }
        return this;
    }

    /// The profiles being scanned for, empty meaning all of them.
    public List<HealthSensorProfile> getProfiles() {
        return Collections.unmodifiableList(profiles);
    }

    /// Stops the scan after this many milliseconds. Defaults to 15
    /// seconds.
    ///
    /// A bounded scan is not a convenience: BLE scanning is one of the
    /// most expensive things an app can do to a phone's battery, and both
    /// platforms throttle or silently stop an app that scans
    /// indefinitely.

    /// How long to scan before giving up.
    ///
    /// The [Duration] form of [#setTimeoutMillis(int)], which is the type the rest of
    /// the framework speaks; the millis form stays for the ports and the
    /// wire format.
    public SensorScanSettings setTimeout(java.time.Duration value) {
        if (value == null) {
            throw new IllegalArgumentException("a duration is required");
        }
        // Range-checked before the narrowing. A duration beyond
        // Integer.MAX_VALUE milliseconds -- about 24.9 days -- wrapped
        // negative, and a negative here does not mean "very long": the scan
        // timeout reads it as disabled and leaves an expensive BLE scan
        // running, and the batch and staleness windows read it as zero and
        // flush every sample or treat every cached one as stale.
        if (value.toMillis() > Integer.MAX_VALUE || value.toMillis() < 0) {
            throw new IllegalArgumentException(
                    "a duration here must be between zero and "
                            + Integer.MAX_VALUE + " milliseconds, got "
                            + value.toMillis());
        }
        return setTimeoutMillis((int) value.toMillis());
    }

    /// How long to scan before giving up, as a [Duration].
    public java.time.Duration getTimeout() {
        return java.time.Duration.ofMillis(getTimeoutMillis());
    }

    public SensorScanSettings setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    /// The scan timeout in milliseconds.
    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    /// Scans at a lower duty cycle, trading discovery latency for battery.
    /// Worth setting when scanning in the background or for a long time.
    public SensorScanSettings setLowPower(boolean lowPower) {
        this.lowPower = lowPower;
        return this;
    }

    /// `true` when a low-power scan was requested.
    public boolean isLowPower() {
        return lowPower;
    }
}
