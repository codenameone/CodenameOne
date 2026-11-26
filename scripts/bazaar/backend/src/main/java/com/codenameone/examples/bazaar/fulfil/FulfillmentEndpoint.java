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
package com.codenameone.examples.bazaar.fulfil;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.BoardColumnDto;
import com.codenameone.examples.bazaar.api.BoardMoveDto;
import com.codenameone.examples.bazaar.api.FulfillmentApiServer;
import com.codenameone.examples.bazaar.api.FulfillmentDto;
import com.codenameone.examples.bazaar.api.LabelRequestDto;
import com.codenameone.examples.bazaar.api.NewFulfillmentDto;
import com.codenameone.examples.bazaar.api.PackDto;
import com.codenameone.examples.bazaar.api.RateQuoteDto;
import com.codenameone.examples.bazaar.api.UnfulfilledDto;

import java.util.List;

/// The server's half of `FulfillmentApi`; [Fulfillments] checks the caller.
@Component
public class FulfillmentEndpoint implements FulfillmentApiServer {
    private final Fulfillments f;

    public FulfillmentEndpoint(Fulfillments f) {
        this.f = f;
    }

    @Override
    public List<UnfulfilledDto> queue(String storeId, String locationId) throws Exception {
        return f.queue(Caller.name(), storeId, locationId);
    }

    @Override
    public List<FulfillmentDto> ofOrder(String storeId, String orderId) throws Exception {
        return f.ofOrder(Caller.name(), storeId, orderId);
    }

    @Override
    public FulfillmentDto create(String storeId, String orderId, NewFulfillmentDto body)
            throws Exception {
        return f.create(Caller.name(), storeId, orderId, body);
    }

    @Override
    public FulfillmentDto pack(String storeId, String id, PackDto body) throws Exception {
        return f.pack(Caller.name(), storeId, id, body);
    }

    @Override
    public List<RateQuoteDto> rates(String storeId, String id) throws Exception {
        return f.rates(Caller.name(), storeId, id);
    }

    @Override
    public FulfillmentDto buyLabel(String storeId, String id, LabelRequestDto body)
            throws Exception {
        return f.buyLabel(Caller.name(), storeId, id, body);
    }

    @Override
    public FulfillmentDto ship(String storeId, String id) throws Exception {
        return f.ship(Caller.name(), storeId, id);
    }

    @Override
    public FulfillmentDto pickupReady(String storeId, String id) throws Exception {
        return f.pickupReady(Caller.name(), storeId, id);
    }

    @Override
    public FulfillmentDto pickedUp(String storeId, String id) throws Exception {
        return f.pickedUp(Caller.name(), storeId, id);
    }

    @Override
    public FulfillmentDto delivered(String storeId, String id) throws Exception {
        return f.delivered(Caller.name(), storeId, id);
    }

    @Override
    public FulfillmentDto cancel(String storeId, String id) throws Exception {
        return f.cancel(Caller.name(), storeId, id);
    }

    @Override
    public List<BoardColumnDto> board(String storeId) throws Exception {
        return f.board(Caller.name(), storeId);
    }

    @Override
    public List<BoardColumnDto> move(String storeId, BoardMoveDto body) throws Exception {
        return f.moveCard(Caller.name(), storeId, body);
    }

    @Override
    public List<FulfillmentDto> tracking(String orderId) throws Exception {
        return f.tracking(Caller.name(), orderId);
    }

    @Override
    public FulfillmentDto track(String trackingNumber) throws Exception {
        return f.track(trackingNumber);
    }
}
