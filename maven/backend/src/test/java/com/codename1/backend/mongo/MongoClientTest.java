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
package com.codename1.backend.mongo;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// BSON and URI parsing offline; the client against a real server when
/// CN1_TEST_MONGODB_URI names one (for example
/// `mongodb://root:pass@127.0.0.1:27017/cn1test?authSource=admin`).
class MongoClientTest {
    @Test
    void bsonRoundTripKeepsTypesAndOrder() throws Exception {
        Map doc = new LinkedHashMap();
        doc.put("z", "first");
        doc.put("i", Integer.valueOf(7));
        doc.put("l", Long.valueOf(1L << 40));
        doc.put("d", Double.valueOf(2.5));
        doc.put("b", Boolean.TRUE);
        doc.put("n", null);
        doc.put("t", new Date(1234567890123L));
        doc.put("o", ObjectId.parse("0123456789abcdef01234567"));
        doc.put("bin", new byte[] {1, 2, 3});
        doc.put("arr", Arrays.asList(new Object[] {"a", Integer.valueOf(1)}));
        Map sub = new LinkedHashMap();
        sub.put("k", "é中😀");
        doc.put("sub", sub);
        Map back = Bson.decode(Bson.encode(doc));
        assertEquals(new ArrayList(doc.keySet()), new ArrayList(back.keySet()));
        assertEquals(Integer.valueOf(7), back.get("i"));
        assertEquals(Long.valueOf(1L << 40), back.get("l"));
        assertEquals(new Date(1234567890123L), back.get("t"));
        assertEquals("0123456789abcdef01234567", back.get("o").toString());
        assertArrayEquals(new byte[] {1, 2, 3}, (byte[]) back.get("bin"));
        assertEquals(doc.get("arr"), back.get("arr"));
        assertEquals(sub, back.get("sub"));
        assertNull(back.get("n"));
    }

    @Test
    void truncatedBsonIsRefused() throws Exception {
        Map doc = new LinkedHashMap();
        doc.put("s", "hello");
        byte[] b = Bson.encode(doc);
        byte[] cut = new byte[b.length - 3];
        System.arraycopy(b, 0, cut, 0, cut.length);
        assertThrows(java.io.IOException.class, () -> Bson.decode(cut));
    }

    @Test
    void uriParsing() throws Exception {
        MongoUri u = MongoUri.parse("mongodb://us%40er:p%2Cw@h1:1,[::1]:2,h3/shop"
                + "?tls=true&maxPoolSize=4");
        assertEquals("us@er", u.user);
        assertEquals("p,w", u.password);
        assertEquals("shop", u.database);
        assertEquals("shop", u.authSource);
        assertTrue(u.tls);
        assertEquals(4, u.maxPoolSize);
        assertEquals(3, u.hosts.size());
        assertEquals("::1", MongoUri.hostName((String) u.hosts.get(1)));
        assertEquals(2, MongoUri.port((String) u.hosts.get(1)));
        assertEquals(27017, MongoUri.port("h3"));
        assertThrows(java.io.IOException.class,
                () -> MongoUri.parse("mongodb+srv://cluster0.example.net/"));
        assertEquals("a=3Db=2Cc", MongoConnection.saslName("a=b,c"));
    }

    @Test
    void crudAgainstServer() throws Exception {
        String uri = System.getenv("CN1_TEST_MONGODB_URI");
        Assumptions.assumeTrue(uri != null && uri.length() > 0,
                "CN1_TEST_MONGODB_URI is not set");
        MongoClient client = MongoClient.open(uri);
        try {
            MongoCollection c = client.database().collection("crud" + System.nanoTime());
            c.createIndex(Collections1.map("email", Integer.valueOf(1)), true);
            List docs = new ArrayList();
            for (int i = 0; i < 250; i++) {
                Map d = new LinkedHashMap();
                d.put("email", "u" + i + "@example.com");
                d.put("n", Integer.valueOf(i));
                docs.add(d);
            }
            c.insertMany(docs);
            assertEquals(250, c.find(Filters.all()).size());
            assertEquals(50, c.countDocuments(Filters.gte("n", Integer.valueOf(200))));
            assertEquals(10, c.find(Filters.all(), Collections1.map("n", Integer.valueOf(-1)),
                    null, 0, 10).size());
            Map one = c.findOne(Filters.eq("n", Integer.valueOf(5)));
            assertEquals("u5@example.com", one.get("email"));
            assertTrue(one.get("_id") instanceof ObjectId);
            assertEquals(1, c.updateOne(Filters.eq("n", Integer.valueOf(5)),
                    Updates.combine(Updates.set("name", "five"),
                            Updates.inc("n", Integer.valueOf(1000)))));
            assertEquals("five", c.findOne(Filters.eq("n", Integer.valueOf(1005))).get("name"));
            Map dup = new LinkedHashMap();
            dup.put("email", "u1@example.com");
            MongoException err = assertThrows(MongoException.class, () -> c.insertOne(dup));
            assertTrue(err.isDuplicateKey());
            assertEquals(99, c.deleteMany(Filters.lt("n", Integer.valueOf(100))));
            assertEquals(151, c.countDocuments(null));
            c.drop();
        } finally {
            client.close();
        }
    }

    private static final class Collections1 {
        static Map map(String k, Object v) {
            Map m = new LinkedHashMap();
            m.put(k, v);
            return m;
        }
    }
}
