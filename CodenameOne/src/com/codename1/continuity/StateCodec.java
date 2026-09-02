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

import com.codename1.io.JSONParser;
import com.codename1.io.JSONWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// Turns an `AppState` into the two forms it has to travel in, and refuses payloads that cannot
/// make the trip.
///
/// The two forms are deliberately different. A *continuation* is handed to the operating system,
/// which stores it as a property list and may deliver it to another device, so it is a nested map
/// of plist-representable values. A *relay* payload crosses a network to a device that may not be
/// an Apple one at all, so it is JSON. Both are lossless for the value types the payload admits,
/// which is the whole reason the payload admits so few.
///
/// This class is public so that a `StateRelay` written by an application can use the same wire
/// format the built-in one does, and so tests can assert on it.
public final class StateCodec {
    private static final String KEY_ROUTES = "routes";
    private static final String KEY_PAYLOAD = "payload";
    private static final String KEY_DEVICE = "device";
    private static final String KEY_TITLE = "title";
    private static final String KEY_SEQUENCE = "seq";
    private static final String KEY_TIMESTAMP = "ts";

    private StateCodec() {
    }

    /// Renders a state as the nested map an operating system can carry between devices.
    ///
    /// #### Parameters
    ///
    /// - `state`: the state, must not be null
    ///
    /// #### Returns
    ///
    /// a map of plist-representable values
    public static Map<String, Object> toMap(AppState state) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put(KEY_ROUTES, new ArrayList<String>(state.getRoutes()));
        m.put(KEY_PAYLOAD, new HashMap<String, Object>(state.getPayload()));
        m.put(KEY_DEVICE, state.getDeviceId());
        if (state.getTitle() != null) {
            m.put(KEY_TITLE, state.getTitle());
        }
        // Written as strings rather than as numbers. Both survive a property list, but a JSON
        // round trip through JSONParser reads every number back as a Double, and a millisecond
        // timestamp is past the range a double represents exactly -- so the same two fields would
        // come back changed on the relay path and unchanged on the continuation path. One
        // encoding for both keeps a state comparable with itself however it arrived.
        m.put(KEY_SEQUENCE, Long.toString(state.getSequence()));
        m.put(KEY_TIMESTAMP, Long.toString(state.getTimestamp()));
        return m;
    }

    /// Rebuilds a state from the map form. Unknown keys are ignored, so a newer build of the app
    /// on another device can add fields without breaking this one.
    ///
    /// #### Parameters
    ///
    /// - `m`: the map, or null
    ///
    /// #### Returns
    ///
    /// the state, or null when the map is null or carries nothing recognizable
    public static AppState fromMap(Map<String, Object> m) {
        if (m == null) {
            return null;
        }
        AppState state = new AppState();
        Object routes = m.get(KEY_ROUTES);
        if (routes instanceof List) {
            List<String> paths = new ArrayList<String>();
            for (Iterator<?> i = ((List<?>) routes).iterator(); i.hasNext();) {
                Object path = i.next();
                if (path instanceof String) {
                    paths.add((String) path);
                }
            }
            state.setRoutes(paths);
        }
        Object payload = m.get(KEY_PAYLOAD);
        if (payload instanceof Map) {
            Map<String, Object> copy = new HashMap<String, Object>();
            Map<?, ?> read = (Map<?, ?>) payload;
            for (Iterator<? extends Map.Entry<?, ?>> i = read.entrySet().iterator();
                    i.hasNext();) {
                Map.Entry<?, ?> entry = i.next();
                if (entry.getKey() instanceof String) {
                    copy.put((String) entry.getKey(), entry.getValue());
                }
            }
            // Not validated on the way in. This map came from another device, and refusing it
            // would turn that device's mistake into an exception on this one at a moment the user
            // cannot connect to anything they did.
            state.setPayloadUnchecked(copy);
        }
        Object device = m.get(KEY_DEVICE);
        if (device instanceof String) {
            state.setDeviceId((String) device);
        }
        Object title = m.get(KEY_TITLE);
        if (title instanceof String) {
            state.setTitle((String) title);
        }
        state.setSequence(asLong(m.get(KEY_SEQUENCE)));
        state.setTimestamp(asLong(m.get(KEY_TIMESTAMP)));
        return state;
    }

    /// Renders a state as JSON, for a relay.
    ///
    /// #### Parameters
    ///
    /// - `state`: the state, must not be null
    ///
    /// #### Returns
    ///
    /// the JSON document
    public static String toJson(AppState state) {
        return JSONWriter.toJson(toMap(state));
    }

    /// Parses the JSON form.
    ///
    /// #### Parameters
    ///
    /// - `json`: the document, or null
    ///
    /// #### Returns
    ///
    /// the state, or null when the document is null, empty or not an object
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: when the document is malformed
    public static AppState fromJson(String json) throws IOException {
        if (json == null || json.trim().length() == 0) {
            return null;
        }
        return fromMap(JSONParser.parseJSON(json));
    }

    /// Throws when any value in the map could not survive being written to a property list, sent
    /// as JSON and read back by another build of the app on another device.
    ///
    /// The admitted types are `String`, `Integer`, `Long`, `Double`, `Boolean`, and `List` and
    /// `Map` of those. `Map` keys must be strings, because neither destination format has any
    /// other kind of key.
    ///
    /// #### Parameters
    ///
    /// - `payload`: the payload, or null
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: naming the path to the first offending value
    public static void requireRepresentable(Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        for (Iterator<Map.Entry<String, Object>> i = payload.entrySet().iterator();
                i.hasNext();) {
            Map.Entry<String, Object> entry = i.next();
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("A continuity payload cannot have a null key.");
            }
            check(entry.getValue(), entry.getKey(), 0);
        }
    }

    /// The number of characters the rendered JSON form occupies, which is the closest portable
    /// stand-in for what a payload costs on any of the transports.
    ///
    /// #### Parameters
    ///
    /// - `state`: the state
    ///
    /// #### Returns
    ///
    /// the encoded size in characters
    public static int encodedSize(AppState state) {
        return toJson(state).length();
    }

    private static void check(Object value, String path, int depth) {
        if (depth > 16) {
            // A payload cannot legitimately be this deep, and a cycle looks exactly like a very
            // deep tree until the stack runs out. Refused with the path so the shape is findable.
            throw new IllegalArgumentException("The continuity payload at \"" + path
                    + "\" nests more than 16 levels deep, or contains a cycle. Neither a property "
                    + "list nor JSON can represent a cycle.");
        }
        if (value == null || value instanceof String || value instanceof Integer
                || value instanceof Long || value instanceof Double || value instanceof Boolean) {
            return;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                check(list.get(i), path + "[" + i + "]", depth + 1);
            }
            return;
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            for (Iterator<? extends Map.Entry<?, ?>> i = map.entrySet().iterator();
                    i.hasNext();) {
                Map.Entry<?, ?> entry = i.next();
                Object key = entry.getKey();
                if (!(key instanceof String)) {
                    throw new IllegalArgumentException("The continuity payload at \"" + path
                            + "\" has a map key of type "
                            + (key == null ? "null" : key.getClass().getName())
                            + ". Only string keys can be written to a property list or to JSON.");
                }
                check(entry.getValue(), path + "." + key, depth + 1);
            }
            return;
        }
        throw new IllegalArgumentException("The continuity payload at \"" + path + "\" is a "
                + value.getClass().getName() + ". A continuity payload has to survive being "
                + "written to a property list and delivered to another device, possibly running a "
                + "different build of this app, so it admits only String, Integer, Long, Double, "
                + "Boolean, and List and Map of those. Convert this value before adding it.");
    }

    private static long asLong(Object o) {
        if (o instanceof String) {
            try {
                return Long.parseLong(((String) o).trim());
            } catch (NumberFormatException err) {
                return 0;
            }
        }
        // Never a cast: on ParparVM a failed CHECKCAST does not throw, so the guarded instanceof
        // is the only portable way to ask. A relay written before the string encoding, or a
        // hand-written server, can still send a number here.
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return 0;
    }
}
