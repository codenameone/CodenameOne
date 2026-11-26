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
package com.codenameone.examples.bazaar.order;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.DraftOrderDto;
import com.codenameone.examples.bazaar.api.OrderApiServer;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderNoteDto;
import com.codenameone.examples.bazaar.api.RefundDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;

import java.util.List;

/// The server's half of `OrderApi`; [Orders] checks the caller's role.
@Component
public class OrderEndpoint implements OrderApiServer {
    private final Orders orders;

    public OrderEndpoint(Orders orders) {
        this.orders = orders;
    }

    @Override
    public List<OrderDto> mine() throws Exception {
        return orders.mine(Caller.name());
    }

    @Override
    public OrderDto mineDetail(String orderId) throws Exception {
        return orders.mineDetail(Caller.name(), orderId);
    }

    @Override
    public OrderDto cancelMine(String orderId) throws Exception {
        return orders.cancelMine(Caller.name(), orderId);
    }

    @Override
    public List<OrderDto> list(String storeId, String status, String financial, String fulfilment,
            String channel, String from, String to, String q) throws Exception {
        return orders.list(Caller.name(), storeId, status, financial, fulfilment, channel, from,
                to, q);
    }

    @Override
    public OrderDto detail(String storeId, String orderId) throws Exception {
        return orders.detail(Caller.name(), storeId, orderId);
    }

    @Override
    public OrderDto note(String storeId, String orderId, OrderNoteDto body) throws Exception {
        return orders.note(Caller.name(), storeId, orderId, body);
    }

    @Override
    public OrderDto cancel(String storeId, String orderId) throws Exception {
        return orders.cancel(Caller.name(), storeId, orderId);
    }

    @Override
    public OrderDto markPaid(String storeId, String orderId) throws Exception {
        return orders.markPaid(Caller.name(), storeId, orderId);
    }

    @Override
    public OrderDto refund(String storeId, String orderId, RefundRequestDto body)
            throws Exception {
        return orders.refund(Caller.name(), storeId, orderId, body);
    }

    @Override
    public List<RefundDto> refunds(String storeId, String orderId) throws Exception {
        return orders.refunds(Caller.name(), storeId, orderId);
    }

    @Override
    public OrderDto createDraft(String storeId, DraftOrderDto body) throws Exception {
        return orders.createDraft(Caller.name(), storeId, body);
    }

    @Override
    public OrderDto sendInvoice(String storeId, String orderId) throws Exception {
        return orders.sendInvoice(Caller.name(), storeId, orderId);
    }

    @Override
    public OrderDto convertDraft(String storeId, String orderId) throws Exception {
        return orders.convertDraft(Caller.name(), storeId, orderId);
    }
}
