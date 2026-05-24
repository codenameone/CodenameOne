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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonHelper is package-private so this test deliberately lives in
 * the same package. We test through a tiny `JsonHelperBridge` shim
 * (also in this package) because the source class is package-private
 * in the *core* artifact, and tests in core-unittests run in the
 * test classpath but still need to reach into the package.
 */
class JsonHelperTest {

    @Test
    void escapesStringsCorrectly() {
        // Newlines, quotes and backslashes in a user prompt should
        // come back as the JSON-escaped equivalents — providers
        // reject unescaped control chars in string values.
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("prompt", "line one\nline \"two\"\\");
        String json = JsonHelperBridge.serialize(m);
        assertTrue(json.contains("line one\\nline \\\"two\\\"\\\\"),
                "expected escaped form, got: " + json);
    }

    @Test
    void omitsNullValuesFromMaps() {
        // Whenever ChatRequest.maxTokens is null, the builder maps
        // it to a Java null in the Map<String,Object>; the wire
        // format must omit the key entirely (some providers reject
        // "max_tokens":null).
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("model", "gpt-4o-mini");
        m.put("max_tokens", null);
        String json = JsonHelperBridge.serialize(m);
        assertFalse(json.contains("max_tokens"),
                "null-valued field must not appear in the body");
        assertTrue(json.contains("\"model\""));
    }

    @Test
    void rawJsonInlinesWithoutReEscaping() {
        // Tool parameter schemas are passed in as already-encoded
        // JSON strings. The serializer must inline them verbatim
        // rather than wrapping the whole string in quotes.
        String rawSchema = "{\"type\":\"object\",\"properties\":{\"x\":{\"type\":\"number\"}}}";
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("parameters", JsonHelperBridge.rawJson(rawSchema));
        String json = JsonHelperBridge.serialize(m);
        // The raw schema must appear unwrapped — no surrounding "..."
        // and no \"-style escaping.
        assertTrue(json.contains("\"parameters\":{\"type\":\"object\""),
                "raw JSON should be inlined; got: " + json);
    }

    @Test
    void emitsIntegersWithoutFloatNotation() {
        // Integers must serialize as `5`, never `5.0` or `5E0`,
        // because some providers' JSON validators are pedantic.
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("n", Integer.valueOf(5));
        m.put("seed", Long.valueOf(42L));
        String json = JsonHelperBridge.serialize(m);
        assertTrue(json.contains("\"n\":5"));
        assertTrue(json.contains("\"seed\":42"));
        assertFalse(json.contains("5.0"));
    }

    @Test
    void roundTripsListOfPrimitives() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("stop", Arrays.asList("A", "B", "C"));
        String json = JsonHelperBridge.serialize(m);
        assertEquals("{\"stop\":[\"A\",\"B\",\"C\"]}", json);
    }
}
