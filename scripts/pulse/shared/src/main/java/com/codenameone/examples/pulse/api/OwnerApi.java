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
package com.codenameone.examples.pulse.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// Running the studio: for accounts with the OWNER role.
@RestClient
public interface OwnerApi {
    @GET("/api/owner/dashboard")
    void dashboard(OnComplete<Response<OwnerDashboardDto>> callback);

    @GET("/api/owner/plans")
    void plans(OnComplete<Response<List<PlanDto>>> callback);

    /// Creates a plan, or changes one; an empty id creates. A changed price
    /// applies from each membership's next renewal.
    @PUT("/api/owner/plans")
    void savePlan(@Body PlanDto plan, OnComplete<Response<PlanDto>> callback);

    @PUT("/api/owner/types")
    void saveClassType(@Body ClassTypeDto type, OnComplete<Response<ClassTypeDto>> callback);

    @GET("/api/owner/rooms")
    void rooms(OnComplete<Response<List<RoomDto>>> callback);

    /// Puts a class on the timetable, every week for `weeks` weeks. Answers 409
    /// when the room or the trainer is taken at that time.
    @POST("/api/owner/classes")
    void schedule(@Body NewSessionDto session, OnComplete<Response<List<SessionDto>>> callback);

    /// Cancels a class. Everyone booked is told by push, and a class pack gets
    /// its credit back.
    @POST("/api/owner/classes/{id}/cancel")
    void cancelClass(@Path("id") String id, @Body ReasonDto reason,
            OnComplete<Response<SessionDto>> callback);

    /// Gives a class to another trainer.
    @POST("/api/owner/classes/{id}/trainer")
    void substitute(@Path("id") String id, @Query("trainer") String trainer,
            OnComplete<Response<SessionDto>> callback);

    @GET("/api/owner/staff")
    void staff(OnComplete<Response<List<StaffDto>>> callback);

    @PUT("/api/owner/staff")
    void saveStaff(@Body StaffDto staff, OnComplete<Response<StaffDto>> callback);

    @GET("/api/owner/shifts")
    void shifts(@Query("location") String location, OnComplete<Response<List<ShiftDto>>> callback);

    @POST("/api/owner/shifts")
    void addShift(@Body ShiftDto shift, OnComplete<Response<ShiftDto>> callback);

    @GET("/api/owner/campaigns")
    void campaigns(OnComplete<Response<List<CampaignDto>>> callback);

    /// Sends a campaign. A FILL_CLASS one goes to members who have taken that
    /// kind of class before and are not booked, not away and not in quiet
    /// hours; a WIN_BACK one to members who have not been in for three weeks.
    @POST("/api/owner/campaigns")
    void send(@Body CampaignDto campaign, OnComplete<Response<CampaignDto>> callback);

    /// Every invoice, newest first, `status` to narrow it (empty for all).
    @GET("/api/owner/invoices")
    void invoices(@Query("status") String status, OnComplete<Response<List<InvoiceDto>>> callback);

    /// Voids an invoice nobody should pay. A PAST_DUE membership it held up
    /// becomes ACTIVE again.
    @POST("/api/owner/invoices/{id}/void")
    void voidInvoice(@Path("id") String id, @Body ReasonDto reason,
            OnComplete<Response<InvoiceDto>> callback);
}
