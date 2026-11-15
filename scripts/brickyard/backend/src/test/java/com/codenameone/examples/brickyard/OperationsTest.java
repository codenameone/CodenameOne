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

import com.codename1.appkit.server.security.AccountSecurity;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.brickyard.account.Accounts;
import com.codenameone.examples.brickyard.api.InviteDto;
import com.codenameone.examples.brickyard.api.JobDto;
import com.codenameone.examples.brickyard.domain.FraudFlag;
import com.codenameone.examples.brickyard.domain.PlusSubscription;
import com.codenameone.examples.brickyard.economy.Economy;
import com.codenameone.examples.brickyard.economy.StoreNotifications;
import com.codenameone.examples.brickyard.jobs.ScheduledJobs;
import com.codenameone.examples.brickyard.referral.Referrals;
import com.codenameone.examples.brickyard.repository.FraudFlagRepository;
import com.codenameone.examples.brickyard.repository.PlusSubscriptionRepository;
import com.codenameone.examples.brickyard.repository.ReferralRepository;
import com.codenameone.examples.brickyard.social.Social;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Receipts by both paths, global bans, the scheduled jobs, seasons,
/// referrals in one transaction, and the admin and dashboard additions.
@BackendTest
class OperationsTest {
    private static final String HOST = "brickyard.test:8080";
    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Economy economy;
    @Autowired
    private ScheduledJobs jobs;
    @Autowired
    private Referrals referrals;
    @Autowired
    private PlusSubscriptionRepository plus;
    @Autowired
    private FraudFlagRepository fraud;
    @Autowired
    private ReferralRepository referralRows;

    private long balance(String u) throws Exception {
        return Api.number(Api.json(mvc, Api.as(get("/api/me/wallet"), u)), "balance");
    }

    private int notify(String store, String sku, String txn, String account) throws Exception {
        return Api.statusOf(mvc, Api.body(post(StoreNotifications.PATH),
                "{\"store\":\"" + store + "\",\"sku\":\"" + sku + "\",\"transactionId\":\"" + txn
                        + "\",\"receipt\":\"r\",\"account\":\"" + account + "\"}"));
    }

    private int receipt(String u, String store, String sku, String txn) throws Exception {
        return Api.statusOf(mvc, Api.as(Api.body(post("/api/me/wallet/receipts"),
                "{\"store\":\"" + store + "\",\"sku\":\"" + sku + "\",\"transactionId\":\"" + txn
                        + "\",\"receipt\":\"r\"}"), u));
    }

    @Test
    void aPurchaseArrivingByBothPathsIsCreditedOnce() throws Exception {
        String u = Api.player(accounts, "bothpaths");
        assertEquals(200, notify("apple", "bricks.400", "t-both-1", u));
        assertEquals(200, receipt(u, "apple", "bricks.400", "t-both-1"));
        // The store's name is not part of what makes it another purchase.
        assertEquals(200, receipt(u, "Apple", "bricks.400", "t-both-1"));
        assertEquals(Accounts.START_BRICKS + 400, balance(u));
        // The other order: the app first, then the store's notification.
        assertEquals(200, receipt(u, "google", "bricks.1000", "t-both-2"));
        assertEquals(200, notify("GOOGLE", "bricks.1000", "t-both-2", u));
        assertEquals(Accounts.START_BRICKS + 400 + 1100, balance(u));
        // A notification naming another account for a credited purchase is refused.
        String other = Api.player(accounts, "bothpathsother");
        assertEquals(409, notify("apple", "bricks.400", "t-both-1", other));
        assertEquals(404, notify("apple", "bricks.400", "t-both-3", "nobody@test.example"));
    }

    // -- bans --------------------------------------------------------------

