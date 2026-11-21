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
package com.codenameone.examples.bazaar.api;

import com.codename1.annotations.Mapped;

/// A shipping rate. `minValue`/`maxValue` are grams for WEIGHT and cents for PRICE and FREE_OVER.
///
/// A CARRIER rate is priced by the store's carrier at checkout: `service`
/// lists the services to offer (comma separated, empty for all),
/// `markupBps` is added to the carrier's price in hundredths of a percent,
/// `priceCents` is a flat handling fee on top, and `fallbackCents` (0 for
/// none) is charged when the carrier cannot answer.
@Mapped
public record ShippingRateDto(
        String id,
        String zoneId,
        String name,
        String kind,
        long priceCents,
        long minValue,
        long maxValue,
        String carrier,
        String service,
        int etaDaysMin,
        int etaDaysMax,
        String locationId,
        int radiusMeters,
        int position,
        int markupBps,
        long fallbackCents) {
}
