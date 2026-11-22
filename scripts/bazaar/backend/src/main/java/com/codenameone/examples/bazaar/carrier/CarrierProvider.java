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
/// `bazaar.carrier.provider`: [SimulatedCarrier] for development, or
/// [EasyPostCarrier] for a real account. Another carrier is one more class
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

    /// The country parcels leave from when a shipment names none, or empty
    /// when it is not known. A parcel to another country is international:
    /// it carries a customs declaration and its packing slip lists the
    /// customs data.
    default String originCountry() {
        return "";
    }

    /// A parcel as a carrier prices it.
    final class Parcel {
        public final int weightGrams;
        /// What is in it, for the customs declaration of a parcel that
        /// crosses a border; empty when that is not known.
        public final List<CustomsItem> items;

        public Parcel(int weightGrams) {
            this(weightGrams, new java.util.ArrayList<CustomsItem>());
        }

        public Parcel(int weightGrams, List<CustomsItem> items) {
            this.weightGrams = weightGrams;
            this.items = items == null ? new java.util.ArrayList<CustomsItem>() : items;
        }
    }

    /// One line of a customs declaration: what it is, how many, what they are
    /// worth and weigh together, its HS code and where it was made.
    final class CustomsItem {
        public final String description;
        public final int quantity;
        /// The value of the whole line, in cents of `currency`.
        public final long valueCents;
        public final String currency;
        /// The weight of the whole line.
        public final int weightGrams;
        /// Digits only, or empty when the variant and product have none.
        public final String hsCode;
        /// ISO 3166 alpha-2, or empty.
        public final String originCountry;
        public final String sku;

        public CustomsItem(String description, int quantity, long valueCents, String currency,
                int weightGrams, String hsCode, String originCountry, String sku) {
            this.description = description == null ? "" : description;
            this.quantity = quantity;
            this.valueCents = valueCents;
            this.currency = currency == null ? "" : currency;
            this.weightGrams = weightGrams;
            this.hsCode = hsCode == null ? "" : hsCode;
            this.originCountry = originCountry == null ? "" : originCountry;
            this.sku = sku == null ? "" : sku;
        }
    }

    /// Whether a parcel from `from` to `to` (ISO countries, either may be
    /// empty when not known) crosses a border.
    static boolean crossesBorder(String from, String to) {
        return from != null && to != null && from.trim().length() == 2
                && to.trim().length() == 2 && !from.trim().equalsIgnoreCase(to.trim());
    }

    /// One end of a shipment.
    final class Place {
        public final String name;
        public final String address;
        public final String city;
        public final double lat;
        public final double lng;
        /// Postal code, region and two-letter country; empty when unknown.
        public final String postal;
        public final String region;
        public final String country;

        public Place(String name, String address, String city, double lat, double lng) {
            this(name, address, city, lat, lng, "", "", "");
        }

        public Place(String name, String address, String city, double lat, double lng,
                String postal, String region, String country) {
            this.postal = postal == null ? "" : postal;
            this.region = region == null ? "" : region;
            this.country = country == null ? "" : country;
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
        /// Where the printable label is, or empty.
        public final String labelUrl;

        public Label(String trackingNumber, String service, long costCents) {
            this(trackingNumber, service, costCents, "");
        }

        public Label(String trackingNumber, String service, long costCents, String labelUrl) {
            this.labelUrl = labelUrl == null ? "" : labelUrl;
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
