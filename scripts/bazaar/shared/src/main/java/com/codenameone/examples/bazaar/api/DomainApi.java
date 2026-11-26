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

/// Merchant mode: the store's custom domain. Claiming one hands out a token;
/// the domain is served once [#verify] finds that token published on it.
@RestClient
public interface DomainApi {
    @GET("/api/merchant/stores/{storeId}/domain")
    void domain(@Path("storeId") String storeId, OnComplete<Response<CustomDomainDto>> callback);

    /// Claims a domain (only `domain` is read; empty removes it).
    @POST("/api/merchant/stores/{storeId}/domain")
    void claim(@Path("storeId") String storeId, @Body CustomDomainDto domain, OnComplete<Response<CustomDomainDto>> callback);

    /// Looks for the token on the domain now.
    @POST("/api/merchant/stores/{storeId}/domain/verify")
    void verify(@Path("storeId") String storeId, OnComplete<Response<CustomDomainDto>> callback);
}
