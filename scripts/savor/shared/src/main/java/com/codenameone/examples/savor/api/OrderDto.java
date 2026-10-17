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

import java.util.List;

/// An order, for its customer, its restaurant, its courier or an admin.
///
/// @param etaMinutes Until it is at the door, as the server estimates it now; 0 once it has ended.
/// @param reason Why it was rejected or cancelled.
/// @param delivery Its courier's leg, or one whose state is NONE before the restaurant accepts.
public record OrderDto(
        String id,
        OrderState state,
        String restaurantId,
        String restaurantName,
        String restaurantAddress,
        double restaurantLat,
        double restaurantLng,
        String customer,
        String customerName,
        List<OrderLineDto> lines,
        long subtotalCents,
        long deliveryFeeCents,
        long serviceFeeCents,
        long smallOrderCents,
        long taxCents,
        long discountCents,
        long tipCents,
        long totalCents,
        String currency,
        double dropoffLat,
        double dropoffLng,
        String dropoffAddress,
        String note,
        String methodLabel,
        String paymentStatus,
        int prepMinutes,
        long placedAt,
        long acceptedAt,
        long readyAt,
        long deliveredAt,
        int etaMinutes,
        String reason,
        int stars,
        DeliveryDto delivery) {
}
