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
package com.codenameone.examples.skirmish;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.skirmish.account.Accounts;
import com.codenameone.examples.skirmish.account.DemoAccounts;
import com.codenameone.examples.skirmish.social.Referrals;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Inviting friends and the operator's Promote tab: who is paid for an
/// invite and when, the claims that are refused, broadcasts and the banner.
@BackendTest
class SocialTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private static MockRequestBuilder admin(MockRequestBuilder request) {
        return Api.asAdmin(request, DemoAccounts.ADMIN);
    }

    private String code(String who) throws Exception {
        return (String) Api.json(mvc, Api.as(get("/api/social/invite"), who)).get("code");
    }

    private int claim(String who, String code) throws Exception {
        return Api.statusOf(mvc, Api.as(Api.body(post("/api/social/claim"),
                "{\"code\":\"" + code + "\",\"campaign\":\"friends\",\"channel\":\"share_sheet\",\"match\":\"direct\"}"),
                who));
    }

    private long coins(String who) throws Exception {
        return Api.number(Api.json(mvc, Api.as(get("/api/me"), who)), "coins");
    }

    // tag::skirmish-referral-test[]
    @Test
    void aFriendIsWelcomedOnJoiningAndTheInviterPaidOnceTheyPlay() throws Exception {
        String host = Api.player(accounts, "inviter-one");
        String friend = Api.player(accounts, "invited-one");
        String code = code(host);
        assertEquals(code, code(host), "a player keeps one code");

        long before = coins(friend);
        assertEquals(200, claim(friend, code));
        assertEquals(before + Referrals.FRIEND_COINS, coins(friend));
        assertEquals(409, claim(friend, code), "an invite counts once");

        List friends = Api.list(mvc, Api.as(get("/api/social/friends"), host));
        assertEquals(1, friends.size());
        assertEquals(Referrals.JOINED, ((Map) friends.get(0)).get("state"));

        long hostBefore = coins(host);
        Api.json(mvc, Api.as(post("/api/progress/tutorial"), friend));
        assertEquals(hostBefore + Referrals.INVITER_COINS, coins(host), "paid when the friend has played");
        Map mine = Api.json(mvc, Api.as(get("/api/social/invite"), host));
        assertEquals(1L, Api.number(mine, "rewarded"));
        assertEquals(Referrals.PLAYED, ((Map) Api.list(mvc, Api.as(get("/api/social/friends"), host)).get(0))
                .get("state"));

        // A second battle pays nothing more.
        Api.json(mvc, Api.as(post("/api/progress/tutorial"), friend));
        assertEquals(hostBefore + Referrals.INVITER_COINS, coins(host));
    }
    // end::skirmish-referral-test[]

    @Test
    void invitesThatWouldFarmCoinsAreRefused() throws Exception {
        String a = Api.player(accounts, "farm-a");
        String b = Api.player(accounts, "farm-b");
        assertEquals(400, claim(a, code(a)), "not your own code");
        assertEquals(404, claim(a, "nosuchcode"));
        assertEquals(200, claim(b, code(a)));
        assertEquals(400, claim(a, code(b)), "not the player you invited");
    }

    @Test
    void anOperatorBroadcastsToASegmentAndSetsTheBanner() throws Exception {
        String everyone = "{\"segment\":\"all\",\"days\":0,\"title\":\"\",\"body\":\"\",\"screen\":\"\"}";
        Map size = Api.json(mvc, admin(Api.body(post("/api/admin/promote/audience"), everyone)));
        assertTrue(Api.number(size, "targets") >= 4, "the demo accounts at least");
        assertEquals(400, Api.statusOf(mvc, admin(Api.body(post("/api/admin/promote/broadcast"), everyone))),
                "a push needs a title and a message");
        assertEquals(400, Api.statusOf(mvc, admin(Api.body(post("/api/admin/promote/audience"),
                "{\"segment\":\"lapsed\",\"days\":0,\"screen\":\"\"}"))), "lapsed for how long");
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/admin/promote/broadcast"), everyone),
                DemoAccounts.PLAYER)));

        Map sent = Api.json(mvc, admin(Api.body(post("/api/admin/promote/broadcast"),
                "{\"segment\":\"new\",\"days\":7,\"title\":\"Double coins\",\"body\":\"All weekend long\","
                        + "\"screen\":\"shop\",\"campaign\":\"weekend\"}")));
        assertEquals("weekend", sent.get("campaign"));
        Map promote = Api.json(mvc, admin(get("/api/admin/promote")));
        assertTrue(((List) promote.get("broadcasts")).size() >= 1);

        Api.json(mvc, admin(Api.body(post("/api/admin/promote/spotlight"),
                "{\"text\":\"The Border War campaign is here\",\"packId\":\"border-war\",\"screen\":\"\"}")));
        Map banner = Api.json(mvc, Api.as(get("/api/social/spotlight"), DemoAccounts.PLAYER));
        assertEquals("The Border War campaign is here", banner.get("text"));
        assertEquals("border-war", banner.get("packId"));
    }
}
