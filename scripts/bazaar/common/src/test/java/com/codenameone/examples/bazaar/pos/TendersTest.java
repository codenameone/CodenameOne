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
package com.codenameone.examples.bazaar.pos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TendersTest {
    @Test
    void cancelledCardTakesItsTipBack() {
        Tenders t = new Tenders(1000);
        assertNull(t.tip(150));
        assertEquals(1150, t.due());
        t.dropTip();
        assertEquals(1000, t.due());
        assertEquals(0, t.tipCents());
        assertNull(t.add(Tenders.CASH, 1000, ""));
        assertTrue(t.complete());
        assertEquals(0, t.saleTipCents());
    }

    @Test
    void cashFinishCarriesNoTipEvenIfOneWasSet() {
        Tenders t = new Tenders(1000);
        t.tip(200);
        assertNull(t.add(Tenders.CASH, 1200, ""));
        assertEquals(0, t.saleTipCents());
    }

    @Test
    void takenCardKeepsTipAndCannotBeRemoved() {
        Tenders t = new Tenders(1000);
        t.tip(100);
        assertNull(t.add(Tenders.CARD, 1100, "pay-1"));
        t.dropTip();
        assertEquals(100, t.saleTipCents());
        assertFalse(t.removable(0));
        t.remove(0);
        assertEquals(1, t.list().size());
    }

    @Test
    void cashIsRemovable() {
        Tenders t = new Tenders(1000);
        t.add(Tenders.CASH, 500, "");
        assertTrue(t.removable(0));
        assertFalse(t.removable(1));
        t.remove(0);
        assertEquals(0, t.list().size());
    }

    @Test
    void paidSaleRefusesMoreTenders() {
        Tenders t = new Tenders(1000);
        assertNull(t.add(Tenders.CASH, 2000, ""));
        assertTrue(t.complete());
        assertNotNull(t.add(Tenders.CASH, 500, ""));
        assertEquals(1, t.list().size());
    }
}
