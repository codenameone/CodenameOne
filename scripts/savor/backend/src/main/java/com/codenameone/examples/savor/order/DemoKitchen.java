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
package com.codenameone.examples.savor.order;

import com.codename1.backend.Config;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.domain.CustomerOrder;
import com.codenameone.examples.savor.repository.CustomerOrderRepository;
import com.codenameone.examples.savor.repository.RestaurantStaffRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

/// The kitchens of the demo restaurants nobody runs, on a development server:
/// it accepts their orders a few seconds after they come in and has them ready
/// half a minute later, so a customer can order anywhere and watch it arrive.
/// A restaurant with staff is left to its staff.
///
/// Off on every other profile, the `test` one included: the tests move orders
/// themselves and must not race a kitchen.
@Component
public class DemoKitchen {
    static final long ACCEPT_AFTER_MILLIS = 8000L;
    static final long READY_AFTER_MILLIS = 30000L;
    static final String ACTOR = "kitchen";

    private final CustomerOrderRepository orders;
    private final RestaurantStaffRepository staff;
    private final Orders kitchen;
    private final boolean on;

    public DemoKitchen(CustomerOrderRepository orders, RestaurantStaffRepository staff,
            Orders kitchen, Config config) {
        this.orders = orders;
        this.staff = staff;
        this.kitchen = kitchen;
        on = config.isDevelopmentProfile() && !"test".equals(config.getProfile());
    }

    @Scheduled(fixedDelay = 3000, initialDelay = 5000, lock = "savor-demo-kitchen")
    public void cook() throws IOException {
        if (!on) {
            return;
        }
        long now = System.currentTimeMillis();
        List<CustomerOrder> waiting = unattended(OrderState.PLACED, Instant.ofEpochMilli(now - ACCEPT_AFTER_MILLIS));
        for (int iter = 0; iter < waiting.size(); iter++) {
            CustomerOrder order = waiting.get(iter);
            try {
                kitchen.accept(ACTOR, order.getRestaurantId(), order.getId(),
                        order.getPrepMinutes() < 1 ? 10 : order.getPrepMinutes());
            } catch (ResponseStatusException moved) {
                // Cancelled or turned down in the meantime.
            }
        }
        List<CustomerOrder> cooking = unattended(OrderState.ACCEPTED, Instant.ofEpochMilli(now - READY_AFTER_MILLIS));
        for (int iter = 0; iter < cooking.size(); iter++) {
            CustomerOrder order = cooking.get(iter);
            try {
                kitchen.ready(ACTOR, order.getRestaurantId(), order.getId());
            } catch (ResponseStatusException moved) {
                // Cancelled by an admin in the meantime.
            }
        }
    }

    /// Orders in `state` since before `before` at restaurants with no staff.
    @Transactional(readOnly = true)
    private List<CustomerOrder> unattended(OrderState state, Instant before) {
        List<CustomerOrder> rows = OrderState.PLACED == state
                ? orders.findByStateAndPlacedAtLessThan(state, before)
                : orders.findByStateAndAcceptedAtLessThan(state, before);
        for (int iter = rows.size() - 1; iter >= 0; iter--) {
            if (!staff.findByRestaurantId(rows.get(iter).getRestaurantId()).isEmpty()) {
                rows.remove(iter);
            }
        }
        return rows;
    }
}
