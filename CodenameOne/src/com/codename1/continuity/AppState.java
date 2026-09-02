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
package com.codename1.continuity;

import com.codename1.io.Externalizable;
import com.codename1.io.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// A snapshot of where the user was and what they were doing: the route stack, plus whatever your
/// `StateProvider` chose to add.
///
/// The same value serves three purposes, which is why it carries more than the two halves above.
/// It is written to storage so the app can come back after its process dies; it is advertised to
/// the user's other devices so one of them can continue the work; and it travels through a
/// `StateRelay` to devices the platform cannot reach on its own. The `deviceId`, `sequence` and
/// `timestamp` are what let the receiving side tell a state it has already seen -- or its own echo
/// -- from one worth acting on.
///
/// #### The routes
///
/// `getRoutes()` is the `com.codename1.router.Navigation` stack as a list of paths, oldest first.
/// Restoring it re-runs each path through the route table, which is why an app that navigates with
/// `@Route` gets its screens back for free and one that calls `new MyForm().show()` does not: those
/// navigations are not URL-addressable, so there is nothing to write down. Such an app restores
/// from the payload instead.
///
/// #### The payload
///
/// `getPayload()` is yours. It has to survive being written to disk, handed to an operating system
/// and delivered to a *different device running a possibly different build of your app*, so it is
/// restricted to values that mean the same thing everywhere: `String`, `Integer`, `Long`, `Double`,
/// `Boolean`, and `List` and `Map` of those. Anything else is refused when the state is built,
/// with a message naming the offending key, rather than being dropped somewhere the failure cannot
/// be traced back here.
public final class AppState implements Externalizable {
    /// The `Util.register` id. Changing it orphans every state already on a device.
    static final String OBJECT_ID = "CN1AppState";

    private List<String> routes = new ArrayList<String>();
    private Map<String, Object> payload = new HashMap<String, Object>();
    private String deviceId = "";
    private String title;
    private long sequence;
    private long timestamp;

    /// Creates an empty state. Applications normally obtain one from
    /// `Continuity.getRestorableState()` or receive one through a `ContinuityListener`; this is
    /// public so tests and relays can build one.
    public AppState() {
    }

    /// The navigation stack as route paths, oldest first. Never null, possibly empty.
    ///
    /// #### Returns
    ///
    /// an unmodifiable view of the route paths
    public List<String> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    /// Replaces the route paths.
    ///
    /// #### Parameters
    ///
    /// - `r`: the paths, oldest first; null is treated as empty
    ///
    /// #### Returns
    ///
    /// this state, for chaining
    public AppState setRoutes(List<String> r) {
        routes = new ArrayList<String>();
        if (r != null) {
            for (Iterator<String> i = r.iterator(); i.hasNext();) {
                String path = i.next();
                if (path != null && path.length() > 0) {
                    routes.add(path);
                }
            }
        }
        return this;
    }

    /// The application payload. Never null, possibly empty.
    ///
    /// #### Returns
    ///
    /// an unmodifiable view of the payload
    public Map<String, Object> getPayload() {
        return Collections.unmodifiableMap(payload);
    }

    /// Replaces the application payload.
    ///
    /// #### Parameters
    ///
    /// - `p`: the payload; null is treated as empty
    ///
    /// #### Returns
    ///
    /// this state, for chaining
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when a value cannot cross to another device
    public AppState setPayload(Map<String, Object> p) {
        StateCodec.requireRepresentable(p);
        payload = new HashMap<String, Object>();
        if (p != null) {
            payload.putAll(p);
        }
        return this;
    }

    /// Replaces the payload without validating it. Used only for a payload that arrived from
    /// another device: it was already validated where it was produced, and refusing it here would
    /// turn a remote mistake into an exception on this device at a moment the user cannot connect
    /// to anything they did.
    ///
    /// #### Parameters
    ///
    /// - `p`: the payload; null is treated as empty
    void setPayloadUnchecked(Map<String, Object> p) {
        payload = new HashMap<String, Object>();
        if (p != null) {
            payload.putAll(p);
        }
    }

    /// The device this state was produced on. Used to drop a state's own echo when it comes back
    /// through a relay. Never null.
    ///
    /// #### Returns
    ///
    /// the originating device id
    public String getDeviceId() {
        return deviceId;
    }

