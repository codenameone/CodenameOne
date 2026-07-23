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
package com.codename1.bluetooth;

import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattDescriptor;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.ConnectionOptions;
import com.codename1.bluetooth.le.ConnectionPriority;
import com.codename1.bluetooth.le.ConnectionState;
import com.codename1.bluetooth.le.L2capChannel;
import com.codename1.util.AsyncResource;

import java.util.LinkedList;
import java.util.List;

/**
 * Scripted {@link BlePeripheral} implementing every {@code do*} SPI method by
 * RECORDING the pending operation; nothing completes until the test drains
 * the queue explicitly via {@link #completeNext(Object)} /
 * {@link #failNext(BluetoothException)}. This makes every asynchronous flow
 * synchronous-on-demand and therefore deterministic.
 */
public class FakeBlePeripheral extends BlePeripheral {

    /**
     * The SPI entry points that can be recorded.
     */
    public enum OpKind {
        CONNECT,
        DISCONNECT,
        DISCOVER_SERVICES,
        READ_CHARACTERISTIC,
        WRITE_CHARACTERISTIC,
        READ_DESCRIPTOR,
        WRITE_DESCRIPTOR,
        SET_NOTIFICATIONS,
        READ_RSSI,
        REQUEST_MTU,
        CONNECTION_PRIORITY,
        CREATE_BOND,
        OPEN_L2CAP
    }

    /**
     * One recorded, not-yet-completed platform operation.
     */
    public static final class PendingOp {
        public final OpKind kind;
        /** The operation's result handle; {@code null} for DISCONNECT. */
        public final AsyncResource<?> out;
        public final GattCharacteristic characteristic;
        public final GattDescriptor descriptor;
        public final byte[] value;
        public final boolean withResponse;
        public final boolean enable;
        public final boolean indication;
        public final int intArg;

        PendingOp(OpKind kind, AsyncResource<?> out,
                GattCharacteristic characteristic, GattDescriptor descriptor,
                byte[] value, boolean withResponse, boolean enable,
                boolean indication, int intArg) {
            this.kind = kind;
            this.out = out;
            this.characteristic = characteristic;
            this.descriptor = descriptor;
            this.value = value;
            this.withResponse = withResponse;
            this.enable = enable;
            this.indication = indication;
            this.intArg = intArg;
        }
    }

    private final Object pendingLock = new Object();
    private final LinkedList<PendingOp> pending = new LinkedList<PendingOp>();
    private final String address;
    private final String name;

    public FakeBlePeripheral(String address, String name) {
        this.address = address;
        this.name = name;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public String getName() {
        return name;
    }

    // ------------------------------------------------------------------
    // test scripting
    // ------------------------------------------------------------------

    /**
     * The number of recorded operations the test has not drained yet.
     */
    public int pendingCount() {
        synchronized (pendingLock) {
            return pending.size();
        }
    }

    /**
     * The head of the recorded-operation queue without removing it, or
     * {@code null} when empty.
     */
    public PendingOp peekNext() {
        synchronized (pendingLock) {
            return pending.peek();
        }
    }

    /**
     * Removes and returns the head of the recorded-operation queue; throws
     * when no operation was recorded (the test scripted a flow that never
     * reached the SPI).
     */
    public PendingOp takeNext() {
        synchronized (pendingLock) {
            PendingOp op = pending.poll();
            if (op == null) {
                throw new IllegalStateException(
                        "No pending platform operation was recorded");
            }
            return op;
        }
    }

    /**
     * Hang-guarded wait until at least {@code minCount} operations are
     * recorded -- only needed when a non-test thread (the safety-timeout
     * timer) advances the queue. Throws on timeout instead of hanging.
     */
    public void awaitPendingCount(int minCount, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        synchronized (pendingLock) {
            while (pending.size() < minCount) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new IllegalStateException("Timed out waiting for "
                            + minCount + " pending ops, have "
                            + pending.size());
                }
                try {
                    pendingLock.wait(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted", e);
                }
            }
        }
    }

    /**
     * Completes the next recorded operation with the given value. A
     * DISCONNECT operation instead reports the DISCONNECTED transition, as
     * the platform would.
     */
    @SuppressWarnings("unchecked")
    public PendingOp completeNext(Object value) {
        PendingOp op = takeNext();
        if (op.kind == OpKind.DISCONNECT) {
            fireConnectionStateChanged(ConnectionState.DISCONNECTED, null);
            return op;
        }
        ((AsyncResource<Object>) op.out).complete(value);
        return op;
    }

    /**
     * Fails the next recorded operation with the given error. A DISCONNECT
     * operation instead reports a DISCONNECTED transition carrying the
     * error as its reason.
     */
    public PendingOp failNext(BluetoothException error) {
        PendingOp op = takeNext();
        if (op.kind == OpKind.DISCONNECT) {
            fireConnectionStateChanged(ConnectionState.DISCONNECTED, error);
            return op;
        }
        if (!op.out.isDone()) {
            op.out.error(error);
        }
        return op;
    }

