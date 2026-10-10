/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.admin;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PreAuthorize;
import com.codenameone.examples.wayline.Caller;
import com.codenameone.examples.wayline.Text;
import com.codenameone.examples.wayline.account.Accounts;
import com.codenameone.examples.wayline.api.AdminApiServer;
import com.codenameone.examples.wayline.api.AdminStatsDto;
import com.codenameone.examples.wayline.api.DocumentContentDto;
import com.codenameone.examples.wayline.api.DriverApplicationDto;
import com.codenameone.examples.wayline.api.DriverDto;
import com.codenameone.examples.wayline.api.FlagDto;
import com.codenameone.examples.wayline.api.PricingDto;
import com.codenameone.examples.wayline.api.ReasonDto;
import com.codenameone.examples.wayline.api.ReceiptDto;
import com.codenameone.examples.wayline.api.RideDto;
import com.codenameone.examples.wayline.api.RoleChangeDto;
import com.codenameone.examples.wayline.api.StatsSeriesDto;
import com.codenameone.examples.wayline.api.SuspendDto;
import com.codenameone.examples.wayline.api.UserDetailDto;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.driver.Drivers;
import com.codenameone.examples.wayline.driving.Applications;
import com.codenameone.examples.wayline.live.LiveHub;
import com.codenameone.examples.wayline.pay.Payments;
import com.codenameone.examples.wayline.ride.Fares;
import com.codenameone.examples.wayline.ride.Rides;

import java.util.List;

/// The server's half of `AdminApi`. Guarded by path in the filter chain and by
/// role here, as `DriverEndpoint` is.
///
/// This is the one place a name in the path chooses whose data is read: an
/// applicant's documents, a rider's receipts. Everywhere else the caller is
/// the subject, and only an admin gets to name somebody else.
@Component
@PreAuthorize("hasRole('ADMIN')")
public class AdminEndpoint implements AdminApiServer {
    private final Accounts accounts;
    private final Rides rides;
    private final Drivers drivers;
    private final Fares fares;
    private final LiveHub live;
    private final Applications applications;
    private final Payments payments;
    private final Statistics statistics;

    public AdminEndpoint(Accounts accounts, Rides rides, Drivers drivers,
            Fares fares, LiveHub live, Applications applications, Payments payments,
            Statistics statistics) {
        this.applications = applications;
        this.payments = payments;
        this.statistics = statistics;
        this.accounts = accounts;
        this.rides = rides;
        this.drivers = drivers;
        this.fares = fares;
        this.live = live;
    }

    @Override
    public AdminStatsDto stats() throws Exception {
        return statistics.dashboard();
    }

    @Override
    public List<RideDto> rides() throws Exception {
        return rides.recent();
    }

    @Override
    public List<UserDto> users() throws Exception {
        return accounts.all();
    }

    @Override
    public List<DriverDto> drivers() throws Exception {
        return drivers.all();
    }

    @Override
    public UserDto setRoles(String username, RoleChangeDto roles) throws Exception {
        return accounts.setRoles(Caller.name(), username, roles != null && roles.driver,
                roles != null && roles.admin);
    }

    @Override
    public UserDto suspend(String username, SuspendDto suspend) throws Exception {
        UserDto user = accounts.suspend(Caller.name(), username,
                suspend != null && suspend.suspended, suspend == null ? "" : suspend.reason);
        if (user.suspended) {
            // The API turns a suspended account away on its next request. A
            // live channel already open makes no further request, so it is
            // closed here.
            live.drop(username);
        }
        return user;
    }

    @Override
    public StatsSeriesDto statsSeries(int days) throws Exception {
        return statistics.series(days);
    }

    @Override
    public UserDetailDto user(String username) throws Exception {
        return statistics.user(username);
    }

    @Override
    public UserDto flag(String username, FlagDto flag) throws Exception {
        return accounts.flag(Caller.name(), username, flag != null && flag.flagged,
                flag == null ? "" : flag.reason);
    }

    @Override
    public List<DriverApplicationDto> applications(String status) throws Exception {
        return applications.list(status);
    }

    @Override
    public DriverApplicationDto application(String username) throws Exception {
        return applications.review(username);
    }

    @Override
    public DocumentContentDto document(String username, String kind) throws Exception {
        return applications.document(username, kind);
    }

    @Override
    public DriverApplicationDto approve(String username) throws Exception {
        return applications.approve(Caller.name(), username);
    }

    @Override
    public DriverApplicationDto reject(String username, ReasonDto reason) throws Exception {
        return applications.reject(Caller.name(), username, reason == null ? "" : reason.reason);
    }

    @Override
    public RideDto cancelRide(String id, ReasonDto reason) throws Exception {
        return rides.cancelByAdmin(Caller.name(), id, reason == null ? "" : reason.reason);
    }

    @Override
    public ReceiptDto refundRide(String id, ReasonDto reason) throws Exception {
        return payments.refund(id, Text.required(reason == null ? "" : reason.reason, 255,
                "why it is refunded"));
    }

    @Override
    public List<ReceiptDto> payments() throws Exception {
        return payments.allReceipts();
    }

    @Override
    public PricingDto pricing() throws Exception {
        return fares.pricing();
    }

    @Override
    public PricingDto savePricing(PricingDto pricing) throws Exception {
        return fares.save(Caller.name(), pricing);
    }
}
