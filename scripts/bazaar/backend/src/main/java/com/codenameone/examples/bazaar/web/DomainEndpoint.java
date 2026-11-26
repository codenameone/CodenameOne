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
package com.codenameone.examples.bazaar.web;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.CustomDomainDto;
import com.codenameone.examples.bazaar.api.DomainApiServer;

/// The server's half of `DomainApi`; [DomainVerification] checks the caller.
@Component
public class DomainEndpoint implements DomainApiServer {
    private final DomainVerification domains;

    public DomainEndpoint(DomainVerification domains) {
        this.domains = domains;
    }

    @Override
    public CustomDomainDto domain(String storeId) throws Exception {
        return domains.status(Caller.name(), storeId);
    }

    @Override
    public CustomDomainDto claim(String storeId, CustomDomainDto body) throws Exception {
        return domains.claim(Caller.name(), storeId, body == null ? "" : body.domain());
    }

    @Override
    public CustomDomainDto verify(String storeId) throws Exception {
        return domains.verify(Caller.name(), storeId);
    }
}
