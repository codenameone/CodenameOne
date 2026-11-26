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
package com.codenameone.examples.bazaar.pos;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.LayawayApiServer;
import com.codenameone.examples.bazaar.api.LayawayCancelDto;
import com.codenameone.examples.bazaar.api.LayawayDto;
import com.codenameone.examples.bazaar.api.LayawayRequestDto;
import com.codenameone.examples.bazaar.api.PosTenderDto;
import java.util.List;

/// The server's half of `LayawayApi`.
@Component
public class LayawayEndpoint implements LayawayApiServer {
    private final Layaways layaways;

    public LayawayEndpoint(Layaways layaways) {
        this.layaways = layaways;
    }

    @Override
    public LayawayDto create(String storeId, String registerId, LayawayRequestDto body)
            throws Exception {
        return layaways.create(Caller.name(), storeId, registerId, body);
    }

    @Override
    public List<LayawayDto> layaways(String storeId) throws Exception {
        return layaways.layaways(Caller.name(), storeId);
    }

    @Override
    public LayawayDto layaway(String storeId, String layawayId) throws Exception {
        return layaways.layaway(Caller.name(), storeId, layawayId);
    }

    @Override
    public LayawayDto pay(String storeId, String layawayId, PosTenderDto body) throws Exception {
        return layaways.pay(Caller.name(), storeId, layawayId, body);
    }

    @Override
    public LayawayDto cancel(String storeId, String layawayId, LayawayCancelDto body)
            throws Exception {
        return layaways.cancel(Caller.name(), storeId, layawayId, body);
    }
}
