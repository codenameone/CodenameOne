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
import com.codenameone.examples.bazaar.api.OrderEditApiServer;
import com.codenameone.examples.bazaar.api.OrderEditDto;
import com.codenameone.examples.bazaar.api.OrderEditEntryDto;
import com.codenameone.examples.bazaar.api.OrderEditResultDto;
import java.util.List;

/// The server's half of `OrderEditApi`.
@Component
public class OrderEditEndpoint implements OrderEditApiServer {
    private final OrderEdits edits;

    public OrderEditEndpoint(OrderEdits edits) {
        this.edits = edits;
    }

    @Override
    public OrderEditResultDto edit(String storeId, String orderId, OrderEditDto body)
            throws Exception {
        return edits.edit(Caller.name(), storeId, orderId, body);
    }

    @Override
    public List<OrderEditEntryDto> edits(String storeId, String orderId) throws Exception {
        return edits.edits(Caller.name(), storeId, orderId);
    }
}
