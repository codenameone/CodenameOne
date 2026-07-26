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

import com.codename1.bluetooth.Bluetooth;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.BleScan;
import com.codename1.bluetooth.le.ScanFilter;
import com.codename1.bluetooth.le.ScanListener;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.bluetooth.le.ScanSettings;
import com.codename1.health.HealthError;
import com.codename1.health.HealthException;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Discovers and streams from standard Bluetooth SIG health sensors --
/// heart-rate straps, power meters, speed and cadence sensors, foot pods,
/// thermometers, scales, blood-pressure cuffs and glucose meters.
///
/// Obtain one from [com.codename1.health.Health#getSensors()].
///
/// #### This works everywhere Bluetooth LE does
///
/// Unlike the rest of the health API, this layer needs no platform health
/// store and no per-port implementation: it is built entirely on
/// `com.codename1.bluetooth.le`, so it behaves identically on iOS,
/// Android, the simulator, the desktop ports and -- where the browser
/// exposes Web Bluetooth -- JavaScript. A desktop app that reports
/// [com.codename1.health.HealthAvailability#LOCAL_ONLY] for the store can
/// still stream a chest strap.
///
/// #### Quick start
///
/// ```java
/// HealthSensors sensors = Health.getInstance().getSensors();
/// SensorScanSettings settings = new SensorScanSettings()
///         .addProfile(HealthSensorProfile.HEART_RATE)
///         .setTimeoutMillis(15000);
/// SensorScan scan = sensors.startScan(settings, new SensorDiscoveryListener() {
///     public void sensorDiscovered(HealthSensor sensor) {
///         scan.stop();
///         sensors.connect(sensor, HealthSensorProfile.HEART_RATE,
///                 new SensorSessionOptions())
///               .onResult((session, err) -> { ... });
///     }
///     public void scanFailed(HealthException e) { Log.e(e); }
/// });
/// ```
///
/// #### Permissions
///
/// These are Bluetooth operations, not health-store operations, so they
/// need Bluetooth permission -- `BluetoothPermission.SCAN` and
/// `CONNECT` -- and **not** a HealthKit entitlement or a Health Connect
/// declaration. The build server makes the same distinction: an app that
/// only uses this package is not treated as a health-data app.
public class HealthSensors {

    private final List<SensorSession> active = new ArrayList<SensorSession>();

    /// `true` when this device can talk to BLE sensors at all -- that is,
    /// when Bluetooth LE is supported by the port and the hardware.
    public boolean isSupported() {
        return Bluetooth.getInstance().isLeSupported();
    }

    /// Scans for sensors matching `settings`, reporting each discovery to
    /// `listener`.
    ///
    /// Returns a handle even when scanning cannot start; the failure
    /// arrives through [SensorDiscoveryListener#scanFailed(HealthException)]
    /// rather than as a null return, so callers never need a null check.
    public final SensorScan startScan(SensorScanSettings settings,
            final SensorDiscoveryListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("startScan needs a listener");
        }
        if (!isSupported()) {
            notifyScanFailed(listener, new HealthException(
                    HealthError.NOT_SUPPORTED,
                    "Bluetooth LE is not available on this device"));
            return new SensorScan(null);
        }
        SensorScanSettings s = settings == null
                ? new SensorScanSettings() : settings;
        final List<HealthSensorProfile> wanted = s.getProfiles().isEmpty()
                ? HealthSensorProfile.values() : s.getProfiles();

