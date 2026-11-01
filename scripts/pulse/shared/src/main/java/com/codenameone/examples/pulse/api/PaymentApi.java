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
package com.codenameone.examples.pulse.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Ways to pay, and what was paid. Everything here is the signed-in user's own.
@RestClient
public interface PaymentApi {
    @GET("/api/payments/config")
    void config(OnComplete<Response<PaymentConfigDto>> callback);

    /// The saved cards, and cash, which is always there.
    @GET("/api/payments/methods")
    void methods(OnComplete<Response<List<PaymentMethodDto>>> callback);

    /// Begins adding a card; see [PaymentSetupDto] for the two ways it goes on.
    @POST("/api/payments/methods/setup")
    void startSetup(OnComplete<Response<PaymentSetupDto>> callback);

    /// Finishes adding a card. `card` is read by the simulated processor and
    /// ignored when the setup was hosted.
    @POST("/api/payments/methods/setup/{id}/complete")
    void completeSetup(@Path("id") String id, @Body CardDto card,
            OnComplete<Response<PaymentMethodDto>> callback);

    @POST("/api/payments/methods/{id}/default")
    void makeDefault(@Path("id") String id, OnComplete<Response<PaymentMethodDto>> callback);

    @POST("/api/payments/methods/{id}/remove")
    void remove(@Path("id") String id, OnComplete<Response<PaymentMethodDto>> callback);

    /// The caller's receipts as a customer, newest first.
}
