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
package com.codenameone.examples.bazaar.cart;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.events.StoreEvents;
import com.codenameone.examples.bazaar.repository.CartLineRepository;
import com.codenameone.examples.bazaar.repository.CartRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Finds carts left with items in them and fires `cart.abandoned` once per
/// cart. A cart counts when it was untouched for [#IDLE_MILLIS], has lines,
/// and has someone to reach: an email, or a signed-in shopper. The mark is
/// `Cart.reminderSentAt`, set by a conditional update, so two servers
/// sweeping at once fire it once between them.
@Component
public class AbandonedCarts {
    public static final long IDLE_MILLIS = 3600000L;

    private final CartRepository carts;
    private final CartLineRepository lines;
    private final CustomerRepository customers;
    private final StoreEvents events;

    public AbandonedCarts(CartRepository carts, CartLineRepository lines,
            CustomerRepository customers, StoreEvents events) {
        this.carts = carts;
        this.lines = lines;
        this.customers = customers;
        this.events = events;
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 120000, lock = "bazaar-abandoned-carts")
    public void sweep() throws IOException {
        sweepAbandoned(System.currentTimeMillis());
    }

    /// [#sweep] as of `now`; answers how many carts fired. Public so that
    /// tests can run the clock forward.
    public int sweepAbandoned(long now) throws IOException {
        List<Cart> idle = idleRead(now);
        int fired = 0;
        for (int iter = 0; iter < idle.size(); iter++) {
            Cart cart = idle.get(iter);
            if (!claim(cart.getId(), now)) {
                continue;
            }
            events.cartAbandoned(cart, customerRead(cart));
            fired++;
        }
        return fired;
    }

    @Transactional(readOnly = true)
    private List<Cart> idleRead(long now) {
        List<Cart> stale = carts.findByReminderSentAtAndUpdatedAtLessThan(Instant.EPOCH,
                Instant.ofEpochMilli(now - IDLE_MILLIS));
        List<Cart> out = new ArrayList<Cart>();
        for (int iter = 0; iter < stale.size(); iter++) {
            Cart c = stale.get(iter);
            boolean reachable = c.getEmail().length() > 0 || c.getUsername().length() > 0;
            if (reachable && !lines.findByCartId(c.getId()).isEmpty()) {
                out.add(c);
            }
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    private boolean claim(String cartId, long now) throws IOException {
        return carts.claimReminder(cartId, Instant.EPOCH, Instant.ofEpochMilli(now)) == 1;
    }

    @Transactional(readOnly = true)
    private Customer customerRead(Cart cart) {
        return cart.getUsername().length() == 0 ? null
                : customers.findByStoreIdAndUsername(cart.getStoreId(), cart.getUsername());
    }
}
