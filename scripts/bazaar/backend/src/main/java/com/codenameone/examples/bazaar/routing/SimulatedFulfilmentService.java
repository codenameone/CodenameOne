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
package com.codenameone.examples.bazaar.routing;

import com.codenameone.examples.bazaar.carrier.CarrierProvider;

import java.util.ArrayList;
import java.util.List;

/// A fulfilment service that needs no account: every order is accepted at
/// once, ships one step later with a tracking number derived from its
/// reference, is in transit a step after that and delivered a step later.
/// Deterministic, so a test can name the moment each event happens.
public final class SimulatedFulfilmentService implements FulfilmentService {
    public static final String NAME = OrderRouting.SimulatedServiceName.NAME;
    /// The carrier the simulated warehouse ships with.
    public static final String CARRIER = "SIMULATED-3PL";
    private final long stepMillis;

    public SimulatedFulfilmentService(long stepMillis) {
        this.stepMillis = Math.max(1, stepMillis);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String submit(Submission order) {
        return "3PL-" + order.orderId + "-" + order.locationId;
    }

    /// The tracking number of a simulated shipment: `3PL` and 12 digits.
    public static String trackingNumber(String externalId) {
        long h = 1125899906842597L;
        for (int iter = 0; iter < externalId.length(); iter++) {
            h = 31 * h + externalId.charAt(iter);
        }
        StringBuilder b = new StringBuilder("3PL");
        String digits = Long.toString(Math.abs(h % 1000000000000L));
        for (int iter = digits.length(); iter < 12; iter++) {
            b.append('0');
        }
        return b.append(digits).toString();
    }

    @Override
    public Update status(String externalId, long submittedAt, long now) {
        long steps = (now - submittedAt) / stepMillis;
        if (steps < 1) {
            return new Update("ACCEPTED", "", "", new ArrayList<CarrierProvider.Scan>());
        }
        List<CarrierProvider.Scan> scans = new ArrayList<CarrierProvider.Scan>();
        if (steps >= 2) {
            scans.add(new CarrierProvider.Scan("IN_TRANSIT", "In transit from the fulfilment "
                    + "centre", "", 0, 0, submittedAt + 2 * stepMillis));
        }
        if (steps >= 3) {
            scans.add(new CarrierProvider.Scan("DELIVERED", "Delivered", "", 0, 0,
                    submittedAt + 3 * stepMillis));
        }
        return new Update("SHIPPED", CARRIER, trackingNumber(externalId), scans);
    }
}
