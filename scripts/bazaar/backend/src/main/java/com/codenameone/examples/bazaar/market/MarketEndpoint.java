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
package com.codenameone.examples.bazaar.market;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.CustomsCodeDto;
import com.codenameone.examples.bazaar.api.DutyRateDto;
import com.codenameone.examples.bazaar.api.FxRateDto;
import com.codenameone.examples.bazaar.api.MarketApiServer;
import com.codenameone.examples.bazaar.api.MarketDto;
import com.codenameone.examples.bazaar.api.MarketPriceDto;
import com.codenameone.examples.bazaar.api.TranslationDto;
import java.util.List;

/// The server's half of `MarketApi`.
@Component
public class MarketEndpoint implements MarketApiServer {
    private final Markets markets;
    private final Translations translations;

    public MarketEndpoint(Markets markets, Translations translations) {
        this.markets = markets;
        this.translations = translations;
    }

    @Override
    public List<MarketDto> markets(String storeId) throws Exception {
        return markets.list(Caller.name(), storeId);
    }

    @Override
    public MarketDto createMarket(String storeId, MarketDto body) throws Exception {
        return markets.save(Caller.name(), storeId, null, body);
    }

    @Override
    public MarketDto saveMarket(String storeId, String marketId, MarketDto body) throws Exception {
        return markets.save(Caller.name(), storeId, marketId, body);
    }

    @Override
    public List<MarketDto> removeMarket(String storeId, String marketId) throws Exception {
        markets.remove(Caller.name(), storeId, marketId);
        return markets.list(Caller.name(), storeId);
    }

    @Override
    public List<MarketPriceDto> marketPrices(String storeId, String marketId) throws Exception {
        return markets.prices(Caller.name(), storeId, marketId);
    }

    @Override
    public List<MarketPriceDto> putMarketPrice(String storeId, String marketId, MarketPriceDto body) throws Exception {
        return markets.putPrice(Caller.name(), storeId, marketId, body);
    }

    @Override
    public List<FxRateDto> fxRates(String storeId) throws Exception {
        return markets.rates(Caller.name(), storeId);
    }

    @Override
    public List<FxRateDto> putFxRate(String storeId, FxRateDto body) throws Exception {
        return markets.putRate(Caller.name(), storeId, body);
    }

    @Override
    public List<DutyRateDto> dutyRates(String storeId) throws Exception {
        return markets.duties(Caller.name(), storeId);
    }

    @Override
    public List<DutyRateDto> addDutyRate(String storeId, DutyRateDto body) throws Exception {
        return markets.addDuty(Caller.name(), storeId, body);
    }

    @Override
    public List<DutyRateDto> removeDutyRate(String storeId, String dutyId) throws Exception {
        return markets.removeDuty(Caller.name(), storeId, dutyId);
    }

    @Override
    public List<CustomsCodeDto> customsCodes(String storeId) throws Exception {
        return markets.customsCodes(Caller.name(), storeId);
    }

    @Override
    public List<CustomsCodeDto> putCustomsCode(String storeId, CustomsCodeDto body) throws Exception {
        return markets.putCustomsCode(Caller.name(), storeId, body);
    }

    @Override
    public List<TranslationDto> translations(String storeId, String kind, String refId) throws Exception {
        return translations.of(Caller.name(), storeId, kind, refId);
    }

    @Override
    public List<com.codenameone.examples.bazaar.api.TranslationFieldDto> translationSheet(
            String storeId, String kind, String refId, String locale) throws Exception {
        return translations.sheet(Caller.name(), storeId, kind, refId, locale);
    }

    @Override
    public List<TranslationDto> putTranslation(String storeId, TranslationDto body) throws Exception {
        return translations.put(Caller.name(), storeId, body);
    }
}
