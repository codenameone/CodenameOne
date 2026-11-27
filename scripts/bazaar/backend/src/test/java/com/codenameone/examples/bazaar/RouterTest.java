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

import com.codenameone.examples.bazaar.routing.Router;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The routing arithmetic, without a database.
class RouterTest {
    private static List<Router.Place> places() {
        List<Router.Place> out = new ArrayList<Router.Place>();
        out.add(new Router.Place("east", 40.7, -74.0));
        out.add(new Router.Place("west", 34.0, -118.2));
        out.add(new Router.Place("mid", 41.9, -87.6));
        return out;
    }

    private static Router.Need need(int qty, int east, int west, int mid) {
        return new Router.Need(qty, new int[] {east, west, mid});
    }

    private static List<Router.Rule> rules(Router.Rule... rs) {
        List<Router.Rule> out = new ArrayList<Router.Rule>();
        for (Router.Rule r : rs) {
            out.add(r);
        }
        return out;
    }

    private static List<String> ids(String... ids) {
        List<String> out = new ArrayList<String>();
        for (String id : ids) {
            out.add(id);
        }
        return out;
    }

    @Test
    void withoutRulesTheFirstLocationHoldingTheUnits() {
        List<Router.Need> needs = new ArrayList<Router.Need>();
        needs.add(need(2, 1, 5, 5));
        Router.Plan p = Router.route(places(), needs, rules(), true, false, 0, 0);
        assertArrayEquals(new String[] {"west"}, p.locations);
    }

    @Test
    void closestPriorityAndMostStock() {
        List<Router.Need> needs = new ArrayList<Router.Need>();
        needs.add(need(1, 5, 5, 9));
        // A customer in Los Angeles.
        assertArrayEquals(new String[] {"west"}, Router.route(places(), needs,
                rules(new Router.Rule(Router.CLOSEST, null)), true, true, 34.05, -118.25)
                .locations);
        // Without a point CLOSEST decides nothing and the next rule does.
        assertArrayEquals(new String[] {"mid"}, Router.route(places(), needs,
                rules(new Router.Rule(Router.CLOSEST, null),
                    new Router.Rule(Router.MOST_STOCK, null)), true, false, 0, 0).locations);
        assertArrayEquals(new String[] {"mid"}, Router.route(places(), needs,
                rules(new Router.Rule(Router.PRIORITY, ids("mid", "east"))), true, true, 34.05,
                -118.25).locations);
        // Holding the units comes before every rule.
        List<Router.Need> big = new ArrayList<Router.Need>();
        big.add(need(7, 5, 5, 9));
        assertArrayEquals(new String[] {"mid"}, Router.route(places(), big,
                rules(new Router.Rule(Router.PRIORITY, ids("east"))), true, false, 0, 0)
                .locations);
    }

    @Test
    void splittingFollowsTheSetting() {
        List<Router.Need> needs = new ArrayList<Router.Need>();
        needs.add(need(1, 3, 0, 3));
        needs.add(need(1, 0, 3, 3));
        List<Router.Rule> r = rules(new Router.Rule(Router.PRIORITY, ids("east", "west")));
        Router.Plan split = Router.route(places(), needs, r, true, false, 0, 0);
        assertArrayEquals(new String[] {"east", "west"}, split.locations);
        assertFalse(split.forcedSplit);
        // Not allowed: the one location holding both lines sends the order.
        Router.Plan whole = Router.route(places(), needs, r, false, false, 0, 0);
        assertArrayEquals(new String[] {"mid", "mid"}, whole.locations);
        // Not allowed and nobody holds both: it splits anyway, and says so.
        List<Router.Need> apart = new ArrayList<Router.Need>();
        apart.add(need(1, 3, 0, 0));
        apart.add(need(1, 0, 3, 0));
        Router.Plan forced = Router.route(places(), apart, r, false, false, 0, 0);
        assertArrayEquals(new String[] {"east", "west"}, forced.locations);
        assertTrue(forced.forcedSplit);
    }
}
