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
package com.codenameone.examples.papercut;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.papercut.account.Accounts;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Achievements, worked out from accepted runs.
@BackendTest
class ProgressTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    @Test
    void achievementsFollowTheRunsAPlayerHasHad() throws Exception {
        String who = Api.player(accounts, "achiever");
        Map before = byId(Api.list(mvc, Api.as(get("/api/achievements"), who)));
        assertFalse((Boolean) ((Map) before.get("first-cut")).get("unlocked"));
        assertEquals(1L, Api.number((Map) before.get("papers-3"), "progress"));
        Api.submit(mvc, who, "CLASSIC", 150000, 200);
        Api.json(mvc, Api.as(Api.body(post("/api/runs"), Api.run(Api.ticket(mvc, who, "FOLD"), "FOLD",
                30000, 30, 27, 190, 20)), who));
        Map after = byId(Api.list(mvc, Api.as(get("/api/achievements"), who)));
        assertTrue((Boolean) ((Map) after.get("first-cut")).get("unlocked"));
        assertTrue((Boolean) ((Map) after.get("classic-100k")).get("unlocked"));
        Map razor = (Map) after.get("classic-500k");
        assertFalse((Boolean) razor.get("unlocked"));
        assertEquals(150000L, Api.number(razor, "progress"));
        assertEquals(500000L, Api.number(razor, "goal"));
        assertTrue((Boolean) ((Map) after.get("fold-25")).get("unlocked"));
        assertTrue((Boolean) ((Map) after.get("survive-3m")).get("unlocked"));
        assertTrue((Boolean) ((Map) after.get("multiplier-20")).get("unlocked"));
        assertEquals(0L, Api.number((Map) after.get("daily-10"), "progress"));
    }

    private static Map byId(List achievements) {
        Map out = new java.util.HashMap();
        for (Object a : achievements) {
            out.put(((Map) a).get("id"), a);
        }
        return out;
    }
}
