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
package com.codenameone.examples.morsel.admin;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PreAuthorize;
import com.codenameone.examples.morsel.Caller;
import com.codenameone.examples.morsel.Text;
import com.codenameone.examples.morsel.account.Accounts;
import com.codenameone.examples.morsel.api.AdminApiServer;
import com.codenameone.examples.morsel.api.AdminStatsDto;
import com.codenameone.examples.morsel.api.DocumentContentDto;
import com.codenameone.examples.morsel.api.CourierApplicationDto;
import com.codenameone.examples.morsel.api.CourierDto;
import com.codenameone.examples.morsel.api.FlagDto;
import com.codenameone.examples.morsel.api.PricingDto;
import com.codenameone.examples.morsel.api.ReasonDto;
import com.codenameone.examples.morsel.api.ReceiptDto;
import com.codenameone.examples.morsel.api.DeliveryDto;
import com.codenameone.examples.morsel.api.RoleChangeDto;
import com.codenameone.examples.morsel.api.StatsSeriesDto;
import com.codenameone.examples.morsel.api.SuspendDto;
import com.codenameone.examples.morsel.api.UserDetailDto;
import com.codenameone.examples.morsel.api.UserDto;
import com.codenameone.examples.morsel.courier.Couriers;
import com.codenameone.examples.morsel.onboarding.Applications;
import com.codenameone.examples.morsel.live.LiveHub;
import com.codenameone.examples.morsel.pay.Payments;
import com.codenameone.examples.morsel.delivery.Fares;
import com.codenameone.examples.morsel.delivery.Deliveries;

import java.util.List;

/// The server's half of `AdminApi`. Guarded by path in the filter chain and by
/// role here, as `CourierEndpoint` is.
///
/// This is the one place a name in the path chooses whose data is read: an
/// applicant's documents, a customer's receipts. Everywhere else the caller is
/// the subject, and only an admin gets to name somebody else.
@Component
@PreAuthorize("hasRole('ADMIN')")
public class AdminEndpoint implements AdminApiServer {
    private final Accounts accounts;
    private final Deliveries deliveries;
    private final Couriers couriers;
    private final Fares fares;
    private final LiveHub live;
    private final Applications applications;
    private final Payments payments;
    private final Statistics statistics;

    public AdminEndpoint(Accounts accounts, Deliveries deliveries, Couriers couriers,
            Fares fares, LiveHub live, Applications applications, Payments payments,
            Statistics statistics) {
        this.applications = applications;
        this.payments = payments;
        this.statistics = statistics;
        this.accounts = accounts;
        this.deliveries = deliveries;
        this.couriers = couriers;
        this.fares = fares;
        this.live = live;
    }

    @Override
    public AdminStatsDto stats() throws Exception {
        return statistics.dashboard();
    }

    @Override
    public List<DeliveryDto> deliveries() throws Exception {
        return deliveries.recent();
    }

    @Override
    public List<UserDto> users() throws Exception {
        return accounts.all();
    }

    @Override
    public List<CourierDto> couriers() throws Exception {
        return couriers.all();
    }

    @Override
    public UserDto setRoles(String username, RoleChangeDto roles) throws Exception {
        UserDto user = accounts.setRoles(Caller.name(), username, roles != null && roles.courier(),
                roles != null && roles.admin());
        // A live channel keeps the roles it was opened with: one opened by an
        // admin goes on receiving where every courier is after the role is
        // taken away, since it asks for nothing again. Closed here, the app
        // opens another with a fresh ticket and gets the frames of the roles
        // the account has now.
        live.drop(username);
        return user;
    }

    @Override
    public UserDto suspend(String username, SuspendDto suspend) throws Exception {
        UserDto user = accounts.suspend(Caller.name(), username,
                suspend != null && suspend.suspended(), suspend == null ? "" : suspend.reason());
        if (user.suspended()) {
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
        return accounts.flag(Caller.name(), username, flag != null && flag.flagged(),
                flag == null ? "" : flag.reason());
    }

    @Override
    public List<CourierApplicationDto> applications(String status) throws Exception {
        return applications.list(status);
    }

    @Override
    public CourierApplicationDto application(String username) throws Exception {
        return applications.review(username);
    }

    @Override
    public DocumentContentDto document(String username, String kind) throws Exception {
        return applications.document(username, kind);
    }

    @Override
    public CourierApplicationDto approve(String username) throws Exception {
        return applications.approve(Caller.name(), username);
    }

    @Override
    public CourierApplicationDto reject(String username, ReasonDto reason) throws Exception {
        return applications.reject(Caller.name(), username, reason == null ? "" : reason.reason());
    }

    @Override
    public DeliveryDto cancelDelivery(String id, ReasonDto reason) throws Exception {
        return deliveries.cancelByAdmin(Caller.name(), id, reason == null ? "" : reason.reason());
    }

    @Override
    public ReceiptDto refundDelivery(String id, ReasonDto reason) throws Exception {
        return payments.refund(id, Text.required(reason == null ? "" : reason.reason(), 255,
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
