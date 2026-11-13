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
package com.codename1.backend.dynamo;

import com.codename1.backend.aws.Credentials;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Attribute conversion offline; the client against DynamoDB Local when
/// CN1_TEST_DYNAMODB_ENDPOINT names one (for example `http://127.0.0.1:8000`).
class DynamoDbTest {
    @Test
    void attributesRoundTrip() {
        Map item = new LinkedHashMap();
        item.put("s", "x");
        item.put("n", Long.valueOf(42));
        item.put("f", Double.valueOf(1.5));
        item.put("whole", Double.valueOf(3.0));
        item.put("b", Boolean.FALSE);
        item.put("bin", new byte[] {9, 8});
        item.put("l", Arrays.asList(new Object[] {"a", Long.valueOf(1)}));
        item.put("nul", null);
        Map attrs = Items.toAttributes(item);
        assertEquals("3", ((Map) attrs.get("whole")).get("N"));
        Map back = Items.fromAttributes(attrs);
        assertEquals("x", back.get("s"));
        assertEquals(Long.valueOf(42), back.get("n"));
        assertEquals(Double.valueOf(1.5), back.get("f"));
        assertEquals(Long.valueOf(3), back.get("whole"));
        assertArrayEquals(new byte[] {9, 8}, (byte[]) back.get("bin"));
        assertEquals(item.get("l"), back.get("l"));
        assertNull(back.get("nul"));
        assertTrue(back.containsKey("nul"));
        assertEquals(Double.valueOf(1e30), Items.number("1000000000000000000000000000000"));
    }

    @Test
    void crudAgainstServer() throws Exception {
        String endpoint = System.getenv("CN1_TEST_DYNAMODB_ENDPOINT");
        Assumptions.assumeTrue(endpoint != null && endpoint.length() > 0,
                "CN1_TEST_DYNAMODB_ENDPOINT is not set");
        DynamoDb db = DynamoDb.forEndpoint(new Credentials("local", "local", null),
                "us-east-1", endpoint);
        String table = "t" + System.nanoTime();
        db.createTable(table, "pk", "sk");
        try {
            List items = new ArrayList();
            for (int i = 0; i < 60; i++) {
                Map it = new LinkedHashMap();
                it.put("pk", i % 2 == 0 ? "even" : "odd");
                it.put("sk", "k" + (100 + i));
                it.put("n", Long.valueOf(i));
                items.add(it);
            }
            db.batchPut(table, items);
            Map values = new LinkedHashMap();
            values.put(":p", "even");
            assertEquals(30, db.query(table, null, "pk = :p", null, values, 0).size());
            assertEquals(5, db.query(table, null, "pk = :p", null, values, 5).size());
            assertEquals(60, db.scan(table, null, null, null, 0).size());

            Map key = new LinkedHashMap();
            key.put("pk", "odd");
            key.put("sk", "k101");
            assertEquals(Long.valueOf(1), db.getItem(table, key).get("n"));
            Map names = new LinkedHashMap();
            names.put("#n", "n");
            Map inc = new LinkedHashMap();
            inc.put(":one", Long.valueOf(1));
            assertEquals(Long.valueOf(2),
                    db.updateItem(table, key, "SET #n = #n + :one", null, names, inc).get("n"));
            Map clash = new LinkedHashMap(key);
            DynamoDbException err = assertThrows(DynamoDbException.class,
                    () -> db.putItem(table, clash, "attribute_not_exists(pk)", null));
            assertTrue(err.isConditionalCheckFailed());
            db.deleteItem(table, key);
            assertNull(db.getItem(table, key));
        } finally {
            db.deleteTable(table);
        }
    }
}
