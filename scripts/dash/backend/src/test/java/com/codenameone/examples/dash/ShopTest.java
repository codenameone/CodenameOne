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

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.dash.account.Accounts;
import com.codenameone.examples.dash.shop.Shop;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Spending coins and gems, store receipts and rewarded ads. The test profile
/// checks receipts with the simulated store, which accepts any of them.
@BackendTest
class ShopTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    @Test
    void anItemCostsWhatTheCatalogSaysAndIsOwnedOnce() throws Exception {
        String who = Api.player(accounts, "shopper");
        assertTrue(Api.list(mvc, Api.as(get("/api/shop"), who)).size() >= 6);
        // 100 coins buys nothing that costs 500.
        assertEquals(402, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/buy"),
                "{\"itemId\":\"runner-nova\"}"), who)));
        // A real-money item is never sold for coins.
        assertEquals(404, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/buy"),
                "{\"itemId\":\"gems-80\"}"), who)));
        assertEquals(402, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/buy"),
                "{\"itemId\":\"emotes-taunt\"}"), who)));
        Api.json(mvc, Api.as(Api.body(post("/api/shop/receipt"), "{\"store\":\"play\",\"sku\":"
                + "\"dash.gems_80\",\"transactionId\":\"tx-shopper\",\"receipt\":\"r\"}"), who));
        Map wallet = Api.json(mvc, Api.as(Api.body(post("/api/shop/buy"),
                "{\"itemId\":\"emotes-taunt\"}"), who));
        assertEquals(70L, Api.number(wallet, "gems"));
        assertTrue(((List) wallet.get("owned")).size() >= 2);
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/equip"),
                "{\"outfit\":\"trail-comet\"}"), who)));
    }

    // tag::dash-receipt-test[]
    @Test
    void aReceiptIsRedeemedOnceAndOnlyByItsOwner() throws Exception {
        String who = Api.player(accounts, "buyer");
        String thief = Api.player(accounts, "thief");
        String receipt = "{\"store\":\"itunes\",\"sku\":\"dash.gems_80\","
                + "\"transactionId\":\"tx-1001\",\"receipt\":\"opaque\"}";
        Map first = Api.json(mvc, Api.as(Api.body(post("/api/shop/receipt"), receipt), who));
        assertEquals(100L, Api.number(first, "gems"));
        // The app retries after a lost answer: the same purchase, granted once.
        Map again = Api.json(mvc, Api.as(Api.body(post("/api/shop/receipt"), receipt), who));
        assertEquals(100L, Api.number(again, "gems"));
        assertEquals(409, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/receipt"),
                receipt), thief)));
        assertEquals(404, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/receipt"),
                "{\"store\":\"itunes\",\"sku\":\"nope\",\"transactionId\":\"tx-2\",\"receipt\":\"r\"}"),
                who)));
    }
    // end::dash-receipt-test[]

    @Test
    void rewardedAdsAreCappedPerDay() throws Exception {
        String who = Api.player(accounts, "watcher");
        long coins = 100;
        for (int i = 0; i < Shop.AD_REWARDS_PER_DAY; i++) {
            Map w = Api.json(mvc, Api.as(Api.body(post("/api/shop/ad-reward"), "{\"nonce\":\"n" + i
                    + "\"}"), who));
            coins += Shop.AD_REWARD_COINS;
            assertEquals(coins, Api.number(w, "coins"));
        }
        assertEquals(429, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/ad-reward"),
                "{\"nonce\":\"more\"}"), who)));
    }
}
