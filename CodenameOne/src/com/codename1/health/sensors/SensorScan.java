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

import com.codename1.bluetooth.le.BleScan;

/// A running sensor scan. Stop it as soon as the user has chosen a device
/// -- scanning is expensive and both platforms throttle apps that leave it
/// running.
public final class SensorScan {

    private final BleScan delegate;
    private boolean stopped;

    /// Wraps an underlying BLE scan. Created by [HealthSensors].
    SensorScan(BleScan delegate) {
        this.delegate = delegate;
    }

    /// Stops the scan. Idempotent.
    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        if (delegate != null) {
            delegate.stop();
        }
    }

    /// `true` until [#stop()] is called or the scan times out.
    public boolean isScanning() {
        return !stopped && delegate != null && delegate.isActive();
    }
}
