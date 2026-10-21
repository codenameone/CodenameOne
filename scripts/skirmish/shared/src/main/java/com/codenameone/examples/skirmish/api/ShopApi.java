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
