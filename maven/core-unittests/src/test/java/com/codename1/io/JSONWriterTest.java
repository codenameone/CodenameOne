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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JSONWriterTest {

    @Test
    void encodesScalarsAndNull() {
        assertEquals("null", JSONWriter.toJson(null));
        assertEquals("true", JSONWriter.toJson(Boolean.TRUE));
        assertEquals("false", JSONWriter.toJson(Boolean.FALSE));
        assertEquals("42", JSONWriter.toJson(42));
        assertEquals("3.5", JSONWriter.toJson(3.5));
        // A bare String is quoted.
        assertEquals("\"hello\"", JSONWriter.toJson("hello"));
    }

    @Test
    void encodesMapPreservingInsertionOrder() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("b", 1);
        m.put("a", 2);
        m.put("c", 3);
        // LinkedHashMap preserves insertion order, so output is b,a,c not sorted.
        assertEquals("{\"b\":1,\"a\":2,\"c\":3}", JSONWriter.toJson(m));
    }

    @Test
    void encodesEmptyMapAndList() {
        assertEquals("{}", JSONWriter.toJson(new LinkedHashMap<String, Object>()));
        assertEquals("[]", JSONWriter.toJson(new ArrayList<Object>()));
    }

    @Test
    void encodesNestedStructure() {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        List<Object> values = new ArrayList<Object>();
        values.add("a");
        values.add("b");
        root.put("name", "x");
        root.put("values", values);
        Map<String, Object> nested = new LinkedHashMap<String, Object>();
        nested.put("flag", Boolean.TRUE);
        nested.put("missing", null);
        root.put("nested", nested);
        assertEquals("{\"name\":\"x\",\"values\":[\"a\",\"b\"],\"nested\":{\"flag\":true,\"missing\":null}}",
                JSONWriter.toJson(root));
    }

    @Test
    void encodesNonStringKeysViaStringValueOf() {
        Map<Object, Object> m = new LinkedHashMap<Object, Object>();
        m.put(7, "seven");
        m.put(Boolean.TRUE, "yes");
        assertEquals("{\"7\":\"seven\",\"true\":\"yes\"}", JSONWriter.toJson(m));
    }

    @Test
    void escapesSpecialCharactersInStrings() {
        // Quote, backslash, newline, carriage return, tab, backspace, form feed.
        String raw = "\"\\\n\r\t\b\f";
        assertEquals("\"\\\"\\\\\\n\\r\\t\\b\\f\"", JSONWriter.toJson(raw));
    }

    @Test
    void escapesControlCharactersAsUnicode() {
        // 0x01 ->  with 4-digit zero padding.
        assertEquals("\"\\u0001\"", JSONWriter.toJson(""));
        // 0x1f ->  (just below the 0x20 threshold).
        assertEquals("\"\\u001f\"", JSONWriter.toJson(""));
        // 0x20 (space) is emitted literally.
        assertEquals("\" \"", JSONWriter.toJson(" "));
    }

    @Test
    void writesToWriterWithoutClosing() throws IOException {
        StringWriter sw = new StringWriter();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("k", "v");
        JSONWriter.toJson(m, sw);
        assertEquals("{\"k\":\"v\"}", sw.toString());
    }

    @Test
    void writesToOutputStreamAsUtf8() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        JSONWriter.toJson("café", bos);
        assertEquals("\"café\"", new String(bos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void objectBuilderEmitsObjectJson() {
        JSONWriter.ObjectBuilder b = JSONWriter.object()
                .put("name", "Codename One")
                .put("count", 3)
                .put("on", Boolean.TRUE);
        assertEquals("{\"name\":\"Codename One\",\"count\":3,\"on\":true}", b.toJson());
        // toString delegates to toJson.
        assertEquals(b.toJson(), b.toString());
    }

    @Test
    void objectBuilderToMapExposesBackingMap() {
        JSONWriter.ObjectBuilder b = JSONWriter.object().put("a", 1).put("b", 2);
        Map<String, Object> map = b.toMap();
        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    void arrayBuilderEmitsArrayJson() {
        JSONWriter.ArrayBuilder b = JSONWriter.array().add("a").add(2).add(null);
        assertEquals("[\"a\",2,null]", b.toJson());
        assertEquals(b.toJson(), b.toString());
        assertEquals(3, b.toList().size());
    }

    @Test
    void buildersNestTransparently() {
        // A builder put into another builder stores the unwrapped collection,
        // so the whole tree encodes correctly.
        String json = JSONWriter.object()
                .put("name", "x")
                .put("values", JSONWriter.array().add("a").add("b"))
                .put("child", JSONWriter.object().put("k", 1))
                .toJson();
        assertEquals("{\"name\":\"x\",\"values\":[\"a\",\"b\"],\"child\":{\"k\":1}}", json);
    }

    @Test
    void topLevelBuilderPassedToToJsonEncodes() {
        // toJson(Object) must handle a raw builder instance too.
        assertEquals("[\"a\"]", JSONWriter.toJson(JSONWriter.array().add("a")));
        assertEquals("{\"k\":1}", JSONWriter.toJson(JSONWriter.object().put("k", 1)));
    }
}
