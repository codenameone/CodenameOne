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
package com.codename1.impl.bluetooth;

import com.codename1.bluetooth.AdapterState;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.le.AdvertisementData;
import com.codename1.bluetooth.le.BlePeripheral;
import com.codename1.bluetooth.le.L2capServer;
import com.codename1.bluetooth.le.ScanResult;
import com.codename1.bluetooth.le.server.AdvertiseData;
import com.codename1.bluetooth.le.server.AdvertiseSettings;
import com.codename1.bluetooth.le.server.BleAdvertisement;
import com.codename1.bluetooth.le.server.GattServer;
import com.codename1.bluetooth.le.server.GattServerListener;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/// The real-radio {@link BleBackend}: drives the host machine's Bluetooth
/// adapter in-process through the bundled `libcn1ble` shared library (a Rust
/// [btleplug](https://github.com/deviceplug/btleplug) bridge -- CoreBluetooth
/// on macOS, BlueZ on Linux, WinRT on Windows), reached only through a
/// {@link NativeBleBridge}.
///
/// The bridge is the sole coupling to the native world: commands are typed
/// native calls and events are drained by this class's single reader thread
/// from {@link NativeBleBridge#pollEvent}. That is deliberate -- the engine
/// never re-enters the VM from its own worker threads -- so a host with an
/// operating-system JNI shim (the JavaSE simulator) and a host reaching the
/// same library through ParparVM `nativeSources` (the native Windows/Linux
/// ports) share this exact protocol, GATT and lifecycle logic.
///
/// btleplug is central-only: LE scanning and GATT client operations are
/// supported; peripheral mode (GATT server / advertising), classic
/// Bluetooth, L2CAP channels and bonding are not, and the corresponding
/// capability queries report {@code false}.
///
/// Internal to the native-backend implementation -- not a public API.
public class NativeBleBackend implements BleBackend {

    /// The backend name reported by {@link #getName()}.
    public static final String NAME = "native";

    /// The name of the fallback simulator backend, for error messages.
    private static final String SIMULATOR_NAME = "simulator";

    /// The event-protocol version this backend speaks.
    public static final long PROTOCOL_VERSION = 1;

    /// Reader-thread poll slice: bounds how long the thread lingers in the
    /// native call before it re-checks {@link #shutdownRequested}.
    private static final long POLL_TIMEOUT_MILLIS = 250;

    /// Completion of one in-flight command. Exactly one of the two methods
    /// fires, from the reader thread.
    public interface PendingOp {
        /// The command's terminal success event.
        void onEvent(String event, Map<String, Object> payload);

        /// The command failed -- engine error event, crash or shutdown.
        void onFailure(BluetoothException failure);
    }

    /// Shared fire-and-forget completion for commands whose result the caller
    /// ignores (e.g. scanStop while already tearing down). Static so it holds
    /// no reference to the enclosing backend.
    private static final PendingOp NO_OP = new PendingOp() {
        @Override
        public void onEvent(String event, Map<String, Object> payload) {
        }

        @Override
        public void onFailure(BluetoothException failure) {
            // nothing to report -- the caller does not observe this op
        }
    };

    private final NativeBleBridge bridge;

    private final Object engineLock = new Object();
    private boolean started;
    private final AtomicBoolean shutdownRequested = new AtomicBoolean();
    /// Set after a crash: the backend stays dead until switched away.
    private final AtomicBoolean engineFailed = new AtomicBoolean();

    private final AtomicLong nextRequestId = new AtomicLong(1);
    // ConcurrentHashMap is not on the device API surface (this class is
    // translated for the native ports); a synchronized HashMap gives the
    // same thread-safety for the caller-thread put / reader-thread remove
    // access pattern.
    private final Map<Long, PendingOp> pending = Collections.synchronizedMap(
            new HashMap<Long, PendingOp>());
    private final HashMap<String, NativeBlePeripheral> peripheralCache =
            new HashMap<String, NativeBlePeripheral>();
    private final Set<String> connectedAddresses = Collections.synchronizedSet(
            new HashSet<String>());

