/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.brickyard.account.Accounts;
import com.codenameone.examples.brickyard.data.DataStores;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// The data stores an engine server keeps for an experience's scripts.
@BackendTest
class DataStoreTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private Accounts accounts;

    @Test
    void anEngineSetsGetsIncrementsAndLists() throws Exception {
        String owner = Api.player(accounts, "keeper");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        String base = "/api/data/" + id + "/PlayerData/global/";
        Map set = Api.json(mvc, Api.asEngine(Api.body(post(base + "coins"), "{\"value\":\"5\"}")));
        assertEquals(1L, Api.number(set, "revision"));
        Map inc = Api.json(mvc, Api.asEngine(Api.body(post(base + "coins/increment"), "{\"delta\":7}")));
        assertEquals("12", inc.get("value"));
        assertEquals(2L, Api.number(inc, "revision"));
        Api.json(mvc, Api.asEngine(Api.body(post(base + "profile"),
                "{\"value\":\"{\\\"hat\\\":\\\"red\\\"}\"}")));
        Map got = Api.json(mvc, Api.asEngine(get(base + "profile")));
        assertEquals("{\"hat\":\"red\"}", got.get("value"));
        List all = Api.list(mvc, Api.asEngine(get("/api/data/" + id + "/PlayerData/global")));
        assertEquals(2, all.size());
        assertEquals("coins", ((Map) all.get(0)).get("key"));
        Map fresh = Api.json(mvc, Api.asEngine(Api.body(post(base + "gems/increment"), "{\"delta\":3}")));
        assertEquals("3", fresh.get("value"));
        assertEquals(409, Api.statusOf(mvc, Api.asEngine(Api.body(post(base + "profile/increment"),
                "{\"delta\":1}"))));
        assertEquals(404, Api.statusOf(mvc, Api.asEngine(get(base + "missing"))));
    }

    @Test
    void aPlayerClientCannotTouchTheStore() throws Exception {
        String owner = Api.player(accounts, "storeowner");
        String player = Api.player(accounts, "snoop");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        String key = "/api/data/" + id + "/PlayerData/global/coins";
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post(key), "{\"value\":\"999\"}"), player)));
        assertEquals(403, Api.statusOf(mvc, Api.as(get(key), player)));
        Api.json(mvc, Api.as(Api.body(post(key), "{\"value\":\"1\"}"), owner));
    }

    @Test
    void aValueMustBeJsonAndAtMostFourMegabytes() throws Exception {
        String owner = Api.player(accounts, "bigdata");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        String key = "/api/data/" + id + "/Blobs/global/big";
        assertEquals(400, Api.statusOf(mvc, Api.asEngine(Api.body(post(key), "{\"value\":\"{not json\"}"))));
        StringBuilder huge = new StringBuilder(DataStores.MAX_VALUE_BYTES + 16);
        huge.append("\\\"");
        while (huge.length() < DataStores.MAX_VALUE_BYTES + 8) {
            huge.append("xxxxxxxxxxxxxxxx");
        }
        huge.append("\\\"");
        assertEquals(413, Api.statusOf(mvc, Api.asEngine(Api.body(post(key),
                "{\"value\":\"" + huge + "\"}"))));
        assertEquals(404, Api.statusOf(mvc, Api.asEngine(get(key))));
    }
}
