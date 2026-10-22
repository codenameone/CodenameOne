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
package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// The wallet, the shop, store purchases (verified on the server) and rewarded ads.
@RestClient
public interface ShopApi {
    @GET("/api/shop")
    void items(OnComplete<Response<List<ShopItemDto>>> callback);

    @GET("/api/wallet")
    void wallet(OnComplete<Response<WalletDto>> callback);

    @POST("/api/shop/buy")
    void buy(@Body BuyDto item, OnComplete<Response<WalletDto>> callback);

    @POST("/api/shop/equip")
    void equip(@Body EquipDto arena, OnComplete<Response<WalletDto>> callback);

    @POST("/api/shop/receipt")
    void receipt(@Body ReceiptDto receipt, OnComplete<Response<WalletDto>> callback);

    @POST("/api/shop/ad-reward")
    void adReward(@Body AdRewardDto reward, OnComplete<Response<WalletDto>> callback);
}
