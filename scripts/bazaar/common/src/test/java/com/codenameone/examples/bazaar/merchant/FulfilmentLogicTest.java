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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.pdf.PdfReader;
import com.codenameone.examples.bazaar.api.BoardColumnDto;
import com.codenameone.examples.bazaar.api.OpenLineDto;
import com.codenameone.examples.bazaar.api.OrderCardDto;
import com.codenameone.examples.bazaar.api.RateQuoteDto;
import com.codenameone.examples.bazaar.api.UnfulfilledDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// The fulfilment board's pure logic: column mapping, moves, ages, rate
/// order, pick list aggregation and the pick list PDF. No display.
class FulfilmentLogicTest {
    private static OrderCardDto card(String id, int n, String column) {
        return new OrderCardDto(id, n, "Ann", 2, 1000, "USD", column, "", "", 0);
    }

    private static UnfulfilledDto order(String id, int n, OpenLineDto... lines) {
        return new UnfulfilledDto(id, n, "Ann", "Standard", 0, Arrays.asList(lines));
    }

    private static OpenLineDto line(String sku, String loc, int open) {
        return new OpenLineDto("l" + sku + open, "Mug " + sku, "Default", sku, loc, open);
    }

    @Test
    void boardColumnsComeInBoardOrderWithCards() {
        List<BoardColumnDto> cols = Arrays.asList(
                new BoardColumnDto("DELIVERED", "Delivered", new ArrayList<OrderCardDto>()),
                new BoardColumnDto("EXTRA", "Extra", new ArrayList<OrderCardDto>()),
                new BoardColumnDto("TO_PACK", "To pack", Arrays.asList(card("o1", 1, "TO_PACK"))),
                new BoardColumnDto("SHIPPED", "Shipped", new ArrayList<OrderCardDto>()),
                new BoardColumnDto("PACKED", "Packed", Arrays.asList(card("o2", 2, "PACKED"))));
        Map<String, String> where = new HashMap<String, String>();
        where.put("o1", "Main store");
        DefaultKanbanModel m = Fulfil.model(cols, where, 0);
        assertEquals("TO_PACK", m.getColumns().get(0).getId());
        assertEquals("PACKED", m.getColumns().get(1).getId());
        assertEquals("DELIVERED", m.getColumns().get(3).getId());
        assertEquals("EXTRA", m.getColumns().get(4).getId());
        assertEquals("TO_PACK", m.getCard("o1").getColumnId());
        assertEquals("Main store", m.getCard("o1").getTags().get(0));
        assertTrue(m.getCard("o2").getTags().isEmpty());
    }

    @Test
    void movesOnlyOneColumnRight() {
        assertTrue(Fulfil.canMove("TO_PACK", "PACKED"));
        assertTrue(Fulfil.canMove("shipped", "DELIVERED"));
        assertFalse(Fulfil.canMove("TO_PACK", "SHIPPED"));
        assertFalse(Fulfil.canMove("PACKED", "TO_PACK"));
        assertFalse(Fulfil.canMove("NOPE", "PACKED"));
        assertEquals("PACKED", Fulfil.next("TO_PACK"));
        assertNull(Fulfil.next("DELIVERED"));
    }

    @Test
    void agesInMinutesHoursDays() {
        long now = 10L * 24 * 3600000L;
        assertEquals("5m", Fulfil.age(now - 5 * 60000L, now));
        assertEquals("3h", Fulfil.age(now - 3 * 3600000L, now));
        assertEquals("3d", Fulfil.age(now - 72 * 3600000L, now));
        assertEquals("", Fulfil.age(0, now));
    }

    @Test
    void ratesCheapestThenFastest() {
        List<RateQuoteDto> sorted = Fulfil.sortRates(Arrays.asList(
                new RateQuoteDto("UPS", "air", "Air", 2500, 1),
                new RateQuoteDto("USPS", "slow", "Slow", 900, 6),
                new RateQuoteDto("USPS", "std", "Std", 900, 3),
                null));
        assertEquals(3, sorted.size());
        assertEquals("std", sorted.get(0).service());
        assertEquals("slow", sorted.get(1).service());
        assertEquals("air", Fulfil.fastest(sorted).service());
        assertTrue(Fulfil.sortRates(null).isEmpty());
    }

    @Test
    void pickListAddsUpSkusPerLocation() {
        List<UnfulfilledDto> queue = Arrays.asList(
                order("o1", 1, line("B", "L1", 2), line("A", "L2", 1)),
                order("o2", 2, line("B", "L1", 3), line("A", "L1", 0)),
                order("o3", 3, line("B", "L1", 7)));
        Map<String, String> names = new HashMap<String, String>();
        names.put("L1", "Back room");
        names.put("L2", "Annex");
        List<Fulfil.PickLine> all = Fulfil.pickList(queue, Arrays.asList("o1", "o2"), names);
        assertEquals(2, all.size());
        assertEquals("Annex", all.get(0).location);
        assertEquals("Back room", all.get(1).location);
        assertEquals(5, all.get(1).quantity);
        assertEquals(Arrays.asList(1, 2), all.get(1).orders);
        assertEquals(6, Fulfil.units(all));
        assertEquals(13, Fulfil.units(Fulfil.pickList(queue, null, names)));
    }

    @Test
    void pickListPdfIsMadeWithoutADisplay() throws Exception {
        List<UnfulfilledDto> queue = new ArrayList<UnfulfilledDto>();
        for (int iter = 0; iter < 40; iter++) {
            queue.add(order("o" + iter, iter, line("S" + iter, "L1", 1 + iter % 3)));
        }
        byte[] pdf = PickListPdf.build("Northwind - To pack", "today", queue, null, null);
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, "ISO-8859-1"));
        assertTrue(PdfReader.open(pdf).getPageCount() >= 2);
        assertEquals("bazaar:order:o1", PickListPdf.qrText(queue.get(1)));
    }
}
