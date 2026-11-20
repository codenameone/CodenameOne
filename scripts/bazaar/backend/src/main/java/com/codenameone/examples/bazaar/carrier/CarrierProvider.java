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
package com.codenameone.examples.bazaar.carrier;

import com.codenameone.examples.bazaar.api.RateQuoteDto;

import java.io.IOException;
import java.util.List;

/// A parcel carrier: prices, labels and scans.
///
/// [com.codenameone.examples.bazaar.Services#carrierProvider] picks one from
/// `bazaar.carrier.provider`. Only [SimulatedCarrier] ships with the sample: a
/// real integration (EasyPost, Shippo, a carrier's own API) is one more class
/// implementing this, chosen there the way the payment provider is.
public interface CarrierProvider {
    /// The carrier's name as stored on a fulfilment.
    String name();

    /// What each service would charge to carry `parcel` from `from` to `to`,
    /// cheapest first.
    List<RateQuoteDto> rates(Parcel parcel, Place from, Place to) throws IOException;

    /// Buys a label for one service. `reference` (the fulfilment id) makes a
    /// retry buy the same label rather than a second one.
    Label buyLabel(Parcel parcel, Place from, Place to, String service, String reference)
            throws IOException;

    /// The scans of a shipment up to `now`. A real carrier knows its own
    /// parcels and needs only the number; the simulated one keeps nothing, so
    /// it is also told when the parcel was handed over and between where.
    List<Scan> track(String trackingNumber, long shippedAt, long now, Place from, Place to)
            throws IOException;

    /// A parcel as a carrier prices it.
    final class Parcel {
        public final int weightGrams;

        public Parcel(int weightGrams) {
            this.weightGrams = weightGrams;
        }
    }

    /// One end of a shipment.
    final class Place {
        public final String name;
        public final String address;
        public final String city;
        public final double lat;
        public final double lng;

        public Place(String name, String address, String city, double lat, double lng) {
            this.name = name == null ? "" : name;
            this.address = address == null ? "" : address;
            this.city = city == null ? "" : city;
            this.lat = lat;
            this.lng = lng;
        }
    }

    /// A bought label.
    final class Label {
        public final String trackingNumber;
        public final String service;
        public final long costCents;

        public Label(String trackingNumber, String service, long costCents) {
            this.trackingNumber = trackingNumber;
            this.service = service;
            this.costCents = costCents;
        }
    }

    /// One scan: a status of [com.codenameone.examples.bazaar.fulfil.Fulfillments]
    /// (IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED) and where it happened.
    final class Scan {
        public final String status;
        public final String message;
        public final String place;
        public final double lat;
        public final double lng;
        public final long at;

        public Scan(String status, String message, String place, double lat, double lng,
                long at) {
            this.status = status;
            this.message = message;
            this.place = place;
            this.lat = lat;
            this.lng = lng;
            this.at = at;
        }
    }
}
