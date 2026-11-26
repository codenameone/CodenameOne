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
import com.codenameone.examples.bazaar.api.ReturnOptionsDto;
import com.codenameone.examples.bazaar.api.ReturnQuoteDto;
import com.codenameone.examples.bazaar.api.SelfReturnApiServer;
import com.codenameone.examples.bazaar.api.SelfReturnDto;

/// The server's half of `SelfReturnApi`.
@Component
public class SelfReturnEndpoint implements SelfReturnApiServer {
    private final SelfReturns returns;

    public SelfReturnEndpoint(SelfReturns returns) {
        this.returns = returns;
    }

    @Override
    public ReturnOptionsDto options(String orderId) throws Exception {
        return returns.options(Caller.name(), orderId);
    }

    @Override
    public ReturnQuoteDto quote(String orderId, SelfReturnDto request) throws Exception {
        return returns.quote(Caller.name(), orderId, request);
    }

    @Override
    public ReturnQuoteDto request(String orderId, SelfReturnDto request) throws Exception {
        return returns.request(Caller.name(), orderId, request);
    }
}
