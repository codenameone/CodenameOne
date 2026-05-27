/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details.
 */
package com.codename1.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Convenience JSON writer to complement [JSONParser]. Two access modes:
///
/// 1. **One-shot**: `JSONWriter.toJson(map)` /
///    `JSONWriter.toJson(map, writer)` -- accepts a `Map`, `List`, `String`,
///    `Number`, `Boolean`, `null`, and arbitrarily nested combinations.
///
/// 2. **Fluent builder**: `JSONWriter.object().put("name", x).put("values",
///    JSONWriter.array().add("a").add("b")).toJson()` -- for ad-hoc request
///    bodies where a `Map` literal would be noisier than the chain.
///
/// Encoded output is strict JSON: no trailing commas, all strings
/// double-quoted with the standard backslash escapes for `"`, `\`, `\n`,
/// `\r`, `\t`, and control chars `< 0x20` emitted as `\` + `u00xx`. No
/// pretty-printing layer is included; if you need indented output, run
/// the result through an external formatter at debug time.
///
/// For typed mapper-based serialization (DTOs annotated with `@Mapped`
/// from the binding framework), use `com.codename1.mapping.Mappers#toJson`
/// instead. `JSONWriter` is for ad-hoc and untyped payloads.
public final class JSONWriter {

    private JSONWriter() { }

    // ---- one-shot ----

    /// Encodes `value` as JSON and returns the resulting string. Accepts
    /// `Map`, `List`, `String`, `Number`, `Boolean`, `null`. Maps with
    /// non-String keys are encoded using `String.valueOf(key)`.
    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        writeJson(value, sb);
        return sb.toString();
    }

    /// Streams `value` as JSON into `writer`. The writer is **not** closed
    /// or flushed by this method -- the caller owns the writer.
    public static void toJson(Object value, Writer writer) throws IOException {
        StringBuilder sb = new StringBuilder();
        writeJson(value, sb);
        writer.write(sb.toString());
    }

    /// Streams `value` as JSON into `out` using UTF-8 encoding. The stream
    /// is flushed but **not** closed.
    public static void toJson(Object value, OutputStream out) throws IOException {
        OutputStreamWriter w = new OutputStreamWriter(out, "UTF-8");
        toJson(value, w);
        w.flush();
    }

    // ---- fluent builder ----

    /// Starts a JSON-object builder. Insertion order is preserved.
    public static ObjectBuilder object() {
        return new ObjectBuilder();
    }

    /// Starts a JSON-array builder.
    public static ArrayBuilder array() {
        return new ArrayBuilder();
    }

    /// Fluent builder for `{ "k": v, ... }`. Implements `toJson()` to emit
    /// the encoded string and exposes the backing `Map` via `toMap()` for
    /// callers that need to embed the builder into a larger structure.
    public static final class ObjectBuilder {
        private final Map<String, Object> map = new LinkedHashMap<String, Object>();

        ObjectBuilder() { }

        public ObjectBuilder put(String key, Object value) {
            map.put(key, unwrap(value));
            return this;
        }

        public Map<String, Object> toMap() { return map; }

        public String toJson() { return JSONWriter.toJson(map); }

        @Override
        public String toString() { return toJson(); }
    }

    /// Fluent builder for `[ ..., ..., ... ]`.
    public static final class ArrayBuilder {
        private final List<Object> list = new ArrayList<Object>();

        ArrayBuilder() { }

        public ArrayBuilder add(Object value) {
            list.add(unwrap(value));
            return this;
        }

        public List<Object> toList() { return list; }

        public String toJson() { return JSONWriter.toJson(list); }

        @Override
        public String toString() { return toJson(); }
    }

    // ---- internal encoding ----

    /// Lets builders embed each other transparently: `object().put("xs",
    /// array().add(1).add(2))` stores the *list*, not the builder wrapper.
    private static Object unwrap(Object value) {
        if (value instanceof ObjectBuilder) {
            return ((ObjectBuilder) value).map;
        }
        if (value instanceof ArrayBuilder) {
            return ((ArrayBuilder) value).list;
        }
        return value;
    }

    private static void writeJson(Object o, StringBuilder sb) {
        if (o == null) {
            sb.append("null");
            return;
        }
        if (o instanceof Boolean || o instanceof Number) {
            sb.append(o);
            return;
        }
        if (o instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) o;
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(String.valueOf(e.getKey()), sb);
                sb.append(':');
                writeJson(e.getValue(), sb);
            }
            sb.append('}');
            return;
        }
        if (o instanceof List) {
            sb.append('[');
            boolean first = true;
            for (Object e : (List<?>) o) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeJson(e, sb);
            }
            sb.append(']');
            return;
        }
        if (o instanceof ObjectBuilder) {
            writeJson(((ObjectBuilder) o).map, sb);
            return;
        }
        if (o instanceof ArrayBuilder) {
            writeJson(((ArrayBuilder) o).list, sb);
            return;
        }
        writeString(String.valueOf(o), sb);
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int p = hex.length(); p < 4; p++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
