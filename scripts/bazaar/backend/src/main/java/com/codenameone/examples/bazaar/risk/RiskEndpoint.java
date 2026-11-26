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
package com.codenameone.examples.bazaar.risk;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.FraudRuleDto;
import com.codenameone.examples.bazaar.api.OrderRiskDto;
import com.codenameone.examples.bazaar.api.RiskApiServer;
import java.util.List;

/// The server's half of `RiskApi`.
@Component
public class RiskEndpoint implements RiskApiServer {
    private final Fraud fraud;

    public RiskEndpoint(Fraud fraud) {
        this.fraud = fraud;
    }

    @Override
    public List<OrderRiskDto> orderRisks(String storeId, String held) throws Exception {
        return fraud.list(Caller.name(), storeId, "true".equals(held));
    }

    @Override
    public OrderRiskDto orderRisk(String storeId, String orderId) throws Exception {
        return fraud.detail(Caller.name(), storeId, orderId);
    }

    @Override
    public OrderRiskDto approveOrderRisk(String storeId, String orderId) throws Exception {
        return fraud.approve(Caller.name(), storeId, orderId);
    }

    @Override
    public OrderRiskDto cancelOrderRisk(String storeId, String orderId) throws Exception {
        return fraud.cancel(Caller.name(), storeId, orderId);
    }

    @Override
    public List<FraudRuleDto> fraudRules(String storeId) throws Exception {
        return fraud.rules(Caller.name(), storeId);
    }

    @Override
    public List<FraudRuleDto> addFraudRule(String storeId, FraudRuleDto rule) throws Exception {
        return fraud.addRule(Caller.name(), storeId, rule);
    }

    @Override
    public List<FraudRuleDto> removeFraudRule(String storeId, String ruleId) throws Exception {
        return fraud.removeRule(Caller.name(), storeId, ruleId);
    }
}
