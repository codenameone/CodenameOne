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
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The moderation queue, decisions and the blocklist.
@RestClient
public interface ModerationApi {
    @GET("/api/moderation/queue")
    void queue(@Query("kind") String kind, OnComplete<Response<List<ModerationItemDto>>> callback);

    /// Approves, rejects, or escalates an item; `action` is APPROVE, REJECT, AGE_RESTRICT or ESCALATE.
    @POST("/api/moderation/items/{id}")
    void decide(@Path("id") String id, @Body ModerationDecisionDto decision,
            OnComplete<Response<ModerationItemDto>> callback);

    @GET("/api/moderation/blocklist")
    void blocklist(OnComplete<Response<List<BlocklistEntryDto>>> callback);

    @POST("/api/moderation/blocklist")
    void block(@Body BlocklistEntryDto entry, OnComplete<Response<BlocklistEntryDto>> callback);

    @DELETE("/api/moderation/blocklist/{id}")
    void unblock(@Path("id") String id, OnComplete<Response<String>> callback);

    @GET("/api/moderation/events")
    void events(@Query("username") String username, OnComplete<Response<List<ModerationEventDto>>> callback);
}
