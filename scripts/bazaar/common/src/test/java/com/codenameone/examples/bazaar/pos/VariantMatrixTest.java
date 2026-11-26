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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.codenameone.examples.bazaar.api.PosItemDto;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class VariantMatrixTest {
    private static PosItemDto v(String id, String title, long price, int stock) {
        return new PosItemDto(id, "p", "Tee", title, "", "", price, 0, true, true, stock, "");
    }

    private static VariantMatrix tees() {
        List<PosItemDto> l = new ArrayList<PosItemDto>();
        l.add(v("s-c", "S / Charcoal", 2800, 0));
        l.add(v("m-c", "M / Charcoal", 2800, 4));
        l.add(v("l-c", "L / Charcoal", 3000, 2));
        l.add(v("s-o", "S / Oat", 2800, 1));
        l.add(v("m-o", "M / Oat", 2800, 0));
        return new VariantMatrix(l);
    }

    @Test
    void splitsTitlesIntoNamedOptionRows() {
        VariantMatrix m = tees();
        assertEquals(2, m.groups());
        assertEquals("Size", m.name(0));
        assertEquals("Colour", m.name(1));
        assertEquals(3, m.values(0).size());
        assertEquals(2, m.values(1).size());
        assertArrayEquals(new String[] {"Heather grey"}, VariantMatrix.parts("Heather grey"));
    }

    @Test
    void statesDimSoldOutAndMissingCombinations() {
        VariantMatrix m = tees();
        String[] oat = {null, "Oat"};
        assertEquals(VariantMatrix.AVAILABLE, m.state(0, "S", oat));
        assertEquals(VariantMatrix.SOLD_OUT, m.state(0, "M", oat));
        assertEquals(VariantMatrix.MISSING, m.state(0, "L", oat));
    }

    @Test
    void startsOnAVariantInStockAndOneTapLandsOnARealOne() {
        VariantMatrix m = tees();
        String[] first = m.initial();
        assertArrayEquals(new String[] {"M", "Charcoal"}, first);
        assertEquals("m-c", m.find(first).variantId());
        // L comes only in charcoal: picking it from oat moves the colour too.
        String[] l = m.choose(new String[] {"S", "Oat"}, 0, "L");
        assertArrayEquals(new String[] {"L", "Charcoal"}, l);
        assertEquals(3000, m.find(l).priceCents());
        assertNull(m.find(new String[] {null, "Oat"}));
    }
}
