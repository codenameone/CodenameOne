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

import com.codename1.io.JSONParser;
import com.codename1.util.Base64;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Internal decoder for the inbound events of the in-process native BLE engine
/// (`libcn1ble`) -- one JSON object per {@link NativeBleBridge#pollEvent}. The
/// engine's commands are typed native calls, so nothing is serialized on the
/// way out. This is not a JSON implementation: parsing is the core
/// {@link JSONParser} and Base64 payloads use the core {@link Base64}; the
/// helpers here are just typed reads over the parsed event map.
/// Package-private -- not part of any API.
final class Json {

    private Json() {
    }

    /// Parses one event object.
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
        return v instanceof List ? (List<Object>) v
                : new ArrayList<Object>();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object v) {
        return v instanceof Map ? (Map<String, Object>) v
                : new HashMap<String, Object>();
    }

    /// Standard-Base64 encode of a characteristic/descriptor value, delegating
    /// to the core {@link Base64} (device-safe; not {@code java.util.Base64}).
    static String encodeBase64(byte[] data) {
        return data == null || data.length == 0 ? ""
                : Base64.encodeNoNewline(data);
    }

    /// Standard-Base64 decode via the core {@link Base64}.
    static byte[] decodeBase64(String s) {
        return s == null || s.length() == 0 ? new byte[0]
                : Base64.decode(s.getBytes());
    }
}
