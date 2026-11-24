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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.bazaar.account.DemoAccounts;
import com.codenameone.examples.bazaar.api.PipelineCardDto;
import com.codenameone.examples.bazaar.api.PipelineColumnDto;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.live.LiveHub;
import com.codenameone.examples.bazaar.ops.SalesPipeline;
import com.codenameone.examples.bazaar.pipeline.Pipeline;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.DemoPipeline;
import com.codenameone.examples.bazaar.store.DemoStores;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// The walkthrough's self-moving pipeline: off on a test server, a seed that
/// puts cards in every column, and steps that are real transitions, each one
/// published on the live socket.
@BackendTest
class DemoPipelineTest {
    @Autowired
    private DemoPipeline demo;
    @Autowired
    private DemoStores demoStores;
    @Autowired
    private StoreRepository storeRows;
    @Autowired
    private Pipeline pipeline;
    @Autowired
    private LiveHub hub;

    @Test
    void seedsEveryColumnAndEachStepMovesACardAndIsPublished() throws Exception {
        assertFalse(demo.enabled(), "never on unless bazaar.demo.pipeline asks");
        demoStores.seed(DemoAccounts.MERCHANT, DemoAccounts.CASHIER);
        Store store = storeRows.findByHandle(DemoStores.NORTHWIND);
        long before = hub.changes();
        assertTrue(demo.seed(store.getId()));
        assertFalse(demo.seed(store.getId()), "seeds once");
        assertTrue(hub.changes() > before, "the seeded orders were published");
        Map<String, Integer> counts = counts(board(store));
        String[] filled = {SalesPipeline.BROWSING, SalesPipeline.CHECKOUT, SalesPipeline.PAID,
            SalesPipeline.FULFILLING, SalesPipeline.SHIPPED, SalesPipeline.DELIVERED};
        for (String column : filled) {
            assertTrue(counts.get(column) != null && counts.get(column) > 0,
                    () -> column + " empty: " + counts);
        }
        Map<String, String> was = columns(board(store));
        int moved = 0;
        for (int iter = 0; iter < 6; iter++) {
            long heard = hub.changes();
            String kind = demo.step(store.getId());
            assertTrue(DemoPipeline.IDLE.equals(kind) || hub.changes() > heard,
                    () -> kind + " was not published");
            Map<String, String> now = columns(board(store));
            for (Map.Entry<String, String> e : now.entrySet()) {
                String old = was.get(e.getKey());
                if (old == null || !old.equals(e.getValue())) {
                    moved++;
                }
            }
            was = now;
        }
        final int total = moved;
        assertTrue(total >= 4, () -> "cards arrived or moved: " + total);
    }

    private List<PipelineColumnDto> board(Store store) throws Exception {
        return pipeline.board(DemoAccounts.MERCHANT, store.getId(), null, 0);
    }

    private static Map<String, Integer> counts(List<PipelineColumnDto> board) {
        Map<String, Integer> out = new HashMap<String, Integer>();
        for (PipelineColumnDto c : board) {
            out.put(c.id(), c.total());
        }
        return out;
    }

    private static Map<String, String> columns(List<PipelineColumnDto> board) {
        Map<String, String> out = new HashMap<String, String>();
        for (PipelineColumnDto c : board) {
            for (PipelineCardDto card : c.cards()) {
                out.put(card.id(), c.id());
            }
        }
        return out;
    }
}
