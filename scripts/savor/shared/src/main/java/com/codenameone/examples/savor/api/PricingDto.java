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

/// What an order costs on top of its food, and what the service keeps of it.
///
/// @param baseCents The delivery fee's base.
/// @param perKmCents Added to it for each kilometre from the restaurant to the door.
/// @param surge The delivery fee is multiplied by it.
/// @param smallOrderCents Food worth less than this pays the difference ...
/// @param smallOrderFeeCents ... up to this much.
/// @param commissionPercent Kept of the food; the restaurant is paid the rest.
@Mapped
public record PricingDto(
        String currency,
        long baseCents,
        long perKmCents,
        double surge,
        long smallOrderCents,
        long smallOrderFeeCents,
        double serviceFeePercent,
        double taxPercent,
        double commissionPercent) {
}
