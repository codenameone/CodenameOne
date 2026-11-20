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
package com.codenameone.examples.bazaar.returns;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.ReturnApiServer;
import com.codenameone.examples.bazaar.api.ReturnDto;
import com.codenameone.examples.bazaar.api.ReturnReceiveDto;
import com.codenameone.examples.bazaar.api.ReturnRefundDto;
import com.codenameone.examples.bazaar.api.ReturnRequestDto;

import java.util.List;

/// The server's half of `ReturnApi`; [Returns] checks the caller.
@Component
public class ReturnEndpoint implements ReturnApiServer {
    private final Returns returns;

    public ReturnEndpoint(Returns returns) {
        this.returns = returns;
    }

    @Override
    public ReturnDto request(String orderId, ReturnRequestDto body) throws Exception {
        return returns.request(Caller.name(), orderId, body);
    }

    @Override
    public List<ReturnDto> mine(String orderId) throws Exception {
        return returns.mine(Caller.name(), orderId);
    }

    @Override
    public List<ReturnDto> list(String storeId) throws Exception {
        return returns.list(Caller.name(), storeId);
    }

    @Override
    public ReturnDto approve(String storeId, String id) throws Exception {
        return returns.approve(Caller.name(), storeId, id);
    }

    @Override
    public ReturnDto decline(String storeId, String id) throws Exception {
        return returns.decline(Caller.name(), storeId, id);
    }

    @Override
    public ReturnDto receive(String storeId, String id, ReturnReceiveDto body) throws Exception {
        return returns.receive(Caller.name(), storeId, id, body);
    }

    @Override
    public ReturnDto refund(String storeId, String id, ReturnRefundDto body) throws Exception {
        return returns.refund(Caller.name(), storeId, id, body);
    }
}
