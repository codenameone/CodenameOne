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
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The addresses the signed-in shopper keeps. The first one saved is the
/// default until another is made it.
@RestClient
public interface AddressApi {
    @GET("/api/account/addresses")
    void list(OnComplete<Response<List<SavedAddressDto>>> callback);

    @POST("/api/account/addresses")
    void add(@Body SavedAddressDto address, OnComplete<Response<SavedAddressDto>> callback);

    @PUT("/api/account/addresses/{id}")
    void update(@Path("id") String id, @Body SavedAddressDto address,
            OnComplete<Response<SavedAddressDto>> callback);

    @DELETE("/api/account/addresses/{id}")
    void remove(@Path("id") String id, OnComplete<Response<List<SavedAddressDto>>> callback);

    @POST("/api/account/addresses/{id}/default")
    void makeDefault(@Path("id") String id, OnComplete<Response<List<SavedAddressDto>>> callback);
}
