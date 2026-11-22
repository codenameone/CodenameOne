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
package com.codenameone.examples.bazaar;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.ai.MerchantAiController;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.web.security.Throttle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/// The routes where one request costs the server real work or reaches a
/// stranger's text into a store are held to a rate ([Throttle]): the
/// merchant assistant (each request a model call) and a review sent through
/// an e-mailed review link.
@BackendTest
class CostlyRoutesRateLimitTest {
    private static final long DAY = 86400000L;
    /// Grows with every test, so the clock only ever moves forward.
    private static long skipped;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private DataSource db;
    @Autowired
    private Inventory inventory;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private Throttle throttle;

    /// Every bucket refills before and after each test: the throttle's clock
    /// jumps a day ahead (and stays ahead, so no bucket ever sees time run backward),
    /// and the rest of the suite starts with full allowances.
    @BeforeEach
    @AfterEach
    void refill() {
        skipped += DAY;
        final long offset = skipped;
        throttle.useClock(new Throttle.Clock() {
            @Override
            public long now() {
                return System.currentTimeMillis() + offset;
            }
        });
    }

    private int status(com.codename1.backend.test.MockRequestBuilder r) throws Exception {
        return mvc.perform(r).andReturn().getResponse().getStatus();
    }

    @Test
    void theMerchantAssistantIsHeldToTwentyRequestsAMinute() throws Exception {
        Shop s = new Shop("rl-ai", 1, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        String path = MerchantAiController.PATH.replace("{storeId}", s.storeId);
        String body = "{\"messages\":[{\"role\":\"system\",\"content\":\"bazaar:assistant\"},"
                + "{\"role\":\"user\",\"content\":\"hi\"}]}";
        for (int iter = 0; iter < 20; iter++) {
            assertNotEquals(429, status(People.asMerchant(Rest.body(post(path), body), s.owner)),
                    "request " + (iter + 1));
        }
        assertEquals(429, status(People.asMerchant(Rest.body(post(path), body), s.owner)));
    }

    @Test
    void aReviewLinkIsHeldToTenPosts() throws Exception {
        for (int iter = 0; iter < 10; iter++) {
            assertNotEquals(429, status(post("/review/rl-no-such-link?s=forged")),
                    "post " + (iter + 1));
        }
        assertEquals(429, status(post("/review/rl-no-such-link?s=forged")));
    }
}
