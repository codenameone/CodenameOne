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

import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.io.JSONParser;
import com.codename1.util.Base64;
import com.codename1.util.StringUtil;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON read helpers for the JavaSE simulator's Bluetooth fixture files
 * ({@link BluetoothFixture}). Parsing delegates to the core
 * {@code com.codename1.io.JSONParser} and Base64 payloads to the core
 * {@code com.codename1.util.Base64}; the helpers here are just typed reads
 * over the parsed map. Package-private -- the runtime BLE path in core has
 * no JSON in its API.
 */
final class FixtureJson {

    private FixtureJson() {
    }

    static Map<String, Object> parse(String text) throws IOException {
        JSONParser parser = new JSONParser();
        parser.setUseLongsInstance(true);
        parser.setUseBooleanInstance(true);
        return parser.parseJSON(new StringReader(text));
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

    static boolean boolVal(Map<String, Object> m, String key, boolean def) {
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
        return v instanceof List ? (List<Object>) v : new ArrayList<Object>();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object v) {
        return v instanceof Map ? (Map<String, Object>) v
                : new HashMap<String, Object>();
    }

    static String encodeBase64(byte[] data) {
        return data == null || data.length == 0 ? ""
                : Base64.encodeNoNewline(data);
    }

    static byte[] decodeBase64(String s) {
        return s == null || s.length() == 0 ? new byte[0]
                : Base64.decode(StringUtil.getBytes(s));
    }

    /** Maps fixture characteristic-property names onto GattCharacteristic bits. */
    static int propertiesMask(List<Object> names) {
        int mask = 0;
        for (int i = 0; i < names.size(); i++) {
            String p = String.valueOf(names.get(i));
            if ("broadcast".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_BROADCAST;
            } else if ("read".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_READ;
            } else if ("writeWithoutResponse".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_WRITE_WITHOUT_RESPONSE;
            } else if ("write".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_WRITE;
            } else if ("notify".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_NOTIFY;
            } else if ("indicate".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_INDICATE;
            } else if ("signedWrite".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_SIGNED_WRITE;
            } else if ("extendedProps".equals(p)) {
                mask |= GattCharacteristic.PROPERTY_EXTENDED_PROPS;
            }
        }
        return mask;
    }
}