    private final AtomicReference<AdapterState> adapterState =
            new AtomicReference<AdapterState>(AdapterState.UNKNOWN);
    private final AtomicReference<AdapterStateSink> stateSink =
            new AtomicReference<AdapterStateSink>();
    private final AtomicReference<ScanSink> scanSink =
            new AtomicReference<ScanSink>();
    private final AtomicReference<Map<String, Object>> capabilities =
            new AtomicReference<Map<String, Object>>();

    /// Creates a backend over the given native bridge. The bridge owns how
    /// the shared library is opened -- a JNI shim on the JavaSE simulator, a
    /// ParparVM native binding on the Windows/Linux ports.
    public NativeBleBackend(NativeBleBridge bridge) {
        this.bridge = bridge;
    }

    // ------------------------------------------------------------------
    // engine lifecycle
    // ------------------------------------------------------------------

    private boolean ensureStarted() {
        synchronized (engineLock) {
            if (started && bridge.isAlive()) {
                return true;
            }
            if (shutdownRequested.get() || engineFailed.get()) {
                return false;
            }
            // start() brings the engine up and returns an adapter-availability
            // hint. The engine emits its capabilities + adapter-state handshake
            // (POWERED_ON when a radio is present, otherwise UNSUPPORTED /
            // UNAUTHORIZED / POWERED_OFF) either way, so always run the reader
            // to drain it -- a radioless host is a valid, observable state, not
            // a start failure. If the engine cannot run at all (e.g. the native
            // dlopen stub with no library) pollEvent reports it closed and the
            // reader exits at once, flipping the backend to UNSUPPORTED.
            bridge.start();
            started = true;
            startReader();
            return true;
        }
    }

    private void startReader() {
        // Not a daemon thread: Thread.setDaemon is not on the device API
        // surface (CLDC11) this core class compiles against. The reader loop
        // exits when pollEvent reports the engine closed -- shutdown() closes
        // it -- so it never blocks process exit.
        Thread th = new Thread("cn1ble-events") {
            @Override
            public void run() {
                try {
                    while (!shutdownRequested.get()) {
                        String event = bridge.pollEvent(POLL_TIMEOUT_MILLIS);
                        if (event == null) {
                            continue; // timeout -- poll again
                        }
                        if (event.length() == 0) {
                            break; // engine closed
                        }
                        handleEvent(event);
                    }
                } catch (RuntimeException ignored) {
                    // engine error -- fall through to the death handler
                }
                onEngineExited();
            }
        };
        th.start();
    }

    /// Reader-thread epilogue: distinguish clean shutdown from a crash.
    private void onEngineExited() {
        synchronized (engineLock) {
            if (!started) {
                return; // superseded
            }
            started = false;
            if (!shutdownRequested.get()) {
                engineFailed.set(true);
            }
        }
        if (shutdownRequested.get()) {
            return;
        }
        System.err.println("NativeBleBackend: the native BLE engine "
                + "terminated unexpectedly. The native Bluetooth backend is "
                + "unavailable; switch back with switchBackend(\""
                + SIMULATOR_NAME + "\").");
        BluetoothException failure = new BluetoothException(
                BluetoothError.IO_ERROR,
                "The native BLE engine terminated unexpectedly");
        failAllPending(failure);
        ScanSink scan = scanSink.get();
        scanSink.set(null);
        if (scan != null) {
            scan.onFailed(new BluetoothException(BluetoothError.SCAN_FAILED,
                    "Scan aborted: the native BLE engine terminated"));
        }
        List<NativeBlePeripheral> all;
        synchronized (peripheralCache) {
            all = new ArrayList<NativeBlePeripheral>(
                    peripheralCache.values());
        }
        connectedAddresses.clear();
        int size = all.size();
        for (int i = 0; i < size; i++) {
            all.get(i).handleEngineDied(failure);
        }
        setAdapterState(AdapterState.UNSUPPORTED);
    }

