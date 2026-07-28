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

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattDescriptor;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.ConnectionOptions;
import com.codename1.bluetooth.le.ConnectionPriority;
import com.codename1.bluetooth.le.ConnectionState;
import com.codename1.bluetooth.le.L2capChannel;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The auto-reconnect ladder, driven through a fake peripheral rather than
 * a radio.
 *
 * <p>The behaviour under test is what happens when the link comes back but
 * the work that follows it does not: a session that reconnects and then
 * trips over discovery used to be retired outright, which is not something
 * the caller can see coming or recover from.</p>
 */
class BleSensorReconnectTest extends UITestBase {

    /** Heart Rate service and its measurement characteristic. */
    private static final BluetoothUuid HR_SERVICE =
            BluetoothUuid.fromShort(0x180D);
    private static final BluetoothUuid HR_MEASUREMENT =
            BluetoothUuid.fromShort(0x2A37);

    /**
     * A transient discovery failure on the way back must not end the
     * session: the reconnect listener never retries from FAILED, so one
     * stumble used to stop the documented auto-reconnect for good.
     */
    @Test
    void aTransientDiscoveryFailureOnReconnectIsRetried() {
        FakePeripheral p = new FakePeripheral();
        BleSensorSession session = start(p);
        assertEquals(SensorSessionState.STREAMING, session.getState());

        p.failDiscoveries = 1;
        p.dropLink();

        pumpUntil(session, SensorSessionState.STREAMING);
        assertEquals(SensorSessionState.STREAMING, session.getState(),
                "one failed discovery should be retried, not fatal");
        assertTrue(p.discoveries >= 3,
                "expected a retry after the failed discovery, saw "
                        + p.discoveries + " discoveries");
    }

    /**
     * It cannot retry forever either. A peripheral that has genuinely
     * changed will never expose the characteristic again, and a session
     * reconnecting at it all day is its own failure mode.
     */
    @Test
    void aPersistentDiscoveryFailureEventuallyEndsTheSession() {
        FakePeripheral p = new FakePeripheral();
        BleSensorSession session = start(p);

        p.failDiscoveries = 99;
        p.dropLink();

        pumpUntil(session, SensorSessionState.FAILED);
        assertEquals(SensorSessionState.FAILED, session.getState());
        assertTrue(p.discoveries <= 5,
                "the ladder should be bounded, saw " + p.discoveries
                        + " discoveries");
    }

    /**
     * A failure on the *initial* start still fails the caller's resource,
     * because somebody is waiting on it.
     */
    @Test
    void anInitialDiscoveryFailureStillFailsTheCaller() {
        FakePeripheral p = new FakePeripheral();
        p.failDiscoveries = 1;
        BleSensorSession session = new BleSensorSession("fake",
                HealthSensorProfile.HEART_RATE, new SensorSessionOptions(),
                p);
        AsyncResource<SensorSession> out =
                new AsyncResource<SensorSession>();
        session.start(out);
        flushSerialCalls();

        assertTrue(out.isDone());
        assertNotNull(errorOf(out));
        assertEquals(SensorSessionState.FAILED, session.getState());
    }

    private static Throwable errorOf(AsyncResource<?> r) {
        final Throwable[] err = new Throwable[1];
        r.except(new com.codename1.util.SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                err[0] = t;
            }
        });
        return err[0];
    }

    private BleSensorSession start(FakePeripheral p) {
        BleSensorSession session = new BleSensorSession("fake",
                HealthSensorProfile.HEART_RATE, new SensorSessionOptions(),
                p);
        AsyncResource<SensorSession> out =
                new AsyncResource<SensorSession>();
        session.start(out);
        flushSerialCalls();
        return session;
    }

    /**
     * The ladder runs over {@code callSerially}, so each hop needs the
     * queue pumped. Bounded so a session that never settles fails the
     * test rather than hanging it.
     */
    private void pumpUntil(BleSensorSession session,
            SensorSessionState wanted) {
        for (int iter = 0; iter < 50; iter++) {
            flushSerialCalls();
            if (session.getState() == wanted) {
                return;
            }
        }
        fail("session stayed in " + session.getState() + ", wanted "
                + wanted);
    }

    /** A peripheral whose every operation succeeds unless scripted not to. */
    private static final class FakePeripheral extends BlePeripheral {

        private int failDiscoveries;
        private int discoveries;
        private int connects;

        @Override
        public String getAddress() {
            return "FA:KE:00:00:00:01";
        }

        @Override
        public String getName() {
            return "Fake Strap";
        }

        void dropLink() {
            fireConnectionStateChanged(ConnectionState.DISCONNECTED,
                    new BluetoothException(BluetoothError.NOT_CONNECTED,
                            "link dropped"));
        }

        @Override
        protected void doConnect(ConnectionOptions options,
                AsyncResource<BlePeripheral> out) {
            connects++;
            out.complete(this);
        }

        @Override
        protected void doDisconnect() {
            fireConnectionStateChanged(ConnectionState.DISCONNECTED, null);
        }

        @Override
        protected void doDiscoverServices(
                AsyncResource<List<GattService>> out) {
            discoveries++;
            if (failDiscoveries > 0) {
                failDiscoveries--;
                out.error(new BluetoothException(
                        BluetoothError.GATT_ERROR,
                        "discovery failed"));
                return;
            }
            GattService hr = new GattService(this, HR_SERVICE, true, 0);
            hr.addCharacteristic(new GattCharacteristic(hr, HR_MEASUREMENT,
                    GattCharacteristic.PROPERTY_NOTIFY, 0));
            List<GattService> services = new ArrayList<GattService>();
            services.add(hr);
            out.complete(services);
        }

        @Override
        protected void doReadCharacteristic(GattCharacteristic c,
                AsyncResource<byte[]> out) {
            out.complete(new byte[] { 0 });
        }

        @Override
        protected void doWriteCharacteristic(GattCharacteristic c,
                byte[] value, boolean withResponse,
                AsyncResource<Boolean> out) {
            out.complete(Boolean.TRUE);
        }

        @Override
        protected void doReadDescriptor(GattDescriptor d,
                AsyncResource<byte[]> out) {
            out.complete(new byte[] { 0 });
        }

        @Override
        protected void doWriteDescriptor(GattDescriptor d, byte[] value,
                AsyncResource<Boolean> out) {
            out.complete(Boolean.TRUE);
        }

        @Override
        protected void doSetNotifications(GattCharacteristic c,
                boolean enable, boolean indication,
                AsyncResource<Boolean> out) {
            out.complete(Boolean.TRUE);
        }

        @Override
        protected void doReadRssi(AsyncResource<Integer> out) {
            out.complete(Integer.valueOf(-50));
        }

        @Override
        protected void doRequestMtu(int mtu, AsyncResource<Integer> out) {
            out.complete(Integer.valueOf(mtu));
        }

        @Override
        protected void doRequestConnectionPriority(
                ConnectionPriority priority, AsyncResource<Boolean> out) {
            out.complete(Boolean.TRUE);
        }

        @Override
        protected void doCreateBond(AsyncResource<Boolean> out) {
            out.complete(Boolean.TRUE);
        }

        @Override
        protected void doOpenL2cap(int psm, boolean secure,
                AsyncResource<L2capChannel> out) {
            out.error(new BluetoothException(BluetoothError.NOT_SUPPORTED,
                    "no channels here"));
        }
    }
}
