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
package com.codename1.impl.javase.bluetooth;

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.gatt.GattStatus;
import com.codename1.bluetooth.le.server.GattLocalCharacteristic;
import com.codename1.bluetooth.le.server.GattLocalService;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The engine of the JavaSE simulator's virtual Bluetooth world. Pure Java:
 * no UI, no Swing, no Codename One {@code Display} -- remote devices are
 * {@link VirtualPeripheral} models, transports are piped streams and time
 * exists only through the single {@link SimScheduler} the stack was
 * constructed with. Every state mutation and every callback runs on that
 * scheduler, which makes the whole simulation deterministic under a
 * {@link ManualScheduler} and thread-confined under an
 * {@link AutoScheduler}.
 *
 * <p>Asynchronous completions are delayed by a configurable latency
 * (default {@value #DEFAULT_LATENCY_MILLIS}ms) so races that would only
 * show on a real radio also show in the simulator. Failures are scripted
 * per operation key via
 * {@link #failNext(String, BluetoothError, String)}.</p>
 *
 * <p>The stack simulates both directions:</p>
 * <ul>
 *   <li><b>App as central/client</b> -- scanning, connect/disconnect,
 *       GATT client operations, L2CAP channels and RFCOMM connections
 *       against registered {@link VirtualPeripheral}s.</li>
 *   <li><b>App as peripheral/server</b> -- a GATT server registry with
 *       {@link VirtualCentral} counterparts, advertising state, published
 *       L2CAP PSMs and RFCOMM listeners with virtual clients.</li>
 * </ul>
 *
 * <p>Application code normally goes through {@link BluetoothSimulator},
 * a thin static facade over one shared instance; tests construct their own
 * stack with a {@link ManualScheduler}.</p>
 */
public final class SimulatedBluetoothStack {

    /** The default artificial completion latency in milliseconds. */
    public static final long DEFAULT_LATENCY_MILLIS = 10;

    // ------------------------------------------------------------------
    // callback interfaces
    // ------------------------------------------------------------------

    /** Plain completion callback of an asynchronous stack operation. */
    public interface Callback<T> {
        void onSuccess(T value);

        void onError(BluetoothError error, String message);
    }

    /** Receives the sightings of a scan feed. */
    public interface ScanFeedSink {
        /**
         * One advertisement sighting.
         *
         * @param timestamp a stack-provided monotonic counter value --
         * NOT wall-clock time
         */
        void onSighting(VirtualPeripheral peripheral, long timestamp);

        void onScanFailed(BluetoothError error, String message);
    }

    /** Receives the sightings of a classic discovery run. */
    public interface ClassicDiscoverySink {
        void onSighting(VirtualPeripheral peripheral);

        /** The inquiry finished after reporting every classic device. */
        void onComplete();

        void onFailed(BluetoothError error, String message);
    }

    /** Remote-initiated events of one connected peripheral. */
    public interface PeripheralSink {
        void onNotification(BluetoothUuid serviceUuid,
                BluetoothUuid characteristicUuid, byte[] value);

        void onConnectionLost(BluetoothError error, String message);
    }

    /** Observes simulated adapter on/off transitions. */
    public interface AdapterListener {
        void adapterEnabledChanged(boolean enabled);
    }

    /**
     * The app-side GATT server's view of its virtual centrals -- wired by
     * the port adapter to the core {@code GattServer} fire helpers.
     */
    public interface AppServerSink {
        void centralConnected(String centralAddress);

        void centralDisconnected(String centralAddress);

        void subscriptionChanged(String centralAddress,
                GattLocalCharacteristic characteristic, boolean subscribed);

        void characteristicReadRequest(AppReadRequest request);

        void characteristicWriteRequest(AppWriteRequest request);
    }

    /**
     * A virtual central's read of an app-served characteristic without a
     * static value. Answer exactly once via {@link #respond(byte[])} or
     * {@link #reject(GattStatus)}; both are safe from any thread.
     */
    public static final class AppReadRequest {
        private final SimulatedBluetoothStack stack;
        private final String centralAddress;
        private final GattLocalCharacteristic characteristic;
        private final Callback<byte[]> callback;
        private boolean answered;

        AppReadRequest(SimulatedBluetoothStack stack, String centralAddress,
                GattLocalCharacteristic characteristic,
                Callback<byte[]> callback) {
            this.stack = stack;
            this.centralAddress = centralAddress;
            this.characteristic = characteristic;
            this.callback = callback;
        }

        public String getCentralAddress() {
            return centralAddress;
        }

        public GattLocalCharacteristic getCharacteristic() {
            return characteristic;
        }

        public void respond(byte[] value) {
            final byte[] v = ByteArrays.copy(value);
            if (claim()) {
                stack.postCompletion("centralRead", new Runnable() {
                    public void run() {
                        callback.onSuccess(v);
                    }
                });
            }
        }

        public void reject(GattStatus status) {
            final GattStatus s = status == null
                    ? GattStatus.UNLIKELY_ERROR : status;
            if (claim()) {
                stack.postCompletion("centralRead", new Runnable() {
                    public void run() {
                        callback.onError(BluetoothError.GATT_ERROR,
                                "Read rejected with ATT status " + s);
                    }
                });
            }
        }

        private synchronized boolean claim() {
            if (answered) {
                return false;
            }
            answered = true;
            return true;
        }
    }

    /**
     * A virtual central's write of an app-served characteristic. Answer
     * exactly once via {@link #respond()} or {@link #reject(GattStatus)};
     * both are safe from any thread.
     */
    public static final class AppWriteRequest {
        private final SimulatedBluetoothStack stack;
        private final String centralAddress;
        private final GattLocalCharacteristic characteristic;
        private final byte[] value;
        private final Callback<Boolean> callback;
        private boolean answered;

        AppWriteRequest(SimulatedBluetoothStack stack, String centralAddress,
                GattLocalCharacteristic characteristic, byte[] value,
                Callback<Boolean> callback) {
            this.stack = stack;
            this.centralAddress = centralAddress;
            this.characteristic = characteristic;
            this.value = ByteArrays.copy(value);
            this.callback = callback;
        }

        public String getCentralAddress() {
            return centralAddress;
        }

        public GattLocalCharacteristic getCharacteristic() {
            return characteristic;
        }

        /** The written bytes (a copy). */
        public byte[] getValue() {
            return ByteArrays.copy(value);
        }

        public void respond() {
            if (claim()) {
                stack.postCompletion("centralWrite", new Runnable() {
                    public void run() {
                        callback.onSuccess(Boolean.TRUE);
                    }
                });
            }
        }

        public void reject(GattStatus status) {
            final GattStatus s = status == null
                    ? GattStatus.UNLIKELY_ERROR : status;
            if (claim()) {
                stack.postCompletion("centralWrite", new Runnable() {
                    public void run() {
                        callback.onError(BluetoothError.GATT_ERROR,
                                "Write rejected with ATT status " + s);
                    }
                });
            }
        }

        private synchronized boolean claim() {
            if (answered) {
                return false;
            }
            answered = true;
            return true;
        }
    }

    // ------------------------------------------------------------------
    // internal state
    // ------------------------------------------------------------------

    private static final class ScriptedFailure {
        final BluetoothError error;
        final String message;

        ScriptedFailure(BluetoothError error, String message) {
            this.error = error == null ? BluetoothError.UNKNOWN : error;
            this.message = message;
        }
    }

    private static final class ConnState {
        boolean discovered;
        int mtu = 23;
        final HashSet<String> subscriptions = new HashSet<String>();
    }

    private static final class ScanFeed {
        final ScanFeedSink sink;
        boolean active = true;
        final HashSet<String> emitted = new HashSet<String>();

        ScanFeed(ScanFeedSink sink) {
            this.sink = sink;
        }
    }

    private static final class ClassicFeed {
        final ClassicDiscoverySink sink;
        boolean active = true;

        ClassicFeed(ClassicDiscoverySink sink) {
            this.sink = sink;
        }
    }

    private static final class ServerEndpoint {
        final ArrayDeque<SimStreamChannel> pendingChannels =
                new ArrayDeque<SimStreamChannel>();
        final ArrayDeque<Callback<SimStreamChannel>> pendingAccepts =
                new ArrayDeque<Callback<SimStreamChannel>>();
    }

    static final class CentralState {
        final String address;
        /** subscription key -> notification listener */
        final HashMap<String, VirtualCentral.NotificationListener> listeners =
                new HashMap<String, VirtualCentral.NotificationListener>();

        CentralState(String address) {
            this.address = address;
        }
    }

    private final SimScheduler scheduler;
    private final Object lock = new Object();

    private boolean adapterEnabled = true;
    private long latencyMillis = DEFAULT_LATENCY_MILLIS;
    private long monotonicTimestamp;
    private AdapterListener adapterListener;

    private final LinkedHashMap<String, VirtualPeripheral> peripherals =
            new LinkedHashMap<String, VirtualPeripheral>();
    private final HashMap<String, ConnState> connections =
            new HashMap<String, ConnState>();
    private final HashMap<String, PeripheralSink> peripheralSinks =
            new HashMap<String, PeripheralSink>();
    private final HashSet<String> bonded = new HashSet<String>();
    private final HashMap<String, ArrayDeque<ScriptedFailure>> failures =
            new HashMap<String, ArrayDeque<ScriptedFailure>>();
    private final ArrayList<StackEventListener> eventListeners =
            new ArrayList<StackEventListener>();
    private final ArrayList<ScanFeed> scanFeeds = new ArrayList<ScanFeed>();

    // app-side (peripheral role) registries
    private AppServerSink appServerSink;
    private final ArrayList<GattLocalService> appServices =
            new ArrayList<GattLocalService>();
    private final LinkedHashMap<String, CentralState> virtualCentrals =
            new LinkedHashMap<String, CentralState>();
    private final LinkedHashMap<Object, Object> advertisements =
            new LinkedHashMap<Object, Object>();
    private final HashMap<Integer, ServerEndpoint> l2capServers =
            new HashMap<Integer, ServerEndpoint>();
    private final HashMap<BluetoothUuid, ServerEndpoint> rfcommServers =
            new HashMap<BluetoothUuid, ServerEndpoint>();
    private final HashMap<BluetoothUuid, SimStreamHandler> rfcommEndpoints =
            new HashMap<BluetoothUuid, SimStreamHandler>();
    private final ArrayList<SimStreamChannel> openChannels =
            new ArrayList<SimStreamChannel>();
    private int nextPsm = 0x81;
    private int nextCentralId = 1;

    public SimulatedBluetoothStack(SimScheduler scheduler) {
        if (scheduler == null) {
            throw new IllegalArgumentException("scheduler is required");
        }
        this.scheduler = scheduler;
    }

    /** The scheduler this stack runs on. */
    public SimScheduler getScheduler() {
        return scheduler;
    }

    // ------------------------------------------------------------------
    // plumbing helpers
    // ------------------------------------------------------------------

    private void run(Runnable r) {
        scheduler.post(r);
    }

    /** Schedules a completion callback after the configured latency. */
    private void completeLater(Runnable r) {
        long latency;
        synchronized (lock) {
            latency = latencyMillis;
        }
        scheduler.postDelayed(r, latency);
    }

    /** Latency-delayed completion used by the request objects. */
    void postCompletion(String op, Runnable r) {
        fireEvent(op, "response");
        completeLater(r);
    }

    private void fireEvent(String op, String detail) {
        Object[] snapshot;
        synchronized (lock) {
            if (eventListeners.isEmpty()) {
                return;
            }
            snapshot = eventListeners.toArray();
        }
        for (int i = 0; i < snapshot.length; i++) {
            ((StackEventListener) snapshot[i]).event(op, detail);
        }
    }

    private static <T> void succeedNow(Callback<T> cb, T value) {
        if (cb != null) {
            cb.onSuccess(value);
        }
    }

    private <T> void succeed(final Callback<T> cb, final T value) {
        completeLater(new Runnable() {
            public void run() {
                succeedNow(cb, value);
            }
        });
    }

    private <T> void fail(final Callback<T> cb, final BluetoothError error,
            final String message) {
        completeLater(new Runnable() {
            public void run() {
                if (cb != null) {
                    cb.onError(error, message);
                }
            }
        });
    }

    /**
     * Consumes and applies a scripted failure for the op key when one is
     * queued. Must run on the scheduler thread.
     */
    private <T> boolean failScripted(String op, Callback<T> cb) {
        ScriptedFailure f;
        synchronized (lock) {
            ArrayDeque<ScriptedFailure> q = failures.get(op);
            f = q == null ? null : q.pollFirst();
        }
        if (f == null) {
            return false;
        }
        fireEvent(op, "scripted failure: " + f.error
                + (f.message == null ? "" : " (" + f.message + ")"));
        fail(cb, f.error, f.message);
        return true;
    }

    private long nextTimestamp() {
        synchronized (lock) {
            return ++monotonicTimestamp;
        }
    }

    private static String subKey(BluetoothUuid serviceUuid,
            BluetoothUuid characteristicUuid) {
        return serviceUuid + "|" + characteristicUuid;
    }

    // ------------------------------------------------------------------
    // configuration, scripting and the event log
    // ------------------------------------------------------------------

    /** Registers an event-log listener; effective immediately. */
    public void addEventListener(StackEventListener l) {
        if (l == null) {
            return;
        }
        synchronized (lock) {
            if (!eventListeners.contains(l)) {
                eventListeners.add(l);
            }
        }
    }

    /** Removes an event-log listener. */
    public void removeEventListener(StackEventListener l) {
        synchronized (lock) {
            eventListeners.remove(l);
        }
    }

    /** Writes a free-form entry to the event log (via the scheduler). */
    public void logEvent(final String op, final String detail) {
        run(new Runnable() {
            public void run() {
                fireEvent(op, detail);
            }
        });
    }

    /**
     * Sets the artificial latency applied to every asynchronous
     * completion. Ordered with subsequently issued operations.
     */
    public void setLatencyMillis(final long millis) {
        run(new Runnable() {
            public void run() {
                synchronized (lock) {
                    latencyMillis = Math.max(0, millis);
                }
                fireEvent("config", "latency=" + Math.max(0, millis) + "ms");
            }
        });
    }

    /** The current artificial completion latency in milliseconds. */
    public long getLatencyMillis() {
        synchronized (lock) {
            return latencyMillis;
        }
    }

    /**
     * Scripts the next occurrence of the given operation to fail. Multiple
     * calls queue up (FIFO). Operation keys: {@code "connect"},
     * {@code "disconnect"}, {@code "read"}, {@code "write"},
     * {@code "discover"}, {@code "subscribe"}, {@code "scan"},
     * {@code "rssi"}, {@code "mtu"}, {@code "bond"},
     * {@code "rfcommConnect"}, {@code "l2cap"}.
     */
    public void failNext(final String op, final BluetoothError error,
            final String message) {
        if (op == null) {
            return;
        }
        run(new Runnable() {
            public void run() {
                synchronized (lock) {
                    ArrayDeque<ScriptedFailure> q = failures.get(op);
                    if (q == null) {
                        q = new ArrayDeque<ScriptedFailure>();
                        failures.put(op, q);
                    }
                    q.addLast(new ScriptedFailure(error, message));
                }
                fireEvent("script", "failNext " + op + " -> " + error);
            }
        });
    }

    /**
     * Restores pristine state: peripherals, connections, scripted
     * failures, app-side registries and virtual centrals are cleared; the
     * adapter is re-enabled; latency returns to the default; open piped
     * channels are closed and pending accepts fail.
     */
    public void reset() {
        run(new Runnable() {
            public void run() {
                fireEvent("reset", "stack reset");
                ArrayList<SimStreamChannel> channels;
                ArrayList<Callback<SimStreamChannel>> accepts =
                        new ArrayList<Callback<SimStreamChannel>>();
                synchronized (lock) {
                    channels = new ArrayList<SimStreamChannel>(openChannels);
                    openChannels.clear();
                    for (ServerEndpoint ep : l2capServers.values()) {
                        accepts.addAll(ep.pendingAccepts);
                        channels.addAll(ep.pendingChannels);
                    }
                    for (ServerEndpoint ep : rfcommServers.values()) {
                        accepts.addAll(ep.pendingAccepts);
                        channels.addAll(ep.pendingChannels);
                    }
                    l2capServers.clear();
                    rfcommServers.clear();
                    rfcommEndpoints.clear();
                    peripherals.clear();
                    connections.clear();
                    peripheralSinks.clear();
                    bonded.clear();
                    failures.clear();
                    scanFeeds.clear();
                    appServerSink = null;
                    appServices.clear();
                    virtualCentrals.clear();
                    advertisements.clear();
                    adapterEnabled = true;
                    latencyMillis = DEFAULT_LATENCY_MILLIS;
                    adapterListener = null;
                    nextPsm = 0x81;
                    nextCentralId = 1;
                }
                int size = channels.size();
                for (int i = 0; i < size; i++) {
                    channels.get(i).close();
                }
                size = accepts.size();
                for (int i = 0; i < size; i++) {
                    final Callback<SimStreamChannel> cb = accepts.get(i);
                    cb.onError(BluetoothError.IO_ERROR,
                            "Server closed by stack reset");
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // adapter
    // ------------------------------------------------------------------

    /** Turns the simulated adapter on or off. */
    public void setAdapterEnabled(final boolean enabled) {
        run(new Runnable() {
            public void run() {
                AdapterListener l;
                boolean changed;
                synchronized (lock) {
                    changed = adapterEnabled != enabled;
                    adapterEnabled = enabled;
                    l = adapterListener;
                }
                fireEvent("adapter", enabled ? "enabled" : "disabled");
                if (changed && l != null) {
                    l.adapterEnabledChanged(enabled);
                }
            }
        });
    }

    public boolean isAdapterEnabled() {
        synchronized (lock) {
            return adapterEnabled;
        }
    }

    /** Sets the single adapter-state listener (the port adapter). */
    public void setAdapterListener(final AdapterListener listener) {
        run(new Runnable() {
            public void run() {
                synchronized (lock) {
                    adapterListener = listener;
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // peripheral registry
    // ------------------------------------------------------------------

    /**
     * Registers a virtual peripheral. Active scan feeds pick it up
     * immediately (after the configured latency).
     */
    public void addPeripheral(final VirtualPeripheral peripheral) {
        if (peripheral == null) {
            return;
        }
        run(new Runnable() {
            public void run() {
                ArrayList<ScanFeed> feeds = new ArrayList<ScanFeed>();
                synchronized (lock) {
                    peripherals.put(peripheral.getAddress(), peripheral);
                    if (peripheral.isBonded()) {
                        bonded.add(peripheral.getAddress());
                    }
                    if (peripheral.isLe()) {
                        for (int i = 0; i < scanFeeds.size(); i++) {
                            ScanFeed f = scanFeeds.get(i);
                            if (f.active) {
                                feeds.add(f);
                            }
                        }
                    }
                }
                fireEvent("registerPeripheral", peripheral.getAddress());
                int size = feeds.size();
                for (int i = 0; i < size; i++) {
                    scheduleSighting(feeds.get(i), peripheral, 1);
                }
            }
        });
    }

    /** Removes a peripheral; an existing connection is dropped silently. */
    public void removePeripheral(final String address) {
        run(new Runnable() {
            public void run() {
                synchronized (lock) {
                    peripherals.remove(address);
                    connections.remove(address);
                    bonded.remove(address);
                }
                fireEvent("removePeripheral", address);
            }
        });
    }

    /** Removes every registered peripheral. */
    public void clearPeripherals() {
        run(new Runnable() {
            public void run() {
                synchronized (lock) {
                    peripherals.clear();
                    connections.clear();
                    bonded.clear();
                }
                fireEvent("clearPeripherals", "");
            }
        });
    }

    /**
     * Replays a recorded trace: every fixture device is registered as a
     * {@link VirtualPeripheral} at its first-sighting time and its RSSI
     * timeline is replayed through the stack's scheduler
     * ({@code postDelayed} with the fixture's relative timestamps -- on a
     * {@link ManualScheduler} that is virtual-clock deterministic).
     * Devices with a captured GATT database become fully connectable
     * peripherals; the rest are advertisement-only. Safe to call again
     * after {@link #reset()}.
     */
    public void loadFixture(final BluetoothFixture fixture) {
        if (fixture == null) {
            return;
        }
        run(new Runnable() {
            public void run() {
                List<BluetoothFixture.Device> devices =
                        fixture.getDevices();
                fireEvent("fixture", "loading " + devices.size()
                        + " device(s)");
                int size = devices.size();
                for (int i = 0; i < size; i++) {
                    BluetoothFixture.Device d = devices.get(i);
                    final VirtualPeripheral p = d.toVirtualPeripheral();
                    List<BluetoothFixture.RssiSample> timeline =
                            d.rssiTimeline;
                    long firstSeen = timeline.isEmpty() ? 0
                            : timeline.get(0).relTimeMs;
                    scheduler.postDelayed(new Runnable() {
                        public void run() {
                            addPeripheral(p);
                        }
                    }, firstSeen);
                    int ts = timeline.size();
                    for (int j = 1; j < ts; j++) {
                        final BluetoothFixture.RssiSample s =
                                timeline.get(j);
                        scheduler.postDelayed(new Runnable() {
                            public void run() {
                                p.setRssi(s.rssi);
                            }
                        }, s.relTimeMs);
                    }
                }
            }
        });
    }

    public boolean isPeripheralRegistered(String address) {
        synchronized (lock) {
            return peripherals.containsKey(address);
        }
    }

    /** The registered peripheral at the address, or {@code null}. */
    public VirtualPeripheral getPeripheral(String address) {
        synchronized (lock) {
            return peripherals.get(address);
        }
    }

    /** The registered addresses in registration order. */
    public List<String> getPeripheralAddresses() {
        synchronized (lock) {
            return new ArrayList<String>(peripherals.keySet());
        }
    }

    // ------------------------------------------------------------------
    // scanning
    // ------------------------------------------------------------------

    /**
     * Opens a scan feed: every registered LE peripheral is emitted once,
     * staggered by the configured latency, and peripherals registered
     * while the feed is active are emitted as they appear. Returns an
     * opaque token for {@link #stopScanFeed(Object)}.
     */
    public Object startScanFeed(final ScanFeedSink sink) {
        final ScanFeed feed = new ScanFeed(sink);
        run(new Runnable() {
            public void run() {
                fireEvent("scan", "start");
                if (failScriptedScan(sink)) {
                    return;
                }
                ArrayList<VirtualPeripheral> toEmit =
                        new ArrayList<VirtualPeripheral>();
                synchronized (lock) {
                    if (!adapterEnabled) {
                        feed.active = false;
                    } else {
                        scanFeeds.add(feed);
                        for (VirtualPeripheral p : peripherals.values()) {
                            if (p.isLe()) {
                                toEmit.add(p);
                            }
                        }
                    }
                }
                if (!feed.active) {
                    fireEvent("scan", "failed: adapter disabled");
                    completeLater(new Runnable() {
                        public void run() {
                            sink.onScanFailed(BluetoothError.POWERED_OFF,
                                    "The Bluetooth adapter is disabled");
                        }
                    });
                    return;
                }
                int size = toEmit.size();
                for (int i = 0; i < size; i++) {
                    scheduleSighting(feed, toEmit.get(i), i + 1);
                }
            }
        });
        return feed;
    }

    private boolean failScriptedScan(final ScanFeedSink sink) {
        ScriptedFailure f;
        synchronized (lock) {
            ArrayDeque<ScriptedFailure> q = failures.get("scan");
            f = q == null ? null : q.pollFirst();
        }
        if (f == null) {
            return false;
        }
        final ScriptedFailure failure = f;
        fireEvent("scan", "scripted failure: " + failure.error);
        completeLater(new Runnable() {
            public void run() {
                sink.onScanFailed(failure.error, failure.message);
            }
        });
        return true;
    }

    private void scheduleSighting(final ScanFeed feed,
            final VirtualPeripheral p, int slot) {
        long latency;
        synchronized (lock) {
            latency = latencyMillis;
        }
        scheduler.postDelayed(new Runnable() {
            public void run() {
                synchronized (lock) {
                    if (!feed.active
                            || !peripherals.containsKey(p.getAddress())
                            || !feed.emitted.add(p.getAddress())) {
                        return;
                    }
                }
                fireEvent("scan", "sighting " + p.getAddress());
                feed.sink.onSighting(p, nextTimestamp());
            }
        }, latency * slot);
    }

    /** Stops a scan feed opened via {@link #startScanFeed(ScanFeedSink)}. */
    public void stopScanFeed(final Object token) {
        run(new Runnable() {
            public void run() {
                if (!(token instanceof ScanFeed)) {
                    return;
                }
                ScanFeed feed = (ScanFeed) token;
                synchronized (lock) {
                    feed.active = false;
                    scanFeeds.remove(feed);
                }
                fireEvent("scan", "stop");
            }
        });
    }

    /**
     * Runs a classic (BR/EDR) discovery: every classic-flagged peripheral
     * is emitted once, staggered by the latency, then the sink completes.
     * Returns an opaque token for {@link #stopClassicDiscovery(Object)}.
     */
    public Object startClassicDiscovery(final ClassicDiscoverySink sink) {
        final ClassicFeed feed = new ClassicFeed(sink);
        run(new Runnable() {
            public void run() {
                fireEvent("scan", "classic discovery start");
                ScriptedFailure f;
                synchronized (lock) {
                    ArrayDeque<ScriptedFailure> q = failures.get("scan");
                    f = q == null ? null : q.pollFirst();
                }
                if (f != null) {
                    final ScriptedFailure failure = f;
                    fireEvent("scan", "scripted failure: " + failure.error);
                    completeLater(new Runnable() {
                        public void run() {
                            sink.onFailed(failure.error, failure.message);
                        }
                    });
                    return;
                }
                final ArrayList<VirtualPeripheral> toEmit =
                        new ArrayList<VirtualPeripheral>();
                boolean enabled;
                synchronized (lock) {
                    enabled = adapterEnabled;
                    if (enabled) {
                        for (VirtualPeripheral p : peripherals.values()) {
                            if (p.isClassic()) {
                                toEmit.add(p);
                            }
                        }
                    }
                }
                if (!enabled) {
                    completeLater(new Runnable() {
                        public void run() {
                            sink.onFailed(BluetoothError.POWERED_OFF,
                                    "The Bluetooth adapter is disabled");
                        }
                    });
                    return;
                }
                long latency;
                synchronized (lock) {
                    latency = latencyMillis;
                }
                int size = toEmit.size();
                for (int i = 0; i < size; i++) {
                    final VirtualPeripheral p = toEmit.get(i);
                    scheduler.postDelayed(new Runnable() {
                        public void run() {
                            if (feed.active) {
                                fireEvent("scan",
                                        "classic sighting " + p.getAddress());
                                sink.onSighting(p);
                            }
                        }
                    }, latency * (i + 1));
                }
                scheduler.postDelayed(new Runnable() {
                    public void run() {
                        if (feed.active) {
                            feed.active = false;
                            fireEvent("scan", "classic discovery complete");
                            sink.onComplete();
                        }
                    }
                }, latency * (size + 1));
            }
        });
        return feed;
    }

    /** Aborts a running classic discovery; the sink stops firing. */
    public void stopClassicDiscovery(final Object token) {
        run(new Runnable() {
            public void run() {
                if (token instanceof ClassicFeed) {
                    ((ClassicFeed) token).active = false;
                    fireEvent("scan", "classic discovery stop");
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // central-role connection + GATT client
    // ------------------------------------------------------------------

    /**
     * Registers the sink receiving remote-initiated events (notifications,
     * connection loss) of the given peripheral. Survives connect cycles;
     * cleared by {@link #reset()}.
     */
    public void setPeripheralSink(final String address,
            final PeripheralSink sink) {
        run(new Runnable() {
            public void run() {
                synchronized (lock) {
                    if (sink == null) {
                        peripheralSinks.remove(address);
                    } else {
                        peripheralSinks.put(address, sink);
                    }
                }
            }
        });
    }

    /** Connects to a registered, connectable peripheral. */
    public void connect(final String address, final Callback<Boolean> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("connect", address);
                if (failScripted("connect", cb)) {
                    return;
                }
                VirtualPeripheral p;
                boolean enabled;
                synchronized (lock) {
                    enabled = adapterEnabled;
                    p = peripherals.get(address);
                }
                if (!enabled) {
                    fail(cb, BluetoothError.POWERED_OFF,
                            "The Bluetooth adapter is disabled");
                    return;
                }
                if (p == null) {
                    fail(cb, BluetoothError.CONNECTION_FAILED,
                            "No such peripheral: " + address);
                    return;
                }
                if (!p.isConnectable()) {
                    fail(cb, BluetoothError.CONNECTION_FAILED,
                            "Peripheral is not connectable: " + address);
                    return;
                }
                synchronized (lock) {
                    if (!connections.containsKey(address)) {
                        connections.put(address, new ConnState());
                    }
                }
                succeed(cb, Boolean.TRUE);
            }
        });
    }

    /**
     * Disconnects from a peripheral; succeeds silently when not
     * connected (mirroring platform behavior on redundant disconnects).
     */
    public void disconnect(final String address, final Callback<Boolean> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("disconnect", address);
                if (failScripted("disconnect", cb)) {
                    return;
                }
                synchronized (lock) {
                    connections.remove(address);
                }
                succeed(cb, Boolean.TRUE);
            }
        });
    }

    public boolean isConnected(String address) {
        synchronized (lock) {
            return connections.containsKey(address);
        }
    }

    /** The addresses currently connected, in connection order. */
    public List<String> getConnectedAddresses() {
        synchronized (lock) {
            ArrayList<String> out = new ArrayList<String>();
            for (String address : peripherals.keySet()) {
                if (connections.containsKey(address)) {
                    out.add(address);
                }
            }
            return out;
        }
    }

    /** Discovers the GATT database of a connected peripheral. */
    public void discoverServices(final String address,
            final Callback<List<VirtualService>> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("discover", address);
                if (failScripted("discover", cb)) {
                    return;
                }
                ConnState conn = requireConnection(address, cb);
                if (conn == null) {
                    return;
                }
                VirtualPeripheral p;
                synchronized (lock) {
                    p = peripherals.get(address);
                    conn.discovered = true;
                }
                succeed(cb, p == null
                        ? new ArrayList<VirtualService>() : p.getServices());
            }
        });
    }

    /** Reads a characteristic value of a connected peripheral. */
    public void readCharacteristic(final String address,
            final BluetoothUuid serviceUuid,
            final BluetoothUuid characteristicUuid,
            final Callback<byte[]> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("read", address + " " + serviceUuid + "/"
                        + characteristicUuid);
                if (failScripted("read", cb)) {
                    return;
                }
                VirtualCharacteristic c = requireCharacteristic(address,
                        serviceUuid, characteristicUuid, cb);
                if (c == null) {
                    return;
                }
                if (!c.canRead()) {
                    fail(cb, BluetoothError.GATT_ERROR,
                            "Characteristic is not readable: "
                                    + characteristicUuid);
                    return;
                }
                succeed(cb, c.getValue());
            }
        });
    }

    /** Writes a characteristic value of a connected peripheral. */
    public void writeCharacteristic(final String address,
            final BluetoothUuid serviceUuid,
            final BluetoothUuid characteristicUuid, byte[] value,
            final Callback<Boolean> cb) {
        final byte[] v = ByteArrays.copy(value);
        run(new Runnable() {
            public void run() {
                fireEvent("write", address + " " + serviceUuid + "/"
                        + characteristicUuid + " (" + v.length + " bytes)");
                if (failScripted("write", cb)) {
                    return;
                }
                VirtualCharacteristic c = requireCharacteristic(address,
                        serviceUuid, characteristicUuid, cb);
                if (c == null) {
                    return;
                }
                if (!c.canWrite()) {
                    fail(cb, BluetoothError.GATT_ERROR,
                            "Characteristic is not writable: "
                                    + characteristicUuid);
                    return;
                }
                c.setValue(v);
                succeed(cb, Boolean.TRUE);
            }
        });
    }

    /** Reads a descriptor value of a connected peripheral. */
    public void readDescriptor(final String address,
            final BluetoothUuid serviceUuid,
            final BluetoothUuid characteristicUuid,
            final BluetoothUuid descriptorUuid, final Callback<byte[]> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("read", address + " descriptor " + descriptorUuid);
                if (failScripted("read", cb)) {
                    return;
                }
                VirtualDescriptor d = requireDescriptor(address, serviceUuid,
                        characteristicUuid, descriptorUuid, cb);
                if (d == null) {
                    return;
                }
                succeed(cb, d.getValue());
            }
        });
    }

    /** Writes a descriptor value of a connected peripheral. */
    public void writeDescriptor(final String address,
            final BluetoothUuid serviceUuid,
            final BluetoothUuid characteristicUuid,
            final BluetoothUuid descriptorUuid, byte[] value,
            final Callback<Boolean> cb) {
        final byte[] v = ByteArrays.copy(value);
        run(new Runnable() {
            public void run() {
                fireEvent("write", address + " descriptor " + descriptorUuid
                        + " (" + v.length + " bytes)");
                if (failScripted("write", cb)) {
                    return;
                }
                VirtualDescriptor d = requireDescriptor(address, serviceUuid,
                        characteristicUuid, descriptorUuid, cb);
                if (d == null) {
                    return;
                }
                d.setValue(v);
                succeed(cb, Boolean.TRUE);
            }
        });
    }

    /** Arms or disarms notifications for a characteristic. */
    public void setNotifications(final String address,
            final BluetoothUuid serviceUuid,
            final BluetoothUuid characteristicUuid, final boolean enable,
            final Callback<Boolean> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("subscribe", address + " " + characteristicUuid
                        + " enable=" + enable);
                if (failScripted("subscribe", cb)) {
                    return;
                }
                VirtualCharacteristic c = requireCharacteristic(address,
                        serviceUuid, characteristicUuid, cb);
                if (c == null) {
                    return;
                }
                if (enable && !c.canNotifyOrIndicate()) {
                    fail(cb, BluetoothError.GATT_ERROR,
                            "Characteristic supports neither notify nor "
                                    + "indicate: " + characteristicUuid);
                    return;
                }
                synchronized (lock) {
                    ConnState conn = connections.get(address);
                    if (conn != null) {
                        String key = subKey(serviceUuid, characteristicUuid);
                        if (enable) {
                            conn.subscriptions.add(key);
                        } else {
                            conn.subscriptions.remove(key);
                        }
                    }
                }
                succeed(cb, Boolean.TRUE);
            }
        });
    }

    /** {@code true} while the app is subscribed to the characteristic. */
    public boolean isSubscribed(String address, BluetoothUuid serviceUuid,
            BluetoothUuid characteristicUuid) {
        synchronized (lock) {
            ConnState conn = connections.get(address);
            return conn != null && conn.subscriptions.contains(
                    subKey(serviceUuid, characteristicUuid));
        }
    }

    /** Reads the RSSI of a connected peripheral. */
    public void readRssi(final String address, final Callback<Integer> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("rssi", address);
                if (failScripted("rssi", cb)) {
                    return;
                }
                if (requireConnection(address, cb) == null) {
                    return;
                }
                VirtualPeripheral p;
                synchronized (lock) {
                    p = peripherals.get(address);
                }
                succeed(cb, Integer.valueOf(p == null ? -127 : p.getRssi()));
            }
        });
    }

    /** Negotiates an MTU; grants the request clamped to 23..517. */
    public void requestMtu(final String address, final int requested,
            final Callback<Integer> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("mtu", address + " request=" + requested);
                if (failScripted("mtu", cb)) {
                    return;
                }
                ConnState conn = requireConnection(address, cb);
                if (conn == null) {
                    return;
                }
                int granted = Math.max(23, Math.min(517, requested));
                synchronized (lock) {
                    conn.mtu = granted;
                }
                succeed(cb, Integer.valueOf(granted));
            }
        });
    }

    /** The negotiated MTU of a connection; 23 when not connected. */
    public int getMtu(String address) {
        synchronized (lock) {
            ConnState conn = connections.get(address);
            return conn == null ? 23 : conn.mtu;
        }
    }

    /** Bonds with a registered peripheral. */
    public void bond(final String address, final Callback<Boolean> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("bond", address);
                if (failScripted("bond", cb)) {
                    return;
                }
                boolean known;
                synchronized (lock) {
                    known = peripherals.containsKey(address);
                    if (known) {
                        bonded.add(address);
                    }
                }
                if (!known) {
                    fail(cb, BluetoothError.BOND_FAILED,
                            "No such peripheral: " + address);
                    return;
                }
                succeed(cb, Boolean.TRUE);
            }
        });
    }

    public boolean isBonded(String address) {
        synchronized (lock) {
            return bonded.contains(address);
        }
    }

    /** The bonded addresses, in registration order. */
    public List<String> getBondedAddresses() {
        synchronized (lock) {
            ArrayList<String> out = new ArrayList<String>();
            for (String address : peripherals.keySet()) {
                if (bonded.contains(address)) {
                    out.add(address);
                }
            }
            return out;
        }
    }

    /** Requires a live connection; fails the callback otherwise. */
    private <T> ConnState requireConnection(String address, Callback<T> cb) {
        ConnState conn;
        synchronized (lock) {
            conn = connections.get(address);
        }
        if (conn == null) {
            fail(cb, BluetoothError.NOT_CONNECTED,
                    "Peripheral is not connected: " + address);
        }
        return conn;
    }

    private <T> VirtualCharacteristic requireCharacteristic(String address,
            BluetoothUuid serviceUuid, BluetoothUuid characteristicUuid,
            Callback<T> cb) {
        ConnState conn = requireConnection(address, cb);
        if (conn == null) {
            return null;
        }
        if (!conn.discovered) {
            fail(cb, BluetoothError.GATT_ERROR,
                    "Services were not discovered yet: " + address);
            return null;
        }
        VirtualPeripheral p;
        synchronized (lock) {
            p = peripherals.get(address);
        }
        VirtualCharacteristic c = p == null
                ? null : p.getCharacteristic(serviceUuid, characteristicUuid);
        if (c == null) {
            fail(cb, BluetoothError.GATT_ERROR, "No such characteristic: "
                    + serviceUuid + "/" + characteristicUuid);
        }
        return c;
    }

    private <T> VirtualDescriptor requireDescriptor(String address,
            BluetoothUuid serviceUuid, BluetoothUuid characteristicUuid,
            BluetoothUuid descriptorUuid, Callback<T> cb) {
        VirtualCharacteristic c = requireCharacteristic(address, serviceUuid,
                characteristicUuid, cb);
        if (c == null) {
            return null;
        }
        VirtualDescriptor d = c.getDescriptor(descriptorUuid);
        if (d == null) {
            fail(cb, BluetoothError.GATT_ERROR,
                    "No such descriptor: " + descriptorUuid);
        }
        return d;
    }

    // ------------------------------------------------------------------
    // remote-initiated events
    // ------------------------------------------------------------------

    /**
     * Pushes a notification from a virtual peripheral to the app. Only
     * delivered while the app is connected and subscribed.
     */
    public void pushNotification(final String address,
            final BluetoothUuid serviceUuid,
            final BluetoothUuid characteristicUuid, byte[] value) {
        final byte[] v = ByteArrays.copy(value);
        run(new Runnable() {
            public void run() {
                final PeripheralSink sink;
                boolean subscribed;
                synchronized (lock) {
                    ConnState conn = connections.get(address);
                    subscribed = conn != null && conn.subscriptions.contains(
                            subKey(serviceUuid, characteristicUuid));
                    sink = peripheralSinks.get(address);
                }
                if (!subscribed || sink == null) {
                    fireEvent("notification", address + " "
                            + characteristicUuid + " dropped (no subscriber)");
                    return;
                }
                fireEvent("notification", address + " " + characteristicUuid
                        + " (" + v.length + " bytes)");
                completeLater(new Runnable() {
                    public void run() {
                        sink.onNotification(serviceUuid, characteristicUuid,
                                v);
                    }
                });
            }
        });
    }

    /**
     * Simulates the remote peripheral dropping the link: connection state
     * is cleared and the peripheral's sink observes a connection loss.
     */
    public void disconnectFromRemote(final String address) {
        run(new Runnable() {
            public void run() {
                final PeripheralSink sink;
                boolean wasConnected;
                synchronized (lock) {
                    wasConnected = connections.remove(address) != null;
                    sink = peripheralSinks.get(address);
                }
                fireEvent("connectionLost", address
                        + (wasConnected ? "" : " (was not connected)"));
                if (wasConnected && sink != null) {
                    completeLater(new Runnable() {
                        public void run() {
                            sink.onConnectionLost(
                                    BluetoothError.CONNECTION_LOST,
                                    "The remote device dropped the link");
                        }
                    });
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // L2CAP -- app as central
    // ------------------------------------------------------------------

    /**
     * Opens an L2CAP channel to a virtual peripheral's endpoint at the
     * given PSM (registered via
     * {@link VirtualPeripheral#withL2capEndpoint(int, SimStreamHandler)}).
     */
    public void openL2capChannel(final String address, final int psm,
            final Callback<SimStreamChannel> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("l2cap", "open " + address + " psm=" + psm);
                if (failScripted("l2cap", cb)) {
                    return;
                }
                boolean enabled;
                VirtualPeripheral p;
                synchronized (lock) {
                    enabled = adapterEnabled;
                    p = peripherals.get(address);
                }
                if (!enabled) {
                    fail(cb, BluetoothError.POWERED_OFF,
                            "The Bluetooth adapter is disabled");
                    return;
                }
                final SimStreamHandler handler =
                        p == null ? null : p.getL2capEndpoint(psm);
                if (handler == null) {
                    fail(cb, BluetoothError.IO_ERROR,
                            "No L2CAP endpoint at PSM " + psm + " on "
                                    + address);
                    return;
                }
                SimStreamChannel[] pair = SimStreamChannel.createPair();
                final SimStreamChannel local = pair[0];
                final SimStreamChannel remote = pair[1];
                trackChannels(pair);
                completeLater(new Runnable() {
                    public void run() {
                        handler.onConnection(remote);
                        succeedNow(cb, local);
                    }
                });
            }
        });
    }

    private void trackChannels(SimStreamChannel[] pair) {
        synchronized (lock) {
            openChannels.add(pair[0]);
            openChannels.add(pair[1]);
        }
    }

    // ------------------------------------------------------------------
    // app-side GATT server + virtual centrals
    // ------------------------------------------------------------------

    /**
     * Opens the app's GATT server. The sink receives virtual-central
     * events; any previously open server is replaced.
     */
    public void openAppGattServer(final AppServerSink sink,
            final Callback<Boolean> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("gattServer", "open");
                synchronized (lock) {
                    appServerSink = sink;
                    appServices.clear();
                }
                succeed(cb, Boolean.TRUE);
            }
        });
    }

    /** Publishes a service definition on the app's GATT server. */
    public void addAppService(final GattLocalService service,
            final Callback<Boolean> cb) {
        run(new Runnable() {
            public void run() {
                if (service == null) {
                    fail(cb, BluetoothError.UNKNOWN, "service is required");
                    return;
                }
                fireEvent("gattServer", "addService " + service.getUuid());
                synchronized (lock) {
                    appServices.add(service);
                }
                succeed(cb, Boolean.TRUE);
            }
        });
    }

    /** Removes a previously added app service. */
    public void removeAppService(final GattLocalService service) {
        run(new Runnable() {
            public void run() {
                boolean removed;
                synchronized (lock) {
                    removed = appServices.remove(service);
                }
                fireEvent("gattServer", "removeService "
                        + (service == null ? "null" : service.getUuid())
                        + (removed ? "" : " (not registered)"));
            }
        });
    }

    /** The app's published service definitions (a snapshot). */
    public List<GattLocalService> getAppServices() {
        synchronized (lock) {
            return new ArrayList<GattLocalService>(appServices);
        }
    }

    /**
     * Closes the app's GATT server: virtual centrals are disconnected and
     * the sink is released.
     */
    public void closeAppGattServer() {
        run(new Runnable() {
            public void run() {
                final AppServerSink sink;
                final ArrayList<String> centrals;
                synchronized (lock) {
                    sink = appServerSink;
                    centrals = new ArrayList<String>(virtualCentrals.keySet());
                    appServerSink = null;
                    appServices.clear();
                    virtualCentrals.clear();
                }
                fireEvent("gattServer", "close");
                if (sink != null) {
                    int size = centrals.size();
                    for (int i = 0; i < size; i++) {
                        final String address = centrals.get(i);
                        completeLater(new Runnable() {
                            public void run() {
                                sink.centralDisconnected(address);
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * Pushes a value change of an app-served characteristic to subscribed
     * virtual centrals ({@code centralAddress == null}) or to one central.
     */
    public void notifyAppValue(final GattLocalCharacteristic characteristic,
            byte[] value, final String centralAddress,
            final Callback<Boolean> cb) {
        final byte[] v = ByteArrays.copy(value);
        run(new Runnable() {
            public void run() {
                if (characteristic == null) {
                    fail(cb, BluetoothError.UNKNOWN,
                            "characteristic is required");
                    return;
                }
                BluetoothUuid serviceUuid =
                        findAppServiceUuid(characteristic);
                if (serviceUuid == null) {
                    fail(cb, BluetoothError.GATT_ERROR,
                            "Characteristic is not part of a published "
                                    + "service: " + characteristic.getUuid());
                    return;
                }
                final BluetoothUuid svc = serviceUuid;
                final BluetoothUuid chr = characteristic.getUuid();
                String key = subKey(svc, chr);
                int delivered = 0;
                synchronized (lock) {
                    for (CentralState central : virtualCentrals.values()) {
                        if (centralAddress != null
                                && !centralAddress.equals(central.address)) {
                            continue;
                        }
                        final VirtualCentral.NotificationListener l =
                                central.listeners.get(key);
                        if (l == null) {
                            continue;
                        }
                        delivered++;
                        completeLater(new Runnable() {
                            public void run() {
                                l.valueChanged(svc, chr, v);
                            }
                        });
                    }
                }
                fireEvent("notifyValue", chr + " (" + v.length
                        + " bytes) to " + delivered + " central(s)");
                succeed(cb, Boolean.TRUE);
            }
        });
    }

    /** Must run on the scheduler thread or under external quiescence. */
    private BluetoothUuid findAppServiceUuid(
            GattLocalCharacteristic characteristic) {
        ArrayList<GattLocalService> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<GattLocalService>(appServices);
        }
        int size = snapshot.size();
        for (int i = 0; i < size; i++) {
            GattLocalService s = snapshot.get(i);
            List<GattLocalCharacteristic> chars = s.getCharacteristics();
            int cs = chars.size();
            for (int j = 0; j < cs; j++) {
                GattLocalCharacteristic c = chars.get(j);
                if (c == characteristic || (c.getUuid() != null
                        && c.getUuid().equals(characteristic.getUuid()))) {
                    return s.getUuid();
                }
            }
        }
        return null;
    }

    /** Finds an app-served characteristic by service/characteristic UUID. */
    GattLocalCharacteristic findAppCharacteristic(BluetoothUuid serviceUuid,
            BluetoothUuid characteristicUuid) {
        ArrayList<GattLocalService> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<GattLocalService>(appServices);
        }
        int size = snapshot.size();
        for (int i = 0; i < size; i++) {
            GattLocalService s = snapshot.get(i);
            if (!s.getUuid().equals(serviceUuid)) {
                continue;
            }
            List<GattLocalCharacteristic> chars = s.getCharacteristics();
            int cs = chars.size();
            for (int j = 0; j < cs; j++) {
                if (chars.get(j).getUuid().equals(characteristicUuid)) {
                    return chars.get(j);
                }
            }
        }
        return null;
    }

    /**
     * Connects a scripted virtual central to the app's GATT server and
     * returns its handle. The app-side sink observes the connection.
     */
    public VirtualCentral connectVirtualCentral() {
        final String address;
        synchronized (lock) {
            address = String.format("F0:00:00:00:00:%02X",
                    Integer.valueOf(nextCentralId++ & 0xFF));
        }
        final VirtualCentral central = new VirtualCentral(this, address);
        run(new Runnable() {
            public void run() {
                final AppServerSink sink;
                synchronized (lock) {
                    virtualCentrals.put(address, new CentralState(address));
                    sink = appServerSink;
                }
                fireEvent("central", "connected " + address);
                if (sink != null) {
                    completeLater(new Runnable() {
                        public void run() {
                            sink.centralConnected(address);
                        }
                    });
                }
            }
        });
        return central;
    }

    /** The addresses of the connected virtual centrals. */
    public List<String> getConnectedCentralAddresses() {
        synchronized (lock) {
            return new ArrayList<String>(virtualCentrals.keySet());
        }
    }

    // called by VirtualCentral --------------------------------------------

    void centralRead(final String centralAddress,
            final BluetoothUuid serviceUuid,
            final BluetoothUuid characteristicUuid,
            final Callback<byte[]> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("centralRead", centralAddress + " " + serviceUuid
                        + "/" + characteristicUuid);
                if (requireCentral(centralAddress, cb) == null) {
                    return;
                }
                final GattLocalCharacteristic c = findAppCharacteristic(
                        serviceUuid, characteristicUuid);
                if (c == null) {
                    fail(cb, BluetoothError.GATT_ERROR,
                            "No such app characteristic: " + serviceUuid + "/"
                                    + characteristicUuid);
                    return;
                }
                byte[] staticValue = c.getValue();
                if (staticValue != null) {
                    succeed(cb, ByteArrays.copy(staticValue));
                    return;
                }
                final AppServerSink sink;
                synchronized (lock) {
                    sink = appServerSink;
                }
                if (sink == null) {
                    fail(cb, BluetoothError.GATT_ERROR,
                            "The app GATT server is not open");
                    return;
                }
                final AppReadRequest request = new AppReadRequest(
                        SimulatedBluetoothStack.this, centralAddress, c, cb);
                completeLater(new Runnable() {
                    public void run() {
                        sink.characteristicReadRequest(request);
                    }
                });
            }
        });
    }

    void centralWrite(final String centralAddress,
            final BluetoothUuid serviceUuid,
            final BluetoothUuid characteristicUuid, byte[] value,
            final Callback<Boolean> cb) {
        final byte[] v = ByteArrays.copy(value);
        run(new Runnable() {
            public void run() {
                fireEvent("centralWrite", centralAddress + " " + serviceUuid
                        + "/" + characteristicUuid + " (" + v.length
                        + " bytes)");
                if (requireCentral(centralAddress, cb) == null) {
                    return;
                }
                final GattLocalCharacteristic c = findAppCharacteristic(
                        serviceUuid, characteristicUuid);
                if (c == null) {
                    fail(cb, BluetoothError.GATT_ERROR,
                            "No such app characteristic: " + serviceUuid + "/"
                                    + characteristicUuid);
                    return;
                }
                final AppServerSink sink;
                synchronized (lock) {
                    sink = appServerSink;
                }
                if (sink == null) {
                    fail(cb, BluetoothError.GATT_ERROR,
                            "The app GATT server is not open");
                    return;
                }
                final AppWriteRequest request = new AppWriteRequest(
                        SimulatedBluetoothStack.this, centralAddress, c, v,
                        cb);
                completeLater(new Runnable() {
                    public void run() {
                        sink.characteristicWriteRequest(request);
                    }
                });
            }
        });
    }

    void centralSubscribe(final String centralAddress,
            final BluetoothUuid serviceUuid,
            final BluetoothUuid characteristicUuid,
            final VirtualCentral.NotificationListener listener,
            final boolean subscribe, final Callback<Boolean> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("centralSubscribe", centralAddress + " "
                        + characteristicUuid + " subscribe=" + subscribe);
                CentralState central = requireCentral(centralAddress, cb);
                if (central == null) {
                    return;
                }
                final GattLocalCharacteristic c = findAppCharacteristic(
                        serviceUuid, characteristicUuid);
                if (c == null) {
                    fail(cb, BluetoothError.GATT_ERROR,
                            "No such app characteristic: " + serviceUuid + "/"
                                    + characteristicUuid);
                    return;
                }
                String key = subKey(serviceUuid, characteristicUuid);
                final AppServerSink sink;
                boolean changed;
                synchronized (lock) {
                    if (subscribe) {
                        changed = central.listeners.put(key, listener)
                                == null;
                    } else {
                        changed = central.listeners.remove(key) != null;
                    }
                    sink = appServerSink;
                }
                if (changed && sink != null) {
                    completeLater(new Runnable() {
                        public void run() {
                            sink.subscriptionChanged(centralAddress, c,
                                    subscribe);
                        }
                    });
                }
                succeed(cb, Boolean.TRUE);
            }
        });
    }

    void centralDisconnect(final String centralAddress) {
        run(new Runnable() {
            public void run() {
                final AppServerSink sink;
                boolean removed;
                synchronized (lock) {
                    removed = virtualCentrals.remove(centralAddress) != null;
                    sink = appServerSink;
                }
                fireEvent("central", "disconnected " + centralAddress
                        + (removed ? "" : " (was not connected)"));
                if (removed && sink != null) {
                    completeLater(new Runnable() {
                        public void run() {
                            sink.centralDisconnected(centralAddress);
                        }
                    });
                }
            }
        });
    }

    private <T> CentralState requireCentral(String centralAddress,
            Callback<T> cb) {
        CentralState central;
        synchronized (lock) {
            central = virtualCentrals.get(centralAddress);
        }
        if (central == null) {
            fail(cb, BluetoothError.NOT_CONNECTED,
                    "Virtual central is not connected: " + centralAddress);
        }
        return central;
    }

    // ------------------------------------------------------------------
    // advertising (app side)
    // ------------------------------------------------------------------

    /**
     * Registers an advertisement; the payload is an opaque description for
     * the event log / debug UI. The callback receives the stop token.
     */
    public void startAppAdvertising(final Object payload,
            final Callback<Object> cb) {
        final Object token = new Object();
        run(new Runnable() {
            public void run() {
                boolean enabled;
                synchronized (lock) {
                    enabled = adapterEnabled;
                }
                if (!enabled) {
                    fail(cb, BluetoothError.POWERED_OFF,
                            "The Bluetooth adapter is disabled");
                    return;
                }
                synchronized (lock) {
                    advertisements.put(token,
                            payload == null ? "advertisement" : payload);
                }
                fireEvent("advertise", "start "
                        + (payload == null ? "" : payload.toString()));
                succeed(cb, token);
            }
        });
    }

    /** Stops an advertisement by its token. */
    public void stopAppAdvertising(final Object token) {
        run(new Runnable() {
            public void run() {
                boolean removed;
                synchronized (lock) {
                    removed = advertisements.remove(token) != null;
                }
                if (removed) {
                    fireEvent("advertise", "stop");
                }
            }
        });
    }

    /** Stops an advertisement after a delay (advertise timeouts). */
    public void stopAppAdvertisingAfter(final Object token, long millis) {
        scheduler.postDelayed(new Runnable() {
            public void run() {
                stopAppAdvertising(token);
            }
        }, millis);
    }

    /** {@code true} while the given advertisement token is live. */
    public boolean isAppAdvertising(Object token) {
        synchronized (lock) {
            return advertisements.containsKey(token);
        }
    }

    /** {@code true} while any advertisement is live. */
    public boolean isAdvertising() {
        synchronized (lock) {
            return !advertisements.isEmpty();
        }
    }

    /** The live advertisement payloads (a snapshot), for the debug UI. */
    public List<Object> getAdvertisingPayloads() {
        synchronized (lock) {
            return new ArrayList<Object>(advertisements.values());
        }
    }

    // ------------------------------------------------------------------
    // L2CAP -- app as server
    // ------------------------------------------------------------------

    /** Publishes an app L2CAP listener; the callback receives its PSM. */
    public void publishAppL2capServer(final Callback<Integer> cb) {
        run(new Runnable() {
            public void run() {
                int psm;
                synchronized (lock) {
                    psm = nextPsm++;
                    l2capServers.put(Integer.valueOf(psm),
                            new ServerEndpoint());
                }
                fireEvent("l2cap", "listen psm=" + psm);
                succeed(cb, Integer.valueOf(psm));
            }
        });
    }

    /** Accepts the next virtual client on a published PSM. */
    public void acceptAppL2cap(final int psm,
            final Callback<SimStreamChannel> cb) {
        run(new Runnable() {
            public void run() {
                acceptOnEndpoint(l2capEndpoint(psm), "l2cap",
                        "psm=" + psm, cb);
            }
        });
    }

    /** Unpublishes an app L2CAP listener; pending accepts fail. */
    public void closeAppL2capServer(final int psm) {
        run(new Runnable() {
            public void run() {
                ServerEndpoint ep;
                synchronized (lock) {
                    ep = l2capServers.remove(Integer.valueOf(psm));
                }
                fireEvent("l2cap", "close psm=" + psm);
                failPendingAccepts(ep);
            }
        });
    }

    /**
     * Connects a virtual (remote) L2CAP client to the app's published PSM
     * and returns the remote side of the channel immediately. When no
     * listener is published at the PSM the returned channel observes EOF.
     */
    public SimStreamChannel connectVirtualL2capClient(final int psm) {
        SimStreamChannel[] pair = SimStreamChannel.createPair();
        final SimStreamChannel appSide = pair[0];
        final SimStreamChannel remoteSide = pair[1];
        trackChannels(pair);
        run(new Runnable() {
            public void run() {
                fireEvent("l2cap", "virtual client connect psm=" + psm);
                deliverToEndpoint(l2capEndpoint(psm), appSide);
            }
        });
        return remoteSide;
    }

    private ServerEndpoint l2capEndpoint(int psm) {
        synchronized (lock) {
            return l2capServers.get(Integer.valueOf(psm));
        }
    }

    // ------------------------------------------------------------------
    // RFCOMM
    // ------------------------------------------------------------------

    /**
     * Registers a virtual remote RFCOMM endpoint under the given service
     * UUID -- the counterpart the app connects to as a client.
     */
    public void addRfcommEndpoint(final BluetoothUuid serviceUuid,
            final SimStreamHandler handler) {
        run(new Runnable() {
            public void run() {
                synchronized (lock) {
                    if (handler == null) {
                        rfcommEndpoints.remove(serviceUuid);
                    } else {
                        rfcommEndpoints.put(serviceUuid, handler);
                    }
                }
                fireEvent("rfcomm", "endpoint "
                        + (handler == null ? "removed" : "registered")
                        + " " + serviceUuid);
            }
        });
    }

    /** Connects the app (as a client) to a registered RFCOMM endpoint. */
    public void connectRfcomm(final BluetoothUuid serviceUuid,
            final Callback<SimStreamChannel> cb) {
        run(new Runnable() {
            public void run() {
                fireEvent("rfcommConnect", String.valueOf(serviceUuid));
                if (failScripted("rfcommConnect", cb)) {
                    return;
                }
                boolean enabled;
                final SimStreamHandler handler;
                synchronized (lock) {
                    enabled = adapterEnabled;
                    handler = rfcommEndpoints.get(serviceUuid);
                }
                if (!enabled) {
                    fail(cb, BluetoothError.POWERED_OFF,
                            "The Bluetooth adapter is disabled");
                    return;
                }
                if (handler == null) {
                    fail(cb, BluetoothError.IO_ERROR,
                            "No RFCOMM endpoint registered for "
                                    + serviceUuid);
                    return;
                }
                SimStreamChannel[] pair = SimStreamChannel.createPair();
                final SimStreamChannel local = pair[0];
                final SimStreamChannel remote = pair[1];
                trackChannels(pair);
                completeLater(new Runnable() {
                    public void run() {
                        handler.onConnection(remote);
                        succeedNow(cb, local);
                    }
                });
            }
        });
    }

    /** Registers the app's RFCOMM listener under the service UUID. */
    public void listenRfcomm(final BluetoothUuid serviceUuid,
            final Callback<Boolean> cb) {
        run(new Runnable() {
            public void run() {
                boolean conflict;
                synchronized (lock) {
                    conflict = rfcommServers.containsKey(serviceUuid);
                    if (!conflict) {
                        rfcommServers.put(serviceUuid, new ServerEndpoint());
                    }
                }
                fireEvent("rfcomm", "listen " + serviceUuid
                        + (conflict ? " (already listening)" : ""));
                if (conflict) {
                    fail(cb, BluetoothError.BUSY,
                            "An RFCOMM server is already listening on "
                                    + serviceUuid);
                    return;
                }
                succeed(cb, Boolean.TRUE);
            }
        });
    }

    /** Accepts the next virtual client on the app's RFCOMM listener. */
    public void acceptRfcomm(final BluetoothUuid serviceUuid,
            final Callback<SimStreamChannel> cb) {
        run(new Runnable() {
            public void run() {
                acceptOnEndpoint(rfcommEndpointServer(serviceUuid), "rfcomm",
                        String.valueOf(serviceUuid), cb);
            }
        });
    }

    /** Stops the app's RFCOMM listener; pending accepts fail. */
    public void closeRfcommServer(final BluetoothUuid serviceUuid) {
        run(new Runnable() {
            public void run() {
                ServerEndpoint ep;
                synchronized (lock) {
                    ep = rfcommServers.remove(serviceUuid);
                }
                fireEvent("rfcomm", "close " + serviceUuid);
                failPendingAccepts(ep);
            }
        });
    }

    /**
     * Connects a virtual (remote) RFCOMM client to the app's listener and
     * returns the remote side of the channel immediately. When no listener
     * exists for the UUID the returned channel observes EOF.
     */
    public SimStreamChannel connectVirtualRfcommClient(
            final BluetoothUuid serviceUuid) {
        SimStreamChannel[] pair = SimStreamChannel.createPair();
        final SimStreamChannel appSide = pair[0];
        final SimStreamChannel remoteSide = pair[1];
        trackChannels(pair);
        run(new Runnable() {
            public void run() {
                fireEvent("rfcomm", "virtual client connect " + serviceUuid);
                deliverToEndpoint(rfcommEndpointServer(serviceUuid), appSide);
            }
        });
        return remoteSide;
    }

    private ServerEndpoint rfcommEndpointServer(BluetoothUuid serviceUuid) {
        synchronized (lock) {
            return rfcommServers.get(serviceUuid);
        }
    }

    // shared server-endpoint plumbing -----------------------------------

    /** Must run on the scheduler thread. */
    private void acceptOnEndpoint(ServerEndpoint ep, String op, String what,
            final Callback<SimStreamChannel> cb) {
        if (ep == null) {
            fail(cb, BluetoothError.IO_ERROR,
                    "The server is not listening (" + what + ")");
            return;
        }
        final SimStreamChannel queued;
        synchronized (lock) {
            queued = ep.pendingChannels.pollFirst();
            if (queued == null) {
                ep.pendingAccepts.addLast(cb);
            }
        }
        fireEvent(op, "accept " + what
                + (queued == null ? " (waiting)" : ""));
        if (queued != null) {
            succeed(cb, queued);
        }
    }

    /** Must run on the scheduler thread. */
    private void deliverToEndpoint(ServerEndpoint ep,
            SimStreamChannel appSide) {
        if (ep == null) {
            // nobody listening: the virtual client observes EOF at once
            appSide.close();
            return;
        }
        final Callback<SimStreamChannel> waiting;
        synchronized (lock) {
            waiting = ep.pendingAccepts.pollFirst();
            if (waiting == null) {
                ep.pendingChannels.addLast(appSide);
            }
        }
        if (waiting != null) {
            succeed(waiting, appSide);
        }
    }

    /** Must run on the scheduler thread. */
    private void failPendingAccepts(ServerEndpoint ep) {
        if (ep == null) {
            return;
        }
        ArrayList<Callback<SimStreamChannel>> accepts;
        ArrayList<SimStreamChannel> channels;
        synchronized (lock) {
            accepts = new ArrayList<Callback<SimStreamChannel>>(
                    ep.pendingAccepts);
            channels = new ArrayList<SimStreamChannel>(ep.pendingChannels);
            ep.pendingAccepts.clear();
            ep.pendingChannels.clear();
        }
        int size = accepts.size();
        for (int i = 0; i < size; i++) {
            fail(accepts.get(i), BluetoothError.IO_ERROR,
                    "The server was closed");
        }
        size = channels.size();
        for (int i = 0; i < size; i++) {
            channels.get(i).close();
        }
    }
}
