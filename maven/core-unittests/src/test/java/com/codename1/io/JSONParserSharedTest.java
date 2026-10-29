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

import com.codename1.processing.Result;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// JSONParser is shared with the backend, so its writer no longer goes through
/// com.codename1.processing.Result. These pin mapToJson to what Result printed,
/// and cover the strict reader both halves now parse with.
class JSONParserSharedTest {

    private static void same(Map<String, ?> m) {
        assertEquals(Result.fromContent(m).toString(), JSONParser.mapToJson(m));
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static List<Object> list(Object... v) {
        List<Object> l = new ArrayList<Object>();
        for (Object o : v) {
            l.add(o);
        }
        return l;
    }

    @Test
    void mapToJsonMatchesResultByteForByte() {
        same(map());
        same(map("a", "b"));
        same(map("n", null));
        same(map("one", map("two", map("three", list()))));
        same(map("i", 1, "l", 1234567890123L, "d", 1.5, "d0", 2.0, "f", 3.25f, "e", 1.0E20,
                "small", 1.0E-7, "neg", -0.0, "s", (short) 7, "b", (byte) -3));
        same(map("t", Boolean.TRUE, "f", Boolean.FALSE, "empty", "", "nil", null));
        same(map("esc", "q\"b\\s/x</y\b\f\n\r\t\u0001\u001f\u00e9\u20ac end"));
        same(map("list1", list("x"), "list", list(1, "two", null, list(), map(), list(map("k", 2)))));
        same(map("deep", list(map("a", list(1, 2, map("b", list(list("c")))), "z", map("y", 1)), 3)));
        same(map("date", new Date(1234567890123L), "other", new StringBuilder("sb")));
        same(map("ROOT", map("inner", list(1, 2), "x", "y")));
        Map<String, Object> h = new Hashtable<String, Object>();
        h.put("a", list(1, 2));
        h.put("b", map("c", "d", "e", 5L));
        same(h);
    }

    @Test
    void strictReaderValuesAndErrors() throws IOException {
        Object v = JSONParser.parseStrict(" {\"a\":1,\"b\":[true,null,\"x\\u00e9\"],\"c\":1.5,\"d\":-9223372036854775808} ");
        assertTrue(v instanceof LinkedHashMap);
        Map<?, ?> m = (Map<?, ?>) v;
        assertEquals(Long.valueOf(1), m.get("a"));
        assertEquals(list(Boolean.TRUE, null, "x\u00e9"), m.get("b"));
        assertEquals(Double.valueOf(1.5), m.get("c"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), m.get("d"));
        assertEquals("abc", JSONParser.parseStrict("\"abc\""));
        assertTrue(JSONParser.parseStrict("[]") instanceof ArrayList);
        String[] bad = {null, "", "{\"a\":1} junk", "{\"a\":}", "\"a\nb\"", "01", "1.", ".5", "+1",
                "1e999", "\"\\u+041\"", "[1,]", "{\"a\" 1}", "tru", "9223372036854775808"};
        for (String b : bad) {
            try {
                JSONParser.parseStrict(b);
                fail("accepted " + b);
            } catch (IOException expected) {
                // refused, as it should be
            }
        }
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            deep.append('[');
        }
        try {
            JSONParser.parseStrict(deep.toString());
            fail("accepted 600 levels");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().indexOf("512") > 0);
        }
    }

    @Test
    void exactIntegersKeepTheirDigits() throws IOException {
        String big = "115792089237316195423570985008687907853269984665640564039457584007913129639935";
        Object v = JSONParser.parseStrict("{\"v\":" + big + ",\"n\":-" + big + ",\"l\":9223372036854775807}", true);
        Map<?, ?> m = (Map<?, ?>) v;
        assertEquals(big, ((JSONParser.RawJson) m.get("v")).getJson());
        assertEquals("-" + big, ((JSONParser.RawJson) m.get("n")).getJson());
        assertEquals(Long.valueOf(Long.MAX_VALUE), m.get("l"));
        assertEquals("{\"v\":" + big + ",\"n\":-" + big + ",\"l\":9223372036854775807}", JSONParser.toJson(v));
    }
}
