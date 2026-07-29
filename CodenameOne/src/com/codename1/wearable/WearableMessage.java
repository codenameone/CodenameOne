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
package com.codename1.wearable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A payload addressed to a path, used both for live messages and for replicated data.
///
/// The path is what the receiving side matches on -- `"/steps"`, `"/workout/start"` -- and works
/// like a URL path, so give related payloads a common prefix. Values are the primitive types every
/// wearable transport can carry natively on both platforms: string, int, long, double, boolean and
/// raw bytes.
///
/// ```java
/// WearableMessage m = new WearableMessage("/steps")
///         .put("count", 8412)
///         .put("goalReached", true);
/// WearableConnection.putData(m);
/// ```
///
/// Reads name a default, so a peer running an older version of your app that never sent a key gets
/// a sane value rather than an exception. That matters more than usual here: the two apps are
/// updated independently and can be different versions of each other for a long time.
public class WearableMessage {
    /// Wire format version, so a newer peer can recognize a payload it cannot parse instead of
    /// misreading it.
    private static final int FORMAT_VERSION = 1;

    private static final int TYPE_STRING = 1;
    private static final int TYPE_INT = 2;
    private static final int TYPE_LONG = 3;
    private static final int TYPE_DOUBLE = 4;
    private static final int TYPE_BOOLEAN = 5;
    private static final int TYPE_BYTES = 6;

    private final String path;
    private final Map<String, Object> values = new LinkedHashMap<String, Object>();

