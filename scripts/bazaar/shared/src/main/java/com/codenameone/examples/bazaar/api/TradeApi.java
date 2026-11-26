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
package com.codenameone.examples.bazaar.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// B2B: companies, their sites and buyers, price lists with quantity rules,
/// quotes moving through a pipeline, net-terms orders and the buyer's side of it.
@RestClient
public interface TradeApi {
    @GET("/api/merchant/stores/{storeId}/companies")
    void companies(@Path("storeId") String storeId,
            OnComplete<Response<List<CompanyDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/companies")
    void createCompany(@Path("storeId") String storeId,
            @Body CompanyDto body,
            OnComplete<Response<CompanyDto>> callback);

    @POST("/api/merchant/stores/{storeId}/companies/{companyId}")
    void saveCompany(@Path("storeId") String storeId,
            @Path("companyId") String companyId,
            @Body CompanyDto body,
            OnComplete<Response<CompanyDto>> callback);

    @GET("/api/merchant/stores/{storeId}/companies/{companyId}/sites")
    void sites(@Path("storeId") String storeId,
            @Path("companyId") String companyId,
            OnComplete<Response<List<CompanySiteDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/companies/{companyId}/sites")
    void addSite(@Path("storeId") String storeId,
            @Path("companyId") String companyId,
            @Body CompanySiteDto body,
            OnComplete<Response<List<CompanySiteDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/companies/{companyId}/buyers")
    void buyers(@Path("storeId") String storeId,
            @Path("companyId") String companyId,
            OnComplete<Response<List<CompanyBuyerDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/companies/{companyId}/buyers")
    void addBuyer(@Path("storeId") String storeId,
            @Path("companyId") String companyId,
            @Body CompanyBuyerDto body,
            OnComplete<Response<List<CompanyBuyerDto>>> callback);

    @DELETE("/api/merchant/stores/{storeId}/companies/{companyId}/buyers/{buyerId}")
    void removeBuyer(@Path("storeId") String storeId,
            @Path("companyId") String companyId,
            @Path("buyerId") String buyerId,
            OnComplete<Response<List<CompanyBuyerDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/price-lists")
    void priceLists(@Path("storeId") String storeId,
            OnComplete<Response<List<PriceListDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/price-lists")
    void createPriceList(@Path("storeId") String storeId,
            @Body PriceListDto body,
            OnComplete<Response<PriceListDto>> callback);

    @POST("/api/merchant/stores/{storeId}/price-lists/{priceListId}")
    void savePriceList(@Path("storeId") String storeId,
            @Path("priceListId") String priceListId,
            @Body PriceListDto body,
            OnComplete<Response<PriceListDto>> callback);

    @GET("/api/merchant/stores/{storeId}/price-lists/{priceListId}/entries")
    void entries(@Path("storeId") String storeId,
            @Path("priceListId") String priceListId,
            OnComplete<Response<List<PriceListEntryDto>>> callback);

    /// Sets a variant's entry, replacing any it had.
    @POST("/api/merchant/stores/{storeId}/price-lists/{priceListId}/entries")
    void putEntry(@Path("storeId") String storeId,
            @Path("priceListId") String priceListId,
            @Body PriceListEntryDto body,
            OnComplete<Response<List<PriceListEntryDto>>> callback);

    @DELETE("/api/merchant/stores/{storeId}/price-lists/{priceListId}/entries/{entryId}")
    void removeEntry(@Path("storeId") String storeId,
            @Path("priceListId") String priceListId,
            @Path("entryId") String entryId,
            OnComplete<Response<List<PriceListEntryDto>>> callback);

    /// Every quote, newest first: the columns of the quote board.
    @GET("/api/merchant/stores/{storeId}/quotes")
    void quotes(@Path("storeId") String storeId,
            OnComplete<Response<List<TradeQuoteDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/quotes")
    void createQuote(@Path("storeId") String storeId,
            @Body TradeQuoteRequestDto body,
            OnComplete<Response<TradeQuoteDto>> callback);

    @GET("/api/merchant/stores/{storeId}/quotes/{quoteId}")
    void quote(@Path("storeId") String storeId,
            @Path("quoteId") String quoteId,
            OnComplete<Response<TradeQuoteDto>> callback);

    /// Moves a quote along the pipeline; 409 for a move the pipeline does not allow.
    @POST("/api/merchant/stores/{storeId}/quotes/{quoteId}/move")
    void moveQuote(@Path("storeId") String storeId,
            @Path("quoteId") String quoteId,
            @Body QuoteMoveDto body,
            OnComplete<Response<TradeQuoteDto>> callback);

    @GET("/api/merchant/stores/{storeId}/net-terms")
    void netTerms(@Path("storeId") String storeId,
            OnComplete<Response<List<NetTermDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/net-terms/{orderId}/paid")
    void markNetTermPaid(@Path("storeId") String storeId,
            @Path("orderId") String orderId,
            OnComplete<Response<NetTermDto>> callback);

    /// Checks a VAT id's format; only `vatId` of the body is read.
    @POST("/api/merchant/stores/{storeId}/vat-check")
    void checkVat(@Path("storeId") String storeId,
            @Body VatCheckDto body,
            OnComplete<Response<VatCheckDto>> callback);

    @GET("/api/shop/stores/{storeId}/b2b/quotes")
    void myQuotes(@Path("storeId") String storeId,
            OnComplete<Response<List<TradeQuoteDto>>> callback);

    @POST("/api/shop/stores/{storeId}/b2b/quotes")
    void requestQuote(@Path("storeId") String storeId,
            @Body TradeQuoteRequestDto body,
            OnComplete<Response<TradeQuoteDto>> callback);

    /// Accepts or declines a sent quote, or orders an accepted one.
    @POST("/api/shop/stores/{storeId}/b2b/quotes/{quoteId}/move")
    void moveMyQuote(@Path("storeId") String storeId,
            @Path("quoteId") String quoteId,
            @Body QuoteMoveDto body,
            OnComplete<Response<TradeQuoteDto>> callback);

    @POST("/api/shop/stores/{storeId}/b2b/bulk-order")
    void bulkOrder(@Path("storeId") String storeId,
            @Body BulkOrderDto body,
            OnComplete<Response<BulkOrderResultDto>> callback);

    @GET("/api/shop/stores/{storeId}/b2b/net-terms")
    void myNetTerms(@Path("storeId") String storeId,
            OnComplete<Response<List<NetTermDto>>> callback);
}
