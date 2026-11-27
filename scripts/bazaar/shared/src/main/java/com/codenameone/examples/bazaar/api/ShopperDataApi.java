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
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The signed-in shopper's own personal data: a download of everything the stores they shop
/// at hold about them, and requests to a store to erase it.
@RestClient
public interface ShopperDataApi {
    /// The shopper's data at every store, as a zip of JSON files.
    @GET("/api/account/data-export")
    void export(OnComplete<Response<DataExportDto>> callback);

    /// The shopper's data requests, newest first.
    @GET("/api/account/data-requests")
    void requests(OnComplete<Response<List<DataRequestDto>>> callback);

    /// Asks a store to send (`EXPORT`) or erase (`ERASURE`) the shopper's data there. A
    /// request of the same kind still open is answered instead of a second one.
    @POST("/api/account/data-requests/{storeId}")
    void request(@Path("storeId") String storeId, @Body DataRequestKindDto kind,
            OnComplete<Response<DataRequestDto>> callback);
}