    /// Creates an empty message addressed to a path.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path the peer matches on, conventionally starting with `/`
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the path is null or empty
    public WearableMessage(String path) {
        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException("A wearable message needs a path");
        }
        this.path = path;
    }

    /// Returns the path this message is addressed to.
    ///
    /// #### Returns
    ///
    /// the path
    public String getPath() {
        return path;
    }

    /// Returns the keys carried by this message, in insertion order.
    ///
    /// #### Returns
    ///
    /// the keys present in the payload
    public List<String> getKeys() {
        return new ArrayList<String>(values.keySet());
    }

    /// Returns true if the payload carries a value under the supplied key.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key to look for
    ///
    /// #### Returns
    ///
    /// true if the key is present
    public boolean contains(String key) {
        return values.containsKey(key);
    }

    /// Adds a string value.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    /// - `value`: the value; a null value removes the key
    ///
    /// #### Returns
    ///
    /// this message, for chaining
    public WearableMessage put(String key, String value) {
        return set(key, value);
    }

    /// Adds an int value.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    /// - `value`: the value
    ///
    /// #### Returns
    ///
    /// this message, for chaining
    public WearableMessage put(String key, int value) {
        return set(key, new Integer(value));
    }

    /// Adds a long value.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    /// - `value`: the value
    ///
    /// #### Returns
    ///
    /// this message, for chaining
    public WearableMessage put(String key, long value) {
        return set(key, new Long(value));
    }

    /// Adds a double value.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    /// - `value`: the value
    ///
    /// #### Returns
    ///
    /// this message, for chaining
    public WearableMessage put(String key, double value) {
        return set(key, new Double(value));
    }

    /// Adds a boolean value.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    /// - `value`: the value
    ///
    /// #### Returns
    ///
    /// this message, for chaining
    public WearableMessage put(String key, boolean value) {
        return set(key, Boolean.valueOf(value));
    }

    /// Adds a raw byte payload. Keep it small: a message is delivered over a low-bandwidth link and
    /// the platforms reject oversized payloads outright. Use
    /// [WearableConnection#transferFile(String,String,byte[])] for anything substantial.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    /// - `value`: the bytes; a null value removes the key
    ///
    /// #### Returns
    ///
    /// this message, for chaining
    public WearableMessage put(String key, byte[] value) {
        return set(key, value);
    }

    private WearableMessage set(String key, Object value) {
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("A wearable message value needs a key");
        }
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
        return this;
    }

    /// Reads a string value.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    /// - `defaultValue`: returned when the key is absent or holds another type
    ///
    /// #### Returns
    ///
    /// the value, or the default
    public String getString(String key, String defaultValue) {
        Object o = values.get(key);
        return o instanceof String ? (String) o : defaultValue;
    }

    /// Reads an int value. Accepts any numeric value, so a peer that sent a long or a double still
    /// reads back sensibly.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    /// - `defaultValue`: returned when the key is absent or holds a non-numeric type
    ///
    /// #### Returns
    ///
    /// the value, or the default
    public int getInt(String key, int defaultValue) {
        Object o = values.get(key);
        return o instanceof Number ? ((Number) o).intValue() : defaultValue;
    }

    /// Reads a long value. Accepts any numeric value.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    /// - `defaultValue`: returned when the key is absent or holds a non-numeric type
    ///
    /// #### Returns
    ///
    /// the value, or the default
    public long getLong(String key, long defaultValue) {
        Object o = values.get(key);
        return o instanceof Number ? ((Number) o).longValue() : defaultValue;
    }

    /// Reads a double value. Accepts any numeric value.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    /// - `defaultValue`: returned when the key is absent or holds a non-numeric type
    ///
    /// #### Returns
    ///
    /// the value, or the default
    public double getDouble(String key, double defaultValue) {
        Object o = values.get(key);
        return o instanceof Number ? ((Number) o).doubleValue() : defaultValue;
    }

    /// Reads a boolean value.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    /// - `defaultValue`: returned when the key is absent or holds another type
    ///
    /// #### Returns
    ///
    /// the value, or the default
    public boolean getBoolean(String key, boolean defaultValue) {
        Object o = values.get(key);
        return o instanceof Boolean ? ((Boolean) o).booleanValue() : defaultValue;
    }

    /// Reads a raw byte payload.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    /// - `defaultValue`: returned when the key is absent or holds another type
    ///
    /// #### Returns
    ///
    /// the value, or the default
    public byte[] getBytes(String key, byte[] defaultValue) {
        Object o = values.get(key);
        return o instanceof byte[] ? (byte[]) o : defaultValue;
    }

    // --- wire format --------------------------------------------------------

    /// Serializes the payload to the compact form the platform bridges carry. Application code does
    /// not normally call this; [WearableConnection] does it on the way out.
    ///
    /// #### Returns
    ///
    /// the encoded payload, never null
    public byte[] toByteArray() {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bo);
        try {
            out.writeByte(FORMAT_VERSION);
            out.writeShort(values.size());
            for (Map.Entry<String, Object> e : values.entrySet()) {
                out.writeUTF(e.getKey());
                Object v = e.getValue();
                if (v instanceof String) {
                    out.writeByte(TYPE_STRING);
                    out.writeUTF((String) v);
                } else if (v instanceof Integer) {
                    out.writeByte(TYPE_INT);
                    out.writeInt(((Integer) v).intValue());
                } else if (v instanceof Long) {
                    out.writeByte(TYPE_LONG);
                    out.writeLong(((Long) v).longValue());
                } else if (v instanceof Double) {
                    out.writeByte(TYPE_DOUBLE);
                    out.writeDouble(((Double) v).doubleValue());
                } else if (v instanceof Boolean) {
                    out.writeByte(TYPE_BOOLEAN);
                    out.writeBoolean(((Boolean) v).booleanValue());
                } else {
                    byte[] b = (byte[]) v;
                    out.writeByte(TYPE_BYTES);
                    out.writeInt(b.length);
                    out.write(b);
                }
            }
            out.flush();
        } catch (IOException err) {
            // A ByteArrayOutputStream cannot fail; rethrowing keeps callers honest
            // if that ever stops being true.
            throw new IllegalStateException("Failed to encode wearable payload: " + err);
        }
        return bo.toByteArray();
    }

    /// Reconstructs a payload received from the peer. Application code does not normally call this;
    /// [WearableConnection] does it on the way in.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path the payload arrived on
    /// - `data`: the encoded payload, may be null or empty for a payload with no values
    ///
    /// #### Returns
    ///
    /// the decoded message, never null; a payload this build cannot parse decodes to an empty
    /// message on the same path rather than throwing
    public static WearableMessage fromByteArray(String path, byte[] data) {
        WearableMessage m = new WearableMessage(path);
        if (data == null || data.length == 0) {
            return m;
        }
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        try {
            int version = in.readByte();
            if (version != FORMAT_VERSION) {
                // A peer running a future version of the app. Reading on would
                // produce garbage values, which is worse than no values at all.
                com.codename1.io.Log.p("Wearable: ignoring a payload on " + path
                        + " in wire format " + version + "; this build understands "
                        + FORMAT_VERSION);
                return m;
            }
            int count = in.readShort();
            for (int i = 0; i < count; i++) {
                String key = in.readUTF();
                int type = in.readByte();
                switch (type) {
                    case TYPE_STRING:
                        m.put(key, in.readUTF());
                        break;
                    case TYPE_INT:
                        m.put(key, in.readInt());
                        break;
                    case TYPE_LONG:
                        m.put(key, in.readLong());
                        break;
                    case TYPE_DOUBLE:
                        m.put(key, in.readDouble());
                        break;
                    case TYPE_BOOLEAN:
                        m.put(key, in.readBoolean());
                        break;
                    case TYPE_BYTES:
                        byte[] b = new byte[in.readInt()];
                        in.readFully(b);
                        m.put(key, b);
                        break;
                    default:
                        com.codename1.io.Log.p("Wearable: unknown value type " + type
                                + " on " + path + "; the rest of the payload is unreadable");
                        return m;
                }
            }
        } catch (IOException err) {
            com.codename1.io.Log.p("Wearable: truncated payload on " + path + ": " + err);
        }
        return m;
    }

    @Override
    public String toString() {
        return "WearableMessage[" + path + " " + values.keySet() + "]";
    }
}
