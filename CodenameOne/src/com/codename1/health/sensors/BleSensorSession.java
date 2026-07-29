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
import com.codename1.bluetooth.le.ConnectionEvent;
import com.codename1.bluetooth.le.ConnectionListener;
import com.codename1.bluetooth.le.ConnectionState;
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

    /// How many times the reconnect ladder may stumble on discovery or
    /// subscription before the session is retired. Bounded so a device
    /// that has genuinely changed does not get reconnected at forever;
    /// more than one so a single transient failure does not end a session
    /// the caller still wants.
    private static final int MAX_RECONNECT_FAILURES = 3;

    private final BlePeripheral peripheral;
    private GattCharacteristic measurement;
    /// Read and written from connection callbacks and from stop(), which
    /// are not the same thread. Guarded by [#reconnectLock], like the
    /// count below -- a lock rather than `volatile`, which this codebase
    /// does not use.
    private Reconnector reconnectListener;

    /// Guards the failure count. Volatile is not enough for it: the
    /// increment and the test against the limit are one decision, and
    /// two callbacks racing through a volatile `++` can lose a count and
    /// let the ladder run past the bound it exists to enforce.
    private final Object reconnectLock = new Object();
    /// Guarded by [#reconnectLock].
    private int reconnectFailures;

    BleSensorSession(String sensorId, HealthSensorProfile profile,
            SensorSessionOptions options, BlePeripheral peripheral) {
        super(sensorId, profile, options);
        this.peripheral = peripheral;
    }

    /// Connects, discovers and subscribes, resolving `out` once
    /// measurements can start arriving.
    void start(final AsyncResource<SensorSession> out) {
        if (!setState(SensorSessionState.CONNECTING)) {
            // stop() won between the check above and this line. The
            // session is already disconnected and off the registry, so
            // connecting the peripheral now would leave a stopped
            // session holding a live link that nothing will close.
            return;
        }
        boolean needsListener;
        synchronized (reconnectLock) {
            needsListener = getOptions().isAutoReconnect()
                    && reconnectListener == null;
        }
        if (needsListener) {
            // A strap that walks out of range mid-workout otherwise leaves
            // the session marked STREAMING forever, receiving nothing and
            // never retrying, which is the opposite of what the default
            // promises.
            Reconnector listener = new Reconnector(this);
            synchronized (reconnectLock) {
                reconnectListener = listener;
            }
            peripheral.addConnectionListener(listener);
        }
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

    /// Re-runs discovery and subscription after an unexpected drop.
    void reconnect() {
        // Either terminal state. A DISCONNECTED listener runs on the EDT
        // and can pass its own state check while a connect, discovery or
        // subscribe callback on another thread is exhausting the retry
        // budget -- so a stopped-only guard moved a session the ladder
        // had already retired as FAILED back to CONNECTING, and
        // reconnected a handle the manager no longer holds.
        if (isTerminal() || !getOptions().isAutoReconnect()) {
            return;
        }
        setState(SensorSessionState.CONNECTING);
        // The cumulative trackers hold the previous connection's baseline.
        // A sensor that power-cycled while away restarts its counters at
        // zero, and even one that did not will have wrapped its 16-bit
        // event timer several times across the gap, so the first delta
        // after reconnecting would publish a large fabricated cadence into
        // the live workout and into anything persisted from it.
        resetCounters();
        peripheral.connect().onResult(new AsyncResult<BlePeripheral>() {
            @Override
            public void onReady(BlePeripheral value, Throwable err) {
                if (err == null) {
                    discover(null);
                    return;
                }
                // Counted, not ignored. A failed connect publishes
                // DISCONNECTED, the listener fires on that transition and
                // reconnects immediately, so a sensor that has gone for
                // good span the whole ladder at full speed and never
                // reached the three-failure limit -- which only discovery
                // and subscribe failures were incrementing.
                failReconnect(wrapStartFailure(err));
            }
        });
    }

    /// Watches for the link dropping while the session is still wanted.
    ///
    /// Named rather than anonymous so it can be removed again on stop and
    /// so SpotBugs sees no synthetic outer reference.
    private static final class Reconnector implements ConnectionListener {
        private final BleSensorSession session;

        Reconnector(BleSensorSession session) {
            this.session = session;
        }

        @Override
        public void connectionStateChanged(ConnectionEvent event) {
            // Only a session that actually got streaming reconnects. A
            // failed initial connect publishes DISCONNECTED too, and
            // retrying from there resurrected a session the caller had
            // already been told had failed -- streaming and routing
            // samples while absent from getActiveSessions().
            if (event.getState() == ConnectionState.DISCONNECTED
                    && (session.getState() == SensorSessionState.STREAMING
                        || session.getState()
                            == SensorSessionState.CONNECTING)
                    && session.hasStreamed()) {
                session.reconnect();
            }
        }
    }

    private void discover(final AsyncResource<SensorSession> out) {
        peripheral.discoverServices().onResult(
                new AsyncResult<List<GattService>>() {
                    @Override
                    public void onReady(List<GattService> value,
                            Throwable err) {
                        if (err != null) {
                            failStart(out, err);
                            return;
                        }
                        GattService service = peripheral.getService(
                                getProfile().getServiceUuid());
                        if (service == null) {
                            failStart(out, new HealthException(
                                    HealthError.TYPE_NOT_SUPPORTED,
                                    "this device does not expose the "
                                            + getProfile().getName()
                                            + " service"));
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
                                    // 0 to 100, and nothing else. The
                                    // Battery Level characteristic
                                    // reserves everything above 100, and
                                    // a peripheral that answers 0xFF for
                                    // "unknown" was reported to the app
                                    // as a 255% battery. Left unset
                                    // instead, which getBatteryPercent
                                    // already documents as null when the
                                    // device does not report one.
                                    int percent = value[0] & 0xFF;
                                    if (percent <= 100) {
                                        setBatteryPercent(
                                                Integer.valueOf(percent));
                                    }
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
        final GattNotificationListener listener =
                new GattNotificationListener() {
                    @Override
                    public void valueChanged(
                            GattCharacteristic characteristic,
                            byte[] value) {
                        // A notification can still arrive between stop()
                        // and the CCCD actually being disarmed. Dropping
                        // it here keeps a finished session from
                        // delivering one more reading.
                        if (isStopped()) {
                            return;
                        }
                        onMeasurement(value, System.currentTimeMillis());
                    }
                };
        peripheral.subscribe(measurement, listener)
                .onResult(new AsyncResult<Boolean>() {
                    @Override
                    public void onReady(Boolean value, Throwable err) {
                        // Stopping does not cancel a subscribe already in
                        // flight, and the reconnect path issues one
                        // without anybody waiting on it. A completion
                        // arriving after stop() used to move the session
                        // from STOPPED back to STREAMING -- already
                        // unregistered from the manager and disconnected,
                        // yet reporting itself live and delivering
                        // notifications. Stopped is terminal.
                        if (isStopped()) {
                            unsubscribeLate(listener);
                            return;
                        }
                        if (err != null) {
                            failStart(out, err);
                            return;
                        }
                        setState(SensorSessionState.STREAMING);
                        // A run of failures only retires the session when
                        // it is uninterrupted: a strap that reconnects
                        // cleanly has spent whatever budget it used
                        // getting there.
                        synchronized (reconnectLock) {
                            reconnectFailures = 0;
                        }
                        if (out != null) {
                            out.complete(BleSensorSession.this);
                        }
                    }
                });
    }

    /// Whether this session has ended, however it ended.
    ///
    /// Both terminal states, not STOPPED alone. The reconnect ladder
    /// retires a session as FAILED, and an indication the transport had
    /// already queued then passed a stopped-only check: it reached the
    /// listeners after the session was over, and with write-through on it
    /// landed in a buffer whose final flush had already run and whose
    /// timer is disarmed -- so it could never be persisted either.
    private boolean isStopped() {
        return isTerminal();
    }

    /// Tears down a subscription that completed after the session stopped.
    ///
    /// Best effort: the peripheral is normally already disconnected by
    /// then, in which case this fails harmlessly. Leaving it is the worse
    /// option -- a live characteristic notification on a session the app
    /// has finished with.
    private void unsubscribeLate(GattNotificationListener listener) {
        try {
            peripheral.unsubscribe(measurement, listener);
        } catch (Throwable t) {
            com.codename1.io.Log.p("[health] late unsubscribe after stop: "
                    + t);
        }
    }

    /// Reports a failed start.
    ///
    /// `out` is null on the reconnect path: nobody is waiting on a
    /// resource that was already resolved when the session first started,
    /// and completing it a second time -- or dereferencing it -- would
    /// throw on the BLE callback thread.
    private void failStart(AsyncResource<SensorSession> out, Throwable err) {
        HealthException wrapped = wrapStartFailure(err);
        if (out == null && !isStopped()) {
            // No caller waiting means this came from the reconnect ladder,
            // and a session that has streamed before is one the caller
            // still wants. Failing it here retired the whole session over
            // one transient discovery or subscribe error: FAILED is not a
            // state the reconnect listener retries from, and the session
            // had already been dropped from the active registry, so the
            // documented auto-reconnect stopped for good after a single
            // stumble.
            failReconnect(wrapped);
            return;
        }
        endSession();
        if (out != null) {
            out.error(wrapped);
        } else {
            fireError(wrapped);
        }
    }

    /// Terminal teardown for a session that has failed.
    ///
    /// Every step matters and each was missing at some point. The listener
    /// goes first so the disconnect below is not mistaken for a dropped
    /// link and retried. The link is dropped because a failure after a
    /// successful connect -- discovery or subscribe, on the first attempt
    /// or the last reconnect -- otherwise left a live, unusable connection
    /// and a registered listener behind a handle the caller had been told
    /// was finished, with nothing but an explicit `stop()` to reclaim
    /// them.
    private void endSession() {
        if (isTerminal()) {
            // Two paths reach here -- a discovery or subscribe failure,
            // and the reconnect ladder giving up -- and a session tears
            // down once. The second pass used to flush again, which is
            // how a buffer refilled by a failed final write went out a
            // second time.
            return;
        }
        Reconnector listener = takeReconnectListener();
        if (listener != null) {
            peripheral.removeConnectionListener(listener);
        }
        setState(SensorSessionState.FAILED);
        // The buffered readings go to the store, exactly as stop() does
        // with them. A session that gave up reconnecting still holds
        // whatever arrived since the last batch boundary -- up to a full
        // storeBatchMillis of it, sixty seconds by default -- and the
        // transition above cancels the timer that would have written it.
        // Without this they sat in the buffer until the process ended and
        // were never seen again, silently.
        flushPendingWrites();
        forgetFromManager();
        peripheral.disconnect();
    }

    private HealthException wrapStartFailure(Throwable err) {
        return err instanceof HealthException
                ? (HealthException) err
                : new HealthException(HealthError.SENSOR_DISCONNECTED,
                        "could not start the " + getProfile().getName()
                                + " session", err);
    }

    /// Reports a reconnect-path failure and lets the ladder try again.
    ///
    /// The link is dropped deliberately. Leaving a connected-but-unusable
    /// peripheral in place would be a quieter failure than the one this
    /// replaces: the reconnect listener only fires on a disconnection, so
    /// a session left sitting on a live link with no subscription would
    /// never retry and never report itself broken either.
    ///
    /// Attempts are counted so this cannot cycle forever against a device
    /// that has genuinely changed -- a peripheral reflashed with different
    /// services will never expose the measurement characteristic again,
    /// and the session should end rather than reconnect at it all day.
    /// Clears the reconnect listener and hands it back, so a caller can
    /// deregister it without holding the lock across a peripheral call.
    private Reconnector takeReconnectListener() {
        synchronized (reconnectLock) {
            Reconnector listener = reconnectListener;
            reconnectListener = null;
            return listener;
        }
    }

    private void failReconnect(HealthException wrapped) {
        boolean giveUp;
        synchronized (reconnectLock) {
            reconnectFailures++;
            giveUp = reconnectFailures >= MAX_RECONNECT_FAILURES;
        }
        if (giveUp) {
            endSession();
            fireError(wrapped);
            return;
        }
        fireError(wrapped);
        peripheral.disconnect();
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
        if (isTerminal()) {
            // Either terminal state, not STOPPED alone. A session the
            // reconnect ladder had already retired as FAILED went through
            // the whole teardown a second time -- flushing again, and
            // reporting itself as STOPPED, which is a different account
            // of how it ended than the one its listeners were given.
            return;
        }
        Reconnector listener = takeReconnectListener();
        if (listener != null) {
            // Removed before the state changes, so the disconnect this
            // method causes is not mistaken for a dropped link.
            peripheral.removeConnectionListener(listener);
        }
        setState(SensorSessionState.STOPPED);
        // A short ride would otherwise lose whatever had not reached a
        // batch boundary yet.
        flushPendingWrites();
        forgetFromManager();
        peripheral.disconnect();
    }
}
