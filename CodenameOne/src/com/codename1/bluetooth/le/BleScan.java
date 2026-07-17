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

import com.codename1.util.AsyncResource;

/// Live handle of a running BLE scan returned by
/// [BluetoothLE#startScan(ScanSettings, ScanListener)]. Any number of
/// scans may run concurrently -- each handle only sees advertisements
/// passing its own filters.
///
/// As an `AsyncResource<Boolean>` the handle resolves when the scan
/// *ends*: with `true` after a normal [#stop()], or with a
/// [com.codename1.bluetooth.BluetoothException] when the OS aborted the
/// scan. `cancel()` is equivalent to [#stop()].
public class BleScan extends AsyncResource<Boolean> {

    /// Created by [BluetoothLE]; application code receives instances from
    /// `startScan`.
    protected BleScan() {
    }

    /// Stops this scan. The handle resolves with `true`; the underlying
    /// platform scan keeps running while other handles remain active.
    /// Calling `stop()` on an already ended scan is a no-op.
    public void stop() {
        if (isDone()) {
            return;
        }
        onStop();
        if (!isDone()) {
            complete(Boolean.TRUE);
        }
    }

    /// `true` while the scan is running.
    public boolean isActive() {
        return !isDone();
    }

    /// Equivalent to [#stop()].
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone()) {
            return false;
        }
        stop();
        return true;
    }

    /// Hook for [BluetoothLE] to unregister the handle and stop the
    /// platform scan when it was the last one; invoked exactly once from
    /// [#stop()].
    protected void onStop() {
    }
}
