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
package com.codename1.io;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// Exercises the toJson + parseJSON serializer additions on
/// JSONParser. The serializer is shared between the AI client and
/// any other code that needs to build JSON request bodies without
/// pulling in a third-party library.
class JsonParserToJsonTest {

    @Test
    void escapesStringsCorrectly() {
        // Newlines, quotes and backslashes in a user prompt should
        // come back as the JSON-escaped equivalents -- providers
        // reject unescaped control chars in string values.
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("prompt", "line one\nline \"two\"\\");
        String json = JSONParser.toJson(m);
        assertTrue(json.contains("line one\\nline \\\"two\\\"\\\\"),
                "expected escaped form, got: " + json);
    }

    @Test
    void omitsNullValuesFromMaps() {
        // Whenever a builder field (like ChatRequest.maxTokens) is
        // null, toJson must omit the key entirely -- some providers
        // reject "field":null on the wire.
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("model", "gpt-4o-mini");
        m.put("max_tokens", null);
        String json = JSONParser.toJson(m);
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
        m.put("parameters", JSONParser.rawJson(rawSchema));
        String json = JSONParser.toJson(m);
        assertTrue(json.contains("\"parameters\":{\"type\":\"object\""),
                "raw JSON should be inlined; got: " + json);
    }

    @Test
    void emitsIntegersWithoutFloatNotation() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("n", Integer.valueOf(5));
        m.put("seed", Long.valueOf(42L));
        String json = JSONParser.toJson(m);
        assertTrue(json.contains("\"n\":5"));
        assertTrue(json.contains("\"seed\":42"));
        assertFalse(json.contains("5.0"));
    }

    @Test
    void roundTripsListOfPrimitives() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("stop", Arrays.asList("A", "B", "C"));
        String json = JSONParser.toJson(m);
        assertEquals("{\"stop\":[\"A\",\"B\",\"C\"]}", json);
    }

    @Test
    void parsesByteArrayDirectly() throws Exception {
        // parseJSON(byte[]) is the convenience helper used by every
        // HTTP response code path; round-trip a small object.
        byte[] body = "{\"x\":1,\"y\":\"hi\"}".getBytes("UTF-8");
        Map<String, Object> root = JSONParser.parseJSON(body);
        // Numeric values default to Double on the way out; pull them
        // via getInt so we don't depend on the parser's default
        // numeric type.
        assertEquals(1, JSONParser.getInt(root, "x", -1));
        assertEquals("hi", JSONParser.getString(root, "y"));
    }

    @Test
    void getIntFallsBackOnMissingOrUnparseable() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("count", Integer.valueOf(7));
        m.put("text", "not a number");
        assertEquals(7, JSONParser.getInt(m, "count", -1));
        assertEquals(-1, JSONParser.getInt(m, "absent", -1));
        assertEquals(-1, JSONParser.getInt(m, "text", -1));
    }
}
