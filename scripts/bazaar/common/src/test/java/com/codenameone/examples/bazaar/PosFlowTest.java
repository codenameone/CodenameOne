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

import com.codename1.mapping.Mappers;
import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.api.PosSaleDto;
import com.codenameone.examples.bazaar.api.PosSaleResultDto;
import com.codenameone.examples.bazaar.api.PosTenderDto;
import com.codenameone.examples.bazaar.pos.Outbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The register end to end: the cashier signs in with a PIN, opens a shift, rings
/// a sale while offline -- it waits in the [Outbox] -- syncs it, replays it (the
/// server answers DUPLICATE, never a second order) and closes the shift short.
public class PosFlowTest extends E2e {
    @Override
    protected boolean run() throws Exception {
        String sid = storeId();
        Actor cashier = Actor.signIn(CASHIER);
        String base = "/api/merchant/stores/" + sid + "/pos";
        String register = null;
        String openShift = "";
        for (Object o : cashier.list(base + "/registers")) {
            Map<String, Object> r = (Map<String, Object>) o;
            if ("true".equals(String.valueOf(r.get("active")))) {
                register = Actor.text(r, "id");
                openShift = Actor.text(r, "openShiftId");
                break;
            }
        }
        assertNotNull(register, "the store has a register");

        Map<String, Object> session = cashier.post(base + "/registers/" + register + "/sign-in",
                "{\"pin\":\"1234\"}");
        assertEqual(200, cashier.status, "the PIN signs the cashier in");
        cashier.post(base + "/registers/" + register + "/sign-in", "{\"pin\":\"0000\"}");
        assertTrue(cashier.status >= 400, "a wrong PIN is refused");
        if (openShift.length() > 0) {
            // A shift an earlier run left open: closed, so this one starts clean.
            cashier.post(base + "/shifts/" + openShift + "/close", "{\"countedCents\":0,\"note\":\"e2e\"}");
        }
        Map<String, Object> shift = cashier.post(base + "/registers/" + register + "/shifts",
                "{\"openingCents\":10000,\"note\":\"e2e\"}");
        String shiftId = Actor.text(shift, "id");
        assertTrue(shiftId.length() > 0, "the shift opens (" + cashier.status + ")");

        Map<String, Object> catalog = cashier.get(base + "/registers/" + register + "/catalog");
        Map<String, Object> item = null;
        for (Object o : Actor.maps(catalog.get("items"))) {
            item = (Map<String, Object>) o;
            break;
        }
        assertNotNull(item, "the register has a catalog");
        long price = Actor.num(item, "priceCents");

        // Offline: the sale is only in the outbox.
        Outbox outbox = new Outbox(new MemoryPersist(), new Outbox.Codec() {
            @Override
            public String encode(PosSaleDto sale) {
                return Mappers.toJson(sale);
            }

            @Override
            public PosSaleDto decode(String text) {
                return Mappers.fromJson(text, PosSaleDto.class);
            }
        });
        List<PosLineDto> lines = new ArrayList<PosLineDto>();
        lines.add(new PosLineDto(Actor.text(item, "variantId"), 1, price));
        List<PosTenderDto> tenders = new ArrayList<PosTenderDto>();
        tenders.add(new PosTenderDto("CASH", price * 2 + 1000, ""));
        PosSaleDto sale = new PosSaleDto("e2e-" + System.currentTimeMillis(), shiftId, null, null,
                null, "", System.currentTimeMillis(), lines, tenders);
        outbox.add(sale);
        assertEqual(1, outbox.pending(), "the offline sale waits in the outbox");

        // Back online: the outbox syncs it.
        long now = System.currentTimeMillis();
        List<Outbox.Entry> batch = outbox.due(now);
        assertEqual(1, batch.size());
        outbox.sending(batch);
        outbox.answered(batch, sync(cashier, base, register, sale), now);
        assertEqual(Outbox.SYNCED, outbox.find(sale.clientId()).state, "the sale syncs");
        assertEqual(0, outbox.pending());

        // Replayed, as a register that lost the answer would: no second order.
        List<PosSaleResultDto> again = sync(cashier, base, register, sale);
        assertEqual(Outbox.DUPLICATE, again.isEmpty() ? "" : again.get(0).status(),
                "a replayed sale is a duplicate");

        // Close the shift five dollars short of what the drawer should hold.
        shift = cashier.get(base + "/shifts/" + shiftId);
        long expected = Actor.num(shift, "expectedCents");
        assertTrue(expected > 10000, "the cash sale is in the drawer");
        shift = cashier.post(base + "/shifts/" + shiftId + "/close",
                "{\"countedCents\":" + (expected - 500) + ",\"note\":\"e2e\"}");
        assertEqual("CLOSED", Actor.text(shift, "status"));
        assertEqual(-500L, Actor.num(shift, "varianceCents"), "the variance is what was missing");
        return true;
    }

    private static List<PosSaleResultDto> sync(Actor cashier, String base, String register,
            PosSaleDto sale) {
        Map<String, Object> answer = cashier.post(base + "/registers/" + register + "/sales",
                "{\"sales\":[" + Mappers.toJson(sale) + "]}");
        List<PosSaleResultDto> out = new ArrayList<PosSaleResultDto>();
        for (Object o : Actor.maps(answer.get("results"))) {
            Map<String, Object> r = (Map<String, Object>) o;
            out.add(new PosSaleResultDto(Actor.text(r, "clientId"), Actor.text(r, "status"),
                    Actor.text(r, "orderId"), (int) Actor.num(r, "number"),
                    Actor.num(r, "totalCents"), Actor.num(r, "changeCents"),
                    "true".equals(String.valueOf(r.get("oversold"))), Actor.text(r, "message")));
        }
        return out;
    }

    private static final class MemoryPersist implements Outbox.Persist {
        private final Map<String, String> kept = new HashMap<String, String>();

        @Override
        public String read(String key) {
            return kept.get(key);
        }

        @Override
        public void write(String key, String value) {
            kept.put(key, value);
        }
    }
}
