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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.mapping.Mappers;
import com.codenameone.examples.bazaar.api.ParkedCartDto;
import com.codenameone.examples.bazaar.api.ParkedCartDtoCn1Mapper;
import com.codenameone.examples.bazaar.api.PosItemDto;
import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.api.PosLineDtoCn1Mapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// Holds kept offline and replayed, the tip on the card and custom items.
class ParkedCartsTest {
    private final Map<String, String> disk = new HashMap<String, String>();

    private final Outbox.Persist persist = new Outbox.Persist() {
        @Override
        public String read(String key) {
            return disk.get(key);
        }

        @Override
        public void write(String key, String value) {
            disk.put(key, value);
        }
    };

    private static ParkedCarts.Codec codec() {
        ParkedCartDtoCn1Mapper.register();
        PosLineDtoCn1Mapper.register();
        return new ParkedCarts.Codec() {
            @Override
            public String encode(ParkedCartDto cart) {
                return Mappers.toJson(cart);
            }

            @Override
            public ParkedCartDto decode(String text) {
                return Mappers.fromJson(text, ParkedCartDto.class);
            }
        };
    }

    private static ParkedCartDto hold(String clientId, int qty) {
        return new ParkedCartDto("", clientId, "Coat", "", "", "",
                Arrays.asList(new PosLineDto("v1", qty, 1000, "", true, false)), "", 1L);
    }

    private static ParkedCartDto server(String clientId, String status) {
        return new ParkedCartDto("s-" + clientId, clientId, "Coat", "", "", "", null, status, 1L);
    }

    @Test
    void aHoldMadeOfflineIsReplayedOnceAndInOrder() {
        ParkedCarts carts = new ParkedCarts(persist, codec());
        ParkedCarts.Entry first = carts.park(hold("h1", 2));
        assertSame(first, carts.park(hold("h1", 5)), "the same hold again is the same hold");
        carts.park(hold("h2", 1));
        assertEquals(2, carts.held().size());
        // Resumed while still offline: the hold goes up first, then the resume.
        ParkedCartDto back = carts.resume("h1");
        assertNotNull(back);
        assertEquals(2, back.lines().get(0).quantity());
        assertNull(carts.resume("h1"), "a hold is resumed once");
        assertNull(carts.discard("h1"));
        assertEquals("PARK", carts.find("h1").due());
        List<ParkedCarts.Entry> due = carts.due();
        assertEquals(2, due.size());

        // The device restarts before anything is sent.
        ParkedCarts again = new ParkedCarts(persist, codec());
        assertEquals(1, again.held().size());
        assertEquals(ParkedCarts.RESUMED, again.find("h1").status);
        assertEquals("PARK", again.find("h1").due());

        // The server answers the hold (twice, a retried request): PARKED.
        again.parked("h1", server("h1", ParkedCarts.PARKED));
        again.parked("h1", server("h1", ParkedCarts.PARKED));
        assertEquals("RESUME", again.find("h1").due());
        assertEquals(ParkedCarts.RESUMED, again.find("h1").status,
                "a late answer for the hold does not bring the cart back");
        again.moved("h1");
        again.moved("h1");
        assertNull(again.find("h1").due());
        again.parked("h2", server("h2", ParkedCarts.PARKED));
        assertTrue(again.due().isEmpty());
        assertEquals("s-h2", new ParkedCarts(persist, codec()).find("h2").serverId);
    }

    @Test
    void anotherRegisterResumingWins() {
        ParkedCarts carts = new ParkedCarts(persist, codec());
        carts.park(hold("h3", 1));
        carts.parked("h3", server("h3", ParkedCarts.PARKED));
        assertNotNull(carts.discard("h3"));
        carts.refused("h3", ParkedCarts.RESUMED);
        assertEquals(ParkedCarts.RESUMED, carts.find("h3").status);
        assertNull(carts.find("h3").due());
        assertTrue(carts.held().isEmpty());
        // A replayed hold answered RESUMED by the server stays off the list.
        carts.park(hold("h4", 1));
        carts.parked("h4", server("h4", ParkedCarts.RESUMED));
        assertTrue(carts.held().isEmpty());
        assertFalse(carts.find("h4").due() != null);
    }

    @Test
    void aTipIsAddedOnceBeforeTheCard() {
        Tenders t = new Tenders(1040);
        assertNull(t.tip(PosCart.tipFor(1040, 1500)));
        assertEquals(156, t.tipCents());
        assertEquals(1196, t.due());
        assertNull(t.tip(100));
        assertEquals(1140, t.due(), "a second tip replaces the first");
        assertNull(t.add(Tenders.CARD, 1140, "ref"));
        assertNotNull(t.tip(50), "no tip after the card was charged");
        assertNotNull(t.tip(-1));
        assertEquals(0, PosCart.tipFor(0, 2000));
    }

    @Test
    void customItemsAreSeparateLinesWithTheirTitle() {
        PosCart cart = new PosCart();
        PosItemDto wrap = PosCart.customItem("Gift wrap", 300, false);
        assertTrue(PosCart.custom(wrap));
        cart.add(wrap, 1);
        cart.add(PosCart.customItem("Engraving", 500, true), 1);
        List<PosLineDto> lines = cart.uploadLines();
        assertEquals(2, lines.size());
        assertEquals("", lines.get(0).variantId());
        assertEquals("Gift wrap", lines.get(0).title());
        assertFalse(lines.get(0).taxable());
        assertEquals(500, lines.get(1).unitCents());
        assertFalse(cart.overridden());
        cart.setTipCents(200);
        cart.setApprovalId("a1");
        cart.clear();
        assertEquals(0, cart.tipCents());
        assertEquals("", cart.approvalId());
    }

    @Test
    void tipPresetsParseByHand() {
        long[] bp = PosState.parseBp("1500,,abc,1800, 2000,99999");
        assertEquals(3, bp.length);
        assertEquals(1800, bp[1]);
        assertEquals(0, PosState.parseBp("").length);
    }
}
