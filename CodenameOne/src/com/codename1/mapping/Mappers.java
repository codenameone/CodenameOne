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
/// `@Mapped` classes are picked up by the Maven plugin's annotation processor
/// at build time. A generated `Mapper` is wired into this registry through a
/// generated `com.codename1.mapping.generated.MappersIndex` whose static
/// initializer fires the first time the registry is touched.
///
/// Typical use:
///
/// ```java
/// String json = Mappers.toJson(user);
/// User u = Mappers.fromJson(json, User.class);
///
/// String xml = Mappers.toXml(user);
/// User u = Mappers.fromXml(xml, User.class);
/// ```
///
/// `register(...)` is public so application code can install a hand-written
/// mapper for a class the build-time processor cannot see (e.g. classes that
/// live in a dependency JAR). Generated mappers register themselves through
/// the same call.
public final class Mappers {

    private static final Map<Class<?>, Mapper<?>> BY_TYPE = new HashMap<Class<?>, Mapper<?>>();

    private Mappers() { }

    /// Installs a mapper for `mapper.type()`. Subsequent calls with the same
    /// type replace the previously registered mapper. Thread-safe relative to
    /// `get` (the registry is a plain HashMap, written from app init and read
    /// at steady state; concurrent registration during steady-state lookups
    /// is not supported and not needed in practice).
    public static <T> void register(Mapper<T> mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("mapper is null");
        }
        BY_TYPE.put(mapper.type(), mapper);
    }

    /// Looks up the mapper for `type`, returning `null` when no mapper is
    /// registered. The first call lazily triggers the generated
    /// `MappersIndex` static initializer when present, so application code
    /// does not need to wire it up explicitly.
    @SuppressWarnings("unchecked")
    public static <T> Mapper<T> get(Class<T> type) {
        ensureIndexLoaded();
        return (Mapper<T>) BY_TYPE.get(type);
    }

    /// Serializes `instance` to JSON. Throws `IllegalStateException` when no
    /// mapper is registered for its concrete class; that always points at a
    /// missing `@Mapped` annotation or a build that ran without the
    /// process-annotations Mojo.
    public static String toJson(Object instance) {
        if (instance == null) {
            return "null";
        }
        @SuppressWarnings("unchecked")
        Mapper<Object> m = (Mapper<Object>) get(instance.getClass());
        if (m == null) {
            throw missing(instance.getClass());
        }
        Map<String, Object> root = m.toMap(instance);
        StringBuilder sb = new StringBuilder();
        writeJson(sb, root);
        return sb.toString();
    }

    /// Inverse of `#toJson`. Parses the JSON text and hands the resulting Map
    /// to the registered mapper.
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
        Mapper<Object> m = (Mapper<Object>) get(instance.getClass());
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

    // ---------------------------------------------------------------
    // Index bootstrap
    // ---------------------------------------------------------------

    /// Forces the lazy `IndexHolder` class to initialize. Its static
    /// initializer constructs the build-time-generated `MappersIndex`,
    /// whose constructor registers every generated mapper with this
    /// registry. Calling `bootstrap()` is harmless after the first call;
    /// the JVM guarantees the holder's class init runs exactly once.
    ///
    /// The iOS / Android per-build application stub invokes this from the
    /// `annotationFrameworksInstallSource` fragment before `Display.init`.
    public static void bootstrap() {
        IndexHolder.touch();
    }

    private static void ensureIndexLoaded() {
        IndexHolder.touch();
    }

    /// Initialization-on-demand holder. The JVM defers class init until
    /// the first field reference, then runs it exactly once under the
    /// class-init monitor -- a race-free lazy singleton without
    /// `volatile`. Direct symbol reference (no `Class.forName`) so
    /// ParparVM / R8 rewrite the call site and the generated class
    /// together; the binding survives obfuscation in shipped builds.
    private static final class IndexHolder {
        static final Object INDEX;
        static {
            Object resolved;
            try {
                resolved = new com.codename1.mapping.generated.MappersIndex();
            } catch (NoClassDefFoundError missing) {
                // No @Mapped types in this project -- nothing to register.
                resolved = Boolean.FALSE;
            } catch (RuntimeException failed) {
                // The generated index hit a registration failure. Pin a
                // sentinel so we don't retry on every call; the missing
                // mapper surfaces from `Mappers.get` as the regular
                // "no mapper registered" error.
                resolved = Boolean.FALSE;
            }
            INDEX = resolved;
        }

        private IndexHolder() {
            // Utility-style holder: only the static initializer + touch
            // matter. PMD `UseUtilityClass` requires this private ctor.
        }

        static void touch() {
            // Read INDEX so SpotBugs sees the field as used. Defensive
            // null check documents the contract; createIndex pins a
            // non-null sentinel on every fallback, so the throw is
            // unreachable in practice.
            if (INDEX == null) {
                throw new IllegalStateException(
                        "MappersIndex failed to initialize");
            }
        }
    }

    private static IllegalStateException missing(Class<?> type) {
        return new IllegalStateException("No mapper registered for "
                + type.getName() + ". Add @Mapped and ensure the cn1:process-annotations "
                + "Mojo ran during build.");
    }

    // ---------------------------------------------------------------
    // Tiny JSON writer
    // ---------------------------------------------------------------
    //
    // Hand-rolled rather than reusing PropertyIndex / Result so this class
    // works on plain POJO maps without any cn1.properties dependency. We only
    // emit the subset that JSONParser can read back: strings, numbers,
    // booleans, null, nested maps, lists.

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
                        // cn1's java.lang.Character is a stripped-down subset
                        // and does not include Character.forDigit. Inline the
                        // hex-digit lookup so this class stays portable to
                        // every cn1 target (ParparVM iOS, Android, JavaSE,
                        // CLDC11, ...).
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