    /**
     * Convenience: issues {@link #connect()} from the DISCONNECTED state and
     * immediately completes the recorded platform attempt, leaving the
     * peripheral CONNECTED.
     */
    public AsyncResource<BlePeripheral> connectNow() {
        AsyncResource<BlePeripheral> r = connect();
        completeNext(this);
        return r;
    }

    /**
     * Exposes the protected operation-timeout knob for the timeout test.
     */
    public void setOpTimeout(int millis) {
        setOperationTimeout(millis);
    }

    /**
     * Exposes {@code fireConnectionStateChanged} for scripting unsolicited
     * transitions (link loss and the like).
     */
    public void fireState(ConnectionState newState, BluetoothException reason) {
        fireConnectionStateChanged(newState, reason);
    }

    /**
     * Exposes {@code fireNotification} for delivering scripted
     * notification/indication values.
     */
    public void notifyValue(GattCharacteristic c, byte[] value) {
        fireNotification(c, value);
    }

    /**
     * Exposes {@code fireServicesInvalidated}.
     */
    public void invalidateServices() {
        fireServicesInvalidated();
    }

    /**
     * Builds a primary {@link GattService} owned by this peripheral via the
     * public gatt-package constructors.
     */
    public GattService buildService(BluetoothUuid uuid) {
        return new GattService(this, uuid, true, 0);
    }

    /**
     * Builds a {@link GattCharacteristic} and registers it on the service.
     */
    public GattCharacteristic buildCharacteristic(GattService service,
            BluetoothUuid uuid, int properties) {
        GattCharacteristic c = new GattCharacteristic(service, uuid,
                properties, 0);
        service.addCharacteristic(c);
        return c;
    }

    private void record(PendingOp op) {
        synchronized (pendingLock) {
            pending.add(op);
            pendingLock.notifyAll();
        }
    }

    // ------------------------------------------------------------------
    // recorded SPI
    // ------------------------------------------------------------------

    @Override
    protected void doConnect(ConnectionOptions options,
            AsyncResource<BlePeripheral> out) {
        record(new PendingOp(OpKind.CONNECT, out, null, null, null, false,
                false, false, 0));
    }

    @Override
    protected void doDisconnect() {
        record(new PendingOp(OpKind.DISCONNECT, null, null, null, null, false,
                false, false, 0));
    }

    @Override
    protected void doDiscoverServices(AsyncResource<List<GattService>> out) {
        record(new PendingOp(OpKind.DISCOVER_SERVICES, out, null, null, null,
                false, false, false, 0));
    }

    @Override
    protected void doReadCharacteristic(GattCharacteristic c,
            AsyncResource<byte[]> out) {
        record(new PendingOp(OpKind.READ_CHARACTERISTIC, out, c, null, null,
                false, false, false, 0));
    }

    @Override
    protected void doWriteCharacteristic(GattCharacteristic c, byte[] value,
            boolean withResponse, AsyncResource<Boolean> out) {
        record(new PendingOp(OpKind.WRITE_CHARACTERISTIC, out, c, null, value,
                withResponse, false, false, 0));
    }

    @Override
    protected void doReadDescriptor(GattDescriptor d,
            AsyncResource<byte[]> out) {
        record(new PendingOp(OpKind.READ_DESCRIPTOR, out, null, d, null, false,
                false, false, 0));
    }

    @Override
    protected void doWriteDescriptor(GattDescriptor d, byte[] value,
            AsyncResource<Boolean> out) {
        record(new PendingOp(OpKind.WRITE_DESCRIPTOR, out, null, d, value,
                false, false, false, 0));
    }

    @Override
    protected void doSetNotifications(GattCharacteristic c, boolean enable,
            boolean indication, AsyncResource<Boolean> out) {
        record(new PendingOp(OpKind.SET_NOTIFICATIONS, out, c, null, null,
                false, enable, indication, 0));
    }

    @Override
    protected void doReadRssi(AsyncResource<Integer> out) {
        record(new PendingOp(OpKind.READ_RSSI, out, null, null, null, false,
                false, false, 0));
    }

    @Override
    protected void doRequestMtu(int mtu, AsyncResource<Integer> out) {
        record(new PendingOp(OpKind.REQUEST_MTU, out, null, null, null, false,
                false, false, mtu));
    }

    @Override
    protected void doRequestConnectionPriority(ConnectionPriority priority,
            AsyncResource<Boolean> out) {
        record(new PendingOp(OpKind.CONNECTION_PRIORITY, out, null, null, null,
                false, false, false, 0));
    }

    @Override
    protected void doCreateBond(AsyncResource<Boolean> out) {
        record(new PendingOp(OpKind.CREATE_BOND, out, null, null, null, false,
                false, false, 0));
    }

    @Override
    protected void doOpenL2cap(int psm, boolean secure,
            AsyncResource<L2capChannel> out) {
        record(new PendingOp(OpKind.OPEN_L2CAP, out, null, null, null, false,
                false, false, psm));
    }
}