    private static MockRequestBuilder form(String path, String... pairs) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (body.length() > 0) {
                body.append('&');
            }
            body.append(OAuth2Parameters.encode(pairs[i])).append('=').append(OAuth2Parameters.encode(pairs[i + 1]));
        }
        return post(path).contentType("application/x-www-form-urlencoded").content(body.toString())
                .header("Host", HOST);
    }

    private MockResponse signIn(String u) throws Exception {
        return mvc.perform(form("/login", "username", u, "password", Api.PASSWORD)).andReturn().getResponse();
    }

    private String code(String session) throws Exception {
        String location = mvc.perform(get("/oauth2/authorize?response_type=code&client_id="
                        + SecurityConfig.CLIENT_ID + "&redirect_uri="
                        + OAuth2Parameters.encode(AccountSecurity.LOOPBACK_REDIRECT)
                        + "&scope=openid%20profile&state=s-1&nonce=n-1&code_challenge=" + CHALLENGE
                        + "&code_challenge_method=S256").header("Host", HOST).cookie("CN1SESSION", session))
                .andReturn().getResponse().getRedirectedUrl();
        assertNotNull(location);
        int at = location.indexOf("code=") + 5;
        int end = location.indexOf('&', at);
        return location.substring(at, end < 0 ? location.length() : end);
    }

    private MockResponse exchange(String code) throws Exception {
        return mvc.perform(form("/oauth2/token", "grant_type", "authorization_code", "code", code,
                "redirect_uri", AccountSecurity.LOOPBACK_REDIRECT, "code_verifier", VERIFIER,
                "client_id", SecurityConfig.CLIENT_ID)).andReturn().getResponse();
    }

    private void ban(String u, String kind, long minutes) throws Exception {
        String mod = Api.player(accounts, "banner" + Math.abs(u.hashCode()) + kind.length());
        Map s = Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/sanctions"),
                "{\"username\":\"" + u + "\",\"kind\":\"" + kind + "\",\"reason\":\"cheating\",\"durationMinutes\":"
                        + minutes + "}"), mod, Roles.MODERATOR));
        if ("TEMP_BAN".equals(kind)) {
            long left = Api.number(s, "untilAt") - Clock.now();
            assertTrue(left > (minutes - 1) * Clock.MINUTE && left <= minutes * Clock.MINUTE, "until " + left);
        }
    }

    @Test
    void aBanStopsSignInRefreshAndEveryWriteButAnAppeal() throws Exception {
        String u = Api.player(accounts, "banned");
        MockResponse before = signIn(u);
        assertEquals(200, before.getStatus());
        String session = before.getCookie("CN1SESSION");
        String issued = exchange(code(session)).getContentAsString();
        Map tokens = Json.parseObject(issued);
        assertNotNull(tokens.get("access_token"), issued);
        String laterCode = code(session);

        ban(u, "TEMP_BAN", 90);
        MockResponse refused = signIn(u);
        assertEquals(403, refused.getStatus());
        assertTrue(refused.getContentAsString().contains("banned for"), refused.getContentAsString());
        assertNull(refused.getCookie("CN1SESSION"));
        // A code handed out before the ban buys no token after it.
        MockResponse noToken = exchange(laterCode);
        assertEquals(400, noToken.getStatus());
        assertTrue(noToken.getContentAsString().contains("banned"));
        Object refresh = tokens.get("refresh_token");
        assertTrue(refresh instanceof String, issued);
        {
            MockResponse noRefresh = mvc.perform(form("/oauth2/token", "grant_type", "refresh_token",
                    "refresh_token", (String) refresh, "client_id", SecurityConfig.CLIENT_ID)).andReturn().getResponse();
            assertEquals(400, noRefresh.getStatus());
            assertTrue(noRefresh.getContentAsString().contains("banned"));
        }

        // Writes are refused with the reason; reads, and an appeal, are not.
        MockResponse write = mvc.perform(Api.as(Api.body(post("/api/experiences"),
                "{\"title\":\"Obby\",\"description\":\"Jump\",\"genre\":\"OBBY\",\"maxPlayers\":12,"
                        + "\"access\":\"PUBLIC\",\"ageRating\":\"ALL\"}"), u)).andReturn().getResponse();
        assertEquals(403, write.getStatus());
        assertTrue(write.getContentAsString().contains("cheating"), write.getContentAsString());
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/invites"),
                "{\"deviceId\":\"device-banned-1\"}"), u)));
        assertEquals(200, Api.statusOf(mvc, Api.as(get("/api/me/wallet"), u)));
        List mine = Api.list(mvc, Api.as(get("/api/me/sanctions"), u));
        assertEquals(1, mine.size());
        String sanctionId = (String) ((Map) mine.get(0)).get("id");
        Map appeal = Api.json(mvc, Api.as(Api.body(post("/api/me/appeals"),
                "{\"sanctionId\":\"" + sanctionId + "\",\"body\":\"It was my brother\"}"), u));
        assertEquals("OPEN", appeal.get("status"));

        // Granting the appeal lifts the ban everywhere.
        String mod = Api.player(accounts, "appealmod");
        Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/appeals/" + appeal.get("id") + "/decide"),
                "{\"kind\":\"GRANT\",\"reason\":\"fair\"}"), mod, Roles.MODERATOR));
        assertEquals(200, signIn(u).getStatus());
        assertEquals(200, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/invites"),
                "{\"deviceId\":\"device-banned-1\"}"), u)));
    }

    @Test
    void aPermanentBanIsGlobalAndAWarnIsNot() throws Exception {
        String warned = Api.player(accounts, "warned");
        ban(warned, "WARN", 0);
        assertEquals(200, signIn(warned).getStatus());
        String gone = Api.player(accounts, "permabanned");
        ban(gone, "PERM_BAN", 0);
        MockResponse refused = signIn(gone);
        assertEquals(403, refused.getStatus());
        assertTrue(refused.getContentAsString().contains("permanently"));
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/invites"),
                "{\"deviceId\":\"device-perma-1\"}"), gone)));
    }

    // -- scheduled jobs and the friend-request cap -----------------------

    @Test
    void theStipendJobPaysALiveSubscriptionOnceAMonthAndOnceADay() throws Exception {
        String u = Api.player(accounts, "stipended");
        assertEquals(200, receipt(u, "apple", Economy.PLUS_SKU, "t-stipend-1"));
        long after = balance(u);
        assertEquals(Accounts.START_BRICKS + Economy.PLUS_STIPEND, after);
        // Not a month yet: nothing.
        economy.payStipends();
        assertEquals(after, balance(u));
        // A month on, the job pays; run again, it does not pay twice.
        PlusSubscription s = plus.findById(u);
        s.setLastStipendAt(Clock.now() - Economy.MONTH - Clock.MINUTE);
        plus.save(s);
        JobDto first = economy.payStipends();
        assertTrue(first.processed() >= 1);
        assertEquals(after + Economy.PLUS_STIPEND, balance(u));
        economy.payStipends();
        assertEquals(after + Economy.PLUS_STIPEND, balance(u));
        // A lapsed subscription is not paid.
        s = plus.findById(u);
        s.setLastStipendAt(Clock.now() - Economy.MONTH - Clock.MINUTE);
        s.setExpiresAt(Clock.now() - 1);
        plus.save(s);
        economy.payStipends();
        assertEquals(after + Economy.PLUS_STIPEND, balance(u));
        // The scheduled entry runs a job once per day, whatever triggers it again.
        String job = "test-job-" + Clock.now();
        final int[] runs = {0};
        assertNotNull(jobs.daily(job, () -> {
            runs[0]++;
            return new JobDto("t", 0, 0);
        }));
        assertNull(jobs.daily(job, () -> {
            runs[0]++;
            return new JobDto("t", 0, 0);
        }));
        assertEquals(1, runs[0]);
    }

    @Test
    void friendRequestsAreCappedPerHour() throws Exception {
        String u = Api.player(accounts, "befriender");
        for (int i = 0; i < Accounts.REQUESTS_PER_HOUR; i++) {
            String other = Api.player(accounts, "befriended" + i);
            assertEquals(200, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/friends"),
                    "{\"username\":\"" + other + "\"}"), u)));
        }
        String one = Api.player(accounts, "befriendedlast");
        assertEquals(429, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/friends"),
                "{\"username\":\"" + one + "\"}"), u)));
        // Accepting a request someone sent is not a request, so it is not capped.
        String asker = Api.player(accounts, "asker");
        Api.json(mvc, Api.as(Api.body(post("/api/me/friends"), "{\"username\":\"" + u + "\"}"), asker));
        assertEquals(200, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/friends"),
                "{\"username\":\"" + asker + "\"}"), u)));
    }

    // -- seasons -----------------------------------------------------------

    @Test
    void startingASeasonResetsSeasonProgress() throws Exception {
        String u = Api.player(accounts, "seasoned");
        String admin = Api.player(accounts, "seasonadmin");
        Api.json(mvc, Api.asEngine(Api.body(post("/api/engine/progress"),
                "{\"username\":\"" + u + "\",\"eventName\":\"win\",\"amount\":60}")));
        Api.json(mvc, Api.as(post("/api/me/season/claim?tier=1&track=FREE"), u));
        Map before = Api.json(mvc, Api.as(get("/api/me/progress"), u));
        assertEquals(1, Api.number(before, "seasonTier"));
        long xp = Api.number(before, "xp");

        assertEquals(403, Api.statusOf(mvc, Api.asStaff(Api.body(post("/api/admin/seasons"),
                "{\"name\":\"Nope\"}"), admin, Roles.MODERATOR)));
        Map season = Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/seasons"),
                "{\"name\":\"Season of Bricks\",\"endsAt\":0}"), admin, Roles.ADMIN));
        assertEquals("Season of Bricks", season.get("name"));
        assertTrue(Api.number(season, "endsAt") > Clock.now() + 29 * Clock.DAY);
        Map current = Api.json(mvc, Api.as(get("/api/season/current"), u));
        assertEquals(season.get("id"), current.get("id"));

        Map after = Api.json(mvc, Api.as(get("/api/me/progress"), u));
        assertEquals(0, Api.number(after, "seasonXp"));
        assertEquals(0, Api.number(after, "premium"));
        assertEquals(xp, Api.number(after, "xp"));
        Map tier1 = (Map) Api.list(mvc, Api.as(get("/api/season"), u)).get(0);
        assertEquals(0, Api.number(tier1, "freeClaimed"));
        // The new season's tier is earned and claimed afresh.
        Api.json(mvc, Api.asEngine(Api.body(post("/api/engine/progress"),
                "{\"username\":\"" + u + "\",\"eventName\":\"win\",\"amount\":60}")));
        assertEquals(200, Api.statusOf(mvc, Api.as(post("/api/me/season/claim?tier=1&track=FREE"), u)));
        assertEquals(409, Api.statusOf(mvc, Api.as(post("/api/me/season/claim?tier=1&track=FREE"), u)));
    }

    // -- referrals -----------------------------------------------------------

    private int flags(String username, String kind) {
        int n = 0;
        for (FraudFlag f : fraud.since(Clock.now() - Clock.DAY)) {
            if (f.getUsername().equals(username) && f.getKind().equals(kind)) {
                n++;
            }
        }
        return n;
    }

    @Test
    void aRefusedReferralKeepsItsFlagAndASharedNetworkIsFlagged() throws Exception {
        String inviter = Api.player(accounts, "netinviter");
        ClientAddress.set("203.0.113.7");
        try {
            String code = referrals.createInvite(inviter, new InviteDto(null, "device-net-inviter")).code();
            // Refused: nothing moves, no referral row, and the flag stays.
            String self = inviter;
            assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/referral"),
                    "{\"code\":\"" + code + "\",\"deviceId\":\"device-net-other\"}"), self)));
            assertEquals(1, flags(inviter, "SELF_REFERRAL"));
            assertNull(referralRows.findById(inviter));

            // Allowed from the inviter's network, but flagged for staff.
            String friend = Api.player(accounts, "netfriend");
            long start = balance(friend);
            referrals.redeem(friend, new InviteDto(code, "device-net-friend"));
            assertEquals(start + Social.REFERRAL_BRICKS, balance(friend));
            assertEquals(1, flags(friend, "SAME_NETWORK"));
            assertFalse(referralRows.findById(friend).getIpHash().isEmpty());

            // From elsewhere, no flag.
            ClientAddress.set("198.51.100.9");
            String far = Api.player(accounts, "netfar");
            referrals.redeem(far, new InviteDto(code, "device-net-far"));
            assertEquals(0, flags(far, "SAME_NETWORK"));
        } finally {
            ClientAddress.set(null);
        }
    }

    // -- admin and dashboard -------------------------------------------------

    @Test
    void activeSanctionsArePagedAndHomeRowsReorder() throws Exception {
        String mod = Api.player(accounts, "listmod");
        String muted = Api.player(accounts, "listmuted");
        Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/sanctions"),
                "{\"username\":\"" + muted + "\",\"kind\":\"MUTE\",\"reason\":\"spam\"}"), mod, Roles.MODERATOR));
        List page = Api.list(mvc, Api.asStaff(get("/api/admin/sanctions?page=0"), mod, Roles.SUPPORT));
        assertTrue(page.toString().contains(muted));
        assertEquals(403, Api.statusOf(mvc, Api.as(get("/api/admin/sanctions?page=0"), muted)));
        // A WARN is over as it is given, so it is not active.
        String warned = Api.player(accounts, "listwarned");
        Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/sanctions"),
                "{\"username\":\"" + warned + "\",\"kind\":\"WARN\",\"reason\":\"rude\"}"), mod, Roles.MODERATOR));
        assertFalse(Api.list(mvc, Api.asStaff(get("/api/admin/sanctions?page=0"), mod, Roles.SUPPORT))
                .toString().contains(warned));

        String owner = Api.player(accounts, "rowowner");
        String admin = Api.player(accounts, "rowadmin");
        String row = "Row" + Clock.now();
        String[] ids = new String[3];
        for (int i = 0; i < 3; i++) {
            String x = Api.experience(mvc, owner, "PUBLIC", "ALL");
            Api.publishSample(mvc, owner, x);
            ids[i] = (String) Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/featured"),
                    "{\"rowName\":\"" + row + "\",\"experienceId\":\"" + x + "\",\"ordinal\":" + i + "}"),
                    admin, Roles.ADMIN)).get("id");
        }
        Map moved = Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/featured/" + ids[2]),
                "{\"rowName\":\"" + row + "\",\"ordinal\":0}"), admin, Roles.ADMIN));
        assertEquals(0, Api.number(moved, "ordinal"));
        StringBuilder order = new StringBuilder();
        for (Object o : Api.list(mvc, Api.as(get("/api/home/featured"), owner))) {
            Map f = (Map) o;
            if (row.equals(f.get("rowName"))) {
                order.append(f.get("id")).append(',');
            }
        }
        assertEquals(ids[2] + "," + ids[0] + "," + ids[1] + ",", order.toString());
        assertEquals(403, Api.statusOf(mvc, Api.asStaff(Api.body(post("/api/admin/featured/" + ids[0]),
                "{\"ordinal\":2}"), admin, Roles.MODERATOR)));
    }

    @Test
    void aCreatorEditsAndRetiresProductsAndSeesTheirPayouts() throws Exception {
        String owner = Api.player(accounts, "retirer");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        Api.publishSample(mvc, owner, id);
        Map pass = Api.json(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/products"),
                "{\"kind\":\"PASS\",\"name\":\"VIP\",\"price\":100}"), owner));
        String pid = (String) pass.get("id");
        String buyer = Api.player(accounts, "retiredbuyer");
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/products/" + pid),
                "{\"price\":1}"), buyer)));
        Map edited = Api.json(mvc, Api.as(Api.body(post("/api/products/" + pid),
                "{\"name\":\"Gold VIP\",\"price\":20}"), owner));
        assertEquals("Gold VIP", edited.get("name"));
        assertEquals(20, Api.number(edited, "price"));
        Api.json(mvc, Api.as(post("/api/products/" + pid + "/buy"), buyer));
        assertEquals(Accounts.START_BRICKS - 20, balance(buyer));

        Api.json(mvc, Api.as(post("/api/products/" + pid + "/deactivate"), owner));
        assertTrue(Api.list(mvc, Api.as(get("/api/experiences/" + id + "/products"), buyer)).isEmpty());
        String late = Api.player(accounts, "latebuyer");
        assertEquals(404, Api.statusOf(mvc, Api.as(post("/api/products/" + pid + "/buy"), late)));
        // What was bought stays owned.
        assertEquals(1, Api.number(Api.json(mvc, Api.asEngine(get("/api/engine/ownership?username=" + buyer
                + "&productId=" + pid))), "count"));

        String creator = Api.player(accounts, "historian");
        assertEquals(200, receipt(creator, "apple", "bricks.1000", "t-history-1"));
        Api.json(mvc, Api.asCreator(Api.body(post("/api/me/payouts"), "{\"bricks\":1000}"), creator));
        List history = Api.list(mvc, Api.asCreator(get("/api/me/payouts"), creator));
        assertEquals(1, history.size());
        assertEquals("PENDING", ((Map) history.get(0)).get("status"));
    }
}
