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
package com.codename1.impl.javase;

import com.codename1.io.JSONParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Build Hint editor schema for every hint that has a build hint annotation.
 *
 * <p>Loaded from the generated data file rather than generated as Java. The
 * annotations are where these hints are declared; this reads the rendering of
 * them that scripts/gen-build-hint-annotations.sh writes, because the simulator
 * has no bytecode reader and this module does not depend on the catalog.</p>
 *
 * <p>Registered after {@link BuildHintSchemaDefaults} and skipping every hint
 * that class already describes. Precedence cannot be left to the setter: the
 * group name is part of the property key, so registering harden.level under
 * both `hardening` and `Hardening` does not overwrite anything -- it makes a
 * second group, and the editor renders both, giving the user duplicate controls
 * for one setting.</p>
 */
final class BuildHintCatalogDefaults {

    /** Where the generator writes it, on this module's own classpath. */
    private static final String RESOURCE = "/cn1-build-hints.json";

    private BuildHintCatalogDefaults() {
    }

    static void register() {
        List<Map<String, Object>> hints = load();
        if (hints.isEmpty()) {
            return;
        }
        Set<String> handWritten = BuildHintSchemaDefaults.declaredHints();
        for (Map<String, Object> h : hints) {
            String name = str(h, "name");
            String group = str(h, "group");
            if (name == null || group == null) {
                continue;
            }
            String annotation = str(h, "attr") == null ? group : groupAnnotation(group);
            set("{{@" + annotation + "}}.label", str(h, "groupLabel"));
            if (handWritten.contains(name)) {
                continue;
            }
            String key = "{{#" + annotation + "#" + name + "}}";
            set(key + ".label", str(h, "label"));
            set(key + ".type", str(h, "editor"));
            List<Object> values = list(h, "values");
            if (!values.isEmpty()) {
                StringBuilder joined = new StringBuilder();
                for (int i = 0; i < values.size(); i++) {
                    joined.append(i == 0 ? "" : ",").append(String.valueOf(values.get(i)));
                }
                set(key + ".values", joined.toString());
            }
            set(key + ".description", str(h, "doc"));
        }
    }

    /**
     * ANDROID to Android, ON_DEVICE_DEBUG to OnDeviceDebug.
     *
     * <p>The annotation's simple name is what the editor's property key uses,
     * and the data file records the enum constant.</p>
     */
    private static String groupAnnotation(String group) {
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (int i = 0; i < group.length(); i++) {
            char c = group.charAt(i);
            if (c == '_') {
                upper = true;
                continue;
            }
            sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
            upper = false;
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> load() {
        InputStream in = BuildHintCatalogDefaults.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            return java.util.Collections.emptyList();
        }
        try {
            // The parser wants an object at the root, so the array is wrapped.
            Map<String, Object> root = new JSONParser().parseJSON(
                    new InputStreamReader(wrap(in), "UTF-8"));
            Object hints = root.get("hints");
            return hints instanceof List ? (List<Map<String, Object>>) hints
                    : java.util.Collections.<Map<String, Object>>emptyList();
        } catch (Exception ex) {
            System.err.println("Warning: could not read " + RESOURCE + ": " + ex);
            return java.util.Collections.emptyList();
        } finally {
            try {
                in.close();
            } catch (java.io.IOException ignored) {
                // read-only stream
            }
        }
    }

    /** The array wrapped in an object, which is what JSONParser accepts. */
    private static InputStream wrap(InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write("{\"hints\":".getBytes("UTF-8"));
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        out.write("}".getBytes("UTF-8"));
        return new java.io.ByteArrayInputStream(out.toByteArray());
    }

    private static String str(Map<String, Object> h, String key) {
        Object v = h.get(key);
        return v instanceof String ? (String) v : null;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Map<String, Object> h, String key) {
        Object v = h.get(key);
        return v instanceof List ? (List<Object>) v : java.util.Collections.emptyList();
    }

    /** Idempotent setter: does not overwrite user or project-level metadata. */
    private static void set(String suffix, String value) {
        if (value == null || value.length() == 0) {
            return;
        }
        String key = "codename1.arg." + suffix;
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
