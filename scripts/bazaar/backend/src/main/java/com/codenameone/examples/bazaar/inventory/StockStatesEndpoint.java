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
package com.codenameone.examples.bazaar.inventory;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.InventoryStateDto;
import com.codenameone.examples.bazaar.api.LotDto;
import com.codenameone.examples.bazaar.api.SerialAssignDto;
import com.codenameone.examples.bazaar.api.SerialDto;
import com.codenameone.examples.bazaar.api.StockStatesApiServer;
import com.codenameone.examples.bazaar.api.TrackedReceiptDto;
import com.codenameone.examples.bazaar.api.TrackingDto;
import com.codenameone.examples.bazaar.api.UnavailableDto;
import java.util.List;

/// The server's half of `StockStatesApi`.
@Component
public class StockStatesEndpoint implements StockStatesApiServer {
    private final InventoryStates states;
    private final Tracking tracking;

    public StockStatesEndpoint(InventoryStates states, Tracking tracking) {
        this.states = states;
        this.tracking = tracking;
    }

    @Override
    public List<InventoryStateDto> states(String storeId, String locationId) throws Exception {
        return states.states(Caller.name(), storeId, locationId);
    }

    @Override
    public InventoryStateDto unavailable(String storeId, UnavailableDto move) throws Exception {
        return states.move(Caller.name(), storeId, move);
    }

    @Override
    public List<TrackingDto> tracking(String storeId) throws Exception {
        return tracking.tracked(Caller.name(), storeId);
    }

    @Override
    public TrackingDto setTracking(String storeId, TrackingDto mode) throws Exception {
        return tracking.setMode(Caller.name(), storeId, mode);
    }

    @Override
    public List<SerialDto> receive(String storeId, TrackedReceiptDto receipt) throws Exception {
        return tracking.receive(Caller.name(), storeId, receipt);
    }

    @Override
    public List<SerialDto> serials(String storeId, String q) throws Exception {
        return tracking.find(Caller.name(), storeId, q);
    }

    @Override
    public List<LotDto> lots(String storeId, String variantId) throws Exception {
        return tracking.lots(Caller.name(), storeId, variantId);
    }

    @Override
    public List<SerialDto> assignSerials(String storeId, String orderId, SerialAssignDto serials)
            throws Exception {
        return tracking.assign(Caller.name(), storeId, orderId, serials);
    }
}
