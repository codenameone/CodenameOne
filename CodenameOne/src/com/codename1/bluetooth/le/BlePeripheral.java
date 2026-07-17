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

import com.codename1.bluetooth.BluetoothDevice;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattDescriptor;
import com.codename1.bluetooth.gatt.GattNotificationListener;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimerTask;

/// A remote BLE peripheral: connection lifecycle, GATT client operations
/// and L2CAP channels. Obtained from scans
/// ([ScanResult#getPeripheral()]) or from
/// [BluetoothLE#getPeripheral(String)].
///
/// All operations return independent `AsyncResource` handles and may be
/// issued concurrently -- an internal per-peripheral queue serializes them
/// toward the platform stack (which only allows one in-flight GATT request
/// per connection) and applies a safety timeout so a lost platform
/// callback can never wedge the queue. Operations on *different*
/// peripherals run fully concurrently.
///
/// Ports subclass this and implement the protected `do*` methods; they
/// report events through the `fire*` methods, which are safe to call from
/// any thread. All application-facing callbacks are delivered on the EDT.
public abstract class BlePeripheral extends BluetoothDevice {

    private final GattOperationQueue queue = new GattOperationQueue();
    private final Object stateLock = new Object();
    private ConnectionState state = ConnectionState.DISCONNECTED;
    private AsyncResource<BlePeripheral> pendingConnect;
    private TimerTask connectTimeout;
    private java.util.Timer connectTimeoutTimer;
    private ArrayList<ConnectionListener> connectionListeners;
    private ArrayList<GattService> services;
    private final HashMap<GattCharacteristic, ArrayList<GattNotificationListener>>
            subscriptions =
            new HashMap<GattCharacteristic, ArrayList<GattNotificationListener>>();
    private final HashSet<GattCharacteristic> armed =
            new HashSet<GattCharacteristic>();
    private final HashMap<GattCharacteristic, AsyncResource<Boolean>> armOps =
            new HashMap<GattCharacteristic, AsyncResource<Boolean>>();
    private int mtu = 23;

    /// Ports construct subclasses; application code receives instances
    /// from scans and [BluetoothLE].
    protected BlePeripheral() {
    }

    // ------------------------------------------------------------------
    // connection lifecycle
    // ------------------------------------------------------------------

    /// Connects with default [ConnectionOptions].
    public final AsyncResource<BlePeripheral> connect() {
        return connect(new ConnectionOptions());
    }

    /// Establishes a connection. Resolves with this peripheral once
    /// connected or fails with a [BluetoothException]. Calling `connect`
    /// while already [ConnectionState#CONNECTING] returns the existing
    /// attempt's handle; while [ConnectionState#CONNECTED] it resolves
    /// immediately.
    public final AsyncResource<BlePeripheral> connect(ConnectionOptions options) {
        final ConnectionOptions opts =
                options == null ? new ConnectionOptions() : options;
        final AsyncResource<BlePeripheral> out;
        synchronized (stateLock) {
            if (state == ConnectionState.CONNECTED) {
                AsyncResource<BlePeripheral> done =
                        new AsyncResource<BlePeripheral>();
                done.complete(this);
                return done;
            }
            if (state == ConnectionState.CONNECTING && pendingConnect != null) {
                return pendingConnect;
            }
            out = new AsyncResource<BlePeripheral>();
            pendingConnect = out;
        }
        out.onResult(new AsyncResult<BlePeripheral>() {
            public void onReady(BlePeripheral value, Throwable err) {
                connectFinished(out, err);
            }
        });
        setState(ConnectionState.CONNECTING, null);
        if (opts.getTimeout() > 0) {
            TimerTask t = connectTimeoutTask(out, opts.getTimeout());
            synchronized (stateLock) {
                connectTimeout = t;
                connectTimeoutTimer =
                        GattOperationQueue.schedule(t, opts.getTimeout());
            }
        }
        try {
            doConnect(opts, out);
        } catch (RuntimeException ex) {
            if (!out.isDone()) {
                out.error(new BluetoothException(BluetoothError.CONNECTION_FAILED,
                        "Connect failed to start: " + ex, ex));
            }
        }
        return out;
    }

