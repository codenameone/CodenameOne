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
package com.codenameone.examples.wayline.driver;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PreAuthorize;
import com.codenameone.examples.wayline.Caller;
import com.codenameone.examples.wayline.api.DriverApiServer;
import com.codenameone.examples.wayline.api.DriverDto;
import com.codenameone.examples.wayline.api.DriverStatusDto;
import com.codenameone.examples.wayline.api.EarningsDto;
import com.codenameone.examples.wayline.api.EarningsSummaryDto;
import com.codenameone.examples.wayline.api.PayoutAccountDto;
import com.codenameone.examples.wayline.api.PayoutDto;
import com.codenameone.examples.wayline.api.RideDto;
import com.codenameone.examples.wayline.ride.Rides;

import java.util.List;

/// The server's half of `DriverApi`.
///
/// Guarded twice, on purpose. The filter chain refuses `/api/driver/**` to
/// anyone who is not a driver, by path; `@PreAuthorize` refuses these methods to
/// anyone who is not a driver, whatever path led to them. Either alone would do
/// today. The second is what still holds after someone moves a path.
@Component
@PreAuthorize("hasRole('DRIVER')")
public class DriverEndpoint implements DriverApiServer {
    private final Drivers drivers;
    private final Rides rides;
    private final Earnings earnings;

    public DriverEndpoint(Drivers drivers, Rides rides, Earnings earnings) {
        this.earnings = earnings;
        this.drivers = drivers;
        this.rides = rides;
    }

    @Override
    public DriverDto status(DriverStatusDto status) throws Exception {
        return drivers.report(Caller.name(), status);
    }

    @Override
    public DriverDto me() throws Exception {
        return drivers.describe(Caller.name());
    }

    @Override
    public RideDto active() throws Exception {
        return rides.activeForDriver(Caller.name());
    }

    @Override
    public RideDto accept(String id) throws Exception {
        return rides.accept(Caller.name(), id);
    }

    @Override
    public RideDto decline(String id) throws Exception {
        return rides.decline(Caller.name(), id);
    }

    @Override
    public RideDto arrived(String id) throws Exception {
        return rides.arrived(Caller.name(), id);
    }

    @Override
    public RideDto start(String id) throws Exception {
        return rides.start(Caller.name(), id);
    }

    @Override
    public RideDto complete(String id) throws Exception {
        return rides.complete(Caller.name(), id);
    }

    @Override
    public RideDto cancel(String id) throws Exception {
        return rides.cancelByDriver(Caller.name(), id);
    }

    @Override
    public EarningsDto earnings() throws Exception {
        return drivers.earnings(Caller.name());
    }

    @Override
    public EarningsSummaryDto earningsSummary() throws Exception {
        return earnings.summary(Caller.name());
    }

    @Override
    public List<PayoutDto> payouts() throws Exception {
        return earnings.payouts(Caller.name());
    }

    @Override
    public PayoutDto cashOut() throws Exception {
        return earnings.cashOut(Caller.name());
    }

    @Override
    public PayoutAccountDto payoutAccount() throws Exception {
        return earnings.account(Caller.name());
    }

    @Override
    public PayoutAccountDto savePayoutAccount(PayoutAccountDto account) throws Exception {
        return earnings.saveAccount(Caller.name(), account);
    }

    @Override
    public List<RideDto> history() throws Exception {
        return rides.historyForDriver(Caller.name());
    }
}
