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
package com.codenameone.examples.dash;

import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.dash.account.Accounts;
import com.codenameone.examples.dash.rules.CityRoute;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.LevelDef;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// City runs: the server builds the level from the route the result carries
/// and verifies the run on it, with no map of its own.
@BackendTest
class CityTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private static CityRoute route() {
        return new CityRoute("Old Harbour").leg(420, CityRoute.STRAIGHT, "Quay Street")
                .leg(380, CityRoute.LEFT, "Rope Walk").leg(500, CityRoute.RIGHT, "Lantern Avenue");
    }

    private static String body(String replay, String city) {
        Map<String, Object> m = new HashMap<>();
        m.put("replay", replay);
        m.put("city", city);
        return Json.write(m);
    }

    @Test
    void aCityRunIsVerifiedOnTheRouteItCarriesAndHasItsOwnBoard() throws Exception {
        String who = Api.player(accounts, "city-runner");
        CityRoute r = route();
        LevelDef level = r.level();
        // The app's seed for a route: its content hash, the same every time.
        String run = Runs.bot(level, Content.catalog(), level.contentHash(), 99).encode();
        Map out = Api.json(mvc, Api.as(Api.body(post("/api/forge/results"), body(run, r.encode())), who));
        assertEquals(level.id, out.get("levelId"));
        assertTrue(Api.number(out, "score") > 0);
        List board = Api.list(mvc, Api.as(get("/api/scores?level=" + level.id + "&board=all&limit=10"), who));
        assertEquals(1, board.size());
        // An easier route under the same run is another level: refused.
        CityRoute easier = new CityRoute("Old Harbour").leg(1300, CityRoute.STRAIGHT, "Quay Street");
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/forge/results"),
                body(run, easier.encode())), who)));
        // A city run without its route cannot be checked.
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/forge/results"), body(run, null)), who)));
    }
}
