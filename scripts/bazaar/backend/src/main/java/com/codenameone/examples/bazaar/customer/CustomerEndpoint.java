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
package com.codenameone.examples.bazaar.customer;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.CreditAdjustDto;
import com.codenameone.examples.bazaar.api.CustomerApiServer;
import com.codenameone.examples.bazaar.api.CustomerDetailDto;
import com.codenameone.examples.bazaar.api.CustomerDto;
import com.codenameone.examples.bazaar.api.CustomerEditDto;
import com.codenameone.examples.bazaar.api.ExportDto;
import com.codenameone.examples.bazaar.api.SegmentDto;

import java.util.List;

/// The server's half of `CustomerApi`; [Customers] checks the caller.
@Component
public class CustomerEndpoint implements CustomerApiServer {
    private final Customers customers;
    @com.codename1.backend.annotations.Autowired
    private CustomerData data;

    public CustomerEndpoint(Customers customers) {
        this.customers = customers;
    }

    @Override
    public List<CustomerDto> list(String storeId, String q, String tag, String minSpent,
            String minOrders, String marketing, String segment, String sort) throws Exception {
        Customers.Filter f = new Customers.Filter();
        f.q = q == null ? "" : q;
        f.tag = tag == null ? "" : tag;
        f.minSpent = number(minSpent);
        f.minOrders = (int) Math.min(Integer.MAX_VALUE, number(minOrders));
        f.marketing = marketing == null ? "" : marketing;
        f.segment = segment == null ? "" : segment;
        f.sort = sort == null ? "" : sort;
        return customers.list(Caller.name(), storeId, f);
    }

    static long number(String text) {
        if (text == null || text.trim().length() == 0) {
            return 0;
        }
        try {
            return Math.max(0, Long.parseLong(text.trim()));
        } catch (NumberFormatException bad) {
            throw new ResponseStatusException(400, "Not a number: " + text);
        }
    }

    @Override
    public CustomerDetailDto detail(String storeId, String customerId) throws Exception {
        return customers.detail(Caller.name(), storeId, customerId);
    }

    @Override
    public CustomerDto edit(String storeId, String customerId, CustomerEditDto body)
            throws Exception {
        return customers.edit(Caller.name(), storeId, customerId, body);
    }

    @Override
    public CustomerDto adjustCredit(String storeId, String customerId, CreditAdjustDto body)
            throws Exception {
        return customers.adjustCredit(Caller.name(), storeId, customerId, body);
    }

    @Override
    public List<SegmentDto> segments(String storeId) throws Exception {
        return customers.segments(Caller.name(), storeId);
    }

    @Override
    public ExportDto export(String storeId, String segment) throws Exception {
        return customers.export(Caller.name(), storeId, segment);
    }

    @Override
    public CustomerDto merge(String storeId, String customerId,
            com.codenameone.examples.bazaar.api.CustomerMergeDto body) throws Exception {
        return data.merge(Caller.name(), storeId, customerId, body == null ? null
                : body.duplicateId());
    }

    @Override
    public com.codenameone.examples.bazaar.api.DataExportDto dataExport(String storeId,
            String customerId) throws Exception {
        return data.exportFor(Caller.name(), storeId, customerId);
    }

    @Override
    public CustomerDto erase(String storeId, String customerId) throws Exception {
        return data.erase(Caller.name(), storeId, customerId);
    }

    @Override
    public List<com.codenameone.examples.bazaar.api.DataRequestDto> dataRequests(String storeId)
            throws Exception {
        return data.forStore(Caller.name(), storeId);
    }

    @Override
    public com.codenameone.examples.bazaar.api.DataRequestDto completeRequest(String storeId,
            String requestId) throws Exception {
        return data.complete(Caller.name(), storeId, requestId);
    }
}
