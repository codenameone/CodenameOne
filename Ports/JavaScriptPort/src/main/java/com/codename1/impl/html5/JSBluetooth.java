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
package com.codename1.impl.html5;

import com.codename1.bluetooth.AdapterState;
import com.codename1.bluetooth.Bluetooth;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.BluetoothPermission;
import com.codename1.io.JSONParser;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Web Bluetooth backend of the Codename One Bluetooth API for the
 * JavaScript port.
 *
 * <p>Architecture: the translated Java runs in a Web Worker where
 * {@code navigator.bluetooth} does not exist. Every {@code nativeBt*}
 * method below is bound in {@code port.js} to a generator that performs an
 * {@code invokeHostNative("__cn1_bt_*__", ...)} round-trip to the main
 * thread, where {@code browser_bridge.js} owns the actual
 * {@code BluetoothDevice}/{@code BluetoothRemoteGATTServer} handles keyed
 * by opaque string/int ids. The worker side only ever holds those ids.</p>
 *
 * <p>Every native returns a JSON string of the shape
 * {@code {"ok":1, ...}} on success or
 * {@code {"ok":0,"code":"<BluetoothError name>","message":"..."}} on
 * failure, so failures always surface as typed
 * {@link BluetoothException}s -- including the case of a stale
 * {@code browser_bridge.js} that predates the {@code __cn1_bt_*__}
 * handlers (the port.js bindings catch the unhandled-host-call rejection
 * and synthesize a {@code NOT_SUPPORTED} result instead of throwing).</p>
 *
 * <p>Host-initiated events (adapter availability changes, GATT server
 * disconnects, characteristic notifications) flow back through a single
 * worker-callback registered at {@link #nativeBtInit()} time; port.js
 * converts each payload to Java types and invokes
 * {@link #dispatchNativeEvent(String, String, String, byte[])}.</p>
 *
 * <p>Capabilities: only the BLE central role is available. Web Bluetooth
 * exposes no peripheral mode, no classic Bluetooth and no L2CAP channels;
 * those report {@code false} here and their operations fail fast with
 * {@link BluetoothError#NOT_SUPPORTED} through the core fallbacks.</p>
 */
public class JSBluetooth extends Bluetooth {

    private static JSBluetooth instance;

    private final JSBluetoothLE le;
    private final HashMap<String, JSBlePeripheral> peripherals =
            new HashMap<String, JSBlePeripheral>();
    private final boolean supported;
    private AdapterState adapterState;

    public JSBluetooth() {
        instance = this;
        le = new JSBluetoothLE(this);
        boolean sup = false;
        AdapterState state = AdapterState.UNSUPPORTED;
        try {
            sup = nativeBtSupported() == 1;
            if (sup) {
                nativeBtInit();
                Map<String, Object> res = parseResult(nativeBtAdapterState());
                state = mapAdapterState(JSONParser.getString(res, "state"));
            }
        } catch (Throwable t) {
            sup = false;
            state = AdapterState.UNSUPPORTED;
        }
        supported = sup;
        adapterState = state;
        // Keep dispatchNativeEvent reachable for the translator's dead-code
        // elimination: port.js invokes it by its mangled name at event time,
        // which the reachability analysis cannot see. This call is a no-op
        // (the "__" prefix is filtered out) but forms a real call site.
        dispatchNativeEvent("__keepalive__", null, null, null);
    }

    // ------------------------------------------------------------------
    // capability surface
    // ------------------------------------------------------------------

    public boolean isSupported() {
        return supported;
    }

    public boolean isLeSupported() {
        return supported;
    }

    // classic / peripheral / L2CAP deliberately stay at the base-class
    // false: Web Bluetooth has no API for any of them.

    public AdapterState getAdapterState() {
        if (!supported) {
            return AdapterState.UNSUPPORTED;
        }
        return adapterState;
    }

    public com.codename1.bluetooth.le.BluetoothLE getLE() {
        return le;
    }

    /**
     * Web Bluetooth has no standalone permission grant -- authorization is
     * per-device and happens inside the {@code requestDevice} chooser. As
     * long as the API is available the SCAN/CONNECT permissions are
     * considered granted; the chooser itself is the real gate.
     */
    public boolean hasPermission(BluetoothPermission permission) {
        return supported;
    }

    public AsyncResource<Boolean> requestPermissions(
            BluetoothPermission... permissions) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.complete(supported ? Boolean.TRUE : Boolean.FALSE);
        return r;
    }

    // ------------------------------------------------------------------
    // peripheral registry
    // ------------------------------------------------------------------

    JSBlePeripheral obtainPeripheral(String deviceId, String name) {
        synchronized (peripherals) {
            JSBlePeripheral p = peripherals.get(deviceId);
            if (p == null) {
                p = new JSBlePeripheral(deviceId, name);
                peripherals.put(deviceId, p);
            } else {
                p.setDeviceName(name);
            }
            return p;
        }
    }

    JSBlePeripheral peripheralOrNull(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        synchronized (peripherals) {
            return peripherals.get(deviceId);
        }
    }

    // ------------------------------------------------------------------
    // event entry point from port.js
    // ------------------------------------------------------------------

    /**
     * Invoked (via a spawned green thread) by the port.js worker-callback
     * that browser_bridge.js posts host events into. Also called once from
     * the constructor with a {@code "__keepalive__"} kind so the
     * translator keeps this method alive.
     *
     * <p>Kinds: {@code adapter} (detail = AdapterState name),
     * {@code disconnect} (deviceId), {@code notify} (deviceId, detail =
     * characteristic instance id, value = notification bytes).</p>
     */
    public static void dispatchNativeEvent(String kind, String deviceId,
            String detail, byte[] value) {
        JSBluetooth bt = instance;
        if (bt == null || kind == null || kind.startsWith("__")) {
            return;
        }
        if ("adapter".equals(kind)) {
            AdapterState s = mapAdapterState(detail);
            bt.adapterState = s;
            bt.fireAdapterStateChanged(s);
            return;
        }
        JSBlePeripheral p = bt.peripheralOrNull(deviceId);
        if (p == null) {
            return;
        }
        if ("disconnect".equals(kind)) {
            p.onNativeDisconnected();
        } else if ("notify".equals(kind)) {
            int iid = parseIntSafe(detail);
            if (iid >= 0) {
                p.onNativeNotification(iid, value);
            }
        }
    }

    // ------------------------------------------------------------------
    // shared helpers for the JS Bluetooth backend
    // ------------------------------------------------------------------

    /** Runs r off the caller's thread so blocking native round-trips never stall the EDT. */
    static void async(Runnable r) {
        new Thread(r, "WebBluetooth").start();
    }

    static Map<String, Object> parseResult(String json) {
        if (json == null || json.length() == 0) {
            return null;
        }
        try {
            return JSONParser.parseJSON(json);
        } catch (IOException e) {
            return null;
        }
    }

    static boolean isOk(Map<String, Object> result) {
        return result != null && JSONParser.getInt(result, "ok", 0) == 1;
    }

    static BluetoothException toException(Map<String, Object> result,
            BluetoothError fallback, String operation) {
        if (result == null) {
            return new BluetoothException(fallback, operation
                    + " failed: no response from the Bluetooth host bridge");
        }
        BluetoothError error = fallback;
        String code = JSONParser.getString(result, "code");
        if (code != null) {
            try {
                error = BluetoothError.valueOf(code);
            } catch (IllegalArgumentException ignored) {
            }
        }
        String message = JSONParser.getString(result, "message");
        return new BluetoothException(error, operation + " failed"
                + (message == null || message.length() == 0
                        ? "" : ": " + message));
    }

    static AdapterState mapAdapterState(String name) {
        if (name != null) {
            try {
                return AdapterState.valueOf(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return AdapterState.UNKNOWN;
    }

    static int parseIntSafe(String s) {
        if (s == null) {
            return -1;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Minimal JSON string escaper for values embedded in request options. */
    static void appendJsonString(StringBuilder sb, String value) {
        sb.append('"');
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
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
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int p = hex.length(); p < 4; p++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------
    // natives -- bound in port.js, backed by __cn1_bt_*__ host handlers
    // in browser_bridge.js. All return the JSON result contract described
    // in the class javadoc (except the two int-returning probes).
    // ------------------------------------------------------------------

    /** 1 when navigator.bluetooth.requestDevice exists on the main thread. */
    static native int nativeBtSupported();

    /** Registers the host-to-worker event callback; 1 on success. */
    static native int nativeBtInit();

    /** {"ok":1,"state":"POWERED_ON"|"POWERED_OFF"|"UNKNOWN"|"UNSUPPORTED"} */
    static native String nativeBtAdapterState();

    /**
     * Reads the (setter-only) criteria of a core ScanFilter directly off
     * its translated field representation in the worker -- the core API
     * deliberately exposes no getters to ports, but the JS port lives
     * inside the translated object model where the fields are visible.
     * Returns a JSON object with optional keys: service (uuid string),
     * name, namePrefix, address, manufacturerId (int, -1 when unset),
     * manufacturerData / manufacturerDataMask (int arrays).
     */
    static native String nativeBtExtractScanFilter(Object filter);

    /**
     * Runs the Web Bluetooth chooser. optionsJson mirrors the
     * requestDevice options dictionary ({@code filters} /
     * {@code acceptAllDevices} / {@code optionalServices}). Success:
     * {"ok":1,"id":"...","name":"..."}.
     */
    static native String nativeBtRequestDevice(String optionsJson);

    static native String nativeBtConnect(String deviceId);

    static native String nativeBtDisconnect(String deviceId);

    /**
     * One-shot full GATT database discovery -- a single host round-trip
     * returning {"ok":1,"services":[{uuid,primary,iid,characteristics:
     * [{uuid,iid,properties,descriptors:[{uuid,iid}]}]}]}. The iid values
     * are host-side handle ids used by the read/write/subscribe natives.
     */
    static native String nativeBtDiscoverServices(String deviceId);

    /** {"ok":1,"value":"<base64>"} */
    static native String nativeBtReadCharacteristic(String deviceId, int iid);

    static native String nativeBtWriteCharacteristic(String deviceId, int iid,
            byte[] value, boolean withResponse);

    /** {"ok":1,"value":"<base64>"} */
    static native String nativeBtReadDescriptor(String deviceId, int iid);

    static native String nativeBtWriteDescriptor(String deviceId, int iid,
            byte[] value);

    static native String nativeBtSetNotifications(String deviceId, int iid,
            boolean enable);
}
