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
