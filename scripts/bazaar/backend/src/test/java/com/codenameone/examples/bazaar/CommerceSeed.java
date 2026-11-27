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
package com.codenameone.examples.bazaar;

import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.CompanyBuyerDto;
import com.codenameone.examples.bazaar.api.CompanyDto;
import com.codenameone.examples.bazaar.api.CompanySiteDto;
import com.codenameone.examples.bazaar.api.PriceListDto;
import com.codenameone.examples.bazaar.api.PriceListEntryDto;
import com.codenameone.examples.bazaar.api.SellingPlanDto;
import com.codenameone.examples.bazaar.api.SubscribeDto;
import com.codenameone.examples.bazaar.api.TradeQuoteDto;
import com.codenameone.examples.bazaar.api.TradeQuoteRequestDto;
import com.codenameone.examples.bazaar.subscribe.Subscriptions;
import com.codenameone.examples.bazaar.trade.Trade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Rows of every subscription and B2B table in one store: a selling plan, a
/// subscription, a price list with an entry, a company on net 30 with a
/// site and a buyer, and a quote ordered on terms.
final class CommerceSeed {
    final SellingPlanDto plan;
    final PriceListDto list;
    final CompanyDto company;
    final String buyer;
    final String subscriber;
    final TradeQuoteDto ordered;

    CommerceSeed(Shop s, String prefix, Accounts accounts, MockMvc mvc,
            Subscriptions subscriptions, Trade trade) throws Exception {
        plan = subscriptions.writePlan(s.storeId, null, new SellingPlanDto(null, s.productId,
                "Monthly", "MONTH", 1, 1000, true));
        subscriber = People.customer(accounts, prefix + "-subscriber");
        Map card = Rest.addCard(mvc, subscriber, Rest.VISA);
        subscriptions.subscribe(subscriber, s.storeId, new SubscribeDto(plan.id(), s.variantId, 1,
                (String) card.get("id"), Shop.NY));
        list = trade.writePriceList(s.storeId, null, new PriceListDto(null, "Trade", 1000));
        trade.writeEntry(s.storeId, list.id(), new PriceListEntryDto(null, s.variantId, 0, 0, 0,
                0));
        company = trade.writeCompany(s.storeId, null, new CompanyDto(null, prefix + " GmbH",
                "DE123456789", 30, list.id()));
        trade.addSite(s.owner, s.storeId, company.id(), new CompanySiteDto(null, "HQ", "1 Hafen",
                "Hamburg", "20457", "DE"));
        buyer = People.customer(accounts, prefix + "-buyer");
        trade.addBuyer(s.owner, s.storeId, company.id(), new CompanyBuyerDto(null, buyer, ""));
        ordered = order(s, trade, buyer, 1);
    }

    /// A quote for `qty` mugs asked for by `buyer`, sent, accepted and ordered.
    static TradeQuoteDto order(Shop s, Trade trade, String buyer, int qty) throws Exception {
        TradeQuoteDto q = trade.requestQuote(buyer, s.storeId, request(s.variantId, qty));
        trade.moveQuote(s.owner, s.storeId, q.id(), "SENT");
        trade.moveMyQuote(buyer, s.storeId, q.id(), "ACCEPTED");
        return trade.moveMyQuote(buyer, s.storeId, q.id(), "ORDERED");
    }

    static TradeQuoteRequestDto request(String variantId, int qty) {
        List<CartLineChangeDto> lines = new ArrayList<CartLineChangeDto>();
        lines.add(new CartLineChangeDto(variantId, qty));
        return new TradeQuoteRequestDto(null, "", lines);
    }
}
