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
package com.codenameone.examples.bazaar.admin;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PreAuthorize;
import com.codename1.appkit.server.Caller;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.AdminApiServer;
import com.codenameone.examples.bazaar.api.AdminStatsDto;
import com.codenameone.examples.bazaar.api.FlagDto;
import com.codenameone.examples.bazaar.api.ReceiptDto;
import com.codenameone.examples.bazaar.api.RoleChangeDto;
import com.codenameone.examples.bazaar.api.StatsSeriesDto;
import com.codenameone.examples.bazaar.api.SuspendDto;
import com.codenameone.examples.bazaar.api.UserDetailDto;
import com.codenameone.examples.bazaar.api.UserDto;
import com.codenameone.examples.bazaar.live.LiveHub;
import com.codenameone.examples.bazaar.pay.Payments;

import java.util.List;

/// The server's half of `AdminApi`. Guarded by path in the filter chain and by
/// role here.
///
/// This is the one place a name in the path chooses whose data is read: a
/// customer's account and receipts. Everywhere else the caller is the subject,
/// and only an admin gets to name somebody else.
@Component
@PreAuthorize("hasRole('ADMIN')")
public class AdminEndpoint implements AdminApiServer {
    private final Accounts accounts;
    private final LiveHub live;
    private final Payments payments;
    private final Statistics statistics;

    public AdminEndpoint(Accounts accounts, LiveHub live, Payments payments,
            Statistics statistics) {
        this.accounts = accounts;
        this.live = live;
        this.payments = payments;
        this.statistics = statistics;
    }

    @Override
    public AdminStatsDto stats() throws Exception {
        return statistics.dashboard();
    }

    @Override
    public List<UserDto> users() throws Exception {
        return accounts.all();
    }

    @Override
    public UserDto setRoles(String username, RoleChangeDto roles) throws Exception {
        UserDto user = accounts.setRoles(Caller.name(), username, roles != null && roles.merchant(),
                roles != null && roles.admin());
        // A live channel keeps the roles it was opened with, since it asks for
        // nothing again. Closed here, the app opens another with a fresh ticket
        // and gets the frames of the roles the account has now.
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
    public List<ReceiptDto> payments() throws Exception {
        return payments.allReceipts();
    }
}
