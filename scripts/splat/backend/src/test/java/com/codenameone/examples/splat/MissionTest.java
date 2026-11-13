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
package com.codenameone.examples.splat;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.splat.account.Accounts;
import com.codenameone.examples.splat.game.Arenas;
import com.codenameone.examples.splat.mission.Missions;
import com.codenameone.examples.splat.rules.Mission;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BackendTest
class MissionTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Missions missions;

    private static String quoted(String text) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') {
                b.append('\\');
            }
            b.append(c);
        }
        return b.append('"').toString();
    }

    private int publish(String who, Mission m) throws Exception {
        return Api.statusOf(mvc, Api.as(Api.body(post("/api/missions"), "{\"json\":" + quoted(m.toJson()) + "}"),
                who));
    }

    @Test
    void aPublishedMissionIsListedFetchedAndPlayable() throws Exception {
        String me = Api.player(accounts, "mission-maker");
        Mission m = Mission.blank();
        m.code = "client-picked";
        m.name = "Operation Snack";
        Map made = Api.json(mvc, Api.as(Api.body(post("/api/missions"),
                "{\"json\":" + quoted(m.toJson()) + "}"), me));
        String code = (String) made.get("code");
        assertEquals(Missions.CODE_LENGTH, code.length());
        assertNotEquals("client-picked", code, "the server names the mission");
        assertEquals("Operation Snack", made.get("name"));

        List mine = Api.list(mvc, Api.as(get("/api/missions"), me));
        assertEquals(code, ((Map) mine.get(0)).get("code"));
        assertEquals(0L, Api.number((Map) mine.get(0), "plays"));

        Map fetched = Api.json(mvc, Api.as(get("/api/missions/" + code), me));
        Mission back = Mission.fromJson((String) fetched.get("json"));
        assertEquals(code, back.code);
        assertEquals(404, Api.statusOf(mvc, Api.as(get("/api/missions/nosuchone"), me)));

        String ref = Mission.PREFIX + code + ":TEAM_PAINT";
        assertEquals(ref, Arenas.normalize(missions.resolve(ref)));
        assertNull(Arenas.normalize(missions.resolve(Mission.PREFIX + "zzzzzzzz:TEAM_PAINT")));
    }

    @Test
    void anUnplayableMissionOrGarbageIsRefused() throws Exception {
        String me = Api.player(accounts, "mission-breaker");
        Mission m = Mission.blank();
        m.spawns.clear();
        assertEquals(400, publish(me, m), "no spawn points");
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/missions"), "{\"json\":\"nonsense\"}"), me)));
        assertTrue(Api.list(mvc, Api.as(get("/api/missions"), me)).isEmpty());
    }
}
