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
package com.codename1.bluetooth.classic;

import com.codename1.util.AsyncResource;

/// Live handle of a running classic discovery (inquiry scan, ~12 seconds)
/// returned by
/// [BluetoothClassic#startDiscovery(ClassicDiscoveryListener)].
///
/// As an `AsyncResource<Boolean>` the handle resolves when discovery
/// *ends*: with `true` when the inquiry finished (naturally or via
/// [#stop()]), or with a
/// [com.codename1.bluetooth.BluetoothException] when the OS aborted it.
/// `cancel()` is equivalent to [#stop()].
public class ClassicDiscovery extends AsyncResource<Boolean> {

    /// Created by [BluetoothClassic]; application code receives instances
    /// from `startDiscovery`.
    protected ClassicDiscovery() {
    }

    /// Aborts the inquiry; the handle resolves with `true`. A no-op when
    /// discovery already ended.
    public void stop() {
        if (isDone()) {
            return;
        }
        onStop();
        if (!isDone()) {
            complete(Boolean.TRUE);
        }
    }

    /// `true` while the inquiry is running.
    public boolean isActive() {
        return !isDone();
    }

    /// Equivalent to [#stop()].
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone()) {
            return false;
        }
        stop();
        return true;
    }

    /// Hook for the port to abort the platform inquiry; invoked exactly
    /// once from [#stop()].
    protected void onStop() {
    }
}