    // Static so the TimerTask doesn't carry a synthetic outer-BlePeripheral
    // reference (SpotBugs SIC_INNER_SHOULD_BE_STATIC_ANON).
    private static TimerTask connectTimeoutTask(
            final AsyncResource<BlePeripheral> out, final int timeout) {
        return new TimerTask() {
            public void run() {
                if (!out.isDone()) {
                    out.error(new BluetoothException(BluetoothError.TIMEOUT,
                            "Connect attempt timed out after " + timeout
                                    + "ms"));
                }
            }
        };
    }

    private void connectFinished(AsyncResource<BlePeripheral> out,
            Throwable err) {
        synchronized (stateLock) {
            if (pendingConnect == out) {
                pendingConnect = null;
            }
            if (connectTimeout != null) {
                connectTimeout.cancel();
                connectTimeout = null;
            }
            if (connectTimeoutTimer != null) {
                connectTimeoutTimer.cancel();
                connectTimeoutTimer = null;
            }
        }
        if (err == null) {
            setState(ConnectionState.CONNECTED, null);
        } else {
            // abort a possibly still ongoing platform attempt (timeout or
            // cancellation path)
            try {
                doDisconnect();
            } catch (RuntimeException ignored) {
            }
            setState(ConnectionState.DISCONNECTED, asBluetoothException(err,
                    BluetoothError.CONNECTION_FAILED));
        }
    }

    /// Disconnects. A no-op while already disconnected; an in-flight
    /// connect attempt is aborted with
    /// [BluetoothError#USER_CANCELED].
    public final void disconnect() {
        AsyncResource<BlePeripheral> pending;
        synchronized (stateLock) {
            if (state == ConnectionState.DISCONNECTED
                    || state == ConnectionState.DISCONNECTING) {
                return;
            }
            pending = pendingConnect;
        }
        if (pending != null && !pending.isDone()) {
            pending.error(new BluetoothException(BluetoothError.USER_CANCELED,
                    "disconnect() called during connect"));
            return;
        }
        setState(ConnectionState.DISCONNECTING, null);
        try {
            doDisconnect();
        } catch (RuntimeException ignored) {
        }
    }

    /// The current connection state.
    public final ConnectionState getConnectionState() {
        synchronized (stateLock) {
            return state;
        }
    }

    /// Registers a listener notified on the EDT of every connection state
    /// transition.
    public final void addConnectionListener(ConnectionListener l) {
        if (l == null) {
            return;
        }
        synchronized (stateLock) {
            if (connectionListeners == null) {
                connectionListeners = new ArrayList<ConnectionListener>();
            }
            if (!connectionListeners.contains(l)) {
                connectionListeners.add(l);
            }
        }
    }

    /// Removes a listener added via
    /// [#addConnectionListener(ConnectionListener)].
    public final void removeConnectionListener(ConnectionListener l) {
        synchronized (stateLock) {
            if (connectionListeners != null) {
                connectionListeners.remove(l);
            }
        }
    }

    // ------------------------------------------------------------------
    // GATT client
    // ------------------------------------------------------------------

    /// Discovers the peripheral's services. Resolves with the service list
    /// (also cached -- see [#getServices()]) or fails with a
    /// [BluetoothException].
    public final AsyncResource<List<GattService>> discoverServices() {
        final AsyncResource<List<GattService>> out =
                new AsyncResource<List<GattService>>();
        if (failIfNotConnected(out)) {
            return out;
        }
        out.onResult(new AsyncResult<List<GattService>>() {
            public void onReady(List<GattService> value, Throwable err) {
                if (err == null && value != null) {
                    synchronized (stateLock) {
                        services = new ArrayList<GattService>(value);
                    }
                }
            }
        });
        queue.enqueue(new GattOperationQueue.Op(out) {
            void start() {
                doDiscoverServices(out);
            }
        });
        return out;
    }

