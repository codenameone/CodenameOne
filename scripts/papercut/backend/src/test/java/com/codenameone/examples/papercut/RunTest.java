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
import com.codenameone.examples.papercut.api.ScoreBounds;
import com.codenameone.examples.papercut.domain.RunTicket;
import com.codenameone.examples.papercut.repository.RunTicketRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/// The ticket checks a submitted run is held to, and what an accepted run pays.
@BackendTest
class RunTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private RunTicketRepository tickets;

    @Test
    void anAcceptedRunPaysAndCountsAsABest() throws Exception {
        String who = Api.player(accounts, "runner");
        Map result = Api.submit(mvc, who, "CLASSIC", 250000, 300);
        assertTrue((Boolean) result.get("accepted"));
        assertFalse((Boolean) result.get("flagged"));
        assertTrue((Boolean) result.get("newBest"));
        // A coin per thousand points, capped at a hundred.
        assertEquals(100L, Api.number(result, "coinsEarned"));
        assertTrue(Api.number(result, "rank") > 0);
        Map me = Api.json(mvc, Api.as(get("/api/me"), who));
        assertEquals(250000L, Api.number(me, "bestClassic"));
        assertEquals(Accounts.START_COINS + 100L, Api.number(me, "inkCoins"));
        Map lower = Api.submit(mvc, who, "CLASSIC", 20000, 100);
        assertFalse((Boolean) lower.get("newBest"));
        assertEquals(20L, Api.number(lower, "coinsEarned"));
        assertEquals(250000L, Api.number(lower, "best"));
        Map daily = Api.submit(mvc, who, "DAILY", 3000, 60);
        assertEquals(3L + 5, Api.number(daily, "coinsEarned"));
    }

    @Test
    void aTicketIsGoodOnceForItsModeAndUntilItExpires() throws Exception {
        String who = Api.player(accounts, "ticketed");
        String token = Api.ticket(mvc, who, "FOLD");
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/runs"),
                Api.run(token, "CLASSIC", 1000, 10, 0, 60, 1)), who)));
        Api.json(mvc, Api.as(Api.body(post("/api/runs"), Api.run(token, "FOLD", 1000, 10, 3, 60, 1)), who));
        assertEquals(409, Api.statusOf(mvc, Api.as(Api.body(post("/api/runs"),
                Api.run(token, "FOLD", 1000, 10, 3, 60, 1)), who)));
        assertEquals(404, Api.statusOf(mvc, Api.as(Api.body(post("/api/runs"),
                Api.run("no-such-ticket", "FOLD", 1000, 10, 3, 60, 1)), who)));
        // Somebody else's ticket is as good as none.
        String other = Api.player(accounts, "borrower");
        String theirs = Api.ticket(mvc, other, "FOLD");
        assertEquals(404, Api.statusOf(mvc, Api.as(Api.body(post("/api/runs"),
                Api.run(theirs, "FOLD", 1000, 10, 3, 60, 1)), who)));
        String stale = Api.ticket(mvc, who, "CLASSIC");
        RunTicket t = tickets.findById(stale);
        t.setExpiresAt(Instant.ofEpochMilli(System.currentTimeMillis() - 1000));
        tickets.save(t);
        assertEquals(410, Api.statusOf(mvc, Api.as(Api.body(post("/api/runs"),
                Api.run(stale, "CLASSIC", 1000, 10, 0, 60, 1)), who)));
    }

    @Test
    void anImpossibleScoreIsRefusedAndASuspiciousOneFlagged() throws Exception {
        String who = Api.player(accounts, "cheater");
        String token = Api.ticket(mvc, who, "CLASSIC");
        // Ten cuts cannot be worth more than ten times the best cut at x20.
        long ceiling = 10L * ScoreBounds.MAX_POINTS * ScoreBounds.MAX_MULTIPLIER;
        Map refused = Api.json(mvc, Api.as(Api.body(post("/api/runs"),
                Api.run(token, "CLASSIC", ceiling + 1, 10, 0, 60, 20)), who));
        assertFalse((Boolean) refused.get("accepted"));
        assertEquals(0L, Api.number(refused, "rank"));
        assertTrue(((String) refused.get("message")).length() > 0);
        assertEquals(0L, Api.number(Api.json(mvc, Api.as(get("/api/me"), who)), "runs"));
        Map odd = Api.json(mvc, Api.as(Api.body(post("/api/runs"),
                Api.run(Api.ticket(mvc, who, "CLASSIC"), "CLASSIC", 700000, 200, 0, 60, 20)), who));
        assertTrue((Boolean) odd.get("accepted"));
        assertTrue((Boolean) odd.get("flagged"));
        assertTrue((Boolean) Api.json(mvc, Api.as(get("/api/me"), who)).get("flagged"));
    }
}
