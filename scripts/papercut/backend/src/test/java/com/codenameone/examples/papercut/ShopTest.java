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

/// Spending ink coins, store receipts and rewarded ads. The test profile
/// checks receipts with the simulated store, which accepts any of them.
@BackendTest
class ShopTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    @Test
    void papersAreBoughtOnceAndEquippedOnlyWhenOwned() throws Exception {
        String who = Api.player(accounts, "shopper");
        List items = Api.list(mvc, Api.as(get("/api/shop"), who));
        assertEquals(9, items.size());
        assertEquals(402, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/buy"),
                "{\"itemId\":\"quill-kraft\"}"), who)));
        // A coin pack costs real money and is never sold for coins.
        assertEquals(404, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/buy"),
                "{\"itemId\":\"coins-small\"}"), who)));
        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/equip"),
                "{\"quill\":\"quill-kraft\"}"), who)));
        Api.json(mvc, Api.as(Api.body(post("/api/shop/receipt"), "{\"store\":\"play\",\"sku\":"
                + "\"papercut.coins.small\",\"transactionId\":\"tx-shopper\",\"receipt\":\"r\"}"), who));
        Map wallet = Api.json(mvc, Api.as(Api.body(post("/api/shop/buy"), "{\"itemId\":\"quill-kraft\"}"), who));
        assertEquals(Accounts.START_COINS + 500L - 150, Api.number(wallet, "inkCoins"));
        assertTrue(((List) wallet.get("owned")).contains("quill-kraft"));
        assertEquals(409, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/buy"),
                "{\"itemId\":\"quill-kraft\"}"), who)));
        Map equipped = Api.json(mvc, Api.as(Api.body(post("/api/shop/equip"), "{\"quill\":\"quill-kraft\"}"), who));
        assertEquals("quill-kraft", equipped.get("quill"));
    }

    @Test
    void spareErasersAreBoughtInPairsAndSpentTogether() throws Exception {
        String who = Api.player(accounts, "eraser");
        Map wallet = Api.json(mvc, Api.as(Api.body(post("/api/shop/buy"), "{\"itemId\":\"erasers-2\"}"), who));
        assertEquals(2L, Api.number(wallet, "erasers"));
        assertEquals(Accounts.START_COINS - 40L, Api.number(wallet, "inkCoins"));
        Map used = Api.json(mvc, Api.as(post("/api/shop/use-erasers"), who));
        assertEquals(0L, Api.number(used, "erasers"));
    }

    @Test
    void aReceiptIsRedeemedOnceAndOnlyByItsOwner() throws Exception {
        String who = Api.player(accounts, "buyer");
        String thief = Api.player(accounts, "thief");
        String receipt = "{\"store\":\"itunes\",\"sku\":\"papercut.coins.large\","
                + "\"transactionId\":\"tx-1001\",\"receipt\":\"opaque\"}";
        Map first = Api.json(mvc, Api.as(Api.body(post("/api/shop/receipt"), receipt), who));
        assertEquals(Accounts.START_COINS + 3000L, Api.number(first, "inkCoins"));
        // The app retries after a lost answer: the same purchase, granted once.
        Map again = Api.json(mvc, Api.as(Api.body(post("/api/shop/receipt"), receipt), who));
        assertEquals(Accounts.START_COINS + 3000L, Api.number(again, "inkCoins"));
        assertEquals(409, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/receipt"), receipt), thief)));
        assertEquals(404, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/receipt"),
                "{\"store\":\"itunes\",\"sku\":\"papercut.coins.huge\",\"transactionId\":\"tx-2\","
                + "\"receipt\":\"r\"}"), who)));
    }

    @Test
    void anAdNoncePaysOnce() throws Exception {
        String who = Api.player(accounts, "watcher");
        Map paid = Api.json(mvc, Api.as(Api.body(post("/api/shop/ad-reward"), "{\"nonce\":\"ad-77\"}"), who));
        assertEquals(Accounts.START_COINS + 15L, Api.number(paid, "inkCoins"));
        assertEquals(409, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/ad-reward"),
                "{\"nonce\":\"ad-77\"}"), who)));
        assertFalse(Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/ad-reward"), "{}"), who)) == 200);
        // A client mints its own nonces, so the day's count is what bounds it.
        for (int i = 1; i < com.codenameone.examples.papercut.shop.Shop.AD_REWARDS_PER_DAY; i++) {
            assertEquals(200, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/ad-reward"),
                    "{\"nonce\":\"cap-" + i + "\"}"), who)));
        }
        assertEquals(429, Api.statusOf(mvc, Api.as(Api.body(post("/api/shop/ad-reward"),
                "{\"nonce\":\"cap-over\"}"), who)));
    }
}
