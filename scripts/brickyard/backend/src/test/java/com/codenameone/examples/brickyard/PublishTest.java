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

import com.codename1.backend.Base64;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.brickyard.account.Accounts;
import com.codenameone.examples.brickyard.api.RegisterDto;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Creating an experience, publishing a place file, refusing one that is not a
/// place, and rolling back.
@BackendTest
class PublishTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private Accounts accounts;

    @Test
    void aPublishedPlaceRoundTripsWithItsReport() throws Exception {
        String owner = Api.player(accounts, "maker");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        Map v = Api.publishSample(mvc, owner, id);
        assertEquals(1L, Api.number(v, "number"));
        assertEquals("PUBLISHED", v.get("status"));
        String report = (String) v.get("report");
        assertTrue(report != null && report.length() > 0, report);
        Map e = Api.json(mvc, Api.as(get("/api/experiences/" + id), owner));
        assertEquals(1L, Api.number(e, "publishedVersion"));

        String player = Api.player(accounts, "visitor");
        Map place = Api.json(mvc, Api.as(get("/api/experiences/" + id + "/place"), player));
        assertTrue(Arrays.equals(Api.sample(), Base64.decode((String) place.get("data"))));
        Map visited = Api.json(mvc, Api.as(post("/api/experiences/" + id + "/visit"), player));
        assertEquals(1L, Api.number(visited, "visits"));
        Map voted = Api.json(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/vote"),
                "{\"vote\":1}"), player));
        assertEquals(1L, Api.number(voted, "upVotes"));
        voted = Api.json(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/vote"),
                "{\"vote\":-1}"), player));
        assertEquals(0L, Api.number(voted, "upVotes"));
        assertEquals(1L, Api.number(voted, "downVotes"));
        List browse = Api.list(mvc, Api.as(get("/api/experiences"), player));
        assertTrue(browse.toString().contains(id));
    }

    @Test
    void bytesThatAreNotAPlaceAreRefused() throws Exception {
        String owner = Api.player(accounts, "sloppy");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        String junk = Base64.encode("this is not a place file at all".getBytes("UTF-8"));
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/publish"),
                "{\"data\":\"" + junk + "\"}"), owner)));
        assertEquals(0, Api.list(mvc, Api.as(get("/api/experiences/" + id + "/versions"), owner)).size());
        assertEquals(0L, Api.number(Api.json(mvc, Api.as(get("/api/experiences/" + id), owner)),
                "publishedVersion"));
    }

    @Test
    void rollbackRepointsThePublishedVersion() throws Exception {
        String owner = Api.player(accounts, "undoer");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        Api.publishSample(mvc, owner, id);
        Api.publishSample(mvc, owner, id);
        Map back = Api.json(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/rollback"),
                "{\"number\":1}"), owner));
        assertEquals("PUBLISHED", back.get("status"));
        List versions = Api.list(mvc, Api.as(get("/api/experiences/" + id + "/versions"), owner));
        assertEquals("PUBLISHED", ((Map) versions.get(0)).get("status"));
        assertEquals("ROLLED_BACK", ((Map) versions.get(1)).get("status"));
        assertEquals(1L, Api.number(Api.json(mvc, Api.as(get("/api/experiences/" + id), owner)),
                "publishedVersion"));
    }

    @Test
    void onlyCreatorsCreateAndOnlyTheOwnerPublishes() throws Exception {
        String owner = Api.player(accounts, "owner");
        String other = Api.player(accounts, "intruder");
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/experiences"),
                "{\"title\":\"Nope\",\"maxPlayers\":4}"), other)));
        String id = Api.experience(mvc, owner, "PRIVATE", "ALL");
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/publish"),
                "{\"data\":\"" + Base64.encode(Api.sample()) + "\"}"), other)));
        Api.publishSample(mvc, owner, id);
        assertEquals(404, Api.statusOf(mvc, Api.as(get("/api/experiences/" + id), other)));
    }

    @Test
    void anAgeRatingKeepsYoungerPlayersOut() throws Exception {
        String owner = Api.player(accounts, "grownup");
        String id = Api.experience(mvc, owner, "PUBLIC", "17+");
        Api.publishSample(mvc, owner, id);
        String teen = accounts.register(new RegisterDto("teen@test.example", Api.PASSWORD, "Teen",
                Clock.yearOf(Clock.now()) - 14)).username();
        assertEquals(403, Api.statusOf(mvc, Api.as(get("/api/experiences/" + id), teen)));
        assertTrue(!Api.list(mvc, Api.as(get("/api/experiences"), teen)).toString().contains(id));
    }
}
