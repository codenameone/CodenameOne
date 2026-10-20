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
package com.codenameone.examples.parley.admin;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.parley.Caller;
import com.codenameone.examples.parley.account.Accounts;
import com.codenameone.examples.parley.api.AdminApiServer;
import com.codenameone.examples.parley.api.AdminStatsDto;
import com.codenameone.examples.parley.api.BridgeLinkDto;
import com.codenameone.examples.parley.api.FlagDto;
import com.codenameone.examples.parley.api.ReasonDto;
import com.codenameone.examples.parley.api.ReportViewDto;
import com.codenameone.examples.parley.api.RoleChangeDto;
import com.codenameone.examples.parley.api.StatsSeriesDto;
import com.codenameone.examples.parley.api.SuspendDto;
import com.codenameone.examples.parley.api.UserDetailDto;
import com.codenameone.examples.parley.api.UserDto;
import com.codenameone.examples.parley.bridge.Bridges;
import com.codenameone.examples.parley.live.LiveHub;

import java.util.List;

/// The admin console's API. [com.codenameone.examples.parley.SecurityConfig]
/// lets only an admin reach `/api/admin/**`, so nothing here checks a role.
@Component
public class AdminEndpoint implements AdminApiServer {
    private final Accounts accounts;
    private final Statistics statistics;
    private final Reports reports;
    private final Bridges bridges;
    private final LiveHub live;

    public AdminEndpoint(Accounts accounts, Statistics statistics, Reports reports,
            Bridges bridges, LiveHub live) {
        this.accounts = accounts;
        this.statistics = statistics;
        this.reports = reports;
        this.bridges = bridges;
        this.live = live;
    }

    @Override
    public AdminStatsDto stats() throws Exception {
        return statistics.dashboard();
    }

    @Override
    public StatsSeriesDto statsSeries(int days) throws Exception {
        return statistics.series(days);
    }

    @Override
    public List<UserDto> users() throws Exception {
        return accounts.all();
    }

    @Override
    public UserDetailDto user(String username) throws Exception {
        return statistics.user(username);
    }

    @Override
    public UserDto setRoles(String username, RoleChangeDto roles) throws Exception {
        UserDto user = accounts.setRoles(Caller.name(), username, roles != null && roles.admin());
        // A live channel keeps the roles it was opened with: one opened by an
        // admin goes on receiving the admins' frames after the role is taken
        // away, since it asks for nothing again. Closed here, the app opens
        // another with a fresh ticket.
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
    public UserDto flag(String username, FlagDto flag) throws Exception {
        return accounts.flag(Caller.name(), username, flag != null && flag.flagged(),
                flag == null ? "" : flag.reason());
    }

    @Override
    public List<ReportViewDto> reports(String status) throws Exception {
        return reports.list(status);
    }

    @Override
    public ReportViewDto resolve(String id, ReasonDto outcome) throws Exception {
        return reports.resolve(Caller.name(), id, outcome == null ? "" : outcome.reason());
    }

    @Override
    public List<BridgeLinkDto> bridges() throws Exception {
        return bridges.all();
    }
}