    private void failAllPending(BluetoothException failure) {
        // remove per key (never clear()) so an op registered concurrently is
        // either failed here or rejected by ensureStarted -- not dropped.
        // Snapshot the keys under the map's own lock (synchronizedMap
        // requires manual synchronization to iterate its views).
        List<Long> ids;
        synchronized (pending) {
            ids = new ArrayList<Long>(pending.keySet());
        }
        int size = ids.size();
        for (int i = 0; i < size; i++) {
            PendingOp op = pending.remove(ids.get(i));
            if (op == null) {
                continue;
            }
            try {
                op.onFailure(failure);
            } catch (RuntimeException ex) {
                System.err.println("NativeBleBackend: pending-op failure "
                        + "callback threw: " + ex);
            }
        }
    }

    private void setAdapterState(AdapterState newState) {
        if (adapterState.get() == newState) {
            return;
        }
        adapterState.set(newState);
        AdapterStateSink sink = stateSink.get();
        if (sink != null) {
            sink.adapterStateChanged(newState);
        }
    }

    // ------------------------------------------------------------------
    // incoming events
    // ------------------------------------------------------------------

    private void handleEvent(String text) {
        Map<String, Object> obj;
        try {
            obj = Json.parse(text);
        } catch (Throwable t) {
            System.err.println("NativeBleBackend: malformed engine event: "
                    + text);
            return;
        }
        String event = Json.str(obj, "event", "");
        if ("capabilities".equals(event)) {
            capabilities.set(obj);
            long version = Json.longVal(obj, "version", -1);
            if (version != PROTOCOL_VERSION) {
                System.err.println("NativeBleBackend: engine speaks protocol "
                        + "version " + version + ", expected "
                        + PROTOCOL_VERSION + " -- continuing best-effort");
            }
            return;
        }
        if ("stateChanged".equals(event)) {
            setAdapterState(mapAdapterState(Json.str(obj, "state", "")));
            return;
        }
        if ("scanResult".equals(event)) {
            handleScanResult(obj);
            return;
        }
        if ("notification".equals(event)) {
            NativeBlePeripheral p = cachedPeripheral(
                    Json.str(obj, "address", ""));
            if (p != null) {
                p.handleNotification(Json.str(obj, "service", ""),
                        Json.str(obj, "characteristic", ""),
                        Json.str(obj, "value", ""));
            }
            return;
        }
        // connection transitions update shared state whether solicited or not
        if ("connected".equals(event)) {
            String address = Json.str(obj, "address", "");
            connectedAddresses.add(address);
            NativeBlePeripheral p = cachedPeripheral(address);
            if (p != null) {
                p.handleConnected(Json.str(obj, "name", null));
            }
        } else if ("disconnected".equals(event)) {
            String address = Json.str(obj, "address", "");
            connectedAddresses.remove(address);
            NativeBlePeripheral p = cachedPeripheral(address);
            if (p != null) {
                p.handleDisconnected(Json.str(obj, "reason", ""));
            }
        }
        long requestId = Json.longVal(obj, "requestId", -1);
        if (requestId >= 0) {
            PendingOp op = pending.remove(Long.valueOf(requestId));
            if (op == null) {
                return; // late event of an already failed op
            }
            if ("error".equals(event)) {
                op.onFailure(new BluetoothException(
                        mapErrorCode(Json.str(obj, "code", "")),
                        Json.str(obj, "message", "engine error")));
            } else {
                op.onEvent(event, obj);
            }
            return;
        }
        if ("error".equals(event)) {
            System.err.println("NativeBleBackend: engine error ("
                    + Json.str(obj, "command", "?") + "): "
                    + Json.str(obj, "message", ""));
            return;
        }
        if (!"connected".equals(event) && !"disconnected".equals(event)) {
            System.err.println("NativeBleBackend: unknown event '" + event
                    + "'");
        }
    }

