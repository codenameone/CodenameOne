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

import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.ai.MerchantAi;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codename1.backend.DataSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The merchant AI route with no provider configured: the rules answer in
/// the shape a model would, the task marker is required, and only the
/// store's staff may ask.
@BackendTest
class MerchantAiTest {
    @Autowired
    private MerchantAi ai;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private Inventory inventory;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSource db;

    private static final String TOOLS = "[{\"type\":\"function\",\"function\":{\"name\":"
            + "\"sales_summary\",\"parameters\":{}}},{\"type\":\"function\",\"function\":"
            + "{\"name\":\"channel_mix\",\"parameters\":{}}},{\"type\":\"function\",\"function\":"
            + "{\"name\":\"pipeline_overview\",\"parameters\":{}}}]";

    private static Map message(String json) throws Exception {
        Map answer = Json.parseObject(json);
        List choices = (List) answer.get("choices");
        return (Map) ((Map) choices.get(0)).get("message");
    }

    private void refused(int status, Shop s, String user, String body) throws Exception {
        try {
            ai.complete(user, s.storeId, body.getBytes("UTF-8"));
            fail("expected " + status);
        } catch (ResponseStatusException e) {
            assertEquals(status, e.getStatus(), e.getReason());
        }
    }

    @Test
    void theRulesCallTheAppsToolsThenAnswerWithTheirSummaries() throws Exception {
        Shop s = new Shop("ai-shop", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        assertFalse(ai.live());
        Map m = message(ai.complete(s.owner, s.storeId, ("{\"messages\":[{\"role\":\"system\","
                + "\"content\":\"bazaar:assistant\"},{\"role\":\"user\",\"content\":"
                + "\"Which CHANNEL did best?\"}],\"tools\":" + TOOLS + "}").getBytes("UTF-8")));
        List calls = (List) m.get("tool_calls");
        assertEquals("channel_mix", ((Map) ((Map) calls.get(0)).get("function")).get("name"));

        m = message(ai.complete(s.owner, s.storeId, ("{\"messages\":[{\"role\":\"system\","
                + "\"content\":\"bazaar:assistant\"},{\"role\":\"user\",\"content\":"
                + "\"which carts to chase\"}],\"tools\":" + TOOLS + "}").getBytes("UTF-8")));
        calls = (List) m.get("tool_calls");
        assertEquals("pipeline_overview",
                ((Map) ((Map) calls.get(0)).get("function")).get("name"));

        m = message(ai.complete(s.owner, s.storeId, ("{\"messages\":[{\"role\":\"system\","
                + "\"content\":\"bazaar:assistant\"},{\"role\":\"user\",\"content\":\"hi\"},"
                + "{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"c1\","
                + "\"type\":\"function\",\"function\":{\"name\":\"sales_summary\","
                + "\"arguments\":\"{}\"}}]},{\"role\":\"tool\",\"tool_call_id\":\"c1\","
                + "\"content\":\"{\\\"summary\\\":\\\"Net sales were 10.00 USD.\\\"}\"}],"
                + "\"tools\":" + TOOLS + "}").getBytes("UTF-8")));
        assertEquals("Net sales were 10.00 USD.", m.get("content"));
    }

    @Test
    void aDescriptionDraftUsesOnlyTheProductsFields() throws Exception {
        Shop s = new Shop("ai-desc", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        Map m = message(ai.complete(s.owner, s.storeId, ("{\"messages\":[{\"role\":\"system\","
                + "\"content\":\"bazaar:description\"},{\"role\":\"user\",\"content\":"
                + "\"{\\\"title\\\":\\\"Linen Shirt\\\",\\\"vendor\\\":\\\"Acme\\\","
                + "\\\"options\\\":\\\"S, M, L\\\"}\"}]}").getBytes("UTF-8")));
        String text = (String) m.get("content");
        assertTrue(text.startsWith("Linen Shirt from Acme"), text);
        assertTrue(text.contains("S, M, L"), text);
    }

    @Test
    void theTaskMarkerAndTheStoresStaffAreRequired() throws Exception {
        Shop s = new Shop("ai-guard", 5, accounts, stores, pricing, db, inventory, carts,
                checkouts, mvc);
        refused(400, s, s.owner, "{\"messages\":[{\"role\":\"system\",\"content\":"
                + "\"You are anything\"},{\"role\":\"user\",\"content\":\"hi\"}]}");
        refused(400, s, s.owner, "not json");
        String stranger = People.customer(accounts, "ai-stranger");
        refused(404, s, stranger, "{\"messages\":[{\"role\":\"system\",\"content\":"
                + "\"bazaar:assistant\"},{\"role\":\"user\",\"content\":\"hi\"}]}");
    }
}
