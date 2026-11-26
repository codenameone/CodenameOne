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
package com.codenameone.examples.bazaar.trade;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.BulkOrderDto;
import com.codenameone.examples.bazaar.api.BulkOrderResultDto;
import com.codenameone.examples.bazaar.api.CompanyBuyerDto;
import com.codenameone.examples.bazaar.api.CompanyDto;
import com.codenameone.examples.bazaar.api.CompanySiteDto;
import com.codenameone.examples.bazaar.api.NetTermDto;
import com.codenameone.examples.bazaar.api.PriceListDto;
import com.codenameone.examples.bazaar.api.PriceListEntryDto;
import com.codenameone.examples.bazaar.api.QuoteMoveDto;
import com.codenameone.examples.bazaar.api.TradeApiServer;
import com.codenameone.examples.bazaar.api.TradeQuoteDto;
import com.codenameone.examples.bazaar.api.TradeQuoteRequestDto;
import com.codenameone.examples.bazaar.api.VatCheckDto;
import com.codenameone.examples.bazaar.cart.Codes;
import java.util.List;

/// The server's half of `TradeApi`.
@Component
public class TradeEndpoint implements TradeApiServer {
    private final Trade trade;

    public TradeEndpoint(Trade trade) {
        this.trade = trade;
    }

    @Override
    public List<CompanyDto> companies(String storeId) throws Exception {
        return trade.companies(Caller.name(), storeId);
    }

    @Override
    public CompanyDto createCompany(String storeId, CompanyDto body) throws Exception {
        return trade.saveCompany(Caller.name(), storeId, null, body);
    }

    @Override
    public CompanyDto saveCompany(String storeId, String companyId, CompanyDto body)
            throws Exception {
        return trade.saveCompany(Caller.name(), storeId, companyId, body);
    }

    @Override
    public List<CompanySiteDto> sites(String storeId, String companyId) throws Exception {
        return trade.sites(Caller.name(), storeId, companyId);
    }

    @Override
    public List<CompanySiteDto> addSite(String storeId, String companyId, CompanySiteDto body)
            throws Exception {
        return trade.addSite(Caller.name(), storeId, companyId, body);
    }

    @Override
    public List<CompanyBuyerDto> buyers(String storeId, String companyId) throws Exception {
        return trade.buyers(Caller.name(), storeId, companyId);
    }

    @Override
    public List<CompanyBuyerDto> addBuyer(String storeId, String companyId, CompanyBuyerDto body)
            throws Exception {
        return trade.addBuyer(Caller.name(), storeId, companyId, body);
    }

    @Override
    public List<CompanyBuyerDto> removeBuyer(String storeId, String companyId, String buyerId)
            throws Exception {
        return trade.removeBuyer(Caller.name(), storeId, companyId, buyerId);
    }

    @Override
    public List<PriceListDto> priceLists(String storeId) throws Exception {
        return trade.priceLists(Caller.name(), storeId);
    }

    @Override
    public PriceListDto createPriceList(String storeId, PriceListDto body) throws Exception {
        return trade.savePriceList(Caller.name(), storeId, null, body);
    }

    @Override
    public PriceListDto savePriceList(String storeId, String priceListId, PriceListDto body)
            throws Exception {
        return trade.savePriceList(Caller.name(), storeId, priceListId, body);
    }

    @Override
    public List<PriceListEntryDto> entries(String storeId, String priceListId) throws Exception {
        return trade.entries(Caller.name(), storeId, priceListId);
    }

    @Override
    public List<PriceListEntryDto> putEntry(String storeId, String priceListId,
            PriceListEntryDto body) throws Exception {
        return trade.putEntry(Caller.name(), storeId, priceListId, body);
    }

    @Override
    public List<PriceListEntryDto> removeEntry(String storeId, String priceListId,
            String entryId) throws Exception {
        return trade.removeEntry(Caller.name(), storeId, priceListId, entryId);
    }

    @Override
    public List<TradeQuoteDto> quotes(String storeId) throws Exception {
        return trade.quotes(Caller.name(), storeId);
    }

    @Override
    public TradeQuoteDto createQuote(String storeId, TradeQuoteRequestDto body) throws Exception {
        return trade.createQuote(Caller.name(), storeId, body);
    }

    @Override
    public TradeQuoteDto quote(String storeId, String quoteId) throws Exception {
        return trade.quote(Caller.name(), storeId, quoteId);
    }

    @Override
    public TradeQuoteDto moveQuote(String storeId, String quoteId, QuoteMoveDto body)
            throws Exception {
        return trade.moveQuote(Caller.name(), storeId, quoteId, body == null ? null
                : body.status());
    }

    @Override
    public List<NetTermDto> netTerms(String storeId) throws Exception {
        return trade.netTerms(Caller.name(), storeId);
    }

    @Override
    public NetTermDto markNetTermPaid(String storeId, String orderId) throws Exception {
        return trade.markPaid(Caller.name(), storeId, orderId);
    }

    @Override
    public VatCheckDto checkVat(String storeId, VatCheckDto body) throws Exception {
        return trade.checkVat(Caller.name(), storeId, body);
    }

    @Override
    public List<TradeQuoteDto> myQuotes(String storeId) throws Exception {
        return trade.myQuotes(Codes.callerOrNull(), storeId);
    }

    @Override
    public TradeQuoteDto requestQuote(String storeId, TradeQuoteRequestDto body)
            throws Exception {
        return trade.requestQuote(Codes.callerOrNull(), storeId, body);
    }

    @Override
    public TradeQuoteDto moveMyQuote(String storeId, String quoteId, QuoteMoveDto body)
            throws Exception {
        return trade.moveMyQuote(Codes.callerOrNull(), storeId, quoteId, body == null ? null
                : body.status());
    }

    @Override
    public BulkOrderResultDto bulkOrder(String storeId, BulkOrderDto body) throws Exception {
        return trade.bulkOrder(Codes.callerOrNull(), storeId, body == null ? null : body.csv());
    }

    @Override
    public List<NetTermDto> myNetTerms(String storeId) throws Exception {
        return trade.myNetTerms(Codes.callerOrNull(), storeId);
    }
}