    private void handleScanResult(Map<String, Object> obj) {
        String address = Json.str(obj, "address", "");
        if (address.length() == 0) {
            return;
        }
        String name = Json.str(obj, "name", "");
        int rssi = Json.intVal(obj, "rssi", -127);
        NativeBlePeripheral p = peripheral(address);
        p.updateFromScan(name, rssi);
        ScanSink sink = scanSink.get();
        if (sink == null) {
            return;
        }
        AdvertisementData ad = new AdvertisementData();
        if (name.length() > 0) {
            ad.setLocalName(name);
        }
        List<Object> uuids = Json.list(obj, "serviceUuids");
        int size = uuids.size();
        for (int i = 0; i < size; i++) {
            BluetoothUuid u = parseUuid(String.valueOf(uuids.get(i)));
            if (u != null) {
                ad.addServiceUuid(u);
            }
        }
        Map<String, Object> manufacturer = Json.map(obj.get(
                "manufacturerData"));
        for (Map.Entry<String, Object> e : manufacturer.entrySet()) {
            try {
                ad.addManufacturerData(Integer.parseInt(e.getKey()),
                        Json.decodeBase64(String.valueOf(e.getValue())));
            } catch (RuntimeException ignored) {
            }
        }
        Map<String, Object> serviceData = Json.map(obj.get("serviceData"));
        for (Map.Entry<String, Object> e : serviceData.entrySet()) {
            BluetoothUuid u = parseUuid(e.getKey());
            if (u != null) {
                ad.addServiceData(u,
                        Json.decodeBase64(String.valueOf(e.getValue())));
            }
        }
        if (obj.containsKey("txPower")) {
            ad.setTxPowerLevel(Integer.valueOf(
                    Json.intVal(obj, "txPower", 0)));
        }
        sink.onResult(new ScanResult(p, rssi, ad, true,
                System.currentTimeMillis()));
    }

