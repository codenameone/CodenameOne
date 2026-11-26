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
package com.codenameone.examples.bazaar.platform;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.AppApiServer;
import com.codenameone.examples.bazaar.api.AppAuthorizeDto;
import com.codenameone.examples.bazaar.api.AppConsentDto;
import com.codenameone.examples.bazaar.api.AppDto;
import com.codenameone.examples.bazaar.api.AppInstallationDto;
import com.codenameone.examples.bazaar.api.AppRedirectDto;

import java.util.List;

/// The server's half of `AppApi`; [Apps] checks the caller.
@Component
public class AppEndpoint implements AppApiServer {
    private final Apps apps;

    public AppEndpoint(Apps apps) {
        this.apps = apps;
    }

    @Override
    public List<AppDto> mine() throws Exception {
        return apps.mine(Caller.name());
    }

    @Override
    public AppDto register(AppDto app) throws Exception {
        return apps.register(Caller.name(), app);
    }

    @Override
    public AppDto update(String appId, AppDto app) throws Exception {
        return apps.update(Caller.name(), appId, app);
    }

    @Override
    public List<AppInstallationDto> installed(String storeId) throws Exception {
        return apps.installed(Caller.name(), storeId);
    }

    @Override
    public AppConsentDto consent(String storeId, AppAuthorizeDto request) throws Exception {
        return apps.consent(Caller.name(), storeId, request);
    }

    @Override
    public AppRedirectDto authorize(String storeId, AppAuthorizeDto request) throws Exception {
        return apps.authorize(Caller.name(), storeId, request);
    }

    @Override
    public List<AppInstallationDto> uninstall(String storeId, String installationId)
            throws Exception {
        return apps.uninstall(Caller.name(), storeId, installationId);
    }
}
