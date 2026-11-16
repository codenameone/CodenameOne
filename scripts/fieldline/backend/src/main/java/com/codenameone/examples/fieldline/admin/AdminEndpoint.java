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
package com.codenameone.examples.fieldline.admin;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.fieldline.account.Accounts;
import com.codenameone.examples.fieldline.api.AccountChangeDto;
import com.codenameone.examples.fieldline.api.AdminApiServer;
import com.codenameone.examples.fieldline.api.CrewMemberDto;
import com.codenameone.examples.fieldline.api.PolicyDto;
import com.codenameone.examples.fieldline.api.RateDto;
import com.codenameone.examples.fieldline.api.TimesheetDto;
import com.codenameone.examples.fieldline.api.UserDto;
import com.codenameone.examples.fieldline.field.Crew;
import com.codenameone.examples.fieldline.field.Planner;
import com.codenameone.examples.fieldline.field.Timekeeping;
import com.codenameone.examples.fieldline.live.LiveHub;
import java.util.List;

/// The server's half of `AdminApi`. Every path is under `/api/admin`, which
/// the security configuration opens to admins alone.
@Component
public class AdminEndpoint implements AdminApiServer {
    private final Accounts accounts;
    private final Crew crew;
    private final Planner planner;
    private final LiveHub live;
    private final Timekeeping clock;

    public AdminEndpoint(Accounts accounts, Crew crew, Planner planner, LiveHub live, Timekeeping clock) {
        this.clock = clock;
        this.accounts = accounts;
        this.crew = crew;
        this.planner = planner;
        this.live = live;
    }

    @Override
    public List<UserDto> accounts() throws Exception {
        return accounts.all();
    }

    @Override
    public UserDto changeAccount(AccountChangeDto change) throws Exception {
        boolean replan = accounts.change(Caller.name(), change);
        String username = Accounts.email(change.username());
        // A live channel keeps the roles it was opened with, and makes no
        // further request a suspension would refuse: it is closed, and the
        // app opens another with what the account may see now.
        live.drop(username);
        if (replan) {
            // The plan was made with or without this technician; it is
            // repaired after the change has committed.
            planner.repair(Planner.CREW, "Crew changed: " + username, Caller.name());
        }
        return accounts.describe(username);
    }

    @Override
    public List<CrewMemberDto> technicians() throws Exception {
        return crew.members();
    }

    @Override
    public CrewMemberDto changeTechnician(CrewMemberDto change) throws Exception {
        CrewMemberDto saved = crew.change(change);
        planner.repair(Planner.CREW, "Crew changed: " + saved.name(), Caller.name());
        return saved;
    }

    @Override
    public List<RateDto> rates() throws Exception {
        return clock.rates();
    }

    @Override
    public List<RateDto> saveRate(RateDto rate) throws Exception {
        return clock.saveRate(rate);
    }

    @Override
    public TimesheetDto timesheets(long week) throws Exception {
        return clock.timesheets(week);
    }

    @Override
    public PolicyDto policy() throws Exception {
        return Planner.toDto(planner.policy());
    }

    @Override
    public PolicyDto changePolicy(PolicyDto change) throws Exception {
        return planner.changePolicy(change, Caller.name());
    }
}
