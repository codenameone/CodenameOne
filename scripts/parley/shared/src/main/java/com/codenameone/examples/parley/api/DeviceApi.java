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
import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The devices of the signed-in account and the public keys each publishes.
/// Only public keys ever reach the server; private keys stay on the device.
@RestClient
public interface DeviceApi {
    @POST("/api/devices")
    void register(@Body DeviceRegistrationDto device, OnComplete<Response<DeviceDto>> callback);

    @GET("/api/devices")
    void list(OnComplete<Response<List<DeviceDto>>> callback);

    @DELETE("/api/devices/{id}")
    void unlink(@Path("id") String id, OnComplete<Response<DeviceDto>> callback);

    @POST("/api/devices/{id}/prekeys")
    void uploadPreKeys(@Path("id") String id, @Body PreKeyUploadDto keys,
            OnComplete<Response<KeyCountDto>> callback);

    @GET("/api/devices/{id}/prekeys/count")
    void preKeyCount(@Path("id") String id, OnComplete<Response<KeyCountDto>> callback);

    @GET("/api/keys/{username}")
    void bundles(@Path("username") String username,
            OnComplete<Response<List<PreKeyBundleDto>>> callback);

    @GET("/api/keys/{username}/{device}")
    void bundle(@Path("username") String username, @Path("device") String device,
            OnComplete<Response<PreKeyBundleDto>> callback);
}
