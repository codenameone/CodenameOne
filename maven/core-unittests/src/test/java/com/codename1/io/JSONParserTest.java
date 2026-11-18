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
package com.codename1.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JSONParserTest {
    private JSONParser parser;

    @BeforeEach
    void setUp() {
        parser = new JSONParser();
    }

    @Test
    void parsesObjectsWithLongsBooleansAndNulls() throws IOException {
        parser.setUseLongsInstance(true);
        parser.setUseBooleanInstance(true);
        parser.setIncludeNullsInstance(true);

        String json = "{" +
                "\"name\":\"Alice\"," +
                "\"age\":30," +
                "\"premium\":true," +
                "\"scores\":[1,2.5,null]," +
                "\"address\":{\"city\":\"Paris\"}" +
                "}";

        Map<String, Object> result = parser.parseJSON(new StringReader(json));

        assertEquals("Alice", result.get("name"));
        assertTrue(result.get("age") instanceof Long);
        assertEquals(30L, result.get("age"));
        assertEquals(Boolean.TRUE, result.get("premium"));

        @SuppressWarnings("unchecked")
        List<Object> scores = (List<Object>) result.get("scores");
        assertEquals(3, scores.size());
        assertEquals(1L, scores.get(0));
        assertEquals(2.5d, (Double) scores.get(1));
        assertNull(scores.get(2));

        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) result.get("address");
        assertEquals("Paris", address.get("city"));
    }

    @Test
    void decodesEveryStringEscapeTheSpecDefines() throws IOException {
        // \b and \f were read as the letters b and f.
        String json = "{\"s\":\"a\\bb\\fc\\nd\\te\\rf\\\"g\\\\h\\/i\\u0041\"}";
        Map<String, Object> result = parser.parseJSON(new StringReader(json));
        assertEquals("a\bb\fc\nd\te\rf\"g\\h/iA", result.get("s"));
    }

    @Test
    void omitsNullValuesWhenIncludeNullsDisabled() throws IOException {
        parser.setIncludeNullsInstance(false);
        parser.setUseBooleanInstance(true);

        String json = "{\"optional\":null,\"active\":false}";

        Map<String, Object> result = parser.parseJSON(new StringReader(json));

        assertFalse(result.containsKey("optional"));
        assertEquals(Boolean.FALSE, result.get("active"));
    }

    @Test
    void wrapsArrayRootsInsideMap() throws IOException {
        String json = "[{\"id\":1},{\"id\":2}]";

        Map<String, Object> result = parser.parseJSON(new StringReader(json));

        assertTrue(result.containsKey("root"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("root");
        assertEquals(2, list.size());
        assertEquals(1.0, list.get(0).get("id"));
        assertEquals(2.0, list.get(1).get("id"));
    }

    @Test
    void legacyParseReturnsHashtable() throws IOException {
        parser.setUseLongsInstance(false);
        String json = "{\"value\":123.5,\"flag\":\"yes\"}";

        Hashtable<String, Object> legacy = parser.parse(new StringReader(json));

        assertEquals(123.5d, legacy.get("value"));
        assertEquals("yes", legacy.get("flag"));
    }

    @Test
    void nonStrictModeSanitizesInput() throws IOException {
        parser.setStrict(false);
        parser.setUseBooleanInstance(true);
        parser.setUseLongsInstance(true);

        String jsonish = "{foo:'bar',count:5,on:true}";

        Map<String, Object> result = parser.parseJSON(new StringReader(jsonish));

        assertEquals("bar", result.get("foo"));
        assertEquals(5L, result.get("count"));
        assertEquals(Boolean.TRUE, result.get("on"));
    }
}
