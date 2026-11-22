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

import java.io.IOException;
import java.util.List;

/// A fulfilment service (third-party logistics, 3PL): a warehouse the store
/// does not run, which is handed the orders routed to its location and
/// ships them itself. [ThirdPartyFulfilment] hands orders over and turns
/// what the service reports into shipments; a real service is one more
/// implementation (its API behind [#submit] and [#status]).
public interface FulfilmentService {
    /// The name stored with each order handed over, e.g. `SIMULATED`.
    String name();

    /// Hands an order over; answers the service's own reference for it.
    /// Called once per order and location: the caller keeps the answer.
    String submit(Submission order) throws IOException;

    /// What became of an order handed over at `submittedAt`, as of `now`.
    Update status(String externalId, long submittedAt, long now) throws IOException;

    /// An order as the service receives it.
    final class Submission {
        public final String orderId;
        public final long orderNumber;
        public final String locationId;
        public final List<String> skus;

        public Submission(String orderId, long orderNumber, String locationId,
                List<String> skus) {
            this.orderId = orderId;
            this.orderNumber = orderNumber;
            this.locationId = locationId;
            this.skus = skus;
        }
    }

    /// ACCEPTED (not shipped yet), or SHIPPED with the carrier and tracking
    /// number and the scans of the parcel since.
    final class Update {
        public final String status;
        public final String carrier;
        public final String trackingNumber;
        public final List<CarrierProvider.Scan> scans;

        public Update(String status, String carrier, String trackingNumber,
                List<CarrierProvider.Scan> scans) {
            this.status = status;
            this.carrier = carrier;
            this.trackingNumber = trackingNumber;
            this.scans = scans;
        }
    }
}
