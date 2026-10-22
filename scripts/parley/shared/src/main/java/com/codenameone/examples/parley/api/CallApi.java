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
package com.codenameone.examples.parley.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Voice and video calls. The server rings the other side and relays call
/// setup; the media itself flows directly between the devices, or through
/// the TURN server when they cannot reach each other.
@RestClient
public interface CallApi {
    @POST("/api/calls")
    void start(@Body CallStartDto call, OnComplete<Response<CallDto>> callback);

    @POST("/api/calls/{id}/signal")
    void signal(@Path("id") String id, @Body CallSignalDto signal,
            OnComplete<Response<CallDto>> callback);

    @GET("/api/calls")
    void history(OnComplete<Response<List<CallDto>>> callback);

    @GET("/api/calls/ice")
    void iceServers(OnComplete<Response<IceConfigDto>> callback);
}
