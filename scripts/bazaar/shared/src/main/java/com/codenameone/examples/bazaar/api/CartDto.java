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

/// A cart with every amount worked out. `token` is what a guest sends back in the `X-Cart-Token` header. With tax inclusive, `taxCents` is already inside the prices and not added to `totalCents`. `giftCardCents` is what the gift card will pay; `dueCents` what is left for the card. `problems` lists why the cart cannot be checked out as it is. `shippingRequired` is false for a cart of digital goods only, which checks out without an address or a delivery option.
@Mapped
public record CartDto(
        String id,
        String storeId,
        String token,
        String currency,
        java.util.List<CartLineDto> lines,
        String discountCode,
        String giftCardCode,
        long subtotalCents,
        long discountCents,
        long shippingCents,
        long taxCents,
        boolean taxInclusive,
        long totalCents,
        long giftCardCents,
        long dueCents,
        boolean freeShipping,
        java.util.List<AppliedDiscountDto> discounts,
        java.util.List<ShippingOptionDto> shippingOptions,
        String shippingRateId,
        java.util.List<String> problems,
        boolean shippingRequired) {
}
