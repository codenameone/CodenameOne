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

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.WebhookDeliveryDto;
import com.codenameone.examples.bazaar.api.WebhookDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import com.codenameone.examples.bazaar.webhook.Webhooks;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Webhooks: what an order sends, how it is signed, and retrying with
/// backoff until the attempts run out.
@BackendTest
class WebhookTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private DataSource db;
    @Autowired
    private Inventory inventory;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private Webhooks webhooks;

    private static final String SECRET = "0123456789abcdef0123";

    private Shop shop(String name) throws Exception {
        return new Shop(name, 50, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static List<String[]> to(Webhooks.RecordingTransport t, String url) {
        List<String[]> out = new ArrayList<String[]>();
        for (String[] s : t.sent()) {
            if (s[0].equals(url)) {
                out.add(s);
            }
        }
        return out;
    }

    private static String header(String headers, String name) {
        // No String.split on the compiled runtime: walk the lines by hand.
        int at = 0;
        while (at < headers.length()) {
            int end = headers.indexOf('\n', at);
            String line = headers.substring(at, end < 0 ? headers.length() : end);
            if (line.startsWith(name + ": ")) {
                return line.substring(name.length() + 2);
            }
            at = end < 0 ? headers.length() : end + 1;
        }
        return null;
    }

    @Test
    void anOrderIsSignedAndDelivered() throws Exception {
        Webhooks.RecordingTransport t = new Webhooks.RecordingTransport();
        webhooks.useTransport(t);
        Shop s = shop("hook-sign");
        String url = "https://example.com/hook-sign";
        WebhookDto hook = webhooks.create(s.owner, s.storeId, new WebhookDto(null,
                Webhooks.ORDERS_CREATE, url, SECRET, true, 0));
        s.buy(People.customer(accounts, "hook-sign-buyer"), 1);
        webhooks.deliverDue(System.currentTimeMillis());

        List<String[]> sent = to(t, url);
        assertEquals(1, sent.size());
        byte[] body = sent.get(0)[2].getBytes("UTF-8");
        String sig = header(sent.get(0)[1], Webhooks.SIGNATURE_HEADER);
        String ts = header(sent.get(0)[1], Webhooks.TIMESTAMP_HEADER);
        long signedAt = Long.parseLong(ts);
        long now = System.currentTimeMillis() / 1000L;
        assertTrue(Math.abs(now - signedAt) < 60, ts);
        // The timestamp is part of what is signed, not the body alone.
        assertEquals(Webhooks.signature(SECRET, signedAt, body), sig);
        assertFalse(Webhooks.signature(SECRET, signedAt + 1, body).equals(sig));
        assertTrue(sig.startsWith("sha256="));
        assertTrue(Webhooks.verify(SECRET, body, sig, ts, now));
        assertFalse(Webhooks.verify("another-secret-0000", body, sig, ts, now));
        // A replay: the same delivery, accepted once, refused ten minutes on.
        assertFalse(Webhooks.verify(SECRET, body, sig, ts, signedAt + 600));
        // Nor can the timestamp be moved forward to dodge the window.
        assertFalse(Webhooks.verify(SECRET, body, sig, String.valueOf(signedAt + 600),
                signedAt + 600));
        assertFalse(Webhooks.verify(SECRET, body, sig, null, now));
        assertEquals(Webhooks.ORDERS_CREATE, header(sent.get(0)[1], "X-Bazaar-Topic"));
        assertTrue(sent.get(0)[2].indexOf("\"totalCents\"") > 0, sent.get(0)[2]);

        List<WebhookDeliveryDto> log = webhooks.deliveries(s.owner, s.storeId, hook.id());
        assertEquals(1, log.size());
        assertEquals(Webhooks.DELIVERED, log.get(0).status());
        assertEquals(200, log.get(0).responseCode());
    }

    @Test
    void aFailingEndpointIsRetriedWithBackoffSixTimes() throws Exception {
        assertEquals(60000L, Webhooks.backoffMillis(1));
        assertEquals(120000L, Webhooks.backoffMillis(2));
        assertEquals(960000L, Webhooks.backoffMillis(5));

        Webhooks.RecordingTransport t = new Webhooks.RecordingTransport();
        t.answer(500);
        webhooks.useTransport(t);
        Shop s = shop("hook-retry");
        String url = "https://example.com/hook-retry";
        WebhookDto hook = webhooks.create(s.owner, s.storeId, new WebhookDto(null,
                Webhooks.ORDERS_CREATE, url, SECRET, true, 0));
        WebhookDeliveryDto first = webhooks.test(s.owner, s.storeId, hook.id());

        // A test send is tried at once.
        assertEquals(1, first.attempts());
        assertEquals(500, first.responseCode());
        WebhookDeliveryDto d = webhooks.deliveries(s.owner, s.storeId, hook.id()).get(0);
        assertEquals(first.id(), d.id());
        assertEquals(Webhooks.PENDING, d.status());
        long gap = d.nextAttemptAt() - d.createdAt();
        assertTrue(gap >= 59000L && gap <= 61000L + 5000L, "gap " + gap);

        // Not due yet: nothing is sent.
        webhooks.deliverDue(d.nextAttemptAt() - 1);
        assertEquals(1, to(t, url).size());
        long now = d.nextAttemptAt();

        for (int attempt = 2; attempt <= Webhooks.MAX_ATTEMPTS; attempt++) {
            d = webhooks.deliveries(s.owner, s.storeId, hook.id()).get(0);
            now = d.nextAttemptAt();
            webhooks.deliverDue(now);
        }
        d = webhooks.deliveries(s.owner, s.storeId, hook.id()).get(0);
        assertEquals(Webhooks.MAX_ATTEMPTS, d.attempts());
        assertEquals(Webhooks.FAILED, d.status());
        webhooks.deliverDue(now + 365L * 86400000L);
        assertEquals(Webhooks.MAX_ATTEMPTS, to(t, url).size());

        // Sending it again starts over, and a working endpoint takes it.
        t.answer(204);
        webhooks.redeliver(s.owner, s.storeId, d.id());
        List<WebhookDeliveryDto> log = webhooks.deliveries(s.owner, s.storeId, hook.id());
        boolean delivered = false;
        for (WebhookDeliveryDto x : log) {
            delivered |= Webhooks.DELIVERED.equals(x.status());
        }
        assertTrue(delivered, "redelivered: " + log);
    }
}
