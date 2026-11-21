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
package com.codenameone.examples.bazaar.taxes;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.TaxExemptionApiServer;
import com.codenameone.examples.bazaar.api.TaxExemptionDto;
import com.codenameone.examples.bazaar.api.TaxReportRowDto;
import java.util.List;

/// The server's half of `TaxExemptionApi`.
@Component
public class TaxExemptionEndpoint implements TaxExemptionApiServer {
    private final TaxExemptions taxes;

    public TaxExemptionEndpoint(TaxExemptions taxes) {
        this.taxes = taxes;
    }

    @Override
    public List<TaxExemptionDto> taxExemptions(String storeId) throws Exception {
        return taxes.list(Caller.name(), storeId);
    }

    @Override
    public TaxExemptionDto addTaxExemption(String storeId, TaxExemptionDto body) throws Exception {
        return taxes.add(Caller.name(), storeId, body);
    }

    @Override
    public List<TaxExemptionDto> removeTaxExemption(String storeId, String exemptionId) throws Exception {
        taxes.remove(Caller.name(), storeId, exemptionId);
        return taxes.list(Caller.name(), storeId);
    }

    @Override
    public List<TaxReportRowDto> taxReport(String storeId, String from, String to) throws Exception {
        return taxes.report(Caller.name(), storeId, from, to);
    }
}
