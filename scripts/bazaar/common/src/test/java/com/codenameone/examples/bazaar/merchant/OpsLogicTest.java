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
package com.codenameone.examples.bazaar.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.bazaar.ops.Cohorts;
import com.codenameone.examples.bazaar.ops.Forecast;
import com.codenameone.examples.bazaar.ops.MarketplaceRows;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.ops.WarehouseFlow;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The pure logic behind the warehouse board, forecasts, cohorts,
/// permission sets and the marketplace import.
class OpsLogicTest {
    @Test
    void warehouseStepsOneStageAtATime() {
        assertEquals(WarehouseFlow.RECEIVING, WarehouseFlow.first(WarehouseFlow.INBOUND));
        assertEquals(WarehouseFlow.PICK, WarehouseFlow.first(WarehouseFlow.OUTBOUND));
        assertTrue(WarehouseFlow.legal(WarehouseFlow.OUTBOUND, WarehouseFlow.PICK, WarehouseFlow.PACK));
        assertTrue(WarehouseFlow.legal(WarehouseFlow.OUTBOUND, WarehouseFlow.PACK, WarehouseFlow.PICK));
        assertFalse(WarehouseFlow.legal(WarehouseFlow.OUTBOUND, WarehouseFlow.PICK, WarehouseFlow.DISPATCH));
        assertFalse(WarehouseFlow.legal(WarehouseFlow.OUTBOUND, WarehouseFlow.DONE, WarehouseFlow.DISPATCH));
        assertFalse(WarehouseFlow.legal(WarehouseFlow.INBOUND, WarehouseFlow.RECEIVING, WarehouseFlow.PICK));
        assertEquals(WarehouseFlow.DONE, WarehouseFlow.next(WarehouseFlow.INBOUND, WarehouseFlow.PUTAWAY));
    }

    @Test
    void pickPathIsASerpentine() {
        int[][] bins = {{2, 1, 1}, {1, 1, 2}, {1, 1, 1}, {2, 1, 2}};
        int[] order = WarehouseFlow.pathOrder(bins);
        assertEquals(4, order.length);
        // Aisle 1 first, ascending; aisle 2 walked back down.
        assertEquals(2, order[0]);
        assertEquals(1, order[1]);
        assertEquals(3, order[2]);
        assertEquals(0, order[3]);
    }

    @Test
    void forecastCoverAndReorder() {
        assertEquals(Forecast.NO_DEMAND, Forecast.daysOfCover(10, 0));
        assertEquals(40, Forecast.daysOfCover(10, 25));
        assertEquals(0, Forecast.reorderQuantity(100, 0, 100));
        // 1 a day, 5 on hand: short, ordered up to 30 days.
        assertEquals(25, Forecast.reorderQuantity(5, 0, 100));
        assertEquals(20, Forecast.reorderQuantity(5, 5, 100));
        int[] flat = new int[Forecast.WINDOW];
        for (int iter = 0; iter < flat.length; iter++) {
            flat[iter] = 1;
        }
        assertEquals(100, Forecast.baseX100(flat));
        assertEquals(0, Forecast.baseX100(new int[Forecast.WINDOW]));
    }

    @Test
    void rebalanceMovesFromOverstockToShort() {
        List<Forecast.Move> moves = Forecast.rebalance(new long[] {200, 2}, new long[] {100, 100});
        assertEquals(1, moves.size());
        assertEquals(0, moves.get(0).from);
        assertEquals(1, moves.get(0).to);
        assertTrue(moves.get(0).quantity > 0);
        assertTrue(Forecast.rebalance(new long[] {30, 30}, new long[] {100, 100}).isEmpty());
    }

    @Test
    void cohortsCountReturningCustomers() {
        String[] who = {"a", "b", "a", "c", "b"};
        int[] month = {10, 10, 11, 11, 12};
        List<Cohorts.Row> grid = Cohorts.grid(who, month, 12, 6);
        assertEquals(2, grid.size());
        Cohorts.Row first = grid.get(0);
        assertEquals(10, first.month);
        assertEquals(2, first.size);
        assertEquals(10000, first.retainedBp[0]);
        assertEquals(5000, first.retainedBp[1]);
        assertEquals(5000, first.retainedBp[2]);
        assertEquals(1, grid.get(1).size);
    }

    @Test
    void permissionSetsReadAndWrite() {
        char[] levels = new char[Perms.AREAS.length];
        for (int iter = 0; iter < levels.length; iter++) {
            levels[iter] = Perms.NONE;
        }
        levels[0] = Perms.WRITE;
        levels[1] = Perms.READ;
        String grants = Perms.format(levels);
        assertTrue(Perms.allows(grants, Perms.AREAS[0], true));
        assertTrue(Perms.allows(grants, Perms.AREAS[1], false));
        assertFalse(Perms.allows(grants, Perms.AREAS[1], true));
        assertFalse(Perms.allows(grants, Perms.AREAS[2], false));
        assertFalse(Perms.allows(grants, "nonsense", false));
        assertEquals(grants, Perms.normalize(grants));
    }

    @Test
    void marketplaceRowsGroupAndRefuse() {
        String csv = "listing_id,title,description,brand,category,color,size,sku,price,quantity\n"
                + "L1,\"Tee, cotton\",Soft,Acme,Tops,Red,S,T-R-S,12.50,3\n"
                + "L1,\"Tee, cotton\",Soft,Acme,Tops,Blue,S,T-B-S,12.50,4\n"
                + "L2,Mug,,Acme,Home,,,M-1,abc,1\n";
        MarketplaceRows.Plan plan = MarketplaceRows.plan(csv);
        assertEquals(3, plan.rows);
        assertEquals("Tee, cotton", plan.listings.get(0).title);
        assertEquals(2, plan.listings.get(0).items.size());
        assertEquals(1250, plan.listings.get(0).items.get(0).priceCents);
        assertEquals(1, plan.errors.size());
        assertEquals(Integer.valueOf(4), plan.errors.get(0)[0]);
        assertEquals(1250, MarketplaceRows.cents("12.5"));
    }
}
