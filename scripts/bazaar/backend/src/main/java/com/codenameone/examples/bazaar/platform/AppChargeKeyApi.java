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

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RestController;
import com.codenameone.examples.bazaar.api.AppChargeDto;

/// App billing on the key API, for an app's token only:
/// `POST /api/v1/stores/{storeId}/app-charges` asks the store for a charge
/// (`name`, `kind`, `amount` in minor units, `currency`, `trialDays`,
/// `returnUrl`) and answers it `PENDING` with the `confirmUrl` to send the
/// owner to; `GET` lists the app's own charges in the store.
@RestController
public class AppChargeKeyApi {
    private static final String PATH = "/api/v1/stores/{storeId}/app-charges";

    private final StoreApiKeys keys;
    private final AppBilling billing;

    public AppChargeKeyApi(StoreApiKeys keys, AppBilling billing) {
        this.keys = keys;
        this.billing = billing;
    }

    @PostMapping(PATH)
    public HttpServer.Response create(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        StoreApiKeys.Key k = keys.requireApp(storeId, null);
        AppChargeDto wanted = Json.read(request.getBody() == null ? "{}" : request.getBody(),
                AppChargeDto.class);
        return json(billing.create(k, wanted));
    }

    @GetMapping(PATH)
    public HttpServer.Response list(@PathVariable("storeId") String storeId) throws Exception {
        return json(billing.ofInstallation(keys.requireApp(storeId, null)));
    }

    private static HttpServer.Response json(Object value) {
        return HttpServer.Response.json(200, Json.write(value)).header("Cache-Control", "no-store");
    }
}
