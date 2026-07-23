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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Internal event decoder for the in-process native BLE engine
/// (`libcn1ble`). The engine's commands are typed native calls, so nothing is
/// serialized on the way out; only inbound events arrive as one JSON object
/// per {@link NativeBleBridge#pollEvent}, with Base64-encoded
/// characteristic/descriptor payloads decoded here. Package-private -- it is
/// not part of any API.
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

    // Standard Base64 (RFC 4648) is hand-rolled rather than using
    // java.util.Base64: this class is translated by ParparVM for the native
    // ports and java.util.Base64 is not part of the device API surface. Pure
    // array arithmetic, so it runs identically on the JVM and on-device.
    private static final char[] B64 =
            ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/")
                    .toCharArray();

    static String encodeBase64(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder((data.length + 2) / 3 * 4);
        int i = 0;
        while (i + 3 <= data.length) {
            int n = ((data[i] & 0xFF) << 16) | ((data[i + 1] & 0xFF) << 8)
                    | (data[i + 2] & 0xFF);
            sb.append(B64[(n >> 18) & 0x3F]).append(B64[(n >> 12) & 0x3F])
                    .append(B64[(n >> 6) & 0x3F]).append(B64[n & 0x3F]);
            i += 3;
        }
        int rem = data.length - i;
        if (rem == 1) {
            int n = (data[i] & 0xFF) << 16;
            sb.append(B64[(n >> 18) & 0x3F]).append(B64[(n >> 12) & 0x3F])
                    .append("==");
        } else if (rem == 2) {
            int n = ((data[i] & 0xFF) << 16) | ((data[i + 1] & 0xFF) << 8);
            sb.append(B64[(n >> 18) & 0x3F]).append(B64[(n >> 12) & 0x3F])
                    .append(B64[(n >> 6) & 0x3F]).append('=');
        }
        return sb.toString();
    }

    static byte[] decodeBase64(String s) {
        if (s == null || s.length() == 0) {
            return new byte[0];
        }
        int symbols = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '=') {
                break;
            }
            if (b64Value(c) >= 0) {
                symbols++;
            }
        }
        byte[] out = new byte[symbols * 6 / 8];
        int acc = 0;
        int bits = 0;
        int o = 0;
        for (int i = 0; i < s.length(); i++) {
            int v = b64Value(s.charAt(i));
            if (v < 0) {
                continue;
            }
            acc = (acc << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out[o++] = (byte) ((acc >> bits) & 0xFF);
                if (o >= out.length) {
                    break;
                }
            }
        }
        return out;
    }

    private static int b64Value(char c) {
        if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        }
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 26;
        }
        if (c >= '0' && c <= '9') {
            return c - '0' + 52;
        }
        if (c == '+') {
            return 62;
        }
        if (c == '/') {
            return 63;
        }
        return -1;
    }
}
