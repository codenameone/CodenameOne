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
package com.codenameone.examples.bazaar.webhook;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.WebhookApiServer;
import com.codenameone.examples.bazaar.api.WebhookDeliveryDto;
import com.codenameone.examples.bazaar.api.WebhookDto;

import java.util.List;

/// The server's half of `WebhookApi`; [Webhooks] checks the caller.
@Component
public class WebhookEndpoint implements WebhookApiServer {
    private final Webhooks webhooks;

    public WebhookEndpoint(Webhooks webhooks) {
        this.webhooks = webhooks;
    }

    @Override
    public List<String> topics(String storeId) throws Exception {
        return Webhooks.TOPICS;
    }

    @Override
    public List<WebhookDto> list(String storeId) throws Exception {
        return webhooks.list(Caller.name(), storeId);
    }

    @Override
    public WebhookDto create(String storeId, WebhookDto webhook) throws Exception {
        return webhooks.create(Caller.name(), storeId, webhook);
    }

    @Override
    public WebhookDto save(String storeId, String webhookId, WebhookDto webhook)
            throws Exception {
        return webhooks.save(Caller.name(), storeId, webhookId, webhook);
    }

    @Override
    public List<WebhookDto> remove(String storeId, String webhookId) throws Exception {
        return webhooks.remove(Caller.name(), storeId, webhookId);
    }

    @Override
    public List<WebhookDeliveryDto> deliveries(String storeId, String webhookId)
            throws Exception {
        return webhooks.deliveries(Caller.name(), storeId, webhookId);
    }

    @Override
    public WebhookDeliveryDto test(String storeId, String webhookId) throws Exception {
        return webhooks.test(Caller.name(), storeId, webhookId);
    }

    @Override
    public WebhookDeliveryDto redeliver(String storeId, String deliveryId) throws Exception {
        return webhooks.redeliver(Caller.name(), storeId, deliveryId);
    }
}
