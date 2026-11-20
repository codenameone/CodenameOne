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
package com.codenameone.examples.bazaar.admin;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PreAuthorize;
import com.codenameone.examples.bazaar.api.PlatformApiServer;
import com.codenameone.examples.bazaar.api.PlatformStatsDto;
import com.codenameone.examples.bazaar.api.StoreStatsDto;
import com.codenameone.examples.bazaar.api.StoreStatusDto;

/// The server's half of `PlatformApi`; admins only, as all of `/api/admin`.
@Component
@PreAuthorize("hasRole('ADMIN')")
public class PlatformEndpoint implements PlatformApiServer {
    private final Platform platform;

    public PlatformEndpoint(Platform platform) {
        this.platform = platform;
    }

    @Override
    public PlatformStatsDto stats() throws Exception {
        return platform.stats();
    }

    @Override
    public StoreStatsDto setStatus(String storeId, StoreStatusDto body) throws Exception {
        return platform.setStatus(Caller.name(), storeId, body);
    }
}
