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
package com.codenameone.examples.bastion.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.Header;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The crypto exchange, and the wallets' way out of the bank.
///
/// Trading needs no signature: an order moves value only between the
/// customer's own accounts and the counterparty's, inside the bank, and it is
/// as reversible as the customer's next order. Coins *leaving* the bank are
/// another matter -- a blockchain transaction is never reversed -- so a
/// withdrawal is signed by a trusted device, goes only to an address the
/// customer allow-listed a cooling-off period earlier, and is held to limits
/// valued at the index price.
@RestClient
public interface ExchangeApi {
    @GET("/api/exchange/markets")
    void markets(OnComplete<Response<List<MarketDto>>> callback);

    @GET("/api/exchange/markets/{id}/book")
    void book(@Path("id") String market, OnComplete<Response<BookDto>> callback);

    @GET("/api/exchange/markets/{id}/ticker")
    void ticker(@Path("id") String market, OnComplete<Response<TickerDto>> callback);

    /// Places an order. The `Idempotency-Key` makes a retry answer the order
    /// the first attempt placed instead of placing a second.
    @POST("/api/exchange/orders")
    void place(@Header("Idempotency-Key") String idempotencyKey, @Body NewOrderDto order,
            OnComplete<Response<OrderDto>> callback);

    @DELETE("/api/exchange/orders/{id}")
    void cancel(@Path("id") String id, OnComplete<Response<OrderDto>> callback);

    /// The caller's open orders, newest first.
    @GET("/api/exchange/orders")
    void openOrders(OnComplete<Response<List<OrderDto>>> callback);

    /// The caller's fills, newest first.
    @GET("/api/exchange/trades")
    void trades(OnComplete<Response<List<TradeDto>>> callback);

    @GET("/api/crypto/addresses")
    void addresses(OnComplete<Response<List<CryptoAddressDto>>> callback);

    /// Allow-lists an address. Signed, like adding a payee.
    @POST("/api/crypto/addresses")
    void addAddress(@Header("Bastion-Signature") String signature,
            @Body NewCryptoAddressDto address, OnComplete<Response<CryptoAddressDto>> callback);

    @DELETE("/api/crypto/addresses/{id}")
    void removeAddress(@Path("id") String id, OnComplete<Response<CryptoAddressDto>> callback);

    /// Coins out of wallet `id` to an allow-listed address. Always signed.
    @POST("/api/accounts/{id}/crypto-withdraw")
    void withdrawCrypto(@Path("id") String id, @Header("Idempotency-Key") String idempotencyKey,
            @Header("Bastion-Signature") String signature, @Body CryptoWithdrawalDto withdrawal,
            OnComplete<Response<ReceiptDto>> callback);
}
