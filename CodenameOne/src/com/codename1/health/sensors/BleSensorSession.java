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

import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattNotificationListener;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.health.HealthError;
import com.codename1.health.HealthException;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncResult;

import java.util.List;

/// The GATT transport behind [SensorSession]: connects, discovers the
/// profile's service, subscribes to its measurement characteristic and
/// feeds raw values into the shared decoder.
///
/// Package-private -- apps see only [SensorSession].
final class BleSensorSession extends SensorSession {

    /// Battery Service, `0x180F`, and its Battery Level characteristic.
    private static final BluetoothUuid BATTERY_SERVICE =
            BluetoothUuid.fromShort(0x180F);
    private static final BluetoothUuid BATTERY_LEVEL =
            BluetoothUuid.fromShort(0x2A19);

    /// Body Sensor Location, `0x2A38`, on the Heart Rate service.
    private static final BluetoothUuid BODY_SENSOR_LOCATION =
            BluetoothUuid.fromShort(0x2A38);

    /// Heart Rate Control Point, `0x2A39`. Writing `0x01` resets the
    /// accumulated energy-expended counter.
    private static final BluetoothUuid HEART_RATE_CONTROL_POINT =
            BluetoothUuid.fromShort(0x2A39);

    private final BlePeripheral peripheral;
    private GattCharacteristic measurement;

    BleSensorSession(String sensorId, HealthSensorProfile profile,
            SensorSessionOptions options, BlePeripheral peripheral) {
        super(sensorId, profile, options);
        this.peripheral = peripheral;
    }

    /// Connects, discovers and subscribes, resolving `out` once
    /// measurements can start arriving.
    void start(final AsyncResource<SensorSession> out) {
        setState(SensorSessionState.CONNECTING);
        peripheral.connect().onResult(new AsyncResult<BlePeripheral>() {
            @Override
            public void onReady(BlePeripheral value, Throwable err) {
                if (err != null) {
                    failStart(out, err);
                    return;
                }
                discover(out);
            }
        });
    }

    private void discover(final AsyncResource<SensorSession> out) {
        peripheral.discoverServices().onResult(
                new AsyncResult<List<GattService>>() {
            @Override
            public void onReady(List<GattService> value, Throwable err) {
                if (err != null) {
                    failStart(out, err);
                    return;
                }
                GattService service =
                        peripheral.getService(getProfile().getServiceUuid());
                if (service == null) {
                    failStart(out, new HealthException(
                            HealthError.TYPE_NOT_SUPPORTED,
                            "this device does not expose the "
                                    + getProfile().getName() + " service"));
                    return;
                }
                measurement = service.getCharacteristic(
                        getProfile().getMeasurementUuid());
                if (measurement == null) {
                    failStart(out, new HealthException(
                            HealthError.TYPE_NOT_SUPPORTED,
                            "this device exposes the "
                                    + getProfile().getName()
                                    + " service but not its measurement"
                                    + " characteristic"));
                    return;
                }
                readOptionalMetadata(service);
                subscribe(out);
            }
        });
    }

    /// Reads the extras a device may or may not expose. Failures here are
    /// deliberately ignored: a strap without a battery service is still a
    /// perfectly good strap, and refusing to stream over a missing
    /// optional characteristic would be worse than not knowing the
    /// battery level.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private void readOptionalMetadata(GattService service) {
        GattService battery = peripheral.getService(BATTERY_SERVICE);
        if (battery != null) {
            GattCharacteristic level =
                    battery.getCharacteristic(BATTERY_LEVEL);
            if (level != null) {
                peripheral.readCharacteristic(level).onResult(
                        new AsyncResult<byte[]>() {
                    @Override
                    public void onReady(byte[] value, Throwable err) {
                        if (err == null && value != null
                                && value.length > 0) {
                            setBatteryPercent(
                                    Integer.valueOf(value[0] & 0xFF));
                        }
                    }
                });
            }
        }
        if (getProfile() == HealthSensorProfile.HEART_RATE) {
            GattCharacteristic loc =
                    service.getCharacteristic(BODY_SENSOR_LOCATION);
            if (loc != null) {
                peripheral.readCharacteristic(loc).onResult(
                        new AsyncResult<byte[]>() {
                    @Override
                    public void onReady(byte[] value, Throwable err) {
                        if (err == null && value != null
                                && value.length > 0) {
                            setBodySensorLocation(value[0] & 0xFF);
                        }
                    }
                });
            }
        }
    }

    private void subscribe(final AsyncResource<SensorSession> out) {
        // The Bluetooth layer picks notify or indicate from the
        // characteristic's own properties, which matters here: blood
        // pressure, weight and temperature are indicate-only.
        peripheral.subscribe(measurement, new GattNotificationListener() {
            public void valueChanged(GattCharacteristic characteristic,
                    byte[] value) {
                onMeasurement(value, System.currentTimeMillis());
            }
        }).onResult(new AsyncResult<Boolean>() {
            public void onReady(Boolean value, Throwable err) {
                if (err != null) {
                    failStart(out, err);
                    return;
                }
                setState(SensorSessionState.STREAMING);
                out.complete(BleSensorSession.this);
            }
        });
    }

    private void failStart(AsyncResource<SensorSession> out, Throwable err) {
        setState(SensorSessionState.FAILED);
        out.error(err instanceof HealthException ? err
                : new HealthException(HealthError.SENSOR_DISCONNECTED,
                        "could not start the " + getProfile().getName()
                                + " session", err));
    }

    @Override
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public AsyncResource<Boolean> resetEnergyExpended() {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (getProfile() != HealthSensorProfile.HEART_RATE) {
            out.error(new HealthException(HealthError.NOT_SUPPORTED,
                    "energy expended applies to the heart rate profile"));
            return out;
        }
        GattService service =
                peripheral.getService(getProfile().getServiceUuid());
        GattCharacteristic cp = service == null ? null
                : service.getCharacteristic(HEART_RATE_CONTROL_POINT);
        if (cp == null) {
            out.error(new HealthException(HealthError.NOT_SUPPORTED,
                    "this strap does not expose the heart rate control"
                            + " point"));
            return out;
        }
        // The control point is a reliable write: the strap acknowledges
        // it, and a silently dropped reset would leave the energy counter
        // accumulating across what the user thinks are separate workouts.
        return peripheral.writeCharacteristic(cp, new byte[] { 0x01 }, true);
    }

    @Override
    public void stop() {
        if (getState() == SensorSessionState.STOPPED) {
            return;
        }
        setState(SensorSessionState.STOPPED);
        peripheral.disconnect();
    }
}