    /// Sets the originating device id.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id; null is treated as the empty string
    ///
    /// #### Returns
    ///
    /// this state, for chaining
    public AppState setDeviceId(String id) {
        deviceId = id == null ? "" : id;
        return this;
    }

    /// A human readable label for what the user is doing, which a receiving device may show
    /// before they accept the continuation. Null when the app did not set one.
    ///
    /// #### Returns
    ///
    /// the title, or null
    public String getTitle() {
        return title;
    }

    /// Sets the human readable label.
    ///
    /// #### Parameters
    ///
    /// - `t`: the title, or null for none
    ///
    /// #### Returns
    ///
    /// this state, for chaining
    public AppState setTitle(String t) {
        title = t;
        return this;
    }

    /// A counter that increases with every state this device publishes. Together with the device
    /// id it identifies a state exactly, which is how a receiver recognizes one it has already
    /// acted on -- two states can share a timestamp, because clocks are coarse.
    ///
    /// #### Returns
    ///
    /// the sequence number
    public long getSequence() {
        return sequence;
    }

    /// Sets the sequence number.
    ///
    /// #### Parameters
    ///
    /// - `s`: the sequence number
    ///
    /// #### Returns
    ///
    /// this state, for chaining
    public AppState setSequence(long s) {
        sequence = s;
        return this;
    }

    /// When this state was produced, as milliseconds since the epoch on the producing device.
    ///
    /// Treat it as advisory. It comes from another device's clock, so it is only as trustworthy as
    /// that clock: it can be behind, ahead, or -- across a daylight saving change or a manual
    /// correction -- both within one session.
    ///
    /// #### Returns
    ///
    /// the timestamp
    public long getTimestamp() {
        return timestamp;
    }

    /// Sets the production timestamp.
    ///
    /// #### Parameters
    ///
    /// - `t`: milliseconds since the epoch
    ///
    /// #### Returns
    ///
    /// this state, for chaining
    public AppState setTimestamp(long t) {
        timestamp = t;
        return this;
    }

    /// True when there is nothing here worth restoring or sending.
    ///
    /// #### Returns
    ///
    /// true when both the routes and the payload are empty
    public boolean isEmpty() {
        return routes.isEmpty() && payload.isEmpty();
    }

    @Override
    public String toString() {
        return "AppState{routes=" + routes.size() + ", payload=" + payload.size()
                + ", device=" + deviceId + ", seq=" + sequence + "}";
    }

    // ------------------------------------------------------------------
    // Externalizable -- the on-device format
    // ------------------------------------------------------------------

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String getObjectId() {
        return OBJECT_ID;
    }

    @Override
    public void externalize(DataOutputStream out) throws IOException {
        Util.writeUTF(deviceId, out);
        Util.writeUTF(title, out);
        out.writeLong(sequence);
        out.writeLong(timestamp);
        out.writeInt(routes.size());
        for (Iterator<String> i = routes.iterator(); i.hasNext();) {
            Util.writeUTF(i.next(), out);
        }
        // The payload goes through the framework's own object writer rather than a hand-rolled
        // encoding: it already knows every type requireRepresentable admits, including nested
        // lists and maps, and it is the same writer Storage uses for everything else.
        Util.writeObject(payload, out);
    }

    @Override
    public void internalize(int version, DataInputStream in) throws IOException {
        deviceId = Util.readUTF(in);
        if (deviceId == null) {
            deviceId = "";
        }
        title = Util.readUTF(in);
        sequence = in.readLong();
        timestamp = in.readLong();
        int count = in.readInt();
        routes = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            String path = Util.readUTF(in);
            if (path != null && path.length() > 0) {
                routes.add(path);
            }
        }
        Object p = Util.readObject(in);
        payload = new HashMap<String, Object>();
        if (p instanceof Map) {
            Map<?, ?> read = (Map<?, ?>) p;
            for (Iterator<? extends Map.Entry<?, ?>> i = read.entrySet().iterator();
                    i.hasNext();) {
                Map.Entry<?, ?> entry = i.next();
                if (entry.getKey() instanceof String) {
                    payload.put((String) entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
