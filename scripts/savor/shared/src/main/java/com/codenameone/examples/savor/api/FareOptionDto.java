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

/// One product a delivery can be taken as, priced for the delivery being quoted.
///
/// @param product `standard`, `comfort` or `xl`.
/// @param fare Cents, like every amount here.
/// @param total What the customer would pay: [#fare()] plus [#serviceFee()].
/// @param couriersNearby Couriers of this product in reach; 0 when there is none.
/// @param etaMinutes Minutes for the nearest of them to arrive; 0 when there is none.
@Mapped
public record FareOptionDto(
        String product,
        String name,
        String description,
        int seats,
        long fare,
        long serviceFee,
        long total,
        int couriersNearby,
        int etaMinutes) {
}
