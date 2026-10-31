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
package com.codenameone.examples.dash.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/// A route becomes the same level wherever it is built, and a strong runner
/// can finish it.
class CityRouteTest {
    static CityRoute harbour() {
        return new CityRoute("Old Harbour")
                .leg(420, CityRoute.STRAIGHT, "Quay Street")
                .leg(310, CityRoute.LEFT, "Rope Walk")
                .leg(30, CityRoute.RIGHT, "Tide Lane")
                .leg(650, CityRoute.RIGHT, "Lantern Avenue")
                .leg(280, CityRoute.STRAIGHT, "")
                .leg(510, CityRoute.LEFT, "Market Square")
                .leg(900, CityRoute.RIGHT, "Ferry Road");
    }

    @Test
    void theSpecRoundTripsToTheSameLevel() {
        CityRoute r = harbour();
        CityRoute back = CityRoute.decode(r.encode());
        assertEquals(r.encode(), back.encode());
        assertEquals(r.id(), back.id());
        LevelDef a = r.level();
        LevelDef b = back.level();
        assertEquals(a.id, b.id);
        assertEquals(a.contentHash(), b.contentHash());
        assertEquals(a.dialog.size(), b.dialog.size());
        assertTrue(a.id.startsWith(CityRoute.ID_PREFIX));
    }

    @Test
    void aLongRouteRunsToScaleAndAShortOneIsStretched() {
        assertEquals(CityRoute.MAX_METRES * Track.UNIT, harbour().level().length);
        LevelDef shortRun = new CityRoute("Corner").leg(120, CityRoute.STRAIGHT, "A").level();
        assertEquals(CityRoute.MIN_METRES * Track.UNIT, shortRun.length);
    }

    @Test
    void cornersAreCalledByStreetAndBlockTheOtherLanes() {
        LevelDef l = harbour().level();
        boolean left = false;
        boolean right = false;
        for (DialogLine line : l.dialog) {
            left |= line.text.equals("Left onto Rope Walk!") && line.hint == Input.LEFT;
            right |= line.text.startsWith("Right onto ") && line.hint == Input.RIGHT;
        }
        assertTrue(left, "the left turn is called");
        assertTrue(right, "a right turn is called");
        // Tide Lane is 30 m between two turns: the turn out of it is passed,
        // not cornered a few metres after the turn into it.
        for (DialogLine line : l.dialog) {
            assertTrue(!line.text.contains("Lantern Avenue"), "a corner every few metres");
        }
        // Dialog runs in order, so the client can walk it front to back.
        for (int i = 1; i < l.dialog.size(); i++) {
            assertTrue(l.dialog.get(i).at >= l.dialog.get(i - 1).at, "dialog out of order");
        }
    }

    @Test
    void aStrongRunnerFinishesIt() {
        assertTrue(Validator.beatable(harbour().level(), Content.catalog()));
        assertTrue(!Validator.hasErrors(Validator.check(harbour().level(), Content.catalog())));
    }

    @Test
    void anotherRouteIsAnotherLevel() {
        CityRoute other = new CityRoute("Old Harbour").leg(420, CityRoute.STRAIGHT, "Quay Street")
                .leg(310, CityRoute.RIGHT, "Rope Walk");
        assertNotEquals(harbour().id(), other.id());
    }

    @Test
    void namesLoseTheSeparatorsAndJunkIsRefused() {
        CityRoute r = new CityRoute("A~B|C").leg(500, CityRoute.STRAIGHT, "One, Two");
        assertEquals("ABC", r.place);
        assertEquals("One Two", r.legs().get(0).street);
        assertThrows(IllegalArgumentException.class, () -> CityRoute.decode("nonsense"));
        assertThrows(IllegalArgumentException.class, () -> CityRoute.decode("1~x~ten,0,a"));
        assertThrows(IllegalArgumentException.class, () -> CityRoute.decode("1~x~10,7,a"));
        assertThrows(IllegalArgumentException.class, () -> CityRoute.decode(null));
    }
}
