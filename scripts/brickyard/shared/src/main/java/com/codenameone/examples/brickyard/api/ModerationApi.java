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

/// Reports, the moderator queue, sanctions, appeals, the audit log, roles, featuring, KPIs and economy administration.
@RestClient
public interface ModerationApi {
    @POST("/api/reports")
    void report(@Body ReportDto report, OnComplete<Response<ReportDto>> callback);

    @GET("/api/admin/reports")
    void queue(@Query("status") String status, OnComplete<Response<List<ReportDto>>> callback);

    @POST("/api/admin/reports/{reportId}/resolve")
    void resolveReport(@Path("reportId") String reportId, @Body ActionDto action, OnComplete<Response<ReportDto>> callback);

    @POST("/api/admin/sanctions")
    void sanction(@Body SanctionDto sanction, OnComplete<Response<SanctionDto>> callback);

    @GET("/api/admin/users/{username}/sanctions")
    void sanctionsOf(@Path("username") String username, OnComplete<Response<List<SanctionDto>>> callback);

    @POST("/api/admin/experiences/{experienceId}/unpublish")
    void unpublish(@Path("experienceId") String experienceId, @Body ActionDto action, OnComplete<Response<AuditDto>> callback);

    @GET("/api/me/sanctions")
    void mySanctions(OnComplete<Response<List<SanctionDto>>> callback);

    @POST("/api/me/appeals")
    void appeal(@Body AppealDto appeal, OnComplete<Response<AppealDto>> callback);

    @GET("/api/admin/appeals")
    void appeals(OnComplete<Response<List<AppealDto>>> callback);

    @POST("/api/admin/appeals/{appealId}/decide")
    void decideAppeal(@Path("appealId") String appealId, @Body ActionDto action, OnComplete<Response<AppealDto>> callback);

    @GET("/api/admin/audit")
    void audit(OnComplete<Response<List<AuditDto>>> callback);

    @POST("/api/admin/roles")
    void setRole(@Body RoleDto role, OnComplete<Response<AuditDto>> callback);

    @POST("/api/admin/featured")
    void feature(@Body FeaturedDto featured, OnComplete<Response<FeaturedDto>> callback);

    @POST("/api/admin/featured/{featuredId}/remove")
    void unfeature(@Path("featuredId") String featuredId, OnComplete<Response<FeaturedDto>> callback);

    @GET("/api/home/featured")
    void featured(OnComplete<Response<List<FeaturedDto>>> callback);

    @GET("/api/admin/kpis")
    void kpis(OnComplete<Response<KpiDto>> callback);

    @POST("/api/admin/economy/adjust")
    void adjust(@Body AdjustDto adjust, OnComplete<Response<WalletDto>> callback);

    @POST("/api/admin/economy/refund")
    void refund(@Body ActionDto action, OnComplete<Response<WalletDto>> callback);

    @GET("/api/admin/fraud")
    void fraudFlags(OnComplete<Response<List<FraudFlagDto>>> callback);

    @GET("/api/admin/payouts")
    void payouts(OnComplete<Response<List<PayoutDto>>> callback);

    @POST("/api/admin/payouts/{payoutId}/decide")
    void decidePayout(@Path("payoutId") String payoutId, @Body ActionDto action, OnComplete<Response<PayoutDto>> callback);

    @POST("/api/admin/quests")
    void defineQuest(@Body QuestDto quest, OnComplete<Response<QuestDto>> callback);

    @POST("/api/admin/catalog")
    void addCatalogItem(@Body CatalogItemDto item, OnComplete<Response<CatalogItemDto>> callback);

    @POST("/api/admin/jobs/{job}")
    void runJob(@Path("job") String job, OnComplete<Response<JobDto>> callback);
}