        ScanSettings bleSettings = new ScanSettings();
        for (HealthSensorProfile wantedItem : wanted) {
            bleSettings.addFilter(new ScanFilter()
                    .setServiceUuid(wantedItem.getServiceUuid()));
        }
        BleScan scan = Bluetooth.getInstance().getLE().startScan(bleSettings,
                makeScanListener(wanted, listener));
        SensorScan out = new SensorScan(scan);
        out.scheduleTimeout(s.getTimeoutMillis());
        return out;
    }

    /// Built in a static method so the listener carries no synthetic
    /// reference to this object (SpotBugs
    /// `SIC_INNER_SHOULD_BE_STATIC_ANON`).
    private static ScanListener makeScanListener(
            final List<HealthSensorProfile> wanted,
            final SensorDiscoveryListener listener) {
        return new ScanListener() {
            @Override
            public void peripheralDiscovered(ScanResult result) {
                HealthSensor sensor = toSensor(result, wanted);
                if (sensor != null) {
                    listener.sensorDiscovered(sensor);
                }
            }
        };
    }

    /// Which of the wanted profiles a discovered device actually
    /// advertises. Returns null when it advertises none of them, which
    /// happens when a platform scan filter is coarser than requested.
    private static HealthSensor toSensor(ScanResult result,
            List<HealthSensorProfile> wanted) {
        if (result == null || result.getPeripheral() == null) {
            return null;
        }
        List<BluetoothUuid> advertised = result.getAdvertisementData() == null
                ? Collections.<BluetoothUuid>emptyList()
                : result.getAdvertisementData().getServiceUuids();
        List<HealthSensorProfile> matched =
                new ArrayList<HealthSensorProfile>();
        for (HealthSensorProfile p : wanted) {
            if (advertised.contains(p.getServiceUuid())) {
                matched.add(p);
            }
        }
        if (matched.isEmpty()) {
            return null;
        }
        BlePeripheral peripheral = result.getPeripheral();
        return new HealthSensor(peripheral.getAddress(), peripheral.getName(),
                matched, result.getRssi());
    }

    /// Connects to a discovered sensor and starts streaming.
    public final AsyncResource<SensorSession> connect(HealthSensor sensor,
            HealthSensorProfile profile, SensorSessionOptions options) {
        if (sensor == null) {
            AsyncResource<SensorSession> out =
                    new AsyncResource<SensorSession>();
            out.error(new HealthException(HealthError.INVALID_ARGUMENT,
                    "connect needs a sensor"));
            return out;
        }
        return connect(sensor.getId(), profile, options);
    }

    /// Reconnects to a sensor by the identifier persisted from an earlier
    /// session, without scanning first.
    ///
    /// This is how a fitness app should reconnect to the user's own strap:
    /// remember [HealthSensor#getId()] the first time and go straight to
    /// it afterwards, rather than making them pick from a list every
    /// session.
    public final AsyncResource<SensorSession> connect(final String sensorId,
            final HealthSensorProfile profile,
            final SensorSessionOptions options) {
        final AsyncResource<SensorSession> out =
                new AsyncResource<SensorSession>();
        if (sensorId == null || profile == null) {
            out.error(new HealthException(HealthError.INVALID_ARGUMENT,
                    "connect needs a sensor id and a profile"));
            return out;
        }
        if (!isSupported()) {
            out.error(new HealthException(HealthError.NOT_SUPPORTED,
                    "Bluetooth LE is not available on this device"));
            return out;
        }
        BlePeripheral peripheral =
                Bluetooth.getInstance().getLE().getPeripheral(sensorId);
        if (peripheral == null) {
            out.error(new HealthException(HealthError.SENSOR_DISCONNECTED,
                    "no sensor known with id " + sensorId));
            return out;
        }
        BleSensorSession session = new BleSensorSession(sensorId, profile,
                options, peripheral);
        synchronized (active) {
            active.add(session);
        }
        session.start(out);
        return out;
    }

    /// Every session currently connected or reconnecting.
    public final List<SensorSession> getActiveSessions() {
        synchronized (active) {
            return new ArrayList<SensorSession>(active);
        }
    }

    /// Forgets a session that has stopped. Called by the session itself.
    final void sessionEnded(SensorSession session) {
        synchronized (active) {
            active.remove(session);
        }
    }

    /// Built in a static method so the `Runnable` carries no synthetic
    /// reference to this object (SpotBugs
    /// `SIC_INNER_SHOULD_BE_STATIC_ANON`).
    private static void notifyScanFailed(
            final SensorDiscoveryListener listener,
            final HealthException error) {
        com.codename1.ui.Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                listener.scanFailed(error);
            }
        });
    }
}
