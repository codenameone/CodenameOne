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
package com.codenameone.examples.morsel.courier;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PreAuthorize;
import com.codenameone.examples.morsel.Caller;
import com.codenameone.examples.morsel.api.CourierApiServer;
import com.codenameone.examples.morsel.api.CourierDto;
import com.codenameone.examples.morsel.api.CourierStatusDto;
import com.codenameone.examples.morsel.api.EarningsDto;
import com.codenameone.examples.morsel.api.EarningsSummaryDto;
import com.codenameone.examples.morsel.api.PayoutAccountDto;
import com.codenameone.examples.morsel.api.PayoutDto;
import com.codenameone.examples.morsel.api.DeliveryDto;
import com.codenameone.examples.morsel.delivery.Deliveries;

import java.util.List;

/// The server's half of `CourierApi`.
///
/// Guarded twice, on purpose. The filter chain refuses `/api/courier/**` to
/// anyone who is not a courier, by path; `@PreAuthorize` refuses these methods to
/// anyone who is not a courier, whatever path led to them. Either alone would do
/// today. The second is what still holds after someone moves a path.
@Component
@PreAuthorize("hasRole('COURIER')")
public class CourierEndpoint implements CourierApiServer {
    private final Couriers couriers;
    private final Deliveries deliveries;
    private final Earnings earnings;

    public CourierEndpoint(Couriers couriers, Deliveries deliveries, Earnings earnings) {
        this.earnings = earnings;
        this.couriers = couriers;
        this.deliveries = deliveries;
    }

    @Override
    public CourierDto status(CourierStatusDto status) throws Exception {
        return couriers.report(Caller.name(), status);
    }

    @Override
    public CourierDto me() throws Exception {
        return couriers.describe(Caller.name());
    }

    @Override
    public DeliveryDto active() throws Exception {
        return deliveries.activeForCourier(Caller.name());
    }

    @Override
    public DeliveryDto accept(String id) throws Exception {
        return deliveries.accept(Caller.name(), id);
    }

    @Override
    public DeliveryDto decline(String id) throws Exception {
        return deliveries.decline(Caller.name(), id);
    }

    @Override
    public DeliveryDto arrived(String id) throws Exception {
        return deliveries.arrived(Caller.name(), id);
    }

    @Override
    public DeliveryDto start(String id) throws Exception {
        return deliveries.start(Caller.name(), id);
    }

    @Override
    public DeliveryDto complete(String id) throws Exception {
        return deliveries.complete(Caller.name(), id);
    }

    @Override
    public DeliveryDto cancel(String id) throws Exception {
        return deliveries.cancelByCourier(Caller.name(), id);
    }

    @Override
    public EarningsDto earnings() throws Exception {
        return couriers.earnings(Caller.name());
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
    public List<DeliveryDto> history() throws Exception {
        return deliveries.historyForCourier(Caller.name());
    }
}
