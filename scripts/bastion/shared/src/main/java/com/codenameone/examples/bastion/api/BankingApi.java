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
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Money: the caller's accounts, moving money between them and to other
/// customers, payees, cards and the devices allowed to sign for the caller.
///
/// Two headers recur:
///
/// - **`Idempotency-Key`** on every call that moves money: 16 to 80 random
///   characters, made once per payment and sent again unchanged on every retry
///   of it. However many times the request arrives, the money moves once and
///   every answer is the same receipt. Reusing a key for a different payment
///   is refused (422).
/// - **`Bastion-Signature`** where a request must come from a registered
///   device: transfers above the step-up threshold, anything paying someone
///   else, withdrawals and new payees. It is
///   `v1;device=<id>;ts=<millis>;nonce=<random>;sig=<base64url>`, where `sig`
///   is the device key's ES256 signature (r and s, 32 bytes each, as JOSE
///   writes them) of
///   `METHOD|path|hex(sha256(body))|nonce|ts`. The body is the exact bytes
///   sent, so the client serializes once and signs what it sends. A stolen
///   access token alone therefore cannot move money out: it needs the private
///   key, which never leaves the device's key store.
///
/// A null header argument is left out of the request.
@RestClient
public interface BankingApi {
    /// The assets the bank holds, from the ledger's own table: the app counts
    /// every amount in the decimals named here rather than in a copy of them.
    @GET("/api/assets")
    void assets(OnComplete<Response<List<AssetDto>>> callback);

    @GET("/api/accounts")
    void accounts(OnComplete<Response<List<AccountDto>>> callback);

    @POST("/api/accounts")
    void open(@Body OpenAccountDto request, OnComplete<Response<AccountDto>> callback);

    /// The newest 100 postings. Another customer's account answers 404, as
    /// one that does not exist does.
    @GET("/api/accounts/{id}/postings")
    void history(@Path("id") String id, OnComplete<Response<List<PostingDto>>> callback);

    @POST("/api/transfers")
    void transfer(@Header("Idempotency-Key") String idempotencyKey,
            @Header("Bastion-Signature") String signature, @Body TransferDto transfer,
            OnComplete<Response<ReceiptDto>> callback);

    /// Simulated cash arriving. Development and test servers only; elsewhere
    /// the route answers 404.
    @POST("/api/accounts/{id}/deposit")
    void deposit(@Path("id") String id, @Header("Idempotency-Key") String idempotencyKey,
            @Body CashDto cash, OnComplete<Response<ReceiptDto>> callback);

    /// Money, or coins, leaving the bank. Always signed.
    @POST("/api/accounts/{id}/withdraw")
    void withdraw(@Path("id") String id, @Header("Idempotency-Key") String idempotencyKey,
            @Header("Bastion-Signature") String signature, @Body CashDto cash,
            OnComplete<Response<ReceiptDto>> callback);

    @GET("/api/payees")
    void payees(OnComplete<Response<List<PayeeDto>>> callback);

    /// Always signed: a new payee is where stolen money would go.
    @POST("/api/payees")
    void addPayee(@Header("Bastion-Signature") String signature, @Body NewPayeeDto payee,
            OnComplete<Response<PayeeDto>> callback);

    @PUT("/api/payees/{id}")
    void renamePayee(@Path("id") String id, @Body NewPayeeDto payee,
            OnComplete<Response<PayeeDto>> callback);

    @DELETE("/api/payees/{id}")
    void removePayee(@Path("id") String id, OnComplete<Response<PayeeDto>> callback);

    @GET("/api/cards")
    void cards(OnComplete<Response<List<CardDto>>> callback);

    /// Issues the virtual card of a checking account; one per account.
    @POST("/api/accounts/{id}/card")
    void issueCard(@Path("id") String accountId, OnComplete<Response<CardDto>> callback);

    @POST("/api/cards/{id}/freeze")
    void freezeCard(@Path("id") String id, OnComplete<Response<CardDto>> callback);

    @POST("/api/cards/{id}/unfreeze")
    void unfreezeCard(@Path("id") String id, OnComplete<Response<CardDto>> callback);

    @PUT("/api/cards/{id}/limit")
    void cardLimit(@Path("id") String id, @Body CardLimitDto limit,
            OnComplete<Response<CardDto>> callback);

    /// The card network asking to charge the card. Simulated, so development
    /// and test servers only.
    @POST("/api/cards/{id}/spend")
    void spend(@Path("id") String id, @Header("Idempotency-Key") String idempotencyKey,
            @Body CardSpendDto spend, OnComplete<Response<ReceiptDto>> callback);

    @GET("/api/devices")
    void devices(OnComplete<Response<List<DeviceDto>>> callback);

    @POST("/api/devices")
    void registerDevice(@Body DeviceRegistrationDto device,
            OnComplete<Response<DeviceDto>> callback);

    /// Withdraws a device's key, for a phone that was lost.
    @DELETE("/api/devices/{id}")
    void revokeDevice(@Path("id") String id, OnComplete<Response<DeviceDto>> callback);
}
