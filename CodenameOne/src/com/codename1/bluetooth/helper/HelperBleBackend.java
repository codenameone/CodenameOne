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
package com.codename1.bluetooth.helper;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The real-radio {@link BleBackend}: drives the host machine's Bluetooth
 * adapter through the bundled {@code cn1-ble-helper} subprocess (a Rust
 * <a href="https://github.com/deviceplug/btleplug">btleplug</a> bridge --
 * CoreBluetooth on macOS, BlueZ on Linux, WinRT on Windows). Commands and
 * events travel as line-delimited JSON over the helper's stdin/stdout; see
 * {@code Ports/JavaSE/native/cn1-ble-helper/PROTOCOL.md}.
 *
 * <p>This class is transport-agnostic: the child process's standard I/O is
 * reached only through a {@link HelperTransport} created by the injected
 * {@link HelperTransportFactory}, so a host with an operating-system process
 * API (the JavaSE simulator) and a host without it (the native Windows/Linux
 * ports, reaching the subprocess through a native bridge) share this exact
 * protocol, GATT and lifecycle logic. Nothing here references an OS process
 * API or a shutdown hook -- the owner calls {@link #shutdown()} on app
 * exit.</p>
 *
 * <p>btleplug is central-only: LE scanning and GATT client operations are
 * supported; peripheral mode (GATT server / advertising), classic
 * Bluetooth, L2CAP channels and bonding are not, and the corresponding
 * capability queries report {@code false}.</p>
 */
public class HelperBleBackend implements BleBackend {

    /** The backend name reported by {@link #getName()}. */
    public static final String NAME = "native";

    /** The name of the fallback simulator backend, for error messages. */
    private static final String SIMULATOR_NAME = "simulator";

    /** The wire protocol version this backend speaks. */
    public static final long PROTOCOL_VERSION = 1;

    /**
     * Completion of one in-flight helper command. Exactly one of the two
     * methods fires, from the helper reader thread.
     */
    public interface PendingOp {
        /** The command's terminal success event. */
        void onEvent(String event, Map<String, Object> payload);

        /** The command failed -- helper error event, crash or shutdown. */
        void onFailure(BluetoothException failure);
    }

    /**
     * Shared fire-and-forget completion for commands whose result the caller
     * ignores (e.g. scanStop while already tearing down). Static so it holds
     * no reference to the enclosing backend.
     */
    private static final PendingOp NO_OP = new PendingOp() {
        public void onEvent(String event, Map<String, Object> payload) {
        }

        public void onFailure(BluetoothException failure) {
            // nothing to report -- the caller does not observe this op
        }
    };

    private final HelperTransportFactory transportFactory;

    private final Object processLock = new Object();
    private HelperTransport transport;
    private final AtomicBoolean shutdownRequested = new AtomicBoolean();
    /** Set after a crash: the backend stays dead until switched away. */
    private final AtomicBoolean helperFailed = new AtomicBoolean();

    private final AtomicLong nextRequestId = new AtomicLong(1);
    // ConcurrentHashMap is not on the device API surface (this class is
    // translated for the native ports); a synchronized HashMap gives the
    // same thread-safety for the caller-thread put / reader-thread
    // remove access pattern.
    private final Map<Long, PendingOp> pending = Collections.synchronizedMap(
            new HashMap<Long, PendingOp>());
    private final HashMap<String, HelperBlePeripheral> peripheralCache =
            new HashMap<String, HelperBlePeripheral>();
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

    /**
     * Creates a backend that (re)starts the helper through the given
     * transport factory. The factory is responsible for how the child
     * process is launched -- resolving the binary and, on the JavaSE
     * simulator, building the OS subprocess.
     */
    public HelperBleBackend(HelperTransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    // ------------------------------------------------------------------
    // helper process lifecycle
    // ------------------------------------------------------------------

    private boolean ensureStarted() {
        synchronized (processLock) {
            if (transport != null && transport.isAlive()) {
                return true;
            }
            if (shutdownRequested.get() || helperFailed.get()) {
                return false;
            }
            try {
                HelperTransport t = transportFactory.create();
                t.start(null);
                transport = t;
                startReader(t);
                return true;
            } catch (IOException ex) {
                System.err.println("HelperBleBackend: failed to start "
                        + "cn1-ble-helper: " + ex);
                transport = null;
                return false;
            }
        }
    }

    private void startReader(final HelperTransport t) {
        // Not a daemon thread: Thread.setDaemon is not on the device API
        // surface (CLDC11) this core class compiles against. The reader
        // always terminates when the transport closes -- shutdown() closes
        // it, and JavaSE's ProcessTransport additionally closes it from a
        // JVM shutdown hook -- so it never blocks process exit.
        Thread th = new Thread("cn1ble-helper-stdout") {
            @Override
            public void run() {
                try {
                    String line;
                    while ((line = t.readLine()) != null) {
                        handleLine(line);
                    }
                } catch (IOException ignored) {
                    // pipe closed -- fall through to the death handler
                }
                onHelperExited(t);
            }
        };
        th.start();
    }

    /** Reader thread epilogue: distinguish clean shutdown from a crash. */
    private void onHelperExited(HelperTransport t) {
        synchronized (processLock) {
            if (transport != t) {
                return; // superseded by a restart
            }
            transport = null;
            if (!shutdownRequested.get()) {
                helperFailed.set(true);
            }
        }
        if (shutdownRequested.get()) {
            return;
        }
        System.err.println("HelperBleBackend: the cn1-ble-helper process "
                + "terminated unexpectedly. The native Bluetooth backend "
                + "is unavailable; switch back with switchBackend(\""
                + SIMULATOR_NAME + "\").");
        BluetoothException failure = new BluetoothException(
                BluetoothError.IO_ERROR,
                "The cn1-ble-helper process terminated unexpectedly");
        failAllPending(failure);
        ScanSink scan = scanSink.get();
        scanSink.set(null);
        if (scan != null) {
            scan.onFailed(new BluetoothException(BluetoothError.SCAN_FAILED,
                    "Scan aborted: the cn1-ble-helper process terminated"));
        }
        List<HelperBlePeripheral> all;
        synchronized (peripheralCache) {
            all = new ArrayList<HelperBlePeripheral>(
                    peripheralCache.values());
        }
        connectedAddresses.clear();
        int size = all.size();
        for (int i = 0; i < size; i++) {
            all.get(i).handleHelperDied(failure);
        }
        setAdapterState(AdapterState.UNSUPPORTED);
    }

    private void failAllPending(BluetoothException failure) {
        // remove per key (never clear()) so an op registered concurrently
        // is either failed here or rejected by ensureStarted -- not dropped.
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
                System.err.println("HelperBleBackend: pending-op failure "
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

    private void handleLine(String line) {
        if (line.length() == 0) {
            return;
        }
        Map<String, Object> obj;
        try {
            obj = Wire.parse(line);
        } catch (Throwable t) {
            System.err.println("HelperBleBackend: malformed helper output: "
                    + line);
            return;
        }
        String event = Wire.str(obj, "event", "");
        if ("capabilities".equals(event)) {
            capabilities.set(obj);
            long version = Wire.longVal(obj, "version", -1);
            if (version != PROTOCOL_VERSION) {
                System.err.println("HelperBleBackend: helper speaks "
                        + "protocol version " + version + ", expected "
                        + PROTOCOL_VERSION + " -- continuing best-effort");
            }
            return;
        }
        if ("stateChanged".equals(event)) {
            setAdapterState(mapAdapterState(Wire.str(obj, "state", "")));
            return;
        }
        if ("scanResult".equals(event)) {
            handleScanResult(obj);
            return;
        }
        if ("notification".equals(event)) {
            HelperBlePeripheral p = cachedPeripheral(
                    Wire.str(obj, "address", ""));
            if (p != null) {
                p.handleNotification(Wire.str(obj, "service", ""),
                        Wire.str(obj, "characteristic", ""),
                        Wire.str(obj, "value", ""));
            }
            return;
        }
        // connection transitions update shared state whether solicited or not
        if ("connected".equals(event)) {
            String address = Wire.str(obj, "address", "");
            connectedAddresses.add(address);
            HelperBlePeripheral p = cachedPeripheral(address);
            if (p != null) {
                p.handleConnected(Wire.str(obj, "name", null));
            }
        } else if ("disconnected".equals(event)) {
            String address = Wire.str(obj, "address", "");
            connectedAddresses.remove(address);
            HelperBlePeripheral p = cachedPeripheral(address);
            if (p != null) {
                p.handleDisconnected(Wire.str(obj, "reason", ""));
            }
        }
        long requestId = Wire.longVal(obj, "requestId", -1);
        if (requestId >= 0) {
            PendingOp op = pending.remove(Long.valueOf(requestId));
            if (op == null) {
                return; // late event of an already failed op
            }
            if ("error".equals(event)) {
                op.onFailure(new BluetoothException(
                        mapErrorCode(Wire.str(obj, "code", "")),
                        Wire.str(obj, "message", "helper error")));
            } else {
                op.onEvent(event, obj);
            }
            return;
        }
        if ("error".equals(event)) {
            System.err.println("HelperBleBackend: helper error ("
                    + Wire.str(obj, "command", "?") + "): "
                    + Wire.str(obj, "message", ""));
            return;
        }
        if (!"connected".equals(event) && !"disconnected".equals(event)) {
            System.err.println("HelperBleBackend: unknown event '" + event
                    + "'");
        }
    }

    private void handleScanResult(Map<String, Object> obj) {
        String address = Wire.str(obj, "address", "");
        if (address.length() == 0) {
            return;
        }
        String name = Wire.str(obj, "name", "");
        int rssi = Wire.intVal(obj, "rssi", -127);
        HelperBlePeripheral p = peripheral(address);
        p.updateFromScan(name, rssi);
        ScanSink sink = scanSink.get();
        if (sink == null) {
            return;
        }
        AdvertisementData ad = new AdvertisementData();
        if (name.length() > 0) {
            ad.setLocalName(name);
        }
        List<Object> uuids = Wire.list(obj, "serviceUuids");
        int size = uuids.size();
        for (int i = 0; i < size; i++) {
            BluetoothUuid u = parseUuid(String.valueOf(uuids.get(i)));
            if (u != null) {
                ad.addServiceUuid(u);
            }
        }
        Map<String, Object> manufacturer = Wire.map(obj.get(
                "manufacturerData"));
        for (Map.Entry<String, Object> e : manufacturer.entrySet()) {
            try {
                ad.addManufacturerData(Integer.parseInt(e.getKey()),
                        Wire.decodeBase64(String.valueOf(e.getValue())));
            } catch (RuntimeException ignored) {
            }
        }
        Map<String, Object> serviceData = Wire.map(obj.get("serviceData"));
        for (Map.Entry<String, Object> e : serviceData.entrySet()) {
            BluetoothUuid u = parseUuid(e.getKey());
            if (u != null) {
                ad.addServiceData(u,
                        Wire.decodeBase64(String.valueOf(e.getValue())));
            }
        }
        if (obj.containsKey("txPower")) {
            ad.setTxPowerLevel(Integer.valueOf(
                    Wire.intVal(obj, "txPower", 0)));
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
        if ("connectFailed".equals(code)
                || "unknownPeripheral".equals(code)) {
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

    /**
     * Registers the pending op and writes the command line; the id inside
     * {@code json} must be the returned request id, so callers build the
     * line via {@link #nextId()} first.
     */
    private void send(long id, String json, PendingOp op) {
        pending.put(Long.valueOf(id), op);
        if (!ensureStarted()) {
            pending.remove(Long.valueOf(id));
            op.onFailure(new BluetoothException(BluetoothError.IO_ERROR,
                    "The cn1-ble-helper process could not be started"));
            return;
        }
        IOException failure = null;
        synchronized (processLock) {
            try {
                if (transport == null) {
                    throw new IOException("helper stdin closed");
                }
                transport.writeLine(json);
            } catch (IOException ex) {
                failure = ex;
            }
        }
        if (failure != null) {
            PendingOp mine = pending.remove(Long.valueOf(id));
            if (mine != null) {
                mine.onFailure(new BluetoothException(
                        BluetoothError.IO_ERROR,
                        "Failed to send command to cn1-ble-helper: "
                                + failure));
            }
        }
    }

    private long nextId() {
        return nextRequestId.getAndIncrement();
    }

    void connectPeripheral(String address, PendingOp op) {
        long id = nextId();
        send(id, Wire.obj().put("cmd", "connect").put("id", id)
                .put("address", address).line(), op);
    }

    void disconnectPeripheral(String address, PendingOp op) {
        long id = nextId();
        send(id, Wire.obj().put("cmd", "disconnect").put("id", id)
                .put("address", address).line(), op);
    }

    void discoverServices(String address, PendingOp op) {
        long id = nextId();
        send(id, Wire.obj().put("cmd", "discover").put("id", id)
                .put("address", address).line(), op);
    }

    void readCharacteristic(String address, String service,
            String characteristic, PendingOp op) {
        long id = nextId();
        send(id, Wire.obj().put("cmd", "read").put("id", id)
                .put("address", address).put("service", service)
                .put("characteristic", characteristic).line(), op);
    }

    void writeCharacteristic(String address, String service,
            String characteristic, String valueBase64, boolean noResponse,
            PendingOp op) {
        long id = nextId();
        send(id, Wire.obj().put("cmd", "write").put("id", id)
                .put("address", address).put("service", service)
                .put("characteristic", characteristic)
                .put("value", valueBase64)
                .put("noResponse", noResponse).line(), op);
    }

    void setNotifications(String address, String service,
            String characteristic, boolean enable, PendingOp op) {
        long id = nextId();
        send(id, Wire.obj()
                .put("cmd", enable ? "subscribe" : "unsubscribe")
                .put("id", id).put("address", address)
                .put("service", service)
                .put("characteristic", characteristic).line(), op);
    }

    void readDescriptor(String address, String service,
            String characteristic, String descriptor, PendingOp op) {
        long id = nextId();
        send(id, Wire.obj().put("cmd", "readDescriptor").put("id", id)
                .put("address", address).put("service", service)
                .put("characteristic", characteristic)
                .put("descriptor", descriptor).line(), op);
    }

    void writeDescriptor(String address, String service,
            String characteristic, String descriptor, String valueBase64,
            PendingOp op) {
        long id = nextId();
        send(id, Wire.obj().put("cmd", "writeDescriptor").put("id", id)
                .put("address", address).put("service", service)
                .put("characteristic", characteristic)
                .put("descriptor", descriptor)
                .put("value", valueBase64).line(), op);
    }

    void readRssi(String address, PendingOp op) {
        long id = nextId();
        send(id, Wire.obj().put("cmd", "readRssi").put("id", id)
                .put("address", address).line(), op);
    }

    /** True when the helper's capability handshake advertises the key. */
    public boolean helperSupports(String capability) {
        Map<String, Object> caps = capabilities.get();
        return caps != null && Wire.boolVal(caps, capability, false);
    }

    // ------------------------------------------------------------------
    // peripheral cache
    // ------------------------------------------------------------------

    /** The canonical peripheral wrapper for the address. */
    HelperBlePeripheral peripheral(String address) {
        synchronized (peripheralCache) {
            HelperBlePeripheral p = peripheralCache.get(address);
            if (p == null) {
                p = new HelperBlePeripheral(this, address);
                peripheralCache.put(address, p);
            }
            return p;
        }
    }

    private HelperBlePeripheral cachedPeripheral(String address) {
        synchronized (peripheralCache) {
            return peripheralCache.get(address);
        }
    }

    // ------------------------------------------------------------------
    // BleBackend
    // ------------------------------------------------------------------

    public String getName() {
        return NAME;
    }

    public boolean isLeSupported() {
        return true;
    }

    public boolean isPeripheralModeSupported() {
        return false; // btleplug is central-only
    }

    public boolean isClassicSupported() {
        return false;
    }

    public boolean isL2capSupported() {
        return false;
    }

    public AdapterState getAdapterState() {
        return adapterState.get();
    }

    public void setAdapterStateSink(AdapterStateSink sink) {
        stateSink.set(sink);
        if (sink != null) {
            // installing the sink is the backend's activation point --
            // boot the helper so the adapter state handshake arrives
            ensureStarted();
        }
    }

    public void startScan(final ScanSink sink) {
        stopScan();
        scanSink.set(sink);
        long id = nextId();
        send(id, Wire.obj().put("cmd", "scanStart").put("id", id).line(),
                new PendingOp() {
                    public void onEvent(String event,
                            Map<String, Object> payload) {
                        // scanStarted -- sightings follow as scanResult
                    }

                    public void onFailure(BluetoothException failure) {
                        if (scanSink.get() == sink) {
                            scanSink.set(null);
                        }
                        sink.onFailed(failure);
                    }
                });
    }

    public void stopScan() {
        scanSink.set(null);
        synchronized (processLock) {
            if (transport == null || !transport.isAlive()) {
                return; // never boot the helper just to stop a scan
            }
        }
        long id = nextId();
        send(id, Wire.obj().put("cmd", "scanStop").put("id", id).line(), NO_OP);
    }

    public BlePeripheral getPeripheral(String address) {
        return cachedPeripheral(address);
    }

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
            HelperBlePeripheral p = cachedPeripheral(address);
            if (p == null) {
                continue;
            }
            if (serviceFilter != null
                    && p.getService(serviceFilter) == null) {
                continue;
            }
            out.add(p);
        }
        return out;
    }

    public List<BlePeripheral> getBondedPeripherals() {
        // btleplug exposes no bonded-device registry
        return new ArrayList<BlePeripheral>();
    }

    public AsyncResource<GattServer> openGattServer(
            GattServerListener listener) {
        AsyncResource<GattServer> out = new AsyncResource<GattServer>();
        out.error(notSupported("GATT server"));
        return out;
    }

    public AsyncResource<BleAdvertisement> startAdvertising(
            AdvertiseSettings settings, AdvertiseData data,
            AdvertiseData scanResponse) {
        AsyncResource<BleAdvertisement> out =
                new AsyncResource<BleAdvertisement>();
        out.error(notSupported("BLE advertising"));
        return out;
    }

    public AsyncResource<L2capServer> openL2capServer(boolean secure) {
        AsyncResource<L2capServer> out = new AsyncResource<L2capServer>();
        out.error(notSupported("L2CAP"));
        return out;
    }

    private static BluetoothException notSupported(String feature) {
        return new BluetoothException(BluetoothError.NOT_SUPPORTED, feature
                + " is not supported by the native JavaSE backend"
                + " (btleplug is central-only)");
    }

    public void shutdown() {
        shutdownInternal();
    }

    private void shutdownInternal() {
        HelperTransport t;
        synchronized (processLock) {
            shutdownRequested.set(true);
            t = transport;
            if (t != null) {
                try {
                    t.writeLine("{\"cmd\":\"shutdown\"}");
                } catch (IOException ignored) {
                }
            }
            transport = null;
        }
        scanSink.set(null);
        failAllPending(new BluetoothException(BluetoothError.IO_ERROR,
                "The native Bluetooth backend was shut down"));
        if (t != null) {
            // the transport honors the shutdown command with a grace period
            // before destroying the child process
            t.close();
        }
    }
}
