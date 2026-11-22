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

import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.RateQuoteDto;

import com.codename1.backend.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// A carrier that moves no parcels: rates from weight and distance, tracking
/// numbers derived from the fulfilment id (so buying twice answers the same
/// label), and scans that follow the parcel from the origin through a hub to
/// the door, one every `stepMillis` after it was handed over.
///
/// Rates are deterministic: the same weight and ends always cost the same.
/// A destination with postal code [#OUTAGE_POSTAL] is refused with an
/// IOException, as an unreachable carrier would be, so the checkout's
/// fallback can be seen working in development and tests.
public class SimulatedCarrier implements CarrierProvider {
    public static final String NAME = "simulated";
    /// The postal code the simulated carrier fails for.
    public static final String OUTAGE_POSTAL = "00000";

    /// Service code, title, base cents, cents per kilogram, cents per 100 km,
    /// days.
    private static final Object[][] SERVICES = {
        {"GROUND", "Ground", 495L, 90L, 25L, 5},
        {"EXPRESS", "Express", 1295L, 160L, 45L, 2},
        {"OVERNIGHT", "Overnight", 2995L, 250L, 80L, 1},
    };

    /// When nothing says where an end is, the parcel is taken to travel this far.
    private static final double UNKNOWN_KM = 500;

    private final long stepMillis;
    private final String originCountry;

    /// @param stepMillis the time between two scans
    public SimulatedCarrier(long stepMillis) {
        this(stepMillis, "");
    }

    /// @param stepMillis the time between two scans
    /// @param originCountry the country parcels leave from, or empty
    public SimulatedCarrier(long stepMillis, String originCountry) {
        this.stepMillis = Math.max(1, stepMillis);
        this.originCountry = originCountry == null ? "" : originCountry.trim();
    }

    @Override
    public String originCountry() {
        return originCountry;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<RateQuoteDto> rates(Parcel parcel, Place from, Place to) throws IOException {
        if (to != null && OUTAGE_POSTAL.equals(to.postal.trim())) {
            throw new IOException("The simulated carrier is unreachable for " + OUTAGE_POSTAL);
        }
        List<RateQuoteDto> out = new ArrayList<RateQuoteDto>();
        for (int iter = 0; iter < SERVICES.length; iter++) {
            out.add(quote(SERVICES[iter], parcel, from, to));
        }
        return out;
    }

    @Override
    public Label buyLabel(Parcel parcel, Place from, Place to, String service, String reference)
            throws IOException {
        Object[] row = service(service);
        if (row == null) {
            throw new ResponseStatusException(400, "No such service: " + service);
        }
        return new Label(trackingNumber(reference), (String) row[0],
                quote(row, parcel, from, to).cents());
    }

    /// `SIM` and twelve digits, the same for the same reference.
    public static String trackingNumber(String reference) throws IOException {
        String hash = Ids.sha256("label:" + reference);
        StringBuilder out = new StringBuilder("SIM");
        for (int iter = 0; iter < hash.length() && out.length() < 15; iter++) {
            char ch = hash.charAt(iter);
            out.append((char) ('0' + (Character.digit(ch, 16) % 10)));
        }
        return out.toString();
    }

    @Override
    public List<Scan> track(String trackingNumber, long shippedAt, long now, Place from,
            Place to) {
        List<Scan> plan = new ArrayList<Scan>();
        String origin = from.city.length() > 0 ? from.city : from.name;
        String dest = to.city.length() > 0 ? to.city : "Destination";
        boolean located = from.lat != 0 && to.lat != 0;
        plan.add(new Scan("IN_TRANSIT", "Departed origin facility", origin + " sort center",
                from.lat, from.lng, shippedAt + stepMillis));
        plan.add(new Scan("IN_TRANSIT", "Arrived at regional hub", "Regional hub",
                located ? (from.lat + to.lat) / 2 : 0, located ? (from.lng + to.lng) / 2 : 0,
                shippedAt + 2 * stepMillis));
        plan.add(new Scan("OUT_FOR_DELIVERY", "Out for delivery", dest, to.lat, to.lng,
                shippedAt + 3 * stepMillis));
        plan.add(new Scan("DELIVERED", "Delivered", dest, to.lat, to.lng,
                shippedAt + 4 * stepMillis));
        List<Scan> out = new ArrayList<Scan>();
        for (int iter = 0; iter < plan.size(); iter++) {
            if (plan.get(iter).at <= now) {
                out.add(plan.get(iter));
            }
        }
        return out;
    }

    private static Object[] service(String code) {
        for (int iter = 0; iter < SERVICES.length; iter++) {
            if (((String) SERVICES[iter][0]).equalsIgnoreCase(code)) {
                return SERVICES[iter];
            }
        }
        return null;
    }

    private static RateQuoteDto quote(Object[] row, Parcel parcel, Place from, Place to) {
        long grams = Math.max(1, parcel.weightGrams);
        long kilos = (grams + 999) / 1000;
        long hundredKm = (long) Math.ceil(km(from, to) / 100.0);
        long cents = ((Long) row[2]).longValue() + kilos * ((Long) row[3]).longValue()
                + hundredKm * ((Long) row[4]).longValue();
        return new RateQuoteDto(NAME, (String) row[0], (String) row[1], cents,
                ((Integer) row[5]).intValue());
    }

    /// Great-circle distance in kilometres.
    static double km(Place a, Place b) {
        if (a == null || b == null || a.lat == 0 && a.lng == 0 || b.lat == 0 && b.lng == 0) {
            return UNKNOWN_KM;
        }
        double p1 = Math.toRadians(a.lat);
        double p2 = Math.toRadians(b.lat);
        double dp = p2 - p1;
        double dl = Math.toRadians(b.lng - a.lng);
        double h = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }
}
