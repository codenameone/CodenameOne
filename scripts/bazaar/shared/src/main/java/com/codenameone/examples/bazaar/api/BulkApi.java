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

/// Merchant mode: bulk operations. An export or import runs in the background;
/// poll the job, then fetch `bulk-jobs/{jobId}/result.jsonl`.
@RestClient
public interface BulkApi {
    /// The newest jobs first.
    @GET("/api/merchant/stores/{storeId}/bulk-jobs")
    void list(@Path("storeId") String storeId, OnComplete<Response<List<BulkJobDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/bulk-jobs")
    void start(@Path("storeId") String storeId, @Body BulkJobDto job, OnComplete<Response<BulkJobDto>> callback);

    @GET("/api/merchant/stores/{storeId}/bulk-jobs/{jobId}")
    void job(@Path("storeId") String storeId, @Path("jobId") String jobId, OnComplete<Response<BulkJobDto>> callback);
}