    /// The cached service list from the last [#discoverServices()] call;
    /// empty before discovery.
    public final List<GattService> getServices() {
        synchronized (stateLock) {
            return services == null
                    ? new ArrayList<GattService>()
                    : new ArrayList<GattService>(services);
        }
    }

    /// The first cached service with the given UUID, or `null`.
    public final GattService getService(BluetoothUuid uuid) {
        List<GattService> list = getServices();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            GattService s = list.get(i);
            if (s.getUuid().equals(uuid)) {
                return s;
            }
        }
        return null;
    }

    /// Convenience for `getService(service).getCharacteristic(characteristic)`
    /// that returns `null` instead of throwing when either is absent.
    public final GattCharacteristic getCharacteristic(BluetoothUuid service,
            BluetoothUuid characteristic) {
        GattService s = getService(service);
        return s == null ? null : s.getCharacteristic(characteristic);
    }

    /// Reads a characteristic value; prefer the convenience
    /// [GattCharacteristic#read()].
    public final AsyncResource<byte[]> readCharacteristic(
            final GattCharacteristic c) {
        final AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        if (failIfNotConnected(out)) {
            return out;
        }
        queue.enqueue(new GattOperationQueue.Op(out) {
            void start() {
                doReadCharacteristic(c, out);
            }
        });
        return out;
    }

    /// Writes a characteristic value; prefer the convenience
    /// [GattCharacteristic#write(byte[])] /
    /// [GattCharacteristic#writeWithoutResponse(byte[])].
    public final AsyncResource<Boolean> writeCharacteristic(
            final GattCharacteristic c, final byte[] value,
            final boolean withResponse) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (failIfNotConnected(out)) {
            return out;
        }
        queue.enqueue(new GattOperationQueue.Op(out) {
            void start() {
                doWriteCharacteristic(c, value, withResponse, out);
            }
        });
        return out;
    }

    /// Reads a descriptor value; prefer the convenience
    /// [GattDescriptor#read()].
    public final AsyncResource<byte[]> readDescriptor(final GattDescriptor d) {
        final AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        if (failIfNotConnected(out)) {
            return out;
        }
        queue.enqueue(new GattOperationQueue.Op(out) {
            void start() {
                doReadDescriptor(d, out);
            }
        });
        return out;
    }

    /// Writes a descriptor value; prefer the convenience
    /// [GattDescriptor#write(byte[])].
    public final AsyncResource<Boolean> writeDescriptor(final GattDescriptor d,
            final byte[] value) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (failIfNotConnected(out)) {
            return out;
        }
        queue.enqueue(new GattOperationQueue.Op(out) {
            void start() {
                doWriteDescriptor(d, value, out);
            }
        });
        return out;
    }

    /// Subscribes a listener to a characteristic's notifications; prefer
    /// the convenience
    /// [GattCharacteristic#subscribe(GattNotificationListener)]. The CCCD
    /// is written only on the transition from zero to one listener.
    public final AsyncResource<Boolean> subscribe(final GattCharacteristic c,
            GattNotificationListener l) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (l == null) {
            out.error(new BluetoothException(BluetoothError.UNKNOWN,
                    "subscribe requires a listener"));
            return out;
        }
        if (failIfNotConnected(out)) {
            return out;
        }
        AsyncResource<Boolean> arm = null;
        boolean startArm = false;
        synchronized (subscriptions) {
            ArrayList<GattNotificationListener> list = subscriptions.get(c);
            if (list == null) {
                list = new ArrayList<GattNotificationListener>();
                subscriptions.put(c, list);
            }
            if (!list.contains(l)) {
                list.add(l);
            }
            if (armed.contains(c)) {
                out.complete(Boolean.TRUE);
                return out;
            }
            arm = armOps.get(c);
            if (arm == null) {
                arm = new AsyncResource<Boolean>();
                armOps.put(c, arm);
                startArm = true;
            }
        }
        arm.addListener(out);
        if (startArm) {
            final AsyncResource<Boolean> armRes = arm;
            arm.onResult(new AsyncResult<Boolean>() {
                public void onReady(Boolean value, Throwable err) {
                    synchronized (subscriptions) {
                        armOps.remove(c);
                        if (err == null) {
                            armed.add(c);
                        }
                    }
                    checkDisarm(c);
                }
            });
            final boolean indication = !c.canNotify() && c.canIndicate();
            queue.enqueue(new GattOperationQueue.Op(armRes) {
                void start() {
                    doSetNotifications(c, true, indication, armRes);
                }
            });
        }
        return out;
    }

    /// Removes a notification listener; prefer the convenience
    /// [GattCharacteristic#unsubscribe(GattNotificationListener)]. The
    /// CCCD is disarmed when the last listener is removed.
    public final AsyncResource<Boolean> unsubscribe(GattCharacteristic c,
            GattNotificationListener l) {
        synchronized (subscriptions) {
            ArrayList<GattNotificationListener> list = subscriptions.get(c);
            if (list != null) {
                list.remove(l);
                if (list.isEmpty()) {
                    subscriptions.remove(c);
                }
            }
        }
        return checkDisarm(c);
    }

    /// `true` while notifications are armed for the characteristic.
    public final boolean isSubscribed(GattCharacteristic c) {
        synchronized (subscriptions) {
            return armed.contains(c);
        }
    }

    private AsyncResource<Boolean> checkDisarm(final GattCharacteristic c) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        boolean needDisarm;
        synchronized (subscriptions) {
            boolean hasListeners = subscriptions.containsKey(c);
            boolean armInFlight = armOps.containsKey(c);
            needDisarm = !hasListeners && !armInFlight && armed.contains(c);
            if (needDisarm) {
                armed.remove(c);
            }
        }
        if (!needDisarm) {
            out.complete(Boolean.TRUE);
            return out;
        }
        if (getConnectionState() != ConnectionState.CONNECTED) {
            out.complete(Boolean.TRUE);
            return out;
        }
        final boolean indication = !c.canNotify() && c.canIndicate();
        queue.enqueue(new GattOperationQueue.Op(out) {
            void start() {
                doSetNotifications(c, false, indication, out);
            }
        });
        return out;
    }

    /// Reads the current RSSI of the connection.
    public final AsyncResource<Integer> readRssi() {
        final AsyncResource<Integer> out = new AsyncResource<Integer>();
        if (failIfNotConnected(out)) {
            return out;
        }
        queue.enqueue(new GattOperationQueue.Op(out) {
            void start() {
                doReadRssi(out);
            }
        });
        return out;
    }

    /// Requests an MTU; resolves with the granted value (which may be
    /// smaller). iOS negotiates the MTU itself -- there the request
    /// resolves immediately with the current value.
    public final AsyncResource<Integer> requestMtu(final int mtu) {
        final AsyncResource<Integer> out = new AsyncResource<Integer>();
        if (failIfNotConnected(out)) {
            return out;
        }
        out.onResult(new AsyncResult<Integer>() {
            public void onReady(Integer value, Throwable err) {
                if (err == null && value != null) {
                    BlePeripheral.this.mtu = value.intValue();
                }
            }
        });
        queue.enqueue(new GattOperationQueue.Op(out) {
            void start() {
                doRequestMtu(mtu, out);
            }
        });
        return out;
    }

    /// The current MTU of the connection; `23` (the BLE default) until a
    /// larger value was negotiated.
    public final int getMtu() {
        return mtu;
    }

    /// Requests a connection-interval preference; a successful no-op on
    /// platforms that manage intervals themselves (iOS).
    public final AsyncResource<Boolean> requestConnectionPriority(
            final ConnectionPriority priority) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        if (failIfNotConnected(out)) {
            return out;
        }
        queue.enqueue(new GattOperationQueue.Op(out) {
            void start() {
                doRequestConnectionPriority(priority, out);
            }
        });
        return out;
    }

    /// Initiates bonding/pairing. On iOS bonding is OS-managed (triggered
    /// by encrypted characteristics), so the request resolves `true`
    /// without user interaction.
    public final AsyncResource<Boolean> createBond() {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        queue.enqueue(new GattOperationQueue.Op(out) {
            void start() {
                doCreateBond(out);
            }
        });
        return out;
    }

    /// Opens an L2CAP connection-oriented channel to the given PSM. Not
    /// serialized with GATT operations. On Android this establishes its
    /// own link and does not require [#connect()] first; on iOS the
    /// peripheral must be connected.
    public final AsyncResource<L2capChannel> openL2capChannel(int psm,
            boolean secure) {
        final AsyncResource<L2capChannel> out =
                new AsyncResource<L2capChannel>();
        try {
            doOpenL2cap(psm, secure, out);
        } catch (RuntimeException ex) {
            if (!out.isDone()) {
                out.error(new BluetoothException(BluetoothError.IO_ERROR,
                        "L2CAP open failed: " + ex, ex));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // port SPI -- one queued operation is in flight at a time
    // ------------------------------------------------------------------

    /// Establishes the platform connection and completes/fails `out`.
    protected abstract void doConnect(ConnectionOptions options,
            AsyncResource<BlePeripheral> out);

    /// Tears down the platform connection;
    /// [#fireConnectionStateChanged(ConnectionState, BluetoothException)]
    /// reports the resulting `DISCONNECTED` transition.
    protected abstract void doDisconnect();

    /// Performs service discovery and completes `out` with the discovered
    /// [GattService] list (constructed via the public gatt-package
    /// constructors).
    protected abstract void doDiscoverServices(
            AsyncResource<List<GattService>> out);

    /// Reads a characteristic and completes `out` with its value.
    protected abstract void doReadCharacteristic(GattCharacteristic c,
            AsyncResource<byte[]> out);

    /// Writes a characteristic and completes `out` once acknowledged
    /// (`withResponse`) or queued to the controller.
    protected abstract void doWriteCharacteristic(GattCharacteristic c,
            byte[] value, boolean withResponse, AsyncResource<Boolean> out);

    /// Reads a descriptor and completes `out` with its value.
    protected abstract void doReadDescriptor(GattDescriptor d,
            AsyncResource<byte[]> out);

    /// Writes a descriptor and completes `out` once acknowledged.
    protected abstract void doWriteDescriptor(GattDescriptor d, byte[] value,
            AsyncResource<Boolean> out);

    /// Arms or disarms notifications/indications (including the CCCD
    /// write) and completes `out` once done.
    protected abstract void doSetNotifications(GattCharacteristic c,
            boolean enable, boolean indication, AsyncResource<Boolean> out);

    /// Reads the connection RSSI and completes `out`.
    protected abstract void doReadRssi(AsyncResource<Integer> out);

    /// Requests an MTU and completes `out` with the granted value.
    protected abstract void doRequestMtu(int mtu, AsyncResource<Integer> out);

    /// Requests a connection-interval preference and completes `out`.
    protected abstract void doRequestConnectionPriority(
            ConnectionPriority priority, AsyncResource<Boolean> out);

    /// Initiates bonding and completes `out` when the bond state settles.
    protected abstract void doCreateBond(AsyncResource<Boolean> out);

    /// Opens an L2CAP channel and completes `out` with it. Called
    /// directly (not through the operation queue).
    protected abstract void doOpenL2cap(int psm, boolean secure,
            AsyncResource<L2capChannel> out);

    // ------------------------------------------------------------------
    // port event entry points -- safe to call from any thread
    // ------------------------------------------------------------------

    /// Reports an (unsolicited) connection state transition -- link loss,
    /// remote disconnect, or connection established outside a pending
    /// connect call. `reason` is `null` for app-requested transitions.
    protected final void fireConnectionStateChanged(ConnectionState newState,
            BluetoothException reason) {
        setState(newState, reason);
    }

    /// Delivers a notification/indication value to the subscribed
    /// listeners on the EDT. Ports must pass the canonical
    /// [GattCharacteristic] instance from the discovered database.
    protected final void fireNotification(final GattCharacteristic c,
            final byte[] value) {
        final Object[] snapshot;
        synchronized (subscriptions) {
            ArrayList<GattNotificationListener> list = subscriptions.get(c);
            if (list == null || list.isEmpty()) {
                return;
            }
            snapshot = list.toArray();
        }
        dispatchNotification(snapshot, c, value);
    }

    // The dispatch helpers are static so their Runnables don't carry a
    // synthetic outer-BlePeripheral reference (SpotBugs
    // SIC_INNER_SHOULD_BE_STATIC_ANON).
    private static void dispatchNotification(final Object[] snapshot,
            final GattCharacteristic c, final byte[] value) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                for (int i = 0; i < snapshot.length; i++) {
                    ((GattNotificationListener) snapshot[i])
                            .valueChanged(c, value);
                }
            }
        });
    }

    /// Invalidates the cached service database (iOS
    /// `didModifyServices`); the app should re-run
    /// [#discoverServices()].
    protected final void fireServicesInvalidated() {
        synchronized (stateLock) {
            services = null;
        }
    }

    /// Adjusts the safety timeout applied to each queued GATT operation
    /// (default 30000ms; `0` disables).
    protected final void setOperationTimeout(int millis) {
        queue.setTimeoutMillis(millis);
    }

    /// Records the negotiated MTU -- for ports whose platform reports MTU
    /// changes outside a [#requestMtu(int)] call.
    protected final void setMtu(int mtu) {
        this.mtu = mtu;
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    private void setState(ConnectionState newState, BluetoothException reason) {
        AsyncResource<BlePeripheral> pending = null;
        final Object[] snapshot;
        synchronized (stateLock) {
            if (state == newState) {
                return;
            }
            state = newState;
            if (newState == ConnectionState.CONNECTED
                    || newState == ConnectionState.DISCONNECTED) {
                pending = pendingConnect;
            }
            snapshot = connectionListeners == null
                    || connectionListeners.isEmpty()
                    ? null : connectionListeners.toArray();
        }
        if (newState == ConnectionState.DISCONNECTED) {
            BluetoothException failReason = reason != null ? reason
                    : new BluetoothException(BluetoothError.NOT_CONNECTED,
                            "Peripheral disconnected");
            queue.failAll(failReason);
            synchronized (subscriptions) {
                armed.clear();
            }
            if (pending != null && !pending.isDone()) {
                pending.error(reason != null ? reason
                        : new BluetoothException(
                                BluetoothError.CONNECTION_FAILED,
                                "Disconnected while connecting"));
            }
        } else if (newState == ConnectionState.CONNECTED) {
            if (pending != null && !pending.isDone()) {
                pending.complete(this);
            }
        }
        if (snapshot != null) {
            dispatchConnectionEvent(snapshot,
                    new ConnectionEvent(this, newState, reason));
        }
    }

    private static void dispatchConnectionEvent(final Object[] snapshot,
            final ConnectionEvent ev) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                for (int i = 0; i < snapshot.length; i++) {
                    ((ConnectionListener) snapshot[i])
                            .connectionStateChanged(ev);
                }
            }
        });
    }

    private boolean failIfNotConnected(AsyncResource<?> out) {
        if (getConnectionState() != ConnectionState.CONNECTED) {
            out.error(new BluetoothException(BluetoothError.NOT_CONNECTED,
                    "Peripheral is not connected"));
            return true;
        }
        return false;
    }

    private static BluetoothException asBluetoothException(Throwable t,
            BluetoothError fallback) {
        if (t instanceof BluetoothException) {
            return (BluetoothException) t;
        }
        return new BluetoothException(fallback,
                t == null ? null : t.toString(), t);
    }
}
