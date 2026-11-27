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
package com.codenameone.examples.bazaar.staff;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.SsoProviderDto;
import com.codenameone.examples.bazaar.api.StaffSecurityApiServer;
import com.codenameone.examples.bazaar.api.StaffSecurityDto;

import java.util.List;

/// The server's half of `StaffSecurityApi`; [StaffSecurity] and
/// [SsoProviders] check the caller.
@Component
public class StaffSecurityEndpoint implements StaffSecurityApiServer {
    private final StaffSecurity security;
    private final SsoProviders sso;

    public StaffSecurityEndpoint(StaffSecurity security, SsoProviders sso) {
        this.security = security;
        this.sso = sso;
    }

    @Override
    public StaffSecurityDto settings(String storeId) throws Exception {
        return security.settings(Caller.name(), storeId);
    }

    @Override
    public StaffSecurityDto save(String storeId, StaffSecurityDto body) throws Exception {
        return security.save(Caller.name(), storeId, body);
    }

    @Override
    public List<SsoProviderDto> providers(String storeId) throws Exception {
        return sso.list(Caller.name(), storeId);
    }

    @Override
    public SsoProviderDto saveProvider(String storeId, SsoProviderDto body) throws Exception {
        return sso.save(Caller.name(), storeId, body);
    }

    @Override
    public List<SsoProviderDto> removeProvider(String storeId, String providerId) throws Exception {
        return sso.remove(Caller.name(), storeId, providerId);
    }
}
