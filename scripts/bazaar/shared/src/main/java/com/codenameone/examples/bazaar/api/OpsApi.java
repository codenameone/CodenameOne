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

/// Operations and insight: the warehouse board, bins and pick lists,
/// transfer and purchase order timelines, forecasts and the drafts they
/// suggest, custom reports, cohorts and attribution, permission sets, the
/// chat inbox, combined listings and marketplace import.
@RestClient
public interface OpsApi {
    // ------------------------------------------------------------ warehouse

    @GET("/api/merchant/stores/{storeId}/warehouse/{locationId}/board")
    void warehouseBoard(@Path("storeId") String storeId, @Path("locationId") String locationId,
            OnComplete<Response<List<WarehouseTaskDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/warehouse/{locationId}/sync")
    void syncWarehouse(@Path("storeId") String storeId, @Path("locationId") String locationId,
            OnComplete<Response<List<WarehouseTaskDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/warehouse/move")
    void moveWarehouseTask(@Path("storeId") String storeId, @Body WarehouseMoveDto body,
            OnComplete<Response<WarehouseTaskDto>> callback);

    @GET("/api/merchant/stores/{storeId}/warehouse/{locationId}/bins")
    void bins(@Path("storeId") String storeId, @Path("locationId") String locationId,
            OnComplete<Response<List<BinDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/warehouse/{locationId}/bins")
    void saveBin(@Path("storeId") String storeId, @Path("locationId") String locationId,
            @Body BinDto body, OnComplete<Response<List<BinDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/warehouse/{locationId}/pick-list")
    void pickList(@Path("storeId") String storeId, @Path("locationId") String locationId,
            OnComplete<Response<List<PickLineDto>>> callback);

    // ------------------------------------------------------------ timelines

    @GET("/api/merchant/stores/{storeId}/transfers/{transferId}/timeline")
    void transferTimeline(@Path("storeId") String storeId,
            @Path("transferId") String transferId,
            OnComplete<Response<List<TimelineEventDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/purchase-orders/{orderId}/timeline")
    void purchaseOrderTimeline(@Path("storeId") String storeId,
            @Path("orderId") String orderId,
            OnComplete<Response<List<TimelineEventDto>>> callback);

    // ------------------------------------------------------------- forecast

    @GET("/api/merchant/stores/{storeId}/forecast")
    void forecast(@Path("storeId") String storeId,
            OnComplete<Response<List<ForecastDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/forecast/suggestions")
    void suggestions(@Path("storeId") String storeId,
            OnComplete<Response<List<SuggestionDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/forecast/reorder")
    void draftReorders(@Path("storeId") String storeId, @Body SuggestionApplyDto body,
            OnComplete<Response<List<PurchaseOrderDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/forecast/rebalance")
    void draftTransfers(@Path("storeId") String storeId, @Body SuggestionApplyDto body,
            OnComplete<Response<List<TransferDto>>> callback);

    // -------------------------------------------------------------- reports

    @POST("/api/merchant/stores/{storeId}/reports/run")
    void runReport(@Path("storeId") String storeId, @Body ReportSpecDto body,
            OnComplete<Response<ReportDto>> callback);

    @GET("/api/merchant/stores/{storeId}/reports")
    void savedReports(@Path("storeId") String storeId,
            OnComplete<Response<List<SavedReportDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/reports")
    void saveReport(@Path("storeId") String storeId, @Body ReportSpecDto body,
            OnComplete<Response<SavedReportDto>> callback);

    @GET("/api/merchant/stores/{storeId}/reports/{reportId}")
    void savedReport(@Path("storeId") String storeId, @Path("reportId") String reportId,
            OnComplete<Response<ReportDto>> callback);

    @GET("/api/merchant/stores/{storeId}/cohorts")
    void cohorts(@Path("storeId") String storeId,
            OnComplete<Response<List<CohortRowDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/profit-loss")
    void profitLoss(@Path("storeId") String storeId, @Query("from") String from,
            @Query("to") String to, OnComplete<Response<ProfitLossDto>> callback);

    @GET("/api/merchant/stores/{storeId}/attribution")
    void attribution(@Path("storeId") String storeId, @Query("from") String from,
            @Query("to") String to, OnComplete<Response<List<AttributionDto>>> callback);

    // ---------------------------------------------------------- permissions

    @GET("/api/merchant/stores/{storeId}/permission-sets")
    void permissionSets(@Path("storeId") String storeId,
            OnComplete<Response<List<PermissionSetDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/permission-sets")
    void createPermissionSet(@Path("storeId") String storeId, @Body PermissionSetDto body,
            OnComplete<Response<PermissionSetDto>> callback);

    @POST("/api/merchant/stores/{storeId}/permission-sets/{setId}")
    void updatePermissionSet(@Path("storeId") String storeId, @Path("setId") String setId,
            @Body PermissionSetDto body, OnComplete<Response<PermissionSetDto>> callback);

    @GET("/api/merchant/stores/{storeId}/staff-permissions")
    void staffPermissions(@Path("storeId") String storeId,
            OnComplete<Response<List<StaffPermissionDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/staff-permissions")
    void assignPermissionSet(@Path("storeId") String storeId, @Body StaffPermissionDto body,
            OnComplete<Response<List<StaffPermissionDto>>> callback);

    // ----------------------------------------------------------------- chat

    @GET("/api/merchant/stores/{storeId}/chats")
    void chats(@Path("storeId") String storeId,
            OnComplete<Response<List<ChatConversationDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/chats/{chatId}")
    void chatMessages(@Path("storeId") String storeId, @Path("chatId") String chatId,
            OnComplete<Response<List<ChatMessageDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/chats/{chatId}/reply")
    void replyChat(@Path("storeId") String storeId, @Path("chatId") String chatId,
            @Body ChatReplyDto body, OnComplete<Response<List<ChatMessageDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/chats/{chatId}/assign")
    void assignChat(@Path("storeId") String storeId, @Path("chatId") String chatId,
            @Body ChatReplyDto body, OnComplete<Response<ChatConversationDto>> callback);

    @POST("/api/merchant/stores/{storeId}/chats/{chatId}/close")
    void closeChat(@Path("storeId") String storeId, @Path("chatId") String chatId,
            OnComplete<Response<ChatConversationDto>> callback);

    @GET("/api/merchant/stores/{storeId}/canned-replies")
    void cannedReplies(@Path("storeId") String storeId,
            OnComplete<Response<List<CannedReplyDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/canned-replies")
    void saveCannedReply(@Path("storeId") String storeId, @Body CannedReplyDto body,
            OnComplete<Response<List<CannedReplyDto>>> callback);

    // ------------------------------------------------------------- listings

    @GET("/api/merchant/stores/{storeId}/combined-listings")
    void combinedListings(@Path("storeId") String storeId,
            OnComplete<Response<List<CombinedListingDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/combined-listings")
    void saveCombinedListing(@Path("storeId") String storeId, @Body CombinedListingDto body,
            OnComplete<Response<CombinedListingDto>> callback);

    @POST("/api/merchant/stores/{storeId}/imports/marketplace")
    void importMarketplace(@Path("storeId") String storeId, @Body ImportDto body,
            OnComplete<Response<ImportResultDto>> callback);
}
