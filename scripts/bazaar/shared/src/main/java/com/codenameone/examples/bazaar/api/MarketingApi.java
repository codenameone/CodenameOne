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
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Segments, e-mail and SMS campaigns, and signup forms (OWNER, MANAGER); a form's public half.
@RestClient
public interface MarketingApi {
    @GET("/api/merchant/stores/{storeId}/segments")
    void segments(@Path("storeId") String storeId,
            OnComplete<Response<List<AudienceDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/segments")
    void createSegment(@Path("storeId") String storeId,
            @Body AudienceDto segment,
            OnComplete<Response<AudienceDto>> callback);

    @POST("/api/merchant/stores/{storeId}/segments/{segmentId}")
    void saveSegment(@Path("storeId") String storeId,
            @Path("segmentId") String segmentId,
            @Body AudienceDto segment,
            OnComplete<Response<AudienceDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/segments/{segmentId}")
    void removeSegment(@Path("storeId") String storeId,
            @Path("segmentId") String segmentId,
            OnComplete<Response<List<AudienceDto>>> callback);

    /// Counts a rule tree's customers without saving it, with a sample.
    @POST("/api/merchant/stores/{storeId}/segments-preview")
    void preview(@Path("storeId") String storeId,
            @Body AudienceDto segment,
            OnComplete<Response<AudiencePreviewDto>> callback);

    @GET("/api/merchant/stores/{storeId}/campaigns")
    void campaigns(@Path("storeId") String storeId,
            OnComplete<Response<List<CampaignDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/campaigns")
    void createCampaign(@Path("storeId") String storeId,
            @Body CampaignDto campaign,
            OnComplete<Response<CampaignDto>> callback);

    /// Edits a campaign that has not gone out.
    @POST("/api/merchant/stores/{storeId}/campaigns/{campaignId}")
    void saveCampaign(@Path("storeId") String storeId,
            @Path("campaignId") String campaignId,
            @Body CampaignDto campaign,
            OnComplete<Response<CampaignDto>> callback);

    /// Schedules a campaign; `at` 0 is now.
    @POST("/api/merchant/stores/{storeId}/campaigns/{campaignId}/schedule")
    void schedule(@Path("storeId") String storeId,
            @Path("campaignId") String campaignId,
            @Body ScheduleDto when,
            OnComplete<Response<CampaignDto>> callback);

    @POST("/api/merchant/stores/{storeId}/campaigns/{campaignId}/cancel")
    void cancelCampaign(@Path("storeId") String storeId,
            @Path("campaignId") String campaignId,
            OnComplete<Response<CampaignDto>> callback);

    @GET("/api/merchant/stores/{storeId}/campaigns/{campaignId}/stats")
    void stats(@Path("storeId") String storeId,
            @Path("campaignId") String campaignId,
            OnComplete<Response<CampaignStatsDto>> callback);

    @GET("/api/merchant/stores/{storeId}/signup-forms")
    void forms(@Path("storeId") String storeId,
            OnComplete<Response<List<SignupFormDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/signup-forms")
    void createForm(@Path("storeId") String storeId,
            @Body SignupFormDto form,
            OnComplete<Response<SignupFormDto>> callback);

    @POST("/api/merchant/stores/{storeId}/signup-forms/{formId}")
    void saveForm(@Path("storeId") String storeId,
            @Path("formId") String formId,
            @Body SignupFormDto form,
            OnComplete<Response<SignupFormDto>> callback);

    /// An active form's definition, for the storefront to draw.
    @GET("/api/shop/stores/{storeId}/signup-forms/{formId}")
    void publicForm(@Path("storeId") String storeId,
            @Path("formId") String formId,
            OnComplete<Response<SignupFormDto>> callback);

    /// Submits a form. Rate limited per address; a filled honeypot is quietly ignored.
    @POST("/api/shop/stores/{storeId}/signup-forms/{formId}/submit")
    void submit(@Path("storeId") String storeId,
            @Path("formId") String formId,
            @Body FormSubmitDto entry,
            OnComplete<Response<FormResultDto>> callback);
}
