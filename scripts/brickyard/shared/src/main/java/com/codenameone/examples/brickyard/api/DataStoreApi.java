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
package com.codenameone.examples.brickyard.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// An experience's persistent data stores, which an engine server reads and writes for its scripts.
/// Only an engine server, or the experience's owner, may call these.
@RestClient
public interface DataStoreApi {
    @GET("/api/data/{experienceId}/{store}/{scope}/{key}")
    void get(@Path("experienceId") String experienceId, @Path("store") String store,
            @Path("scope") String scope, @Path("key") String key, OnComplete<Response<DataValueDto>> callback);

    @POST("/api/data/{experienceId}/{store}/{scope}/{key}")
    void set(@Path("experienceId") String experienceId, @Path("store") String store,
            @Path("scope") String scope, @Path("key") String key, @Body DataSetDto value,
            OnComplete<Response<DataValueDto>> callback);

    @POST("/api/data/{experienceId}/{store}/{scope}/{key}/increment")
    void increment(@Path("experienceId") String experienceId, @Path("store") String store,
            @Path("scope") String scope, @Path("key") String key, @Body IncrementDto delta,
            OnComplete<Response<DataValueDto>> callback);

    @GET("/api/data/{experienceId}/{store}/{scope}")
    void list(@Path("experienceId") String experienceId, @Path("store") String store,
            @Path("scope") String scope, OnComplete<Response<List<DataValueDto>>> callback);
}
