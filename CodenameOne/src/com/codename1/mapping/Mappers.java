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
package com.codename1.mapping;

import com.codename1.io.JSONParser;
import com.codename1.util.regex.StringReader;
import com.codename1.xml.Element;
import com.codename1.xml.XMLParser;
import com.codename1.xml.XMLWriter;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/// Public entry point for the build-time JSON / XML mapping framework.
///
/// `@Mapped` classes get a generated mapper at build time. The generated
/// mapper's static initializer self-registers with this registry. The
/// registry stays empty until something triggers each generated class's
/// `<clinit>`:
///
/// - **iOS / Android** -- the build server probes the project zip for
///   `cn1app.MapperBootstrap`, and when present splices a
///   `new cn1app.MapperBootstrap();` into the per-build application stub
///   before `Display.init`. That constructor references every generated
///   mapper, triggering their static initializers.
/// - **JavaSE simulator + desktop** -- `JavaSEPort#postInit` calls
///   `Class.forName("cn1app.MapperBootstrap")` so the registry is populated
///   on the same boundary. Classloading is the legitimate path here:
///   JavaSE runs unobfuscated.
/// - **Unit tests / manual init** -- application code can call
///   `Mappers.register(...)` directly to install a hand-written mapper for
///   a class the build can't annotate.
///
/// Typical use after init:
///
/// ```java
/// String json = Mappers.toJson(user);
/// User u = Mappers.fromJson(json, User.class);
///
/// String xml = Mappers.toXml(user);
/// User u = Mappers.fromXml(xml, User.class);
/// ```
///
/// The registry is keyed on `getClass().getName()` so it survives ParparVM
/// rename and R8 obfuscation: both the registration site and the lookup
/// site see the same renamed name within a single execution. The map keys
/// are never persisted, so the renaming has no observable effect on
/// behavior.
public final class Mappers {

    private static final Map<String, Mapper<?>> BY_NAME = new HashMap<String, Mapper<?>>();

    private Mappers() {
    }

    /// Installs `mapper` under `mapper.type().getName()`. The generated
    /// per-class mapper's static initializer calls this; hand-written
    /// mappers for classes outside the build's annotation scan call it
    /// explicitly.
    public static <T> void register(Mapper<T> mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("mapper is null");
        }
        BY_NAME.put(mapper.type().getName(), mapper);
    }

    /// Looks up the mapper for `type` (by `type.getName()`) or null when
    /// none is registered.
    @SuppressWarnings("unchecked")
    public static <T> Mapper<T> get(Class<T> type) {
        if (type == null) {
            return null;
        }
        return (Mapper<T>) BY_NAME.get(type.getName());
    }

    /// Serializes `instance` to JSON. Throws `IllegalStateException` when
    /// no mapper is registered for its concrete class; that always points
    /// at a missing `@Mapped` annotation or a build that ran without the
    /// process-annotations Mojo.
    public static String toJson(Object instance) {
        if (instance == null) {
            return "null";
        }
        @SuppressWarnings("unchecked")
        Mapper<Object> m = (Mapper<Object>) BY_NAME.get(instance.getClass().getName());
        if (m == null) {
            throw missing(instance.getClass());
        }
        Map<String, Object> root = m.toMap(instance);
        StringBuilder sb = new StringBuilder();
        writeJson(sb, root);
        return sb.toString();
    }

    /// Inverse of `#toJson`. Parses the JSON text and hands the resulting
    /// Map to the registered mapper.
    public static <T> T fromJson(String json, Class<T> type) {
        if (json == null) {
            return null;
        }
        Mapper<T> m = get(type);
        if (m == null) {
            throw missing(type);
        }
        try {
            JSONParser p = new JSONParser();
            Map<String, Object> root = p.parseJSON(new StringReader(json));
            return m.fromMap(root);
        } catch (IOException ioe) {
            throw new RuntimeException("Mappers.fromJson failed: " + ioe.getMessage(), ioe);
        }
    }

    /// Parses JSON read from a `Reader` (file, network response, ...) without
    /// fully buffering it into a String first.
    public static <T> T fromJson(Reader json, Class<T> type) {
        if (json == null) {
            return null;
        }
        Mapper<T> m = get(type);
        if (m == null) {
            throw missing(type);
        }
        try {
            JSONParser p = new JSONParser();
            Map<String, Object> root = p.parseJSON(json);
            return m.fromMap(root);
        } catch (IOException ioe) {
            throw new RuntimeException("Mappers.fromJson failed: " + ioe.getMessage(), ioe);
        }
    }

    /// Serializes `instance` to XML.
    public static String toXml(Object instance) {
        if (instance == null) {
            return "";
        }
        @SuppressWarnings("unchecked")
        Mapper<Object> m = (Mapper<Object>) BY_NAME.get(instance.getClass().getName());
        if (m == null) {
            throw missing(instance.getClass());
        }
        Element root = new Element(m.xmlRootName());
        m.writeXml(instance, root);
        return new XMLWriter(true).toXML(root);
    }

    /// Inverse of `#toXml`. Parses the XML text and hands the resulting
    /// Element to the registered mapper.
    public static <T> T fromXml(String xml, Class<T> type) {
        if (xml == null) {
            return null;
        }
        Mapper<T> m = get(type);
        if (m == null) {
            throw missing(type);
        }
        XMLParser p = new XMLParser();
        Element root = p.parse(new StringReader(xml));
        return m.readXml(root);
    }

    /// Parses XML read from a `Reader` without fully buffering it first.
    public static <T> T fromXml(Reader xml, Class<T> type) {
        if (xml == null) {
            return null;
        }
        Mapper<T> m = get(type);
        if (m == null) {
            throw missing(type);
        }
        XMLParser p = new XMLParser();
        Element root = p.parse(xml);
        return m.readXml(root);
    }

    private static IllegalStateException missing(Class<?> type) {
        return new IllegalStateException("No mapper registered for "
                + type.getName() + ". Add @Mapped and ensure the cn1:process-annotations "
                + "Mojo ran during build, then re-run -- the generated MapperBootstrap "
                + "populates this registry at startup.");
    }

    // ---------------------------------------------------------------
    // Tiny JSON writer
    // ---------------------------------------------------------------

    static void writeJson(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof Map) {
            sb.append('{');
            boolean first = true;
            Map<?, ?> map = (Map<?, ?>) value;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeJsonString(sb, String.valueOf(e.getKey()));
                sb.append(':');
                writeJson(sb, e.getValue());
            }
            sb.append('}');
            return;
        }
        if (value instanceof java.util.Collection) {
            sb.append('[');
            boolean first = true;
            for (Object item : (java.util.Collection<?>) value) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeJson(sb, item);
            }
            sb.append(']');
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());
            return;
        }
        writeJsonString(sb, value.toString());
    }

    private static void writeJsonString(StringBuilder sb, String s) {
        sb.append('"');
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append("\\u00");
                        sb.append("0123456789abcdef".charAt((c >> 4) & 0xF));
                        sb.append("0123456789abcdef".charAt(c & 0xF));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
