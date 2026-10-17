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
package com.codenameone.examples.savor.api;

import com.codename1.annotations.Mapped;

import java.util.List;

/// What a delivery would cost and how soon a courier could be there.
///
/// @param amountCents The fare of the product asked for, before the service fee.
/// @param couriersNearby Couriers online within reach of the pickup right now.
/// @param etaSeconds A rough time for the nearest of them to arrive, or 0 when there is none.
/// @param options The same delivery priced as each product, cheapest first.
@Mapped
public record FareQuoteDto(
        long amountCents,
        String currency,
        double distanceMeters,
        double durationSeconds,
        int couriersNearby,
        int etaSeconds,
        List<FareOptionDto> options) {
}
