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
package com.codenameone.examples.fieldline.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// The admin's console: who has an account and what it may do, and the
/// crew records the planner works from. Everything here needs the ADMIN role.
///
/// An account is named in the body, not the path: account names are e-mail
/// addresses, and an `@` in a path segment is one more thing to escape.
@RestClient
public interface AdminApi {
    /// Every account, newest first.
    @GET("/api/admin/accounts")
    void accounts(OnComplete<Response<List<UserDto>>> callback);

    /// Sets an account's roles and whether it is suspended. Granting the
    /// technician role enrolls the account in the crew; withdrawing it takes
    /// the technician off the plan, which is repaired around them.
    @POST("/api/admin/accounts")
    void changeAccount(@Body AccountChangeDto change, OnComplete<Response<UserDto>> callback);

    /// The planning rules and company settings.
    @GET("/api/admin/policy")
    void policy(OnComplete<Response<PolicyDto>> callback);

    @POST("/api/admin/policy")
    void changePolicy(@Body PolicyDto policy, OnComplete<Response<PolicyDto>> callback);

    /// Every technician record, active or not, with what the planner reads.
    @GET("/api/admin/technicians")
    void technicians(OnComplete<Response<List<CrewMemberDto>>> callback);

    /// Saves a technician's skills, limits and whether they are planned at
    /// all, and repairs the plan around the change.
    @POST("/api/admin/technicians")
    void changeTechnician(@Body CrewMemberDto change, OnComplete<Response<CrewMemberDto>> callback);

    /// The labour rate table, with how many technicians each one pays.
    @GET("/api/admin/rates")
    void rates(OnComplete<Response<List<RateDto>>> callback);

    /// Adds a rate (no id) or changes one. A rate nobody is paid at is
    /// removed by saving it with no name.
    @POST("/api/admin/rates")
    void saveRate(@Body RateDto rate, OnComplete<Response<List<RateDto>>> callback);

    /// Everyone's clocked time for the week holding `week`, costed.
    @GET("/api/admin/timesheets")
    void timesheets(@Query("week") long week, OnComplete<Response<TimesheetDto>> callback);
}
