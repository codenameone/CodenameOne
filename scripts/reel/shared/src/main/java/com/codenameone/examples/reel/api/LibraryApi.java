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
package com.codenameone.examples.reel.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// A profile's own things: its list, ratings, history and downloads.
@RestClient
public interface LibraryApi {
    @GET("/api/library/list")
    void myList(@Query("profile") String profile, OnComplete<Response<List<TitleCardDto>>> callback);

    @PUT("/api/library/list/{titleId}")
    void addToList(@Path("titleId") String titleId, @Query("profile") String profile,
            OnComplete<Response<String>> callback);

    @DELETE("/api/library/list/{titleId}")
    void removeFromList(@Path("titleId") String titleId, @Query("profile") String profile,
            OnComplete<Response<String>> callback);

    @PUT("/api/library/ratings/{titleId}")
    void rate(@Path("titleId") String titleId, @Query("profile") String profile, @Body RatingDto rating,
            OnComplete<Response<String>> callback);

    @GET("/api/library/history")
    void history(@Query("profile") String profile, OnComplete<Response<List<HistoryEntryDto>>> callback);

    /// Removes a title from the history and from what recommendations learn from.
    @DELETE("/api/library/history/{titleId}")
    void forget(@Path("titleId") String titleId, @Query("profile") String profile,
            OnComplete<Response<String>> callback);

    @GET("/api/library/downloads")
    void downloads(@Query("device") String device, OnComplete<Response<List<DownloadDto>>> callback);

    /// Licenses a title for offline play on this device. Answers 403 when the plan has no downloads.
    @POST("/api/library/downloads/{titleId}")
    void download(@Path("titleId") String titleId, @Query("device") String device, @Query("episode") String episode,
            OnComplete<Response<DownloadDto>> callback);

    @DELETE("/api/library/downloads/{id}")
    void deleteDownload(@Path("id") String id, OnComplete<Response<String>> callback);
}
