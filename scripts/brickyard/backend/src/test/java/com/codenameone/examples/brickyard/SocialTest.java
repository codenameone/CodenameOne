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
import com.codenameone.examples.brickyard.api.RegisterDto;
import com.codenameone.examples.brickyard.social.ChatFilter;
import com.codenameone.examples.brickyard.social.Social;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Requests, blocks, presence and joining, parties, chat and its filter, DMs and referrals.
@BackendTest
class SocialTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    @Test
    void aRequestBothWaysIsAFriendshipAndABlockEndsIt() throws Exception {
        String a = Api.player(accounts, "mutuala");
        String b = Api.player(accounts, "mutualb");
        Api.json(mvc, Api.as(Api.body(post("/api/me/friends"), "{\"username\":\"" + b + "\"}"), a));
        Api.json(mvc, Api.as(Api.body(post("/api/me/friends"), "{\"username\":\"" + a + "\"}"), b));
        assertEquals(1, Api.list(mvc, Api.as(get("/api/me/friends"), a)).size());
        Api.json(mvc, Api.as(post("/api/me/blocks/" + a), b));
        assertEquals(0, Api.list(mvc, Api.as(get("/api/me/friends"), a)).size());
        assertEquals(404, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/friends"),
                "{\"username\":\"" + b + "\"}"), a)));
        assertEquals(1, Api.list(mvc, Api.as(get("/api/me/blocks"), b)).size());
    }

    @Test
    void aFriendInAGameCanBeJoinedAndPartiesAreForFriends() throws Exception {
        String a = Api.player(accounts, "joiner");
        String b = Api.player(accounts, "host");
        String stranger = Api.player(accounts, "stranger");
        Api.befriend(mvc, a, b);
        assertEquals(409, Api.statusOf(mvc, Api.as(get("/api/me/friends/" + b + "/join"), a)));
        Api.json(mvc, Api.as(Api.body(post("/api/me/presence"),
                "{\"status\":\"IN_GAME\",\"experienceId\":\"exp1\",\"serverId\":\"srv-9\"}"), b));
        Map target = Api.json(mvc, Api.as(get("/api/me/friends/" + b + "/join"), a));
        assertEquals("srv-9", target.get("serverId"));
        assertEquals(404, Api.statusOf(mvc, Api.as(get("/api/me/friends/" + b + "/join"), stranger)));
        Map party = Api.json(mvc, Api.as(post("/api/parties"), b));
        assertEquals(404, Api.statusOf(mvc, Api.as(post("/api/parties/" + party.get("id") + "/join"), stranger)));
        Map joined = Api.json(mvc, Api.as(post("/api/parties/" + party.get("id") + "/join"), a));
        assertEquals(2, ((List) joined.get("members")).size());
        Map left = Api.json(mvc, Api.as(post("/api/parties/" + party.get("id") + "/leave"), b));
        assertEquals(a, left.get("leader"));
        follow(a, b);
    }

    private void follow(String a, String creator) throws Exception {
        Map f = Api.json(mvc, Api.as(post("/api/creators/" + creator + "/follow"), a));
        assertEquals(1, Api.number(f, "followers"));
        assertEquals(1, Api.list(mvc, Api.as(get("/api/me/following"), a)).size());
    }

    @Test
    void chatIsFilteredForTheReadersAge() throws Exception {
        String adult = Api.player(accounts, "chatadult");
        String teen = accounts.register(new RegisterDto("chatteen@test.example", Api.PASSWORD, "teen",
                Clock.yearOf(Clock.now()) - 15)).username();
        Api.json(mvc, Api.as(Api.body(post("/api/chat/exp1:srv1"),
                "{\"body\":\"call 555 123 4567 you shit\"}"), adult));
        String seenByAdult = (String) ((Map) Api.list(mvc, Api.as(get("/api/chat/exp1:srv1"), adult)).get(0)).get("body");
        String seenByTeen = (String) ((Map) Api.list(mvc, Api.as(get("/api/chat/exp1:srv1"), teen)).get(0)).get("body");
        assertTrue(seenByAdult.contains("555 123 4567") && !seenByAdult.contains("shit"));
        assertFalse(seenByTeen.contains("555"));
        assertEquals("my age is ##", ChatFilter.filter("my age is 12", 11));
        assertEquals("my age is 12", ChatFilter.filter("my age is 12", 15));
        assertEquals("add me on ####", ChatFilter.filter("add me on snap", 12));
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/chat/dm:x"), "{\"body\":\"hi\"}"), adult)));
        assertEquals(404, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/dm/" + teen), "{\"body\":\"hi\"}"), adult)));
        Api.befriend(mvc, adult, teen);
        Api.json(mvc, Api.as(Api.body(post("/api/me/dm/" + teen), "{\"body\":\"hi there\"}"), adult));
        assertEquals(1, Api.list(mvc, Api.as(get("/api/me/dm/" + adult), teen)).size());
    }

    private int redeem(String u, String code, String device) throws Exception {
        return Api.statusOf(mvc, Api.as(Api.body(post("/api/me/referral"),
                "{\"code\":\"" + code + "\",\"deviceId\":\"" + device + "\"}"), u));
    }

    @Test
    void referralsRewardBothSidesAndRefuseFraud() throws Exception {
        String inviter = Api.player(accounts, "inviter");
        String code = (String) Api.json(mvc, Api.as(Api.body(post("/api/me/invites"),
                "{\"deviceId\":\"device-inviter-1\"}"), inviter)).get("code");
        String friend = Api.player(accounts, "invitee");
        assertEquals(403, redeem(inviter, code, "device-other-1"));
        assertEquals(403, redeem(friend, code, "device-inviter-1"));
        assertEquals(200, redeem(friend, code, "device-invitee-1"));
        assertEquals(409, redeem(friend, code, "device-invitee-1"));
        assertEquals(Accounts.START_BRICKS + Social.REFERRAL_BRICKS,
                Api.number(Api.json(mvc, Api.as(get("/api/me/wallet"), inviter)), "balance"));
        for (int i = 2; i <= 5; i++) {
            assertEquals(200, redeem(Api.player(accounts, "invitee" + i), code, "device-invitee-" + i));
        }
        assertEquals(403, redeem(Api.player(accounts, "invitee6"), code, "device-invitee-6"));
        String admin = Api.player(accounts, "fraudadmin");
        String flags = Api.list(mvc, Api.asStaff(get("/api/admin/fraud"), admin, Roles.SUPPORT)).toString();
        assertTrue(flags.contains("SELF_REFERRAL") && flags.contains("SAME_DEVICE")
                && flags.contains("REFERRAL_VELOCITY"));
    }
}
