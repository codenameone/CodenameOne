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
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// Merchant mode: the sales pipeline, every cart and order of a store as a
/// card on a board from browsing to a return.
@RestClient
public interface PipelineApi {
    /// The board. `column` blank answers the first page of every column;
    /// otherwise only that column, at page `page` (0 first).
    @GET("/api/merchant/stores/{storeId}/pipeline")
    void board(@Path("storeId") String storeId, @Query("column") String column,
            @Query("page") String page, OnComplete<Response<List<PipelineColumnDto>>> callback);

    /// Moves a card to another column, doing what that column means; a move
    /// the pipeline does not allow is a 409. Answers the board as it now is.
    @POST("/api/merchant/stores/{storeId}/pipeline/move")
    void move(@Path("storeId") String storeId, @Body PipelineMoveDto move,
            OnComplete<Response<List<PipelineColumnDto>>> callback);
}
