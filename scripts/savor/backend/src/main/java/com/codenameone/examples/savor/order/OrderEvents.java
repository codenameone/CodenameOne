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
import com.codenameone.examples.savor.Ids;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.domain.OrderEvent;
import com.codenameone.examples.savor.repository.OrderEventRepository;
import java.io.IOException;
import java.time.Instant;

/// The history of an order: one row per move, with who made it. Written in
/// the transaction that made the move.
@Component
public class OrderEvents {
    private final OrderEventRepository events;

    public OrderEvents(OrderEventRepository events) {
        this.events = events;
    }

    public void record(String orderId, OrderState state, String actor) throws IOException {
        OrderEvent event = new OrderEvent();
        event.setId(Ids.next());
        event.setOrderId(orderId);
        event.setState(state);
        event.setActor(actor == null ? "" : actor);
        event.setCreatedAt(Instant.now());
        events.insert(event);
    }
}
