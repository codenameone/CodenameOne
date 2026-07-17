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
package com.codename1.bluetooth;

import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;

/// Entry point for the Codename One Bluetooth API -- adapter state, runtime
/// permissions and the capability queries that let cross-platform code
/// branch cleanly. Obtain the platform implementation via [#getInstance()];
/// the returned subclass is owned by the active port and never `null`.
///
/// The API is split by role:
/// - `com.codename1.bluetooth.le` -- BLE central: scanning, connections and
///   the GATT client, plus L2CAP channels.
/// - `com.codename1.bluetooth.le.server` -- BLE peripheral: advertising and
///   the local GATT server.
/// - `com.codename1.bluetooth.classic` -- classic Bluetooth: discovery and
///   RFCOMM stream connections (Android and the simulator only).
///
/// #### Quick start: check support and permissions
///
/// ```java
/// Bluetooth bt = Bluetooth.getInstance();
/// if (!bt.isLeSupported()) {
///     // hide the feature
///     return;
/// }
/// bt.requestPermissions(BluetoothPermission.SCAN, BluetoothPermission.CONNECT)
///   .onResult((granted, err) -> {
///        if (err == null && granted) {
///            // start scanning...
///        }
///   });
/// ```
///
/// #### Threading
///
/// Every callback of the Bluetooth API -- `AsyncResource` results, scan
/// results, connection events, notifications, adapter state changes -- is
/// delivered on the EDT. The only exceptions are the blocking
/// `InputStream`/`OutputStream` pairs of RFCOMM and L2CAP connections,
/// which must be consumed off the EDT.
///
/// #### Platform support
///
/// - **Android** -- full stack: BLE central + peripheral, L2CAP (API 29+),
///   classic RFCOMM. Permissions and manifest entries are auto-injected at
///   build time when these packages are referenced.
/// - **iOS / Mac Catalyst** -- CoreBluetooth: BLE central + peripheral and
///   L2CAP. Classic RFCOMM is not exposed by iOS. The `NSBluetooth*` plist
///   entries and CoreBluetooth framework are auto-injected at build time.
/// - **JavaSE simulator** -- a scriptable virtual Bluetooth stack with a
///   full feature matrix (Simulate -> Bluetooth Simulation), plus an
///   optional native backend that talks to the host machine's real
///   Bluetooth hardware (BLE central only).
/// - **JavaScript** -- Web Bluetooth where the browser supports it: central
///   role via the browser's device chooser, GATT client and notifications.
/// - **All other ports** -- this base class is returned as-is and reports
///   Bluetooth as unsupported; every operation fails fast with
///   [BluetoothError#NOT_SUPPORTED].
public class Bluetooth {

    private ArrayList<AdapterStateListener> adapterListeners;

    /// Ports construct subclasses. Application code obtains the active
    /// instance via [#getInstance()].
    protected Bluetooth() {
    }

    /// Returns the platform-specific singleton owned by the current port.
    /// On ports that do not implement Bluetooth this returns a base
    /// [Bluetooth] instance that reports the feature as unsupported;
    /// calling code never needs a `null` check or a platform-specific
    /// `if`.
    public static Bluetooth getInstance() {
        Bluetooth b = Display.getInstance().getBluetooth();
        return b != null ? b : DEFAULT;
    }

    private static final Bluetooth DEFAULT = new Bluetooth();

    /// `true` when this port/device exposes any Bluetooth functionality at
    /// all. Returns `false` on the fallback base class.
    public boolean isSupported() {
        return false;
    }

    /// `true` when the BLE central role (scanning, connections, GATT
    /// client) is available. Defaults to `false`.
    public boolean isLeSupported() {
        return false;
    }

    /// `true` when classic Bluetooth (discovery, RFCOMM) is available.
    /// Always `false` on iOS, which does not expose classic Bluetooth to
    /// apps. Defaults to `false`.
    public boolean isClassicSupported() {
        return false;
    }

