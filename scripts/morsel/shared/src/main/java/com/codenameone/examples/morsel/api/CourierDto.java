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
package com.codenameone.examples.morsel.api;

import com.codename1.annotations.Mapped;

/// A courier, for the admin's fleet view and for the courier's own status.
///
/// @param approved Whether the onboarding application was approved. Only an approved courier can go on
///     line.
/// @param product `standard`, `comfort` or `xl`: the kind of delivery this car is offered.
/// @param acceptanceRate The share of the offers this courier was sent that they took, 0 to 1; 1
///     before any was sent.
@Mapped
public record CourierDto(
        String username,
        String displayName,
        String vehicle,
        String plate,
        boolean online,
        double lat,
        double lng,
        double heading,
        double rating,
        long lastSeen,
        int deliveries,
        boolean approved,
        String product,
        String gender,
        boolean accessible,
        double acceptanceRate,
        boolean flagged) {
}
