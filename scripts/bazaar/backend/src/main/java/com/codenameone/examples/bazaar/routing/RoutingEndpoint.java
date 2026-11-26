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
package com.codenameone.examples.bazaar.routing;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.FulfilmentServiceOrderDto;
import com.codenameone.examples.bazaar.api.RoutingApiServer;
import com.codenameone.examples.bazaar.api.RoutingDto;
import com.codenameone.examples.bazaar.api.RoutingRuleDto;

import java.util.List;

/// The server's half of `RoutingApi`.
@Component
public class RoutingEndpoint implements RoutingApiServer {
    private final OrderRouting routing;

    public RoutingEndpoint(OrderRouting routing) {
        this.routing = routing;
    }

    @Override
    public RoutingDto routing(String storeId) throws Exception {
        return routing.routing(Caller.name(), storeId);
    }

    @Override
    public RoutingDto saveRouting(String storeId, RoutingDto body) throws Exception {
        return routing.save(Caller.name(), storeId, body);
    }

    @Override
    public RoutingRuleDto saveRule(String storeId, RoutingRuleDto body) throws Exception {
        return routing.saveRule(Caller.name(), storeId, body);
    }

    @Override
    public RoutingRuleDto deleteRule(String storeId, String ruleId) throws Exception {
        return routing.deleteRule(Caller.name(), storeId, ruleId);
    }

    @Override
    public List<FulfilmentServiceOrderDto> serviceOrders(String storeId) throws Exception {
        return routing.serviceOrders(Caller.name(), storeId);
    }
}