    private static BluetoothUuid parseUuid(String s) {
        try {
            return BluetoothUuid.fromString(s);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static AdapterState mapAdapterState(String state) {
        if ("poweredOn".equals(state)) {
            return AdapterState.POWERED_ON;
        }
        if ("poweredOff".equals(state)) {
            return AdapterState.POWERED_OFF;
        }
        if ("unsupported".equals(state)) {
            return AdapterState.UNSUPPORTED;
        }
        if ("unauthorized".equals(state)) {
            return AdapterState.UNAUTHORIZED;
        }
        return AdapterState.UNKNOWN;
    }

    public static BluetoothError mapErrorCode(String code) {
        if ("notSupported".equals(code)) {
            return BluetoothError.NOT_SUPPORTED;
        }
        if ("unauthorized".equals(code)) {
            return BluetoothError.UNAUTHORIZED;
        }
        if ("poweredOff".equals(code)) {
            return BluetoothError.POWERED_OFF;
        }
        if ("scanFailed".equals(code)) {
            return BluetoothError.SCAN_FAILED;
        }
        if ("connectFailed".equals(code) || "unknownPeripheral".equals(code)) {
            return BluetoothError.CONNECTION_FAILED;
        }
        if ("notConnected".equals(code)) {
            return BluetoothError.NOT_CONNECTED;
        }
        if ("unknownCharacteristic".equals(code)
                || "unknownDescriptor".equals(code)) {
            return BluetoothError.GATT_ERROR;
        }
        if ("timeout".equals(code)) {
            return BluetoothError.TIMEOUT;
        }
        if ("ioError".equals(code)) {
            return BluetoothError.IO_ERROR;
        }
        return BluetoothError.UNKNOWN;
    }

    // ------------------------------------------------------------------
    // outgoing commands
    // ------------------------------------------------------------------

    /// Registers the pending op and ensures the engine is running. On failure
    /// the op is already failed and the caller must not dispatch. On success
    /// the caller issues its typed bridge command with {@code id}.
    private boolean beginCommand(long id, PendingOp op) {
        pending.put(Long.valueOf(id), op);
        if (!ensureStarted()) {
            pending.remove(Long.valueOf(id));
            op.onFailure(new BluetoothException(BluetoothError.IO_ERROR,
                    "The native BLE engine could not be started"));
            return false;
        }
        return true;
    }

    private void commandFailed(long id, RuntimeException ex) {
        PendingOp mine = pending.remove(Long.valueOf(id));
        if (mine != null) {
            mine.onFailure(new BluetoothException(BluetoothError.IO_ERROR,
                    "Failed to dispatch command to the native BLE engine: "
                            + ex));
        }
    }

    private long nextId() {
        return nextRequestId.getAndIncrement();
    }

    void connectPeripheral(String address, PendingOp op) {
        long id = nextId();
        if (!beginCommand(id, op)) {
            return;
        }
        try {
            bridge.connect(id, address);
        } catch (RuntimeException ex) {
            commandFailed(id, ex);
        }
    }

    void disconnectPeripheral(String address, PendingOp op) {
        long id = nextId();
        if (!beginCommand(id, op)) {
            return;
        }
        try {
            bridge.disconnect(id, address);
        } catch (RuntimeException ex) {
            commandFailed(id, ex);
        }
    }

    void discoverServices(String address, PendingOp op) {
        long id = nextId();
        if (!beginCommand(id, op)) {
            return;
        }
        try {
            bridge.discover(id, address);
        } catch (RuntimeException ex) {
            commandFailed(id, ex);
        }
    }

    void readCharacteristic(String address, String service,
            String characteristic, PendingOp op) {
        long id = nextId();
        if (!beginCommand(id, op)) {
            return;
        }
        try {
            bridge.read(id, address, service, characteristic);
        } catch (RuntimeException ex) {
            commandFailed(id, ex);
        }
    }

    void writeCharacteristic(String address, String service,
            String characteristic, byte[] value, boolean noResponse,
            PendingOp op) {
        long id = nextId();
        if (!beginCommand(id, op)) {
            return;
        }
        try {
            bridge.write(id, address, service, characteristic, value,
                    noResponse);
        } catch (RuntimeException ex) {
            commandFailed(id, ex);
        }
    }

    void setNotifications(String address, String service,
            String characteristic, boolean enable, PendingOp op) {
        long id = nextId();
        if (!beginCommand(id, op)) {
            return;
        }
        try {
            bridge.subscribe(id, address, service, characteristic, enable);
        } catch (RuntimeException ex) {
            commandFailed(id, ex);
        }
    }

    void readDescriptor(String address, String service, String characteristic,
            String descriptor, PendingOp op) {
        long id = nextId();
        if (!beginCommand(id, op)) {
            return;
        }
        try {
            bridge.readDescriptor(id, address, service, characteristic,
                    descriptor);
        } catch (RuntimeException ex) {
            commandFailed(id, ex);
        }
    }

    void writeDescriptor(String address, String service, String characteristic,
            String descriptor, byte[] value, PendingOp op) {
        long id = nextId();
        if (!beginCommand(id, op)) {
            return;
        }
        try {
            bridge.writeDescriptor(id, address, service, characteristic,
                    descriptor, value);
        } catch (RuntimeException ex) {
            commandFailed(id, ex);
        }
    }

    void readRssi(String address, PendingOp op) {
        long id = nextId();
        if (!beginCommand(id, op)) {
            return;
        }
        try {
            bridge.readRssi(id, address);
        } catch (RuntimeException ex) {
            commandFailed(id, ex);
        }
    }

    /// True when the engine's capability handshake advertises the key.
    public boolean engineSupports(String capability) {
        Map<String, Object> caps = capabilities.get();
        return caps != null && Json.boolVal(caps, capability, false);
    }

    // ------------------------------------------------------------------
    // peripheral cache
    // ------------------------------------------------------------------

    /// The canonical peripheral wrapper for the address.
    NativeBlePeripheral peripheral(String address) {
        synchronized (peripheralCache) {
            NativeBlePeripheral p = peripheralCache.get(address);
            if (p == null) {
                p = new NativeBlePeripheral(this, address);
                peripheralCache.put(address, p);
            }
            return p;
        }
    }

    private NativeBlePeripheral cachedPeripheral(String address) {
        synchronized (peripheralCache) {
            return peripheralCache.get(address);
        }
    }

    // ------------------------------------------------------------------
    // BleBackend
    // ------------------------------------------------------------------

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isLeSupported() {
        return true;
    }

    @Override
    public boolean isPeripheralModeSupported() {
        return false; // btleplug is central-only
    }

    @Override
    public boolean isClassicSupported() {
        return false;
    }

    @Override
    public boolean isL2capSupported() {
        return false;
    }

    @Override
    public AdapterState getAdapterState() {
        return adapterState.get();
    }

    @Override
    public void setAdapterStateSink(AdapterStateSink sink) {
        stateSink.set(sink);
        if (sink != null) {
            // installing the sink is the backend's activation point -- boot
            // the engine so the adapter-state handshake arrives
            ensureStarted();
        }
    }

    @Override
    public void startScan(final ScanSink sink) {
        stopScan();
        scanSink.set(sink);
        long id = nextId();
        if (!beginCommand(id, new PendingOp() {
            @Override
            public void onEvent(String event, Map<String, Object> payload) {
                // scanStarted -- sightings follow as scanResult
            }

            @Override
            public void onFailure(BluetoothException failure) {
                if (scanSink.get() == sink) { //NOPMD CompareObjectsWithEquals - identity check: is this still the registered sink?
                    scanSink.set(null);
                }
                sink.onFailed(failure);
            }
        })) {
            return;
        }
        try {
            bridge.scanStart(id, "");
        } catch (RuntimeException ex) {
            commandFailed(id, ex);
        }
    }

    @Override
    public void stopScan() {
        scanSink.set(null);
        synchronized (engineLock) {
            if (!started || !bridge.isAlive()) {
                return; // never boot the engine just to stop a scan
            }
        }
        long id = nextId();
        if (!beginCommand(id, NO_OP)) {
            return;
        }
        try {
            bridge.scanStop(id);
        } catch (RuntimeException ex) {
            commandFailed(id, ex);
        }
    }

    @Override
    public BlePeripheral getPeripheral(String address) {
        return cachedPeripheral(address);
    }

    @Override
    public List<BlePeripheral> getConnectedPeripherals(
            BluetoothUuid serviceFilter) {
        ArrayList<BlePeripheral> out = new ArrayList<BlePeripheral>();
        // snapshot under the set's lock (synchronizedSet needs manual
        // synchronization to iterate)
        List<String> addresses;
        synchronized (connectedAddresses) {
            addresses = new ArrayList<String>(connectedAddresses);
        }
        int n = addresses.size();
        for (int ai = 0; ai < n; ai++) {
            String address = addresses.get(ai);
            NativeBlePeripheral p = cachedPeripheral(address);
            if (p == null) {
                continue;
            }
            if (serviceFilter != null && p.getService(serviceFilter) == null) {
                continue;
            }
            out.add(p);
        }
        return out;
    }

    @Override
    public List<BlePeripheral> getBondedPeripherals() {
        // btleplug exposes no bonded-device registry
        return new ArrayList<BlePeripheral>();
    }

    @Override
    public AsyncResource<GattServer> openGattServer(
            GattServerListener listener) {
        AsyncResource<GattServer> out = new AsyncResource<GattServer>();
        out.error(notSupported("GATT server"));
        return out;
    }

    @Override
    public AsyncResource<BleAdvertisement> startAdvertising(
            AdvertiseSettings settings, AdvertiseData data,
            AdvertiseData scanResponse) {
        AsyncResource<BleAdvertisement> out =
                new AsyncResource<BleAdvertisement>();
        out.error(notSupported("BLE advertising"));
        return out;
    }

    @Override
    public AsyncResource<L2capServer> openL2capServer(boolean secure) {
        AsyncResource<L2capServer> out = new AsyncResource<L2capServer>();
        out.error(notSupported("L2CAP"));
        return out;
    }

    private static BluetoothException notSupported(String feature) {
        return new BluetoothException(BluetoothError.NOT_SUPPORTED, feature
                + " is not supported by the native backend"
                + " (btleplug is central-only)");
    }

    @Override
    public void shutdown() {
        boolean wasStarted;
        synchronized (engineLock) {
            shutdownRequested.set(true);
            wasStarted = started;
            started = false;
        }
        scanSink.set(null);
        if (wasStarted) {
            bridge.close();
        }
        failAllPending(new BluetoothException(BluetoothError.IO_ERROR,
                "The native Bluetooth backend was shut down"));
    }
}
