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
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Gift cards: issued and managed by OWNER and MANAGER; anyone may look up a
/// card's balance by its code, a few times a minute.
@RestClient
public interface GiftCardApi {
    @GET("/api/merchant/stores/{storeId}/gift-cards")
    void list(@Path("storeId") String storeId, OnComplete<Response<List<GiftCardDto>>> callback);

    /// Issues a card worth `initialCents`; the answer is the only one that
    /// carries the whole code.
    @POST("/api/merchant/stores/{storeId}/gift-cards")
    void issue(@Path("storeId") String storeId, @Body GiftCardDto card,
            OnComplete<Response<GiftCardDto>> callback);

    @POST("/api/merchant/stores/{storeId}/gift-cards/{cardId}/disable")
    void disable(@Path("storeId") String storeId, @Path("cardId") String cardId,
            OnComplete<Response<GiftCardDto>> callback);

    @GET("/api/shop/gift-cards/balance")
    void balance(@Query("code") String code, OnComplete<Response<GiftCardBalanceDto>> callback);
}
