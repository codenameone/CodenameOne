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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The wallet and ledger, receipts, passes and products, the catalog, Plus,
/// engagement payouts and payout requests.
@BackendTest
class EconomyTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private long balance(String u) throws Exception {
        return Api.number(Api.json(mvc, Api.as(get("/api/me/wallet"), u)), "balance");
    }

    private Map receipt(String u, String sku, String txn) throws Exception {
        return Api.json(mvc, Api.as(Api.body(post("/api/me/wallet/receipts"),
                "{\"store\":\"apple\",\"sku\":\"" + sku + "\",\"transactionId\":\"" + txn
                        + "\",\"receipt\":\"r\"}"), u));
    }

    @Test
    void theBalanceIsTheSumOfTheLedgerAndAReceiptCountsOnce() throws Exception {
        String u = Api.player(accounts, "ledgerer");
        Map w = Api.json(mvc, Api.as(get("/api/me/wallet"), u));
        assertEquals(Accounts.START_BRICKS, Api.number(w, "balance"));
        assertEquals(1, ((List) w.get("entries")).size());
        receipt(u, "bricks.400", "t-ledger-1");
        Map again = receipt(u, "bricks.400", "t-ledger-1");
        assertEquals(Accounts.START_BRICKS + 400, Api.number(again, "balance"));
        assertEquals(Accounts.START_BRICKS + 400,
                Api.number(Api.json(mvc, Api.as(get("/api/me"), u)), "bricks"));
        String other = Api.player(accounts, "thief");
        assertEquals(409, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/wallet/receipts"),
                "{\"store\":\"apple\",\"sku\":\"bricks.400\",\"transactionId\":\"t-ledger-1\"}"), other)));
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/wallet/receipts"),
                "{\"store\":\"apple\",\"sku\":\"bricks.free\",\"transactionId\":\"x\"}"), u)));
    }

    @Test
    void aPassIsBoughtOnceAProductAnyNumberOfTimesAndTheCreatorEarns() throws Exception {
        String owner = Api.player(accounts, "passmaker");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        Api.publishSample(mvc, owner, id);
        Map pass = Api.json(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/products"),
                "{\"kind\":\"PASS\",\"name\":\"VIP\",\"price\":100}"), owner));
        Map potion = Api.json(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/products"),
                "{\"kind\":\"PRODUCT\",\"name\":\"Potion\",\"price\":10}"), owner));
        String buyer = Api.player(accounts, "passbuyer");
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/products"),
                "{\"kind\":\"PASS\",\"name\":\"Mine\",\"price\":1}"), buyer)));
        assertEquals(402, Api.statusOf(mvc, Api.as(post("/api/products/" + pass.get("id") + "/buy"), buyer)));
        receipt(buyer, "bricks.400", "t-pass-1");
        Api.json(mvc, Api.as(post("/api/products/" + pass.get("id") + "/buy"), buyer));
        assertEquals(409, Api.statusOf(mvc, Api.as(post("/api/products/" + pass.get("id") + "/buy"), buyer)));
        Api.json(mvc, Api.as(post("/api/products/" + potion.get("id") + "/buy"), buyer));
        Map two = Api.json(mvc, Api.as(post("/api/products/" + potion.get("id") + "/buy"), buyer));
        assertEquals(2, Api.number(two, "count"));
        assertEquals(Accounts.START_BRICKS + 400 - 120, balance(buyer));
        assertEquals(Accounts.START_BRICKS + 70 + 14, balance(owner));
        Map owns = Api.json(mvc, Api.asEngine(get("/api/engine/ownership?username=" + buyer
                + "&productId=" + pass.get("id"))));
        assertEquals(1, Api.number(owns, "count"));
        assertEquals(403, Api.statusOf(mvc, Api.as(get("/api/engine/ownership?username=" + buyer
                + "&productId=" + pass.get("id")), buyer)));
    }

    @Test
    void catalogItemsArePlatformItemsAnAdminAdds() throws Exception {
        String admin = Api.player(accounts, "catadmin");
        String u = Api.player(accounts, "shopper");
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/admin/catalog"),
                "{\"name\":\"Hat\",\"slot\":\"HEAD\",\"price\":10}"), u)));
        Map hat = Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/catalog"),
                "{\"name\":\"Hat\",\"slot\":\"HEAD\",\"price\":10}"), admin, Roles.ADMIN));
        Api.json(mvc, Api.as(post("/api/catalog/" + hat.get("id") + "/buy"), u));
        assertEquals(409, Api.statusOf(mvc, Api.as(post("/api/catalog/" + hat.get("id") + "/buy"), u)));
        assertEquals(1, Api.list(mvc, Api.as(get("/api/me/inventory"), u)).size());
        assertTrue(Api.list(mvc, Api.as(get("/api/catalog"), u)).toString().contains("Hat"));
    }

    @Test
    void plusPlayTimePaysTheCreatorThroughTheJob() throws Exception {
        String owner = Api.player(accounts, "plusmaker");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        String fan = Api.player(accounts, "plusfan");
        String freeloader = Api.player(accounts, "noplus");
        receipt(fan, "plus.monthly", "t-plus-1");
        assertEquals(1, Api.number(Api.json(mvc, Api.as(get("/api/me/plus"), fan)), "active"));
        assertEquals(Accounts.START_BRICKS + 450, balance(fan));
        Map counted = Api.json(mvc, Api.asEngine(Api.body(post("/api/engine/plus-play"),
                "{\"experienceId\":\"" + id + "\",\"username\":\"" + fan + "\",\"seconds\":600}")));
        assertEquals(600, Api.number(counted, "seconds"));
        Map ignored = Api.json(mvc, Api.asEngine(Api.body(post("/api/engine/plus-play"),
                "{\"experienceId\":\"" + id + "\",\"username\":\"" + freeloader + "\",\"seconds\":600}")));
        assertEquals(0, Api.number(ignored, "seconds"));
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/engine/plus-play"),
                "{\"experienceId\":\"" + id + "\",\"username\":\"" + fan + "\",\"seconds\":600}"), fan)));
        String admin = Api.player(accounts, "jobadmin");
        Api.json(mvc, Api.asStaff(post("/api/admin/jobs/engagement-payouts"), admin, Roles.ADMIN));
        assertEquals(Accounts.START_BRICKS + 10, balance(owner));
        Api.json(mvc, Api.asStaff(post("/api/admin/jobs/engagement-payouts"), admin, Roles.ADMIN));
        assertEquals(Accounts.START_BRICKS + 10, balance(owner));
    }

    @Test
    void aPayoutHoldsTheBricksUntilAnAdminDecides() throws Exception {
        String creator = Api.player(accounts, "cashout");
        String admin = Api.player(accounts, "payadmin");
        Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/economy/adjust"),
                "{\"username\":\"" + creator + "\",\"amount\":2000,\"reason\":\"contest prize\"}"), admin, Roles.ADMIN));
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/me/payouts"), "{\"bricks\":1000}"), creator)));
        Map p = Api.json(mvc, Api.asCreator(Api.body(post("/api/me/payouts"), "{\"bricks\":1500}"), creator));
        assertEquals(Accounts.START_BRICKS + 500, balance(creator));
        Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/payouts/" + p.get("id") + "/decide"),
                "{\"kind\":\"REJECT\",\"reason\":\"verify identity\"}"), admin, Roles.ADMIN));
        assertEquals(Accounts.START_BRICKS + 2000, balance(creator));
        assertEquals(409, Api.statusOf(mvc, Api.asStaff(Api.body(post("/api/admin/payouts/" + p.get("id")
                + "/decide"), "{\"kind\":\"APPROVE\"}"), admin, Roles.ADMIN)));
        assertEquals(403, Api.statusOf(mvc, Api.asStaff(Api.body(post("/api/admin/economy/adjust"),
                "{\"username\":\"" + creator + "\",\"amount\":5,\"reason\":\"x\"}"), admin, Roles.MODERATOR)));
    }

    @Test
    void supportRefundsAPurchaseEvenWhenTheBricksAreSpent() throws Exception {
        String u = Api.player(accounts, "refundee");
        String support = Api.player(accounts, "helpdesk");
        receipt(u, "bricks.1000", "t-refund-1");
        Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/economy/adjust"),
                "{\"username\":\"" + u + "\",\"amount\":-1100,\"reason\":\"spent\"}"), support, Roles.ADMIN));
        Map w = Api.json(mvc, Api.asStaff(Api.body(post("/api/admin/economy/refund"),
                "{\"target\":\"apple|sim:t-refund-1\",\"reason\":\"chargeback\"}"), support, Roles.SUPPORT));
        assertEquals(Accounts.START_BRICKS - 1100, Api.number(w, "balance"));
        assertEquals(409, Api.statusOf(mvc, Api.asStaff(Api.body(post("/api/admin/economy/refund"),
                "{\"target\":\"apple|sim:t-refund-1\",\"reason\":\"again\"}"), support, Roles.SUPPORT)));
        assertTrue(Api.list(mvc, Api.asStaff(get("/api/admin/audit"), support, Roles.SUPPORT))
                .toString().contains("chargeback"));
    }
}
