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

    /** Battery service and its level characteristic. */
    private static final BluetoothUuid BATTERY_SERVICE =
            BluetoothUuid.fromShort(0x180F);
    private static final BluetoothUuid BATTERY_LEVEL =
            BluetoothUuid.fromShort(0x2A19);

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
            started.add(session);
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
            started.add(session);
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
        private int failConnects;
        /// What the Battery Level characteristic answers, or null for no
        /// battery service at all.
        Byte batteryLevel;
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

        /// Delivers a 0x2A37 notification with the given rate, as a
        /// strap would.
        void notifyHeartRate(int bpm) {
            GattService hr = peripheralService();
            if (hr == null) {
                return;
            }
            fireNotification(hr.getCharacteristic(HR_MEASUREMENT),
                    new byte[] { 0x00, (byte) bpm });
        }

        private GattService peripheralService() {
            return getService(HR_SERVICE);
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
            if (failConnects > 0) {
                failConnects--;
                out.error(new BluetoothException(
                        BluetoothError.CONNECTION_FAILED, "no such device"));
                return;
            }
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
            if (batteryLevel != null) {
                GattService battery = new GattService(this,
                        BATTERY_SERVICE, true, 0);
                battery.addCharacteristic(new GattCharacteristic(battery,
                        BATTERY_LEVEL, GattCharacteristic.PROPERTY_READ, 0));
                services.add(battery);
            }
            out.complete(services);
        }

        @Override
        protected void doReadCharacteristic(GattCharacteristic c,
                AsyncResource<byte[]> out) {
            if (batteryLevel != null
                    && BATTERY_LEVEL.equals(c.getUuid())) {
                out.complete(new byte[] { batteryLevel.byteValue() });
                return;
            }
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

    /**
     * A session that has ended must not keep retrying its store writes.
     *
     * <p>The re-arm guard tested only for STOPPED, but the reconnect
     * ladder retires a session as FAILED -- so exactly the session nobody
     * holds a handle to any more, already gone from the manager's
     * registry, went on firing writes and errors on a timer that nothing
     * could cancel.</p>
     */
    @Test
    void aFailedSessionStopsRetryingItsWrites() throws Exception {
        CountingStore store = new CountingStore();
        com.codename1.health.Health health =
                new com.codename1.health.Health() {
                    @Override
                    public boolean isSupported() {
                        return true;
                    }

                    @Override
                    public com.codename1.health.HealthStore getStore() {
                        return store;
                    }
                };
        implementation.setHealth(health);
        try {
            FakePeripheral p = new FakePeripheral();
            SensorSessionOptions options = new SensorSessionOptions()
                    .setWriteToStore(true)
                    .setStoreBatchMillis(10);
            BleSensorSession session = new BleSensorSession("fake",
                    HealthSensorProfile.HEART_RATE, options, p);
            started.add(session);
            AsyncResource<SensorSession> out =
                    new AsyncResource<SensorSession>();
            session.start(out);
            flushSerialCalls();
            assertEquals(SensorSessionState.STREAMING, session.getState());

            // One reading, so a batch is pending, then a permanent
            // failure while it is still unwritten.
            final int[] samplesSeen = new int[1];
            session.addListener(new SensorSampleListener() {
                public void sensorSample(SensorSession s,
                        com.codename1.health.HealthSample sample) {
                    samplesSeen[0]++;
                }

                public void sensorStateChanged(SensorSession s,
                        SensorSessionState state) {
                }

                public void sensorError(SensorSession s,
                        com.codename1.health.HealthException error) {
                }
            });
            p.notifyHeartRate(72);
            flushSerialCalls();
            assertTrue(samplesSeen[0] > 0,
                    "the notification should have decoded to a sample");
            // The batch has to have been attempted at least once, or
            // the retry this guards against never had anything to retry
            // and the test would pass for no reason.
            int beforeFailure = pumpFor(200, store);
            assertTrue(beforeFailure > 0,
                    "the session should have tried to write its batch");

            p.failDiscoveries = 99;
            p.dropLink();
            pumpUntil(session, SensorSessionState.FAILED);
            assertTrue(session.isTerminal());

            int afterFailure = pumpFor(300, store);
            assertEquals(afterFailure, pumpFor(300, store),
                    "a session that has ended must issue no further"
                            + " writes; it issued more");
        } finally {
            implementation.setHealth(null);
        }
    }

    /** Pumps the EDT for a while and reports the store's write count. */
    private int pumpFor(long millis, WriteCounter store) throws Exception {
        long until = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < until) {
            flushSerialCalls();
            Thread.sleep(10);
        }
        return store.writeCount();
    }

    /** Anything that counts the writes it was handed. */
    private interface WriteCounter {
        int writeCount();
    }

    /** A store that refuses every write and counts the attempts. */
    private static final class CountingStore
            extends com.codename1.health.HealthStore implements WriteCounter {

        /// Per instance, not static. A shared counter let a session
        /// leaked by one test move another test's numbers with nothing
        /// pointing at the leak -- which is what made
        /// aFailedSessionStopsRetryingItsWrites fail in CI while passing
        /// on its own.
        int writes;

        public int writeCount() {
            return writes;
        }

        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public boolean isTypeSupported(
                com.codename1.health.HealthDataType type) {
            return true;
        }

        @Override
        public boolean isWritable(
                com.codename1.health.HealthDataType type) {
            return true;
        }

        @Override
        protected void doWrite(
                java.util.List<com.codename1.health.HealthSample> samples,
                AsyncResource<com.codename1.health.HealthWriteResult> out) {
            writes++;
            out.error(new com.codename1.health.HealthException(
                    com.codename1.health.HealthError.UNAUTHORIZED,
                    "scripted permanent refusal"));
        }
    }

    /**
     * Samples of a type the store will never accept are dropped, not
     * retried.
     *
     * <p>An Android cycling-power or cadence session produces types
     * Health Connect can read but not write. Shared validation rejects
     * the batch before it reaches the store, so the retry resent the
     * identical batch for as long as the session streamed -- an error
     * every {@code storeBatchMillis}, for ever, and a buffer that only
     * grew. Counted through the error callback rather than through store
     * writes, because a refused batch never reaches the store at all.</p>
     */
    @Test
    void permanentlyUnwritableSamplesAreDroppedRatherThanRetried()
            throws Exception {
        RefusingStore store = new RefusingStore();
        com.codename1.health.Health health =
                new com.codename1.health.Health() {
                    @Override
                    public boolean isSupported() {
                        return true;
                    }

                    @Override
                    public com.codename1.health.HealthStore getStore() {
                        return store;
                    }
                };
        implementation.setHealth(health);
        try {
            FakePeripheral p = new FakePeripheral();
            BleSensorSession session = new BleSensorSession("fake",
                    HealthSensorProfile.HEART_RATE,
                    new SensorSessionOptions().setWriteToStore(true)
                            .setStoreBatchMillis(10), p);
            started.add(session);
            final int[] errors = new int[1];
            AsyncResource<SensorSession> out =
                    new AsyncResource<SensorSession>();
            session.start(out);
            flushSerialCalls();
            session.addListener(new SensorSampleListener() {
                public void sensorSample(SensorSession s,
                        com.codename1.health.HealthSample sample) {
                }

                public void sensorStateChanged(SensorSession s,
                        SensorSessionState state) {
                }

                public void sensorError(SensorSession s,
                        com.codename1.health.HealthException error) {
                    errors[0]++;
                }
            });
            p.notifyHeartRate(72);
            flushSerialCalls();

            pump(300);
            int afterFirstRound = errors[0];
            assertTrue(afterFirstRound > 0,
                    "the refused batch should have reported an error");
            pump(300);
            assertEquals(afterFirstRound, errors[0],
                    "a type the store refuses outright must not be"
                            + " retried; it kept failing");
        } finally {
            implementation.setHealth(null);
        }
    }

    /// Sessions any test started, stopped before the next test runs.
    ///
    /// A session left streaming keeps its flush timer, and the timer
    /// resolves the store through Health.getInstance() at flush time --
    /// so a session leaked by one test writes into the *next* test's
    /// store and moves its counts. That is what broke
    /// aFailedSessionStopsRetryingItsWrites in CI while it passed alone:
    /// the write it saw after the session ended was not its own session's.
    private final List<SensorSession> started =
            new ArrayList<SensorSession>();

    @org.junit.jupiter.api.AfterEach
    void stopStartedSessions() {
        for (SensorSession s : started) {
            try {
                s.stop();
            } catch (Throwable ignored) {
                // A test may already have torn it down.
            }
        }
        started.clear();
        flushSerialCalls();
    }

    /** Pumps the EDT for a while so timers can fire. */
    private void pump(long millis) throws Exception {
        long until = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < until) {
            flushSerialCalls();
            Thread.sleep(10);
        }
    }

    /** A store that supports the type for reading but never for writing. */
    private static final class RefusingStore
            extends com.codename1.health.HealthStore {

        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public boolean isTypeSupported(
                com.codename1.health.HealthDataType type) {
            return true;
        }

        @Override
        public boolean isWritable(
                com.codename1.health.HealthDataType type) {
            return false;
        }

        @Override
        protected void doWrite(
                java.util.List<com.codename1.health.HealthSample> samples,
                AsyncResource<com.codename1.health.HealthWriteResult> out) {
            out.error(new com.codename1.health.HealthException(
                    com.codename1.health.HealthError.TYPE_NOT_SUPPORTED,
                    "not writable here"));
        }
    }

    /**
     * Repeated connect failures are bounded, like discovery ones.
     *
     * <p>A failed {@code connect()} publishes DISCONNECTED, the reconnect
     * listener fires on that transition and reconnects at once, so a
     * sensor that has gone for good span the ladder at full speed --
     * forever, because only discovery and subscribe failures were
     * counting toward the limit.</p>
     */
    @Test
    void repeatedConnectFailuresRetireTheSession() {
        FakePeripheral p = new FakePeripheral();
        BleSensorSession session = start(p);
        assertEquals(SensorSessionState.STREAMING, session.getState());

        p.failConnects = 99;
        p.dropLink();

        pumpUntil(session, SensorSessionState.FAILED);
        assertTrue(session.isTerminal());
        assertTrue(p.connects <= 6,
                "the ladder should be bounded, saw " + p.connects
                        + " connect attempts");
    }

    /**
     * A store that commits its first chunk and then fails, so a caller
     * that requeues the whole batch writes the committed part twice.
     */
    private static final class HalfCommittingStore
            extends com.codename1.health.HealthStore {

        /** Every value handed to doWrite, across every attempt. */
        final java.util.List<Double> written = new ArrayList<Double>();

        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public boolean isTypeSupported(
                com.codename1.health.HealthDataType type) {
            return true;
        }

        @Override
        public boolean isWritable(
                com.codename1.health.HealthDataType type) {
            return true;
        }

        /** One record per chunk, so the batch splits sample by sample. */
        @Override
        public int getMaxWriteBatchSize() {
            return 1;
        }

        @Override
        protected void doWrite(
                java.util.List<com.codename1.health.HealthSample> samples,
                AsyncResource<com.codename1.health.HealthWriteResult> out) {
            com.codename1.health.QuantitySample q =
                    (com.codename1.health.QuantitySample) samples.get(0);
            double bpm = q.getQuantity().getValue(
                    com.codename1.health.HealthUnit.COUNT_PER_MINUTE);
            written.add(Double.valueOf(bpm));
            if (bpm >= 71) {
                // The later sample always fails, so the earlier one is
                // always a committed prefix of a failed write.
                out.error(new com.codename1.health.HealthException(
                        com.codename1.health.HealthError.DATABASE_INACCESSIBLE,
                        "scripted failure on the second chunk"));
                return;
            }
            com.codename1.health.HealthWriteResult r =
                    new com.codename1.health.HealthWriteResult();
            r.addSampleId("committed-" + written.size());
            out.complete(r);
        }

        int timesWritten(double bpm) {
            int n = 0;
            for (Double d : written) {
                if (d.doubleValue() == bpm) {
                    n++;
                }
            }
            return n;
        }
    }

    /**
     * A retry after a partly-successful write does not resend what was
     * already committed.
     *
     * <p>A buffered batch larger than the platform's chunk can fail on a
     * later chunk with the earlier ones already in the store.
     * {@code getPartialResult()} names those, but the retry rebuilt its
     * list from the whole batch -- so every retry wrote the committed
     * prefix again, and a session that kept streaming kept duplicating
     * it. Asserted as an invariant rather than a count: the committed
     * sample is written exactly once no matter how often the tail is
     * retried.</p>
     */
    @Test
    void aRetryDoesNotResendTheCommittedPrefix() throws Exception {
        final HalfCommittingStore store = new HalfCommittingStore();
        implementation.setHealth(new com.codename1.health.Health() {
            @Override
            public boolean isSupported() {
                return true;
            }

            @Override
            public com.codename1.health.HealthStore getStore() {
                return store;
            }
        });
        try {
            FakePeripheral p = new FakePeripheral();
            BleSensorSession session = new BleSensorSession("fake",
                    HealthSensorProfile.HEART_RATE,
                    // Long enough that two notifications issued back to
                    // back are unambiguously one batch, rather than
                    // relying on them landing inside a few milliseconds.
                    new SensorSessionOptions().setWriteToStore(true)
                            .setStoreBatchMillis(400), p);
            started.add(session);
            AsyncResource<SensorSession> out =
                    new AsyncResource<SensorSession>();
            session.start(out);
            flushSerialCalls();

            p.notifyHeartRate(70);
            p.notifyHeartRate(71);
            flushSerialCalls();

            pump(2000);

            assertTrue(store.timesWritten(71) > 1,
                    "the uncommitted sample must keep being retried, or"
                            + " this proves nothing");
            assertEquals(1, store.timesWritten(70),
                    "the committed sample must never be written again");
        } finally {
            implementation.setHealth(null);
        }
    }

    /**
     * A session that gives up flushes exactly once, and never again.
     *
     * <p>Two things have to hold at once here, and an earlier version of
     * this test asserted only the second by demanding no write at all.
     * The buffer must reach the store -- a session that exhausted its
     * reconnects still holds whatever arrived since the last batch
     * boundary, up to a full {@code storeBatchMillis} of it -- and the
     * timer that was armed for that batch must not then fire a second
     * write from a session nobody holds.</p>
     */
    @Test
    void aSessionThatGivesUpFlushesOnceAndNeverAgain() throws Exception {
        final CountingStore store = new CountingStore();
        implementation.setHealth(new com.codename1.health.Health() {
            @Override
            public boolean isSupported() {
                return true;
            }

            @Override
            public com.codename1.health.HealthStore getStore() {
                return store;
            }
        });
        try {
            FakePeripheral p = new FakePeripheral();
            BleSensorSession session = new BleSensorSession("fake",
                    HealthSensorProfile.HEART_RATE,
                    // Long enough that nothing is written on the timer:
                    // any write this test sees is the teardown's.
                    new SensorSessionOptions().setWriteToStore(true)
                            .setStoreBatchMillis(600), p);
            started.add(session);
            AsyncResource<SensorSession> out =
                    new AsyncResource<SensorSession>();
            session.start(out);
            flushSerialCalls();

            p.notifyHeartRate(70);
            flushSerialCalls();
            p.failDiscoveries = 99;
            p.dropLink();
            pumpUntil(session, SensorSessionState.FAILED);

            assertEquals(1, pumpFor(1500, store),
                    "the buffered reading must reach the store when the"
                            + " session gives up");
            assertEquals(1, pumpFor(800, store),
                    "and the timer armed for that batch must not fire a"
                            + " second write afterwards");
        } finally {
            implementation.setHealth(null);
        }
    }

    /**
     * Tearing a session down twice does not write twice.
     *
     * <p>{@code endSession()} and {@code stop()} both flush, and a failed
     * flush used to requeue what it could not write -- even from a
     * session that had already ended, which could never flush it again.
     * So the buffer refilled and the next teardown sent it. That is one
     * more write from a dead session, and it survived both the terminal
     * state check and making the buffer claim atomic, because neither of
     * them is on this path.</p>
     *
     * <p>Deterministic where the reconnect test was not: the second
     * teardown is this test calling {@code stop()}, not a timer.</p>
     */
    @Test
    void tearingDownTwiceDoesNotWriteTwice() throws Exception {
        CountingStore store = new CountingStore();
        implementation.setHealth(new com.codename1.health.Health() {
            @Override
            public boolean isSupported() {
                return true;
            }

            @Override
            public com.codename1.health.HealthStore getStore() {
                return store;
            }
        });
        try {
            FakePeripheral p = new FakePeripheral();
            BleSensorSession session = new BleSensorSession("fake",
                    HealthSensorProfile.HEART_RATE,
                    new SensorSessionOptions().setWriteToStore(true)
                            .setStoreBatchMillis(10), p);
            started.add(session);
            AsyncResource<SensorSession> out =
                    new AsyncResource<SensorSession>();
            session.start(out);
            flushSerialCalls();
            p.notifyHeartRate(72);
            flushSerialCalls();

            p.failDiscoveries = 99;
            p.dropLink();
            pumpUntil(session, SensorSessionState.FAILED);
            int settled = pumpFor(400, store);

            // The second teardown, by hand.
            session.stop();
            assertEquals(SensorSessionState.FAILED, session.getState(),
                    "stopping a failed session must not rewrite how it"
                            + " ended");
            assertEquals(settled, pumpFor(400, store),
                    "a second teardown must not issue another write");
        } finally {
            implementation.setHealth(null);
        }
    }

    /**
     * A reserved battery level is not reported as a percentage.
     *
     * <p>The Battery Level characteristic reserves everything above 100,
     * and a peripheral answering {@code 0xFF} for "unknown" was handed to
     * the app as a 255% battery.</p>
     */
    @Test
    void aReservedBatteryLevelLeavesTheBatteryUnknown() throws Exception {
        FakePeripheral p = new FakePeripheral();
        p.batteryLevel = (byte) 0xFF;
        BleSensorSession session = new BleSensorSession("fake",
                HealthSensorProfile.HEART_RATE,
                new SensorSessionOptions(), p);
        started.add(session);
        AsyncResource<SensorSession> out =
                new AsyncResource<SensorSession>();
        session.start(out);
        flushSerialCalls();

        assertNull(session.getBatteryPercent(),
                "255 is reserved, not a percentage");

        FakePeripheral ok = new FakePeripheral();
        ok.batteryLevel = (byte) 72;
        BleSensorSession good = new BleSensorSession("fake2",
                HealthSensorProfile.HEART_RATE,
                new SensorSessionOptions(), ok);
        started.add(good);
        good.start(new AsyncResource<SensorSession>());
        flushSerialCalls();
        assertEquals(Integer.valueOf(72), good.getBatteryPercent(),
                "and an ordinary level is still reported");
    }
}
