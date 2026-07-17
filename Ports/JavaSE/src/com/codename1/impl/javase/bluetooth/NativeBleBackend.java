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
import com.codename1.io.JSONParser;
import com.codename1.util.AsyncResource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The real-radio {@link BleBackend}: drives the host machine's Bluetooth
 * adapter through the bundled {@code cn1-ble-helper} subprocess (a Rust
 * <a href="https://github.com/deviceplug/btleplug">btleplug</a> bridge --
 * CoreBluetooth on macOS, BlueZ on Linux, WinRT on Windows). Commands and
 * events travel as line-delimited JSON over the helper's stdin/stdout; see
 * {@code Ports/JavaSE/native/cn1-ble-helper/PROTOCOL.md}.
 *
 * <p>btleplug is central-only: LE scanning and GATT client operations are
 * supported; peripheral mode (GATT server / advertising), classic
 * Bluetooth, L2CAP channels and bonding are not, and the corresponding
 * capability queries report {@code false}.</p>
 *
 * <p>The helper binary is located, in order, via the
 * {@link #HELPER_PATH_PROPERTY} system property, the OS-keyed classpath
 * resource under {@link #HELPER_RESOURCE_DIR} (extracted to a temp file),
 * and finally a {@code PATH} lookup. When none is found
 * {@link #isAvailable()} is {@code false} and
 * {@link JavaSEBluetooth#switchBackend(String)} refuses the switch with a
 * message from {@link #describeResolution()}.</p>
 */
class NativeBleBackend implements BleBackend {

    /** The backend name reported by {@link #getName()}. */
    static final String NAME = JavaSEBluetooth.BACKEND_NATIVE;

    /**
     * System property naming an explicit helper binary; checked before the
     * classpath resource and the {@code PATH} lookup.
     */
    static final String HELPER_PATH_PROPERTY = "cn1.bluetooth.helperPath";

    /** Classpath directory of the OS-keyed bundled helper binaries. */
    static final String HELPER_RESOURCE_DIR =
            "/com/codename1/impl/javase/bluetooth/native/";

    /** Base name of the helper executable. */
    static final String HELPER_BASENAME = "cn1-ble-helper";

    /** The wire protocol version this backend speaks. */
    static final long PROTOCOL_VERSION = 1;

    /**
     * Completion of one in-flight helper command. Exactly one of the two
     * methods fires, from the helper reader thread.
     */
    interface PendingOp {
        /** The command's terminal success event. */
        void onEvent(String event, Map<String, Object> payload);

        /** The command failed -- helper error event, crash or shutdown. */
        void onFailure(BluetoothException failure);
    }

    private final Object processLock = new Object();
    private Process helper;
    private Writer helperIn;
    private Thread shutdownHook;
    private volatile boolean shutdownRequested;
    /** Set after a crash: the backend stays dead until switched away. */
    private volatile boolean helperFailed;

    /** Explicit launch command -- the unit-test seam; null for the real helper. */
    private final List<String> launchOverride;
    private final File helperBinary;
    private final String resolutionDescription;

    private final AtomicLong nextRequestId = new AtomicLong(1);
    private final Map<Long, PendingOp> pending =
            new ConcurrentHashMap<Long, PendingOp>();
    private final HashMap<String, NativeBlePeripheral> peripheralCache =
            new HashMap<String, NativeBlePeripheral>();
    private final Set<String> connectedAddresses = Collections.newSetFromMap(
            new ConcurrentHashMap<String, Boolean>());

    private volatile AdapterState adapterState = AdapterState.UNKNOWN;
    private volatile AdapterStateSink stateSink;
    private volatile ScanSink scanSink;
    private volatile Map<String, Object> capabilities;

    /** Resolves the helper binary for this host; check {@link #isAvailable()}. */
    NativeBleBackend() {
        List<String> attempted = new ArrayList<String>();
        this.helperBinary = resolveHelperBinary(
                System.getProperty(HELPER_PATH_PROPERTY),
                System.getProperty("os.name", ""),
                System.getProperty("os.arch", ""),
                System.getenv("PATH"), attempted);
        this.resolutionDescription = join(attempted);
        this.launchOverride = null;
    }

    /**
     * Unit-test seam: runs the given command line (a fake helper speaking
     * the wire protocol) instead of resolving a real binary.
     */
    NativeBleBackend(List<String> launchCommand) {
        this.launchOverride = launchCommand;
        this.helperBinary = null;
        this.resolutionDescription = "explicit launch command (test)";
    }

    // ------------------------------------------------------------------
    // helper binary resolution
    // ------------------------------------------------------------------

    /**
     * Resolution order: explicit system property, bundled classpath
     * resource for the OS (extracted to a temp file), {@code PATH} lookup.
     * Returns {@code null} when nothing was found; {@code attempted}
     * collects a human-readable trace for error messages.
     */
    static File resolveHelperBinary(String propertyValue, String osName,
            String osArch, String pathEnv, List<String> attempted) {
        if (propertyValue != null && propertyValue.length() > 0) {
            File f = new File(propertyValue);
            if (f.isFile()) {
                attempted.add("system property " + HELPER_PATH_PROPERTY
                        + " -> " + f.getAbsolutePath());
                return f;
            }
            attempted.add("system property " + HELPER_PATH_PROPERTY + "="
                    + propertyValue + " (no such file)");
        } else {
            attempted.add("system property " + HELPER_PATH_PROPERTY
                    + " (not set)");
        }
        String resourcePath = helperResourcePath(osName, osArch);
        if (resourcePath == null) {
            attempted.add("no bundled helper for os.name=" + osName
                    + " os.arch=" + osArch);
        } else {
            File extracted = extractResource(resourcePath, attempted);
            if (extracted != null) {
                return extracted;
            }
        }
        File onPath = resolveFromPathEnv(pathEnv,
                helperExecutableName(osName), attempted);
        if (onPath != null) {
            return onPath;
        }
        return null;
    }

    /**
     * Classpath location of the helper binary for the OS and CPU
     * architecture, or {@code null} when no binary is bundled for the
     * combination. The binaries ship from the cn1-binaries repository via
     * the maven/javase resource mapping, laid out as
     * {@code ble/macos/cn1-ble-helper} (a universal Mach-O binary covering
     * both architectures) and {@code ble/{linux,windows}/{x64,arm64}/}
     * (ELF and PE have no fat-binary format, so those are per-arch).
     */
    static String helperResourcePath(String osName, String osArch) {
        String os = osName == null ? "" : osName.toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            // universal binary: one file serves x86_64 and arm64
            return HELPER_RESOURCE_DIR + "macos/" + HELPER_BASENAME;
        }
        String arch = normalizeArch(osArch);
        if (arch == null) {
            return null;
        }
        if (os.contains("linux")) {
            return HELPER_RESOURCE_DIR + "linux/" + arch + "/"
                    + HELPER_BASENAME;
        }
        if (os.contains("windows")) {
            return HELPER_RESOURCE_DIR + "windows/" + arch + "/"
                    + HELPER_BASENAME + ".exe";
        }
        return null;
    }

    /**
     * Maps {@code os.arch} onto the directory names used by the bundled
     * binaries, or {@code null} for architectures no binary is shipped for
     * (32-bit x86 and 32-bit ARM among them) -- resolution then falls
     * through to the {@code PATH} lookup, so a self-built helper still
     * works there.
     */
    static String normalizeArch(String osArch) {
        String arch = osArch == null ? "" : osArch.toLowerCase();
        if (arch.equals("amd64") || arch.equals("x86_64")
                || arch.equals("x64")) {
            return "x64";
        }
        if (arch.equals("aarch64") || arch.equals("arm64")) {
            return "arm64";
        }
        return null;
    }

    /** The platform file name of the helper executable. */
    static String helperExecutableName(String osName) {
        String os = osName == null ? "" : osName.toLowerCase();
        return os.contains("windows") ? HELPER_BASENAME + ".exe"
                : HELPER_BASENAME;
    }

    /** Extracts the bundled helper to a temp file, or null when absent. */
    private static File extractResource(String resourcePath,
            List<String> attempted) {
        InputStream src =
                NativeBleBackend.class.getResourceAsStream(resourcePath);
        if (src == null) {
            attempted.add("classpath resource " + resourcePath
                    + " (missing)");
            return null;
        }
        try {
            boolean exe = resourcePath.endsWith(".exe");
            File out = File.createTempFile(HELPER_BASENAME + "-",
                    exe ? ".exe" : "");
            out.deleteOnExit();
            FileOutputStream sink = new FileOutputStream(out);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = src.read(buf)) >= 0) {
                    sink.write(buf, 0, n);
                }
            } finally {
                sink.close();
            }
            out.setExecutable(true, true);
            attempted.add("classpath resource " + resourcePath
                    + " -> " + out.getAbsolutePath());
            return out;
        } catch (IOException ex) {
            attempted.add("classpath resource " + resourcePath
                    + " (extraction failed: " + ex + ")");
            return null;
        } finally {
            try {
                src.close();
            } catch (IOException ignored) {
            }
        }
    }

    /** Scans the given PATH-style value for the helper executable. */
    static File resolveFromPathEnv(String pathEnv, String executableName,
            List<String> attempted) {
        if (pathEnv != null) {
            String[] dirs = pathEnv.split(File.pathSeparator);
            for (int i = 0; i < dirs.length; i++) {
                if (dirs[i].length() == 0) {
                    continue;
                }
                File candidate = new File(dirs[i], executableName);
                if (candidate.isFile()) {
                    attempted.add("PATH lookup -> "
                            + candidate.getAbsolutePath());
                    return candidate;
                }
            }
        }
        attempted.add("PATH lookup for " + executableName + " (not found)");
        return null;
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        int size = parts.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    /** True when a helper binary was located for this host. */
    boolean isAvailable() {
        return helperBinary != null || launchOverride != null;
    }

    /** Human-readable trace of the binary locations that were tried. */
    String describeResolution() {
        return resolutionDescription;
    }

    // ------------------------------------------------------------------
    // helper process lifecycle
    // ------------------------------------------------------------------

    private boolean ensureStarted() {
        synchronized (processLock) {
            if (helper != null && helper.isAlive()) {
                return true;
            }
            if (shutdownRequested || helperFailed || !isAvailable()) {
                return false;
            }
            List<String> command = launchOverride != null ? launchOverride
                    : Collections.singletonList(
                            helperBinary.getAbsolutePath());
            try {
                Process p = new ProcessBuilder(command).start();
                helper = p;
                helperIn = new BufferedWriter(new OutputStreamWriter(
                        p.getOutputStream(), "UTF-8"));
                startReader(p);
                startStderrPump(p);
                if (shutdownHook == null) {
                    shutdownHook = new Thread("cn1ble-helper-shutdown") {
                        public void run() {
                            shutdownInternal(false);
                        }
                    };
                    Runtime.getRuntime().addShutdownHook(shutdownHook);
                }
                return true;
            } catch (IOException ex) {
                System.err.println("NativeBleBackend: failed to start "
                        + command.get(0) + ": " + ex);
                helper = null;
                helperIn = null;
                return false;
            }
        }
    }

    private void startReader(final Process p) {
        Thread t = new Thread("cn1ble-helper-stdout") {
            public void run() {
                try {
                    BufferedReader r = new BufferedReader(
                            new InputStreamReader(p.getInputStream(),
                                    "UTF-8"));
                    String line;
                    while ((line = r.readLine()) != null) {
                        handleLine(line);
                    }
                } catch (IOException ignored) {
                    // pipe closed -- fall through to the death handler
                }
                onHelperExited(p);
            }
        };
        t.setDaemon(true);
        t.start();
    }

    private void startStderrPump(final Process p) {
        Thread t = new Thread("cn1ble-helper-stderr") {
            public void run() {
                try {
                    BufferedReader r = new BufferedReader(
                            new InputStreamReader(p.getErrorStream(),
                                    "UTF-8"));
                    String line;
                    while ((line = r.readLine()) != null) {
                        System.err.println("[Cn1BleHelper] " + line);
                    }
                } catch (IOException ignored) {
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    /** Reader thread epilogue: distinguish clean shutdown from a crash. */
    private void onHelperExited(Process p) {
        synchronized (processLock) {
            if (helper != p) {
                return; // superseded by a restart
            }
            helper = null;
            helperIn = null;
            if (!shutdownRequested) {
                helperFailed = true;
            }
        }
        if (shutdownRequested) {
            return;
        }
        System.err.println("NativeBleBackend: the cn1-ble-helper process "
                + "terminated unexpectedly. The native Bluetooth backend "
                + "is unavailable; switch back with switchBackend(\""
                + JavaSEBluetooth.BACKEND_SIMULATOR + "\").");
        BluetoothException failure = new BluetoothException(
                BluetoothError.IO_ERROR,
                "The cn1-ble-helper process terminated unexpectedly");
        failAllPending(failure);
        ScanSink scan = scanSink;
        scanSink = null;
        if (scan != null) {
            scan.onFailed(new BluetoothException(BluetoothError.SCAN_FAILED,
                    "Scan aborted: the cn1-ble-helper process terminated"));
        }
        List<NativeBlePeripheral> all;
        synchronized (peripheralCache) {
            all = new ArrayList<NativeBlePeripheral>(
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
        // is either failed here or rejected by ensureStarted -- not dropped
        List<Long> ids = new ArrayList<Long>(pending.keySet());
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
        if (adapterState == newState) {
            return;
        }
        adapterState = newState;
        AdapterStateSink sink = stateSink;
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
            System.err.println("NativeBleBackend: malformed helper output: "
                    + line);
            return;
        }
        String event = Wire.str(obj, "event", "");
        if ("capabilities".equals(event)) {
            capabilities = obj;
            long version = Wire.longVal(obj, "version", -1);
            if (version != PROTOCOL_VERSION) {
                System.err.println("NativeBleBackend: helper speaks "
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
            NativeBlePeripheral p = cachedPeripheral(
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
            NativeBlePeripheral p = cachedPeripheral(address);
            if (p != null) {
                p.handleConnected(Wire.str(obj, "name", null));
            }
        } else if ("disconnected".equals(event)) {
            String address = Wire.str(obj, "address", "");
            connectedAddresses.remove(address);
            NativeBlePeripheral p = cachedPeripheral(address);
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
            System.err.println("NativeBleBackend: helper error ("
                    + Wire.str(obj, "command", "?") + "): "
                    + Wire.str(obj, "message", ""));
            return;
        }
        if (!"connected".equals(event) && !"disconnected".equals(event)) {
            System.err.println("NativeBleBackend: unknown event '" + event
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
        NativeBlePeripheral p = peripheral(address);
        p.updateFromScan(name, rssi);
        ScanSink sink = scanSink;
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

    static AdapterState mapAdapterState(String state) {
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

    static BluetoothError mapErrorCode(String code) {
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
                if (helperIn == null) {
                    throw new IOException("helper stdin closed");
                }
                helperIn.write(json);
                helperIn.write('\n');
                helperIn.flush();
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
    boolean helperSupports(String capability) {
        Map<String, Object> caps = capabilities;
        return caps != null && Wire.boolVal(caps, capability, false);
    }

    // ------------------------------------------------------------------
    // peripheral cache
    // ------------------------------------------------------------------

    /** The canonical peripheral wrapper for the address. */
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
        return adapterState;
    }

    public void setAdapterStateSink(AdapterStateSink sink) {
        stateSink = sink;
        if (sink != null) {
            // installing the sink is the backend's activation point --
            // boot the helper so the adapter state handshake arrives
            ensureStarted();
        }
    }

    public void startScan(final ScanSink sink) {
        stopScan();
        scanSink = sink;
        long id = nextId();
        send(id, Wire.obj().put("cmd", "scanStart").put("id", id).line(),
                new PendingOp() {
                    public void onEvent(String event,
                            Map<String, Object> payload) {
                        // scanStarted -- sightings follow as scanResult
                    }

                    public void onFailure(BluetoothException failure) {
                        if (scanSink == sink) {
                            scanSink = null;
                        }
                        sink.onFailed(failure);
                    }
                });
    }

    public void stopScan() {
        scanSink = null;
        synchronized (processLock) {
            if (helper == null || !helper.isAlive()) {
                return; // never boot the helper just to stop a scan
            }
        }
        long id = nextId();
        send(id, Wire.obj().put("cmd", "scanStop").put("id", id).line(),
                new PendingOp() {
                    public void onEvent(String event,
                            Map<String, Object> payload) {
                    }

                    public void onFailure(BluetoothException failure) {
                        // already stopping -- nothing to report
                    }
                });
    }

    public BlePeripheral getPeripheral(String address) {
        return cachedPeripheral(address);
    }

    public List<BlePeripheral> getConnectedPeripherals(
            BluetoothUuid serviceFilter) {
        ArrayList<BlePeripheral> out = new ArrayList<BlePeripheral>();
        for (String address : connectedAddresses) {
            NativeBlePeripheral p = cachedPeripheral(address);
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
        shutdownInternal(true);
    }

    private void shutdownInternal(boolean removeHook) {
        Process p;
        synchronized (processLock) {
            shutdownRequested = true;
            p = helper;
            if (helperIn != null) {
                try {
                    helperIn.write("{\"cmd\":\"shutdown\"}\n");
                    helperIn.flush();
                } catch (IOException ignored) {
                }
                try {
                    helperIn.close();
                } catch (IOException ignored) {
                }
                helperIn = null;
            }
            helper = null;
            if (removeHook && shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (IllegalStateException ignored) {
                    // VM already exiting
                }
                shutdownHook = null;
            }
        }
        scanSink = null;
        failAllPending(new BluetoothException(BluetoothError.IO_ERROR,
                "The native Bluetooth backend was shut down"));
        if (p != null) {
            try {
                // grace period for the helper to honor the shutdown command
                long deadline = System.currentTimeMillis() + 2000;
                while (p.isAlive()
                        && System.currentTimeMillis() < deadline) {
                    Thread.sleep(20);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            if (p.isAlive()) {
                p.destroy();
            }
        }
    }

    // ------------------------------------------------------------------
    // wire codec
    // ------------------------------------------------------------------

    /**
     * The line-delimited JSON codec of the helper protocol. Package-visible
     * for deterministic unit tests.
     */
    static final class Wire {

        private Wire() {
        }

        /** Builder for one command line. */
        static final class Obj {
            private final StringBuilder sb = new StringBuilder("{");
            private boolean first = true;

            private void key(String k) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('"').append(escape(k)).append("\":");
            }

            Obj put(String k, String v) {
                key(k);
                sb.append('"').append(escape(v)).append('"');
                return this;
            }

            Obj put(String k, long v) {
                key(k);
                sb.append(v);
                return this;
            }

            Obj put(String k, boolean v) {
                key(k);
                sb.append(v);
                return this;
            }

            /** The finished single-line JSON object. */
            String line() {
                return sb.toString() + "}";
            }
        }

        static Obj obj() {
            return new Obj();
        }

        static String escape(String s) {
            if (s == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder(s.length());
            int len = s.length();
            for (int i = 0; i < len; i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"':
                        sb.append("\\\"");
                        break;
                    case '\\':
                        sb.append("\\\\");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    default:
                        if (c < 0x20) {
                            String t = "000" + Integer.toHexString(c);
                            sb.append("\\u").append(
                                    t.substring(t.length() - 4));
                        } else {
                            sb.append(c);
                        }
                }
            }
            return sb.toString();
        }

        /** Parses one event line into a map. */
        static Map<String, Object> parse(String line) throws IOException {
            JSONParser parser = new JSONParser();
            parser.setUseLongsInstance(true);
            parser.setUseBooleanInstance(true);
            return parser.parseJSON(new StringReader(line));
        }

        static String str(Map<String, Object> m, String key, String def) {
            Object v = m.get(key);
            return v == null ? def : v.toString();
        }

        static long longVal(Map<String, Object> m, String key, long def) {
            Object v = m.get(key);
            if (v instanceof Number) {
                return ((Number) v).longValue();
            }
            if (v instanceof String) {
                try {
                    return Long.parseLong((String) v);
                } catch (NumberFormatException ignored) {
                }
            }
            return def;
        }

        static int intVal(Map<String, Object> m, String key, int def) {
            return (int) longVal(m, key, def);
        }

        static boolean boolVal(Map<String, Object> m, String key,
                boolean def) {
            Object v = m.get(key);
            if (v instanceof Boolean) {
                return ((Boolean) v).booleanValue();
            }
            if (v instanceof String) {
                return "true".equals(v);
            }
            return def;
        }

        @SuppressWarnings("unchecked")
        static List<Object> list(Map<String, Object> m, String key) {
            Object v = m.get(key);
            return v instanceof List ? (List<Object>) v
                    : new ArrayList<Object>();
        }

        @SuppressWarnings("unchecked")
        static Map<String, Object> map(Object v) {
            return v instanceof Map ? (Map<String, Object>) v
                    : new HashMap<String, Object>();
        }

        static byte[] decodeBase64(String s) {
            if (s == null || s.length() == 0) {
                return new byte[0];
            }
            return java.util.Base64.getDecoder().decode(s);
        }

        static String encodeBase64(byte[] data) {
            if (data == null) {
                return "";
            }
            return java.util.Base64.getEncoder().encodeToString(data);
        }
    }
}
