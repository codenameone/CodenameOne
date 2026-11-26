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

/// An order. Lists carry no lines, events, tenders or fulfilments; a detail carries all four. `paymentStatus` is the card payment's state, empty when nothing was charged to a card. `tipCents` is the tip, outside the total and never taxed or discounted; `fields` are the answers to the store's checkout fields.
@Mapped
public record OrderDto(
        String id,
        String storeId,
        int number,
        String channel,
        String status,
        String financial,
        String fulfilment,
        String username,
        String email,
        long subtotalCents,
        long discountCents,
        long shippingCents,
        long taxCents,
        long totalCents,
        long refundedCents,
        String currency,
        String discountCode,
        AddressDto address,
        String shippingRateId,
        String shippingTitle,
        String note,
        String tags,
        long createdAt,
        String paymentStatus,
        java.util.List<OrderLineDto> lines,
        java.util.List<OrderEventDto> events,
        java.util.List<TenderDto> tenders,
        java.util.List<FulfillmentSummaryDto> fulfillments,
        long tipCents,
        java.util.List<OrderFieldDto> fields) {
}
