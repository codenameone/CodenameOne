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
package com.codenameone.examples.bazaar.customer;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.DataExportDto;
import com.codenameone.examples.bazaar.api.DataRequestDto;
import com.codenameone.examples.bazaar.api.DataRequestKindDto;
import com.codenameone.examples.bazaar.api.ShopperDataApiServer;
import com.codenameone.examples.bazaar.cart.Codes;

import java.util.List;

/// The server's half of `ShopperDataApi`: the signed-in shopper's own data.
@Component
public class ShopperDataEndpoint implements ShopperDataApiServer {
    private final CustomerData data;

    public ShopperDataEndpoint(CustomerData data) {
        this.data = data;
    }

    @Override
    public DataExportDto export() throws Exception {
        return data.exportMine(Codes.callerOrNull());
    }

    @Override
    public List<DataRequestDto> requests() throws Exception {
        return data.mine(Codes.callerOrNull());
    }

    @Override
    public DataRequestDto request(String storeId, DataRequestKindDto kind) throws Exception {
        return data.request(Codes.callerOrNull(), storeId, kind == null ? null : kind.kind());
    }
}
