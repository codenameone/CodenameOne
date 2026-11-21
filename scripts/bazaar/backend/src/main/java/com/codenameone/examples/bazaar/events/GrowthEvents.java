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
package com.codenameone.examples.bazaar.events;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.journey.Journeys;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.marketing.Campaigns;
import com.codenameone.examples.bazaar.sale.SaleEvents;

/// What the growth features do when an order changes: points are earned and
/// reversed, a campaign that brought the order gets the credit, a journey
/// waiting on a purchase moves on, and a flash sale gets back the units of a
/// cancelled order. [StoreEvents] calls these after the order's transaction
/// commits; each step is caught alone so one failing never stops the rest,
/// and each is idempotent, since a paid order is told about more than once.
@Component
public class GrowthEvents {
    private final Loyalty loyalty;
    private final SaleEvents sales;
    private final Campaigns campaigns;
    private final Journeys journeys;

    public GrowthEvents(Loyalty loyalty, SaleEvents sales, Campaigns campaigns, Journeys journeys) {
        this.loyalty = loyalty;
        this.sales = sales;
        this.campaigns = campaigns;
        this.journeys = journeys;
    }

    /// An order was placed; `paid` when it was paid on the spot.
    public void placed(String orderId, boolean paid) {
        try {
            campaigns.attribute(orderId);
        } catch (Exception failed) {
            log("attribute", orderId, failed);
        }
        if (paid) {
            paid(orderId);
        }
    }

    /// An order's payment was taken.
    public void paid(String orderId) {
        try {
            loyalty.earn(orderId);
        } catch (Exception failed) {
            log("earn", orderId, failed);
        }
        try {
            journeys.orderPaid(orderId);
        } catch (Exception failed) {
            log("journey", orderId, failed);
        }
    }

    /// An order was cancelled.
    public void cancelled(String orderId) {
        try {
            loyalty.reverse(orderId, true);
        } catch (Exception failed) {
            log("reverse", orderId, failed);
        }
        try {
            sales.release(orderId);
        } catch (Exception failed) {
            log("release", orderId, failed);
        }
    }

    /// Money went back on an order.
    public void refunded(String orderId) {
        try {
            loyalty.reverse(orderId, false);
        } catch (Exception failed) {
            log("reverse", orderId, failed);
        }
    }

    private static void log(String step, String orderId, Exception failed) {
        System.err.println("growth " + step + " " + orderId + ": " + failed);
    }
}
