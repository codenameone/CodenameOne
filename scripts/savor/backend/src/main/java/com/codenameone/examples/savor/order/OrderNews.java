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

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.savor.domain.CustomerOrder;
import com.codenameone.examples.savor.domain.Delivery;
import com.codenameone.examples.savor.domain.RestaurantStaff;
import com.codenameone.examples.savor.live.LiveHub;
import com.codenameone.examples.savor.push.OrderPushes;
import com.codenameone.examples.savor.repository.CustomerOrderRepository;
import com.codenameone.examples.savor.repository.DeliveryRepository;
import com.codenameone.examples.savor.repository.RestaurantStaffRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Tells an order's people that it changed: its customer, its kitchen's staff
/// and its courier over the live channel, and by push whoever is not looking.
///
/// Both machines move an order -- [Orders] for what the customer, the kitchen
/// and the admin do, [com.codenameone.examples.savor.delivery.Deliveries] for
/// the pickup, the drop-off and a search that found nobody -- so both tell it
/// through here, once the change has committed.
@Component
public class OrderNews {
    private final CustomerOrderRepository orders;
    private final DeliveryRepository deliveries;
    private final RestaurantStaffRepository staff;
    private final LiveHub live;
    private final OrderPushes pushes;

    public OrderNews(CustomerOrderRepository orders, DeliveryRepository deliveries,
            RestaurantStaffRepository staff, LiveHub live, OrderPushes pushes) {
        this.orders = orders;
        this.deliveries = deliveries;
        this.staff = staff;
        this.live = live;
        this.pushes = pushes;
    }

    /// Tells everyone concerned with order `id` how it now stands. Never
    /// fails the change it is about: a read that fails is logged.
    public void told(String id) {
        Object[] found;
        try {
            found = read(id);
        } catch (IOException | RuntimeException err) {
            System.err.println("[order] nobody was told of order " + id + ": " + err);
            return;
        }
        if (found == null) {
            return;
        }
        CustomerOrder order = (CustomerOrder) found[0];
        @SuppressWarnings("unchecked")
        List<String> kitchen = (List<String>) found[1];
        live.orderChanged(id, order.getState(), order.getCustomer(), kitchen, (String) found[2]);
        // The same moment, and for the same reason: a phone told of a change
        // must find it made.
        pushes.orderChanged(id, order.getState());
    }

    /// `{order, staff, courier}`, or null for an order that is not there.
    @Transactional(readOnly = true)
    private Object[] read(String id) throws IOException {
        CustomerOrder order = orders.findById(id);
        if (order == null) {
            return null;
        }
        List<String> kitchen = new ArrayList<String>();
        List<RestaurantStaff> rows = staff.findByRestaurantId(order.getRestaurantId());
        for (int iter = 0; iter < rows.size(); iter++) {
            kitchen.add(rows.get(iter).getUsername());
        }
        Delivery delivery = deliveries.findById(id);
        String courier = delivery == null || delivery.getCourier() == null ? ""
                : delivery.getCourier();
        return new Object[] {order, kitchen, courier};
    }
}
