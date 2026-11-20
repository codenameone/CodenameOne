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

/// The carrier a server gets when the one it was told to use cannot be set
/// up: every call fails with an `IOException` naming what to fix, as an outage
/// of the carrier would, and the problem is logged at start-up.
///
/// There is deliberately no real carrier integration in this sample. A
/// production store writes one more [CarrierProvider] (EasyPost and Shippo
/// both take a parcel, two addresses and a service over REST and answer a
/// label URL and a tracking number) and selects it in
/// [com.codenameone.examples.bazaar.Services#carrierProvider].
public class MisconfiguredCarrier implements CarrierProvider {
    private final String provider;
    private final String problem;

    public MisconfiguredCarrier(String provider, String problem) {
        this.provider = provider;
        this.problem = problem;
        System.err.println("[config] " + problem);
    }

    @Override
    public String name() {
        return provider;
    }

    @Override
    public List<RateQuoteDto> rates(Parcel parcel, Place from, Place to) throws IOException {
        throw fail();
    }

    @Override
    public Label buyLabel(Parcel parcel, Place from, Place to, String service, String reference)
            throws IOException {
        throw fail();
    }

    @Override
    public List<Scan> track(String trackingNumber, long shippedAt, long now, Place from, Place to)
            throws IOException {
        throw fail();
    }

    private IOException fail() {
        System.err.println("[config] " + problem);
        return new IOException("Shipping labels are unavailable: " + problem);
    }
}
