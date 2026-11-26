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
package com.codenameone.examples.bazaar.order;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.BulkActionDto;
import com.codenameone.examples.bazaar.api.BulkActionResultDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderSettingsDto;
import com.codenameone.examples.bazaar.api.OrderToolsApiServer;
import com.codenameone.examples.bazaar.api.PayLinkDto;
import com.codenameone.examples.bazaar.api.PayLinkRequestDto;
import com.codenameone.examples.bazaar.api.TemplateDto;
import com.codenameone.examples.bazaar.notify.Templates;
import java.util.List;

/// The server's half of `OrderToolsApi`.
@Component
public class OrderToolsEndpoint implements OrderToolsApiServer {
    private final Orders orders;
    private final BulkOrders bulk;
    private final OrderSettings settings;
    private final PayLinks payLinks;
    private final Templates templates;
    private final com.codenameone.examples.bazaar.checkout.CheckoutSettings checkout;

    public OrderToolsEndpoint(Orders orders, BulkOrders bulk, OrderSettings settings,
            PayLinks payLinks, Templates templates,
            com.codenameone.examples.bazaar.checkout.CheckoutSettings checkout) {
        this.checkout = checkout;
        this.orders = orders;
        this.bulk = bulk;
        this.settings = settings;
        this.payLinks = payLinks;
        this.templates = templates;
    }

    @Override
    public OrderDto archive(String storeId, String orderId) throws Exception {
        return orders.archive(Caller.name(), storeId, orderId, true);
    }

    @Override
    public OrderDto unarchive(String storeId, String orderId) throws Exception {
        return orders.archive(Caller.name(), storeId, orderId, false);
    }

    @Override
    public List<OrderDto> archived(String storeId) throws Exception {
        return orders.archived(Caller.name(), storeId);
    }

    @Override
    public List<BulkActionResultDto> bulk(String storeId, BulkActionDto action) throws Exception {
        return bulk.run(Caller.name(), storeId, action);
    }

    @Override
    public OrderSettingsDto settings(String storeId) throws Exception {
        return settings.read(Caller.name(), storeId);
    }

    @Override
    public OrderSettingsDto saveSettings(String storeId, OrderSettingsDto wanted)
            throws Exception {
        return settings.save(Caller.name(), storeId, wanted);
    }

    @Override
    public PayLinkDto payLink(String storeId, String orderId, PayLinkRequestDto request)
            throws Exception {
        return payLinks.create(Caller.name(), storeId, orderId, request);
    }

    @Override
    public List<TemplateDto> templates(String storeId, String language) throws Exception {
        return templates.list(Caller.name(), storeId, language);
    }

    @Override
    public TemplateDto saveTemplate(String storeId, TemplateDto template) throws Exception {
        return templates.save(Caller.name(), storeId, template);
    }

    @Override
    public TemplateDto resetTemplate(String storeId, TemplateDto template) throws Exception {
        return templates.reset(Caller.name(), storeId, template);
    }

    @Override
    public TemplateDto previewTemplate(String storeId, TemplateDto template) throws Exception {
        return templates.preview(Caller.name(), storeId, template);
    }

    @Override
    public com.codenameone.examples.bazaar.api.CheckoutSettingsDto checkoutSettings(
            String storeId) throws Exception {
        return checkout.read(Caller.name(), storeId);
    }

    @Override
    public com.codenameone.examples.bazaar.api.CheckoutSettingsDto saveCheckoutSettings(
            String storeId, com.codenameone.examples.bazaar.api.CheckoutSettingsDto wanted)
            throws Exception {
        return checkout.save(Caller.name(), storeId, wanted);
    }

    @Override
    public java.util.List<com.codenameone.examples.bazaar.api.PaymentTypeDto> paymentTypes(
            String storeId) throws Exception {
        return checkout.types(Caller.name(), storeId);
    }
}
