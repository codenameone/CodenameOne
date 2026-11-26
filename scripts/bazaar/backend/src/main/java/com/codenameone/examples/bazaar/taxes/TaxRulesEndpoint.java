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
import com.codenameone.examples.bazaar.api.OssRowDto;
import com.codenameone.examples.bazaar.api.TaxCategoryDto;
import com.codenameone.examples.bazaar.api.TaxOverrideDto;
import com.codenameone.examples.bazaar.api.TaxRulesApiServer;
import com.codenameone.examples.bazaar.api.TaxSettingsDto;
import com.codenameone.examples.bazaar.api.VatRateDto;

import java.util.List;

/// The server's half of `TaxRulesApi`; [TaxRules] checks the caller, except
/// for the EU table's administration, which the API chain keeps to admins.
@Component
public class TaxRulesEndpoint implements TaxRulesApiServer {
    private final TaxRules rules;

    public TaxRulesEndpoint(TaxRules rules) {
        this.rules = rules;
    }

    @Override
    public TaxSettingsDto settings(String storeId) throws Exception {
        return rules.settings(Caller.name(), storeId);
    }

    @Override
    public TaxSettingsDto saveSettings(String storeId, TaxSettingsDto body) throws Exception {
        return rules.saveSettings(Caller.name(), storeId, body);
    }

    @Override
    public List<TaxOverrideDto> overrides(String storeId) throws Exception {
        return rules.overrides(Caller.name(), storeId);
    }

    @Override
    public TaxOverrideDto saveOverride(String storeId, TaxOverrideDto body) throws Exception {
        return rules.saveOverride(Caller.name(), storeId, body);
    }

    @Override
    public List<TaxOverrideDto> removeOverride(String storeId, String overrideId)
            throws Exception {
        return rules.removeOverride(Caller.name(), storeId, overrideId);
    }

    @Override
    public TaxCategoryDto setCategory(String storeId, String productId, TaxCategoryDto body)
            throws Exception {
        return rules.setCategory(Caller.name(), storeId, productId, body);
    }

    @Override
    public List<VatRateDto> vatRates(String storeId) throws Exception {
        return rules.vatRates(Caller.name(), storeId);
    }

    @Override
    public List<OssRowDto> ossReport(String storeId, String from, String to) throws Exception {
        return rules.ossReport(Caller.name(), storeId, OssReportCsv.millis(from, 0),
                OssReportCsv.millis(to, Long.MAX_VALUE));
    }

    @Override
    public VatRateDto saveVatRate(VatRateDto body) throws Exception {
        return rules.saveVatRate(body);
    }
}