    /// `true` when the BLE peripheral role (advertising and a local GATT
    /// server) is available. `false` on tvOS/watchOS and on the JavaScript
    /// port. Defaults to `false`.
    public boolean isPeripheralModeSupported() {
        return false;
    }

    /// `true` when L2CAP connection-oriented channels are available
    /// (Android 10+, iOS 11+). Defaults to `false`.
    public boolean isL2capSupported() {
        return false;
    }

    /// The current state of the local adapter. The fallback base class
    /// reports [AdapterState#UNSUPPORTED].
    public AdapterState getAdapterState() {
        return AdapterState.UNSUPPORTED;
    }

    /// Convenience for `getAdapterState() == AdapterState.POWERED_ON`.
    public boolean isEnabled() {
        return getAdapterState() == AdapterState.POWERED_ON;
    }

    /// Registers a listener notified on the EDT whenever the adapter state
    /// changes. Listeners stay registered until removed via
    /// [#removeAdapterStateListener(AdapterStateListener)].
    public void addAdapterStateListener(AdapterStateListener l) {
        if (l == null) {
            return;
        }
        synchronized (this) {
            if (adapterListeners == null) {
                adapterListeners = new ArrayList<AdapterStateListener>();
            }
            if (!adapterListeners.contains(l)) {
                adapterListeners.add(l);
            }
        }
    }

    /// Removes a listener previously added via
    /// [#addAdapterStateListener(AdapterStateListener)].
    public void removeAdapterStateListener(AdapterStateListener l) {
        synchronized (this) {
            if (adapterListeners != null) {
                adapterListeners.remove(l);
            }
        }
    }

    /// Called by ports (from any thread) when the adapter transitions to a
    /// new state; dispatches to the registered listeners on the EDT.
    protected final void fireAdapterStateChanged(final AdapterState newState) {
        final Object[] snapshot;
        synchronized (this) {
            if (adapterListeners == null || adapterListeners.isEmpty()) {
                return;
            }
            snapshot = adapterListeners.toArray();
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                for (int i = 0; i < snapshot.length; i++) {
                    ((AdapterStateListener) snapshot[i])
                            .adapterStateChanged(newState);
                }
            }
        });
    }

    /// Asks the user to enable the Bluetooth adapter where the platform
    /// supports it (the Android system dialog). Resolves `true` when the
    /// adapter was enabled, `false` when the user declined or the platform
    /// offers no programmatic enable flow (iOS, and this fallback base
    /// class). Never fails just because the feature is unsupported.
    public AsyncResource<Boolean> requestEnable() {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.complete(Boolean.FALSE);
        return r;
    }

    /// The BLE central role entry point -- scanning, connections, GATT
    /// client. Never `null`: on ports without BLE a no-op instance is
    /// returned whose operations fail fast with
    /// [BluetoothError#NOT_SUPPORTED]; branch via [#isLeSupported()].
    public com.codename1.bluetooth.le.BluetoothLE getLE() {
        return DefaultLE.INSTANCE;
    }

    /// Lazy holder so the fallback LE instance is only created when a
    /// port without BLE support is actually asked for it.
    private static final class DefaultLE {
        static final com.codename1.bluetooth.le.BluetoothLE INSTANCE =
                new com.codename1.bluetooth.le.BluetoothLE() {
                };

        private DefaultLE() {
        }
    }

    /// `true` when the given runtime permission is currently granted. See
    /// [BluetoothPermission] for the per-platform mapping. Defaults to
    /// `false`.
    public boolean hasPermission(BluetoothPermission permission) {
        return false;
    }

    /// Requests the given runtime permissions, prompting the user where
    /// needed. Resolves `true` when every requested permission is granted.
    /// Resolves `false` (rather than failing) when denied or when the
    /// platform has no Bluetooth support at all.
    public AsyncResource<Boolean> requestPermissions(
            BluetoothPermission... permissions) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.complete(Boolean.FALSE);
        return r;
    }
}
