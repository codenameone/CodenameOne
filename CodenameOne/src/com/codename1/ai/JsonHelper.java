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
package com.codename1.ai;

import com.codename1.io.JSONParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

/// Internal helper. Concentrates the unchecked casts that
/// `JSONParser`'s `Hashtable`-era return types force on us, and
/// provides a tiny `Map`/`List`-to-JSON serializer so request bodies
/// can be built without bringing in a third-party JSON library.
final class JsonHelper {

    private JsonHelper() {
    }

    static Map parseObject(String json) throws IOException {
        return parseObject(json.getBytes("UTF-8"));
    }

    static Map parseObject(byte[] bytes) throws IOException {
        Reader r = new InputStreamReader(new ByteArrayInputStream(bytes), "UTF-8");
        try {
            return new JSONParser().parseJSON(r);
        } finally {
            try {
                r.close();
            } catch (IOException ignored) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object o) {
        if (o == null) {
            return null;
        }
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    static List<Object> asList(Object o) {
        if (o == null) {
            return null;
        }
        return (List<Object>) o;
    }

    static String string(Map m, String key) {
        if (m == null) {
            return null;
        }
        Object v = m.get(key);
        return v == null ? null : v.toString();
    }

    static int intValue(Map m, String key, int defaultValue) {
        if (m == null) {
            return defaultValue;
        }
        Object v = m.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    static double doubleValue(Map m, String key, double defaultValue) {
        if (m == null) {
            return defaultValue;
        }
        Object v = m.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    /// Serializes a `Map`/`List`/`String`/`Number`/`Boolean`/`null`
    /// tree to JSON. Keys must be `String`s. Raw `String` values that
    /// already contain JSON are NOT detected -- wrap them in a marker
    /// (see [RawJson]) to inline pre-built fragments like tool
    /// parameter schemas.
    static String serialize(Object o) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, o);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object o) {
        if (o == null) {
            sb.append("null");
            return;
        }
        if (o instanceof RawJson) {
            String s = ((RawJson) o).getJson();
            sb.append(s == null || s.length() == 0 ? "null" : s);
            return;
        }
        if (o instanceof String) {
            writeString(sb, (String) o);
            return;
        }
        if (o instanceof Boolean) {
            sb.append(((Boolean) o).booleanValue() ? "true" : "false");
            return;
        }
        if (o instanceof Number) {
            // Avoid 1.0E10 etc. when an integer fits; otherwise the
            // provider may reject the body.
            Number n = (Number) o;
            if (n instanceof Float || n instanceof Double) {
                double d = n.doubleValue();
                if (Double.isInfinite(d) || Double.isNaN(d)) {
                    sb.append("null");
                } else {
                    sb.append(d);
                }
            } else {
                sb.append(n.longValue());
            }
            return;
        }
        if (o instanceof Map) {
            sb.append('{');
            boolean first = true;
            Map m = (Map) o;
            for (Object kObj : m.keySet()) {
                Object v = m.get(kObj);
                if (v == null) {
                    // Skip nulls so "don't send" fields don't appear
                    // as `"field":null` on the wire -- many providers
                    // reject that.
                    continue;
                }
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, kObj.toString());
                sb.append(':');
                writeValue(sb, v);
            }
            sb.append('}');
            return;
        }
        if (o instanceof List) {
            sb.append('[');
            List l = (List) o;
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                writeValue(sb, l.get(i));
            }
            sb.append(']');
            return;
        }
        // Last resort: stringify.
        writeString(sb, o.toString());
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
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
                        sb.append("\\u");
                        String h = Integer.toHexString(c);
                        for (int p = h.length(); p < 4; p++) {
                            sb.append('0');
                        }
                        sb.append(h);
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    /// Marker that lets a caller embed a pre-built JSON fragment
    /// (e.g. a tool's `parametersJsonSchema`) into a Map tree without
    /// having it re-escaped as a string.
    static final class RawJson {
        private final String json;

        RawJson(String json) {
            this.json = json;
        }

        String getJson() {
            return json;
        }
    }
}
